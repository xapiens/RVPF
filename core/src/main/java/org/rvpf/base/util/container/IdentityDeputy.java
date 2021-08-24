/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.base.util.container;

import java.util.Iterator;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.tool.Require;

/**
 * Identity deputy.
 *
 * <p>Represents objects for reference equality.</p>
 *
 * @param <E> The element type.
 */
@Immutable
public final class IdentityDeputy<E>
{
    /**
     * Constructs an instance.
     *
     * @param represented The object represented.
     */
    public IdentityDeputy(@Nonnull final E represented)
    {
        _represented = Require.notNull(represented);
    }

    /**
     * Returns an iterator over the represented elements.
     *
     * @param <E> The element type.
     * @param iterator The iterator over the deputies.
     *
     * @return The iterator over the represented elements.
     */
    @Nonnull
    @CheckReturnValue
    public static <E> Iterator<E> iterator(
            @Nonnull final Iterator<IdentityDeputy<E>> iterator)
    {
        return new _DeputyIterator<E>(Require.notNull(iterator));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (object == null) {
            return false;
        }

        if (object == this) {
            return true;
        }

        if (object.getClass() == getClass()) {
            return ((IdentityDeputy<?>) object)
                ._getRepresented() == _getRepresented();
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return System.identityHashCode(_represented);
    }

    /**
     * Gets the represented object.
     *
     * @return The represented object.
     */
    @Nonnull
    @CheckReturnValue
    E _getRepresented()
    {
        return _represented;
    }

    private final E _represented;

    /**
     * Deputy iterator.
     */
    private static class _DeputyIterator<E>
        implements Iterator<E>
    {
        /**
         * Constructs an instance.
         *
         * @param iterator The iterator over the deputies.
         */
        _DeputyIterator(@Nonnull final Iterator<IdentityDeputy<E>> iterator)
        {
            _iterator = iterator;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean hasNext()
        {
            return _iterator.hasNext();
        }

        /** {@inheritDoc}
         */
        @Override
        public E next()
        {
            return _iterator.next()._getRepresented();
        }

        private final Iterator<IdentityDeputy<E>> _iterator;
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
