/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StreamedMessagesAccess.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.xml.streamer;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;

/**
 * Streamed messages access.
 */
@NotThreadSafe
abstract class StreamedMessagesAccess
{
    /**
     * Sets up this.
     *
     * @param configProperties The optional configuration properties.
     * @param moduleProperties The optional module properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(
            @Nonnull final Optional<KeyedGroups> configProperties,
            @Nonnull final Optional<KeyedValues> moduleProperties)
    {
        Require.success(_streamer == null);

        _streamer = Streamer.newInstance();

        return _streamer.setUp(configProperties, moduleProperties);
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        if (_streamer != null) {
            _streamer.tearDown();
            _streamer = null;
        }
    }

    /**
     * Gets the streamer.
     *
     * @return The streamer.
     */
    @Nonnull
    @CheckReturnValue
    Streamer getStreamer()
    {
        return Require.notNull(_streamer);
    }

    private Streamer _streamer;
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
