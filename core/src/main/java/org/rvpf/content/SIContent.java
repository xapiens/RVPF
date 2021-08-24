/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SIContent.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Content;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.service.ServiceMessages;

/**
 * SI content converter.
 *
 * <p>Instances of this class implements <a
 * href="http://physics.nist.gov/cuu/Units/">SI</a> base units and derived units
 * with special names. Using these instances avoids the specification of a large
 * number of content definitions in the metadata.</p>
 *
 * <p>Each point using this content will have a 'Unit' param holding an optional
 * SI prefix and a mandatory SI symbol.</p>
 *
 * <p>This implementation keeps a cache of the generated instances keyed by the
 * value multiplier.</p>
 */
public final class SIContent
    extends FloatingPointContent
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
    public Double denormalize(final NormalizedValue normalizedValue)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public Content getInstance(final Point point)
    {
        final Optional<String> unit = point.getParams().getString(UNIT_PARAM);

        if (!unit.isPresent()) {
            getThisLogger().error(BaseMessages.MISSING_PARAMETER, UNIT_PARAM);

            return null;
        }

        String base = unit.get();
        Double multiplier = Double.valueOf(1.0);

        if (!_units.contains(base)) {
            String prefix = base.substring(0, 1);

            base = base.substring(1);

            if ("d".equals(prefix) && base.startsWith("a")) {
                prefix += "a";
                base = base.substring(1);
            }

            multiplier = _multipliers.get(prefix);

            if ((multiplier == null) || !_units.contains(base)) {
                getThisLogger()
                    .error(ServiceMessages.UNRECOGNIZED_UNIT, unit.get());

                return null;
            }
        }

        if ("g".equals(base)) {
            multiplier = Double.valueOf(multiplier.doubleValue() / 10.0e+3);
        }

        _Instance instance = _instances.get(multiplier);

        if (instance == null) {
            instance = new _Instance(multiplier.doubleValue());
            _instances.put(multiplier, instance);
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

        _units = new HashSet<String>(Arrays.asList(_UNITS));

        Require.success(_PREFIXES.length == _MULTIPLIERS.length);
        _multipliers = new HashMap<String, Double>();

        for (int i = 0; i < _PREFIXES.length; ++i) {
            _multipliers.put(_PREFIXES[i], Double.valueOf(_MULTIPLIERS[i]));
        }

        _multipliers.put("\u00b5", _multipliers.get("u"));    // Greek letter mu.

        _instances = new HashMap<Double, _Instance>();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _instances = null;
        _multipliers = null;
        _units = null;

        super.tearDown();
    }

    /**
     * Param holding the abbreviation of a base or derived unit, including an
     * optional prefix.
     */
    public static final String UNIT_PARAM = "Unit";

    /**  */

    private static final double[] _MULTIPLIERS = {10.0e+24, 10.0e+21, 10.0e+18,
            10.0e+15, 10.0e+12, 10.0e+9, 10.0e+6, 10.0e+3, 10.0e+2, 10.0e+1,
            10.0e-1, 10.0e-2, 10.0e-3, 10.0e-6, 10.0e-9, 10.0e-12, 10.0e-15,
            10.0e-18, 10.0e-21, 10.0e-24, };
    private static final String[] _PREFIXES = {"Y", "Z", "E", "P", "T", "G",
            "M", "k", "h", "da", "d", "c", "m", "u", "n", "p", "f", "a", "z",
            "y", };
    private static final String[] _UNITS = {"m", "g", "s", "A", "K", "mol",
            "cd", "rad", "sr", "Hz", "N", "Pa", "J", "W", "C", "V", "F", "O",
            "S", "Wb", "T", "H", "lm", "lx", "Bq", "Gy", "Sv", "kat", };

    private Map<Double, _Instance> _instances;
    private Map<String, Double> _multipliers;
    private Set<String> _units;

    /**
     * Instance.
     */
    private static final class _Instance
        extends DoubleContent
    {
        /**
         * Constructs an instance.
         *
         * @param multiplier The multiplier.
         */
        _Instance(final double multiplier)
        {
            _multiplier = multiplier;
        }

        /** {@inheritDoc}
         */
        @Override
        public Double denormalize(final NormalizedValue normalizedValue)
        {
            Double doubleValue = super.denormalize(normalizedValue);

            if (doubleValue != null) {
                doubleValue = Double
                    .valueOf(doubleValue.doubleValue() / _multiplier);
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
                doubleValue = Double
                    .valueOf(doubleValue.doubleValue() * _multiplier);
            }

            return doubleValue;
        }

        private final double _multiplier;
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
