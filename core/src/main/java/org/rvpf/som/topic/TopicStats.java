/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TopicStats.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.som.topic;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.Stats;
import org.rvpf.base.StatsOwner;
import org.rvpf.service.ServiceMessages;
import org.rvpf.som.SOMStats;

/**
 * Topic stats.
 */
@ThreadSafe
public final class TopicStats
    extends SOMStats
{
    /**
     * Constructs an instance.
     *
     * @param statsOwner The stats owner.
     */
    TopicStats(@Nonnull final StatsOwner statsOwner)
    {
        super(statsOwner);
    }

    /** {@inheritDoc}
     */
    @Override
    public void buildText()
    {
        addLine(
            ServiceMessages.TOPIC_PUBLISHER_SESSIONS,
            String.valueOf(getPublisherSessionsOpened()),
            String.valueOf(getPublisherSessionsClosed()));
        addLine(
            ServiceMessages.TOPIC_SUBSCRIBER_SESSIONS,
            String.valueOf(getSubscriberSessionsOpened()),
            String.valueOf(getSubscriberSessionsClosed()));

        addLine(
            ServiceMessages.TOPIC_MESSAGES_STATS,
            String.valueOf(getMessagesPublished()));

        super.buildText();
    }

    /** {@inheritDoc}
     */
    @Override
    public Stats clone()
    {
        final TopicStats clone = (TopicStats) super.clone();

        clone._messagesPublished = new AtomicInteger(getMessagesPublished());
        clone._publisherSessionsClosed = new AtomicInteger(
            getPublisherSessionsClosed());
        clone._publisherSessionsOpened = new AtomicInteger(
            getPublisherSessionsOpened());
        clone._subscriberSessionsClosed = new AtomicInteger(
            getSubscriberSessionsClosed());
        clone._subscriberSessionsOpened = new AtomicInteger(
            getSubscriberSessionsOpened());

        return clone;
    }

    /**
     * Gets the number of messages published.
     *
     * @return The number of messages published.
     */
    @CheckReturnValue
    public int getMessagesPublished()
    {
        return _messagesPublished.get();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getObjectType()
    {
        return OBJECT_TYPE;
    }

    /**
     * Gets the number of publisher sessions closed.
     *
     * @return The number of publisher sessions closed.
     */
    @CheckReturnValue
    public int getPublisherSessionsClosed()
    {
        return _publisherSessionsClosed.get();
    }

    /**
     * Gets the number of publisher sessions opened.
     *
     * @return The number of publisher sessions opened.
     */
    @CheckReturnValue
    public int getPublisherSessionsOpened()
    {
        return _publisherSessionsOpened.get();
    }

    /**
     * Gets the number of subscriber sessions closed.
     *
     * @return The number of subscriber sessions closed.
     */
    @CheckReturnValue
    public int getSubscriberSessionsClosed()
    {
        return _subscriberSessionsClosed.get();
    }

    /**
     * Gets the number of subscriber sessions opened.
     *
     * @return The number of subscriber sessions opened.
     */
    @CheckReturnValue
    public int getSubscriberSessionsOpened()
    {
        return _subscriberSessionsOpened.get();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void substract(final Stats snapshot)
    {
        final TopicStats stats = (TopicStats) snapshot;

        _messagesPublished.addAndGet(-stats.getMessagesPublished());
        _publisherSessionsClosed.addAndGet(-stats.getPublisherSessionsClosed());
        _publisherSessionsOpened.addAndGet(-stats.getPublisherSessionsOpened());
        _subscriberSessionsClosed
            .addAndGet(-stats.getSubscriberSessionsClosed());
        _subscriberSessionsOpened
            .addAndGet(-stats.getSubscriberSessionsOpened());

        super.substract(snapshot);
    }

    /**
     * Called when a message is published.
     */
    void messagePublished()
    {
        _messagesPublished.incrementAndGet();
        updated();
    }

    /**
     * Called when a publisher session is closed.
     */
    void publisherSessionClosed()
    {
        _publisherSessionsClosed.incrementAndGet();
        updated();
    }

    /**
     * Called when a publisher session is opened.
     */
    void publisherSessionOpened()
    {
        _publisherSessionsOpened.incrementAndGet();
        updated();
    }

    /**
     * Called when a subscriber session is closed.
     */
    void subscriberSessionClosed()
    {
        _subscriberSessionsClosed.incrementAndGet();
        updated();
    }

    /**
     * Called when a subscriber session is opened.
     */
    void subscriberSessionOpened()
    {
        _subscriberSessionsOpened.incrementAndGet();
        updated();
    }

    public static final String OBJECT_TYPE = "SOMTopic";
    private static final long serialVersionUID = 1L;

    private AtomicInteger _messagesPublished = new AtomicInteger();
    private AtomicInteger _publisherSessionsClosed = new AtomicInteger();
    private AtomicInteger _publisherSessionsOpened = new AtomicInteger();
    private AtomicInteger _subscriberSessionsClosed = new AtomicInteger();
    private AtomicInteger _subscriberSessionsOpened = new AtomicInteger();
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
