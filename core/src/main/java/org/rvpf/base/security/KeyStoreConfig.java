/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: KeyStoreConfig.java 3986 2019-05-15 18:52:38Z SFB $
 */

package org.rvpf.base.security;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;

/**
 * KeyStore config.
 */
public final class KeyStoreConfig
    extends StoreConfig
{
    /**
     * Constructs an instance.
     *
     * @param logger The logger instance to use.
     */
    protected KeyStoreConfig(@Nonnull final Logger logger)
    {
        super(logger);
    }

    /**
     * Gets the store key ident.
     *
     * @return The optional store key ident.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getKeyIdent()
    {
        return _keyIdent;
    }

    /**
     * Gets the store key password.
     *
     * @return The optional store key password.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<char[]> getKeyPassword()
    {
        return _keyPassword;
    }

    /**
     * Sets the store key ident.
     *
     * @param keyIdent The optional store key ident.
     */
    public void setKeyIdent(@Nonnull final Optional<String> keyIdent)
    {
        _keyIdent = keyIdent;
    }

    /**
     * Sets the store key password.
     *
     * @param keyPassword The optional store key password.
     */
    public void setKeyPassword(@Nonnull final Optional<char[]> keyPassword)
    {
        _keyPassword = (keyPassword.isPresent()
                && (keyPassword.get().length > 0))? keyPassword: Optional
                    .empty();
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getKind()
    {
        return KIND;
    }

    /** The store kind. */
    public static final String KIND = "KeyStore";

    private volatile Optional<String> _keyIdent = Optional.empty();
    private volatile Optional<char[]> _keyPassword = Optional.empty();
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
