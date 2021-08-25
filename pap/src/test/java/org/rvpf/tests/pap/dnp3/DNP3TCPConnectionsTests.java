/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3TCPConnectionsTests.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.tests.pap.dnp3;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.rvpf.pap.dnp3.transport.Connection;

import org.testng.annotations.Test;

/**
 * DNP3 TCP connections tests.
 */
public final class DNP3TCPConnectionsTests
    extends DNP3ConnectionsTests
{
    /**
     * Constructs an instance.
     *
     * @throws Exception On failure.
     */
    public DNP3TCPConnectionsTests()
        throws Exception
    {
        _outstationSocketChannel = ServerSocketChannel.open();
        _outstationSocketChannel.bind(_tcpSocketAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    @Test
    public void test()
        throws Exception
    {
        super.test();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Connection newMasterConnection()
        throws Exception
    {
        return getSupport()
            .newTCPConnection(true, SocketChannel.open(_tcpSocketAddress));
    }

    /** {@inheritDoc}
     */
    @Override
    protected Connection newOutstationConnection()
        throws Exception
    {
        final Connection serverConnection = getSupport()
            .newTCPConnection(false, _outstationSocketChannel.accept());

        _outstationSocketChannel.close();

        return serverConnection;
    }

    private final ServerSocketChannel _outstationSocketChannel;
    private final InetSocketAddress _tcpSocketAddress = new InetSocketAddress(
        InetAddress.getLoopbackAddress(),
        allocateTCPPort());
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
