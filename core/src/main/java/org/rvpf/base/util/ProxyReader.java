/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyReader.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

import java.nio.CharBuffer;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Proxy reader.
 *
 * <p>This is similar to a {@link FilterReader} but allows changing the proxied
 * {@link Reader} with {@link #setProxied} which must be called at least once
 * with a non null argument before calling any other method except
 * {@link #getProxied} and {@link #close} to avoid a null pointer exception.</p>
 */
public class ProxyReader
    extends Reader
{
    /** {@inheritDoc}
     */
    @Override
    public void close()
        throws IOException
    {
        final Reader proxied = _proxied;

        if (proxied != null) {
            proxied.close();
        }
    }

    /**
     * Gets the proxied reader.
     *
     * @return The optional proxied reader.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Reader> getProxied()
    {
        return Optional.ofNullable(_proxied);
    }

    /** {@inheritDoc}
     */
    @Override
    public void mark(final int readAheadLimit)
        throws IOException
    {
        _proxied.mark(readAheadLimit);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean markSupported()
    {
        return _proxied.markSupported();
    }

    /** {@inheritDoc}
     */
    @Override
    public int read()
        throws IOException
    {
        return _proxied.read();
    }

    /** {@inheritDoc}
     */
    @Override
    public int read(final char[] buffer)
        throws IOException
    {
        return _proxied.read(buffer);
    }

    /** {@inheritDoc}
     */
    @Override
    public int read(final CharBuffer target)
        throws IOException
    {
        return _proxied.read(target);
    }

    /** {@inheritDoc}
     */
    @Override
    public int read(
            final char[] buffer,
            final int offset,
            final int length)
        throws IOException
    {
        return _proxied.read(buffer, offset, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean ready()
        throws IOException
    {
        return _proxied.ready();
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset()
        throws IOException
    {
        _proxied.reset();
    }

    /**
     * Sets the proxied reader.
     *
     * @param proxied The proxied reader (empty to cancel).
     */
    public void setProxied(@Nonnull final Optional<Reader> proxied)
    {
        _proxied = proxied.orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public long skip(final long n)
        throws IOException
    {
        return _proxied.skip(n);
    }

    private volatile Reader _proxied;
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
