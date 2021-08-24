/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StatsOwner.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.management.ObjectName;

/**
 * Stats owner.
 */
public interface StatsOwner
{
    /**
     * Gets the object name.
     *
     * @return The object name.
     */
    @Nonnull
    @CheckReturnValue
    ObjectName getObjectName();

    /**
     * Gets the object's version.
     *
     * @return The optional object's version.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getObjectVersion();

    /**
     * Called when the stats are updated.
     */
    void onStatsUpdated();

    /**
     * Updates the stats.
     */
    void updateStats();
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
