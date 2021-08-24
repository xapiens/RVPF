/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMStats.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.som;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.StatsOwner;
import org.rvpf.config.Config;
import org.rvpf.jmx.Agent;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceStats;
import org.rvpf.service.StatsHolder;

/**
 * SOM stats.
 */
public abstract class SOMStats
    extends ServiceStats
{
    /**
     * Constructs an instance.
     *
     * @param statsOwner The stats owner.
     */
    protected SOMStats(@Nonnull final StatsOwner statsOwner)
    {
        super(statsOwner);

        ((SOMStatsHolder) getStatsOwner()).setStats(this);
    }

    /**
     * Gets the JMX object type.
     *
     * @return The JMX object type.
     */
    @Nonnull
    @CheckReturnValue
    public abstract String getObjectType();

    /**
     * Registers with the JMX agent.
     *
     * @param config The configuration.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public final boolean register(@Nonnull final Config config)
    {
        if (config.hasService()) {
            final Service service = config.getService();

            service.addStats(this);
            _registrationEnabled = service.isJMXRegistrationEnabled();

            if (_registrationEnabled) {
                return Agent
                    .getInstance()
                    .registerStatsHolder((StatsHolder) getStatsOwner());
            }
        }

        return true;
    }

    /**
     * Unregisters with the JMX agent.
     */
    public final void unregister()
    {
        if (_registrationEnabled) {
            Agent
                .getInstance()
                .unregisterStatsHolder((StatsHolder) getStatsOwner());
            _registrationEnabled = false;
        }
    }

    private static final long serialVersionUID = 1L;

    private boolean _registrationEnabled;
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
