/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DictContent.java 3936 2019-04-28 19:25:00Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import java.util.Map;

import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.Dict;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;

/**
 * Dict content converter.
 */
public class DictContent
    extends ContainerContent
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable decode(final PointValue pointValue)
    {
        final Dict dict = _getDict(pointValue);

        if (dict != null) {
            final PointValue clone = pointValue.copy();

            for (final Map.Entry<String, Serializable> entry: dict.entrySet()) {
                clone.setValue(entry.getValue());
                entry.setValue(getContent().decode(clone));
            }
        }

        return dict;
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable denormalize(final NormalizedValue normalizedValue)
    {
        final Dict dict = _getDict(normalizedValue);

        if (dict != null) {
            final NormalizedValue clone = normalizedValue.copy();

            for (final Map.Entry<String, Serializable> entry: dict.entrySet()) {
                clone.setValue(entry.getValue());
                entry.setValue(getContent().denormalize(clone));
            }
        }

        return dict;
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable encode(final PointValue pointValue)
    {
        final Dict dict = _getDict(pointValue);

        if (dict != null) {
            final PointValue clone = pointValue.copy();

            for (final Map.Entry<String, Serializable> entry: dict.entrySet()) {
                clone.setValue(entry.getValue());
                entry.setValue(getContent().encode(clone));
            }
        }

        return dict;
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable normalize(final PointValue pointValue)
    {
        final Dict dict = _getDict(pointValue);

        if (dict != null) {
            final PointValue clone = pointValue.copy();

            for (final Map.Entry<String, Serializable> entry: dict.entrySet()) {
                clone.setValue(entry.getValue());
                entry.setValue(getContent().normalize(clone));
            }
        }

        return dict;
    }

    private Dict _getDict(final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        if (value instanceof Dict) {
            final Dict dict = (Dict) value;

            return dict.isFrozen()? dict.clone(): dict;
        }

        if (value instanceof Map<?, ?>) {
            final Map<?, ?> map = (Map<?, ?>) value;
            final Dict dict = new Dict(KeyedValues.hashCapacity(map.size()));

            try {
                for (final Map.Entry<?, ?> entry: map.entrySet()) {
                    dict
                        .put(
                            (String) entry.getKey(),
                            (Serializable) entry.getValue());
                }

                return dict;
            } catch (final ClassCastException exception) {
                // Ignore (will warn).
            }
        }

        if (value != null) {
            warnBadValue(pointValue);
        }

        return null;
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
