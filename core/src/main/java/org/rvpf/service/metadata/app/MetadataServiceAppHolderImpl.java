/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataServiceAppHolderImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.service.metadata.app;

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
import org.rvpf.metadata.Metadata;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceStats;
import org.rvpf.service.ServiceThread;
import org.rvpf.service.metadata.MetadataServiceImpl;

/**
 * Metadata service application holder implementation.
 */
public class MetadataServiceAppHolderImpl
    extends MetadataServiceImpl
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getEntityName()
    {
        return getMetadataServiceApp().getEntityName();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onAlert(final Optional<Alert> alert)
    {
        if (!super.onAlert(alert)) {
            return false;
        }

        final MetadataServiceApp metadataServiceApp = _metadataServiceApp;

        return (metadataServiceApp != null)? metadataServiceApp
            .onAlert(alert.get()): false;
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceStats createStats(final StatsOwner statsOwner)
    {
        return (_metadataServiceApp != null)? _metadataServiceApp
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
                    getMetadataServiceApp().onServicesReady();
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
        getMetadataServiceApp().start();
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
            getMetadataServiceApp().stop();
            ServiceThread.yieldAll();
        }
    }

    /**
     * Gets the metadata service application.
     *
     * @return The metadata service application.
     */
    @Nonnull
    @CheckReturnValue
    protected MetadataServiceApp getMetadataServiceApp()
    {
        return Require.notNull(_metadataServiceApp);
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
    protected MetadataServiceApp newMetadataServiceApp()
    {
        final ClassDef classDef = getServiceAppClassDef();

        return classDef.createInstance(MetadataServiceApp.class);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean onEvent(final Event event)
    {
        final MetadataServiceApp serviceApp = _metadataServiceApp;

        return (serviceApp != null)? serviceApp.onEvent(event): false;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean onNewMetadata(final Metadata metadata)
    {
        if (!getMetadataServiceApp().onNewMetadata(metadata)) {
            return false;
        }

        return super.onNewMetadata(metadata);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void onServicesNotReady()
    {
        synchronized (getMutex()) {
            if (_servicesReady) {
                final MetadataServiceApp serviceApp = _metadataServiceApp;

                _servicesReady = false;

                if (serviceApp != null) {
                    serviceApp.onServicesNotReady();
                }

                super.onServicesNotReady();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean onSignal(final Signal signal)
    {
        final MetadataServiceApp serviceApp = _metadataServiceApp;

        if ((serviceApp != null) && !serviceApp.onSignal(signal)) {
            return false;
        }

        return super.onSignal(signal);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp()
    {
        if (!super.setUp()) {
            return false;
        }

        _metadataServiceApp = newMetadataServiceApp();

        if ((_metadataServiceApp == null) || !_metadataServiceApp.setUp(this)) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void tearDown()
    {
        final MetadataServiceApp serviceApp = _metadataServiceApp;

        if (serviceApp != null) {
            serviceApp.tearDown();
            _metadataServiceApp = null;
        }

        super.tearDown();
    }

    /** The class of the service application to activate. */
    public static final String SERVICE_APP_CLASS_PROPERTY = "service.app.class";

    private volatile MetadataServiceApp _metadataServiceApp;
    private boolean _servicesReady;
    private volatile boolean _started;
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
