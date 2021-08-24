/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DateTimeTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.sql.Timestamp;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import java.util.Optional;
import java.util.TimeZone;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.tests.Tests;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * DateTime tests.
 */
public final class DateTimeTests
    extends Tests
{
    /**
     * Provides input strings.
     *
     * @return Pairs of input string and expected representation.
     */
    @DataProvider
    public static Object[][] provideInputStrings()
    {
        return new Object[][] {
            {"2005-10-29_23:00-02:30", "2005-10-29T21:30-04", },
            {"2005-10-29t23:00Z", "2005-10-29T19:00-04", },
            {"20000101T0601011000000Z", "2000-01-01T01:01:01.1-05", },
            {"201004201530", "2010-04-20T15:30-04", },
            {"20100420T153025.1234567-04", "2010-04-20T15:30:25.1234567-04", },
            {"2000-366 12:00", "2000-12-31T12:00-05"},
        };
    }

    /**
     * Should allow the earliest time.
     */
    @Test
    public static void shouldAllowEarliestTime()
    {
        final DateTime.Context context;
        final DateTime dateTime;

        // Given the GMT context
        context = new DateTime.Context(_GMT_TIME_ZONE);

        // when getting the beginning of time plus a microsecond,
        dateTime = DateTime.BEGINNING_OF_TIME.after(ElapsedTime.MICRO);

        // then it should produce an equivalent string
        Require
            .equal(
                dateTime,
                context.fromString("-12755-01-09T10:35:57.2612106Z"));

        // and full string
        Require
            .equal(
                context.toFullString(dateTime),
                "-12755-01-09T10:35:57.2612106-00:00");

        // and ordinal string
        Require
            .equal(
                context.toOrdinalString(dateTime),
                "-12755-009T10:35:57.2612106Z");

        // and correspond to the appropriate constants.
        Require.equal(dateTime.toHexString(), "-0X3ffffffffffffff6");
        Require.equal(dateTime, context.fromString("-0X3ffffffffffffff6"));
        Require.success(dateTime.toRaw() == -0X3ffffffffffffff6L);
        Require.equal(dateTime, DateTime.fromRaw(-0X3ffffffffffffff6L));
    }

    /**
     * Should allow the latest time.
     */
    @Test
    public static void shouldAllowLatestTime()
    {
        final DateTime.Context context;
        final DateTime dateTime;

        // Given the GMT context
        context = new DateTime.Context(_GMT_TIME_ZONE);

        // when getting the end of time minus a microsecond,
        dateTime = DateTime.END_OF_TIME.before(ElapsedTime.MICRO);

        // then it should produce an equivalent string
        Require
            .equal(
                dateTime,
                context.fromString("16472-09-22T13:24:02.7387893Z"));

        // and full string
        Require
            .equal(
                context.toFullString(dateTime),
                "16472-09-22T13:24:02.7387893-00:00");

        // and ordinal string
        Require
            .equal(
                context.toOrdinalString(dateTime),
                "16472-266T13:24:02.7387893Z");

        // and correspond to the appropriate constants.
        Require.equal(dateTime.toHexString(), "0X3ffffffffffffff5");
        Require.equal(dateTime, context.fromString("0X3ffffffffffffff5"));
        Require.success(dateTime.toRaw() == 0X3ffffffffffffff5L);
        Require.equal(dateTime, DateTime.fromRaw(0X3ffffffffffffff5L));
    }

    /**
     * Should handle the beginning of time.
     */
    @Test
    public static void shouldHandleBeginningOfTime()
    {
        final DateTime.Context context;

        // Given a context
        context = new DateTime.Context(_MONTREAL_TIME_ZONE);

        // and the special meaning of 'BoT'
        // and the constant limit for the earliest time,
        // then these should be recognized as equivalent.
        Require
            .equal(
                context.toString(DateTime.BEGINNING_OF_TIME),
                DateTime.BEGINNING_OF_TIME_STRING);
        Require
            .equal(
                context.fromString(DateTime.BEGINNING_OF_TIME_STRING),
                DateTime.BEGINNING_OF_TIME);
    }

    /**
     * Should handle the end of time.
     */
    @Test
    public static void shouldHandleEndOfTime()
    {
        final DateTime.Context context;

        // Given a context
        context = new DateTime.Context(_MONTREAL_TIME_ZONE);

        // and the special meaning of 'EoT'
        // and the constant limit for the latest time,
        // then these should be recognized as equivalent.
        Require
            .equal(
                context.toString(DateTime.END_OF_TIME),
                DateTime.END_OF_TIME_STRING);
        Require
            .equal(
                context.fromString(DateTime.END_OF_TIME_STRING),
                DateTime.END_OF_TIME);
    }

    /**
     * Should handle MJD epoch.
     */
    @Test
    public static void shouldHandleMJDEpoch()
    {
        final DateTime.Context context;
        final DateTime dateTime;

        // Given the GMT context,
        context = new DateTime.Context(_GMT_TIME_ZONE);

        // when creating a DateTime at raw value 0,
        dateTime = DateTime.fromRaw(0);

        // then it should return 0 as its raw value
        Require.success(dateTime.toRaw() == 0);

        // and the MJD epoch time as its string
        Require.equal(context.toString(dateTime), "1858-11-17T00:00Z");

        // and ordinal string
        Require.equal(context.toOrdinalString(dateTime), "1858-321T00:00Z");

        // and correspond to the appropriate constant.
        Require.equal(dateTime.toHexString(), "0X0");
        Require.equal(dateTime, DateTime.fromString("0X0"));
    }

    /**
     * Should handle Unix epoch.
     */
    @Test
    public static void shouldHandleUnixEpoch()
    {
        final DateTime.Context context;
        final Timestamp timestamp;
        final DateTime dateTime;

        // Given the GMT context,
        context = new DateTime.Context(_GMT_TIME_ZONE);

        // when creating a DateTime at Timestamp value 0,
        timestamp = new Timestamp(0);
        dateTime = DateTime.at(timestamp);

        // then it should return 0 as its millis value
        Require.success(dateTime.toMillis() == 0);

        // and the Unix epoch time as its string
        Require.equal(context.toString(dateTime), "1970-01-01T00:00Z");

        // and full string
        Require
            .equal(
                context.toFullString(dateTime),
                "1970-01-01T00:00:00.0000000-00:00");

        // and ordinal string
        Require.equal(context.toOrdinalString(dateTime), "1970-001T00:00Z");

        // and file name
        Require.equal(dateTime.toFileName(), "19700101T0000000000000Z");

        // and correspond to the appropriate constants.
        Require.success(dateTime.toRaw() == DateTime.UNIX_EPOCH.toRaw());
        Require.equal(dateTime.toTimestamp(), timestamp);
        Require.equal(DateTime.fromRaw(dateTime.toRaw()), dateTime);
        Require.equal(dateTime.toHexString(), "0X7c95674beb4000");
        Require.equal(dateTime, DateTime.fromString("0X7c95674beb4000"));
        Require.equal(DateTime.fromBytes(dateTime.toBytes()), dateTime);
    }

    /**
     * Should handle year 0000.
     */
    @Test
    public static void shouldHandleYear0000()
    {
        final DateTime.Context context;
        final DateTime dateTime;

        // Given the GMT context,
        context = new DateTime.Context(_GMT_TIME_ZONE);

        // when creating a DateTime for the year 0000,
        dateTime = context.fromString("0000-00-00", Optional.empty());

        // then it should produce an equivalent string
        Require.equal(context.toString(dateTime), "0000-01-01T00:00Z");

        // and full string
        Require
            .equal(
                context.toFullString(dateTime),
                "0000-01-01T00:00:00.0000000-00:00");

        // and ordinal string
        Require.equal(context.toOrdinalString(dateTime), "0000-001T00:00Z");

        // then is should correspond to the appropriate constants.
        Require.equal(dateTime.toHexString(), "-0X8240a294efac000");
        Require.equal(dateTime, context.fromString("-0X8240a294efac000"));
        Require.success(dateTime.toRaw() == -0X8240a294efac000L);
        Require.equal(dateTime, DateTime.fromRaw(-0X8240a294efac000L));
    }

    /**
     * Should recognize input strings.
     *
     * @param input An input string.
     * @param expected The expected result string.
     */
    @Test(dataProvider = "provideInputStrings")
    public static void shouldRecognizeInputString(
            final String input,
            final String expected)
    {
        final DateTime.Context context;
        final DateTime dateTime;
        final String output;

        // Given a context
        context = new DateTime.Context(_MONTREAL_TIME_ZONE);

        // and an input string representing a date-time,
        // when creating a DateTime with the input string
        dateTime = context.fromString(input);

        // and converting back to string,
        output = context.toString(dateTime);

        // then it should match the expected output.
        Require.equal(output, expected, "value of '" + input + "'");
    }

    /**
     * Should support floored.
     */
    @Test
    public static void shouldSupportFloored()
    {
        final DateTime.Context context;
        final DateTime at1300;
        final DateTime dateTime;
        final DateTime floored;

        // Given the GMT context
        context = new DateTime.Context(_GMT_TIME_ZONE);

        // and an arbitrary time at 13:00 minus a microsecond,
        at1300 = context.fromString("13:00", Optional.of(DateTime.now()));
        dateTime = at1300.before(ElapsedTime.MICRO);

        // when flooring,
        floored = dateTime.floored(ElapsedTime.HOUR);

        // then the floored value should be at 12:00.
        Require
            .equal(floored, context.fromString("12:00", Optional.of(dateTime)));
    }

    /**
     * Should support Instant.
     */
    @Test
    public static void shouldSupportInstant()
    {
        final DateTime.Context context;
        final Instant instant;
        final DateTime dateTime;

        // Given the UTC context,
        context = new DateTime.Context(ZoneOffset.UTC);

        // and an Instant,
        instant = ZonedDateTime
            .of(2000, 12, 31, 12, 0, 0, 0, context.getZoneId())
            .toInstant();

        // when creating a DateTime from the Instant,
        dateTime = DateTime.fromInstant(instant);

        // then it should match the input of the Instant
        Require.equal(context.toString(dateTime), "2000-12-31T12:00Z");

        // and should be able to convert back to an Instant.
        Require.equal(dateTime.toInstant(), instant);
    }

    /**
     * Should support rounded.
     */
    @Test
    public static void shouldSupportRounded()
    {
        final DateTime.Context context;
        final DateTime dateTime;
        final DateTime roundedDown;
        final DateTime roundedUp;

        // Given the GMT context
        context = new DateTime.Context(_GMT_TIME_ZONE);

        // and an arbitrary time at 12:30,
        dateTime = context.fromString("12:30", Optional.of(DateTime.now()));

        // when rounding using a microsecond float operations margin,
        roundedDown = dateTime
            .before(ElapsedTime.MICRO)
            .rounded(ElapsedTime.HOUR);
        roundedUp = dateTime.after(ElapsedTime.MICRO).rounded(ElapsedTime.HOUR);

        // then the rounded down value should be at 12:00
        Require
            .equal(
                roundedDown,
                context.fromString("12:00", Optional.of(dateTime)));

        // and the rounded up value should be at 13:00.
        Require
            .equal(
                roundedUp,
                context.fromString("13:00", Optional.of(dateTime)));
    }

    /**
     * Tests the serialization.
     *
     * @throws Exception On failure.
     */
    @Test
    public static void testSerialization()
        throws Exception
    {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream = new ObjectOutputStream(
            outputStream);
        final DateTime localDateTime = DateTime.now();

        objectOutputStream.writeObject(localDateTime);
        objectOutputStream.close();

        final InputStream inputStream = new ByteArrayInputStream(
            outputStream.toByteArray());
        final ObjectInputStream objectInputStream = new ObjectInputStream(
            inputStream);
        final DateTime remoteDateTime = (DateTime) objectInputStream
            .readObject();

        Require.equal(remoteDateTime, localDateTime);
        objectInputStream.close();
    }

    private static final TimeZone _GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");
    private static final TimeZone _MONTREAL_TIME_ZONE = TimeZone
        .getTimeZone("America/Montreal");
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
