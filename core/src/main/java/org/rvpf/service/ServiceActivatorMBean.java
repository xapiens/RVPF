/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceActivatorMBean.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.service;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service activator MBean interface.
 */
public interface ServiceActivatorMBean
    extends ServiceActivatorBaseMBean
{
    /**
     * Gets the Configuration URL.
     *
     * @return The configuration URL.
     */
    @Nullable
    @CheckReturnValue
    String getConfigURL();

    /**
     * Asks if the service is in a 'zombie' state.
     *
     * @return True if the service is in a 'zombie' state.
     */
    @CheckReturnValue
    boolean isZombie();

    /**
     * Sets the configuration file specification.
     *
     * @param configURL The configuration file specification.
     */
    void setConfigURL(@Nonnull String configURL);
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
