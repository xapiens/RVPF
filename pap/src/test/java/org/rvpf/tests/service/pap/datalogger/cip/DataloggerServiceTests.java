/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DataloggerServiceTests.java 4099 2019-06-26 21:33:35Z SFB $
 */

package org.rvpf.tests.service.pap.datalogger.cip;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.Attributes;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.cip.CIP;
import org.rvpf.pap.cip.CIPClient;
import org.rvpf.pap.cip.CIPSupport;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.pap.datalogger.DataloggerServiceActivator;
import org.rvpf.tests.pap.cip.CIPTestsSupport;
import org.rvpf.tests.service.MetadataServiceTests;
import org.rvpf.tests.som.SOMSupport;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Datalogger service tests.
 */
public class DataloggerServiceTests
    extends MetadataServiceTests
{
    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUp()
        throws Exception
    {
        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);
        setProperty(_TESTS_REPLICATED_PROPERTY, "1");

        DateTime.simulateTime(DateTime.now().midnight());

        setProperty(
            CIPTestsSupport.LISTEN_PORT_PROPERTY,
            String.valueOf(allocateTCPPort()));

        setUpAlerter();

        _somReceiver = getMessaging().createServerReceiver(_SINK_QUEUE_NAME);

        final Metadata metadata = getMetadata(true);

        final CIPSupport support = new CIPSupport();
        final CIPClient client = support
            .newClient(support.newClientContext(metadata, Optional.empty()));
        final Point registerPoint = metadata.getPoint(_TESTS_REGISTER_1).get();
        final PointValue registerPointValue = new PointValue(
            registerPoint,
            Optional.empty(),
            null,
            Integer.valueOf(12345));

        Require.present(client.requestPointUpdate(registerPointValue));

        final Point dintArrayPoint = metadata.getPoint(_TESTS_SPLITTED_1).get();
        final Attributes dintArrayAttributes = dintArrayPoint
            .getAttributes(CIP.ATTRIBUTES_USAGE)
            .get();
        final int dintArrayElements = dintArrayAttributes
            .getInt(CIP.ELEMENTS_ATTRIBUTE, 0);
        final Tuple dintArray = new Tuple(dintArrayElements);

        for (int index = 0; index < dintArrayElements; ++index) {
            dintArray.add(Integer.valueOf(index));
        }

        final PointValue dintArrayPointValue = new PointValue(
            dintArrayPoint,
            Optional.empty(),
            null,
            dintArray);

        Require.present(client.requestPointUpdate(dintArrayPointValue));

        final Point boolArrayPoint = metadata.getPoint(_TESTS_SPLITTED_2).get();
        final Attributes boolArrayAttributes = boolArrayPoint
            .getAttributes(CIP.ATTRIBUTES_USAGE)
            .get();
        final int boolArrayElements = boolArrayAttributes
            .getInt(CIP.ELEMENTS_ATTRIBUTE, 0);
        final Tuple boolArray = new Tuple(boolArrayElements);

        for (int index = 0; index < boolArrayElements; ++index) {
            boolArray.add(Boolean.valueOf((index & 1) == 0));
        }

        final PointValue boolArrayPointValue = new PointValue(
            boolArrayPoint,
            Optional.empty(),
            null,
            boolArray);

        Require.present(client.requestPointUpdate(boolArrayPointValue));

        final Point realArrayPoint = metadata.getPoint(_TESTS_SPLITTED_3).get();
        final Attributes realArrayAttributes = realArrayPoint
            .getAttributes(CIP.ATTRIBUTES_USAGE)
            .get();
        final int realArrayElements = realArrayAttributes
            .getInt(CIP.ELEMENTS_ATTRIBUTE, 0);
        final Tuple realArray = new Tuple(realArrayElements);

        for (int index = 0; index < realArrayElements; ++index) {
            realArray.add(Float.valueOf((float) (index + 0.5)));
        }

        final PointValue realArrayPointValue = new PointValue(
            realArrayPoint,
            Optional.empty(),
            null,
            realArray);

        Require.present(client.requestPointUpdate(realArrayPointValue));
        Require.notNull(client.commitPointUpdateRequests());

        client.close();
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
        _somReceiver.close();

        tearDownAlerter();

        DateTime.clearSimulatedTime();

        restoreSystemProperties();
    }

    /**
     * Tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void test()
        throws Exception
    {
        final ServiceActivator dataloggerService = startService(
            DataloggerServiceActivator.class,
            Optional.of(_CIP_NAME));

        try {
            _receive(_TESTS_STATE_1, null);
            _receive(_TESTS_STATE_1, Boolean.TRUE);
            _receive(_TESTS_SCANNED_1, Long.valueOf(12345));
            _receive(_TESTS_SPLIT_0_1, Boolean.FALSE);
            _receive(_TESTS_SPLIT_0_2, Boolean.TRUE);
            _receive(_TESTS_SPLIT_1_1, Long.valueOf(1));
            _receive(_TESTS_SPLIT_1_2, Long.valueOf(2));
            _receive(_TESTS_SPLIT_1_3, Boolean.TRUE);
            _receive(_TESTS_SPLIT_2_1, Boolean.FALSE);
            _receive(_TESTS_SPLIT_2_2, Boolean.TRUE);
            _receive(_TESTS_SPLIT_3_1, Double.valueOf(1.5));
            _receive(_TESTS_SPLIT_3_2, Double.valueOf(2.5));

            _somReceiver.commit();
        } finally {
            stopService(dataloggerService);
        }

        _receive(_TESTS_STATE_1, Boolean.FALSE);
    }

    private void _receive(
            final String pointName,
            final Serializable expectedValue)
        throws Exception
    {
        final Point point = getMetadata().getPoint(pointName).get();
        final Serializable serializable = _somReceiver.receive(getTimeout());

        Require.notNull(serializable, "Expected: " + pointName);

        final PointValue pointValue = (PointValue) serializable;
        final Serializable receivedValue = pointValue.getValue();
        final String receivedValueClass = (receivedValue != null)? receivedValue
            .getClass()
            .getSimpleName(): "null";

        Require.equal(pointValue.getStamp(), DateTime.now());
        Require.equal(null, pointValue.getState());
        Require.equal(pointValue.getPointUUID(), point.getUUID().get());
        Require
            .equal(
                receivedValue,
                expectedValue,
                pointName + ": " + receivedValueClass);
    }

    private static final String _CIP_NAME = "CIP";
    private static final String _SINK_QUEUE_NAME = "TestsSink";
    private static final String _TESTS_PROPERTIES =
        "rvpf-cip-datalogger.properties";
    private static final String _TESTS_REGISTER_1 = "TESTS-CIP.REGISTER.1";
    private static final String _TESTS_REPLICATED_PROPERTY = "tests.replicated";
    private static final String _TESTS_SCANNED_1 = "TESTS-CIP.SCANNED.1";
    private static final String _TESTS_SPLITTED_1 = "TESTS-CIP.SPLITTED.1";
    private static final String _TESTS_SPLITTED_2 = "TESTS-CIP.SPLITTED.2";
    private static final String _TESTS_SPLITTED_3 = "TESTS-CIP.SPLITTED.3";
    private static final String _TESTS_SPLIT_0_1 = "TESTS-CIP.SPLIT.0.1";
    private static final String _TESTS_SPLIT_0_2 = "TESTS-CIP.SPLIT.0.2";
    private static final String _TESTS_SPLIT_1_1 = "TESTS-CIP.SPLIT.1.1";
    private static final String _TESTS_SPLIT_1_2 = "TESTS-CIP.SPLIT.1.2";
    private static final String _TESTS_SPLIT_1_3 = "TESTS-CIP.SPLIT.1.3";
    private static final String _TESTS_SPLIT_2_1 = "TESTS-CIP.SPLIT.2.1";
    private static final String _TESTS_SPLIT_2_2 = "TESTS-CIP.SPLIT.2.2";
    private static final String _TESTS_SPLIT_3_1 = "TESTS-CIP.SPLIT.3.1";
    private static final String _TESTS_SPLIT_3_2 = "TESTS-CIP.SPLIT.3.2";
    private static final String _TESTS_STATE_1 = "TESTS-CIP.STATE.1";

    private SOMSupport.Receiver _somReceiver;
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
