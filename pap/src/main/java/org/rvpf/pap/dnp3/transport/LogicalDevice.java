/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LogicalDevice.java 4082 2019-06-14 18:38:55Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Logical device.
 */
public final class LogicalDevice
{
    /**
     * Constructs an instance.
     *
     * @param name The logical device name.
     * @param address The logical device address.
     */
    public LogicalDevice(
            @Nonnull final String name,
            @Nonnull final Short address)
    {
        _name = name;
        _address = address;
    }

    /**
     * Gets the address.
     *
     * @return The address.
     */
    @Nonnull
    @CheckReturnValue
    public Short getAddress()
    {
        return _address;
    }

    /**
     * Gets the name.
     *
     * @return The name.
     */
    @Nonnull
    @CheckReturnValue
    public String getName()
    {
        return _name;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return _name + ':' + Integer.toHexString(
            _address.shortValue() & 0xFFFF);
    }

    /**
     * Activates.
     *
     * @param connectionManager The connection manager.
     */
    synchronized void activate(
            @Nonnull final ConnectionManager connectionManager)
    {
        if (_manager == null) {
            _manager = new _ServerManager(connectionManager);
        }
    }

    /**
     * Deactivates.
     */
    synchronized void deactivate()
    {
        if (_manager != null) {
            _manager.close();
            _manager = null;
        }
    }

    /**
     * Called when a frame is received.
     *
     * @param remoteEndPoint The sender of the frame.
     * @param remoteAddress The DNP3 address of the sender.
     *
     * @return True if the frame should be accepted.
     *
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    boolean onFrameReceived(
            @Nonnull final RemoteEndPoint remoteEndPoint,
            final short remoteAddress)
        throws IOException
    {
        return (_manager != null)? _manager
            .onFrameReceived(remoteEndPoint, remoteAddress): false;
    }

    private final Short _address;
    private _ServerManager _manager;
    private final String _name;

    /**
     * Server key.
     */
    private static class _ServerKey
    {
        /**
         * Constructs an instance.
         *
         * @param remoteEndPoint The sender of the frame.
         * @param remoteAddress The DNP3 address of the sender.
         */
        _ServerKey(
                final RemoteEndPoint remoteEndPoint,
                final short remoteAddress)
        {
            _remoteEndPoint = remoteEndPoint;
            _remoteAddress = remoteAddress;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object)
        {
            if (object == this) {
                return true;
            }

            if (object instanceof _ServerKey) {
                final _ServerKey other = (_ServerKey) object;

                return (_remoteAddress == other._remoteAddress)
                       && _remoteEndPoint.equals(other._remoteEndPoint);
            }

            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            return _remoteEndPoint.hashCode() ^ Short.hashCode(_remoteAddress);
        }

        private final short _remoteAddress;
        private final RemoteEndPoint _remoteEndPoint;
    }


    /**
     * Manager.
     */
    private class _ServerManager
    {
        /**
         * Constructs an instance.
         *
         * @param connectionManager The connection manager.
         */
        _ServerManager(@Nonnull final ConnectionManager connectionManager)
        {
            _connectionManager = connectionManager;
        }

        /**
         * Gets the connection manager.
         *
         * @return The connection manager.
         */
        @Nonnull
        @CheckReturnValue
        public ConnectionManager getConnectionManager()
        {
            return _connectionManager;
        }

        /**
         * Closes.
         */
        void close()
        {
            final Iterator<_Server> serverIterator = _servers
                .values()
                .iterator();

            while (serverIterator.hasNext()) {
                serverIterator.next().stop();
                serverIterator.remove();
            }
        }

        /**
         * Called when a frame is received.
         *
         * @param remoteEndPoint The sender of the frame.
         * @param remoteAddress The DNP3 address of the sender.
         *
         * @return True if the frame should be accepted.
         *
         * @throws IOException On I/O exception.
         */
        @CheckReturnValue
        boolean onFrameReceived(
                final RemoteEndPoint remoteEndPoint,
                final short remoteAddress)
            throws IOException
        {
            final _ServerKey serverKey = new _ServerKey(
                remoteEndPoint,
                remoteAddress);

            if (!_servers.containsKey(serverKey)) {
                final _Server server = new _Server(
                    remoteEndPoint,
                    remoteAddress);

                _servers.put(serverKey, server);
                server.start();

                _connectionManager.onNewAssociation(server.getAssociation());
            }

            return true;
        }

        private final ConnectionManager _connectionManager;
        private final Map<_ServerKey, _Server> _servers =
            new ConcurrentHashMap<>();

        /**
         * Server.
         */
        private class _Server
            implements ServiceThread.Target
        {
            /**
             * Constructs an instance.
             *
             * @param remoteEndPoint The sender of the frame.
             * @param remoteAddress The DNP3 address of the sender.
             */
            _Server(
                    @Nonnull final RemoteEndPoint remoteEndPoint,
                    final short remoteAddress)
            {
                _association = remoteEndPoint
                    .getAssociation(getAddress().shortValue(), remoteAddress);

                _thread
                    .set(
                        new ServiceThread(
                            this,
                            "Server thread for '"
                            + LogicalDevice.this.toString() + "' < "
                            + remoteEndPoint.getRemoteProxy() + " ("
                            + Integer.toHexString(
                                    remoteAddress & 0xFFFF) + ")"));
            }

            /** {@inheritDoc}
             */
            @Override
            public void run()
                throws Exception
            {
                getConnectionManager().receiveMessages(_association);
            }

            /**
             * Gets the association.
             *
             * @return The association.
             */
            @Nonnull
            @CheckReturnValue
            Association getAssociation()
            {
                return _association;
            }

            /**
             * Starts.
             */
            void start()
            {
                final Thread thread = _thread.get();

                if (thread != null) {
                    thread.start();
                }
            }

            /**
             * Stops.
             */
            void stop()
            {
                final ServiceThread thread = _thread.getAndSet(null);

                if (thread != null) {
                    final Logger logger = Logger.getInstance(getClass());

                    logger
                        .debug(
                            ServiceMessages.STOPPING_THREAD,
                            thread.getName());
                    Require.ignored(thread.interruptAndJoin(logger, 0));
                }
            }

            private final Association _association;
            private final AtomicReference<ServiceThread> _thread =
                new AtomicReference<>();
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
