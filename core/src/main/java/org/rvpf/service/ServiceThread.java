/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceThread.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.service;

import java.io.InterruptedIOException;

import java.nio.channels.ClosedByInterruptException;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;

/**
 * Service thread.
 */
public class ServiceThread
    extends Thread
{
    /**
     * Constructs an instance.
     *
     * @param name The thread name.
     */
    public ServiceThread(@Nonnull final String name)
    {
        super(name);

        _targets = new LinkedBlockingQueue<>();
        _target = this::_executeTargets;
    }

    /**
     * Constructs an instance.
     *
     * @param target The service thread target.
     * @param name The thread name.
     */
    public ServiceThread(
            @Nonnull final Target target,
            @Nonnull final String name)
    {
        super(name);

        _targets = null;
        _target = target;
    }

    /**
     * Called by {@link #run()} to indicate failure.
     */
    public static void failed()
    {
        ((ServiceThread) Thread.currentThread())._failed();
    }

    /**
     * Called to allow a waiting {@link #start(boolean)} to complete.
     */
    public static void ready()
    {
        ((ServiceThread) Thread.currentThread())._ready(true);
    }

    /**
     * Yields control to other threads, including those of lower priority.
     */
    public static void yieldAll()
    {
        final Thread currentThread = Thread.currentThread();
        final int priority = currentThread.getPriority();

        currentThread.setPriority(Thread.MIN_PRIORITY + 1);
        Thread.yield();
        currentThread.setPriority(priority);
    }

    /**
     * Exeecutes a target.
     *
     * @param target The target.
     */
    public void execute(@Nonnull final Target target)
    {
        if (_targets == null) {
            throw new UnsupportedOperationException();
        }

        _targets.add(target);
    }

    /**
     * Gets the throwable.
     *
     * @return The throwable (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Throwable> getThrowable()
    {
        return Optional.ofNullable(_throwable);
    }

    /**
     * Asks if this thread has failed.
     *
     * @return True if this thread has failed.
     */
    @CheckReturnValue
    public boolean hasFailed()
    {
        return _failed;
    }

    /**
     * Interrupts this thread and waits for its death.
     *
     * @param logger A logger.
     * @param joinTimeout The join timeout in millis (0 means infinite).
     *
     * @return False on interrupt or timeout (the thread may still be alive).
     */
    @CheckReturnValue
    public boolean interruptAndJoin(
            @Nonnull final Logger logger,
            final long joinTimeout)
    {
        logger.debug(ServiceMessages.THREAD_INTERRUPT, getName());
        interrupt();

        return join(logger, joinTimeout);
    }

    /**
     * Waits for the death of this thread.
     *
     * @param logger A logger.
     * @param timeout The timeout in millis (0 means infinite).
     *
     * @return False on interrupt or timeout (the thread may still be alive).
     */
    @CheckReturnValue
    public boolean join(@Nonnull final Logger logger, final long timeout)
    {
        logger.debug(ServiceMessages.THREAD_JOIN, getName());

        try {
            join((timeout >= 0)? timeout: 0);

            if (isAlive()) {
                logger.warn(ServiceMessages.THREAD_JOIN_TIMEOUT, getName());
            } else {
                logger.debug(ServiceMessages.THREAD_JOINED, getName());
            }
        } catch (final InterruptedException exception) {
            interrupt();
            logger.warn(ServiceMessages.THREAD_JOIN_INTERRUPTED, getName());
        }

        return !isAlive();
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        Logger.setLogID(Optional.ofNullable(_logID));

        if (!_quiet) {
            _getLogger()
                .debug(
                    ServiceMessages.THREAD_STARTED,
                    Thread.currentThread().getName());
        }

        boolean terminated = true;

        try {
            try {
                _target.run();
                terminated = false;
            } catch (final InterruptedException exception) {
                terminated = false;
            } catch (final InterruptedIOException exception) {
                terminated = false;
            } catch (final ClosedByInterruptException exception) {
                terminated = false;
            } catch (final RuntimeException exception) {
                terminated = _terminated(exception);

                if (terminated) {
                    _throwable = exception;

                    throw exception;
                }
            } catch (final ServiceNotAvailableException exception) {
                terminated = _terminated(exception);

                if (terminated) {
                    final String message = exception.getMessage();

                    if (message == null) {
                        _throwable = exception;

                        throw new RuntimeException(exception);
                    }

                    _getLogger()
                        .warn(
                            exception.getCause(),
                            BaseMessages.VERBATIM,
                            message);

                    throw new SilentException();
                }
            } catch (final Exception exception) {
                _throwable = exception;

                throw new RuntimeException(exception);
            } finally {
                if (terminated && !_quiet) {
                    if (_throwable != null) {
                        _getLogger()
                            .warn(
                                _throwable,
                                ServiceMessages.THREAD_TERMINATED,
                                Thread.currentThread().getName());
                    } else {
                        _getLogger()
                            .warn(
                                ServiceMessages.THREAD_TERMINATED,
                                Thread.currentThread().getName());
                    }
                }
            }
        } catch (final Throwable throwable) {
            _throwable = throwable;
            _failed();
            _ready(false);

            throw throwable;
        }

        _ready(false);

        if (!_quiet) {
            _getLogger()
                .debug(
                    ServiceMessages.THREAD_STOPPED,
                    Thread.currentThread().getName());
        }
    }

    /**
     * Sets the logger.
     *
     * @param logger The logger.
     */
    public void setLogger(@Nonnull final Logger logger)
    {
        Require.success(getState() == State.NEW);

        _logger = Require.notNull(logger);
    }

    /**
     * Sets the quiet indicator.
     *
     * @param quiet The quiet indicator.
     */
    public void setQuiet(final boolean quiet)
    {
        _quiet = quiet;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        Require.ignored(start(false));
    }

    /**
     * Starts this thread.
     *
     * @param wait True waits for a call by {@link #run()} to {@link #ready()}.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean start(final boolean wait)
    {
        synchronized (this) {
            _logID = Logger.currentLogID().orElse(null);

            setUncaughtExceptionHandler(
                Thread.currentThread().getUncaughtExceptionHandler());

            if (!(Thread.currentThread().getContextClassLoader()
                    instanceof ServiceClassLoader)) {
                _getLogger()
                    .debug(ServiceMessages.NO_SERVICE_CLASS_LOADER, getName());
            }

            super.start();
        }

        if (wait) {
            Require.ignored(waitForReady());
        }

        return !hasFailed();
    }

    /**
     * Waits for a call to {@link #ready()}.
     *
     * @return True unless {@link #hasFailed()} or interrupted.
     */
    @CheckReturnValue
    public boolean waitForReady()
    {
        try {
            _startLatch.await();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();

            return false;
        }

        return !hasFailed();
    }

    private static boolean _terminated(final Exception exception)
    {
        Throwable cause = exception.getCause();

        for (;;) {
            if (cause == null) {
                return true;
            }

            if ((cause instanceof InterruptedException)
                    || (cause instanceof InterruptedIOException)
                    || (cause instanceof ClosedByInterruptException)) {
                break;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private void _executeTargets()
        throws Exception
    {
        for (;;) {
            _targets.take().run();
        }
    }

    private void _failed()
    {
        _failed = true;
    }

    private Logger _getLogger()
    {
        Logger logger = _logger;

        if (logger == null) {
            logger = Logger.getInstance(_target.getClass());
            _logger = logger;
        }

        return logger;
    }

    private void _ready(final boolean log)
    {
        if (_startLatch.getCount() > 0) {
            if (log && !hasFailed()) {
                _getLogger()
                    .debug(
                        ServiceMessages.THREAD_READY,
                        Thread.currentThread().getName());
            }

            _startLatch.countDown();
        }
    }

    private volatile boolean _failed;
    private String _logID;
    private volatile Logger _logger;
    private boolean _quiet;
    private final CountDownLatch _startLatch = new CountDownLatch(1);
    private final Target _target;
    private final BlockingQueue<Target> _targets;
    private Throwable _throwable;

    /**
     * Target.
     */
    public interface Target
    {
        /**
         * Runs.
         *
         * <p>Similar to same in Runnable but may throw exceptions.</p>
         *
         * @throws Exception Catched by the starting thread.
         */
        void run()
            throws Exception;
    }


    /**
     * Silent exception.
     */
    public static class SilentException
        extends RuntimeException
    {
        /**
         * Constructs an instance.
         */
        public SilentException() {}

        private static final long serialVersionUID = 1L;
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
