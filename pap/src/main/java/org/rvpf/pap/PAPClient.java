/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPClient.java 3979 2019-05-12 14:48:04Z SFB $
 */

package org.rvpf.pap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;

/**
 * PAP client.
 */
public interface PAPClient
{
    /**
     * Adds a connection listener.
     *
     * @param connectionListener The connection listener.
     *
     * @return True if added, false if already added.
     */
    boolean addConnectionListener(
            @Nonnull PAPConnectionListener connectionListener);

    /**
     * Closes this client.
     */
    void close();

    /**
     * Connects to a server.
     *
     * @param origin The origin representing the server.
     *
     * @return True on success.
     *
     * @throws InterruptedException When interrupted.
     */
    @CheckReturnValue
    boolean connect(@Nonnull Origin origin)
        throws InterruptedException;

    /**
     * Disconnects all open connections.
     */
    void disconnect();

    /**
     * Disconnects from a server.
     *
     * @param origin The origin representing the server.
     */
    void disconnect(@Nonnull Origin origin);

    /**
     * Fetches point values.
     *
     * @param points The points.
     *
     * @return The point values.
     *
     * @throws InterruptedException When interrupted.
     * @throws ServiceNotAvailableException On service not available.
     */
    @Nonnull
    @CheckReturnValue
    PointValue[] fetchPointValues(
            @Nonnull Point[] points)
        throws InterruptedException, ServiceNotAvailableException;

    /**
     * Gets an origin by its name.
     *
     * @param originName The origin name.
     *
     * @return The origin (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    Optional<Origin> getOrigin(@Nonnull Optional<String> originName);

    /**
     * Gets the proxy for a point.
     *
     * @param point The point.
     *
     * @return The proxy (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<? extends PAPProxy> getPointProxy(Point point);

    /**
     * Asks if a point is active.
     *
     * @param point The point.
     *
     * @return True if the point is active.
     */
    @CheckReturnValue
    boolean isPointActive(@Nonnull Point point);

    /**
     * Opens.
     */
    void open();

    /**
     * Removes a connection listener.
     *
     * @param connectionListener The connection listener.
     *
     * @return True if removed, false if already removed.
     */
    boolean removeConnectionListener(PAPConnectionListener connectionListener);

    /**
     * Updates point values.
     *
     * @param pointValues The point values.
     *
     * @return An exception array.
     *
     * @throws InterruptedException When interrupted.
     * @throws ServiceNotAvailableException On service not available.
     */
    @Nonnull
    @CheckReturnValue
    Exception[] updatePointValues(
            @Nonnull PointValue[] pointValues)
        throws InterruptedException, ServiceNotAvailableException;

    /**
     * Abstract PAP client.
     */
    abstract class Abstract
        implements PAPClient, PAPConnectionListener
    {
        /**
         * Constructs an instance.
         *
         * @param clientContext The client context.
         */
        protected Abstract(@Nonnull final PAPContext clientContext)
        {
            _context = clientContext;
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean addConnectionListener(
                final PAPConnectionListener connectionListener)
        {
            return _connectionListenerManager.addListener(connectionListener);
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _connectionListenerManager.clear();
        }

        /** {@inheritDoc}
         */
        @Override
        public final void disconnect()
        {
            synchronized (_knownServerProxies) {
                final Collection<PAPProxy> knownServerProxies = new ArrayList<>(
                    _knownServerProxies.values());

                for (final PAPProxy knownServerProxyroxy: knownServerProxies) {
                    disconnect(knownServerProxyroxy.getOrigin());
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final Optional<Origin> getOrigin(
                final Optional<String> originName)
        {
            return getContext().getRemoteOrigin(originName);
        }

        /** {@inheritDoc}
         */
        @Override
        public final Optional<? extends PAPProxy> getPointProxy(
                final Point point)
        {
            return getContext().getRemoteProxy(point);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPointActive(final Point point)
        {
            return getContext().isPointActive(point);
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean onLostConnection(
                final PAPProxy remoteProxy,
                final Optional<Exception> cause)
        {
            return _connectionListenerManager
                .onLostConnection(remoteProxy, cause);
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean onNewConnection(final PAPProxy remoteProxy)
        {
            return _connectionListenerManager.onNewConnection(remoteProxy);
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean removeConnectionListener(
                final PAPConnectionListener connectionListener)
        {
            return _connectionListenerManager
                .removeListener(connectionListener);
        }

        /**
         * Forgets a server proxy for an origin.
         *
         * @param origin The origin.
         *
         * @return The server proxy (may be empty).
         */
        @Nonnull
        protected final Optional<PAPProxy> forgetServerProxy(
                @Nonnull final Origin origin)
        {
            final PAPProxy knownServerProxy;

            synchronized (_knownServerProxies) {
                knownServerProxy = _knownServerProxies
                    .get(Require.notNull(origin));

                if (knownServerProxy != null) {
                    forgetServerProxy(knownServerProxy);
                }
            }

            return Optional.ofNullable(knownServerProxy);
        }

        /**
         * Forgets a server proxy.
         *
         * @param knownServerProxy The known server proxy.
         */
        protected final void forgetServerProxy(
                @Nonnull final PAPProxy knownServerProxy)
        {
            synchronized (_knownServerProxies) {
                _knownServerProxies.remove(knownServerProxy.getOrigin());
                knownServerProxy.disconnect();
            }
        }

        /**
         * Gets the context.
         *
         * @return The context.
         */
        @Nonnull
        @CheckReturnValue
        protected final PAPContext getContext()
        {
            return _context;
        }

        /**
         * Gets the known server proxies.
         *
         * @return The known server proxies.
         */
        @Nonnull
        @CheckReturnValue
        protected final Collection<PAPProxy> getKnownServerProxies()
        {
            synchronized (_knownServerProxies) {
                return _knownServerProxies.values();
            }
        }

        /**
         * Gets the server proxy for an origin.
         *
         * @param origin The origin.
         *
         * @return The server proxy (empty if unknown).
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<? extends PAPProxy> getServerProxy(
                @Nonnull final Origin origin)
        {
            PAPProxy knownServerProxy;

            synchronized (_knownServerProxies) {
                knownServerProxy = _knownServerProxies.get(origin);

                if (knownServerProxy == null) {
                    final Optional<? extends PAPProxy> serverProxy =
                        getContext()
                            .getRemoteProxyByOrigin(origin);

                    if (serverProxy.isPresent()) {
                        knownServerProxy = serverProxy.get().copy();
                        _knownServerProxies.put(origin, knownServerProxy);
                        knownServerProxy.setConnectionListener(this);
                    } else {
                        getThisLogger()
                            .warn(PAPMessages.UNKNOWN_ORIGIN, origin);
                    }

                }
            }

            return Optional.ofNullable(knownServerProxy);
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
            return _logger;
        }

        private final PAPConnectionListener.Manager _connectionListenerManager =
            new PAPConnectionListener.Manager();
        private final PAPContext _context;
        private final Map<Origin, PAPProxy> _knownServerProxies =
            new IdentityHashMap<>();
        private final Logger _logger = Logger.getInstance(getClass());
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
