/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValveServiceActivatorMBean.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.valve;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/** Valve MBean interface.
 */
public interface ValveServiceActivatorMBean
    extends org.rvpf.service.ServiceActivatorBaseMBean
{
    /** Gets the environment properties file path.
     *
     * @return The environment properties file path.
     */
    @Nonnull
    @CheckReturnValue
    String getEnvPath();

    /** Asks if the controlled ports are paused.
     *
     * @return True if paused.
     */
    @CheckReturnValue
    boolean isPaused();

    /** Sets the environment properties file path.
     *
     * @param envPath The environment properties file path.
     */
    void setEnvPath(String envPath);
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
