/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PolationTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.store;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.CrontabSync;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.ServiceActivator;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Inter/extra-polation tests.
 */
public final class PolationTests
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
        setProperty(NULL_ALERTER_PROPERTY, "!");
        setProperty(NULL_NOTIFIER_PROPERTY, "!");
        _storeService = startStoreService(true);

        _storeMetadata = getMetadata(_storeService);

        _store = getStore(_STORE_NAME, _storeMetadata).get();
        _store.connect();
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
        if (_store != null) {
            _store.close();
            _store = null;
        }

        if (_storeService != null) {
            stopService(_storeService);
            _storeService = null;
        }

        _storeMetadata = null;
        _point = null;
    }

    /**
     * Test level polation.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testLevelPolation()
        throws Exception
    {
        StoreValuesQuery storeQuery;
        PointValue pointValue;

        _point = _storeMetadata.getPoint(_LEVEL_POINT_NAME).get();
        Require.notNull(_point);

        _add(10, 10.0);
        _add(30, 30.0);
        Require.success(_store.sendUpdates());

        pointValue = _askValue(_storeQuery(_at(10), true, true));
        Require.failure(pointValue.isSynthesized());
        Require.equal(pointValue.getValue(), (Double.valueOf(10.0)));

        storeQuery = _storeQuery(_at(20), true, true);
        pointValue = _askValue(storeQuery);
        Require.success(pointValue.isInterpolated());
        Require.failure(pointValue.isExtrapolated());
        Require.equal(pointValue.getValue(), (Double.valueOf(10.0)));
        pointValue = _askValue(_storeQuery(storeQuery, false, true, false));
        Require.failure(pointValue.isInterpolated());
        Require.success(pointValue.isExtrapolated());
        Require.success(pointValue.isSynthesized());
        Require.equal(pointValue.getValue(), (Double.valueOf(10.0)));

        pointValue = _askValue(_storeQuery(_at(30), true, true));
        Require.failure(pointValue.isSynthesized());
        Require.equal(pointValue.getValue(), (Double.valueOf(30.0)));

        storeQuery = _storeQuery(_at(40), true, true);
        pointValue = _askValue(storeQuery);
        Require.success(pointValue.isExtrapolated());
        Require.equal(pointValue.getValue(), (Double.valueOf(30.0)));
        pointValue = _askValue(_storeQuery(storeQuery, true, false, false));
        Require.failure(pointValue.isInterpolated());
        Require.failure(pointValue.isExtrapolated());
        Require.success(pointValue.isSynthesized());
        Require.success(pointValue.getValue() == null);

        StoreValues storeResponse;

        storeQuery = _storeQuery(
            _notBefore(_at(0)).notAfter(_at(50)),
            true,
            true,
            false,
            0);
        storeResponse = _askResponse(storeQuery);
        Require
            .equal(
                storeResponse,
                _askResponse(
                    _storeQuery(_notBefore(_at(0)), true, true, false, 6)));
        Require.success(storeResponse.size() == 6);
        Require.success(storeResponse.getPointValue(0).isSynthesized());
        Require.failure(storeResponse.getPointValue(0).isInterpolated());
        Require.failure(storeResponse.getPointValue(0).isExtrapolated());
        Require.success(storeResponse.getPointValue(0).getValue() == null);
        Require.failure(storeResponse.getPointValue(1).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(1).getValue(),
                (Double.valueOf(10.0)));
        Require.success(storeResponse.getPointValue(2).isInterpolated());
        Require
            .equal(
                storeResponse.getPointValue(2).getValue(),
                (Double.valueOf(10.0)));
        Require.failure(storeResponse.getPointValue(3).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(3).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(4).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(4).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(5).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(5).getValue(),
                (Double.valueOf(30.0)));
        storeResponse = _askResponse(
            _storeQuery(storeQuery, true, false, false));
        Require.success(storeResponse.size() == 6);
        Require.success(storeResponse.getPointValue(0).isSynthesized());
        Require.failure(storeResponse.getPointValue(0).isInterpolated());
        Require.failure(storeResponse.getPointValue(0).isExtrapolated());
        Require.success(storeResponse.getPointValue(0).getValue() == null);
        Require.failure(storeResponse.getPointValue(1).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(1).getValue(),
                (Double.valueOf(10.0)));
        Require.success(storeResponse.getPointValue(2).isInterpolated());
        Require
            .equal(
                storeResponse.getPointValue(2).getValue(),
                (Double.valueOf(10.0)));
        Require.failure(storeResponse.getPointValue(3).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(3).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(4).getValue() == null);
        Require.success(storeResponse.getPointValue(5).getValue() == null);
        storeResponse = _askResponse(
            _storeQuery(storeQuery, false, true, false));
        Require.success(storeResponse.size() == 6);
        Require.success(storeResponse.getPointValue(0).isSynthesized());
        Require.failure(storeResponse.getPointValue(0).isInterpolated());
        Require.failure(storeResponse.getPointValue(0).isExtrapolated());
        Require.success(storeResponse.getPointValue(0).getValue() == null);
        Require.failure(storeResponse.getPointValue(1).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(1).getValue(),
                (Double.valueOf(10.0)));
        Require
            .equal(
                storeResponse.getPointValue(2).getValue(),
                (Double.valueOf(10.0)));
        Require.failure(storeResponse.getPointValue(3).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(3).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(4).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(4).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(5).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(5).getValue(),
                (Double.valueOf(30.0)));
        storeResponse = _askResponse(_storeQuery(storeQuery, true, true, true));
        Require
            .equal(
                storeResponse,
                _askResponse(
                    _storeQuery(_notAfter(_at(50)), true, true, true, 6)));
        Require.success(storeResponse.size() == 6);
        Require.success(storeResponse.getPointValue(5).isSynthesized());
        Require.failure(storeResponse.getPointValue(5).isInterpolated());
        Require.failure(storeResponse.getPointValue(5).isExtrapolated());
        Require.success(storeResponse.getPointValue(5).getValue() == null);
        Require.failure(storeResponse.getPointValue(4).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(4).getValue(),
                (Double.valueOf(10.0)));
        Require.success(storeResponse.getPointValue(3).isInterpolated());
        Require
            .equal(
                storeResponse.getPointValue(3).getValue(),
                (Double.valueOf(10.0)));
        Require.failure(storeResponse.getPointValue(2).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(2).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(1).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(1).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(0).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(0).getValue(),
                (Double.valueOf(30.0)));
    }

    /**
     * Test linear polation.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testLinearPolation()
        throws Exception
    {
        StoreValuesQuery storeQuery;
        PointValue pointValue;

        _point = _storeMetadata.getPoint(_LINEAR_POINT_NAME).get();
        Require.notNull(_point);

        _add(10, 10.0);
        _add(30, 30.0);
        Require.success(_store.sendUpdates());

        pointValue = _askValue(_storeQuery(_at(10), true, true));
        Require.failure(pointValue.isSynthesized());
        Require.equal(pointValue.getValue(), (Double.valueOf(10.0)));

        storeQuery = _storeQuery(_at(20), true, true);
        pointValue = _askValue(storeQuery);
        Require.success(pointValue.isInterpolated());
        Require.failure(pointValue.isExtrapolated());
        Require.equal(pointValue.getValue(), (Double.valueOf(20.0)));
        pointValue = _askValue(_storeQuery(storeQuery, false, true, false));
        Require.failure(pointValue.isInterpolated());
        Require.failure(pointValue.isExtrapolated());
        Require.success(pointValue.isSynthesized());
        Require.success(pointValue.getValue() == null);

        pointValue = _askValue(_storeQuery(_at(30), true, true));
        Require.failure(pointValue.isSynthesized());
        Require.equal(pointValue.getValue(), (Double.valueOf(30.0)));

        storeQuery = _storeQuery(_at(40), true, true);
        pointValue = _askValue(storeQuery);
        Require.success(pointValue.isExtrapolated());
        Require.equal(pointValue.getValue(), (Double.valueOf(40.0)));
        pointValue = _askValue(_storeQuery(storeQuery, true, false, false));
        Require.failure(pointValue.isInterpolated());
        Require.failure(pointValue.isExtrapolated());
        Require.success(pointValue.isSynthesized());
        Require.success(pointValue.getValue() == null);

        StoreValues storeResponse;

        storeQuery = _storeQuery(
            _notBefore(_at(0)).notAfter(_at(50)),
            true,
            true,
            false,
            0);

        storeResponse = _askResponse(storeQuery);
        Require
            .equal(
                storeResponse,
                _askResponse(
                    _storeQuery(_notBefore(_at(0)), true, true, false, 6)));
        Require.success(storeResponse.size() == 6);
        Require.success(storeResponse.getPointValue(0).isSynthesized());
        Require.failure(storeResponse.getPointValue(0).isInterpolated());
        Require.failure(storeResponse.getPointValue(0).isExtrapolated());
        Require.success(storeResponse.getPointValue(0).getValue() == null);
        Require.failure(storeResponse.getPointValue(1).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(1).getValue(),
                (Double.valueOf(10.0)));
        Require.success(storeResponse.getPointValue(2).isInterpolated());
        Require
            .equal(
                storeResponse.getPointValue(2).getValue(),
                (Double.valueOf(20.0)));
        Require.failure(storeResponse.getPointValue(3).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(3).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(4).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(4).getValue(),
                (Double.valueOf(40.0)));
        Require.success(storeResponse.getPointValue(5).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(5).getValue(),
                (Double.valueOf(50.0)));

        storeResponse = _askResponse(
            _storeQuery(storeQuery, true, false, false));
        Require.success(storeResponse.size() == 6);
        Require.success(storeResponse.getPointValue(0).isSynthesized());
        Require.failure(storeResponse.getPointValue(0).isInterpolated());
        Require.failure(storeResponse.getPointValue(0).isExtrapolated());
        Require.success(storeResponse.getPointValue(0).getValue() == null);
        Require.failure(storeResponse.getPointValue(1).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(1).getValue(),
                (Double.valueOf(10.0)));
        Require.success(storeResponse.getPointValue(2).isInterpolated());
        Require
            .equal(
                storeResponse.getPointValue(2).getValue(),
                (Double.valueOf(20.0)));
        Require.failure(storeResponse.getPointValue(3).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(3).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(4).getValue() == null);
        Require.success(storeResponse.getPointValue(5).getValue() == null);

        storeResponse = _askResponse(
            _storeQuery(storeQuery, false, true, false));
        Require.success(storeResponse.size() == 6);
        Require.success(storeResponse.getPointValue(0).isSynthesized());
        Require.failure(storeResponse.getPointValue(0).isInterpolated());
        Require.failure(storeResponse.getPointValue(0).isExtrapolated());
        Require.success(storeResponse.getPointValue(0).getValue() == null);
        Require.failure(storeResponse.getPointValue(1).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(1).getValue(),
                (Double.valueOf(10.0)));
        Require.success(storeResponse.getPointValue(2).getValue() == null);
        Require.failure(storeResponse.getPointValue(3).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(3).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(4).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(4).getValue(),
                (Double.valueOf(40.0)));
        Require.success(storeResponse.getPointValue(5).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(5).getValue(),
                (Double.valueOf(50.0)));

        storeResponse = _askResponse(_storeQuery(storeQuery, true, true, true));
        Require
            .equal(
                storeResponse,
                _askResponse(
                    _storeQuery(_notAfter(_at(50)), true, true, true, 6)));
        Require.success(storeResponse.size() == 6);
        Require.success(storeResponse.getPointValue(5).isSynthesized());
        Require.failure(storeResponse.getPointValue(5).isInterpolated());
        Require.failure(storeResponse.getPointValue(5).isExtrapolated());
        Require.success(storeResponse.getPointValue(5).getValue() == null);
        Require.failure(storeResponse.getPointValue(4).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(4).getValue(),
                (Double.valueOf(10.0)));
        Require.success(storeResponse.getPointValue(3).isInterpolated());
        Require
            .equal(
                storeResponse.getPointValue(3).getValue(),
                (Double.valueOf(20.0)));
        Require.failure(storeResponse.getPointValue(2).isSynthesized());
        Require
            .equal(
                storeResponse.getPointValue(2).getValue(),
                (Double.valueOf(30.0)));
        Require.success(storeResponse.getPointValue(1).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(1).getValue(),
                (Double.valueOf(40.0)));
        Require.success(storeResponse.getPointValue(0).isExtrapolated());
        Require
            .equal(
                storeResponse.getPointValue(0).getValue(),
                (Double.valueOf(50.0)));

        storeQuery = StoreValuesQuery
            .newBuilder()
            .copyFrom(storeQuery)
            .setLimit(3)
            .build();
        storeResponse = _askResponse(
            _storeQuery(storeQuery, true, true, false));
        Require.success(storeResponse.size() == 3);
        Require.present(storeResponse.getMark());
        storeQuery = storeResponse.createQuery();
        storeResponse = _askResponse(storeQuery);
        Require.success(storeResponse.size() == 3);
        Require.notPresent(storeResponse.getMark());
    }

    private static TimeInterval _notAfter(final DateTime end)
    {
        return TimeInterval.UNLIMITED.notAfter(end);
    }

    private static TimeInterval _notBefore(final DateTime beginning)
    {
        return TimeInterval.UNLIMITED.notBefore(beginning);
    }

    private static StoreValuesQuery _storeQuery(
            final StoreValuesQuery storeQuery,
            final boolean interpolated,
            final boolean extrapolated,
            final boolean reverse)
    {
        return StoreValuesQuery
            .newBuilder()
            .copyFrom(storeQuery)
            .setInterpolated(interpolated)
            .setExtrapolated(extrapolated)
            .setReverse(reverse)
            .build();
    }

    private void _add(final int minutes, final double value)
    {
        _store
            .addUpdate(
                new PointValue(
                    _point,
                    Optional.of(_at(minutes)),
                    null,
                    Double.valueOf(value)));
    }

    private StoreValues _askResponse(
            final StoreValuesQuery query)
        throws Exception
    {
        final StoreValues storeResponse = _store.select(query);

        Require.notPresent(storeResponse.getException());

        return storeResponse;
    }

    private PointValue _askValue(final StoreValuesQuery query)
        throws Exception
    {
        final StoreValues storeResponse = _store.select(query);
        final PointValue pointValue = storeResponse.getPointValue().get();

        Require.notPresent(storeResponse.getException());

        return pointValue;
    }

    private DateTime _at(final int minutes)
    {
        return _yesterday.after(minutes * ElapsedTime.MINUTE.toRaw());
    }

    private StoreValuesQuery _storeQuery(
            final DateTime at,
            final boolean interpolated,
            final boolean extrapolated)
    {
        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPoint(_point);

        storeQueryBuilder.setAt(at);
        storeQueryBuilder.setInterpolated(interpolated);
        storeQueryBuilder.setExtrapolated(extrapolated);

        return storeQueryBuilder.build();
    }

    private StoreValuesQuery _storeQuery(
            final TimeInterval interval,
            final boolean interpolated,
            final boolean extrapolated,
            final boolean reverse,
            final int rows)
    {
        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPoint(_point)
            .setInterval(interval)
            .setInterpolated(interpolated)
            .setExtrapolated(extrapolated)
            .setReverse(reverse);

        if (rows > 0) {
            storeQueryBuilder.setRows(rows);
        }

        return storeQueryBuilder.setSync(_sync).build();
    }

    private static final String _LEVEL_POINT_NAME = "TESTS.NUMERIC.01";
    private static final String _LINEAR_POINT_NAME = "TESTS.NUMERIC.02";
    private static final String _STORE_NAME = "TestsStore";

    private Point _point;
    private Store _store;
    private Metadata _storeMetadata;
    private ServiceActivator _storeService;
    private Sync _sync = new CrontabSync("*/10 * * * *");
    private DateTime _yesterday = DateTime.now().midnight().previousDay();
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
