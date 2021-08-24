/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Stopper.java 4081 2019-06-14 14:54:30Z SFB $
 */

package org.rvpf.service;

import java.lang.reflect.UndeclaredThrowableException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.LoginInfo;
import org.rvpf.config.Config;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.jmx.Agent;
import org.rvpf.jmx.AgentSessionProxy;
import org.rvpf.service.rmi.ServiceRegistry;

/**
 * Stopper.
 *
 * <p>Utility for stopping services.</p>
 *
 * <p>This implementation uses a JMX agent.</p>
 */
public final class Stopper
{
    /**
     * Main program.
     *
     * @param args Command line arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        Logger.startUp(false);
        Logger.setLogID();

        if (args.length == 0) {
            _LOGGER.error(ServiceMessages.SERVICES_NAME);

            return;
        }

        Require.ignored(new Stopper().stop(args));
    }

    /**
     * Stops the specified services.
     *
     * @param args The service names.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean stop(@Nonnull final String[] args)
    {
        final Config config = ConfigDocumentLoader
            .loadConfig("", Optional.empty(), Optional.empty());

        if (config == null) {
            _LOGGER.error(ServiceMessages.CONFIG_LOAD_FAILED);

            return false;
        }

        if (!ServiceRegistry.setUp(config.getProperties())) {
            return false;
        }

        final Set<String> serviceNames = new HashSet<String>();

        for (final String arg: args) {
            final Optional<String> serviceName = config.getServiceName(arg);

            if (!serviceName.isPresent()) {
                _LOGGER.error(ServiceMessages.SERVICE_UNKNOWN, arg);

                return false;
            }

            serviceNames.add(serviceName.get());
        }

        _loginInfo = Agent.getLoginInfo(config);

        for (final String serviceName: serviceNames) {
            if (!_stop(Require.notNull(serviceName))) {
                return false;
            }
        }

        return true;
    }

    private boolean _stop(final String serviceName)
    {
        _LOGGER.info(ServiceMessages.STOPPING_SERVICE, serviceName);

        final RegistryEntry registryEntry = RegistryEntry
            .newBuilder()
            .setName(Optional.of(serviceName))
            .setDefaultPrefix(Agent.REGISTRY_PREFIX)
            .setDefaultRegistryAddress(ServiceRegistry.getRegistryAddress())
            .setDefaultRegistryPort(ServiceRegistry.getRegistryPort())
            .build();

        if (registryEntry == null) {
            return false;
        }

        final AgentSessionProxy proxy = (AgentSessionProxy) AgentSessionProxy
            .newBuilder()
            .setRegistryEntry(registryEntry)
            .setLoginInfo(_loginInfo)
            .setAutoconnect(true)
            .setClientName(getClass().getName())
            .setClientLogger(_LOGGER)
            .build();

        if (proxy == null) {
            return false;
        }

        final ServiceActivatorMBean service;

        try {
            service = proxy.getMBeanProxy(ServiceActivatorMBean.class);
        } catch (final SessionException exception) {
            Throwable cause = exception;

            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            _LOGGER
                .error(
                    ServiceMessages.SERVICE_CONNECT_FAILED,
                    serviceName,
                    cause.getMessage());

            return false;
        }

        try {
            service.stop();
        } catch (final UndeclaredThrowableException exception) {
            // Ignores expected RemoteException caught by MBean Proxy.
        }

        _LOGGER.info(ServiceMessages.SERVICE_STOPPED, serviceName);

        proxy.tearDown();

        return true;
    }

    private static final Logger _LOGGER = Logger.getInstance(Stopper.class);

    private LoginInfo _loginInfo;
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
