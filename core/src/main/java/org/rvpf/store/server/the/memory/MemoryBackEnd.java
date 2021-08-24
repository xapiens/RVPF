/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MemoryBackEnd.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.memory;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreCursor;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.archiver.Archiver;
import org.rvpf.store.server.archiver.JiTArchiver;
import org.rvpf.store.server.the.BackEnd;
import org.rvpf.store.server.the.TheStoreServiceAppImpl;

/**
 * Memory back-end.
 */
public class MemoryBackEnd
    implements BackEnd
{
    /** {@inheritDoc}
     */
    @Override
    public void beginUpdates()
    {
        _lock.writeLock().lock();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
    {
        if (!_closed) {
            beginUpdates();
            rollback();
            _closed = true;
            endUpdates();

            _backup.stop();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void commit() {}

    /** {@inheritDoc}
     */
    @Override
    public StoreValues createResponse(
            final StoreValuesQuery query,
            final Optional<Identity> identity)
    {
        final Responder responder = new Responder(
            _archiveMap,
            _snapshotMap,
            _versionsMap,
            _theStoreAppImpl.getService().getMetadata(),
            _theStoreAppImpl.getBackEndLimit());
        final StoreCursor cursor = new StoreCursor(
            query,
            _theStoreAppImpl.getServer(),
            responder);
        final StoreValues response;

        _lock.readLock().lock();

        try {
            response = cursor.createResponse(identity);
        } finally {
            _lock.readLock().unlock();
        }

        return response;
    }

    /** {@inheritDoc}
     */
    @Override
    public int delete(final VersionedValue versionedValue)
    {
        final VersionedValue pointValue = versionedValue
            .isDeleted()? new VersionedValue(versionedValue): versionedValue;
        final VersionedValue deletedValue = versionedValue
            .isDeleted()? versionedValue: new VersionedValue.Deleted(
                versionedValue);
        final boolean purged = versionedValue instanceof VersionedValue.Purged;
        int deleted;

        deleted = _remove(pointValue);

        if (purged) {
            deleted |= _remove(deletedValue);
        }

        if (deleted > 0) {
            _LOGGER.trace(StoreMessages.ROW_DELETED, pointValue);

            if (!(purged || _theStoreAppImpl.isDropDeleted())) {
                _update(deletedValue);
            }
        }

        return deleted;
    }

    /** {@inheritDoc}
     */
    @Override
    public void endUpdates()
    {
        _lock.writeLock().unlock();
    }

    /** {@inheritDoc}
     */
    @Override
    public Archiver newArchiver()
    {
        return new JiTArchiver();
    }

    /** {@inheritDoc}
     */
    @Override
    public void open()
    {
        if (_theStoreAppImpl.isSnapshot()) {
            _snapshotMap = Optional.of(new ConcurrentHashMap<>());
        } else {
            _archiveMap = Optional.of(new ConcurrentHashMap<>());

            if (!_theStoreAppImpl.isPullDisabled()) {
                _versionsMap = Optional.of(new TreeMap<>());
            }
        }

        try {
            _backup.start();
        } catch (final ServiceNotAvailableException exception) {
            _theStoreAppImpl.fail();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void rollback() {}

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final TheStoreServiceAppImpl theStoreAppImpl)
    {
        _theStoreAppImpl = theStoreAppImpl;

        return _backup.setUp(_theStoreAppImpl);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void tearDown()
    {
        close();

        _backup.tearDown();

        if (_archiveMap.isPresent()) {
            for (final NavigableMap<DateTime, VersionedValue> pointValues:
                    _archiveMap.get().values()) {
                pointValues.clear();
            }

            _archiveMap.get().clear();
        }

        if (_snapshotMap.isPresent()) {
            _snapshotMap.get().clear();
        }

        if (_versionsMap.isPresent()) {
            _versionsMap.get().clear();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void update(final VersionedValue versionedValue)
    {
        final UUID pointUUID = _getPointUUID(versionedValue);
        final VersionedValue previousPointValue;

        if (_snapshotMap.isPresent()) {
            previousPointValue = _snapshotMap
                .get()
                .put(pointUUID, versionedValue);
        } else {
            final NavigableMap<DateTime, VersionedValue> pointValuesMap =
                _getPointValuesMap(
                    pointUUID);

            previousPointValue = pointValuesMap
                .put(versionedValue.getStamp(), versionedValue);

            if (_versionsMap.isPresent()) {
                if (previousPointValue != null) {
                    _versionsMap.get().remove(previousPointValue.getVersion());
                }

                _versionsMap
                    .get()
                    .put(versionedValue.getVersion(), versionedValue);
            }
        }

        _LOGGER
            .trace(
                ((previousPointValue == null)
                 ? StoreMessages.ROW_INSERTED: StoreMessages.ROW_REPLACED),
                versionedValue);
    }

    /**
     * Gets the archive maps.
     *
     * @return The optional archive maps.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Collection<NavigableMap<DateTime,
            VersionedValue>>> getArchiveMaps()
    {
        return _archiveMap
            .isPresent()? Optional
                .of(_archiveMap.get().values()): Optional.empty();
    }

    /**
     * Gets the snapshot values.
     *
     * @return The optional snapshot values.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Collection<VersionedValue>> getSnapshotValues()
    {
        return _snapshotMap
            .isPresent()? Optional
                .of(_snapshotMap.get().values()): Optional.empty();
    }

    @Nonnull
    @CheckReturnValue
    private static UUID _getPointUUID(
            @Nonnull final VersionedValue versionedValue)
    {
        final UUID pointUUID = versionedValue.getPointUUID();

        return versionedValue.isDeleted()? pointUUID.deleted(): pointUUID;
    }

    private NavigableMap<DateTime, VersionedValue> _getPointValuesMap(
            final UUID pointUUID)
    {
        NavigableMap<DateTime, VersionedValue> pointValuesMap = _archiveMap
            .get()
            .get(pointUUID);

        if (pointValuesMap == null) {
            pointValuesMap = new TreeMap<>();
            _archiveMap.get().put(pointUUID, pointValuesMap);
        }

        return pointValuesMap;
    }

    private int _remove(final VersionedValue versionedValue)
    {
        final UUID pointUUID = _getPointUUID(versionedValue);
        final VersionedValue previousPointValue;

        if (_snapshotMap.isPresent()) {
            previousPointValue = _snapshotMap.get().remove(pointUUID);
        } else {
            final NavigableMap<DateTime, VersionedValue> pointValuesMap =
                _getPointValuesMap(
                    pointUUID);

            previousPointValue = pointValuesMap
                .remove(versionedValue.getStamp());

            if (_versionsMap.isPresent() && (previousPointValue != null)) {
                _versionsMap.get().remove(previousPointValue.getVersion());
            }
        }

        return (previousPointValue != null)? 1: 0;
    }

    private void _update(final VersionedValue versionedValue)
    {
        final UUID pointUUID = _getPointUUID(versionedValue);
        final VersionedValue previousPointValue;

        if (_snapshotMap.isPresent()) {
            previousPointValue = _snapshotMap
                .get()
                .put(pointUUID, versionedValue);
        } else {
            final NavigableMap<DateTime, VersionedValue> pointValuesMap =
                _getPointValuesMap(
                    pointUUID);

            previousPointValue = pointValuesMap
                .put(versionedValue.getStamp(), versionedValue);

            if (_versionsMap.isPresent()) {
                if (previousPointValue != null) {
                    _versionsMap.get().remove(previousPointValue.getVersion());
                }

                _versionsMap
                    .get()
                    .put(versionedValue.getVersion(), versionedValue);
            }
        }

        _LOGGER
            .trace(
                ((previousPointValue == null)
                 ? StoreMessages.ROW_INSERTED: StoreMessages.ROW_REPLACED),
                versionedValue);
    }

    private static final Logger _LOGGER = Logger
        .getInstance(MemoryBackEnd.class);

    private Optional<Map<UUID, NavigableMap<DateTime, VersionedValue>>> _archiveMap =
        Optional
            .empty();
    private final Backup _backup = new Backup(this);
    private boolean _closed;
    private final ReadWriteLock _lock = new ReentrantReadWriteLock(true);
    private TheStoreServiceAppImpl _theStoreAppImpl;
    private Optional<Map<UUID, VersionedValue>> _snapshotMap = Optional.empty();
    private Optional<NavigableMap<DateTime, VersionedValue>> _versionsMap =
        Optional
            .empty();
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
