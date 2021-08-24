/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DateTime.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.sql.Timestamp;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import java.util.Collection;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;

/**
 * Date and time.
 *
 * <p>Implements a date and time object based on the Modified Julian Date
 * (introduced by the Smithsonian Astrophysical Observatory in 1957). Its
 * internal representation is a 64 bits integer (long) holding the number of 100
 * nanoseconds since 1858-11-17 00:00 UTC.</p>
 *
 * <p>That representation has been chosen to simplify time manipulations in a
 * timed events database. The fondness of the author for OpenVMS may also have
 * something to do with it.</p>
 *
 * <p>This implementation uses negative values to represent times before
 * 1858-11-17 00:00 UTC. The years before 1 A.D. are numbered according to
 * astronomical conventions: e.g. 2 B.C is formatted as -0001. This allows the
 * representation of times from 12754 B.C. to 16472 A.D.</p>
 *
 * <p>The limits have been set to half the possible range to avoid an elapsed
 * time overflow when subtracting two time values.</p>
 */
@Immutable
public final class DateTime
    implements Serializable, Comparable<DateTime>
{
    /**
     * Constructs an instance.
     *
     * @param raw The raw value.
     */
    private DateTime(final long raw)
    {
        _raw = raw;
    }

    /**
     * Returns a DateTime from a Timestamp.
     *
     * @param timestamp The Timestamp.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime at(@Nonnull final Timestamp timestamp)
    {
        final long milliRaw = ElapsedTime.MILLI.toRaw();
        long time = timestamp.getTime() * milliRaw;

        time += (timestamp.getNanos() / 100) % milliRaw;

        return _fromRaw(time + _UNIX_EPOCH_RAW);
    }

    /**
     * Clears the simulated time.
     */
    public static void clearSimulatedTime()
    {
        _simulatedTime = null;
    }

    /**
     * Returns the default context.
     *
     * @return The default context.
     */
    @Nonnull
    @CheckReturnValue
    public static Context defaultContext()
    {
        return _defaultContext;
    }

    /**
     * Returns a DateTime from a bytes representation.
     *
     * @param bytes The bytes representation.
     *
     * @return The DateTime.
     *
     * @throws IllegalArgumentException When the time value is out of range.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime fromBytes(@Nonnull final byte[] bytes)
    {
        long raw = 0;

        for (int i = 0; i < bytes.length; ++i) {
            raw <<= 8;
            raw |= bytes[i] & 0xFF;
        }

        return _fromRaw(raw);
    }

    /**
     * Reads in an external representation of a DateTime.
     *
     * @param source The source.
     *
     * @return The DateTime (may be INVALID).
     *
     * @throws IOException When an I/O error occurs.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime fromDataInput(
            @Nonnull final DataInput source)
        throws IOException
    {
        final long raw = source.readLong();

        return (raw == INVALID.toRaw())? INVALID: _fromRaw(raw);
    }

    /**
     * Returns a DateTime built from fields.
     *
     * @param fields The fields.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime fromFields(@Nonnull final Fields fields)
    {
        return defaultContext().fromFields(fields);
    }

    /**
     * Returns a DateTime built from an Instant.
     *
     * @param instant The Instant.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime fromInstant(@Nonnull final Instant instant)
    {
        final long secondsRaw = instant
            .getEpochSecond() * ElapsedTime.SECOND.toRaw();
        final long unixRaw = secondsRaw + instant.getNano() / 100;

        return _fromRaw(unixRaw + _UNIX_EPOCH_RAW);
    }

    /**
     * Returns a DateTime for the specified number of milliseconds since
     * 1970-01-01 00:00:00.
     *
     * @param millis The number of milliseconds since 1970-01-01 00:00:00.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime fromMillis(final long millis)
    {
        return _fromRaw((millis * ElapsedTime.MILLI.toRaw()) + _UNIX_EPOCH_RAW);
    }

    /**
     * Returns a DateTime from an internal (raw) representation.
     *
     * @param raw The internal (raw) representation.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime fromRaw(final long raw)
    {
        return _fromRaw(raw);
    }

    /**
     * Returns a DateTime decoded from a time string.
     *
     * <p>It accepts as input the output of {@link #toString()},
     * {@link #toFullString()}, {@link #toHexString()} or
     * {@link Timestamp#toString()}. This input may also be null or empty.</p>
     *
     * @param timeString The optional time string.
     *
     * @return The optional DateTime.
     *
     * @throws IllegalArgumentException When the time string is invalid.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<DateTime> fromString(
            @Nonnull final Optional<String> timeString)
    {
        return (timeString.isPresent()
                && !timeString.get().isEmpty())? Optional
                    .of(fromString(timeString.get())): Optional.empty();
    }

    /**
     * Returns a DateTime decoded from a time string.
     *
     * <p>It accepts as input the output of {@link #toString()},
     * {@link #toFullString()}, {@link #toHexString()} or
     * {@link Timestamp#toString()}. This input must not be null or empty.</p>
     *
     * @param timeString The time string.
     *
     * @return The DateTime.
     *
     * @throws IllegalArgumentException When the time string is invalid.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime fromString(@Nonnull final String timeString)
    {
        return defaultContext().fromString(timeString);
    }

    /**
     * Returns a DateTime from Win32::FileTime.
     *
     * @param win32 The Win32::FileTime.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime fromWin32(final long win32)
    {
        return _fromRaw(win32 + _WINDOWS_EPOCH_RAW);
    }

    /**
     * Returns a DateTime built from a ZonedDateTime.
     *
     * @param zonedDateTime The ZonedDateTime.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime fromZonedDateTime(
            @Nonnull final ZonedDateTime zonedDateTime)
    {
        return fromInstant(zonedDateTime.toInstant());
    }

    /**
     * Gets the time zone.
     *
     * @return The time zone.
     */
    @Nonnull
    @CheckReturnValue
    public static TimeZone getTimeZone()
    {
        return defaultContext().getTimeZone();
    }

    /**
     * Gets the ZoneId.
     *
     * @return The ZoneId.
     */
    @Nonnull
    @CheckReturnValue
    public static ZoneId getZoneId()
    {
        return defaultContext().getZoneId();
    }

    /**
     * Returns the maximum DateTime instance.
     *
     * @param dateTimes The DateTime instances.
     *
     * @return The maximum DateTime instance.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime max(@Nonnull final Collection<DateTime> dateTimes)
    {
        Require.failure(dateTimes.isEmpty());

        DateTime max = null;

        for (final DateTime dateTime: dateTimes) {
            if ((max == null) || dateTime.isAfter(max)) {
                max = dateTime;
            }
        }

        return max;
    }

    /**
     * Returns the maximum DateTime instance.
     *
     * @param dateTimes The DateTime instances.
     *
     * @return The maximum DateTime instance.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime max(@Nonnull final DateTime... dateTimes)
    {
        Require.success(dateTimes.length > 0);

        DateTime max = null;

        for (final DateTime dateTime: dateTimes) {
            if ((max == null) || dateTime.isAfter(max)) {
                max = dateTime;
            }
        }

        return max;
    }

    /**
     * Returns the minimum DateTime instance.
     *
     * @param dateTimes The DateTime instances.
     *
     * @return The minimum DateTime instance.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime min(@Nonnull final Collection<DateTime> dateTimes)
    {
        Require.failure(dateTimes.isEmpty());

        DateTime min = null;

        for (final DateTime dateTime: dateTimes) {
            if ((min == null) || dateTime.isBefore(min)) {
                min = dateTime;
            }
        }

        return min;
    }

    /**
     * Returns the minimum DateTime instance.
     *
     * @param dateTimes The DateTime instances.
     *
     * @return The minimum DateTime instance.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime min(@Nonnull final DateTime... dateTimes)
    {
        Require.success(dateTimes.length > 0);

        DateTime min = null;

        for (final DateTime dateTime: dateTimes) {
            if ((min == null) || dateTime.isBefore(min)) {
                min = dateTime;
            }
        }

        return min;
    }

    /**
     * Returns a DateTime for the current or simulated time.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime now()
    {
        final DateTime simulated = _simulatedTime;

        return (simulated == null)? fromMillis(
            System.currentTimeMillis()): simulated;
    }

    /**
     * Reads in an external representation of a DateTime.
     *
     * <p>This is a helper method for
     * {@link java.io.Externalizable Externalizable} objects containing a
     * DateTime.</p>
     *
     * @param source The source.
     *
     * @return The optional DateTime.
     *
     * @throws IOException When an I/O error occurs.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<DateTime> readExternal(
            @Nonnull final ObjectInput source)
        throws IOException
    {
        final DateTime dateTime = fromDataInput(source);

        return dateTime.isInvalid()? Optional.empty(): Optional.of(dateTime);
    }

    /**
     * Resets the time zone.
     */
    public static void resetTimeZone()
    {
        _defaultContext = _DEFAULT_CONTEXT;
    }

    /**
     * Simulates the supplied time as current.
     *
     * <p>This is used by tests scenarios.</p>
     *
     * @param simulatedTime The simulated time.
     */
    public static void simulateTime(@Nonnull final DateTime simulatedTime)
    {
        _simulatedTime = Require.notNull(simulatedTime);
    }

    /**
     * Simulates the supplied time zone.
     *
     * @param timeZone The simulated time zone.
     */
    public static void simulateTimeZone(@Nonnull final TimeZone timeZone)
    {
        _defaultContext = new Context(timeZone);
    }

    /**
     * Writes out an external representation of a DateTime.
     *
     * <p>This is a helper method for
     * {@link java.io.Externalizable Externalizable} objects containing a
     * DateTime.</p>
     *
     * @param dateTime The optional DateTime.
     * @param destination The destination.
     *
     * @throws IOException When an I/O error occurs.
     */
    public static void writeExternal(
            @Nonnull final Optional<DateTime> dateTime,
            @Nonnull final ObjectOutput destination)
        throws IOException
    {
        (dateTime.isPresent()? dateTime.get(): INVALID)
            .toDataOutput(destination);
    }

    /**
     * Returns a DateTime which would be just after this.
     *
     * <p>Note: if this is either the beginning or the end of time, the time
     * change is skipped.</p>
     *
     * @return The DateTime after this.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime after()
    {
        if ((_raw == _BEGINNING_OF_TIME_RAW) || (_raw == _END_OF_TIME_RAW)) {
            return this;
        }

        return after(1);
    }

    /**
     * Returns the higher DateTime between this and after a limit.
     *
     * @param limit The limit.
     *
     * @return The higher of this and after the provided limit.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime after(@Nonnull final DateTime limit)
    {
        return isAfter(limit)? this: limit.after();
    }

    /**
     * Returns a DateTime which would be some time after this.
     *
     * @param elapsed An elapsed time.
     *
     * @return The DateTime after this.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime after(@Nonnull final ElapsedTime elapsed)
    {
        return elapsed.isInfinity()? END_OF_TIME: after(elapsed.toRaw());
    }

    /**
     * Returns a DateTime which would be at an interval after this.
     *
     * @param interval A number of 100 nanoseconds.
     *
     * @return The DateTime after this.
     *
     * @throws IllegalArgumentException When the time value is out of range.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime after(final long interval)
    {
        return (interval < 0)? before(-interval): _fromRaw(_raw + interval);
    }

    /**
     * Returns a DateTime which would be some time after this. /** Returns a
     * DateTime which would be just before this.
     *
     * <p>Note: if this is either the beginning or the end of time, the time
     * change is skipped.</p>
     *
     * @return The DateTime before this.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime before()
    {
        if ((_raw == _BEGINNING_OF_TIME_RAW) || (_raw == _END_OF_TIME_RAW)) {
            return this;
        }

        return before(1);
    }

    /**
     * Returns the lower DateTime between this and before a limit.
     *
     * @param limit The limit.
     *
     * @return The lower of this and before the provided limit.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime before(@Nonnull final DateTime limit)
    {
        return isBefore(limit)? this: limit.before();
    }

    /**
     * Returns a DateTime which would be some time before this.
     *
     * @param elapsed An elapsed time.
     *
     * @return The DateTime before this.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime before(@Nonnull final ElapsedTime elapsed)
    {
        return elapsed.isInfinity()? BEGINNING_OF_TIME: before(elapsed.toRaw());
    }

    /**
     * Returns a DateTime which would be at an interval before this.
     *
     * @param interval A number of 100 nanoseconds.
     *
     * @return The DateTime before this.
     *
     * @throws IllegalArgumentException When the interval or time value is out
     *                                  of range.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime before(final long interval)
    {
        if (interval < 0) {
            if (interval == Long.MIN_VALUE) {
                throw new IllegalArgumentException(
                    Message.format(BaseMessages.TIME_INTERVAL));
            }

            return after(-interval);
        }

        return _fromRaw(_raw - interval);
    }

    /** {@inheritDoc}
     */
    @Override
    public int compareTo(final DateTime other)
    {
        return Long.compare(toRaw(), other.toRaw());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (other instanceof DateTime) {
            return toRaw() == ((DateTime) other).toRaw();
        }

        return false;
    }

    /**
     * Returns a floored DateTime.
     *
     * @param whole An ElapsedTime representing a whole value.
     *
     * @return The floored DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime floored(@Nonnull final ElapsedTime whole)
    {
        return floored(whole.toRaw());
    }

    /**
     * Returns a floored DateTime based on this with respect to a given time
     * interval.
     *
     * @param whole An internal representation (raw) of a whole value.
     *
     * @return The floored DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime floored(final long whole)
    {
        Require.success(whole > 0);

        return before((_raw >= 0)? (_raw % whole): (whole + (_raw % whole)));
    }

    /**
     * Gets the day of week.
     *
     * @return The day of week (0 - 6 where 0 is Sunday).
     */
    @CheckReturnValue
    public int getDayOfWeek()
    {
        return defaultContext().getDayOfWeek(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return (int) (_raw ^ (_raw >>> Integer.SIZE));
    }

    /**
     * Asks if this is after an other DateTime.
     *
     * @param other An other DateTime.
     *
     * @return A true value if this is after.
     */
    @CheckReturnValue
    public boolean isAfter(@Nonnull final DateTime other)
    {
        return toRaw() > other.toRaw();
    }

    /**
     * Asks if this is after a TimeInterval.
     *
     * @param interval The TimeInterval.
     *
     * @return A true value if this is after.
     */
    @CheckReturnValue
    public boolean isAfter(@Nonnull final TimeInterval interval)
    {
        final Optional<DateTime> before = interval.getBefore();

        return before.isPresent() && (_raw >= before.get().toRaw());
    }

    /**
     * Asks if this is at the beginning of a TimeInterval.
     *
     * @param interval The TimeInterval.
     *
     * @return A true value if this is at the beginning.
     */
    @CheckReturnValue
    public boolean isAtBeginning(@Nonnull final TimeInterval interval)
    {
        final Optional<DateTime> after = interval.getAfter();

        return after.isPresent() && (_raw == (after.get().toRaw() + 1));
    }

    /**
     * Asks if this is at the end of a TimeInterval.
     *
     * @param interval The TimeInterval.
     *
     * @return A true value if this is at the end.
     */
    @CheckReturnValue
    public boolean isAtEnd(@Nonnull final TimeInterval interval)
    {
        final Optional<DateTime> before = interval.getBefore();

        return before.isPresent() && (_raw == (before.get().toRaw() - 1));
    }

    /**
     * Asks if this is before an other DateTime.
     *
     * @param other An other DateTime.
     *
     * @return A true value if this is before.
     */
    @CheckReturnValue
    public boolean isBefore(@Nonnull final DateTime other)
    {
        return toRaw() < other.toRaw();
    }

    /**
     * Asks if this is before a TimeInterval.
     *
     * @param interval The TimeInterval.
     *
     * @return A true value if this is before.
     */
    @CheckReturnValue
    public boolean isBefore(@Nonnull final TimeInterval interval)
    {
        final Optional<DateTime> after = interval.getAfter();

        return after.isPresent() && (_raw <= after.get().toRaw());
    }

    /**
     * Asks if this is the beginning of time.
     *
     * @return A true value if this is the beginning of time.
     */
    @CheckReturnValue
    public boolean isBeginningOfTime()
    {
        return _raw == _BEGINNING_OF_TIME_RAW;
    }

    /**
     * Asks if this is the end of time.
     *
     * @return A true value if this is the end of time.
     */
    @CheckReturnValue
    public boolean isEndOfTime()
    {
        return _raw == _END_OF_TIME_RAW;
    }

    /**
     * Asks if this is inside a TimeInterval.
     *
     * @param interval The TimeInterval.
     *
     * @return A true value if this is inside.
     */
    @CheckReturnValue
    public boolean isInside(@Nonnull final TimeInterval interval)
    {
        final Optional<DateTime> after = interval.getAfter();

        if (!after.isPresent() || (_raw > after.get().toRaw())) {
            final Optional<DateTime> before = interval.getBefore();

            if (!before.isPresent() || (_raw < before.get().toRaw())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Asks if this is the invalid DateTime.
     *
     * @return True if this is the invalid DateTime.
     */
    @CheckReturnValue
    public boolean isInvalid()
    {
        return this == INVALID;
    }

    /**
     * Asks if this is not after an other DateTime.
     *
     * @param other An other DateTime.
     *
     * @return A true value if this is not after.
     */
    @CheckReturnValue
    public boolean isNotAfter(@Nonnull final DateTime other)
    {
        return toRaw() <= other.toRaw();
    }

    /**
     * Asks if this is not after a TimeInterval.
     *
     * @param interval The TimeInterval.
     *
     * @return A true value if this is not after.
     */
    @CheckReturnValue
    public boolean isNotAfter(@Nonnull final TimeInterval interval)
    {
        final Optional<DateTime> before = interval.getBefore();

        return !before.isPresent() || (_raw < before.get().toRaw());
    }

    /**
     * Asks if this is not before an other DateTime.
     *
     * @param other An other DateTime.
     *
     * @return A true value if this is not before.
     */
    @CheckReturnValue
    public boolean isNotBefore(@Nonnull final DateTime other)
    {
        return toRaw() >= other.toRaw();
    }

    /**
     * Asks if this is not before a TimeInterval.
     *
     * @param interval The TimeInterval.
     *
     * @return A true value if this is not before.
     */
    @CheckReturnValue
    public boolean isNotBefore(@Nonnull final TimeInterval interval)
    {
        final Optional<DateTime> after = interval.getAfter();

        return !after.isPresent() || (_raw > after.get().toRaw());
    }

    /**
     * Returns a DateTime from this at midnight.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime midnight()
    {
        return defaultContext().midnight(this);
    }

    /**
     * Returns a DateTime from this at midnight on the next day.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime nextDay()
    {
        return defaultContext().nextDay(this);
    }

    /**
     * Returns a DateTime from this at noon.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime noon()
    {
        return defaultContext().noon(this);
    }

    /**
     * Returns the lower DateTime between this and a limit.
     *
     * @param limit The limit.
     *
     * @return The lower of this and the provided limit.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime notAfter(@Nonnull final DateTime limit)
    {
        return isNotAfter(limit)? this: limit;
    }

    /**
     * Returns the higher DateTime between this and a limit.
     *
     * @param limit The limit.
     *
     * @return The higher of this and the provided limit.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime notBefore(@Nonnull final DateTime limit)
    {
        return isNotBefore(limit)? this: limit;
    }

    /**
     * Returns a DateTime from this at midnight on the previous day.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime previousDay()
    {
        return defaultContext().previousDay(this);
    }

    /**
     * Returns a rounded DateTime.
     *
     * @param whole An ElapsedTime representing a whole value.
     *
     * @return The rounded DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime rounded(@Nonnull final ElapsedTime whole)
    {
        return rounded(whole.toRaw());
    }

    /**
     * Returns a rounded DateTime.
     *
     * @param whole An internal representation (raw) of a whole value.
     *
     * @return The rounded DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime rounded(long whole)
    {
        Require.success(whole > 0);

        if (_raw < 0) {
            whole = -whole;
        }

        return _fromRaw(Math.round(_raw / (double) whole) * whole);
    }

    /**
     * Returns a scaled representation of time.
     *
     * @param whole An ElapsedTime representing a whole value.
     *
     * @return The scaled representation of time.
     */
    @Nonnull
    @CheckReturnValue
    public double scaled(@Nonnull final ElapsedTime whole)
    {
        return scaled(whole.toRaw());
    }

    /**
     * Returns a scaled representation of time.
     *
     * @param whole An internal representation (raw) of a whole value.
     *
     * @return The scaled representation of time.
     */
    @CheckReturnValue
    public double scaled(final long whole)
    {
        return ((double) _raw) / whole;
    }

    /**
     * Substracts an other DateTime from this.
     *
     * @param other The other DateTime.
     *
     * @return The resulting ElapsedTime.
     */
    @Nonnull
    @CheckReturnValue
    public ElapsedTime sub(@Nonnull final DateTime other)
    {
        return ElapsedTime.fromRaw(toRaw() - other.toRaw());
    }

    /**
     * Provides a base string representation of a this.
     *
     * <p>Generates a base string in ISO 8601 format adjusted to astronomical
     * conventions.</p>
     *
     * @return The base string representation of this.
     */
    @Nonnull
    @CheckReturnValue
    public String toBaseString()
    {
        return defaultContext().toBaseString(this);
    }

    /**
     * Returns a bytes representation of itself.
     *
     * @return A bytes representation of itself.
     */
    @Nonnull
    @CheckReturnValue
    public byte[] toBytes()
    {
        final byte[] bytes = new byte[8];

        bytes[0] = (byte) (_raw >>> 56);
        bytes[1] = (byte) (_raw >>> 48);
        bytes[2] = (byte) (_raw >>> 40);
        bytes[3] = (byte) (_raw >>> 32);
        bytes[4] = (byte) (_raw >>> 24);
        bytes[5] = (byte) (_raw >>> 16);
        bytes[6] = (byte) (_raw >>> 8);
        bytes[7] = (byte) (_raw >>> 0);

        return bytes;
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
     * Returns this as fields.
     *
     * @return The fields.
     */
    @Nonnull
    @CheckReturnValue
    public Fields toFields()
    {
        return defaultContext().toFields(this);
    }

    /**
     * Provides a String representation of this usable as a file name.
     *
     * <p>To make sure that the lexical and chronological order of the
     * representations are in sync, the time zone is set to GMT. The
     * representation is then produced from a full String representation with
     * the '-' and ':' characters removed.</p>
     *
     * @return A String usable as a file name.
     */
    @Nonnull
    @CheckReturnValue
    public String toFileName()
    {
        final String fileName = UTC_CONTEXT._toString(this, true, false, false);

        if (fileName.startsWith("-")) {
            throw new IllegalArgumentException(
                Message.format(
                    BaseMessages.TIME_VALUE,
                    String.valueOf(toRaw())));
        }

        return fileName;
    }

    /**
     * Provides a full extended string representation of this.
     *
     * <p>Generates an extended tring in ISO 8601 format adjusted to
     * astronomical conventions.</p>
     *
     * @return The string representation of this.
     */
    @Nonnull
    @CheckReturnValue
    public String toFullString()
    {
        return defaultContext().toFullString(this);
    }

    /**
     * Provides an hexadecimal String representation of the internal value.
     *
     * @return The hexadecimal String representation of the internal value.
     */
    @Nonnull
    @CheckReturnValue
    public String toHexString()
    {
        return (_raw >= 0)? ("0X" + Long.toHexString(
            _raw)): ("-0X" + Long.toHexString(-_raw));
    }

    /**
     * Returns an Instant representation.
     *
     * @return The Instant representation.
     */
    @Nonnull
    @CheckReturnValue
    public Instant toInstant()
    {
        final long unixRaw = _raw - _UNIX_EPOCH_RAW;
        final long secondRaw = ElapsedTime.SECOND.toRaw();
        final long nanos = (unixRaw % secondRaw) * 100;
        final long seconds = unixRaw / secondRaw;

        return Instant.ofEpochSecond(seconds, nanos);
    }

    /**
     * Returns the number of milliseconds since 1970-01-01 00:00:00 UTC.
     *
     * @return The number of milliseconds since 1970-01-01 00:00:00 UTC.
     */
    @CheckReturnValue
    public long toMillis()
    {
        final int hint = (int) (_raw % 10);
        long millis = (_raw - _UNIX_EPOCH_RAW) / ElapsedTime.MILLI.toRaw();

        if (hint != 0) {
            millis = ((millis / 10) * 10) + hint;
        }

        return millis;
    }

    /**
     * Provides an ordinal string representation of this.
     *
     * <p>Generates a string in ISO 8601 format adjusted to astronomical
     * conventions with the date in ordinal (yyyy-ddd) representation.</p>
     *
     * @return The string representation of this.
     */
    @Nonnull
    @CheckReturnValue
    public String toOrdinalString()
    {
        return defaultContext().toOrdinalString(this);
    }

    /**
     * Returns the internal (raw) representation of time.
     *
     * @return A long holding the number of 100 nanoseconds since 1858-11-17
     *         00:00 UTC.
     */
    @CheckReturnValue
    public long toRaw()
    {
        return _raw;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return defaultContext().toString(this);
    }

    /**
     * Returns the time in a Timestamp.
     *
     * @return The requested Timestamp.
     */
    @Nonnull
    @CheckReturnValue
    public Timestamp toTimestamp()
    {
        final long milliRaw = ElapsedTime.MILLI.toRaw();
        final Timestamp timestamp = new Timestamp(
            (_raw - _UNIX_EPOCH_RAW) / milliRaw);
        final int nanos = (int) ((_raw % milliRaw) * 100);

        timestamp.setNanos(timestamp.getNanos() + nanos);

        return timestamp;
    }

    /**
     * Provides a URL compatible representation of this.
     *
     * @return The URL compatible representation of this.
     */
    @Nonnull
    @CheckReturnValue
    public String toURLString()
    {
        return defaultContext().toURLString(this);
    }

    /**
     * Returns the number of 100 nanoseconds since 1601-01-01 00:00:00 UTC.
     *
     * @return The number of 100 nanoseconds since 1601-01-01 00:00:00 UTC.
     */
    @CheckReturnValue
    public long toWin32()
    {
        return _raw - _WINDOWS_EPOCH_RAW;
    }

    /**
     * Returns a ZonedDateTime representation of this.
     *
     * @return The ZonedDateTime representation of this.
     */
    @Nonnull
    @CheckReturnValue
    public ZonedDateTime toZonedDateTime()
    {
        return defaultContext().toZonedDateTime(this);
    }

    /**
     * Returns a DateTime decoded from a time String.
     *
     * <p>It accepts as input the output of {@link #toString()},
     * {@link #toFullString()}, {@link #toHexString()} or
     * {@link Timestamp#toString()}. The instance is used as a reference when
     * the year is missing.</p>
     *
     * @param timeString The time String.
     *
     * @return The DateTime for that time.
     *
     * @throws IllegalArgumentException When the time String is invalid.
     */
    @Nonnull
    @CheckReturnValue
    public DateTime valueOf(
            @Nonnull final String timeString)
        throws IllegalArgumentException
    {
        return defaultContext().fromString(timeString, Optional.of(this));
    }

    private static DateTime _fromRaw(final long raw)
    {
        return new DateTime(
            Math.min(Math.max(raw, _BEGINNING_OF_TIME_RAW), _END_OF_TIME_RAW));
    }

    /** Beginning of time. */
    public static final DateTime BEGINNING_OF_TIME;

    /** Beginning of time string. */
    public static final String BEGINNING_OF_TIME_STRING = "BoT";

    /** End of time. */
    public static final DateTime END_OF_TIME;

    /** End of time string. */
    public static final String END_OF_TIME_STRING = "EoT";

    /** Generated file name length. */
    public static final int FILE_NAME_LENGTH = 23;

    /** Invalid time representation. */
    public static final DateTime INVALID;

    /** MJD epoch (1858-11-17 00:00 UTC). */
    public static final DateTime MJD_EPOCH;

    /** Unix epoch (1970-01-01 00:00 UTC). */
    public static final DateTime UNIX_EPOCH;

    /** UTC context. */
    public static final Context UTC_CONTEXT = new Context(ZoneOffset.UTC);

    /** Windows epoch (1601-01-01 00:00 UTC). */
    public static final DateTime WINDOWS_EPOCH;

    /**  */

    private static final long _BEGINNING_OF_TIME_RAW = (Long.MAX_VALUE / 2)
        - Long.MAX_VALUE;
    private static final Context _DEFAULT_CONTEXT;
    private static final long _END_OF_TIME_RAW = Long.MAX_VALUE / 2;
    private static final long _INVALID_RAW = Long.MIN_VALUE;
    private static final long _MJD_EPOCH_RAW = 0L;
    private static final long _UNIX_EPOCH_RAW = 0x007C95674BEB4000L;
    private static final long _WINDOWS_EPOCH_RAW = -0X1211c7789534000L;

    /**  */

    private static volatile Context _defaultContext;
    private static volatile DateTime _simulatedTime;

    /**  */

    private static final long serialVersionUID = 1L;

    static {
        BEGINNING_OF_TIME = new DateTime(_BEGINNING_OF_TIME_RAW);
        END_OF_TIME = new DateTime(_END_OF_TIME_RAW);
        INVALID = new DateTime(_INVALID_RAW);
        MJD_EPOCH = new DateTime(_MJD_EPOCH_RAW);
        UNIX_EPOCH = new DateTime(_UNIX_EPOCH_RAW);
        WINDOWS_EPOCH = new DateTime(_WINDOWS_EPOCH_RAW);

        _DEFAULT_CONTEXT = new Context();

        _defaultContext = _DEFAULT_CONTEXT;
    }

    private final long _raw;

    /**
     * Context.
     */
    public static class Context
    {
        /**
         * Constructs an instance.
         */
        public Context()
        {
            this(ZoneId.systemDefault());
        }

        /**
         * Constructs an instance.
         *
         * @param timeZone The TimeZone.
         */
        public Context(@Nonnull final TimeZone timeZone)
        {
            this(timeZone.toZoneId());
        }

        /**
         * Constructs an instance.
         *
         * @param zoneId The ZoneId.
         */
        public Context(@Nonnull final ZoneId zoneId)
        {
            _zoneId = zoneId;
        }

        /**
         * Returns a DateTime built from fields.
         *
         * @param fields The fields.
         *
         * @return The DateTime.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime fromFields(@Nonnull final Fields fields)
        {
            final ZonedDateTime zonedDateTime = ZonedDateTime
                .of(
                    fields.year,
                    fields.month,
                    fields.day,
                    fields.hour,
                    fields.minute,
                    fields.second,
                    fields.nano,
                    getZoneId());

            return DateTime.fromZonedDateTime(zonedDateTime);
        }

        /**
         * Returns a DateTime decoded from a time string.
         *
         * <p>It accepts as input the output of {@link #toString()},
         * {@link #toFullString()}, {@link #toHexString()} or
         * {@link Timestamp#toString()}.</p>
         *
         * @param timeString The required and not empty time String.
         *
         * @return The DateTime.
         *
         * @throws IllegalArgumentException When the time String is invalid.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime fromString(@Nonnull final String timeString)
        {
            return fromString(timeString, Optional.empty());
        }

        /**
         * Returns a DateTime decoded from a time string.
         *
         * <p>It accepts as input the output of {@link #toString()},
         * {@link #toFullString()}, {@link #toHexString()} or
         * {@link Timestamp#toString()}. It will return null for a null
         * input.</p>
         *
         * @param timeString The required and not empty time String.
         * @param reference An optional reference date to use when the year is
         *                  missing.
         *
         * @return The DateTime.
         *
         * @throws IllegalArgumentException When the time String is invalid.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime fromString(
                @Nonnull String timeString,
                @Nonnull final Optional<DateTime> reference)
            throws IllegalArgumentException
        {
            timeString = Require.notEmptyTrimmed(timeString);

            if (timeString.equalsIgnoreCase(BEGINNING_OF_TIME_STRING)) {
                return BEGINNING_OF_TIME;
            }

            if (timeString.equalsIgnoreCase(END_OF_TIME_STRING)) {
                return END_OF_TIME;
            }

            final DateTime dateTime;
            final int minus = (timeString.charAt(0) == '-')? 1: 0;

            if ((timeString.length() > (minus + 2))
                    && (timeString.charAt(minus) == '0')
                    && ((timeString.charAt(
                        minus + 1) == 'X') || (timeString.charAt(
                                minus + 1) == 'x'))) {
                try {
                    dateTime = DateTime
                        .fromRaw(Long.decode(timeString).longValue());
                } catch (final NumberFormatException exception) {
                    throw new IllegalArgumentException(
                        Message.format(
                            BaseMessages.TIME_RAW_STRING,
                            timeString));
                }
            } else {
                Matcher matcher = _EXTENDED_PATTERN.matcher(timeString);

                if (!matcher.matches()) {
                    matcher = _BASIC_PATTERN.matcher(timeString);

                    if (!matcher.matches()) {
                        throw new IllegalArgumentException(
                            Message.format(
                                BaseMessages.TIME_FORMAT,
                                timeString));
                    }
                }

                final Fields fields;
                ZoneId zoneId = getZoneId();
                String string;
                int fieldValue;

                string = matcher.group(_Pattern.YEAR.group());

                if (string != null) {
                    final int year = Integer.parseInt(string);

                    fieldValue = Integer.parseInt(string);

                    string = matcher.group(_Pattern.MONTH.group());
                    fieldValue = Integer.parseInt(string);

                    string = matcher.group(_Pattern.DAY.group());

                    if (string != null) {
                        fields = new Fields();
                        fields.year = year;

                        if (fieldValue == 0) {
                            ++fieldValue;    // Month 0 is 1.
                        }

                        fields.month = fieldValue;

                        fieldValue = Integer.parseInt(string);

                        if (fieldValue == 0) {
                            ++fieldValue;    // Day zero is 1.
                        }

                        fields.day = fieldValue;
                    } else {
                        if (fieldValue == 0) {
                            ++fieldValue;    // Day zero is 1.
                        }

                        fields = toFields(
                            fromZonedDateTime(
                                ZonedDateTime
                                    .of(year, 1, 1, 0, 0, 0, 0, getZoneId())
                                    .withDayOfYear(fieldValue)));
                    }
                } else if (reference.isPresent()) {
                    fields = toFields(reference.get());
                } else {
                    throw new IllegalArgumentException(
                        Message.format(BaseMessages.TIME_YEAR, timeString));
                }

                string = matcher.group(_Pattern.HOUR.group());
                fields.hour = (string != null)? Integer.parseInt(string): 0;

                string = matcher.group(_Pattern.MINUTE.group());
                fields.minute = (string != null)? Integer.parseInt(string): 0;

                string = matcher.group(_Pattern.SECOND.group());
                fields.second = (string != null)? Integer.parseInt(string): 0;

                if (matcher.group(_Pattern.ZULU.group()) != null) {
                    zoneId = ZoneOffset.UTC;
                } else {
                    final String zoneSign = matcher
                        .group(_Pattern.ZONE_SIGN.group());

                    if (zoneSign != null) {
                        int zoneOffset = Integer
                            .parseInt(
                                matcher
                                    .group(_Pattern.ZONE_HOURS.group())) * 60;

                        string = matcher.group(_Pattern.ZONE_MINUTES.group());

                        if (string != null) {
                            zoneOffset += Integer.parseInt(string);
                        }

                        if ("-".equals(zoneSign)) {
                            zoneOffset = -zoneOffset;
                        }

                        zoneId = ZoneOffset.ofTotalSeconds(zoneOffset * 60);
                    }
                }

                string = matcher.group(_Pattern.NANOS.group());
                fields.nano = (string != null)? Context
                    .nanosFromString(string): 0;

                dateTime = fromZonedDateTime(
                    ZonedDateTime
                        .of(
                            fields.year,
                            fields.month,
                            fields.day,
                            fields.hour,
                            fields.minute,
                            fields.second,
                            fields.nano,
                            zoneId));
            }

            return dateTime;
        }

        /**
         * Gets the day of week for a DateTime.
         *
         * @param dateTime The DateTime.
         *
         * @return The day of week (0 - 6 where 0 is Sunday).
         */
        @CheckReturnValue
        public int getDayOfWeek(@Nonnull final DateTime dateTime)
        {
            return toZonedDateTime(dateTime).getDayOfWeek().getValue() % 7;
        }

        /**
         * Gets the number of days in the month of a DateTime.
         *
         * @param dateTime The DateTime.
         *
         * @return The number of days (28 - 31).
         */
        @CheckReturnValue
        public int getDaysInMonth(@Nonnull final DateTime dateTime)
        {
            return toZonedDateTime(dateTime).toLocalDate().lengthOfMonth();
        }

        /**
         * Gets the time zone.
         *
         * @return The time zone.
         */
        @Nonnull
        @CheckReturnValue
        public TimeZone getTimeZone()
        {
            return TimeZone.getTimeZone(getZoneId());
        }

        /**
         * Gets the ZoneId.
         *
         * @return The ZoneId.
         */
        @Nonnull
        @CheckReturnValue
        public ZoneId getZoneId()
        {
            return _zoneId;
        }

        /**
         * Returns a DateTime from a DateTime at midnight.
         *
         * @param dateTime The original DateTime.
         *
         * @return The new DateTime.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime midnight(@Nonnull final DateTime dateTime)
        {
            final Fields fields = toFields(dateTime);

            fields.hour = 0;
            fields.minute = 0;
            fields.second = 0;
            fields.nano = 0;

            return fromFields(fields);
        }

        /**
         * Returns a DateTime from a DateTime at midnight on the next day.
         *
         * @param dateTime The original DateTime.
         *
         * @return The new DateTime.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime nextDay(@Nonnull final DateTime dateTime)
        {
            final ZonedDateTime zonedDateTime = midnight(dateTime)
                .toZonedDateTime();

            return fromZonedDateTime(zonedDateTime.plusDays(1));
        }

        /**
         * Returns a DateTime from a DateTime at noon.
         *
         * @param dateTime The original DateTime.
         *
         * @return The new DateTime.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime noon(@Nonnull final DateTime dateTime)
        {
            final ZonedDateTime zonedDateTime = midnight(dateTime)
                .toZonedDateTime();

            return fromZonedDateTime(zonedDateTime.withHour(12));
        }

        /**
         * Returns a DateTime from a DateTime at midnight on the previous day.
         *
         * @param dateTime The original DateTime.
         *
         * @return The new DateTime.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime previousDay(@Nonnull final DateTime dateTime)
        {
            final ZonedDateTime zonedDateTime = midnight(dateTime)
                .toZonedDateTime();

            return fromZonedDateTime(zonedDateTime.minusDays(1));
        }

        /**
         * Provides a base string representation of a DateTime.
         *
         * <p>Generates a base string in ISO 8601 format adjusted to
         * astronomical conventions.</p>
         *
         * @param dateTime The DateTime.
         *
         * @return The base string representation of the DateTime.
         */
        @Nonnull
        @CheckReturnValue
        public String toBaseString(@Nonnull final DateTime dateTime)
        {
            return _toString(dateTime, false, false, false);
        }

        /**
         * Returns the fields from a DateTime.
         *
         * @param dateTime The DateTime.
         *
         * @return The fields.
         */
        @Nonnull
        @CheckReturnValue
        public Fields toFields(@Nonnull final DateTime dateTime)
        {
            final Fields fields = new Fields();
            final ZonedDateTime zonedDateTime = toZonedDateTime(dateTime);

            fields.year = zonedDateTime.getYear();
            fields.month = zonedDateTime.getMonthValue();
            fields.day = zonedDateTime.getDayOfMonth();
            fields.hour = zonedDateTime.getHour();
            fields.minute = zonedDateTime.getMinute();
            fields.second = zonedDateTime.getSecond();
            fields.nano = zonedDateTime.getNano();

            return fields;
        }

        /**
         * Provides a full extended string representation of a DateTime.
         *
         * <p>Generates an extended string in ISO 8601 format adjusted to
         * astronomical conventions.</p>
         *
         * @param dateTime The DateTime.
         *
         * @return The full extended string representation of the DateTime.
         */
        @Nonnull
        @CheckReturnValue
        public String toFullString(@Nonnull final DateTime dateTime)
        {
            return _toString(dateTime, true, true, false);
        }

        /**
         * Provides an ordinal string representation of a DateTime.
         *
         * <p>Generates a string in ISO 8601 format adjusted to astronomical
         * conventions with the date in ordinal (yyyy-ddd) representation.</p>
         *
         * @param dateTime The DateTime.
         *
         * @return The string representation of the DateTime.
         */
        @Nonnull
        @CheckReturnValue
        public String toOrdinalString(@Nonnull final DateTime dateTime)
        {
            return _toString(dateTime, false, true, true);
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return "(" + getZoneId() + ")";
        }

        /**
         * Provides a string representation of a DateTime.
         *
         * <p>Generates a string in ISO 8601 format adjusted to astronomical
         * conventions.</p>
         *
         * @param dateTime The DateTime.
         *
         * @return The string representation of the DateTime.
         */
        @Nonnull
        @CheckReturnValue
        public String toString(@Nonnull final DateTime dateTime)
        {
            return _toString(dateTime, false, true, false);
        }

        /**
         * Provides a URL compatible representation of a DateTime.
         *
         * @param dateTime The DateTime.
         *
         * @return The URL compatible representation of the DateTime.
         */
        @Nonnull
        @CheckReturnValue
        public String toURLString(@Nonnull final DateTime dateTime)
        {
            try {
                return URLEncoder
                    .encode(toString(dateTime), StandardCharsets.UTF_8.name());
            } catch (final UnsupportedEncodingException exception) {
                throw new RuntimeException(exception);
            }
        }

        /**
         * Returns a ZonedDateTime for a DateTime.
         *
         * @param dateTime The DateTime.
         *
         * @return The ZonedDateTime.
         */
        @Nonnull
        @CheckReturnValue
        public ZonedDateTime toZonedDateTime(@Nonnull final DateTime dateTime)
        {
            return ZonedDateTime.ofInstant(dateTime.toInstant(), getZoneId());
        }

        /**
         * Formats a number to a zero filled width.
         *
         * @param number The number.
         * @param width The width.
         *
         * @return The formatted number.
         */
        @Nonnull
        @CheckReturnValue
        static char[] formatNumber(int number, final int width)
        {
            final char[] chars = new char[width];

            for (int i = width - 1; i >= 0; --i) {
                if (number > 0) {
                    if (number >= _DEC_BASE) {
                        chars[i] = (char) ('0' + (number % _DEC_BASE));
                        number /= _DEC_BASE;
                    } else {
                        chars[i] = (char) ('0' + number);
                        number = 0;
                    }
                } else {
                    chars[i] = '0';
                }
            }

            return chars;
        }

        /**
         * Returns the number of nanoseconds represented by the supplied
         * String.
         *
         * @param nanosString The String representation.
         *
         * @return The number of nanoseconds.
         */
        @CheckReturnValue
        static int nanosFromString(@Nonnull final String nanosString)
        {
            return Integer
                .parseInt(
                    nanosString + _NANOS_ZEROES.substring(
                        0,
                        _MAX_NANOS_WIDTH - Math.min(
                                nanosString.length(),
                                        _MAX_NANOS_WIDTH))) * 100;
        }

        /**
         * Returns a String representation of a number of nanoseconds.
         *
         * @param nanos The number of nanoseconds.
         *
         * @return The String representation.
         */
        @Nonnull
        @CheckReturnValue
        static String nanosToString(final int nanos)
        {
            final char[] chars = DateTime.Context
                .formatNumber(nanos / 100, _MAX_NANOS_WIDTH);
            int index;

            for (index = _MAX_NANOS_WIDTH; index > 1; --index) {
                if (chars[index - 1] != '0') {
                    break;
                }
            }

            return new String(chars, 0, index);
        }

        /**
         * Converts a DateTime to string.
         *
         * @param dateTime The DateTime.
         * @param full True for full representation.
         * @param extended True for extended representation.
         * @param ordinal True for ordinal representation.
         *
         * @return The resulting string.
         */
        @Nonnull
        @CheckReturnValue
        String _toString(
                @Nonnull final DateTime dateTime,
                final boolean full,
                final boolean extended,
                final boolean ordinal)
        {
            if (dateTime.isBeginningOfTime()) {
                return BEGINNING_OF_TIME_STRING;
            }

            if (dateTime.isEndOfTime()) {
                return END_OF_TIME_STRING;
            }

            final StringBuilder stringBuilder = new StringBuilder();
            final ZonedDateTime zonedDateTime = toZonedDateTime(dateTime);
            final int year = zonedDateTime.getYear();
            final int second = zonedDateTime.getSecond();
            final int nanos = zonedDateTime.getNano();
            int offset = zonedDateTime.getOffset().getTotalSeconds() * 1_000;
            final char[] chars;
            int index;

            chars = Context
                .formatNumber((year < 0)? -year: year, _MAX_YEAR_WIDTH);
            index = (chars[1] == '0')? 2: 1;

            if (year < 0) {
                chars[--index] = '-';
            }

            stringBuilder.append(chars, index, _MAX_YEAR_WIDTH - index);

            if (extended) {
                stringBuilder.append('-');
            }

            if (ordinal) {
                stringBuilder
                    .append(
                        Context.formatNumber(zonedDateTime.getDayOfYear(), 3));
            } else {
                stringBuilder
                    .append(
                        Context.formatNumber(zonedDateTime.getMonthValue(), 2));

                if (extended) {
                    stringBuilder.append('-');
                }

                stringBuilder
                    .append(
                        Context.formatNumber(zonedDateTime.getDayOfMonth(), 2));
            }

            stringBuilder.append('T');
            stringBuilder
                .append(Context.formatNumber(zonedDateTime.getHour(), 2));

            if (extended) {
                stringBuilder.append(':');
            }

            stringBuilder
                .append(Context.formatNumber(zonedDateTime.getMinute(), 2));

            if (full || (second > 0) || (nanos > 0)) {
                if (extended) {
                    stringBuilder.append(':');
                }

                stringBuilder.append(Context.formatNumber(second, 2));

                if (full || (nanos > 0)) {
                    if (extended) {
                        stringBuilder.append('.');
                    }

                    if (full) {
                        stringBuilder
                            .append(
                                Context
                                    .formatNumber(
                                            nanos / 100,
                                                    _MAX_NANOS_WIDTH));
                    } else {
                        stringBuilder.append(Context.nanosToString(nanos));
                    }
                }
            }

            if ((full && extended) || (offset != 0)) {
                if (offset > 0) {
                    stringBuilder.append('+');
                } else {
                    stringBuilder.append('-');
                    offset = -offset;
                }

                offset /= 60000;
                stringBuilder.append(Context.formatNumber(offset / 60, 2));
                offset %= 60;

                if (full || !extended || (offset > 0)) {
                    if (extended) {
                        stringBuilder.append(':');
                    }

                    stringBuilder.append(Context.formatNumber(offset, 2));
                }
            } else {
                stringBuilder.append('Z');
            }

            return stringBuilder.toString();
        }

        private static final int _DEC_BASE = 10;
        private static final Pattern _BASIC_PATTERN = Pattern
            .compile(
                "([0-9]{4})([0-9]{2})([0-9]{2})"
                + "(?:(?:T|_|[ ]*+)([0-9]{2})([0-9]{2})"
                + "(?:([0-9]{2})(?:[.,]?([0-9]{1,7}))?)?"
                + "(?:(Z)|(?:([-+])([0-9]{2})(?:([0-9]{2}))?))?)?",
                Pattern.CASE_INSENSITIVE);
        private static final Pattern _EXTENDED_PATTERN = Pattern
            .compile(
                "(?:(?:(-?[0-9]++)-([0-9]++)(?:-([0-9]++))?)?(?:T|_|[ ]*+)"
                + "(?:([0-9]{1,2}+)(?::([0-9]{1,2}+)"
                + "(?::([0-9]{1,2}+)(?:[.,]([0-9]++))?+)?+)?+)?+"
                + "(?:(Z)|(?:([-+])([0-9]{2})(?::?([0-9]{2}))?))?)",
                Pattern.CASE_INSENSITIVE);
        private static final int _MAX_NANOS_WIDTH = 7;
        private static final int _MAX_YEAR_WIDTH = 6;
        private static final String _NANOS_ZEROES = "0000000";

        private final ZoneId _zoneId;

        /**
         * Pattern groups.
         */
        private enum _Pattern
        {
            YEAR,
            MONTH,
            DAY,
            HOUR,
            MINUTE,
            SECOND,
            NANOS,
            ZULU,
            ZONE_SIGN,
            ZONE_HOURS,
            ZONE_MINUTES;

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


    /**
     * Fields.
     */
    public static final class Fields
    {
        public int day;
        public int hour;
        public int minute;
        public int month;
        public int nano;
        public int second;
        public int year;
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
