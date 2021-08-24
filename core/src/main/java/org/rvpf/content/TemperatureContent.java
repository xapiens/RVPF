/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TemperatureContent.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.content;

import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;

/**
 * Temperature content converter.
 *
 * <p>This content extends the {@link UnitValueContent} content converter by
 * adding a constant whose value comes from the 'Constant' param. This constant
 * is substracted during denormalization.</p>
 */
public final class TemperatureContent
    extends UnitValueContent
{
    /** {@inheritDoc}
     */
    @Override
    public Double denormalize(final NormalizedValue normalizedValue)
    {
        Double doubleValue = getDouble(normalizedValue);

        if (doubleValue != null) {
            doubleValue = Double.valueOf(doubleValue.doubleValue() - _constant);
            doubleValue = Double.valueOf(
                doubleValue.doubleValue() / getMultiplier());
        }

        return doubleValue;
    }

    /** {@inheritDoc}
     */
    @Override
    public Double normalize(final PointValue pointValue)
    {
        Double doubleValue = super.normalize(pointValue);

        if (doubleValue != null) {
            doubleValue = Double.valueOf(doubleValue.doubleValue() + _constant);
        }

        return doubleValue;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        super.setUp(metadata, proxyEntity);

        _constant = getParams().getDouble(CONSTANT_PARAM, 0.0);

        return true;
    }

    /**
     * Param specifying a number used to offset the Content during
     * normalization / denormalization.
     */
    public static final String CONSTANT_PARAM = "Constant";

    private double _constant;
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
