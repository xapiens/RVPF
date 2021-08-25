/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.service.pap.datalogger;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.util.Schedule;

/**
 * Scan schedule.
 */
final class ScanSchedule
    extends Schedule<Schedule.PointEvent>
{
    /**
     * Constructs an instance.
     */
    public ScanSchedule()
    {
        super(true);
    }

    /**
     * Returns all due events.
     *
     * @param wait True will wait for at least one event.
     * @param distinct True will return only distinct events.
     *
     * @return All due events.
     *
     * @throws InterruptedException When interrupted.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<PointEvent> allDue(
            boolean wait,
            final boolean distinct)
        throws InterruptedException
    {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        final Collection<PointEvent> allDue = distinct
            ? new LinkedHashSet<>(): new LinkedList<>();

        synchronized (getMutex()) {
            if (!_allDue) {
                final PointEvent firstEvent = peek()
                    .orElse(null);    // Gets reuse protection.

                while (hasNext()) {
                    final PointEvent event = next(wait).orElse(null);

                    if (event == null) {
                        break;
                    }

                    if ((event == firstEvent)
                            && !event.equals(firstEvent)) {    // Avoids reuse.
                        rollback();

                        break;
                    }

                    allDue.add(event);
                    wait = false;
                }
            }

            if (wait && _allDue) {
                for (final PointEvent event: getEvents()) {
                    allDue.add(event);
                }

                _allDue = false;
            }
        }

        return allDue;
    }

    /**
     * Resyncs.
     *
     * @param sync The sync template.
     */
    public void resync(@Nonnull final Sync sync)
    {
        synchronized (getMutex()) {
            final PointEvent[] pointEvents = toArray(new PointEvent[size()]);
            final DateTime startStamp = DateTime.now();

            clear();

            for (final PointEvent pointEvent: pointEvents) {
                add(
                    new PointEvent(
                        pointEvent.getPoint(),
                        startStamp,
                        sync.copy()));
            }
        }
    }

    /**
     * Triggers now.
     */
    public void triggerNow()
    {
        synchronized (getMutex()) {
            _allDue = true;
            cancelWait();
        }
    }

    private boolean _allDue;
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
