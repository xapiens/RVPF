/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StreamedMessagesReader.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base.xml.streamer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;

import java.nio.charset.Charset;

import java.util.Iterator;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.util.NoCloseReader;
import org.rvpf.base.xml.XMLElement;

/**
 * Streamed messages reader.
 */
@NotThreadSafe
public class StreamedMessagesReader
    extends StreamedMessagesAccess
    implements Iterator<Serializable>, Iterable<Serializable>
{
    /**
     * Closes the current file.
     */
    public void close()
    {
        if (_input != null) {
            _input.close();
            _input = null;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean hasNext()
    {
        return _input.hasNext();
    }

    /** {@inheritDoc}
     */
    @Override
    public Iterator<Serializable> iterator()
    {
        return _input.iterator();
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable next()
    {
        return _input.next();
    }

    /**
     * Opens an input.
     *
     * @param reader The input reader.
     */
    public void open(@Nonnull final Reader reader)
    {
        _input = getStreamer().newInput(new NoCloseReader(reader));
    }

    /**
     * Opens an input.
     *
     * @param parentElement The input parent element.
     */
    public void open(@Nonnull final XMLElement parentElement)
    {
        _input = getStreamer().newInput(parentElement);
    }

    /**
     * Opens an input.
     *
     * @param file The input file.
     * @param charset A charset (may be null).
     *
     * @throws FileNotFoundException When the file is not found.
     */
    public void open(
            @Nonnull final File file,
            @Nonnull final Optional<Charset> charset)
        throws FileNotFoundException
    {
        _input = getStreamer().newInput(file, charset);
    }

    /**
     * Opens an input.
     *
     * @param inputStream The input stream.
     * @param charset A charset (may be null).
     */
    public void open(
            @Nonnull final InputStream inputStream,
            @Nonnull final Optional<Charset> charset)
    {
        _input = getStreamer().newInput(inputStream, charset);
    }

    /** {@inheritDoc}
     */
    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        close();

        super.tearDown();
    }

    private Streamer.Input _input;
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
