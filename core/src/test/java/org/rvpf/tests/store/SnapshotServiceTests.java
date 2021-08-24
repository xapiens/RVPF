/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SnapshotServiceTests.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.tests.store;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.the.TheStoreServiceActivator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Snapshot service tests.
 */
public class SnapshotServiceTests
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

        _snapshotService = startService(
            TheStoreServiceActivator.class,
            Optional.of(SNAPSHOT_SERVICE_NAME));
        setUpExpectedUpdates(_snapshotService, _SNAPSHOT_POINT_NAME_1);
        _storeProxy = getStoreProxy(_snapshotService);
        _storeMetadata = getMetadata(_snapshotService);
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
        _storeMetadata = null;
        _storeProxy.disconnect();
        tearDownExpectedUpdates();

        if (_snapshotService != null) {
            stopService(_snapshotService);
            _snapshotService = null;
        }

        tearDownAlerter();
    }

    /**
     * Tests the snapshot service.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testSnapshotService()
        throws Exception
    {
        final DateTime stamp = DateTime.now().midnight();

        _test(stamp, 1, "Test-A");
        _test(stamp.previousDay().previousDay(), 2, "Test-B");
        _test(stamp.previousDay(), 3, "Test-C");
    }

    /**
     * Requires equal point values.
     *
     * @param first
     * @param second
     */
    void requireEqualPointValues(
            final PointValue first,
            final PointValue second)
    {
        Require.equal(first.getPointUUID(), second.getPointUUID());
        Require.equal(first.getStamp(), second.getStamp());
        Require.equal(first.getValue(), second.getValue());
    }

    private void _test(
            final DateTime stamp,
            final long longValue,
            final String textValue)
        throws Exception
    {
        expectUpdates(_SNAPSHOT_POINT_NAME_1);

        final Point point1 = _storeMetadata
            .getPointByName(_SNAPSHOT_POINT_NAME_1)
            .get();
        final Point point2 = _storeMetadata
            .getPointByName(_SNAPSHOT_POINT_NAME_2)
            .get();

        Require.notNull(point1);
        Require.notNull(point2);

        final PointValue[] updates = new PointValue[] {new PointValue(
            point1,
            Optional.of(stamp),
            null,
            Long.valueOf(
                longValue)), new PointValue(
                    point2,
                    Optional.of(stamp),
                    null,
                    textValue), };

        Require.success(_storeProxy.updateAndCheck(updates, getThisLogger()));

        final PointValue pointValue = waitForUpdate(_SNAPSHOT_POINT_NAME_1);

        requireEqualPointValues(pointValue, updates[0]);

        final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
            .newBuilder();
        final StoreValuesQuery[] queries = new StoreValuesQuery[] {
            queryBuilder.setPoint(
                new Point.Named(
                    _SNAPSHOT_POINT_NAME_1)).build(), queryBuilder.setPoint(
                            new Point.Named(_SNAPSHOT_POINT_NAME_2)).build(), };

        final StoreValues[] storeResponses = _storeProxy.select(queries);

        for (int i = 0; i < queries.length; ++i) {
            requireEqualPointValues(
                storeResponses[i].getPointValue().orElse(null),
                updates[i]);
        }
    }

    private static final String _SNAPSHOT_POINT_NAME_1 = "TESTS.SNAPSHOT.01";
    private static final String _SNAPSHOT_POINT_NAME_2 = "TESTS.SNAPSHOT.02";

    private ServiceActivator _snapshotService;
    private Metadata _storeMetadata;
    private StoreSessionProxy _storeProxy;
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
