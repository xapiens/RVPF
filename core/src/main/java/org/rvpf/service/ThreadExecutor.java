/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ThreadExecutor.java 4102 2019-06-30 15:41:17Z SFB $
 */

package org.rvpf.service;

import java.lang.Thread.UncaughtExceptionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;

/**
 * Thread executor.
 */
@ThreadSafe
public class ThreadExecutor
    implements ExecutorService, Runnable
{
    /**
     * Constructs an instance.
     *
     * @param logger An optional logger.
     */
    public ThreadExecutor(@Nonnull final Optional<Logger> logger)
    {
        _logger = logger.orElse(Logger.getInstance(getClass()));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean awaitTermination(
            final long timeout,
            final TimeUnit unit)
        throws InterruptedException
    {
        long nanos = unit.toNanos(timeout);

        _mainLock.lock();

        try {
            if (_runState < _RUNNING_STATE) {
                throw new IllegalStateException();
            }

            for (;;) {
                if (_runState == _TERMINATED_STATE) {
                    return true;
                }

                if (nanos <= 0) {
                    return false;
                }

                nanos = _termination.awaitNanos(nanos);
            }
        } finally {
            _mainLock.unlock();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void execute(final Runnable runnable)
    {
        Require.notNull(runnable);

        _mainLock.lock();

        try {
            if ((_runThread == null) || (_runState != _RUNNING_STATE)) {
                throw new RejectedExecutionException();
            }

            if (!_runThread.isAlive()) {
                _logID = Logger.currentLogID().orElse(null);
                _runThread.start();
            }

            try {
                _workQueue.put(runnable);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();

                throw new RejectedExecutionException();
            }
        } finally {
            _mainLock.unlock();
        }
    }

    /**
     * Invokes a callable from inside the thread.
     *
     * @param <T> The type returned by the callable.
     * @param callable The callable.
     *
     * @return The value returned by the callable.
     *
     * @throws InterruptedException When the invoke is interrupted.
     * @throws ExecutionException When the callable throws something.
     */
    @CheckReturnValue
    public <T> T invoke(
            @Nonnull final Callable<T> callable)
        throws InterruptedException, ExecutionException
    {
        final Future<T> future = submit(callable);

        return future.get();
    }

    /**
     * Invokes a callable from inside the thread.
     *
     * @param <T> The type returned by the callable.
     * @param callable The callable.
     * @param timeout The maximum time to wait.
     * @param unit The time ounit of the timeout.
     *
     * @return The value returned by the callable.
     *
     * @throws InterruptedException When the invoke is interrupted.
     * @throws ExecutionException When the callable throws something.
     * @throws TimeoutException If the timeout elapses.
     */
    @CheckReturnValue
    public <T> T invoke(
            final Callable<T> callable,
            final long timeout,
            final TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        final Future<T> future = submit(callable);

        return future.get(timeout, unit);
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(
            final Collection<? extends Callable<T>> callables)
        throws InterruptedException
    {
        final List<Future<T>> futures = new ArrayList<Future<T>>(
            callables.size());

        for (final Callable<T> callable: callables) {
            futures.add(submit(callable));
        }

        boolean done = false;

        try {
            for (final Future<T> future: futures) {
                if (!future.isDone()) {
                    try {
                        future.get();
                    } catch (final CancellationException
                             |ExecutionException exception) {
                        // Ignores.
                    }
                }
            }

            done = true;
        } finally {
            if (!done) {
                for (final Future<T> future: futures) {
                    future.cancel(true);
                }
            }
        }

        return futures;
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(
            final Collection<? extends Callable<T>> callables,
            final long timeout,
            final TimeUnit unit)
        throws InterruptedException
    {
        final List<Future<T>> futures = new ArrayList<Future<T>>(
            callables.size());

        for (final Callable<T> callable: callables) {
            futures.add(submit(callable));
        }

        boolean done = false;
        long nanos = unit.toNanos(timeout);
        long lastTime = System.nanoTime();

        try {
            for (final Future<T> future: futures) {
                if (!future.isDone()) {
                    try {
                        future.get(nanos, TimeUnit.NANOSECONDS);
                    } catch (final CancellationException
                             |ExecutionException exception) {
                        // Ignores.
                    } catch (final TimeoutException exception) {
                        return futures;
                    }

                    final long now = System.nanoTime();

                    nanos -= now - lastTime;
                    lastTime = now;
                }
            }

            done = true;
        } finally {
            if (!done) {
                for (final Future<T> future: futures) {
                    future.cancel(true);
                }
            }
        }

        return futures;
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(
            final Collection<? extends Callable<T>> callables)
        throws InterruptedException, ExecutionException
    {
        ExecutionException lastException = null;

        for (final Callable<T> callable: callables) {
            try {
                return invoke(callable);
            } catch (final ExecutionException exception) {
                lastException = exception;
            } catch (final RuntimeException exception) {
                lastException = new ExecutionException(exception);
            }
        }

        throw (lastException != null)? lastException: new ExecutionException(
            null);
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(
            final Collection<? extends Callable<T>> callables,
            final long timeout,
            final TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        ExecutionException lastException = null;
        long nanos = unit.toNanos(timeout);
        long lastTime = System.nanoTime();

        for (final Callable<T> callable: callables) {
            try {
                return invoke(callable, nanos, TimeUnit.NANOSECONDS);
            } catch (final ExecutionException exception) {
                lastException = exception;
            }

            final long now = System.nanoTime();

            nanos -= now - lastTime;
            lastTime = now;
        }

        throw (lastException != null)? lastException: new ExecutionException(
            null);
    }

    /**
     * Asks if this executor is busy.
     *
     * @return True if busy.
     */
    @CheckReturnValue
    public boolean isBusy()
    {
        _mainLock.lock();

        try {
            if ((_runState > _READY_STATE) && (_runState < _STOPPING_STATE)) {
                if (_runLock.tryLock()) {
                    _runLock.unlock();

                    return false;
                }

                return true;
            }
        } finally {
            _mainLock.unlock();
        }

        return false;
    }

    /**
     * Asks if this executor is running.
     *
     * @return True if running.
     */
    @CheckReturnValue
    public boolean isRunning()
    {
        _mainLock.lock();

        try {
            return (_runThread != null) && (_runState == _RUNNING_STATE);
        } finally {
            _mainLock.unlock();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isShutdown()
    {
        return _runState > _RUNNING_STATE;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isTerminated()
    {
        return _runState == _TERMINATED_STATE;
    }

    /**
     * Resets.
     *
     * @param threadName The optional thread name.
     * @param daemonThread Sets the thread as daemon if true.
     */
    public void reset(
            @Nonnull final Optional<String> threadName,
            final boolean daemonThread)
    {
        _mainLock.lock();

        try {
            if (_runThread != null) {
                shutdownNow();
            }

            _runThread = threadName
                .isPresent()? new Thread(
                    this,
                    threadName.get()): new Thread(this);
            _runThread.setDaemon(daemonThread);
            _runThread
                .setUncaughtExceptionHandler(
                    Thread.currentThread().getUncaughtExceptionHandler());
            _runLock = new ReentrantLock();
            _runState = _READY_STATE;
        } finally {
            _mainLock.unlock();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        Logger.setLogID(Optional.ofNullable(_logID));
        _logger
            .debug(
                ServiceMessages.THREAD_STARTED,
                Thread.currentThread().getName());

        try {
            final Lock runLock;

            _mainLock.lock();

            try {
                if (_runThread == null) {
                    return;
                }

                runLock = _runLock;
            } finally {
                _mainLock.unlock();
            }

            for (;;) {
                final int state = _runState;

                if (state > _SHUTDOWN_STATE) {
                    break;
                }

                final Runnable runnable;

                try {
                    runnable = (state < _SHUTDOWN_STATE)? _workQueue
                        .take(): _workQueue.poll();
                } catch (final InterruptedException exception) {
                    continue;
                }

                if (runnable != null) {
                    runLock.lock();

                    try {
                        if ((_runState < _STOPPING_STATE)
                                && Thread.interrupted()) {
                            Thread.currentThread().interrupt();

                            break;
                        }

                        runnable.run();
                    } finally {
                        runLock.unlock();
                    }
                } else if (_runState > _RUNNING_STATE) {
                    break;
                }
            }
        } catch (final Throwable throwable) {
            final UncaughtExceptionHandler uncaughtExceptionHandler = Thread
                .currentThread()
                .getUncaughtExceptionHandler();

            if (uncaughtExceptionHandler
                    == Thread.currentThread().getThreadGroup()) {
                _logger
                    .error(
                        throwable,
                        ServiceMessages.UNEXPECTED_THREAD_EXCEPTION,
                        Thread.currentThread().getName());
            } else {
                uncaughtExceptionHandler
                    .uncaughtException(Thread.currentThread(), throwable);
            }
        } finally {
            _mainLock.lock();

            try {
                if (Thread.currentThread() == _runThread) {
                    _terminated();
                }
            } finally {
                _mainLock.unlock();
            }

            _logger
                .debug(
                    ServiceMessages.THREAD_STOPPED,
                    Thread.currentThread().getName());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        _mainLock.lock();

        try {
            if (_runState < _SHUTDOWN_STATE) {
                _runState = _SHUTDOWN_STATE;

                if (_runLock.tryLock()) {
                    try {
                        _interrupt();
                    } finally {
                        _runLock.unlock();
                    }
                }
            }
        } finally {
            _mainLock.unlock();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public List<Runnable> shutdownNow()
    {
        final List<Runnable> runnables = new LinkedList<Runnable>();

        _mainLock.lock();

        try {
            if (_runState < _STOPPING_STATE) {
                _runState = _STOPPING_STATE;

                _interrupt();
                _workQueue.drainTo(runnables);
            }
        } finally {
            _mainLock.unlock();
        }

        return runnables;
    }

    /**
     * Starts the thread.
     */
    public void startThread()
    {
        _logID = Logger.currentLogID().orElse(null);

        _mainLock.lock();

        try {
            if (_runState == _INITIAL_STATE) {
                reset(Optional.empty(), true);
            }

            _runThread.start();
            _runState = _RUNNING_STATE;
        } finally {
            _mainLock.unlock();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> Future<T> submit(final Callable<T> callable)
    {
        final RunnableFuture<T> runnableFuture = new FutureTask<>(callable);

        execute(runnableFuture);

        return runnableFuture;
    }

    /** {@inheritDoc}
     */
    @Override
    public Future<?> submit(final Runnable runnable)
    {
        final RunnableFuture<Object> runnableFuture = new FutureTask<>(
            runnable,
            null);

        execute(runnableFuture);

        return runnableFuture;
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> Future<T> submit(final Runnable runnable, final T result)
    {
        final RunnableFuture<T> runnableFuture = new FutureTask<>(
            runnable,
            result);

        execute(runnableFuture);

        return runnableFuture;
    }

    @GuardedBy("_mainLock")
    private void _interrupt()
    {
        if (_runThread != null) {
            if (_runThread.isAlive()) {
                _runThread.interrupt();
            } else {
                _terminated();
            }

            _runThread = null;
        }
    }

    @GuardedBy("_mainLock")
    private void _terminated()
    {
        _runState = _TERMINATED_STATE;
        _termination.signalAll();
    }

    private static final int _INITIAL_STATE = 0;
    private static final int _READY_STATE = 1;
    private static final int _RUNNING_STATE = 2;
    private static final int _SHUTDOWN_STATE = 3;
    private static final int _STOPPING_STATE = 4;
    private static final int _TERMINATED_STATE = 5;

    private String _logID;
    private final Logger _logger;
    private final Lock _mainLock = new ReentrantLock();
    private Lock _runLock = new ReentrantLock();
    private volatile int _runState = _INITIAL_STATE;
    private Thread _runThread;
    private final Condition _termination = _mainLock.newCondition();
    private final BlockingQueue<Runnable> _workQueue =
        new LinkedBlockingQueue<>();
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
