/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CrontabTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.sync;

import org.rvpf.base.DateTime;
import org.rvpf.base.sync.Crontab;
import org.rvpf.base.tool.Require;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Crontab tests.
 */
public final class CrontabTests
{
    private CrontabTests() {}

    /**
     * Provides invalid entries.
     *
     * @return Invalid entry strings.
     */
    @DataProvider
    public static Object[][] provideInvalidEntries()
    {
        return new Object[][] {
            {"/", }, {"0/0", }, {"-", }, {"1-0", }, {"60", }, {"0-60", },
        };
    }

    /**
     * Provides shedule times.
     *
     * @return Tuples of schedule, time and expected result.
     */
    @DataProvider
    public static Object[][] provideScheduleTimes()
    {
        return new Object[][] {
            {"", "2000-12-31T23:59", Boolean.TRUE},
            {"", "2000-12-31T23:59:59", Boolean.FALSE},
            {"0", "2000-12-31T23", Boolean.TRUE},
            {"0", "2000-12-31T23:59", Boolean.FALSE},
            {"0 0", "2000-12-31", Boolean.TRUE},
            {"0 0", "2000-12-31T23", Boolean.FALSE},
            {"0 0 1", "2000-12-01", Boolean.TRUE},
            {"0 0 1", "2000-12-31", Boolean.FALSE},
            {"0 0 1 1", "2000-01-01", Boolean.TRUE},
            {"0 0 1 1", "2000-12-01", Boolean.FALSE},
            {"0 0 * * 0", "2000-12-03", Boolean.TRUE},
            {"0 0 * * 0", "2000-12-01", Boolean.FALSE},
        };
    }

    /**
     * Provides valid entries.
     *
     * @return Tuples of valid entry and corresponding masks.
     */
    @DataProvider
    public static Object[][] provideValidEntries()
    {
        return new Object[][] {
            // At the beginning of every minute.
            {"", _ANY, _ANY, _ANY, _ANY, _NONE, },

            // At the beginning of every hour.
            {"0", lng(0x1L), _ANY, _ANY, _ANY, _NONE, },

            // Every quarter of every hour.
            {"0/15", lng(0x200040008001L), _ANY, _ANY, _ANY, _NONE, },

            // At the beginning of every day.
            {"0 0", lng(0x1L), lng(0x1L), _ANY, _ANY, _NONE, },

            // Each working day of the week at 12:00.
            {"0 12 * * 1-5", lng(
                0x1L), lng(0x1000L), _NONE, _ANY, lng(0x3eL), },

            // At the beginning of every month.
            {"0 0 1", lng(0x1L), lng(0x1L), lng(0x1L), _ANY, _NONE, },

            // At the beginning of every year.
            {"0 0 1 1", lng(0x1L), lng(0x1L), lng(0x1L), lng(0x1L), _NONE, },

            // At the beginning of every week.
            {"0 0 * * 0", lng(0x1L), lng(0x1L), _NONE, _ANY, lng(0x1L), },
        };
    }

    /**
     * Should match its schedule.
     *
     * @param schedule The schedule.
     * @param timeString A time string.
     * @param expected True if it is in schedule.
     *
     * @throws Exception On failure.
     */
    @Test(
        dataProvider = "provideScheduleTimes",
        priority = 30
    )
    public static void shouldMatchSchedule(
            final String schedule,
            final String timeString,
            final boolean expected)
        throws Exception
    {
        final Crontab crontab;
        final DateTime stamp;
        final boolean inSchedule;

        // Given a crontab based on a specified schedule
        crontab = Crontab.parse(schedule);

        // and a specified time,
        stamp = DateTime.fromString(timeString);

        // when asked if the clock is in schedule,
        inSchedule = crontab.isInSchedule(stamp, DateTime.getZoneId());

        // then it should return the expected answer.
        Require
            .equal(
                Boolean.valueOf(inSchedule),
                Boolean.valueOf(expected),
                timeString + " in schedule '" + schedule + "'");
    }

    /**
     * Should parse a valid entry.
     *
     * @param entry The entry string.
     * @param minutes The minutes mask (60 lower bits).
     * @param hours The hours (24 lower bits).
     * @param days The days (31 lower bits).
     * @param months The months (12 lower bits).
     * @param daysOfWeek The days of week (7 lower bits).
     *
     * @throws Exception On failure.
     */
    @Test(
        dataProvider = "provideValidEntries",
        priority = 10
    )
    public static void shouldParseValidEntry(
            final String entry,
            final long minutes,
            final long hours,
            final long days,
            final long months,
            final long daysOfWeek)
        throws Exception
    {
        final Crontab crontab;

        // Given a valid entry string,
        // when parsing the entry string,
        crontab = Crontab.parse(entry);

        // then the resulting crontab values should match the expected values.
        _match(crontab.getMinutes(), minutes);
        _match(crontab.getHours(), hours);
        _match(crontab.getDays(), days);
        _match(crontab.getMonths(), months);
        _match(crontab.getDaysOfWeek(), daysOfWeek);
    }

    /**
     * Should recognize the end of a month.
     *
     * @throws Exception On failure.
     */
    @Test(priority = 35)
    public static void shouldRecognizeEndOfMonth()
        throws Exception
    {
        final Crontab crontab;

        // Given a shedule with a specified day of month
        crontab = Crontab.parse("0 0 31");

        // and any date whose day is the last of the month,
        // when the day in schedule is equal or higher,
        // then the date day should be considered in schedule.
        Require
            .failure(
                crontab
                    .isInSchedule(
                            DateTime.fromString("2008-01-30"),
                                    DateTime.getZoneId()));
        Require
            .success(
                crontab
                    .isInSchedule(
                            DateTime.fromString("2008-01-31"),
                                    DateTime.getZoneId()));
        Require
            .failure(
                crontab
                    .isInSchedule(
                            DateTime.fromString("2008-02-28"),
                                    DateTime.getZoneId()));
        Require
            .success(
                crontab
                    .isInSchedule(
                            DateTime.fromString("2008-02-29"),
                                    DateTime.getZoneId()));
    }

    /**
     * Should reject an invalid entry.
     *
     * @param entry The entry string.
     */
    @Test(
        dataProvider = "provideInvalidEntries",
        priority = 20
    )
    public static void shouldRejectInvalidEntry(final String entry)
    {
        Exception catchedException = null;

        // Given an invalid entry string,
        // when parsing the entry string,
        try {
            Crontab.parse(entry);
        } catch (final Crontab.BadItemException exception) {
            catchedException = exception;
        }

        // then it should throw a BadItemException.
        Require
            .notNull(
                catchedException,
                "bad item exception for '" + entry + "'");
    }

    private static void _match(final boolean[] actual, long expected)
    {
        for (int i = 0; i < actual.length; ++i) {
            Require
                .equal(
                    Boolean.valueOf(actual[i]),
                    Boolean.valueOf((expected & 0x1) != 0));
            expected >>= 1;
        }
    }

    private static Long lng(final long value)
    {
        return Long.valueOf(value);
    }

    private static final Long _ANY = lng(-1);
    private static final Long _NONE = lng(0);
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
