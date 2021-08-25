/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SplitterEngineTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.processor.engine.pap.modbus;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.config.Config;
import org.rvpf.processor.ProcessorServiceActivator;
import org.rvpf.service.ServiceActivator;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Splitter engine tests.
 */
public final class SplitterEngineTests
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

        setUpAlerter();

        Require.notNull(getMetadata(true));

        _storeService = startStoreService(true);

        _storeProxy = getStoreProxy(_storeService);

        _processorService = createService(
            ProcessorServiceActivator.class,
            Optional.empty());
        startService(_processorService);
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
        if (_processorService != null) {
            stopService(_processorService);
            _processorService = null;
        }

        _storeProxy.disconnect();

        if (_storeService != null) {
            stopService(_storeService);
            _storeService = null;
        }

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
        final Point splitPoint1 = getPoint(_SPLIT_POINT_1);
        final Point splitPoint2 = getPoint(_SPLIT_POINT_2);
        final Point splitPoint3 = getPoint(_SPLIT_POINT_3);
        final Point splitPoint4 = getPoint(_SPLIT_POINT_4);
        final UUID[] subscribed = new UUID[] {splitPoint1.getUUID().get(),
                splitPoint2.getUUID().get(), splitPoint3.getUUID().get(),
                splitPoint4.getUUID().get(), };

        Require
            .success(
                _storeProxy.subscribeAndCheck(subscribed, getThisLogger()));

        final Point splittedPoint = getPoint(_SPLITTED_POINT);
        final Tuple registers = new Tuple();
        final PointValue splittedPointValue;

        registers.add(Short.valueOf((short) -1));
        registers.add(Short.valueOf((short) -1));
        registers.add(Short.valueOf((short) -1));
        registers.add(Short.valueOf((short) 0b10));

        splittedPointValue = new PointValue(
            splittedPoint,
            Optional.of(DateTime.now()),
            null,
            registers);
        Require
            .success(
                _storeProxy
                    .updateAndCheck(splittedPointValue, getThisLogger()));

        final StoreValues response = _storeProxy.deliver(100, getTimeout());

        Require.success(response.isSuccess());
        Require.success(response.size() == 4);

        final PointValue splitPointValue1 = response.getPointValue(0);

        Require
            .equal(splitPointValue1.getStamp(), splittedPointValue.getStamp());
        Require.equal(splitPointValue1.getValue(), Long.valueOf(65535));

        final PointValue splitPointValue2 = response.getPointValue(1);

        Require
            .equal(splitPointValue2.getStamp(), splittedPointValue.getStamp());
        Require.equal(splitPointValue2.getValue(), Long.valueOf(-1));

        final PointValue splitPointValue3 = response.getPointValue(2);

        Require
            .equal(splitPointValue3.getStamp(), splittedPointValue.getStamp());
        Require.equal(splitPointValue3.getValue(), Boolean.FALSE);

        final PointValue splitPointValue4 = response.getPointValue(3);

        Require
            .equal(splitPointValue4.getStamp(), splittedPointValue.getStamp());
        Require.equal(splitPointValue4.getValue(), Boolean.TRUE);

        Require
            .success(
                _storeProxy.unsubscribeAndCheck(subscribed, getThisLogger()));
    }

    private static final String _SPLITTED_POINT = "TESTS-MODBUS.SPLITTED.1";
    private static final String _SPLIT_POINT_1 = "TESTS-MODBUS.SPLIT.1";
    private static final String _SPLIT_POINT_2 = "TESTS-MODBUS.SPLIT.2";
    private static final String _SPLIT_POINT_3 = "TESTS-MODBUS.SPLIT.3";
    private static final String _SPLIT_POINT_4 = "TESTS-MODBUS.SPLIT.4";
    private static final String _TESTS_PROPERTIES = "rvpf-modbus.properties";

    private ServiceActivator _processorService;
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
