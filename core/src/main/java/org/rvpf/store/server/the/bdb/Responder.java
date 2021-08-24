/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Responder.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.bdb;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.Points;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.StoreCursor;

/**
 * Responder.
 */
public final class Responder
    implements StoreCursor.Responder
{
    /**
     * Constructs an instance.
     *
     * @param wrapper The wrapper.
     * @param points The points.
     * @param limit The limit.
     */
    public Responder(
            @Nonnull final JEWrapper wrapper,
            @Nonnull final Points points,
            final int limit)
    {
        _wrapper = wrapper;
        _points = points;
        _limit = limit;
    }

    /**
     * Closes this.
     */
    public void close()
    {
        reset(Optional.empty());
    }

    /** {@inheritDoc}
     */
    @Override
    public long count()
    {
        return _wrapper.countPointValues(_wrapperCursor);
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
        Optional<VersionedValue> pointValue = _wrapper
            .nextPointValue(_wrapperCursor);

        if (pointValue.isPresent()) {
            if (_point != null) {
                pointValue = Optional
                    .of((VersionedValue) pointValue.get().restore(_point));
            } else {
                pointValue = Optional
                    .of((VersionedValue) pointValue.get().restore(_points));
            }

            if (_pull) {
                if ((_version != null)
                        && pointValue.get().getVersion().isNotAfter(_version)) {
                    throw new RuntimeException(
                        "Non increasing version value at: " + pointValue);
                }

                _version = pointValue.get().getVersion();
            }
        }

        return pointValue;
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset(final Optional<StoreCursor> storeCursor)
    {
        if (_wrapperCursor != null) {
            try {
                _wrapper.closeCursor(_wrapperCursor);
            } catch (final JEWrapperException exception) {
                throw new RuntimeException(exception.getCause());
            }

            _wrapperCursor = null;
        }

        _point = null;

        if (storeCursor.isPresent()) {
            _wrapperCursor = _wrapper.getCursor(storeCursor.get());
            _point = storeCursor.get().getPoint().orElse(null);
            _pull = storeCursor.get().isPull();
            _version = null;
        }
    }

    private final int _limit;
    private Point _point;
    private final Points _points;
    private boolean _pull;
    private DateTime _version;
    private final JEWrapper _wrapper;
    private Object _wrapperCursor;
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
