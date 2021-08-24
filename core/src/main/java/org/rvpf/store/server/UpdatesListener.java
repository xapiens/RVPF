/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UpdatesListener.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.store.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Updates listener.
 */
public interface UpdatesListener
{
    /**
     * Called when the metadata has been refreshed.
     *
     * @param metadata The new metadata.
     *
     * @throws ServiceNotAvailableException When the service is not available.
     */
    void onMetadataRefreshed(
            @Nonnull Metadata metadata)
        throws ServiceNotAvailableException;

    /**
     * Sets the store server.
     *
     * @param server The store server.
     */
    void setServer(@Nonnull StoreServer server);

    /**
     * Sets up for processing.
     *
     * @param storeAppImpl The store application implementation.
     * @param listenerProperties The listener properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull StoreServiceAppImpl storeAppImpl,
            @Nonnull KeyedGroups listenerProperties);

    /**
     * Starts this.
     */
    void start();

    /**
     * Stops this.
     */
    void stop();

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Abstract updates listener.
     */
    abstract class Abstract
        implements UpdatesListener, ServiceThread.Target
    {
        /** {@inheritDoc}
         */
        @Override
        public void onMetadataRefreshed(final Metadata metadata) {}

        /** {@inheritDoc}
         */
        @Override
        public final void run()
            throws ServiceNotAvailableException, InterruptedException
        {
            final List<PointValue> updates = new ArrayList<PointValue>(
                _batchLimit);

            doStart();

            ServiceThread.ready();

            for (;;) {
                int count = 0;
                PointValue update = nextUpdate(_batchLimit, true).get();

                if (update == null) {
                    getThisLogger().warn(StoreMessages.LOST_UPDATE_QUEUE);

                    break;
                }

                _server.disableSuspend();

                try {
                    do {
                        if (getThisLogger().isTraceEnabled()) {
                            update = update.restore(_server.getMetadata());
                            getThisLogger()
                                .trace(StoreMessages.UPDATE_RECEIVED, update);
                        }

                        updates.add(update);

                        if (++count >= _batchLimit) {
                            Require.success(count == _batchLimit);

                            break;
                        }

                        update = nextUpdate(_batchLimit - count, false)
                            .orElse(null);
                    } while (update != null);

                    if (_server
                        .update(
                            updates.toArray(new PointValue[updates.size()]),
                            Optional.empty()) == null) {
                        throw new InterruptedException();
                    }

                    synchronized (this) {
                        doCommit();
                    }

                    updates.clear();
                } finally {
                    _server.enableSuspend();
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final void setServer(final StoreServer server)
        {
            _server = server;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(
                final StoreServiceAppImpl storeAppImpl,
                final KeyedGroups listenerProperties)
        {
            _storeAppImpl = storeAppImpl;
            _stats = storeAppImpl.getStoreStats();

            _batchLimit = listenerProperties
                .getInt(BATCH_LIMIT_PROPERTY, DEFAULT_BATCH_LIMIT);

            if (_batchLimit <= 0) {
                _batchLimit = 1;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public final void start()
        {
            if (_server == null) {
                throw new IllegalStateException(
                    "The store server has not been registered");
            }

            final ServiceThread thread = new ServiceThread(
                this,
                "Store updates listener for ["
                + _storeAppImpl.getService().getServiceName() + "]");

            if (_thread.compareAndSet(null, thread)) {
                getThisLogger()
                    .debug(ServiceMessages.STARTING_THREAD, thread.getName());
                Require.ignored(thread.start(true));
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final void stop()
        {
            final ServiceThread thread = _thread.getAndSet(null);

            if (thread != null) {
                getThisLogger()
                    .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
                thread.interrupt();
                doStop();
                Require.ignored(thread.join(getThisLogger(), 0));
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            stop();
        }

        /**
         * Registers that the updates have been processed.
         *
         * <p>Called while synchronized on this.</p>
         *
         * @throws InterruptedException When the Service is stopped.
         */
        protected abstract void doCommit()
            throws InterruptedException;

        /**
         * Does start.
         *
         * @throws InterruptedException When the service is stopped.
         */
        protected void doStart()
            throws InterruptedException {}

        /**
         * Stops in preparation to tear down.
         *
         * <p>Called while synchronized on this.</p>
         */
        protected abstract void doStop();

        /**
         * Gets the store server.
         *
         * @return The store server.
         */
        @Nonnull
        @CheckReturnValue
        protected final StoreServer getServer()
        {
            return Require.notNull(_server);
        }

        /**
         * Gets the stats.
         *
         * @return The stats.
         */
        @Nonnull
        @CheckReturnValue
        protected final StoreStats getStats()
        {
            return Require.notNull(_stats);
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
         * Returns the next update.
         *
         * @param limit The read ahead limit.
         * @param wait True to wait.
         *
         * @return A point value (empty if none).
         *
         * @throws InterruptedException When the Service is stopped.
         */
        @Nonnull
        @CheckReturnValue
        protected abstract Optional<PointValue> nextUpdate(
                int limit,
                boolean wait)
            throws InterruptedException;

        /**
         * Specifies to batch the updates by this number. A value less than 1
         * disables batching.
         */
        public static final String BATCH_LIMIT_PROPERTY = "batch.limit";

        /** Default batch limit. */
        public static final int DEFAULT_BATCH_LIMIT = 1000;

        private int _batchLimit;
        private final Logger _logger = Logger.getInstance(getClass());
        private StoreServer _server;
        private StoreStats _stats;
        private StoreServiceAppImpl _storeAppImpl;
        private final AtomicReference<ServiceThread> _thread =
            new AtomicReference<>();
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
