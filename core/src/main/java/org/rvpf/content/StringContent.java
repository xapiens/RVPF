/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StringContent.java 3936 2019-04-28 19:25:00Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;

/**
 * String content converter.
 *
 * <p>This content is used for character data. It will accept any value by
 * calling its 'toString' method.</p>
 */
public final class StringContent
    extends AbstractContent
{
    /** {@inheritDoc}
     */
    @Override
    public String decode(final PointValue pointValue)
    {
        return _getString(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public String denormalize(final NormalizedValue normalizedValue)
    {
        return _getString(normalizedValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public String normalize(final PointValue pointValue)
    {
        return _getString(pointValue);
    }

    private static String _getString(final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        return (value != null)? value.toString(): null;
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
