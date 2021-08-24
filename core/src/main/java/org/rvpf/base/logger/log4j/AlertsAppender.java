/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AlertsAppender.java 4067 2019-06-08 13:39:16Z SFB $
 */

package org.rvpf.base.logger.log4j;

import java.io.Serializable;

import java.rmi.NoSuchObjectException;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Booleans;

import org.rvpf.base.alert.Alert;
import org.rvpf.base.logger.Logger;

/**
 * Alerts appender.
 */
@Plugin(
    name = "Alerts",
    category = "Core",
    elementType = "appender",
    printObject = true
)
public final class AlertsAppender
    extends AbstractAppender
{
    /**
     * Constructs an instance.
     *
     * @param name The name of the appender.
     * @param layout The layout or null.
     * @param filter The filter or null.
     * @param ignoreExceptions True ignores exceptions.
     */
    private AlertsAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions)
    {
        super(name, filter, layout, ignoreExceptions);
    }

    /**
     * Creates an alerts appender.
     *
     * @param name The name of the appender.
     * @param layout The layout or null.
     * @param filter The filter or null.
     * @param ignoreExceptions True ignores exceptions.
     *
     * @return the new alerts appender.
     */
    @PluginFactory
    public static AlertsAppender createAppender(
    //J-
        @PluginAttribute("name")
        final String name,
        @PluginElement("Layout")
        final Layout<? extends Serializable> layout,
        @PluginElement("Filter")
        final Filter filter,
        @PluginAttribute("ignoreExceptions")
        final String ignoreExceptions)
    //J+
    {
        if (name == null) {
            LOGGER.error("No name provided for MailAppender");

            return null;
        }

        final boolean ignore = Booleans.parseBoolean(ignoreExceptions, true);

        return new AlertsAppender(name, filter, layout, ignore);
    }

    /** {@inheritDoc}
     */
    @Override
    public void append(final LogEvent event)
    {
        _sender.send(event);
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        _sender.start();

        super.start();
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        super.stop();

        _sender.close();
    }

    private final _AlertsSender _sender = new _AlertsSender();

    /**
     * Alerts sender.
     */
    private static final class _AlertsSender
        implements Runnable
    {
        /**
         * Constructs an instance.
         */
        _AlertsSender() {}

        /** {@inheritDoc}
         */
        @Override
        public void run()
        {
            Logger.setLogID(Optional.ofNullable(_logID));

            try {
                for (;;) {
                    final _ThreadEvent threadEvent = _queue.take();
                    final ClassLoader classLoader = threadEvent
                        .getThread()
                        .getContextClassLoader();

                    if (classLoader instanceof Alert.Dispatcher) {
                        final LogEvent event = threadEvent.getEvent();
                        final Object id = event
                            .getContextData()
                            .getValue(Logger.ID_KEY);
                        final String alertName = ((id != null)? id
                            .toString(): "") + ':' + event.getLoggerName();
                        final String alertInfo = event
                            .getMessage()
                            .getFormattedMessage();
                        final Level level = event.getLevel();
                        final Logger.LogLevel alertLevel;

                        if (level.isMoreSpecificThan(Level.FATAL)) {
                            alertLevel = Logger.LogLevel.FATAL;
                        } else if (level.isMoreSpecificThan(Level.ERROR)) {
                            alertLevel = Logger.LogLevel.ERROR;
                        } else if (level.isMoreSpecificThan(Level.WARN)) {
                            alertLevel = Logger.LogLevel.WARN;
                        } else {
                            alertLevel = Logger.LogLevel.INFO;
                        }

                        try {
                            ((Alert.Dispatcher) classLoader)
                                .dispatchAlert(
                                    alertLevel,
                                    alertName,
                                    alertInfo);
                        } catch (final RuntimeException exception) {
                            if (!(exception.getCause()
                                    instanceof NoSuchObjectException)) {
                                throw exception;
                            }
                        }
                    }
                }
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Closes current activities.
         */
        void close()
        {
            final Thread currentThread = Thread.currentThread();
            final int priority = currentThread.getPriority();

            currentThread.setPriority(Thread.MIN_PRIORITY + 1);
            Thread.yield();
            currentThread.setPriority(priority);

            if (_thread != null) {
                _thread.interrupt();
            }
        }

        /**
         * Sends a logging event.
         *
         * @param event The logging event.
         */
        void send(final LogEvent event)
        {
            _queue.add(new _ThreadEvent(event));
        }

        /**
         * Starts the thread.
         */
        void start()
        {
            _thread = new Thread(this, "Alerts sender");
            _logID = Logger.currentLogID().orElse(null);
            _thread.start();
        }

        private String _logID;
        private final BlockingQueue<_ThreadEvent> _queue =
            new LinkedBlockingQueue<>();
        private Thread _thread;
    }


    /**
     * Thread event.
     */
    private static final class _ThreadEvent
    {
        /**
         * Constructs an instance.
         *
         * @param event The log event.
         */
        _ThreadEvent(final LogEvent event)
        {
            _thread = Thread.currentThread();
            _event = event;
        }

        /**
         * Gets the log event.
         *
         * @return The log event.
         */
        LogEvent getEvent()
        {
            return _event;
        }

        /**
         * Gets the thread.
         *
         * @return The thread.
         */
        Thread getThread()
        {
            return _thread;
        }

        private final LogEvent _event;
        private final Thread _thread;
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
