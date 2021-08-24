/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMFactory.java 4081 2019-06-14 14:54:30Z SFB $
 */

package org.rvpf.service.som;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.som.QueueProxy;
import org.rvpf.base.som.ReceiverActiveException;
import org.rvpf.base.som.TopicProxy;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.SessionFactoryImpl;
import org.rvpf.service.rmi.SessionSecurityContext;
import org.rvpf.som.queue.QueueServerImpl;
import org.rvpf.som.topic.TopicServerImpl;

/**
 * SOM factory.
 */
public final class SOMFactory
{
    /**
     * Constructs an instance.
     *
     * @param config The service config.
     */
    public SOMFactory(@Nonnull final Config config)
    {
        _config = config;
    }

    /**
     * Asks if configured to be private.
     *
     * @param somProperties SOM properties.
     *
     * @return True if configured to be private.
     */
    @CheckReturnValue
    public static boolean isPrivate(@Nonnull final KeyedGroups somProperties)
    {
        return somProperties.getBoolean(PRIVATE_PROPERTY);
    }

    /**
     * Asks if configured to be a server.
     *
     * @param somProperties SOM properties.
     *
     * @return True if configured to be a server.
     */
    @CheckReturnValue
    public static boolean isServer(@Nonnull final KeyedGroups somProperties)
    {
        return somProperties.getBoolean(SERVER_PROPERTY)
               || isPrivate(somProperties);
    }

    /**
     * Creates a queue instance.
     *
     * @param queueProperties The queue properties.
     *
     * @return A SOMFactory.Queue instance.
     */
    @Nonnull
    @CheckReturnValue
    public Queue createQueue(@Nonnull final KeyedGroups queueProperties)
    {
        return new Queue(queueProperties);
    }

    /**
     * Creates a topic instance.
     *
     * @param topicProperties The topic properties.
     *
     * @return A SOMFactory.Topic instance.
     */
    @Nonnull
    @CheckReturnValue
    public Topic createTopic(@Nonnull final KeyedGroups topicProperties)
    {
        return new Topic(topicProperties);
    }

    /**
     * Returns the client name.
     *
     * @return The client name.
     */
    String _clientName()
    {
        return _config.getServiceName();
    }

    /**
     * Returns the config properties.
     *
     * @return The config properties.
     */
    KeyedGroups _configProperties()
    {
        return _config.getProperties();
    }

    /**
     * Creates a queue server.
     *
     * @param queueProperties The queue properties.
     *
     * @return The queue server (null on failure).
     */
    @Nullable
    @CheckReturnValue
    QueueServerImpl _createQueueServer(
            @Nonnull final KeyedGroups queueProperties)
    {
        final SessionSecurityContext securityContext;

        if (isPrivate(queueProperties)) {
            securityContext = null;
        } else {
            securityContext = SessionFactoryImpl
                .createSecurityContext(
                    _config.getProperties(),
                    queueProperties
                        .getGroup(SecurityContext.SECURITY_PROPERTIES),
                    _LOGGER);

            if (securityContext == null) {
                return null;
            }
        }

        final QueueServerImpl queueServer = new QueueServerImpl(
            Optional.ofNullable(securityContext));

        if (!queueServer.setUp(_config, queueProperties)) {
            queueServer.tearDown();

            return null;
        }

        return queueServer;
    }

    /**
     * Creates a topic server.
     *
     * @param topicProperties The topic properties.
     *
     * @return The topic server (null on failure).
     */
    @Nullable
    @CheckReturnValue
    TopicServerImpl _createTopicServer(
            @Nonnull final KeyedGroups topicProperties)
    {
        final SessionSecurityContext securityContext;

        if (isPrivate(topicProperties)) {
            securityContext = null;
        } else {
            securityContext = SessionFactoryImpl
                .createSecurityContext(
                    _config.getProperties(),
                    topicProperties
                        .getGroup(SecurityContext.SECURITY_PROPERTIES),
                    _LOGGER);

            if (securityContext == null) {
                return null;
            }
        }

        final TopicServerImpl topicServer = new TopicServerImpl(
            Optional.ofNullable(securityContext));

        if (!topicServer.setUp(_config, topicProperties)) {
            topicServer.tearDown();

            return null;
        }

        return topicServer;
    }

    /** Private property. */
    public static final String PRIVATE_PROPERTY = "private";

    /** Server property. */
    public static final String SERVER_PROPERTY = "server";

    /**  */

    static final Logger _LOGGER = Logger.getInstance(SOMFactory.class);

    private final Config _config;

    /**
     * Properties.
     */
    public static final class Properties
        extends KeyedGroups
    {
        /**
         * Constructs an instance.
         */
        public Properties()
        {
            super(ServiceMessages.PROPERTIES_TYPE.toString(), Optional.empty());
        }

        private Properties(final Properties other)
        {
            super(other);
        }

        /** {@inheritDoc}
         */
        @Override
        public Properties copy()
        {
            return new Properties(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public Properties freeze()
        {
            super.freeze();

            return this;
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Queue.
     */
    public final class Queue
    {
        /**
         * Constructs an instance.
         *
         * @param queueProperties The queue properties.
         */
        Queue(@Nonnull final KeyedGroups queueProperties)
        {
            if (isServer(queueProperties)) {
                _queueServer = _createQueueServer(queueProperties);
                _queueProperties = (_queueServer != null)
                        ? queueProperties: null;
            } else {
                _queueServer = null;
                _queueProperties = queueProperties;
            }
        }

        /**
         * Creates a receiver.
         *
         * @param autoconnect The autoconnect indicator.
         *
         * @return The receiver (null on failure).
         */
        @Nullable
        @CheckReturnValue
        public SOMReceiver createReceiver(final boolean autoconnect)
        {
            if (!isUsable()) {
                return null;
            }

            final SOMReceiver receiver;

            if (_queueServer != null) {
                try {
                    receiver = new SOMReceiver.Server(
                        _queueServer.createReceiverWrapper());
                } catch (final ReceiverActiveException exception) {
                    _LOGGER
                        .error(
                            ServiceMessages.RECEIVER_ACTIVE,
                            _queueServer.getName());

                    return null;
                }
            } else {
                final QueueProxy.Receiver clientReceiver = QueueProxy.Receiver
                    .newBuilder()
                    .prepare(
                        _configProperties(),
                        _queueProperties,
                        _clientName(),
                        _LOGGER)
                    .setAutoconnect(autoconnect)
                    .build();

                receiver = (clientReceiver != null)? new SOMReceiver.Client(
                    clientReceiver): null;
            }

            return receiver;
        }

        /**
         * Creates a SOM sender.
         *
         * @param autoconnect The autoconnect indicator.
         *
         * @return The SOM sender (null on failure).
         */
        @Nullable
        @CheckReturnValue
        public SOMSender createSender(final boolean autoconnect)
        {
            if (!isUsable()) {
                return null;
            }

            final SOMSender sender;

            if (_queueServer != null) {
                sender = new SOMSender.Server(
                    _queueServer.createSenderWrapper());
            } else {
                final QueueProxy.Sender clientSender = QueueProxy.Sender
                    .newBuilder()
                    .prepare(
                        _configProperties(),
                        _queueProperties,
                        _clientName(),
                        _LOGGER)
                    .setAutoconnect(autoconnect)
                    .build();

                sender = (clientSender != null)? new SOMSender.Client(
                    clientSender): null;
            }

            return sender;
        }

        /**
         * Asks if this queue is usable.
         *
         * @return True if usable.
         */
        @CheckReturnValue
        public boolean isUsable()
        {
            return _queueProperties != null;
        }

        private final KeyedGroups _queueProperties;
        private final QueueServerImpl _queueServer;
    }


    /**
     * Topic.
     */
    public final class Topic
    {
        /**
         * Constructs an instance.
         *
         * @param topicProperties The topic properties.
         */
        Topic(@Nonnull final KeyedGroups topicProperties)
        {
            if (isServer(topicProperties)) {
                _topicServer = _createTopicServer(topicProperties);
                _topicProperties = (_topicServer != null)
                        ? topicProperties: null;
            } else {
                _topicServer = null;
                _topicProperties = topicProperties;
            }
        }

        /**
         * Creates a publisher.
         *
         * @param autoconnect The autoconnect indicator.
         *
         * @return The publisher (null on failure).
         */
        @Nullable
        @CheckReturnValue
        public SOMPublisher createPublisher(final boolean autoconnect)
        {
            if (!isUsable()) {
                return null;
            }

            final SOMPublisher publisher;

            if (_topicServer != null) {
                publisher = new SOMPublisher.Server(
                    _topicServer.createPublisherWrapper());
            } else {
                final TopicProxy.Publisher clientPublisher =
                    TopicProxy.Publisher
                        .newBuilder()
                        .prepare(
                            _configProperties(),
                            _topicProperties,
                            _clientName(),
                            _LOGGER)
                        .setAutoconnect(autoconnect)
                        .build();

                publisher = (clientPublisher != null)? new SOMPublisher.Client(
                    clientPublisher): null;
            }

            return publisher;
        }

        /**
         * Creates a subscriber.
         *
         * @param autoconnect The autoconnect indicator.
         *
         * @return The subscriber (null on failure).
         */
        @Nullable
        @CheckReturnValue
        public SOMSubscriber createSubscriber(final boolean autoconnect)
        {
            if (!isUsable()) {
                return null;
            }

            final SOMSubscriber subscriber;

            if (_topicServer != null) {
                subscriber = new SOMSubscriber.Server(
                    _topicServer.createSubscriberWrapper());
            } else {
                final TopicProxy.Subscriber clientSubscriber =
                    TopicProxy.Subscriber
                        .newBuilder()
                        .prepare(
                            _configProperties(),
                            _topicProperties,
                            _clientName(),
                            _LOGGER)
                        .setAutoconnect(autoconnect)
                        .build();

                subscriber = (clientSubscriber != null)
                        ? new SOMSubscriber.Client(
                            clientSubscriber): null;
            }

            return subscriber;
        }

        /**
         * Asks if this topic is usable.
         *
         * @return True if usable.
         */
        @CheckReturnValue
        public boolean isUsable()
        {
            return _topicProperties != null;
        }

        private final KeyedGroups _topicProperties;
        private final TopicServerImpl _topicServer;
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
