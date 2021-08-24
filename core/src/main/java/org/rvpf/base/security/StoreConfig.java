/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreConfig.java 4066 2019-06-07 20:23:56Z SFB $
 */

package org.rvpf.base.security;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.ResourceFileFactory;

/**
 * Store config.
 */
public abstract class StoreConfig
{
    /**
     * Constructs an instance.
     *
     * @param logger The logger instance to use.
     */
    protected StoreConfig(@Nonnull final Logger logger)
    {
        _logger = Require.notNull(logger);
    }

    /**
     * Gets the store password.
     *
     * @return The optional trust store password.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<char[]> getPassword()
    {
        return _password;
    }

    /**
     * Gets the store path.
     *
     * @return The optional path to the trust store file.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getPath()
    {
        return _path;
    }

    /**
     * Gets the store provider.
     *
     * @return The optional trust store provider.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getProvider()
    {
        return _provider;
    }

    /**
     * Gets the store type.
     *
     * @return The optional trust store type.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getType()
    {
        return _type;
    }

    /**
     * Sets the store password.
     *
     * @param password The optional store password.
     */
    public void setPassword(@Nonnull final Optional<char[]> password)
    {
        _password = password;
    }

    /**
     * Sets the store path.
     *
     * @param path The optional path to the store.
     *
     * @throws FileNotFoundException When appropriate.
     */
    public final void setPath(
            @Nonnull Optional<String> path)
        throws FileNotFoundException
    {
        if (!path.isPresent() || path.get().trim().isEmpty()) {
            _path = Optional.empty();

            return;
        }

        final File file;

        if (_verify || _decrypt) {
            if (!path.get().endsWith(_XML_EXT)) {
                path = Optional.of(path.get() + _XML_EXT);
            }
        }

        file = ResourceFileFactory.newResourceFile(path.get());

        if ((file == null) || !file.isFile()) {
            throw new FileNotFoundException(
                Message.format(BaseMessages.RESOURCE_NOT_FOUND, path.get()));
        }

        path = Optional.of(file.getAbsolutePath());

        _logger.debug(BaseMessages.STORE_PATH, getKind(), path.get());
        _path = path;
    }

    /**
     * Sets the store provider.
     *
     * @param provider The optional store provider.
     */
    public final void setProvider(@Nonnull final Optional<String> provider)
    {
        if (provider.isPresent()) {
            _logger
                .debug(BaseMessages.STORE_PROVIDER, getKind(), provider.get());
        }

        _provider = provider;
    }

    /**
     * Sets the store type.
     *
     * @param type The optional store type.
     */
    public final void setType(@Nonnull final Optional<String> type)
    {
        if (type.isPresent()) {
            _logger.debug(BaseMessages.STORE_TYPE, getKind(), type.get());
        }

        _type = type;
    }

    /**
     * Gets the store kind.
     *
     * @return The store kind.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract String getKind();

    /**
     * Gets the decrypt key idents.
     *
     * @return The decrypt key idents.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String[]> getDecryptKeyIdents()
    {
        return _decryptKeyIdents;
    }

    /**
     * Gets the verify key idents.
     *
     * @return The verify key idents.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String[]> getVerifyKeyIdents()
    {
        return _verifyKeyIdents;
    }

    /**
     * Gets the decrypt indicator.
     *
     * @return The decrypt indicator.
     */
    @CheckReturnValue
    boolean isDecrypt()
    {
        return _decrypt;
    }

    /**
     * Gets the verify indicator.
     *
     * @return The verify indicator.
     */
    @CheckReturnValue
    boolean isVerify()
    {
        return _verify;
    }

    /**
     * Sets the decrypt indicator.
     *
     * @param decrypt The decrypt indicator.
     */
    void setDecrypt(final boolean decrypt)
    {
        _decrypt = decrypt;
    }

    /**
     * Sets the decrypt key idents.
     *
     * @param decryptKeyIdents The decrypt key idents.
     */
    void setDecryptKeyIdents(@Nonnull final Optional<String[]> decryptKeyIdents)
    {
        _decryptKeyIdents = decryptKeyIdents;
    }

    /**
     * Sets the verify indicator.
     *
     * @param verify The verify indicator.
     */
    void setVerify(final boolean verify)
    {
        _verify = verify;
    }

    /**
     * Sets the verify key idents.
     *
     * @param verifyKeyIdents The verify key idents.
     */
    void setVerifyKeyIdents(@Nonnull final Optional<String[]> verifyKeyIdents)
    {
        _verifyKeyIdents = verifyKeyIdents;
    }

    private static final String _XML_EXT = ".xml";

    private volatile boolean _decrypt;
    private volatile Optional<String[]> _decryptKeyIdents = Optional.empty();
    private final Logger _logger;
    private volatile Optional<char[]> _password = Optional.empty();
    private volatile Optional<String> _path = Optional.empty();
    private volatile Optional<String> _provider = Optional.empty();
    private volatile Optional<String> _type = Optional.empty();
    private volatile boolean _verify;
    private volatile Optional<String[]> _verifyKeyIdents = Optional.empty();
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
