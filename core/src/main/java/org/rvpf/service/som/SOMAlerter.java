/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMAlerter.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.service.som;

import java.io.Serializable;

import java.rmi.RemoteException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.rmi.SessionNotConnectedException;
import org.rvpf.base.som.SOMServer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.service.Alerter;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.service.rmi.ServiceRegistry;
import org.rvpf.som.SOMServerImpl;

/**
 * SOM alerter.
 *
 * <p>Implements an {@link Alerter} using Simple Messaging Objects as
 * transport.</p>
 */
public final class SOMAlerter
    extends Alerter.Abstract
    implements ServiceThread.Target
{
    /** {@inheritDoc}
     */
    @Override
    public boolean isEmbedded()
    {
        return _topic.isServer();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isRunning()
    {
        return _topic.isOpen();
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        boolean reconnecting = false;

        while (!isStopped()) {
            if (!_topic.open(reconnecting) || isStopped()) {
                break;
            }

            ServiceThread.ready();

            for (;;) {
                Serializable message;

                message = _topic.receive(0);

                if (message == null) {
                    message = _topic.receive(-1);

                    if (message == null) {
                        notifyListeners(Optional.empty());
                        _topic.close();
                        reconnecting = true;

                        break;
                    }
                }

                if (isStopped()) {
                    break;
                }

                if (message instanceof Alert) {
                    notifyListeners(Optional.of((Alert) message));
                }
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doSend(
            final Alert alert)
        throws InterruptedException, ServiceNotAvailableException
    {
        try {
            _topic.send(alert);
        } catch (final ServiceClosedException
                 |SessionNotConnectedException exception) {
            final InterruptedException interruptedException =
                new InterruptedException();

            interruptedException.initCause(exception);

            throw interruptedException;
        } catch (final SessionException exception) {
            throw new ServiceNotAvailableException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean doSetUp()
    {
        final KeyedGroups alerterProperties = getAlerterProperties();
        KeyedGroups topicProperties = alerterProperties
            .getGroup(TOPIC_PROPERTIES);
        final RegistryEntry registryEntry = RegistryEntry
            .newBuilder()
            .setBinding(topicProperties.getString(SOMServer.BINDING_PROPERTY))
            .setName(topicProperties.getString(SOMServer.NAME_PROPERTY))
            .setDefaultPrefix(SOMServer.DEFAULT_TOPIC_BINDING_PREFIX)
            .setDefaultName(DEFAULT_TOPIC)
            .setDefaultRegistryAddress(ServiceRegistry.getRegistryAddress())
            .setDefaultRegistryPort(ServiceRegistry.getRegistryPort())
            .build();

        if (registryEntry == null) {
            return false;
        }

        boolean embeddedTopic = false;

        if ((alerterProperties.getBoolean(
                EMBEDDED_PROPERTY) || SOMFactory.isServer(topicProperties))
                && (getService() != null)) {
            final String path = registryEntry.getPath();

            try {
                if (!ServiceRegistry.getInstance().isRegistered(path)) {
                    embeddedTopic = true;
                    Logger
                        .getInstance(getOwner().getClass())
                        .info(ServiceMessages.EMBEDDING_ALERTER_TOPIC);
                }
            } catch (final RemoteException exception) {
                getThisLogger()
                    .error(
                        exception,
                        ServiceMessages.RMI_REGISTRY_FAILED,
                        String.valueOf(ServiceRegistry.getRegistryPort()));

                return false;
            }
        }

        topicProperties = topicProperties
            .isMissing()? new SOMFactory.Properties(): topicProperties.copy();

        if (!registryEntry.isPrivate()) {
            topicProperties
                .setValue(SOMServer.BINDING_PROPERTY, registryEntry.toString());
        }

        if (embeddedTopic) {
            topicProperties.setValue(SOMFactory.SERVER_PROPERTY, Boolean.TRUE);
        }

        topicProperties.freeze();

        return _topic.setUp(topicProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doStart()
        throws InterruptedException
    {
        final Optional<Service> service = getService();
        final String serviceName = service
            .isPresent()? service.get().getServiceName(): null;
        final ServiceThread thread = new ServiceThread(
            this,
            "Alerter listener"
            + ((serviceName != null)? (" for [" + serviceName + "]"): ""));

        if (_thread.compareAndSet(null, thread)) {
            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            Require.ignored(thread.start(true));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doStop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            thread.interrupt();
            _topic.close();
            Require
                .ignored(
                    thread.join(Logger.getInstance(getOwner().getClass()), 0));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doTearDown()
    {
        _topic.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected SharedContext sharedContext()
    {
        return _SHARED_CONTEXT;
    }

    /**
     * Asks if the alerter is stopped.
     *
     * @return True if stopped.
     */
    @CheckReturnValue
    boolean isStopped()
    {
        return _thread.get() == null;
    }

    /** Default alerter topic. */
    public static final String DEFAULT_TOPIC = "Alerts";

    /** The topic properties. */
    public static final String TOPIC_PROPERTIES = "som.topic";
    private static final SharedContext _SHARED_CONTEXT = new SharedContext();

    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
    private final _AlerterTopic _topic = new _AlerterTopic();

    /**
     * Alerter topic.
     */
    private final class _AlerterTopic
    {
        /**
         * Constructs an instance.
         */
        _AlerterTopic() {}

        /**
         * Closes the topic.
         */
        void close()
        {
            final SOMSubscriber subscriber = _subscriber;

            if (subscriber != null) {
                subscriber.close();
            }

            final SOMPublisher publisher = _publisher;

            if (publisher != null) {
                final Optional<? extends SOMServerImpl> server = publisher
                    .getServer();

                publisher.close();

                if (server.isPresent()) {
                    server.get().close();
                }
            }
        }

        /**
         * Asks if this alerter topic is open.
         *
         * @return A true value if it is open.
         */
        @CheckReturnValue
        boolean isOpen()
        {
            final SOMSubscriber subscriber = _subscriber;

            return (subscriber != null) && subscriber.isOpen();
        }

        /**
         * Asks if this alerter topic is server.
         *
         * @return True if server.
         */
        @CheckReturnValue
        boolean isServer()
        {
            final SOMPublisher publisher = _publisher;

            return (publisher != null)? publisher.isServer(): false;
        }

        /**
         * Opens the topic.
         *
         * @param reconnecting True if reconnecting.
         *
         * @return True on success.
         */
        @CheckReturnValue
        boolean open(boolean reconnecting)
        {
            final SOMSubscriber subscriber = _subscriber;
            final SOMPublisher publisher = _publisher;

            if ((subscriber == null) || (publisher == null)) {
                return false;
            }

            boolean retryNotified = reconnecting;

            while (!isStopped()) {
                if (!reconnecting) {
                    if (subscriber.open()) {
                        if (publisher.open()) {
                            if (retryNotified) {
                                getThisLogger()
                                    .info(ServiceMessages.ALERTER_AVAILABLE);
                            }

                            break;
                        }

                        subscriber.close();
                    }
                }

                final Optional<Service> service = getService();

                if ((_connectionRetryDelay == null) || !service.isPresent()) {
                    return false;
                }

                if (isStopped()) {
                    break;
                }

                if (reconnecting || !retryNotified) {
                    getThisLogger().info(ServiceMessages.ALERTER_WAIT);
                    retryNotified = true;
                    reconnecting = false;
                }

                try {
                    service.get().snooze(_connectionRetryDelay);

                    if (service.get().isStopping()) {
                        getThisLogger().warn(ServiceMessages.CANCELLED);

                        return false;
                    }
                } catch (final InterruptedException interruptedException) {
                    getThisLogger().warn(ServiceMessages.INTERRUPTED);
                    Thread.currentThread().interrupt();

                    return false;
                }
            }

            return !isStopped();
        }

        /**
         * Receives a message.
         *
         * @param timeout A time limit in millis (negative for infinite).
         *
         * @return The message (null on failure).
         */
        @Nullable
        @CheckReturnValue
        Serializable receive(final long timeout)
        {
            final SOMSubscriber subscriber = _subscriber;

            if (subscriber == null) {
                return null;
            }

            final Serializable[] messages = subscriber.receive(1, timeout);

            return ((messages != null)
                    && (messages.length > 0))? messages[0]: null;
        }

        /**
         * Sends an alert.
         *
         * @param alert The alert.
         *
         * @throws SessionException On failure.
         */
        void send(@Nonnull final Alert alert)
            throws SessionException
        {
            if (isStopped()) {
                throw new ServiceClosedException();
            }

            final SOMPublisher publisher = _publisher;

            if (publisher != null) {
                if (!publisher.send(new Serializable[] {alert, })) {
                    throw publisher.getException().get();
                }
            }
        }

        /**
         * Sets up the topic.
         *
         * @param topicProperties Topic properties.
         *
         * @return A true value on success.
         */
        @CheckReturnValue
        boolean setUp(@Nonnull final KeyedGroups topicProperties)
        {
            final SOMFactory factory = new SOMFactory(getConfig());
            final SOMFactory.Topic factoryTopic = factory
                .createTopic(topicProperties);
            final SOMPublisher publisher = factoryTopic.createPublisher(false);
            final SOMSubscriber subscriber = factoryTopic
                .createSubscriber(false);

            if ((publisher == null) || (subscriber == null)) {
                return false;
            }

            if (publisher.isRemote()) {
                getThisLogger()
                    .info(
                        ServiceMessages.ALERTER_IS_REMOTE,
                        publisher.getServerURI());
            }

            _publisher = publisher;
            _subscriber = subscriber;

            if (isServer()) {
                _connectionRetryDelay = null;
            } else {
                _connectionRetryDelay = getAlerterProperties()
                    .getElapsed(
                        CONNECTION_RETRY_DELAY_PROPERTY,
                        Optional.of(DEFAULT_CONNECTION_RETRY_DELAY),
                        Optional.of(DEFAULT_CONNECTION_RETRY_DELAY))
                    .get();

                if (!SnoozeAlarm
                    .validate(
                        _connectionRetryDelay,
                        this,
                        ServiceMessages.CONNECTION_RETRY_DELAY_TEXT)) {
                    return false;
                }

                getThisLogger()
                    .debug(
                        ServiceMessages.CONNECTION_RETRY_DELAY,
                        _connectionRetryDelay);
            }

            return true;
        }

        /**
         * Tears down the topic.
         */
        void tearDown()
        {
            close();

            final SOMSubscriber subscriber = _subscriber;

            if (subscriber != null) {
                _subscriber = null;
                subscriber.tearDown();
            }

            final SOMPublisher publisher = _publisher;

            if (publisher != null) {
                _publisher = null;
                publisher.tearDown();
            }
        }

        private ElapsedTime _connectionRetryDelay;
        private volatile SOMPublisher _publisher;
        private volatile SOMSubscriber _subscriber;
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
