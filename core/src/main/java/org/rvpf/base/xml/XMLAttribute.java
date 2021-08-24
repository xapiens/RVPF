/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: XMLAttribute.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base.xml;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.tool.Require;

/**
 * XML attribute.
 *
 * <p>Instances of this class are immutable.</p>
 */
@Immutable
public class XMLAttribute
{
    /**
     * Constructs an instance.
     *
     * @param name The attribute's name.
     * @param value The attribute's value.
     */
    public XMLAttribute(@Nonnull final String name, @Nonnull final String value)
    {
        _name = name.trim();
        _value = Require.notNull(value);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (other instanceof XMLAttribute) {
            final XMLAttribute otherAttribute = (XMLAttribute) other;

            return getName().equals(otherAttribute.getName())
                   && getValue().equals(otherAttribute.getValue());
        }

        return false;
    }

    /**
     * Gets the attribute's name.
     *
     * @return The attribute's name.
     */
    @Nonnull
    @CheckReturnValue
    public String getName()
    {
        return _name;
    }

    /**
     * Gets the attribute's value.
     *
     * @return The attribute's value.
     */
    @Nonnull
    @CheckReturnValue
    public String getValue()
    {
        return _value;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return getName().hashCode() ^ getValue().hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return _name + "='" + _value + "'";
    }

    private final String _name;
    private final String _value;
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
