/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Sync.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.sync;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.time.ZoneId;

import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TimeZone;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.Params;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.ReverseListIterator;

/**
 * Sync.
 *
 * <p>This interface is implemented by objects able to synthesize
 * synchronization timestamps.</p>
 */
public interface Sync
    extends Serializable
{
    /**
     * Creates a copy of this.
     *
     * @return The copy.
     */
    @Nonnull
    @CheckReturnValue
    Sync copy();

    /**
     * Gets the current stamp.
     *
     * @return The current stamp.
     */
    @Nonnull
    @CheckReturnValue
    DateTime getCurrentStamp();

    /**
     * Gets the default limits.
     *
     * @return The default limits.
     */
    @Nonnull
    @CheckReturnValue
    TimeInterval getDefaultLimits();

    /**
     * Gets the first stamp.
     *
     * @return The first stamp.
     */
    @Nonnull
    @CheckReturnValue
    DateTime getFirstStamp();

    /**
     * Gets the last stamp.
     *
     * @return The last stamp.
     */
    @Nonnull
    @CheckReturnValue
    DateTime getLastStamp();

    /**
     * Gets the limits.
     *
     * @return The limits.
     */
    @Nonnull
    @CheckReturnValue
    TimeInterval getLimits();

    /**
     * Gets the next time stamp.
     *
     * @return The next time stamp (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getNextStamp();

    /**
     * Gets the next time stamp.
     *
     * @param stamp The reference time stamp.
     *
     * @return The next time stamp (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getNextStamp(@Nonnull DateTime stamp);

    /**
     * Gets the next time stamp separated by a number of intervals.
     *
     * @param intervals A number of intervals.
     *
     * @return The next time stamp (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getNextStamp(int intervals);

    /**
     * Gets the next time stamp separated by a number of intervals.
     *
     * @param stamp The reference time stamp.
     * @param intervals A number of intervals.
     *
     * @return The next time stamp (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getNextStamp(@Nonnull DateTime stamp, int intervals);

    /**
     * Gets the previous time stamp.
     *
     * @return The previous time stamp (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getPreviousStamp();

    /**
     * Gets the previous time stamp.
     *
     * @param stamp The reference time stamp.
     *
     * @return The previous time stamp (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getPreviousStamp(@Nonnull DateTime stamp);

    /**
     * Gets the previous time stamp separated by a number of intervals.
     *
     * @param intervals A number of intervals.
     *
     * @return The next time stamp (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getPreviousStamp(int intervals);

    /**
     * Gets the previous time stamp separated by a number of intervals.
     *
     * @param stamp The reference time stamp.
     * @param intervals A number of intervals.
     *
     * @return The previous time stamp (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getPreviousStamp(@Nonnull DateTime stamp, int intervals);

    /**
     * Returns true if the limits are inside BoT to EoT.
     *
     * @return True if the limits are inside BoT to EoT.
     */
    @CheckReturnValue
    boolean isBounded();

    /**
     * Asks if the current time stamp is in sync.
     *
     * @return True when in sync.
     */
    @CheckReturnValue
    boolean isInSync();

    /**
     * Asks if the reference time stamp is in sync.
     *
     * @param stamp The reference time stamp.
     *
     * @return True when in sync.
     */
    @CheckReturnValue
    boolean isInSync(@Nonnull DateTime stamp);

    /**
     * Returns an iterator.
     *
     * @return The iterator.
     */
    @Nonnull
    @CheckReturnValue
    ListIterator<DateTime> iterator();

    /**
     * Returns an iterator.
     *
     * @param stamp The initial stamp.
     *
     * @return The iterator.
     */
    @Nonnull
    @CheckReturnValue
    ListIterator<DateTime> iterator(@Nonnull DateTime stamp);

    /**
     * Returns a reverse iterator.
     *
     * @return The iterator.
     */
    @Nonnull
    @CheckReturnValue
    ListIterator<DateTime> reverseIterator();

    /**
     * Returns a reverse iterator.
     *
     * @param stamp The initial stamp.
     *
     * @return The iterator.
     */
    @Nonnull
    @CheckReturnValue
    ListIterator<DateTime> reverseIterator(@Nonnull DateTime stamp);

    /**
     * Seeds the time stamp.
     *
     * @param stamp The time stamp.
     */
    void seed(@Nonnull DateTime stamp);

    /**
     * Sets the limits.
     *
     * @param limits The limits.
     */
    void setLimits(@Nonnull TimeInterval limits);

    /** Specifies a schedule in the format of an extended crontab entry. */
    String CRONTAB_PARAM = "Crontab";

    /** Elapsed parameter. */
    String ELAPSED_PARAM = "Elapsed";

    /** Offset parameter. */
    String OFFSET_PARAM = "Offset";

    /** Specifies a schedule with time stamps. */
    String STAMP_PARAM = "Stamp";

    /** Specifies a time zone. */
    String TIME_ZONE_PARAM = "TimeZone";

    /**
     * Abstract.
     */
    abstract class Abstract
        implements Sync, Externalizable
    {
        /**
         * Constructs an instance.
         */
        protected Abstract()
        {
            this(DateTime.getZoneId());
        }

        /**
         * Constructs an instance from an other.
         *
         * @param other The other instance.
         */
        protected Abstract(@Nonnull final Abstract other)
        {
            _zoneId = other._zoneId;
            _backupStamp = other._backupStamp;
            _currentStamp = other._currentStamp;
            _limits = other._limits;
            _frozen = other._frozen;
        }

        /**
         * Constructs an instance.
         *
         * @param zoneId The zone id.
         */
        protected Abstract(@Nonnull final ZoneId zoneId)
        {
            _zoneId = zoneId;
        }

        /** {@inheritDoc}
         */
        @Override
        public final DateTime getCurrentStamp()
        {
            return Require.notNull(_currentStamp);
        }

        /** {@inheritDoc}
         */
        @Override
        public TimeInterval getDefaultLimits()
        {
            return TimeInterval.UNLIMITED;
        }

        /** {@inheritDoc}
         */
        @Override
        public DateTime getFirstStamp()
        {
            final DateTime beginning = getLimits().getBeginning(true);

            return isInSync(
                beginning)? beginning: getNextStamp(beginning).get();
        }

        /** {@inheritDoc}
         */
        @Override
        public DateTime getLastStamp()
        {
            final DateTime end = getLimits().getEnd(true);

            return isInSync(end)? end: getPreviousStamp(end).get();
        }

        /** {@inheritDoc}
         */
        @Override
        public final TimeInterval getLimits()
        {
            if (_limits == null) {
                _limits = getDefaultLimits();
            }

            return _limits;
        }

        /** {@inheritDoc}
         */
        @Override
        public final Optional<DateTime> getNextStamp(final DateTime stamp)
        {
            seed(stamp);

            return getNextStamp();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<DateTime> getNextStamp(final int intervals)
        {
            Optional<DateTime> stamp = Optional.empty();

            for (int i = 0; i < intervals; ++i) {
                stamp = getNextStamp();
            }

            return stamp;
        }

        /** {@inheritDoc}
         */
        @Override
        public final Optional<DateTime> getNextStamp(
                final DateTime stamp,
                final int intervals)
        {
            seed(stamp);

            return getNextStamp(intervals);
        }

        /** {@inheritDoc}
         */
        @Override
        public final Optional<DateTime> getPreviousStamp(final DateTime stamp)
        {
            seed(stamp);

            return getPreviousStamp();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<DateTime> getPreviousStamp(final int intervals)
        {
            Optional<DateTime> stamp = Optional.empty();

            for (int i = 0; i < intervals; ++i) {
                stamp = getPreviousStamp();
            }

            return stamp;
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<DateTime> getPreviousStamp(
                final DateTime stamp,
                final int intervals)
        {
            seed(stamp);

            return getPreviousStamp(intervals);
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean isBounded()
        {
            final TimeInterval limits = getLimits();

            return limits.isFromBeginningOfTime() && limits.isToEndOfTime();
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean isInSync(final DateTime stamp)
        {
            seed(stamp);

            return isInSync();
        }

        /** {@inheritDoc}
         */
        @Override
        public ListIterator<DateTime> iterator()
        {
            return iterator(getFirstStamp());
        }

        /** {@inheritDoc}
         */
        @Override
        public ListIterator<DateTime> iterator(final DateTime stamp)
        {
            return new _Iterator(this, stamp);
        }

        /** {@inheritDoc}
         */
        @Override
        public void readExternal(final ObjectInput input)
            throws IOException
        {
            checkNotFrozen();

            _zoneId = TimeZone.getTimeZone(input.readUTF()).toZoneId();
        }

        /** {@inheritDoc}
         */
        @Override
        public ListIterator<DateTime> reverseIterator()
        {
            return reverseIterator(getLastStamp());
        }

        /** {@inheritDoc}
         */
        @Override
        public ListIterator<DateTime> reverseIterator(final DateTime stamp)
        {
            return new ReverseListIterator<>(new _Iterator(this, stamp));
        }

        /** {@inheritDoc}
         */
        @Override
        public final void seed(final DateTime stamp)
        {
            setCurrentStamp(stamp, 0);
        }

        /** {@inheritDoc}
         */
        @Override
        public void setLimits(final TimeInterval limits)
        {
            _limits = limits;
        }

        /**
         * Sets up this sync.
         *
         * @param params The sync params.
         *
         * @return True on success.
         */
        @CheckReturnValue
        public boolean setUp(@Nonnull final Params params)
        {
            checkNotFrozen();

            final Optional<String> timeZoneParam = params
                .getString(TIME_ZONE_PARAM);

            if (timeZoneParam.isPresent()) {
                final TimeZone timeZone = TimeZone
                    .getTimeZone(timeZoneParam.get());

                if ("GMT".equals(timeZone.getID())
                        && !"GMT".equals(timeZoneParam.get())) {
                    getThisLogger()
                        .warn(
                            BaseMessages.UNKNOWN_TIME_ZONE,
                            timeZoneParam.get());

                    return false;
                }

                setZoneId(timeZone.toZoneId());
            } else {
                setZoneId(DateTime.getZoneId());
            }

            return true;
        }

        /**
         * Tears down what has been set up.
         */
        public void tearDown() {}

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return getClass()
                .getSimpleName() + "@" + Integer.toHexString(
                    System.identityHashCode(this));
        }

        /** {@inheritDoc}
         */
        @Override
        public void writeExternal(final ObjectOutput output)
            throws IOException
        {
            output.writeUTF(TimeZone.getTimeZone(_zoneId).getID());
        }

        /**
         * Checks that the sync is not frozen.
         */
        protected final void checkNotFrozen()
        {
            Require.failure(_frozen, BaseMessages.FROZEN);
        }

        /**
         * Freezes.
         */
        protected final void freeze()
        {
            getLimits();

            _frozen = true;
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
            return Logger.getInstance(getClass());
        }

        /**
         * Gets the zone id.
         *
         * @return The time id.
         */
        @Nonnull
        @CheckReturnValue
        protected final ZoneId getZoneId()
        {
            return _zoneId;
        }

        /**
         * Returns the next stamp.
         *
         * @return The next stamp (empty if after limit ends).
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<DateTime> nextStamp()
        {
            final DateTime stamp = getCurrentStamp();
            final Optional<DateTime> endStamp = getLimits().getNotAfter();

            if (endStamp.isPresent() && stamp.isAfter(endStamp.get())) {
                _currentStamp = _backupStamp;

                return Optional.empty();
            }

            return Optional.of(stamp);
        }

        /**
         * Returns the previous stamp.
         *
         * @return The previous stamp (empty if before limit begins).
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<DateTime> previousStamp()
        {
            final DateTime stamp = getCurrentStamp();
            final Optional<DateTime> beginStamp = getLimits().getNotBefore();

            if (beginStamp.isPresent() && stamp.isBefore(beginStamp.get())) {
                _currentStamp = _backupStamp;

                return Optional.empty();
            }

            return Optional.of(stamp);
        }

        /**
         * Sets the current stamp.
         *
         * @param stamp The stamp.
         * @param direction The direction of the change (0 for don't care).
         */
        protected final void setCurrentStamp(
                @Nonnull final DateTime stamp,
                final int direction)
        {
            if (direction != 0) {
                if (direction > 0) {
                    Require.success(stamp.isAfter(_currentStamp));
                } else {
                    Require.success(stamp.isBefore(_currentStamp));
                }
            }

            _backupStamp = _currentStamp;
            _currentStamp = stamp;
        }

        /**
         * Sets the zone id.
         *
         * @param zoneId The zone id.
         */
        protected final void setZoneId(@Nonnull final ZoneId zoneId)
        {
            Require.notNull(zoneId);

            _zoneId = zoneId;
        }

        private static final long serialVersionUID = 1L;

        private DateTime _backupStamp;
        private DateTime _currentStamp;
        private transient boolean _frozen;
        private transient TimeInterval _limits;
        private ZoneId _zoneId;

        /**
         * Iterator.
         */
        private static class _Iterator
            implements ListIterator<DateTime>
        {
            /**
             * Constructs an instance.
             *
             * @param sync The calling sync instance.
             * @param stamp The initial stamp.
             */
            _Iterator(final Sync sync, final DateTime stamp)
            {
                _sync = sync.copy();
                _sync.seed(stamp);
            }

            /** {@inheritDoc}
             */
            @Override
            public void add(final DateTime stamp)
            {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean hasNext()
            {
                if (_previousStamp != null) {
                    _nextStamp = _sync
                        .getNextStamp(_previousStamp)
                        .orElse(null);
                    _previousStamp = null;
                } else if (_nextStamp == null) {
                    _nextStamp = _sync.getNextStamp().orElse(null);
                }

                return _nextStamp != null;
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean hasPrevious()
            {
                if (_nextStamp != null) {
                    _previousStamp = _sync
                        .getPreviousStamp(_nextStamp)
                        .orElse(null);
                    _nextStamp = null;
                } else if (_previousStamp == null) {
                    _previousStamp = _sync.getPreviousStamp().orElse(null);
                }

                return _previousStamp != null;
            }

            /** {@inheritDoc}
             */
            @Override
            public DateTime next()
            {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final DateTime nextStamp = _nextStamp;

                _nextStamp = null;

                return nextStamp;
            }

            /** {@inheritDoc}
             */
            @Override
            public int nextIndex()
            {
                return 0;
            }

            /** {@inheritDoc}
             */
            @Override
            public DateTime previous()
            {
                if (!hasPrevious()) {
                    throw new NoSuchElementException();
                }

                final DateTime previousStamp = _previousStamp;

                _previousStamp = null;

                return previousStamp;
            }

            /** {@inheritDoc}
             */
            @Override
            public int previousIndex()
            {
                return 0;
            }

            /** {@inheritDoc}
             */
            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }

            /** {@inheritDoc}
             */
            @Override
            public void set(final DateTime stamp)
            {
                throw new UnsupportedOperationException();
            }

            /**
             * Gets the sync.
             *
             * @return The sync.
             */
            @Nonnull
            @CheckReturnValue
            protected final Sync getSync()
            {
                return _sync;
            }

            private DateTime _nextStamp;
            private DateTime _previousStamp;
            private final Sync _sync;
        }
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
