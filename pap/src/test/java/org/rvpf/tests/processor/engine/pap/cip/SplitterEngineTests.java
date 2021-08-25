/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SplitterEngineTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.processor.engine.pap.cip;

import java.util.Optional;

import org.rvpf.base.Attributes;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.cip.CIP;
import org.rvpf.pap.cip.CIPClient;
import org.rvpf.pap.cip.CIPSupport;
import org.rvpf.processor.ProcessorServiceActivator;
import org.rvpf.service.ServiceActivator;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Splitter engine tests.
 */
public class SplitterEngineTests
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

        final Metadata metadata = getMetadata(true);

        _storeService = startStoreService(true);

        _storeProxy = getStoreProxy(_storeService);

        _processorService = createService(
            ProcessorServiceActivator.class,
            Optional.empty());
        startService(_processorService);

        final CIPSupport support = new CIPSupport();
        final CIPClient client = support
            .newClient(support.newClientContext(metadata, Optional.empty()));
        final Point arrayPoint = metadata.getPoint(_TESTS_SPLITTED_1).get();
        final Attributes attributes = arrayPoint
            .getAttributes(CIP.ATTRIBUTES_USAGE)
            .get();
        final int elements = attributes.getInt(CIP.ELEMENTS_ATTRIBUTE, 0);
        final Tuple array = new Tuple();

        for (int i = 0; i < elements; ++i) {
            array.add(Integer.valueOf(i));
        }

        Require
            .present(
                client
                    .requestPointUpdate(
                            new PointValue(
                                    arrayPoint,
                                            Optional.empty(),
                                            null,
                                            array)));

        Require.ignored(client.commitPointUpdateRequests());

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
        final Point splitPoint3 = getPoint(_TESTS_SPLIT_3);
        final Point splitPoint4 = getPoint(_TESTS_SPLIT_4);
        final UUID[] subscribed = new UUID[] {splitPoint3.getUUID().get(),
                splitPoint4.getUUID().get(), };

        Require
            .success(
                _storeProxy.subscribeAndCheck(subscribed, getThisLogger()));

        final Point splittedPoint = getPoint(_TESTS_SPLITTED_1);
        final Attributes attributes = splittedPoint
            .getAttributes(CIP.ATTRIBUTES_USAGE)
            .get();
        final int elements = attributes.getInt(CIP.ELEMENTS_ATTRIBUTE, 0);
        final Tuple array = new Tuple(elements);
        final PointValue splittedPointValue;

        for (int index = 0; index < elements; ++index) {
            array.add(Integer.valueOf(index * 10));
        }

        splittedPointValue = new PointValue(
            splittedPoint,
            Optional.of(DateTime.now()),
            null,
            array);
        Require
            .success(
                _storeProxy
                    .updateAndCheck(splittedPointValue, getThisLogger()));

        final StoreValues response = _storeProxy.deliver(100, getTimeout());

        Require.success(response.isSuccess());
        Require.success(response.size() == 2);

        final PointValue splitPointValue3 = response.getPointValue(0);

        Require
            .equal(splitPointValue3.getStamp(), splittedPointValue.getStamp());
        Require.equal(splitPointValue3.getValue(), Long.valueOf(30));

        final PointValue splitPointValue4 = response.getPointValue(1);

        Require
            .equal(splitPointValue4.getStamp(), splittedPointValue.getStamp());
        Require.equal(splitPointValue4.getValue(), Long.valueOf(40));

        Require
            .success(
                _storeProxy.unsubscribeAndCheck(subscribed, getThisLogger()));
    }

    private static final String _TESTS_PROPERTIES = "rvpf-cip.properties";
    private static final String _TESTS_SPLITTED_1 = "TESTS-CIP.SPLITTED.1";
    private static final String _TESTS_SPLIT_3 = "TESTS-CIP.SPLIT.3";
    private static final String _TESTS_SPLIT_4 = "TESTS-CIP.SPLIT.4";

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
