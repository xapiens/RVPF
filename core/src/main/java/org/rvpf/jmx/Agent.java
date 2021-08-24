/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Agent.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.jmx;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanServer;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.util.LoginInfo;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.StatsHolder;
import org.rvpf.service.rmi.ServiceRegistry;
import org.rvpf.service.rmi.SessionFactoryImpl;
import org.rvpf.service.rmi.SessionSecurityContext;

/**
 * JMX agent.
 *
 * <p>Used to register a {@link ServiceActivator} with a {@link MBeanServer}.
 * </p>
 *
 * <p>Also used to export an {@link AgentSessionFactory} bound to the RMI
 * registry under the name of a {@link ServiceActivator}.</p>
 *
 * <p>Implemented as a singleton, it will be shared by {@link ServiceActivator}s
 * running together.</p>
 */
public final class Agent
{
    private Agent() {}

    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance.
     */
    @Nonnull
    @CheckReturnValue
    public static Agent getInstance()
    {
        return _INSTANCE;
    }

    /**
     * Gets login informations.
     *
     * @param config The configuration.
     *
     * @return The login informations.
     */
    @Nonnull
    @CheckReturnValue
    public static LoginInfo getLoginInfo(@Nonnull final Config config)
    {
        final KeyedGroups jmxProperties = config
            .getPropertiesGroup(JMX_PROPERTIES);

        if (jmxProperties.isMissing()) {
            return new LoginInfo(Optional.empty(), Optional.empty());
        }

        return new LoginInfo(
            jmxProperties.getString(USER_PROPERTY),
            jmxProperties.getPassword(PASSWORD_PROPERTY));
    }

    /**
     * Asks if automatic registration of standalone services is enabled.
     *
     * @param config The config.
     *
     * @return True if registration is enabled.
     */
    @CheckReturnValue
    public static boolean isRegistrationEnabled(@Nonnull final Config config)
    {
        final KeyedGroups jmxProperties = config
            .getPropertiesGroup(JMX_PROPERTIES);

        return (jmxProperties != null)
               && jmxProperties.getBoolean(REGISTRATION_ENABLED_PROPERTY);
    }

    /**
     * Exports the {@link AgentSessionFactory} under the name of a service.
     *
     * @param serviceActivator The service activator.
     *
     * @return A true value on success.
     */
    @CheckReturnValue
    public synchronized boolean exportService(
            @Nonnull final ServiceActivator serviceActivator)
    {
        final String name = serviceActivator.getObjectName().toString();

        if (!ServiceRegistry
            .getInstance()
            .register(_sessionFactory, REGISTRY_PREFIX + name, _LOGGER)) {
            return false;
        }

        _LOGGER.info(ServiceMessages.JMX_AGENT_EXPORTED, name);

        return true;
    }

    /**
     * Registers a service.
     *
     * @param serviceActivator The service activator.
     *
     * @return A true value on success.
     */
    @CheckReturnValue
    public synchronized boolean registerService(
            @Nonnull final ServiceActivator serviceActivator)
    {
        return setUp(serviceActivator.getConfig().get())
               && registerStatsHolder(serviceActivator);
    }

    /**
     * Registers a stats holder.
     *
     * @param statsHolder The stats holder.
     *
     * @return A true value on success.
     */
    @CheckReturnValue
    public synchronized boolean registerStatsHolder(
            @Nonnull final StatsHolder statsHolder)
    {
        try {
            _sessionFactory
                .getServer()
                .registerMBean(statsHolder, statsHolder.getObjectName());
        } catch (final InstanceAlreadyExistsException exception) {
            _LOGGER
                .warn(
                    ServiceMessages.OBJECT_NAME_COLLISION,
                    statsHolder.getObjectName());

            return false;
        } catch (final JMException exception) {
            throw new RuntimeException(exception);
        }

        _LOGGER
            .info(
                ServiceMessages.JMX_OBJECT_REGISTERED,
                statsHolder.getObjectName());

        return true;
    }

    /**
     * Sets up the agent.
     *
     * <p>The first call will perform the initializations. Further calls will be
     * ignored.</p>
     *
     * @param config The config.
     *
     * @return A true value on success or when ignored.
     */
    @CheckReturnValue
    public boolean setUp(@Nonnull final Config config)
    {
        if (_sessionFactory == null) {
            final KeyedGroups jmxProperties = config
                .getPropertiesGroup(JMX_PROPERTIES);
            final SessionSecurityContext securityContext = SessionFactoryImpl
                .createSecurityContext(
                    config.getProperties(),
                    jmxProperties.getGroup(SecurityContext.SECURITY_PROPERTIES),
                    _LOGGER);

            if (securityContext == null) {
                return false;
            }

            final AgentSessionFactoryImpl sessionFactory =
                new AgentSessionFactoryImpl(
                    securityContext);

            if (!sessionFactory
                .setUp(config, jmxProperties.getBoolean(PLATFORM_PROPERTY))) {
                return false;
            }

            _sessionFactory = sessionFactory;
            _LOGGER
                .debug(
                    ServiceMessages.AGENT_FACTORY_OPENED,
                    config.getServiceName());
        }

        return true;
    }

    /**
     * Tears down what has been set up.
     *
     * <p>Calls with a config other than the one used on the first call to
     * {@link #setUp} will be ignored.</p>
     *
     * @param config The config that was passed to {@link #setUp}.
     */
    public void tearDown(@Nonnull final Config config)
    {
        final AgentSessionFactoryImpl sessionFactory = _sessionFactory;

        if ((sessionFactory != null)
                && (config == sessionFactory.getConfig())) {
            _sessionFactory = null;
            sessionFactory.tearDown();
            _LOGGER.debug(ServiceMessages.AGENT_FACTORY_CLOSED);
        }
    }

    /**
     * Unexports a service.
     *
     * @param serviceActivator The service activator.
     */
    public synchronized void unexportService(
            @Nonnull final ServiceActivator serviceActivator)
    {
        final AgentSessionFactoryImpl sessionFactory = _sessionFactory;

        if (sessionFactory == null) {
            _LOGGER
                .warn(
                    ServiceMessages.UNEXPORT_OUT_OF_ORDER,
                    serviceActivator.getObjectName());

            return;
        }

        ServiceRegistry
            .getInstance()
            .unregister(
                REGISTRY_PREFIX + serviceActivator.getObjectName(),
                _LOGGER);
    }

    /**
     * Unregisters a service.
     *
     * @param serviceActivator The service activator.
     */
    public synchronized void unregisterService(
            @Nonnull final ServiceActivator serviceActivator)
    {
        unregisterStatsHolder(serviceActivator);

        tearDown(serviceActivator.getConfig().get());
    }

    /**
     * Unregisters a stats holder.
     *
     * @param statsHolder The stats holder.
     */
    public synchronized void unregisterStatsHolder(
            @Nonnull final StatsHolder statsHolder)
    {
        final AgentSessionFactoryImpl sessionFactory = _sessionFactory;

        if (sessionFactory == null) {
            _LOGGER
                .warn(
                    ServiceMessages.UNREGISTRATION_OUT_OF_ORDER,
                    statsHolder.getObjectName());

            return;
        }

        try {
            sessionFactory
                .getServer()
                .unregisterMBean(statsHolder.getObjectName());
        } catch (final JMException exception) {
            _LOGGER
                .warn(
                    ServiceMessages.UNREGISTRATION_FAILED,
                    statsHolder.getObjectName());

            return;
        }

        _LOGGER
            .info(
                ServiceMessages.UNREGISTERED_JMX,
                statsHolder.getObjectName());
    }

    /** JMX properties. */
    public static final String JMX_PROPERTIES = "jmx";

    /** Login password property. */
    public static final String PASSWORD_PROPERTY = "password";

    /** Platform property. */
    public static final String PLATFORM_PROPERTY = "platform";

    /** Enables automatic registration of standalone services. */
    public static final String REGISTRATION_ENABLED_PROPERTY =
        "registration.enabled";

    /** The prefix used for registry names. */
    public static final String REGISTRY_PREFIX = "service/";

    /** The role needed for connection to the agent. */
    public static final String ROLE_CONNECT_PROPERTY = "role.connect";

    /** Login user property. */
    public static final String USER_PROPERTY = "user";
    private static final Agent _INSTANCE = new Agent();
    private static final Logger _LOGGER = Logger.getInstance(Agent.class);

    private volatile AgentSessionFactoryImpl _sessionFactory;
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
