/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DataloggerServiceTests.java 4060 2019-06-06 13:49:41Z SFB $
 */

package org.rvpf.tests.service.pap.datalogger.dnp3;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.rvpf.base.Attributes;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.dnp3.DNP3;
import org.rvpf.pap.dnp3.DNP3Outstation;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.pap.datalogger.DataloggerServiceActivator;
import org.rvpf.tests.pap.dnp3.DNP3TestsSupport;
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
    implements PAPProxy.Responder
{
    /** {@inheritDoc}
     */
    @Override
    public PointValue[] select(
            final Point[] points)
        throws InterruptedException, ServiceNotAvailableException
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
            DNP3TestsSupport.TESTS_OUTSTATION_TCP_PORT_PROPERTY,
            String.valueOf(allocateTCPPort()));

        setUpAlerter();

        _somReceiver = getMessaging().createServerReceiver(_SINK_QUEUE_NAME);

        final Metadata metadata = getMetadata(true);

        _dnp3Support = new DNP3TestsSupport(Optional.of(metadata));

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

        final Point splittedPoint = metadata.getPoint(_TESTS_SPLITTED_1).get();
        final Attributes attributes = splittedPoint
            .getAttributes(DNP3.ATTRIBUTES_USAGE)
            .get();
        final int startIndex = attributes
            .getInt(DNP3.START_INDEX_ATTRIBUTE, -1);
        final int stopIndex = attributes.getInt(DNP3.STOP_INDEX_ATTRIBUTE, -1);
        final int elements = 1 + stopIndex - startIndex;
        final Tuple tuple = new Tuple(elements);

        for (int index = 0; index < elements; ++index) {
            tuple.add(Integer.valueOf(index));
        }

        _pointValues
            .put(
                splittedPoint,
                new PointValue(splittedPoint, Optional.empty(), null, tuple));

        final DNP3Outstation outstation = _dnp3Support.getOutstation();

        outstation.setResponder(this);
        outstation.start();
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
        _dnp3Support.getOutstation().stop();

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
        quell(PAPMessages.NO_POINTS_TO_SCAN);

        final ServiceActivator dataloggerService = startService(
            DataloggerServiceActivator.class,
            Optional.of(_DNP3_NAME));

        try {
            _receive(_TESTS_STATE_1, null);
            _receive(_TESTS_STATE_1, Boolean.TRUE);
            _receive(_TESTS_SCANNED_1, Long.valueOf(12345));
            _receive(_TESTS_SPLIT_1, Long.valueOf(1));
            _receive(_TESTS_SPLIT_2, Long.valueOf(2));

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

    private static final String _DNP3_NAME = "DNP3";
    private static final String _SINK_QUEUE_NAME = "TestsSink";
    private static final String _TESTS_PROPERTIES =
        "rvpf-dnp3-datalogger.properties";
    private static final String _TESTS_REGISTER_1 = "TESTS-DNP3.REGISTER.1";
    private static final String _TESTS_SCANNED_1 = "TESTS-DNP3.SCANNED.1";
    private static final String _TESTS_SPLITTED_1 = "TESTS-DNP3.SPLITTED.1";
    private static final String _TESTS_SPLIT_1 = "TESTS-DNP3.SPLIT.1";
    private static final String _TESTS_SPLIT_2 = "TESTS-DNP3.SPLIT.2";
    private static final String _TESTS_STATE_1 = "TESTS-DNP3.STATE.1";

    private DNP3TestsSupport _dnp3Support;
    private final Map<Point, PointValue> _pointValues = new HashMap<Point,
        PointValue>();
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
