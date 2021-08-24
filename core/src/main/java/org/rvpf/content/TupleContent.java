/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TupleContent.java 3936 2019-04-28 19:25:00Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import java.util.Collection;
import java.util.ListIterator;

import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;

/**
 * Tuple content converter.
 */
public class TupleContent
    extends ContainerContent
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable decode(final PointValue pointValue)
    {
        final Tuple tuple = _getTuple(pointValue);

        if (tuple != null) {
            final PointValue clone = pointValue.copy();

            for (final ListIterator<Serializable> iterator =
                    tuple.listIterator();
                    iterator.hasNext(); ) {
                clone.setValue(iterator.next());
                iterator.set(getContent().decode(clone));
            }
        }

        return tuple;
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable denormalize(final NormalizedValue normalizedValue)
    {
        final Tuple tuple = _getTuple(normalizedValue);

        if (tuple != null) {
            final NormalizedValue clone = normalizedValue.copy();

            for (final ListIterator<Serializable> iterator =
                    tuple.listIterator();
                    iterator.hasNext(); ) {
                clone.setValue(iterator.next());
                iterator.set(getContent().denormalize(clone));
            }
        }

        return tuple;
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable encode(final PointValue pointValue)
    {
        final Tuple tuple = _getTuple(pointValue);

        if (tuple != null) {
            final PointValue clone = pointValue.copy();

            for (final ListIterator<Serializable> iterator =
                    tuple.listIterator();
                    iterator.hasNext(); ) {
                clone.setValue(iterator.next());
                iterator.set(getContent().encode(clone));
            }
        }

        return tuple;
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable normalize(final PointValue pointValue)
    {
        final Tuple tuple = _getTuple(pointValue);

        if (tuple != null) {
            final PointValue clone = pointValue.copy();

            for (final ListIterator<Serializable> iterator =
                    tuple.listIterator();
                    iterator.hasNext(); ) {
                clone.setValue(iterator.next());
                iterator.set(getContent().normalize(clone));
            }
        }

        return tuple;
    }

    private Tuple _getTuple(final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();

        if (value instanceof Tuple) {
            final Tuple tuple = (Tuple) value;

            return tuple.isFrozen()? tuple.clone(): tuple;
        }

        if (value instanceof Collection<?>) {
            final Collection<?> collection = (Collection<?>) value;
            final Tuple tuple = new Tuple(collection.size());

            try {
                for (final Object object: collection) {
                    tuple.add((Serializable) object);
                }

                return tuple;
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
