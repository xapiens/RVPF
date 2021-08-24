/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PublisherImpl.java 4102 2019-06-30 15:41:17Z SFB $
 */

package org.rvpf.som.topic;

import java.io.Serializable;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.tool.Require;

/**
 * Publisher implementation.
 */
@ThreadSafe
final class PublisherImpl
    implements Topic.Publisher
{
    /**
     * Constructs an instance.
     *
     * @param topic The topic served.
     */
    PublisherImpl(@Nonnull final TopicImpl topic)
    {
        _topic = topic;
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        if (_closed.compareAndSet(false, true)) {
            _topic.onPublisherClosed(this);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void send(
            final Serializable[] messages)
        throws ServiceClosedException
    {
        if (_closed.get()) {
            throw new ServiceClosedException();
        }

        for (final Serializable message: messages) {
            _topic.publish(Require.notNull(message));
        }
    }

    private final AtomicBoolean _closed = new AtomicBoolean();
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
