/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ContainerServiceAppImpl.java 4055 2019-06-04 13:05:05Z SFB $
 */

package org.rvpf.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceActivatorListener;
import org.rvpf.service.ServiceContext;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.app.ServiceAppImpl;

/**
 * Container service application implementation.
 */
final class ContainerServiceAppImpl
    extends ServiceAppImpl
{
    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Service service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        final Config config = service.getConfig();
        final KeyedGroups[] services = config
            .getPropertiesGroups(SERVICE_PROPERTIES);

        if (services.length == 0) {
            getThisLogger().error(ServiceMessages.NO_SERVICES);

            return false;
        }

        try {
            for (final KeyedGroups serviceProperties: services) {
                if (!_prepareService(serviceProperties, config)) {
                    return false;
                }
            }
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        final Collection<ServiceActivator> serviceActivators =
            new ArrayList<ServiceActivator>(
                _serviceActivators);
        final Service containerService = _getContainerService();

        for (final ServiceActivator serviceActivator: serviceActivators) {
            _startService(serviceActivator);

            if (_stopping || containerService.isStopping()) {
                break;
            }
        }

        if (_stopping) {
            stop();
            containerService.getServiceActivator().stop();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final LinkedList<ServiceActivator> serviceActivators =
            new LinkedList<ServiceActivator>(
                _serviceActivators);

        while (!serviceActivators.isEmpty()) {
            final ServiceActivator serviceActivator = serviceActivators
                .removeLast();

            if (serviceActivator.isStarted()) {
                serviceActivator.stop();
                _getContainerService().stopping();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        final LinkedList<ServiceActivator> serviceActivators =
            new LinkedList<ServiceActivator>(
                _serviceActivators);

        while (!serviceActivators.isEmpty()) {
            serviceActivators.removeLast().destroy();
        }

        _serviceActivators.clear();
        _serviceActivatorMap.clear();

        super.tearDown();
    }

    /**
     * Gets the container service.
     *
     * @return The container service.
     */
    Service _getContainerService()
    {
        return super.getService();
    }

    /**
     * Starts a service.
     *
     * @param service The service.
     */
    void _startService(final ServiceActivator service)
    {
        boolean started;

        try {
            service.start();
            started = service.isStarted();
        } catch (final Exception exception) {
            started = false;
        }

        if (!started) {
            getThisLogger().warn(ServiceMessages.SERVICE_START_FAILED, service);
            _stopped(service);
        }
    }

    /**
     * Informs that a service has stopped.
     *
     * @param service The service.
     */
    void _stopped(final ServiceActivator service)
    {
        final Service containerService = _getContainerService();

        if (_serviceActivators.remove(service)) {
            if (!_optionals.remove(service) && !containerService.isStopping()) {
                getThisLogger()
                    .warn(ServiceMessages.SERVICE_NOT_OPTIONAL, service);

                if (containerService.isRunning()) {
                    containerService.getServiceActivator().stop();
                } else {
                    _stopping = true;
                }
            } else if (_serviceActivators.isEmpty()
                       && !containerService.isStopping()) {
                getThisLogger().info(ServiceMessages.NO_SERVICES_RUNNING);
                containerService.getServiceActivator().stop();
            }
        }
    }

    /**
     * Gets the service activator for a service name or alias.
     *
     * @param serviceKey The service name or alias.
     *
     * @return The service activator (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    Optional<ServiceActivator> getServiceActivator(
            @Nonnull final String serviceKey)
    {
        return Optional
            .ofNullable(
                _serviceActivatorMap.get(serviceKey.toUpperCase(Locale.ROOT)));
    }

    private boolean _prepareService(
            final KeyedGroups serviceProperties,
            final Config containerConfig)
        throws Exception
    {
        final Optional<ClassDef> classDef = serviceProperties
            .getClassDef(SERVICE_CLASS_PROPERTY, Optional.empty());

        if (!classDef.isPresent()) {
            getThisLogger()
                .error(
                    ServiceMessages.MISSING_SERVICE_PROPERTY,
                    SERVICE_CLASS_PROPERTY);

            return false;
        }

        final ServiceActivator service = classDef
            .get()
            .createInstance(ServiceActivator.class);

        if (service == null) {
            return false;
        }

        service
            .setObjectNameProperty(
                Optional.empty());    // Avoids own name propagation.

        final Optional<String> name = serviceProperties
            .getString(NAME_PROPERTY);

        if (name.isPresent()) {
            service.setObjectName(service.makeObjectName(name));
            getThisLogger()
                .debug(ServiceMessages.SERVICE_NAME, service.getObjectName());
        }

        service.create();

        final Optional<String> configURL = serviceProperties
            .getString(CONFIG_PROPERTY);

        if (configURL.isPresent()) {
            service.setConfigURL(configURL.get());
        }

        final boolean wait = serviceProperties.getBoolean(WAIT_PROPERTY);

        service.setWait(wait | _getContainerService().isWait());
        service.setListener(new _Listener(service));

        _serviceActivators.add(service);

        final boolean optional = serviceProperties
            .getBoolean(OPTIONAL_PROPERTY);

        if (optional) {
            _optionals.add(service);
        }

        final String serviceName = service.getObjectName().toString();

        _serviceActivatorMap.put(serviceName.toUpperCase(Locale.ROOT), service);

        final Optional<ServiceContext> serviceContext = containerConfig
            .getServiceContext(serviceName);

        if (!serviceContext.isPresent()) {
            getThisLogger()
                .warn(ServiceMessages.SERVICE_NOT_CONFIGURED, serviceName);

            return false;
        }

        for (final String alias: serviceContext.get().getServiceAliases()) {
            _serviceActivatorMap.put(alias.toUpperCase(Locale.ROOT), service);
        }

        getThisLogger().info(ServiceMessages.SERVICE_PREPARED, service);

        return true;
    }

    /**
     * Specification of the configuration file to use for the service. Defaults
     * to the configuration file used by the container service.
     */
    public static final String CONFIG_PROPERTY = "config";

    /** Name of the service. */
    public static final String NAME_PROPERTY = "name";

    /**
     * Optional service: if it fails to start or terminates prematurely, other
     * services may still run.
     */
    public static final String OPTIONAL_PROPERTY = "optional";

    /** The class of the service to activate. */
    public static final String SERVICE_CLASS_PROPERTY = "service.class";

    /** Properties used to declare a contained service. */
    public static final String SERVICE_PROPERTIES = "service";

    /** Wait until the service is running before starting the next service. */
    public static final String WAIT_PROPERTY = "wait";

    private final Set<ServiceActivator> _optionals =
        new HashSet<ServiceActivator>();
    private final Map<String, ServiceActivator> _serviceActivatorMap =
        new HashMap<String, ServiceActivator>();
    private final Set<ServiceActivator> _serviceActivators =
        new LinkedHashSet<ServiceActivator>();
    private boolean _stopping;

    /**
     * _Listener.
     */
    private final class _Listener
        implements ServiceActivatorListener
    {
        /**
         * Constructs an instance.
         *
         * @param service The child service.
         */
        _Listener(final ServiceActivator service)
        {
            _service = service;
        }

        /** {@inheritDoc}
         */
        @Override
        public void restart(final Optional<ElapsedTime> delay)
        {
            _restarting = true;
            _service.stop();

            if (delay.isPresent()) {
                try {
                    Thread.sleep(delay.get().toMillis());
                } catch (final InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
            }

            _restarting = false;

            _startService(_service);
        }

        /** {@inheritDoc}
         */
        @Override
        public void starting(final Optional<ElapsedTime> waitHint)
        {
            final Service containerService = _getContainerService();

            if (!containerService.isStarted()) {
                containerService.starting(waitHint);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void stopped()
        {
            if (!_restarting) {
                ContainerServiceAppImpl.this._stopped(_service);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void stopping(final Optional<ElapsedTime> waitHint)
        {
            final Service containerService = _getContainerService();

            if (!containerService.isStopped()) {
                containerService.stopping(waitHint);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void terminate()
        {
            _service.stop();

            Logger
                .getInstance(getClass())
                .info(ServiceMessages.SERVICE_TERMINATED, _service);
        }

        private boolean _restarting;
        private final ServiceActivator _service;
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
