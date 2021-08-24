/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RLPServiceAppImpl.java 4059 2019-06-05 20:44:44Z SFB $
 */

package org.rvpf.service.rlp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import java.nio.charset.StandardCharsets;

import java.util.Locale;
import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.rlp.Protocol;
import org.rvpf.base.util.rlp.RLPMessages;
import org.rvpf.base.util.rlp.RLPServer;
import org.rvpf.base.util.rlp.ResourceSpecifier;
import org.rvpf.service.Service;
import org.rvpf.service.app.ServiceAppImpl;

/**
 * RLP service application implementation.
 */
public final class RLPServiceAppImpl
    extends ServiceAppImpl
{
    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Service service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        final KeyedGroups rlpServerProperties = getConfigProperties()
            .getGroup(RLP_SERVER_PROPERTIES);
        final Optional<String> addressString = rlpServerProperties
            .getString(ADDRESS_PROPERTY);
        final Optional<InetSocketAddress> socketAddress;
        final InetAddress address;
        int port;

        if (addressString.isPresent()) {
            socketAddress = Inet.socketAddress(addressString.get());

            if (!socketAddress.isPresent()) {
                getThisLogger()
                    .error(BaseMessages.BAD_ADDRESS, addressString.get());

                return false;
            }

            address = socketAddress.get().getAddress();
            port = socketAddress.get().getPort();
        } else {
            address = null;
            port = 0;
        }

        if (port == 0) {
            port = rlpServerProperties.getInt(PORT_PROPERTY, 0);
        }

        final RLPServer.Builder serverBuilder = RLPServer
            .newBuilder()
            .setLocalAddress(Optional.ofNullable(address))
            .setLocalPort(port);

        for (final KeyedGroups resourceProperties:
                rlpServerProperties.getGroups(RESOURCE_PROPERTIES)) {
            final String protocolString = resourceProperties
                .getString(PROTOCOL_PROPERTY, Optional.of(""))
                .get()
                .toUpperCase(Locale.ENGLISH);
            final String identifierString = resourceProperties
                .getString(IDENTIFIER_PROPERTY, Optional.of(""))
                .get();
            int protocol;

            try {
                protocol = Integer.parseInt(protocolString);
            } catch (final NumberFormatException exception1) {
                try {
                    protocol = Protocol.valueOf(protocolString).code();
                } catch (final IllegalArgumentException exception2) {
                    getThisLogger()
                        .error(RLPMessages.UNKNOWN_PROTOCOL, protocolString);

                    return false;
                }
            }

            serverBuilder
                .addResourceSpecifier(
                    ResourceSpecifier
                        .newBuilder()
                        .setProtocol(protocol)
                        .setIdentifier(
                                identifierString
                                        .getBytes(StandardCharsets.UTF_8))
                        .build());
        }

        _server = serverBuilder.build();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        try {
            _server.start();
        } catch (final SocketException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        _server.stop(getJoinTimeout());
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_server != null) {
            stop();
        }

        super.tearDown();
    }

    /** Specifies on which address to listen. */
    public static final String ADDRESS_PROPERTY = "address";

    /** Resource identifier property. */
    public static final String IDENTIFIER_PROPERTY = "identifier";

    /** Specifies on which port to listen. */
    public static final String PORT_PROPERTY = "port";

    /** Resource protocol property. */
    public static final String PROTOCOL_PROPERTY = "protocol";

    /** Resource properties. */
    public static final String RESOURCE_PROPERTIES = "resource";

    /** RLP server properties. */
    public static final String RLP_SERVER_PROPERTIES = "rlp.server";

    private RLPServer _server;
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
