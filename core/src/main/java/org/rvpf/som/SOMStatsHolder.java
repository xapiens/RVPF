/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMStatsHolder.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.som;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.Stats;
import org.rvpf.service.StatsHolder;

/**
 * SOM stats holder.
 */
@ThreadSafe
public final class SOMStatsHolder
    extends StatsHolder
{
    /**
     * Constructs an instance.
     *
     * @param somName The service name.
     */
    public SOMStatsHolder(@Nonnull final String somName)
    {
        _somName = somName;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<? extends Stats> getStats()
    {
        return Optional.of(_stats.clone());
    }

    /**
     * Sets the stats.
     *
     * @param stats The stats.
     */
    public void setStats(@Nonnull final SOMStats stats)
    {
        setObjectName(
            makeObjectName(
                getDefaultDomain(),
                stats.getObjectType(),
                Optional.of(_somName)));

        _stats = stats;
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateStats() {}

    private final String _somName;
    private volatile Stats _stats;
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
