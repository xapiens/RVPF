/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CStoreServiceTests.java 4100 2019-06-29 17:22:34Z SFB $
 */

package org.rvpf.tests.store;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.c.CStoreServiceActivator;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * C store service tests.
 */
public final class CStoreServiceTests
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
        setProperty(_TESTS_SERVICE_PROPERTY, "!");

        setUpAlerter();

        setProperty(NULL_NOTIFIER_PROPERTY, "!");
        _storeService = startStoreService(true);
    }

    /**
     * Stops the CStore.
     *
     * @throws Exception On failure.
     */
    @AfterMethod(alwaysRun = true)
    public void stopCStore()
        throws Exception
    {
        if (_cStoreService != null) {
            stopService(_cStoreService);
            _cStoreService = null;
        }
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
        if (_storeService != null) {
            stopService(_storeService);
            _storeService = null;

            tearDownAlerter();
        }
    }

    /**
     * Tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testProxyImpl()
        throws Exception
    {
        if (!CStoreServiceActivator.isImplemented()) {
            return;
        }

        _cStoreService = startService(
            CStoreServiceActivator.class,
            Optional.empty());

        final StoreSessionProxy storeProxy = getStoreProxy(_cStoreService);
        final Point point = getMetadata(_cStoreService)
            .getPointByName(_CSTORE_POINT_NAME)
            .get();
        final DateTime stamp = DateTime.now().floored(ElapsedTime.MINUTE);
        PointValue updateValue;

        updateValue = new PointValue(
            point,
            Optional.of(stamp),
            null,
            Double.valueOf(1.0));
        Require
            .success(storeProxy.updateAndCheck(updateValue, getThisLogger()));

        final UUID pointUUID = point.getUUID().get();
        final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
            .newBuilder();
        StoreValues storeResponse;
        PointValue responseValue;

        queryBuilder.setPointUUID(pointUUID);

        storeResponse = storeProxy.select(queryBuilder.build()).get();
        responseValue = storeResponse.getPointValue().get();
        Require.equal(pointUUID, responseValue.getPointUUID());
        Require.equal(responseValue.getStamp(), updateValue.getStamp());
        Require.equal(responseValue.getValue(), updateValue.getValue());

        final boolean deliverEnabled = getConfig()
            .getBooleanValue(_TESTS_DELIVER_ENABLED_PROPERTY);

        if (deliverEnabled) {
            Require
                .success(
                    storeProxy.subscribeAndCheck(pointUUID, getThisLogger()));
            storeResponse = storeProxy.getResponse().get();
            Require.equal(storeResponse.getPointValue().get(), updateValue);
        }

        updateValue = new PointValue(
            point,
            Optional.of(stamp),
            null,
            Double.valueOf(2.0));
        Require
            .success(storeProxy.updateAndCheck(updateValue, getThisLogger()));

        storeResponse = storeProxy.select(queryBuilder.build()).get();
        responseValue = storeResponse.getPointValue().get();
        Require.equal(responseValue.getPointUUID(), pointUUID);
        Require.equal(responseValue.getStamp(), updateValue.getStamp());
        Require.equal(responseValue.getValue(), updateValue.getValue());

        if (deliverEnabled) {
            storeResponse = storeProxy.deliver(100, 15000);
            responseValue = storeResponse.getPointValue().get();
            Require.equal(responseValue.getPointUUID(), pointUUID);
            Require.equal(responseValue.getStamp(), updateValue.getStamp());
            Require.equal(responseValue.getValue(), updateValue.getValue());

            Require
                .success(
                    storeProxy.unsubscribeAndCheck(pointUUID, getThisLogger()));
        }

        if (storeProxy.supportsCount()) {
            queryBuilder.clear();
            queryBuilder.setAt(stamp).setAll(true).setCount(true);
            storeResponse = storeProxy.select(queryBuilder.build()).get();
            Require.success(storeResponse.getCount() == 1);
        }

        storeProxy.disconnect();
    }

    private static final String _CSTORE_POINT_NAME = "TESTS.CSTORE.01";
    private static final String _TESTS_DELIVER_ENABLED_PROPERTY =
        "tests.cstore.deliver.enabled";
    private static final String _TESTS_SERVICE_PROPERTY = "tests.cstore";

    private ServiceActivator _cStoreService;
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
