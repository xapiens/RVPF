/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Transport.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.pap.modbus.transport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.ProtocolException;
import java.net.Socket;
import java.net.SocketException;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.pap.SerialPortInputStream;
import org.rvpf.pap.SerialPortOutputStream;
import org.rvpf.pap.SerialPortWrapper;
import org.rvpf.pap.SerialPortWrapper.StatusChangeListener;
import org.rvpf.pap.TraceBuffer;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.message.Prefix;

import jssc.SerialPort;

/**
 * Transport.
 */
public abstract class Transport
    implements Closeable
{
    /**
     * Constructs an instance.
     *
     * @param unitIdentifier The unit identifier.
     * @param traces The traces (optional).
     */
    protected Transport(final byte unitIdentifier, @Nonnull final Traces traces)
    {
        _unitIdentifier = unitIdentifier;
        _traces = traces;
        _receivedBytes = new TraceBuffer(_traces.isEnabled());
        _sentBytes = new TraceBuffer(_traces.isEnabled());
    }

    /**
     * Returns a new serial port transport.
     *
     * @param serialPort The serial port.
     * @param serialMode The serial mode.
     * @param unitIdentifier The unit identifier.
     * @param traces The traces.
     *
     * @return The new serial port transport.
     */
    @Nonnull
    @CheckReturnValue
    public static Transport newSerialPortTransport(
            @Nonnull final SerialPortWrapper serialPort,
            final int serialMode,
            final byte unitIdentifier,
            @Nonnull final Traces traces)
    {
        switch (serialMode) {
            case SERIAL_MODE_RTU: {
                return new _OnSerialRTUPort(serialPort, unitIdentifier, traces);
            }
            case SERIAL_MODE_ASCII: {
                return new _OnSerialASCIIPort(
                    serialPort,
                    unitIdentifier,
                    traces);
            }
            default: {
                throw new InternalError();
            }
        }
    }

    /**
     * Returns a new socket transport.
     *
     * @param socket The socket.
     * @param unitIdentifier The unit identifier.
     * @param traces The traces.
     *
     * @return The new socket transport.
     */
    @Nonnull
    @CheckReturnValue
    public static Transport newSocketTransport(
            @Nonnull final Socket socket,
            final byte unitIdentifier,
            @Nonnull final Traces traces)
    {
        return new _OnSocket(socket, unitIdentifier, traces);
    }

    /**
     * Returns the serial mode for its name ('RTU' or 'ASCII').
     *
     * @param serialModeName The serial mode name.
     *
     * @return The serial mode.
     */
    @CheckReturnValue
    public static int serialMode(final String serialModeName)
    {
        if ((serialModeName == null) || serialModeName.isEmpty()) {
            return SERIAL_MODE_RTU;
        }

        switch (serialModeName.toUpperCase(Locale.ROOT)) {
            case SERIAL_MODE_RTU_NAME: {
                return SERIAL_MODE_RTU;
            }
            case SERIAL_MODE_ASCII_NAME: {
                return SERIAL_MODE_ASCII;
            }
            default: {
                Logger
                    .getInstance(Transport.class)
                    .warn(
                        ModbusMessages.UNRECOGNIZED_SERIAL_MODE,
                        serialModeName);

                return SERIAL_MODE_RTU;
            }
        }
    }

    /**
     * Returns the number of data bits per character for a serial mode.
     *
     * @param serialMode The serial mode.
     *
     * @return The number of character data bits.
     */
    @CheckReturnValue
    public static int serialModeDataBits(final int serialMode)
    {
        switch (serialMode) {
            case SERIAL_MODE_RTU: {
                return SERIAL_MODE_RTU_DATA_BITS;
            }
            case SERIAL_MODE_ASCII: {
                return SERIAL_MODE_ASCII_DATA_BITS;
            }
            default: {
                throw new InternalError();
            }
        }
    }

    /**
     * Returns the name of a serial mode.
     *
     * @param serialMode The seerial mode.
     *
     * @return The name of a serial mode.
     */
    @Nonnull
    @CheckReturnValue
    public static String serialModeName(final int serialMode)
    {
        switch (serialMode) {
            case SERIAL_MODE_RTU: {
                return SERIAL_MODE_RTU_NAME;
            }
            case SERIAL_MODE_ASCII: {
                return SERIAL_MODE_ASCII_NAME;
            }
            default: {
                throw new InternalError();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
        throws IOException
    {
        if (!_sentBytes.isEmpty()) {
            traceSentBytes();
        }

        if (!_receivedBytes.isEmpty()) {
            traceReceivedBytes();
        }

        trace("End");

        if (_traces.isEnabled()) {
            _traces.commit();
            _traces.tearDown();
        }
    }

    /**
     * Flushes the output.
     *
     * @throws IOException On I/O exception.
     */
    public final void flush()
        throws IOException
    {
        _sendStream.flush();
    }

    /**
     * Gets the batch size.
     *
     * @return The batch size.
     */
    @CheckReturnValue
    public abstract int getBatchSize();

    /**
     * Gets the local address.
     *
     * @return The local address.
     */
    @Nonnull
    @CheckReturnValue
    public abstract String getLocalAddress();

    /**
     * Gets the remote address.
     *
     * @return The remote address.
     */
    @Nonnull
    @CheckReturnValue
    public abstract String getRemoteAddress();

    /**
     * Gets the unit identifier.
     *
     * @return The unit identifier.
     */
    @CheckReturnValue
    public byte getUnitIdentifier()
    {
        return _unitIdentifier;
    }

    /**
     * Returns a new prefix.
     *
     * @return A new prefix.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Prefix newPrefix();

    /**
     * Called after a message has been received.
     *
     * @throws InterruptedException When interrupted.
     */
    public abstract void onMessageReceiveCompleted()
        throws InterruptedException;

    /**
     * Called before sending a message.
     *
     * @throws IOException On I/O exception.
     */
    public abstract void onMessageSendBegins()
        throws IOException;

    /**
     * Receives a byte.
     *
     * @return The byte.
     *
     * @throws EOFException On end-of-file.
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    public byte receiveByte()
        throws EOFException, IOException
    {
        final int read = _receiveStream.read();

        if (read < 0) {
            throw new EOFException();
        }

        _receivedBytes.append((byte) read);

        return (byte) read;
    }

    /**
     * Receives the prefix.
     *
     * @return The prefix.
     *
     * @throws EOFException On end-of-file.
     * @throws IOException On I/O exception.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Prefix receivePrefix()
        throws EOFException, IOException;

    /**
     * Receives a short.
     *
     * @return The short.
     *
     * @throws EOFException On end-of-file.
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    public final short receiveShort()
        throws EOFException, IOException
    {
        final byte read1 = receiveByte();
        final byte read2 = receiveByte();

        return (short) (_littleEndian
                ? ((read1 & 0xFF) | (read2 << 8))
                : ((read1 << 8) | (read2 & 0xFF)));
    }

    /**
     * Receives the suffix.
     *
     * @return True if the suffix is accepted.
     *
     * @throws EOFException On end-of-file.
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    public abstract boolean receiveSuffix()
        throws EOFException, IOException;

    /**
     * Sends a byte.
     *
     * @param byteValue The byte value.
     *
     * @throws IOException On I/O exception.
     */
    public void sendByte(final int byteValue)
        throws IOException
    {
        _sendStream.write(byteValue);
        _sentBytes.append((byte) byteValue);
    }

    /**
     * Sends the prefix.
     *
     * @param prefix The prefix.
     *
     * @throws IOException On I/O exception.
     */
    public abstract void sendPrefix(@Nonnull Prefix prefix)
        throws IOException;

    /**
     * Sends a short.
     *
     * @param shortValue The short value.
     *
     * @throws IOException On I/O exception.
     */
    public final void sendShort(final int shortValue)
        throws IOException
    {
        if (_littleEndian) {
            sendByte(shortValue);
            sendByte(shortValue >> 8);
        } else {
            sendByte(shortValue >> 8);
            sendByte(shortValue);
        }
    }

    /**
     * Sends the suffix.
     *
     * @throws IOException On I/O exception.
     */
    public abstract void sendSuffix()
        throws IOException;

    /**
     * Sets the batch size.
     *
     * @param batchSize The batch size.
     */
    public abstract void setBatchSize(int batchSize);

    /**
     * Sets the little-endian indicator.
     *
     * @param littleEndian The little-endian indicator.
     */
    public final void setLittleEndian(final boolean littleEndian)
    {
        _littleEndian = littleEndian;
    }

    /**
     * Sets the receive timeout.
     *
     * @param receiveTimeout The optional receive timeout.
     */
    public final void setReceiveTimeout(
            @Nonnull final Optional<ElapsedTime> receiveTimeout)
    {
        _receiveTimeout = (receiveTimeout
            .isPresent())? receiveTimeout.get().toMillis(): -1;
    }

    /**
     * Converts a byte value to hex string.
     *
     * @param byteValue The byte value.
     *
     * @return A string of two hex digits.
     */
    @Nonnull
    @CheckReturnValue
    protected static String byteToHexString(final byte byteValue)
    {
        final StringBuilder builder = new StringBuilder();

        builder.append((char) halfByteToHexDigit((byte) (byteValue >> 4)));
        builder.append((char) halfByteToHexDigit(byteValue));

        return builder.toString();
    }

    /**
     * Converts a half byte to a hex digit.
     *
     * @param halfByte The half byte in the 4 lower bits.
     *
     * @return The hex digit.
     */
    @CheckReturnValue
    protected static byte halfByteToHexDigit(byte halfByte)
    {
        final byte hexDigit;

        halfByte &= 0xF;

        if (halfByte < 0xA) {
            hexDigit = (byte) ('0' + halfByte);
        } else {
            hexDigit = (byte) ('A' + halfByte - 0xA);
        }

        return hexDigit;
    }

    /**
     * Converts a hex digit to a half byte.
     *
     * @param hexDigit The hex digit.
     *
     * @return The half byte.
     *
     * @throws ProtocolException On non hex digit.
     */
    @CheckReturnValue
    protected static byte hexDigitToHalfByte(
            final byte hexDigit)
        throws ProtocolException
    {
        if (('0' <= hexDigit) && (hexDigit <= '9')) {
            return (byte) (hexDigit - '0');
        }

        if (('A' <= hexDigit) && (hexDigit <= 'F')) {
            return (byte) (0xA + hexDigit - 'A');
        }

        if (('a' <= hexDigit) && (hexDigit <= 'f')) {
            return (byte) (0xA + hexDigit - 'a');
        }

        throw new ProtocolException();
    }

    /**
     * Gets the receive timeout.
     *
     * @return The receive timeout.
     */
    @CheckReturnValue
    protected long getReceiveTimeout()
    {
        return _receiveTimeout;
    }

    /**
     * Gets the received bytes buffer.
     *
     * @return The received bytes buffer.
     */
    @Nonnull
    @CheckReturnValue
    protected TraceBuffer getReceivedBytes()
    {
        return _receivedBytes;
    }

    /**
     * Gets the sent bytes buffer.
     *
     * @return The sent bytes buffer.
     */
    @Nonnull
    @CheckReturnValue
    protected TraceBuffer getSentBytes()
    {
        return _sentBytes;
    }

    /**
     * Gets the logger for this.
     *
     * @return The logger for this.
     */
    @Nonnull
    @CheckReturnValue
    protected Logger getThisLogger()
    {
        return _logger;
    }

    /**
     * Sets the receive stream.
     *
     * @param receiveStream The receive stream.
     */
    protected final void setReceiveStream(
            @Nonnull final InputStream receiveStream)
    {
        _receiveStream = new BufferedInputStream(receiveStream);
    }

    /**
     * Sets the send stream.
     *
     * @param sendStream The send stream.
     */
    protected final void setSendStream(@Nonnull final OutputStream sendStream)
    {
        _sendStream = new BufferedOutputStream(sendStream);
    }

    /**
     * Traces.
     *
     * @param message The message.
     */
    protected final void trace(@Nonnull final String message)
    {
        if (_traces.isEnabled()) {
            _traces.add(Optional.of(getRemoteAddress()), message);
            _traces.commit();
        }
    }

    /**
     * Traces received bytes.
     */
    protected final void traceReceivedBytes()
    {
        if (_traces.isEnabled()) {
            trace("Received: " + _receivedBytes);
            _receivedBytes.reset();
        }
    }

    /**
     * Traces sent bytes.
     */
    protected final void traceSentBytes()
    {
        if (_traces.isEnabled()) {
            trace("Sent: " + _sentBytes);
            _sentBytes.reset();
        }
    }

    /** Serial mode ASCII. */
    public static final int SERIAL_MODE_ASCII = 2;

    /** Number of character data bits for serial mode ASCII. */
    public static final int SERIAL_MODE_ASCII_DATA_BITS = 7;

    /** Serial mode ASCII name. */
    public static final String SERIAL_MODE_ASCII_NAME = "ASCII";

    /** Serial mode RTU. */
    public static final int SERIAL_MODE_RTU = 1;

    /** Number of character data bits for serial mode RTU. */
    public static final int SERIAL_MODE_RTU_DATA_BITS = 8;

    /** Serial mode RTU name. */
    public static final String SERIAL_MODE_RTU_NAME = "RTU";

    private boolean _littleEndian;
    private final Logger _logger = Logger.getInstance(getClass());
    private InputStream _receiveStream;
    private long _receiveTimeout;
    private final TraceBuffer _receivedBytes;
    private OutputStream _sendStream;
    private final TraceBuffer _sentBytes;
    private final Traces _traces;
    private final byte _unitIdentifier;

    /**
     * On serial ASCII port.
     */
    private static final class _OnSerialASCIIPort
        extends _OnSerialPort
    {
        /**
         * Constructs an instance.
         *
         * @param serialPort The serial port.
         * @param unitIdentifier The unit identifier.
         * @param traces The traces (optional).
         */
        _OnSerialASCIIPort(
                @Nonnull final SerialPortWrapper serialPort,
                final byte unitIdentifier,
                @Nonnull final Traces traces)
        {
            super(serialPort, unitIdentifier, traces);

            Require
                .success(
                    serialPort.getPortDataBits()
                    == Transport.SERIAL_MODE_ASCII_DATA_BITS);
        }

        /** {@inheritDoc}
         */
        @Override
        public byte receiveByte()
            throws EOFException, IOException
        {
            final byte firstHalfByte = hexDigitToHalfByte(super.receiveByte());
            final byte secondHalfByte = hexDigitToHalfByte(super.receiveByte());
            final byte byteValue = (byte) ((firstHalfByte << 4)
                | secondHalfByte);

            _inputLRC.update(byteValue);

            return byteValue;
        }

        /** {@inheritDoc}
         */
        @Override
        public Prefix receivePrefix()
            throws EOFException, IOException
        {
            byte unitIdentifier;

            for (;;) {
                while (super.receiveByte() != _START_BYTE) {}

                _inputLRC.reset();
                getReceivedBytes().reset();
                getReceivedBytes().append(_START_BYTE);

                unitIdentifier = receiveByte();

                if (unitIdentifier == getUnitIdentifier()) {
                    break;
                }

                getThisLogger()
                    .trace(
                        ModbusMessages.IGNORED_MESSAGE_FOR_UNIT,
                        String.valueOf(unitIdentifier & 0xFF));
            }

            return new Prefix(unitIdentifier);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean receiveSuffix()
            throws EOFException, IOException
        {
            final byte lrc = (byte) _inputLRC.getValue();
            final boolean success = receiveByte() == lrc;
            final byte firstEndByte = super.receiveByte();
            final byte secondEndByte = (firstEndByte == _FIRST_END_BYTE)? super
                .receiveByte(): -1;

            traceReceivedBytes();

            if ((firstEndByte != _FIRST_END_BYTE)
                    || (secondEndByte != _SECOND_END_BYTE)) {
                getThisLogger()
                    .warn(
                        ModbusMessages.BAD_MESSAGE_TERMINATION,
                        byteToHexString(firstEndByte),
                        byteToHexString(secondEndByte));
            }

            return success;
        }

        /** {@inheritDoc}
         */
        @Override
        public void sendByte(final int byteValue)
            throws IOException
        {
            super.sendByte(halfByteToHexDigit((byte) (byteValue >> 4)));
            super.sendByte(halfByteToHexDigit((byte) byteValue));

            _outputLRC.update(byteValue);
        }

        /** {@inheritDoc}
         */
        @Override
        public void sendPrefix(final Prefix prefix)
            throws IOException
        {
            getSentBytes().reset();

            super.sendByte(_START_BYTE);

            _outputLRC.reset();
            prefix.write(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public void sendSuffix()
            throws IOException
        {
            sendByte((int) _outputLRC.getValue());

            super.sendByte(_FIRST_END_BYTE);
            super.sendByte(_SECOND_END_BYTE);

            traceSentBytes();
        }

        private static final byte _FIRST_END_BYTE = 0x0D;
        private static final byte _SECOND_END_BYTE = 0x0A;
        private static final byte _START_BYTE = 0x3A;

        private final LRC _inputLRC = new LRC();
        private final LRC _outputLRC = new LRC();
    }


    /**
     * On serial port.
     */
    private abstract static class _OnSerialPort
        extends Transport
        implements StatusChangeListener
    {
        /**
         * Constructs an instance.
         *
         * @param serialPort The serial port.
         * @param unitIdentifier The unit identifier.
         * @param traces The traces (optional).
         */
        _OnSerialPort(
                @Nonnull final SerialPortWrapper serialPort,
                final byte unitIdentifier,
                @Nonnull final Traces traces)
        {
            super(unitIdentifier, traces);

            _serialPort = serialPort;
            setReceiveStream(new SerialPortInputStream(serialPort));
            setSendStream(new SerialPortOutputStream(serialPort));
            trace("Begin");
            _serialPort.addStatusChangeListener(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws IOException
        {
            try {
                _serialPort.removeStatusChangeListener(this);
                _serialPort.close();
            } finally {
                super.close();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public int getBatchSize()
        {
            return 1;
        }

        /** {@inheritDoc}
         */
        @Override
        public String getLocalAddress()
        {
            return _serialPort.getPortName();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getRemoteAddress()
        {
            return _serialPort.getPortName();
        }

        /** {@inheritDoc}
         */
        @Override
        public Prefix newPrefix()
        {
            return new Prefix(getUnitIdentifier());
        }

        /** {@inheritDoc}
         */
        @Override
        public void onMessageReceiveCompleted()
            throws InterruptedException
        {
            final int portSpeed = _serialPort.getPortSpeed();
            final int millis;

            if (portSpeed <= SerialPort.BAUDRATE_19200) {
                // Waits for 3.5 11 bits characters.
                millis = (int) Math.ceil(((3.5 * 11 * 1000) / portSpeed) - 0.1);
            } else {
                millis = 2;
            }

            Thread.sleep(millis);
        }

        /** {@inheritDoc}
         */
        @Override
        public void onMessageSendBegins()
            throws IOException
        {
            _serialPort.purge();
        }

        /** {@inheritDoc}
         */
        @Override
        public void onStatusChange(
                final SerialPortWrapper serialPort,
                final SerialPortWrapper.Event event)
        {
            Require.success(serialPort == _serialPort);

            final String message;

            switch (event) {
                case DSR_OFF: {
                    message = "DSR off";

                    break;
                }
                case FRAME: {
                    message = "Framing error";

                    break;
                }
                case OVERRUN: {
                    message = "Overrun";

                    break;
                }
                case PARITY: {
                    message = "Parity error";

                    break;
                }
                default: {
                    message = null;

                    break;
                }
            }

            if (message != null) {
                trace(message);

                try {
                    close();
                } catch (final IOException exception) {
                    throw new InternalError(exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void setBatchSize(final int batchSize) {}

        private final SerialPortWrapper _serialPort;
    }


    /**
     * On serial RTU port.
     */
    private static final class _OnSerialRTUPort
        extends _OnSerialPort
    {
        /**
         * Constructs an instance.
         *
         * @param serialPort The serial port.
         * @param unitIdentifier The unit identifier.
         * @param traces The traces (optional).
         */
        _OnSerialRTUPort(
                @Nonnull final SerialPortWrapper serialPort,
                final byte unitIdentifier,
                @Nonnull final Traces traces)
        {
            super(serialPort, unitIdentifier, traces);

            Require
                .success(
                    serialPort.getPortDataBits()
                    == Transport.SERIAL_MODE_RTU_DATA_BITS);
        }

        /** {@inheritDoc}
         */
        @Override
        public byte receiveByte()
            throws EOFException, IOException
        {
            final byte readByte = super.receiveByte();

            _inputCRC.update(readByte);

            return readByte;
        }

        /** {@inheritDoc}
         */
        @Override
        public Prefix receivePrefix()
            throws EOFException, IOException
        {
            _inputCRC.reset();
            getReceivedBytes().reset();

            return new Prefix(receiveByte());
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean receiveSuffix()
            throws EOFException, IOException
        {
            final long crc = _inputCRC.getValue();
            final int crcLow = super.receiveByte();
            final int crcHigh = super.receiveByte();
            final boolean success = ((crcLow & 0xFF) | ((crcHigh & 0xFF) << 8))
                == crc;

            traceReceivedBytes();

            return success;
        }

        /** {@inheritDoc}
         */
        @Override
        public void sendByte(final int byteValue)
            throws IOException
        {
            super.sendByte(byteValue);
            _outputCRC.update(byteValue);
        }

        /** {@inheritDoc}
         */
        @Override
        public void sendPrefix(final Prefix prefix)
            throws IOException
        {
            _outputCRC.reset();
            prefix.write(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public void sendSuffix()
            throws IOException
        {
            final int crc = (int) _outputCRC.getValue();

            super.sendByte(crc);
            super.sendByte(crc >> 8);

            traceSentBytes();
        }

        private final CRC _inputCRC = new CRC();
        private final CRC _outputCRC = new CRC();
    }


    /**
     * On socket.
     */
    private static final class _OnSocket
        extends Transport
    {
        /**
         * Constructs an instance.
         *
         * @param socket The socket object.
         * @param unitIdentifier The unit identifier.
         * @param traces The traces.
         */
        _OnSocket(
                @Nonnull final Socket socket,
                final byte unitIdentifier,
                @Nonnull final Traces traces)
        {
            super(unitIdentifier, traces);

            _socket = socket;

            try {
                setReceiveStream(_socket.getInputStream());
                setSendStream(_socket.getOutputStream());
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            trace("Begin");
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws IOException
        {
            try {
                _socket.close();
            } finally {
                super.close();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public int getBatchSize()
        {
            return _batchSize;
        }

        /** {@inheritDoc}
         */
        @Override
        public String getLocalAddress()
        {
            return _socket.getLocalSocketAddress().toString();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getRemoteAddress()
        {
            return _socket.getRemoteSocketAddress().toString();
        }

        /** {@inheritDoc}
         */
        @Override
        public Prefix newPrefix()
        {
            return new Prefix.MBAP(
                (short) _nextTransaction.getAndIncrement(),
                getUnitIdentifier());
        }

        /** {@inheritDoc}
         */
        @Override
        public void onMessageReceiveCompleted() {}

        /** {@inheritDoc}
         */
        @Override
        public void onMessageSendBegins() {}

        /** {@inheritDoc}
         */
        @Override
        public byte receiveByte()
            throws EOFException, IOException
        {
            try {
                return super.receiveByte();
            } catch (final SocketException exception) {
                throw _socket.isClosed()? new EOFException(): exception;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public Prefix receivePrefix()
            throws EOFException, IOException
        {
            getReceivedBytes().reset();

            return Prefix.MBAP.read(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean receiveSuffix()
        {
            traceReceivedBytes();

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void sendPrefix(final Prefix prefix)
            throws IOException
        {
            prefix.write(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public void sendSuffix()
        {
            traceSentBytes();
        }

        /** {@inheritDoc}
         */
        @Override
        public void setBatchSize(final int batchSize)
        {
            _batchSize = batchSize;
        }

        private int _batchSize;
        private final AtomicInteger _nextTransaction = new AtomicInteger();
        private final Socket _socket;
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
