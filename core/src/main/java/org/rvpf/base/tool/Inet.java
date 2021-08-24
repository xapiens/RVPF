/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Inet.java 4107 2019-07-13 13:18:26Z SFB $
 */

package org.rvpf.base.tool;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;

/**
 * Inet.
 *
 * <p>Provides some utility functions for Inet addresses.</p>
 */
@Immutable
public final class Inet
{
    /**
     * No instances.
     */
    private Inet() {}

    /**
     * Allocates a TCP port.
     *
     * <p>Used by some tests to get an unallocated port.
     * Also used to create a 'stealth' service registry.</p>
     *
     * @return A free TCP port.
     */
    @CheckReturnValue
    public static int allocateTCPPort()
    {
        final Socket socket = new Socket();
        final int port;

        try {
            socket.bind(null);
            port = socket.getLocalPort();
            socket.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);    // Should not happen.
        }

        return port;
    }

    /**
     * Allocates an UDP port.
     *
     * <p>Used by some tests to get an unallocated port.</p>
     *
     * @return A free UDP port.
     */
    @CheckReturnValue
    public static int allocateUDPPort()
    {
        final int port;

        try {
            final DatagramSocket socket = new DatagramSocket(null);

            socket.bind(null);
            port = socket.getLocalPort();
            socket.close();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);    // Should not happen.
        }

        return port;
    }

    /**
     * Gets the hardware address.
     *
     * @param name An optional host name to select an interface.
     *
     * @return The hardware address (empty if not available).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<byte[]> getHardwareAddress(
            @Nonnull final Optional<String> name)
    {
        final InetAddress address;

        try {
            if ((name.isPresent()) && (name.get().length() > 0)) {
                address = InetAddress.getByName(name.get());
            } else {
                address = InetAddress.getLocalHost();
            }
        } catch (final UnknownHostException exception) {
            return Optional.empty();    // No IP address or name not found.
        }

        if (address.isLoopbackAddress()) {
            return Optional.empty();    // Local host defined on loopback.
        }

        final NetworkInterface networkInterface;

        try {
            networkInterface = NetworkInterface.getByInetAddress(address);
        } catch (final SocketException exception) {
            return Optional.empty();    // Name not on this host.
        }

        try {
            return Optional.ofNullable(networkInterface.getHardwareAddress());
        } catch (final Exception exception) {
            return Optional.empty();    // Network configuration problem.
        }
    }

    /**
     * Gets a local address.
     *
     * @param addressString An optional address string.
     *
     * @return The local address (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public static InetAddress getLocalAddress(
            @Nonnull final Optional<String> addressString)
    {
        final InetAddress address;

        try {
            address = InetAddress.getByName(addressString.orElse(null));
        } catch (final UnknownHostException exception) {
            Logger
                .getInstance(Inet.class)
                .warn(BaseMessages.UNKNOWN_HOST, addressString.get());

            return null;
        }

        if (!isOnLocalHost(address)) {
            Logger
                .getInstance(Inet.class)
                .warn(BaseMessages.HOST_NOT_LOCAL, address);

            return null;
        }

        return address;
    }

    /**
     * Gets the local host address.
     *
     * @return The local host address (loopback address on failure).
     */
    @Nonnull
    @CheckReturnValue
    public static InetAddress getLocalHostAddress()
    {
        InetAddress localHost;

        try {
            localHost = InetAddress.getLocalHost();
        } catch (final UnknownHostException exception) {
            localHost = InetAddress.getLoopbackAddress();
        }

        return Require.notNull(localHost);
    }

    /**
     * Asks if the supplied address is on an interface of the local host.
     *
     * @param address The address.
     *
     * @return True if on local host.
     */
    @CheckReturnValue
    public static boolean isOnLocalHost(@Nonnull final InetAddress address)
    {
        if (address.isLoopbackAddress()) {
            return true;
        }

        try {
            return NetworkInterface.getByInetAddress(address) != null;
        } catch (final SocketException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Asks if the supplied URI points to an interface on the local host.
     *
     * @param uri The URI.
     *
     * @return True if on local host.
     */
    @CheckReturnValue
    public static boolean isOnLocalHost(@Nonnull final URI uri)
    {
        final InetAddress address;

        try {
            address = InetAddress.getByName(uri.getHost());
        } catch (final UnknownHostException exception) {
            return false;
        }

        return isOnLocalHost(address);
    }

    /**
     * Returns a SocketAddress from an address string.
     *
     * <p>The format of the address string is "host:port". When present, a
     * leading "//" or a trailing "/" are ignored to permit a more general, URL
     * like, format.</p>
     *
     * @param addressString The address string.
     *
     * @return The SocketAddress (empty when not available).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<InetSocketAddress> socketAddress(
            @Nonnull final String addressString)
    {
        final Matcher matcher = _ADDRESS_PATTERN.matcher(addressString);
        InetSocketAddress socketAddress = null;

        if (matcher.matches()) {
            final String address = matcher.group(1);
            final int port = (matcher
                .group(2) != null)? Integer.parseInt(matcher.group(2)): 0;

            socketAddress = new InetSocketAddress(address, port);

            if (socketAddress.isUnresolved()) {
                socketAddress = null;
            }
        }

        return Optional.ofNullable(socketAddress);
    }

    /**
     * Returns the loopback address if the host address is on localhost.
     *
     * @param address The original address.
     *
     * @return The original address or the loopback address.
     */
    @Nonnull
    @CheckReturnValue
    public static InetAddress substituteAddress(@Nonnull InetAddress address)
    {
        if (!address.isLoopbackAddress() && isOnLocalHost(address)) {
            address = Require.notNull(InetAddress.getLoopbackAddress());
        }

        return address;
    }

    /**
     * Returns a loopback URI if the host address is on localhost.
     *
     * @param uri The original URI.
     *
     * @return The original URI, a loopback URI or empty if the host is unknown.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<URI> substituteURI(@Nonnull final URI uri)
    {
        try {
            final InetAddress address = InetAddress.getByName(uri.getHost());

            if (!address.isLoopbackAddress() && isOnLocalHost(address)) {
                return Optional
                    .of(
                        new URI(
                            uri.getScheme(),
                            uri.getUserInfo(),
                            InetAddress.getLoopbackAddress().getHostAddress(),
                            uri.getPort(),
                            uri.getPath(),
                            uri.getQuery(),
                            uri.getFragment()));
            }
        } catch (final UnknownHostException exception) {
            return Optional.empty();
        } catch (final URISyntaxException exception) {
            throw new RuntimeException(exception);    // Should not happen.
        }

        return Optional.of(uri);
    }

    /** Local host. */
    public static final String LOCAL_HOST = "127.0.0.1";

    /**  */

    private static final Pattern _ADDRESS_PATTERN = Pattern
        .compile("(?://)?(.*?)(?::([0-9]++)/?)?");
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
