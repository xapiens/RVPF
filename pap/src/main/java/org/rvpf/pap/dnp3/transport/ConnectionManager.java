/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ConnectionManager.java 4098 2019-06-25 16:46:46Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.dnp3.DNP3;
import org.rvpf.pap.dnp3.DNP3Context;
import org.rvpf.pap.dnp3.DNP3MasterProxy;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.pap.dnp3.DNP3Proxy;

/**
 * Connection manager.
 */
public final class ConnectionManager
{
    /**
     * Constructs an instance.
     *
     * @param context The DNP3 context.
     */
    public ConnectionManager(@Nonnull final DNP3Context context)
    {
        _context = Require.notNull(context);
        _localEndPoint = new LocalEndPoint(this);
    }

    /**
     * Adds an association listener.
     *
     * @param associationListener The association listener.
     *
     * @return True if added, false if already added.
     */
    @CheckReturnValue
    public boolean addAssociationListener(
            @Nonnull final AssociationListener associationListener)
    {
        return _associationListenerManager.addListener(associationListener);
    }

    /**
     * Adds a received fragment listener.
     *
     * @param receivedFragmentListener The received fragment listener.
     *
     * @return True if added, false if already added.
     */
    @CheckReturnValue
    public boolean addReceivedFragmentListener(
            @Nonnull final ReceivedFragmentListener receivedFragmentListener)
    {
        return _receivedFragmentListenerManager
            .addListener(receivedFragmentListener);
    }

    /**
     * Connects via a DNP3 proxy
     *
     * @param proxy The DNP3 proxy
     * @param localAddress The local DNP3 address.
     * @param remoteAddress The remote DNP3 address.
     *
     * @return An association instance (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public Association connect(
            @Nonnull final DNP3Proxy proxy,
            final short localAddress,
            final short remoteAddress)
    {
        Require.notNull(_connectionListener);

        RemoteEndPoint remoteEndPoint = _remoteEndPointByProxy.get(proxy);

        // Adds remote end point if needed.

        if (remoteEndPoint == null) {
            remoteEndPoint = new RemoteEndPoint(this, proxy);
            _remoteEndPointByProxy.put(proxy, remoteEndPoint);
            _LOGGER
                .debug(
                    DNP3Messages.NEW_CONNECTION,
                    proxy.getName().orElse(null));
        }

        // Registers the local address if needed.

        final Short logicalDeviceAddress = Short.valueOf(localAddress);

        if (!_logicalDeviceByAddress.containsKey(logicalDeviceAddress)) {
            final LogicalDevice logicalDevice = new LogicalDevice(
                "",
                logicalDeviceAddress);

            _logicalDeviceByAddress.put(logicalDeviceAddress, logicalDevice);
            _LOGGER.debug(DNP3Messages.REGISTERED_LOCAL_ADDRESS, logicalDevice);
            logicalDevice.activate(this);
        }

        // Activates the connection.

        final Association association = remoteEndPoint
            .getAssociation(localAddress, remoteAddress);
        final DataLinkLayer dataLinkLayer = association.getDataLinkLayer();

        try {
            return dataLinkLayer
                .isLinkActive(
                    remoteEndPoint.getReplyTimeout())? association: null;
        } catch (final IOException exception) {
            _LOGGER
                .trace(
                    exception,
                    BaseMessages.VERBATIM,
                    exception.getMessage());

            return null;
        }
    }

    /**
     * Disconnects all.
     */
    public void disconnect()
    {
        for (final RemoteEndPoint remoteEndPoint:
                new ArrayList<>(_connectionByEndPoint.keySet())) {
            disconnect(remoteEndPoint.getRemoteProxy());
        }
    }

    /**
     * Disconnects a DNP3 proxy.
     *
     * @param proxy The DNP3 proxy.
     */
    public void disconnect(@Nonnull final DNP3Proxy proxy)
    {
        final RemoteEndPoint remoteEndPoint = _remoteEndPointByProxy.get(proxy);
        final Connection connection = (remoteEndPoint != null)
            ? _connectionByEndPoint
                .remove(remoteEndPoint): null;
        final Optional<Exception> connectionException;

        if (connection != null) {
            connectionException = connection.getException();

            connection.close();
        } else {
            connectionException = Optional.empty();
        }

        if (remoteEndPoint != null) {
            _remoteEndPointByProxy.remove(proxy);
            remoteEndPoint.close();

            if (connectionException.isPresent()) {
                _LOGGER
                    .debug(
                        DNP3Messages.LOST_CONNECTION_,
                        proxy.getName().orElse(null),
                        connectionException.get().getMessage());
            } else {
                _LOGGER
                    .debug(
                        DNP3Messages.LOST_CONNECTION,
                        proxy.getName().orElse(null));
            }
        }
    }

    /**
     * Gets the local end point.
     *
     * @return The local end point.
     */
    @Nonnull
    @CheckReturnValue
    public LocalEndPoint getLocalEndPoint()
    {
        return _localEndPoint;
    }

    /**
     * Gets a logical device by its address.
     *
     * @param address The address.
     *
     * @return The logical device (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<LogicalDevice> getLogicalDevice(
            @Nonnull final Short address)
    {
        return Optional.ofNullable(_logicalDeviceByAddress.get(address));
    }

    /**
     * Gets a logical device by its name.
     *
     * @param name The `name.
     *
     * @return The logical device (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<LogicalDevice> getLogicalDevice(@Nonnull final String name)
    {
        return Optional.ofNullable(_logicalDeviceByName.get(name));
    }

    /**
     * Gets the remote end point for a proxy.
     *
     * @param proxy The proxy.
     *
     * @return The remote end point (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<RemoteEndPoint> getRemoteEndPoint(
            @Nonnull final DNP3Proxy proxy)
    {
        return Optional.ofNullable(_remoteEndPointByProxy.get(proxy));
    }

    /**
     * Gets the traces instance.
     *
     * @return The traces instance.
     */
    @Nonnull
    @CheckReturnValue
    public Traces getTraces()
    {
        return _context.getTraces();
    }

    /**
     * Registers logical devices.
     *
     * @param logicalDevicesByAddress The logical devices by address.
     */
    public void registerLogicalDevices(
            @Nonnull final Map<Short, LogicalDevice> logicalDevicesByAddress)
    {
        _logicalDeviceByAddress.putAll(logicalDevicesByAddress);

        _logicalDeviceByAddress
            .values()
            .stream()
            .forEach(
                logicalDevice -> {
                    _LOGGER
                        .debug(
                                DNP3Messages.REGISTERED_LOCAL_ADDRESS,
                                        logicalDevice);

                    final String name = logicalDevice.getName();

                    if (!name.isEmpty()) {
                        _logicalDeviceByName.put(name, logicalDevice);
                    }
                });
    }

    /**
     * Removes an association listener.
     *
     * @param associationListener The association listener.
     *
     * @return True if removed, false if already removed.
     */
    @CheckReturnValue
    public boolean removeAssociationListener(
            @Nonnull final AssociationListener associationListener)
    {
        return _associationListenerManager.removeListener(associationListener);
    }

    /**
     * Removes a received fragment listener.
     *
     * @param receivedFragmentListener The received fragment listener.
     *
     * @return True if removed, false if already removed.
     */
    @CheckReturnValue
    public boolean removeReceivedFragmentListener(
            @Nonnull final ReceivedFragmentListener receivedFragmentListener)
    {
        return _receivedFragmentListenerManager
            .removeListener(receivedFragmentListener);
    }

    /**
     * Sets up this.
     *
     * @param listenProperties The listen properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(@Nonnull final KeyedValues listenProperties)
    {
        // Sets up a TCP listener if configured.

        final Optional<String> tcpListenAddress = listenProperties
            .getString(DNP3.TCP_LISTEN_ADDRESS_PROPERTY);
        final Optional<Integer> tcpListenPort = listenProperties
            .getInteger(DNP3.TCP_LISTEN_PORT_PROPERTY, Optional.empty());

        if (tcpListenAddress.isPresent() || tcpListenPort.isPresent()) {
            final Optional<InetSocketAddress> tcpListenSocketAddress =
                _listenSocketAddress(
                    tcpListenAddress.orElse(null),
                    tcpListenPort.orElse(null));

            if (!tcpListenSocketAddress.isPresent()) {
                return false;
            }

            _tcpListener = new TCPSocketListener(
                tcpListenSocketAddress.get(),
                this);
        }

        // Sets up an UDP listener if configured.

        final Optional<String> udpListenAddress = listenProperties
            .getString(DNP3.UDP_LISTEN_ADDRESS_PROPERTY);
        final Optional<Integer> udpListenPort = listenProperties
            .getInteger(DNP3.UDP_LISTEN_PORT_PROPERTY, Optional.empty());

        if (udpListenAddress.isPresent() || udpListenPort.isPresent()) {
            final Optional<InetSocketAddress> udpListenSocketAddress =
                _listenSocketAddress(
                    udpListenAddress.orElse(null),
                    udpListenPort.orElse(null));

            if (!udpListenSocketAddress.isPresent()) {
                return false;
            }

            _udpListener = new UDPDatagramListener(
                udpListenSocketAddress.get(),
                this);
        }

        _setUpCompleted = true;

        return true;
    }

    /**
     * Starts listening.
     *
     * @param connectionListener A connection listener.
     *
     * @throws IOException On I/O exception.
     */
    public void startListening(
            @Nonnull final PAPConnectionListener connectionListener)
        throws IOException
    {
        if (!_setUpCompleted) {
            Require.success(setUp(KeyedGroups.MISSING_KEYED_GROUP));
        }

        _connectionListener = connectionListener;

        _logicalDeviceByAddress
            .values()
            .stream()
            .forEach(logicalDevice -> logicalDevice.activate(this));

        if (_tcpListener != null) {
            _tcpListener.start();
        }

        if (_udpListener != null) {
            _udpListener.start();
        }
    }

    /**
     * Stops listening.
     *
     * @throws IOException On I/O exception.
     */
    public void stopListening()
        throws IOException
    {
        if (_setUpCompleted && (_connectionListener != null)) {
            _logicalDeviceByAddress
                .values()
                .stream()
                .forEach(logicalDevice -> logicalDevice.deactivate());

            if (_udpListener != null) {
                _udpListener.stop();
            }

            if (_tcpListener != null) {
                _tcpListener.stop();
            }

            disconnect();

            _receivedFragmentListenerManager.clear();
            _associationListenerManager.clear();
        }

        _connectionListener = null;
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        if (_connectionListener != null) {
            try {
                stopListening();
            } catch (final IOException exception) {
                // Ignores.
            }
        }

        _localEndPoint.close();

        getTraces().tearDown();
    }

    /**
     * Gets a connection to a remote end point.
     *
     * @param remoteEndPoint The remote end point.
     *
     * @return The connection (null on failure).
     */
    @Nullable
    @CheckReturnValue
    Connection getConnection(@Nonnull final RemoteEndPoint remoteEndPoint)
    {
        Connection connection = _connectionByEndPoint.get(remoteEndPoint);

        if (connection != null) {
            if (!connection.isClosed()) {
                return connection;
            }

            _connectionByEndPoint.remove(remoteEndPoint);
            connection = null;
        } else {
            final RemoteEndPoint knownEndPoint = _remoteEndPoints
                .get(remoteEndPoint);

            if (knownEndPoint == null) {
                _remoteEndPoints.put(remoteEndPoint, remoteEndPoint);
            } else if (remoteEndPoint != knownEndPoint) {
                throw new IllegalArgumentException(
                    "Duplicate remote end point");
            }
        }

        final int connectTimeout = remoteEndPoint.getConnectTimeout();

        for (final InetSocketAddress socketAddress:
                remoteEndPoint.getTCPSocketAddresses()) {
            SocketChannel socketChannel = null;

            _LOGGER.debug(DNP3Messages.TRYING_CONNECTION_TO, socketAddress);

            try {
                socketChannel = SocketChannel.open();
                socketChannel.socket().connect(socketAddress, connectTimeout);
            } catch (final IOException exception1) {
                if (socketChannel != null) {
                    try {
                        socketChannel.close();
                    } catch (final IOException exception2) {
                        // Ignores.
                    }
                }

                continue;
            }

            connection = new TCPConnection(
                _localEndPoint,
                remoteEndPoint,
                socketChannel);
            _remoteEndPointByTCPSocketAddress
                .put(socketAddress, remoteEndPoint);
            _remoteEndPointByTCPAddress
                .put(socketAddress.getAddress(), remoteEndPoint);

            break;
        }

        if ((connection == null) && (_udpListener != null)) {
            final DatagramChannel datagramChannel = _udpListener.getChannel();
            final SocketAddress localAddress;

            try {
                localAddress = datagramChannel.getLocalAddress();
            } catch (final IOException exception) {
                throw new InternalError(exception);
            }

            for (final InetSocketAddress socketAddress:
                    remoteEndPoint.getUDPSocketAddresses()) {
                _LOGGER
                    .debug(
                        DNP3Messages.TRYING_CONNECTION_FROM_TO,
                        localAddress,
                        socketAddress);

                connection = new UDPConnection(
                    _localEndPoint,
                    remoteEndPoint,
                    datagramChannel,
                    socketAddress);
                _remoteEndPointByUDPSocketAddress
                    .put(socketAddress, remoteEndPoint);
                _remoteEndPointByUDPAddress
                    .put(socketAddress.getAddress(), remoteEndPoint);

                break;
            }
        }

        if (connection == null) {
            final String serialPortName = remoteEndPoint.getSerialPortName();

            if (!serialPortName.isEmpty()) {
                _LOGGER
                    .debug(DNP3Messages.TRYING_CONNECTION_THRU, serialPortName);

                connection = new SerialConnection(
                    _localEndPoint,
                    remoteEndPoint,
                    serialPortName,
                    remoteEndPoint.getSerialPortSpeed());

                try {
                    ((SerialConnection) connection).purge();
                } catch (final IOException exception1) {
                    connection.close();
                    connection = null;
                }
            }
        }

        if (connection != null) {
            _activateConnection(connection);
            _LOGGER.debug(DNP3Messages.CONNECTION_OPENED, connection);
        } else {
            _LOGGER
                .warn(
                    DNP3Messages.CONNECTION_FAILED,
                    remoteEndPoint.getRemoteProxyName().orElse(null));
        }

        return connection;
    }

    /**
     * Asks if on a master.
     *
     * @return True if on a master.
     */
    @CheckReturnValue
    boolean isOnMaster()
    {
        return _context.isClientContext();
    }

    /**
     * Asks if on an outstation.
     *
     * @return True if on an outstation.
     */
    @CheckReturnValue
    boolean isOnOutstation()
    {
        return _context.isServerContext();
    }

    /**
     * Called on closed connection.
     *
     * @param connection The connection.
     */
    void onClosedConnection(@Nonnull final Connection connection)
    {
        final PAPConnectionListener connectionListener = _connectionListener;

        if (connectionListener != null) {
            Require
                .ignored(
                    connectionListener
                        .onLostConnection(
                                connection.getRemoteEndPoint().getRemoteProxy(),
                                        connection.getException()));
        }
    }

    /**
     * Called when a new datagram is received.
     *
     * @param sourceAddress The source address.
     * @param buffer The new datagram.
     */
    void onDatagramReceived(
            @Nonnull final InetSocketAddress sourceAddress,
            @Nonnull final ByteBuffer buffer)
    {
        RemoteEndPoint remoteEndPoint = _remoteEndPointByUDPSocketAddress
            .get(sourceAddress);

        if (isOnMaster()) {
            final Connection connection = _connectionByEndPoint
                .get(remoteEndPoint);

            if (connection instanceof UDPConnection) {
                ((UDPConnection) connection).onDatagramReceived(buffer);
            }
        } else {
            if (remoteEndPoint == null) {
                remoteEndPoint = _remoteEndPointByUDPAddress
                    .get(sourceAddress.getAddress());

                if (remoteEndPoint == null) {
                    final DNP3Proxy remoteProxy = new DNP3MasterProxy(
                        _context,
                        "Unknown-UDP-"
                        + _remoteEndPointByUDPSocketAddress.size());

                    remoteProxy
                        .setMaxFragmentSize(DNP3.DEFAULT_MAX_FRAGMENT_SIZE);
                    remoteEndPoint = new RemoteEndPoint(this, remoteProxy);
                    _remoteEndPointByProxy.put(remoteProxy, remoteEndPoint);
                    _remoteEndPointByUDPSocketAddress
                        .put(sourceAddress, remoteEndPoint);
                }
            }

            Connection connection = _connectionByEndPoint.get(remoteEndPoint);

            if ((connection != null)
                    && !(connection instanceof UDPConnection)) {
                connection.close();
                connection = null;
            }

            if (connection == null) {
                connection = new UDPConnection(
                    _localEndPoint,
                    remoteEndPoint,
                    _udpListener.getChannel(),
                    sourceAddress);
                _activateConnection(connection);
                _LOGGER
                    .debug(
                        DNP3Messages.MASTER_CONNECTION_ACCEPTED,
                        remoteEndPoint.getRemoteProxyName().orElse(null),
                        sourceAddress);
            }

            ((UDPConnection) connection).onDatagramReceived(buffer);
        }
    }

    /**
     * Called when a frame is received.
     *
     * @param frame The frame.
     * @param remoteEndPoint The sender of the frame.
     *
     * @return True if the frame should be accepted.
     *
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    boolean onFrameReceived(
            final Frame frame,
            final RemoteEndPoint remoteEndPoint)
        throws IOException
    {
        final Frame.Header frameHeader = frame.getHeader();

        final LogicalDevice logicalDevice = _logicalDeviceByAddress
            .get(Short.valueOf(frameHeader.getDestination()));

        if (logicalDevice == null) {
            return false;
        }

        return logicalDevice
            .onFrameReceived(remoteEndPoint, frameHeader.getSource());
    }

    /**
     * Called on new association.
     *
     * @param association The association.
     *
     * @throws IOException On I/O exception.
     */
    void onNewAssociation(
            @Nonnull final Association association)
        throws IOException
    {
        _associationListenerManager.onNewAssociation(association);
    }

    /**
     * Called when a fragment is received.
     *
     * @param association The receiving association.
     * @param receivedFragment The received fragment.
     *
     * @return True if this event has been handled.
     *
     * @throws IOException On I/O exception.
     */
    @CheckReturnValue
    boolean onReceivedFragment(
            @Nonnull final Association association,
            @Nonnull final Fragment receivedFragment)
        throws IOException
    {
        return _receivedFragmentListenerManager
            .onReceivedFragment(receivedFragment);
    }

    /**
     * Called when a socket is accepted.
     *
     * @param socketChannel The socket channel
     */
    void onSocketAccepted(@Nonnull final SocketChannel socketChannel)
    {
        final InetSocketAddress remoteSocketAddress =
            (InetSocketAddress) socketChannel
                .socket()
                .getRemoteSocketAddress();
        RemoteEndPoint remoteEndPoint;

        remoteEndPoint = _remoteEndPointByTCPSocketAddress
            .get(remoteSocketAddress);

        if (remoteEndPoint == null) {
            remoteEndPoint = _remoteEndPointByTCPAddress
                .get(remoteSocketAddress.getAddress());
        }

        if ((remoteEndPoint == null) && isOnOutstation()) {
            final DNP3Proxy remoteProxy = new DNP3MasterProxy(
                _context,
                "Unknown-TCP-" + _remoteEndPointByTCPSocketAddress.size());

            remoteProxy.setMaxFragmentSize(DNP3.DEFAULT_MAX_FRAGMENT_SIZE);
            remoteEndPoint = new RemoteEndPoint(this, remoteProxy);
            _remoteEndPointByProxy.put(remoteProxy, remoteEndPoint);
            _remoteEndPointByTCPSocketAddress
                .put(remoteSocketAddress, remoteEndPoint);
        }

        if (remoteEndPoint != null) {
            Connection connection = _connectionByEndPoint.get(remoteEndPoint);

            if (connection == null) {
                connection = new TCPConnection(
                    _localEndPoint,
                    remoteEndPoint,
                    socketChannel);
                _activateConnection(connection);
                _LOGGER
                    .debug(
                        isOnMaster()
                        ? DNP3Messages.OUTSTATION_CONNECTION_ACCEPTED
                        : DNP3Messages.MASTER_CONNECTION_ACCEPTED,
                        remoteEndPoint.getRemoteProxyName().orElse(null),
                        remoteSocketAddress);
            }
        }
    }

    /**
     * Receives messages.
     *
     * <p>Called by a logical device server thread.</p>
     *
     * @param association The association.
     *
     * @throws IOException On I/O exception.
     */
    void receiveMessages(
            @Nonnull final Association association)
        throws IOException
    {
        for (;;) {
            new ApplicationMessage(
                association,
                Optional.empty(),
                isOnOutstation())
                .receive();
        }
    }

    private static Optional<InetSocketAddress> _listenSocketAddress(
            final String listenAddress,
            final Integer listenPort)
    {
        final InetAddress localAddress = Inet
            .getLocalAddress(Optional.ofNullable(listenAddress));

        if (localAddress == null) {
            return Optional.empty();
        }

        final int port = (listenPort != null)? listenPort.intValue(): DNP3.PORT;

        return Optional.of(new InetSocketAddress(localAddress, port));
    }

    private void _activateConnection(final Connection connection)
    {
        final RemoteEndPoint remoteEndPoint = connection.getRemoteEndPoint();

        _connectionByEndPoint.put(remoteEndPoint, connection);
        connection.activate();
        Require
            .ignored(
                _connectionListener
                    .onNewConnection(remoteEndPoint.getRemoteProxy()));
    }

    private static final Logger _LOGGER = Logger
        .getInstance(ConnectionManager.class);

    private volatile AssociationListener.Manager _associationListenerManager =
        new AssociationListener.Manager();
    private final Map<RemoteEndPoint, Connection> _connectionByEndPoint =
        Collections
            .synchronizedMap(new IdentityHashMap<>());
    private volatile PAPConnectionListener _connectionListener;
    private final DNP3Context _context;
    private final LocalEndPoint _localEndPoint;
    private final Map<Short, LogicalDevice> _logicalDeviceByAddress =
        new HashMap<>();
    private final Map<String, LogicalDevice> _logicalDeviceByName =
        new HashMap<>();
    private final ReceivedFragmentListener.Manager _receivedFragmentListenerManager =
        new ReceivedFragmentListener.Manager();
    private final Map<DNP3Proxy, RemoteEndPoint> _remoteEndPointByProxy =
        new HashMap<>();
    private final Map<InetAddress, RemoteEndPoint> _remoteEndPointByTCPAddress =
        new HashMap<>();
    private final Map<InetSocketAddress, RemoteEndPoint> _remoteEndPointByTCPSocketAddress =
        new HashMap<>();
    private final Map<InetAddress, RemoteEndPoint> _remoteEndPointByUDPAddress =
        new HashMap<>();
    private final Map<InetSocketAddress, RemoteEndPoint> _remoteEndPointByUDPSocketAddress =
        new HashMap<>();
    private final Map<RemoteEndPoint, RemoteEndPoint> _remoteEndPoints =
        new HashMap<>();
    private boolean _setUpCompleted;
    private TCPSocketListener _tcpListener;
    private UDPDatagramListener _udpListener;
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
