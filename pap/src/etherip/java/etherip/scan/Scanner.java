/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.scan;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import etherip.Tag;

import etherip.protocol.Connection;

/**
 * Timer-based periodic scanner for {@link ScanList Scan Lists}
 *
 * @author Kay Kasemir
 */
@SuppressWarnings(
{
    "hiding", "boxing", "static-method"
})
class Scanner
{
    public Scanner(final Connection connection)
    {
        this.connection = connection;
    }

    public Tag add(final double period_secs, final String tag_name)
    {
        // Locate suitable scan list
        final long ms = this.convertToMillisec(period_secs);
        ScanList list = this.scan_lists.get(ms);

        if (list == null) {
            list = new ScanList(period_secs, this.connection);
            this.scan_lists.put(ms, list);
            this.timer.schedule(list, ms, ms);
        }

        return list.add(tag_name);
    }

    public void stop()
    {
        this.timer.cancel();

        for (final ScanList list: this.scan_lists.values()) {
            list.cancel();
        }
    }

    private long convertToMillisec(final double seconds)
    {
        if (seconds <= 0.1) {
            return 100;
        }

        return (long) (seconds * 1000);
    }

    final Timer timer = new Timer("Scan Timer");

    /** Scan lists by scan period in ms */
    final Map<Long, ScanList> scan_lists = new HashMap<>();
    final private Connection connection;
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
