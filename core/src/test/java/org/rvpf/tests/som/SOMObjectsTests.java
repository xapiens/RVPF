/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMObjectsTests.java 4100 2019-06-29 17:22:34Z SFB $
 */

package org.rvpf.tests.som;

import java.io.Serializable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.som.SOMServer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.SignalTarget;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ServiceRegistry;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.som.queue.FilesQueue;
import org.rvpf.som.queue.QueueServerImpl;
import org.rvpf.som.queue.ReceiverWrapper;
import org.rvpf.som.queue.SenderWrapper;
import org.rvpf.som.topic.PublisherWrapper;
import org.rvpf.som.topic.SubscriberWrapper;
import org.rvpf.som.topic.TopicServerImpl;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * SOM objects tests.
 */
public final class SOMObjectsTests
    extends ServiceTests
{
    /**
     * Sets up the configuration for the class.
     */
    @BeforeClass
    public void setUp()
    {
        loadConfig();
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
        QueueServerImpl queueServer;
        KeyedGroups properties;
        SenderWrapper sender;
        ReceiverWrapper receiver;
        PointValue pointValue;

        Require.success(ServiceRegistry.setUp(getConfig().getProperties()));

        // Tests a memory based queue.

        queueServer = new QueueServerImpl(Optional.empty());
        properties = new SOMFactory.Properties();
        properties.setValue(FilesQueue.AUTOCOMMIT_PROPERTY, "");
        properties.setValue(SOMServer.NAME_PROPERTY, TESTS_QUEUE);
        properties.setValue(QueueServerImpl.MEMORY_PROPERTY, "");
        properties.freeze();

        quell(ServiceMessages.QUEUE_LENGTH_CHANGED);
        Require
            .success(
                queueServer.setUp(getConfig(), properties),
                "Queue set up");

        Require.success(queueServer.getInfo().getSenderCount() == 0);
        Require.notPresent(queueServer.getInfo().getReceiverConnectTime());
        sender = queueServer.createSenderWrapper();
        Require.success(queueServer.getInfo().getSenderCount() == 1);
        receiver = queueServer.createReceiverWrapper();
        Require.present(queueServer.getInfo().getReceiverConnectTime());
        Require.success(queueServer.getInfo().getMessageCount() == 0);
        Require.success(queueServer.getInfo().getFileCount() == 0);
        Require.success(queueServer.getInfo().getFilesSize() == 0);
        Require.notPresent(queueServer.getInfo().getLastSenderCommit());
        Require.notPresent(queueServer.getInfo().getLastReceiverCommit());

        pointValue = new PointValue(
            UUID.generate(),
            Optional.of(DateTime.now()),
            null,
            Long.valueOf(1));
        sender.send(new Serializable[] {pointValue}, false);

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
        Require.present(queueServer.getInfo().getLastSenderCommit());
        Require.notPresent(queueServer.getInfo().getLastReceiverCommit());

        pointValue = (PointValue) receiver.receive(1, 0)[0];
        Require.equal(pointValue.getValue(), Long.valueOf(1));

        pointValue = (PointValue) receiver.receive(1, 0)[0];
        Require.equal(pointValue.getValue(), Long.valueOf(2));

        receiver.commit();
        Require.success(queueServer.getInfo().getMessageCount() == 1);
        Require.notNull(queueServer.getInfo().getLastSenderCommit());
        Require.present(queueServer.getInfo().getLastReceiverCommit());

        sender.close();
        receiver.close();
        Require.success(queueServer.getInfo().getSenderCount() == 0);
        Require.notPresent(queueServer.getInfo().getReceiverConnectTime());

        queueServer.tearDown();

        // Tests a files based queue.

        final long initialQueueLength;
        final long purged;

        queueServer = new QueueServerImpl(Optional.empty());
        properties = new SOMFactory.Properties();
        properties.setValue(FilesQueue.AUTOCOMMIT_PROPERTY, "");
        properties.setValue(SOMServer.NAME_PROPERTY, TESTS_QUEUE);
        properties.freeze();

        quell(ServiceMessages.QUEUE_LENGTH_CHANGED);
        Require
            .success(
                queueServer.setUp(getConfig(), properties),
                "Queue set up");

        Require.success(queueServer.getInfo().getSenderCount() == 0);
        Require.notPresent(queueServer.getInfo().getReceiverConnectTime());
        initialQueueLength = queueServer.getInfo().getMessageCount();
        sender = queueServer.createSenderWrapper();
        Require.success(queueServer.getInfo().getSenderCount() == 1);
        receiver = queueServer.createReceiverWrapper();
        Require.present(queueServer.getInfo().getReceiverConnectTime());
        purged = receiver.purge();
        Require.success(purged == initialQueueLength);
        Require.success(queueServer.getInfo().getMessageCount() == 0);
        Require.success(queueServer.getInfo().getFileCount() == 0);
        Require.success(queueServer.getInfo().getFilesSize() == 0);
        Require.notPresent(queueServer.getInfo().getLastSenderCommit());
        Require.notPresent(queueServer.getInfo().getLastReceiverCommit());

        pointValue = new PointValue(
            UUID.generate(),
            Optional.of(DateTime.now()),
            null,
            Long.valueOf(1));
        sender.send(new Serializable[] {pointValue}, false);

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
        Require.present(queueServer.getInfo().getLastSenderCommit());
        Require.notPresent(queueServer.getInfo().getLastReceiverCommit());

        pointValue = (PointValue) receiver.receive(1, 0)[0];
        Require.notNull(pointValue);
        Require.equal(pointValue.getValue(), Long.valueOf(1));

        pointValue = (PointValue) receiver.receive(1, 0)[0];
        Require.equal(pointValue.getValue(), Long.valueOf(2));

        receiver.commit();
        Require.success(queueServer.getInfo().getMessageCount() == 1);
        Require.present(queueServer.getInfo().getLastSenderCommit());
        Require.present(queueServer.getInfo().getLastReceiverCommit());

        sender.close();
        receiver.close();
        Require.success(queueServer.getInfo().getSenderCount() == 0);
        Require.notPresent(queueServer.getInfo().getReceiverConnectTime());

        queueServer.tearDown();

        queueServer = new QueueServerImpl(Optional.empty());
        properties = new SOMFactory.Properties();
        properties.setValue(FilesQueue.AUTOCOMMIT_PROPERTY, "");
        properties.setValue(SOMServer.NAME_PROPERTY, TESTS_QUEUE);
        properties.freeze();

        quell(ServiceMessages.QUEUE_LENGTH_CHANGED);
        Require
            .success(
                queueServer.setUp(getConfig(), properties),
                "Queue set up");

        Require.success(queueServer.getInfo().getMessageCount() == 1);
        sender = queueServer.createSenderWrapper();
        receiver = queueServer.createReceiverWrapper();

        pointValue = (PointValue) receiver.receive(1, 0)[0];
        Require.equal(pointValue.getValue(), Long.valueOf(3));

        final LinkedHashMap<String, Object> map = new LinkedHashMap<String,
            Object>();

        map.put("Test key", "Test value");
        pointValue = new PointValue(
            UUID.generate(),
            Optional.of(DateTime.now()),
            null,
            map);
        sender.send(new Serializable[] {pointValue}, false);

        final SenderWrapper otherSender = queueServer.createSenderWrapper();

        Require.success(queueServer.getInfo().getSenderCount() == 2);
        pointValue = new PointValue(
            UUID.generate(),
            Optional.of(DateTime.now()),
            null,
            Long.valueOf(4));
        otherSender.send(new Serializable[] {pointValue}, false);
        otherSender.commit();
        otherSender.close();

        final LinkedList<Object> collection = new LinkedList<Object>();

        collection.add("Test item");
        collection.add(map);

        pointValue = new PointValue(
            UUID.generate(),
            Optional.of(DateTime.now()),
            null,
            collection);
        sender.send(new Serializable[] {pointValue}, false);

        sender.commit();

        pointValue = (PointValue) receiver.receive(1, 0)[0];
        Require.equal(pointValue.getValue(), Long.valueOf(4));

        pointValue = (PointValue) receiver.receive(1, 0)[0];
        Require.success(pointValue.getValue() instanceof Map<?, ?>);

        pointValue = (PointValue) receiver.receive(1, 0)[0];
        Require.success(pointValue.getValue() instanceof Collection<?>);

        final Signal sentSignal = new Signal(
            "TestSignal",
            Optional.of("TestService"),
            Optional.of("TestEntity"),
            Optional.of(UUID.generate()),
            Optional.of(
                new SignalTarget(
                    Optional.of("TestPing"),
                    Optional.of(UUID.generate()),
                    Optional.of("TestReference"))));
        final Signal receivedSignal;

        sentSignal.addVisit(UUID.generate());
        sentSignal.addVisit(UUID.generate());
        sender.send(new Serializable[] {sentSignal}, false);
        sender.commit();
        receivedSignal = (Signal) receiver.receive(1, 0)[0];
        Require.equal(receivedSignal, sentSignal);

        sender.close();
        Require.success(queueServer.getInfo().getSenderCount() == 0);

        receiver.commit();
        receiver.close();

        queueServer.tearDown();
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
        final TopicServerImpl topicServer = new TopicServerImpl(
            Optional.empty());
        final KeyedGroups properties = new SOMFactory.Properties();
        final PublisherWrapper publisher;
        final SubscriberWrapper subscriber;
        Event event;

        properties.setValue(SOMServer.NAME_PROPERTY, TESTS_TOPIC);
        properties.freeze();
        Require
            .success(
                topicServer.setUp(getConfig(), properties),
                "Topic set up");

        Require.success(topicServer.getInfo().getPublisherCount() == 0);
        Require.success(topicServer.getInfo().getSubscriberCount() == 0);
        publisher = topicServer.createPublisherWrapper();
        Require.success(topicServer.getInfo().getPublisherCount() == 1);

        Require.equal(null, topicServer.getInfo().getLastPublish());
        event = new Event(
            "Test",
            Optional.of("Service"),
            Optional.of("Entity"),
            Optional.of(UUID.generate()),
            Optional.empty());
        publisher.send(new Serializable[] {event});
        Require.notNull(topicServer.getInfo().getLastPublish());

        subscriber = topicServer.createSubscriberWrapper();
        Require.success(topicServer.getInfo().getSubscriberCount() == 1);

        event = new Event(
            "Test",
            Optional.of("Service"),
            Optional.of("Entity"),
            Optional.of(UUID.generate()),
            Optional.empty());
        publisher.send(new Serializable[] {event});

        final UUID uuid = event.getSourceUUID().get();

        event = (Event) subscriber.receive(1, 0)[0];
        Require.equal(uuid, event.getSourceUUID().get());

        subscriber.close();
        publisher.close();
        Require.success(topicServer.getInfo().getPublisherCount() == 0);
        Require.success(topicServer.getInfo().getSubscriberCount() == 0);
        topicServer.tearDown();
    }

    /** Tests queue. */
    public static final String TESTS_QUEUE = "TestsNotifier";

    /** Tests topic. */
    public static final String TESTS_TOPIC = "Tests";
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
