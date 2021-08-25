/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StorePoints.java 3972 2019-05-10 16:19:41Z SFB $
 */

package org.rvpf.jdbc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;

/**
 * Store points.
 */
final class StorePoints
{
    /**
     * Gets the name associated with a UUID.
     *
     * @param uuid The UUID.
     *
     * @return The optional name.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getName(@Nonnull final UUID uuid)
    {
        return Optional.ofNullable(_names.get(Require.notNull(uuid)));
    }

    /**
     * Gets the UUID associated with a name.
     *
     * @param name The name.
     *
     * @return The optional UUID.
     */
    @Nonnull
    @CheckReturnValue
    Optional<UUID> getUUID(@Nonnull final String name)
    {
        return Optional.ofNullable(_uuids.get(Require.notNull(name)));
    }

    /**
     * Registers a point.
     *
     * @param uuid The point's UUID.
     * @param name The point's name.
     */
    void register(@Nonnull final UUID uuid, @Nonnull final String name)
    {
        _names.put(uuid, name);
        _uuids.put(name, uuid);
    }

    private final Map<UUID, String> _names = new HashMap<UUID, String>();
    private final Map<String, UUID> _uuids = new HashMap<String, UUID>();
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
