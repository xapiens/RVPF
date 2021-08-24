/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceAppImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.service.app;

import java.io.File;

import java.util.Optional;
import java.util.Timer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.StatsOwner;
import org.rvpf.base.UUID;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceStats;

/**
 * Service application implementation.
 *
 * <p>This abstract class supplies a minimal implementation for the service
 * application interface. It also supplies help methods (protected) to its
 * subclasses.</p>
 */
public abstract class ServiceAppImpl
    implements ServiceApp
{
    /** {@inheritDoc}
     */
    @Override
    public ServiceStats createStats(final StatsOwner statsOwner)
    {
        return new ServiceStats(statsOwner);
    }

    /**
     * Fails.
     */
    public final void fail()
    {
        _service.fail();
    }

    /**
     * Gets the service config.
     *
     * @return The config.
     */
    @Nonnull
    @CheckReturnValue
    public final Config getConfig()
    {
        return _service.getConfig();
    }

    /**
     * Gets the config properties.
     *
     * @return The config properties.
     */
    @Nonnull
    @CheckReturnValue
    public KeyedGroups getConfigProperties()
    {
        return getConfig().getProperties();
    }

    /**
     * Gets the data directory.
     *
     * @return The data directory.
     */
    @Nonnull
    @CheckReturnValue
    public final File getDataDir()
    {
        return _service.getDataDir();
    }

    /**
     * Gets the join timeout.
     *
     * @return The interrupt timeout.
     */
    @CheckReturnValue
    public long getJoinTimeout()
    {
        return _service.getJoinTimeout();
    }

    /**
     * Gets the application specific properties.
     *
     * <p>These properties are contained in the non validated group
     * '{@value #SERVICE_APP_PROPERTIES}'.</p>
     *
     * <p>Since this method is usually called during the set up of subclasses,
     * they must call the set up of their superclass first.</p>
     *
     * @return The application specific properties.
     */
    @Nonnull
    @CheckReturnValue
    public KeyedGroups getProperties()
    {
        return _service.getConfig().getPropertiesGroup(SERVICE_APP_PROPERTIES);
    }

    /**
     * Gets the service holding this application.
     *
     * @return The service.
     */
    @Nonnull
    @CheckReturnValue
    public Service getService()
    {
        return Require.notNull(_service);
    }

    /**
     * Gets the source UUID.
     *
     * @return The source UUID (may be generated).
     */
    @Nonnull
    @CheckReturnValue
    public UUID getSourceUUID()
    {
        return _service.getSourceUUID();
    }

    /**
     * Gets the service timer.
     *
     * @return The optional service timer.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Timer> getTimer()
    {
        return _service.getTimer();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onAlert(final Alert alert)
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onEvent(final Event event)
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void onServicesNotReady() {}

    /** {@inheritDoc}
     */
    @Override
    public void onServicesReady() {}

    /** {@inheritDoc}
     */
    @Override
    public boolean onSignal(final Signal signal)
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Service service)
    {
        _service = service;

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start() {}

    /** {@inheritDoc}
     */
    @Override
    public void stop() {}

    /** {@inheritDoc}
     */
    @Override
    public void tearDown() {}

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
     * Called on service not available exception.
     *
     * @param exception The service not available exception.
     */
    protected void onServiceNotAvailableException(
            @Nonnull final ServiceNotAvailableException exception)
    {
        if (exception.getCause() != null) {
            Throwable cause = exception.getCause();

            if (cause instanceof StoreAccessException) {
                do {
                    cause = cause.getCause();
                } while (cause.getCause() != null);
            }

            getThisLogger().error(cause, ServiceMessages.RESTART_NEEDED);
        } else {
            getThisLogger().warn(ServiceMessages.RESTART_NEEDED);
        }

        _service.restart(true);
    }

    /** The properties for the service application. */
    public static final String SERVICE_APP_PROPERTIES =
        "service.app.properties";

    private final Logger _logger = Logger.getInstance(getClass());
    private Service _service;
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
