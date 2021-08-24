/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TopicServerWrapper.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.som.topic;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.som.TopicInfo;
import org.rvpf.som.SOMServerImpl;
import org.rvpf.som.SOMServerWrapper;

/**
 * Topic server wrapper.
 */
public abstract class TopicServerWrapper
    extends SOMServerWrapper
{
    /**
     * Constructs an instance.
     *
     * @param topicServer The topic server.
     */
    protected TopicServerWrapper(@Nonnull final TopicServerImpl topicServer)
    {
        _topicServer = topicServer;
    }

    /**
     * Gets the topic info.
     *
     * @return The topic info.
     */
    @Nonnull
    @CheckReturnValue
    public final TopicInfo getInfo()
    {
        return _topicServer.getInfo();
    }

    /**
     * Gets the topic server.
     *
     * @return The topic server.
     */
    @Nonnull
    @CheckReturnValue
    public final TopicServerImpl getTopicServer()
    {
        return _topicServer;
    }

    /** {@inheritDoc}
     */
    @Override
    protected final SOMServerImpl getServer()
    {
        return getTopicServer();
    }

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
