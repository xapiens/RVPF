/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TopicImpl.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.som.topic;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.som.TopicInfo;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.ConcurrentIdentityHashSet;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.util.container.KeyedValues;

/**
 * Topic implementation.
 */
@ThreadSafe
public final class TopicImpl
    implements Topic
{
    /**
     * Constructs a topic.
     *
     * @param stats The topic stats.
     */
    TopicImpl(@Nonnull final TopicStats stats)
    {
        _stats = stats;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
    {
        for (final Publisher publisher: new ArrayList<>(_publishers)) {
            publisher.close();
        }

        Require.success(_publishers.isEmpty());

        for (final Subscriber subscriber: new ArrayList<>(_subscribers)) {
            subscriber.close();
        }

        Require.success(_subscribers.isEmpty());
    }

    /** {@inheritDoc}
     */
    @Override
    public TopicInfo getInfo()
    {
        return _info;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized Publisher newPublisher()
    {
        final Publisher publisher = new PublisherImpl(this);

        _publishers.add(publisher);
        _info.updatePublisherCount(+1);

        return publisher;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized Subscriber newSubscriber()
    {
        final SubscriberImpl subscriber = new SubscriberImpl(this);

        _subscribers.add(subscriber);
        _info.updateSubscriberCount(+1);

        return subscriber;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final KeyedValues somProperties)
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        close();
    }

    /**
     * Called when a publisher is closed.
     *
     * @param publisher The publisher.
     */
    void onPublisherClosed(@Nonnull final Publisher publisher)
    {
        if (_publishers.remove(publisher)) {
            _info.updatePublisherCount(-1);
        }
    }

    /**
     * Called when a subscriber is closed.
     *
     * @param subscriber The subscriber.
     */
    synchronized void onSubscriberClosed(@Nonnull final Subscriber subscriber)
    {
        if (_subscribers.remove(subscriber)) {
            _info.updateSubscriberCount(-1);
        }

        synchronized (_receiveLatch) {
            _receiveLatch.notifyAll();
        }
    }

    /**
     * Publishes a message.
     *
     * @param message The message.
     */
    void publish(@Nonnull final Serializable message)
    {
        boolean notify = false;

        for (final SubscriberImpl subscriber: _subscribers) {
            notify |= subscriber.accept(message);
        }

        if (notify) {
            synchronized (_receiveLatch) {
                _receiveLatch.notifyAll();
            }
        }

        _info.setLastPublish(DateTime.now());
        _stats.messagePublished();
    }

    /**
     * Waits on receive.
     *
     * @param waitMillis The maximum wait time in milliseconds.
     *
     * @throws InterruptedException When interrupted.
     */
    void waitOnReceive(final long waitMillis)
        throws InterruptedException
    {
        synchronized (_receiveLatch) {
            _receiveLatch.wait(waitMillis);
        }
    }

    private final TopicInfo _info = new TopicInfo();
    private final Set<Publisher> _publishers = new IdentityHashSet<>();
    private final Object _receiveLatch = new Object();
    private final TopicStats _stats;
    private final Set<SubscriberImpl> _subscribers =
        new ConcurrentIdentityHashSet<>();
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
