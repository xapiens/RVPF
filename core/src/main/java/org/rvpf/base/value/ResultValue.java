/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ResultValue.java 4004 2019-05-18 13:46:06Z SFB $
 */

package org.rvpf.base.value;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;

/**
 * Result Value.
 */
@NotThreadSafe
public class ResultValue
    extends PointValue
{
    /**
     * Constructs an instance.
     *
     * <p>This is needed for an Externalizable implementation. It is also used
     * as a never matching reference.</p>
     */
    public ResultValue() {}

    /**
     * Constructs an instance.
     *
     * @param point The point definition.
     * @param stamp The optional time stamp of the value.
     */
    public ResultValue(
            @Nonnull final Point point,
            @Nonnull final Optional<DateTime> stamp)
    {
        super(point, stamp, null, null);
    }

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected ResultValue(@Nonnull final ResultValue other)
    {
        super(other);

        _inputValues.addAll(other._inputValues);
        _fetched = other._fetched;
    }

    /**
     * Adds an input value.
     *
     * @param inputValue The new input value.
     */
    public final void addInputValue(@Nonnull PointValue inputValue)
    {
        if (inputValue.isAbsent() && !(inputValue instanceof Null)) {
            inputValue = new PointValue.Null(inputValue);
        }

        _inputValues.add(inputValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultValue copy()
    {
        return new ResultValue(this);
    }

    /**
     * Gets the input values.
     *
     * @return The List of input values.
     */
    @Nonnull
    @CheckReturnValue
    public final List<PointValue> getInputValues()
    {
        return _inputValues;
    }

    /**
     * Asks if this instance has been fetched.
     *
     * @return True when it has been fetched.
     */
    @CheckReturnValue
    public final boolean isFetched()
    {
        return _fetched;
    }

    /**
     * Asks if this instance is replaceable.
     *
     * @return True if it is replacable.
     */
    @CheckReturnValue
    public boolean isReplaceable()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultValue morph(final Optional<UUID> uuid)
    {
        final ResultValue clone = (ResultValue) super.morph(uuid);

        if (!uuid.isPresent()) {
            clone._inputValues = new ArrayList<>(getInputValues().size());

            for (final PointValue inputValue: getInputValues()) {
                clone.addInputValue(inputValue.morph(Optional.empty()));
            }
        }

        return this;
    }

    /** {@inheritDoc}
     */
    @Override
    public void readExternal(final ObjectInput input)
        throws IOException
    {
        super.readExternal(input);

        final int inputs = input.readInt();

        for (int i = 0; i < inputs; ++i) {
            final PointValue inputValue = new PointValue();

            inputValue.readExternal(input);
            _inputValues.add(inputValue);
        }
    }

    /**
     * Sets the fetched indicator.
     *
     * @param fetched The fetched indicator.
     */
    public final void setFetched(final boolean fetched)
    {
        _fetched = fetched;
    }

    /** {@inheritDoc}
     */
    @Override
    public void writeExternal(final ObjectOutput output)
        throws IOException
    {
        super.writeExternal(output);

        output.writeInt(_inputValues.size());

        for (final PointValue inputValue: _inputValues) {
            inputValue.writeExternal(output);
        }
    }

    private static final long serialVersionUID = 1L;

    private transient boolean _fetched;
    private transient List<PointValue> _inputValues =
        new LinkedList<PointValue>();
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
