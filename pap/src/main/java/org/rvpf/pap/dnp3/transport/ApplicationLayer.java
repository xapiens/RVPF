/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ApplicationLayer.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.pap.dnp3.DNP3ProtocolException;

/**
 * Application layer.
 */
public final class ApplicationLayer
{
    /**
     * Constructs an instance.
     *
     * @param association The association.
     */
    ApplicationLayer(@Nonnull final Association association)
    {
        _association = association;
        _inputBuffer.order(ByteOrder.LITTLE_ENDIAN);
        _outputBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Returns the next solicited sequence.
     *
     * @return The next solicited sequence.
     */
    @CheckReturnValue
    public byte nextSolicitedSequence()
    {
        return (byte) (_nextSolicitedSequence.getAndIncrement() & 0xF);
    }

    /**
     * Returns the next unsolicited sequence.
     *
     * @return The next unsolicited sequence.
     */
    @CheckReturnValue
    public byte nextUnsolicitedSequence()
    {
        return (byte) (_nextUnsolicitedSequence.getAndIncrement() & 0xF);
    }

    /**
     * Receives a fragment.
     *
     * @return The fragment.
     *
     * @throws IOException On I/O exception.
     */
    @Nonnull
    @CheckReturnValue
    public Fragment receive()
        throws IOException
    {
        final TransportFunction transportFunction = _association
            .getTransportFunction();
        final Fragment.Header header;
        final Fragment fragment;

        _inputBuffer.clear();
        transportFunction.receive(_inputBuffer);
        _inputBuffer.flip();

        header = Fragment.Header
            .newInstance(Optional.empty(), !_association.isWithOutstation());
        fragment = new Fragment(_association, header);

        try {
            fragment.loadFromBuffer(_inputBuffer);
        } catch (final BufferUnderflowException exception) {
            throw new DNP3ProtocolException(
                exception,
                DNP3Messages.APPLICATION_HEADER_INCOMPLETE);
        }

        _LOGGER
            .trace(
                () -> new Message(
                    _association.isWithOutstation()
                    ? DNP3Messages.RECEIVED_RESPONSE
                    : DNP3Messages.RECEIVED_REQUEST,
                    fragment));

        return fragment;
    }

    /**
     * Sends a fragment.
     *
     * @param fragment The fragment.
     *
     * @throws IOException On I/O exception.
     */
    public void send(@Nonnull final Fragment fragment)
        throws IOException
    {
        _LOGGER
            .trace(
                () -> new Message(
                    _association.isWithOutstation()
                    ? DNP3Messages.SENDING_REQUEST
                    : DNP3Messages.SENDING_RESPONSE,
                    fragment));

        _outputBuffer.clear();
        fragment.dumpToBuffer(_outputBuffer);
        _outputBuffer.flip();

        _association.getTransportFunction().send(_outputBuffer);
    }

    /** Maximum fragment size. */
    public static final int MAXIMUM_FRAGMENT_SIZE = 2048;

    /**  */

    private static final Logger _LOGGER = Logger
        .getInstance(ApplicationLayer.class);

    private final Association _association;
    private final ByteBuffer _inputBuffer = ByteBuffer
        .allocate(MAXIMUM_FRAGMENT_SIZE);
    private final AtomicInteger _nextUnsolicitedSequence = new AtomicInteger();
    private final AtomicInteger _nextSolicitedSequence = new AtomicInteger();
    private final ByteBuffer _outputBuffer = ByteBuffer
        .allocate(MAXIMUM_FRAGMENT_SIZE);
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
