/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceAppHolderImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.service.app;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.ClassDef;
import org.rvpf.base.StatsOwner;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.tool.Require;
import org.rvpf.service.ServiceImpl;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceStats;
import org.rvpf.service.ServiceThread;

/**
 * Service application holder implementation.
 *
 * <p>A service application holder supports a single service application.</p>
 */
public class ServiceAppHolderImpl
    extends ServiceImpl
{
    /**
     * Gets the service application.
     *
     * @return The service application.
     */
    @Nonnull
    @CheckReturnValue
    public ServiceApp getServiceApp()
    {
        return Require.notNull(_serviceApp);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onAlert(final Optional<Alert> alert)
    {
        if (!super.onAlert(alert)) {
            return false;
        }

        final ServiceApp serviceApp = _serviceApp;

        return (serviceApp == null) || serviceApp.onAlert(alert.get());
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceStats createStats(final StatsOwner statsOwner)
    {
        return (_serviceApp != null)? _serviceApp
            .createStats(statsOwner): super.createStats(statsOwner);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean doPendingActions()
        throws InterruptedException, ServiceNotAvailableException
    {
        synchronized (getMutex()) {
            if (super.doPendingActions()) {
                if (!_servicesReady) {
                    _servicesReady = true;
                    getServiceApp().onServicesReady();
                }

                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doStart()
        throws Exception
    {
        Require.failure(_started);

        getServiceApp().start();
        _started = true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doStop()
        throws Exception
    {
        if (_started) {
            _started = false;
            getServiceApp().stop();
            ServiceThread.yieldAll();
        }
    }

    /**
     * Gets the service application class definition.
     *
     * @return The service application class definition (null on failure).
     */
    @Nullable
    @CheckReturnValue
    protected ClassDef getServiceAppClassDef()
    {
        final Optional<? extends ClassDef> classDef = getConfig()
            .getClassDef(SERVICE_APP_CLASS_PROPERTY, Optional.empty());

        if (!classDef.isPresent()) {
            getThisLogger()
                .error(
                    ServiceMessages.MISSING_SERVICE_PROPERTY,
                    SERVICE_APP_CLASS_PROPERTY);
        }

        return classDef.orElse(null);
    }

    /**
     * Creates a new service application.
     *
     * @return The service application (null on failure).
     */
    @Nullable
    @CheckReturnValue
    protected ServiceApp newServiceApp()
    {
        final ClassDef classDef = getServiceAppClassDef();

        return classDef.createInstance(ServiceApp.class);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean onEvent(final Event event)
    {
        return getServiceApp().onEvent(event);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void onServicesNotReady()
    {
        if (_servicesReady) {
            _servicesReady = false;
            getServiceApp().onServicesNotReady();

            super.onServicesNotReady();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean onSignal(final Signal signal)
    {
        return getServiceApp().onSignal(signal);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp()
    {
        if (!super.setUp()) {
            return false;
        }

        _serviceApp = newServiceApp();

        if (_serviceApp == null) {
            return false;
        }

        return _serviceApp.setUp(this);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void tearDown()
    {
        if (_serviceApp != null) {
            _serviceApp.tearDown();
            _serviceApp = new ServiceAppImpl() {}
            ;
        }

        super.tearDown();
    }

    /** The class of the service application to activate. */
    public static final String SERVICE_APP_CLASS_PROPERTY = "service.app.class";

    private volatile ServiceApp _serviceApp;
    private boolean _servicesReady;
    private boolean _started;
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
