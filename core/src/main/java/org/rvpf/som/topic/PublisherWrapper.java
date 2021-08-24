/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PublisherWrapper.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.som.topic;

import java.io.Serializable;

import javax.annotation.Nonnull;

import org.rvpf.base.rmi.ServiceClosedException;

/**
 * Publisher wrapper.
 */
public class PublisherWrapper
    extends TopicServerWrapper
{
    /**
     * Constructs an instance.
     *
     * @param publisher The publisher.
     * @param topicServer The topic server.
     */
    PublisherWrapper(
            @Nonnull final Topic.Publisher publisher,
            @Nonnull final TopicServerImpl topicServer)
    {
        super(topicServer);

        _publisher = publisher;
    }

    /**
     * Closes the publisher.
     */
    public void close()
    {
        _publisher.close();
    }

    /**
     * Sends messages.
     *
     * @param messages The messages.
     *
     * @throws ServiceClosedException When the service is closed.
     */
    public void send(
            @Nonnull final Serializable[] messages)
        throws ServiceClosedException
    {
        _publisher.send(messages);
    }

    private final Topic.Publisher _publisher;
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
