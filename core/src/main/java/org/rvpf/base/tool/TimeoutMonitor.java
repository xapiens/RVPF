/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TimeoutMonitor.java 4107 2019-07-13 13:18:26Z SFB $
 */

package org.rvpf.base.tool;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.util.container.IdentityHashSet;

/**
 * Timeout monitor.
 */
@ThreadSafe
public class TimeoutMonitor
    implements Runnable
{
    /**
     * Constructs an instance.
     *
     * @param timeout The elapsed time for a timeout.
     */
    public TimeoutMonitor(@Nonnull final ElapsedTime timeout)
    {
        _timeout = timeout;
    }

    /**
     * Shuts down the executor if nothing is scheduled.
     *
     * @return True if the shutdown has started.
     */
    public static synchronized boolean shutdown()
    {
        if (_executor != null) {
            if (_executor.getQueue().isEmpty()) {
                _executor.shutdown();
                _executor = null;
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Adds a client.
     *
     * @param client The client to add.
     */
    public synchronized void addClient(@Nonnull final Client client)
    {
        if (_clients == null) {
            Require.success(_client == null);

            _clients = new IdentityHashSet<Client>();
        }

        _clients.add(client);

        _activate();
    }

    /**
     * Clears the client.
     */
    public synchronized void clearClient()
    {
        Require.success(_clients == null);

        _deactivate();

        _client = null;
    }

    /**
     * Removes a client.
     *
     * @param client The client to remove.
     */
    public synchronized void removeClient(@Nonnull final Client client)
    {
        if (_clients != null) {
            _clients.remove(client);

            if (_clients.isEmpty()) {
                _deactivate();
            }
        } else {
            Require.success(_client == null);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void run()
    {
        if (_client != null) {
            _client.onTimeoutMonitoring();
        } else if (_clients != null) {
            for (final Client client: _clients.toArray(_EMPTY_CLIENT_ARRAY)) {
                client.onTimeoutMonitoring();    // Clients may remove themselves.
            }
        }
    }

    /**
     * Sets the client.
     *
     * @param client The client.
     */
    public synchronized void setClient(@Nonnull final Client client)
    {
        Require.success(_clients == null);

        _deactivate();

        _client = Require.notNull(client);

        _activate();
    }

    private static synchronized ScheduledExecutorService _getExecutor()
    {
        if (_executor == null) {
            final ScheduledThreadPoolExecutor executor =
                new ScheduledThreadPoolExecutor(
                    1);

            executor.setKeepAliveTime(1, TimeUnit.MINUTES);
            executor.allowCoreThreadTimeOut(true);
            executor.setRemoveOnCancelPolicy(true);
            _executor = executor;
        }

        return _executor;
    }

    private void _activate()
    {
        if (_future == null) {
            _future = _getExecutor()
                .schedule(this, _timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void _deactivate()
    {
        if (_future != null) {
            _future.cancel(false);
            _future = null;
        }
    }

    private static final Client[] _EMPTY_CLIENT_ARRAY = new Client[0];
    private static ScheduledThreadPoolExecutor _executor;

    private Client _client;
    private Set<Client> _clients;
    private ScheduledFuture<?> _future;
    private final ElapsedTime _timeout;

    /**
     * Client.
     */
    public interface Client
    {
        /**
         * Called to perform timeout monitoring.
         */
        void onTimeoutMonitoring();
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
