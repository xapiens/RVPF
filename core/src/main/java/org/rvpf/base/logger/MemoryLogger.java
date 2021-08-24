/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MemoryLogger.java 4109 2019-07-22 23:31:37Z SFB $
 */

package org.rvpf.base.logger;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.ValueConverter;

/**
 * Memory logger.
 *
 * <p>
 * Used to log memory informations.
 *
 * These informations include the maximum amount of memory available to the JVM,
 * the total amount actually allocated to the JVM and the amount of that memory
 * available for future allocated objects.
 *
 * The logging may be triggered by a call or activated on an interval.
 * </p>
 */
@ThreadSafe
public final class MemoryLogger
    implements Runnable
{
    private MemoryLogger() {}

    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance.
     */
    @Nonnull
    @CheckReturnValue
    public static MemoryLogger getInstance()
    {
        return _INSTANCE;
    }

    /**
     * Logs memory informations.
     */
    public static void logMemoryInfo()
    {
        if (_LOGGER.isDebugEnabled()) {
            final Runtime runtime = Runtime.getRuntime();
            final long max = ValueConverter
                .roundToMebibytes(runtime.maxMemory());
            final long total = ValueConverter
                .roundToMebibytes(runtime.totalMemory());
            final long free = ValueConverter
                .roundToMebibytes(runtime.freeMemory());
            final long used = total - free;

            _LOGGER
                .debug(
                    BaseMessages.MEMORY,
                    String.valueOf(used),
                    String.valueOf(free),
                    String.valueOf(total),
                    String.valueOf(max));
        }
    }

    /**
     * Activates.
     *
     * <p>
     * Begins by logging memory informations.
     *
     * If a log interval is present, ensure that logging will continue on that
     * interval, starting a new thread if needed.
     *
     * If logging was already active, the previous interval is saved on a stack.
     * </p>
     *
     * @param logInterval The optional elapsed time between logs.
     */
    public void activate(@Nonnull final Optional<ElapsedTime> logInterval)
    {
        synchronized (_activations) {
            logMemoryInfo();

            final ElapsedTime elapsedTime = logInterval.orElse(_elapsedTime);

            if (elapsedTime != null) {
                if (_elapsedTime == null) {
                    final Thread thread = new Thread(this, "Memory logger");

                    thread.setDaemon(true);
                    _elapsedTime = elapsedTime;
                    thread.start();
                } else {
                    _activations.addLast(_elapsedTime);
                    _elapsedTime = elapsedTime;
                    _reset();
                }
            }
        }
    }

    /**
     * Deactivates.
     *
     * <p>
     * Removes the current activation and activates the previous one if present.
     *
     * Logs memory informations a last time for the removed activation.
     * </p>
     */
    public void deactivate()
    {
        synchronized (_activations) {
            if (_elapsedTime != null) {
                if (_activations.isEmpty()) {
                    _elapsedTime = null;
                    _reset();
                } else {
                    _elapsedTime = _activations.removeLast();
                }
            }

            logMemoryInfo();
        }
    }

    /**
     * Called on midnight event.
     *
     * <p>
     * If this is the first call for the day, logs memory informations.
     * </p>
     */
    public void onMidnightEvent()
    {
        synchronized (_activations) {
            final DateTime midnight = DateTime.now().midnight();

            if (!midnight.equals(_lastMidnight)) {
                logMemoryInfo();
                _lastMidnight = midnight;
                _reset();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        synchronized (_activations) {
            try {
                while (_elapsedTime != null) {
                    final long wakeMillis = DateTime
                        .now()
                        .after(_elapsedTime)
                        .toMillis();

                    do {
                        final long snoozeMillis = wakeMillis
                            - System.currentTimeMillis();

                        if (snoozeMillis <= 0) {
                            logMemoryInfo();

                            break;
                        }

                        _activations.wait(snoozeMillis);
                    } while (!_reset);

                    _reset = false;
                }
            } catch (final InterruptedException exception) {
                throw new InternalError(exception);    // Should not happen.
            }
        }
    }

    private void _reset()
    {
        _reset = true;
        _activations.notifyAll();
    }

    private static final MemoryLogger _INSTANCE = new MemoryLogger();
    private static final Logger _LOGGER = Logger
        .getInstance(MemoryLogger.class);

    private final Deque<ElapsedTime> _activations = new LinkedList<>();
    private ElapsedTime _elapsedTime;
    private DateTime _lastMidnight;
    private boolean _reset;
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
