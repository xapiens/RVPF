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

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import org.rvpf.base.tool.Require;

/**
 * Weak deputy.
 *
 * @param <E> The element type.
 */
public final class WeakDeputy<E>
    extends WeakReference<E>
{
    /**
     * Constructs an instance.
     *
     * @param represented The object represented.
     */
    public WeakDeputy(@Nonnull final E represented)
    {
        super(Require.notNull(represented));
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

        if (object.getClass() != getClass()) {
            return false;
        }

        final Object represented = get();
        @SuppressWarnings("unchecked")
        final Object otherRepresented = ((WeakDeputy<E>) object).get();

        if (represented == null) {
            return otherRepresented == null;
        }

        return represented.equals(otherRepresented);
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final Object represented = get();

        return (represented != null)? represented.hashCode(): 0;
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
