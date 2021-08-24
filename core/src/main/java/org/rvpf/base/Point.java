/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Point.java 4077 2019-06-11 17:20:16Z SFB $
 */

package org.rvpf.base;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.store.Store;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.filter.ValueFilter;

/**
 * Point.
 *
 * <p>Holds the reference information for a set of timestamped values. This
 * includes the specification of {@link Origin}, {@link Content}, {@link Store}
 * and relations with other points. As an {@link Entity}, it also has a name and
 * {@link UUID}.</p>
 */
public interface Point
    extends Entity
{
    /**
     * Returns a configured filter.
     *
     * @return The step filter.
     */
    @Nonnull
    @CheckReturnValue
    ValueFilter filter();

    /**
     * Gets this point's content handling instance.
     *
     * <p>The point's content handling provides normalization / denormalization
     * services.</p>
     *
     * @return The optional content handling instance.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Content> getContent();

    /**
     * Gets this point's inputs.
     *
     * <p>The point's inputs are the points that may trigger a computation of
     * this point and / or are needed by this point's transform program.</p>
     *
     * @return The list of input points.
     */
    @Nonnull
    @CheckReturnValue
    List<? extends PointRelation> getInputs();

    /**
     * Gets this point's level.
     *
     * <p>A point's level determines the order of processing for the
     * processor.</p>
     *
     * @return An int where 0 means that the values of this point are not
     *         generated elsewhere. Higher values represent the level of
     *         dependencies.
     */
    @CheckReturnValue
    int getLevel();

    /**
     * Gets this point's origin.
     *
     * @return The optional definition of the origin of this point's values.
     */
    @Nonnull
    @CheckReturnValue
    Optional<? extends Origin> getOrigin();

    /**
     * Gets this point's parameters.
     *
     * @return The point's parameters.
     */
    @Nonnull
    @CheckReturnValue
    Params getParams();

    /**
     * Gets this point's replicates.
     *
     * @return The point's replicates.
     */
    @Nonnull
    @CheckReturnValue
    List<Replicate> getReplicates();

    /**
     * Gets this point's dependents.
     *
     * <p>When a new or updated value of this point may trigger the computation
     * of the value of an other point and / or this point's value is used in its
     * computation, that other point is considered a dependent.</p>
     *
     * @return A List of point relations.
     */
    @Nonnull
    @CheckReturnValue
    List<? extends PointRelation> getResults();

    /**
     * Gets this point's store.
     *
     * @return The optional definition of the store holding this point's values.
     */
    @Nonnull
    @CheckReturnValue
    Optional<? extends Store> getStore();

    /**
     * Gets a synchronization object for this point.
     *
     * @return An optional synchronization object.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Sync> getSync();

    /**
     * Asks if this point is dropped.
     *
     * <p>A point may be dropped from the metadata of a service if it is not
     * used to fulfill the responsibilities of the service.</p>
     *
     * @return True if dropped.
     */
    @CheckReturnValue
    boolean isDropped();

    /**
     * Asks if a null point values should result in the removal of the values
     * at the corresponding time stamp.
     *
     * @param defaultNullRemoves A default value for the answer.
     *
     * @return True if null values should be removed.
     */
    @CheckReturnValue
    boolean isNullRemoves(boolean defaultNullRemoves);

    /**
     * Asks if this point has a synchronization object.
     *
     * @return True if this point has a synchronization object.
     */
    @CheckReturnValue
    boolean isSynced();

    /**
     * Asks if this point is volatile.
     *
     * @return True if volatile.
     */
    @CheckReturnValue
    boolean isVolatile();

    /** Time limit for keeping values in the store. */
    String ARCHIVE_TIME_PARAM = "ArchiveTime";

    /** The capture limit. */
    String CAPTURE_LIMIT_PARAM = "CaptureLimit";

    /** The capture time. */
    String CAPTURE_TIME_PARAM = "CaptureTime";

    /**
     * Specifies the size of the offset relative to the next step which can be
     * filtered.
     */
    String CEILING_GAP_PARAM = "CeilingGap";

    /**
     * Specifies the ratio of the offset relative to the next Step which can be
     * filtered.
     */
    String CEILING_RATIO_PARAM = "CeilingRatio";

    /** Asks that notices be confirmed by a request to the store. */
    String CONFIRM_PARAM = "Confirm";

    /**
     * Specifies the size of the deadband around the previous value which will
     * be filtered.
     */
    String DEADBAND_GAP_PARAM = "DeadbandGap";

    /**
     * Specifies the ratio of the offset relative to the previous value which
     * will be filtered.
     */
    String DEADBAND_RATIO_PARAM = "DeadbandRatio";

    /**
     * The failure to produce a result should return a null value. The default
     * is to cancel the value generation.
     */
    String FAIL_RETURNS_NULL_PARAM = "FailReturnsNull";

    /** Specifies the filter to use for the point. */
    String FILTER_PARAM = "Filter";

    /** Specifies a stamp trim unit for filtering. */
    String FILTER_STAMP_TRIM_UNIT_PARAM = "FilterStampTrimUnit";

    /** Specifies a default time limit for filtering. */
    String FILTER_TIME_LIMIT_PARAM = "FilterTimeLimit";

    /**
     * Specifies the size of the offset relative to the previous Step which can
     * be filtered.
     */
    String FLOOR_GAP_PARAM = "FloorGap";

    /**
     * Specifies the ratio of the offset relative to the previous Step which
     * can be filtered.
     */
    String FLOOR_RATIO_PARAM = "FloorRatio";

    /** Keep at least a number of values in the store. */
    String KEEP_AT_LEAST_PARAM = "KeepAtLeast";

    /** Keep at most a number of values in the store. */
    String KEEP_AT_MOST_PARAM = "KeepAtMost";

    /** Time limit for keeping values in the store. */
    String LIFE_TIME_PARAM = "LifeTime";

    /**
     * Specifies to a store if a notice should be sent when a value of this
     * point is updated. The default, when filtering, is to send notices only
     * for values which may trigger a computation on a processor.
     */
    String NOTIFY_PARAM = "Notify";

    /** Null point. */
    Point NULL = new Named("");

    /** Specifies if a null value should cause a removal from the store. */
    String NULL_REMOVES_PARAM = Store.NULL_REMOVES_PARAM;

    /**
     * Supplies values, using the '@n' notation, where 'n' will be an integer
     * number identifying the parameter position, starting at 1.
     */
    String PARAM_PARAM = "Param";

    /** Specifies the polator to use for the point. */
    String POLATOR_PARAM = "Polator";

    /** Specifies a time limit for interpolation or extrapolation. */
    String POLATOR_TIME_LIMIT_PARAM = "PolatorTimeLimit";

    /**
     * Limits recalculations to the specified number of values (-1 or nothing
     * means no limit, 0 means no recalc, 1 means recalc latest only, etc.).
     * Defaults to -1.
     */
    String RECALC_LATEST_PARAM = "RecalcLatest";

    /** The point should be replicated by the stores in a replicator group. */
    String REPLICATED_PARAM = "Replicated";

    /** The archiver must respect the point version. */
    String RESPECT_VERSION_PARAM = "RespectVersion";

    /**
     * Informs that this point is resynchronized and that 'out of sync' logging
     * is not needeed.
     */
    String RESYNCHRONIZED_PARAM = "Resynchronized";

    /** States reference param. */
    String STATES_PARAM = "States";

    /** Specifies the value step size. */
    String STEP_SIZE_PARAM = "StepSize";

    /** Tag parameter. */
    String TAG_PARAM = "Tag";

    /** Marks this point as volatile to avoid caches. */
    String VOLATILE_PARAM = "Volatile";

    /**
     * Named.
     *
     * <p>A named point object represents a point only by its name or by one of
     * its aliases. This allows requests by clients which do not have access to
     * the complete metadata.</p>
     */
    @Immutable
    final class Named
        implements Point
    {
        /**
         * Constructs an instance.
         *
         * @param name The point's name.
         */
        public Named(@Nonnull final String name)
        {
            this(name, Optional.empty());
        }

        /**
         * Constructs an instance.
         *
         * @param name The point's name.
         * @param uuid The optional point's UUID.
         */
        public Named(
                @Nonnull final String name,
                @Nonnull final Optional<UUID> uuid)
        {
            _name = Require.notNull(name);
            _uuid = uuid;
        }

        /** {@inheritDoc}
         */
        @Override
        public int compareTo(final Entity other)
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public Named copy()
        {
            return new Named(_name, _uuid);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object other)
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public ValueFilter filter()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Attributes> getAttributes(final String usage)
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Content> getContent()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getElementName()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public List<? extends PointRelation> getInputs()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public int getLevel()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<String> getName()
        {
            return Optional.of(_name);
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<String> getNameInUpperCase()
        {
            return Optional.of(_name.toUpperCase(Locale.ROOT));
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Origin> getOrigin()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public Params getParams()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getPrefix()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getReferenceName()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public List<Replicate> getReplicates()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public List<? extends PointRelation> getResults()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Store> getStore()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Sync> getSync()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<UUID> getUUID()
        {
            return _uuid;
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isDropped()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isNullRemoves(final boolean defaultNullRemoves)
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isSynced()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isVolatile()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _name;
        }

        private final String _name;
        private final Optional<UUID> _uuid;
    }


    /**
     * Replicate.
     */
    final class Replicate
    {
        /**
         * Constructs an instance.
         *
         * @param point The replicate point.
         * @param convert The optional convert indicator.
         */
        public Replicate(
                @Nonnull final Point point,
                @Nonnull final Optional<Boolean> convert)
        {
            _point = Require.notNull(point);
            _convert = convert;
        }

        /**
         * Gets the convert indicator.
         *
         * @return The optional convert indicator.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<Boolean> getConvert()
        {
            return _convert;
        }

        /**
         * Gets the replicate point.
         *
         * @return The replicate point.
         */
        @Nonnull
        @CheckReturnValue
        public Point getPoint()
        {
            return _point;
        }

        /**
         * Gets the point's UUID.
         *
         * @return The optional point's UUID.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<UUID> getPointUUID()
        {
            return _point.getUUID();
        }

        private final Optional<Boolean> _convert;
        private final Point _point;
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
