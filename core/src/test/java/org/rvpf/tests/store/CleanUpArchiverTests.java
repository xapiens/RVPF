/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CleanUpArchiverTests.java 4100 2019-06-29 17:22:34Z SFB $
 */

package org.rvpf.tests.store;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.UUID;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.archiver.CleanUpArchiver;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Clean-up archiver tests.
 */
public class CleanUpArchiverTests
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

        _storeService = startStoreService(true);
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
        }

        tearDownAlerter();
    }

    /**
     * Tests the archiver.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testArchiver()
        throws Exception
    {
        final Store store = getStore(_storeService);
        final DateTime recentStamp = DateTime.now().previousDay();
        final DateTime oldStamp = recentStamp.previousDay().previousDay();
        final long hourRaw = ElapsedTime.HOUR.toRaw();

        store
            .addUpdate(
                new PointValue(
                    _POINT_1_UUID,
                    Optional.of(oldStamp),
                    null,
                    Double.valueOf(1.0)));
        store
            .addUpdate(
                new PointValue(
                    _UNKNOWN_UUID,
                    Optional.of(oldStamp),
                    null,
                    Double.valueOf(2.0)));
        store
            .addUpdate(
                new PointValue(
                    _POINT_1_UUID,
                    Optional.of(oldStamp.after(1 * hourRaw)),
                    null,
                    Double.valueOf(1.1)));
        store
            .addUpdate(
                new PointValue(
                    _UNKNOWN_UUID,
                    Optional.of(oldStamp.after(1 * hourRaw)),
                    null,
                    Double.valueOf(2.1)));
        store
            .addUpdate(
                new PointValue(
                    _POINT_1_UUID,
                    Optional.of(oldStamp.after(2 * hourRaw)),
                    null,
                    Double.valueOf(1.2)));
        store
            .addUpdate(
                new PointValue(
                    _UNKNOWN_UUID,
                    Optional.of(oldStamp.after(2 * hourRaw)),
                    null,
                    Double.valueOf(2.2)));
        store
            .addUpdate(
                new PointValue(
                    _POINT_1_UUID,
                    Optional.of(recentStamp.after(1 * hourRaw)),
                    null,
                    Double.valueOf(1.11)));
        store
            .addUpdate(
                new PointValue(
                    _UNKNOWN_UUID,
                    Optional.of(recentStamp.after(1 * hourRaw)),
                    null,
                    Double.valueOf(2.11)));
        Require.success(store.sendUpdates());
        store.close();

        final CleanUpArchiver cleanUpArchiver = new CleanUpArchiver();
        final String storeName = getStore(_storeService).getName();

        Require
            .success(
                cleanUpArchiver
                    .cleanUp(
                            storeName,
                                    getMetadata(),
                                    KeyedGroups.MISSING_KEYED_GROUP));

        final StoreSessionProxy storeProxy = getStoreProxy(_storeService);
        final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
            .newBuilder();
        final StoreValuesQuery[] storeQueries = new StoreValuesQuery[2];

        queryBuilder.setCount(true).setAll(true).setIncludeDeleted(true);
        storeQueries[0] = queryBuilder.setPointUUID(_POINT_1_UUID).build();
        storeQueries[1] = queryBuilder.setPointUUID(_UNKNOWN_UUID).build();

        final StoreValues[] storeResponse = storeProxy.select(storeQueries);

        Require.success(storeResponse[0].getCount() == 4);
        Require.success(storeResponse[1].getCount() == 0);

        storeProxy.disconnect();
    }

    private static final UUID _POINT_1_UUID = UUID
        .fromString("6069f04b-aa26-4e45-8f2d-2d260b88e543")
        .get();
    private static final UUID _UNKNOWN_UUID = UUID
        .fromString("7edf2726-8755-4672-9e22-1db53c5427ed")
        .get();

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
