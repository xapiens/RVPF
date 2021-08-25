/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusServer.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.pap.modbus;

import java.io.IOException;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAP;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.PAPServer;
import org.rvpf.pap.SerialPortWrapper;
import org.rvpf.pap.modbus.transport.Connection;
import org.rvpf.pap.modbus.transport.ServerConnection;
import org.rvpf.pap.modbus.transport.Transport;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Modbus server.
 */
public final class ModbusServer
    extends PAPServer.Abstract
{
    /**
     * Constructs an instance.
     *
     * @param serverContext The context.
     */
    ModbusServer(@Nonnull final ModbusServerContext serverContext)
    {
        _context = serverContext;
    }

    /** {@inheritDoc}
     */
    @Override
    public void addPointValue(@Nonnull final PointValue pointValue)
    {
        synchronized (_mutex) {
            _updating = true;
            _updates.add(pointValue);
        }
    }

    /**
     * Gets a client proxy by its origin.
     *
     * @param origin The origin.
     *
     * @return The client proxy (empty if unknown).
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    @CheckReturnValue
    public Optional<ModbusClientProxy> getClientProxy(
            @Nonnull final Origin origin)
    {
        return (Optional<ModbusClientProxy>) getContext()
            .getRemoteProxyByOrigin(origin);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isUpdating()
    {
        synchronized (_mutex) {
            for (final PAPProxy remoteProxy: getContext().getRemoteProxies()) {
                if (((ModbusClientProxy) remoteProxy).hasPendingUpdates()) {
                    return true;
                }
            }

            return _updating;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<PointValue> nextUpdate(
            final long timeout)
        throws InterruptedException
    {
        return (timeout < 0)? Optional
            .of(_updates.take()): Optional
                .ofNullable(_updates.poll(timeout, TimeUnit.MILLISECONDS));
    }

    /**
     * Called on updates commit.
     */
    public void onUpdatesCommit()
    {
        synchronized (_mutex) {
            if (_updates.isEmpty()) {
                _updating = false;
                _mutex.notifyAll();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUpListener(@Nonnull final KeyedGroups listenerProperties)
    {
        final Optional<String> originName = listenerProperties
            .getString(PAP.ORIGIN_PROPERTY);
        final Origin defaultOrigin;
        boolean success = true;
        int listeners = 0;

        if (originName.isPresent()) {
            final Optional<Origin> origin = getContext()
                .getRemoteOrigin(originName);

            if (origin.isPresent()) {
                defaultOrigin = origin.get();
            } else {
                getThisLogger()
                    .warn(PAPMessages.UNKNOWN_ORIGIN, originName.get());
                defaultOrigin = null;
            }
        } else {
            defaultOrigin = null;
        }

        int unitIdentifier = listenerProperties
            .getInt(
                Modbus.UNIT_IDENTIFIER_PROPERTY,
                Modbus.DEFAULT_UNIT_IDENTIFIER);

        if ((unitIdentifier < Modbus.MINIMUM_UNIT_IDENTIFIER)
                || (Modbus.MAXIMUM_UNIT_IDENTIFIER < unitIdentifier)) {
            getThisLogger()
                .warn(
                    ModbusMessages.BAD_UNIT_IDENTIFIER,
                    String.valueOf(unitIdentifier));
            unitIdentifier = Modbus.DEFAULT_UNIT_IDENTIFIER;
        }

        final Optional<String> listenAddress = listenerProperties
            .getString(Modbus.SOCKET_ADDRESS_PROPERTY);
        final Optional<Integer> listenPort = listenerProperties
            .getInteger(Modbus.SOCKET_PORT_PROPERTY, Optional.empty());

        if (listenAddress.isPresent() || listenPort.isPresent()) {
            success = _setUpSocketListener(
                defaultOrigin,
                listenAddress,
                listenPort,
                (byte) unitIdentifier);
            ++listeners;
        }

        final String portName = listenerProperties
            .getString(Modbus.SERIAL_PORT_PROPERTY)
            .orElse(null);

        if (portName != null) {
            final int portSpeed = listenerProperties
                .getInt(
                    Modbus.SERIAL_SPEED_PROPERTY,
                    Modbus.DEFAULT_SERIAL_SPEED);
            final int portMode = Transport
                .serialMode(
                    listenerProperties
                        .getString(Modbus.SERIAL_MODE_PROPERTY)
                        .orElse(null));
            final int portDataBits = Transport.serialModeDataBits(portMode);
            final String portParity = listenerProperties
                .getString(
                    Modbus.SERIAL_PARITY_PROPERTY,
                    Optional.of(Modbus.DEFAULT_SERIAL_PARITY))
                .get();
            final boolean portModem = listenerProperties
                .getBoolean(
                    Modbus.SERIAL_MODEM_PROPERTY,
                    Modbus.DEFAULT_SERIAL_MODEM);
            final boolean portControl = listenerProperties
                .getBoolean(
                    Modbus.SERIAL_CONTROL_PROPERTY,
                    Modbus.DEFAULT_SERIAL_CONTROL);

            success &= _setupSerialListener(
                defaultOrigin,
                portName,
                portSpeed,
                portDataBits,
                portParity,
                portModem,
                portControl,
                portMode,
                (byte) unitIdentifier);
            ++listeners;
        }

        if (listeners == 0) {
            getThisLogger().warn(PAPMessages.NO_LISTENERS);
            success = false;
        }

        return success;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        _stopped.set(false);

        final Collection<? extends PAPProxy> remoteProxies = getContext()
            .getRemoteProxies();

        if (getThisLogger().isDebugEnabled()) {
            final Collection<Point> points = getContext().getRemotePoints();

            getThisLogger().debug(ModbusMessages.STARTING_SERVER);
            getThisLogger()
                .debug(
                    ModbusMessages.CONFIGURED_PROXIES,
                    String.valueOf(remoteProxies.size()));
            getThisLogger()
                .debug(
                    ModbusMessages.CONFIGURED_POINTS,
                    String.valueOf(points.size()));
        }

        remoteProxies.forEach(proxy -> ((ModbusClientProxy) proxy).start(this));
        _listeners.forEach(listener -> listener.start());

        getThisLogger().debug(ModbusMessages.STARTED_SERVER);
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if (_stopped.compareAndSet(false, true)) {
            getThisLogger().debug(ModbusMessages.STOPPING_SERVER);

            _listeners.forEach(listener -> listener.stop());
            getContext()
                .getRemoteProxies()
                .forEach(proxy -> ((ModbusClientProxy) proxy).stop());

            getThisLogger().debug(ModbusMessages.STOPPED_SERVER);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        stop();
    }

    /**
     * Waits while updating.
     *
     * @throws InterruptedException When interrupted.
     */
    public void waitWhileUpdating()
        throws InterruptedException
    {
        synchronized (_mutex) {
            while (isUpdating()) {
                _mutex.wait();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected PAPContext getContext()
    {
        return _context;
    }

    private boolean _setUpSocketListener(
            final Origin defaultOrigin,
            final Optional<String> listenAddress,
            final Optional<Integer> listenPort,
            final byte unitIdentifier)
    {
        final Optional<ModbusClientProxy> defaultProxy;

        if (defaultOrigin != null) {
            defaultProxy = getClientProxy(defaultOrigin);

            if (!defaultProxy.isPresent()) {
                getThisLogger()
                    .error(PAPMessages.UNKNOWN_ORIGIN, defaultOrigin);

                return false;
            }
        } else {
            defaultProxy = Optional.empty();
        }

        final _Listener._OnSocket listener = new _Listener._OnSocket(
            this,
            defaultProxy);

        if (!listener.setUp(listenAddress, listenPort, unitIdentifier)) {
            return false;
        }

        _listeners.add(listener);

        return true;
    }

    private boolean _setupSerialListener(
            final Origin defaultOrigin,
            final String portName,
            final int portSpeed,
            final int portDataBits,
            final String portParity,
            final boolean portModem,
            final boolean portControl,
            final int portMode,
            final byte unitIdentifier)
    {
        Optional<? extends ModbusProxy> clientProxy =
            ((ModbusContext) getContext())
                .getRemoteProxyBySerialPortName(portName);

        if (!clientProxy.isPresent()) {
            if (defaultOrigin != null) {
                clientProxy = getClientProxy(defaultOrigin);

                if (!clientProxy.isPresent()) {
                    getThisLogger()
                        .error(PAPMessages.UNKNOWN_ORIGIN, defaultOrigin);

                    return false;
                }
            } else {
                getThisLogger().error(ModbusMessages.NO_ORIGIN, portName);

                return false;
            }
        }

        final _Listener._OnSerial listener = new _Listener._OnSerial(
            this,
            clientProxy);

        if (!listener
            .setUp(
                portName,
                portSpeed,
                portDataBits,
                portParity,
                portModem,
                portControl,
                portMode,
                unitIdentifier)) {
            return false;
        }

        _listeners.add(listener);

        return true;
    }

    private final ModbusServerContext _context;
    private final List<_Listener> _listeners = new LinkedList<>();
    private final Object _mutex = new Object();
    private final AtomicBoolean _stopped = new AtomicBoolean();
    private final BlockingQueue<PointValue> _updates =
        new LinkedBlockingQueue<>();
    private boolean _updating;

    /**
     * Listener.
     */
    private abstract static class _Listener
    {
        /**
         * Constructs an instance.
         *
         * @param server The Modbus server.
         * @param defaultProxy An optional default proxy.
         */
        _Listener(
                @Nonnull final ModbusServer server,
                @Nonnull final Optional<? extends ModbusProxy> defaultProxy)
        {
            _server = server;
            _defaultProxy = defaultProxy;
        }

        /**
         * Gets the default proxy.
         *
         * @return The optional default proxy.
         */
        @Nonnull
        @CheckReturnValue
        Optional<? extends ModbusProxy> getDefaultProxy()
        {
            return _defaultProxy;
        }

        /**
         * Gets the server context.
         *
         * @return The server context.
         */
        @Nonnull
        @CheckReturnValue
        ModbusServerContext getServerContext()
        {
            return (ModbusServerContext) _server.getContext();
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        Logger getThisLogger()
        {
            return _logger;
        }

        /**
         * Gets the unit identifier.
         *
         * @return The unit identifier.
         */
        @CheckReturnValue
        byte getUnitIdentifier()
        {
            return _unitIdentifier;
        }

        /**
         * Sets the unit identifier.
         *
         * @param unitIdentifier The unit identifier.
         */
        void setUnitIdentifier(final byte unitIdentifier)
        {
            _unitIdentifier = unitIdentifier;
        }

        /**
         * Starts.
         */
        abstract void start();

        /**
         * Starts a server connection.
         *
         * @param transport The transport for the connection.
         * @param clientProxy The client proxy.
         *
         * @return The server connection.
         */
        final ServerConnection startServerConnection(
                @Nonnull final Transport transport,
                @Nonnull final ModbusClientProxy clientProxy)
        {
            final ServerConnection serverConnection = new ServerConnection(
                transport,
                clientProxy,
                Optional.empty(),
                !_server.getResponder().isPresent());

            serverConnection.start();

            return serverConnection;
        }

        /**
         * Stops.
         */
        abstract void stop();

        private final Optional<? extends ModbusProxy> _defaultProxy;
        private final Logger _logger = Logger.getInstance(getClass());
        private final ModbusServer _server;
        private byte _unitIdentifier;

        /**
         * On serial.
         */
        private static final class _OnSerial
            extends _Listener
            implements ServiceThread.Target,
                SerialPortWrapper.StatusChangeListener
        {
            /**
             * Constructs an instance.
             *
             * @param server The Modbus server.
             * @param defaultProxy An optional default proxy.
             */
            _OnSerial(
                    @Nonnull final ModbusServer server,
                    @Nonnull final Optional<? extends ModbusProxy> defaultProxy)
            {
                super(server, defaultProxy);
            }

            /** {@inheritDoc}
             */
            @Override
            public void onStatusChange(
                    final SerialPortWrapper serialPort,
                    final SerialPortWrapper.Event event)
            {
                Require.success(serialPort == _serialPort);

                synchronized (_statusChangeSemaphore) {
                    _statusChangeSemaphore.notifyAll();
                }
            }

            /** {@inheritDoc}
             */
            @Override
            public void run()
            {
                getThisLogger()
                    .info(
                        ModbusMessages.STARTED_LISTENING,
                        _serialPort.getPortName(),
                        String.valueOf(getUnitIdentifier() & 0xFF));
                getThisLogger()
                    .debug(
                        ModbusMessages.SERIAL_MODE,
                        _serialPort.getPortName(),
                        Transport.serialModeName(_serialPortMode));

                try {
                    final Thread currentThread = Thread.currentThread();

                    while (_thread.get() == currentThread) {
                        try {
                            _serialPort.open();
                            _serialPort.purge();
                            _serialPort.addStatusChangeListener(this);
                        } catch (final IOException exception) {
                            throw new RuntimeException(exception);
                        }

                        if (_serialPort.isPortModem()) {
                            synchronized (_statusChangeSemaphore) {
                                while ((_thread.get() == currentThread)
                                        && _serialPort.isOpen()
                                        && !_serialPort.getDSR()) {
                                    _statusChangeSemaphore.wait();
                                }
                            }
                        }

                        if (_thread.get() != currentThread) {
                            break;
                        }

                        if (_serialPort.isClosed()) {
                            continue;
                        }

                        final Connection connection = startServerConnection(
                            Transport
                                .newSerialPortTransport(
                                    _serialPort,
                                    _serialPortMode,
                                    getUnitIdentifier(),
                                    getServerContext().getTraces()),
                            (ModbusClientProxy) getDefaultProxy().get());

                        try {
                            synchronized (_statusChangeSemaphore) {
                                while (_serialPort.isOpen()) {
                                    _statusChangeSemaphore.wait();

                                    if (_thread.get() != currentThread) {
                                        break;
                                    }
                                }
                            }
                        } finally {
                            connection.stop();
                        }
                    }
                } catch (final InterruptedException exception) {
                    // Stops.
                }

                getThisLogger()
                    .info(
                        ModbusMessages.STOPPED_LISTENING,
                        _serialPort.getPortName());
            }

            /**
             * Sets up this.
             *
             * @param portName The port name.
             * @param portSpeed The port speed.
             * @param portDataBits The port data bits.
             * @param portParity The name of the port parity.
             * @param portModem The port modem control.
             * @param portControl The port flow control.
             * @param portMode The port mode.
             * @param unitIdentifier The unit identifier.
             *
             * @return True on success.
             */
            @CheckReturnValue
            boolean setUp(
                    @Nonnull final String portName,
                    final int portSpeed,
                    final int portDataBits,
                    @Nonnull final String portParity,
                    final boolean portModem,
                    final boolean portControl,
                    final int portMode,
                    final byte unitIdentifier)
            {
                _serialPort = SerialPortWrapper
                    .newBuilder()
                    .setPortName(portName)
                    .setPortSpeed(portSpeed)
                    .setPortDataBits(portDataBits)
                    .setPortParity(portParity)
                    .setPortModem(portModem)
                    .setPortControl(portControl)
                    .build();

                _serialPortMode = portMode;

                setUnitIdentifier(unitIdentifier);

                return true;
            }

            /** {@inheritDoc}
             */
            @Override
            void start()
            {
                final ServiceThread thread = new ServiceThread(
                    this,
                    "Modbus server (listener on " + _serialPort.getPortName()
                    + ")");

                if (_thread.compareAndSet(null, thread)) {
                    getThisLogger()
                        .debug(
                            ServiceMessages.STARTING_THREAD,
                            thread.getName());
                    thread.start();
                }
            }

            /** {@inheritDoc}
             */
            @Override
            void stop()
            {
                final ServiceThread thread;

                synchronized (_statusChangeSemaphore) {
                    thread = _thread.getAndSet(null);

                    if (thread != null) {
                        getThisLogger()
                            .debug(
                                ServiceMessages.STOPPING_THREAD,
                                thread.getName());
                    }

                    _statusChangeSemaphore.notifyAll();
                }

                if (thread != null) {
                    try {
                        _serialPort.close();
                    } catch (final IOException exception) {
                        // Ignores.
                    }

                    if (thread != Thread.currentThread()) {
                        Require.ignored(thread.join(getThisLogger(), 0));
                    }
                }
            }

            private SerialPortWrapper _serialPort;
            private int _serialPortMode;
            private final Object _statusChangeSemaphore = new Object();
            private final AtomicReference<ServiceThread> _thread =
                new AtomicReference<>();
        }


        /**
         * On socket.
         */
        private static final class _OnSocket
            extends _Listener
            implements ServiceThread.Target
        {
            /**
             * Constructs an instance.
             *
             * @param server The Modbus server.
             * @param defaultProxy An optional default proxy.
             */
            _OnSocket(
                    @Nonnull final ModbusServer server,
                    @Nonnull final Optional<? extends ModbusProxy> defaultProxy)
            {
                super(server, defaultProxy);
            }

            /** {@inheritDoc}
             */
            @Override
            public void run()
            {
                final ServerSocket serverSocket = _serverSocket;

                getThisLogger()
                    .info(
                        ModbusMessages.STARTED_LISTENING,
                        serverSocket.getLocalSocketAddress(),
                        String.valueOf(getUnitIdentifier() & 0xFF));

                for (;;) {
                    final Socket socket;

                    try {
                        socket = serverSocket.accept();
                    } catch (final SocketException exception) {
                        break;
                    } catch (final IOException exception) {
                        throw new RuntimeException(exception);
                    }

                    final InetAddress proxyAddress = socket.getInetAddress();
                    final ModbusContext context = getServerContext();
                    Optional<? extends ModbusProxy> remoteProxy = context
                        .getRemoteProxyByInetAddress(proxyAddress);

                    if (!remoteProxy.isPresent()) {
                        remoteProxy = getDefaultProxy();

                        if (!remoteProxy.isPresent()) {
                            try {
                                socket.close();
                            } catch (final IOException exception) {
                                // Ignores.
                            }

                            getThisLogger()
                                .warn(
                                    ModbusMessages.CLIENT_CONNECTION_REJECTED,
                                    proxyAddress);

                            continue;
                        }
                    }

                    remoteProxy.get().disconnect();

                    startServerConnection(
                        Transport
                            .newSocketTransport(
                                socket,
                                getUnitIdentifier(),
                                context.getTraces()),
                        (ModbusClientProxy) remoteProxy.get());
                }

                getThisLogger()
                    .info(
                        ModbusMessages.STOPPED_LISTENING,
                        serverSocket.getLocalSocketAddress());
            }

            /**
             * Sets up this.
             *
             * @param optionalListenAddress The optional listen address.
             * @param listenPort The optional listen port.
             * @param unitIdentifier The unit identifier.
             *
             * @return True on success.
             */
            @CheckReturnValue
            boolean setUp(
                    @Nonnull final Optional<String> optionalListenAddress,
                    @Nonnull final Optional<Integer> listenPort,
                    final byte unitIdentifier)
            {
                if (optionalListenAddress.isPresent()) {
                    String listenAddress = optionalListenAddress.get().trim();

                    if (listenAddress.isEmpty()) {
                        listenAddress = "*";
                    }

                    _listenAddress = listenAddress;
                    getThisLogger()
                        .info(ModbusMessages.LISTEN_ADDRESS, listenAddress);
                }

                if (listenPort.isPresent()) {
                    _listenPort = listenPort.get().intValue();
                    getThisLogger()
                        .info(
                            ModbusMessages.LISTEN_PORT,
                            String.valueOf(listenPort.get()));
                }

                setUnitIdentifier(unitIdentifier);

                return true;
            }

            /** {@inheritDoc}
             */
            @Override
            void start()
            {
                final int listenPort = (_listenPort > 0)
                    ? _listenPort: Modbus.DEFAULT_PORT;
                InetAddress listenAddress = null;

                try {
                    listenAddress = "*"
                        .equals(
                            _listenAddress)? null: InetAddress
                                .getByName(_listenAddress);
                    _serverSocket = new ServerSocket(
                        listenPort,
                        0,
                        listenAddress);
                } catch (final IOException exception) {
                    throw new RuntimeException(
                        "Listen port " + listenPort + " , listen address '"
                        + listenAddress + "'",
                        exception);
                }

                final ServiceThread thread = new ServiceThread(
                    this,
                    "Modbus server (listener on "
                    + _serverSocket.getLocalSocketAddress() + ")");

                if (_thread.compareAndSet(null, thread)) {
                    getThisLogger()
                        .debug(
                            ServiceMessages.STARTING_THREAD,
                            thread.getName());
                    thread.start();
                }
            }

            /** {@inheritDoc}
             */
            @Override
            void stop()
            {
                final ServerSocket serverSocket = _serverSocket;

                if (serverSocket != null) {
                    _serverSocket = null;

                    try {
                        serverSocket.close();
                    } catch (final IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }

                final ServiceThread thread = _thread.getAndSet(null);

                if (thread != null) {
                    getThisLogger()
                        .debug(
                            ServiceMessages.STOPPING_THREAD,
                            thread.getName());
                    Require.ignored(thread.join(getThisLogger(), 0));
                }
            }

            private String _listenAddress;
            private int _listenPort;
            private ServerSocket _serverSocket;
            private final AtomicReference<ServiceThread> _thread =
                new AtomicReference<>();
        }
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
