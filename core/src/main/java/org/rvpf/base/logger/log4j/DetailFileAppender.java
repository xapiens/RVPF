/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DetailFileAppender.java 3981 2019-05-13 15:00:56Z SFB $
 */

package org.rvpf.base.logger.log4j;

import java.io.Serializable;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.OptionConverter;

/**
 * Detail file appender.
 */
@Plugin(
    name = "DetailFile",
    category = "Core",
    elementType = "appender",
    printObject = true
)
public final class DetailFileAppender
    extends AbstractOutputStreamAppender<DetailFileManager>
{
    /**
     * Constructs an instance.
     *
     * @param name The name of the appender.
     * @param filter The filter or null.
     * @param immediateFlush True to flush on every write.
     * @param layout The layout or null.
     * @param ignoreExceptions True ignores exceptions.
     * @param manager The detail file manager.
     */
    private DetailFileAppender(
            final String name,
            final Filter filter,
            final boolean immediateFlush,
            final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions,
            final DetailFileManager manager)
    {
        super(name, layout, filter, ignoreExceptions, immediateFlush, manager);
    }

    /**
     * Creates a detail file appender.
     *
     * @param name The name of the appender.
     * @param directory The log file directory.
     * @param fileNamePrefix The log file name prefix.
     * @param fileNameSuffix The log file name suffix.
     * @param maxFileSize The maximum file size.
     * @param maxBackups The maximum number of backup files.
     * @param locking True if the file should be locked.
     * @param immediateFlush True to flush on every write.
     * @param bufferedIo True if I/O should be buffered.
     * @param bufferSize Buffer size for buffered IO.
     * @param layout The layout or null.
     * @param filter The filter or null.
     * @param ignoreExceptions True ignores exceptions.
     *
     * @return the new detail file appender.
     */
    @PluginFactory
    public static DetailFileAppender createAppender(
    //J-
        @PluginAttribute(value = "name")
        final String name,
        @PluginAttribute(value = "directory", defaultString = ".")
        final String directory,
        @PluginAttribute(value = "fileNamePrefix")
        String fileNamePrefix,
        @PluginAttribute(value = "fileNameSuffix", defaultString = ".log")
        final String fileNameSuffix,
        @PluginAttribute(value = "maxFileSize", defaultString = "0")
        String maxFileSize,
        @PluginAttribute(value = "maxBackups", defaultInt = 1)
        int maxBackups,
        @PluginAttribute(value = "locking", defaultBoolean = false)
        final boolean locking,
        @PluginAttribute(value = "immediateFlush", defaultBoolean = true)
        final boolean immediateFlush,
        @PluginAttribute(value = "bufferedIo", defaultBoolean = true)
        boolean bufferedIo,
        @PluginAttribute(value = "bufferSize", defaultInt = _DEFAULT_BUFFER_SIZE)
        int bufferSize,
        @PluginElement("Filter")
        final Filter filter,
        @PluginElement("Layout")
        final Layout<? extends Serializable> layout,
        @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true)
        final boolean ignoreExceptions)
    //J+
    {
        if (name == null) {
            LOGGER.error("No name provided for DetailFileAppender");

            return null;
        }

        if (locking && bufferedIo) {
            LOGGER
                .warn("Locking and buffering are mutually exclusive: buffering is disabled");
            bufferedIo = false;
        }

        if (!bufferedIo) {
            bufferSize = -1;
        }

        String property;

        property = System.getProperty(LOG_PREFIX_PROPERTY);

        if (property != null) {
            fileNamePrefix = property;
        }

        property = System.getProperty(LOG_SIZE_PROPERTY);

        if (property != null) {
            maxFileSize = property;
        }

        property = System.getProperty(LOG_BACKUPS_PROPERTY);

        if (property != null) {
            maxBackups = Integer.parseInt(property);
        }

        final DetailFileManager manager = DetailFileManager
            .getFileManager(
                directory,
                (fileNamePrefix != null)? fileNamePrefix: "",
                maxBackups,
                fileNameSuffix,
                OptionConverter.toFileSize(maxFileSize, 0),
                locking,
                bufferSize,
                layout);

        if (manager == null) {
            return null;
        }

        return new DetailFileAppender(
            name,
            filter,
            immediateFlush,
            layout,
            ignoreExceptions,
            manager);
    }

    /** {@inheritDoc}
     */
    @Override
    public void append(final LogEvent event)
    {
        final DetailFileManager manager = getManager();

        synchronized (manager) {
            if (manager.checkRollover()) {
                super.append(event);
            }
        }
    }

    /** Specifies the number of backup log files. */
    public static final String LOG_BACKUPS_PROPERTY = "rvpf.log.backups";

    /** Specifies the log file prefix. */
    public static final String LOG_PREFIX_PROPERTY = "rvpf.log.prefix";

    /** Specifies the log file size. */
    public static final String LOG_SIZE_PROPERTY = "rvpf.log.size";

    /**  */

    private static final int _DEFAULT_BUFFER_SIZE = 8192;
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
