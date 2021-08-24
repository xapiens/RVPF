/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NotifierThread.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.store.server.c;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.store.server.StoreMessages;

/**
 * Notifier thread.
 */
final class NotifierThread
    implements ServiceThread.Target
{
    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws InterruptedException, ExecutionException
    {
        Logger.setLogID(Optional.ofNullable(_logID));

        final CStoreServiceAppImpl cStoreAppImpl = _cStoreAppImpl;
        final int limit = cStoreAppImpl.getBackEndLimit();
        final CStore cStore = _cStore;
        final long timeout = cStore.supportsThreads()? -1: 0;
        final Callable<Collection<PointValue>> receiver =
            new Callable<Collection<PointValue>>()
        {
            @Override
            public Collection<PointValue> call()
                throws Exception
            {
                final Values values = cStore.deliver(limit, timeout);

                if (values.statusCode() != Status.SUCCESS_CODE) {
                    if (values.statusCode() == Status.DISCONNECTED_CODE) {
                        return null;
                    }

                    throw new Status.FailedException(values.statusCode());
                }

                return (values
                    .size() > 0)? cStore
                        .pointValues(
                            values): Collections.<PointValue>emptyList();
            }
        };

        while (!Thread.interrupted()) {
            final CStore.Task<Collection<PointValue>> task =
                new CStore.Task<Collection<PointValue>>(
                    receiver);
            final Collection<PointValue> pointValues;

            cStore.execute(task);
            pointValues = task.get();

            if (pointValues == null) {
                break;
            }

            if (!pointValues.isEmpty()) {
                cStoreAppImpl.getService().disableSuspend();

                try {
                    final Metadata metadata = cStoreAppImpl
                        .getService()
                        .getMetadata();

                    synchronized (cStore) {
                        for (PointValue pointValue: pointValues) {
                            pointValue = pointValue.restore(metadata);
                            _LOGGER
                                .debug(
                                    ServiceMessages.RECEIVED_VALUE,
                                    pointValue);
                            cStoreAppImpl.addNotice(pointValue);
                        }

                        cStoreAppImpl.sendNotices();
                    }
                } finally {
                    cStoreAppImpl.getService().enableSuspend();
                }
            }

            final long pollInterval = _pollInterval;

            if (pollInterval > 0) {
                final long startMillis = System.currentTimeMillis();

                synchronized (this) {
                    while (!_wakeUp) {
                        final long elapsedMillis = System
                            .currentTimeMillis() - startMillis;

                        if ((elapsedMillis < 0)
                                || (elapsedMillis >= pollInterval)) {
                            break;
                        }

                        wait(pollInterval - elapsedMillis);
                    }

                    _wakeUp = false;
                }
            }
        }
    }

    /**
     * Set up this.
     *
     * @param cStoreAppImpl The C store service application instance.
     * @param cStore The C store.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull final CStoreServiceAppImpl cStoreAppImpl,
            @Nonnull final CStore cStore)
    {
        _cStoreAppImpl = cStoreAppImpl;
        _cStore = cStore;

        final Optional<ElapsedTime> pollInterval;

        if (_cStore.supportsThreads()) {
            pollInterval = Optional.empty();
        } else {
            pollInterval = _cStoreAppImpl
                .getServerProperties()
                .getElapsed(
                    CStoreServiceAppImpl.NOTIFIER_POLL_INTERVAL_PROPERTY,
                    Optional
                        .of(CStoreServiceAppImpl.DEFAULT_NOTIFIER_POLL_INTERVAL),
                    Optional
                        .of(CStoreServiceAppImpl.DEFAULT_NOTIFIER_POLL_INTERVAL));
        }

        if (pollInterval.isPresent()) {
            if (!SnoozeAlarm
                .validate(
                    pollInterval.get(),
                    this,
                    StoreMessages.POLL_INTERVAL_TEXT)) {
                return false;
            }

            _LOGGER.info(StoreMessages.POLL_INTERVAL, pollInterval.get());
            _pollInterval = pollInterval.get().toMillis();
        } else {
            _pollInterval = 0;
        }

        return true;
    }

    /**
     * Starts the thread.
     */
    void start()
    {
        final ServiceThread thread = new ServiceThread(
            this,
            "Notifier for [" + _cStoreAppImpl.getService().getServiceName()
            + "]");

        if (_thread.compareAndSet(null, thread)) {
            thread
                .setUncaughtExceptionHandler(
                    Thread.currentThread().getUncaughtExceptionHandler());
            _LOGGER.debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
            _logID = Logger.currentLogID().orElse(null);
            _LOGGER.debug(StoreMessages.NOTIFIER_STARTED);
        }
    }

    /**
     * Stops the thread.
     */
    void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if ((thread != null) && thread.isAlive()) {
            _LOGGER.debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            thread.interrupt();

            if (_cStore.supportsThreads()) {
                _cStore.interrupt();
            }

            Require
                .ignored(thread.join(_LOGGER, _cStoreAppImpl.getJoinTimeout()));
            _LOGGER.debug(StoreMessages.NOTIFIER_STOPPED);
        }
    }

    /**
     * Tears down this.
     */
    void tearDown()
    {
        stop();
        _cStore = null;
        _cStoreAppImpl = null;
    }

    /**
     * Wakes up.
     */
    synchronized void wakeUp()
    {
        _wakeUp = true;
        notifyAll();
    }

    private static final Logger _LOGGER = Logger
        .getInstance(NotifierThread.class);

    private volatile CStore _cStore;
    private volatile CStoreServiceAppImpl _cStoreAppImpl;
    private String _logID;
    private volatile long _pollInterval;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
    private boolean _wakeUp;
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
