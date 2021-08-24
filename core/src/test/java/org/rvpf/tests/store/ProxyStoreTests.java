/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyStoreTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.store;

import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.proxy.ProxyStoreServiceActivator;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Proxy-store tests.
 */
public final class ProxyStoreTests
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
        setUpAlerter();

        _proxyService = startService(
            ProxyStoreServiceActivator.class,
            Optional.empty());
        _storeProxy = getStoreProxy(_proxyService);
        _point = getMetadata().getPointByName(_NUMERIC_POINT_NAME).get();
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
        _storeProxy.disconnect();

        if (_proxyService != null) {
            stopService(_proxyService);
            _proxyService = null;
        }

        tearDownAlerter();
    }

    /**
     * Test with a restarted store.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testWithStoppedStore")
    public void testWithRestartedStore()
        throws Exception
    {
        _storeService = startStoreService(false);

        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPoint(_point);
        final StoreValues storeResponse = _storeProxy
            .select(storeQueryBuilder.build())
            .get();
        final Optional<PointValue> pointValue = storeResponse.getPointValue();

        Require.notPresent(storeResponse.getException());

        if (pointValue.isPresent()) {    // Skipped for memory stores.
            Require
                .equal(pointValue.get().getPointUUID(), _point.getUUID().get());
            Require.equal(pointValue.get().getStamp(), _stamp.after());
            Require.equal(pointValue.get().getValue(), Double.valueOf(2.0));
        }

        stopService(_storeService);
        Require.failure(_storeProxy.probe());
    }

    /**
     * Test with a started store.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testWithStartedStore()
        throws Exception
    {
        Require.failure(_storeProxy.probe());
        _storeService = startStoreService(false);

        _stamp = DateTime.now();
        Require.success(_storeProxy.probe());
        Require.success(_storeProxy.supportsCount());
        Require
            .success(
                _storeProxy
                    .updateAndCheck(
                            new PointValue(
                                    _point,
                                            Optional.of(_stamp),
                                            null,
                                            Double.valueOf(1.0)),
                                    getThisLogger()));

        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPoint(_point);
        StoreValues storeResponse = _storeProxy
            .select(storeQueryBuilder.build())
            .get();

        Require.notPresent(storeResponse.getException());

        final PointValue pointValue = storeResponse.getPointValue().get();

        Require.equal(pointValue.getPointUUID(), _point.getUUID().get());
        Require.equal(pointValue.getStamp(), _stamp);
        Require.equal(pointValue.getValue(), Double.valueOf(1.0));

        Require
            .success(
                _storeProxy
                    .updateAndCheck(
                            new PointValue(
                                    _point,
                                            Optional.of(_stamp.after()),
                                            null,
                                            Double.valueOf(2.0)),
                                    getThisLogger()));

        storeQueryBuilder.setCount(true);
        storeResponse = _storeProxy.select(storeQueryBuilder.build()).get();

        Require.success(storeResponse.isSuccess());
        Require.success(storeResponse.getCount() > 0);
        Require.success(storeResponse.getCount() == 1);
    }

    /**
     * Test with a stopped store.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testWithStartedStore")
    public void testWithStoppedStore()
        throws Exception
    {
        expectLogs(BaseMessages.POINT_UPDATE_FAILED);

        stopService(_storeService);

        Require.failure(_storeProxy.probe());
        Require
            .success(
                _storeProxy
                    .updateAndCheck(
                            new PointValue(
                                    _point,
                                            Optional.of(DateTime.now()),
                                            null,
                                            Double.valueOf(2.0)),
                                    getThisLogger()));

        waitForLogs(BaseMessages.POINT_UPDATE_FAILED);

        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPoint(_point);
        final StoreValues storeResponse = _storeProxy
            .select(storeQueryBuilder.build())
            .get();

        Require.present(storeResponse.getException());
    }

    private static final String _NUMERIC_POINT_NAME = "TESTS.NUMERIC.01";

    private Point _point;
    private ServiceActivator _proxyService;
    private DateTime _stamp;
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
