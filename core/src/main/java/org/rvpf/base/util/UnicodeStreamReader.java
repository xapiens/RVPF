/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UnicodeStreamReader.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;

import java.nio.charset.StandardCharsets;

import java.util.Objects;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.rvpf.base.tool.Require;

/**
 * Unicode stream reader.
 *
 * <p>Supports UTF-8 with optional BOM and UTF-16(BE/LE).</p>
 *
 * <p>Note: when the BOM is absent, the presence of a 0x00 byte in only one of
 * the first two bytes is used to recognize UTF-16.</p>
 */
@ThreadSafe
public class UnicodeStreamReader
    extends Reader
{
    /**
     * Constructs an instance.
     *
     * @param inputFile The input file.
     *
     * @throws FileNotFoundException When the file is not found.
     */
    public UnicodeStreamReader(
            @Nonnull final File inputFile)
        throws FileNotFoundException
    {
        this(new FileInputStream(inputFile));
    }

    /**
     * Constructs an instance.
     *
     * @param stream The Unicode input stream.
     */
    public UnicodeStreamReader(@Nonnull final InputStream stream)
    {
        _stream = new PushbackInputStream(Require.notNull(stream), 3);
    }

    /**
     * Constructs an instance.
     *
     * @param inputPath The input path.
     *
     * @throws FileNotFoundException When the file is not found.
     */
    public UnicodeStreamReader(
            @Nonnull final String inputPath)
        throws FileNotFoundException
    {
        this(new File(inputPath));
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
        throws IOException
    {
        if (_reader != null) {
            _reader.close();
        } else {
            _stream.close();
        }
    }

    /**
     * Gets the name of the character set encoding detected.
     *
     * @return The name of the character set encoding detected.
     *
     * @throws IOException When an I/O error occurs.
     */
    @Nonnull
    @CheckReturnValue
    public String getEncoding()
        throws IOException
    {
        final String encoding = Objects
            .requireNonNull(_getReader().getEncoding());

        return encoding;
    }

    /** {@inheritDoc}
     */
    @Override
    public int read()
        throws IOException
    {
        return _getReader().read();
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
        return _getReader().read(buffer, offset, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean ready()
        throws IOException
    {
        return _getReader().ready();
    }

    private synchronized InputStreamReader _getReader()
        throws IOException
    {
        if (_reader == null) {
            final byte[] bom = new byte[2];
            final int read = _stream.read(bom);
            final int unread;
            final String charsetName;

            if (read < 2) {
                unread = read;
                charsetName = StandardCharsets.UTF_8.name();
            } else if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB)) {
                final int next = _stream.read();

                if ((byte) next == (byte) 0xBF) {
                    unread = 0;
                } else {
                    _stream.unread(next);
                    unread = read;
                }

                charsetName = StandardCharsets.UTF_8.name();
            } else if (((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE))
                       || ((bom[0] == (byte) 0xFE)
                       && (bom[1] == (byte) 0xFF))) {
                unread = read;
                charsetName = StandardCharsets.UTF_16.name();
            } else if ((bom[0] != (byte) 0x00) && (bom[1] == (byte) 0x00)) {
                unread = read;
                charsetName = StandardCharsets.UTF_16LE.name();
            } else if ((bom[0] == (byte) 0x00) && (bom[1] != (byte) 0x00)) {
                unread = read;
                charsetName = StandardCharsets.UTF_16BE.name();
            } else {
                unread = read;
                charsetName = StandardCharsets.UTF_8.name();
            }

            if (unread > 0) {
                _stream.unread(bom, 0, unread);
            }

            _reader = new InputStreamReader(_stream, charsetName);
        }

        return _reader;
    }

    private InputStreamReader _reader;
    private final PushbackInputStream _stream;
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
