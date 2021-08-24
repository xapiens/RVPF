/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SessionProxy.java 4093 2019-06-24 13:40:22Z SFB $
 */

package org.rvpf.base.rmi;

import java.io.InterruptedIOException;

import java.net.URI;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.ServerError;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.ClientSecurityContext;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.LoginInfo;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * Session proxy.
 *
 * <p>A session proxy wraps the communication with a remote session object. This
 * includes the creation of that object thru the appropriate session
 * factory.</p>
 */
@ThreadSafe
public abstract class SessionProxy
    implements Session
{
    /**
     * Constructs an instance.
     *
     * @param clientName The client name.
     * @param loginInfo The optional login informations.
     * @param context The session client context.
     * @param listener The optional listener.
     * @param autoconnect The autoconnect indicator.
     */
    protected SessionProxy(
            @Nonnull final String clientName,
            @Nonnull final Optional<LoginInfo> loginInfo,
            @Nonnull final SessionClientContext context,
            @Nonnull final Optional<Listener> listener,
            final boolean autoconnect)
    {
        _clientName = Require.notNull(clientName);
        _loginInfo = loginInfo.orElse(null);
        _context = Require.notNull(context);
        _listener = listener.orElse(null);
        _autoconnect = autoconnect;
    }

    /**
     * Connects to a session.
     *
     * <p>This method may be called redundantly.</p>
     *
     * @throws SessionConnectFailedException When connect fails.
     */
    public void connect()
        throws SessionConnectFailedException
    {
        try {
            lockConnectInterruptibly();
        } catch (final CatchedSessionException exception) {
            throw new SessionConnectFailedException(
                (Exception) exception.getCause());
        }

        try {
            if ((_context == null) || (_session != null)) {
                return;
            }

            _context.register();

            try {
                _session = createSession();

                if (_loginInfo != null) {
                    _session
                        .login(
                            _loginInfo.getUser().orElse(null),
                            _loginInfo.getPassword().orElse(null));
                }
            } catch (final SessionConnectFailedException exception) {
                _session = null;

                throw exception;
            } catch (final Exception exception) {
                _session = null;

                throw new SessionConnectFailedException(exception);
            } finally {
                if (_session == null) {
                    _context.unregister();
                    _factory = null;
                }
            }

            final Listener listener = _listener;

            if (listener != null) {
                if (!listener.onSessionConnected(this)) {
                    disconnect();

                    throw new SessionConnectVetoException();
                }
            }
        } finally {
            unlockConnect();
        }
    }

    /**
     * Disconnects from the session.
     *
     * <p>This method may be called redundantly.</p>
     */
    public void disconnect()
    {
        try {
            lockConnectInterruptibly();
        } catch (final CatchedSessionException exception) {
            Thread.currentThread().interrupt();

            return;
        }

        try {
            final Session session = _session;

            if (session != null) {
                try {
                    _session.logout();
                } catch (final NoSuchObjectException exception) {
                    // Ignores.
                } finally {
                    _session = null;
                }

                if (_context != null) {
                    _context.unregister();
                }
            }

            _factory = null;

            if (session != null) {
                final Listener listener = _listener;

                if (listener != null) {
                    listener.onSessionDisconnected(this);
                }
            }
        } catch (final Exception exception) {
            getThisLogger().debug(exception, BaseMessages.DISCONNECT_FAILED);
        } finally {
            unlockConnect();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public ConnectionMode getConnectionMode()
    {
        final ConnectionMode connectionMode;

        lockConnect();

        try {
            if (_session == null) {
                return null;
            }

            try {
                connectionMode = _session.getConnectionMode();
            } catch (final RemoteException exception) {
                throw new RuntimeException(exception);
            }
        } finally {
            unlockConnect();
        }

        return connectionMode;
    }

    /**
     * Gets the context.
     *
     * @return The context.
     */
    @Nonnull
    @CheckReturnValue
    public SessionClientContext getContext()
    {
        final SessionClientContext context;

        lockConnect();

        try {
            context = _context;
        } finally {
            unlockConnect();
        }

        Require.notNull(context, BaseMessages.PROXY_NOT_SET_UP);

        return context;
    }

    /**
     * Gets the context UUID.
     *
     * @return The context UUID.
     */
    @Nonnull
    @CheckReturnValue
    public UUID getContextUUID()
    {
        return getContext().getUUID();
    }

    /**
     * Gets the server name.
     *
     * @return The server name.
     */
    @Nonnull
    @CheckReturnValue
    public String getServerName()
    {
        final String serverName;

        lockConnect();

        try {
            Require.notNull(_context, BaseMessages.PROXY_NOT_SET_UP);

            serverName = _context.getServerName();
        } finally {
            unlockConnect();
        }

        return serverName;
    }

    /**
     * Gets the server URI.
     *
     * @return The server URI.
     */
    @Nonnull
    @CheckReturnValue
    public URI getServerURI()
    {
        return getContext().getServerURI();
    }

    /**
     * Asks if login info is available.
     *
     * @return True if login info is available.
     */
    @CheckReturnValue
    public final boolean hasLoginInfo()
    {
        return _loginInfo != null;
    }

    /**
     * Asks if the session is connected.
     *
     * @return True if connected.
     */
    @CheckReturnValue
    public boolean isConnected()
    {
        final boolean isConnected;

        try {
            lockConnectInterruptibly();
        } catch (final CatchedSessionException exception) {
            Thread.currentThread().interrupt();

            return false;
        }

        try {
            isConnected = _session != null;
        } finally {
            unlockConnect();
        }

        return isConnected;
    }

    /**
     * Asks if this is a private session.
     *
     * @return True if this is a private session.
     */
    @CheckReturnValue
    public boolean isPrivate()
    {
        return getContext().isPrivate();
    }

    /**
     * Asks if this is a remote session.
     *
     * @return True if this is a remote session.
     */
    @CheckReturnValue
    public boolean isRemote()
    {
        return getContext().isRemote();
    }

    /** {@inheritDoc}
     */
    @Override
    public void login(final String identifier, final char[] password)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public void logout()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        try {
            lockConnectInterruptibly();
        } catch (final CatchedSessionException exception) {
            Thread.currentThread().interrupt();

            return;
        }

        try {
            disconnect();
        } finally {
            _factory = null;
            unlockConnect();
        }
    }

    /**
     * Creates a session.
     *
     * @return The session.
     *
     * @throws RemoteException From RMI.
     * @throws SessionException When session creation fails.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Session createSession()
        throws RemoteException, SessionException;

    /**
     * Gets the clientName.
     *
     * @return The clientName.
     */
    @Nonnull
    @CheckReturnValue
    protected String getClientName()
    {
        return _clientName;
    }

    /**
     * Gets the session factory.
     *
     * @return The factory.
     *
     * @throws SessionConnectFailedException When connect fails.
     */
    @Nonnull
    @CheckReturnValue
    protected final Remote getFactory()
        throws SessionConnectFailedException
    {
        final Remote factory;

        try {
            lockConnectInterruptibly();
        } catch (final CatchedSessionException exception) {
            throw new SessionConnectFailedException(
                (Exception) exception.getCause());
        }

        try {
            if (_factory == null) {
                Require.notNull(_context, BaseMessages.PROXY_NOT_SET_UP);

                try {
                    _factory = _context.lookup(sessionMode());
                } catch (final Exception exception) {
                    throw new SessionConnectFailedException(exception);
                }
            }

            factory = _factory;
        } finally {
            unlockConnect();
        }

        return factory;
    }

    /**
     * Gets the session.
     *
     * @return The session.
     *
     * @throws SessionException When the session can't be obtained.
     */
    @Nonnull
    @CheckReturnValue
    protected final Session getSession()
        throws SessionException
    {
        final Session session;

        try {
            lockConnectInterruptibly();
        } catch (final CatchedSessionException exception) {
            final Exception exceptionCause = (Exception) exception.getCause();

            if (exceptionCause instanceof InterruptedException) {
                throw new ServiceClosedException(exceptionCause);
            }

            throw new SessionConnectFailedException(exceptionCause);
        }

        try {
            if (_session == null) {
                if (_autoconnect) {
                    connect();
                }

                if (_session == null) {
                    throw new SessionNotConnectedException();
                }
            }

            session = _session;
        } catch (final Exception exception) {
            throw sessionException(exception);
        } finally {
            unlockConnect();
        }

        return session;
    }

    /**
     * Gets the logger for this instance.
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
     * Locks connect.
     */
    protected void lockConnect()
    {
        _connectMutex.lock();
    }

    /**
     * Locks connect interruptibly.
     *
     * @throws CatchedSessionException When an exception is catched.
     */
    protected void lockConnectInterruptibly()
        throws CatchedSessionException
    {
        try {
            _connectMutex.lockInterruptibly();
        } catch (final InterruptedException exception) {
            throw new CatchedSessionException(exception);
        }
    }

    /**
     * Throws a session exception.
     *
     * @param exception A catched exception.
     *
     * @return A session exception.
     */
    @Nonnull
    @CheckReturnValue
    protected SessionException sessionException(
            @Nonnull final Exception exception)
    {
        final SessionException sessionException;

        if (exception instanceof SessionException) {
            sessionException = (SessionException) exception;
        } else if (exception instanceof NoSuchObjectException) {
            sessionException = new ServiceClosedException(exception);
        } else if (exception instanceof InterruptedException) {
            sessionException = new ServiceClosedException(exception);
        } else if ((exception instanceof RemoteException)
                   && (exception.getCause()
                   instanceof InterruptedIOException)) {
            sessionException = new ServiceClosedException(
                (InterruptedIOException) exception.getCause());
        } else if (exception instanceof ServerError) {
            sessionException = new SessionException(exception);
        } else {
            sessionException = new CatchedSessionException(exception);
        }

        if (_autoconnect) {
            disconnect();
        }

        return sessionException;
    }

    /**
     * Returns some session mode identifying text.
     *
     * @return The session mode identifying text.
     */
    @Nonnull
    @CheckReturnValue
    protected String sessionMode()
    {
        return "";
    }

    /**
     * Unlocks connect.
     */
    protected void unlockConnect()
    {
        _connectMutex.unlock();
    }

    private final boolean _autoconnect;
    private final String _clientName;
    private final ReentrantLock _connectMutex = new ReentrantLock();
    private final SessionClientContext _context;
    private Remote _factory;
    private final Listener _listener;
    private final Logger _logger = Logger.getInstance(getClass());
    private final LoginInfo _loginInfo;
    private Session _session;

    /**
     * Listener.
     */
    public interface Listener
    {
        /**
         * Called when the session is connected.
         *
         * @param sessionProxy The session proxy.
         *
         * @return True to accept the connection.
         */
        @CheckReturnValue
        boolean onSessionConnected(@Nonnull SessionProxy sessionProxy);

        /**
         * Called when the session is disconnected.
         *
         * @param sessionProxy The session proxy.
         */
        void onSessionDisconnected(@Nonnull SessionProxy sessionProxy);
    }


    /**
     * Builder.
     */
    public abstract static class Builder
    {
        /**
         * Builds a store session proxy.
         *
         * @return The store session proxy (null on failure).
         */
        @Nullable
        @CheckReturnValue
        public abstract SessionProxy build();

        /**
         * Sets the autoconnect indicator.
         *
         * @param autoconnect The autoconnect indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setAutoconnect(final boolean autoconnect)
        {
            _autoconnect = autoconnect;

            return this;
        }

        /**
         * Sets the client logger.
         *
         * @param clientLogger The client logger.
         *
         * @return This.
         */
        @Nonnull
        public Builder setClientLogger(@Nonnull final Logger clientLogger)
        {
            _clientLogger = clientLogger;

            return this;
        }

        /**
         * Sets the client name.
         *
         * @param clientName The client name.
         *
         * @return This.
         */
        @Nonnull
        public Builder setClientName(@Nonnull final String clientName)
        {
            _clientName = clientName;

            return this;
        }

        /**
         * Sets the config properties.
         *
         * @param configProperties The config properties.
         *
         * @return This.
         */
        @Nonnull
        public Builder setConfigProperties(
                @Nonnull final KeyedGroups configProperties)
        {
            _configProperties = Require.notNull(configProperties);

            return this;
        }

        /**
         * Sets the listener.
         *
         * @param listener The listener.
         *
         * @return This.
         */
        @Nonnull
        public Builder setListener(@Nonnull final Listener listener)
        {
            _listener = Require.notNull(listener);

            return this;
        }

        /**
         * Sets the login informations.
         *
         * @param loginInfo The login informations.
         *
         * @return This.
         */
        @Nonnull
        public Builder setLoginInfo(@Nonnull final LoginInfo loginInfo)
        {
            _loginUser = loginInfo.getUser().orElse(null);
            _loginPassword = loginInfo.getPassword().orElse(null);

            return this;
        }

        /**
         * Sets the login password.
         *
         * @param loginPassword The optional login password.
         *
         * @return This.
         */
        @Nonnull
        public Builder setLoginPassword(
                @Nonnull final Optional<char[]> loginPassword)
        {
            _loginPassword = loginPassword.orElse(null);

            return this;
        }

        /**
         * Sets the login user.
         *
         * @param loginUser The optional login user.
         *
         * @return This.
         */
        @Nonnull
        public Builder setLoginUser(@Nonnull final Optional<String> loginUser)
        {
            _loginUser = loginUser.orElse(null);

            return this;
        }

        /**
         * Sets the registry entry.
         *
         * @param registryEntry The registry entry.
         *
         * @return This.
         */
        @Nonnull
        public Builder setRegistryEntry(
                @Nonnull final RegistryEntry registryEntry)
        {
            _registryEntry = registryEntry;

            return this;
        }

        /**
         * Sets the security properties.
         *
         * @param securityProperties The security properties.
         *
         * @return This.
         */
        @Nonnull
        public Builder setSecurityProperties(
                @Nonnull final KeyedGroups securityProperties)
        {
            _securityProperties = Require.notNull(securityProperties);

            return this;
        }

        /**
         * Gets the client name.
         *
         * @return The client name.
         */
        @Nonnull
        @CheckReturnValue
        protected String getClientName()
        {
            return _clientName;
        }

        /**
         * Gets the context.
         *
         * @return The context.
         */
        @Nonnull
        @CheckReturnValue
        protected SessionClientContext getContext()
        {
            return _context;
        }

        /**
         * Gets the listener.
         *
         * @return The optional listener.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<Listener> getListener()
        {
            return Optional.ofNullable(_listener);
        }

        /**
         * Gets the login info.
         *
         * @return The optional login info.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<LoginInfo> getLoginInfo()
        {
            return Optional.ofNullable(_loginInfo);
        }

        /**
         * Gets the autoconnect indicator.
         *
         * @return The autoconnect indicator.
         */
        @CheckReturnValue
        protected boolean isAutoconnect()
        {
            return _autoconnect;
        }

        /**
         * Sets up for the build.
         *
         * @return True on success.
         */
        @CheckReturnValue
        protected boolean setUp()
        {
            _context = new SessionClientContext(_registryEntry, _clientLogger);

            final ClientSecurityContext securityContext = _context
                .getSecurityContext();

            if (!securityContext
                .setUp(
                    (_configProperties != null)
                    ? _configProperties: KeyedGroups.MISSING_KEYED_GROUP,
                    (_securityProperties != null)
                    ? _securityProperties: KeyedGroups.MISSING_KEYED_GROUP)) {
                return false;
            }

            if (_loginInfo == null) {
                final LoginInfo loginInfo = new LoginInfo(
                    Optional.ofNullable(_loginUser),
                    Optional.ofNullable(_loginPassword));

                _loginInfo = loginInfo.isEnabled()? loginInfo: null;
            }

            return true;
        }

        private boolean _autoconnect;
        private Logger _clientLogger;
        private String _clientName;
        private KeyedGroups _configProperties;
        private SessionClientContext _context;
        private Listener _listener;
        private LoginInfo _loginInfo;
        private char[] _loginPassword;
        private String _loginUser;
        private RegistryEntry _registryEntry;
        private KeyedGroups _securityProperties;
    }


    /**
     * Connect veto exception.
     */
    public static final class SessionConnectVetoException
        extends SessionConnectFailedException
    {
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
