/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DataloggerServiceTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.service.pap.datalogger.modbus;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.config.Config;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.modbus.ModbusServer;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.pap.datalogger.DataloggerServiceActivator;
import org.rvpf.tests.pap.modbus.ModbusTestsSupport;
import org.rvpf.tests.service.MetadataServiceTests;
import org.rvpf.tests.som.SOMSupport;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Datalogger service tests.
 */
public final class DataloggerServiceTests
    extends MetadataServiceTests
    implements PAPProxy.Responder
{
    /** {@inheritDoc}
     */
    @Override
    public PointValue[] select(final Point[] points)
    {
        final PointValue[] pointValues = new PointValue[points.length];

        for (int i = 0; i < pointValues.length; ++i) {
            pointValues[i] = _pointValues.get(points[i]);
        }

        return pointValues;
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
        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);

        DateTime.simulateTime(DateTime.now().midnight());

        setProperty(
            ModbusTestsSupport.LISTEN_PORT_PROPERTY,
            String.valueOf(allocateTCPPort()));

        setUpAlerter();

        _somSinkReceiver = getMessaging()
            .createServerReceiver(_SINK_QUEUE_NAME);

        _modbusSupport = new ModbusTestsSupport(getMetadata(true));

        final Point registerPoint = getMetadata()
            .getPoint(_TESTS_REGISTER_1)
            .get();

        _pointValues
            .put(
                registerPoint,
                new PointValue(
                    registerPoint,
                    Optional.empty(),
                    null,
                    Integer.valueOf(12345)));

        final Point splittedPoint1 = getMetadata()
            .getPoint(_TESTS_SPLITTED_1)
            .get();
        final Tuple registers = new Tuple();

        registers.add(Short.valueOf((short) -1));
        registers.add(Short.valueOf((short) 0x5678));
        registers.add(Short.valueOf((short) 0x1234));
        registers.add(Short.valueOf((short) 0b10));
        registers.add(Short.valueOf((short) Float.floatToIntBits(1.2345F)));
        registers
            .add(Short.valueOf((short) (Float.floatToIntBits(1.2345F) >>> 16)));
        _pointValues
            .put(
                splittedPoint1,
                new PointValue(
                    splittedPoint1,
                    Optional.empty(),
                    null,
                    registers));

        final Point splittedPoint2 = getMetadata()
            .getPoint(_TESTS_SPLITTED_2)
            .get();

        _pointValues
            .put(
                splittedPoint2,
                new PointValue(
                    splittedPoint2,
                    Optional.empty(),
                    null,
                    Short.valueOf((short) 456)));

        final Point splittedPoint3 = getMetadata()
            .getPoint(_TESTS_SPLITTED_3)
            .get();

        _pointValues
            .put(
                splittedPoint3,
                new PointValue(
                    splittedPoint3,
                    Optional.empty(),
                    null,
                    Short.valueOf((short) 789)));

        final Point splittedPoint4 = getMetadata()
            .getPoint(_TESTS_SPLITTED_4)
            .get();
        final Tuple bits = new Tuple();

        bits.add(Boolean.TRUE);
        bits.add(Boolean.FALSE);
        bits.add(Boolean.TRUE);
        bits.add(Boolean.FALSE);
        bits.add(Boolean.TRUE);
        bits.add(Boolean.FALSE);

        _pointValues
            .put(
                splittedPoint4,
                new PointValue(splittedPoint4, Optional.empty(), null, bits));

        final ModbusServer server = _modbusSupport.getServer();

        server.setResponder(this);
        server.start();
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
        _modbusSupport.getServer().stop();

        _somSinkReceiver.close();

        tearDownAlerter();

        DateTime.clearSimulatedTime();

        restoreSystemProperties();
    }

    /**
     * Tests with output to a queue.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testToQueue()
        throws Exception
    {
        _somReceiver = getMessaging()
            .createServerReceiver(
                getConfig().getPropertiesGroup(_FORWARDER_QUEUE_PROPERTIES));
        setProperty(_DATALOGGER_QUEUE_PROPERTY, _FORWARDER_QUEUE_PROPERTIES);

        try {
            _test();
        } finally {
            _somReceiver.close();
            _somReceiver = null;
            clearProperty(_DATALOGGER_QUEUE_PROPERTY);
        }
    }

    /**
     * Tests with output to a sink.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testToSink()
        throws Exception
    {
        _somReceiver = _somSinkReceiver;

        try {
            _test();
        } finally {
            _somReceiver = null;
        }
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

        Require.success(pointValue.getStamp().isNotBefore(_startTime));
        Require.success(pointValue.getState() == null);
        Require.equal(pointValue.getPointUUID(), point.getUUID().get());
        Require
            .equal(
                receivedValue,
                expectedValue,
                pointName + ": " + receivedValueClass);
    }

    private void _test()
        throws Exception
    {
        _startTime = DateTime.now();

        final ServiceActivator dataloggerService = startService(
            DataloggerServiceActivator.class,
            Optional.of(_MODBUS_NAME));

        try {
            _receive(_TESTS_STATE_1, null);
            _receive(_TESTS_STATE_1, Boolean.TRUE);
            _receive(_TESTS_SCANNED_1, Long.valueOf(12345));
            _receive(_TESTS_SPLIT_1_1, Long.valueOf(65535));
            _receive(_TESTS_SPLIT_1_2, Long.valueOf(0x12345678));
            _receive(_TESTS_SPLIT_1_3, Boolean.FALSE);
            _receive(_TESTS_SPLIT_1_4, Boolean.TRUE);
            _receive(_TESTS_SPLIT_1_5, Double.valueOf(1.2345F));
            _receive(_TESTS_SPLIT_2_1, Long.valueOf(456));
            _receive(_TESTS_SPLIT_2_2, Double.valueOf(456));
            _receive(_TESTS_SPLIT_3_1, Long.valueOf(789));
            _receive(_TESTS_SPLIT_4_1, Boolean.TRUE);
            _receive(_TESTS_SPLIT_4_2, Boolean.FALSE);

            _somReceiver.commit();
        } finally {
            stopService(dataloggerService);
        }

        _receive(_TESTS_STATE_1, Boolean.FALSE);
    }

    private static final String _DATALOGGER_QUEUE_PROPERTY =
        "tests.datalogger.queue";
    private static final String _FORWARDER_QUEUE_PROPERTIES =
        "tests.forwarder.queue";
    private static final String _MODBUS_NAME = "Modbus";
    private static final String _SINK_QUEUE_NAME = "TestsSink";
    private static final String _TESTS_PROPERTIES =
        "rvpf-modbus-datalogger.properties";
    private static final String _TESTS_REGISTER_1 = "TESTS-MODBUS.REGISTER.1";
    private static final String _TESTS_SCANNED_1 = "TESTS-MODBUS.SCANNED.1";
    private static final String _TESTS_SPLITTED_1 = "TESTS-MODBUS.SPLITTED.1";
    private static final String _TESTS_SPLITTED_2 = "TESTS-MODBUS.SPLITTED.2";
    private static final String _TESTS_SPLITTED_3 = "TESTS-MODBUS.SPLITTED.3";
    private static final String _TESTS_SPLITTED_4 = "TESTS-MODBUS.SPLITTED.4";
    private static final String _TESTS_SPLIT_1_1 = "TESTS-MODBUS.SPLIT.1.1";
    private static final String _TESTS_SPLIT_1_2 = "TESTS-MODBUS.SPLIT.1.2";
    private static final String _TESTS_SPLIT_1_3 = "TESTS-MODBUS.SPLIT.1.3";
    private static final String _TESTS_SPLIT_1_4 = "TESTS-MODBUS.SPLIT.1.4";
    private static final String _TESTS_SPLIT_1_5 = "TESTS-MODBUS.SPLIT.1.5";
    private static final String _TESTS_SPLIT_2_1 = "TESTS-MODBUS.SPLIT.2.1";
    private static final String _TESTS_SPLIT_2_2 = "TESTS-MODBUS.SPLIT.2.2";
    private static final String _TESTS_SPLIT_3_1 = "TESTS-MODBUS.SPLIT.3.1";
    private static final String _TESTS_SPLIT_4_1 = "TESTS-MODBUS.SPLIT.4.1";
    private static final String _TESTS_SPLIT_4_2 = "TESTS-MODBUS.SPLIT.4.2";
    private static final String _TESTS_STATE_1 = "TESTS-MODBUS.STATE.1";

    private ModbusTestsSupport _modbusSupport;
    private final Map<Point, PointValue> _pointValues = new HashMap<Point,
        PointValue>();
    private SOMSupport.Receiver _somReceiver;
    private SOMSupport.Receiver _somSinkReceiver;
    private DateTime _startTime;
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
