/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DailyFileAppender.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.logger.log4j;

import java.io.Serializable;

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.OptionConverter;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StringMap;

/**
 * Daily file appender.
 */
@Plugin(
    name = "DailyFile",
    category = "Core",
    elementType = "appender",
    printObject = true
)
public final class DailyFileAppender
    extends AbstractOutputStreamAppender<DailyFileManager>
{
    /**
     * Constructs an instance.
     *
     * @param name The name of the appender.
     * @param filter The filter or null.
     * @param layout The layout or null.
     * @param ignoreExceptions True ignores exceptions.
     * @param manager The daily file manager.
     */
    private DailyFileAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions,
            final DailyFileManager manager)
    {
        super(name, layout, filter, ignoreExceptions, true, manager);
    }

    /**
     * Creates a daily file appender.
     *
     * @param name The name of the appender.
     * @param directory The log file directory.
     * @param fileNamePrefix The log file name prefix.
     * @param dateFormat The date format.
     * @param fileNameSuffix The log file name suffix.
     * @param maxFileSize The maximum file size.
     * @param layout The layout or null.
     * @param filter The filter or null.
     * @param ignoreExceptions True ignores exceptions.
     *
     * @return the new daily file appender.
     */
    @PluginFactory
    public static DailyFileAppender createAppender(
    //J-
        @PluginAttribute(value = "name")
        final String name,
        @PluginAttribute(value = "directory", defaultString = ".")
        final String directory,
        @PluginAttribute(value = "fileNamePrefix")
        final String fileNamePrefix,
        @PluginAttribute(value = "dateFormat", defaultString = "yyyy-MM-dd")
        final String dateFormat,
        @PluginAttribute(value = "fileNameSuffix", defaultString = ".log")
        final String fileNameSuffix,
        @PluginAttribute(value = "maxFileSize", defaultString = "0")
        final String maxFileSize,
        @PluginElement("Filter")
        Filter filter,
        @PluginElement("Layout")
        final Layout<? extends Serializable> layout,
        @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true)
        final boolean ignoreExceptions)
    //J+
    {
        if (name == null) {
            LOGGER.error("No name provided for DailyFileAppender");

            return null;
        }

        if (filter == null) {
            filter = ThresholdFilter.createFilter(Level.INFO, null, null);
        }

        final DailyFileManager manager = DailyFileManager.getFileManager(
            directory,
            (fileNamePrefix != null)? fileNamePrefix: "",
            dateFormat,
            fileNameSuffix,
            OptionConverter.toFileSize(maxFileSize, 0),
            layout);

        if (manager == null) {
            return null;
        }

        return new DailyFileAppender(
            name,
            filter,
            layout,
            ignoreExceptions,
            manager);
    }

    /** {@inheritDoc}
     */
    @Override
    public void append(final LogEvent event)
    {
        final DailyFileManager manager = getManager();

        synchronized (manager) {
            if (manager.checkRollover()) {
                if (manager.isOversize()) {
                    if (!manager.isTruncated()) {
                        final Log4jLogEvent.Builder eventBuilder =
                            Log4jLogEvent.newBuilder();

                        eventBuilder.setLoggerName(event.getLoggerName());
                        eventBuilder.setLoggerFqcn(getClass().getName());
                        eventBuilder.setLevel(Level.INFO);
                        eventBuilder.setMessage(
                            new SimpleMessage("... [truncated]"));

                        final StringMap contextData =
                            ContextDataFactory.createContextData();

                        for (final Map.Entry<String, String> entry:
                                ThreadContext.getImmutableContext()
                                    .entrySet()) {
                            contextData.putValue(
                                entry.getKey(),
                                entry.getValue());
                        }

                        eventBuilder.setContextData(contextData);
                        eventBuilder.setContextStack(
                            ThreadContext.getImmutableStack());
                        eventBuilder.setThreadName(
                            Thread.currentThread().getName());
                        eventBuilder.setTimeMillis(System.currentTimeMillis());
                        super.append(eventBuilder.build());
                        manager.setTruncated(true);
                    }
                } else {
                    super.append(event);
                }
            }
        }
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
