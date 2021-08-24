/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ReceiverWrapper.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.som.queue;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.rmi.ServiceClosedException;

/**
 * Receiver wrapper.
 */
public class ReceiverWrapper
    extends QueueServerWrapper
{
    /**
     * Constructs a receiver wrapper.
     *
     * @param receiver The receiver;
     * @param queueServer The queue server.
     */
    ReceiverWrapper(
            @Nonnull final Queue.Receiver receiver,
            @Nonnull final QueueServerImpl queueServer)
    {
        super(queueServer);

        _receiver = receiver;
    }

    /**
     * Closes the receiver.
     */
    public void close()
    {
        _receiver.close();
        getQueueServer().closed(this);
    }

    /**
     * Commits uncommitted messages.
     *
     * @throws ServiceClosedException When the service is closed.
     */
    public void commit()
        throws ServiceClosedException
    {
        if (setDirty(false)) {
            _receiver.commit();
        }
    }

    /**
     * Purges the queue.
     *
     * @return The number of messages purged.
     *
     * @throws ServiceClosedException When the service is closed.
     */
    public long purge()
        throws ServiceClosedException
    {
        final long purged = _receiver.purge();

        setDirty(false);

        return purged;
    }

    /**
     * Receives messages.
     *
     * @param limit The maximum number of messages.
     * @param timeout A time limit in millis to wait for the first message
     *                (negative for infinite).
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
        setDirty(true);

        return _receiver.receive(limit, timeout);
    }

    /**
     * Rolls back uncommitted messages.
     *
     * @throws ServiceClosedException When the service is closed.
     */
    public void rollback()
        throws ServiceClosedException
    {
        if (setDirty(false)) {
            _receiver.rollback();
        }
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
