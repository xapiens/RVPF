/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Encrypted.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.security;

import java.io.Serializable;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/** Encrypted.
 */
@Immutable
final class Encrypted
    implements Serializable
{
    /** Constructs an instance.
     *
     * @param encrypted The encrypted value.
     */
    public Encrypted(@Nonnull final String encrypted)
    {
        _encrypted = encrypted;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (object instanceof Encrypted) {
            return _encrypted.equals(((Encrypted) object)._encrypted);
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return _encrypted.hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return _encrypted.toString();
    }

    /** Gets the encrypted value.
     *
     * @return The encrypted value.
     */
    @Nonnull
    @CheckReturnValue
    String getEncrypted()
    {
        return _encrypted;
    }

    private static final long serialVersionUID = 1L;

    private final String _encrypted;
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
