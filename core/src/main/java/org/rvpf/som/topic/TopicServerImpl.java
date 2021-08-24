/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TopicServerImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.som.topic;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import org.rvpf.base.UUID;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.Session.ConnectionMode;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.som.PublisherSession;
import org.rvpf.base.som.SubscriberSession;
import org.rvpf.base.som.TopicInfo;
import org.rvpf.base.som.TopicServer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ExportedSessionImpl;
import org.rvpf.service.rmi.SessionImpl;
import org.rvpf.service.rmi.SessionSecurityContext;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.SOMStatsHolder;

/**
 * Topic server implementation.
 */
public final class TopicServerImpl
    extends SOMServerImpl
    implements TopicServer
{
    /**
     * Constructs an instance.
     *
     * @param securityContext The optional security context.
     */
    public TopicServerImpl(
            @Nonnull final Optional<SessionSecurityContext> securityContext)
    {
        super(securityContext);
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        final Topic topic = _topic.get();

        if (topic != null) {
            super.close();

            topic.close();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public PublisherSession createPublisherSession(
            final UUID uuid,
            final String clientName)
        throws SessionException
    {
        return (PublisherSession) createSession(
            uuid,
            new Descriptor(_PUBLISHER, clientName));
    }

    /**
     * Creates a publisher wrapper.
     *
     * @return The publisher wrapper.
     */
    @Nonnull
    @CheckReturnValue
    public PublisherWrapper createPublisherWrapper()
    {
        return new PublisherWrapper(getTopic().newPublisher(), this);
    }

    /** {@inheritDoc}
     */
    @Override
    public SubscriberSession createSubscriberSession(
            final UUID uuid,
            final String clientName)
        throws SessionException
    {
        return (SubscriberSession) createSession(
            uuid,
            new Descriptor(_SUBSCRIBER, clientName));
    }

    /**
     * Creates a subscriber wrapper.
     *
     * @return The subscriber wrapper.
     */
    @Nonnull
    @CheckReturnValue
    public SubscriberWrapper createSubscriberWrapper()
    {
        return new SubscriberWrapper(getTopic().newSubscriber(), this);
    }

    /** {@inheritDoc}
     */
    @Override
    public TopicInfo getInfo()
    {
        return getTopic().getInfo();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean removeSession(final SessionImpl session)
    {
        final boolean removed = super.removeSession(session);
        final TopicStats stats = _stats.get();

        if (stats != null) {
            if (session instanceof PublisherSessionImpl) {
                stats.publisherSessionClosed();
            } else if (session instanceof SubscriberSessionImpl) {
                stats.subscriberSessionClosed();
            }
        }

        return removed;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Config config, final KeyedGroups topicProperties)
    {
        if (!super
            .setUp(config, topicProperties, DEFAULT_TOPIC_BINDING_PREFIX)) {
            return false;
        }

        if (!hasSecurityContext()) {
            getThisLogger().info(ServiceMessages.TOPIC_IS_PRIVATE, getName());
        }

        final SOMStatsHolder statsOwner = new SOMStatsHolder(getName());
        final TopicStats stats = new TopicStats(statsOwner);

        statsOwner.setStats(stats);

        if (hasSecurityContext() && !stats.register(config)) {
            return false;
        }

        _stats.set(stats);

        final Topic topic = new TopicImpl(stats);

        if (!topic.setUp(topicProperties)) {
            return false;
        }

        _topic.set(topic);

        return hasSecurityContext()? bind(): true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        final Topic topic = _topic.getAndSet(null);

        if (topic != null) {
            if (hasSecurityContext()) {
                super.tearDown();
            }

            topic.tearDown();
        }

        final TopicStats stats = _stats.getAndSet(null);

        if ((stats != null) && hasSecurityContext()) {
            stats.unregister();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    @GuardedBy("this")
    protected Session newSession(
            final ConnectionMode connectionMode,
            final Optional<RMIClientSocketFactory> clientSocketFactory,
            final Optional<RMIServerSocketFactory> serverSocketFactory,
            final Object reference)
    {
        final Descriptor descriptor = (Descriptor) reference;
        final String modeName = descriptor.getModeName();
        final String clientName = descriptor.getClientName();
        final ExportedSessionImpl session;

        if (modeName == _PUBLISHER) {
            session = new PublisherSessionImpl(
                this,
                connectionMode,
                clientName);
            _stats.get().publisherSessionOpened();
        } else if (modeName == _SUBSCRIBER) {
            session = new SubscriberSessionImpl(
                this,
                connectionMode,
                clientName);
            _stats.get().subscriberSessionOpened();
        } else {
            throw new AssertionError();
        }

        session
            .open(
                clientSocketFactory.orElse(null),
                serverSocketFactory.orElse(null));

        return session;
    }

    /**
     * Gets the topic.
     *
     * @return The topic.
     */
    @Nonnull
    @CheckReturnValue
    Topic getTopic()
    {
        return Require.notNull(_topic.get());
    }

    private static final String _PUBLISHER = "publisher";
    private static final String _SUBSCRIBER = "subscriber";

    private final AtomicReference<TopicStats> _stats =
        new AtomicReference<TopicStats>();
    private final AtomicReference<Topic> _topic = new AtomicReference<>();
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
