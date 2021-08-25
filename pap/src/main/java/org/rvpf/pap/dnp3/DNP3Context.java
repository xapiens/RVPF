/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3Context.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.pap.dnp3;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.Attributes;
import org.rvpf.base.BaseMessages;
import org.rvpf.base.Content;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Traces;
import org.rvpf.content.BooleanContent;
import org.rvpf.content.NumberContent;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.pap.PAP;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.dnp3.object.ObjectRange;
import org.rvpf.pap.dnp3.object.PointType;
import org.rvpf.pap.dnp3.object.content.DataType;
import org.rvpf.pap.dnp3.transport.LogicalDevice;

/**
 * DNP3 context.
 */
public abstract class DNP3Context
    extends PAPContext
{
    /**
     * Constructs an instance.
     *
     * @param metadata The optional metadata.
     * @param traces The traces (optional).
     */
    protected DNP3Context(
            @Nonnull final Optional<Metadata> metadata,
            @Nonnull final Optional<Traces> traces)
    {
        super(new DNP3Support(), metadata, traces);

        for (final PointType pointType: EnumSet.allOf(PointType.class)) {
            _pointsByType.put(pointType, new ConcurrentHashMap<>());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean addRemoteOrigin(
            final Origin remoteOrigin,
            final Attributes originAttributes)
    {
        final boolean isFirstProxy = getRemoteProxyByOrigin().isEmpty();

        if (_remoteProxyByTCPAddress.isEmpty() && !isFirstProxy) {
            getThisLogger().warn(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);

            return false;
        }

        final DNP3Proxy remoteProxy = newRemoteProxy(remoteOrigin);

        if (!remoteProxy.setUp(originAttributes)) {
            return false;
        }

        if (!_addTCPAddressesforRemoteProxy(
                remoteProxy,
                isFirstProxy,
                originAttributes)) {
            return false;
        }

        if (!_addUDPAddressesForRemoteProxy(
                remoteProxy,
                isFirstProxy,
                originAttributes)) {
            return false;
        }

        final String serialPortName = remoteProxy.getSerialPortName();

        if (!serialPortName.isEmpty()) {
            _remoteProxyBySerialPortName
                .put(serialPortName.toUpperCase(Locale.ROOT), remoteProxy);
        }

        final LogicalDevice[] logicalDevices = logicalDevices(
            originAttributes.getStrings(DNP3.LOGICAL_DEVICE_ATTRIBUTE),
            remoteProxy.getName().orElse(null));

        if (logicalDevices == null) {
            return false;
        }

        Arrays
            .stream(logicalDevices)
            .forEach(
                logicalDevice -> remoteProxy
                    .registerLogicalDevice(logicalDevice));

        registerRemoteProxy(remoteProxy);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean addRemotePoint(
            final Point remotePoint,
            final Attributes pointAttributes)
    {
        if (!((PointEntity) remotePoint).setUp(getMetadata())) {
            return false;
        }

        final DNP3Proxy remoteProxy = (DNP3Proxy) getRemoteProxy(remotePoint)
            .orElse(null);

        if (remoteProxy == null) {
            return false;
        }

        if (pointAttributes.getBoolean(PAP.CONNECTION_STATE_ATTRIBUTE)) {
            return true;
        }

        final LogicalDevice logicalDevice = _logicalDevice(remotePoint);
        final ObjectRange objectRange = _objectRange(remotePoint);
        final PointType pointType = _pointType(remotePoint);
        final DataType dataType = _dataType(remotePoint, pointType);

        if ((logicalDevice == null)
                || (objectRange == null)
                || (pointType == null)
                || (dataType == null)) {
            return false;
        }

        _remotePointsMap
            .put(
                remotePoint,
                new DNP3StationPoint(
                    remotePoint,
                    logicalDevice,
                    pointType,
                    objectRange,
                    dataType));

        registerRemotePoint(remotePoint);

        final Map<ObjectRange, Point> points = _pointsByType.get(pointType);
        final Point previousPoint = points.put(objectRange, remotePoint);

        if (previousPoint != null) {
            getThisLogger()
                .warn(
                    DNP3Messages.RECONFIGURED_OBJECT,
                    pointType,
                    objectRange,
                    previousPoint,
                    remotePoint);

            return false;
        }

        return true;
    }

    /**
     * Gets the point for a point type and object range.
     *
     * @param pointType The point type.
     * @param objectRange The object range for the point.
     *
     * @return The point (empty if unconfigured).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Point> getPoint(
            @Nonnull final PointType pointType,
            @Nonnull final ObjectRange objectRange)
    {
        final Map<ObjectRange, Point> points = _pointsByType.get(pointType);
        Point point = points.get(objectRange);

        if (point == null) {
            Logger
                .getInstance(getClass())
                .warn(
                    DNP3Messages.UNCONFIGURED_OBJECT,
                    pointType,
                    String.valueOf(objectRange.getStartIndex()));

            point = Point.NULL;
            points.put(objectRange, point);
        }

        return (point != Point.NULL)? Optional.of(point): Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getProtocolName()
    {
        return DNP3.PROTOCOL_NAME;
    }

    /** {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends DNP3Proxy> getRemoteProxies()
    {
        return (Collection<? extends DNP3Proxy>) super.getRemoteProxies();
    }

    /**
     * Gets the remote proxy for the specified serial port name.
     *
     * @param serialPortName The serial port name.
     *
     * @return The remote proxy (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<DNP3Proxy> getRemoteProxyBySerialPortName(
            @Nonnull final String serialPortName)
    {
        return Optional
            .ofNullable(
                _remoteProxyBySerialPortName
                    .get(serialPortName.trim().toUpperCase(Locale.ROOT)));
    }

    /**
     * Gets the remote proxy for the specified TCP address.
     *
     * @param address The TCP address.
     *
     * @return The remote proxy (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<DNP3Proxy> getRemoteProxyByTCPAddress(
            @Nonnull final InetAddress address)
    {
        final DNP3Proxy remoteProxy;

        if (_remoteProxyByTCPAddress.isEmpty()) {
            final Collection<? extends DNP3Proxy> remoteProxies =
                getRemoteProxies();

            if (remoteProxies.isEmpty()) {
                remoteProxy = null;
            } else {
                remoteProxy = remoteProxies.iterator().next();
            }
        } else {
            remoteProxy = _remoteProxyByTCPAddress.get(address);
        }

        return Optional.ofNullable(remoteProxy);
    }

    /**
     * Gets the remote proxy for the specified UDP address.
     *
     * @param address The UDP address.
     *
     * @return The remote proxy (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<DNP3Proxy> getRemoteProxyByUDPAddress(
            @Nonnull final InetAddress address)
    {
        final DNP3Proxy remoteProxy;

        if (_remoteProxyByUDPAddress.isEmpty()) {
            final Collection<? extends DNP3Proxy> remoteProxies =
                getRemoteProxies();

            if (remoteProxies.isEmpty()) {
                remoteProxy = null;
            } else {
                remoteProxy = remoteProxies.iterator().next();
            }
        } else {
            remoteProxy = _remoteProxyByUDPAddress.get(address);
        }

        return Optional.ofNullable(remoteProxy);
    }

    /**
     * Gets a remote station point.
     *
     * @param point A metadata point.
     *
     * @return The remote station point.
     *
     * @throws IllegalArgumentException When the station point is unknown.
     */
    @Nonnull
    @CheckReturnValue
    public DNP3StationPoint getRemoteStationPoint(@Nonnull final Point point)
    {
        final DNP3StationPoint stationPoint = _remotePointsMap.get(point);

        if (stationPoint == null) {
            throw new IllegalArgumentException(
                "Point unknown in DNP3 context: " + point);
        }

        return stationPoint;
    }

    /**
     * Returns logical devices from the declarations.
     *
     * @param logicalDeviceDecls The logical device declarations.
     * @param endPointName The end point name.
     *
     * @return The logical devices (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public final LogicalDevice[] logicalDevices(
            @Nonnull final String[] logicalDeviceDecls,
            @Nonnull final String endPointName)
    {
        final LogicalDevice[] logicalDevices =
            new LogicalDevice[logicalDeviceDecls.length];

        for (int i = 0; i < logicalDevices.length; ++i) {
            final String logicalDeviceDecl = logicalDeviceDecls[i];
            final Matcher logicalDeviceMatcher = _LOGICAL_DEVICE_PATTERN
                .matcher(logicalDeviceDecl.trim());

            if (!logicalDeviceMatcher.matches()) {
                throw new InternalError();
            }

            String namePart = logicalDeviceMatcher.group(_NAME_GROUP);
            String addressPart = logicalDeviceMatcher.group(_ADDRESS_GROUP);

            if (namePart.isEmpty() && (addressPart == null)) {
                getThisLogger()
                    .warn(
                        DNP3Messages.BAD_LOGICAL_DEVICE,
                        logicalDeviceDecl,
                        endPointName);

                return null;
            }

            if (addressPart == null) {    // Implies a leading ':'.
                addressPart = namePart;
                namePart = "";
            }

            final Short address;

            try {
                address = Short.valueOf(addressPart);
            } catch (final NumberFormatException exception) {
                getThisLogger()
                    .warn(
                        DNP3Messages.BAD_LOGICAL_DEVICE,
                        logicalDeviceDecl,
                        endPointName);

                return null;
            }

            logicalDevices[i] = new LogicalDevice(namePart, address);
        }

        return logicalDevices;
    }

    /** {@inheritDoc}
     */
    @Override
    protected int getDefaultPortForRemoteOrigin()
    {
        return DNP3.PORT;
    }

    /** {@inheritDoc}
     */
    @Override
    protected abstract DNP3Proxy newRemoteProxy(Origin remoteOrigin);

    private boolean _addTCPAddressesforRemoteProxy(
            final DNP3Proxy remoteProxy,
            final boolean isFirstProxy,
            final Attributes originAttributes)
    {
        final String[] socketAddressStrings = originAttributes
            .getStrings(DNP3.TCP_ADDRESS_ATTRIBUTE);

        for (final String remoteProxyAddress: socketAddressStrings) {
            if (remoteProxy.supportsWildcardAddress()
                    && "*".equals(remoteProxyAddress.trim())) {
                if (!isFirstProxy || (socketAddressStrings.length > 1)) {
                    getThisLogger()
                        .warn(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);

                    return false;
                }

                return true;
            }

            final Optional<InetSocketAddress> socketAddress = Inet
                .socketAddress(remoteProxyAddress);

            if (!socketAddress.isPresent()) {
                getThisLogger()
                    .warn(BaseMessages.BAD_ADDRESS, remoteProxyAddress);

                return false;
            }

            int port = socketAddress.get().getPort();

            if (port <= 0) {
                port = originAttributes
                    .getInt(
                        DNP3.TCP_PORT_ATTRIBUTE,
                        getDefaultPortForRemoteOrigin());
            }

            try {
                final int tcpPort = port;

                Arrays
                    .stream(
                        InetAddress
                            .getAllByName(socketAddress.get().getHostString()))
                    .forEach(
                        address -> remoteProxy
                            .addTCPSocketAddress(
                                    new InetSocketAddress(address, tcpPort)));
            } catch (final UnknownHostException exception) {
                getThisLogger()
                    .warn(
                        PAPMessages.UNKNOWN_ORIGIN_ADDRESS,
                        remoteProxy.getOrigin(),
                        remoteProxyAddress);

                return false;
            }
        }

        final List<InetSocketAddress> socketAddresses = remoteProxy
            .getTCPSocketAddresses();

        for (final InetSocketAddress socketAddress: socketAddresses) {
            final DNP3Proxy otherProxy = _remoteProxyByTCPAddress
                .get(socketAddress.getAddress());

            if (otherProxy != null) {
                getThisLogger()
                    .warn(
                        PAPMessages.AMBIGUOUS_ORIGIN_ADDRESS,
                        remoteProxy.getOrigin(),
                        socketAddress.getAddress(),
                        otherProxy.getOrigin());

                return false;
            }

            _remoteProxyByTCPAddress
                .put(socketAddress.getAddress(), remoteProxy);
        }

        return true;
    }

    private boolean _addUDPAddressesForRemoteProxy(
            final DNP3Proxy remoteProxy,
            final boolean isFirstProxy,
            final Attributes originAttributes)
    {
        final String[] socketAddressStrings = originAttributes
            .getStrings(DNP3.UDP_ADDRESS_ATTRIBUTE);
        final List<InetSocketAddress> socketAddresses = remoteProxy
            .getUDPSocketAddresses();

        for (final String remoteProxyAddress: socketAddressStrings) {
            if (remoteProxy.supportsWildcardAddress()
                    && "*".equals(remoteProxyAddress.trim())) {
                if (!isFirstProxy || (socketAddressStrings.length > 1)) {
                    getThisLogger()
                        .warn(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);

                    return false;
                }

                return true;
            }

            final Optional<InetSocketAddress> socketAddress = Inet
                .socketAddress(remoteProxyAddress);

            if (!socketAddress.isPresent()) {
                getThisLogger()
                    .warn(BaseMessages.BAD_ADDRESS, remoteProxyAddress);

                return false;
            }

            int port = socketAddress.get().getPort();

            if (port <= 0) {
                port = originAttributes
                    .getInt(
                        DNP3.UDP_PORT_ATTRIBUTE,
                        getDefaultPortForRemoteOrigin());
            }

            try {
                for (final InetAddress address:
                        InetAddress
                            .getAllByName(
                                    socketAddress.get().getHostString())) {
                    remoteProxy
                        .addUDPSocketAddress(
                            new InetSocketAddress(address, port));
                }
            } catch (final UnknownHostException exception) {
                getThisLogger()
                    .warn(
                        PAPMessages.UNKNOWN_ORIGIN_ADDRESS,
                        remoteProxy.getOrigin(),
                        remoteProxyAddress);

                return false;
            }
        }

        for (final InetSocketAddress socketAddress: socketAddresses) {
            final DNP3Proxy otherProxy = _remoteProxyByUDPAddress
                .get(socketAddress.getAddress());

            if (otherProxy != null) {
                getThisLogger()
                    .warn(
                        PAPMessages.AMBIGUOUS_ORIGIN_ADDRESS,
                        remoteProxy.getOrigin(),
                        socketAddress.getAddress(),
                        otherProxy.getOrigin());

                return false;
            }

            _remoteProxyByUDPAddress
                .put(socketAddress.getAddress(), remoteProxy);
        }

        return true;
    }

    private DataType _dataType(
            final Point remotePoint,
            final PointType pointType)
    {
        if (pointType == null) {
            return null;
        }

        final String dataTypeName = remotePoint
            .getAttributes(DNP3.ATTRIBUTES_USAGE)
            .get()
            .getString(DNP3.DATA_TYPE_ATTRIBUTE)
            .orElse(null);
        DataType dataType = null;

        if (dataTypeName != null) {
            try {
                dataType = DataType
                    .valueOf(dataTypeName.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException exception) {
                getThisLogger()
                    .warn(
                        DNP3Messages.UNKNOWN_DATA_TYPE,
                        dataTypeName,
                        remotePoint);
            }
        } else {
            final Optional<Content> content = remotePoint.getContent();

            if (content.isPresent()) {
                dataType = pointType
                    .getSupport()
                    .getDataType(content.get())
                    .orElse(DataType.ANY);
            }
        }

        if (dataType == null) {
            getThisLogger()
                .warn(DNP3Messages.UNKNOWN_DATA_TYPE, "", remotePoint);
        }

        return dataType;
    }

    private LogicalDevice _logicalDevice(final Point remotePoint)
    {
        final DNP3Proxy remoteProxy = (DNP3Proxy) getRemoteProxy(remotePoint)
            .get();
        final Optional<Attributes> pointAttributes = remotePoint
            .getAttributes(DNP3.ATTRIBUTES_USAGE);

        if (!pointAttributes.isPresent()) {
            getThisLogger()
                .warn(
                    PAPMessages.MISSING_ATTRIBUTES,
                    DNP3.ATTRIBUTES_USAGE,
                    remotePoint);

            return null;
        }

        final String logicalDeviceAttribute = pointAttributes
            .get()
            .getString(DNP3.LOGICAL_DEVICE_ATTRIBUTE)
            .orElse(null);
        Optional<LogicalDevice> logicalDevice;

        if (logicalDeviceAttribute != null) {
            final Matcher logicalDeviceMatcher = _LOGICAL_DEVICE_PATTERN
                .matcher(logicalDeviceAttribute.trim());

            if (!logicalDeviceMatcher.matches()) {
                throw new InternalError();
            }

            final String namePart = logicalDeviceMatcher.group(_NAME_GROUP);
            final String addressPart = logicalDeviceMatcher
                .group(_ADDRESS_GROUP);

            if (addressPart == null) {
                if (namePart.isEmpty()) {
                    getThisLogger()
                        .warn(
                            DNP3Messages.BAD_LOGICAL_DEVICE,
                            logicalDeviceAttribute,
                            remoteProxy.getName().orElse(null));

                    return null;
                }

                logicalDevice = remoteProxy.getLogicalDevice(namePart);

                if (!logicalDevice.isPresent()) {
                    final Short address;

                    try {
                        address = Short.valueOf(namePart);
                    } catch (final NumberFormatException exception) {
                        getThisLogger()
                            .warn(
                                DNP3Messages.UNKNOWN_LOGICAL_DEVICE,
                                namePart,
                                remoteProxy.getName().orElse(null),
                                remotePoint);

                        return null;
                    }

                    logicalDevice = remoteProxy.getLogicalDevice(address);

                    if (!logicalDevice.isPresent()) {
                        logicalDevice = Optional
                            .of(new LogicalDevice("", address));
                        remoteProxy.registerLogicalDevice(logicalDevice.get());
                    }
                }
            } else {
                final Short address;

                try {
                    address = Short.valueOf(addressPart);
                } catch (final NumberFormatException exception) {
                    getThisLogger()
                        .warn(
                            DNP3Messages.BAD_LOGICAL_DEVICE,
                            addressPart,
                            remoteProxy.getName().orElse(null));

                    return null;
                }

                logicalDevice = remoteProxy.getLogicalDevice(address);

                if (!logicalDevice.isPresent()) {
                    logicalDevice = Optional.of(new LogicalDevice("", address));
                    remoteProxy.registerLogicalDevice(logicalDevice.get());
                }
            }
        } else {
            logicalDevice = remoteProxy.getLogicalDevice();

            if (!logicalDevice.isPresent()) {
                logicalDevice = Optional.of(_DEFAULT_LOGICAL_DEVICE);
            }
        }

        return logicalDevice.get();
    }

    private ObjectRange _objectRange(final Point remotePoint)
    {
        final Attributes pointAttributes = remotePoint
            .getAttributes(DNP3.ATTRIBUTES_USAGE)
            .get();
        final Optional<Integer> index = pointAttributes
            .getInteger(DNP3.INDEX_ATTRIBUTE, Optional.empty());
        final ObjectRange objectRange;

        if (index.isPresent()) {
            if (pointAttributes.containsValueKey(DNP3.START_INDEX_ATTRIBUTE)) {
                getThisLogger()
                    .warn(
                        PAPMessages.CONFLICTING_ATTRIBUTES,
                        DNP3.INDEX_ATTRIBUTE,
                        DNP3.START_INDEX_ATTRIBUTE,
                        DNP3.ATTRIBUTES_USAGE);
                objectRange = null;
            } else if (pointAttributes
                .containsValueKey(DNP3.STOP_INDEX_ATTRIBUTE)) {
                getThisLogger()
                    .warn(
                        PAPMessages.CONFLICTING_ATTRIBUTES,
                        DNP3.INDEX_ATTRIBUTE,
                        DNP3.STOP_INDEX_ATTRIBUTE,
                        DNP3.ATTRIBUTES_USAGE);
                objectRange = null;
            } else {
                objectRange = ObjectRange
                    .newIndexInstance(index.get().intValue());
            }
        } else {
            final Optional<Integer> startIndex = pointAttributes
                .getInteger(DNP3.START_INDEX_ATTRIBUTE, Optional.empty());
            final Optional<Integer> stopIndex = pointAttributes
                .getInteger(DNP3.STOP_INDEX_ATTRIBUTE, Optional.empty());

            if (startIndex.isPresent()) {
                if (!stopIndex.isPresent()) {
                    getThisLogger()
                        .warn(
                            PAPMessages.MISSING_ATTRIBUTE,
                            DNP3.STOP_INDEX_ATTRIBUTE,
                            DNP3.ATTRIBUTES_USAGE,
                            remotePoint);
                    objectRange = null;
                } else {
                    objectRange = ObjectRange
                        .newIndexInstance(
                            startIndex.get().intValue(),
                            stopIndex.get().intValue());
                }
            } else if (stopIndex.isPresent()) {
                getThisLogger()
                    .warn(
                        PAPMessages.MISSING_ATTRIBUTE,
                        DNP3.START_INDEX_ATTRIBUTE,
                        DNP3.ATTRIBUTES_USAGE,
                        remotePoint);
                objectRange = null;
            } else {
                getThisLogger()
                    .warn(
                        PAPMessages.MISSING_ATTRIBUTE,
                        DNP3.INDEX_ATTRIBUTE,
                        DNP3.ATTRIBUTES_USAGE,
                        remotePoint);
                objectRange = null;
            }
        }

        return objectRange;
    }

    private PointType _pointType(final Point remotePoint)
    {
        final Attributes pointAttributes = remotePoint
            .getAttributes(DNP3.ATTRIBUTES_USAGE)
            .get();
        final String pointTypeName = pointAttributes
            .getString(DNP3.POINT_TYPE_ATTRIBUTE)
            .orElse(null);
        PointType pointType;

        if (pointTypeName != null) {
            try {
                pointType = PointType
                    .valueOf(pointTypeName.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException exception) {
                getThisLogger()
                    .warn(
                        DNP3Messages.UNKNOWN_POINT_TYPE,
                        pointTypeName,
                        remotePoint);

                pointType = null;
            }
        } else {
            final Content content = remotePoint.getContent().orElse(null);

            if (content != null) {
                if (content instanceof BooleanContent) {
                    pointType = PointType.SINGLE_BIT_INPUT;
                } else if (content instanceof NumberContent) {
                    pointType = PointType.ANALOG_INPUT;
                } else {
                    pointType = null;
                }
            } else {
                getThisLogger()
                    .warn(
                        PAPMessages.MISSING_ATTRIBUTE,
                        DNP3.POINT_TYPE_ATTRIBUTE,
                        DNP3.ATTRIBUTES_USAGE,
                        remotePoint);

                pointType = null;
            }
        }

        return pointType;
    }

    private static final int _ADDRESS_GROUP = 2;
    private static final LogicalDevice _DEFAULT_LOGICAL_DEVICE =
        new LogicalDevice(
            "",
            Short.valueOf((short) 2));
    private static final Pattern _LOGICAL_DEVICE_PATTERN = Pattern
        .compile("([^:\\s]*)(?:\\s*[:]\\s*([0-9]+))?");
    private static final int _NAME_GROUP = 1;

    private final Map<PointType, Map<ObjectRange, Point>> _pointsByType =
        new HashMap<>();
    private final Map<String, DNP3Proxy> _remoteProxyBySerialPortName =
        new HashMap<>();
    private final Map<InetAddress, DNP3Proxy> _remoteProxyByTCPAddress =
        new HashMap<>();
    private final Map<InetAddress, DNP3Proxy> _remoteProxyByUDPAddress =
        new HashMap<>();
    private final Map<Point, DNP3StationPoint> _remotePointsMap =
        new ConcurrentHashMap<>();
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
