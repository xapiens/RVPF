/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMContainerServiceTests.java 4100 2019-06-29 17:22:34Z SFB $
 */

package org.rvpf.tests.som;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.alert.Event;
import org.rvpf.base.som.QueueProxy;
import org.rvpf.base.som.TopicProxy;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceActivator;
import org.rvpf.som.SOMContainerServiceActivator;
import org.rvpf.som.SOMContainerServiceAppImpl;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * SOM server tests.
 */
public final class SOMContainerServiceTests
    extends ServiceTests
{
    /**
     * Sets up the service.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUpService()
        throws Exception
    {
        _service = startService(
            SOMContainerServiceActivator.class,
            Optional.of(TESTS_SOM_SERVICE_NAME));
    }

    /**
     * Tears down the service.
     *
     * @throws Exception On failure.
     */
    @AfterClass(alwaysRun = true)
    public void tearDownService()
        throws Exception
    {
        if (_service != null) {
            stopService(_service);
            _service = null;
        }
    }

    /**
     * Tests a SOM queue.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testQueue()
        throws Exception
    {
        final KeyedGroups configProperties = _service
            .getService()
            .getConfig()
            .getProperties();
        final KeyedGroups queueProperties = configProperties
            .getGroup(SOMContainerServiceAppImpl.SOM_QUEUE_PROPERTIES);
        final QueueProxy.Sender sender = QueueProxy.Sender
            .newBuilder()
            .prepare(
                configProperties,
                queueProperties,
                _SOM_CLIENT_NAME,
                getThisLogger())
            .build();
        final QueueProxy.Receiver receiver = QueueProxy.Receiver
            .newBuilder()
            .prepare(
                configProperties,
                queueProperties,
                _SOM_CLIENT_NAME,
                getThisLogger())
            .build();

        Require.notNull(sender);
        Require.notNull(receiver);

        final QueueProxy queueProxy = QueueProxy
            .newBuilder()
            .prepare(
                configProperties,
                queueProperties,
                _SOM_CLIENT_NAME,
                getThisLogger())
            .build();

        Require.notPresent(queueProxy.getInfo().getReceiverConnectTime());
        receiver.connect();
        Require.present(queueProxy.getInfo().getReceiverConnectTime());

        final long initialQueueLength = queueProxy.getInfo().getMessageCount();
        final long purged = receiver.purge();

        Require.success(purged == initialQueueLength);

        PointValue pointValue;

        pointValue = new PointValue(
            UUID.generate(),
            Optional.of(DateTime.now()),
            null,
            Long.valueOf(1));
        Require.success(queueProxy.getInfo().getSenderCount() == 0);
        sender.connect();
        sender.send(new Serializable[] {pointValue}, false);
        Require.success(queueProxy.getInfo().getSenderCount() == 1);

        sender.commit();

        pointValue = new PointValue(
            UUID.generate(),
            Optional.of(DateTime.now()),
            null,
            Long.valueOf(2));
        sender.send(new Serializable[] {pointValue}, false);

        pointValue = new PointValue(
            UUID.generate(),
            Optional.of(DateTime.now()),
            null,
            Long.valueOf(3));
        sender.send(new Serializable[] {pointValue}, false);

        sender.commit();

        pointValue = Require.notNull((PointValue) receiver.receive(1, 0)[0]);
        Require.equal(pointValue.getValue(), Long.valueOf(1));

        pointValue = Require.notNull((PointValue) receiver.receive(1, 0)[0]);
        Require.equal(pointValue.getValue(), Long.valueOf(2));

        receiver.commit();
        Require.success(queueProxy.getInfo().getMessageCount() == 1);
        Require.success(queueProxy.getInfo().getSenderCount() == 1);

        sender.tearDown();
        Require.success(queueProxy.getInfo().getSenderCount() == 0);
        receiver.purge();
        Require.success(queueProxy.getInfo().getMessageCount() == 0);
        Require.present(queueProxy.getInfo().getReceiverConnectTime());
        receiver.tearDown();
        Require.notPresent(queueProxy.getInfo().getReceiverConnectTime());
        queueProxy.tearDown();
    }

    /**
     * Tests a SOM topic.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testTopic()
        throws Exception
    {
        final KeyedGroups configProperties = _service
            .getService()
            .getConfig()
            .getProperties();
        final KeyedGroups topicProperties = configProperties
            .getGroup(SOMContainerServiceAppImpl.SOM_TOPIC_PROPERTIES);
        final TopicProxy.Publisher publisher = TopicProxy.Publisher
            .newBuilder()
            .prepare(
                configProperties,
                topicProperties,
                _SOM_CLIENT_NAME,
                getThisLogger())
            .build();
        final TopicProxy.Subscriber subscriber = TopicProxy.Subscriber
            .newBuilder()
            .prepare(
                configProperties,
                topicProperties,
                _SOM_CLIENT_NAME,
                getThisLogger())
            .build();

        Require.notNull(publisher);
        Require.notNull(subscriber);

        final TopicProxy topicProxy = TopicProxy
            .newBuilder()
            .prepare(
                configProperties,
                topicProperties,
                _SOM_CLIENT_NAME,
                getThisLogger())
            .build();

        Require.success(topicProxy.getInfo().getPublisherCount() == 0);
        publisher.connect();
        Require.success(topicProxy.getInfo().getPublisherCount() == 1);

        Event event;

        event = new Event(
            "Test",
            Optional.of("Service"),
            Optional.of("Entity"),
            Optional.of(UUID.generate()),
            Optional.empty());
        publisher.send(new Serializable[] {event});

        Require.success(topicProxy.getInfo().getSubscriberCount() == 0);
        subscriber.connect();
        Require.success(topicProxy.getInfo().getSubscriberCount() == 1);

        event = new Event(
            "Test",
            Optional.of("Service"),
            Optional.of("Entity"),
            Optional.of(UUID.generate()),
            Optional.empty());
        publisher.send(new Serializable[] {event});

        final UUID uuid = event.getSourceUUID().get();

        event = Require.notNull((Event) subscriber.receive(1, 0)[0]);
        Require.equal(uuid, event.getSourceUUID().get());
        Require.success(topicProxy.getInfo().getPublisherCount() == 1);

        publisher.tearDown();
        Require.success(topicProxy.getInfo().getPublisherCount() == 0);
        Require.success(topicProxy.getInfo().getSubscriberCount() == 1);
        subscriber.tearDown();
        Require.success(topicProxy.getInfo().getSubscriberCount() == 0);
        topicProxy.tearDown();
    }

    /** Tests SOM service name. */
    public static final String TESTS_SOM_SERVICE_NAME = "Tests";

    /**  */

    private static final String _SOM_CLIENT_NAME = "Tests";

    private ServiceActivator _service;
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
