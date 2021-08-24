/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DeadbandFilterFactory.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.metadata.entity.filter;

import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.value.filter.DeadbandFilter;
import org.rvpf.base.value.filter.ValueFilter;

/**
 * Deadband filter factory.
 */
public class DeadbandFilterFactory
    extends ValueFilterFactory.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public ValueFilter newFilter(final Params params)
    {
        return filter(
            new DeadbandFilter(
                getTimeLimit(params),
                getStampTrimUnit(params),
                getDeadbandGap(params),
                getDeadbandRatio(params)));
    }

    /**
     * Gets the deadband gap.
     *
     * @param params The filter parameters.
     *
     * @return The deadband gap.
     */
    protected static final double getDeadbandGap(@Nonnull final Params params)
    {
        return params.getDouble(Point.DEADBAND_GAP_PARAM, -1.0);
    }

    /**
     * Gets the deadband ratio.
     *
     * @param params The filter parameters.
     *
     * @return The deadband ratio.
     */
    protected static final double getDeadbandRatio(@Nonnull final Params params)
    {
        return params.getDouble(Point.DEADBAND_RATIO_PARAM, -1.0);
    }
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
