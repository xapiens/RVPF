/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LevelPolator.java 3893 2019-02-15 14:41:02Z SFB $
 */

package org.rvpf.store.server.polator;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.ExtrapolatedValue;
import org.rvpf.base.value.InterpolatedValue;
import org.rvpf.base.value.PointValue;

/**
 * Level polator.
 */
public final class LevelPolator
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
            decode(pointValuesBefore[0]));
    }

    /** {@inheritDoc}
     */
    @Override
    protected int extrapolationNeedsBefore()
    {
        return 1;
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
            decode(pointValuesBefore[0]));
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
