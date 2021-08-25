/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ArrayRegister.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.modbus.message.ReadTransaction;
import org.rvpf.pap.modbus.message.WriteTransaction;

/**
 * Array register.
 */
public abstract class ArrayRegister
    extends Register
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param size The array size.
     * @param point The optional point associated with the register.
     * @param readOnly True when read-only.
     */
    protected ArrayRegister(
            @Nonnull final Optional<Integer> address,
            final int size,
            @Nonnull final Optional<Point> point,
            final boolean readOnly)
    {
        super(address, readOnly);

        _size = size;
        _contents = new short[size];
        _points = point.isPresent()? new Point[] {point.get(), }: NO_POINTS;
    }

    /** {@inheritDoc}
     */
    @Override
    public final short getContent()
    {
        return _contents[0];
    }

    /** {@inheritDoc}
     */
    @Override
    public final Point[] getPoints()
    {
        return _points;
    }

    /**
     * Returns a new minion at the specified offset.
     *
     * @param offset The offset for the address of the minion (1..size-1).
     *
     * @return The new minion.
     */
    @Nonnull
    @CheckReturnValue
    public final Register newMinion(final int offset)
    {
        return new Minion(
            Integer.valueOf(getAddress().get().intValue() + offset),
            isReadOnly());
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setContent(final short content)
    {
        _contents[0] = content;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setContents(final short[] contents)
    {
        Require.success(contents.length == size());

        System.arraycopy(contents, 0, _contents, 0, size());
    }

    /** {@inheritDoc}
     */
    @Override
    public final int size()
    {
        return _size;
    }

    /**
     * Gets the contents.
     *
     * @return The contents.
     */
    @Nonnull
    @CheckReturnValue
    protected final short[] getContents()
    {
        return _contents;
    }

    /**
     * Gets the content for a minion.
     *
     * @param contentIndex The contents index.
     *
     * @return The content.
     */
    @CheckReturnValue
    final short getContent(final int contentIndex)
    {
        return _contents[contentIndex];
    }

    /**
     * Sets the content for a minion.
     *
     * @param contentIndex The contents index.
     * @param content The content.
     */
    final void setContent(final int contentIndex, final short content)
    {
        _contents[contentIndex] = content;
    }

    private final short[] _contents;
    private final Point[] _points;
    private final int _size;

    /**
     * Minion.
     */
    private final class Minion
        extends Register
    {
        /**
         * Constructs an instance.
         *
         * @param address
         * @param readOnly
         */
        protected Minion(@Nonnull final Integer address, final boolean readOnly)
        {
            super(Optional.of(address), readOnly);
        }

        /** {@inheritDoc}
         */
        @Override
        public ReadTransaction.Request createReadRequest()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public WriteTransaction.Request createWriteRequest()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public short getContent()
        {
            return ArrayRegister.this.getContent(_getContentIndex());
        }

        /** {@inheritDoc}
         */
        @Override
        public PointValue[] getPointValues()
        {
            return NO_POINT_VALUES;
        }

        /** {@inheritDoc}
         */
        @Override
        public Point[] getPoints()
        {
            return NO_POINTS;
        }

        /** {@inheritDoc}
         */
        @Override
        public void putPointValue(final PointValue pointValue)
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public void setContent(final short content)
        {
            ArrayRegister.this.setContent(_getContentIndex(), content);
        }

        /** {@inheritDoc}
         */
        @Override
        public int size()
        {
            return 1;
        }

        private int _getContentIndex()
        {
            return this
                .getAddress()
                .get()
                .intValue() - ArrayRegister.this.getAddress().get().intValue();
        }
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
