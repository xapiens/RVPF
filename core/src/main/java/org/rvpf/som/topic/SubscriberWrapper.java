/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SubscriberWrapper.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.som.topic;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.rmi.ServiceClosedException;

/**
 * Subscriber wrapper.
 */
public class SubscriberWrapper
    extends TopicServerWrapper
{
    /**
     * Constructs a subscriber wrapper.
     *
     * @param subscriber The subscriber.
     * @param topicServer The topic server.
     */
    SubscriberWrapper(
            @Nonnull final Topic.Subscriber subscriber,
            @Nonnull final TopicServerImpl topicServer)
    {
        super(topicServer);

        _subscriber = subscriber;
    }

    /**
     * Closes the subscriber.
     */
    public void close()
    {
        _subscriber.close();
    }

    /**
     * Receives messages.
     *
     * @param limit The maximum number of messages.
     * @param timeout A time limit in millis (negative for infinite).
     *
     * @return The messages.
     *
     * @throws ServiceClosedException When the service is closed.
     */
    @Nonnull
    @CheckReturnValue
    public Serializable[] receive(
            final int limit,
            final long timeout)
        throws ServiceClosedException
    {
        return _subscriber.receive(limit, timeout);
    }

    private final Topic.Subscriber _subscriber;
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
