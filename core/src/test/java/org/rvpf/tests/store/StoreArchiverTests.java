/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreArchiverTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.store;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.metadata.MetadataServiceImpl;
import org.rvpf.store.server.StoreServer;
import org.rvpf.store.server.archiver.Archiver;
import org.rvpf.tests.core.CoreTestsMessages;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Store archiver tests.
 */
public final class StoreArchiverTests
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

        setProperty(_TESTS_ARCHIVE_TIME_PROPERTY, _TESTS_ARCHIVE_TIME);
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
        final StoreServer storeServer = getStoreServer(_storeService);

        if (!Archiver.supports(storeServer)) {
            getThisLogger().info(CoreTestsMessages.STORE_WITHOUT_ARCHIVER);

            return;
        }

        final Store store = getStore(_storeService);
        final DateTime recentStamp = DateTime.now().previousDay();
        final DateTime oldStamp = recentStamp.previousDay().previousDay();
        final long hourRaw = ElapsedTime.HOUR.toRaw();

        store
            .addUpdate(
                new PointValue(
                    _POINT_1_NAME,
                    Optional.of(oldStamp),
                    null,
                    Double.valueOf(1.0)));
        store
            .addUpdate(
                new PointValue(
                    _POINT_2_NAME,
                    Optional.of(oldStamp),
                    null,
                    Double.valueOf(2.0)));
        store
            .addUpdate(
                new PointValue(
                    _POINT_1_NAME,
                    Optional.of(oldStamp.after(1 * hourRaw)),
                    null,
                    Double.valueOf(1.1)));
        store
            .addUpdate(
                new PointValue(
                    _POINT_2_NAME,
                    Optional.of(oldStamp.after(1 * hourRaw)),
                    null,
                    Double.valueOf(2.1)));
        store
            .addUpdate(
                new PointValue(
                    _POINT_1_NAME,
                    Optional.of(oldStamp.after(2 * hourRaw)),
                    null,
                    Double.valueOf(1.2)));
        store
            .addUpdate(
                new PointValue(
                    _POINT_2_NAME,
                    Optional.of(oldStamp.after(2 * hourRaw)),
                    null,
                    Double.valueOf(2.2)));
        store
            .addUpdate(
                new PointValue(
                    _POINT_1_NAME,
                    Optional.of(recentStamp.after(1 * hourRaw)),
                    null,
                    Double.valueOf(1.11)));
        store
            .addUpdate(
                new PointValue(
                    _POINT_2_NAME,
                    Optional.of(recentStamp.after(1 * hourRaw)),
                    null,
                    Double.valueOf(2.11)));
        Require.success(store.sendUpdates());
        store.close();

        expectEvents(Archiver.STORE_REMOVED_EVENT);
        stopService(_storeService);
        _storeService = startStoreService(false);
        waitForEvent(Archiver.STORE_REMOVED_EVENT);

        final StoreSessionProxy storeProxy = getStoreProxy(_storeService);
        final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
            .newBuilder();
        final StoreValuesQuery[] storeQueries = new StoreValuesQuery[2];

        queryBuilder.setCount(true).setAll(true).setIncludeDeleted(true);
        storeQueries[0] = queryBuilder
            .setPoint(new Point.Named(_POINT_1_NAME))
            .build();
        storeQueries[1] = queryBuilder
            .setPoint(new Point.Named(_POINT_2_NAME))
            .build();

        final StoreValues[] storeResponse = storeProxy.select(storeQueries);

        Require.notNull(storeResponse);
        Require.success(storeResponse[0].getCount() == 2);
        Require.success(storeResponse[1].getCount() == 2);

        expectLogs(ServiceMessages.METADATA_REFRESHED);
        sendSignal(
            MetadataServiceImpl.REFRESH_METADATA_SIGNAL,
            Optional.empty());
        waitForLogs(ServiceMessages.METADATA_REFRESHED);

        storeProxy.disconnect();
    }

    private static final String _POINT_1_NAME = "TESTS.NUMERIC.01";
    private static final String _POINT_2_NAME = "TESTS.NUMERIC.02";
    private static final String _TESTS_ARCHIVE_TIME = "P2";
    private static final String _TESTS_ARCHIVE_TIME_PROPERTY =
        "tests.archive.time";

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
