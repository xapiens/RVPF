/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Params.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.util.container.KeyedValues;

/**
 * Dictionary of parameters.
 *
 * <p>Params may hold any number of parameters. Each parameter is a key
 * associated with one value or more. The order of parameters within the
 * dictionary is unspecified, but the values of a parameter are in their order
 * of insertion.</p>
 */
@NotThreadSafe
public final class Params
    extends KeyedValues
{
    /**
     * Constructs an instance.
     */
    public Params()
    {
        super(BaseMessages.PARAMETER_TYPE.toString());
    }

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    private Params(final Params other)
    {
        super(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean containsValueKey(final String key)
    {
        boolean containsValueKey = super.containsValueKey(key);

        if (!containsValueKey && (_defaults != null)) {
            containsValueKey = _defaults.containsValueKey(key);
        }

        return containsValueKey;
    }

    /** {@inheritDoc}
     */
    @Override
    public Params copy()
    {
        return new Params(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        return super.equals(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public Params freeze()
    {
        super.freeze();

        return this;
    }

    /** {@inheritDoc}
     */
    @Override
    public List<Object> getObjects(final String key)
    {
        List<Object> objects = super.getObjects(key);

        if (objects.isEmpty() && (_defaults != null)) {
            objects = _defaults.getObjects(key);
        }

        return objects;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Sets the defaults.
     *
     * @param defaults The defaults.
     */
    public void setDefaults(@Nonnull final Params defaults)
    {
        checkNotFrozen();

        _defaults = defaults;
    }

    /** Empty parameters. */
    public static final Params EMPTY_PARAMS = new Params().freeze();

    /**  */

    private static final long serialVersionUID = 1L;

    private Params _defaults;
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
