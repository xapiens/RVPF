/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PublisherSessionImpl.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.som.topic;

import java.io.Serializable;

import javax.annotation.Nonnull;

import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.som.PublisherSession;
import org.rvpf.service.ServiceMessages;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.SOMSessionImpl;

/**
 * Publisher session implementation.
 */
public class PublisherSessionImpl
    extends SOMSessionImpl
    implements PublisherSession
{
    /**
     * Constructs an insgtance.
     *
     * @param topicServer The topic server.
     * @param connectionMode The connection mode.
     * @param clientName A descriptive name for the client.
     */
    PublisherSessionImpl(
            @Nonnull final TopicServerImpl topicServer,
            @Nonnull final ConnectionMode connectionMode,
            @Nonnull final String clientName)
    {
        super(topicServer, connectionMode, clientName);

        _topicServer = topicServer;
        _publisher = _topicServer.getTopic().newPublisher();
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        super.close();

        _publisher.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public void send(final Serializable[] messages)
        throws SessionException
    {
        try {
            securityCheck(SOMServerImpl.WRITE_ROLE);

            _publisher.send(messages);
        } catch (final RuntimeException|Error throwable) {
            getThisLogger()
                .warn(
                    throwable,
                    ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                    throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getType()
    {
        return ServiceMessages.TOPIC_PUBLISHER.toString();
    }

    private final Topic.Publisher _publisher;
    private final TopicServerImpl _topicServer;
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
