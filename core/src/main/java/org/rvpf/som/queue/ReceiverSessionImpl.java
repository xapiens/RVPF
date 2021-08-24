/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ReceiverSessionImpl.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.som.queue;

import java.io.Serializable;

import javax.annotation.Nonnull;

import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.som.ReceiverSession;
import org.rvpf.service.ServiceMessages;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.SOMSessionImpl;

/**
 * Receiver session implementation.
 */
public class ReceiverSessionImpl
    extends SOMSessionImpl
    implements ReceiverSession
{
    /**
     * Constructs an instance.
     *
     * @param queueServer The queue server.
     * @param connectionMode The connection mode.
     * @param clientName A descriptive name for the client.
     */
    ReceiverSessionImpl(
            @Nonnull final QueueServerImpl queueServer,
            @Nonnull final ConnectionMode connectionMode,
            @Nonnull final String clientName)
    {
        super(queueServer, connectionMode, clientName);

        _receiver = queueServer.getQueue().newReceiver();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
    {
        _receiver.close();

        super.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public void commit()
        throws SessionException
    {
        try {
            securityCheck(SOMServerImpl.READ_ROLE);

            _receiver.commit();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger().warn(
                throwable,
                ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public long purge()
        throws SessionException
    {
        try {
            securityCheck(SOMServerImpl.READ_ROLE);

            return _receiver.purge();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger().warn(
                throwable,
                ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Serializable[] receive(
            final int limit,
            final long timeout)
        throws SessionException
    {
        try {
            securityCheck(SOMServerImpl.READ_ROLE);

            return _receiver.receive(limit, adjustTimeout(timeout));
        } catch (final RuntimeException|Error throwable) {
            getThisLogger().warn(
                throwable,
                ServiceMessages.UNEXPECTED_SESSION_EXCEPTION,
                throwable);

            throw throwable;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void rollback()
        throws SessionException
    {
        try {
            securityCheck(SOMServerImpl.READ_ROLE);

            _receiver.rollback();
        } catch (final RuntimeException|Error throwable) {
            getThisLogger().warn(
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
        return ServiceMessages.QUEUE_RECEIVER.toString();
    }

    private final Queue.Receiver _receiver;
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
