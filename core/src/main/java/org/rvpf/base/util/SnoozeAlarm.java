/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SnoozeAlarm.java 4079 2019-06-12 13:40:21Z SFB $
 */

package org.rvpf.base.util;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;

/**
 * Snooze alarm.
 *
 * <p>Allows sleepy threads to take a nap until either a specified amount of
 * time has elapsed or something happened that required everybody to wake
 * up.</p>
 *
 * <p>Each instance can control any number of threads. A {@link #wakeUp} call
 * will affect all the threads snoozing on the same instance.</p>
 */
@ThreadSafe
public final class SnoozeAlarm
{
    /**
     * Constructs an instance.
     */
    public SnoozeAlarm()
    {
        this(Optional.empty());
    }

    /**
     * Constructs an instance.
     *
     * <p>This constructor allows the specification of a synchronization object
     * (defaults to the created instance) to solve some deadlock problems.</p>
     *
     * @param sync A sync object (when null, will be this).
     */
    public SnoozeAlarm(@Nonnull final Optional<Object> sync)
    {
        _sync = sync.orElse(this);
    }

    /**
     * Validates a snooze time.
     *
     * @param time The snooze time.
     * @param caller The caller.
     * @param nameEntry The entry for an identifying name for the error message.
     *
     * @return True if the snooze time is valid.
     */
    @CheckReturnValue
    public static boolean validate(
            @Nonnull final ElapsedTime time,
            @Nonnull final Object caller,
            @Nonnull final Messages.Entry nameEntry)
    {
        Require.notNull(caller);
        Require.notNull(nameEntry);

        if (time.compareTo(_getMinimumSnoozeTime()) < 0) {
            Logger
                .getInstance(caller.getClass())
                .error(
                    BaseMessages.SNOOZE_TIME_LOW,
                    nameEntry,
                    time,
                    _getMinimumSnoozeTime());

            return false;
        }

        return true;
    }

    /**
     * Closes.
     *
     * <p>Also wakes up the snoozers.</p>
     */
    public void close()
    {
        synchronized (_sync) {
            if (!_closed) {
                _closed = true;
                wakeUp();
            }
        }
    }

    /**
     * Snoozes until awoken.
     *
     * <p>Caution: calling this method while holding a lock other than the sync
     * object supplied to the constructor might cause a stall.</p>
     *
     * @throws InterruptedException When interrupted.
     */
    public void snooze()
        throws InterruptedException
    {
        Require.ignored(snooze(Optional.of(DateTime.END_OF_TIME)));
    }

    /**
     * Snoozes for the specified time.
     *
     * <p>Caution: calling this method while holding a lock other than the sync
     * object supplied to the constructor might cause a stall.</p>
     *
     * @param snoozeTime The snooze time.
     *
     * @return True when snooze time completed.
     *
     * @throws InterruptedException When interrupted.
     */
    @CheckReturnValue
    public boolean snooze(
            @Nonnull final ElapsedTime snoozeTime)
        throws InterruptedException
    {
        if (snoozeTime.toMillis() < _getMinimumSnoozeMillis()) {
            throw new IllegalArgumentException(
                Message.format(
                    BaseMessages.SNOOZE_TIME_LOW,
                    "?",
                    ElapsedTime.fromMillis(snoozeTime.toMillis()),
                    _getMinimumSnoozeTime()));
        }

        final Optional<DateTime> wakeTime;

        final DateTime now =    // Protects against simulated time.
            DateTime.fromMillis(System.currentTimeMillis());

        wakeTime = Optional.of(now.after(snoozeTime));

        return snooze(wakeTime);
    }

    /**
     * Snoozes until the specified time.
     *
     * <p>Caution: calling this method while holding a lock other than the sync
     * object supplied to the constructor may cause a stall.</p>
     *
     * @param wakeTime The wake time (empty for infinite).
     *
     * @return True when snooze time completed.
     *
     * @throws InterruptedException When interrupted.
     */
    @CheckReturnValue
    public boolean snooze(
            @Nonnull final Optional<DateTime> wakeTime)
        throws InterruptedException
    {
        final long wakeMillis = wakeTime
            .orElse(DateTime.END_OF_TIME)
            .toMillis();

        synchronized (_sync) {
            if (!_closed) {
                final int snooze = _snooze;    // Keeps a copy of the identifier.

                do {
                    final long snoozeMillis = wakeMillis
                        - System.currentTimeMillis();

                    if (snoozeMillis <= 0) {
                        return true;
                    }

                    _sync.wait(snoozeMillis);
                } while (_snooze == snooze);    // Unless 'wakeUp' called.
            }

            if (_closed) {
                throw new InterruptedException();
            }
        }

        return false;
    }

    /**
     * Wakes up all snoozers.
     */
    public void wakeUp()
    {
        synchronized (_sync) {
            ++_snooze;    // Bumps the snooze identifier.
            _sync.notifyAll();
        }
    }

    private static long _getMinimumSnoozeMillis()
    {
        if (_minimumSnoozeMillis <= 0) {
            _minimumSnoozeMillis = Long
                .parseLong(
                    System
                        .getProperty(
                                MINIMUM_SNOOZE_MILLIS_PROPERTY,
                                        DEFAULT_MINIMUM_SNOOZE_MILLIS));
        }

        return _minimumSnoozeMillis;
    }

    private static ElapsedTime _getMinimumSnoozeTime()
    {
        if (_minimumSnoozeTime == null) {
            _minimumSnoozeTime = ElapsedTime
                .fromMillis(_getMinimumSnoozeMillis());
        }

        return _minimumSnoozeTime;
    }

    /** Minimum snooze time in milliseconds. */
    public static final String DEFAULT_MINIMUM_SNOOZE_MILLIS = "1000";

    /** Minimum snooze millis system property. */
    public static final String MINIMUM_SNOOZE_MILLIS_PROPERTY =
        "rvpf.snooze.minimum.millis";

    /**  */

    private static volatile long _minimumSnoozeMillis;
    private static volatile ElapsedTime _minimumSnoozeTime;

    @GuardedBy("_sync")
    private boolean _closed;
    @GuardedBy("_sync")
    private int _snooze;
    private final Object _sync;
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
