/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DetailFileManager.java 3981 2019-05-13 15:00:56Z SFB $
 */

package org.rvpf.base.logger.log4j;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckReturnValue;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.util.Constants;

import org.rvpf.base.tool.Require;
 
/**
 * Detail file manager. 
 */
public class DetailFileManager
    extends FileManager
{
    /**
     * Constructs an instance.
     *
     * @param fileName The file name.
     * @param outputStream The output stream.
     * @param logFiles The log files.
     * @param maxFileSize The maximum file size.
     * @param locking True if the file should be locked.
     * @param bufferSize Buffer size for buffered IO.
     * @param layout The layout.
     */
    @SuppressWarnings("hiding")
    protected DetailFileManager(
            final String fileName,
            final OutputStream outputStream,
            final _LogFiles logFiles,
            final long maxFileSize,
            final boolean locking,
            final int bufferSize,
            final Layout<? extends Serializable> layout)
    {
        super(
            null,
            fileName,
            outputStream,
            false,
            locking,
            false,
            null,
            layout,
            layout != null,
            ByteBuffer.wrap(
                new byte[bufferSize > 0? bufferSize
                    : Constants.ENCODER_BYTE_BUFFER_SIZE]));

        _logFiles = logFiles;
        _maxFileSize = maxFileSize;
    }

    /**
     * Gets the detail file manager.
     *
     * @param directory The log file directory.
     * @param fileNamePrefix The log file name prefix.
     * @param maxBackups The maximum number of backup files.
     * @param fileNameSuffix The log file name suffix.
     * @param maxFileSize The maximum file size.
     * @param locking True if the file should be locked.
     * @param bufferSize Buffer size for buffered IO.
     * @param layout The layout.
     *
     * @return A detail file manager.
     */
    public static DetailFileManager getFileManager(
            final String directory,
            final String fileNamePrefix,
            final int maxBackups,
            final String fileNameSuffix,
            final long maxFileSize,
            final boolean locking,
            final int bufferSize,
            final Layout<? extends Serializable> layout)
    {
        return (DetailFileManager) getManager(
            directory + fileNamePrefix + maxBackups + fileNameSuffix,
            new _FactoryData(
                directory,
                fileNamePrefix,
                maxBackups,
                fileNameSuffix,
                maxFileSize,
                locking,
                bufferSize,
                layout),
            _FACTORY);
    }

    /**
     * Checks if a rollover should occur.
     *
     * @return False on rollover error.
     */
    @CheckReturnValue
    public boolean checkRollover()
    {
        if (getOutputStream() == null) {
            return false;
        }

        if (_fileSize > _maxFileSize) {
            close();

            final OutputStream outputStream = _makeOutputStream(
                _logFiles.newLogFile(),
                getBufferSize());

            setOutputStream(outputStream);

            if (outputStream == null) {
                return false;
            }

            _fileSize = 0;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected OutputStream getOutputStream()
    {
        try {
            return super.getOutputStream();
        } catch (final IOException exception) {
            throw new AppenderLoggingException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected synchronized void write(
            final byte[] bytes,
            final int offset,
            final int length)
    {
        _fileSize += length;

        super.write(bytes, offset, length);
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    static Logger _getLogger()
    {
        return LOGGER;
    }

    static OutputStream _makeOutputStream(
            final File outputFile,
            final int bufferSize)
    {
        OutputStream outputStream;

        try {
            outputStream = new FileOutputStream(outputFile, false);
        } catch (final FileNotFoundException exception) {
            LOGGER
                .error("Failed to open log file '" + outputFile + "': "
                       + exception);

            return null;
        }

        if (bufferSize > 0) {
            outputStream = new BufferedOutputStream(outputStream, bufferSize);
        }

        return outputStream;
    }

    private static final _DetailFileManagerFactory _FACTORY =
        new _DetailFileManagerFactory();

    private long _fileSize;
    private final _LogFiles _logFiles;
    private final long _maxFileSize;

    /**
     * Detail file manager factory.
     */
    private static class _DetailFileManagerFactory
        implements ManagerFactory<FileManager, _FactoryData>
    {
        /**
         * Constructs an instance.
         */
        _DetailFileManagerFactory() {}

        /**
         * Creates a detail file manager.
         *
         * @param name The name of the file.
         * @param data The factory data.
         *
         * @return The detail file manager.
         */
        @Override
        public DetailFileManager createManager(
                final String name,
                final _FactoryData data)
        {
            final File directoryFile = new File(data.directory);

            if (!directoryFile.mkdirs()) {
                if (!directoryFile.isDirectory()) {
                    _getLogger()
                        .error("Failed to create log directory '"
                               + directoryFile.getAbsolutePath() + "'");

                    return null;
                }
            }

            final _LogFiles logFiles = new _LogFiles(
                data.directory,
                data.fileNamePrefix,
                data.maxBackups,
                data.fileNameSuffix);

            logFiles.scan();

            final OutputStream outputStream = _makeOutputStream(
                logFiles.newLogFile(),
                data.bufferSize);

            if (outputStream == null) {
                return null;
            }

            return new DetailFileManager(
                name,
                outputStream,
                logFiles,
                data.maxFileSize,
                data.locking,
                data.bufferSize,
                data.layout);
        }
    }


    /**
     * Factory data.
     */
    private static class _FactoryData
    {
        /**
         * Constructs an instance.
         *
         * @param directory The log file directory.
         * @param fileNamePrefix The log file name prefix.
         * @param maxBackups The maximum number of backup files.
         * @param fileNameSuffix The log file name suffix.
         * @param maxFileSize The maximum file size.
         * @param locking True if the file should be locked.
         * @param bufferSize Buffer size for buffered IO.
         * @param layout The layout.
         */
        @SuppressWarnings("hiding")
        _FactoryData(
                final String directory,
                final String fileNamePrefix,
                final int maxBackups,
                final String fileNameSuffix,
                final long maxFileSize,
                final boolean locking,
                final int bufferSize,
                final Layout<? extends Serializable> layout)
        {
            this.directory = directory;
            this.fileNamePrefix = fileNamePrefix;
            this.maxBackups = maxBackups;
            this.fileNameSuffix = fileNameSuffix;
            this.maxFileSize = maxFileSize;
            this.locking = locking;
            this.bufferSize = bufferSize;
            this.layout = layout;
        }

        final int bufferSize;
        final String directory;
        final String fileNamePrefix;
        final String fileNameSuffix;
        final Layout<? extends Serializable> layout;
        final boolean locking;
        final int maxBackups;
        final long maxFileSize;
    }


    /**
     * Log files.
     */
    private static class _LogFiles
        implements FileFilter
    {
        /**
         * Constructs an instance.
         *
         * @param directory
         * @param fileNamePrefix
         * @param maxBackups
         * @param fileNameSuffix
         */
        _LogFiles(
                final String directory,
                final String fileNamePrefix,
                final int maxBackups,
                final String fileNameSuffix)
        {
            _directory = directory;
            _fileNamePrefix = fileNamePrefix;
            _maxBackups = maxBackups;
            _fileNameSuffix = fileNameSuffix;

            _sequenceLength = 1 + (int) Math.log10(3 * _maxBackups);
            _maxSequence = (10 * _sequenceLength) - 1;
            _fileNamePattern = Pattern
                .compile(
                    Pattern
                        .quote(_fileNamePrefix) + "-([0-9]{" + _sequenceLength
                        + "})" + Pattern.quote(
                                _fileNameSuffix),
                    Pattern.CASE_INSENSITIVE);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean accept(final File file)
        {
            return _fileNamePattern.matcher(file.getName()).matches();
        }

        /**
         * Returns a new log file.
         *
         * @return The new log file.
         */
        File newLogFile()
        {
            while (_logFiles.size() > _maxBackups) {
                final File oldestFile = _logFiles.removeFirst();

                if (!oldestFile.delete()) {
                    _getLogger()
                        .warn("Failed to delete old log file '"
                              + oldestFile.getAbsolutePath() + "'");
                }
            }

            int sequence;

            if (_logFiles.isEmpty()) {
                sequence = 0;
            } else {
                final File newestFile = _logFiles.getLast();
                final Matcher matcher = _fileNamePattern
                    .matcher(newestFile.getName());

                matcher.matches();
                sequence = Integer.parseInt(matcher.group(1));
            }

            if (++sequence > _maxSequence) {
                sequence = 1;
            }

            final File newLogFile = new File(
                _directory,
                String.format(
                    (Locale) null,
                    "%s-%0" + _sequenceLength + "d%s",
                    _fileNamePrefix,
                    Integer.valueOf(sequence),
                    _fileNameSuffix));

            _logFiles.addLast(newLogFile);

            return newLogFile;
        }

        /**
         * Scans the log files.
         */
        void scan()
        {
            final class _FileCreationTime
                implements Comparable<_FileCreationTime>
            {
                _FileCreationTime(final File file, final FileTime creationTime)
                {
                    _file = file;
                    _creationTime = creationTime;
                }

                /** {@inheritDoc}
                 */
                @Override
                public int compareTo(final _FileCreationTime other)
                {
                    return _creationTime.compareTo(other._creationTime);
                }

                /** {@inheritDoc}
                 */
                @Override
                public boolean equals(final Object other)
                {
                    throw new UnsupportedOperationException();
                }

                /** {@inheritDoc}
                 */
                @Override
                public int hashCode()
                {
                    throw new UnsupportedOperationException();
                }

                /**
                 * Gets the file.
                 *
                 * @return The file.
                 */
                File getFile()
                {
                    return _file;
                }

                private final FileTime _creationTime;
                private final File _file;
            }

            final File directoryFile = new File(_directory);
            final File[] directoryFiles =
                Require.notNull(directoryFile.listFiles(this));

            _logFiles.clear();

            if (directoryFiles.length < 1) {
                return;
            }

            final _FileCreationTime[] fileCreationTimes =
                new _FileCreationTime[directoryFiles.length];

            for (int i = 0; i < fileCreationTimes.length; ++i) {
                final File file = directoryFiles[i];

                try {
                    fileCreationTimes[i] = new _FileCreationTime(
                        file,
                        (FileTime) Files.getAttribute(
                            file.toPath(),
                            "creationTime"));
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }
            }

            Arrays.sort(fileCreationTimes);

            for (final _FileCreationTime fileCreationTime: fileCreationTimes) {
                _logFiles.add(fileCreationTime.getFile());
            }
        }

        private final String _directory;
        private final Pattern _fileNamePattern;
        private final String _fileNamePrefix;
        private final String _fileNameSuffix;
        private final LinkedList<File> _logFiles = new LinkedList<>();
        private final int _maxBackups;
        private final int _maxSequence;
        private final int _sequenceLength;
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
