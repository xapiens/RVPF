/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreServerTests.java 4099 2019-06-26 21:33:35Z SFB $
 */

package org.rvpf.tests.store.server.pap.cip;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.Attributes;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.cip.CIP;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.pap.PAPStoreServiceActivator;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.pap.cip.CIPTestsSupport;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Store server tests.
 */
public class StoreServerTests
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
        setProperty(_TESTS_REPLICATED_PROPERTY, "1");

        setProperty(
            CIPTestsSupport.LISTEN_PORT_PROPERTY,
            String.valueOf(allocateTCPPort()));

        setUpAlerter();

        Require.notNull(getMetadata(true));

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

        _sendPointValue(_TESTS_BOOL_1_POINT, Boolean.TRUE);
        Require.equal(_receivePointValue(_TESTS_BOOL_1_POINT), Boolean.TRUE);

        final long timeout = getTimeout();
        final Point replicatedPoint = metadata
            .getPointByName(_TESTS_BOOL_1_POINT)
            .get();
        final PointValue replicatedValue = (PointValue) replicatedReceiver
            .receive(timeout);

        Require.notNull(replicatedValue);
        Require
            .equal(
                replicatedValue.getPointUUID(),
                replicatedPoint.getUUID().get());
        Require.equal(replicatedValue.getValue(), Boolean.TRUE);

        _sendPointValue(_TESTS_SINT_1_POINT, Byte.valueOf((byte) 43));
        Require
            .equal(
                _receivePointValue(_TESTS_SINT_1_POINT),
                Byte.valueOf((byte) 43));

        _sendPointValue(_TESTS_INT_1_POINT, Short.valueOf((short) 432));
        Require
            .equal(
                _receivePointValue(_TESTS_INT_1_POINT),
                Short.valueOf((short) 432));

        _sendPointValue(_TESTS_DINT_1_POINT, Integer.valueOf(4321));
        Require
            .equal(
                _receivePointValue(_TESTS_DINT_1_POINT),
                Integer.valueOf(4321));

        final Point replicatePoint = metadata
            .getPointByName(_TESTS_REPLICATE_POINT)
            .get();
        final PointValue replicateValue = (PointValue) replicateReceiver
            .receive(timeout);

        Require.notNull(replicateValue);
        Require
            .equal(
                replicateValue.getPointUUID(),
                replicatePoint.getUUID().get());
        Require.equal(replicateValue.getValue(), Double.valueOf(4321.0));

        final Point arrayPoint = metadata
            .getPoint(_TESTS_DINT_ARRAY_1_POINT)
            .get();
        final Attributes attributes = arrayPoint
            .getAttributes(CIP.ATTRIBUTES_USAGE)
            .get();
        final int elements = attributes.getInt(CIP.ELEMENTS_ATTRIBUTE, 0);
        final Tuple array = new Tuple(elements);

        for (int index = 0; index < elements; ++index) {
            array.add(Integer.valueOf(index));
        }

        _sendPointValue(_TESTS_DINT_ARRAY_1_POINT, array);
        Require.equal(_receivePointValue(_TESTS_DINT_ARRAY_1_POINT), array);

        _sendPointValue(_TESTS_REAL_1_POINT, Float.valueOf((float) 43.21));
        Require
            .equal(
                _receivePointValue(_TESTS_REAL_1_POINT),
                Float.valueOf((float) 43.21));

        replicateReceiver.purge();
        replicateReceiver.close();

        replicatedReceiver.purge();
        replicatedReceiver.close();
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

        return Require.notNull(pointValue.getValue());
    }

    private void _sendPointValue(
            final String pointKey,
            final Serializable value)
        throws Exception
    {
        final PointValue pointValue = new PointValue(
            getPoint(pointKey),
            Optional.of(DateTime.now()),
            null,
            value);

        Require
            .success(_storeProxy.updateAndCheck(pointValue, getThisLogger()));
    }

    private static final String _SERVICE_NAME = "CIP";
    private static final String _TESTS_BOOL_1_POINT = "TESTS-CIP.BOOL.1";
    private static final String _TESTS_DINT_1_POINT = "TESTS-CIP.DINT.1";
    private static final String _TESTS_DINT_ARRAY_1_POINT =
        "TESTS-CIP.DINT.ARRAY.1";
    private static final String _TESTS_INT_1_POINT = "TESTS-CIP.INT.1";
    private static final String _TESTS_PROPERTIES = "rvpf-cip.properties";
    private static final String _TESTS_REAL_1_POINT = "TESTS-CIP.REAL.1";
    private static final String _TESTS_REPLICATED_PROPERTY = "tests.replicated";
    private static final String _TESTS_REPLICATE_POINT = "TESTS.REPLICATE.01";
    private static final String _TESTS_SINT_1_POINT = "TESTS-CIP.SINT.1";

    private StoreSessionProxy _storeProxy;
    private ServiceActivator _storeService;
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
