/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Frame.java 4085 2019-06-16 15:17:12Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Traces;
import org.rvpf.pap.TraceBuffer;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.pap.dnp3.DNP3ProtocolException;

/**
 * Frame.
 */
public final class Frame
{
    /**
     * Constructs an instance.
     *
     * @param header The header.
     * @param data The data.
     */
    Frame(@Nonnull final Header header, @Nonnull final byte[] data)
    {
        _header = header;
        _data = data;
    }

    /**
     * Gets the data.
     *
     * @return The data.
     */
    @Nonnull
    @CheckReturnValue
    public byte[] getData()
    {
        return _data;
    }

    /**
     * Gets the header.
     *
     * @return The header.
     */
    @Nonnull
    @CheckReturnValue
    public Header getHeader()
    {
        return _header;
    }

    /** Maximum data size. */
    public static final int MAXIMUM_DATA_SIZE = 250;

    /** Maximum frame size. */
    public static final int MAXIMUM_FRAME_SIZE = 292;

    /** Minimum frame size. */
    public static final int MINIMUM_FRAME_SIZE = 10;

    /** No data. */
    public static final byte[] NO_DATA = new byte[0];

    /**  */

    static final int _BYTE_BITS = 8;
    static final int _BYTE_MASK = 0xFF;
    static final int _DATA_BLOCK_SIZE = 16;
    static final int _DFC_MASK = 0x10;
    static final int _DIR_MASK = 0x80;
    static final int _FCB_MASK = 0x20;
    static final int _FCV_MASK = 0x10;
    static final int _FRAME_DATA_LIMIT = MAXIMUM_DATA_SIZE;
    static final int _FUNCTION_CODE_MASK = 0x0F;
    static final int _HEADER_LENGTH = 5;
    static final int _PRM_MASK = 0x40;
    static final short _START_FIELD = 0x6405;

    private final byte[] _data;
    private final Header _header;

    /**
     * Primary code.
     */
    public enum PrimaryCode
        implements FunctionCode
    {
        RESET_LINK_STATES(0),
        TEST_LINK_STATES(2),
        CONFIRMED_USER_DATA(3),
        UNCONFIRMED_USER_DATA(4),
        REQUEST_LINK_STATUS(9);

        /**
         * Constructs an instance.
         *
         * @param code The code.
         */
        PrimaryCode(final int code)
        {
            _code = code;
        }

        /**
         * Gets the instance for a code.
         *
         * @param code The code.
         *
         * @return The instance.
         *
         * @throws DNP3ProtocolException On bad code.
         */
        @Nonnull
        @CheckReturnValue
        public static PrimaryCode instance(
                final int code)
            throws DNP3ProtocolException
        {
            if ((code < 0) || (_CODE_ARRAY.length <= code)) {
                throw new DNP3ProtocolException(
                    DNP3Messages.UNEXPECTED_FRAME_DATA,
                    String.valueOf(code));
            }

            return _CODE_ARRAY[code];
        }

        /** {@inheritDoc}
         */
        @Override
        public int getCode()
        {
            return _code;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPrimary()
        {
            return true;
        }

        private static final PrimaryCode[] _CODE_ARRAY =
            new PrimaryCode[MAXIMUM_VALUE + 1];

        static {
            Arrays
                .stream(values())
                .forEach(code -> _CODE_ARRAY[code.getCode()] = code);
        }

        private final int _code;
    }

    /**
     * Secondary code.
     */
    public enum SecondaryCode
        implements FunctionCode
    {
        ACK(0),
        NACK(1),
        LINK_STATUS(11),
        NOT_SUPPORTED(15);

        /**
         * Constructs an instance.
         *
         * @param code The code.
         */
        SecondaryCode(final int code)
        {
            _code = code;
        }

        /**
         * Gets the instance for a code.
         *
         * @param code The code.
         *
         * @return The instance (null if unknown).
         *
         * @throws DNP3ProtocolException On bad code.
         */
        @Nonnull
        @CheckReturnValue
        public static SecondaryCode instance(
                final int code)
            throws DNP3ProtocolException
        {
            if ((code < 0) || (_CODE_ARRAY.length <= code)) {
                throw new DNP3ProtocolException(
                    DNP3Messages.UNEXPECTED_FRAME_DATA,
                    String.valueOf(code));
            }

            return _CODE_ARRAY[code];
        }

        /** {@inheritDoc}
         */
        @Override
        public int getCode()
        {
            return _code;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPrimary()
        {
            return false;
        }

        private static final SecondaryCode[] _CODE_ARRAY =
            new SecondaryCode[MAXIMUM_VALUE + 1];

        static {
            Arrays
                .stream(values())
                .forEach(code -> _CODE_ARRAY[code.getCode()] = code);
        }

        private final int _code;
    }

    /**
     * Function code.
     */
    public interface FunctionCode
    {
        /**
         * Gets the code.
         *
         * @return The code.
         */
        @CheckReturnValue
        int getCode();

        /**
         * Asks if this is a primary code.
         *
         * @return True if this is a primary code.
         */
        @CheckReturnValue
        boolean isPrimary();

        /** Maximum value. */
        int MAXIMUM_VALUE = 15;
    }


    /**
     * Header.
     */
    public static final class Header
    {
        /**
         * Constructs an instance.
         *
         * @param functionCode The function code.
         * @param dataLength The data length.
         * @param source The source.
         * @param destination The destination.
         * @param frameCountBit The frame count bit.
         * @param frameCountValid The frame count valid indicator.
         * @param dataFlowControl The data flow control indicator.
         */
        Header(
                @Nonnull final FunctionCode functionCode,
                final int dataLength,
                final short source,
                final short destination,
                final boolean frameCountBit,
                final boolean frameCountValid,
                final boolean dataFlowControl)
        {
            _functionCode = functionCode;
            _dataLength = dataLength;
            _source = source;
            _destination = destination;
            _frameCountBit = frameCountBit;
            _frameCountValid = frameCountValid;
            _dataFlowControl = dataFlowControl;
        }

        /**
         * Gets the data length.
         *
         * @return The data length.
         */
        @CheckReturnValue
        public int getDataLength()
        {
            return _dataLength;
        }

        /**
         * Gets the destination.
         *
         * @return The destination.
         */
        @CheckReturnValue
        public short getDestination()
        {
            return _destination;
        }

        /**
         * Gets the frame count bit.
         *
         * @return The frame count bit.
         */
        @CheckReturnValue
        public boolean getFrameCountBit()
        {
            return _frameCountBit;
        }

        /**
         * Gets the function code.
         *
         * @return The function code.
         */
        @Nonnull
        @CheckReturnValue
        public FunctionCode getFunctionCode()
        {
            return _functionCode;
        }

        /**
         * Gets the source.
         *
         * @return The source.
         */
        @CheckReturnValue
        public short getSource()
        {
            return _source;
        }

        /**
         * Asks if the data flow control is active.
         *
         * @return True if the data flow control is active.
         */
        @CheckReturnValue
        public boolean hasDataFlowControl()
        {
            return _dataFlowControl;
        }

        /**
         * Asks if the frame count bit is valid.
         *
         * @return True if the frame count bit is valid.
         */
        @CheckReturnValue
        public boolean isFrameCountValid()
        {
            return _frameCountValid;
        }

        private final boolean _dataFlowControl;
        private final int _dataLength;
        private final short _destination;
        private final boolean _frameCountBit;
        private final boolean _frameCountValid;
        private final FunctionCode _functionCode;
        private final short _source;
    }


    /**
     * Receiver.
     */
    static final class Receiver
        extends _Traced
    {
        /**
         * Constructs an instance.
         *
         * @param remoteEndPoint The remote end point.
         */
        Receiver(@Nonnull final RemoteEndPoint remoteEndPoint)
        {
            super(remoteEndPoint);

            _frameBuffer.flip();    // Starts empty.
            _receivedBytes = newTraceBuffer();
            trace("Receiver begins");
        }

        /** {@inheritDoc}
         */
        @Override
        void close()
        {
            trace("Receiver ends");

            super.close();
        }

        /**
         * Receives a frame.
         *
         * @param connection The connection.
         *
         * @return The received frame.
         *
         * @throws IOException On I/O exception.
         */
        @Nonnull
        @CheckReturnValue
        Frame receive(@Nonnull final Connection connection)
            throws IOException
        {
            final Header header;
            final byte[] data;

            try {
                header = _receiveHeader(connection);
                data = new byte[header.getDataLength()];

                int offset = 0;

                while (offset < data.length) {
                    offset += _receiveData(connection, data, offset);
                }
            } catch (final IOException exception) {
                // Drops the input frame content.
                // Needed for UDP since the socket will be kept open.
                _frameBuffer.clear();
                _frameBuffer.flip();

                throw exception;
            }

            if (isTracesEnabled()) {
                trace("Rx: " + _receivedBytes);
                _receivedBytes.reset();
            }

            _LOGGER
                .trace(
                    DNP3Messages.RECEIVED_FROM,
                    header.getFunctionCode(),
                    connection,
                    Integer.toHexString(header.getSource() & 0xFFFF),
                    Integer.toHexString(header.getDestination() & 0xFFFF));

            return new Frame(header, data);
        }

        private byte _receiveByte(
                final Connection connection)
            throws IOException
        {
            if (!_frameBuffer.hasRemaining()) {
                _frameBuffer.clear();
                connection.receive(_frameBuffer);
                _frameBuffer.flip();

                if (!_frameBuffer.hasRemaining()) {
                    throw new ClosedChannelException();
                }
            }

            final byte b = _frameBuffer.get();

            _receivedBytes.append(b);

            return b;
        }

        private byte _receiveByteThruCRC(
                final Connection connection)
            throws IOException
        {
            final byte b = _receiveByte(connection);

            _crc.update(b);

            return b;
        }

        private int _receiveData(
                final Connection connection,
                final byte[] data,
                int offset)
            throws IOException
        {
            final int length = Math.min(data.length - offset, _DATA_BLOCK_SIZE);

            _crc.reset();

            try {
                for (int i = 0; i < length; ++i) {
                    data[offset++] = _receiveByteThruCRC(connection);
                }
            } catch (final IndexOutOfBoundsException exception) {
                throw new DNP3ProtocolException(
                    DNP3Messages.SEGMENT_BUFFER_OVERFLOW);
            }

            if (_receiveWord(connection) != (short) _crc.getValue()) {
                throw new DNP3ProtocolException(DNP3Messages.BAD_CRC);
            }

            return length;
        }

        private Header _receiveHeader(
                final Connection connection)
            throws IOException
        {
            _crc.reset();

            final int startField = _receiveWordThruCRC(connection);

            if (startField != _START_FIELD) {
                throw new DNP3ProtocolException(
                    DNP3Messages.INVALID_START_FIELD,
                    String.valueOf(startField));
            }

            final int length = _receiveByteThruCRC(connection) & _BYTE_MASK;

            if ((length < _HEADER_LENGTH)
                    || (length > (_HEADER_LENGTH + _FRAME_DATA_LIMIT))) {
                throw new DNP3ProtocolException(
                    DNP3Messages.INVALID_FRAME_LENGTH,
                    String.valueOf(length));
            }

            final int control = _receiveByteThruCRC(connection) & _BYTE_MASK;
            final int code = control & _FUNCTION_CODE_MASK;
            final FunctionCode functionCode;
            final boolean frameCountBit;
            final boolean frameCountValid;
            final boolean dataFlowControl;

            if ((control & _PRM_MASK) != 0) {
                functionCode = PrimaryCode.instance(code);
                frameCountBit = (control & _FCB_MASK) != 0;
                frameCountValid = (control & _FCV_MASK) != 0;
                dataFlowControl = false;
            } else {
                functionCode = SecondaryCode.instance(code);
                frameCountBit = false;
                frameCountValid = false;
                dataFlowControl = (control & _DFC_MASK) != 0;
            }

            final int dataLength = length - _HEADER_LENGTH;

            if (((control & _DIR_MASK) != 0) == connection.isOnMaster()) {
                throw new DNP3ProtocolException(DNP3Messages.INVERTED_DIR_BIT);
            }

            if ((functionCode == PrimaryCode.CONFIRMED_USER_DATA)
                    || (functionCode == PrimaryCode.UNCONFIRMED_USER_DATA)) {
                if (dataLength < 1) {
                    throw new DNP3ProtocolException(
                        DNP3Messages.MISSING_FRAME_DATA,
                        String.valueOf(functionCode));
                }
            } else if (dataLength != 0) {
                throw new DNP3ProtocolException(
                    DNP3Messages.UNEXPECTED_FRAME_DATA,
                    String.valueOf(functionCode));
            }

            final short destination = _receiveWordThruCRC(connection);
            final short source = _receiveWordThruCRC(connection);

            if (_receiveWord(connection) != (short) _crc.getValue()) {
                throw new DNP3ProtocolException(DNP3Messages.BAD_CRC);
            }

            return new Header(
                functionCode,
                dataLength,
                source,
                destination,
                frameCountBit,
                frameCountValid,
                dataFlowControl);
        }

        private short _receiveWord(
                final Connection connection)
            throws IOException
        {
            final byte low = _receiveByte(connection);
            final byte high = _receiveByte(connection);

            return (short) ((high << _BYTE_BITS) | (low & _BYTE_MASK));
        }

        private short _receiveWordThruCRC(
                final Connection connection)
            throws IOException
        {
            final short w = _receiveWord(connection);

            _crc.update(w & _BYTE_MASK);
            _crc.update(w >> _BYTE_BITS);

            return w;
        }

        static final Logger _LOGGER = Logger.getInstance(Receiver.class);

        private final CRC _crc = new CRC();
        private final ByteBuffer _frameBuffer = ByteBuffer
            .allocate(MAXIMUM_FRAME_SIZE);
        private final TraceBuffer _receivedBytes;
    }


    /**
     * Sender.
     */
    static final class Sender
        extends _Traced
    {
        /**
         * Constructs an instance.
         *
         * @param remoteEndPoint The remote end point.
         */
        Sender(@Nonnull final RemoteEndPoint remoteEndPoint)
        {
            super(remoteEndPoint);

            _sentBytes = newTraceBuffer();
            trace("Sender begins");
        }

        /** {@inheritDoc}
         */
        @Override
        void close()
        {
            trace("Sender ends");

            super.close();
        }

        /**
         * Sends a frame.
         *
         * @param connection The connection.
         * @param frame The frame.
         *
         * @throws IOException On I/O exception.
         */
        void send(
                @Nonnull final Connection connection,
                @Nonnull final Frame frame)
            throws IOException
        {
            final Header header = frame.getHeader();

            _sendHeader(connection.isOnMaster(), header);

            final byte[] data = frame.getData();
            int offset = 0;

            while (offset < data.length) {
                offset += _sendData(data, offset);
            }

            _frameBuffer.flip();
            connection.send(_frameBuffer);

            if (isTracesEnabled()) {
                trace("Tx: " + _sentBytes);
                _sentBytes.reset();
            }

            _LOGGER
                .trace(
                    DNP3Messages.SENT_TO,
                    header.getFunctionCode(),
                    connection,
                    Integer.toHexString(header.getSource() & 0xFFFF),
                    Integer.toHexString(header.getDestination() & 0xFFFF));
        }

        private void _sendByte(final byte b)
        {
            _frameBuffer.put(b);
            _sentBytes.append(b);
        }

        private void _sendByteThruCRC(final byte b)
        {
            _crc.update(b);
            _sendByte(b);
        }

        private int _sendData(final byte[] data, int offset)
        {
            final int length = Math.min(data.length - offset, _DATA_BLOCK_SIZE);

            _crc.reset();

            for (int i = 0; i < length; ++i) {
                _sendByteThruCRC(data[offset++]);
            }

            _sendWord((short) _crc.getValue());

            return length;
        }

        private void _sendHeader(final boolean onMaster, final Header header)
        {
            final FunctionCode functionCode = header.getFunctionCode();
            int control = functionCode.getCode();

            if (onMaster) {
                control |= _DIR_MASK;
            }

            if (functionCode.isPrimary()) {
                control |= _PRM_MASK;

                if (header.isFrameCountValid()) {
                    if (header.getFrameCountBit()) {
                        control |= _FCB_MASK;
                    }

                    control |= _FCV_MASK;
                }
            } else {
                if (header.hasDataFlowControl()) {
                    control |= _DFC_MASK;
                }
            }

            _frameBuffer.clear();
            _crc.reset();

            _sendWordThruCRC(_START_FIELD);
            _sendByteThruCRC((byte) (_HEADER_LENGTH + header.getDataLength()));
            _sendByteThruCRC((byte) control);
            _sendWordThruCRC(header.getDestination());
            _sendWordThruCRC(header.getSource());

            _sendWord((short) _crc.getValue());
        }

        private void _sendWord(final short w)
        {
            _sendByte((byte) w);
            _sendByte((byte) (w >> _BYTE_BITS));
        }

        private void _sendWordThruCRC(final short w)
        {
            _sendByteThruCRC((byte) w);
            _sendByteThruCRC((byte) (w >> _BYTE_BITS));
        }

        static final Logger _LOGGER = Logger.getInstance(Sender.class);

        private final CRC _crc = new CRC();
        private final ByteBuffer _frameBuffer = ByteBuffer
            .allocate(MAXIMUM_FRAME_SIZE);
        private final TraceBuffer _sentBytes;
    }


    /**
     * Traced.
     */
    private abstract static class _Traced
    {
        /**
         * Constructs an instance.
         *
         * @param remoteEndPoint The remote end point.
         */
        _Traced(@Nonnull final RemoteEndPoint remoteEndPoint)
        {
            _proxyName = remoteEndPoint.getRemoteProxyName();
            _traces = remoteEndPoint.getConnectionManager().getTraces();
        }

        /**
         * Asks if traces are enabled.
         *
         * @return True if traces are enabled.
         */
        @CheckReturnValue
        protected final boolean isTracesEnabled()
        {
            return _traces.isEnabled();
        }

        /**
         * Returns a new trace buffer.
         *
         * @return The trace buffer.
         */
        @Nonnull
        @CheckReturnValue
        protected final TraceBuffer newTraceBuffer()
        {
            return new TraceBuffer(_traces.isEnabled());
        }

        /**
         * Traces.
         *
         * @param message The message.
         */
        protected final void trace(@Nonnull final String message)
        {
            if (_traces.isEnabled()) {
                _traces.add(_proxyName, message);
                _traces.commit();
            }
        }

        /**
         * Closes.
         */
        void close()
        {
            if (_traces.isEnabled()) {
                _traces.commit();
            }
        }

        private final Optional<String> _proxyName;
        private final Traces _traces;
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
