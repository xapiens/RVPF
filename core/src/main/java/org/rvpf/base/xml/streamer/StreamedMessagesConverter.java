/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StreamedMessagesConverter.java 3984 2019-05-14 12:28:33Z SFB $
 */

package org.rvpf.base.xml.streamer;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.Serializable;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.util.NoCloseReader;

/**
 * Streamed messages converter.
 */
@NotThreadSafe
public final class StreamedMessagesConverter
    extends StreamedMessagesAccess
{
    /**
     * Converts from XML characters.
     *
     * @param xml The XML characters.
     *
     * @return The optional message.
     */
    @CheckReturnValue
    public Optional<Serializable> fromXMLChars(@Nonnull final char[] xml)
    {
        synchronized (_reader) {
            if (_input == null) {
                _input = getStreamer().newInput(new NoCloseReader(_reader));
            }

            _reader.reset(xml);

            final Serializable message = _input.next();

            if (message == null) {
                _input.close();
                _input = null;
            }

            return Optional.ofNullable(message);
        }
    }

    /**
     * Converts from a XML string.
     *
     * @param xml The XML string.
     *
     * @return The optional message.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Serializable> fromXMLString(@Nonnull final String xml)
    {
        if (xml.trim().isEmpty()) {
            return Optional.empty();
        }

        return fromXMLChars(xml.toCharArray());
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        synchronized (_reader) {
            if (_input != null) {
                _input.close();
                _input = null;
            }
        }

        synchronized (_writer) {
            if (_output != null) {
                _output.close();
                _output = null;
            }
        }

        super.tearDown();
    }

    /**
     * Converts to XML characters.
     *
     * @param message The message.
     *
     * @return The XML characters (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public char[] toXMLChars(@Nonnull final Serializable message)
    {
        synchronized (_writer) {
            if (_output == null) {
                _output = getStreamer().newOutput(_writer);
            }

            _writer.reset();

            if (!_output.add(message)) {
                return null;
            }

            _output.flush();

            return _writer.toCharArray();
        }
    }

    /**
     * Converts to a XML string.
     *
     * @param message The message.
     *
     * @return The XML string (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public String toXMLString(@Nonnull final Serializable message)
    {
        final char[] chars = toXMLChars(message);

        return (chars != null)? new String(chars): null;
    }

    private Streamer.Input _input;
    private Streamer.Output _output;
    private final _XMLReader _reader = new _XMLReader();
    private final _XMLWriter _writer = new _XMLWriter();

    /**
     * XML reader.
     */
    private static final class _XMLReader
        extends CharArrayReader
    {
        /**
         * Constructs an instance.
         */
        _XMLReader()
        {
            super(_EMPTY_CHAR_ARRAY);
        }

        /**
         * Resets the XML buffer.
         *
         * @param xml The new XML buffer.
         */
        void reset(@Nonnull final char[] xml)
        {
            synchronized (getLock()) {
                setBuffer(xml);
            }
        }

        @Nonnull
        @CheckReturnValue
        private Object getLock()
        {
            return lock;
        }

        private void setBuffer(@Nonnull final char[] xml)
        {
            this.markedPos = 0;
            this.pos = 0;
            this.buf = xml;
            this.count = xml.length;
        }

        private static final char[] _EMPTY_CHAR_ARRAY = new char[0];
    }


    /**
     * XML writer.
     */
    private static final class _XMLWriter
        extends CharArrayWriter
    {
        /**
         * Constructs an instance.
         */
        _XMLWriter() {}
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
