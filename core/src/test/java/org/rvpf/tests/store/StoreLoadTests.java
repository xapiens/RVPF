/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreLoadTests.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.tests.store;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.nio.charset.StandardCharsets;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.UUID;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.ElapsedSync;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.metadata.MetadataServiceImpl;
import org.rvpf.store.server.the.TheStoreServiceActivator;
import org.rvpf.tests.core.CoreTestsMessages;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Store load tests.
 */
public final class StoreLoadTests
    extends StoreClientTests
{
    /**
     * Adds one fourth of the point values into the store.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "loadHalf")
    public void addFourth()
        throws Exception
    {
        final int valuesAdded = _valuesPerPoint / 4;

        getThisLogger()
            .info(CoreTestsMessages.ADDING_VALUES, String.valueOf(valuesAdded));
        _markTime();

        final int startPosition = (_valuesPerPoint / 2) + 1;
        DateTime stamp = _firstStamp
            .after(startPosition * ElapsedTime.SECOND.toRaw());
        double value = startPosition;

        for (int i = 0; i < valuesAdded; ++i) {
            for (final UUID pointUUID: _pointUUIDs) {
                _addUpdate(
                    new PointValue(
                        pointUUID,
                        Optional.of(stamp),
                        null,
                        Double.valueOf(value)));
            }

            stamp = stamp.after(2 * ElapsedTime.SECOND.toRaw());
            value += 2.0;
        }

        _flushUpdates();
        _logSent();
        _logTime();
    }

    /**
     * Deletes one fourth of the point values into the store.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "insertFourth")
    public void deleteFourth()
        throws Exception
    {
        final int valuesDeleted = _valuesPerPoint / 4;

        getThisLogger()
            .info(
                CoreTestsMessages.DELETING_VALUES,
                String.valueOf(valuesDeleted));
        _markTime();

        DateTime stamp = _firstStamp;

        for (int i = 0; i < (valuesDeleted); ++i) {
            for (final UUID pointUUID: _pointUUIDs) {
                _addUpdate(
                    new VersionedValue.Deleted(pointUUID, Optional.of(stamp)));
            }

            stamp = stamp.after(2 * ElapsedTime.SECOND.toRaw());
        }

        _flushUpdates();
        _deleted = _sent;
        _logSent();
        _logTime();
    }

    /**
     * Inserts one fourth of the point values into the store.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "addFourth")
    public void insertFourth()
        throws Exception
    {
        final int valuesInserted = (_valuesPerPoint + 2) / 4;

        getThisLogger()
            .info(
                CoreTestsMessages.INSERTING_VALUES,
                String.valueOf(valuesInserted));
        _markTime();

        final int startPosition = _valuesPerPoint / 2;
        DateTime stamp = _firstStamp
            .after(startPosition * ElapsedTime.SECOND.toRaw());
        double value = startPosition;

        for (int i = 0; i < valuesInserted; ++i) {
            for (final UUID pointUUID: _pointUUIDs) {
                _addUpdate(
                    new PointValue(
                        pointUUID,
                        Optional.of(stamp),
                        null,
                        Double.valueOf(value)));
            }

            stamp = stamp.after(2 * ElapsedTime.SECOND.toRaw());
            value += 2.0;
        }

        _flushUpdates();
        _logSent();
        _logTime();
    }

    /**
     * Loads one half of the point values into the store.
     *
     * @throws Exception On failure.
     */
    @Test
    public void loadHalf()
        throws Exception
    {
        final int valuesLoaded = _valuesPerPoint / 2;

        getThisLogger()
            .info(
                CoreTestsMessages.LOADING_VALUES,
                String.valueOf(valuesLoaded));
        _markTime();

        for (final UUID pointUUID: _pointUUIDs) {
            DateTime stamp = _firstStamp;
            double value = 0.0;

            for (int i = 0; i < valuesLoaded; ++i) {
                _addUpdate(
                    new PointValue(
                        pointUUID,
                        Optional.of(stamp),
                        null,
                        Double.valueOf(value)));
                stamp = stamp.after(ElapsedTime.SECOND);
                value += 1.0;
            }
        }

        _flushUpdates();
        _logSent();
        _logTime();
    }

    /**
     * Pulls all point values.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "updateFourth")
    public void pullAll()
        throws Exception
    {
        if (!_store.supportsPull()) {
            getThisLogger().debug(CoreTestsMessages.SKIPPED_NO_PULL);

            return;
        }

        StoreValuesQuery.Builder storeQueryBuilder;

        getThisLogger().info(CoreTestsMessages.PULLING_ALL_NO_DELETED);
        _markTime();

        storeQueryBuilder = StoreValuesQuery.newBuilder();
        storeQueryBuilder.setNotBefore(_startTime);
        storeQueryBuilder.setPull(true);
        Require
            .success(
                _queryAll(
                    storeQueryBuilder.build()) == _valuesPerStore - _deleted);
        _logTime();

        getThisLogger().info(CoreTestsMessages.PULLING_ALL);
        _markTime();

        storeQueryBuilder = StoreValuesQuery.newBuilder();
        storeQueryBuilder.setNotBefore(_startTime);
        storeQueryBuilder.setIncludeDeleted(true);
        Require
            .success(
                _queryAll(
                    storeQueryBuilder.build()) == _valuesPerStore
                    - (storeDropsDeleted(
                            _storeService)? _deleted: 0));
        _logTime();
    }

    /**
     * Purges all tests values.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods =
    {
        "queryCount", "queryPolated"
    })
    public void purgeAll()
        throws Exception
    {
        if (_keepValues) {
            getThisLogger().debug(CoreTestsMessages.VALUES_KEPT);

            return;
        }

        getThisLogger()
            .info(
                CoreTestsMessages.PURGING_ALL,
                String.valueOf(_valuesPerStore));
        _markTime();

        for (final UUID pointUUID: _pointUUIDs) {
            DateTime stamp = _firstStamp;

            for (int i = 0; i < (_valuesPerPoint); ++i) {
                _addUpdate(
                    new VersionedValue.Purged(pointUUID, Optional.of(stamp)));
                stamp = stamp.after(ElapsedTime.SECOND);
            }
        }

        _flushUpdates();
        _logSent();
        _logTime();
    }

    /**
     * Queries all points for their value.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "queryEach")
    public void queryAll()
        throws Exception
    {
        getThisLogger().info(CoreTestsMessages.QUERYING_ALL);
        _markTime();

        final int deletedPerPoint = (int) (_deleted / _pointsPerStore);
        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder();
        boolean reverse = false;

        storeQueryBuilder.setNotBefore(_firstStamp);
        storeQueryBuilder.setAll(true);

        for (final UUID pointUUID: _pointUUIDs) {
            storeQueryBuilder.setPointUUID(pointUUID);
            storeQueryBuilder.setReverse(reverse);
            Require
                .success(
                    _queryAll(
                        storeQueryBuilder.build()) == (_snapshot
                        ? 1: (_valuesPerPoint - deletedPerPoint)));
            reverse = !reverse;
        }

        _logTime();
    }

    /**
     * Queries all points for their count.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "queryAll")
    public void queryCount()
        throws Exception
    {
        if (!_store.supportsCount()) {
            getThisLogger().debug(CoreTestsMessages.SKIPPED_NO_COUNT);

            return;
        }

        getThisLogger().info(CoreTestsMessages.QUERYING_COUNT);
        _markTime();

        final int deletedPerPoint = (int) (_deleted / _pointsPerStore);
        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder();

        storeQueryBuilder.setNotBefore(_firstStamp);
        storeQueryBuilder.setAll(true);
        storeQueryBuilder.setCount(true);

        for (final UUID pointUUID: _pointUUIDs) {
            final StoreValues storeResponse;

            storeQueryBuilder.setPointUUID(pointUUID);
            storeResponse = _store.select(storeQueryBuilder.build());
            Require
                .success(
                    storeResponse.getCount()
                    == (_snapshot? 1: (_valuesPerPoint - deletedPerPoint)));
        }

        _logTime();
    }

    /**
     * Queries all points for each of their values.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "queryLast")
    public void queryEach()
        throws Exception
    {
        getThisLogger().info(CoreTestsMessages.QUERYING_EACH);
        _markTime();

        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder();
        DateTime stamp = _firstStamp;
        boolean nullIgnored = false;
        long seen = 0;

        for (int i = 0; i < _valuesPerPoint; ++i) {
            final Iterator<UUID> points = _pointUUIDs.iterator();

            storeQueryBuilder.setAt(stamp);

            while (points.hasNext()) {
                for (final UUID pointUUID: _pointsBatch(points)) {
                    storeQueryBuilder.setPointUUID(pointUUID);
                    storeQueryBuilder.setNotNull(nullIgnored);
                    Require.ignored(_store.addQuery(storeQueryBuilder.build()));

                    nullIgnored = !nullIgnored;
                }

                for (;;) {
                    final Optional<StoreValues> optionalStoreResponse = _store
                        .nextValues();

                    if (!optionalStoreResponse.isPresent()) {
                        break;
                    }

                    final StoreValues storeResponse = optionalStoreResponse
                        .get();

                    Require.notPresent(storeResponse.getException());
                    Require.success(storeResponse.size() == 1);

                    final PointValue pointValue = storeResponse
                        .getPointValue()
                        .get();

                    _verify(pointValue, null);
                    ++seen;
                }
            }

            stamp = stamp.after(ElapsedTime.SECOND);
        }

        Require
            .success(
                seen
                == (_snapshot? _pointsPerStore: (_valuesPerStore - _deleted)));
        _logTime();
    }

    /**
     * Queries all points for their last value.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "pullAll")
    public void queryLast()
        throws Exception
    {
        getThisLogger().info(CoreTestsMessages.QUERYING_LAST);
        _markTime();

        final Iterator<UUID> points = _pointUUIDs.iterator();
        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder();
        long seen = 0;

        while (points.hasNext()) {
            for (final UUID pointUUID: _pointsBatch(points)) {
                storeQueryBuilder.setPointUUID(pointUUID);
                Require.ignored(_store.addQuery(storeQueryBuilder.build()));
            }

            for (;;) {
                final Optional<StoreValues> optionalStoreResponse = _store
                    .nextValues();

                if (!optionalStoreResponse.isPresent()) {
                    break;
                }

                final StoreValues storeResponse = optionalStoreResponse.get();

                Require.notPresent(storeResponse.getException());
                Require.success(storeResponse.size() == 1);

                final PointValue pointValue = storeResponse
                    .getPointValue()
                    .get();

                _verify(pointValue, null);
                ++seen;
            }
        }

        Require.success(seen == _pointsPerStore);
        _logTime();
    }

    /**
     * Queries all points for their inter/extra-polated value.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "queryEach")
    public void queryPolated()
        throws Exception
    {
        if (_snapshot) {
            getThisLogger().debug(CoreTestsMessages.SKIPPED_SNAPSHOT);

            return;
        }

        getThisLogger().info(CoreTestsMessages.QUERYING_POLATED);
        _markTime();

        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder();
        boolean reverse = false;
        boolean interpolated = true;
        boolean extrapolated = true;

        for (final UUID pointUUID: _pointUUIDs) {
            storeQueryBuilder.setPointUUID(pointUUID).clear();

            if (reverse) {
                storeQueryBuilder
                    .setBefore(
                        _firstStamp
                            .after(_valuesPerPoint
                            * ElapsedTime.SECOND.toRaw()));
            } else {
                storeQueryBuilder.setNotBefore(_firstStamp);
            }

            storeQueryBuilder.setSync(_sync);
            storeQueryBuilder.setRows(_valuesPerPoint);

            storeQueryBuilder.setInterpolated(interpolated);
            storeQueryBuilder.setExtrapolated(extrapolated);
            storeQueryBuilder.setReverse(reverse);
            Require
                .success(
                    _queryAll(storeQueryBuilder.build()) == _valuesPerPoint);

            reverse = !reverse;
            extrapolated = !extrapolated | !interpolated;
            interpolated = !interpolated | !extrapolated;
        }

        _logTime();
    }

    /**
     * Queries all points expecting empty responses.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "purgeAll")
    public void queryPurged()
        throws Exception
    {
        if (_keepValues) {
            getThisLogger().debug(CoreTestsMessages.VALUES_KEPT);

            return;
        }

        getThisLogger().info(CoreTestsMessages.QUERYING_PURGED);
        _markTime();

        final Iterator<UUID> points = _pointUUIDs.iterator();
        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder();

        storeQueryBuilder.setNotBefore(_firstStamp);

        while (points.hasNext()) {
            for (final UUID pointUUID: _pointsBatch(points)) {
                storeQueryBuilder.setPointUUID(pointUUID);

                Require.ignored(_store.addQuery(storeQueryBuilder.build()));
            }

            for (;;) {
                final Optional<StoreValues> optionalStoreResponse = _store
                    .nextValues();

                if (!optionalStoreResponse.isPresent()) {
                    break;
                }

                final StoreValues storeResponse = optionalStoreResponse.get();

                Require.notPresent(storeResponse.getException());
                Require.success(storeResponse.isEmpty());
            }
        }

        _logTime();

        if (_store.supportsPull()) {
            getThisLogger().info(CoreTestsMessages.PULLING_PURGED);
            _markTime();

            storeQueryBuilder.clear();
            storeQueryBuilder.setIncludeDeleted(true);
            storeQueryBuilder.setNotBefore(_startTime);

            final StoreValues storeResponse = _store
                .select(storeQueryBuilder.build());

            Require.notPresent(storeResponse.getException());
            Require.success(storeResponse.isEmpty());
            _logTime();
        }
    }

    /**
     * Sets up this.
     *
     * @param snapshot True if running in snapshot mode.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    @Parameters("snapshot")
    public void setUp(
            @org.testng.annotations.Optional("no") final String snapshot)
        throws Exception
    {
        _snapshot = ValueConverter
            .convertToBoolean(
                "Test parameter",
                "snapshot",
                Optional.of(snapshot),
                false);

        loadConfig();

        _pointsPerStore = getConfig()
            .getIntValue(POINTS_PROPERTY, DEFAULT_POINTS);
        _valuesPerPoint = getConfig()
            .getIntValue(VALUES_PROPERTY, DEFAULT_VALUES);
        _valuesPerStore = _pointsPerStore * _valuesPerPoint;
        _keepValues = getConfig().getBooleanValue(KEEP_VALUES_PROPERTY);
        _batchSize = getConfig()
            .getIntValue(BATCH_SIZE_PROPERTY, DEFAULT_BATCH_SIZE);
        _startTime = DateTime.now();
        _firstStamp = _startTime
            .midnight()
            .before(
                ElapsedTime
                    .fromRaw(_valuesPerPoint * ElapsedTime.SECOND.toRaw()));

        getThisLogger()
            .debug(
                CoreTestsMessages.GENERATING_METADATA,
                String.valueOf(_pointsPerStore));

        final File metadataFile = new File(METADATA_FILE_PATH);
        final File metadataDir = metadataFile.getParentFile();

        Require.success(metadataDir.isDirectory() || metadataDir.mkdirs());

        _generateMetadata();
        setProperty(METADATA_PROPERTY, metadataFile.toURI().toURL().toString());

        getThisLogger().debug(CoreTestsMessages.STARTING_STORE);

        setProperty(LOAD_TESTS_PROPERTY, "!");
        setProperty(NULL_ALERTER_PROPERTY, "!");
        setProperty(NULL_NOTIFIER_PROPERTY, "!");

        if (_snapshot) {
            _storeService = startService(
                TheStoreServiceActivator.class,
                Optional.of(SNAPSHOT_SERVICE_NAME));

            if (!_keepValues) {
                purgeStoreValues(_storeService);
            }
        } else {
            _storeService = startStoreService(!_keepValues);
        }

        getThisLogger().debug(CoreTestsMessages.CONNECTING_TO_STORE);

        _store = getStore(
            STORE_NAME,
            ((MetadataServiceImpl) _storeService.getService()).getMetadata())
            .get();
        _store.connect();

        getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);
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
    }

    /**
     * Updates one fourth of the point values into the store.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "deleteFourth")
    public void updateFourth()
        throws Exception
    {
        final int updatedValues = _valuesPerPoint / 4;

        getThisLogger()
            .info(
                CoreTestsMessages.UPDATING_FOURTH,
                String.valueOf(updatedValues));
        _markTime();

        final int startPosition = (_valuesPerPoint / 2) + 1;
        DateTime stamp = _firstStamp
            .after(startPosition * ElapsedTime.SECOND.toRaw());
        double value = startPosition;

        for (int i = 0; i < updatedValues; ++i) {
            for (final UUID pointUUID: _pointUUIDs) {
                _addUpdate(
                    new PointValue(
                        pointUUID,
                        Optional.of(stamp),
                        null,
                        Double.valueOf(value)));
            }

            stamp = stamp.after(2 * ElapsedTime.SECOND.toRaw());
            value += 2.0;
        }

        _flushUpdates();
        _logSent();
        _logTime();
    }

    private void _addUpdate(final PointValue pointValue)
        throws Exception
    {
        if (_store.getUpdateCount() >= _batchSize) {
            _flushUpdates();
        }

        _store.addUpdate(pointValue);
    }

    private void _flushUpdates()
        throws Exception
    {
        final int updatesSize = _store.getUpdateCount();

        getThisLogger()
            .trace(
                CoreTestsMessages.SENDING_VALUES,
                String.valueOf(updatesSize));

        Require.success(_store.sendUpdates());
        _sent += updatesSize;
    }

    private void _generateMetadata()
        throws Exception
    {
        final Writer writer = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(new File(METADATA_FILE_PATH)),
                StandardCharsets.UTF_8));

        writer.write("<?xml version='1.0'?>\n");
        writer.write("<metadata>\n");
        writer.write("<?include href='resource:rvpf-metadata-shared.xml'?>\n");
        writer.write("<Store name='" + STORE_NAME + "'");
        writer.write(" uuid='" + STORE_UUID + "'>\n");
        writer.write("\t<param name='User' property='tests.user'/>\n");
        writer.write("\t<param name='Password' property='tests.password'/>\n");
        writer.write("</Store>\n");

        for (int i = 1; i <= _pointsPerStore; ++i) {
            final String name = "RVPF-" + i;
            final UUID uuid = UUID.synthesize(i, 0, 0, 0);

            Require.success(_pointUUIDs.add(uuid));
            writer.write("<Point name='" + name + "' uuid='" + uuid + "'");
            writer.write(" content='Numeric' store='" + STORE_NAME + "'");
            writer.write(" anchored='yes'/>\n");
        }

        writer.write("</metadata>\n");
        writer.close();
    }

    private void _logSent()
    {
        getThisLogger()
            .debug(CoreTestsMessages.SENT_VALUES, String.valueOf(_sent));
        _sent = 0;
    }

    private void _logTime()
    {
        getThisLogger()
            .info(
                CoreTestsMessages.TIME,
                String
                    .valueOf((System.currentTimeMillis() - _markTime)
                    / 1000.0f));
    }

    private void _markTime()
    {
        _markTime = System.currentTimeMillis();
    }

    private Collection<UUID> _pointsBatch(final Iterator<UUID> points)
    {
        final Collection<UUID> pointsBatch = new LinkedList<UUID>();

        do {
            pointsBatch.add(points.next());
        } while (points.hasNext() && (pointsBatch.size() < _batchSize));

        return pointsBatch;
    }

    private long _queryAll(final StoreValuesQuery storeQuery)
        throws Exception
    {
        long seen = 0;

        for (final PointValue pointValue: _store.iterate(storeQuery)) {
            _verify(pointValue, storeQuery);
            ++seen;
        }

        return seen;
    }

    private void _verify(
            final PointValue pointValue,
            final StoreValuesQuery storeQuery)
    {
        if (pointValue.isDeleted()) {
            Require.equal(null, pointValue.getValue(), pointValue.toString());
        } else if (pointValue.getValue() == null) {
            Require
                .success(pointValue.isSynthesized(), pointValue.toString());

            if (!pointValue.getStamp().equals(_firstStamp)) {
                Require
                    .failure(storeQuery.isInterpolated(), pointValue.toString());
                Require
                    .equal(
                        pointValue.getStamp(),
                        _firstStamp.after(2 * ElapsedTime.SECOND.toRaw()),
                        pointValue.toString());
            }
        } else {
            Require
                .equal(
                    pointValue.getValue(),
                    Double
                        .valueOf(
                                pointValue
                                        .getStamp()
                                        .sub(_firstStamp)
                                        .scaled(ElapsedTime.SECOND)),
                    pointValue.toString());
        }
    }

    /** Batch size property. */
    public static final String BATCH_SIZE_PROPERTY =
        "tests.store.server.the.load.batch";

    /** Default batch size. */
    public static final int DEFAULT_BATCH_SIZE = 1000;

    /** Default number of points to generate. */
    public static final int DEFAULT_POINTS = 10;

    /** Default number of values per point to generate. */
    public static final int DEFAULT_VALUES = 100;

    /** Property asking to keep the values. */
    public static final String KEEP_VALUES_PROPERTY =
        "tests.store.server.the.load.values.keep";

    /** Load tests property. */
    public static final String LOAD_TESTS_PROPERTY =
        "tests.store.server.the.load";

    /** Metadata file path. */
    public static final String METADATA_FILE_PATH =
        "tests/data/tmp/rvpf-metadata-load.xml";

    /** Property holding the path to the metadata XML text. */
    public static final String METADATA_PROPERTY = "metadata";

    /** Property for the number of points to generate. */
    public static final String POINTS_PROPERTY =
        "tests.store.server.the.load.points";

    /** Store name. */
    public static final String STORE_NAME = "TestsStore";

    /** Store UUID. */
    public static final UUID STORE_UUID = UUID
        .fromString("6392efbe-9d47-4ac6-96b6-e2ef9fdcb1f5")
        .get();

    /** Property for the number of values per point to generate. */
    public static final String VALUES_PROPERTY =
        "tests.store.server.the.load.values";

    private int _batchSize;
    private long _deleted;
    private DateTime _firstStamp;
    private boolean _keepValues;
    private long _markTime;
    private Set<UUID> _pointUUIDs = new LinkedHashSet<UUID>();
    private int _pointsPerStore;
    private long _sent;
    private boolean _snapshot;
    private DateTime _startTime;
    private Store _store;
    private ServiceActivator _storeService;
    private final Sync _sync = new ElapsedSync(
        ElapsedTime.fromMillis(1000),
        Optional.empty());
    private int _valuesPerPoint;
    private long _valuesPerStore;
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
