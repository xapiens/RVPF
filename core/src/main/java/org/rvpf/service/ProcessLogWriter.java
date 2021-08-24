/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProcessLogWriter.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.service;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.pipe.PipeRequest;
import org.rvpf.base.tool.Require;

/**
 * Process log writer.
 *
 * <p>Runs in a thread to transfer the content of 'stderr' to the logging
 * service.</p>
 *
 * <p>The log object identification is created by appending ':' and an owner
 * name to the class name. This helps identification and control of the Log.</p>
 *
 * <p>Each line is scanned for a prefix identifying the log level.</p>
 *
 * <p>The case sensitive identifiers "TRACE", "DEBUG", "INFO", "WARN", "ERROR"
 * and "FATAL" are recognized when they start at the beginning of a line and are
 * followed by a space. If a line begins with a space or a tab, it is considered
 * a continuation of the last identified level. The default level is "WARN".</p>
 */
public final class ProcessLogWriter
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param ownerName The name of the owner.
     * @param stderr The stream to read from.
     */
    ProcessLogWriter(
            @Nonnull final String ownerName,
            @Nonnull final BufferedReader stderr)
    {
        _ownerName = ownerName;
        _logger = Logger.getInstance(getClass().getName() + ':' + ownerName);
        _stderr = stderr;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        Level previousLevel = Level.WARN;

        try {
            for (;;) {
                String line;

                try {
                    line = _stderr.readLine();
                } catch (final IOException exception) {
                    line = null;
                }

                if (line == null) {
                    if (!_closed) {
                        _logger.debug(ServiceMessages.PROCESS_LOG_END);
                    }

                    try {
                        _stderr.close();
                    } catch (final IOException exception) {
                        _logger
                            .debug(
                                ServiceMessages.PROCESS_LOG_CLOSE_FAILED,
                                exception.getMessage());
                    }

                    break;
                }

                if ((_closed) || line.trim().isEmpty()) {
                    continue;
                }

                final String prefix;
                final Level level;

                if (line.startsWith(_TRACE_PREFIX)) {
                    prefix = _TRACE_PREFIX;
                    level = Level.TRACE;
                } else if (line.startsWith(_DEBUG_PREFIX)) {
                    prefix = _DEBUG_PREFIX;
                    level = Level.DEBUG;
                } else if (line.startsWith(_INFO_PREFIX)) {
                    prefix = _INFO_PREFIX;
                    level = Level.INFO;
                } else if (line.startsWith(_WARN_PREFIX)) {
                    prefix = _WARN_PREFIX;
                    level = Level.WARN;
                } else if (line.startsWith(_ERROR_PREFIX)) {
                    prefix = _ERROR_PREFIX;
                    level = Level.ERROR;
                } else if (line.startsWith(_FATAL_PREFIX)) {
                    prefix = _FATAL_PREFIX;
                    level = Level.FATAL;
                } else {
                    prefix = "";

                    if (line.startsWith(" ") || line.startsWith("\t")) {
                        level = previousLevel;
                    } else {
                        level = Level.WARN;
                    }
                }

                line = line.substring(prefix.length());

                switch (level) {
                    case TRACE: {
                        _logger.trace(BaseMessages.VERBATIM, line);

                        break;
                    }
                    case DEBUG: {
                        _logger.debug(BaseMessages.VERBATIM, line);

                        break;
                    }
                    case INFO: {
                        _logger.info(BaseMessages.VERBATIM, line);

                        break;
                    }
                    case WARN: {
                        _logger.warn(BaseMessages.VERBATIM, line);

                        break;
                    }
                    case ERROR: {
                        _logger.error(BaseMessages.VERBATIM, line);

                        break;
                    }
                    case FATAL: {
                        _logger.fatal(BaseMessages.VERBATIM, line);

                        break;
                    }
                    default: {
                        Require.failure();
                    }
                }

                previousLevel = level;
            }
        } finally {
            if (_closed) {
                _thread.get().setQuiet(true);
            }
        }
    }

    /**
     * Closes output operations.
     *
     * <p>This makes sure that the log will not be used since this is called
     * before closing the log.</p>
     */
    void close()
    {
        _closed = true;
    }

    /**
     * Asks if the thread is alive.
     *
     * @return True if alive.
     */
    @CheckReturnValue
    boolean isAlive()
    {
        final ServiceThread thread = _thread.get();

        return (thread != null) && thread.isAlive();
    }

    /**
     * Waits for the source to close.
     *
     * @param millis The maximum number of millis (0 is forever).
     *
     * @throws InterruptedException When appropriate.
     */
    void join(final long millis)
        throws InterruptedException
    {
        final ServiceThread thread = _thread.get();

        if (thread != null) {
            thread.join(millis);
        }
    }

    /**
     * Starts the thread.
     */
    void start()
    {
        final ServiceThread thread = new ServiceThread(
            this,
            "Log writer (" + _ownerName + ")");

        if (_thread.compareAndSet(null, thread)) {
            thread.setDaemon(true);
            _logger.debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    private static final String _DEBUG_PREFIX = PipeRequest.DEBUG_LEVEL + ' ';
    private static final String _ERROR_PREFIX = PipeRequest.ERROR_LEVEL + ' ';
    private static final String _FATAL_PREFIX = PipeRequest.FATAL_LEVEL + ' ';
    private static final String _INFO_PREFIX = PipeRequest.INFO_LEVEL + ' ';
    private static final String _TRACE_PREFIX = PipeRequest.TRACE_LEVEL + ' ';
    private static final String _WARN_PREFIX = PipeRequest.WARN_LEVEL + ' ';

    private volatile boolean _closed;
    private final Logger _logger;
    private final String _ownerName;
    private final BufferedReader _stderr;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();

    private enum Level
    {
        FATAL,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE
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
