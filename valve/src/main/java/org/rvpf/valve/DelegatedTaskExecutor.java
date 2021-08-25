/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DelegatedTaskExecutor.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.valve;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import javax.net.ssl.SSLEngine;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;

/**
 * Delegated task executor.
 */
@ThreadSafe
public final class DelegatedTaskExecutor
    implements Runnable
{
    private DelegatedTaskExecutor(
            @Nonnull final SSLEngine sslEngine,
            @Nonnull final Connection.Direction direction,
            final int inputInterests,
            final int outputInterests)
    {
        _sslEngine = sslEngine;
        _direction = direction;
        _inputInterests = inputInterests;
        _outputInterests = outputInterests;
    }

    /**
     * Delegates tasks.
     *
     * @param sslEngine The SSL engine.
     * @param direction The connection direction.
     */
    public static void delegateTasks(
            @Nonnull final SSLEngine sslEngine,
            @Nonnull final Connection.Direction direction)
    {
        final int inputInterests;
        final int outputInterests;

        try {
            inputInterests = direction.getInputKey().interestOps();
            outputInterests = direction.getOutputKey().interestOps();
            direction.getInputKey().interestOps(0);
            direction.getOutputKey().interestOps(0);
        } catch (final CancelledKeyException exception) {
            return;
        }

        _LOGGER.trace(
            () -> new Message(
                ValveMessages.SSL_DELEGATING,
                ((SocketChannel) direction.getOutputKey().channel()).socket()));

        new DelegatedTaskExecutor(
            sslEngine,
            direction,
            inputInterests,
            outputInterests).run();
    }

    /**
     * Shuts down.
     */
    public static void shutdown()
    {
        synchronized (DelegatedTaskExecutor.class) {
            if (_executorService != null) {
                _executorService.shutdown();
                _executorService = null;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        try {
            if (_delegated()) {
                _executorService().submit(this);
            } else {
                _finish();
            }
        } catch (final Throwable throwable) {
            _LOGGER.warn(
                throwable,
                ValveMessages.SSL_DELEGATED_FAILED,
                ((SocketChannel) _direction.getOutputKey().channel()).socket());
        }
    }

    private static ExecutorService _executorService()
    {
        synchronized (DelegatedTaskExecutor.class) {
            if (_executorService == null) {
                _executorService = Executors.newSingleThreadExecutor();
            }

            return _executorService;
        }
    }

    private boolean _delegated()
    {
        boolean delegated = false;

        synchronized (_direction.getConnection()) {
            for (;;) {
                final Runnable delegatedTask = _sslEngine.getDelegatedTask();

                if (delegatedTask == null) {
                    break;
                }

                _submitted.add(_executorService().submit(delegatedTask));
                delegated = true;
            }
        }

        return delegated;
    }

    private void _finish()
    {
        final int submitted = _submitted.size();

        // Logs possible exceptions from delegated tasks.
        for (;;) {
            final Future<?> task = _submitted.poll();

            if (task == null) {
                break;
            }

            try {
                task.get();
            } catch (final InterruptedException exception) {
                throw new Error(exception);    // Should not happen.
            } catch (final ExecutionException exception) {
                _LOGGER.warn(
                    exception,
                    ValveMessages.SSL_DELEGATED_FAILED,
                    ((SocketChannel) _direction.getOutputKey().channel())
                        .socket());
            }
        }

        _LOGGER.trace(
            () -> new Message(
                ValveMessages.SSL_DELEGATED_COMPLETED,
                String.valueOf(submitted),
                ((SocketChannel) _direction.getOutputKey().channel())
                    .socket()));

        final Connection.Direction direction = _direction;
        final int inputInterests = _inputInterests;
        final int outputInterests = _outputInterests;

        // Resumes operations on the connection.
        direction.getConnection().getConnectionsManager().wakeUp(
            () -> {
                synchronized (direction.getConnection()) {
                    try {
                        direction.getInputKey().interestOps(inputInterests);
                        direction.getOutputKey().interestOps(
                            outputInterests | SelectionKey.OP_READ
                            | SelectionKey.OP_WRITE);
                    } catch (final CancelledKeyException exception) {
                        // Ignores: the direction has been closed.
                    }
                }
            });
    }

    private static final Logger _LOGGER = Logger.getInstance(
        DelegatedTaskExecutor.class);
    private static ExecutorService _executorService;

    private final Connection.Direction _direction;
    private final int _inputInterests;
    private final int _outputInterests;
    private final SSLEngine _sslEngine;
    private final Queue<Future<?>> _submitted =
        new ConcurrentLinkedQueue<Future<?>>();
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
