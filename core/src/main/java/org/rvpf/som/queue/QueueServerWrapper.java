/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueueServerWrapper.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.som.queue;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.som.QueueInfo;
import org.rvpf.base.tool.Require;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.SOMServerWrapper;

/**
 * Queue server wrapper.
 */
public abstract class QueueServerWrapper
    extends SOMServerWrapper
{
    /**
     * Constructs an instance.
     *
     * @param queueServer The queue server.
     */
    protected QueueServerWrapper(@Nonnull final QueueServerImpl queueServer)
    {
        _queueServer = Require.notNull(queueServer);
    }

    /**
     * Gets the queue info.
     *
     * @return The queue info.
     */
    @Nonnull
    @CheckReturnValue
    public final QueueInfo getInfo()
    {
        return _queueServer.getInfo();
    }

    /**
     * Gets the queue server.
     *
     * @return The queue server.
     */
    @Nonnull
    @CheckReturnValue
    public final QueueServerImpl getQueueServer()
    {
        return _queueServer;
    }

    /**
     * Asks if the queue is dirty.
     *
     * @return True if the queue is dirty..
     */
    @CheckReturnValue
    public final boolean isDirty()
    {
        return _dirty.get();
    }

    /** {@inheritDoc}
     */
    @Override
    protected final SOMServerImpl getServer()
    {
        return getQueueServer();
    }

    /**
     * Sets the dirty indicator.
     *
     * @param dirty The new value for the dirty indicator.
     *
     * @return False if the value was already set to the new value.
     */
    final boolean setDirty(final boolean dirty)
    {
        return _dirty.compareAndSet(!dirty, dirty);
    }

    private final AtomicBoolean _dirty = new AtomicBoolean();
    private final QueueServerImpl _queueServer;
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
