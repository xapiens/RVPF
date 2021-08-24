/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SubscriberSessionImpl.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.som.topic;

import java.io.Serializable;

import javax.annotation.Nonnull;

import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.rmi.UnauthorizedAccessException;
import org.rvpf.base.som.SubscriberSession;
import org.rvpf.service.ServiceMessages;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.SOMSessionImpl;

/**
 * Subscriber session implementation.
 */
public class SubscriberSessionImpl
    extends SOMSessionImpl
    implements SubscriberSession
{
    /**
     * Constructs an instance.
     *
     * @param topicServer The topic server.
     * @param connectionMode The connection mode.
     * @param clientName A descriptive name for the client.
     */
    SubscriberSessionImpl(
            @Nonnull final TopicServerImpl topicServer,
            @Nonnull final ConnectionMode connectionMode,
            @Nonnull final String clientName)
    {
        super(topicServer, connectionMode, clientName);

        _topicServer = topicServer;
        _subscriber = _topicServer.getTopic().newSubscriber();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
    {
        _subscriber.close();

        super.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isClosed()
    {
        return super.isClosed() || _topicServer.isClosed();
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable[] receive(
            final int limit,
            final long timeout)
        throws UnauthorizedAccessException, SessionException
    {
        try {
            securityCheck(SOMServerImpl.READ_ROLE);

            return _subscriber.receive(limit, adjustTimeout(timeout));
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
        return ServiceMessages.TOPIC_SUBSCRIBER.toString();
    }

    private final Topic.Subscriber _subscriber;
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
