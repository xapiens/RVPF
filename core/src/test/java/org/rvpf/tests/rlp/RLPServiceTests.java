/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RLPServiceTests.java 4100 2019-06-29 17:22:34Z SFB $
 */

package org.rvpf.tests.rlp;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.rlp.Protocol;
import org.rvpf.base.util.rlp.RLPClient;
import org.rvpf.base.util.rlp.ResourceProvider;
import org.rvpf.base.util.rlp.ResourceSpecifier;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.rlp.RLPServiceActivator;
import org.rvpf.tests.service.ServiceTests;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Resource Location Protocol service tests.
 */
public final class RLPServiceTests
    extends ServiceTests
{
    /**
     * Constructs an instance.
     */
    public RLPServiceTests()
    {
        _resourceSpecifiers = new ResourceSpecifier[4];

        _resourceSpecifiers[0] = ResourceSpecifier
            .newBuilder()
            .setProtocol(Protocol.UDP)
            .setIdentifier("Test 1")
            .build();
        _resourceSpecifiers[1] = ResourceSpecifier
            .newBuilder()
            .setProtocol(Protocol.TCP)
            .setIdentifier("Test 1")
            .build();
        _resourceSpecifiers[2] = ResourceSpecifier
            .newBuilder()
            .setProtocol(Protocol.UDP)
            .setIdentifier("Test 2")
            .build();
        _resourceSpecifiers[3] = ResourceSpecifier
            .newBuilder()
            .setProtocol(Protocol.UDP)
            .setIdentifier("Test 3")
            .build();
    }

    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUp()
        throws Exception
    {
        setUpAlerter();

        _serverPort = allocateUDPPort();
        setProperty(
            TESTS_SERVER_UDP_PORT_PROPERTY,
            String.valueOf(_serverPort));

        _service = createService(RLPServiceActivator.class, Optional.empty());
        _service.start(true);
    }

    /**
     * Tears down what has been set up.
     *
     * @throws Exception On failure.
     */
    @AfterClass(alwaysRun = true)
    public void tearDown()
        throws Exception
    {
        if (_service != null) {
            stopService(_service);
            _service = null;
        }

        tearDownAlerter();
    }

    /**
     * Tests broadcast 'Who-Provides?'.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testBroadcastWhoProvides()
        throws Exception
    {
        if (System.getProperty("os.name").toLowerCase().startsWith("mac os")) {
            throw new SkipException(
                "Not supported on " + System.getProperty("os.name"));
        }

        final RLPClient client = RLPClient.newBuilder().build();
        final ResourceProvider provider = ResourceProvider
            .newBuilder()
            .setAddress(
                Optional.of(InetAddress.getByName(LAN_BROADCAST_ADDRESS)))
            .setPort(_serverPort)
            .build();
        final ResourceProvider[] providers = client
            .whoProvides(
                provider,
                _resourceSpecifiers,
                false,
                DEFAULT_TIMEOUT.get(),
                ElapsedTime.EMPTY)
            .get();

        _validateResources(providers[0].getResourceSpecifiers());
    }

    /**
     * Tests 'Do-You-Provide?'.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testDoYouProvide()
        throws Exception
    {
        final RLPClient client = RLPClient.newBuilder().build();
        final ResourceProvider provider = ResourceProvider
            .newBuilder()
            .setPort(_serverPort)
            .build();
        final ResourceSpecifier[] provided = client
            .doYouProvide(
                provider,
                _resourceSpecifiers,
                true,
                DEFAULT_TIMEOUT.get())
            .get();

        _validateResources(provided);
    }

    /**
     * Tests subnet 'Who-Provides?'.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testSubnetWhoProvides()
        throws Exception
    {
        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
            .getNetworkInterfaces();
        final List<InterfaceAddress> interfaceAdresses = new LinkedList<>();

        while (networkInterfaces.hasMoreElements()) {
            final NetworkInterface networkInterface = networkInterfaces
                .nextElement();

            if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                continue;
            }

            interfaceAdresses.addAll(networkInterface.getInterfaceAddresses());
        }

        for (final InterfaceAddress interfaceAddress: interfaceAdresses) {
            final InetAddress broadcastAddress = interfaceAddress
                .getBroadcast();

            if (broadcastAddress == null) {
                continue;
            }

            final RLPClient client = RLPClient
                .newBuilder()
                .setLocalAddress(Optional.of(interfaceAddress.getAddress()))
                .build();
            final ResourceProvider provider = ResourceProvider
                .newBuilder()
                .setAddress(Optional.of(broadcastAddress))
                .setPort(_serverPort)
                .build();
            final ResourceProvider[] providers = client
                .whoProvides(
                    provider,
                    _resourceSpecifiers,
                    true,
                    DEFAULT_TIMEOUT.get(),
                    ElapsedTime.EMPTY)
                .get();

            _validateResources(providers[0].getResourceSpecifiers());
        }
    }

    private void _validateResources(
            final ResourceSpecifier[] resourceSpecifiers)
    {
        Require.success(resourceSpecifiers.length == 2);
        Require.success(resourceSpecifiers[0].equals(_resourceSpecifiers[0]));
        Require.success(resourceSpecifiers[1].equals(_resourceSpecifiers[3]));
    }

    /** LAN broadcast address. */
    public static final String LAN_BROADCAST_ADDRESS = "255.255.255.255";

    /** Server UDP port property. */
    public static final String TESTS_SERVER_UDP_PORT_PROPERTY =
        "tests.rlp.server.udp.port";

    private final ResourceSpecifier[] _resourceSpecifiers;
    private int _serverPort;
    private ServiceActivator _service;
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
