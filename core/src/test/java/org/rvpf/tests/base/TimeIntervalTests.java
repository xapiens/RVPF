/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TimeIntervalTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base;

import org.rvpf.base.DateTime;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.tool.Require;
import org.rvpf.tests.Tests;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Time interval tests.
 */
public final class TimeIntervalTests
    extends Tests
{
    /**
     * The default constructor should create an unlimited interval.
     */
    @Test(priority = 10)
    public static void defaultConstructorShouldCreateUnlimited()
    {
        final TimeInterval sut;

        // When an interval is created from the default constructor,

        sut = TimeInterval.UNLIMITED;

        // then it should represent an unlimited interval.
        Require.success(sut.isFromBeginningOfTime(), "from BOT");
        Require.success(sut.isToEndOfTime(), "to EOT");
    }

    /**
     * Provides instant strings.
     *
     * @return Pairs of interval string and DateTime creation strings.
     */
    @DataProvider
    public static Object[][] provideInstantStrings()
    {
        return new Object[][] {
            {"[2000-01-01/2000-01-01]", "2000-01-01"},
            {"[2000-01-01]", "2000-01-01"},
        };
    }

    /**
     * Provides interval strings and interval.
     *
     * @return Pairs of interval string and matching interval.
     */
    @DataProvider
    public static Object[][] provideIntervalStringsAndInterval()
    {
        return new Object[][] {
            {"[2000-01-01/2001-01-01]",
             TimeInterval.UNLIMITED.notBefore(
                 DateTime.fromString(
                     "2000-01-01")).notAfter(
                             DateTime.fromString(
                                     "2001-01-01")), }, {"[2000-01-01--_)",
                                     TimeInterval.UNLIMITED.notBefore(
                                             DateTime.fromString(
                                                     "2000-01-01")), },
            {"(_--2001-01-01]", TimeInterval.UNLIMITED.notAfter(
                DateTime.fromString(
                    "2001-01-01")), }, {"(2000-01-01/2001-01-01)",
                    TimeInterval.UNLIMITED.after(
                        DateTime.fromString(
                                "2000-01-01")).before(
                                        DateTime.fromString(
                                                "2001-01-01")), },
        };
    }

    /**
     * Provides invalid interval strings.
     *
     * @return Invalid interval strings.
     */
    @DataProvider
    public static Object[][] provideInvalidIntervalStrings()
    {
        return new Object[][] {
            {"(2001-01-01/2000-01-01)", }, {"(2000-01-01/2000-01-01)", },
            {"[2000-01-01/2000-01-01)", }, {"(2000-01-01/2000-01-01]", },
        };
    }

    /**
     * Provides unlimited interval strings.
     *
     * @return String values which should be interpreted as unlimited interval.
     */
    @DataProvider
    public static Object[][] provideUnlimitedIntervalStrings()
    {
        return new Object[][] {
            {" ", }, {"(/)", }, {"(_/_)", }, {"[/]", }, {"[_/_]", }, {"(--)", },
            {"(_--_)", }, {"[--]", }, {"[_--_]", }, {"(,)", }, {"(_,_)", },
            {"[,]", }, {"[_,_]", },
        };
    }

    /**
     * Should create an instant from a string.
     *
     * @param intervalString An interval creation string.
     * @param dateTimeString A DateTime creation string.
     */
    @Test(
        dataProvider = "provideInstantStrings",
        priority = 20
    )
    public static void shouldCreateAnInstantFromString(
            final String intervalString,
            final String dateTimeString)
    {
        final TimeInterval sut;
        final DateTime dateTime;

        // Given an interval string representing an instant
        // and a date-time creation for that instant,
        dateTime = DateTime.fromString(dateTimeString);

        // when a TimeInterval instance is created from the interval string,
        sut = TimeInterval.Builder.fromString(intervalString);

        // then the TimeInterval should see itself as an instant
        Require.success(sut.isInstant(), "is instant");

        // and it should see itself as equal to an other defined as a closed
        // interval on the provided date-time
        Require
            .equal(
                sut,
                TimeInterval.UNLIMITED.notBefore(dateTime).notAfter(dateTime));

        // and it should see itself as equal to an other defined as an instant
        // on the provided date-time.
        Require.equal(sut, TimeInterval.newBuilder().setAt(dateTime).build());
    }

    /**
     * Should create an interval from a string.
     *
     * @param intervalString An interval creation string.
     * @param interval A reference interval.
     */
    @Test(
        dataProvider = "provideIntervalStringsAndInterval",
        priority = 20
    )
    public static void shouldCreateIntervalFromString(
            final String intervalString,
            final TimeInterval interval)
    {
        final TimeInterval sut;

        // Given an interval string
        // and a reference interval,
        // when a time interval is created from the string,

        sut = TimeInterval.Builder.fromString(intervalString);

        // then it should be equal to the reference interval
        // and frozen.
        Require.equal(sut, interval, "interval");
    }

    /**
     * Should create unlimited interval from string.
     *
     * @param intervalString An unlimited interval creation string.
     */
    @Test(
        dataProvider = "provideUnlimitedIntervalStrings",
        priority = 20
    )
    public static void shouldCreateUnlimitedIntervalFromString(
            final String intervalString)
    {
        final TimeInterval sut;

        // Given an interval string representing an unlimited interval,
        // when a time interval is created from the string,

        sut = TimeInterval.Builder.fromString(intervalString);

        // then it should be equal to an unlimited interval.
        Require.equal(sut, TimeInterval.UNLIMITED, "unlimited interval");
    }

    /**
     * Should throw an invalid interval exception.
     *
     * @param intervalString An invalid interval string.
     *
     * @throws TimeInterval.InvalidIntervalException As expected.
     */
    @Test(
        dataProvider = "provideInvalidIntervalStrings",
        priority = 40
    )
    public static void shouldThrowInvalidIntervalException(
            final String intervalString)
    {
        Exception catchedException = null;

        // Given an invalid interval string,
        // when creating a time interval from that string,
        try {
            TimeInterval.Builder.fromString(intervalString);
        } catch (final TimeInterval.InvalidIntervalException exception) {
            catchedException = exception;
        }

        // then it should throw an InvalidIntervalException.
        Require.notNull(catchedException, "invalid interval exception");
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
