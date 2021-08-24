/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SessionFactoryImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.service.rmi;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.CertifiedClientSocketFactory;
import org.rvpf.base.rmi.ClientSocketFactory;
import org.rvpf.base.rmi.SecureClientSocketFactory;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.security.Realm;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.ConcurrentIdentityHashSet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.ListHashMap;
import org.rvpf.base.util.container.ListMap;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceClassLoader;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Session factory implementation.
 */
@ThreadSafe
public abstract class SessionFactoryImpl
    implements SessionFactory
{
    /**
     * Constructs an instance.
     */
    protected SessionFactoryImpl()
    {
        this(Optional.empty());
    }

    /**
     * Constructs an instance.
     *
     * @param securityContext The optional security context.
     */
    protected SessionFactoryImpl(
            @Nonnull final Optional<SessionSecurityContext> securityContext)
    {
        _securityContext = securityContext.orElse(null);
    }

    /**
     * Creates a security context.
     *
     * @param configProperties The configuration properties (may be empty).
     * @param securityProperties Security properties (may be empty).
     * @param logger The logger instance to use.
     *
     * @return The security context (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public static SessionSecurityContext createSecurityContext(
            @Nonnull final KeyedGroups configProperties,
            @Nonnull final KeyedGroups securityProperties,
            @Nonnull final Logger logger)
    {
        final SessionSecurityContext securityContext =
            new SessionSecurityContext(
                logger);

        if (!securityContext.setUp(configProperties, securityProperties)) {
            return null;
        }

        return securityContext;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean addSession(final SessionImpl session)
    {
        return _sessions.add(session);
    }

    /**
     * Closes the sessions.
     */
    public void close()
    {
        if (_closed.compareAndSet(false, true)) {
            for (final SessionImpl session: new ArrayList<>(_sessions)) {
                session.close();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean equals(final Object object)
    {
        if (object == null) {
            return false;
        }

        return (object == this) || object.equals(_stub);
    }

    /** {@inheritDoc}
     */
    @Override
    public final synchronized Remote export()
        throws RemoteException
    {
        Require.success(_stub == null);

        if (ServiceRegistry.isPrivate()) {
            _stub = (Remote) this;
        } else {
            final Optional<String> savedLogID = Logger.currentLogID();
            final Optional<ServiceClassLoader> savedClassLoader =
                ServiceClassLoader
                    .hideInstance();

            try {
                Logger.restoreLogID(Optional.empty());
                _stub = UnicastRemoteObject.exportObject((Remote) this, 0);
            } finally {
                ServiceClassLoader.restoreInstance(savedClassLoader);
                Logger.restoreLogID(savedLogID);
            }

            Require.notNull(_stub);
            getThisLogger().trace(ServiceMessages.EXPORTED_RMI, this, _stub);
        }

        return _stub;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Config getConfig()
    {
        return Require.notNull(_config);
    }

    /** {@inheritDoc}
     */
    @Override
    public final String[] getMappedRoles(final String requiredRole)
    {
        final List<String> roles = _rolesMap.getAll(requiredRole);

        return roles.toArray(new String[roles.size()]);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<Realm> getRealm()
    {
        return Optional.of(_realm);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean hasRolesMap()
    {
        return _rolesMap != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public final int hashCode()
    {
        final Remote stub = _stub;

        return ((stub != null)
                && (stub != this))? stub.hashCode(): super.hashCode();
    }

    /**
     * Asks if this service is closed.
     *
     * @return True if this service is closed.
     */
    @CheckReturnValue
    public final boolean isClosed()
    {
        return _closed.get();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean removeSession(final SessionImpl session)
    {
        return _sessions.remove(session);
    }

    /**
     * Sets up this session factory.
     *
     * @param config The config.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public synchronized boolean setUp(@Nonnull final Config config)
    {
        _config = config;

        if (_securityContext == null) {
            _securityContext = createSecurityContext(
                config.getProperties(),
                KeyedGroups.MISSING_KEYED_GROUP,
                getThisLogger());

            if (_securityContext == null) {
                return false;
            }
        }

        final KeyedGroups realmProperties = _securityContext
            .getRealmProperties();

        if (!realmProperties.isMissing()) {
            if (!_realm
                .setUp(
                    _securityContext.getRealmProperties(),
                    _securityContext)) {
                return false;
            }
        }

        fillRolesMap();

        for (final List<String> roles: _rolesMap.values()) {
            if (roles.size() > 0) {
                if (realmProperties.isMissing()) {
                    getThisLogger().error(ServiceMessages.ROLES_REQUIRE_REALM);

                    return false;
                }

                break;
            }
        }

        _logID = Logger.currentLogID().orElse(null);

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        close();
    }

    /** {@inheritDoc}
     */
    @Override
    public final synchronized void unexport()
        throws NoSuchObjectException
    {
        Require.notNull(_stub);

        if (!ServiceRegistry.isPrivate()) {
            final Optional<ServiceClassLoader> savedClassLoader =
                ServiceClassLoader
                    .hideInstance();

            try {
                UnicastRemoteObject.unexportObject((Remote) this, true);
            } finally {
                ServiceClassLoader.restoreInstance(savedClassLoader);
            }

            getThisLogger().trace(ServiceMessages.UNEXPORTED_RMI, this, _stub);
        }

        _stub = null;
    }

    /**
     * Creates a session.
     *
     * @param uuid A UUID used to locate the RMI client instance.
     * @param reference A reference object.
     *
     * @return The new session.
     *
     * @throws SessionException When the session creation fails.
     */
    @Nonnull
    @CheckReturnValue
    protected final Session createSession(
            @Nonnull final UUID uuid,
            @Nonnull final Object reference)
        throws SessionException
    {
        Logger.setLogID(Optional.ofNullable(_logID));

        final Session session;

        if (ServiceRegistry.isPrivate()) {
            session = createSession(
                SessionImpl.ConnectionMode.PRIVATE,
                Optional.empty(),
                Optional.empty(),
                reference);
        } else {
            final SessionSecurityContext securityContext = _securityContext;
            final boolean local;

            if (securityContext.isSecure()) {
                local = false;
            } else {
                final String name;

                try {
                    name = RemoteServer.getClientHost();
                } catch (final ServerNotActiveException exception) {
                    throw new RuntimeException(exception);    // Should not happen.
                }

                final InetAddress address;

                try {
                    address = InetAddress.getByName(name);
                } catch (final UnknownHostException exception) {
                    throw new RuntimeException(exception);    // Should not happen.
                }

                local = Inet.isOnLocalHost(address);
            }

            final Optional<InetAddress> registerAddress = ServiceRegistry
                .getRegistryAddress();

            if (local) {
                session = createSession(
                    SessionImpl.ConnectionMode.LOCAL,
                    Optional.of(new ClientSocketFactory(uuid)),
                    Optional
                        .of(new BaseRMIServerSocketFactory(registerAddress)),
                    reference);
            } else if (securityContext.isCertified()) {
                session = createSession(
                    SessionImpl.ConnectionMode.CERTIFIED,
                    Optional.of(new CertifiedClientSocketFactory(uuid)),
                    Optional
                        .of(
                            new CertifiedRMIServerSocketFactory(
                                    registerAddress,
                                            securityContext)),
                    reference);
            } else {
                session = createSession(
                    SessionImpl.ConnectionMode.SECURE,
                    Optional.of(new SecureClientSocketFactory(uuid)),
                    Optional
                        .of(
                            new SecureRMIServerSocketFactory(
                                    registerAddress,
                                            securityContext)),
                    reference);
            }
        }

        return session;
    }

    /**
     * Creates a session.
     *
     * @param connectionMode The connection mode.
     * @param clientSocketFactory The optional client socket factory.
     * @param serverSocketFactory The optional server socket factory.
     * @param reference A reference object.
     *
     * @return The session.
     *
     * @throws SessionException When the session creation fails.
     */
    @Nonnull
    @CheckReturnValue
    protected final Session createSession(
            @Nonnull final SessionImpl.ConnectionMode connectionMode,
            @Nonnull final Optional<RMIClientSocketFactory> clientSocketFactory,
            @Nonnull final Optional<RMIServerSocketFactory> serverSocketFactory,
            @Nonnull final Object reference)
        throws SessionException
    {
        final Session session;

        synchronized (this) {
            if (isClosed()) {
                throw new ServiceClosedException();
            }

            session = newSession(
                connectionMode,
                clientSocketFactory,
                serverSocketFactory,
                reference);
        }

        ServiceThread.yieldAll();

        return session;
    }

    /**
     * Fills the role map.
     *
     * <p>This associates internal roles to external required roles.</p>
     */
    protected abstract void fillRolesMap();

    /**
     * Gets the session count.
     *
     * @return The session count.
     */
    @CheckReturnValue
    protected final int getSessionCount()
    {
        return _sessions.size();
    }

    /**
     * Gets the sessions.
     *
     * @return The sessions.
     */
    @Nonnull
    @CheckReturnValue
    protected final Collection<SessionImpl> getSessions()
    {
        return _sessions;
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
     * Asks if the security context has been defined.
     *
     * @return True if the security context has been defined.
     */
    @CheckReturnValue
    protected boolean hasSecurityContext()
    {
        return _securityContext != null;
    }

    /**
     * Adds a role to a roles map.
     *
     * @param requiredRole The name of the required role.
     * @param roles The name of the mapped roles
     */
    protected final void mapRoles(
            @Nonnull final String requiredRole,
            @Nonnull final String[] roles)
    {
        for (final String role: roles) {
            _rolesMap.add(requiredRole, role);
            getThisLogger().debug(ServiceMessages.ROLE, requiredRole, role);
        }
    }

    /**
     * Returns a new session
     *
     * @param connectionMode The connection mode.
     * @param clientSocketFactory The optional client socket factory.
     * @param serverSocketFactory The optional server socket factory.
     * @param reference A reference object.
     *
     * @return The session.
     *
     * @throws SessionException When the session creation fails.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Session newSession(
            @Nonnull SessionImpl.ConnectionMode connectionMode,
            @Nonnull Optional<RMIClientSocketFactory> clientSocketFactory,
            @Nonnull Optional<RMIServerSocketFactory> serverSocketFactory,
            Object reference)
        throws SessionException;

    private final AtomicBoolean _closed = new AtomicBoolean();
    private volatile Config _config;
    private String _logID;
    private final Logger _logger = Logger.getInstance(getClass());
    private final Realm _realm = new Realm();
    private final ListMap<String, String> _rolesMap = new ListHashMap<>();
    private volatile SessionSecurityContext _securityContext;
    private final Set<SessionImpl> _sessions =
        new ConcurrentIdentityHashSet<>();
    private volatile Remote _stub;
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
