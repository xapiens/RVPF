/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NoticeContext.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.http.notice;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.Point;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.http.HTTPModule;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;

/**
 * Notice context.
 */
@ThreadSafe
public abstract class NoticeContext
    extends HTTPModule.Context
{
    /**
     * Gets the metadata.
     *
     * @return The metadata.
     */
    @Nonnull
    @CheckReturnValue
    public final Metadata getMetadata()
    {
        return Require.notNull(_metadata);
    }

    /**
     * Queues the supplied point values.
     *
     * @param notices The point values.
     *
     * @throws InterruptedException When the Service is stopped.
     */
    public abstract void notify(
            @Nonnull PointValue[] notices)
        throws InterruptedException;

    /**
     * Sets up this.
     *
     * @param metadata The metadata.
     * @param contextProperties The context properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(
            @Nonnull final Metadata metadata,
            @Nonnull final KeyedGroups contextProperties)
    {
        for (final Point point: metadata.getPointsCollection()) {
            if (!((PointEntity) point).setUp(metadata)) {
                return false;
            }
        }

        _metadata = metadata;

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        _metadata = null;
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return _logger;
    }

    private final Logger _logger = Logger.getInstance(getClass());
    private volatile Metadata _metadata;
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
