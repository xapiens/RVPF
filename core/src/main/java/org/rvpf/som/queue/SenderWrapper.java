/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SenderWrapper.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.som.queue;

import java.io.Serializable;

import javax.annotation.Nonnull;

import org.rvpf.base.rmi.ServiceClosedException;

/**
 * Sender wrapper.
 */
public class SenderWrapper
    extends QueueServerWrapper
{
    /**
     * Constructs a sender wrapper.
     *
     * @param sender The sender.
     * @param queueServer The queue server.
     */
    SenderWrapper(
            @Nonnull final Queue.Sender sender,
            @Nonnull final QueueServerImpl queueServer)
    {
        super(queueServer);

        _sender = sender;
    }

    /**
     * Closes.
     */
    public void close()
    {
        _sender.close();
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
            _sender.commit();
        }
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
            _sender.rollback();
        }
    }

    /**
     * Sends messages.
     *
     * @param messages The messages.
     * @param commit If true, commits.
     *
     * @throws ServiceClosedException When the service is closed.
     */
    public void send(
            @Nonnull final Serializable[] messages,
            final boolean commit)
        throws ServiceClosedException
    {
        setDirty(true);
        _sender.send(messages, commit);
    }

    private final Queue.Sender _sender;
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
