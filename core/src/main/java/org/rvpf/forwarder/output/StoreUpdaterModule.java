/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreUpdaterModule.java 4056 2019-06-04 15:21:02Z SFB $
 */

package org.rvpf.forwarder.output;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.rmi.SessionConnectFailedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreSessionFactory;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ServiceRegistry;

/**
 * Store updater module.
 */
public class StoreUpdaterModule
    extends OutputModule
{
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        setOutput(new _StoreUpdater());

        return super.setUp(moduleProperties);
    }

    /**
     * Store updater.
     */
    private final class _StoreUpdater
        extends AbstractOutput
    {
        /**
         * Constructs an instance.
         */
        _StoreUpdater() {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void close()
        {
            if (_sessionProxy != null) {
                _sessionProxy.disconnect();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean commit()
        {
            if (!_updates.isEmpty()) {
                try {
                    final boolean success = _sessionProxy
                        .updateAndCheck(_updates, getThisLogger());

                    _updates.clear();

                    if (!success) {
                        return false;
                    }
                } catch (final SessionException exception) {
                    throw new RuntimeException(exception);
                }
            }

            return super.commit();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDestinationName()
        {
            return _sessionProxy.getServerName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "Store updater";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            final StoreSessionProxy sessionProxy = _sessionProxy;

            return (sessionProxy == null) || !sessionProxy.isConnected();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isOpen()
        {
            return _sessionProxy.isConnected();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isReliable()
        {
            return !_sessionProxy.isRemote();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean open()
            throws InterruptedException
        {
            try {
                _sessionProxy.connect();
            } catch (final SessionConnectFailedException exception) {
                getThisLogger()
                    .error(
                        ServiceMessages.CONNECTION_FAILED,
                        getDestinationName(),
                        exception.getMessage());

                return false;
            }

            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean output(final Serializable[] messages)
        {
            for (final Serializable message: messages) {
                if (message instanceof PointValue) {
                    _updates.add((PointValue) message);
                }
            }

            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean setUp(final KeyedGroups moduleProperties)
        {
            if (!super.setUp(moduleProperties)) {
                return false;
            }

            final KeyedGroups storeProperties = moduleProperties
                .getGroup(StoreSessionFactory.STORE_PROPERTIES);
            KeyedGroups securityProperties;

            securityProperties = storeProperties
                .getGroup(SecurityContext.SECURITY_PROPERTIES);

            if (securityProperties.isMissing()) {
                securityProperties = moduleProperties
                    .getGroup(SecurityContext.SECURITY_PROPERTIES);
            }

            final Optional<char[]> password;
            Optional<String> user = storeProperties
                .getString(StoreSessionFactory.USER_PROPERTY);

            if (user.isPresent()) {
                password = storeProperties
                    .getPassword(StoreSessionFactory.PASSWORD_PROPERTY);
            } else {
                user = moduleProperties
                    .getString(StoreSessionFactory.USER_PROPERTY);
                password = moduleProperties
                    .getPassword(StoreSessionFactory.PASSWORD_PROPERTY);
            }

            final RegistryEntry registryEntry = RegistryEntry
                .newBuilder()
                .setBinding(
                    storeProperties
                        .getString(StoreSessionFactory.BINDING_PROPERTY))
                .setName(
                    storeProperties
                        .getString(StoreSessionFactory.NAME_PROPERTY))
                .setDefaultName(Store.DEFAULT_STORE_NAME)
                .setDefaultRegistryAddress(ServiceRegistry.getRegistryAddress())
                .setDefaultRegistryPort(ServiceRegistry.getRegistryPort())
                .build();

            _sessionProxy = (StoreSessionProxy) StoreSessionProxy
                .newBuilder()
                .setRegistryEntry(registryEntry)
                .setConfigProperties(getConfigProperties())
                .setSecurityProperties(securityProperties)
                .setLoginUser(user)
                .setLoginPassword(password)
                .setClientName(getConfig().getServiceName())
                .setClientLogger(getThisLogger())
                .build();

            if (_sessionProxy == null) {
                return false;
            }

            if (_sessionProxy.isRemote()) {
                getThisLogger()
                    .warn(
                        ServiceMessages.REMOTE_SERVICE_WARNING,
                        _sessionProxy.getServerURI());
            }

            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            close();
            _sessionProxy = null;

            super.tearDown();
        }

        private volatile StoreSessionProxy _sessionProxy;
        private final List<PointValue> _updates = new LinkedList<>();
    }
}

/*
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License version 2.1 as published by
 * the Free Software Foundation. This software is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License along with this software; if
 * not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA
 */
