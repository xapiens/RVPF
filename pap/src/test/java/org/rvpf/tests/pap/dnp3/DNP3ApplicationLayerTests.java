/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3ApplicationLayerTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap.dnp3;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.dnp3.object.ObjectHeader;
import org.rvpf.pap.dnp3.object.ObjectHeader.PrefixCode;
import org.rvpf.pap.dnp3.object.ObjectHeader.RangeCode;
import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectRange;
import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.pap.dnp3.object.groupCategory.analogInputs.AnalogInputVariation;
import org.rvpf.pap.dnp3.transport.ApplicationLayer;
import org.rvpf.pap.dnp3.transport.Association;
import org.rvpf.pap.dnp3.transport.Connection;
import org.rvpf.pap.dnp3.transport.Fragment;
import org.rvpf.pap.dnp3.transport.FunctionCode;
import org.rvpf.service.ServiceThread;
import org.rvpf.tests.Tests;

import org.testng.annotations.Test;

/**
 * DNP3 application layer tests.
 */
public final class DNP3ApplicationLayerTests
    extends Tests
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @throws Exception On failure.
     */
    public DNP3ApplicationLayerTests()
        throws Exception
    {
        _outstationSocketChannel = ServerSocketChannel.open();
        _outstationSocketChannel.bind(_tcpSocketAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws Exception
    {
        final Fragment receivedFragment = _outstationApplicationLayer.receive();
        final Fragment.Header receivedFragmentHeader = receivedFragment
            .getHeader();

        Require
            .equal(receivedFragmentHeader.getFunctionCode(), FunctionCode.READ);
        Require.success(receivedFragmentHeader.isInRequest());
        Require.success(receivedFragmentHeader.isFirst());
        Require.success(receivedFragmentHeader.isLast());
        Require.failure(receivedFragmentHeader.isConfirmRequested());
        Require.failure(receivedFragmentHeader.isUnsolicited());

        final List<Fragment.Item> receivedItems = receivedFragment.getItems();

        Require.success(receivedItems.size() == 1);

        final ObjectHeader receivedObjectHeader = receivedItems
            .get(0)
            .getObjectHeader();

        Require.notNull(receivedObjectHeader);
        Require.equal(receivedObjectHeader.getPrefixCode(), PrefixCode.NONE);
        Require
            .equal(
                receivedObjectHeader.getRangeCode(),
                RangeCode.START_STOP_INDEX_BYTE);
        Require
            .success(
                Arrays
                    .equals(
                            (byte[]) receivedObjectHeader.getRange().get(),
                                    new byte[] {3, 3}));

        final ObjectVariation receivedVariation = receivedObjectHeader
            .getObjectVariation();

        Require
            .equal(receivedVariation, AnalogInputVariation.SHORT_WITHOUT_FLAG);

        final Optional<ObjectInstance[]> receivedInstances = receivedItems
            .get(0)
            .getObjectInstances();

        Require.failure(receivedInstances.isPresent());

        final Fragment.Header fragmentHeader = Fragment.Header
            .newInstance(Optional.of(FunctionCode.RESPONSE), false);
        final Fragment sentFragment = new Fragment(
            receivedFragment.getAssociation(),
            fragmentHeader);
        final ObjectHeader sentObjectHeader = ObjectHeader
            .newInstance(receivedObjectHeader);
        final ObjectInstance.WithValue sentObject =
            (ObjectInstance.WithValue) receivedObjectHeader
                .getObjectVariation()
                .newObjectInstance();

        sentObject.setValue(Integer.valueOf(1234));

        fragmentHeader.setFirst();
        Require
            .success(
                sentFragment
                    .add(
                            new Fragment.Item(
                                    sentObjectHeader,
                                            Optional.of(
                                                    new ObjectInstance[] {
                                                    sentObject, }))));
        fragmentHeader.setLast();
        sentFragment.send();
    }

    /**
     * Tests application layer.
     *
     * @throws Exception On failure.
     */
    @Test
    public void test()
        throws Exception
    {
        final DNP3TestsSupport support = new DNP3TestsSupport(Optional.empty());
        final Connection masterConnection = support
            .newTCPConnection(true, SocketChannel.open(_tcpSocketAddress));

        getThisLogger()
            .debug(DNP3TestsMessages.MASTER_CONNECTION, masterConnection);

        final Connection outstationConnection = support
            .newTCPConnection(false, _outstationSocketChannel.accept());

        getThisLogger()
            .debug(
                DNP3TestsMessages.OUTSTATION_CONNECTION,
                outstationConnection);

        outstationConnection.activate();
        masterConnection.activate();

        final Association outstationMasterAssociation = outstationConnection
            .getRemoteEndPoint()
            .getAssociation((short) 0, (short) 0);

        _outstationApplicationLayer = outstationMasterAssociation
            .getApplicationLayer();

        final Association masterOutstationAssociation = masterConnection
            .getRemoteEndPoint()
            .getAssociation((short) 0, (short) 0);
        final ApplicationLayer masterApplicationLayer =
            masterOutstationAssociation
                .getApplicationLayer();
        final ServiceThread serviceThread = new ServiceThread(
            this,
            "Transport tests");

        serviceThread.start();

        final Fragment.Header sentFragmentHeader = Fragment.Header
            .newInstance(Optional.of(FunctionCode.READ), true);
        final Fragment sentFragment = new Fragment(
            masterOutstationAssociation,
            sentFragmentHeader);
        final ObjectHeader sentObjectHeader = ObjectHeader
            .newInstance(
                AnalogInputVariation.SHORT_WITHOUT_FLAG,
                ObjectRange.newIndexInstance(3, 3));

        sentFragmentHeader.setFirst();
        Require
            .success(sentFragment.add(new Fragment.Item(sentObjectHeader)));
        sentFragmentHeader.setLast();
        sentFragment.send();

        final Fragment receivedFragment = masterApplicationLayer.receive();
        final Fragment.Header receivedFragmentHeader = receivedFragment
            .getHeader();

        Require
            .equal(
                receivedFragmentHeader.getFunctionCode(),
                FunctionCode.RESPONSE);
        Require.failure(receivedFragmentHeader.isInRequest());
        Require.success(receivedFragmentHeader.isFirst());
        Require.success(receivedFragmentHeader.isLast());
        Require.failure(receivedFragmentHeader.isConfirmRequested());
        Require.failure(receivedFragmentHeader.isUnsolicited());

        final List<Fragment.Item> receivedItems = receivedFragment.getItems();
        final ObjectHeader receivedObjectHeader = receivedItems
            .get(0)
            .getObjectHeader();

        Require.equal(receivedObjectHeader.getPrefixCode(), PrefixCode.NONE);
        Require
            .equal(
                receivedObjectHeader.getRangeCode(),
                RangeCode.START_STOP_INDEX_BYTE);
        Require
            .success(
                Arrays
                    .equals(
                            (byte[]) receivedObjectHeader.getRange().get(),
                                    new byte[] {3, 3}));

        final ObjectVariation receivedVariation = receivedObjectHeader
            .getObjectVariation();

        Require
            .equal(receivedVariation, AnalogInputVariation.SHORT_WITHOUT_FLAG);

        final ObjectInstance[] receivedInstances = receivedItems
            .get(0)
            .getObjectInstances()
            .get();

        Require.success(receivedInstances.length == 1);

        final ObjectInstance.WithValue receivedInstance =
            (ObjectInstance.WithValue) receivedInstances[0];

        Require
            .equal(
                receivedInstance.getObjectVariation(),
                AnalogInputVariation.SHORT_WITHOUT_FLAG);
        Require.success(receivedInstance.getObjectIndex() == 3);
        Require.equal(receivedInstance.getValue(), Short.valueOf((short) 1234));

        Require
            .ignored(
                serviceThread
                    .join(getThisLogger(), getTimeout(DEFAULT_TIMEOUT)));

        masterConnection.close();
        outstationConnection.close();
    }

    private ApplicationLayer _outstationApplicationLayer;
    private ServerSocketChannel _outstationSocketChannel;
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
