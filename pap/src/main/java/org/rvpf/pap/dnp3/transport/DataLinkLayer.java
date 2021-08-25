/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DataLinkLayer.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.ConnectException;

import java.nio.ByteBuffer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.pap.dnp3.DNP3Messages;

/**
 * Data link layer.
 */
public final class DataLinkLayer
{
    /**
     * Constructs an instance.
     *
     * @param association The association.
     */
    public DataLinkLayer(@Nonnull final Association association)
    {
        _association = association;
        _frameSender = new Frame.Sender(association.getRemoteEndPoint());
        _primaryState.set(PrimaryState.SEC_UN_RESET_IDLE);
        _secondaryState.set(SecondaryState.UN_RESET);
    }

    /**
     * Accepts a frame.
     *
     * @param frame The frame.
     *
     * @throws IOException On I/O exception.
     */
    public void accept(@Nonnull final Frame frame)
        throws IOException
    {
        final Frame.Header frameHeader = frame.getHeader();
        final Frame.FunctionCode functionCode = frameHeader.getFunctionCode();

        _linkActive = true;

        if (functionCode.isPrimary()) {
            switch ((Frame.PrimaryCode) functionCode) {
                case UNCONFIRMED_USER_DATA: {
                    if (!frameHeader.isFrameCountValid()) {
                        _inputSegments.add(frame.getData());
                    }

                    break;
                }
                case CONFIRMED_USER_DATA: {
                    if (frameHeader.isFrameCountValid()) {
                        if (_secondaryState.get() == SecondaryState.IDLE) {
                            if (frameHeader.getFrameCountBit()
                                    == _expectedFrameCountBit) {
                                _expectedFrameCountBit =
                                    !_expectedFrameCountBit;
                                _inputSegments.add(frame.getData());
                            }

                            _send(Frame.SecondaryCode.ACK);
                        } else {
                            _send(Frame.SecondaryCode.NACK);
                        }
                    } else {
                        _send(Frame.SecondaryCode.NOT_SUPPORTED);
                    }

                    break;
                }
                case RESET_LINK_STATES: {
                    if (frameHeader.isFrameCountValid()) {
                        _send(Frame.SecondaryCode.NOT_SUPPORTED);
                    } else {
                        _expectedFrameCountBit = true;
                        _send(Frame.SecondaryCode.ACK);
                        _secondaryState.compareAndSet(
                            SecondaryState.UN_RESET,
                            SecondaryState.IDLE);
                    }

                    break;
                }
                case REQUEST_LINK_STATUS: {
                    if (frameHeader.isFrameCountValid()) {
                        _send(Frame.SecondaryCode.NOT_SUPPORTED);
                    } else {
                        _send(Frame.SecondaryCode.LINK_STATUS);
                    }

                    break;
                }
                case TEST_LINK_STATES: {
                    if (frameHeader.isFrameCountValid()) {
                        if (_secondaryState.get() == SecondaryState.IDLE) {
                            if (frameHeader.getFrameCountBit()
                                    == _expectedFrameCountBit) {
                                _expectedFrameCountBit =
                                    !_expectedFrameCountBit;
                                _send(Frame.SecondaryCode.ACK);
                            } else {
                                _send(_ackOrNack);
                            }
                        } else {
                            _send(Frame.SecondaryCode.NACK);
                        }
                    } else {
                        _send(Frame.SecondaryCode.NOT_SUPPORTED);
                    }

                    break;
                }
                default: {
                    _send(Frame.SecondaryCode.NOT_SUPPORTED);

                    break;
                }
            }
        } else {
            switch ((Frame.SecondaryCode) functionCode) {
                case ACK: {
                    break;
                }
                case LINK_STATUS: {
                    if (_primaryState.compareAndSet(
                            PrimaryState.UR_LINK_STATUS_WAIT,
                            PrimaryState.SEC_UN_RESET_IDLE)) {
                        final CountDownLatch watchdogLatch = _watchdogLatch;

                        if (watchdogLatch != null) {
                            watchdogLatch.countDown();
                        }
                    } else {
                        _LOGGER.debug(
                            DNP3Messages.IGNORED_FRAME,
                            functionCode,
                            _association.getRemoteEndPoint().getRemoteProxy(),
                            Integer.toHexString(
                                frameHeader.getSource() & 0xFFFF),
                            Integer.toHexString(
                                frameHeader.getDestination() & 0xFFFF));
                    }

                    break;
                }
                case NACK: {
                    break;
                }
                case NOT_SUPPORTED: {
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    /**
     * Closes.
     */
    public void close()
    {
        _frameSender.close();
    }

    /**
     * Gets the association.
     *
     * @return The association.
     */
    @Nonnull
    @CheckReturnValue
    public Association getAssociation()
    {
        return _association;
    }

    /**
     * Asks if the link is active.
     *
     * @param replyTimeout The reply timeout.
     *
     * @return True if the link is active, null if its status is unknown.
     *
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    public boolean isLinkActive(final long replyTimeout)
        throws IOException
    {
        if (!_linkActive) {
            if (_primaryState.compareAndSet(
                    PrimaryState.SEC_UN_RESET_IDLE,
                    PrimaryState.UR_LINK_STATUS_WAIT)) {
                _watchdogLatch = new CountDownLatch(1);

                try {
                    final Frame sentFrame = _send(
                        Frame.PrimaryCode.REQUEST_LINK_STATUS);

                    if (!_watchdogLatch.await(
                            replyTimeout,
                            TimeUnit.MILLISECONDS)) {
                        _primaryState.compareAndSet(
                            PrimaryState.UR_LINK_STATUS_WAIT,
                            PrimaryState.SEC_UN_RESET_IDLE);

                        final Frame.Header frameHeader = sentFrame.getHeader();

                        _LOGGER.debug(
                            DNP3Messages.FRAME_RESPONSE_TIMEOUT,
                            frameHeader.getFunctionCode(),
                            _association.getRemoteEndPoint().getRemoteProxy(),
                            Integer.toHexString(
                                frameHeader.getSource() & 0xFFFF),
                            Integer.toHexString(
                                frameHeader.getDestination() & 0xFFFF));
                    }
                } catch (final InterruptedException exception) {
                    throw (IOException) new InterruptedIOException().initCause(
                        exception);
                } finally {
                    _watchdogLatch = null;
                }
            } else {
                _LOGGER.warn(
                    DNP3Messages.UNEXPECTED_LINK_STATE,
                    _association,
                    PrimaryState.SEC_UN_RESET_IDLE,
                    _primaryState);
            }
        }

        return _linkActive;
    }

    /**
     * Receives.
     *
     * @param segmentBuffer A buffer to receive data.
     *
     * @throws IOException On I/O exception.
     */
    public void receive(
            @Nonnull final ByteBuffer segmentBuffer)
        throws IOException
    {
        final byte[] inputSegment;

        try {
            inputSegment = _inputSegments.take();
        } catch (final InterruptedException exception) {
            throw new InterruptedIOException();
        }

        segmentBuffer.put(inputSegment);
    }

    /**
     * Sends.
     *
     * @param segmentBuffer A buffer containing the data.
     *
     * @throws IOException On I/O exception.
     */
    public void send(@Nonnull final ByteBuffer segmentBuffer)
        throws IOException
    {
        synchronized (_sendMutex) {
            while (segmentBuffer.hasRemaining()) {
                _send(
                    Frame.PrimaryCode.UNCONFIRMED_USER_DATA,
                    false,
                    false,
                    segmentBuffer);
            }
        }
    }

    /**
     * Sends a frame with a specified function code.
     *
     * @param functionCode The function code (no user data).
     *
     * @return The frame sent.
     *
     * @throws IOException On I/O exception.
     */
    private Frame _send(
            @Nonnull final Frame.FunctionCode functionCode)
        throws IOException
    {
        final Frame frame;

        synchronized (_outputMutex) {
            final Frame.Header header = new Frame.Header(
                functionCode,
                0,
                _association.getLocalDeviceAddress(),
                _association.getRemoteDeviceAddress(),
                false,
                false,
                false);
            final Connection connection = _association.getConnection();

            if (connection == null) {
                throw new ConnectException();
            }

            frame = new Frame(header, Frame.NO_DATA);
            _frameSender.send(connection, frame);

            if ((functionCode == Frame.SecondaryCode.ACK)
                    || (functionCode == Frame.SecondaryCode.NACK)) {
                _ackOrNack = functionCode;
            }
        }

        return frame;
    }

    /**
     * Sends.
     *
     * @param functionCode The function code.
     * @param frameCountValid The frame count valid indicator.
     * @param frameCountBit The frame count bit.
     * @param segmentBuffer The buffer containing the data.
     *
     * @throws IOException On I/O exception.
     */
    private void _send(
            @Nonnull final Frame.PrimaryCode functionCode,
            final boolean frameCountValid,
            final boolean frameCountBit,
            final ByteBuffer segmentBuffer)
        throws IOException
    {
        synchronized (_outputMutex) {
            final int frameDataLength = Math.min(
                segmentBuffer.remaining(),
                _FRAME_DATA_LIMIT);

            final Frame.Header header = new Frame.Header(
                functionCode,
                frameDataLength,
                _association.getLocalDeviceAddress(),
                _association.getRemoteDeviceAddress(),
                frameCountBit,
                frameCountValid,
                false);
            final byte[] data = new byte[frameDataLength];

            segmentBuffer.get(data);
            _frameSender.send(
                _association.getConnection(),
                new Frame(header, data));
        }
    }

    private static final int _FRAME_DATA_LIMIT = Frame.MAXIMUM_DATA_SIZE;
    private static final Logger _LOGGER = Logger.getInstance(
        DataLinkLayer.class);

    private Frame.FunctionCode _ackOrNack = Frame.SecondaryCode.NACK;
    private final Association _association;
    private boolean _expectedFrameCountBit;
    private final Frame.Sender _frameSender;
    private final BlockingQueue<byte[]> _inputSegments =
        new LinkedBlockingQueue<>();
    private volatile boolean _linkActive;
    private final Object _outputMutex = new Object();
    private final AtomicReference<PrimaryState> _primaryState =
        new AtomicReference<>();
    private final AtomicReference<SecondaryState> _secondaryState =
        new AtomicReference<>();
    private final Object _sendMutex = new Object();
    private volatile CountDownLatch _watchdogLatch;

    /**
     * Primary state.
     */
    protected enum PrimaryState
    {
        SEC_UN_RESET_IDLE,
        SEC_RESET_IDLE,
        RESET_LINK_WAIT_1,
        RESET_LINK_WAIT_2,
        UR_LINK_STATUS_WAIT,
        TEST_WAIT,
        CFMD_DATA_WAIT,
        R_LINK_STATUS_WAIT;
    }

    /**
     * Secondary state.
     */
    enum SecondaryState
    {
        UN_RESET,
        IDLE;
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
