/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StampSyncTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.sync;

import org.rvpf.base.DateTime;
import org.rvpf.base.sync.StampsSync;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;

import org.testng.annotations.Test;

/**
 * Stamp sync tests.
 */
public class StampSyncTests
    extends SyncTests
{
    /**
     * Should sync backward.
     */
    @Test(priority = 20)
    public void shouldSyncBackward()
    {
        final Sync sync;

        // Given a stamp sync with some strings representing time stamps,
        DateTime.simulateTimeZone(GMT_TIME_ZONE);
        sync = new StampsSync(
            new String[] {"2007-10-01", "2007-10-10", "2007-01-01",
                    "2007-01-10", },
            DateTime.getZoneId());

        // when starting with the end of time
        // and asking for each previous stamp,
        Require.failure(sync.isInSync(DateTime.END_OF_TIME), "in sync");
        Require
            .equal(
                sync.getPreviousStamp().get(),
                DateTime.fromString("2007-10-10"),
                "previous stamp");
        Require
            .equal(
                sync.getPreviousStamp().get(),
                DateTime.fromString("2007-10-01"),
                "previous stamp");
        Require
            .equal(
                sync.getPreviousStamp().get(),
                DateTime.fromString("2007-01-10"),
                "previous stamp");
        Require
            .equal(
                sync.getPreviousStamp().get(),
                DateTime.fromString("2007-01-01"),
                "previous stamp");
        Require.notPresent(sync.getPreviousStamp(), "previous stamp");

        // then it should have returned expected values.
    }

    /**
     * Should sync forward.
     */
    @Test(priority = 10)
    public void shouldSyncForward()
    {
        final Sync sync;

        // Given a stamp sync with some strings representing time stamps,
        DateTime.simulateTimeZone(GMT_TIME_ZONE);
        sync = new StampsSync(
            new String[] {"2007-10-01", "2007-10-10", "2007-01-01",
                    "2007-01-10", },
            DateTime.getZoneId());

        // when starting with the beginning of time
        // and asking for each next stamp,
        Require.failure(sync.isInSync(DateTime.BEGINNING_OF_TIME), "in sync");
        Require
            .equal(
                sync.getNextStamp().get(),
                DateTime.fromString("2007-01-01"),
                "next stamp");
        Require
            .equal(
                sync.getNextStamp().get(),
                DateTime.fromString("2007-01-10"),
                "next stamp");
        Require
            .equal(
                sync.getNextStamp().get(),
                DateTime.fromString("2007-10-01"),
                "next stamp");
        Require
            .equal(
                sync.getNextStamp().get(),
                DateTime.fromString("2007-10-10"),
                "next stamp");
        Require.notPresent(sync.getNextStamp(), "next stamp");

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
