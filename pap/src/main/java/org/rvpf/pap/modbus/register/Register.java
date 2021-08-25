/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Register.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.modbus.message.ReadTransaction;
import org.rvpf.pap.modbus.message.WriteTransaction;

/**
 * Register.
 */
public abstract class Register
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param readOnly True when read-only.
     */
    protected Register(
            @Nonnull final Optional<Integer> address,
            final boolean readOnly)
    {
        _address = address;
        _readOnly = readOnly;
    }

    /**
     * Creates a read request.
     *
     * @return The read request.
     */
    @Nonnull
    @CheckReturnValue
    public abstract ReadTransaction.Request createReadRequest();

    /**
     * Creates a write request.
     *
     * @return The write request.
     */
    @Nonnull
    @CheckReturnValue
    public abstract WriteTransaction.Request createWriteRequest();

    /**
     * Gets the address.
     *
     * @return The optional address.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Integer> getAddress()
    {
        return _address;
    }

    /**
     * Gets the content.
     *
     * @return The content.
     */
    @CheckReturnValue
    public abstract short getContent();

    /**
     * Gets point values from the content.
     *
     * @return The point values.
     */
    @Nonnull
    @CheckReturnValue
    public abstract PointValue[] getPointValues();

    /**
     * Gets the points associated with this register.
     *
     * @return The points associated with this register.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Point[] getPoints();

    /**
     * Asks if this register is read-only.
     *
     * @return True if it is read-only.
     */
    @CheckReturnValue
    public final boolean isReadOnly()
    {
        return _readOnly;
    }

    /**
     * Puts a point value as content.
     *
     * @param pointValue The point value.
     */
    public abstract void putPointValue(@Nonnull PointValue pointValue);

    /**
     * Sets the content.
     *
     * @param content The content.
     */
    public abstract void setContent(final short content);

    /**
     * Sets the contents.
     *
     * @param contents The contents.
     */
    public void setContents(@Nonnull final short[] contents)
    {
        setContent(contents[0]);
    }

    /**
     * Returns the size of this register.
     *
     * @return The size.
     */
    @CheckReturnValue
    public abstract int size();

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return Logger.getInstance(getClass());
    }

    /** No points. */
    protected static final Point[] NO_POINTS = new Point[0];

    /** No points. */
    protected static final PointValue[] NO_POINT_VALUES = new PointValue[0];

    private final Optional<Integer> _address;
    private final boolean _readOnly;
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
