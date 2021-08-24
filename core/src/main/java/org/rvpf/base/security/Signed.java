/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Signed.java 3933 2019-04-25 20:23:38Z SFB $
 */

package org.rvpf.base.security;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.tool.Require;

/**
 * Signed.
 */
@Immutable
final class Signed
    implements Serializable
{
    /**
     * Constructs an instance.
     *
     * @param signed The signed object.
     * @param signature The signature.
     */
    public Signed(
            @Nullable final Serializable signed,
            @Nonnull final String signature)
    {
        _signed = signed;
        _signature = Require.notNull(signature);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (object instanceof Signed) {
            return _signature.equals(((Signed) object)._signature);
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return _signature.hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.valueOf(_signed) + ": " + _signature;
    }

    /**
     * Gets the signature.
     *
     * @return The signature.
     */
    @Nonnull
    @CheckReturnValue
    String getSignature()
    {
        return _signature;
    }

    /**
     * Gets the signed.
     *
     * @return The signed.
     */
    @Nullable
    @CheckReturnValue
    Serializable getSigned()
    {
        return _signed;
    }

    private static final long serialVersionUID = 1L;

    private final String _signature;
    private final Serializable _signed;
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
