/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Responder.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.memory;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.Points;
import org.rvpf.base.UUID;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreCursor;

/**
 * Responder.
 */
public class Responder
    implements StoreCursor.Responder
{
    Responder(
            @Nonnull final Optional<Map<UUID,
            NavigableMap<DateTime, VersionedValue>>> archiveMap,
            @Nonnull final Optional<Map<UUID, VersionedValue>> snapshotMap,
            @Nonnull final Optional<NavigableMap<DateTime,
            VersionedValue>> versionsMap,
            @Nonnull final Points points,
            final int limit)
    {
        _archiveMap = archiveMap;
        _snapshotMap = snapshotMap;
        _versionsMap = versionsMap;
        _points = points;
        _limit = limit;
    }

    /** {@inheritDoc}
     */
    @Override
    public long count()
    {
        return _cursor.count();
    }

    /** {@inheritDoc}
     */
    @Override
    public int limit()
    {
        return _limit;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<VersionedValue> next()
    {
        Optional<VersionedValue> pointValue = _cursor.next();

        if (pointValue.isPresent()) {
            if (_point != null) {
                pointValue = Optional
                    .of((VersionedValue) pointValue.get().restore(_point));
            } else {
                pointValue = Optional
                    .of((VersionedValue) pointValue.get().restore(_points));
            }
        }

        return pointValue;
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset(final Optional<StoreCursor> storeCursor)
    {
        _cursor = null;
        _point = null;

        if (storeCursor.isPresent()) {
            if (_snapshotMap.isPresent()) {
                _cursor = new MemoryCursor.Snapshot(
                    storeCursor.get(),
                    _snapshotMap.get());
            } else if (storeCursor.get().isPull()) {
                _cursor = new MemoryCursor.Version(
                    storeCursor.get(),
                    _versionsMap.get());
            } else {
                final NavigableMap<DateTime, VersionedValue> pointValuesMap =
                    _archiveMap
                        .get()
                        .get(storeCursor.get().getPointUUID().orElse(null));

                _cursor = new MemoryCursor.Stamp(
                    storeCursor.get(),
                    (pointValuesMap != null)? pointValuesMap: _EMPTY_MAP);
            }

            _point = storeCursor.get().getPoint().orElse(null);
        }
    }

    private static final NavigableMap<DateTime, VersionedValue> _EMPTY_MAP =
        new TreeMap<>();

    private final Optional<Map<UUID, NavigableMap<DateTime, VersionedValue>>> _archiveMap;
    private MemoryCursor _cursor;
    private final int _limit;
    private Point _point;
    private final Points _points;
    private final Optional<Map<UUID, VersionedValue>> _snapshotMap;
    private final Optional<NavigableMap<DateTime, VersionedValue>> _versionsMap;
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
