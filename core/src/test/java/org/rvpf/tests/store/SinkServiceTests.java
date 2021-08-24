/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SinkServiceTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.store;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.store.Store;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.sink.SinkServiceActivator;
import org.rvpf.tests.MessagingSupport;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Sink service tests.
 */
public final class SinkServiceTests
    extends StoreClientTests
{
    /**
     * Null test.
     *
     * <p>The second start of the service triggers a ClassCastException in RMI
     * on VMS when this test class is run alone.</p>
     *
     * @throws Exception On failure.
     */
    @Test
    public void nullTest()
        throws Exception
    {
        ServiceActivator sinkService;

        setProperty(_TESTS_SERVICE_PROPERTY, "Null");

        sinkService = startService(
            SinkServiceActivator.class,
            Optional.empty());
        stopService(sinkService);

        sinkService = startService(
            SinkServiceActivator.class,
            Optional.empty());
        stopService(sinkService);
    }

    /**
     * Pipe test.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "nullTest")
    public void pipeTest()
        throws Exception
    {
        setProperty(_TESTS_SERVICE_PROPERTY, "Pipe");

        final ServiceActivator sinkService = startService(
            SinkServiceActivator.class,
            Optional.empty());
        final Metadata metadata = getMetadata(sinkService);
        final Store store = getStore(
            metadata
                .getStringValue(StoreServiceAppImpl.STORE_NAME_PROPERTY)
                .get(),
            metadata)
            .get();
        final MessagingSupport.Receiver noticeReceiver;
        Point point;
        PointValue pointValue;

        Require.notNull(store);
        noticeReceiver = getMessaging()
            .createClientReceiver(
                metadata.getPropertiesGroup(NOTIFIER_QUEUE_PROPERTIES));
        noticeReceiver.purge();

        point = metadata.getPointByName(_SINK_POINT_1_NAME).get();
        Require.notNull(point);

        store
            .addUpdate(
                new PointValue(
                    point,
                    Optional.of(DateTime.now()),
                    null,
                    Long.valueOf(1)));
        Require.success(store.sendUpdates());

        pointValue = (PointValue) noticeReceiver.receive(getTimeout());
        Require.equal(pointValue.getPointUUID(), point.getUUID().get());
        Require.equal(null, pointValue.getState());
        Require.equal(pointValue.getValue(), Long.valueOf(1));

        final String state = "State with [] inside";
        final String value = "Value with \" inside";

        point = metadata.getPointByName(_SINK_POINT_2_NAME).get();
        Require.notNull(point);

        store
            .addUpdate(
                new PointValue(
                    point,
                    Optional.of(DateTime.now()),
                    state,
                    value));
        Require.success(store.sendUpdates());

        pointValue = (PointValue) noticeReceiver.receive(getTimeout());
        Require.equal(pointValue.getPointUUID(), point.getUUID().get());
        Require.equal(state, pointValue.getState());
        Require.equal(value, pointValue.getValue());

        noticeReceiver.close();
        store.close();

        stopService(sinkService);
    }

    /**
     * Script test.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "nullTest")
    public void scriptTest()
        throws Exception
    {
        setProperty(_TESTS_SERVICE_PROPERTY, "Script");

        final ServiceActivator sinkService = startService(
            SinkServiceActivator.class,
            Optional.empty());
        final Metadata metadata = getMetadata(sinkService);
        final Store store = getStore(
            metadata
                .getStringValue(StoreServiceAppImpl.STORE_NAME_PROPERTY)
                .get(),
            metadata)
            .get();
        final MessagingSupport.Receiver noticeReceiver;
        Point point;
        PointValue pointValue;

        Require.notNull(store);
        noticeReceiver = getMessaging()
            .createClientReceiver(
                metadata.getPropertiesGroup(NOTIFIER_QUEUE_PROPERTIES));
        noticeReceiver.purge();

        point = metadata.getPointByName(_SINK_POINT_1_NAME).get();
        Require.notNull(point);

        store
            .addUpdate(
                new PointValue(
                    point,
                    Optional.of(DateTime.now()),
                    null,
                    Long.valueOf(1)));
        Require.success(store.sendUpdates());

        pointValue = (PointValue) noticeReceiver.receive(getTimeout());
        Require.equal(pointValue.getPointUUID(), point.getUUID().get());
        Require.equal(null, pointValue.getState());
        Require.equal(pointValue.getValue(), Long.valueOf(1));

        final String state = "Test state";
        final String value = "Test value";

        point = metadata.getPointByName(_SINK_POINT_2_NAME).get();
        Require.notNull(point);

        store
            .addUpdate(
                new PointValue(
                    point,
                    Optional.of(DateTime.now()),
                    state,
                    value));
        Require.success(store.sendUpdates());

        pointValue = (PointValue) noticeReceiver.receive(getTimeout());
        Require.equal(pointValue.getPointUUID(), point.getUUID().get());
        Require.equal(state, pointValue.getState());
        Require.equal(value, pointValue.getValue());

        noticeReceiver.close();
        store.close();

        stopService(sinkService);
    }

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
        setProperty(_TESTS_SERVICE_PROPERTY, "SOM");
        setUpAlerter();
    }

    /**
     * SOM test.
     *
     * @throws Exception On failure.
     */
    @Test
    public void somTest()
        throws Exception
    {
        final Metadata metadata = getMetadata();
        final PointEntity point = (PointEntity) metadata
            .getPointByName(_SINK_POINT_3_NAME)
            .get();
        final StoreEntity storeEntity = point.getStoreEntity().get();
        final MessagingSupport.Receiver updateReceiver = getMessaging()
            .createClientReceiver(
                metadata.getPropertiesGroup(UPDATER_QUEUE_PROPERTIES));
        final Store store;

        Require.success(storeEntity.setUp(metadata));
        store = storeEntity.getStore().get();
        updateReceiver.purge();

        final PointValue sentValue = new PointValue(
            point,
            Optional.of(DateTime.now()),
            null,
            Double.valueOf(Math.PI));

        store.addUpdate(sentValue);
        Require.success(store.sendUpdates());

        final PointValue receivedValue = (PointValue) updateReceiver
            .receive(getTimeout());

        Require.equal(sentValue, receivedValue);

        store.close();
        updateReceiver.commit();
        updateReceiver.close();
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
        tearDownAlerter();
    }

    private static final String _SINK_POINT_1_NAME = "TESTS.SINK.01";
    private static final String _SINK_POINT_2_NAME = "TESTS.SINK.02";
    private static final String _SINK_POINT_3_NAME = "TESTS.SINK.03";
    private static final String _TESTS_SERVICE_PROPERTY = "tests.sink";
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
