/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreListenerReceptionist.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.processor.receptionist;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Store listener receptionist.
 */
public class StoreListenerReceptionist
    extends Receptionist.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata)
    {
        if (!super.setUp(metadata)) {
            return false;
        }

        final Set<Store> stores = new IdentityHashSet<>();

        for (final Point point: metadata.getPointsCollection()) {
            if (!point.getResults().isEmpty()) {
                final Store store = point.getStore().get();

                if (!stores.contains(store)) {
                    stores.add(store);

                    try {
                        if (!store.supportsDeliver()) {
                            continue;
                        }
                    } catch (final StoreAccessException exception) {
                        getThisLogger().error(BaseMessages.VERBATIM, exception);

                        return false;
                    }
                } else if (!_pointsByStore.containsValueKey(store.getName())) {
                    continue;
                }

                final String storeName = Require.notNull(store.getName());
                final UUID pointUUID = point.getUUID().get();

                _pointsByStore.add(storeName, pointUUID);
            }
        }

        boolean success = true;

        for (final Map.Entry<String, List<Object>> entry:
                _pointsByStore.getValuesEntries()) {
            final List<Object> points = entry.getValue();
            final UUID[] pointUUIDs = points.toArray(new UUID[points.size()]);
            final StoreEntity storeEntity = metadata
                .getStoreEntity(Optional.of(entry.getKey()))
                .get();
            final Store store = storeEntity.getStore().get();

            try {
                success &= store.subscribe(pointUUIDs);
            } catch (final StoreAccessException exception) {
                throw new RuntimeException(exception);
            }

            _listeners.add(new _Listener(store));
        }

        if (!success) {
            return false;
        }

        getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        super.tearDown();

        final Metadata metadata = getMetadata();

        for (final Map.Entry<String, List<Object>> entry:
                _pointsByStore.getValuesEntries()) {
            final List<Object> points = entry.getValue();
            final UUID[] pointUUIDs = points.toArray(new UUID[points.size()]);
            final StoreEntity storeEntity = metadata
                .getStoreEntity(Optional.of(entry.getKey()))
                .get();
            final Store store = storeEntity.getStore().get();

            try {
                store.unsubscribe(pointUUIDs);
            } catch (final StoreAccessException exception) {
                if (!(exception.getCause() instanceof ServiceClosedException)) {
                    throw new RuntimeException(exception);
                }
            }
        }

        getThisLogger().debug(ServiceMessages.TEAR_DOWN_COMPLETED);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doClose()
    {
        for (final _Listener listener: _listeners) {
            listener.stop();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doCommit()
        throws InterruptedException, ServiceNotAvailableException
    {
        _currentTransaction.close();
        _currentTransaction = null;
    }

    /** {@inheritDoc}
     */
    @Override
    protected PointValue doFetchNotice(
            final int limit,
            final long wait)
        throws InterruptedException, ServiceNotAvailableException
    {
        for (;;) {
            if (_currentTransaction == null) {
                Require.failure(wait >= 0);

                _currentTransaction = _transactions.take();
            }

            final PointValue notice = _currentTransaction.nextNotice();

            if ((notice != null) || (wait >= 0)) {
                return notice;
            }

            doCommit();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doOpen()
    {
        for (final _Listener listener: _listeners) {
            listener.start();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doRollback()
        throws InterruptedException, ServiceNotAvailableException
    {
        _currentTransaction.reset();
    }

    /**
     * Adds a transaction.
     *
     * @param transaction The transaction.
     */
    void addTransaction(final _Listener._Transaction transaction)
    {
        _transactions.add(transaction);
    }

    private _Listener._Transaction _currentTransaction;
    private final List<_Listener> _listeners = new LinkedList<>();
    private final KeyedValues _pointsByStore = new KeyedValues();
    private final BlockingQueue<_Listener._Transaction> _transactions =
        new LinkedBlockingQueue<>();

    /**
     * Listener.
     */
    private class _Listener
        implements ServiceThread.Target
    {
        /**
         * Constructs an instance.
         *
         * @param store The store to listen from.
         */
        _Listener(final Store store)
        {
            _store = store;
        }

        /** {@inheritDoc}
         */
        @Override
        public void run()
            throws StoreAccessException, InterruptedException
        {
            final int responseLimit = _store.getResponseLimit();

            for (;;) {
                final StoreValues response;

                try {
                    response = _store.deliver(responseLimit, -1);
                } catch (final StoreAccessException exception) {
                    if (_stopping) {
                        break;
                    }

                    throw exception;
                }

                if (response.isEmpty()) {
                    continue;
                }

                synchronized (this) {
                    for (final PointValue notice: response) {
                        _deliveredNotices.add(notice);
                    }

                    if (_queuedTransaction == null) {
                        _queuedTransaction = new _Transaction();
                        addTransaction(_queuedTransaction);
                    }
                }
            }
        }

        /**
         * Returns the next notice.
         *
         * @return The notice.
         */
        synchronized PointValue nextNotice()
        {
            return _deliveredNotices.poll();
        }

        /**
         * Called when the transaction is closed.
         *
         * @param transaction The transaction.
         */
        synchronized void onTransactionClosed(final _Transaction transaction)
        {
            Require.success(transaction == _queuedTransaction);

            if (_deliveredNotices.isEmpty()) {
                _queuedTransaction = null;
            } else {
                _queuedTransaction = new _Transaction();
                addTransaction(_queuedTransaction);
            }
        }

        /**
         * Starts.
         */
        void start()
        {
            final ServiceThread thread = new ServiceThread(
                this,
                "Store " + _store.getName() + " listener");

            if (_thread.compareAndSet(null, thread)) {
                _stopping = false;
                getThisLogger()
                    .debug(ServiceMessages.STARTING_THREAD, thread.getName());
                thread.start();
            }
        }

        /**
         * Stops.
         */
        void stop()
        {
            final ServiceThread thread = _thread.getAndSet(null);

            if (thread != null) {
                getThisLogger()
                    .debug(ServiceMessages.STOPPING_THREAD, thread.getName());

                _stopping = true;
                _store.close();

                thread.interrupt();

                try {
                    thread.join();
                } catch (final InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * Takes back the notices.
         *
         * @param notices The notices.
         */
        synchronized void takeBack(final Collection<PointValue> notices)
        {
            _deliveredNotices.addAll(0, notices);
        }

        private final LinkedList<PointValue> _deliveredNotices =
            new LinkedList<>();
        private _Transaction _queuedTransaction;
        private volatile boolean _stopping;
        private final Store _store;
        private final AtomicReference<ServiceThread> _thread =
            new AtomicReference<>();

        /**
         * Transaction.
         */
        private class _Transaction
        {
            /**
             * Constructs an instance.
             */
            _Transaction() {}

            void close()
            {
                _notices.clear();
                onTransactionClosed(this);
            }

            /**
             * Return the next notice.
             *
             * @return The next notice (null when none left).
             */
            PointValue nextNotice()
            {
                final PointValue notice = _Listener.this.nextNotice();

                if (notice != null) {
                    _notices.add(notice);
                }

                return notice;
            }

            /**
             * Resets.
             */
            void reset()
            {
                takeBack(_notices);
                _notices.clear();
            }

            private final Collection<PointValue> _notices = new LinkedList<>();
        }
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
