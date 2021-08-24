/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CrontabSyncTests.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.tests.base.sync;

import org.rvpf.base.DateTime;
import org.rvpf.base.sync.CrontabSync;
import org.rvpf.base.sync.Sync;

import org.testng.annotations.Test;

/**
 * Crontab sync tests.
 */
public class CrontabSyncTests
    extends SyncTests
{
    /**
     * Should support Montreal fall time shift.
     */
    @Test
    public void shouldSupportMTLFallTimeShift()
    {
        final Sync sync;
        DateTime stamp;

        // Given an crontab sync in Montreal
        DateTime.simulateTimeZone(MONTREAL_TIME_ZONE);
        sync = new CrontabSync("0", DateTime.getZoneId());

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
     * Should support Montreal spring time shift.
     */
    @Test
    public void shouldSupportMTLSpringTimeShift()
    {
        final Sync sync;
        DateTime stamp;

        // Given an elapsed sync in Montreal
        DateTime.simulateTimeZone(MONTREAL_TIME_ZONE);
        sync = new CrontabSync("0", DateTime.getZoneId());

        // and a starting time before the time shift,
        stamp = DateTime.fromString("2006-04-01 23:00");

        // when going forward thru the time shift
        stamp = nextStamp(stamp, sync, "2006-04-02T00:00-05", false);
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
     * Should support Newfoundland fall time shift.
     */
    @Test
    public void shouldSupportNFLFallTimeShift()
    {
        final Sync sync;
        DateTime stamp;

        // Given an crontab sync in Newfoundland
        DateTime.simulateTimeZone(NEWFOUNDLAND_TIME_ZONE);
        sync = new CrontabSync("0", DateTime.getZoneId());

        // and a starting time before the time shift,
        stamp = DateTime.fromString("2005-10-29 22:00:00.0");

        // when going forward thru the time shift
        stamp = nextStamp(stamp, sync, "2005-10-29T23:00-02:30", true);
        stamp = nextStamp(stamp, sync, "2005-10-30T00:00-03:30", false);
        stamp = nextStamp(stamp, sync, "2005-10-30T01:00-03:30", false);
        stamp = nextStamp(stamp, sync, "2005-10-30T02:00-03:30", false);

        // and then going backward thru the same time shift,
        stamp = prevStamp(stamp, sync, "2005-10-30T01:00-03:30", false);
        stamp = prevStamp(stamp, sync, "2005-10-30T00:00-03:30", false);
        stamp = prevStamp(stamp, sync, "2005-10-29T23:00-02:30", true);

        // then the stamp values should be as expected.
    }

    /**
     * Should support Newfoundland spring time shift.
     */
    @Test
    public void shouldSupportNFLSpringTimeShift()
    {
        final Sync sync;
        DateTime stamp;

        // Given an elapsed sync in Newfoundland
        DateTime.simulateTimeZone(NEWFOUNDLAND_TIME_ZONE);
        sync = new CrontabSync("0", DateTime.getZoneId());

        // and a starting time before the time shift,
        stamp = DateTime.fromString("2006-04-01 22:00:00.0");

        // when going forward thru the time shift
        stamp = nextStamp(stamp, sync, "2006-04-01T23:00-03:30", false);
        stamp = nextStamp(stamp, sync, "2006-04-02T00:00-03:30", false);
        stamp = nextStamp(stamp, sync, "2006-04-02T02:00-02:30", true);
        stamp = nextStamp(stamp, sync, "2006-04-02T03:00-02:30", true);

        // and then going backward thru the same time shift,
        stamp = prevStamp(stamp, sync, "2006-04-02T02:00-02:30", true);
        stamp = prevStamp(stamp, sync, "2006-04-02T00:00-03:30", false);
        stamp = prevStamp(stamp, sync, "2006-04-01T23:00-03:30", false);

        // then the stamp values should be as expected.
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
