/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceActivatorBaseMBean.java 3968 2019-05-09 12:54:51Z SFB $
 */

package org.rvpf.service;

import java.util.Optional;
import java.util.Properties;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Service activator base MBean interface.
 */
public interface ServiceActivatorBaseMBean
    extends StatsHolderMBean
{
    /**
     * Creates the service.
     *
     * @throws Exception As appropriate for the service.
     */
    void create()
        throws Exception;

    /**
     * Destroys the service.
     */
    void destroy();

    /**
     * Gets the MBean properties.
     *
     * @return The MBean properties.
     */
    @Nonnull
    @CheckReturnValue
    Properties getProperties();

    /**
     * Gets the value of a property.
     *
     * @param key The name of the property.
     *
     * @return The value of the property (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getProperty(@Nonnull String key);

    /**
     * Asks if the service is running.
     *
     * @return True if the service is running.
     */
    @CheckReturnValue
    boolean isRunning();

    /**
     * Asks if the service has been started.
     *
     * @return True if the service has been started.
     */
    @CheckReturnValue
    boolean isStarted();

    /**
     * Asks if the service has been stopped.
     *
     * @return True if the service has been started stopped.
     */
    @CheckReturnValue
    boolean isStopped();

    /**
     * Restarts the service.
     *
     * @throws Exception As appropriate for the service.
     */
    void restart()
        throws Exception;

    /**
     * Sets the MBean properties.
     *
     * @param properties The MBean properties.
     */
    void setProperties(@Nonnull Properties properties);

    /**
     * Sets a property.
     *
     * @param key The name of the property.
     * @param value The value of the property.
     */
    void setProperty(@Nonnull String key, @Nonnull String value);

    /**
     * Starts the service.
     *
     * @throws Exception As appropriate for the service.
     */
    void start()
        throws Exception;

    /**
     * Stops the service.
     */
    void stop();
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
