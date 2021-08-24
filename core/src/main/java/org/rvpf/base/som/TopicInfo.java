/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TopicInfo.java 4067 2019-06-08 13:39:16Z SFB $
 */

package org.rvpf.base.som;

import java.io.Serializable;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.DateTime;

/**
 * Topic info.
 */
@ThreadSafe
public final class TopicInfo
    implements Serializable
{
    /**
     * Gets the last publish time.
     *
     * @return The last publish time.
     */
    @CheckReturnValue
    public DateTime getLastPublish()
    {
        return _lastPublish;
    }

    /**
     * Gets the publisher count.
     *
     * @return The publisher count.
     */
    @Nonnegative
    @CheckReturnValue
    public int getPublisherCount()
    {
        return _publisherCount.get();
    }

    /**
     * Gets the subscriber count.
     *
     * @return The subscriber count.
     */
    @Nonnegative
    @CheckReturnValue
    public int getSubscriberCount()
    {
        return _subscriberCount.get();
    }

    /**
     * Sets the last publish time.
     *
     * @param lastPublish The last publish time.
     */
    public void setLastPublish(@Nonnull final DateTime lastPublish)
    {
        _lastPublish = lastPublish;
    }

    /**
     * Updates the publisher count.
     *
     * @param delta The count delta.
     */
    public void updatePublisherCount(@Nonnegative final int delta)
    {
        _publisherCount.addAndGet(delta);
    }

    /**
     * Updates the subscriber count.
     *
     * @param delta The count delta.
     */
    public void updateSubscriberCount(@Nonnegative final int delta)
    {
        _subscriberCount.addAndGet(delta);
    }

    private static final long serialVersionUID = 1L;

    private volatile DateTime _lastPublish;
    private final AtomicInteger _publisherCount = new AtomicInteger();
    private final AtomicInteger _subscriberCount = new AtomicInteger();
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
