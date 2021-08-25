/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreServerTests.java 4099 2019-06-26 21:33:35Z SFB $
 */

package org.rvpf.tests.store.server.pap.modbus;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.pap.PAPStoreServiceActivator;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.pap.modbus.ModbusTestsSupport;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Store server tests.
 */
public final class StoreServerTests
    extends StoreClientTests
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

        setProperty(
            ModbusTestsSupport.LISTEN_PORT_PROPERTY,
            String.valueOf(allocateTCPPort()));

        setUpAlerter();

        _target = new StoreTarget(getMetadata(true));
        _target.start();

        _storeService = createService(
            PAPStoreServiceActivator.class,
            Optional.of(_SERVICE_NAME));
        startService(_storeService);

        _storeProxy = getStoreProxy(_storeService);
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
        _target.stop();

        _storeProxy.tearDown();

        stopService(_storeService);
        _storeService = null;

        tearDownAlerter();
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
        final Metadata metadata = getMetadata();
        final MessagingSupport.Receiver replicateReceiver = getMessaging()
            .createClientReceiver(
                metadata.getPropertiesGroup(REPLICATE_QUEUE_PROPERTIES));
        final MessagingSupport.Receiver replicatedReceiver = getMessaging()
            .createClientReceiver(
                metadata.getPropertiesGroup(REPLICATED_QUEUE_PROPERTIES));

        replicateReceiver.purge();
        replicatedReceiver.purge();

        _sendPointValue(_POINT_COIL_1, Boolean.TRUE);
        Require.equal(_getPointValue(_POINT_COIL_1), Boolean.TRUE);
        Require.equal(_receivePointValue(_POINT_COIL_1), Boolean.TRUE);

        _putPointValue(_POINT_DISCRETE_1, Boolean.TRUE);
        Require.equal(_receivePointValue(_POINT_DISCRETE_1), Boolean.TRUE);

        _putPointValue(_POINT_INPUT_1, Integer.valueOf(1234));
        Require
            .equal(_receivePointValue(_POINT_INPUT_1), Integer.valueOf(1234));

        _sendPointValue(_POINT_REGISTER_1, Integer.valueOf(4321));
        Require.equal(_getPointValue(_POINT_REGISTER_1), Integer.valueOf(4321));
        Require
            .equal(
                _receivePointValue(_POINT_REGISTER_1),
                Integer.valueOf(4321));

        _sendPointValue(_POINT_REGISTER_2, Integer.valueOf(8765));
        Require.equal(_getPointValue(_POINT_REGISTER_2), Integer.valueOf(8765));
        Require
            .equal(
                _receivePointValue(_POINT_REGISTER_2),
                Integer.valueOf(8765));

        final long timeout = getTimeout();
        final Point replicatePoint = metadata
            .getPointByName(_REPLICATE_POINT_NAME)
            .get();
        final PointValue replicateValue = (PointValue) replicateReceiver
            .receive(timeout);

        Require
            .equal(
                replicateValue.getPointUUID(),
                replicatePoint.getUUID().get());
        Require.equal(replicateValue.getValue(), Double.valueOf(4321.0));

        final Point replicatedPoint = metadata
            .getPointByName(_POINT_REGISTER_2)
            .get();
        final PointValue replicatedValue = (PointValue) replicatedReceiver
            .receive(timeout);

        Require
            .equal(
                replicatedValue.getPointUUID(),
                replicatedPoint.getUUID().get());
        Require.equal(replicatedValue.getValue(), Long.valueOf(8765));

        replicateReceiver.purge();
        replicateReceiver.close();

        replicatedReceiver.purge();
        replicatedReceiver.close();
    }

    private Serializable _getPointValue(final String pointKey)
        throws Exception
    {
        final Point point = getPoint(pointKey);
        final PointValue pointValue;
        final Serializable value;

        Require.notNull(point);
        pointValue = _target.getPointValue(point);
        value = pointValue.getValue();
        Require.notNull(value);

        return value;
    }

    private void _putPointValue(final String pointKey, final Serializable value)
    {
        final Point point = getPoint(pointKey);

        Require.notNull(point);
        _target
            .putPointValue(
                new PointValue(
                    point,
                    Optional.of(DateTime.now()),
                    null,
                    value));
    }

    private Serializable _receivePointValue(
            final String pointKey)
        throws Exception
    {
        final Point point = getPoint(pointKey);

        Require.notNull(point);

        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPoint(point);
        final StoreValues storeValues = _storeProxy
            .select(storeQueryBuilder.build())
            .get();
        final PointValue pointValue = storeValues.getPointValue().get();

        Require.equal(pointValue.getPointUUID(), point.getUUID().get());
        Require.equal(null, pointValue.getState());

        final Serializable value = pointValue.getValue();

        Require.notNull(value);

        return value;
    }

    private void _sendPointValue(
            final String pointKey,
            final Serializable value)
        throws Exception
    {
        final Point point = getPoint(pointKey);
        final PointValue pointValue = new PointValue(
            point,
            Optional.of(DateTime.now()),
            null,
            value);

        Require
            .success(_storeProxy.updateAndCheck(pointValue, getThisLogger()));
    }

    private static final String _POINT_COIL_1 = "TESTS-MODBUS.COIL.1";
    private static final String _POINT_DISCRETE_1 = "TESTS-MODBUS.DISCRETE.1";
    private static final String _POINT_INPUT_1 = "TESTS-MODBUS.INPUT.1";
    private static final String _POINT_REGISTER_1 = "TESTS-MODBUS.REGISTER.1";
    private static final String _POINT_REGISTER_2 = "TESTS-MODBUS.REGISTER.2";
    private static final String _REPLICATE_POINT_NAME = "TESTS.REPLICATE.01";
    private static final String _SERVICE_NAME = "Modbus";
    private static final String _TESTS_PROPERTIES = "rvpf-modbus.properties";

    private StoreSessionProxy _storeProxy;
    private ServiceActivator _storeService;
    private StoreTarget _target;
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
