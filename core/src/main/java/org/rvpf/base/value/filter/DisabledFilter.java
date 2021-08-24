/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DisabledFilter.java 4003 2019-05-18 12:38:46Z SFB $
 */

package org.rvpf.base.value.filter;

import java.util.Optional;

import org.rvpf.base.value.PointValue;

/**
 * Disabled filter.
 */
public class DisabledFilter
    implements ValueFilter
{
    /** {@inheritDoc}
     */
    @Override
    public PointValue[] filter(final Optional<PointValue> pointValue)
    {
        return pointValue
            .isPresent()? new PointValue[] {pointValue.get(), }
                : _NO_POINT_VALUES;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isDisabled()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset() {}

    private static final PointValue[] _NO_POINT_VALUES = new PointValue[0];
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