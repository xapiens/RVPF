/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LinearPolator.java 3970 2019-05-09 19:35:44Z SFB $
 */

package org.rvpf.store.server.polator;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.ExtrapolatedValue;
import org.rvpf.base.value.InterpolatedValue;
import org.rvpf.base.value.PointValue;

/**
 * Linear inter/extra-polator.
 */
public final class LinearPolator
    extends Polator.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    protected PointValue extrapolate(
            final StoreValuesQuery polatedQuery,
            final DateTime stamp,
            final PointValue[] pointValuesBefore)
    {
        return new ExtrapolatedValue(
            polatedQuery.getPoint().get(),
            Optional.of(stamp),
            null,
            _polate(stamp, pointValuesBefore[0], pointValuesBefore[1]));
    }

    /** {@inheritDoc}
     */
    @Override
    protected int extrapolationNeedsBefore()
    {
        return 2;
    }

    /** {@inheritDoc}
     */
    @Override
    protected PointValue interpolate(
            final StoreValuesQuery polatedQuery,
            final DateTime stamp,
            final PointValue[] pointValuesBefore,
            final PointValue[] pointValuesAfter)
    {
        return new InterpolatedValue(
            polatedQuery.getPoint().get(),
            Optional.of(stamp),
            null,
            _polate(stamp, pointValuesBefore[0], pointValuesAfter[0]));
    }

    /** {@inheritDoc}
     */
    @Override
    protected int interpolationNeedsAfter()
    {
        return 1;
    }

    /** {@inheritDoc}
     */
    @Override
    protected int interpolationNeedsBefore()
    {
        return 1;
    }

    private static Double _polate(
            final DateTime stamp,
            final PointValue pointValue1,
            final PointValue pointValue2)
    {
        final Double value1 = decode(pointValue1);
        final Double value2 = decode(pointValue2);

        if ((value1 == null) || (value2 == null)) {
            return null;
        }

        final double x1 = Long
            .valueOf(pointValue1.getStamp().toRaw())
            .doubleValue();
        final double x2 = Long
            .valueOf(pointValue2.getStamp().toRaw())
            .doubleValue();
        final double x = Long.valueOf(stamp.toRaw()).doubleValue();
        final double y1 = value1.doubleValue();
        final double y2 = value2.doubleValue();
        final double y = y1 + ((y2 - y1) * (x - x1) / (x2 - x1));

        return Double.valueOf(y);
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
