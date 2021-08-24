/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SubscriberImpl.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.som.topic;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.rmi.ServiceClosedException;

/**
 * Subscriber implementation.
 */
@ThreadSafe
final class SubscriberImpl
    implements Topic.Subscriber
{
    /**
     * Constructs an instance.
     *
     * @param topic The topic served.
     */
    SubscriberImpl(@Nonnull final TopicImpl topic)
    {
        _topic = topic;
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        synchronized (_closeMutex) {
            if (_closed.compareAndSet(false, true)) {
                _messages.clear();
                _topic.onSubscriberClosed(this);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable[] receive(
            int limit,
            long timeout)
        throws ServiceClosedException
    {
        final List<Serializable> messages = new LinkedList<Serializable>();
        final long startMillis = (timeout > 0)? System.currentTimeMillis(): 0;

        try {
            do {
                if (_messages.isEmpty() && !_closed.get()) {
                    if (timeout == 0) {
                        break;
                    }

                    final long waitMillis;

                    if (timeout > 0) {
                        final long elapsedMillis = System
                            .currentTimeMillis() - startMillis;

                        waitMillis = timeout - elapsedMillis;

                        if (waitMillis <= 0) {
                            break;
                        }
                    } else {
                        waitMillis = 0;
                    }

                    _topic.waitOnReceive(waitMillis);
                }

                if (_closed.get()) {
                    throw new ServiceClosedException();
                }

                final Serializable message = _messages.pollFirst();

                if (message != null) {
                    messages.add(message);
                    timeout = 0;
                    --limit;
                }
            } while (limit > 0);
        } catch (final InterruptedException exception) {
            throw new ServiceClosedException();
        }

        return messages.toArray(new Serializable[messages.size()]);
    }

    /**
     * Accepts a message.
     *
     * @param message The message.
     *
     * @return True if the message list was empty.
     */
    boolean accept(@Nonnull final Serializable message)
    {
        synchronized (_closeMutex) {
            if (_closed.get()) {
                return false;
            }

            final boolean wasEmpty = _messages.isEmpty();

            _messages.addLast(message);

            return wasEmpty;
        }
    }

    private final AtomicBoolean _closed = new AtomicBoolean();
    private final Object _closeMutex = new Object();
    private final ConcurrentLinkedDeque<Serializable> _messages =
        new ConcurrentLinkedDeque<Serializable>();
    private final TopicImpl _topic;
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
