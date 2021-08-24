/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMProxy.java 4059 2019-06-05 20:44:44Z SFB $
 */

package org.rvpf.base.som;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.rmi.RemoteException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.CatchedSessionException;
import org.rvpf.base.rmi.RegistryConfig;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.rmi.SessionClientContext;
import org.rvpf.base.rmi.SessionConnectFailedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.rmi.SessionProxy;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.util.LoginInfo;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * SOM proxy.
 */
@ThreadSafe
public abstract class SOMProxy
    extends SessionProxy
    implements SOMSession
{
    /**
     * Constructs an instance.
     *
     * @param clientName A descriptive name for the client.
     * @param loginInfo The optional login informations.
     * @param context The session client context.
     * @param listener The optional listener.
     * @param autoconnect The autoconnect indicator.
     * @param timeout The optional timeout.
     */
    SOMProxy(
            @Nonnull final String clientName,
            @Nonnull final Optional<LoginInfo> loginInfo,
            @Nonnull final SessionClientContext context,
            @Nonnull final Optional<Listener> listener,
            final boolean autoconnect,
            @Nonnull final Optional<ElapsedTime> timeout)
    {
        super(clientName, loginInfo, context, listener, autoconnect);

        _timeout = timeout.orElse(null);

        if (_timeout != null) {
            getThisLogger().debug(BaseMessages.TIMEOUT, getSOMName(), _timeout);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
        throws SessionException
    {
        disconnect();
    }

    /** {@inheritDoc}
     */
    @Override
    public void connect()
        throws SessionConnectFailedException
    {
        super.connect();

        _closed.set(false);

        getThisLogger()
            .debug(
                BaseMessages.SOM_PROXY_CONNECTED,
                getServerName(),
                sessionMode());
    }

    /** {@inheritDoc}
     */
    @Override
    public final void disconnect()
    {
        try {
            lockConnectInterruptibly();
        } catch (final CatchedSessionException exception) {
            Thread.currentThread().interrupt();

            return;
        }

        try {
            if (isConnected()) {
                _somName = null;

                super.disconnect();

                getThisLogger()
                    .debug(
                        BaseMessages.SOM_PROXY_DISCONNECTED,
                        getServerName(),
                        sessionMode());
            }
        } finally {
            unlockConnect();
        }
    }

    /**
     * Gets the SOM name.
     *
     * @return The SOM name.
     */
    @Nonnull
    @CheckReturnValue
    public final String getSOMName()
    {
        String somName = _somName;

        if (somName == null) {
            try {
                somName = ((SOMServer) getFactory()).getName();
                _somName = somName;
            } catch (final RemoteException exception) {
                throw new RuntimeException(exception);
            } catch (final SessionConnectFailedException exception) {
                somName = getServerURI().toString();
            }
        }

        return somName;
    }

    /**
     * Asks if this is closed.
     *
     * @return True if closed.
     */
    @CheckReturnValue
    public final boolean isClosed()
    {
        return _closed.get();
    }

    /**
     * Confirms the timeout to the context.
     *
     * @throws RemoteException From RMI.
     * @throws SessionConnectFailedException When connect fails.
     */
    protected void confirmTimeout()
        throws RemoteException, SessionConnectFailedException
    {
        if (_timeout == null) {
            final ElapsedTime keepAlive = ((SOMServer) getFactory())
                .getKeepAlive();

            _timeout = keepAlive.add(keepAlive);
            getThisLogger().debug(BaseMessages.TIMEOUT, getSOMName(), _timeout);
        }

        final SessionClientContext context = getContext();

        if (context == null) {
            throw new SessionConnectFailedException();
        }

        context.setTimeout((int) _timeout.toMillis());
    }

    /** The user password. */
    public static final String PASSWORD_PROPERTY = "password";

    /** The timeout property. */
    public static final String TIMEOUT_PROPERTY = "timeout";

    /** The user identification. */
    public static final String USER_PROPERTY = "user";

    private final AtomicBoolean _closed = new AtomicBoolean();
    private volatile String _somName;
    private volatile ElapsedTime _timeout;

    /**
     * Builder.
     */
    public abstract static class Builder
        extends SessionProxy.Builder
    {
        /**
         * Sets the timeout.
         *
         * @param timeout The optional timeout.
         *
         * @return This.
         */
        public Builder setTimeout(@Nonnull final Optional<ElapsedTime> timeout)
        {
            _timeout = timeout.orElse(null);

            return this;
        }

        /**
         * Gets the default bind prefix.
         *
         * @return The default bind prefix.
         */
        @Nonnull
        @CheckReturnValue
        protected abstract String getDefaultBindPrefix();

        /**
         * Gets the timeout.
         *
         * @return The optional timeout.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<ElapsedTime> getTimeout()
        {
            return Optional.ofNullable(_timeout);
        }

        /**
         * Prepares.
         *
         * @param configProperties The config properties.
         * @param somProperties The SOM properties.
         * @param clientName The client name.
         * @param clientLogger The client logger.
         *
         * @return The prepared builder (null on failure).
         */
        @Nullable
        @CheckReturnValue
        protected SOMProxy.Builder prepare(
                @Nonnull final KeyedGroups configProperties,
                @Nonnull final KeyedGroups somProperties,
                @Nonnull final String clientName,
                @Nonnull final Logger clientLogger)
        {
            final RegistryConfig registryConfig = new RegistryConfig(
                configProperties);
            final Optional<InetSocketAddress> registrySocketAddress =
                registryConfig
                    .getRegistrySocketAddress();
            final InetAddress defaultRegistryAddress = registrySocketAddress
                .isPresent()? registrySocketAddress
                    .get()
                    .getAddress(): InetAddress.getLoopbackAddress();
            final int defaultRegistryPort = registryConfig.getRegistryPort();

            if (defaultRegistryPort < 0) {
                return null;
            }

            final RegistryEntry registryEntry = RegistryEntry
                .newBuilder()
                .setBinding(somProperties.getString(SOMServer.BINDING_PROPERTY))
                .setName(somProperties.getString(SOMServer.NAME_PROPERTY))
                .setDefaultPrefix(getDefaultBindPrefix())
                .setDefaultRegistryAddress(
                    defaultRegistryAddress.isAnyLocalAddress()
                    ? Optional.empty(): Optional
                        .of(defaultRegistryAddress))
                .setDefaultRegistryPort(defaultRegistryPort)
                .build();

            if (registryEntry == null) {
                return null;
            }

            return (Builder) setRegistryEntry(registryEntry)
                .setConfigProperties(configProperties)
                .setSecurityProperties(
                    somProperties.getGroup(SecurityContext.SECURITY_PROPERTIES))
                .setLoginUser(somProperties.getString(USER_PROPERTY))
                .setLoginPassword(somProperties.getPassword(PASSWORD_PROPERTY))
                .setClientName(clientName)
                .setClientLogger(clientLogger);
        }

        private ElapsedTime _timeout;
    }


    /**
     * Context properties.
     */
    @NotThreadSafe
    public static final class ContextProperties
        extends KeyedGroups
    {
        /**
         * Constructs an instance.
         */
        public ContextProperties() {}

        private ContextProperties(final ContextProperties other)
        {
            super(other);
        }

        /** {@inheritDoc}
         */
        @Override
        public ContextProperties copy()
        {
            return new ContextProperties(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public ContextProperties freeze()
        {
            super.freeze();

            return this;
        }

        /**
         * Sets the certified indicator.
         *
         * @param certified The certified indicator.
         */
        public void setCertified(final boolean certified)
        {
            _setSecurity(
                SecurityContext.CERTIFIED_PROPERTY,
                String.valueOf(certified));
        }

        /**
         * Sets the key password.
         *
         * @param keyPassword The optional key password.
         */
        public void setKeyPassword(@Nonnull final Optional<char[]> keyPassword)
        {
            _setSecurity(
                SecurityContext.PASSWORD_PROPERTY,
                (keyPassword.isPresent())? String.valueOf(keyPassword): null,
                SecurityContext.KEYSTORE_PROPERTIES,
                SecurityContext.KEY_PROPERTIES);
        }

        /**
         * Sets the key-store.
         *
         * @param keyStore The optional path to the key-store file.
         */
        public void setKeyStore(@Nonnull final Optional<String> keyStore)
        {
            _setSecurity(
                SecurityContext.PATH_PROPERTY,
                keyStore.orElse(null),
                SecurityContext.KEYSTORE_PROPERTIES);
        }

        /**
         * Sets the key-store password.
         *
         * @param keyStorePassword The optional key-store password.
         */
        public void setKeyStorePassword(
                @Nonnull final Optional<char[]> keyStorePassword)
        {
            _setSecurity(
                SecurityContext.PASSWORD_PROPERTY,
                (keyStorePassword.isPresent())? String
                    .valueOf(keyStorePassword.get()): null,
                SecurityContext.KEYSTORE_PROPERTIES,
                SecurityContext.KEY_PROPERTIES);
        }

        /**
         * Sets the key-store provider.
         *
         * @param provider The optional key-store provider.
         */
        public void setKeyStoreProvider(
                @Nonnull final Optional<String> provider)
        {
            _setSecurity(
                SecurityContext.PROVIDER_PROPERTY,
                provider.orElse(null),
                SecurityContext.KEYSTORE_PROPERTIES);
        }

        /**
         * Sets the key-store type.
         *
         * @param type The optional key-store type.
         */
        public void setKeyStoreType(@Nonnull final Optional<String> type)
        {
            _setSecurity(
                SecurityContext.TYPE_PROPERTY,
                type.orElse(null),
                SecurityContext.KEYSTORE_PROPERTIES);
        }

        /**
         * Sets the registry port.
         *
         * @param registryPort The registry port.
         */
        public void setRegistryPort(final int registryPort)
        {
            final KeyedGroups rmiRegistryProperties = new KeyedGroups();

            if (registryPort >= 0) {
                rmiRegistryProperties
                    .setValue(
                        RegistryConfig.PORT_PROPERTY,
                        String.valueOf(registryPort));
            }

            setGroup(
                RegistryConfig.RMI_REGISTRY_PROPERTIES,
                rmiRegistryProperties);
        }

        /**
         * Sets security properties.
         *
         * @param properties The optional security properties.
         */
        public void setSecurityProperties(@Nonnull final KeyedGroups properties)
        {
            setGroup(SecurityContext.SECURITY_PROPERTIES, properties);
        }

        /**
         * Sets the trust-store.
         *
         * @param trustStore The optional path to the trust-store file.
         */
        public void setTrustStore(@Nonnull final Optional<String> trustStore)
        {
            _setSecurity(
                SecurityContext.PATH_PROPERTY,
                trustStore.orElse(null),
                SecurityContext.TRUSTSTORE_PROPERTIES);
        }

        /**
         * Sets the trust-store provider.
         *
         * @param provider The optional trust-store provider.
         */
        public void setTrustStoreProvider(
                @Nonnull final Optional<String> provider)
        {
            _setSecurity(
                SecurityContext.PROVIDER_PROPERTY,
                provider.orElse(null),
                SecurityContext.TRUSTSTORE_PROPERTIES);
        }

        /**
         * Sets the trust-store type.
         *
         * @param type The optional trust-store type.
         */
        public void setTrustStoreType(@Nonnull final Optional<String> type)
        {
            _setSecurity(
                SecurityContext.TYPE_PROPERTY,
                type.orElse(null),
                SecurityContext.TRUSTSTORE_PROPERTIES);
        }

        private void _setSecurity(
                final String key,
                final String value,
                final String... groups)
        {
            KeyedGroups securityProperties = getGroup(
                SecurityContext.SECURITY_PROPERTIES);

            if (securityProperties.isMissing()) {
                securityProperties = new KeyedGroups();
                setSecurityProperties(securityProperties);
            }

            for (final String group: groups) {
                KeyedGroups groupProperties = securityProperties
                    .getGroup(group);

                if (groupProperties.isMissing()) {
                    groupProperties = new KeyedGroups();
                    securityProperties.setGroup(group, groupProperties);
                    securityProperties = groupProperties;
                }
            }

            if (value != null) {
                securityProperties.setValue(key, value);
            } else {
                securityProperties.removeValue(key);
            }
        }

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
