/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SyncTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.sync;

import java.util.TimeZone;

import org.rvpf.base.DateTime;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;

import org.testng.annotations.AfterClass;

/**
 * Sync tests.
 */
abstract class SyncTests
{
    /**
     * Resets the time zone.
     */
    @AfterClass(alwaysRun = true)
    public void resetTimeZone()
    {
        DateTime.resetTimeZone();
    }

    /**
     * Returns the next stamp
     *
     * @param stamp The current stamp.
     * @param sync The sync instance.
     * @param expected The expected date-time string for the returned stamp.
     * @param dst True if should be in DST.
     *
     * @return The next stamp.
     */
    protected DateTime nextStamp(
            final DateTime stamp,
            final Sync sync,
            final String expected,
            final boolean dst)
    {
        return _expect(sync.getNextStamp(stamp).get(), sync, expected, dst);
    }

    /**
     * Returns the previous stamp
     *
     * @param stamp The current stamp.
     * @param sync The sync instance.
     * @param expected The expected date-time string for the returned stamp.
     * @param dst True if should be in DST.
     *
     * @return The previous stamp.
     */
    protected DateTime prevStamp(
            final DateTime stamp,
            final Sync sync,
            final String expected,
            final boolean dst)
    {
        return _expect(sync.getPreviousStamp(stamp).get(), sync, expected, dst);
    }

    private static DateTime _expect(
            final DateTime stamp,
            final Sync sync,
            final String expected,
            final boolean dst)
    {
        final boolean inDaylightTime = DateTime
            .getTimeZone()
            .inDaylightTime(stamp.toTimestamp());

        Require
            .equal(
                Boolean.valueOf(inDaylightTime),
                Boolean.valueOf(dst),
                "DST");
        Require.equal(stamp.toString(), expected);
        Require.success(sync.isInSync(stamp));

        return stamp;
    }

    protected static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");
    protected static final TimeZone MONTREAL_TIME_ZONE = TimeZone
        .getTimeZone("America/Montreal");
    protected static final TimeZone NEWFOUNDLAND_TIME_ZONE = TimeZone
        .getTimeZone("Canada/Newfoundland");
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
