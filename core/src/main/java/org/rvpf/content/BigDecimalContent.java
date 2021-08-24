/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BigDecimalContent.java 4055 2019-06-04 13:05:05Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Content;
import org.rvpf.base.Point;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;

/**
 * BigDecimal content converter.
 */
public final class BigDecimalContent
    extends NumberContent
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable decode(final PointValue pointValue)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable denormalize(final NormalizedValue normalizedValue)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public Content getInstance(final Point point)
    {
        final Integer scale;

        if (point.getParams().containsValueKey(SCALE_PARAM)) {
            scale = Integer.valueOf(point.getParams().getInt(SCALE_PARAM, 0));
        } else if (_defaultScale.isPresent()) {
            scale = _defaultScale.get();
        } else {
            getThisLogger().error(BaseMessages.MISSING_PARAMETER, SCALE_PARAM);

            return null;
        }

        _Instance instance = _instances.get(scale);

        if (instance == null) {
            instance = new _Instance(scale.intValue());
            _instances.put(scale, instance);
        }

        return instance;
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable normalize(final PointValue pointValue)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final Optional<String> multiplier = getParams()
            .getString(MULTIPLIER_PARAM);

        _multiplier = multiplier
            .isPresent()? Optional
                .of(new BigDecimal(multiplier.get())): Optional.empty();

        final boolean scalePresent = getParams().containsValueKey(SCALE_PARAM);

        _defaultScale = scalePresent? Optional
            .of(
                Integer
                    .valueOf(
                            getParams()
                                    .getInt(SCALE_PARAM, 0))): Optional.empty();

        _instances = new HashMap<Integer, _Instance>();

        return true;
    }

    Optional<BigDecimal> _getMultiplier()
    {
        return _multiplier;
    }

    /** Conversion factor. */
    public static final String MULTIPLIER_PARAM = "Multiplier";

    /** Param specifying the scale. */
    public static final String SCALE_PARAM = "Scale";

    private Optional<Integer> _defaultScale;
    private Map<Integer, _Instance> _instances;
    private Optional<BigDecimal> _multiplier;

    /**
     * Instance.
     */
    private final class _Instance
        extends AbstractContent
    {
        /**
         * Constructs an instance.
         *
         * @param scale The scale.
         */
        _Instance(final int scale)
        {
            _scale = scale;
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable decode(final PointValue pointValue)
        {
            return _getBigDecimal(pointValue);
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable denormalize(final NormalizedValue normalizedValue)
        {
            final Optional<BigDecimal> multiplier = _getMultiplier();
            BigDecimal bigDecimalValue = _getBigDecimal(normalizedValue);

            if ((bigDecimalValue != null) && multiplier.isPresent()) {
                bigDecimalValue = bigDecimalValue
                    .divide(multiplier.get(), RoundingMode.HALF_EVEN);
            }

            return bigDecimalValue;
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable normalize(final PointValue pointValue)
        {
            final Optional<BigDecimal> multiplier = _getMultiplier();
            BigDecimal bigDecimalValue = _getBigDecimal(pointValue);

            if ((bigDecimalValue != null) && multiplier.isPresent()) {
                bigDecimalValue = bigDecimalValue.multiply(multiplier.get());
            }

            return bigDecimalValue;
        }

        private BigDecimal _getBigDecimal(final PointValue pointValue)
        {
            final Serializable value = pointValue.getValue();
            BigDecimal bigDecimal;

            if (value instanceof BigDecimal) {
                bigDecimal = ((BigDecimal) value);
            } else if (value instanceof Double) {
                bigDecimal = BigDecimal
                    .valueOf(Math.round(((Double) value).doubleValue()));
            } else if (value instanceof Float) {
                bigDecimal = BigDecimal
                    .valueOf(Math.round(((Float) value).floatValue()));
            } else if (value instanceof Number) {
                bigDecimal = BigDecimal.valueOf(((Number) value).longValue());
            } else if (value instanceof String) {
                try {
                    bigDecimal = new BigDecimal((String) value);
                } catch (final NumberFormatException exception) {
                    bigDecimal = null;
                }
            } else if (value instanceof Boolean) {
                bigDecimal = BigDecimal
                    .valueOf(((Boolean) value).booleanValue()? 1: 0);
            } else {
                bigDecimal = null;
            }

            if (bigDecimal != null) {
                bigDecimal = bigDecimal
                    .setScale(_scale, RoundingMode.HALF_EVEN);
            } else if (value != null) {
                warnBadValue(pointValue);
            }

            return bigDecimal;
        }

        private final int _scale;
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
