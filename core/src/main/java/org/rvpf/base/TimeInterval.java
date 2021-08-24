/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TimeInterval.java 4065 2019-06-07 15:08:21Z SFB $
 */

package org.rvpf.base;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.logger.Message;

/**
 * Time interval.
 *
 * <p>A time interval instance represents the time interval between two absolute
 * times. It may be specified as either open or closed at each end.</p>
 *
 * <p>To be valid, a time interval must not go backward.</p>
 */
@Immutable
public final class TimeInterval
    implements Serializable
{
    /**
     * Constructs an instance.
     *
     * @param after After time.
     * @param before Before time.
     */
    TimeInterval(
            @Nonnull final Optional<DateTime> after,
            @Nonnull final Optional<DateTime> before)
    {
        _after = after;
        _before = before;
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

    /**
     * Returns a clone with the after time set.
     *
     * @param after The after time.
     *
     * @return The clone.
     */
    @Nonnull
    @CheckReturnValue
    public TimeInterval after(@Nonnull final DateTime after)
    {
        return newBuilder().setAfter(after)._setBefore(_before).build();
    }

    /**
     * Returns a clone with the before time set.
     *
     * @param before The before time.
     *
     * @return The clone.
     */
    @Nonnull
    @CheckReturnValue
    public TimeInterval before(@Nonnull final DateTime before)
    {
        return newBuilder()._setAfter(_after).setBefore(before).build();
    }

    /**
     * Asks if this interval contains a date and time.
     *
     * @param dateTime The date and time.
     *
     * @return True if this interval contains the date and time.
     */
    @CheckReturnValue
    public boolean contains(@Nonnull final DateTime dateTime)
    {
        return !(isBefore(dateTime) || isAfter(dateTime));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if ((other == null) || !(getClass() == other.getClass())) {
            return false;
        }

        final TimeInterval otherInterval = (TimeInterval) other;

        if (_after.isPresent()) {
            if (!_after.equals(otherInterval._after)) {
                return false;
            }
        } else if (otherInterval._after.isPresent()) {
            return false;
        }

        if (_before.isPresent()) {
            if (!_before.equals(otherInterval._before)) {
                return false;
            }
        } else if (otherInterval._before.isPresent()) {
            return false;
        }

        return true;
    }

    /**
     * Gets the after time.
     *
     * @return The after time.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getAfter()
    {
        return _after;
    }

    /**
     * Gets the before time.
     *
     * @return The before time.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getBefore()
    {
        return _before;
    }

    /**
     * Gets the date-time at the beginning of this.
     *
     * @param closed True for a closed limit.
     *
     * @return The date-time at the beginning of this.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime getBeginning(final boolean closed)
    {
        return _after
            .isPresent()? (closed? _after
                .get()
                .after(): _after.get()): DateTime.BEGINNING_OF_TIME;
    }

    /**
     * Gets the date-time at the end of this.
     *
     * @param closed True for a closed limit.
     *
     * @return The date-time at the end of this.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime getEnd(final boolean closed)
    {
        return _before
            .isPresent()? (closed? _before
                .get()
                .before(): _before.get()): DateTime.END_OF_TIME;
    }

    /**
     * Gets not after time.
     *
     * @return Not after time.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getNotAfter()
    {
        return _before
            .isPresent()? Optional.of(_before.get().before()): Optional.empty();
    }

    /**
     * Gets not before time.
     *
     * @return Not before time.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getNotBefore()
    {
        return _after
            .isPresent()? Optional.of(_after.get().after()): Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int hash = 0;

        if (_after.isPresent()) {
            hash ^= _after.get().hashCode();
        }

        if (_before.isPresent()) {
            hash ^= _before.get().hashCode();
        }

        return hash;
    }

    /**
     * Asks if this interval is after a date and time.
     *
     * @param dateTime The date and time.
     *
     * @return True if this interval is after the date and time.
     */
    @CheckReturnValue
    public boolean isAfter(@Nonnull final DateTime dateTime)
    {
        return !isFromBeginningOfTime()
               && (_after.get().compareTo(dateTime) >= 0);
    }

    /**
     * Asks if this interval is before a date and time.
     *
     * @param dateTime The date and time.
     *
     * @return True if this interval is before the date and time.
     */
    @CheckReturnValue
    public boolean isBefore(@Nonnull final DateTime dateTime)
    {
        return !isToEndOfTime() && (_before.get().compareTo(dateTime) <= 0);
    }

    /**
     * Asks if this time interval is from the beginning of time.
     *
     * @return True if this is from the beginning of time.
     */
    @CheckReturnValue
    public boolean isFromBeginningOfTime()
    {
        return !_after.isPresent() || _after.get().isBeginningOfTime();
    }

    /**
     * Asks if this time interval is an instant.
     *
     * @return True if this is an instant in time.
     */
    @CheckReturnValue
    public boolean isInstant()
    {
        return _after.isPresent()
               && _before.isPresent()
               && ((_after.get().toRaw() + 1) == (_before.get().toRaw() - 1));
    }

    /**
     * Asks if this time interval is to the end of time.
     *
     * @return True if this is to the end of time.
     */
    @CheckReturnValue
    public boolean isToEndOfTime()
    {
        return !_before.isPresent() || _before.get().isEndOfTime();
    }

    /**
     * Returns a clone with the end set.
     *
     * @param notAfter The closed end of the interval.
     *
     * @return The clone.
     */
    @Nonnull
    @CheckReturnValue
    public TimeInterval notAfter(@Nonnull final DateTime notAfter)
    {
        return newBuilder()._setAfter(_after).setNotAfter(notAfter).build();
    }

    /**
     * Returns a clone with the beginning set.
     *
     * @param notBefore The closed beginning of the interval.
     *
     * @return The clone.
     */
    @Nonnull
    @CheckReturnValue
    public TimeInterval notBefore(@Nonnull final DateTime notBefore)
    {
        return newBuilder()._setBefore(_before).setNotBefore(notBefore).build();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        if (isFromBeginningOfTime() && isToEndOfTime()) {
            return "";
        }

        final StringBuilder stringBuilder = new StringBuilder(" ");

        if (isInstant()) {
            stringBuilder.append('[');
            stringBuilder.append(_after.get().after());
            stringBuilder.append(']');
        } else {
            if (isFromBeginningOfTime()) {
                stringBuilder.append("(_");
            } else {
                if ((_after.get().toRaw() % 10) == 9) {
                    stringBuilder.append('[');
                    stringBuilder.append(_after.get().after());
                } else {
                    stringBuilder.append('(');
                    stringBuilder.append(_after.get());
                }
            }

            stringBuilder.append("--");

            if (isToEndOfTime()) {
                stringBuilder.append("_)");
            } else {
                if ((_before.get().toRaw() % 10) == 1) {
                    stringBuilder.append(_before.get().before());
                    stringBuilder.append(']');
                } else {
                    stringBuilder.append(_before.get());
                    stringBuilder.append(')');
                }
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Returns a clone of this time interval trimmed to supplied limits.
     *
     * @param limits The limits.
     *
     * @return The trimmed time interval.
     */
    @Nonnull
    @CheckReturnValue
    public TimeInterval trimmed(@Nonnull final TimeInterval limits)
    {
        return newBuilder().copyFrom(this).trim(limits).build();
    }

    private Object writeReplace()
    {
        return newBuilder().copyFrom(this);
    }

    /** Unlimited time interval. */
    public static final TimeInterval UNLIMITED = newBuilder().build();
    private static final long serialVersionUID = 1L;

    private final Optional<DateTime> _after;
    private final Optional<DateTime> _before;

    /**
     * Time interval builder.
     */
    @NotThreadSafe
    public static final class Builder
        implements Externalizable
    {
        /**
         * Returns a time interval from a string representation.
         *
         * @param intervalString The interval string.
         *
         * @return The time interval.
         */
        @Nonnull
        @CheckReturnValue
        public static TimeInterval fromString(@Nonnull String intervalString)
        {
            final Builder builder = newBuilder();

            intervalString = intervalString.trim();

            if (intervalString.length() > 0) {
                final int separatorLength;
                int separatorIndex;

                separatorIndex = intervalString.indexOf('/');

                if (separatorIndex >= 0) {
                    separatorLength = 1;
                } else {
                    separatorIndex = intervalString.indexOf("--");

                    if (separatorIndex >= 0) {
                        separatorLength = 2;
                    } else {
                        separatorIndex = intervalString.indexOf(',');
                        separatorLength = 1;
                    }
                }

                if (separatorIndex < 0) {
                    if ((intervalString.charAt(0) != '[')
                            || (intervalString.charAt(
                                intervalString.length() - 1) != ']')) {
                        return _invalidIntervalFormat(intervalString);
                    }

                    builder
                        .setAt(
                            _dateTime(
                                intervalString
                                        .substring(
                                                1,
                                                        intervalString.length()
                                                        - 1))
                                .orElse(null));
                } else {
                    char limitChar = intervalString.charAt(0);
                    String timeString = intervalString
                        .substring(1, separatorIndex);

                    if (limitChar == '(') {
                        builder._setAfter(_dateTime(timeString));
                    } else if (limitChar == '[') {
                        builder._setNotBefore(_dateTime(timeString));
                    } else {
                        return _invalidIntervalFormat(intervalString);
                    }

                    limitChar = intervalString
                        .charAt(intervalString.length() - 1);
                    timeString = intervalString
                        .substring(
                            separatorIndex + separatorLength,
                            intervalString.length() - 1);

                    if (limitChar == ')') {
                        builder._setBefore(_dateTime(timeString));
                    } else if (limitChar == ']') {
                        builder._setNotAfter(_dateTime(timeString));
                    } else {
                        return _invalidIntervalFormat(intervalString);
                    }
                }
            }

            return builder.build();
        }

        /**
         * Builds a time interval.
         *
         * @return The time interval.
         */
        @Nonnull
        @CheckReturnValue
        public TimeInterval build()
        {
            if (!isValid()) {
                throw new InvalidIntervalException(
                    Message.format(
                        BaseMessages.NOT_BEFORE,
                        _after.get().after(),
                        _before));
            }

            return new TimeInterval(_after, _before);
        }

        /**
         * Clears this builder.
         *
         * @return This.
         */
        @Nonnull
        public Builder clear()
        {
            _after = Optional.empty();
            _before = Optional.empty();

            return this;
        }

        /**
         * Copies the values from an other time interval.
         *
         * @param otherInterval The other time interval.
         *
         * @return This.
         */
        @Nonnull
        public Builder copyFrom(@Nonnull final TimeInterval otherInterval)
        {
            _setAfter(otherInterval.getAfter());
            _setBefore(otherInterval.getBefore());

            return this;
        }

        /**
         * Gets the after time.
         *
         * @return The after time (may be empty).
         */
        @Nonnull
        @CheckReturnValue
        public Optional<DateTime> getAfter()
        {
            return _after;
        }

        /**
         * Gets the before time.
         *
         * @return The before time (may be empty).
         */
        @Nonnull
        @CheckReturnValue
        public Optional<DateTime> getBefore()
        {
            return _before;
        }

        /**
         * Asks if this time interval is an instant.
         *
         * @return True if this is an instant in time.
         */
        @CheckReturnValue
        public boolean isInstant()
        {
            return _after.isPresent()
                   && _before.isPresent()
                   && ((_after.get().toRaw() + 1)
                       == (_before.get().toRaw() - 1));
        }

        /**
         * Asks if this time interval is valid.
         *
         * <p>To be valid, the interval must either represent an instant or go
         * forward in time.</p>
         *
         * @return True if it is valid.
         */
        @CheckReturnValue
        public boolean isValid()
        {
            return !_after.isPresent()
                   || !_before.isPresent()
                   || (Long.compare(
                       _after.get().toRaw() + 1,
                       _before.get().toRaw()) < 0);
        }

        /** {@inheritDoc}
         */
        @Override
        public void readExternal(
                @Nonnull final ObjectInput input)
            throws IOException
        {
            _after = DateTime.readExternal(input);
            _before = DateTime.readExternal(input);
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
            return _setAfter(Optional.of(after));
        }

        /**
         * Sets an instant.
         *
         * @param at The instant.
         *
         * @return This.
         */
        @Nonnull
        public Builder setAt(@Nonnull final DateTime at)
        {
            return setNotBefore(at).setNotAfter(at);
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
            return _setBefore(Optional.of(before));
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
            setBefore(notAfter.after());

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
            setAfter(notBefore.before());

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return build().toString();
        }

        /**
         * Trims an interval to supplied limits.
         *
         * @param limits The limits.
         *
         * @return This.
         */
        @Nonnull
        public Builder trim(@Nonnull final TimeInterval limits)
        {
            final Optional<DateTime> limitsAfter = limits.getAfter();
            final DateTime trimAfter;

            if (limitsAfter.isPresent()
                    && (!_after.isPresent() || _after.get().isBefore(
                        limitsAfter.get()))) {
                trimAfter = limitsAfter.get();
            } else {
                trimAfter = null;
            }

            final Optional<DateTime> limitsBefore = limits.getBefore();
            final DateTime trimBefore;

            if (limitsBefore.isPresent()
                    && (!_before.isPresent() || _before.get().isAfter(
                        limitsBefore.get()))) {
                trimBefore = limitsBefore.get();
            } else {
                trimBefore = null;
            }

            if ((trimAfter != null) || (trimBefore != null)) {
                if (trimAfter != null) {
                    setAfter(trimAfter);
                }

                if (trimBefore != null) {
                    setBefore(trimBefore);
                }
            }

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public void writeExternal(
                @Nonnull final ObjectOutput output)
            throws IOException
        {
            DateTime.writeExternal(_after, output);
            DateTime.writeExternal(_before, output);
        }

        /**
         * Sets the after time.
         *
         * @param after The after time.
         *
         * @return This.
         */
        @Nonnull
        Builder _setAfter(@Nonnull final Optional<DateTime> after)
        {
            _after = after;

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
        Builder _setBefore(@Nonnull final Optional<DateTime> before)
        {
            _before = before;

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
        Builder _setNotAfter(@Nonnull final Optional<DateTime> notAfter)
        {
            _setBefore(
                notAfter.isPresent()? Optional
                    .of(notAfter.get().after()): Optional.empty());

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
        Builder _setNotBefore(@Nonnull final Optional<DateTime> notBefore)
        {
            _setAfter(
                notBefore.isPresent()? Optional
                    .of(notBefore.get().before()): Optional.empty());

            return this;
        }

        private static Optional<DateTime> _dateTime(String timeString)
        {
            timeString = timeString.trim();

            if (timeString.isEmpty() || "_".equals(timeString)) {
                return Optional.empty();
            }

            return Optional.of(DateTime.fromString(timeString));
        }

        private static TimeInterval _invalidIntervalFormat(
                final String intervalString)
        {
            throw new InvalidIntervalException(
                Message.format(BaseMessages.INTERVAL_FORMAT, intervalString));
        }

        private Object readResolve()
        {
            return build();
        }

        private Optional<DateTime> _after = Optional.empty();
        private Optional<DateTime> _before = Optional.empty();
    }


    /**
     * Invalid interval exception.
     */
    public static final class InvalidIntervalException
        extends RuntimeException
    {
        /**
         * Constructs an instance.
         *
         * @param message The Exception message.
         */
        public InvalidIntervalException(@Nonnull final String message)
        {
            super(message);
        }

        private static final long serialVersionUID = 1L;
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
