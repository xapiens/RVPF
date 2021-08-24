/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SessionImpl.java 3968 2019-05-09 12:54:51Z SFB $
 */

package org.rvpf.service.rmi;

import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.Unreferenced;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.rmi.LoginFailedException;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.UnauthorizedAccessException;
import org.rvpf.base.security.Identity;
import org.rvpf.base.security.Realm;
import org.rvpf.service.ServiceMessages;

/**
 * Session implementation.
 */
@ThreadSafe
public abstract class SessionImpl
    implements Session, Unreferenced
{
    /**
     * Constructs an instance.
     *
     * @param clientName A descriptive name for the client.
     * @param sessionFactory The factory creating this.
     * @param connectionMode The connection mode.
     */
    protected SessionImpl(
            @Nonnull final String clientName,
            @Nonnull final SessionFactory sessionFactory,
            @Nonnull final ConnectionMode connectionMode)
    {
        _sessionFactory = sessionFactory;
        _connectionMode = connectionMode;
        _clientName = clientName;

        _logID = Logger.currentLogID().orElse(null);
    }

    /**
     * Closes the session.
     */
    public void close()
    {
        Logger.setLogID(Optional.ofNullable(_logID));

        synchronized (_mutex) {
            if (!_closed) {
                if (_sessionFactory != null) {
                    if (_sessionFactory.removeSession(this)) {
                        getThisLogger()
                            .debug(ServiceMessages.SESSION_CLOSED, this);
                        _clientHost = null;
                    }
                }

                _closed = true;
            }
        }
    }

    /**
     * Gets the connection mode.
     *
     * @return The connection mode.
     */
    @Override
    public final ConnectionMode getConnectionMode()
    {
        return _connectionMode;
    }

    /**
     * Gets the session factory.
     *
     * @return The session factory.
     */
    @Nonnull
    @CheckReturnValue
    public final SessionFactory getSessionFactory()
    {
        return _sessionFactory;
    }

    /**
     * Asks if the service is closed.
     *
     * @return True if the service is closed.
     */
    @CheckReturnValue
    public boolean isClosed()
    {
        return _closed;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void login(
            final String identifier,
            final char[] password)
        throws LoginFailedException, ServiceClosedException
    {
        Logger.setLogID(Optional.ofNullable(_logID));

        try {
            synchronized (_mutex) {
                if (isClosed()) {
                    throw new ServiceClosedException();
                }

                final Optional<Realm> realm = _sessionFactory.getRealm();

                if (!realm.isPresent()) {
                    throw new LoginFailedException(
                        ServiceMessages.LOGIN_NOT_CONFIGURED);
                }

                if ((identifier == null) || (password == null)) {
                    throw new LoginFailedException(
                        ServiceMessages.ILLEGAL_ARGUMENTS);
                }

                _identity = realm
                    .get()
                    .authenticate(identifier, password)
                    .orElse(null);

                if (_identity != null) {
                    getThisLogger()
                        .debug(
                            ServiceMessages.LOGIN_COMPLETED,
                            identifier,
                            this);
                } else {
                    getThisLogger()
                        .warn(ServiceMessages.LOGIN_FAILED, identifier, this);
                    close();

                    throw new LoginFailedException(
                        ServiceMessages.AUTHENTICATION_FAILED);
                }
            }
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void logout()
    {
        Logger.setLogID(Optional.ofNullable(_logID));

        try {
            synchronized (_mutex) {
                final Optional<String> user = getUser();

                if (user.isPresent()) {
                    _identity = null;
                    getThisLogger()
                        .debug(ServiceMessages.LOGOUT_COMPLETED, user.get());
                }
            }

            close();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /**
     * Opens the session.
     */
    public void open()
    {
        synchronized (_mutex) {
            if (_sessionFactory != null) {
                if (_sessionFactory.addSession(this)) {
                    if (ServiceRegistry.isPrivate()) {
                        _clientHost = "";
                    } else {
                        try {
                            _clientHost = RemoteServer.getClientHost();
                        } catch (final ServerNotActiveException exception) {
                            _clientHost = "";
                        }
                    }

                    getThisLogger().debug(ServiceMessages.SESSION_OPENED, this);
                }
            }

            _closed = false;
        }
    }

    /**
     * Performs security checks.
     *
     * @param requiredRole The required role.
     *
     * @throws UnauthorizedAccessException When access is not authorized.
     * @throws ServiceClosedException When the service is closed.
     */
    public final void securityCheck(
            @Nonnull final String requiredRole)
        throws UnauthorizedAccessException, ServiceClosedException
    {
        if (isClosed()) {
            throw new ServiceClosedException();
        }

        if ((_sessionFactory != null) && _sessionFactory.hasRolesMap()) {
            final String[] roles = _sessionFactory.getMappedRoles(requiredRole);
            boolean ok = roles.length == 0;

            if (!ok) {
                synchronized (_mutex) {
                    final Identity identity = _identity;

                    if (identity != null) {
                        ok = identity.isInRoles(roles);
                    }
                }
            }

            if (!ok) {
                throw new UnauthorizedAccessException(
                    ServiceMessages.ROLE_REQUIRED,
                    requiredRole);
            }
        }

        Logger.setLogID(Optional.ofNullable(_logID));
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return Message
            .format(
                ServiceMessages.SESSION_IMPL,
                (_connectionMode != null)? (_connectionMode + " "): "",
                getClientName(),
                _clientHost);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void unreferenced()
    {
        close();
    }

    /**
     * Gets the client name.
     *
     * @return The client name.
     */
    @Nonnull
    @CheckReturnValue
    protected final String getClientName()
    {
        return (_clientName != null)? _clientName: "";
    }

    /**
     * Gets the identity for an identifier.
     *
     * @param identifier The optional identifier for the identity.
     *
     * @return The roles of an identity.
     */
    @Nonnull
    @CheckReturnValue
    protected final Identity getIdentity(
            @Nonnull final Optional<String> identifier)
    {
        final Optional<Realm> realm = _sessionFactory.getRealm();

        return realm
            .isPresent()? realm
                .get()
                .getIdentity(identifier): Realm.UNKNOWN_IDENTITY;
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

    /**
     * Gets the user.
     *
     * @return The user.
     */
    @Nonnull
    @CheckReturnValue
    protected Optional<String> getUser()
    {
        return Optional
            .ofNullable((_identity != null)? _identity.getName(): null);
    }

    /**
     * Sets the log ID.
     */
    protected void setLogID()
    {
        Logger.setLogID(Optional.ofNullable(_logID));
    }

    /**
     * Sets the user.
     *
     * @param identifier The optional user identifier.
     */
    protected void setUser(@Nonnull final Optional<String> identifier)
    {
        _identity = identifier.isPresent()? getIdentity(identifier): null;
    }

    private volatile String _clientHost;
    private final String _clientName;
    private volatile boolean _closed;
    private final ConnectionMode _connectionMode;
    private volatile Identity _identity;
    private final String _logID;
    private final Logger _logger = Logger.getInstance(getClass());
    private final Object _mutex = new Object();
    private final SessionFactory _sessionFactory;
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
