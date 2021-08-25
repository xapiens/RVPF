/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3Proxy.java 4098 2019-06-25 16:46:46Z SFB $
 */

package org.rvpf.pap.dnp3;

import java.net.InetSocketAddress;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Origin;
import org.rvpf.base.tool.Require;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.SerialPortWrapper;
import org.rvpf.pap.dnp3.transport.LogicalDevice;
import org.rvpf.pap.modbus.Modbus;

/**
 * DNP3 proxy.
 */
public abstract class DNP3Proxy
    extends PAPProxy
{
    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected DNP3Proxy(@Nonnull final DNP3Proxy other)
    {
        super(other);

        _connectTimeout = other._connectTimeout;
        _keepAliveTimeout = other._keepAliveTimeout;
        _logicalDeviceByAddress.putAll(other._logicalDeviceByAddress);
        _logicalDeviceByName.putAll(other._logicalDeviceByName);
        _maxFragmentSize = other._maxFragmentSize;
        _replyTimeout = other._replyTimeout;
        _serialPortName = other._serialPortName;
        _serialPortSpeed = other._serialPortSpeed;
        _tcpSocketAddresses.addAll(other._tcpSocketAddresses);
        _udpSocketAddresses.addAll(other._udpSocketAddresses);
    }

    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param origin The origin representing the remote.
     */
    protected DNP3Proxy(
            @Nonnull final DNP3Context context,
            @Nonnull final Origin origin)
    {
        super(context, origin);
    }

    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param name The name for the synthesized origin.
     */
    protected DNP3Proxy(
            @Nonnull final DNP3Context context,
            @Nonnull final String name)
    {
        this(context, _newOrigin(name));
    }

    /**
     * Adds a TCP socket address.
     *
     * @param socketAddress The TCP socket address.
     */
    public final void addTCPSocketAddress(
            @Nonnull final InetSocketAddress socketAddress)
    {
        _tcpSocketAddresses.add(socketAddress);
    }

    /**
     * Adds a UDP socket address.
     *
     * @param socketAddress The UDP socket address.
     */
    public final void addUDPSocketAddress(
            @Nonnull final InetSocketAddress socketAddress)
    {
        _udpSocketAddresses.add(socketAddress);
    }

    /**
     * Gets the connect timeout.
     *
     * @return The connect timeout.
     */
    @CheckReturnValue
    public final int getConnectTimeout()
    {
        return _connectTimeout;
    }

    /**
     * Gets the keep-alive timeout.
     *
     * @return The keep-alive timeout.
     */
    @CheckReturnValue
    public final long getKeepAliveTimeout()
    {
        return _keepAliveTimeout;
    }

    /**
     * Gets the logical device when it is not ambiguous.
     *
     * @return The logical device (empty when none or ambiguous).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<LogicalDevice> getLogicalDevice()
    {
        final LogicalDevice logicalDevice;

        if (_logicalDeviceByAddress.size() == 1) {
            logicalDevice = _logicalDeviceByAddress.values().iterator().next();
        } else {
            logicalDevice = null;
        }

        return Optional.ofNullable(logicalDevice);
    }

    /**
     * Gets a logical device by its address.
     *
     * @param address The address.
     *
     * @return The logical device (empty when unknown).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<LogicalDevice> getLogicalDevice(
            @Nonnull final Short address)
    {
        return Optional.ofNullable(_logicalDeviceByAddress.get(address));
    }

    /**
     * Gets a logical device by its name.
     *
     * @param name The name.
     *
     * @return The logical device (empty when unknown).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<LogicalDevice> getLogicalDevice(
            @Nonnull final String name)
    {
        return Optional
            .ofNullable(
                _logicalDeviceByName.get(name.toUpperCase(Locale.ROOT)));
    }

    /**
     * Gets the maximum fragment size.
     *
     * @return The maximum fragment size.
     */
    @CheckReturnValue
    public final int getMaxFragmentSize()
    {
        return _maxFragmentSize;
    }

    /**
     * Gets the reply timeout.
     *
     * @return The reply timeout.
     */
    @CheckReturnValue
    public final long getReplyTimeout()
    {
        return _replyTimeout;
    }

    /**
     * Gets the serial port name.
     *
     * @return The serial port name.
     */
    @Nonnull
    @CheckReturnValue
    public final String getSerialPortName()
    {
        return Require.notNull(_serialPortName);
    }

    /**
     * Gets the serial port speed.
     *
     * @return The serial port speed.
     */
    @CheckReturnValue
    public final int getSerialPortSpeed()
    {
        return _serialPortSpeed;
    }

    /**
     * Gets the TCP socket addresses.
     *
     * @return The socket TCP addresses.
     */
    @Nonnull
    @CheckReturnValue
    public final List<InetSocketAddress> getTCPSocketAddresses()
    {
        return Collections.unmodifiableList(_tcpSocketAddresses);
    }

    /**
     * Gets the UDP socket addresses.
     *
     * @return The socket UDP addresses.
     */
    @Nonnull
    @CheckReturnValue
    public final List<InetSocketAddress> getUDPSocketAddresses()
    {
        return Collections.unmodifiableList(_udpSocketAddresses);
    }

    /**
     * Asks if the remote is master.
     *
     * @return True if the remote is master.
     */
    @CheckReturnValue
    public abstract boolean isMaster();

    /**
     * Registers a logical device.
     *
     * @param logicalDevice The logical device.
     */
    public final void registerLogicalDevice(
            @Nonnull final LogicalDevice logicalDevice)
    {
        final String logicalDeviceName = logicalDevice.getName();
        final Short logicalDeviceAddress = logicalDevice.getAddress();
        Optional<LogicalDevice> knownLogicalDevice;

        if (!logicalDeviceName.isEmpty()) {
            knownLogicalDevice = getLogicalDevice(logicalDeviceName);

            if (!knownLogicalDevice.isPresent()) {
                _logicalDeviceByName
                    .put(
                        logicalDeviceName.toUpperCase(Locale.ROOT),
                        logicalDevice);
            } else if (!logicalDeviceAddress
                .equals(knownLogicalDevice.get().getAddress())) {
                getThisLogger()
                    .warn(
                        DNP3Messages.DUPLICATE_LOGICAL_DEVICE,
                        logicalDeviceName,
                        getName().orElse(null));
            }
        }

        knownLogicalDevice = getLogicalDevice(logicalDeviceAddress);

        if (!knownLogicalDevice.isPresent()) {
            _logicalDeviceByAddress.put(logicalDeviceAddress, logicalDevice);
        } else if (!logicalDeviceName
            .equalsIgnoreCase(knownLogicalDevice.get().getName())) {
            getThisLogger()
                .warn(
                    DNP3Messages.DUPLICATE_LOGICAL_DEVICE,
                    logicalDeviceAddress,
                    getName().orElse(null));
        }
    }

    /**
     * Sets the connect timeout.
     *
     * @param connectTimeout The optional connect timeout.
     */
    public final void setConnectTimeout(
            @Nonnull final Optional<ElapsedTime> connectTimeout)
    {
        _connectTimeout = (int) (connectTimeout
            .isPresent()? connectTimeout.get().toMillis(): 0);
    }

    /**
     * Sets the keep-alive timeout.
     *
     * @param keepAliveTimeout The optional keep-alive timeout.
     */
    public final void setKeepAliveTimeout(
            @Nonnull final Optional<ElapsedTime> keepAliveTimeout)
    {
        _keepAliveTimeout = keepAliveTimeout
            .isPresent()? keepAliveTimeout.get().toMillis(): -1;
    }

    /**
     * Sets the maximum fragment size.
     *
     * @param maxFragmentSize The maximum fragment size.
     */
    public final void setMaxFragmentSize(final int maxFragmentSize)
    {
        if (maxFragmentSize < DNP3.MINIMUM_MAX_FRAGMENT_SIZE) {
            throw new IllegalArgumentException();
        }

        _maxFragmentSize = maxFragmentSize;
    }

    /**
     * Sets the reply timeout.
     *
     * @param replyTimeout The optional reply timeout.
     */
    public final void setReplyTimeout(
            @Nonnull final Optional<ElapsedTime> replyTimeout)
    {
        _replyTimeout = replyTimeout
            .isPresent()? replyTimeout.get().toMillis(): -1;
    }

    /**
     * Sets up this proxy.
     *
     * @param originAttributes The origin attributes.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public final boolean setUp(@Nonnull final Attributes originAttributes)
    {
        final int maxFragmentSize = originAttributes
            .getInt(
                DNP3.MAX_FRAGMENT_SIZE_ATTRIBUTE,
                isMaster()
                ? DNP3.DEFAULT_MAX_FRAGMENT_SIZE
                : DNP3.MINIMUM_MAX_FRAGMENT_SIZE);

        try {
            setMaxFragmentSize(maxFragmentSize);
        } catch (final IllegalArgumentException exception) {
            getThisLogger()
                .warn(
                    PAPMessages.BAD_ATTRIBUTE_VALUE,
                    Modbus.TYPE_ATTRIBUTE,
                    getOrigin(),
                    Integer.valueOf(maxFragmentSize));

            return false;
        }

        setConnectTimeout(
            originAttributes
                .getElapsed(
                    DNP3.CONNECT_TIMEOUT_ATTRIBUTE,
                    Optional.of(DEFAULT_CONNECT_TIMEOUT),
                    Optional.empty()));
        setReplyTimeout(
            originAttributes
                .getElapsed(
                    DNP3.REPLY_TIMEOUT_ATTRIBUTE,
                    Optional.of(DEFAULT_REPLY_TIMEOUT),
                    Optional.empty()));
        setKeepAliveTimeout(
            originAttributes
                .getElapsed(
                    DNP3.KEEP_ALIVE_TIMEOUT_ATTRIBUTE,
                    Optional.of(DEFAULT_KEEP_ALIVE_TIMEOUT),
                    Optional.empty()));

        _serialPortName = originAttributes
            .getString(DNP3.SERIAL_PORT_ATTRIBUTE, Optional.of(""))
            .get()
            .trim();
        _serialPortSpeed = originAttributes
            .getInt(
                DNP3.SERIAL_SPEED_ATTRIBUTE,
                SerialPortWrapper.Builder.DEFAULT_PORT_SPEED);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean supportsWildcardAddress()
    {
        return true;
    }

    private static Origin _newOrigin(final String name)
    {
        final OriginEntity.Builder originBuilder = OriginEntity.newBuilder();

        originBuilder.setName(name);

        return originBuilder.build();
    }

    public static final ElapsedTime DEFAULT_CONNECT_TIMEOUT = ElapsedTime
        .fromMillis(30000);
    public static final ElapsedTime DEFAULT_KEEP_ALIVE_TIMEOUT = ElapsedTime
        .fromMillis(60000);
    public static final ElapsedTime DEFAULT_REPLY_TIMEOUT = ElapsedTime
        .fromMillis(5000);

    private int _connectTimeout;
    private long _keepAliveTimeout;
    private final Map<Short, LogicalDevice> _logicalDeviceByAddress =
        new HashMap<>();
    private final Map<String, LogicalDevice> _logicalDeviceByName =
        new HashMap<>();
    private int _maxFragmentSize;
    private long _replyTimeout;
    private String _serialPortName;
    private int _serialPortSpeed;
    private final List<InetSocketAddress> _tcpSocketAddresses =
        new LinkedList<>();
    private final List<InetSocketAddress> _udpSocketAddresses =
        new LinkedList<>();
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
