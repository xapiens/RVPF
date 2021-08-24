/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointRelation.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.sync.Sync;

/**
 * Point relation.
 *
 * <p>Used to access input relations for a result point.</p>
 */
public interface PointRelation
{
    /**
     * Gets the input point definition.
     *
     * @return The input point definition.
     */
    @Nonnull
    @CheckReturnValue
    Point getInputPoint();

    /**
     * Gets the params.
     *
     * @return The params.
     */
    @Nonnull
    @CheckReturnValue
    Params getParams();

    /**
     * Gets the result point definition.
     *
     * @return The result point definition.
     */
    @Nonnull
    @CheckReturnValue
    Point getResultPoint();

    /**
     * Gets a sync object for this.
     *
     * @return The optional sync object.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Sync> getSync();
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
