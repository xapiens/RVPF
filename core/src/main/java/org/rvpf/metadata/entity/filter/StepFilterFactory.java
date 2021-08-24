/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StepFilterFactory.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.metadata.entity.filter;

import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.value.filter.StepFilter;
import org.rvpf.base.value.filter.ValueFilter;

/**
 * Step filter factory.
 */
public class StepFilterFactory
    extends DeadbandFilterFactory
{
    /** {@inheritDoc}
     */
    @Override
    public ValueFilter newFilter(final Params params)
    {
        final double stepSize = params
            .getDouble(Point.STEP_SIZE_PARAM, 0.0);
        double ceilingGap;
        double floorGap;
        double ratio;

        ceilingGap = params.getDouble(Point.CEILING_GAP_PARAM, -1.0);
        ratio = params.getDouble(Point.CEILING_RATIO_PARAM, 0.5);

        if ((ratio > 0.0) && (ceilingGap < 0.0)) {
            ceilingGap = stepSize * ratio;
        }

        floorGap = params.getDouble(Point.FLOOR_GAP_PARAM, -1.0);
        ratio = params.getDouble(Point.FLOOR_RATIO_PARAM, 0.5);

        if ((ratio > 0.0) && (floorGap < 0.0)) {
            floorGap = stepSize * ratio;
        }

        return filter(
            new StepFilter(
                getTimeLimit(params),
                getStampTrimUnit(params),
                getDeadbandGap(params),
                getDeadbandRatio(params),
                stepSize,
                ceilingGap,
                floorGap));
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
