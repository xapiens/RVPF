/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Coder.java 4107 2019-07-13 13:18:26Z SFB $
 */

package org.rvpf.base.tool;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Coder.
 *
 * <p>Simplifies the use of a CharSet encoder / decoder.</p>
 */
@NotThreadSafe
public final class Coder
{
    /**
     * Constructs an instance.
     */
    public Coder()
    {
        this(StandardCharsets.UTF_8);
    }

    /**
     * Constructs an instance.
     *
     * @param charset The Charset used for encoding / decoding.
     */
    public Coder(@Nonnull final Charset charset)
    {
        setCharset(charset);
    }

    /**
     * Decodes a bytes array to a tring.
     *
     * @param bytes The bytes array.
     *
     * @return A String.
     */
    @Nonnull
    @CheckReturnValue
    public String decode(@Nonnull final byte[] bytes)
    {
        return decode(bytes, 0, bytes.length);
    }

    /**
     * Decodes a bytes array to a String.
     *
     * @param bytes The bytes array.
     * @param offset The offset at which to start decoding.
     * @param length The number of bytes to decode.
     *
     * @return A String.
     */
    @Nonnull
    @CheckReturnValue
    public String decode(
            @Nonnull final byte[] bytes,
            final int offset,
            final int length)
    {
        if (length == 0) {
            return "";
        }

        final CharsetDecoder decoder = _getDecoder();
        final ByteBuffer input = _getByteBuffer(length);
        CharBuffer output = _getCharBuffer(
            (int) (length * decoder.averageCharsPerByte()));

        input.put(bytes, offset, length);
        input.flip();
        decoder.reset();

        for (;;) {
            final CoderResult result;

            if (input.hasRemaining()) {
                result = decoder.decode(input, output, true);
            } else {
                result = decoder.flush(output);
            }

            if (result.isUnderflow()) {
                break;
            }

            if (result.isOverflow()) {
                final CharBuffer buffer = _getCharBuffer(output.capacity() * 2);

                output.flip();
                buffer.put(output);
                output = buffer;
            }
        }

        output.flip();

        return output.toString();
    }

    /**
     * Encodes a String to a bytes array.
     *
     * @param string The String.
     *
     * @return A bytes array.
     */
    @Nonnull
    @CheckReturnValue
    public byte[] encode(@Nonnull final String string)
    {
        if (string.isEmpty()) {
            return _EMPTY_BYTE_ARRAY;
        }

        final CharsetEncoder encoder = _getEncoder();
        final CharBuffer input = _getCharBuffer(string.length());
        ByteBuffer output = _getByteBuffer(
            (int) (string.length() * encoder.averageBytesPerChar()));

        input.put(string, 0, string.length());
        input.flip();
        encoder.reset();

        for (;;) {
            final CoderResult result;

            if (input.hasRemaining()) {
                result = encoder.encode(input, output, true);
            } else {
                result = encoder.flush(output);
            }

            if (result.isUnderflow()) {
                break;
            }

            if (result.isOverflow()) {
                final ByteBuffer buffer = _getByteBuffer(output.capacity() * 2);

                output.flip();
                buffer.put(output);
                output = buffer;
            }
        }

        output.flip();

        final byte[] bytes = new byte[output.limit()];

        output.get(bytes, 0, bytes.length);

        return bytes;
    }

    /**
     * Gets the character set for encoding / decoding.
     *
     * @return The Charset.
     */
    @Nonnull
    @CheckReturnValue
    public Charset getCharset()
    {
        if (_charset == null) {
            _charset = Charset.defaultCharset();
        }

        return _charset;
    }

    /**
     * Sets the character set for encoding / decoding.
     *
     * @param charset A Charset.
     */
    public void setCharset(@Nonnull final Charset charset)
    {
        _charset = Require.notNull(charset);
        _decoder = null;
        _encoder = null;
        _byteBuffer = null;
        _charBuffer = null;
    }

    private ByteBuffer _getByteBuffer(final int capacity)
    {
        if ((_byteBuffer == null) || (_byteBuffer.capacity() < capacity)) {
            _byteBuffer = ByteBuffer.allocate(capacity);
        } else {
            _byteBuffer.clear();
        }

        return _byteBuffer;
    }

    private CharBuffer _getCharBuffer(final int capacity)
    {
        if ((_charBuffer == null) || (_charBuffer.capacity() < capacity)) {
            _charBuffer = CharBuffer.allocate(capacity);
        } else {
            _charBuffer.clear();
        }

        return _charBuffer;
    }

    private CharsetDecoder _getDecoder()
    {
        if (_decoder == null) {
            _decoder = getCharset().newDecoder();
            _decoder.onMalformedInput(CodingErrorAction.REPLACE);
            _decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

        return _decoder;
    }

    private CharsetEncoder _getEncoder()
    {
        if (_encoder == null) {
            _encoder = getCharset().newEncoder();
            _encoder.onMalformedInput(CodingErrorAction.REPLACE);
            _encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

        return _encoder;
    }

    private static final byte[] _EMPTY_BYTE_ARRAY = new byte[0];

    private ByteBuffer _byteBuffer;
    private CharBuffer _charBuffer;
    private Charset _charset;
    private CharsetDecoder _decoder;
    private CharsetEncoder _encoder;
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
