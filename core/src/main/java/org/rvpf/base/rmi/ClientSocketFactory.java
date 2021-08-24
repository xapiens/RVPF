/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClientSocketFactory.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.rmi;

import java.io.IOException;
import java.io.Serializable;

import java.net.Socket;

import java.rmi.ConnectException;
import java.rmi.server.RMIClientSocketFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.ClientSecurityContext;
import org.rvpf.base.tool.Require;

/**
 * Client socket factory.
 *
 * <p>This implementation of {@link RMIClientSocketFactory} uses the
 * {@link UUID} provided by a {@link ClientSecurityContext} to get the security
 * informations needed to create {@link Socket}s. The association is done by a
 * call to the static {@link #register} method by the
 * {@link ClientSecurityContext} itself before the connection to a server
 * object.</p>
 *
 * <p>This socket factory is created for each new session and is not reused.</p>
 */
@ThreadSafe
public class ClientSocketFactory
    implements RMIClientSocketFactory, Serializable
{
    /**
     * Constructs an instance.
     *
     * @param uuid The UUID identifying the context.
     */
    public ClientSocketFactory(@Nonnull final UUID uuid)
    {
        _uuid = uuid;
        _logID = Logger.currentLogID().orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Socket createSocket(
            final String host,
            final int port)
        throws IOException
    {
        final Socket socket = newSocket(host, port);
        final int timeout = getContext().getTimeout();

        if (timeout > 0) {
            socket.setSoTimeout(timeout);
            socket.setSoLinger(false, 0);
        }

        return socket;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }

        return _uuid.equals(((ClientSocketFactory) other)._uuid);
    }

    /** {@inheritDoc}
     */
    @Override
    public final int hashCode()
    {
        return _uuid.hashCode();
    }

    /**
     * Gets the context.
     *
     * <p>The context is cached to allow the RMI distributed garbage collector
     * to still be able to connect to the server for 'clean' requests even after
     * the context has been unregistered.</p>
     *
     * @return The context.
     *
     * @throws IOException If the context is unknown.
     */
    @Nonnull
    @CheckReturnValue
    protected synchronized SessionClientContext getContext()
        throws IOException
    {
        boolean logged = false;

        if (_context == null) {
            _context = _CONTEXTS.get(_uuid);

            if (_context != null) {
                Logger.setLogID(Optional.ofNullable(_logID));
                _LOGGER
                    .debug(
                        BaseMessages.CLIENT_CONTEXT_CACHED,
                        _context.getServerURI(),
                        _uuid);
                logged = true;
            }
        }

        if (_context == null) {
            try {
                throw new ConnectException("Context gone: " + _uuid);
            } catch (final ConnectException exception) {
                _warnContextGone(exception);

                throw exception;
            }
        }

        if (_LOGGER.isTraceEnabled()) {
            if (!logged) {
                Logger.setLogID(Optional.ofNullable(_logID));
            }

            _LOGGER
                .trace(
                    BaseMessages.CLIENT_CONTEXT,
                    _context.getServerURI(),
                    _uuid);
        }

        return _context;
    }

    /**
     * Create a client socket connected to the specified host and port.
     *
     * @param host The host name.
     * @param port The port number.
     *
     * @return A socket connected to the specified host and port.
     *
     * @exception IOException When an I/O error occurs during socket creation.
     */
    @Nonnull
    @CheckReturnValue
    protected Socket newSocket(
            @Nonnull final String host,
            final int port)
        throws IOException
    {
        return new Socket(Require.notNull(host), port);
    }

    /**
     * Registers a context
     *
     * @param context The context.
     *
     * @return False if already registered.
     */
    @CheckReturnValue
    static boolean register(@Nonnull final SessionClientContext context)
    {
        return _CONTEXTS.put(context.getUUID(), context) == null;
    }

    /**
     * Unregisters a context
     *
     * @param context The context.
     *
     * @return False if not registered.
     */
    @CheckReturnValue
    static boolean unregister(@Nonnull final SessionClientContext context)
    {
        return _CONTEXTS.remove(context.getUUID()) != null;
    }

    private synchronized void _warnContextGone(
            @Nonnull final Exception exception)
    {
        if (_warns <= _WARN_LIMIT) {
            _LOGGER.warn(exception, BaseMessages.CLIENT_CONTEXT_GONE, _uuid);

            if (++_warns > _WARN_LIMIT) {
                _LOGGER.warn(BaseMessages.CLIENT_CONTEXT_WARNINGS);
            }
        }
    }

    private static final int _WARN_LIMIT = 99;
    private static volatile int _warns;
    private static final long serialVersionUID = 1L;
    private static final Logger _LOGGER = Logger
        .getInstance(SecureClientSocketFactory.class);
    private static final Map<UUID, SessionClientContext> _CONTEXTS =
        new ConcurrentHashMap<>();

    private transient SessionClientContext _context;
    private final String _logID;
    private final UUID _uuid;
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
