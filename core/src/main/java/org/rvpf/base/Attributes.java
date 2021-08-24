/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Attributes.java 4109 2019-07-22 23:31:37Z SFB $
 */

package org.rvpf.base;

import java.util.List;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.util.container.KeyedValues;

/**
 * Attributes.
 *
 * <p>
 * Represents the 'attributes' element in the metadata.
 *
 * Allows usage specific customization.
 * </p>
 */
public final class Attributes
    extends KeyedValues
{
    /**
     * Constructs an instance.
     *
     * @param usage The attributes usage.
     */
    public Attributes(@Nonnull final String usage)
    {
        super(BaseMessages.ATTRIBUTE_TYPE.toString());

        _usage = usage;
    }

    /**
     * Constructs an instance.
     *
     * @param other An other instance.
     */
    private Attributes(final Attributes other)
    {
        super(other);

        _usage = other._usage;
    }

    /** {@inheritDoc}
     */
    @Override
    public Attributes copy()
    {
        return new Attributes(this);
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
    public Attributes freeze()
    {
        super.freeze();

        return this;
    }

    /**
     * Gets the usage.
     *
     * @return The usage.
     */
    @Nonnull
    @CheckReturnValue
    public String getUsage()
    {
        return _usage;
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
    public String toString()
    {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Attributes '");
        stringBuilder.append(getUsage());
        stringBuilder.append("' {\n");

        for (Map.Entry<String, List<Object>> entry: getValuesEntries()) {
            stringBuilder.append('\t');
            stringBuilder.append(entry.getKey());
            stringBuilder.append(": ");
            stringBuilder.append(entry.getValue());
            stringBuilder.append('\n');
        }

        stringBuilder.append("}\n");

        return stringBuilder.toString();
    }

    private static final long serialVersionUID = 1L;

    private final String _usage;
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
