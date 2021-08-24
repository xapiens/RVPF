/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Points.java 3980 2019-05-13 12:52:38Z SFB $
 */

package org.rvpf.base;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.store.PointBinding;
import org.rvpf.base.util.container.KeyedValues;

/**
 * Points.
 *
 * <p>This interface provides access to the point definitions.</p>
 */
public interface Points
{
    /**
     * Gets the point with the specified key.
     *
     * @param key The key (either UUID string or name).
     *
     * @return The optional point.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Point> getPoint(@Nonnull String key);

    /**
     * Gets the point with the specified name.
     *
     * @param name The name.
     *
     * @return The optional point.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Point> getPointByName(@Nonnull String name);

    /**
     * Gets the point with the specified UUID.
     *
     * @param uuid The UUID.
     *
     * @return The optional point.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Point> getPointByUUID(@Nonnull UUID uuid);

    /**
     * Gets a collection of the registered points.
     *
     * @return A collection of points.
     */
    @Nonnull
    @CheckReturnValue
    Collection<Point> getPointsCollection();

    /**
     * Impl.
     */
    final class Impl
        implements Points
    {
        /**
         * Constructs an instance.
         */
        public Impl()
        {
            _pointsByName = new HashMap<String, Reference<Point>>();
            _pointsByUUID = new LinkedHashMap<UUID, Reference<Point>>();
        }

        /**
         * Constructs an instance.
         *
         * @param bindings The point bindings.
         */
        public Impl(@Nonnull final PointBinding[] bindings)
        {
            final int hashCapacity = KeyedValues.hashCapacity(bindings.length);

            _pointsByName = new HashMap<String, Reference<Point>>(hashCapacity);
            _pointsByUUID = new LinkedHashMap<UUID, Reference<Point>>(
                hashCapacity);

            for (final PointBinding binding: bindings) {
                addPoint(
                    new Point.Named(
                        binding.getName(),
                        Optional.of(binding.getUUID())));
            }
        }

        /**
         * Adds an alias for a point.
         *
         * @param alias The alias.
         * @param point The point.
         *
         * @return A possible reference to a previous value for the alias.
         */
        @Nonnull
        public Optional<Reference<Point>> addAlias(
                @Nonnull final String alias,
                @Nonnull final Point point)
        {
            return Optional
                .ofNullable(
                    _pointsByName
                        .put(
                                alias.toUpperCase(Locale.ROOT),
                                        new WeakReference<Point>(point)));
        }

        /**
         * Adds a point.
         *
         * @param point The point.
         *
         * @return A reference to a previous value for the name.
         */
        @Nonnull
        public Optional<Reference<Point>> addPoint(@Nonnull final Point point)
        {
            final Optional<String> name = point.getName();

            _pointsByUUID
                .put(point.getUUID().get(), new WeakReference<Point>(point));

            return name
                .isPresent()? addAlias(name.get(), point): Optional.empty();
        }

        /**
         * Gets the names map.
         *
         * @return The internal names map.
         */
        @Nonnull
        @CheckReturnValue
        public Map<String, Reference<Point>> getNamesMap()
        {
            return _pointsByName;
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Point> getPoint(final String key)
        {
            if (UUID.isUUID(key)) {
                return getPointByUUID(UUID.fromString(key).get());
            }

            return getPointByName(key);
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Point> getPointByName(final String name)
        {
            final Reference<Point> reference = _pointsByName
                .get(name.trim().toUpperCase(Locale.ROOT));

            return Optional
                .ofNullable((reference != null)? reference.get(): null);
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Point> getPointByUUID(final UUID uuid)
        {
            final Reference<Point> pointReference = _pointsByUUID
                .get(uuid.undeleted());

            return Optional
                .ofNullable(
                    (pointReference != null)? pointReference.get(): null);
        }

        /** {@inheritDoc}
         */
        @Override
        public Collection<Point> getPointsCollection()
        {
            final Collection<Point> points = new ArrayList<Point>(
                _pointsByUUID.size());

            for (final Reference<Point> pointReference:
                    _pointsByUUID.values()) {
                final Point point = pointReference.get();

                if (point != null) {
                    points.add(point);
                }
            }

            return points;
        }

        /**
         * Gets the UUID map.
         *
         * @return The internal UUID map.
         */
        @Nonnull
        @CheckReturnValue
        public Map<UUID, Reference<Point>> getUUIDMap()
        {
            return _pointsByUUID;
        }

        private final Map<String, Reference<Point>> _pointsByName;
        private final Map<UUID, Reference<Point>> _pointsByUUID;
    }
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
