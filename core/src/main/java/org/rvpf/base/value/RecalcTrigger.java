/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RecalcTrigger.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.value;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;

/**
 * Recalc trigger.
 */
@NotThreadSafe
public final class RecalcTrigger
    extends PointValue
{
    /**
     * Constructs an instance.
     *
     * <p>This is needed for an Externalizable implementation.</p>
     */
    public RecalcTrigger() {}

    /**
     * Constructs an instance.
     *
     * @param point The point definition.
     * @param stamp The optional time stamp of the value.
     */
    public RecalcTrigger(
            @Nonnull final Point point,
            @Nonnull final Optional<DateTime> stamp)
    {
        super(point, stamp, null, null);
    }

    private RecalcTrigger(@Nonnull final RecalcTrigger other)
    {
        super(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public RecalcTrigger copy()
    {
        return new RecalcTrigger(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public String valueString()
    {
        return "recalc";
    }

    private static final long serialVersionUID = 1L;
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
