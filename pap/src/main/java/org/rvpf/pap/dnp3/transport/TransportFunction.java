/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TransportFunction.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.Nonnull;

import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.pap.dnp3.DNP3ProtocolException;

/**
 * Transport function.
 */
public final class TransportFunction
{
    /**
     * Constructs an instance.
     *
     * @param association The association.
     */
    public TransportFunction(@Nonnull final Association association)
    {
        _association = association;
        _inputSegment.order(ByteOrder.LITTLE_ENDIAN);
        _outputSegment.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Receives.
     *
     * @param fragmentBuffer A buffer to receive data.
     *
     * @throws IOException On I/O exception.
     */
    public void receive(
            @Nonnull final ByteBuffer fragmentBuffer)
        throws IOException
    {
        boolean first = true;
        byte header;

        do {
            _inputSegment.clear();
            _association.getDataLinkLayer().receive(_inputSegment);
            _inputSegment.flip();

            header = _inputSegment.get();

            if ((header & _FIR_MASK) != (first? _FIR_MASK: 0)) {
                throw new DNP3ProtocolException(DNP3Messages.INVERTED_FIR_BIT);
            }

            if (first) {
                _inputSequence = header & _SEQUENCE_MASK;
                first = false;
            } else {
                if ((header & _SEQUENCE_MASK)
                        != (++_inputSequence & _SEQUENCE_MASK)) {
                    throw new DNP3ProtocolException(
                        DNP3Messages.UNEXPECTED_SEGMENT_SEQUENCE,
                        String.valueOf(_inputSequence),
                        String.valueOf(header & _SEQUENCE_MASK));
                }
            }

            try {
                while (_inputSegment.hasRemaining()) {
                    fragmentBuffer.put(_inputSegment.get());
                }
            } catch (final BufferOverflowException exception) {
                throw new DNP3ProtocolException(
                    DNP3Messages.FRAGMENT_BUFFER_OVERFLOW);
            }
        } while ((header & _FIN_MASK) == 0);
    }

    /**
     * Sends.
     *
     * @param fragmentBuffer A buffer containing the data.
     *
     * @throws IOException On I/O exception.
     */
    public void send(
            @Nonnull final ByteBuffer fragmentBuffer)
        throws IOException
    {
        int header = _FIR_MASK;

        while (fragmentBuffer.hasRemaining()) {
            _outputSegment.clear();

            if (fragmentBuffer.remaining() < _outputSegment.capacity()) {
                header |= _FIN_MASK;
            }

            header &= ~_SEQUENCE_MASK;
            header |= _outputSequence++ & _SEQUENCE_MASK;
            _outputSegment.put((byte) header);

            do {
                _outputSegment.put(fragmentBuffer.get());
            } while (fragmentBuffer.hasRemaining()
                     && _outputSegment.hasRemaining());

            _outputSegment.flip();
            _association.getDataLinkLayer().send(_outputSegment);
        }
    }

    /** Maximum segment size. */
    public static final int MAXIMUM_SEGMENT_SIZE = Frame.MAXIMUM_DATA_SIZE;

    /**  */

    private static final int _FIN_MASK = 0x80;
    private static final int _FIR_MASK = 0x40;
    private static final int _SEQUENCE_MASK = 0x3F;

    private final Association _association;
    private final ByteBuffer _inputSegment = ByteBuffer
        .allocate(MAXIMUM_SEGMENT_SIZE);
    private int _inputSequence;
    private final ByteBuffer _outputSegment = ByteBuffer
        .allocate(MAXIMUM_SEGMENT_SIZE);
    private int _outputSequence;
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
