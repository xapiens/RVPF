/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMSupport.java 4100 2019-06-29 17:22:34Z SFB $
 */

package org.rvpf.tests.som;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.som.QueueProxy;
import org.rvpf.base.som.TopicProxy;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.rmi.SessionFactoryImpl;
import org.rvpf.service.rmi.SessionSecurityContext;
import org.rvpf.som.queue.QueueServerImpl;
import org.rvpf.som.queue.ReceiverWrapper;
import org.rvpf.som.queue.SenderWrapper;
import org.rvpf.som.topic.PublisherWrapper;
import org.rvpf.som.topic.SubscriberWrapper;
import org.rvpf.som.topic.TopicServerImpl;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.service.ServiceTests;

/**
 * SOM support.
 */
public final class SOMSupport
    implements MessagingSupport
{
    /** {@inheritDoc}
     */
    @Override
    public Publisher createClientPublisher(
            final KeyedGroups topicProperties)
        throws Exception
    {
        return new _ClientPublisher(topicProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    public Publisher createClientPublisher(final String name)
        throws Exception
    {
        return createClientPublisher(_somProperties(name));
    }

    /** {@inheritDoc}
     */
    @Override
    public Receiver createClientReceiver(
            final KeyedGroups queueProperties)
        throws Exception
    {
        return new _ClientReceiver(queueProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    public Receiver createClientReceiver(final String name)
        throws Exception
    {
        return createClientReceiver(_somProperties(name));
    }

    /** {@inheritDoc}
     */
    @Override
    public Sender createClientSender(
            final KeyedGroups queueProperties)
        throws Exception
    {
        return new _ClientSender(queueProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    public Sender createClientSender(final String name)
        throws Exception
    {
        return createClientSender(_somProperties(name));
    }

    /** {@inheritDoc}
     */
    @Override
    public Subscriber createClientSubscriber(
            final KeyedGroups topicProperties)
        throws Exception
    {
        return new _ClientSubscriber(topicProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    public Subscriber createClientSubscriber(final String name)
        throws Exception
    {
        return createClientSubscriber(_somProperties(name));
    }

    /** {@inheritDoc}
     */
    @Override
    public Publisher createServerPublisher(
            final KeyedGroups topicProperties)
        throws Exception
    {
        return new _ServerPublisher(topicProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    public Publisher createServerPublisher(final String name)
        throws Exception
    {
        return createServerPublisher(_somServerProperties(name));
    }

    /** {@inheritDoc}
     */
    @Override
    public Receiver createServerReceiver(
            final KeyedGroups queueProperties)
        throws Exception
    {
        return new _ServerReceiver(queueProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    public Receiver createServerReceiver(final String name)
        throws Exception
    {
        return createServerReceiver(_somServerProperties(name));
    }

    /** {@inheritDoc}
     */
    @Override
    public Sender createServerSender(
            final KeyedGroups queueProperties)
        throws Exception
    {
        return new _ServerSender(queueProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    public Sender createServerSender(final String name)
        throws Exception
    {
        return createServerSender(_somServerProperties(name));
    }

    /** {@inheritDoc}
     */
    @Override
    public Subscriber createServerSubscriber(
            final KeyedGroups topicProperties)
        throws Exception
    {
        return new _ServerSubscriber(topicProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    public Subscriber createServerSubscriber(final String name)
        throws Exception
    {
        return createServerSubscriber(_somServerProperties(name));
    }

    /** {@inheritDoc}
     */
    @Override
    public void setUp(final Config config, final ServiceTests client)
    {
        final Logger logger = Logger.getInstance(client.getClass());

        _config = config;
        _clientName = client.getClass().getName();

        _securityContext = SessionFactoryImpl
            .createSecurityContext(
                config.getProperties(),
                KeyedGroups.MISSING_KEYED_GROUP,
                logger);
        Require.notNull(_securityContext);
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _securityContext = null;
        _clientName = null;
    }

    /**
     * Gets the client name.
     *
     * @return The client name.
     */
    String getClientName()
    {
        return _clientName;
    }

    /**
     * Gets the config.
     *
     * @return The config.
     */
    Config getConfig()
    {
        return _config;
    }

    /**
     * Gets the security context.
     *
     * @return The security context.
     */
    SessionSecurityContext getSecurityContext()
    {
        return _securityContext;
    }

    private static KeyedGroups _somProperties(final String name)
    {
        final KeyedGroups properties = new KeyedGroups();

        properties.add(_NAME_PROPERTY, name);

        return properties;
    }

    private static KeyedGroups _somServerProperties(final String name)
    {
        final KeyedGroups properties = _somProperties(name);

        properties.add(_SERVER_PROPERTY, "");

        return properties;
    }

    private static final String _NAME_PROPERTY = "name";
    private static final String _SERVER_PROPERTY = "server";

    private String _clientName;
    private Config _config;
    private SessionSecurityContext _securityContext;

    private final class _ClientPublisher
        implements Publisher
    {
        /**
         * Constructs an instance.
         *
         * @param topicProperties The topic properties.
         *
         * @throws Exception On failure.
         */
        _ClientPublisher(final KeyedGroups topicProperties)
            throws Exception
        {
            _publisher = TopicProxy.Publisher
                .newBuilder()
                .prepare(
                    getConfig().getProperties(),
                    topicProperties,
                    getClientName(),
                    Logger.getInstance(getClientName()))
                .build();

            _publisher.connect();
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws Exception
        {
            _publisher.tearDown();
            _publisher = null;
        }

        /** {@inheritDoc}
         */
        @Override
        public void open()
            throws Exception
        {
            _publisher.connect();
        }

        /** {@inheritDoc}
         */
        @Override
        public void send(final Serializable content)
            throws Exception
        {
            _publisher.send(new Serializable[] {content});
        }

        private TopicProxy.Publisher _publisher;
    }


    private final class _ClientReceiver
        implements Receiver
    {
        /**
         * Constructs an instance.
         *
         * @param queueProperties The queue properties.
         *
         * @throws Exception On failure.
         */
        _ClientReceiver(final KeyedGroups queueProperties)
            throws Exception
        {
            _receiver = QueueProxy.Receiver
                .newBuilder()
                .prepare(
                    getConfig().getProperties(),
                    queueProperties,
                    getClientName(),
                    Logger.getInstance(getClientName()))
                .build();

            _receiver.connect();
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws Exception
        {
            _receiver.tearDown();
        }

        /** {@inheritDoc}
         */
        @Override
        public void commit()
            throws Exception
        {
            _receiver.commit();
        }

        /** {@inheritDoc}
         */
        @Override
        public void open()
            throws Exception
        {
            _receiver.connect();
        }

        /** {@inheritDoc}
         */
        @Override
        public long purge()
            throws Exception
        {
            return _receiver.purge();
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable receive(final long timeout)
            throws Exception
        {
            final Serializable[] messages = _receiver.receive(1, timeout);

            return (messages.length > 0)? messages[0]: null;
        }

        private final QueueProxy.Receiver _receiver;
    }


    private final class _ClientSender
        implements Sender
    {
        /**
         * Constructs an instance.
         *
         * @param queueProperties The queue properties.
         *
         * @throws Exception On failure.
         */
        _ClientSender(final KeyedGroups queueProperties)
            throws Exception
        {
            _sender = QueueProxy.Sender
                .newBuilder()
                .prepare(
                    getConfig().getProperties(),
                    queueProperties,
                    getClientName(),
                    Logger.getInstance(getClientName()))
                .build();

            _sender.connect();
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws Exception
        {
            _sender.tearDown();
        }

        /** {@inheritDoc}
         */
        @Override
        public void commit()
            throws Exception
        {
            _sender.commit();
        }

        /** {@inheritDoc}
         */
        @Override
        public void open()
            throws Exception
        {
            _sender.connect();
        }

        /** {@inheritDoc}
         */
        @Override
        public void send(final Serializable content)
            throws Exception
        {
            _sender.send(new Serializable[] {content}, false);
        }

        private final QueueProxy.Sender _sender;
    }


    private final class _ClientSubscriber
        implements Subscriber
    {
        /**
         * Constructs an instance.
         *
         * @param topicProperties The topic properties.
         *
         * @throws Exception On failure.
         */
        _ClientSubscriber(final KeyedGroups topicProperties)
            throws Exception
        {
            _subscriber = TopicProxy.Subscriber
                .newBuilder()
                .prepare(
                    getConfig().getProperties(),
                    topicProperties,
                    getClientName(),
                    Logger.getInstance(getClientName()))
                .build();

            _subscriber.connect();
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws Exception
        {
            _subscriber.tearDown();
            _subscriber = null;
        }

        /** {@inheritDoc}
         */
        @Override
        public void open()
            throws Exception
        {
            _subscriber.connect();
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable receive(final long timeout)
            throws Exception
        {
            final Serializable[] messages = _subscriber.receive(1, timeout);

            return (messages.length > 0)? messages[0]: null;
        }

        private TopicProxy.Subscriber _subscriber;
    }


    private final class _ServerPublisher
        implements Publisher
    {
        /**
         * Constructs an instance.
         *
         * @param topicProperties The topic properties.
         */
        _ServerPublisher(final KeyedGroups topicProperties)
        {
            final TopicServerImpl topicServer = new TopicServerImpl(
                Optional.of(getSecurityContext()));

            Require.success(topicServer.setUp(getConfig(), topicProperties));

            _publisher = topicServer.createPublisherWrapper();
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws Exception
        {
            _publisher.getTopicServer().stop(-1);
            _publisher.tearDownServer();
            _publisher.close();
        }

        /** {@inheritDoc}
         */
        @Override
        public void open()
            throws Exception {}

        /** {@inheritDoc}
         */
        @Override
        public void send(final Serializable content)
            throws Exception
        {
            _publisher.send(new Serializable[] {content});
        }

        private final PublisherWrapper _publisher;
    }


    private final class _ServerReceiver
        implements Receiver
    {
        /**
         * Constructs an instance.
         *
         * @param queueProperties The queue properties.
         *
         * @throws Exception On failure.
         */
        _ServerReceiver(final KeyedGroups queueProperties)
            throws Exception
        {
            final QueueServerImpl queueServer = new QueueServerImpl(
                Optional.of(getSecurityContext()));

            Require.success(queueServer.setUp(getConfig(), queueProperties));

            _receiver = queueServer.createReceiverWrapper();
            _receiver.purge();
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws Exception
        {
            _receiver.getQueueServer().stop(-1);
            _receiver.tearDownServer();
            _receiver.close();
            _receiver = null;
        }

        /** {@inheritDoc}
         */
        @Override
        public void commit()
            throws Exception
        {
            _receiver.commit();
        }

        /** {@inheritDoc}
         */
        @Override
        public void open()
            throws Exception {}

        /** {@inheritDoc}
         */
        @Override
        public long purge()
            throws Exception
        {
            return _receiver.purge();
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable receive(final long timeout)
            throws Exception
        {
            final Serializable[] messages = _receiver.receive(1, timeout);

            return (messages.length > 0)? messages[0]: null;
        }

        private ReceiverWrapper _receiver;
    }


    private final class _ServerSender
        implements Sender
    {
        /**
         * Constructs an instance.
         *
         * @param queueProperties The queue properties.
         *
         * @throws Exception On failure.
         */
        _ServerSender(final KeyedGroups queueProperties)
            throws Exception
        {
            final QueueServerImpl queueServer = new QueueServerImpl(
                Optional.of(getSecurityContext()));
            final ReceiverWrapper receiver;

            Require.success(queueServer.setUp(getConfig(), queueProperties));

            receiver = queueServer.createReceiverWrapper();
            receiver.purge();
            receiver.close();

            _sender = queueServer.createSenderWrapper();
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws Exception
        {
            _sender.getQueueServer().close();
            _sender.tearDownServer();
            _sender.close();
            _sender = null;
        }

        /** {@inheritDoc}
         */
        @Override
        public void commit()
            throws Exception
        {
            _sender.commit();
        }

        /** {@inheritDoc}
         */
        @Override
        public void open()
            throws Exception {}

        /** {@inheritDoc}
         */
        @Override
        public void send(final Serializable content)
            throws Exception
        {
            _sender.send(new Serializable[] {content}, false);
        }

        private SenderWrapper _sender;
    }


    private final class _ServerSubscriber
        implements Subscriber
    {
        /**
         * Constructs an instance.
         *
         * @param topicProperties The topic properties.
         */
        _ServerSubscriber(final KeyedGroups topicProperties)
        {
            final TopicServerImpl topicServer = new TopicServerImpl(
                Optional.of(getSecurityContext()));

            Require.success(topicServer.setUp(getConfig(), topicProperties));

            _subscriber = topicServer.createSubscriberWrapper();
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws Exception
        {
            _subscriber.getTopicServer().stop(-1);
            _subscriber.tearDownServer();
            _subscriber.close();
        }

        /** {@inheritDoc}
         */
        @Override
        public void open()
            throws Exception {}

        /** {@inheritDoc}
         */
        @Override
        public Serializable receive(final long timeout)
            throws Exception
        {
            return _subscriber.receive(1, timeout)[0];
        }

        private final SubscriberWrapper _subscriber;
    }
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
