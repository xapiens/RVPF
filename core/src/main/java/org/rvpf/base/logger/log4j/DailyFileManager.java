/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DailyFileManager.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.logger.log4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;

import java.text.SimpleDateFormat;

import java.util.Calendar;

import javax.annotation.CheckReturnValue;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.util.Constants;

import org.rvpf.base.tool.Require;

/**
 * Daily file manager.
 */
public class DailyFileManager
    extends FileManager
{
    /**
     * Constructs an instance.
     *
     * @param fileName The pseudo file name.
     * @param outputFile The actual file.
     * @param outputStream The output stream.
     * @param layout The layout.
     * @param directory The log file directory.
     * @param fileNamePrefix The log file name prefix.
     * @param dateFormat The date format.
     * @param fileNameSuffix The log file name suffix.
     * @param maxFileSize The maximum file size.
     * @param fileSize The current file size.
     * @param tomorrow Tomorrow at midnight in millis.
     */
    @SuppressWarnings("hiding")
    protected DailyFileManager(
            final String fileName,
            final File outputFile,
            final OutputStream outputStream,
            final Layout<? extends Serializable> layout,
            final String directory,
            final String fileNamePrefix,
            final String dateFormat,
            final String fileNameSuffix,
            final long maxFileSize,
            final long fileSize,
            final long tomorrow)
    {
        super(
            null,
            fileName,
            outputStream,
            true,
            false,
            false,
            null,
            layout,
            layout != null,
            ByteBuffer.wrap(new byte[Constants.ENCODER_BYTE_BUFFER_SIZE]));

        _outputFile = outputFile;
        _directory = directory;
        _fileNamePrefix = fileNamePrefix;
        _dateFormat = dateFormat;
        _fileNameSuffix = fileNameSuffix;
        _maxFileSize = maxFileSize;
        _fileSize = fileSize;
        _tomorrow = tomorrow;
    }

    /**
     * Gets the daily file manager.
     *
     * @param directory The log file directory.
     * @param fileNamePrefix The log file name prefix.
     * @param dateFormat The date format.
     * @param fileNameSuffix The log file name suffix.
     * @param maxFileSize The maximum file size.
     * @param layout The layout.
     *
     * @return A daily file manager.
     */
    public static DailyFileManager getFileManager(
            final String directory,
            final String fileNamePrefix,
            final String dateFormat,
            final String fileNameSuffix,
            final long maxFileSize,
            final Layout<? extends Serializable> layout)
    {
        return (DailyFileManager) getManager(
            directory + '/' + fileNamePrefix + dateFormat + fileNameSuffix,
            new _FactoryData(
                directory,
                fileNamePrefix,
                dateFormat,
                fileNameSuffix,
                maxFileSize,
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

        if (System.currentTimeMillis() >= _tomorrow) {
            close();

            final Calendar calendar = _makeCalendar();
            final File outputFile = _makeOutputFile(
                _directory,
                _fileNamePrefix,
                _dateFormat,
                _fileNameSuffix,
                calendar);
            final OutputStream outputStream = _makeOutputStream(outputFile);

            setOutputStream(outputStream);

            if (outputStream == null) {
                return false;
            }

            _outputFile = outputFile;
            _fileSize = outputFile.length();
            _tomorrow = _tomorrow(calendar);
            _truncated = false;
        }

        return true;
    }

    /**
     * Asks if the file is oversize.
     *
     * @return True if oversize.
     */
    @CheckReturnValue
    public boolean isOversize()
    {
        return _fileSize > _maxFileSize;
    }

    /**
     * Asks if the file has been truncated.
     *
     * @return True if the file has been truncated.
     */
    @CheckReturnValue
    public boolean isTruncated()
    {
        return _truncated;
    }

    /**
     * Sets the 'truncated' indicator.
     *
     * @param truncated The new value of the 'truncated' indicator.
     */
    public void setTruncated(final boolean truncated)
    {
        _truncated = truncated;
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

        boolean interrupted = false;

        for (;;) {
            final FileChannel channel = ((FileOutputStream) getOutputStream())
                .getChannel();

            try {
                final FileLock lock = channel.lock();

                try {
                    super.write(bytes, offset, length);
                } finally {
                    lock.release();
                }

                break;
            } catch (final FileLockInterruptionException exception) {
                interrupted = true;    // Remembers for later.
                Thread.interrupted();    // Clears for now.
                close();
                setOutputStream(_makeOutputStream(_outputFile));
            } catch (final IOException exception) {
                throw new AppenderLoggingException(
                    "Failed to lock " + _outputFile.getAbsolutePath(),
                    exception);
            }
        }

        if (interrupted) {    // Restores the interrupt.
            Thread.currentThread().interrupt();
        }
    }

    static Calendar _makeCalendar()
    {
        final Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    static File _makeOutputFile(
            final String directory,
            final String fileNamePrefix,
            final String dateFormat,
            final String fileNameSuffix,
            final Calendar calendar)
    {
        final File directoryFile = new File(directory);

        if (!directoryFile.mkdirs()) {
            Require.success(directoryFile.isDirectory());
        }

        final String dateString = new SimpleDateFormat(dateFormat)
            .format(calendar.getTime());

        return new File(
            directoryFile,
            fileNamePrefix + dateString + fileNameSuffix);
    }

    static OutputStream _makeOutputStream(final File outputFile)
    {
        final OutputStream outputStream;

        try {
            outputStream = new FileOutputStream(outputFile, true);
        } catch (final FileNotFoundException exception) {
            LOGGER
                .error("Failed to open log file '" + outputFile + "': "
                       + exception);

            return null;
        }

        return outputStream;
    }

    static long _tomorrow(final Calendar calendar)
    {
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        return calendar.getTimeInMillis();
    }

    private static final _DailyFileManagerFactory _FACTORY =
        new _DailyFileManagerFactory();

    private final String _dateFormat;
    private final String _directory;
    private final String _fileNamePrefix;
    private final String _fileNameSuffix;
    private long _fileSize;
    private final long _maxFileSize;
    private File _outputFile;
    private long _tomorrow;
    private boolean _truncated;

    /**
     * Daily file manager factory.
     */
    private static class _DailyFileManagerFactory
        implements ManagerFactory<FileManager, _FactoryData>
    {
        /**
         * Constructs an instance.
         */
        _DailyFileManagerFactory() {}

        /**
         * Creates a daily file manager.
         *
         * @param name The name of the file.
         * @param data The factory data.
         *
         * @return The daily file manager.
         */
        @Override
        public DailyFileManager createManager(
                final String name,
                final _FactoryData data)
        {
            final Calendar calendar = _makeCalendar();
            final File outputFile = _makeOutputFile(
                data.directory,
                data.fileNamePrefix,
                data.dateFormat,
                data.fileNameSuffix,
                calendar);
            final OutputStream outputStream = _makeOutputStream(outputFile);

            if (outputStream == null) {
                return null;
            }

            return new DailyFileManager(
                name,
                outputFile,
                outputStream,
                data.layout,
                data.directory,
                data.fileNamePrefix,
                data.dateFormat,
                data.fileNameSuffix,
                data.maxFileSize,
                outputFile.length(),
                _tomorrow(calendar));
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
         * @param dateFormat The date format.
         * @param fileNameSuffix The log file name suffix.
         * @param maxFileSize The maximum file size.
         * @param layout The layout.
         */
        @SuppressWarnings("hiding")
        _FactoryData(
                final String directory,
                final String fileNamePrefix,
                final String dateFormat,
                final String fileNameSuffix,
                final long maxFileSize,
                final Layout<? extends Serializable> layout)
        {
            this.directory = directory;
            this.fileNamePrefix = fileNamePrefix;
            this.dateFormat = dateFormat;
            this.fileNameSuffix = fileNameSuffix;
            this.maxFileSize = maxFileSize;
            this.layout = layout;
        }

        final String dateFormat;
        final String directory;
        final String fileNamePrefix;
        final String fileNameSuffix;
        final Layout<? extends Serializable> layout;
        final long maxFileSize;
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
