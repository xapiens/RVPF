/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Notifier.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.store.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.value.PointValue;

/**
 * Notifier.
 */
public interface Notifier
    extends NoticeListener
{
    /**
     * Closes this notifier.
     */
    void close();

    /**
     * Joins whatever thread set up by this notifier.
     *
     * @return True on success.
     */
    boolean join();

    /**
     * Sets the filter.
     *
     * @param noticesFilter A Collection of points for which to generate
     *                      notices.
     */
    void setFilter(@Nonnull Optional<Collection<Point>> noticesFilter);

    /**
     * Sets up for processing.
     *
     * @param storeAppImpl The store application implementation.
     *
     * @return True if successful.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull StoreServiceAppImpl storeAppImpl);

    /**
     * Starts this.
     */
    void start();

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Abstract notifier.
     */
    abstract class Abstract
        implements Notifier
    {
        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            if (_closed.compareAndSet(false, true)) {
                if (_noticeCount.get() > 0) {
                    getThisLogger()
                        .warn(
                            StoreMessages.NOTICES_UNCOMMITTED,
                            String.valueOf(_noticeCount.get()));
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void commit()
            throws InterruptedException
        {
            if (!_closed.get()) {
                final long time = _time.getAndSet(0);

                getStoreAppImpl()
                    .getStoreStats()
                    .addNotices(_noticeCount.getAndSet(0), time);

                _traces.commit();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean join()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public final synchronized void notify(
                final PointValue pointValue)
            throws InterruptedException
        {
            if (!_closed.get()) {
                if ((_filter == null)
                        || _filter.contains(pointValue.getPointUUID())) {
                    if (doNotify(pointValue)) {
                        _noticeCount.incrementAndGet();

                        if (_traces != null) {
                            _traces.add(pointValue);
                        }
                    }
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final synchronized void setFilter(
                final Optional<Collection<Point>> noticesFilter)
        {
            if (noticesFilter.isPresent()) {
                _filter = new HashSet<UUID>(noticesFilter.get().size());

                for (final Point point: noticesFilter.get()) {
                    _filter.add(point.getUUID().get());
                }
            } else {
                _filter = null;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final StoreServiceAppImpl storeAppImpl)
        {
            _storeAppImpl = storeAppImpl;

            setFilter(
                storeAppImpl.areNoticesFiltered()? Optional
                    .of(storeAppImpl.getNoticesFilter()): Optional.empty());

            Require
                .ignored(
                    _traces
                        .setUp(
                                storeAppImpl.getDataDir(),
                                        storeAppImpl
                                                .getConfigProperties()
                                                .getGroup(Traces.TRACES_PROPERTIES),
                                        storeAppImpl.getSourceUUID(),
                                        Optional.of(NOTIFIED_TRACES)));

            storeAppImpl.addNoticeListener(this);

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void start() {}

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            close();

            _storeAppImpl.removeNoticeListener(this);

            _traces.tearDown();

            setFilter(Optional.empty());

            _storeAppImpl = null;
        }

        /**
         * Adds time.
         *
         * @param time The time increment in nanoseconds.
         */
        protected final void addTime(final long time)
        {
            _time.addAndGet(time);
        }

        /**
         * Does notify of a new Point value.
         *
         * @param pointValue The Point value.
         *
         * @return True on success.
         *
         * @throws InterruptedException When the Service is stopped.
         */
        @CheckReturnValue
        protected abstract boolean doNotify(
                @Nonnull PointValue pointValue)
            throws InterruptedException;

        /**
         * Gets the notices count.
         *
         * @return The notices count.
         */
        @CheckReturnValue
        protected final int getNoticeCount()
        {
            return _noticeCount.get();
        }

        /**
         * Gets the store application implementation.
         *
         * @return The store application implementation.
         */
        @Nonnull
        @CheckReturnValue
        protected final StoreServiceAppImpl getStoreAppImpl()
        {
            return Require.notNull(_storeAppImpl);
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        protected final Logger getThisLogger()
        {
            return _logger;
        }

        /**
         * Asks if this notifier is closed.
         *
         * @return True if this notifier is closed.
         */
        @CheckReturnValue
        protected boolean isClosed()
        {
            return _closed.get();
        }

        /** Traces subdirectory for notified values. */
        public static final String NOTIFIED_TRACES = "notified";

        private final AtomicBoolean _closed = new AtomicBoolean();
        private Set<UUID> _filter;
        private final Logger _logger = Logger.getInstance(getClass());
        private final AtomicInteger _noticeCount = new AtomicInteger();
        private StoreServiceAppImpl _storeAppImpl;
        private final AtomicLong _time = new AtomicLong();
        private final Traces _traces = new Traces();
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
