/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusServerProxy.java 4098 2019-06-25 16:46:46Z SFB $
 */

package org.rvpf.pap.modbus;

import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.InetSocketAddress;
import java.net.Socket;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ConnectFailedException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.SerialPortWrapper;
import org.rvpf.pap.modbus.message.Transaction;
import org.rvpf.pap.modbus.register.ArrayRegister;
import org.rvpf.pap.modbus.register.DiscreteArrayRegister;
import org.rvpf.pap.modbus.register.DiscreteRegister;
import org.rvpf.pap.modbus.register.DoubleRegister;
import org.rvpf.pap.modbus.register.FloatRegister;
import org.rvpf.pap.modbus.register.IntegerRegister;
import org.rvpf.pap.modbus.register.LongRegister;
import org.rvpf.pap.modbus.register.MaskedRegister;
import org.rvpf.pap.modbus.register.Register;
import org.rvpf.pap.modbus.register.SequenceRegister;
import org.rvpf.pap.modbus.register.ShortRegister;
import org.rvpf.pap.modbus.register.StampRegister;
import org.rvpf.pap.modbus.register.TimeRegister;
import org.rvpf.pap.modbus.register.WordArrayRegister;
import org.rvpf.pap.modbus.transport.ClientConnection;
import org.rvpf.pap.modbus.transport.Connection;
import org.rvpf.pap.modbus.transport.Transport;

/**
 * Modbus server proxy.
 */
public final class ModbusServerProxy
    extends ModbusProxy
    implements SerialPortWrapper.StatusChangeListener
{
    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param origin The origin representing the remote.
     * @param traces The traces.
     */
    ModbusServerProxy(
            @Nonnull final ModbusContext context,
            @Nonnull final Origin origin,
            @Nonnull final Traces traces)
    {
        super(context, origin);

        _traces = traces;
    }

    private ModbusServerProxy(final ModbusServerProxy other)
    {
        super(other);

        _batchSize = other._batchSize;
        _connectTimeout = other._connectTimeout;
        _registersByPoint.putAll(other._registersByPoint);
        _requestRetries = other._requestRetries;
        _requestRetryInterval = other._requestRetryInterval;
        _requestTimeout = other._requestTimeout;
        _serialPort = other._serialPort;
        _serialPortMode = other._serialPortMode;
        _traces = other._traces;
    }

    /**
     * Clears the completed requests (with or without success).
     *
     * @return The completed requests.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<Transaction.Request> clearCompletedRequests()
    {
        final Collection<Transaction.Request> completedRequests =
            new LinkedList<>();

        for (final Iterator<Map.Entry<Transaction.Request,
                Transaction.Response>> iterator =
                    _requests.entrySet().iterator();
                iterator.hasNext(); ) {
            final Map.Entry<Transaction.Request, Transaction.Response> entry =
                iterator
                    .next();

            if (entry.getValue() != Transaction.Response.NULL) {
                completedRequests.add(entry.getKey());
                iterator.remove();
            }
        }

        return completedRequests;
    }

    /** {@inheritDoc}
     */
    @Override
    public PAPProxy copy()
    {
        return new ModbusServerProxy(this);
    }

    /**
     * Gets the batch size.
     *
     * @return The batch size.
     */
    @CheckReturnValue
    public int getBatchSize()
    {
        return _batchSize;
    }

    /**
     * Gets the connect timeout.
     *
     * @return The connect timeout.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime getConnectTimeout()
    {
        return Require.notNull(_connectTimeout);
    }

    /**
     * Gets the register for a point.
     *
     * @param point The point.
     *
     * @return The optional register.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Register> getRegister(@Nonnull final Point point)
    {
        return Optional
            .ofNullable(_registersByPoint.get(Require.notNull(point)));
    }

    /**
     * Gets the number of request retries.
     *
     * @return The number of request retries.
     */
    @CheckReturnValue
    public int getRequestRetries()
    {
        return _requestRetries;
    }

    /**
     * Gets the request timeout.
     *
     * @return The request timeout.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime getRequestTimeout()
    {
        return Require.notNull(_requestTimeout);
    }

    /**
     * Gets the response to a request.
     *
     * @param request The request.
     *
     * @return The response or empty.
     *
     * @throws InterruptedException When interrupted.
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Response> getResponse(
            @Nonnull final Transaction.Request request)
        throws InterruptedException, ConnectFailedException
    {
        final Transaction.Response response = waitForResponse(
            request)? _requests.remove(request): null;

        Require.success(response != Transaction.Response.NULL);

        request.setState(Transaction.State.INACTIVE);

        return Optional.ofNullable(response);
    }

    /**
     * Gets the serial port.
     *
     * @return The optional serial port (empty on failure).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<SerialPortWrapper> getSerialPort()
    {
        if ((_serialPort != null) && _serialPort.isClosed()) {
            try {
                _serialPort.open();

                if (_serialPort.isPortModem()) {
                    final long timeout = _connectTimeout.toMillis();
                    long startMillis = System.currentTimeMillis();

                    synchronized (_mutex) {
                        _serialPort.addStatusChangeListener(this);

                        try {
                            while (_serialPort.isOpen()
                                    && !_serialPort.getDSR()) {
                                if (timeout == 0) {
                                    break;
                                }

                                final long elapsed = System
                                    .currentTimeMillis() - startMillis;

                                if (elapsed < 0) {
                                    startMillis = System.currentTimeMillis();
                                } else {
                                    final long wait = timeout - elapsed;

                                    if (wait <= 0) {
                                        break;
                                    }

                                    try {
                                        _mutex.wait(wait);
                                    } catch (final InterruptedException exception) {
                                        throw (IOException) new InterruptedIOException()
                                            .initCause(exception);
                                    }
                                }
                            }

                            if (_serialPort.isClosed()) {
                                return Optional.empty();
                            }
                        } finally {
                            _serialPort.removeStatusChangeListener(this);
                        }

                        if (!_serialPort.getDSR()) {
                            _serialPort.close();

                            return Optional.empty();
                        }
                    }
                }

                _serialPort.purge();
            } catch (final IOException exception1) {
                try {
                    _serialPort.close();
                } catch (final IOException exception2) {
                    // Ignores.
                }

                return Optional.empty();
            }
        }

        return Optional.ofNullable(_serialPort);
    }

    /** {@inheritDoc}
     */
    @Override
    public void onStatusChange(
            final SerialPortWrapper serialPort,
            final SerialPortWrapper.Event event)
    {
        synchronized (_mutex) {
            Require.success(serialPort == _serialPort);

            switch (event) {
                case CLOSED:
                case DSR_ON: {
                    _mutex.notifyAll();

                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    /**
     * Puts a response as the value in the requests map.
     *
     * @param response The response.
     */
    public void putResponse(@Nonnull final Transaction.Response response)
    {
        final Transaction.Request request = response.getRequest();
        final boolean wasPending = _requests
            .put(request, response) == Transaction.Response.NULL;

        Require.success(wasPending);

        request.setState(Transaction.State.ANSWERED);
    }

    /**
     * Waits for the response to a request.
     *
     * @param request The request.
     *
     * @return True unless the request has failed.
     *
     * @throws InterruptedException When interrupted.
     * @throws ConnectFailedException When connect failed.
     */
    @CheckReturnValue
    public boolean waitForResponse(
            @Nonnull final Transaction.Request request)
        throws InterruptedException, ConnectFailedException
    {
        if (request.getState() == Transaction.State.INACTIVE) {
            return true;
        }

        for (;;) {
            synchronized (request) {
                while (!(request.hasBeenAnswered() || request.hasFailed())) {
                    request.wait();
                }
            }

            if (request.hasFailed()) {
                if (request.updateRetries()) {
                    final long retryIntervalMillis = _requestRetryInterval
                        .toMillis();

                    if (retryIntervalMillis > 0) {
                        Thread.sleep(retryIntervalMillis);
                    }

                    request.setState(Transaction.State.ACTIVE);
                    _doSendRequest(request);

                    continue;
                }

                return false;
            }

            break;
        }

        return true;
    }

    /**
     * Wait for a response to each request.
     *
     * @return True unless a request has failed.
     *
     * @throws InterruptedException When interrupted.
     * @throws ConnectFailedException When connect failed.
     */
    @CheckReturnValue
    public boolean waitForResponses()
        throws InterruptedException, ConnectFailedException
    {
        boolean success = true;

        try {
            for (final Transaction.Request request: _requests.keySet()) {
                success &= waitForResponse(request);
            }
        } catch (final ConnectFailedException exception) {
            _requests.clear();

            throw exception;
        }

        return success;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final Attributes originAttributes)
    {
        if (!super.setUp(originAttributes)) {
            return false;
        }

        final String serialPortName = getSerialPortName();

        if (!serialPortName.isEmpty()) {
            _serialPortMode = Transport
                .serialMode(
                    originAttributes
                        .getString(Modbus.SERIAL_MODE_ATTRIBUTE)
                        .orElse(null));

            _serialPort = SerialPortWrapper
                .newBuilder()
                .setPortName(serialPortName)
                .setPortSpeed(
                    originAttributes
                        .getInt(
                                Modbus.SERIAL_SPEED_ATTRIBUTE,
                                        Modbus.DEFAULT_SERIAL_SPEED))
                .setPortDataBits(Transport.serialModeDataBits(_serialPortMode))
                .setPortParity(
                    originAttributes
                        .getString(
                                Modbus.SERIAL_PARITY_ATTRIBUTE,
                                        Optional
                                                .of(Modbus.DEFAULT_SERIAL_PARITY))
                        .get())
                .setPortModem(
                    originAttributes
                        .getBoolean(
                                Modbus.SERIAL_MODEM_ATTRIBUTE,
                                        Modbus.DEFAULT_SERIAL_MODEM))
                .setPortControl(
                    originAttributes
                        .getBoolean(
                                Modbus.SERIAL_CONTROL_ATTRIBUTE,
                                        Modbus.DEFAULT_SERIAL_CONTROL))
                .build();
        }

        final int batchSize = originAttributes
            .getInt(Modbus.BATCH_SIZE_ATTRIBUTE, Modbus.DEFAULT_BATCH_SIZE);

        _batchSize = (batchSize <= 0)? Integer.MAX_VALUE: batchSize;
        getThisLogger()
            .debug(ModbusMessages.BATCH_SIZE, String.valueOf(_batchSize));

        _connectTimeout = originAttributes
            .getElapsed(
                Modbus.CONNECT_TIMEOUT_ATTRIBUTE,
                Optional.of(Modbus.DEFAULT_CONNECT_TIMEOUT),
                Optional.of(ElapsedTime.INFINITY))
            .get();
        getThisLogger().debug(ModbusMessages.CONNECT_TIMEOUT, _connectTimeout);

        if (_connectTimeout.toMillis() > Integer.MAX_VALUE) {
            _connectTimeout = ElapsedTime.fromMillis(Integer.MAX_VALUE);
        }

        _requestTimeout = originAttributes
            .getElapsed(
                Modbus.REQUEST_TIMEOUT_ATTRIBUTE,
                Optional.of(Modbus.DEFAULT_REQUEST_TIMEOUT),
                Optional.of(ElapsedTime.INFINITY))
            .get();
        getThisLogger().debug(ModbusMessages.REQUEST_TIMEOUT, _requestTimeout);

        _requestRetries = originAttributes
            .getInt(Modbus.REQUEST_RETRIES_ATTRIBUTE, 0);

        if (_requestRetries > 0) {
            getThisLogger()
                .debug(
                    ModbusMessages.REQUEST_RETRIES,
                    String.valueOf(_requestRetries));
            _requestRetryInterval = originAttributes
                .getElapsed(
                    Modbus.REQUEST_RETRY_INTERVAL_ATTRIBUTE,
                    Optional.of(Modbus.DEFAULT_REQUEST_RETRY_INTERVAL),
                    Optional.of(ElapsedTime.EMPTY))
                .get();
            getThisLogger()
                .debug(
                    ModbusMessages.REQUEST_RETRY_INTERVAL,
                    _requestRetryInterval);
        }

        return true;
    }

    /**
     * Connects.
     *
     * @param log True to log connection.
     *
     * @return The client connection (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<? extends Connection> connect(final boolean log)
    {
        Optional<? extends Connection> connection = getConnection();

        if (connection.isPresent()) {
            if (!connection.get().isClosed()) {
                return connection;
            }

            connection.get().stop();
            forgetConnection();
            connection = null;
        }

        final ElapsedTime connectTimeout = getConnectTimeout();
        boolean onSerialPort = false;
        Transport transport = null;
        int transports = 0;

        for (final InetSocketAddress socketAddress: getSocketAddresses()) {
            ++transports;

            if (log) {
                getThisLogger()
                    .debug(
                        ModbusMessages.TRYING_SERVER_CONNECTION,
                        socketAddress);
            }

            final Socket socket = new Socket();

            try {
                socket.connect(socketAddress, (int) connectTimeout.toMillis());
            } catch (final IOException exception1) {
                if (log) {
                    getThisLogger()
                        .warn(
                            PAPMessages.SERVER_CONNECTION_FAILED,
                            socketAddress);
                }

                try {
                    socket.close();
                } catch (final IOException exception2) {
                    // Ignores.
                }

                continue;
            }

            transport = Transport
                .newSocketTransport(socket, getUnitIdentifier(), _traces);

            break;
        }

        if (transport == null) {
            final Optional<SerialPortWrapper> serialPort = getSerialPort();

            if (serialPort.isPresent()) {
                ++transports;
                transport = Transport
                    .newSerialPortTransport(
                        serialPort.get(),
                        _serialPortMode,
                        getUnitIdentifier(),
                        _traces);
                onSerialPort = true;
            }
        }

        if (transport != null) {
            if (log) {
                getThisLogger()
                    .debug(
                        ModbusMessages.SERVER_CONNECTION_SUCCEEDED,
                        transport.getRemoteAddress(),
                        String.valueOf(getUnitIdentifier() & 0xFF));

                if (onSerialPort) {
                    getThisLogger()
                        .debug(
                            ModbusMessages.SERIAL_MODE,
                            transport.getRemoteAddress(),
                            Transport.serialModeName(_serialPortMode));
                }
            }

            connection = Optional
                .of(
                    new ClientConnection(
                        transport,
                        this,
                        getConnectionListener()));
            setConnection(connection.get());
            connection.get().start();
        } else {
            if (log && (transports != 1)) {
                getThisLogger()
                    .warn(
                        ModbusMessages.SERVER_CONNECTIONS_FAILED,
                        getOrigin());
            }
        }

        return connection;
    }

    /**
     * Sends a request.
     *
     * @param request The request.
     *
     * @return The request.
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    Transaction.Request sendRequest(
            @Nonnull final Transaction.Request request)
        throws ConnectFailedException
    {
        request.setServerProxy(this);
        request.setState(Transaction.State.ACTIVE);
        _requests.put(request, Transaction.Response.NULL);
        _doSendRequest(request);

        return request;
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpBitsRegister(
            final Optional<Integer> address,
            final Integer bit,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        return true;    // Ignored.
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpDiscreteArrayRegister(
            final Optional<Integer> address,
            final int size,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent() || ignored) {
            return true;
        }

        return _setUpArrayRegister(
            new DiscreteArrayRegister(
                address,
                size,
                Optional.of(point),
                readOnly));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpDiscreteRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent() || ignored) {
            return true;
        }

        return _addRegister(
            new DiscreteRegister(address, Optional.of(point), readOnly));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpDoubleRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent() || ignored) {
            return true;
        }

        return _addRegister(
            new DoubleRegister(
                address,
                Optional.of(point),
                readOnly,
                isMiddleEndian()));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpFloatRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent() || ignored) {
            return true;
        }

        return _addRegister(
            new FloatRegister(
                address,
                Optional.of(point),
                readOnly,
                isMiddleEndian()));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpIntegerRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly,
            final boolean signed)
    {
        if (!address.isPresent() || ignored) {
            return true;
        }

        return _addRegister(
            new IntegerRegister(
                address,
                Optional.of(point),
                signed,
                readOnly,
                isMiddleEndian()));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpLongRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent() || ignored) {
            return true;
        }

        return _addRegister(
            new LongRegister(
                address,
                Optional.of(point),
                readOnly,
                isMiddleEndian()));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpMaskedRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final int mask)
    {
        if (!address.isPresent()) {
            return true;
        }

        return _addRegister(
            new MaskedRegister(
                address,
                ignored? Optional.empty(): Optional.of(point),
                mask));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpSequenceRegister(
            final Optional<Integer> address,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        return _addRegister(
            new SequenceRegister(address, Optional.empty(), readOnly));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpShortRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly,
            final boolean signed)
    {
        if (!address.isPresent() || ignored) {
            return true;
        }

        return _addRegister(
            new ShortRegister(address, Optional.of(point), signed, readOnly));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpStampRegister(
            final Optional<Integer> address,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        return _addRegister(
            new StampRegister(
                address,
                Optional.empty(),
                readOnly,
                isMiddleEndian()));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpTimeRegister(
            final Optional<Integer> address,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        return _addRegister(
            new TimeRegister(
                address,
                Optional.empty(),
                readOnly,
                isMiddleEndian()));
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpWordArrayRegister(
            final Optional<Integer> address,
            final int size,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent() || ignored) {
            return true;
        }

        return _setUpArrayRegister(
            new WordArrayRegister(address, size, Optional.of(point), readOnly));
    }

    private boolean _addRegister(final Register register)
    {
        final Integer address = register.getAddress().get();

        if ((address.intValue() < 1) || (65536 < address.intValue())) {
            getThisLogger()
                .warn(
                    ModbusMessages.INVALID_ADDRESS,
                    address,
                    Arrays.toString(register.getPoints()));

            return false;
        }

        final Point[] registerPoints = register.getPoints();

        if (registerPoints.length > 0) {
            final Point point = registerPoints[0];

            if (!_registersByPoint.containsKey(point)) {
                _registersByPoint.put(registerPoints[0], register);
            }
        }

        return true;
    }

    private void _doSendRequest(
            final Transaction.Request request)
        throws ConnectFailedException
    {
        final Optional<? extends Connection> connection = connect(
            request.getRetries() == 0);

        if (!connection.isPresent()) {
            request.setState(Transaction.State.FAILED);

            throw new ConnectFailedException();
        }

        ((ClientConnection) connection.get()).sendRequest(request);
    }

    private boolean _setUpArrayRegister(final ArrayRegister arrayRegister)
    {
        if (!_addRegister(arrayRegister)) {
            return false;
        }

        for (int i = 1; i < arrayRegister.size(); ++i) {
            if (!_addRegister(arrayRegister.newMinion(i))) {
                return false;
            }
        }

        return true;
    }

    private int _batchSize;
    private ElapsedTime _connectTimeout;
    private final Object _mutex = new Object();
    private final Map<Point, Register> _registersByPoint = new HashMap<>();
    private int _requestRetries;
    private ElapsedTime _requestRetryInterval;
    private ElapsedTime _requestTimeout;
    private final Map<Transaction.Request, Transaction.Response> _requests =
        new ConcurrentHashMap<>();
    private SerialPortWrapper _serialPort;
    private int _serialPortMode;
    private final Traces _traces;
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
