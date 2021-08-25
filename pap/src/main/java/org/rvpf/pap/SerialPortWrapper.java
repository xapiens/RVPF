/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SerialPortWrapper.java 4098 2019-06-25 16:46:46Z SFB $
 */

package org.rvpf.pap;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.nio.file.AccessDeniedException;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.IdentityHashSet;

import jssc.SerialNativeInterface;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * Serial port wrapper.
 */
@ThreadSafe
public final class SerialPortWrapper
    implements SerialPortEventListener
{
    /**
     * Constructs an instance.
     *
     * @param portName The name of the port.
     * @param portSpeed The port speed.
     * @param portControl True to set flow control.
     * @param portDataBits The number of data bits.
     * @param portStopBits The number of stop bits.
     * @param portParity The parity of the port.
     * @param portParityName The name of the parity of the port.
     * @param portModem True for a modem port.
     */
    SerialPortWrapper(
            final String portName,
            final int portSpeed,
            final boolean portControl,
            final int portDataBits,
            final int portStopBits,
            final int portParity,
            final String portParityName,
            final boolean portModem)
    {
        _portName = Require.notEmptyTrimmed(portName);
        _portSpeed = portSpeed;
        _portControl = portControl;
        _portDataBits = portDataBits;
        _portStopBits = portStopBits;
        _portParity = portParity;
        _portParityName = portParityName;
        _portModem = portModem;
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Adds an output empty listener.
     *
     * @param listener The output empty listener.
     */
    public void addOutputEmptyListener(
            @Nonnull final OutputEmptyListener listener)
    {
        synchronized (_mutex) {
            final boolean wasEmpty = _outputEmptyListeners.isEmpty();

            _outputEmptyListeners.add(listener);

            if (wasEmpty) {
                try {
                    _serialPort
                        .setEventsMask(
                            _serialPort.getEventsMask()
                            | SerialPort.MASK_TXEMPTY);
                } catch (final SerialPortException exception) {
                    _LOGGER.warn(BaseMessages.VERBATIM, exception);
                }
            }
        }
    }

    /**
     * Adds a status change listener.
     *
     * @param listener The status change listener.
     */
    public void addStatusChangeListener(
            @Nonnull final StatusChangeListener listener)
    {
        synchronized (_mutex) {
            _statusChangeListeners.add(listener);
        }
    }

    /**
     * Returns the count of available bytes on input.
     *
     * @return The count of available bytes on input.
     */
    @CheckReturnValue
    public int available()
    {
        synchronized (_mutex) {
            return _available;
        }
    }

    /**
     * Closes the serial port.
     *
     * @throws IOException On failure.
     */
    public void close()
        throws IOException
    {
        synchronized (_mutex) {
            final SerialPort serialPort = _serialPort;

            if (serialPort != null) {
                _serialPort = null;
                _dsr = false;
                _err = 0;

                try {
                    if (_portModem) {
                        serialPort.setDTR(false);
                    }

                    serialPort.closePort();
                    _LOGGER.debug(PAPMessages.SERIAL_PORT_CLOSED, _portName);
                } catch (final SerialPortException exception) {
                    throw new IOException(exception);
                }

                _mutex.notifyAll();
            }

            if (serialPort != null) {
                _callStatusChangeListeners(Event.CLOSED);
                _statusChangeListeners.clear();
                _outputEmptyListeners.clear();
            }
        }
    }

    /**
     * Gets the DSR indicator.
     *
     * @return The DSR indicator.
     */
    @CheckReturnValue
    public boolean getDSR()
    {
        return _dsr;
    }

    /**
     * Gets the port data bits.
     *
     * @return The port data bits.
     */
    @CheckReturnValue
    public int getPortDataBits()
    {
        return _portDataBits;
    }

    /**
     * Gets the port name.
     *
     * @return The port name.
     */
    @Nonnull
    @CheckReturnValue
    public String getPortName()
    {
        return _portName;
    }

    /**
     * Gets the port speed.
     *
     * @return The port speed.
     */
    @CheckReturnValue
    public int getPortSpeed()
    {
        return _portSpeed;
    }

    /**
     * Asks if the serial port is closed.
     *
     * @return True if closed.
     */
    @CheckReturnValue
    public boolean isClosed()
    {
        return _serialPort == null;
    }

    /**
     * Asks if the serial port is open.
     *
     * @return True if open.
     */
    @CheckReturnValue
    public boolean isOpen()
    {
        return _serialPort != null;
    }

    /**
     * Asks if the port has modem control.
     *
     * @return True if the port has modem control.
     */
    @CheckReturnValue
    public boolean isPortModem()
    {
        return _portModem;
    }

    /**
     * Asks if the port has parity none.
     *
     * @return True if the port has parity none.
     */
    @CheckReturnValue
    public boolean isPortParityNone()
    {
        return _portParity == SerialPort.PARITY_NONE;
    }

    /**
     * Opens the serial port.
     *
     * @throws IOException On I/O exception.
     */
    public void open()
        throws IOException
    {
        synchronized (_mutex) {
            Require.notNull(_portName);
            Require.failure(_serialPort != null);

            final SerialPort serialPort = new SerialPort(_portName);
            final boolean dsr;

            _logID = Logger.currentLogID();

            _dsr = !_portModem;
            _err = 0;

            try {
                serialPort.openPort();
                serialPort
                    .setParams(
                        _portSpeed,
                        _portDataBits,
                        _portStopBits,
                        _portParity,
                        _portControl,
                        _portModem);
                serialPort
                    .setFlowControlMode(
                        _portControl
                        ? (SerialPort.FLOWCONTROL_RTSCTS_IN
                           | SerialPort.FLOWCONTROL_RTSCTS_OUT): SerialPort.FLOWCONTROL_NONE);

                int eventMask = SerialPort.MASK_RXCHAR | SerialPort.MASK_ERR;

                if (_portModem) {
                    eventMask |= SerialPort.MASK_DSR;
                }

                serialPort.addEventListener(this, eventMask);
                _available = serialPort.getInputBufferBytesCount();

                if (_available < 0) {
                    throw new IOException();
                }

                dsr = serialPort.isDSR();
            } catch (final SerialPortException exception) {
                if (exception.getExceptionType()
                        == SerialPortException.TYPE_PORT_NOT_FOUND) {
                    throw new FileNotFoundException();
                }

                if (exception.getExceptionType()
                        == SerialPortException.TYPE_PORT_BUSY) {
                    throw new AccessDeniedException(_portName);
                }

                throw new IOException(exception);
            }

            final StringBuilder builder = new StringBuilder();

            builder.append(_portSpeed);
            builder.append('/');
            builder.append(_portDataBits);
            builder.append('-');
            builder
                .append(
                    _portParityName.substring(0, 1).toUpperCase(Locale.ROOT));
            builder.append('-');
            builder.append(_portStopBits);

            if (_portModem) {
                builder.append("+DTR");
            }

            if (_portControl) {
                builder.append("+RTS");
            }

            _LOGGER.info(PAPMessages.SERIAL_PORT_OPENED, _portName, builder);

            _serialPort = serialPort;

            if (_portModem) {
                _updateDSR(dsr);
            }
        }
    }

    /**
     * Purges the serial port.
     *
     * @throws IOException On I/O exception.
     */
    public void purge()
        throws IOException
    {
        synchronized (_mutex) {
            final SerialPort serialPort = _serialPort;

            if (serialPort != null) {
                try {
                    serialPort
                        .purgePort(
                            SerialPort.PURGE_RXCLEAR
                            | SerialPort.PURGE_TXCLEAR);
                } catch (final SerialPortException exception) {
                    throw new IOException(exception);
                }

                _available = 0;
            }
        }
    }

    /**
     * Reads at least one byte, then up to a specified count.
     *
     * @param count The maximum count.
     *
     * @return A byte array (null when closed).
     *
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    public byte[] read(final int count)
        throws IOException
    {
        synchronized (_mutex) {
            if (isClosed()) {
                return null;
            }

            final byte[] bytes;

            while (isOpen() && (_available == 0)) {
                try {
                    _mutex.wait();
                } catch (final InterruptedException exception) {
                    throw new InterruptedIOException();
                }
            }

            if (isClosed()) {
                return null;
            }

            if (_available < 0) {
                throw new IOException();
            }

            final int read = Math.min(count, _available);

            try {
                Require
                    .success(
                        _available <= _serialPort.getInputBufferBytesCount());
                bytes = _serialPort.readBytes(read);
                Require.success(bytes.length == read);
                _available -= read;
            } catch (final SerialPortException exception) {
                if (exception.getExceptionType()
                        == SerialPortException.TYPE_PORT_NOT_OPENED) {
                    return null;
                }

                throw new IOException(exception);
            }

            return bytes;
        }
    }

    /**
     * Removes an output empty listener.
     *
     * @param listener The output empty listener.
     */
    public void removeOutputEmptyListener(
            @Nonnull final OutputEmptyListener listener)
    {
        synchronized (_mutex) {
            if (_outputEmptyListeners.remove(listener)) {
                if (_outputEmptyListeners.isEmpty()) {
                    try {
                        _serialPort
                            .setEventsMask(
                                _serialPort.getEventsMask()
                                & ~SerialPort.MASK_TXEMPTY);
                    } catch (final SerialPortException exception) {
                        _LOGGER.warn(BaseMessages.VERBATIM, exception);
                    }
                }
            }
        }
    }

    /**
     * Removes a status change listener.
     *
     * @param listener The status change listener.
     */
    public void removeStatusChangeListener(
            @Nonnull final StatusChangeListener listener)
    {
        synchronized (_mutex) {
            _statusChangeListeners.remove(listener);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void serialEvent(@Nonnull final SerialPortEvent serialPortEvent)
    {
        synchronized (_mutex) {
            if (_serialPort == null) {
                return;
            }

            if (_logID.isPresent()) {
                Logger.setLogID(_logID);
                _logID = Optional.empty();
            }

            switch (serialPortEvent.getEventType()) {
                case SerialPortEvent.RXCHAR: {
                    try {
                        _available = _serialPort.getInputBufferBytesCount();
                    } catch (final SerialPortException exception) {
                        _available = -1;
                    }

                    if (_available != 0) {
                        _mutex.notifyAll();
                    }

                    break;
                }
                case SerialPortEvent.TXEMPTY: {
                    _callOutputEmptyListeners();

                    break;
                }
                case SerialPortEvent.DSR: {
                    final Event dsrEvent = _updateDSR(
                        serialPortEvent.getEventValue() != 0);

                    if (dsrEvent != null) {
                        _callStatusChangeListeners(dsrEvent);
                    }

                    break;
                }
                case SerialPortEvent.ERR: {
                    _err = serialPortEvent.getEventValue();

                    if ((_err & SerialPort.ERROR_OVERRUN) != 0) {
                        _LOGGER
                            .warn(
                                PAPMessages.OVERRUN_ERROR,
                                _serialPort.getPortName());
                        _callStatusChangeListeners(Event.OVERRUN);
                    }

                    if ((_err & SerialPort.ERROR_FRAME) != 0) {
                        _LOGGER
                            .warn(
                                PAPMessages.FRAME_ERROR,
                                _serialPort.getPortName());
                        _callStatusChangeListeners(Event.FRAME);
                    }

                    if ((_err & SerialPort.ERROR_PARITY) != 0) {
                        _LOGGER
                            .warn(
                                PAPMessages.PARITY_ERROR,
                                _serialPort.getPortName());
                        _callStatusChangeListeners(Event.PARITY);
                    }

                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    /**
     * Writes the supplied bytes.
     *
     * @param bytes The bytes.
     *
     * @throws IOException On I/O exception.
     */
    public void write(@Nonnull final byte[] bytes)
        throws IOException
    {
        final SerialPort serialPort = _serialPort;

        if (serialPort == null) {
            throw new EOFException();
        }

        final boolean succeeded;

        try {
            succeeded = serialPort.writeBytes(bytes);
        } catch (final SerialPortException exception) {
            throw new IOException(exception);
        }

        Require.success(succeeded);
    }

    private void _callOutputEmptyListeners()
    {
        for (final OutputEmptyListener listener: _outputEmptyListeners) {
            listener.onOutputEmpty(this);
        }
    }

    private void _callStatusChangeListeners(final Event event)
    {
        for (final StatusChangeListener listener: _statusChangeListeners) {
            listener.onStatusChange(this, event);
        }
    }

    private Event _updateDSR(final boolean newDSR)
    {
        final boolean oldDSR = _dsr;

        _dsr = newDSR;

        if (newDSR && !oldDSR) {
            _LOGGER.debug(PAPMessages.DSR_ON, _serialPort.getPortName());

            return Event.DSR_ON;
        } else if (!newDSR && oldDSR) {
            _LOGGER.debug(PAPMessages.DSR_OFF, _serialPort.getPortName());

            return Event.DSR_OFF;
        }

        return null;
    }

    private static final Logger _LOGGER = Logger
        .getInstance(SerialPortWrapper.class);

    static {
        _LOGGER
            .debug(
                PAPMessages.JSSC_VERSION,
                SerialNativeInterface.getLibraryVersion());
    }

    private int _available;
    private volatile boolean _dsr;
    private volatile int _err;
    private Optional<String> _logID;
    private final Object _mutex = new Object();
    private final Collection<OutputEmptyListener> _outputEmptyListeners =
        new IdentityHashSet<>();
    private final boolean _portControl;
    private final int _portDataBits;
    private final boolean _portModem;
    private final String _portName;
    private final int _portParity;
    private final String _portParityName;
    private final int _portSpeed;
    private final int _portStopBits;
    private volatile SerialPort _serialPort;
    private final Collection<StatusChangeListener> _statusChangeListeners =
        new IdentityHashSet<>();

    /**
     * Event.
     */
    public enum Event
    {
        DSR_ON,
        DSR_OFF,
        OVERRUN,
        FRAME,
        PARITY,
        CLOSED;
    }

    /**
     * Output empty listener.
     */
    public interface OutputEmptyListener
    {
        /**
         * Called on output empty.
         *
         * @param serialPort The serial port wrapper.
         */
        void onOutputEmpty(@Nonnull SerialPortWrapper serialPort);
    }


    /**
     * Status change listener.
     */
    public interface StatusChangeListener
    {
        /**
         * Called on status change.
         *
         * @param serialPort The serial port wrapper.
         * @param event The serial port event.
         */
        void onStatusChange(
                @Nonnull SerialPortWrapper serialPort,
                @Nonnull Event event);
    }


    /**
     * Builder.
     */
    public static final class Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /**
         * Builds a serial port wrapper.
         *
         * @return The serial port wrapper.
         */
        @Nonnull
        @CheckReturnValue
        public SerialPortWrapper build()
        {
            return new SerialPortWrapper(
                _portName,
                _portSpeed,
                _portControl,
                _portDataBits,
                _portStopBits,
                _portParity,
                _portParityName,
                _portModem);
        }

        /**
         * Sets the port flow control.
         *
         * @param portControl The port flow control.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPortControl(final boolean portControl)
        {
            _portControl = portControl;

            return this;
        }

        /**
         * Sets the port data bits.
         *
         * @param portDataBits The port data bits.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPortDataBits(final int portDataBits)
        {
            _portDataBits = portDataBits;

            return this;
        }

        /**
         * Sets the port modem control.
         *
         * @param portModem The port modem control.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPortModem(final boolean portModem)
        {
            _portModem = portModem;

            return this;
        }

        /**
         * Sets the port name.
         *
         * @param portName The port name.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPortName(@Nonnull final String portName)
        {
            _portName = Require.notNull(portName);

            return this;
        }

        /**
         * Sets the port parity.
         *
         * @param parityName The port parity name.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPortParity(@Nonnull final String parityName)
        {
            final int parity = _parity(Require.notEmptyTrimmed(parityName));

            if (parity < 0) {
                _portParity = _parity(DEFAULT_PARITY_NAME);
            } else {
                _portParity = parity;
            }

            _portStopBits = (_portParity == SerialPort.PARITY_NONE)
                    && (_portDataBits < 8)? 2: 1;

            return this;
        }

        /**
         * Sets the port speed.
         *
         * @param portSpeed The port speed.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPortSpeed(final int portSpeed)
        {
            _portSpeed = portSpeed;

            return this;
        }

        /**
         * Sets the port stop bits.
         *
         * @param portStopBits The port stop bits.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPortStopBits(final int portStopBits)
        {
            _portStopBits = portStopBits;

            return this;
        }

        private int _parity(String parityName)
        {
            final int parity;

            switch (parityName.toUpperCase(Locale.ROOT)) {
                case PARITY_NAME_EVEN: {
                    parity = SerialPort.PARITY_EVEN;

                    break;
                }
                case PARITY_NAME_MARK: {
                    parity = SerialPort.PARITY_MARK;

                    break;
                }
                case PARITY_NAME_NONE: {
                    parity = SerialPort.PARITY_NONE;

                    break;
                }
                case PARITY_NAME_ODD: {
                    parity = SerialPort.PARITY_ODD;

                    break;
                }
                case PARITY_NAME_SPACE: {
                    parity = SerialPort.PARITY_SPACE;

                    break;
                }
                default: {
                    Logger
                        .getInstance(SerialPortWrapper.class)
                        .warn(PAPMessages.UNRECOGNIZED_PARITY, parityName);
                    parityName = DEFAULT_PARITY_NAME;
                    parity = DEFAULT_PARITY;

                    break;
                }
            }

            _portParityName = parityName;

            return parity;
        }

        /** Default data bits. */
        public static final int DEFAULT_DATA_BITS = SerialPort.DATABITS_8;

        /** Default parity. */
        public static final int DEFAULT_PARITY = SerialPort.PARITY_NONE;

        /** Default parity name. */
        public static final String DEFAULT_PARITY_NAME = "NONE";

        /** Default port speed. */
        public static final int DEFAULT_PORT_SPEED = SerialPort.BAUDRATE_9600;

        /** Parity name even. */
        public static final String PARITY_NAME_EVEN = "EVEN";

        /** Parity name mark. */
        public static final String PARITY_NAME_MARK = "MARK";

        /** Parity name none. */
        public static final String PARITY_NAME_NONE = "NONE";

        /** Parity name odd. */
        public static final String PARITY_NAME_ODD = "ODD";

        /** Parity name space. */
        public static final String PARITY_NAME_SPACE = "SPACE";

        /** Default stop bits. */
        public static final int DEFAULT_STOP_BITS = SerialPort.STOPBITS_2;

        private boolean _portControl;
        private int _portDataBits = DEFAULT_DATA_BITS;
        private boolean _portModem;
        private String _portName;
        private int _portParity = DEFAULT_PARITY;
        private String _portParityName = DEFAULT_PARITY_NAME;
        private int _portSpeed = DEFAULT_PORT_SPEED;
        private int _portStopBits = DEFAULT_STOP_BITS;
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
