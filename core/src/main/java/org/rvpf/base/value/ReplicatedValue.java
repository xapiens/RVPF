/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ReplicatedValue.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.value;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Replicated point value.
 */
@NotThreadSafe
public final class ReplicatedValue
    extends VersionedValue
{
    /**
     * Constructs an instance.
     *
     * <p>This is needed for an Externalizable implementation.</p>
     */
    public ReplicatedValue() {}

    /**
     * Constructs an instance.
     *
     * @param pointValue The original versioned point value.
     */
    public ReplicatedValue(@Nonnull final PointValue pointValue)
    {
        super(pointValue);

        _deleted = pointValue.isDeleted();
    }

    /** {@inheritDoc}
     */
    @Override
    public ReplicatedValue copy()
    {
        return new ReplicatedValue(this);
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
    public int hashCode()
    {
        return super.hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isDeleted()
    {
        return _deleted;
    }

    /** {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput input)
        throws IOException
    {
        super.readExternal(input);

        _deleted = input.readBoolean();
    }

    /** {@inheritDoc}
     */
    @Override
    public void readMap(final Map<String, Serializable> map)
    {
        super.readMap(map);

        _deleted = Boolean.parseBoolean((String) map.get(DELETED_FIELD));
    }

    /**
     * Sets the deleted indicator.
     *
     * @param deleted The deleted indicator.
     */
    public void setDeleted(final boolean deleted)
    {
        checkNotFrozen();

        _deleted = deleted;
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput output)
        throws IOException
    {
        super.writeExternal(output);

        output.writeBoolean(_deleted);
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeMap(final Map<String, Serializable> map)
    {
        if (_deleted) {
            map.put(SIMPLE_STRING_MODE, null);
            map.put(DELETED_FIELD, Boolean.toString(_deleted));
        }

        super.writeMap(map);
    }

    /** Deleted field key. */
    public static final String DELETED_FIELD = "deleted";
    private static final long serialVersionUID = 1L;

    private boolean _deleted;
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
