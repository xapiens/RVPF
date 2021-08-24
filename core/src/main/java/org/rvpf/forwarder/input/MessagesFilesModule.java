/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MessagesFilesModule.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.forwarder.input;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.streamer.StreamedMessagesClient;
import org.rvpf.base.xml.streamer.Streamer;
import org.rvpf.forwarder.BatchControl;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Messages files module.
 */
public final class MessagesFilesModule
    extends InputModule
{
    /** {@inheritDoc}
     */
    @Override
    public synchronized void start()
    {
        super.start();

        _filesWatcher.start();
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        _filesWatcher.stop();

        super.stop();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _inputFiles.clear();

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        final Optional<String> inputDir = moduleProperties
            .getString(INPUT_DIR_PROPERTY);

        if (!inputDir.isPresent()) {
            getThisLogger()
                .error(BaseMessages.MISSING_PROPERTY, INPUT_DIR_PROPERTY);

            return false;
        }

        _inputDirectory = new File(inputDir.get());

        if (!_inputDirectory.isDirectory()) {
            if (_inputDirectory.mkdirs()) {
                getThisLogger()
                    .info(
                        ForwarderMessages.INPUT_DIRECTORY_CREATED,
                        _inputDirectory);
            } else {
                getThisLogger()
                    .error(
                        ForwarderMessages.INPUT_DIRECTORY_CREATE_FAILED,
                        _inputDirectory.getAbsolutePath());

                return false;
            }
        } else {
            getThisLogger()
                .info(
                    ForwarderMessages.INPUT_DIRECTORY,
                    _inputDirectory.getAbsolutePath());
        }

        _dataPrefix = moduleProperties
            .getString(DATA_PREFIX_PROPERTY, Optional.of(""))
            .get()
            .trim()
            .toLowerCase(Locale.ROOT);

        if (_dataPrefix.length() > 0) {
            getThisLogger()
                .info(ForwarderMessages.INPUT_FILE_PREFIX, _dataPrefix);
        }

        _scanInterval = moduleProperties
            .getElapsed(
                SCAN_INTERVAL_PROPERTY,
                Optional.of(DEFAULT_SCAN_INTERVAL),
                Optional.of(DEFAULT_SCAN_INTERVAL))
            .orElse(null);
        getThisLogger().info(ForwarderMessages.SCAN_INTERVAL, _scanInterval);

        _dataSuffix = moduleProperties
            .getString(DATA_SUFFIX_PROPERTY, Optional.of(DEFAULT_DATA_SUFFIX))
            .get()
            .toLowerCase(Locale.ROOT);

        if (!_dataSuffix.equalsIgnoreCase(DEFAULT_DATA_SUFFIX)) {
            getThisLogger()
                .info(ForwarderMessages.INPUT_FILE_SUFFIX, _dataSuffix);
        }

        _donePrefix = moduleProperties
            .getString(DONE_PREFIX_PROPERTY, Optional.of(_dataPrefix))
            .get()
            .trim()
            .toLowerCase(Locale.ROOT);

        if (_donePrefix.length() > 0) {
            getThisLogger()
                .info(ForwarderMessages.DONE_FILE_PREFIX, _donePrefix);
        }

        _doneSuffix = moduleProperties
            .getString(DONE_SUFFIX_PROPERTY, Optional.of(DEFAULT_DONE_SUFFIX))
            .get();

        if (!_doneSuffix.equals(DEFAULT_DONE_SUFFIX)) {
            getThisLogger()
                .info(ForwarderMessages.DONE_FILE_SUFFIX, _doneSuffix);
        }

        _compressedSuffix = moduleProperties
            .getString(
                COMPRESSED_SUFFIX_PROPERTY,
                Optional.of(DEFAULT_COMPRESSED_SUFFIX))
            .get()
            .toLowerCase(Locale.ROOT);

        if (!_compressedSuffix.equalsIgnoreCase(DEFAULT_COMPRESSED_SUFFIX)) {
            getThisLogger()
                .info(ForwarderMessages.COMPRESSED_SUFFIX, _compressedSuffix);
        }

        _semSuffix = moduleProperties
            .getString(SEM_SUFFIX_PROPERTY)
            .orElse(null);

        if (_semSuffix != null) {
            getThisLogger().info(ForwarderMessages.SEM_FILE_SUFFIX, _semSuffix);

            _semDirectory = new File(
                moduleProperties.getString(SEM_DIR_PROPERTY, inputDir).get());

            if (!_semDirectory.isDirectory()) {
                getThisLogger()
                    .error(
                        ForwarderMessages.SEM_DIR_MISSING,
                        _semDirectory.getAbsolutePath());

                return false;
            }

            if (!_semDirectory.equals(_inputDirectory)) {
                getThisLogger()
                    .info(
                        ForwarderMessages.SEM_DIR,
                        _semDirectory.getAbsolutePath());
            }

            _semPrefix = moduleProperties
                .getString(SEM_PREFIX_PROPERTY, Optional.of(_dataPrefix))
                .get()
                .trim()
                .toLowerCase(Locale.ROOT);

            if (_semPrefix.length() > 0) {
                getThisLogger()
                    .info(ForwarderMessages.SEM_FILE_PREFIX, _semPrefix);
            }

            _semMatchEnabled = moduleProperties
                .getBoolean(SEM_MATCH_ENABLED_PROPERTY);

            if (_semMatchEnabled) {
                getThisLogger().info(ForwarderMessages.SEM_MATCH_ENABLED);
            }

            _semPurgeEnabled = moduleProperties
                .getBoolean(SEM_PURGE_ENABLED_PROPERTY);

            if (_semPurgeEnabled) {
                getThisLogger().info(ForwarderMessages.SEM_PURGE_ENABLED);
            }
        }

        _filesWatcher = new _FilesWatcher();
        setInput(new _FilesReader());

        return super.setUp(moduleProperties);
    }

    /**
     * Adds an input file.
     *
     * @param inputFile The input file.
     */
    void _addInputFile(@Nonnull final File inputFile)
    {
        _inputFiles.add(inputFile);
        getThisLogger().trace(ForwarderMessages.INPUT_FILE_ADDED, inputFile);
    }

    /**
     * Clears the input files.
     */
    void _clearInputFiles()
    {
        _inputFiles.clear();
    }

    /**
     * Gets the compressed file suffix.
     *
     * @return The compressed file suffix.
     */
    @Nonnull
    @CheckReturnValue
    String _getCompressedSuffix()
    {
        return _compressedSuffix;
    }

    /**
     * Gets the data prefix.
     *
     * @return The data prefix.
     */
    @Nonnull
    @CheckReturnValue
    String _getDataPrefix()
    {
        return _dataPrefix;
    }

    /**
     * Gets the data suffix.
     *
     * @return The data suffix.
     */
    @Nonnull
    @CheckReturnValue
    String _getDataSuffix()
    {
        return _dataSuffix;
    }

    /**
     * Gets the done prefix.
     *
     * @return The done prefix.
     */
    @Nonnull
    @CheckReturnValue
    String _getDonePrefix()
    {
        return _donePrefix;
    }

    /**
     * Gets the done suffix.
     *
     * @return The done suffix.
     */
    @Nonnull
    @CheckReturnValue
    String _getDoneSuffix()
    {
        return _doneSuffix;
    }

    /**
     * Gets the input directory.
     *
     * @return The input directory.
     */
    @Nonnull
    @CheckReturnValue
    File _getInputDirectory()
    {
        return _inputDirectory;
    }

    /**
     * Gets the scan interval.
     *
     * @return The scan interval.
     */
    @Nonnull
    @CheckReturnValue
    ElapsedTime _getScanInterval()
    {
        return _scanInterval;
    }

    /**
     * Gets the semaphore directory.
     *
     * @return The semaphore directory.
     */
    @Nonnull
    @CheckReturnValue
    File _getSemDirectory()
    {
        return Require.notNull(_semDirectory);
    }

    /**
     * Gets the semaphore prefix.
     *
     * @return The semaphore prefix.
     */
    @Nonnull
    @CheckReturnValue
    String _getSemPrefix()
    {
        return Require.notNull(_semPrefix);
    }

    /**
     * Gets the semaphore suffix.
     *
     * @return The semaphore suffix.
     */
    @Nonnull
    @CheckReturnValue
    String _getSemSuffix()
    {
        return Require.notNull(_semSuffix);
    }

    /**
     * Asks if semaphore files are enabled.
     *
     * @return True if semaphore files are enabled.
     */
    @CheckReturnValue
    boolean _isSemEnabled()
    {
        return _semSuffix != null;
    }

    /**
     * Asks if the semaphore files should be matched.
     *
     * @return True when they should be matched.
     */
    @CheckReturnValue
    boolean _isSemMatchEnabled()
    {
        return _semMatchEnabled;
    }

    /**
     * Asks if the semaphore files should be purged.
     *
     * @return True when they should be purged.
     */
    @CheckReturnValue
    boolean _isSemPurgeEnabled()
    {
        return _semPurgeEnabled;
    }

    /**
     * Takes an input file.
     *
     * @return The input file.
     *
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    File _takeInputFile()
        throws InterruptedException
    {
        return _inputFiles.take();
    }

    /** The file name suffix for compressed input files. */
    public static final String COMPRESSED_SUFFIX_PROPERTY =
        "input.compressed.suffix";

    /** The file prefix for data entries. */
    public static final String DATA_PREFIX_PROPERTY = "input.prefix.data";

    /** The file suffix for data entries. */
    public static final String DATA_SUFFIX_PROPERTY = "input.suffix.data";

    /** Default compressed file suffix. */
    public static final String DEFAULT_COMPRESSED_SUFFIX = ".gz";

    /** Default file suffix for data entries. */
    public static final String DEFAULT_DATA_SUFFIX =
        StreamedMessagesClient.DEFAULT_DATA_SUFFIX;

    /** Default file suffix for done entries. */
    public static final String DEFAULT_DONE_SUFFIX = ".backup";

    /** Default input directory scan interval. */
    public static final ElapsedTime DEFAULT_SCAN_INTERVAL = ElapsedTime
        .fromMillis(60000);

    /** The file prefix for data entries. */
    public static final String DONE_PREFIX_PROPERTY = "input.prefix.done";

    /** The file suffix for done entries. */
    public static final String DONE_SUFFIX_PROPERTY = "input.suffix.done";

    /** The module's input directory. */
    public static final String INPUT_DIR_PROPERTY = "input.dir";

    /** The time in millis between scans of the input directory. */
    public static final String SCAN_INTERVAL_PROPERTY = "input.scan.interval";

    /** Semaphore directory property. */
    public static final String SEM_DIR_PROPERTY = "sem.dir";

    /** Semaphore files match enabled property. */
    public static final String SEM_MATCH_ENABLED_PROPERTY = "sem.match.enabled";

    /** Semaphore file name prefix property. */
    public static final String SEM_PREFIX_PROPERTY = "sem.prefix";

    /** Semaphore files purge enabled property. */
    public static final String SEM_PURGE_ENABLED_PROPERTY = "sem.purge.enabled";

    /** The file suffix for semaphore entries. */
    public static final String SEM_SUFFIX_PROPERTY = "sem.suffix";

    private String _compressedSuffix;
    private String _dataPrefix;
    private String _dataSuffix;
    private String _donePrefix;
    private String _doneSuffix;
    private _FilesWatcher _filesWatcher;
    private File _inputDirectory;
    private BlockingQueue<File> _inputFiles = new LinkedBlockingQueue<>();
    private ElapsedTime _scanInterval;
    private File _semDirectory;
    private boolean _semMatchEnabled;
    private String _semPrefix;
    private boolean _semPurgeEnabled;
    private String _semSuffix;

    /**
     * Files reader.
     */
    private final class _FilesReader
        extends AbstractInput
        implements ServiceThread.Target
    {
        /**
         * Constructs an instance.
         */
        _FilesReader() {}

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            final ServiceThread thread = _thread.getAndSet(null);

            if (thread != null) {
                getThisLogger()
                    .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
                Require
                    .ignored(
                        thread
                            .interruptAndJoin(
                                    getThisLogger(),
                                            getService().getJoinTimeout()));
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public synchronized boolean commit()
        {
            _entries = null;
            notifyAll();

            return super.commit();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "Streamed messages files";
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSourceName()
        {
            return _getInputDirectory().getName();
        }

        /** {@inheritDoc}
         */
        @Override
        public synchronized Optional<Serializable[]> input(
                final BatchControl batchControl)
            throws InterruptedException
        {
            while (_entries == null) {
                wait();
            }

            return Optional
                .of(_entries.toArray(new Serializable[_entries.size()]));
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _thread == null;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isReliable()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            final ServiceThread thread = new ServiceThread(
                this,
                "Streamed messages files reader");

            if (_thread.compareAndSet(null, thread)) {
                getThisLogger()
                    .debug(ServiceMessages.STARTING_THREAD, thread.getName());
                thread.start();
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void run()
            throws InterruptedException
        {
            final int batchLimit = getBatchControl().getLimit();

            for (;;) {
                final File inputFile = _takeInputFile();
                final String fileName = inputFile.getName();
                final boolean compressed = fileName
                    .toLowerCase(Locale.ROOT)
                    .endsWith(_getCompressedSuffix());
                InputStream inputStream;

                try {
                    inputStream = Files.newInputStream(inputFile.toPath());
                } catch (final NoSuchFileException exception) {
                    _logger
                        .debug(
                            ForwarderMessages.INPUT_FILE_DISAPPEARED,
                            inputFile);

                    continue;
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }

                if (compressed) {
                    try {
                        inputStream = new GZIPInputStream(inputStream);
                    } catch (final IOException exception) {
                        try {
                            inputStream.close();
                        } catch (final IOException closeException) {
                            throw new RuntimeException(closeException);
                        }

                        throw new RuntimeException(exception);
                    }
                }

                _logger.debug(ForwarderMessages.PROCESSING_FILE, inputFile);

                final String name = fileName
                    .substring(
                        _getDataPrefix().length(),
                        fileName.length()
                        - (_getDataSuffix().length()
                           + (compressed? _getCompressedSuffix()
                                   .length(): 0)));
                final File doneFile = new File(
                    _getInputDirectory(),
                    _getDonePrefix() + name + _getDoneSuffix()
                    + (compressed? _getCompressedSuffix(): ""));

                if (doneFile.delete()) {
                    _logger
                        .debug(
                            ForwarderMessages.DELETED_OLD_DONE_FILE,
                            doneFile);
                }

                final Streamer.Input input = _streamer
                    .newInput(inputStream, Optional.empty());
                final List<Serializable> entries =
                    new LinkedList<Serializable>();

                while (input.hasNext()) {
                    if (entries.size() >= batchLimit) {
                        _flush(entries);
                        entries.clear();
                    }

                    final Serializable message = input.next();

                    entries.add(message);
                    getTraces().add(message);
                }

                if (!entries.isEmpty()) {
                    _flush(entries);
                }

                input.close();

                if (!inputFile.renameTo(doneFile)) {
                    _logger
                        .error(
                            BaseMessages.FILE_RENAME_FAILED,
                            inputFile,
                            doneFile);

                    return;
                }

                _logger.debug(ForwarderMessages.FILE_PROCESSED, inputFile);

                if (_isSemPurgeEnabled()) {
                    final File semFile = new File(
                        _getSemDirectory(),
                        _getSemPrefix() + name + _getSemSuffix());

                    if (semFile.delete()) {
                        _logger
                            .debug(ForwarderMessages.PURGED_SEM_FILE, semFile);
                    }
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final KeyedGroups moduleProperties)
        {
            if (!super.setUp(moduleProperties)) {
                return false;
            }

            return _streamer
                .setUp(
                    Optional.of(getConfigProperties()),
                    Optional.of(moduleProperties));
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            _streamer.tearDown();

            super.tearDown();
        }

        private synchronized void _flush(
                @Nonnull final List<Serializable> entries)
            throws InterruptedException
        {
            _entries = entries;
            notifyAll();

            while (_entries != null) {
                wait();
            }
        }

        private List<Serializable> _entries;
        private final Logger _logger = Logger.getInstance(getClass());
        private final Streamer _streamer = Streamer.newInstance();
        private final AtomicReference<ServiceThread> _thread =
            new AtomicReference<>();
    }


    /**
     * Files watcher.
     */
    private final class _FilesWatcher
        implements ServiceThread.Target
    {
        /**
         * Constructs an instance.
         */
        _FilesWatcher()
        {
            _inputFilter = new FileFilter()
            {
                @Override
                public boolean accept(final File file)
                {
                    String name = file.getName();
                    String nameLower = name.toLowerCase(Locale.ROOT);

                    if (!nameLower.startsWith(_getDataPrefix())) {
                        return false;
                    }

                    if (nameLower.endsWith(_getCompressedSuffix())) {
                        name = name
                            .substring(
                                0,
                                name.length()
                                - _getCompressedSuffix().length());
                        nameLower = name.toLowerCase(Locale.ROOT);
                    }

                    if (!nameLower.endsWith(_getDataSuffix())) {
                        return false;
                    }

                    if (!_isSemEnabled()) {
                        return true;
                    }

                    final long semTime;

                    if (_isSemMatchEnabled()) {
                        name = name
                            .substring(
                                _getDataPrefix().length(),
                                name.length() - _getDataSuffix().length());

                        final File semFile = new File(
                            _getSemDirectory(),
                            _getSemPrefix() + name + _getSemSuffix());

                        semTime = semFile.isFile()? semFile.lastModified(): 0;
                    } else {
                        semTime = _getSemTime();
                    }

                    return semTime >= file.lastModified();
                }
            };

            try {
                _watchService = FileSystems.getDefault().newWatchService();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            _semFilter = _isSemEnabled()? new FileFilter()
            {
                @Override
                public boolean accept(final File file)
                {
                    final String name = file.getName().toLowerCase(Locale.ROOT);

                    return name.startsWith(_getSemPrefix())
                           && name.endsWith(_getSemSuffix());
                }
            }: null;
        }

        /** {@inheritDoc}
         */
        @Override
        public void run()
            throws IOException, InterruptedException
        {
            try {
                _scanInputDirectory();

                _getInputDirectory()
                    .toPath()
                    .register(
                        _watchService,
                        StandardWatchEventKinds.ENTRY_CREATE);

                if (_isSemEnabled()
                        && !_getInputDirectory().equals(_getSemDirectory())) {
                    _getSemDirectory()
                        .toPath()
                        .register(
                            _watchService,
                            StandardWatchEventKinds.ENTRY_CREATE);
                }

                ServiceThread.ready();

                for (;;) {
                    final WatchKey watchKey = _watchService.take();

                    for (WatchEvent<?> event: watchKey.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                            _logger
                                .error(
                                    ForwarderMessages.INPUT_DIRECTORY_OVERFLOW,
                                    watchKey.watchable());
                            _clearInputFiles();
                            _scanInputDirectory();

                            break;
                        }

                        final Path path = ((Path) watchKey.watchable())
                            .resolve(event.context().toString());
                        final File file = path.toFile();

                        if (_isSemEnabled()
                                && _semFilter.accept(path.toFile())) {
                            if (_isSemMatchEnabled()) {
                                final String fileName = file.getName();
                                final String name = fileName
                                    .substring(
                                        _getSemPrefix().length(),
                                        fileName.length()
                                        - _getSemSuffix().length());
                                final File inputFile = new File(
                                    _getInputDirectory(),
                                    _getDataPrefix() + name + _getDataSuffix());

                                if (inputFile.isFile()
                                        && (inputFile.lastModified()
                                            <= file.lastModified())) {
                                    _addInputFile(inputFile);
                                }
                            } else {
                                _clearInputFiles();
                                _scanInputDirectory();

                                break;
                            }
                        } else if (_inputFilter.accept(file)) {
                            _addInputFile(
                                new File(_getInputDirectory(), file.getName()));
                        }
                    }

                    if (!watchKey.reset()) {    // Cancelled by close.
                        break;
                    }
                }
            } catch (final ClosedWatchServiceException exception) {
                throw new InterruptedException();
            }
        }

        /**
         * Gets the semaphore time.
         *
         * @return The semaphore time.
         */
        @CheckReturnValue
        long _getSemTime()
        {
            return _semTime;
        }

        /**
         * Starts.
         */
        void start()
        {
            final ServiceThread thread = new ServiceThread(
                this,
                "Streamed messages files watcher");

            if (_thread.compareAndSet(null, thread)) {
                getThisLogger()
                    .debug(ServiceMessages.STARTING_THREAD, thread.getName());
                Require.ignored(thread.start(true));
            }
        }

        /**
         * Stops.
         */
        void stop()
        {
            final ServiceThread thread = _thread.getAndSet(null);

            if (thread != null) {
                getThisLogger()
                    .debug(ServiceMessages.STOPPING_THREAD, thread.getName());

                try {
                    _watchService.close();
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        private void _scanInputDirectory()
        {
            if (_isSemMatchEnabled() && _isSemPurgeEnabled()) {
                final File[] semFiles = Require
                    .notNull(_getSemDirectory().listFiles(_semFilter));

                for (final File semFile: semFiles) {
                    String name = semFile
                        .getName()
                        .substring(_getSemPrefix().length());

                    name = _getDataPrefix() + name.substring(
                        0,
                        name.length()
                        - _getSemSuffix().length()) + _getDataSuffix();

                    if (!(new File(_getInputDirectory(), name).isFile()
                            || new File(
                                _getInputDirectory(),
                                name + _getCompressedSuffix()).isFile())) {
                        if (semFile.delete()) {
                            _logger
                                .debug(
                                    ForwarderMessages.PURGED_SEM_FILE,
                                    semFile);
                        }
                    }
                }
            }

            if (_isSemEnabled() && !_isSemMatchEnabled()) {
                final File[] semFiles = Require
                    .notNull(_getSemDirectory().listFiles(_semFilter));

                for (final File semFile: semFiles) {
                    _semTime = Math.max(_semTime, semFile.lastModified());
                }

                if (_isSemPurgeEnabled()) {
                    for (final File semFile: semFiles) {
                        if (semFile.lastModified() < _semTime) {
                            if (semFile.delete()) {
                                _logger
                                    .debug(
                                        ForwarderMessages.PURGED_SEM_FILE,
                                        semFile);
                            }
                        }
                    }
                }
            }

            final File[] inputFiles = Require
                .notNull(_getInputDirectory().listFiles(_inputFilter));

            for (final File inputFile: inputFiles) {
                _addInputFile(
                    new File(_getInputDirectory(), inputFile.getName()));
            }
        }

        private final FileFilter _inputFilter;
        private final Logger _logger = Logger.getInstance(getClass());
        private final FileFilter _semFilter;
        private long _semTime;
        private final AtomicReference<ServiceThread> _thread =
            new AtomicReference<ServiceThread>();
        private final WatchService _watchService;
    }
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
