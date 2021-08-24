/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ElapsedTime.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import java.time.Duration;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;

/**
 * Elapsed time.
 *
 * <p>Implements elapsed time using the same time unit as {@link DateTime}: an
 * integer number of 100 nanoseconds.</p>
 *
 * <p>An elapsed time is immutable and never negative.</p>
 */
@Immutable
public final class ElapsedTime
    implements Serializable, Comparable<ElapsedTime>
{
    /**
     * Constructs an instance.
     *
     * @param raw The raw value.
     */
    private ElapsedTime(final long raw)
    {
        _raw = raw;
    }

    /**
     * Reads in an external representation of an elapsed time.
     *
     * @param source The source.
     *
     * @return The elapsed time (may be INVALID).
     *
     * @throws IOException When an I/O error occurs.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime fromDataInput(
            @Nonnull final DataInput source)
        throws IOException
    {
        final long raw = source.readLong();

        return (raw == INVALID.toRaw())? INVALID: _fromRaw(raw);
    }

    /**
     * Returns an elapsed time from a duration.
     *
     * @param duration The duration.
     *
     * @return The elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime fromDuration(final Duration duration)
    {
        final long secondsRaw = duration.getSeconds() * _SECOND_RAW;

        return fromRaw(secondsRaw + duration.getNano() / 100);
    }

    /**
     * Returns an elapsed time from a number of milliseconds.
     *
     * @param millis The number of milliseconds.
     *
     * @return The elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime fromMillis(final long millis)
    {
        return _fromRaw(millis * _MILLI_RAW);
    }

    /**
     * Returns an elapsed time from a number of nanoseconds.
     *
     * @param nanos The number of nanoseconds.
     *
     * @return The elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime fromNanos(final long nanos)
    {
        return _fromRaw(nanos / 100);
    }

    /**
     * Returns an elapsed time from an internal (raw) representation.
     *
     * @param raw The internal (raw) representation.
     *
     * @return The elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime fromRaw(final long raw)
    {
        return _fromRaw(raw);
    }

    /**
     * Returns an elapsed time from a number of seconds.
     *
     * @param seconds The number of seconds.
     *
     * @return The elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime fromSeconds(final double seconds)
    {
        final double raw = Math.abs(seconds) * _SECOND_RAW;

        return _fromRaw((raw < _INFINITY_RAW)? (long) raw: _INFINITY_RAW);
    }

    /**
     * Returns an elapsed time from an optional string representation.
     *
     * <p>A null or empty string will return null.</p>
     *
     * @param string The optional string representation.
     *
     * @return The optional elapsed time or empty.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<ElapsedTime> fromString(
            @Nonnull final Optional<String> string)
    {
        if (!string.isPresent()) {
            return Optional.empty();
        }

        final String elapsedString = string.get().trim();

        if (elapsedString.isEmpty() || (elapsedString.charAt(0) == '-')) {
            return Optional.empty();
        }

        return Optional.of(fromString(elapsedString));
    }

    /**
     * Returns an elapsed time from a valid string representation.
     *
     * @param elapsedString The string representation.
     *
     * @return The elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime fromString(@Nonnull final String elapsedString)
    {
        if (elapsedString.equalsIgnoreCase(INFINITY_STRING)) {
            return INFINITY;
        }

        Matcher matcher;

        matcher = _ELAPSED_PATTERN.matcher(elapsedString);

        if (matcher.matches()) {
            return _fromElapsedMatcher(matcher);
        }

        matcher = _SECONDS_PATTERN.matcher(elapsedString);

        if (matcher.matches()) {
            return _fromSecondsMatcher(matcher);
        }

        matcher = _DURATION_PATTERN.matcher(elapsedString);

        if (matcher.matches()) {
            return _fromDurationMatcher(matcher);
        }

        throw new IllegalArgumentException(
            Message.format(BaseMessages.ELAPSED_FORMAT, elapsedString));
    }

    /**
     * Returns the maximum ElapsedTime instance.
     *
     * <p>Note: a null value is considered infinite.</p>
     *
     * @param elapsedTimes The ElapsedTime instances.
     *
     * @return The maximum ElapsedTime instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime max(
            @Nonnull final Collection<ElapsedTime> elapsedTimes)
    {
        Require.failure(elapsedTimes.isEmpty());

        ElapsedTime max = EMPTY;

        for (final ElapsedTime elapsedTime: elapsedTimes) {
            max = max.max(Optional.ofNullable(elapsedTime));
        }

        return max;
    }

    /**
     * Returns the maximum ElapsedTime instance.
     *
     * <p>Note: a null value is considered infinite.</p>
     *
     * @param elapsedTimes The ElapsedTime instances.
     *
     * @return The maximum ElapsedTime instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime max(@Nonnull final ElapsedTime... elapsedTimes)
    {
        Require.success(elapsedTimes.length > 0);

        ElapsedTime max = EMPTY;

        for (final ElapsedTime elapsedTime: elapsedTimes) {
            max = max.max(Optional.ofNullable(elapsedTime));
        }

        return max;
    }

    /**
     * Returns the minimum ElapsedTime instance.
     *
     * <p>Note: a null value is considered infinite.</p>
     *
     * @param elapsedTimes The ElapsedTime instances.
     *
     * @return The minimum ElapsedTime instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime min(
            @Nonnull final Collection<ElapsedTime> elapsedTimes)
    {
        Require.failure(elapsedTimes.isEmpty());

        ElapsedTime min = INFINITY;

        for (final ElapsedTime elapsedTime: elapsedTimes) {
            min = min.min(Optional.ofNullable(elapsedTime));
        }

        return min;
    }

    /**
     * Returns the minimum ElapsedTime instance.
     *
     * <p>Note: a null value is considered infinite.</p>
     *
     * @param elapsedTimes The ElapsedTime instances.
     *
     * @return The minimum ElapsedTime instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ElapsedTime min(@Nonnull final ElapsedTime... elapsedTimes)
    {
        Require.success(elapsedTimes.length > 0);

        ElapsedTime min = INFINITY;

        for (final ElapsedTime elapsedTime: elapsedTimes) {
            min = min.min(Optional.ofNullable(elapsedTime));
        }

        return min;
    }

    /**
     * Reads in an external representation of an elapsed time.
     *
     * <p>This is a helper method for
     * {@link java.io.Externalizable Externalizable} objects containing an
     * ElapsedTime.</p>
     *
     * @param source The source.
     *
     * @return The elapsed time or empty.
     *
     * @throws IOException When an I/O error occurs.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<ElapsedTime> readExternal(
            @Nonnull final ObjectInput source)
        throws IOException
    {
        final ElapsedTime elapsedTime = fromDataInput(source);

        return elapsedTime
            .isInvalid()? Optional.empty(): Optional.of(elapsedTime);
    }

    /**
     * Writes out an external representation of an elapsed time.
     *
     * <p>This is a helper method for
     * {@link java.io.Externalizable Externalizable} objects containing an
     * ElapsedTime.</p>
     *
     * @param elapsedTime The elapsed time (may be empty).
     * @param destination The destination.
     *
     * @throws IOException When an I/O error occurs.
     */
    public static void writeExternal(
            @Nonnull final Optional<ElapsedTime> elapsedTime,
            @Nonnull final ObjectOutput destination)
        throws IOException
    {
        (elapsedTime.isPresent()? elapsedTime.get(): INVALID)
            .toDataOutput(destination);
    }

    /**
     * Adds an other elapsed time to this.
     *
     * @param other The other elapsed time.
     *
     * @return The combined elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime add(@Nonnull final ElapsedTime other)
    {
        if (isInfinity() || other.isInfinity()) {
            return INFINITY;
        }

        return _fromRaw(_raw + other._raw);
    }

    /** {@inheritDoc}
     */
    @Override
    public int compareTo(@Nonnull final ElapsedTime other)
    {
        return (_raw < other._raw)? -1: ((_raw > other._raw)? 1: 0);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (other instanceof ElapsedTime) {
            return _raw == ((ElapsedTime) other)._raw;
        }

        return false;
    }

    /**
     * Floors the scaled internal (raw) representation of this.
     *
     * @param whole An elapsed time representing a whole value.
     *
     * @return The scaled value.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime floor(@Nonnull final ElapsedTime whole)
    {
        return floor(whole.toRaw());
    }

    /**
     * Floors the scaled internal (raw) representation of this.
     *
     * @param whole The number of 100 nanoseconds representing a whole value.
     *
     * @return The scaled value.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime floor(final long whole)
    {
        return _fromRaw(isInfinity()? _INFINITY_RAW: (_raw / whole));
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return (int) (_raw ^ (_raw >>> Integer.SIZE));
    }

    /**
     * Asks if this is an empty interval.
     *
     * @return A true if this interval is empty.
     */
    @CheckReturnValue
    public boolean isEmpty()
    {
        return _raw == 0;
    }

    /**
     * Asks if this is infinity.
     *
     * @return A true value if this is infinity.
     */
    @CheckReturnValue
    public boolean isInfinity()
    {
        return _raw == _INFINITY_RAW;
    }

    /**
     * Asks if this is the invalid elapsed time.
     *
     * @return True if this is the invalid elapsed time.
     */
    @CheckReturnValue
    public boolean isInvalid()
    {
        return this == INVALID;
    }

    /**
     * Returns the maximum between this and an other.
     *
     * <p>Note: an empty value is considered infinite.</p>
     *
     * @param other The other.
     *
     * @return The maximum elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime max(@Nonnull final Optional<ElapsedTime> other)
    {
        if (!other.isPresent()) {
            return INFINITY;
        }

        return (compareTo(other.get()) > 0)? this: other.get();
    }

    /**
     * Returns the minimum between this and an other.
     *
     * <p>Note: an empty value is considered infinite.</p>
     *
     * @param other The other.
     *
     * @return The minimum elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime min(@Nonnull final Optional<ElapsedTime> other)
    {
        if (!other.isPresent()) {
            return this;
        }

        return (compareTo(other.get()) < 0)? this: other.get();
    }

    /**
     * Multiplies this by a factor.
     *
     * @param factor The factor.
     *
     * @return The resulting elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime mul(final double factor)
    {
        return fromRaw((long) (_raw * factor));
    }

    /**
     * Multiplies this by a factor.
     *
     * @param factor The factor.
     *
     * @return The resulting elapsed time.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime mul(final long factor)
    {
        return fromRaw(_raw * factor);
    }

    /**
     * Returns the ratio of this to an other elapsed time.
     *
     * @param divisor The other elapsed time.
     *
     * @return The ratio.
     */
    @CheckReturnValue
    public double ratio(@Nonnull final ElapsedTime divisor)
    {
        if (divisor.isInfinity()) {
            return isInfinity()? Double.NaN: Double.MIN_VALUE;
        }

        if (isInfinity()) {
            return Double.POSITIVE_INFINITY;
        }

        return _raw / (double) divisor._raw;
    }

    /**
     * Returns the scaled internal (raw) representation of this.
     *
     * @param whole An elapsed time representing a whole value.
     *
     * @return The scaled value.
     */
    @CheckReturnValue
    public double scaled(@Nonnull final ElapsedTime whole)
    {
        return scaled(whole.toRaw());
    }

    /**
     * Returns the scaled internal (raw) representation of this.
     *
     * @param whole The number of 100 nanoseconds representing a whole value.
     *
     * @return The scaled value.
     */
    @CheckReturnValue
    public double scaled(final long whole)
    {
        return isInfinity()
               ? Double.POSITIVE_INFINITY: (((double) _raw) / whole);
    }

    /**
     * Substracts an other elapsed time from this.
     *
     * <p>Subtracting from infinity always results in infinity.</p>
     *
     * <p>Subtracting infinity from non infinity always results in none.</p>
     *
     * <p>The substraction results in the absolute value of the time
     * difference.</p>
     *
     * @param other The other elapsed time.
     *
     * @return The elapsed times difference.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime sub(@Nonnull final ElapsedTime other)
    {
        return isInfinity()? this: (other
            .isInfinity()? EMPTY: _fromRaw(_raw - other._raw));
    }

    /**
     * Sends to a data output.
     *
     * @param destination The destination.
     *
     * @throws IOException On I/O exception.
     */
    public void toDataOutput(
            @Nonnull final DataOutput destination)
        throws IOException

    {
        destination.writeLong(_raw);
    }

    /**
     * Returns a duration.
     *
     * @return The duration.
     */
    @Nonnull
    @CheckReturnValue
    public Duration toDuration()
    {
        return Duration
            .ofSeconds(_raw / _SECOND_RAW, (_raw % _SECOND_RAW) * 100);
    }

    /**
     * Returns this as a duration string.
     *
     * @return This as a duration string.
     */
    @Nonnull
    @CheckReturnValue
    public String toDurationString()
    {
        final StringBuilder stringBuilder = new StringBuilder("P");
        long raw = _raw;
        int integer;

        integer = (int) (raw / (24 * _HOUR_RAW));

        if (integer > 0) {
            stringBuilder.append(integer);
            raw -= integer * 24 * _HOUR_RAW;
            stringBuilder.append('D');
        }

        if (raw > 0) {
            stringBuilder.append('T');

            integer = (int) (raw / (_HOUR_RAW));

            if (integer > 0) {
                stringBuilder.append(integer);
                raw -= integer * _HOUR_RAW;
                stringBuilder.append('H');
            }

            integer = (int) (raw / (_MINUTE_RAW));

            if (integer > 0) {
                stringBuilder.append(integer);
                raw -= integer * _MINUTE_RAW;
                stringBuilder.append('M');
            }

            if (raw > 0) {
                integer = (int) (raw / (_SECOND_RAW));
                stringBuilder.append(integer);
                raw -= integer * _SECOND_RAW;

                if (raw > 0) {
                    stringBuilder.append('.');
                    stringBuilder
                        .append(
                            DateTime.Context.nanosToString((int) (raw * 100)));
                }

                stringBuilder.append('S');
            }
        }

        if (stringBuilder.length() == 1) {
            stringBuilder.append("0D");
        }

        return stringBuilder.toString();
    }

    /**
     * Returns this in millis.
     *
     * @return This in millis.
     */
    @Nonnegative
    @CheckReturnValue
    public long toMillis()
    {
        return isInfinity()? _INFINITY_RAW: (_raw / _MILLI_RAW);
    }

    /**
     * Returns the internal (raw) representation of this.
     *
     * @return A number of 100 nanoseconds.
     */
    @Nonnegative
    @CheckReturnValue
    public long toRaw()
    {
        return _raw;
    }

    /**
     * Returns this in seconds.
     *
     * @return This in seconds.
     */
    @Nonnegative
    @CheckReturnValue
    public double toSeconds()
    {
        return isInfinity()? _INFINITY_RAW: (_raw / (double) _SECOND_RAW);
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        if (isInfinity()) {
            return INFINITY_STRING;
        }

        final StringBuilder stringBuilder = new StringBuilder("P");

        stringBuilder.append((int) (_raw / (24 * _HOUR_RAW)));
        stringBuilder.append('T');
        stringBuilder
            .append(
                DateTime.Context
                    .formatNumber((int) ((_raw / _HOUR_RAW) % 24), 2));
        stringBuilder.append(':');
        stringBuilder
            .append(
                DateTime.Context
                    .formatNumber((int) ((_raw / _MINUTE_RAW) % 60), 2));

        final int seconds = (int) ((_raw / _SECOND_RAW) % 60);
        final int nanos = (int) ((_raw % _SECOND_RAW) * 100);

        if ((seconds > 0) || (nanos > 0)) {
            stringBuilder.append(':');
            stringBuilder.append(DateTime.Context.formatNumber(seconds, 2));

            if (nanos > 0) {
                stringBuilder.append('.');
                stringBuilder.append(DateTime.Context.nanosToString(nanos));
            }
        }

        return stringBuilder.toString();
    }

    private static ElapsedTime _fromDurationMatcher(final Matcher matcher)
    {
        long raw = 0;
        String string;

        string = matcher.group(_DurationGroup.DAYS.group());
        raw += (string != null)? (Integer.parseInt(string) * 24 * _HOUR_RAW): 0;

        string = matcher.group(_DurationGroup.HOURS.group());
        raw += (string != null)? (Integer.parseInt(string) * _HOUR_RAW): 0;

        string = matcher.group(_DurationGroup.MINUTES.group());
        raw += (string != null)? (Integer.parseInt(string) * _MINUTE_RAW): 0;

        string = matcher.group(_DurationGroup.SECONDS.group());
        raw += (string != null)? (Integer.parseInt(string) * _SECOND_RAW): 0;

        string = matcher.group(_DurationGroup.NANOS.group());
        raw += (string != null)? (DateTime.Context
            .nanosFromString(string) / 100): 0;

        return _fromRaw(raw);
    }

    private static ElapsedTime _fromElapsedMatcher(final Matcher matcher)
    {
        long raw;
        String string;

        string = matcher.group(_ElapsedGroup.DAYS.group());

        if (string != null) {
            final int integer = Integer.parseInt(string);

            if ((matcher.group(_ElapsedGroup.PERIOD.group()) != null)
                    || (matcher.group(
                        _ElapsedGroup.SEPARATOR.group()) != null)) {
                raw = integer * 24 * _HOUR_RAW;
            } else {    // Interprets as milliseconds.
                return new ElapsedTime(integer * _MILLI_RAW);
            }
        } else {
            raw = 0;
        }

        string = matcher.group(_ElapsedGroup.HOURS.group());
        raw += (string != null)? (Integer.parseInt(string) * _HOUR_RAW): 0;

        string = matcher.group(_ElapsedGroup.MINUTES.group());
        raw += (string != null)? (Integer.parseInt(string) * _MINUTE_RAW): 0;

        string = matcher.group(_ElapsedGroup.SECONDS.group());
        raw += (string != null)? (Integer.parseInt(string) * _SECOND_RAW): 0;

        string = matcher.group(_ElapsedGroup.NANOS.group());
        raw += (string != null)? (DateTime.Context
            .nanosFromString(string) / 100): 0;

        return new ElapsedTime(raw);
    }

    private static ElapsedTime _fromRaw(long raw)
    {
        raw = Math.abs(raw);

        return new ElapsedTime((raw >= 0)? raw: _INFINITY_RAW);
    }

    private static ElapsedTime _fromSecondsMatcher(final Matcher matcher)
    {
        long raw;

        raw = Integer
            .parseInt(
                matcher.group(_SecondsGroup.SECONDS.group())) * _SECOND_RAW;
        raw += DateTime.Context
            .nanosFromString(matcher.group(_SecondsGroup.NANOS.group())) / 100;

        return new ElapsedTime(raw);
    }

    /** Empty. */
    public static final ElapsedTime EMPTY = new ElapsedTime(0);

    /** One hour. */
    public static final ElapsedTime HOUR;

    /** Infinity. */
    public static final ElapsedTime INFINITY;

    /** Infinity string representation. */
    public static final String INFINITY_STRING = "Infinity";

    /** Invalid elsped time representation. */
    public static final ElapsedTime INVALID;

    /** One microsecond. */
    public static final ElapsedTime MICRO;

    /** One millisecond. */
    public static final ElapsedTime MILLI;

    /** One minute. */
    public static final ElapsedTime MINUTE;

    /** One second. */
    public static final ElapsedTime SECOND;

    /**  */

    private static final long _HOUR_RAW = 36000000000L;
    private static final long _INVALID_RAW = Long.MIN_VALUE;
    private static final long _INFINITY_RAW = Long.MAX_VALUE;
    private static final long _MICRO_RAW = 10L;
    private static final long _MILLI_RAW = 10000L;
    private static final long _MINUTE_RAW = 600000000L;
    private static final long _SECOND_RAW = 10000000L;

    /**  */

    private static final long serialVersionUID = 1L;

    /**  */

    private static final Pattern _SECONDS_PATTERN = Pattern
        .compile("([0-9]++)[.]([0-9]+)");
    private static final Pattern _ELAPSED_PATTERN = Pattern
        .compile(
            "(P)?(?:([0-9]++)(?:(T|_|-|[ ]+)|$))??" + "(?:([0-9]++):([0-9]++)"
            + "(?::([0-9]++)(?:[.]([0-9]+))?)?)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern _DURATION_PATTERN = Pattern
        .compile(
            "P(?:([0-9]+)D)?T?"
            + "(?:([0-9]+)H)?(?:([0-9]+)M)?(?:([0-9]++)(?:[.]([0-9]))?S)?",
            Pattern.CASE_INSENSITIVE);

    static {
        HOUR = new ElapsedTime(_HOUR_RAW);
        INFINITY = new ElapsedTime(_INFINITY_RAW);
        INVALID = new ElapsedTime(_INVALID_RAW);
        MICRO = new ElapsedTime(_MICRO_RAW);
        MILLI = new ElapsedTime(_MILLI_RAW);
        MINUTE = new ElapsedTime(_MINUTE_RAW);
        SECOND = new ElapsedTime(_SECOND_RAW);
    }

    private final long _raw;

    /**
     * Duration groups.
     */
    private enum _DurationGroup
    {
        DAYS,
        HOURS,
        MINUTES,
        SECONDS,
        NANOS;

        /**
         * Returns the group number.
         *
         * @return The group number.
         */
        int group()
        {
            return ordinal() + 1;
        }
    }

    /**
     * Elapsed groups.
     */
    private enum _ElapsedGroup
    {
        PERIOD,
        DAYS,
        SEPARATOR,
        HOURS,
        MINUTES,
        SECONDS,
        NANOS;

        /**
         * Returns the group number.
         *
         * @return The group number.
         */
        int group()
        {
            return ordinal() + 1;
        }
    }

    /**
     * Seconds groups.
     */
    private enum _SecondsGroup
    {
        SECONDS,
        NANOS;

        /**
         * Returns the group number.
         *
         * @return The group number.
         */
        int group()
        {
            return ordinal() + 1;
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
