/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3TestsSupport.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.tests.pap.dnp3;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.pap.PAP;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.dnp3.DNP3;
import org.rvpf.pap.dnp3.DNP3Context;
import org.rvpf.pap.dnp3.DNP3Master;
import org.rvpf.pap.dnp3.DNP3MasterContext;
import org.rvpf.pap.dnp3.DNP3MasterProxy;
import org.rvpf.pap.dnp3.DNP3Outstation;
import org.rvpf.pap.dnp3.DNP3OutstationContext;
import org.rvpf.pap.dnp3.DNP3OutstationProxy;
import org.rvpf.pap.dnp3.DNP3Proxy;
import org.rvpf.pap.dnp3.DNP3StationPoint;
import org.rvpf.pap.dnp3.DNP3Support;
import org.rvpf.pap.dnp3.transport.Association;
import org.rvpf.pap.dnp3.transport.Connection;
import org.rvpf.pap.dnp3.transport.ConnectionManager;
import org.rvpf.pap.dnp3.transport.DataLinkLayer;
import org.rvpf.pap.dnp3.transport.Frame;
import org.rvpf.pap.dnp3.transport.LocalEndPoint;
import org.rvpf.pap.dnp3.transport.RemoteEndPoint;
import org.rvpf.pap.dnp3.transport.SerialConnection;
import org.rvpf.pap.dnp3.transport.TCPConnection;
import org.rvpf.pap.dnp3.transport.UDPConnection;
import org.rvpf.pap.dnp3.transport.UDPDatagramListener;
import org.rvpf.tests.Tests;
import org.rvpf.tests.pap.PAPTestsSupport;
import org.rvpf.tests.service.ServiceTests;

/**
 * DNP3 tests support.
 */
public final class DNP3TestsSupport
    extends PAPTestsSupport
{
    /**
     * Constructs an instance.
     *
     * @param metadata The optional metadata.
     */
    public DNP3TestsSupport(@Nonnull final Optional<Metadata> metadata)
    {
        super(metadata);
    }

    /**
     * Gets the master TCP port.
     *
     * @return The master TCP port.
     */
    @CheckReturnValue
    public static int getMasterTCPPort()
    {
        return _masterTCPPort;
    }

    /**
     * Gets the master UDP port.
     *
     * @return The master UDP port.
     */
    @CheckReturnValue
    public static int getMasterUDPPort()
    {
        return _masterUDPPort;
    }

    /**
     * Gets the outstation TCP port.
     *
     * @return The outstation TCP port.
     */
    @CheckReturnValue
    public static int getOutstationTCPPort()
    {
        return _outstationTCPPort;
    }

    /**
     * Gets the outstation UDP port.
     *
     * @return The outstation UDP port.
     */
    @CheckReturnValue
    public static int getOutstationUDPPort()
    {
        return _outstationUDPPort;
    }

    /**
     * Sets port properties.
     */
    public static void setPortProperties()
    {
        _masterTCPPort = Tests.allocateTCPPort();
        ServiceTests
            .setProperty(
                TESTS_MASTER_TCP_PORT_PROPERTY,
                String.valueOf(_masterTCPPort));
        _masterUDPPort = Tests.allocateUDPPort();
        ServiceTests
            .setProperty(
                TESTS_MASTER_UDP_PORT_PROPERTY,
                String.valueOf(_masterUDPPort));

        _outstationTCPPort = Tests.allocateTCPPort();
        ServiceTests
            .setProperty(
                TESTS_OUTSTATION_TCP_PORT_PROPERTY,
                String.valueOf(_outstationTCPPort));
        _outstationUDPPort = Tests.allocateUDPPort();
        ServiceTests
            .setProperty(
                TESTS_OUTSTATION_UDP_PORT_PROPERTY,
                String.valueOf(_outstationUDPPort));
    }

    /**
     * Clears the master.
     */
    public void clearMaster()
    {
        final DNP3Master master = _master.getAndSet(null);

        if (master != null) {
            master.tearDown();
        }
    }

    /**
     * Clears the outstation.
     */
    public void clearOutstation()
    {
        final DNP3Outstation outstation = _outstation.getAndSet(null);

        if (outstation != null) {
            outstation.tearDown();
        }
    }

    /**
     * Gets the master.
     *
     * @return The master.
     */
    @Nonnull
    @CheckReturnValue
    public DNP3Master getMaster()
    {
        DNP3Master master = _master.get();

        if (master == null) {
            final DNP3Support support = new DNP3Support();
            final Metadata metadata = getMetadata().get();
            final DNP3MasterContext masterContext = support
                .newClientContext(metadata, Optional.empty());

            Require.notNull(masterContext);
            master = support
                .newMaster(masterContext, TESTS_MASTER_DEVICE_ADDRESS);

            Require
                .success(
                    master
                        .setUp(
                                metadata
                                        .getPropertiesGroup(
                                                TESTS_MASTER_LISTEN_PROPERTIES)));

            if (!_master.compareAndSet(null, master)) {
                master = _master.get();
            }
        }

        return master;
    }

    /**
     * Gets the outstation.
     *
     * @return The outstation.
     */
    @Nonnull
    @CheckReturnValue
    public DNP3Outstation getOutstation()
    {
        DNP3Outstation outstation = _outstation.get();

        if (outstation == null) {
            final Metadata metadata = getMetadata().get();
            final DNP3Support support = new DNP3Support();
            final DNP3OutstationContext context = support
                .newServerContext(metadata, new String[0], Optional.empty());
            final KeyedGroups serverProperties = new KeyedGroups();

            outstation = support.newServer(context);
            serverProperties
                .addGroup(
                    PAP.LISTENER_PROPERTIES,
                    metadata
                        .getPropertiesGroup(
                                TESTS_OUTSTATION_LISTEN_PROPERTIES));
            Require.success(outstation.setUp(serverProperties));

            if (!_outstation.compareAndSet(null, outstation)) {
                outstation = _outstation.get();
            }
        }

        _outstationLocal = true;

        return outstation;
    }

    /**
     * Gets the outstation origin.
     *
     * @return The outstation origin.
     */
    @Nonnull
    @CheckReturnValue
    public Origin getOutstationOrigin()
    {
        final Optional<OriginEntity> outstationOrigin = getMetadata()
            .get()
            .getOriginEntity(Optional.of(_DNP3_ORIGIN_NAME));

        return outstationOrigin.get();
    }

    /**
     * Gets a point, given its key.
     *
     * @param key The point's key.
     *
     * @return The point.
     */
    @Nonnull
    @CheckReturnValue
    public Point getPoint(@Nonnull final String key)
    {
        return getMetadata().get().getPoint(key).get();
    }

    /**
     * Gets the proxy for a point.
     *
     * @param point The point.
     *
     * @return The proxy.
     */
    @Nonnull
    @CheckReturnValue
    public DNP3Proxy getPointProxy(final Point point)
    {
        final Optional<? extends PAPProxy> proxy = getMaster()
            .getPointProxy(point);

        return (DNP3Proxy) proxy.get();
    }

    /**
     * Gets the remote end point for a proxy.
     *
     * @param proxy The proxy.
     *
     * @return The remote end point.
     */
    @Nonnull
    @CheckReturnValue
    public RemoteEndPoint getRemoteEndPoint(@Nonnull final DNP3Proxy proxy)
    {
        final Optional<RemoteEndPoint> remoteEndPoint = getMaster()
            .getRemoteEndPoint(proxy);

        return remoteEndPoint.get();
    }

    /**
     * Gets the station point for a metadata point.
     *
     * @param point The metadata point.
     *
     * @return The station point.
     */
    @Nonnull
    @CheckReturnValue
    public DNP3StationPoint getStationPoint(final Point point)
    {
        return getMaster().getStationPoint(point);
    }

    /**
     * Asks if the outstation is local.
     *
     * @return True if the outstation is local.
     */
    @CheckReturnValue
    public boolean isOutstationLocal()
    {
        return _outstationLocal;
    }

    /**
     * Returns a new serial connection.
     *
     * @param fromMaster True if the connection originates from a master.
     * @param portName The port name.
     * @param portSpeed The port speed.
     *
     * @return The new TCP connection.
     *
     * @throws IOException On failure.
     */
    @Nonnull
    @CheckReturnValue
    public Connection newSerialConnection(
            final boolean fromMaster,
            @Nonnull final String portName,
            final int portSpeed)
        throws IOException
    {
        final Optional<Traces> traces = Optional.empty();
        final DNP3Context context = fromMaster? new DNP3MasterContext(
            getMetadata(),
            traces): new DNP3OutstationContext(
                getMetadata(),
                new String[0],
                traces);
        final ConnectionManager connectionManager = new ConnectionManager(
            context);
        final LocalEndPoint localEndPoint = new LocalEndPoint(
            connectionManager);
        final DNP3Proxy proxy = fromMaster? new DNP3OutstationProxy(
            context,
            ""): new DNP3MasterProxy(context, "");
        final _RemoteEndPoint remoteEndPoint = new _RemoteEndPoint(
            connectionManager,
            proxy);
        final SerialConnection connection = new SerialConnection(
            localEndPoint,
            remoteEndPoint,
            portName,
            portSpeed);

        Require.success(proxy.setUp(new Attributes(DNP3.ATTRIBUTES_USAGE)));
        remoteEndPoint.setConnection(connection);
        connection.purge();

        return connection;
    }

    /**
     * Returns a new TCP connection.
     *
     * @param fromMaster True if the connection originates from a master.
     * @param socketChannel The socket channel.
     *
     * @return The new TCP connection.
     */
    @Nonnull
    @CheckReturnValue
    public Connection newTCPConnection(
            final boolean fromMaster,
            @Nonnull final SocketChannel socketChannel)
    {
        final Optional<Traces> traces = Optional.empty();
        final DNP3Context context = fromMaster? new DNP3MasterContext(
            getMetadata(),
            traces): new DNP3OutstationContext(
                getMetadata(),
                new String[0],
                traces);
        final ConnectionManager connectionManager = new ConnectionManager(
            context);
        final LocalEndPoint localEndPoint = new LocalEndPoint(
            connectionManager);
        final DNP3Proxy proxy = fromMaster? new DNP3OutstationProxy(
            context,
            ""): new DNP3MasterProxy(context, "");
        final _RemoteEndPoint remoteEndPoint = new _RemoteEndPoint(
            connectionManager,
            proxy);
        final Connection connection = new TCPConnection(
            localEndPoint,
            remoteEndPoint,
            socketChannel);

        Require.success(proxy.setUp(new Attributes(DNP3.ATTRIBUTES_USAGE)));
        remoteEndPoint.setConnection(connection);

        return connection;
    }

    /**
     * Returns a new UDP connection.
     *
     * @param fromMaster True if the connection originates from a master.
     * @param localAddress The local address.
     * @param remoteAddress The remote address.
     *
     * @return The new UDP connection.
     *
     * @throws IOException On failure.
     */
    @Nonnull
    @CheckReturnValue
    public Connection newUDPConnection(
            final boolean fromMaster,
            @Nonnull final InetSocketAddress localAddress,
            @Nonnull final InetSocketAddress remoteAddress)
        throws IOException
    {
        final DatagramChannel datagramChannel = DatagramChannel
            .open(StandardProtocolFamily.INET);

        datagramChannel.bind(localAddress);

        final Optional<Traces> traces = Optional.empty();
        final DNP3Context context = fromMaster? new DNP3MasterContext(
            getMetadata(),
            traces): new DNP3OutstationContext(
                getMetadata(),
                new String[0],
                traces);
        final ConnectionManager connectionManager = new ConnectionManager(
            context);
        final LocalEndPoint localEndPoint = new LocalEndPoint(
            connectionManager);
        final DNP3Proxy proxy = fromMaster? new DNP3OutstationProxy(
            context,
            ""): new DNP3MasterProxy(context, "");
        final _RemoteEndPoint remoteEndPoint = new _RemoteEndPoint(
            connectionManager,
            proxy);
        final Connection connection = new _UDPTestsConnection(
            localEndPoint,
            remoteEndPoint,
            datagramChannel,
            remoteAddress);

        Require.success(proxy.setUp(new Attributes(DNP3.ATTRIBUTES_USAGE)));
        remoteEndPoint.setConnection(connection);

        return connection;
    }

    /** Master device address. */
    public static final short TESTS_MASTER_DEVICE_ADDRESS = 3;

    /** Master listen properties. */
    public static final String TESTS_MASTER_LISTEN_PROPERTIES =
        "tests.master.listen";

    /** Master TCP port property. */
    public static final String TESTS_MASTER_TCP_PORT_PROPERTY =
        "tests.dnp3.master.tcp.port";

    /** Master TCP port property. */
    public static final String TESTS_MASTER_UDP_PORT_PROPERTY =
        "tests.dnp3.master.udp.port";

    /** Outstation device address. */
    public static final short TESTS_OUTSTATION_DEVICE_ADDRESS = 4;

    /** Outstation listen properties. */
    public static final String TESTS_OUTSTATION_LISTEN_PROPERTIES =
        "tests.outstation.listen";

    /** Outstation TCP port property. */
    public static final String TESTS_OUTSTATION_TCP_PORT_PROPERTY =
        "tests.dnp3.outstation.tcp.port";

    /** Outstation TCP port property. */
    public static final String TESTS_OUTSTATION_UDP_PORT_PROPERTY =
        "tests.dnp3.outstation.udp.port";

    /**  */

    private static final String _DNP3_ORIGIN_NAME = "TestsDNP3";

    /**  */

    private static int _masterTCPPort;
    private static int _masterUDPPort;
    private static int _outstationTCPPort;
    private static int _outstationUDPPort;

    private final AtomicReference<DNP3Master> _master = new AtomicReference<>();
    private final AtomicReference<DNP3Outstation> _outstation =
        new AtomicReference<>();
    private volatile boolean _outstationLocal;

    /**
     * Remote end point.
     */
    private static final class _RemoteEndPoint
        extends RemoteEndPoint
    {
        /**
         * Constructs an instance.
         *
         * @param connectionManager The connection manager.
         * @param remoteProxy The remote proxy.
         */
        public _RemoteEndPoint(
                @Nonnull final ConnectionManager connectionManager,
                @Nonnull final DNP3Proxy remoteProxy)
        {
            super(connectionManager, remoteProxy);
        }

        /** {@inheritDoc}
         */
        @Override
        public Connection getConnection()
        {
            return _connection;
        }

        /** {@inheritDoc}
         */
        @Override
        public void onFrameReceived(
                @Nonnull final Frame frame)
            throws IOException
        {
            final Association association = getAssociation(
                (short) 0,
                (short) 0);
            final DataLinkLayer dataLinkLayer = association.getDataLinkLayer();

            dataLinkLayer.accept(frame);
        }

        /**
         * Sets the connection.
         *
         * @param connection The connection.
         */
        void setConnection(@Nonnull final Connection connection)
        {
            _connection = connection;
        }

        private Connection _connection;
    }


    /**
     * UDP tests connection.
     */
    private static final class _UDPTestsConnection
        extends Connection
    {
        /**
         * Constructs an instance.
         *
         * @param localEndPoint The local end point.
         * @param remoteEndPoint The remote end point.
         * @param datagramChannel The UDP datagram channel.
         * @param remoteAddress The remote address.
         */
        _UDPTestsConnection(
                @Nonnull final LocalEndPoint localEndPoint,
                @Nonnull final RemoteEndPoint remoteEndPoint,
                @Nonnull final DatagramChannel datagramChannel,
                @Nonnull final InetSocketAddress remoteAddress)
        {
            super(localEndPoint, remoteEndPoint);

            _datagramChannel = datagramChannel;
            _remoteAddress = remoteAddress;
            _udpConnection = new UDPConnection(
                localEndPoint,
                remoteEndPoint,
                datagramChannel,
                remoteAddress);
        }

        /** {@inheritDoc}
         */
        @Override
        public void doClose()
            throws IOException
        {
            _udpConnection.close();

            _datagramChannel.close();

            super.doClose();
        }

        /** {@inheritDoc}
         */
        @Override
        protected void doReceive(final ByteBuffer buffer)
            throws IOException
        {
            Require
                .equal(
                    _datagramChannel.receive(_receiveBuffer),
                    _remoteAddress);
            _receiveBuffer.flip();
            _udpConnection.onDatagramReceived(_receiveBuffer);
            _receiveBuffer.clear();
            _udpConnection.receive(buffer);
        }

        /** {@inheritDoc}
         */
        @Override
        protected void doSend(final ByteBuffer buffer)
            throws IOException
        {
            _udpConnection.send(buffer);
        }

        /** {@inheritDoc}
         */
        @Override
        protected String getName()
        {
            return "UDP-Test";
        }

        final UDPConnection _udpConnection;
        private final DatagramChannel _datagramChannel;
        private final ByteBuffer _receiveBuffer = ByteBuffer
            .allocate(UDPDatagramListener.MAXIMUM_DATAGRAM_SIZE);
        private final InetSocketAddress _remoteAddress;
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
