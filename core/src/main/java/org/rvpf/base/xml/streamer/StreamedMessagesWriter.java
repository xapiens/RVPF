/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StreamedMessagesWriter.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.xml.streamer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;

import java.nio.charset.Charset;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.XMLElement;

/**
 * Streamed messages writer.
 */
@NotThreadSafe
public class StreamedMessagesWriter
    extends StreamedMessagesAccess
{
    /**
     * Adds a message.
     *
     * @param message The message.
     *
     * @return True value on success.
     */
    @CheckReturnValue
    public synchronized boolean add(@Nonnull final Serializable message)
    {
        return _output.add(message);
    }

    /**
     * Closes the current file.
     */
    public void close()
    {
        final Streamer.Output output = _output;

        if (output != null) {
            output.close();
            _output = null;
        }
    }

    /**
     * Flushes the output.
     */
    public synchronized void flush()
    {
        _output.flush();
    }

    /**
     * Opens an output.
     *
     * @param writer The output writer.
     */
    public synchronized void open(@Nonnull final Writer writer)
    {
        Require.success(_output == null);

        _output = getStreamer().newOutput(writer);
    }

    /**
     * Opens an output.
     *
     * @param parentElement The output parent element.
     */
    public synchronized void open(@Nonnull final XMLElement parentElement)
    {
        Require.success(_output == null);

        _output = getStreamer().newOutput(parentElement);
    }

    /**
     * Opens an output.
     *
     * @param file The output file.
     * @param charset An optional charset.
     *
     * @throws FileNotFoundException When the file cannot be created.
     */
    public synchronized void open(
            @Nonnull final File file,
            @Nonnull final Optional<Charset> charset)
        throws FileNotFoundException
    {
        Require.success(_output == null);

        _output = getStreamer().newOutput(file, charset);
    }

    /**
     * Opens an output.
     *
     * @param outputStream The output stream.
     * @param charset An optional charset.
     */
    public synchronized void open(
            @Nonnull final OutputStream outputStream,
            @Nonnull final Optional<Charset> charset)
    {
        Require.success(_output == null);

        _output = getStreamer().newOutput(outputStream, charset);
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        close();

        super.tearDown();
    }

    private volatile Streamer.Output _output;
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
