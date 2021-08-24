/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ScheduledArchiver.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.store.server.archiver;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.sync.CrontabSync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServiceAppImpl;

/**
 * Scheduled archiver.
 */
public final class ScheduledArchiver
    extends Archiver.Abstract
    implements ServiceThread.Target
{
    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws ServiceNotAvailableException, InterruptedException
    {
        final SnoozeAlarm snoozeAlarm = new SnoozeAlarm();
        DateTime scanTime = _sync.getNextStamp(DateTime.now()).orElse(null);

        if (scanTime == null) {
            scanTime = DateTime.now();
        }

        do {
            Require.ignored(snoozeAlarm.snooze(Optional.of(scanTime)));

            for (final PointReference pointReference: getPointReferences()) {
                archive(pointReference, scanTime);
            }

            commitAttic();
            updateStats();

            scanTime = _sync.getNextStamp(DateTime.now()).orElse(null);
        } while (scanTime != null);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(@Nonnull final StoreServiceAppImpl storeAppImpl)
    {
        if (!super.setUp(storeAppImpl)) {
            return false;
        }

        if (isDisabled()) {
            return true;
        }

        final String schedule = getProperties()
            .getString(SCHEDULE_PROPERTY, Optional.of(DEFAULT_SCHEDULE))
            .get()
            .trim();

        _sync = new CrontabSync();

        if (schedule.length() > 0) {
            getThisLogger().info(StoreMessages.ARCHIVER_SCHEDULE, schedule);

            if (!_sync.setUp(schedule)) {
                return false;
            }
        }

        final Integer pointCount = Integer.valueOf(getPointReferences().size());

        getThisLogger()
            .info(
                StoreMessages.ARCHIVER_SET_UP,
                pointCount.toString(),
                pointCount);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        if (!getPointReferences().isEmpty()) {
            final ServiceThread thread = new ServiceThread(
                this,
                "Store archiver for ["
                + getStoreAppImpl().getService().getServiceName() + "]");

            if (_thread.compareAndSet(null, thread)) {
                getThisLogger()
                    .debug(ServiceMessages.STARTING_THREAD, thread.getName());
                thread.start();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if ((thread != null) && thread.isAlive()) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            Require
                .ignored(
                    thread
                        .interruptAndJoin(
                                getThisLogger(),
                                        getStoreAppImpl().getJoinTimeout()));
        }
    }

    /** Default schedule. */
    public static final String DEFAULT_SCHEDULE = "0 0 * * *";

    /** Schedule property. */
    public static final String SCHEDULE_PROPERTY = "schedule";

    private CrontabSync _sync;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
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
