/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyStoreClient.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.store.client;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreSessionFactory;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.http.ServiceSessionException;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ServiceRegistry;

/**
 * Proxy store client.
 */
public class ProxyStoreClient
{
    /**
     * Gets the store.
     *
     * @return The store.
     */
    @Nonnull
    @CheckReturnValue
    public StoreSessionProxy getStore()
    {
        return Require.notNull(_store);
    }

    /**
     * Impersonates a user.
     *
     * @param user The other user (empty string for anonymous, empty to cancel).
     *
     * @throws ServiceSessionException On store session exception.
     */
    public void impersonate(
            @Nonnull final Optional<String> user)
        throws ServiceSessionException
    {
        if (_identified) {
            final StoreSessionProxy store = getStore();

            if ((user.isPresent()) || _impersonating) {
                try {
                    store.impersonate(user.orElse(null));
                } catch (final SessionException exception) {
                    throw new ServiceSessionException(exception);
                }
            }

            _impersonating = user.isPresent();
        }
    }

    /**
     * Sets up this.
     *
     * @param config The Config.
     * @param contextProperties The context properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(
            @Nonnull final Config config,
            @Nonnull final KeyedGroups contextProperties)
    {
        final KeyedGroups storeProperties = contextProperties
            .getGroup(StoreSessionFactory.STORE_PROPERTIES);
        KeyedGroups securityProperties;

        securityProperties = storeProperties
            .getGroup(SecurityContext.SECURITY_PROPERTIES);

        if (securityProperties.isMissing()) {
            securityProperties = contextProperties
                .getGroup(SecurityContext.SECURITY_PROPERTIES);
        }

        final Optional<char[]> password;
        Optional<String> user = storeProperties
            .getString(StoreSessionFactory.USER_PROPERTY);

        if (!user.isPresent()) {
            user = contextProperties
                .getString(StoreSessionFactory.USER_PROPERTY);
        }

        password = storeProperties
            .getPassword(StoreSessionFactory.PASSWORD_PROPERTY);

        final RegistryEntry registryEntry = RegistryEntry
            .newBuilder()
            .setBinding(
                storeProperties.getString(StoreSessionFactory.BINDING_PROPERTY))
            .setName(
                storeProperties
                    .getString(
                            StoreSessionFactory.NAME_PROPERTY,
                                    Optional
                                            .of(StoreSessionFactory.DEFAULT_PROXY_STORE_NAME)))
            .setDefaultName(Store.DEFAULT_STORE_NAME)
            .setDefaultRegistryAddress(ServiceRegistry.getRegistryAddress())
            .setDefaultRegistryPort(ServiceRegistry.getRegistryPort())
            .build();
        final StoreSessionProxy store = (StoreSessionProxy) StoreSessionProxy
            .newBuilder()
            .setRegistryEntry(registryEntry)
            .setConfigProperties(config.getProperties())
            .setSecurityProperties(securityProperties)
            .setLoginUser(user)
            .setLoginPassword(password)
            .setClientName(config.getServiceName())
            .setClientLogger(_LOGGER)
            .setAutoconnect(true)
            .build();

        if (store == null) {
            return false;
        }

        if (store.isRemote()) {
            _LOGGER
                .warn(
                    ServiceMessages.REMOTE_SERVICE_WARNING,
                    _store.getServerURI());
        }

        _identified = store.hasLoginInfo();
        _store = store;

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        final StoreSessionProxy store = _store;

        if (store != null) {
            try {
                impersonate(Optional.empty());
            } catch (final ServiceSessionException exception) {
                // Ignores.
            }

            _store = null;
            store.tearDown();
            _identified = false;
        }
    }

    private static final Logger _LOGGER = Logger
        .getInstance(ProxyStoreClient.class);

    private boolean _identified;
    private volatile boolean _impersonating;
    private volatile StoreSessionProxy _store;
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
