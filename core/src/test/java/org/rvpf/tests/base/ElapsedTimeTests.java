/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ElapsedTimeTests.java 4099 2019-06-26 21:33:35Z SFB $
 */

package org.rvpf.tests.base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.time.Duration;

import java.util.Optional;
import java.util.TimeZone;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.tests.Tests;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Elapsed time tests.
 */
public final class ElapsedTimeTests
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
            {"1 11:11:11.11", "P1T11:11:11.11", "P1DT11H11M11.11S", },
            {"", null, null, }, {"999", "P0T00:00:00.999", "PT0.999S", },
            {"1000", "P0T00:00:01", "PT1S", },
            {"1000-", "P1000T00:00", "P1000D", },
            {"P1000", "P1000T00:00", "P1000D", },
            {"P1000-00:00", "P1000T00:00", "P1000D", }, {"-1", null, null, },
        };
    }

    /**
     *     Provides string representation of a 1 minute elapsed time.
     *
     *     @return The strings.
     */
    @DataProvider
    public static Object[][] provideMinuteRepresentations()
    {
        return new Object[][] {
            {"0T00:01", }, {"00:01:00", }, {"0T00:01:00", }, {"0t00:01:00.0", },
            {"60.0", }, {"P1M", }, {"PT60.0S", },
        };
    }

    /**
     * Should recognize input strings.
     *
     * @param input An input string.
     * @param expectedString The expected string.
     * @param expectedDurationString The expected duration string.
     */
    @Test(dataProvider = "provideInputStrings")
    public static void shouldRecognizeInputString(
            final String input,
            final String expectedString,
            final String expectedDurationString)
    {
        final Optional<ElapsedTime> elapsed;
        final String string;
        final String durationString;

        // Given an input string representing an elapsed time,
        // when creating an elapsed time with the input string
        elapsed = ElapsedTime.fromString(Optional.ofNullable(input));

        // and converting back to string
        string = elapsed.isPresent()? elapsed.get().toString(): null;

        // and duration string
        durationString = elapsed
            .isPresent()? elapsed.get().toDurationString(): null;

        // then it should match the expected output.
        Require.equal(string, expectedString, "value of '" + input + "'");
        Require
            .equal(
                durationString,
                expectedDurationString,
                "value of '" + input + "'");
    }

    /**
     * Should recognize an input string representing 1 minute.
     *
     * @param input An input string representing 1 minute.
     */
    @Test(dataProvider = "provideMinuteRepresentations")
    public static void shouldRecognizeMinuteString(final String input)
    {
        final ElapsedTime reference;
        final ElapsedTime elapsed;

        // Given an input string representing 1 minute
        // and a 1 minute reference,
        reference = ElapsedTime.MINUTE;

        // when creating an ElapsedTime from that string,
        elapsed = ElapsedTime.fromString(input);

        // then that ElapsedTime should equals the reference.
        Require.equal(elapsed, reference, "1 minute elapsed");
    }

    /**
     * Should support after.
     */
    @Test
    public static void shouldSupportAfter()
    {
        final DateTime.Context context;
        final DateTime stamp;
        final ElapsedTime elapsed;
        final DateTime result;

        // Given a context
        context = new DateTime.Context(_MONTREAL_TIME_ZONE);

        // and a stamp
        stamp = context.fromString("2000-01-01 00:00:00");

        // and an elapsed time,
        elapsed = ElapsedTime.MINUTE;

        // when adding the elapsed time to the stamp,
        result = stamp.after(elapsed);

        // then the result should be as expected.
        Require.equal(context.toString(result), "2000-01-01T00:01-05");
    }

    /**
     * Should support before.
     */
    @Test
    public static void shouldSupportBefore()
    {
        final DateTime.Context context;
        final DateTime stamp;
        final ElapsedTime elapsed;
        final DateTime result;

        // Given a context
        context = new DateTime.Context(_MONTREAL_TIME_ZONE);

        // and a stamp
        stamp = context.fromString("2000-01-01 00:01:00");

        // and an elapsed time,
        elapsed = ElapsedTime.MINUTE;

        // when subtracting the elapsed time to the stamp,
        result = stamp.before(elapsed);

        // then the result should be as expected.
        Require.equal(context.toString(result), "2000-01-01T00:00-05");
    }

    /**
     * Should support Duration.
     */
    @Test
    public static void shouldSupportDuration()
    {
        final Duration duration;
        final ElapsedTime elapsedTime;

        // Given a Duration,
        duration = Duration.ofMillis(11111);

        // when creating an ElapsedTime from a duration,
        elapsedTime = ElapsedTime.fromDuration(duration);

        // then it should match the input of the Duration
        Require.equal(elapsedTime.toString(), "P0T00:00:11.111");

        // and should be able to convert back to a Duration.
        Require.equal(elapsedTime.toDuration(), duration);
    }

    /**
     * Should support infinity.
     */
    @Test
    public static void shouldSupportInfinity()
    {
        // Given the reserved value for infinity
        // and its string representation,
        // then converting infinity to string should give its representation
        Require
            .equal(
                ElapsedTime.INFINITY.toString(),
                ElapsedTime.INFINITY_STRING);

        // and converting its representation should give its value
        Require
            .equal(
                ElapsedTime.fromString(ElapsedTime.INFINITY_STRING),
                ElapsedTime.INFINITY);

        // and adding infinity to a minute should give infinity
        Require
            .equal(
                ElapsedTime.MINUTE.add(ElapsedTime.INFINITY),
                ElapsedTime.INFINITY);

        // and adding infinity to infinity should give infinity
        Require
            .equal(
                ElapsedTime.INFINITY.add(ElapsedTime.INFINITY),
                ElapsedTime.INFINITY);

        // and adding infinity to a time should give EoT
        Require
            .equal(
                DateTime.now().after(ElapsedTime.INFINITY),
                DateTime.END_OF_TIME);

        // and subtracting infinity from a time shoud give BoT.
        Require
            .equal(
                DateTime.now().before(ElapsedTime.INFINITY),
                DateTime.BEGINNING_OF_TIME);
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
        final ElapsedTime localElapsedTime = DateTime
            .now()
            .sub(DateTime.UNIX_EPOCH);

        objectOutputStream.writeObject(localElapsedTime);
        objectOutputStream.close();

        final InputStream inputStream = new ByteArrayInputStream(
            outputStream.toByteArray());
        final ObjectInputStream objectInputStream = new ObjectInputStream(
            inputStream);
        final ElapsedTime remoteElapsedTime = (ElapsedTime) objectInputStream
            .readObject();

        Require.equal(remoteElapsedTime, localElapsedTime);
        objectInputStream.close();
    }

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
