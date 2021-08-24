/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Backup.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.memory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Collection;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.sync.CrontabSync;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMReceiver;
import org.rvpf.service.som.SOMSender;
import org.rvpf.som.queue.FilesQueue;
import org.rvpf.som.queue.QueueServerImpl;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServiceAppImpl;

/**
 * Backup.
 */
final class Backup
    extends TimerTask
{
    /**
     * Constructs an instance.
     *
     * @param memoryBackEnd The owner.
     */
    Backup(@Nonnull final MemoryBackEnd memoryBackEnd)
    {
        _memoryBackEnd = memoryBackEnd;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        _save();

        _scheduleBackup();
    }

    /**
     * Sets up this.
     *
     * @param storeAppImpl The calling store service.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull final StoreServiceAppImpl storeAppImpl)
    {
        final KeyedGroups backupProperties = storeAppImpl
            .getServerProperties()
            .getGroup(BACKUP_PROPERTIES);

        if (backupProperties.getBoolean(DISABLED_PROPERTY)) {
            return true;    // The backup will be disabled.
        }

        Optional<String> propertyValue;

        _queueProperties = new KeyedGroups();
        _queueProperties
            .setValue(
                FilesQueue.ROOT_PROPERTY,
                storeAppImpl.getStoreDataDir().getAbsolutePath());

        propertyValue = backupProperties.getString(DIRECTORY_PROPERTY);

        if (propertyValue.isPresent()) {
            _queueProperties
                .setValue(
                    FilesQueue.DIRECTORY_PROPERTY,
                    backupProperties.getString(DIRECTORY_PROPERTY).get());
        }

        _queueProperties
            .setValue(
                QueueServerImpl.NAME_PROPERTY,
                backupProperties
                    .getString(NAME_PROPERTY, Optional.of(DEFAULT_BACKUP_NAME))
                    .get());
        _queueProperties.setValue(SOMFactory.PRIVATE_PROPERTY, Boolean.TRUE);
        _queueProperties.setValue(FilesQueue.BACKUP_PROPERTY, Boolean.TRUE);

        propertyValue = backupProperties.getString(COMPRESSED_PROPERTY);

        if (propertyValue.isPresent()) {
            _queueProperties
                .setValue(
                    FilesQueue.COMPRESSED_PROPERTY,
                    backupProperties.getString(COMPRESSED_PROPERTY).get());
        }

        _queueProperties.freeze();

        _somFactory = new SOMFactory(storeAppImpl.getConfig());

        final String schedule = backupProperties
            .getString(SCHEDULE_PROPERTY, Optional.of(""))
            .get()
            .trim();

        if (!schedule.isEmpty()) {
            _timer = storeAppImpl.getTimer();
            _scheduleSync = new CrontabSync();

            if (!_scheduleSync.setUp(schedule)) {
                return false;
            }

            _LOGGER.debug(StoreMessages.BACKUP_SCHEDULE, schedule);
        }

        return true;
    }

    /**
     * Starts.
     *
     * @throws ServiceNotAvailableException When the restore fails.
     */
    void start()
        throws ServiceNotAvailableException
    {
        if (!_restore()) {
            throw new ServiceNotAvailableException();
        }

        _scheduleBackup();
    }

    /**
     * Stops.
     */
    void stop()
    {
        _scheduleSync = null;

        _save();
    }

    /**
     * Tears down what has been set up.
     */
    void tearDown() {}

    private boolean _restore()
    {
        if (_queueProperties == null) {    // Disabled.
            return true;
        }

        SOMFactory.Queue factoryQueue = _somFactory
            .createQueue(_queueProperties);
        SOMReceiver receiver = factoryQueue.createReceiver(false);

        if (receiver == null) {
            return false;
        }

        final QueueServerImpl server = (QueueServerImpl) receiver
            .getServer()
            .get();
        final FilesQueue queue = (FilesQueue) server.getQueue();

        if (queue.getInfo().getFileCount() == 0) {
            final File directoryFile = queue.getDirectoryFile();
            final String dataSuffix = queue.getDataSuffix();
            final String backupSuffix = queue.getBackupSuffix().get();
            final File[] backupFiles = queue.listEntryFiles(backupSuffix);

            receiver.tearDown();

            if (backupFiles.length == 0) {
                return true;    // Empty store.
            }

            // Restores from the last backup.

            _LOGGER.debug(StoreMessages.RESTORE_STARTED);
            _LOGGER
                .info(
                    StoreMessages.RECOVERING_BACKUP_FILES,
                    String.valueOf(backupFiles.length));

            final Path backupPath = backupFiles[backupFiles.length - 1]
                .toPath();
            final String backupString = backupPath.toString();
            final String dataString = backupString
                .substring(
                    0,
                    backupString.length() - backupSuffix.length()) + dataSuffix;
            final Path dataPath = Paths.get(dataString);

            try {
                Files.move(backupPath, dataPath);
            } catch (final IOException exception) {
                Logger
                    .getInstance(getClass())
                    .error(
                        ServiceMessages.QUEUE_FILE_RENAME_FAILED,
                        directoryFile,
                        backupPath.toFile().getName(),
                        dataPath.toFile().getName(),
                        exception.getMessage());

                return false;
            }

            factoryQueue = _somFactory.createQueue(_queueProperties);
            receiver = factoryQueue.createReceiver(false);

            if (receiver == null) {
                return false;
            }
        } else {
            _LOGGER.debug(StoreMessages.RESTORE_STARTED);
        }

        for (;;) {
            final Serializable[] pointValues = receiver
                .receive(_BATCH_LIMIT, 0);

            if (pointValues == null) {
                return false;
            }

            if (pointValues.length == 0) {
                break;
            }

            for (final Serializable pointValue: pointValues) {
                _memoryBackEnd.update((VersionedValue) pointValue);
            }
        }

        Require.success(receiver.commit());

        receiver.tearDown();

        _LOGGER.info(StoreMessages.RESTORE_COMPLETED);

        return true;
    }

    /**
     * Saves.
     */
    private void _save()
    {
        if (_queueProperties == null) {    // Disabled.
            return;
        }

        _LOGGER.debug(StoreMessages.BACKUP_STARTED);

        final SOMFactory.Queue factoryQueue = _somFactory
            .createQueue(_queueProperties);
        final SOMSender sender = factoryQueue.createSender(false);

        if (sender == null) {
            return;
        }

        Collection<VersionedValue> pointValues = _memoryBackEnd
            .getSnapshotValues()
            .orElse(null);

        if (pointValues != null) {
            if (!pointValues.isEmpty()) {
                Require
                    .success(
                        sender
                            .send(
                                    pointValues
                                            .toArray(new VersionedValue[pointValues.size()]),
                                            false));
            }
        } else {
            final Optional<Collection<NavigableMap<DateTime, VersionedValue>>> archiveMaps =
                _memoryBackEnd
                    .getArchiveMaps();

            if (archiveMaps.isPresent()) {
                for (final NavigableMap<DateTime, VersionedValue> archiveMap:
                        archiveMaps.get()) {
                    pointValues = archiveMap.values();

                    if (!pointValues.isEmpty()) {
                        Require
                            .success(
                                sender
                                    .send(
                                            pointValues
                                                    .toArray(new VersionedValue[pointValues.size()]),
                                                    false));
                    }
                }
            }
        }

        Require.success(sender.commit());

        final QueueServerImpl server = (QueueServerImpl) sender
            .getServer()
            .get();
        final FilesQueue queue = (FilesQueue) server.getQueue();
        final File directoryFile = queue.getDirectoryFile();
        final File[] backupFiles = queue
            .listEntryFiles(queue.getBackupSuffix().get());

        sender.tearDown();

        for (final File backupFile: backupFiles) {
            try {
                Files.delete(backupFile.toPath());
            } catch (final FileSystemException exception) {
                _LOGGER
                    .warn(
                        ServiceMessages.QUEUE_FILE_DELETE_FAILED,
                        directoryFile,
                        backupFile.getName(),
                        exception.getReason());
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        _LOGGER.info(StoreMessages.BACKUP_COMPLETED);
    }

    /**
     * Schedules the next backup.
     */
    private void _scheduleBackup()
    {
        final Sync schedule = _scheduleSync;
        final Optional<Timer> timer = _timer;

        if ((schedule != null) && timer.isPresent()) {
            final DateTime nextStamp = _scheduleSync
                .getNextStamp(DateTime.now())
                .get();

            try {
                timer.get().schedule(this, nextStamp.toTimestamp());
            } catch (final IllegalStateException exception) {
                // The service is stopping.
            }
        }
    }

    /** Backup properties. */
    public static final String BACKUP_PROPERTIES = "backup";

    /** Compressed property. */
    public static final String COMPRESSED_PROPERTY = "compressed";

    /** Default queue name. */
    public static final String DEFAULT_BACKUP_NAME = "backup";

    /** The directory property. */
    public static final String DIRECTORY_PROPERTY = "directory";

    /** Disabled property. */
    public static final String DISABLED_PROPERTY = "disabled";

    /** The name property. */
    public static final String NAME_PROPERTY = "name";

    /** The schedule property. */
    public static final String SCHEDULE_PROPERTY = "schedule";
    private static final int _BATCH_LIMIT = 1000;
    private static final Logger _LOGGER = Logger.getInstance(Backup.class);

    private final MemoryBackEnd _memoryBackEnd;
    private KeyedGroups _queueProperties;
    private volatile CrontabSync _scheduleSync;
    private SOMFactory _somFactory;
    private Optional<Timer> _timer = Optional.empty();
}

/* This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * version 2.1 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA
 */
