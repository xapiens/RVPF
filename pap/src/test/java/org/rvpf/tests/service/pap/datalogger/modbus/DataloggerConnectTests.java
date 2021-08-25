/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DataloggerConnectTests.java 4095 2019-06-24 17:44:43Z SFB $
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
import org.rvpf.config.Config;
import org.rvpf.pap.PAPProxy;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.pap.datalogger.DataloggerServiceActivator;
import org.rvpf.tests.pap.modbus.ModbusTestsSupport;
import org.rvpf.tests.service.MetadataServiceTests;
import org.rvpf.tests.som.SOMSupport;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Datalogger connect tests.
 */
public class DataloggerConnectTests
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
        setProperty(_TESTS_CONNECT_PROPERTY, "");

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

        _modbusSupport.getServer().setResponder(this);
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
        _somSinkReceiver.close();

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
        _startTime = DateTime.now();

        _modbusSupport.getServer().start();

        final ServiceActivator dataloggerService = startService(
            DataloggerServiceActivator.class,
            Optional.of(_MODBUS_NAME));

        try {
            _receive(_TESTS_STATE_1, null);
            _receive(_TESTS_STATE_1, Boolean.TRUE);
            _receive(_TESTS_SCANNED_1, Long.valueOf(12345));

            _somSinkReceiver.commit();

            _modbusSupport.getServer().stop();
            _receive(_TESTS_STATE_1, Boolean.FALSE);
        } finally {
            stopService(dataloggerService);
        }
    }

    private void _receive(
            final String pointName,
            final Serializable expectedValue)
        throws Exception
    {
        final Point point = getMetadata().getPoint(pointName).get();
        final Serializable serializable = _somSinkReceiver
            .receive(getTimeout());

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

    private static final String _MODBUS_NAME = "Modbus";
    private static final String _SINK_QUEUE_NAME = "TestsSink";
    private static final String _TESTS_CONNECT_PROPERTY =
        "tests.modbus.datalogger.connect";
    private static final String _TESTS_PROPERTIES =
        "rvpf-modbus-datalogger.properties";
    private static final String _TESTS_REGISTER_1 = "TESTS-MODBUS.REGISTER.1";
    private static final String _TESTS_SCANNED_1 = "TESTS-MODBUS.SCANNED.1";
    private static final String _TESTS_STATE_1 = "TESTS-MODBUS.STATE.1";

    private ModbusTestsSupport _modbusSupport;
    private final Map<Point, PointValue> _pointValues = new HashMap<Point,
        PointValue>();
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
