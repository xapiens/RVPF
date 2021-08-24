/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreServiceTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.store;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.PointValuesDumper;
import org.rvpf.base.util.PointValuesLoader;
import org.rvpf.base.value.BigRational;
import org.rvpf.base.value.Complex;
import org.rvpf.base.value.Dict;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Rational;
import org.rvpf.base.value.State;
import org.rvpf.base.value.Tuple;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.base.xml.streamer.PointValuesDumperOutput;
import org.rvpf.base.xml.streamer.PointValuesLoaderInput;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.service.ServiceActivator;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.TestsMessages;
import org.rvpf.tests.core.CoreTestsMessages;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Store service tests.
 */
public class StoreServiceTests
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
        setProperty(UPDATES_LISTENER_ENABLED, "1");
        setProperty(REPLICATOR_ENABLED_PROPERTY, "1");
        setUpAlerter();

        final MessagingSupport.Receiver updateReceiver;

        updateReceiver = getMessaging()
            .createClientReceiver(
                getConfig().getPropertiesGroup(UPDATER_QUEUE_PROPERTIES));
        updateReceiver.purge();
        updateReceiver.close();

        _storeService = startStoreService(true);
        _startStamp = VersionedValue.newVersion();
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
            purgeReplicates(_storeService);
            stopService(_storeService);
            _storeService = null;
        }

        tearDownAlerter();
    }

    /**
     * Test bound store.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testStoreSessionProxy")
    public void testBoundStore()
        throws Exception
    {
        final Metadata metadata = getMetadata();
        final PointEntity boundPoint = (PointEntity) metadata
            .getPointByName(_TEXT_POINT_2_NAME)
            .get();

        Require.success(boundPoint.setUp(metadata));

        if (_supportsPull) {
            final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                .newBuilder()
                .setPoint(boundPoint);

            queryBuilder.setPull(true);
            queryBuilder.setNotBefore(_startStamp);

            final StoreValues storeResponse = queryBuilder
                .build()
                .getResponse();

            Require.success(storeResponse.size() == 2);

            final VersionedValue pullValue = (VersionedValue) storeResponse
                .getPointValue(storeResponse.size() - 2)
                .restore(metadata);

            Require.equal(pullValue.getPointName(), boundPoint.getName());
        }
    }

    /**
     * Tests dumper and loader.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testStoreSessionProxy")
    public void testDumperLoader()
        throws Exception
    {
        if (!_supportsPull) {
            getThisLogger().info(CoreTestsMessages.SKIPPED_NO_PULL);

            return;
        }

        final boolean storeReplicates = storeReplicates(_storeService);
        final boolean storeDropsDeleted = storeDropsDeleted(_storeService);
        final StoreValuesQuery.Builder dumperQueryBuilder = StoreValuesQuery
            .newBuilder();
        final StoreSessionProxy storeProxy = getStoreProxy(_storeService);
        final StringWriter writer = new StringWriter();
        final PointValuesDumperOutput output = new PointValuesDumperOutput(
            writer);

        dumperQueryBuilder.setAfter(_startStamp);
        dumperQueryBuilder.setLimit(3);
        dumperQueryBuilder.setIncludeDeleted(true);

        final PointValuesDumper dumper = new PointValuesDumper(
            dumperQueryBuilder.build(),
            storeProxy,
            Optional.of(getMetadata()),
            output);
        Integer count;

        count = dumper.call();
        Require
            .success(
                count.intValue()
                == 5 + (storeReplicates? 1: 0) + (storeDropsDeleted? 0: 1));

        purgeStoreValues(_storeService);

        final StringReader reader = new StringReader(writer.toString());
        final PointValuesLoaderInput input = new PointValuesLoaderInput(reader);
        final PointValuesLoader loader = new PointValuesLoader(
            storeProxy,
            input);

        loader.setBatchLimit(3);
        count = loader.call();
        Require
            .success(
                count.intValue()
                == 5 + (storeReplicates? 1: 0) + (storeDropsDeleted? 0: 1));
        reader.close();
        writer.close();

        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder();
        final StoreValues storeResponse;

        storeQueryBuilder.setNotBefore(_startStamp);
        storeResponse = storeProxy.select(storeQueryBuilder.build()).get();
        Require.success(storeResponse.size() == 5 + (storeReplicates? 1: 0));

        storeProxy.disconnect();
    }

    /**
     * Test state queries.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testStoreSessionProxy")
    public void testStateQueries()
        throws Exception
    {
        final StoreSessionProxy storeProxy = getStoreProxy(_storeService);
        final Metadata metadata = getMetadata();
        final Point point = metadata.getPointByName(_STATE_POINT_NAME).get();
        final Optional<String> stateName = point
            .getParams()
            .getString(Point.STATES_PARAM);
        Optional<State.Group> stateGroup = storeProxy.getStateGroup(stateName);
        final List<State> states = stateGroup.get().getStatesByCode();
        final State state0 = states.get(0);
        final State state1 = states.get(1);

        Require
            .equal(
                storeProxy
                    .resolve(
                            new State(state0.getCode(), Optional.empty()),
                                    point),
                state0);
        Require
            .equal(
                storeProxy
                    .resolve(
                            new State(Optional.empty(), state1.getName()),
                                    point),
                state1);

        stateGroup = storeProxy.getStateGroup(Optional.empty());

        final List<State> globalStates = stateGroup.get().getStatesByCode();
        final State globalState0 = globalStates.get(0);
        final State globalState1 = globalStates.get(1);

        Require
            .equal(
                storeProxy
                    .resolve(
                            new State(
                                    Optional.empty(),
                                            globalState0.getName())),
                globalState0);
        Require
            .equal(
                storeProxy
                    .resolve(
                            new State(
                                    globalState1.getCode(),
                                            Optional.empty())),
                globalState1);

        storeProxy.disconnect();
    }

    /**
     * Test store queues.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testStoreQueues()
        throws Exception
    {
        final Metadata storeMetadata = getStoreServer(_storeService)
            .getMetadata();
        final MessagingSupport.Receiver noticeReceiver = getMessaging()
            .createClientReceiver(
                storeMetadata.getPropertiesGroup(NOTIFIER_QUEUE_PROPERTIES));

        noticeReceiver.purge();

        final MessagingSupport.Receiver replicatedReceiver;
        final MessagingSupport.Receiver replicateReceiver;

        if (storeReplicates(_storeService)) {
            replicatedReceiver = getMessaging()
                .createClientReceiver(
                    storeMetadata
                        .getPropertiesGroup(REPLICATED_QUEUE_PROPERTIES));
            replicatedReceiver.purge();
            replicateReceiver = getMessaging()
                .createClientReceiver(
                    storeMetadata
                        .getPropertiesGroup(REPLICATE_QUEUE_PROPERTIES));
            replicateReceiver.purge();
        } else {
            replicatedReceiver = null;
            replicateReceiver = null;
        }

        final MessagingSupport.Sender updateSender = getMessaging()
            .createClientSender(
                storeMetadata.getPropertiesGroup(UPDATER_QUEUE_PROPERTIES));
        final Point numericPoint1 = storeMetadata
            .getPointByName(_NUMERIC_POINT_1_NAME)
            .get();
        PointValue sentPointValue;

        sentPointValue = new PointValue(
            numericPoint1,
            Optional.of(_startStamp),
            null,
            Double.valueOf(1.0));
        updateSender.send(sentPointValue);
        updateSender.commit();

        final long timeout = getTimeout();
        PointValue receivedPointValue;

        receivedPointValue = (PointValue) noticeReceiver.receive(timeout);
        Require.equal(receivedPointValue, sentPointValue);

        if (replicatedReceiver != null) {
            receivedPointValue = (PointValue) replicatedReceiver
                .receive(timeout);
            Require.equal(receivedPointValue, sentPointValue);
        }

        if (replicateReceiver != null) {
            final Point replicatePoint = storeMetadata
                .getPointByName(_REPLICATE_POINT_1_NAME)
                .get();

            receivedPointValue = (PointValue) replicateReceiver
                .receive(timeout);
            Require
                .equal(
                    receivedPointValue.getPointUUID(),
                    replicatePoint.getUUID().get());
            Require
                .equal(
                    receivedPointValue.getStamp(),
                    sentPointValue.getStamp());
            Require
                .equal(
                    receivedPointValue.getValue(),
                    sentPointValue.getValue());
        }

        final Point numericPoint2 = storeMetadata
            .getPointByName(_NUMERIC_POINT_2_NAME)
            .get();

        sentPointValue = new PointValue(
            numericPoint2,
            Optional.of(_startStamp.before(1 + ElapsedTime.MINUTE.toRaw())),
            null,
            Double.valueOf(2.0));
        updateSender.send(sentPointValue);
        updateSender.commit();

        receivedPointValue = (PointValue) noticeReceiver.receive(timeout);
        Require.equal(receivedPointValue, sentPointValue);

        if (replicatedReceiver != null) {
            receivedPointValue = (PointValue) replicatedReceiver
                .receive(timeout);
            Require.equal(receivedPointValue, sentPointValue);
        }

        sentPointValue = new VersionedValue.Deleted(sentPointValue);

        updateSender.send(sentPointValue);
        updateSender.commit();

        receivedPointValue = (PointValue) noticeReceiver.receive(timeout);
        Require.equal(receivedPointValue, sentPointValue);

        if (replicatedReceiver != null) {
            receivedPointValue = (PointValue) replicatedReceiver
                .receive(timeout);
            Require.equal(receivedPointValue, sentPointValue);
        }

        noticeReceiver.commit();
        noticeReceiver.close();

        if (replicatedReceiver != null) {
            replicatedReceiver.commit();
            replicatedReceiver.close();
        }

        if (replicateReceiver != null) {
            replicateReceiver.commit();
            replicateReceiver.close();
        }

        updateSender.close();
    }

    /**
     * Tests store session proxy.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testStoreQueues")
    public void testStoreSessionProxy()
        throws Exception
    {
        final boolean storeReplicates = storeReplicates(_storeService);
        final boolean storeDropsDeleted = storeDropsDeleted(_storeService);
        final StoreSessionProxy storeProxy = getStoreProxy(_storeService);
        final Metadata metadata = getMetadata();
        final VersionedValue pullValue;
        Point point;
        PointBinding[] pointBindings;
        StoreValuesQuery.Builder storeQueryBuilder;
        StoreValues storeResponse;
        PointValue updateValue;

        point = metadata.getPointByName(_TEXT_POINT_1_NAME).get();

        final String pointName = point.getName().get();
        final UUID pointUUID = point.getUUID().get();

        updateValue = new PointValue(
            point,
            Optional.of(
                DateTime.now().floored(
                    ElapsedTime.MINUTE).before(1 * ElapsedTime.MINUTE.toRaw())),
            null,
            _TEXT_VALUE_1);
        Require
            .success(storeProxy.updateAndCheck(updateValue, getThisLogger()));

        _supportsPull = storeProxy.supportsPull();
        getThisLogger()
            .debug(
                CoreTestsMessages.SUPPORTS_PULL,
                Boolean.valueOf(_supportsPull));

        // Tests a pull query.
        if (_supportsPull) {
            storeQueryBuilder = StoreValuesQuery.newBuilder();
            storeQueryBuilder.setNotBefore(_startStamp);
            storeQueryBuilder.setIncludeDeleted(true);
            storeResponse = storeProxy.select(storeQueryBuilder.build()).get();
            Require
                .success(
                    storeResponse.size()
                    == 2 + (storeReplicates? 1: 0) + (storeDropsDeleted? 0: 1));

            final PointValue lastPointValue = storeResponse
                .getLastPointValue()
                .get();

            pullValue = (VersionedValue) lastPointValue.restore(metadata);
            Require.equal(pullValue, updateValue);
        } else {
            pullValue = null;
        }

        // Tests point bindings queries.

        final PointBinding.Request.Builder requestBuilder = PointBinding.Request
            .newBuilder();

        requestBuilder.selectName(pointName);
        pointBindings = storeProxy
            .getPointBindings(requestBuilder.build())
            .get();
        Require.success(pointBindings.length == 1);
        Require.equal(pointUUID, pointBindings[0].getUUID());
        requestBuilder.selectWild(pointName + '*');
        pointBindings = storeProxy
            .getPointBindings(requestBuilder.build())
            .get();
        Require.success(pointBindings.length == 1);
        Require.equal(pointUUID, pointBindings[0].getUUID());

        requestBuilder.selectUUID(pointUUID);
        pointBindings = storeProxy
            .getPointBindings(requestBuilder.build())
            .get();
        Require.success(pointBindings.length == 1);
        Require.equal(pointUUID, pointBindings[0].getUUID());
        Require.equal(pointName, pointBindings[0].getName());

        storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPointUUID(pointBindings[0].getUUID());
        storeResponse = storeProxy.select(storeQueryBuilder.build()).get();

        final PointValue responseValue = storeResponse.getPointValue().get();

        Require.equal(responseValue.getPointUUID(), pointUUID);
        Require.equal(responseValue.getValue(), _TEXT_VALUE_1);

        updateValue = new PointValue(
            point,
            Optional.of(DateTime.now().floored(ElapsedTime.MINUTE)),
            null,
            _TEXT_VALUE_2);
        Require
            .success(storeProxy.updateAndCheck(updateValue, getThisLogger()));

        storeResponse = storeProxy.select(storeQueryBuilder.build()).get();
        storeResponse.restore(metadata);
        Require.equal(storeResponse.getPointValue().get(), updateValue);

        // Tests that the updated value is returned by pull.
        if (pullValue != null) {
            storeQueryBuilder = StoreValuesQuery
                .newBuilder()
                .setPointUUID(pullValue.getPointUUID());
            storeQueryBuilder.setAfter(pullValue.getVersion());
            storeQueryBuilder.setPull(true);
            storeResponse = storeProxy.select(storeQueryBuilder.build()).get();
            storeResponse.restore(metadata);

            final PointValue pointValue = storeResponse.getPointValue().get();

            Require.equal(pointValue, updateValue);
        }

        // Tests a count query.
        if (storeProxy.supportsCount()) {
            storeQueryBuilder = StoreValuesQuery
                .newBuilder()
                .setPoint(metadata.getPointByName(_NUMERIC_POINT_1_NAME).get());
            storeQueryBuilder.setNotBefore(_startStamp);
            storeQueryBuilder.setAll(true).setCount(true);
            storeResponse = storeProxy.select(storeQueryBuilder.build()).get();
            Require.success(storeResponse.size() == 0);
            Require.success(storeResponse.getCount() == 1);
        }

        // Tests requests on numeric values.

        point = metadata.getPointByName(_HOUR_POINT_NAME).get();
        updateValue = new PointValue(
            point,
            Optional.of(DateTime.now().floored(ElapsedTime.MINUTE)),
            null,
            Double.valueOf(2.0));
        Require
            .success(storeProxy.updateAndCheck(updateValue, getThisLogger()));
        storeQueryBuilder = StoreValuesQuery.newBuilder().setPoint(point);
        storeResponse = storeProxy.select(storeQueryBuilder.build()).get();
        Require.success(storeResponse.size() == 1);

        PointValue pointValue = storeResponse.getPointValue().get();

        Require.equal(pointValue.getValue(), Double.valueOf(2.0));
        storeQueryBuilder.setNormalized(true);
        storeResponse = storeProxy.select(storeQueryBuilder.build()).get();
        Require.success(storeResponse.size() == 1);

        pointValue = storeResponse.getPointValue().get();

        Require.equal(pointValue.getValue(), Double.valueOf(7200.0));

        // Tests requests for an unknown point.

        updateValue = new PointValue(
            _UNKNOWN_POINT_UUID,
            Optional.of(DateTime.now()),
            null,
            Integer.valueOf(1234));
        Require
            .success(storeProxy.updateAndCheck(updateValue, getThisLogger()));
        storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPointUUID(_UNKNOWN_POINT_UUID);
        storeResponse = storeProxy.select(storeQueryBuilder.build()).get();
        Require.success(storeResponse.isSuccess());
        Require.success(storeResponse.size() == 1);

        pointValue = storeResponse.getPointValue().get();

        Require.equal(pointValue.getValue(), Integer.valueOf(1234));

        storeProxy.disconnect();
    }

    /**
     * Test value types.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testStoreSessionProxy")
    public void testValueTypes()
        throws Exception
    {
        _storeProxy = getStoreProxy(_storeService);

        final EnumSet<Externalizer.ValueType> valueTypes = _storeProxy
            .supportedValueTypes();

        for (final Externalizer.ValueType valueType: valueTypes) {
            switch (valueType) {
                case BIG_DECIMAL: {
                    _logValueClass(
                        Externalizer.ValueType.BIG_DECIMAL,
                        BigDecimal.class);
                    _verify(
                        new BigDecimal("3.14159265358979323846264338327950"),
                        _BIG_DECIMAL_POINT_NAME);

                    break;
                }
                case BIG_INTEGER: {
                    _logValueClass(
                        Externalizer.ValueType.BIG_INTEGER,
                        BigInteger.class);
                    _verify(
                        new BigInteger("314159265358979323846264338327950"),
                        _BIG_INTEGER_POINT_NAME);

                    break;
                }
                case BIG_RATIONAL: {
                    _logValueClass(
                        Externalizer.ValueType.BIG_RATIONAL,
                        BigRational.class);
                    _verify(
                        BigRational
                            .valueOf("3/14159265358979323846264338327950"),
                        _BIG_RATIONAL_POINT_NAME);

                    break;
                }
                case BOOLEAN: {
                    _logValueClass(
                        Externalizer.ValueType.BOOLEAN,
                        Boolean.class);
                    _verify(Boolean.TRUE, _BOOLEAN_POINT_NAME);

                    break;
                }
                case BYTE_ARRAY: {
                    _logValueClass(
                        Externalizer.ValueType.BYTE_ARRAY,
                        byte[].class);
                    _verify(
                        new byte[] {3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 8, 9, 7,
                                9, },
                        _BYTE_ARRAY_POINT_NAME);

                    break;
                }
                case TUPLE: {
                    final double[] array = new double[] {Math.PI, 826535, 8979};
                    final Tuple tuple = new Tuple(array.length);

                    _logValueClass(Externalizer.ValueType.TUPLE, Tuple.class);

                    for (final double item: array) {
                        tuple.add(Double.valueOf(item));
                    }

                    _verify(tuple, _TUPLE_POINT_NAME);

                    break;
                }
                case COMPLEX: {
                    _logValueClass(
                        Externalizer.ValueType.COMPLEX,
                        Complex.class);
                    _verify(Complex.valueOf("3+1416i"), _COMPLEX_POINT_NAME);

                    break;
                }
                case DICT: {
                    final long[] array = new long[] {31415, 826535, 8979};
                    final Dict dict = new Dict();

                    _logValueClass(Externalizer.ValueType.DICT, Dict.class);

                    for (final long item: array) {
                        dict.put(Long.toString(item), Long.valueOf(item));
                    }

                    _verify(dict, _DICT_POINT_NAME);

                    break;
                }
                case RATIONAL: {
                    _logValueClass(
                        Externalizer.ValueType.RATIONAL,
                        Rational.class);
                    _verify(Rational.valueOf(1, 3), _RATIONAL_POINT_NAME);

                    break;
                }
                default: {
                    break;
                }
            }
        }

        _storeProxy.disconnect();
        _storeProxy = null;
    }

    private void _logValueClass(
            final Externalizer.ValueType valueType,
            final Class<?> valueClass)
    {
        getThisLogger()
            .debug(
                TestsMessages.VALUE_CLASS,
                valueType.name(),
                Character.valueOf((char) valueType.getCode()),
                valueClass);
    }

    private void _verify(
            final Serializable value,
            final String pointName)
        throws Exception
    {
        final Point point = getPoint(pointName);
        final DateTime stamp = _startStamp
            .floored(ElapsedTime.MINUTE)
            .before(1 * ElapsedTime.MINUTE.toRaw());

        Require
            .success(
                _storeProxy
                    .updateAndCheck(
                            new PointValue(
                                    point,
                                            Optional.of(stamp),
                                            null,
                                            value),
                                    getThisLogger()));

        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder()
            .setPoint(point);

        storeQueryBuilder.setAt(stamp);

        final StoreValues storeResponse = _storeProxy
            .select(storeQueryBuilder.build())
            .get();
        final PointValue pointValue = storeResponse.getPointValue().get();

        Require.equal(pointValue.getPointUUID(), point.getUUID().orElse(null));
        Require.equal(pointValue.getStamp(), stamp);
        Require.equal(null, pointValue.getState());

        if (value instanceof byte[]) {
            Require
                .success(
                    Arrays
                        .equals(
                                (byte[]) pointValue.getValue(),
                                        (byte[]) value));
        } else {
            Require.equal(pointValue.getValue(), value);
        }

        Require
            .success(
                _storeProxy
                    .updateAndCheck(
                            new VersionedValue.Purged(pointValue),
                                    getThisLogger()));
    }

    private static final String _BIG_DECIMAL_POINT_NAME = "TESTS.BIG.DECIMAL";
    private static final String _BIG_INTEGER_POINT_NAME = "TESTS.BIG.INTEGER";
    private static final String _BIG_RATIONAL_POINT_NAME = "TESTS.BIG.RATIONAL";
    private static final String _BOOLEAN_POINT_NAME = "TESTS.BOOLEAN";
    private static final String _BYTE_ARRAY_POINT_NAME = "TESTS.BYTE.ARRAY";
    private static final String _COMPLEX_POINT_NAME = "TESTS.COMPLEX";
    private static final String _DICT_POINT_NAME = "TESTS.DICT";
    private static final String _HOUR_POINT_NAME = "TESTS.HOUR";
    private static final String _NUMERIC_POINT_1_NAME = "TESTS.NUMERIC.01";
    private static final String _NUMERIC_POINT_2_NAME = "TESTS.NUMERIC.02";
    private static final String _RATIONAL_POINT_NAME = "TESTS.RATIONAL";
    private static final String _REPLICATE_POINT_1_NAME = "TESTS.REPLICATE.01";
    private static final String _STATE_POINT_NAME = "TESTS.STATE.01";
    private static final String _TEXT_POINT_1_NAME = "TESTS.TEXT.01";
    private static final String _TEXT_POINT_2_NAME = "TESTS.TEXT.02";
    private static final String _TEXT_VALUE_1 = "FIRST";
    private static final String _TEXT_VALUE_2 = "SECOND";
    private static final String _TUPLE_POINT_NAME = "TESTS.TUPLE";
    private static final UUID _UNKNOWN_POINT_UUID = UUID
        .fromString("ea7ed80b-0721-4031-8bc6-91884d776e1d")
        .get();

    private DateTime _startStamp;
    private StoreSessionProxy _storeProxy;
    private ServiceActivator _storeService;
    private boolean _supportsPull;
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
