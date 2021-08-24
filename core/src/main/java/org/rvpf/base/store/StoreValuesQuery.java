/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreValuesQuery.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.store;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.PointValuesQuery;
import org.rvpf.base.Points;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;

/**
 * Store values query.
 *
 * <p>Holds responsibilities related to the preparation, transmission and
 * processing of point values queries.</p>
 *
 * <ol>
 *   <li>Constructor parameters and attribute setters help prepare the
 *     specification of the query.</li>
 *   <li>They can send themselves to the appropriate store for processing.</li>
 *   <li>They participate in their efficient serialization / deserialization to
 *     help reduce the impact of their volume on the system.</li>
 *   <li>They collaborate with the store server to prepare the response.</li>
 *   <li>They accept response values from the store client.</li>
 *   <li>With the help of an inner class, they can prepare new instances to
 *     obtain values retained because of limits on the number of values for a
 *     response.</li>
 * </ol>
 */
@ThreadSafe
public final class StoreValuesQuery
    extends PointValuesQuery.Abstract
    implements Serializable
{
    /**
     * Constructs an instance.
     *
     * @param point An optional point.
     * @param interval An interval.
     * @param sync An optional sync instance.
     * @param pointUUID The optional point UUID.
     * @param rows The number of rows.
     * @param limit The response limit.
     * @param polatorTimeLimit The optional polator time limit.
     * @param type The type indicators.
     */
    StoreValuesQuery(
            @Nonnull final Optional<Point> point,
            @Nonnull final TimeInterval interval,
            @Nonnull final Optional<Sync> sync,
            @Nonnull final Optional<UUID> pointUUID,
            final int rows,
            final int limit,
            @Nonnull final Optional<ElapsedTime> polatorTimeLimit,
            final int type)
    {
        super(point, interval, sync);

        _pointUUID = pointUUID;
        _rows = rows;
        _limit = limit;
        _polatorTimeLimit = polatorTimeLimit;
        _type = type;

        if (isPolated() && !getInterval().isInstant()) {
            Require.success(getSync().isPresent());
        }
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (this == object) {
            return true;
        }

        if ((object == null) || (getClass() != object.getClass())) {
            return false;
        }

        final StoreValuesQuery other = (StoreValuesQuery) object;

        if (!_pointUUID.equals(other._pointUUID)) {
            return false;
        } else if (getPoint().isPresent()) {
            if (!other.getPoint().isPresent()) {
                return false;
            }

            if (!Objects
                .equals(
                    getPoint().get().getName(),
                    other.getPoint().get().getName())) {
                return false;
            }
        } else if (other.getPoint().isPresent()) {
            return false;
        }

        if (!Objects.equals(getInterval(), other.getInterval())) {
            return false;
        }

        if (!Objects.equals(_getSync(), other._getSync())) {
            return false;
        }

        return (_rows == other._rows) && (_type == other._type);
    }

    /**
     * Gets the limit.
     *
     * <p>The limit is the maximum number of values expected in a single
     * response. When there is at least one supplementary value, the response
     * will include a mark to help create a new store query for further
     * values.</p>
     *
     * @return The limit.
     */
    @CheckReturnValue
    public int getLimit()
    {
        return _limit;
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
        return _pointUUID;
    }

    /**
     * Gets the polator time limit.
     *
     * @return The optional polator time limit.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<ElapsedTime> getPolatorTimeLimit()
    {
        return _polatorTimeLimit;
    }

    /**
     * Gets the response from the point's store.
     *
     * @return The store response.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws StoreAccessException When store access fails.
     */
    @Nonnull
    @CheckReturnValue
    public StoreValues getResponse()
        throws InterruptedException, StoreAccessException
    {
        if (isCancelled()) {
            return new StoreValues(this);
        }

        return getPoint().get().getStore().get().select(this);
    }

    /**
     * Gets the rows.
     *
     * <p>The value of rows is the queried number of values. This may exceed the
     * limit.</p>
     *
     * @return The rows.
     */
    @CheckReturnValue
    public int getRows()
    {
        final int rows;

        if ((_type & _ROWS) == 0) {
            rows = ((_type & _MULTIPLE) == 0)? 1: Integer.MAX_VALUE;
        } else {
            rows = _rows;
        }

        return rows;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Sync> getSync()
    {
        final Optional<Sync> sync;

        if ((_type & _SYNCED) == 0) {
            sync = Optional.empty();
        } else if (super.getSync().isPresent()) {
            sync = super.getSync();
        } else if (getPoint().isPresent()) {
            sync = getPoint().get().getSync();
        } else {
            sync = Optional.empty();
        }

        return sync.isPresent()? Optional.of(sync.get().copy()): sync;
    }

    /**
     * Gets this store query's type.
     *
     * <p>The type is a bit mask representing caracteristics of this store
     * query.</p>
     *
     * @return The type.
     */
    @CheckReturnValue
    public int getType()
    {
        return _type;
    }

    /**
    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int hash;

        if (_pointUUID.isPresent()) {
            hash = _pointUUID.get().hashCode();
        } else if (getPoint().isPresent()) {
            hash = Objects.hashCode(getPoint().get().getName());
        } else {
            hash = 0;
        }

        hash ^= getInterval().hashCode();
        hash ^= Objects.hashCode(_getSync());
        hash ^= _rows;
        hash ^= _type;

        return hash;
    }

    /**
     * Asks if this store query is for all values within an interval.
     *
     * @return True for all values.
     */
    @CheckReturnValue
    public boolean isAll()
    {
        return (_type & _ROWS) == 0;
    }

    /**
     * Asks if this store query is for any values.
     *
     * @return True for any values.
     */
    @CheckReturnValue
    public boolean isAny()
    {
        return (_type & _ANY) != 0;
    }

    /**
     * Asks if this store query has been cancelled.
     *
     * @return True if cancelled.
     */
    @CheckReturnValue
    public boolean isCancelled()
    {
        return _cancelled;
    }

    /**
     * Asks if only the count of the requested values is needed.
     *
     * @return True if only the count of the requested values is needed.
     */
    @CheckReturnValue
    public boolean isCount()
    {
        return (_type & _COUNT) != 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isExtrapolated()
    {
        return (_type & _EXTRAPOLATED) != 0;
    }

    /**
     * Asks if the number of values to retrieve is fixed.
     *
     * @return True if that number is fixed.
     */
    @CheckReturnValue
    public boolean isFixed()
    {
        return ((_type & (_VALUE | _SYNCED)) == 0)
               && (((_type & _ROWS) != 0) || !isMultiple());
    }

    /**
     * Asks if this store query asks for values in forward order.
     *
     * @return True when values are queried in forward order.
     */
    @CheckReturnValue
    public boolean isForward()
    {
        return (_type & _REVERSE) == 0;
    }

    /**
     * Asks if this query includes deleted values.
     *
     * @return True when deleted values are included.
     */
    @CheckReturnValue
    public boolean isIncludeDeleted()
    {
        return (_type & _DELETED) != 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInterpolated()
    {
        return (_type & _INTERPOLATED) != 0;
    }

    /**
     * Asks if this store query is the result of a mark.
     *
     * @return True when marked.
     */
    @CheckReturnValue
    public boolean isMarked()
    {
        return (_type & _MARKED) != 0;
    }

    /**
     * Asks if this store query asks for multiple values.
     *
     * @return True when multiple values are queried.
     */
    @CheckReturnValue
    public boolean isMultiple()
    {
        return (getType() & _MULTIPLE) != 0;
    }

    /**
     * Gets the normalized indicator.
     *
     * @return The normalized indicator.
     */
    @CheckReturnValue
    public boolean isNormalized()
    {
        return (_type & _NORMALIZED) != 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isNotNull()
    {
        return (getType() & _VALUE) != 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isPolated()
    {
        return (_type & (_INTERPOLATED | _EXTRAPOLATED)) != 0;
    }

    /**
     * Asks if this is a pull query.
     *
     * @return True when this is a pull query.
     */
    @CheckReturnValue
    public boolean isPull()
    {
        return (getType() & _PULL) != 0;
    }

    /**
     * Asks if this store query asks for values in reverse order.
     *
     * @return True when values are queried in reverse order.
     */
    @CheckReturnValue
    public boolean isReverse()
    {
        return (getType() & _REVERSE) != 0;
    }

    /**
     * Asks if this is a synced query.
     *
     * @return True when this is a synced query.
     */
    @CheckReturnValue
    public boolean isSynced()
    {
        return (getType() & _SYNCED) != 0;
    }

    /**
     * Iterates on the point values returned for this store query.
     *
     * <p>The iteration can extend beyond the store query limit.</p>
     *
     * <p>Note: the returned iterable and its iterator are not thread safe.</p>
     *
     * @return An iterable on store values.
     */
    @Nonnull
    @CheckReturnValue
    public Iterable<PointValue> iterate()
    {
        return getPoint().get().getStore().get().iterate(this);
    }

    /**
     * Returns a new mark.
     *
     * @param pointUUID The optional UUID of the next point value.
     * @param stamp The time stamp of the next point value.
     * @param done The number of values already produced.
     *
     * @return A new mark.
     */
    @Nonnull
    @CheckReturnValue
    public Mark newMark(
            @Nonnull final Optional<UUID> pointUUID,
            @Nonnull final DateTime stamp,
            final int done)
    {
        return new Mark(pointUUID, stamp, done);
    }

    /**
     * Sets the 'cancelled' indicator.
     *
     * @param cancelled The new value for the indicator.
     */
    public void setCancelled(final boolean cancelled)
    {
        _cancelled = cancelled;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder stringBuilder = new StringBuilder();

        if ((_type & (_COUNT | _PULL)) == 0) {
            stringBuilder.append("get ");
        } else {
            if ((_type & _COUNT) != 0) {
                stringBuilder.append("count ");
            }

            if ((_type & _PULL) != 0) {
                stringBuilder.append("pull ");
            }
        }

        if ((_type & _MULTIPLE) != 0) {
            if (((_type & _ROWS) != 0) && (_rows < Integer.MAX_VALUE)) {
                if ((_type & _REVERSE) != 0) {
                    stringBuilder.append("last ");
                } else {
                    stringBuilder.append("first ");
                }

                stringBuilder.append(_rows);
                stringBuilder.append(' ');
            } else {
                stringBuilder.append("all ");
            }
        } else if ((_type & _REVERSE) != 0) {
            stringBuilder.append("last ");
        } else if (!getInterval().isInstant()) {
            stringBuilder.append("first ");
        }

        if ((_type & _VALUE) != 0) {
            stringBuilder.append("non null ");
        }

        if (_pointUUID.isPresent() && _pointUUID.get().isDeleted()) {
            stringBuilder.append("deleted ");
        }

        if ((_type & (_INTERPOLATED | _EXTRAPOLATED)) != 0) {
            if ((_type & _INTERPOLATED) != 0) {
                stringBuilder.append("inter");

                if ((_type & _EXTRAPOLATED) != 0) {
                    stringBuilder.append("/extra-");
                }
            } else {
                stringBuilder.append("extra");
            }

            stringBuilder.append("polated ");
        } else if ((_type & _SYNCED) != 0) {
            stringBuilder.append("sync ");
        }

        if ((_type & _NORMALIZED) != 0) {
            stringBuilder.append("normalized ");
        }

        stringBuilder.append("value");

        if ((_type & _MULTIPLE) != 0) {
            stringBuilder.append('s');
        }

        if ((_type & _DELETED) != 0) {
            stringBuilder.append(" including deleted");
        }

        if (_pointUUID.isPresent() || getPoint().isPresent()) {
            stringBuilder
                .append(" of '")
                .append(
                    (getPoint().isPresent())
                    ? getPoint().get().toString(): _pointUUID
                        .get()
                        .toString())
                .append("'");
        }

        stringBuilder.append(getInterval().toString());

        if (_limit > 0) {
            stringBuilder.append(" limit ");
            stringBuilder.append(_limit);
        }

        return stringBuilder.toString();
    }

    private Sync _getSync()
    {
        return super.getSync().orElse(null);
    }

    private Object writeReplace()
    {
        return newBuilder().copyFrom(this);
    }

    static final int _AFTER = 1 << 1;
    static final int _ANY = 1 << 2;
    static final int _BEFORE = 1 << 3;
    static final int _COUNT = 1 << 4;    // Flag.
    static final int _DELETED = 1 << 5;    // Flag.
    static final int _EXTRAPOLATED = 1 << 6;    // Flag.
    static final int _FORWARD = 1 << 7;    // Flag.
    static final int _INTERPOLATED = 1 << 9;    // Flag.
    static final int _MARKED = 1 << 10;    // Flag.
    static final int _MULTIPLE = 1 << 11;    // Flag.
    static final int _NORMALIZED = 1 << 12;    // Flag.
    static final int _PULL = 1 << 8;    // Flag.
    static final int _REVERSE = 1 << 13;    // Flag.
    static final int _ROWS = 1 << 14;    // Flag.
    static final int _SYNCED = 1 << 15;    // Flag.
    static final int _VALUE = 1 << 16;    // Flag.

    /**  */

    private static final long serialVersionUID = 1L;

    private transient boolean _cancelled;
    private final int _limit;
    private final Optional<UUID> _pointUUID;
    private final Optional<ElapsedTime> _polatorTimeLimit;
    private final int _rows;
    private final int _type;

    /**
     * Builder.
     */
    @NotThreadSafe
    public static final class Builder
        implements Externalizable
    {
        /**
         * Reads in an external representation of a mark.
         *
         * @param input The external representation.
         *
         * @return The mark or empty.
         *
         * @throws IOException When an I/O error occurs.
         */
        @Nonnull
        @CheckReturnValue
        public static Optional<Mark> readMark(
                @Nonnull final ObjectInput input)
            throws IOException
        {
            if (!input.readBoolean()) {
                return Optional.empty();
            }

            final Builder queryBuilder = newBuilder();

            queryBuilder.readExternal(input);

            final Optional<UUID> pointUUID = UUID.readExternal(input);
            final DateTime stamp = DateTime.readExternal(input).get();
            final int done = input.readInt();

            return Optional
                .of(queryBuilder.build().newMark(pointUUID, stamp, done));
        }

        /**
         * Writes out an external representation of a mark.
         *
         * @param optionalMark The optional mark.
         * @param output The external representation.
         *
         * @throws IOException When an I/O error occurs.
         */
        public static void writeMark(
                @Nonnull final Optional<Mark> optionalMark,
                @Nonnull final ObjectOutput output)
            throws IOException
        {
            if (optionalMark.isPresent()) {
                final Mark mark = optionalMark.get();

                output.writeBoolean(true);
                newBuilder().copyFrom((mark.getQuery())).writeExternal(output);
                UUID.writeExternal(mark.getQueryPointUUID(), output);
                DateTime.writeExternal(Optional.of(mark.getStamp()), output);
                output.writeInt(mark.getDone());
            } else {
                output.writeBoolean(false);
            }
        }

        /**
         * Builds a store values query.
         *
         * @return The store values query.
         */
        @Nonnull
        @CheckReturnValue
        public StoreValuesQuery build()
        {
            if ((_flags & (_INTERPOLATED | _EXTRAPOLATED)) != 0
                    && !_intervalBuilder.isInstant()) {
                setSynced(true);
            }

            Require.ignored(limit(_rows));

            final TimeInterval interval = _intervalBuilder.build();

            int type = _flags | 1;    // Starts with flags, marks initialized.

            if ((_pointUUID == null) && (_point == null)) {
                // Selects the values of any point based on the version.
                type |= _PULL | _ANY;
            }

            if (interval.getAfter().isPresent()) {
                // Selects point values after the specified time.
                type |= _AFTER;
            }

            if (interval.getBefore().isPresent()) {
                // Selects point values before the specified time.
                type |= _BEFORE;
            }

            if ((type & (_AFTER | _BEFORE)) == (_AFTER | _BEFORE)) {
                // The interval is fully specified.
                if (interval.isInstant()) {
                    // Selects a single value.
                    type &= ~_MULTIPLE;
                } else {
                    // Selects multiple values.
                    type |= _MULTIPLE;
                }
            } else if ((type & (_AFTER | _FORWARD | _MULTIPLE | _PULL)) == 0) {
                // Starts with the last value.
                type |= _REVERSE;
            }

            if ((_flags & _ROWS) != 0) {
                // Selects the specified number of rows.
                if (_rows <= 1) {
                    type &= ~_MULTIPLE;
                } else {
                    type |= _MULTIPLE | _ROWS;
                }
            } else if ((type & _PULL) != 0) {
                // A pull request expects multiple values.
                type |= _MULTIPLE;
            }

            if ((_sync == null) && ((type & _SYNCED) != 0)) {
                if ((_point != null)
                        && (_point.getUUID().isPresent())
                        && !_point.isSynced()) {
                    // Clears unavailable sync specification.
                    type &= ~_SYNCED;
                }
            }

            boolean cancelled = false;

            if (_sync != null) {
                _intervalBuilder.trim(_sync.getDefaultLimits());

                if ((type & _REVERSE) != 0) {
                    final Optional<DateTime> before = _intervalBuilder
                        .getBefore();

                    if (before.isPresent()) {
                        final Optional<DateTime> previous = _sync
                            .getPreviousStamp(before.get());

                        if (previous.isPresent()
                                && previous.get().isNotBefore(interval)) {
                            setBefore(previous.get().after());
                        } else {
                            cancelled = true;
                        }
                    }
                } else {
                    final Optional<DateTime> after = _intervalBuilder
                        .getAfter();

                    if (after.isPresent()) {
                        final Optional<DateTime> next = _sync
                            .getNextStamp(after.get());

                        if (next.isPresent()
                                && next.get().isNotAfter(interval)) {
                            setAfter(next.get().before());
                        } else {
                            cancelled = true;
                        }
                    }
                }
            }

            final StoreValuesQuery query = new StoreValuesQuery(
                Optional.ofNullable(_point),
                _intervalBuilder.build(),
                Optional.ofNullable(_sync),
                Optional.ofNullable(_pointUUID),
                _rows,
                _limit,
                Optional.ofNullable(_polatorTimeLimit),
                type);

            query.setCancelled(cancelled);

            return query;
        }

        /**
         * Clears this builder.
         *
         * @return This.
         */
        @Nonnull
        public Builder clear()
        {
            _intervalBuilder.clear();
            _rows = 0;
            _flags = 0;
            _limit = 0;
            _sync = null;
            _polatorTimeLimit = null;

            return this;
        }

        /**
         * Copies the values from a store values query.
         *
         * @param storeValuesQuery The store values query.
         *
         * @return This.
         */
        @Nonnull
        public Builder copyFrom(
                @Nonnull final StoreValuesQuery storeValuesQuery)
        {
            _point = storeValuesQuery.getPoint().orElse(null);
            _pointUUID = storeValuesQuery.getPointUUID().orElse(null);
            _intervalBuilder.copyFrom(storeValuesQuery.getInterval());
            _rows = storeValuesQuery.getRows();
            _flags = storeValuesQuery.getType() & _FLAG_BITS;
            _limit = storeValuesQuery.getLimit();
            _sync = storeValuesQuery.getSync().orElse(null);
            _polatorTimeLimit = storeValuesQuery
                .getPolatorTimeLimit()
                .orElse(null);

            return this;
        }

        /**
         * Asks if the interval is valid.
         *
         * @return True if the interval is valid.
         */
        @CheckReturnValue
        public boolean isIntervalValid()
        {
            return _intervalBuilder.isValid();
        }

        /**
         * Limits the generation of responses.
         *
         * @param limit The limit.
         *
         * @return True if the limit has been modified.
         */
        @CheckReturnValue
        public boolean limit(final int limit)
        {
            if ((limit > 0) && ((_limit == 0) || (_limit > limit))) {
                _limit = limit;

                return true;
            }

            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public void readExternal(final ObjectInput input)
            throws IOException
        {
            _pointUUID = UUID.readExternal(input).orElse(null);

            if (_pointUUID == null) {
                final String pointName = Externalizer.readString(input);

                if (pointName != null) {
                    _point = new Point.Named(pointName);
                }
            }

            _intervalBuilder.readExternal(input);

            _rows = input.readInt();

            _flags = input.readInt();

            _limit = input.readInt();

            try {
                _sync = (Sync) input.readObject();
            } catch (final ClassNotFoundException exception) {
                throw new RuntimeException(exception);
            }

            _polatorTimeLimit = ElapsedTime.readExternal(input).orElse(null);
        }

        /**
         * Restores the reference to the point's definition.
         *
         * <p>This is used after deserialization.</p>
         *
         * @param points The object from which to get the points definition.
         *
         * @return False if the point is unknown.
         */
        @CheckReturnValue
        public boolean restore(final Points points)
        {
            if (_point == null) {
                if (_pointUUID == null) {
                    return true;
                }

                _point = points.getPointByUUID(_pointUUID).orElse(null);
            } else {
                final Optional<Point> optionalPoint = points
                    .getPointByName(_point.getName().orElse(null));

                if (!optionalPoint.isPresent()) {
                    return false;
                }

                _point = optionalPoint.get();
                _pointUUID = _point.getUUID().get();
            }

            return true;
        }

        /**
         * Sets the after time.
         *
         * @param after The after time.
         *
         * @return This.
         */
        @Nonnull
        public Builder setAfter(@Nonnull final DateTime after)
        {
            _intervalBuilder.setAfter(after);

            return this;
        }

        /**
         * Sets the all values within an interval.
         *
         * @param all The all values within an interval.
         *
         * @return This.
         */
        @Nonnull
        public Builder setAll(final boolean all)
        {
            if (all) {
                _rows = 0;
                _flags &= ~_ROWS;
                _flags |= _MULTIPLE;
            } else {
                _flags &= ~_MULTIPLE;
            }

            return this;
        }

        /**
         * Sets the time for this query.
         *
         * @param at The time.
         *
         * @return This.
         */
        @Nonnull
        public Builder setAt(@Nonnull final DateTime at)
        {
            _intervalBuilder.setAt(at);

            return this;
        }

        /**
         * Sets the before time.
         *
         * @param before The before time.
         *
         * @return This.
         */
        @Nonnull
        public Builder setBefore(@Nonnull final DateTime before)
        {
            _intervalBuilder.setBefore(before);

            return this;
        }

        /**
         * Sets the 'count' indicator.
         *
         * @param count The new value for the indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setCount(final boolean count)
        {
            if (count) {
                _flags |= _COUNT;
            } else {
                _flags &= ~_COUNT;
            }

            return this;
        }

        /**
         * Sets the extrapolated indicator.
         *
         * @param extrapolated The extrapolated indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setExtrapolated(final boolean extrapolated)
        {
            if (extrapolated) {
                _flags &= ~(_PULL | _DELETED);
                _flags |= _EXTRAPOLATED;
            } else {
                _flags &= ~_EXTRAPOLATED;
            }

            return this;
        }

        /**
         * Sets the include deleted mode.
         *
         * <p>Including deleted values disables ordering of the results of the
         * query.</p>
         *
         * @param includeDeleted The include deleted mode.
         *
         * @return This.
         */
        @Nonnull
        public Builder setIncludeDeleted(final boolean includeDeleted)
        {
            if (includeDeleted) {
                _flags &= ~(_SYNCED | _EXTRAPOLATED | _INTERPOLATED | _REVERSE);
                _flags |= _DELETED;
            } else {
                _flags &= ~_DELETED;
            }

            return this;
        }

        /**
         * Sets the interpolated indicator.
         *
         * @param interpolated The interpolated indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setInterpolated(final boolean interpolated)
        {
            if (interpolated) {
                _flags &= ~(_PULL | _DELETED);
                _flags |= _INTERPOLATED;
            } else {
                _flags &= ~_INTERPOLATED;
            }

            return this;
        }

        /**
         * Sets the time interval.
         *
         * @param interval The time interval.
         *
         * @return This.
         */
        @Nonnull
        public Builder setInterval(@Nonnull final TimeInterval interval)
        {
            _intervalBuilder.copyFrom(interval);

            return this;
        }

        /**
         * Sets the limit.
         *
         * @param limit The limit.
         *
         * @return This.
         */
        @Nonnull
        public Builder setLimit(final int limit)
        {
            _limit = limit;

            return this;
        }

        /**
         * Sets the marked indicator.
         *
         * @param marked The marked indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setMarked(final boolean marked)
        {
            if (marked) {
                _flags |= _MARKED;
            } else {
                _flags &= ~_MARKED;
            }

            return this;
        }

        /**
         * Sets the normalized indicator.
         *
         * @param normalized The normalized indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setNormalized(final boolean normalized)
        {
            if (normalized) {
                _flags |= _NORMALIZED;
            } else {
                _flags &= ~_NORMALIZED;
            }

            return this;
        }

        /**
         * Sets the not after time.
         *
         * @param notAfter The not after time.
         *
         * @return This.
         */
        @Nonnull
        public Builder setNotAfter(@Nonnull final DateTime notAfter)
        {
            _intervalBuilder.setNotAfter(notAfter);

            return this;
        }

        /**
         * Sets the not before time.
         *
         * @param notBefore The not before time.
         *
         * @return This.
         */
        @Nonnull
        public Builder setNotBefore(@Nonnull final DateTime notBefore)
        {
            _intervalBuilder.setNotBefore(notBefore);

            return this;
        }

        /**
         * Sets the not null indicator.
         *
         * @param notNull The not null indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setNotNull(final boolean notNull)
        {
            if (notNull) {
                _flags |= _VALUE;
            } else {
                _flags &= ~_VALUE;
            }

            return this;
        }

        /**
         * Sets the point.
         *
         * @param point The point.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPoint(@Nullable final Point point)
        {
            _point = point;
            _pointUUID = (point != null)? point.getUUID().orElse(null): null;

            if ((_pointUUID != null) && (point.isSynced())) {
                _flags |= _SYNCED;
            }

            return this;
        }

        /**
         * Sets the point UUID for the query.
         *
         * @param pointUUID The point UUID.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPointUUID(@Nullable final UUID pointUUID)
        {
            _point = null;
            _pointUUID = pointUUID;

            return this;
        }

        /**
         * Sets the polator time limit.
         *
         * <p>This time limit establishes the maximum elapsed time between an
         * interpolated or extrapolated value and the actual values used for their
         * computation. A default value may be specified for a point with the
         * {@value org.rvpf.base.Point#POLATOR_TIME_LIMIT_PARAM} parameter; this
         * default value can be overriden by this time limit property.</p>
         *
         * @param polatorTimeLimit The optional polator time limit.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPolatorTimeLimit(
                @Nonnull final Optional<ElapsedTime> polatorTimeLimit)
        {
            _polatorTimeLimit = polatorTimeLimit.orElse(null);

            return this;
        }

        /**
         * Sets the pull mode indicator.
         *
         * @param pull The pull mode indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setPull(final boolean pull)
        {
            if (pull) {
                _flags &= ~(_SYNCED | _EXTRAPOLATED | _INTERPOLATED | _REVERSE);
                _flags |= _PULL;
            } else {
                _flags &= ~_PULL;
            }

            return this;
        }

        /**
         * Sets the reverse mode indicator.
         *
         * @param reverse The reverse mode indicator.
         *
         * @return This.
         */
        @Nonnull
        public Builder setReverse(final boolean reverse)
        {
            if (reverse) {
                _flags &= ~(_FORWARD | _PULL);
                _flags |= _REVERSE;
            } else {
                _flags &= ~_REVERSE;
                _flags |= _FORWARD;
            }

            return this;
        }

        /**
         * Sets the rows.
         *
         * @param rows The rows.
         *
         * @return This.
         */
        @Nonnull
        public Builder setRows(final int rows)
        {
            if (rows <= 0) {
                setAll(true);
            } else {
                _rows = rows;
                _flags |= _ROWS;
            }

            return this;
        }

        /**
         * Sets the sync object.
         *
         * @param sync The sync object.
         *
         * @return This.
         */
        @Nonnull
        public Builder setSync(@Nullable final Sync sync)
        {
            _sync = sync;

            if (sync != null) {
                setSynced(true);
            }

            return this;
        }

        /**
         * Enables sync.
         *
         * @param synced True to enable.
         *
         * @return This.
         */
        @Nonnull
        public Builder setSynced(final boolean synced)
        {
            if (synced) {
                _flags &= ~(_PULL | _DELETED);
                _flags |= _SYNCED;
            } else {
                _flags &= ~_SYNCED;
            }

            return this;
        }

        /**
         * Unlinks from metadata.
         *
         * @return This.
         */
        @Nonnull
        @CheckReturnValue
        public Builder unlink()
        {
            if ((_point != null) && !(_point instanceof Point.Named)) {
                _point = null;
            }

            if (_sync != null) {
                _sync = _sync.copy();
            }

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public void writeExternal(final ObjectOutput output)
            throws IOException
        {
            UUID.writeExternal(Optional.ofNullable(_pointUUID), output);

            if (_pointUUID == null) {
                Externalizer
                    .writeString(
                        (_point != null)? _point.getName().orElse(null): null,
                        output);
            }

            _intervalBuilder.writeExternal(output);

            output.writeInt(_rows);

            output.writeInt(_flags);

            output.writeInt(_limit);

            output.writeObject(_sync);

            ElapsedTime
                .writeExternal(Optional.ofNullable(_polatorTimeLimit), output);
        }

        private Object readResolve()
        {
            return build();
        }

        private static final int _FLAG_BITS = ~(1 | _AFTER | _ANY | _BEFORE);

        private int _flags;
        private final TimeInterval.Builder _intervalBuilder = TimeInterval
            .newBuilder();
        private int _limit;
        private Point _point;
        private UUID _pointUUID;
        private ElapsedTime _polatorTimeLimit;
        private int _rows;
        private Sync _sync;
    }


    /**
     * Iteration exception.
     */
    public static final class IterationException
        extends RuntimeException
    {
        /**
         * Constructs an instance.
         *
         * @param exception The cause of this iteration exception.
         */
        public IterationException(@Nonnull final Exception exception)
        {
            super(exception);
        }

        /** {@inheritDoc}
         */
        @Override
        public Exception getCause()
        {
            return (Exception) super.getCause();
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Mark.
     *
     * <p>When the processing of a store query is suspended because the number
     * of point values produced is exceeding the limit, a mark object is created
     * that will piggyback on the store values. Since the mark class is an inner
     * class of {@link StoreValuesQuery}, the original store query informations
     * will also be included.</p>
     */
    public final class Mark
        implements Serializable
    {
        /**
         * Constructs an instance.
         *
         * <p>The supplied point value should be the first "overflow" value
         * which exceeds the limit for this store query. The mark would then
         * allow the owner of the query to create a new store query from this
         * mark to get further values, beginning with the one supplied here.</p>
         *
         * @param pointUUID The optional UUID of the next point value.
         * @param stamp The time stamp of the next point value.
         * @param done The number of values already produced.
         */
        Mark(
                @Nonnull final Optional<UUID> pointUUID,
                @Nonnull final DateTime stamp,
                final int done)
        {
            _queryPointUUID = getPointUUID()
                .isPresent()? null: pointUUID.orElse(null);
            _stamp = stamp;
            _done = done;
        }

        /**
         * Creates a store query to get further values.
         *
         * @return The new store query.
         */
        @Nonnull
        @CheckReturnValue
        public StoreValuesQuery createQuery()
        {
            final Builder builder = newBuilder()
                .copyFrom(StoreValuesQuery.this);

            builder
                .setPointUUID(
                    (_queryPointUUID == null)? getPointUUID()
                        .orElse(null): _queryPointUUID);

            if (isReverse()) {
                builder.setNotAfter(getStamp());
            } else {
                builder.setNotBefore(getStamp());
            }

            if (getRows() > 0) {
                builder.setRows(getRows() - getDone());
            }

            builder.setMarked(true);

            return builder.build();
        }

        /**
         * Gets the number of values produced at the time of the mark creation.
         *
         * @return The number of values produced.
         */
        @CheckReturnValue
        public int getDone()
        {
            return _done;
        }

        /**
         * Gets the parent store query.
         *
         * @return The parent store query.
         */
        @Nonnull
        @CheckReturnValue
        public StoreValuesQuery getQuery()
        {
            return StoreValuesQuery.this;
        }

        /**
         * Gets the UUID of the point.
         *
         * @return The optional UUID of the point.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<UUID> getQueryPointUUID()
        {
            return Optional.ofNullable(_queryPointUUID);
        }

        /**
         * Gets the time stamp.
         *
         * @return The time stamp.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime getStamp()
        {
            return _stamp;
        }

        private static final long serialVersionUID = 1L;

        /**
         * The number of values produced at the time of the mark creation.
         *
         * @serial
         */
        private final int _done;

        /**
         * The UUID of the point.
         *
         * @serial
         */
        private final UUID _queryPointUUID;

        /**
         * Marked point value's stamp.
         *
         * @serial
         */
        private final DateTime _stamp;
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
