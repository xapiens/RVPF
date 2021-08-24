/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceActivator.java 4081 2019-06-14 14:54:30Z SFB $
 */

package org.rvpf.service;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.Stats;
import org.rvpf.base.tool.Require;
import org.rvpf.config.Config;
import org.rvpf.service.rmi.ServiceRegistry;

/**
 * Service activator.
 *
 * <p>This class supports RVPF services implemented by a {@link ServiceImpl}.
 * </p>
 */
public abstract class ServiceActivator
    extends ServiceActivatorBase
    implements ServiceActivatorMBean
{
    /** {@inheritDoc}
     */
    @Override
    public void create()
        throws Exception
    {
        super.create();

        created();
    }

    /** {@inheritDoc}
     */
    @Override
    public void destroy()
    {
        super.destroy();

        ServiceRegistry.purge();

        destroyed();
    }

    /**
     * Gets the service config.
     *
     * @return The optional config.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Config> getConfig()
    {
        final Service service = _serviceImpl;

        return Optional
            .ofNullable((service != null)? _serviceImpl.getConfig(): null);
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getConfigURL()
    {
        return _configURL;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<String> getObjectVersion()
    {
        final Service serviceImpl = _serviceImpl;

        return (serviceImpl != null)? Optional
            .of(_serviceImpl.getVersion().getImplementationVersion()): Optional
                .empty();
    }

    /**
     * Gets the service.
     *
     * @return The service.
     */
    @Nonnull
    @CheckReturnValue
    public final Service getService()
    {
        return Require.notNull(_serviceImpl);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<Stats> getStats()
    {
        final Service service = _serviceImpl;

        return Optional.ofNullable((service != null)? service.getStats(): null);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isRunning()
    {
        final Service service = _serviceImpl;

        return (service != null) && service.isRunning();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isStarted()
    {
        final Service service = _serviceImpl;

        return (service != null) && service.isStarted();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isStopped()
    {
        final Service service = _serviceImpl;

        return (service == null) || service.isStopped();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isZombie()
    {
        final Service service = _serviceImpl;

        return (service != null) && service.isZombie();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setConfigURL(final String configURL)
    {
        _configURL = Require.notNull(configURL);

        final ServiceImpl serviceImpl = _serviceImpl;

        if (serviceImpl != null) {
            serviceImpl.setConfigURL(configURL);
        }

        getThisLogger().debug(ServiceMessages.CONFIG_URL, configURL);
    }

    /**
     * Sets the wait indicator.
     *
     * <p>This provides the default value for {@link #start}.</p>
     *
     * @param wait The wait indicator.
     */
    public final void setWait(final boolean wait)
    {
        _wait = wait;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
        throws Exception
    {
        start(_wait);
    }

    /**
     * Starts the service.
     *
     * @param wait True asks to wait until running.
     *
     * @throws Exception As appropriate for the service.
     */
    public final void start(final boolean wait)
        throws Exception
    {
        synchronized (this) {
            if (_serviceImpl != null) {
                getThisLogger()
                    .warn(
                        ServiceMessages.SERVICE_START_IGNORED,
                        getObjectName());

                return;
            }

            super.start();

            final ServiceImpl serviceImpl = createServiceImpl();

            final String configURL = _configURL;

            if (configURL != null) {
                serviceImpl.setConfigURL(configURL);
            }

            serviceImpl.putProperties(getProperties());
            serviceImpl.setServiceName(getObjectName().toString());

            _serviceImpl = serviceImpl;
        }

        _serviceImpl.startService(wait);

        if (isStarted()) {
            if (!wait || isRunning()) {
                if (_export) {
                    if (!getService().exportAgent()) {
                        getThisLogger()
                            .warn(
                                ServiceMessages.FAILED_AGENT_EXPORT,
                                getObjectName());
                    }
                }

                started();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        ServiceThread.yieldAll();

        synchronized (this) {
            if (isStopping()) {
                return;
            }

            super.stop();
        }

        if (isStopped()) {
            return;
        }

        final ServiceImpl serviceImpl = _serviceImpl;

        if (serviceImpl != null) {
            serviceImpl.stopService();
            _serviceImpl = null;
        }

        stopped();

        ServiceThread.yieldAll();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateStats()
    {
        final ServiceImpl serviceImpl = _serviceImpl;

        if (serviceImpl != null) {
            serviceImpl.updateStats();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean acceptMainArg(final String arg)
    {
        if (super.acceptMainArg(arg)) {
            return true;
        }

        if (_configURL == null) {
            setConfigURL(arg);

            return true;
        }

        return false;
    }

    /**
     * Creates the service implementation.
     *
     * <p>This must be implemented by the subclasses to provide the actual
     * service.</p>
     *
     * @return The service implementation.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract ServiceImpl createServiceImpl();

    /**
     * Creates a service implementation.
     *
     * @param serviceImplClass The class of the service implementation.
     *
     * @return The service implementation.
     */
    @Nonnull
    @CheckReturnValue
    protected final ServiceImpl createServiceImpl(
            @Nonnull final Class<? extends ServiceImpl> serviceImplClass)
    {
        return Require
            .notNull(createServiceImpl(new ClassDefImpl(serviceImplClass)));
    }

    /**
     * Creates a service implementation.
     *
     * @param classDef The class definition for the service implementation.
     *
     * @return The service implementation (null on failure).
     */
    @Nullable
    @CheckReturnValue
    protected final ServiceImpl createServiceImpl(
            @Nonnull final ClassDef classDef)
    {
        final ServiceClassLoader classLoader = ServiceClassLoader.getInstance();

        if (classDef.getInstanceClass(Optional.of(classLoader)) == null) {
            return null;
        }

        final ServiceImpl serviceImpl = classDef
            .createInstance(ServiceImpl.class);

        if (serviceImpl == null) {
            return null;
        }

        serviceImpl.setServiceActivatorBase(this);
        serviceImpl.setClassLoader(classLoader);

        return serviceImpl;
    }

    /** {@inheritDoc}
     */
    @Override
    protected final void export()
    {
        _export = true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void tearDownStandAlone()
    {
        ServiceImpl.cancelRestarters();

        super.tearDownStandAlone();
    }

    private static void _setSystemProperty(final String key, final String value)
    {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }

    static {
        // Disables automatic remote class loading by RMI.
        _setSystemProperty("java.rmi.server.useCodebaseOnly", "true");

        // Disables alternate protocols for RMI.
        _setSystemProperty("java.rmi.server.disableHttp", "true");

        // Asks for RMI random object identifiers.
        _setSystemProperty("java.rmi.server.randomIDs", "true");
    }

    private volatile String _configURL;
    private volatile boolean _export;
    private volatile ServiceImpl _serviceImpl;
    private volatile boolean _wait;
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
