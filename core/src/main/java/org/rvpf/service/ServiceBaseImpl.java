/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceBaseImpl.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.StatsOwner;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.util.Version;

/**
 * Service base implementation.
 */
public abstract class ServiceBaseImpl
    implements ServiceBase, Runnable
{
    /** {@inheritDoc}
     */
    @Override
    public final void addStats(final ServiceStats serviceStats)
    {
        serviceStats.setLogEnabled(getStats().isLogEnabled());

        final List<ServiceStats> stats = _stats.get();

        if (stats != null) {
            stats.add(serviceStats);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void fail()
    {
        synchronized (_mutex) {
            if (!_terminating) {
                _terminating = true;
                stopTimer();
                _serviceActivatorBase.terminate();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getServiceName()
    {
        return Require.notNull(_serviceName);
    }

    /** {@inheritDoc}
     */
    @Override
    public ServiceStats getStats()
    {
        List<ServiceStats> stats = _stats.get();

        while (stats == null) {
            final ServiceStats serviceStats = createStats(
                getServiceActivatorBase());

            stats = new LinkedList<>();
            stats.add(serviceStats);

            if (_stats.compareAndSet(null, stats)) {
                break;
            }

            stats = _stats.get();
        }

        return stats.get(0);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<Timer> getTimer()
    {
        return _timer;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isJMXRegistrationEnabled()
    {
        return false;
    }

    /**
     * Sets the service name.
     *
     * @param serviceName The service name.
     */
    public final void setServiceName(@Nonnull final String serviceName)
    {
        _serviceName = Require.notNull(serviceName);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void snooze(
            final ElapsedTime snoozeTime)
        throws InterruptedException
    {
        final SnoozeAlarm snoozeAlarm = _snoozeAlarm;

        if (snoozeAlarm == null) {
            throw new InterruptedException();

        }

        Require.ignored(snoozeAlarm.snooze(snoozeTime));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void starting(final Optional<ElapsedTime> waitHint)
    {
        _serviceActivatorBase.starting(waitHint);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void stopping(final Optional<ElapsedTime> waitHint)
    {
        _serviceActivatorBase.stopping(waitHint);
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getServiceName();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void uncaughtException(
            final Thread thread,
            final Throwable throwable)
    {
        if (!(throwable instanceof ServiceThread.SilentException)) {
            getThisLogger().uncaughtException(thread, throwable);
        }

        fail();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void wakeUp()
    {
        final SnoozeAlarm snoozeAlarm = _snoozeAlarm;

        if (snoozeAlarm != null) {
            snoozeAlarm.wakeUp();

        }
    }

    /**
     * Closes the snooze alarm.
     */
    protected final void closeSnoozeAlarm()
    {
        final SnoozeAlarm snoozeAlarm = _snoozeAlarm;

        if (snoozeAlarm != null) {
            snoozeAlarm.close();
        }
    }

    /**
     * Creates a stats instance.
     *
     * @param statsOwner The stats owner.
     *
     * @return The stats instance.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract ServiceStats createStats(@Nonnull StatsOwner statsOwner);

    /**
     * Gets the log ID.
     *
     * @return The optional log ID.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<String> getLogID()
    {
        return Optional.ofNullable(_logID);
    }

    /**
     * Gets the service activator base owning this.
     *
     * @return The service base.
     */
    @Nonnull
    @CheckReturnValue
    protected final ServiceActivatorBase getServiceActivatorBase()
    {
        return Require.notNull(_serviceActivatorBase);
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        Logger logger = _logger;

        if (logger == null) {
            logger = Logger.getInstance(getClass());
            _logger = logger;
        }

        return logger;
    }

    /**
     * Gets the thread.
     *
     * @return The thread.
     */
    @Nonnull
    @CheckReturnValue
    protected Thread getThread()
    {
        return _thread.get();
    }

    /**
     * Gets the version object.
     *
     * @return The version object.
     */
    @Nonnull
    @CheckReturnValue
    protected Version getVersion()
    {
        return _serviceActivatorBase.getVersion();
    }

    /**
     * Interrupts the thread.
     */
    protected final void interrupt()
    {
        final Thread thread = _thread.get();

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.THREAD_INTERRUPT, thread.getName());
            thread.interrupt();
        }
    }

    /**
     * Asks if the service implementation thread is the current thread.
     *
     * @return True if it is the current thread.
     */
    @CheckReturnValue
    protected final boolean isCurrentThread()
    {
        return _thread.get() == Thread.currentThread();
    }

    /**
     * Asks if the thread has been started.
     *
     * @return True when started.
     */
    @CheckReturnValue
    protected final boolean isThreadStarted()
    {
        return _thread.get() != null;
    }

    /**
     * Waits for the thread to die.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected final boolean join()
    {
        final Thread thread = _thread.get();

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.THREAD_JOIN, thread.getName());

            try {
                thread.join();

                if (thread.isAlive()) {
                    getThisLogger()
                        .warn(
                            ServiceMessages.THREAD_JOIN_TIMEOUT,
                            thread.getName());
                } else {
                    getThisLogger()
                        .debug(ServiceMessages.THREAD_JOINED, thread.getName());
                }
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
                getThisLogger()
                    .warn(
                        ServiceMessages.THREAD_JOIN_INTERRUPTED,
                        thread.getName());
            }

            return !thread.isAlive();
        }

        return true;
    }

    /**
     * Logs the stats.
     *
     * @param intermediate True means a final log.
     */
    protected final void logStats(final boolean intermediate)
    {
        final List<ServiceStats> stats = _stats.get();

        if (stats != null) {
            synchronized (_mutex) {
                for (final ServiceStats serviceStats: stats) {
                    serviceStats.log(intermediate);
                }
            }
        }
    }

    /**
     * Schedules the midnight logger.
     *
     * <p>This is used by service threads which are not set up to receive
     * midnight events but still need to switch log files.</p>
     */
    protected final void scheduleMidnightLogger()
    {
        final Optional<Timer> timer = getTimer();

        if (timer.isPresent()) {
            final TimerTask timerTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    synchronized (_getMutex()) {
                        if (getTimer().isPresent()) {
                            Logger
                                .getMidnightLogger()
                                .info(ServiceMessages.MIDNIGHT);
                            logStats(true);
                            scheduleMidnightLogger();
                        }
                    }
                }
            };
            final DateTime midnight = DateTime.now().nextDay();

            try {
                timer.get().schedule(timerTask, midnight.toTimestamp());
            } catch (final IllegalStateException exception) {
                getThisLogger().debug(ServiceMessages.MIDNIGHT_LOG_CANCELED);

                return;
            }

            getThisLogger()
                .debug(ServiceMessages.MIDNIGHT_LOG_SCHEDULED, midnight);
        } else {
            getThisLogger().debug(ServiceMessages.MIDNIGHT_LOG_CANCELED);
        }
    }

    /**
     * Sets the log ID.
     *
     * @param logID The log ID.
     */
    protected final void setLogID(@Nonnull final Optional<String> logID)
    {
        Logger.setLogID(logID);
        _logID = Logger.currentLogID().orElse(null);
    }

    /**
     * Sets the service activator base.
     *
     * @param serviceActivatorBase The service activator base.
     */
    protected final void setServiceActivatorBase(
            @Nonnull final ServiceActivatorBase serviceActivatorBase)
    {
        Require.success(_serviceActivatorBase == null);

        _serviceActivatorBase = Require.notNull(serviceActivatorBase);
    }

    /**
     * Sets up the service thread.
     *
     * <p>Overriding subclasses must call this with success before
     * proceeding.</p>
     *
     * @return True on success.
     */
    @CheckReturnValue
    @OverridingMethodsMustInvokeSuper
    protected boolean setUp()
    {
        _snoozeAlarm = new SnoozeAlarm(Optional.of(this));
        Logger.setLogID(Optional.ofNullable(_logID));

        return true;
    }

    /**
     * Starts the thread.
     */
    protected final void startThread()
    {
        final Thread thread = new Thread(this, _serviceName);

        if (_thread.compareAndSet(null, thread)) {
            thread.setUncaughtExceptionHandler(this);
            _logID = Logger.currentLogID().orElse(null);
            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /**
     * Starts the timer.
     */
    protected final void startTimer()
    {
        synchronized (_mutex) {
            if (!_timer.isPresent()) {
                _timer = Optional.of(new Timer("Service thread timer"));
            }
        }
    }

    /**
     * Stops the timer.
     */
    protected final void stopTimer()
    {
        synchronized (_mutex) {
            final Optional<Timer> timer = _timer;

            _timer = Optional.empty();

            if (timer.isPresent()) {
                timer.get().cancel();
            }
        }
    }

    /**
     * Tears down what has been set up.
     */
    protected void tearDown()
    {
        _snoozeAlarm = null;
        stopTimer();
        _stats.set(null);

        System.gc();
    }

    /**
     * Allows a subclass to update the stats.
     */
    protected void updateStats() {}

    /**
     * Gets the mutex.
     *
     * @return The mutex.
     */
    @Nonnull
    @CheckReturnValue
    final Object _getMutex()
    {
        return _mutex;
    }

    private String _logID;
    private volatile Logger _logger;
    private final Object _mutex = new Object();
    private volatile ServiceActivatorBase _serviceActivatorBase;
    private String _serviceName;
    private volatile SnoozeAlarm _snoozeAlarm;
    private final AtomicReference<List<ServiceStats>> _stats =
        new AtomicReference<>();
    private boolean _terminating;
    private final AtomicReference<Thread> _thread = new AtomicReference<>();
    private volatile Optional<Timer> _timer = Optional.empty();
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
