/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueueReader.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.som.queue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import java.util.zip.GZIPInputStream;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Coder;
import org.rvpf.base.tool.Require;

/**
 * Queue reader.
 *
 * <p>This specialized Reader can read queue files (UTF-8) starting at a
 * specified offset (in bytes). It can also return the current offset when
 * requested.</p>
 */
class QueueReader
    extends Reader
{
    /**
     * Constructs an instance.
     *
     * @param inputFile The input file.
     * @param next The offset to the next byte.
     * @param compressed True if the input file is compressed.
     *
     * @throws IOException On I/O exception.
     */
    QueueReader(
            @Nonnull final File inputFile,
            final long next,
            final boolean compressed)
        throws IOException
    {
        InputStream inputStream = new FileInputStream(inputFile);

        if (compressed) {
            inputStream = new GZIPInputStream(inputStream);
        }

        if (next > 0) {
            final long skipped = inputStream.skip(next);

            Require.success(skipped == next);
        }

        _inputStream = new _QueueInputStream(inputStream, next);
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
        throws IOException
    {
        _inputStream.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public int read(
            final char[] buffer,
            final int offset,
            int length)
        throws IOException
    {
        if (length > 1) {
            length /= 2;
        }

        final byte[] bytes = new byte[length];
        final int read = _inputStream.read(bytes, 0, length);

        if (read < 1) {
            return read;
        }

        final String string = Require.notNull(_coder.decode(bytes, 0, read));

        string.getChars(0, string.length(), buffer, offset);

        return string.length();
    }

    /**
     * Gets the next byte position.
     *
     * @return The next byte position.
     */
    @CheckReturnValue
    long getNext()
    {
        return _inputStream.getNext();
    }

    private final Coder _coder = new Coder();
    private final _QueueInputStream _inputStream;

    /**
     * Queue input stream.
     */
    private static final class _QueueInputStream
        extends BufferedInputStream
    {
        /**
         * Constructs an instance.
         *
         * @param inputStream The input stream.
         * @param next The next byte position.
         */
        _QueueInputStream(
                @Nonnull final InputStream inputStream,
                final long next)
        {
            super(inputStream);

            _next = next;
        }

        /** {@inheritDoc}
         */
        @Override
        public synchronized int read(
                final byte[] buffer,
                final int offset,
                final int length)
            throws IOException
        {
            int byteCount = 0;

            while (byteCount < length) {
                final int read = super.read();

                if (read < 0) {
                    if (byteCount == 0) {
                        return read;
                    }

                    break;
                }

                buffer[offset + byteCount++] = (byte) read;

                if (((read == '\n') || (read == '\r')) && (byteCount > 1)) {
                    break;
                }
            }

            _next += byteCount;

            return byteCount;
        }

        /**
         * Gets the next byte position.
         *
         * @return The next byte position.
         */
        @CheckReturnValue
        long getNext()
        {
            return _next;
        }

        private long _next;
    }
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
