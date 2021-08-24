/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: FilesQueue.java 4057 2019-06-04 18:44:38Z SFB $
 */

package org.rvpf.som.queue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Message;
import org.rvpf.base.som.QueueInfo;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.xml.streamer.Streamer;
import org.rvpf.service.ServiceMessages;

/**
 * Queue implementation.
 */
@ThreadSafe
public final class FilesQueue
    extends Queue.Abstract
{
    /**
     * Constructs an instance.
     *
     * @param name The queue name.
     * @param stats The queue stats.
     * @param streamer The streamer instance.
     */
    FilesQueue(
            @Nonnull final String name,
            @Nonnull final QueueStats stats,
            @Nonnull final Streamer streamer)
    {
        super(name, stats);

        _streamer = streamer;
    }

    /**
     * Gets the 'backup' suffix.
     *
     * @return The optional 'backup' suffix.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getBackupSuffix()
    {
        return Optional.ofNullable(_backupSuffix);
    }

    /**
     * Gets the 'bad' suffix.
     *
     * @return The 'bad' suffix.
     */
    @Nonnull
    @CheckReturnValue
    public String getBadSuffix()
    {
        return _badSuffix;
    }

    /**
     * Gets the 'data' suffix.
     *
     * @return The 'data' suffix.
     */
    @Nonnull
    @CheckReturnValue
    public String getDataSuffix()
    {
        return _dataSuffix;
    }

    /**
     * Gets the queue directory.
     *
     * @return The queue directory.
     */
    @Nonnull
    @CheckReturnValue
    public File getDirectoryFile()
    {
        return _directory;
    }

    /** {@inheritDoc}
     */
    @Override
    public QueueInfo getInfo()
    {
        return _info;
    }

    /**
     * Gets the 'next' suffix.
     *
     * @return The 'next' suffix.
     */
    @Nonnull
    @CheckReturnValue
    public String getNextSuffix()
    {
        return _nextSuffix;
    }

    /**
     * Gets the 'trans' suffix.
     *
     * @return The 'trans' suffix.
     */
    @Nonnull
    @CheckReturnValue
    public String getTransSuffix()
    {
        return _transSuffix;
    }

    /**
     * Lists entry files with the specified suffix.
     *
     * @param withSuffix The suffix.
     *
     * @return A file object array.
     */
    public File[] listEntryFiles(@Nonnull final String withSuffix)
    {
        class QueueFileFilter
            implements FileFilter
        {
            /**
             * Constructs a queue file filter.
             *
             * @param suffix The file name suffix.
             */
            QueueFileFilter(@Nonnull final String suffix)
            {
                _suffix = suffix;
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean accept(final File file)
            {
                if (!file.isFile()) {
                    return false;
                }

                String name = file.getName();

                if (!name.startsWith(getEntryPrefix())
                        || !name.endsWith(_suffix)) {
                    return false;
                }

                name = _entryName(name, _suffix);

                if (name.length() != DateTime.FILE_NAME_LENGTH) {
                    return false;
                }

                try {
                    DateTime.fromString(name);
                } catch (final IllegalArgumentException exception) {
                    return false;
                }

                return true;
            }

            private final String _suffix;
        }

        final File[] entryFiles = _directory
            .listFiles(new QueueFileFilter(withSuffix));

        return Require.notNull(entryFiles);
    }

    /** {@inheritDoc}
     */
    @Override
    public Receiver newReceiver()
    {
        final Receiver receiver = new FilesReceiver(this);

        onNewReceiver(receiver);

        return receiver;
    }

    /** {@inheritDoc}
     */
    @Override
    public Sender newSender()
    {
        final Sender sender = new FilesSender(this);

        onNewSender(sender);

        return sender;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final KeyedValues somProperties)
    {
        if (!super.setUp(somProperties)) {
            return false;
        }

        _compressed = somProperties.getBoolean(COMPRESSED_PROPERTY);

        if (_compressed) {
            _compressedSuffix = somProperties
                .getString(
                    COMPRESSED_SUFFIX_PROPERTY,
                    Optional.of(DEFAULT_COMPRESSED_SUFFIX))
                .get();
            getThisLogger()
                .debug(ServiceMessages.QUEUE_FILES_COMPRESSED, getName());
        }

        _entryPrefix = somProperties
            .getString(ENTRY_PREFIX_PROPERTY, Optional.of(""))
            .get();
        _badSuffix = _suffix(
            somProperties,
            BAD_SUFFIX_PROPERTY,
            DEFAULT_BAD_SUFFIX);
        _dataSuffix = _suffix(
            somProperties,
            DATA_SUFFIX_PROPERTY,
            DEFAULT_DATA_SUFFIX);
        _transSuffix = _suffix(
            somProperties,
            TRANS_SUFFIX_PROPERTY,
            DEFAULT_TRANS_SUFFIX);

        _nextSuffix = somProperties
            .getString(NEXT_SUFFIX_PROPERTY, Optional.of(DEFAULT_NEXT_SUFFIX))
            .get();

        _backup = somProperties.getBoolean(BACKUP_PROPERTY);

        if (_backup) {
            _backupSuffix = _suffix(
                somProperties,
                BACKUP_SUFFIX_PROPERTY,
                DEFAULT_BACKUP_SUFFIX);
            getThisLogger()
                .debug(ServiceMessages.QUEUE_BACKUP, getName(), _backupSuffix);
        }

        _mergeLimit = somProperties.getInt(MERGE_LIMIT_PROPERTY, 0);

        if (_mergeLimit > 0) {
            getThisLogger()
                .debug(
                    ServiceMessages.QUEUE_MERGE_LIMIT,
                    getName(),
                    String.valueOf(_mergeLimit));
            _mergeSplit = somProperties
                .getInt(MERGE_SPLIT_PROPERTY, Integer.MAX_VALUE);

            if (_mergeSplit < Integer.MAX_VALUE) {
                getThisLogger()
                    .debug(
                        ServiceMessages.QUEUE_MERGE_SPLIT,
                        getName(),
                        String.valueOf(_mergeSplit));
            }
        } else {
            _mergeSplit = 0;
        }

        final Path rootPath = Paths
            .get(somProperties.getString(ROOT_PROPERTY).get());
        final Path directoryPath = rootPath
            .resolve(
                somProperties
                    .getString(DIRECTORY_PROPERTY, Optional.of(getName()))
                    .get());

        _directory = directoryPath.toFile();
        getThisLogger()
            .debug(
                ServiceMessages.QUEUE_DIRECTORY,
                _directory.getAbsolutePath());

        if (_directory.mkdirs()) {
            getThisLogger()
                .info(
                    ServiceMessages.QUEUE_DIRECTORY_CREATED,
                    _directory.getAbsolutePath());
        }

        if (!_directory.isDirectory()) {
            getThisLogger()
                .error(
                    ServiceMessages.QUEUE_DIRECTORY_FAILED,
                    _directory.getAbsolutePath());

            return false;
        }

        if (somProperties.getBoolean(LOCK_DISABLED_PROPERTY)) {
            _lockFile = null;
            getThisLogger().info(ServiceMessages.LOCK_FILE_DISABLED);
        } else {
            final String lockPrefix = somProperties
                .getString(LOCK_PREFIX_PROPERTY, Optional.of(_entryPrefix))
                .get();
            final String lockSuffix = somProperties
                .getString(
                    LOCK_SUFFIX_PROPERTY,
                    Optional.of(DEFAULT_LOCK_SUFFIX))
                .get();
            final File lockFile = new File(
                _directory,
                lockPrefix + getName() + lockSuffix);

            try {
                _lockFile = new RandomAccessFile(lockFile, "rw");
            } catch (final FileNotFoundException exception) {
                getThisLogger()
                    .error(ServiceMessages.LOCK_FILE_NOT_FOUND, lockFile);

                return false;
            }

            try {
                if (_lockFile.getChannel().tryLock() == null) {
                    getThisLogger()
                        .error(ServiceMessages.LOCK_FILE_LOCK_FAILED, lockFile);

                    return false;
                }
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        if (!_recover()) {
            return false;
        }

        getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        super.tearDown();

        final long messageCount = _info.getMessageCount();

        if (getThisLogger().isDebugEnabled()) {
            if (messageCount > 0) {
                getThisLogger()
                    .debug(
                        ServiceMessages.QUEUE_MESSAGES_LEFT,
                        String.valueOf(messageCount),
                        getName());
            }

            final long filesSize = _info.getFilesSize();

            if (filesSize > 0) {
                getThisLogger()
                    .debug(
                        ServiceMessages.QUEUE_MESSAGE_FILES_SIZE,
                        getName(),
                        String.valueOf(filesSize));
            }
        }

        final int entriesSize;
        final RandomAccessFile lockFile;

        synchronized (_queueMutex) {
            entriesSize = _entries.size();
            _entries.clear();
            lockFile = _lockFile;
            _lockFile = null;
        }

        if (entriesSize > 0) {
            getThisLogger()
                .debug(
                    ServiceMessages.QUEUE_MESSAGE_FILE_COUNT,
                    getName(),
                    String.valueOf(entriesSize));
        }

        if (lockFile != null) {
            try {
                lockFile.seek(0L);
                lockFile.setLength(0L);
                lockFile.writeBytes(String.valueOf(messageCount));
                lockFile.close();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        getThisLogger().debug(ServiceMessages.TEAR_DOWN_COMPLETED);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void onReceiverClosed(final Queue.Receiver receiver)
    {
        synchronized (_queueMutex) {
            _queueMutex.notifyAll();
        }

        super.onReceiverClosed(receiver);
    }

    /**
     * Removes the prefix and suffix from a file name.
     *
     * @param fileName The file name.The file name suffix.
     * @param suffix The file name suffix.
     *
     * @return The file name stripped of its prefix and suffix.
     */
    @Nonnull
    @CheckReturnValue
    String _entryName(
            @Nonnull final String fileName,
            @Nonnull final String suffix)
    {
        return fileName
            .substring(
                getEntryPrefix().length(),
                fileName.length() - suffix.length());
    }

    /**
     * Drops entries.
     *
     * @param entries The entries to drop.
     * @param nextEntry The optional next entry (partial).
     * @param messageCount The number of messages dropped.
     */
    void dropEntries(
            @Nonnull final LinkedList<QueueEntry> entries,
            @Nonnull final Optional<QueueEntry> nextEntry,
            final int messageCount)
    {
        int dropped = 0;

        synchronized (_queueMutex) {
            for (final QueueEntry entry: entries) {
                final Optional<File> backupFile = entry.getBackupFile();

                if (backupFile.isPresent()) {
                    _deleteFile(backupFile.get());

                    if (!_rename(entry.getDataFile(), backupFile.get())) {
                        throw new RuntimeException(
                            Message.format(
                                BaseMessages.FILE_RENAME_FAILED,
                                entry.getDataFile(),
                                backupFile));
                    }
                } else if (!_deleteFile(entry.getDataFile())) {
                    throw new RuntimeException(
                        Message.format(
                            BaseMessages.FILE_DELETE_FAILED,
                            entry.getDataFile()));
                }

                if ((entry.getNextPosition() > 0)
                        && !_deleteFile(entry.getNextFile())) {
                    throw new RuntimeException(
                        Message.format(
                            BaseMessages.FILE_DELETE_FAILED,
                            entry.getNextFile()));
                }

                _entries.remove(entry.getName());
                _info.updateFileCount(-1);
                _info.updateFilesSize(-entry.getFileSize());
                ++dropped;
            }

            if (nextEntry.isPresent()) {
                try {
                    final Writer nextWriter = new OutputStreamWriter(
                        new FileOutputStream(nextEntry.get().getNextFile()),
                        StandardCharsets.UTF_8);

                    nextWriter
                        .write(
                            Long.toString(nextEntry.get().getNextPosition()));
                    nextWriter.close();
                } catch (final IOException exception) {
                    throw new RuntimeException(
                        Message.format(
                            ServiceMessages.WRITE_FAILED,
                            nextEntry.get().getNextFile(),
                            exception.getMessage()));
                }
            }

            _info.updateMessageCount(-messageCount);
            _info.setLastReceiverCommit(DateTime.now());
        }

        getStats().transactionsSent(dropped, messageCount);
    }

    /**
     * Gets the entry prefix.
     *
     * @return The entry prefix.
     */
    @Nonnull
    @CheckReturnValue
    String getEntryPrefix()
    {
        return _entryPrefix;
    }

    /**
     * Gets the streamer instance.
     *
     * @return The streamer instance.
     */
    @Nonnull
    @CheckReturnValue
    Streamer getStreamer()
    {
        return _streamer;
    }

    /**
     * Asks if the message files of this queue are compressed.
     *
     * @return True if the message files of this queue are compressed.
     */
    @CheckReturnValue
    boolean isCompressed()
    {
        return _compressed;
    }

    /**
     * Returns a new queue entry.
     *
     * @return The new queue entry.
     */
    @Nonnull
    @CheckReturnValue
    QueueEntry newEntry()
    {
        return new QueueEntry(this, _newEntryName());
    }

    /**
     * Returns the next queue entry.
     *
     * @param previousEntry The previous queue entry or empty.
     *
     * @return The next queue entry or empty.
     */
    @Nonnull
    @CheckReturnValue
    Optional<QueueEntry> nextEntry(
            @Nonnull final Optional<QueueEntry> previousEntry)
    {
        QueueEntry nextEntry = null;

        synchronized (_queueMutex) {
            if (!_entries.isEmpty()) {
                if (previousEntry.isPresent()) {
                    final Map.Entry<String, QueueEntry> entry = _entries
                        .higherEntry(previousEntry.get().getName());

                    nextEntry = (entry != null)? entry.getValue(): null;
                } else {
                    nextEntry = _entries.get(_entries.firstKey());
                }
            }
        }

        if (nextEntry != null) {
            if (!nextEntry.isReady()) {
                nextEntry = null;
            }
        }

        return Optional.ofNullable(nextEntry);
    }

    /**
     * Purges the queue.
     *
     * @return The number of messages purged.
     */
    @CheckReturnValue
    long purge()
    {
        final long purged;

        getThisLogger().trace(ServiceMessages.QUEUE_PURGE_STARTED, getName());

        synchronized (_queueMutex) {
            purged = _info.getMessageCount();

            for (final Iterator<QueueEntry> i = _entries.values().iterator();
                    i.hasNext(); ) {
                final QueueEntry entry = i.next();

                if (entry.isReady()) {
                    if (!_deleteFile(entry.getDataFile())) {
                        throw new RuntimeException(
                            Message.format(
                                BaseMessages.FILE_DELETE_FAILED,
                                entry.getDataFile()));
                    }

                    if ((entry.getNextPosition() > 0)
                            && !_deleteFile(entry.getNextFile())) {
                        throw new RuntimeException(
                            Message.format(
                                BaseMessages.FILE_DELETE_FAILED,
                                entry.getNextFile()));
                    }

                    i.remove();
                }
            }

            _info.clearMessageCount();
            _info.clearFilesSize();
        }

        if (purged > 0) {
            getThisLogger()
                .debug(
                    ServiceMessages.PURGED_MESSAGES,
                    String.valueOf(purged),
                    getName());
        }

        getThisLogger().trace(ServiceMessages.QUEUE_PURGE_COMPLETED, getName());

        return purged;
    }

    /**
     * Releases a queue entry.
     *
     * @param entry The queue entry.
     * @param messageCount The number of messages present in the entry.
     */
    void releaseEntry(@Nonnull final QueueEntry entry, final int messageCount)
    {
        synchronized (_queueMutex) {
            final File transFile = entry.getTransFile();
            final QueueEntry lastEntry;
            boolean merge = (_mergeLimit >= messageCount)
                    && !_entries.isEmpty();

            if (merge) {
                lastEntry = _entries.get(_entries.lastKey());
                merge = !lastEntry.isBusy()
                        && (lastEntry.getMessageCount() <= _mergeSplit);
            } else {
                lastEntry = null;
            }

            if (merge) {
                final File lastFile = lastEntry.getDataFile();

                try {
                    final OutputStream outputStream = new FileOutputStream(
                        lastFile,
                        true);

                    Files.copy(transFile.toPath(), outputStream);
                    outputStream.close();
                    _deleteFile(transFile);
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }

                lastEntry
                    .setMesssageCount(
                        lastEntry.getMessageCount() + messageCount);

                final long oldFileSize = lastEntry.getFileSize();
                final long newFileSize = _getFileSize(lastFile);

                lastEntry.setFileSize(newFileSize);
                _info.updateFilesSize(newFileSize - oldFileSize);
            } else {
                entry.setName(_newEntryName());
                entry.setFileSize(_getFileSize(transFile));

                if (!_rename(transFile, entry.getDataFile())) {
                    throw new RuntimeException(
                        Message.format(
                            BaseMessages.FILE_RENAME_FAILED,
                            entry.getTransFile(),
                            entry.getDataFile()));
                }

                entry.setMesssageCount(messageCount);
                entry.setReady(true);
                _entries.put(entry.getName(), entry);
                _info.updateFileCount(+1);
                _info.updateFilesSize(+entry.getFileSize());
            }

            _info.updateMessageCount(+messageCount);
            _info.setLastSenderCommit(DateTime.now());
            getStats().transactionReceived(messageCount);

            _queueMutex.notifyAll();
        }
    }

    /**
     * Waits for messages.
     *
     * @param waitMillis The maximum wait time in milliseconds.
     *
     * @throws InterruptedException When interrupted.
     */
    void waitForMessages(final long waitMillis)
        throws InterruptedException
    {
        synchronized (_queueMutex) {
            _queueMutex.wait(waitMillis);
        }
    }

    private static int _getQueueFileRetries()
    {
        Integer queueFileRetries = _queueFileRetries;

        if (queueFileRetries == null) {
            final String queueFileRetriesString = System
                .getProperty(QUEUE_FILE_RETRIES, DEFAULT_QUEUE_FILE_RETRIES);

            queueFileRetries = Integer.valueOf(queueFileRetriesString);
            _queueFileRetries = queueFileRetries;
        }

        return queueFileRetries.intValue();
    }

    @CheckReturnValue
    private static long _getQueueFileRetryDelay()
    {
        ElapsedTime queueFileRetryDelay = _queueFileRetryDelay;

        if (queueFileRetryDelay == null) {
            final String queueFileRetryDelayString = System
                .getProperty(
                    QUEUE_FILE_RETRY_DELAY,
                    DEFAULT_QUEUE_FILE_RETRY_DELAY);

            queueFileRetryDelay = ElapsedTime
                .fromString(queueFileRetryDelayString);
            _queueFileRetryDelay = queueFileRetryDelay;
        }

        return queueFileRetryDelay.toMillis();
    }

    private boolean _countMessages()
    {
        _info.clearMessageCount();
        _info.clearFilesSize();

        for (final QueueEntry entry:
                new ArrayList<QueueEntry>(_entries.values())) {
            final File file = entry.getDataFile();
            final Streamer.Input input;
            long messageCount = 0;

            try {
                input = _streamer
                    .newInput(
                        new QueueReader(
                            file,
                            entry.getNextPosition(),
                            _compressed));
            } catch (final IOException exception) {
                getThisLogger()
                    .error(
                        ServiceMessages.FILE_ACCESS_FAILED,
                        file,
                        exception.getMessage());

                return false;
            }

            try {
                while (input.skip()) {
                    ++messageCount;
                }
            } catch (final RuntimeException exception) {
                getThisLogger()
                    .warn(
                        ServiceMessages.MESSAGES_COUNT_FAILED,
                        file,
                        exception.getMessage());
                input.close();
                _deleteFile(entry.getBadFile());

                if (!_rename(file, entry.getBadFile())) {
                    getThisLogger()
                        .error(
                            BaseMessages.FILE_RENAME_FAILED,
                            file,
                            entry.getBadFile());

                    return false;
                }

                getThisLogger()
                    .warn(
                        ServiceMessages.FILE_RENAMED,
                        file,
                        entry.getBadFile());
                _entries.remove(entry.getName());
                _info.updateFileCount(-1);

                continue;
            }

            input.close();

            entry.setMesssageCount(messageCount);
            _info.updateMessageCount(+messageCount);
            entry.setFileSize(_getFileSize(file));
            _info.updateFilesSize(+entry.getFileSize());
        }

        return true;
    }

    private boolean _deleteFile(@Nonnull final File file)
    {
        int retries = 0;

        for (;;) {
            try {
                if (!Files.deleteIfExists(file.toPath())) {
                    return false;
                }
            } catch (final FileSystemException exception) {
                getThisLogger()
                    .warn(
                        ServiceMessages.QUEUE_FILE_DELETE_FAILED,
                        getDirectoryFile(),
                        file.getName(),
                        exception.getReason());

                if (++retries > _getQueueFileRetries()) {
                    return false;
                }

                getThisLogger()
                    .info(
                        ServiceMessages.QUEUE_FILE_WILL_RETRY,
                        String.valueOf(_getQueueFileRetryDelay()));

                try {
                    Thread.sleep(_getQueueFileRetryDelay());
                } catch (final InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();

                    return false;
                }

                continue;
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            getThisLogger()
                .trace(
                    ServiceMessages.QUEUE_FILE_DELETED,
                    getDirectoryFile(),
                    file.getName());

            break;
        }

        return true;
    }

    private long _getFileSize(@Nonnull final File file)
    {
        final Long fileSize;

        try {
            fileSize = (Long) Files.getAttribute(file.toPath(), "size");
        } catch (final IOException exception) {
            getThisLogger()
                .warn(ServiceMessages.GET_FILE_SIZE_FAILED, file, exception);

            return 0;
        }

        return fileSize.longValue();
    }

    private String _newEntryName()
    {
        DateTime entryStamp = DateTime.now();

        synchronized (_queueMutex) {
            if (entryStamp.isNotAfter(_entryStamp)) {
                entryStamp = _entryStamp.after();
            }

            _entryStamp = entryStamp;
        }

        return entryStamp.toFileName();
    }

    private boolean _recover()
    {
        getThisLogger()
            .trace(ServiceMessages.QUEUE_RECOVERY_STARTED, getName());

        int kept = 0;
        int recovered = 0;
        int dropped = 0;
        File[] files;

        // Recovers entries ready for reception.

        files = listEntryFiles(_dataSuffix);

        for (final File file: files) {
            final String name = _entryName(file.getName(), _dataSuffix);
            final QueueEntry entry = new QueueEntry(this, name);

            entry.setReady(true);
            _entries.put(name, entry);
            _info.updateFileCount(+1);
            ++kept;
        }

        // Gets next positions for receive.

        files = listEntryFiles(_nextSuffix);

        for (final File file: files) {
            final String name = _entryName(file.getName(), _nextSuffix);
            final QueueEntry entry = _entries.get(name);

            if (entry != null) {
                try {
                    final BufferedReader nextReader = new BufferedReader(
                        new InputStreamReader(
                            new FileInputStream(file),
                            StandardCharsets.UTF_8));

                    entry
                        .setNextPosition(Long.parseLong(nextReader.readLine()));
                    nextReader.close();
                } catch (final Exception exception) {
                    getThisLogger()
                        .error(
                            ServiceMessages.READ_FAILED,
                            file,
                            exception.getMessage());

                    return false;
                }
            } else {
                getThisLogger().warn(ServiceMessages.STALE_FILE, file);

                if (!_deleteFile(file)) {
                    getThisLogger()
                        .error(BaseMessages.FILE_DELETE_FAILED, file);

                    return false;
                }
            }
        }

        // Commits (autocommit) or rollbacks partial sends.

        files = listEntryFiles(_transSuffix);

        for (final File file: files) {
            final String name = _entryName(file.getName(), _transSuffix);

            if (isAutocommit()) {
                final QueueEntry entry = new QueueEntry(this, name);

                if (!_rename(file, entry.getDataFile())) {
                    getThisLogger()
                        .error(
                            BaseMessages.FILE_RENAME_FAILED,
                            file,
                            entry.getDataFile());

                    return false;
                }

                entry.setReady(true);
                _entries.put(name, entry);
                _info.updateFileCount(+1);
                ++recovered;
            } else {
                if (!_deleteFile(file)) {
                    getThisLogger()
                        .error(BaseMessages.FILE_DELETE_FAILED, file);

                    return false;
                }

                ++dropped;
            }
        }

        // Saves ident of last entry.

        if (!_entries.isEmpty()) {
            _entryStamp = DateTime.fromString(_entries.lastKey());
        }

        // Gets the actual length.

        if (!_countMessages()) {
            return false;
        }

        final long messageCount = _info.getMessageCount();

        if (_lockFile != null) {
            final String lockLine;

            try {
                _lockFile.seek(0);
                lockLine = _lockFile.readLine();
            } catch (final IOException exception1) {
                throw new RuntimeException(exception1);
            }

            long savedLength = 0;

            if (lockLine != null) {
                try {
                    savedLength = Long.parseLong(lockLine.trim());
                } catch (final NumberFormatException exception) {
                    // Ignores bad values (keeps 0).
                }
            }

            if (messageCount != savedLength) {
                getThisLogger()
                    .warn(
                        ServiceMessages.QUEUE_LENGTH_CHANGED,
                        getName(),
                        String.valueOf(savedLength),
                        String.valueOf(messageCount));
            }
        }

        // Summarizes.

        if (((kept + recovered + dropped) > 0)
                && getThisLogger().isDebugEnabled()) {
            getThisLogger()
                .debug(
                    ServiceMessages.QUEUE_ENTRIES,
                    getName(),
                    String.valueOf(kept),
                    String.valueOf(recovered),
                    String.valueOf(dropped));

            if (messageCount > 0) {
                getThisLogger()
                    .debug(
                        ServiceMessages.QUEUE_MESSAGES,
                        getName(),
                        String.valueOf(messageCount));
            }

            final long filesSize = _info.getFilesSize();

            if (filesSize > 0) {
                getThisLogger()
                    .debug(
                        ServiceMessages.QUEUE_MESSAGE_FILES_SIZE,
                        getName(),
                        String.valueOf(filesSize));
            }

            if (_entries.size() > 0) {
                getThisLogger()
                    .debug(
                        ServiceMessages.QUEUE_MESSAGE_FILE_COUNT,
                        getName(),
                        String.valueOf(_entries.size()));
            }
        }

        getStats().recovered(kept, recovered, dropped, recovered);

        getThisLogger()
            .trace(ServiceMessages.QUEUE_RECOVERY_COMPLETED, getName());

        return true;
    }

    private boolean _rename(final File fromFile, final File toFile)
    {
        int retries = 0;

        for (;;) {
            try {
                Files.move(fromFile.toPath(), toFile.toPath());
            } catch (final FileSystemException exception) {
                getThisLogger()
                    .warn(
                        ServiceMessages.QUEUE_FILE_RENAME_FAILED,
                        getDirectoryFile(),
                        fromFile.getName(),
                        toFile.getName(),
                        exception.getReason());

                if (++retries > _getQueueFileRetries()) {
                    return false;
                }

                getThisLogger()
                    .info(
                        ServiceMessages.QUEUE_FILE_WILL_RETRY,
                        String.valueOf(_getQueueFileRetryDelay()));

                try {
                    Thread.sleep(_getQueueFileRetryDelay());
                } catch (final InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();

                    return false;
                }

                continue;
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            getThisLogger()
                .trace(
                    ServiceMessages.QUEUE_FILE_RENAMED,
                    getDirectoryFile(),
                    fromFile.getName(),
                    toFile.getName());

            break;
        }

        return true;
    }

    private String _suffix(
            final KeyedValues somProperties,
            final String suffixProperty,
            final String defaultSuffix)
    {
        final String suffix = somProperties
            .getString(suffixProperty, Optional.of(defaultSuffix))
            .get();

        return (_compressedSuffix != null)
               ? (suffix + _compressedSuffix): suffix;
    }

    /** True will backup messages. */
    public static final String BACKUP_PROPERTY = "backup";

    /** Property for file suffix of backup entries. */
    public static final String BACKUP_SUFFIX_PROPERTY = "suffix.backup";

    /** Property for file suffix of bad queue entries. */
    public static final String BAD_SUFFIX_PROPERTY = "suffix.bad";

    /** True will compress message files. */
    public static final String COMPRESSED_PROPERTY = "compressed";

    /** Property for file suffix of compressed files. */
    public static final String COMPRESSED_SUFFIX_PROPERTY = "compressed.suffix";

    /** Property for file suffix of queue data entries. */
    public static final String DATA_SUFFIX_PROPERTY = "suffix.data";

    /** Default file suffix of backup entries. */
    public static final String DEFAULT_BACKUP_SUFFIX = ".backup";

    /** Default file suffix of bad entries. */
    public static final String DEFAULT_BAD_SUFFIX = ".bad";

    /** Default file suffix of compressed files. */
    public static final String DEFAULT_COMPRESSED_SUFFIX = ".gz";

    /** Default file suffix of data entries. */
    public static final String DEFAULT_DATA_SUFFIX = ".data";

    /** Default file suffix of the lock file. */
    public static final String DEFAULT_LOCK_SUFFIX = ".lock";

    /** Default file suffix of next data entries. */
    public static final String DEFAULT_NEXT_SUFFIX = ".next";
    public static final String DEFAULT_QUEUE_FILE_RETRIES = "1";
    public static final String DEFAULT_QUEUE_FILE_RETRY_DELAY = "60.0";

    /** Default file suffix of transaction data entries. */
    public static final String DEFAULT_TRANS_SUFFIX = ".trans";

    /** The directory property. */
    public static final String DIRECTORY_PROPERTY = "directory";

    /** Property for the prefix of queue entries. */
    public static final String ENTRY_PREFIX_PROPERTY = "prefix.entry";

    /** The lock disabled property. */
    public static final String LOCK_DISABLED_PROPERTY = "lock.disabled";

    /** Property for suffix of the lock file. */
    public static final String LOCK_SUFFIX_PROPERTY = "suffix.lock";

    /** Merge limit property. */
    public static final String MERGE_LIMIT_PROPERTY = "merge.limit";

    /** Merge split property. */
    public static final String MERGE_SPLIT_PROPERTY = "merge.split";

    /** Property for file suffix of the next queue entry. */
    public static final String NEXT_SUFFIX_PROPERTY = "suffix.next";

    /** Queue file access retries system property. */
    public static final String QUEUE_FILE_RETRIES = "rvpf.queue.file.retries";

    /** Queue file access retry delay system property. */
    public static final String QUEUE_FILE_RETRY_DELAY =
        "rvpf.queue.file.retry.delay";

    /**
     * Property for the root directory of the SOM queues. It can be specified
     * as a relative or absolute path.
     */
    public static final String ROOT_PROPERTY = "root";

    /** Property for file suffix of queue transaction entries. */
    public static final String TRANS_SUFFIX_PROPERTY = "suffix.trans";

    /** Property for the prefix of the lock file. */
    static final String LOCK_PREFIX_PROPERTY = "prefix.lock";

    /**  */

    private static volatile Integer _queueFileRetries;
    private static volatile ElapsedTime _queueFileRetryDelay;

    private boolean _backup;
    private String _backupSuffix;
    private String _badSuffix;
    private boolean _compressed;
    private String _compressedSuffix;
    private String _dataSuffix;
    private File _directory;
    private final NavigableMap<String, QueueEntry> _entries = new TreeMap<>();
    private String _entryPrefix;
    private DateTime _entryStamp = DateTime.fromRaw(0);
    private final QueueInfo _info = new QueueInfo();
    private RandomAccessFile _lockFile;
    private int _mergeLimit;
    private int _mergeSplit;
    private String _nextSuffix;
    private final Object _queueMutex = new Object();
    private final Streamer _streamer;
    private String _transSuffix;
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
