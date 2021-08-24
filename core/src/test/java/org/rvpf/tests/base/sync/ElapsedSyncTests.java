/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ElapsedSyncTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.sync;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.sync.ElapsedSync;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;

import org.testng.annotations.Test;

/**
 * Elapsed sync tests.
 */
public class ElapsedSyncTests
    extends SyncTests
{
    /**
     * Should support fall time shift.
     */
    @Test(priority = 30)
    public void shouldSupportFallTimeShift()
    {
        final Sync sync;
        DateTime stamp;

        // Given an elapsed sync in Montreal
        DateTime.simulateTimeZone(MONTREAL_TIME_ZONE);
        sync = new ElapsedSync(DateTime.getZoneId(), "01:00", Optional.empty());

        // and a starting time before the time shift,
        stamp = DateTime.fromString("2005-10-29 23:00");

        // when going forward thru the time shift
        stamp = nextStamp(stamp, sync, "2005-10-30T00:00-04", true);
        stamp = nextStamp(stamp, sync, "2005-10-30T01:00-04", true);
        stamp = nextStamp(stamp, sync, "2005-10-30T01:00-05", false);
        stamp = nextStamp(stamp, sync, "2005-10-30T02:00-05", false);
        stamp = nextStamp(stamp, sync, "2005-10-30T03:00-05", false);

        // and then going backward thru the same time shift,
        stamp = prevStamp(stamp, sync, "2005-10-30T02:00-05", false);
        stamp = prevStamp(stamp, sync, "2005-10-30T01:00-05", false);
        stamp = prevStamp(stamp, sync, "2005-10-30T01:00-04", true);
        stamp = prevStamp(stamp, sync, "2005-10-30T00:00-04", true);

        // then the stamp values should be as expected.
    }

    /**
     * Should support spring time shift.
     */
    @Test(priority = 30)
    public void shouldSupportSpringTimeShift()
    {
        final Sync sync;
        DateTime stamp;

        // Given an elapsed sync in Montreal
        DateTime.simulateTimeZone(MONTREAL_TIME_ZONE);
        sync = new ElapsedSync(DateTime.getZoneId(), "01:00", Optional.empty());

        // and a starting time before the time shift,
        stamp = DateTime.fromString("2006-04-02 00:00");

        // when going forward thru the time shift
        stamp = nextStamp(stamp, sync, "2006-04-02T01:00-05", false);
        stamp = nextStamp(stamp, sync, "2006-04-02T03:00-04", true);
        stamp = nextStamp(stamp, sync, "2006-04-02T04:00-04", true);

        // and then going backward thru the same time shift,
        stamp = prevStamp(stamp, sync, "2006-04-02T03:00-04", true);
        stamp = prevStamp(stamp, sync, "2006-04-02T01:00-05", false);
        stamp = prevStamp(stamp, sync, "2006-04-02T00:00-05", false);

        // then the stamp values should be as expected.
    }

    /**
     * Should sync backward.
     */
    @Test(priority = 20)
    public void shouldSyncBackward()
    {
        final Sync sync;

        // Given an elapsed sync,
        DateTime.simulateTimeZone(GMT_TIME_ZONE);
        sync = new ElapsedSync(
            DateTime.getZoneId(),
            "00:00:15",
            Optional.empty());

        // when starting from a point in time
        Require.success(sync.isInSync(DateTime.fromString("2007-01-01")));

        // and going backward,
        Require
            .equal(
                sync.getPreviousStamp().get(),
                DateTime.fromString("2006-12-31T23:59:45"),
                "previous stamp");
        Require
            .equal(
                sync.getPreviousStamp().get(),
                DateTime.fromString("2006-12-31T23:59:30"),
                "previous stamp");

        // then it should have returned expected values.
    }

    /**
     * Should sync forward.
     */
    @Test(priority = 10)
    public void shouldSyncForward()
    {
        final Sync sync;

        // Given an elapsed sync based on some elapsed time,
        DateTime.simulateTimeZone(GMT_TIME_ZONE);
        sync = new ElapsedSync(
            DateTime.getZoneId(),
            "00:00:15",
            Optional.empty());

        // when starting from a point in time
        Require.success(sync.isInSync(DateTime.fromString("2007-01-01")));

        // and going forward,
        Require
            .equal(
                sync.getNextStamp().get(),
                DateTime.fromString("2007-01-01T00:00:15"),
                "next stamp");
        Require
            .equal(
                sync.getNextStamp().get(),
                DateTime.fromString("2007-01-01T00:00:30"),
                "next stamp");

        // then it should have returned expected values.
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
