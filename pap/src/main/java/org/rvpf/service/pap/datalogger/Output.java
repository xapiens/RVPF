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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.StorageMonitor;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.service.som.SOMFactory;
import org.rvpf.service.som.SOMSender;
import org.rvpf.store.client.ProxyStoreClient;

/**
 * Output.
 *
 * <p>A datalogger output sends updates either to a queue, a common store or the
 * store specified by the update point.</p>
 */
final class Output
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param sender The sender.
     * @param storeClient The store client.
     * @param storageMonitor The storage monitor.
     * @param updatesLimit The updates limit.
     * @param traces The traces instance.
     */
    Output(
            @Nonnull final Optional<SOMSender> sender,
            @Nonnull final Optional<ProxyStoreClient> storeClient,
            @Nonnull final StorageMonitor storageMonitor,
            final int updatesLimit,
            @Nonnull final Traces traces)
    {
        _storeClient = storeClient;
        _sender = sender;
        _storageMonitor = storageMonitor;
        _updatesLimit = updatesLimit;
        _traces = traces;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws InterruptedException, ServiceNotAvailableException
    {
        final List<PointValue> pointValues = new LinkedList<>();
        boolean stopping = false;

        do {
            {
                PointValue pointValue = _updates.take();

                do {
                    if (pointValue == PointValue.NULL) {
                        stopping = true;

                        if (pointValues.isEmpty()) {
                            return;
                        }

                        break;
                    }

                    _LOGGER
                        .trace(ModbusMessages.OUTPUT_POINT_VALUE, pointValue);
                    pointValues.add(pointValue);
                    pointValue = _updates.poll();
                } while (pointValue != null);
            }

            if (_traces.isEnabled()) {
                for (final PointValue pointValue: pointValues) {
                    _traces.add(pointValue);
                }

                _traces.commit();
            }

            if (_sender.isPresent()) {
                if (!_sender
                    .get()
                    .send(
                        pointValues.toArray(new PointValue[pointValues.size()]),
                        true)) {
                    throw new ServiceNotAvailableException(
                        _sender.get().getException().get());
                }
            } else if (_storeClient.isPresent()) {
                final StoreSessionProxy store = _storeClient.get().getStore();

                if (!store.updateAndCheck(pointValues, _LOGGER)) {
                    throw new ServiceNotAvailableException(
                        store.getException().get());
                }
            } else {
                final Set<Store> stores = new IdentityHashSet<>();

                for (final PointValue pointValue: pointValues) {
                    final Point point = pointValue.getPoint().get();
                    final Optional<? extends Store> store = point.getStore();

                    if (store.isPresent()) {
                        store.get().addUpdate(pointValue);
                        stores.add(store.get());
                    }
                }

                for (final Store store: stores) {
                    if (!store.sendUpdates()) {
                        throw new ServiceNotAvailableException();
                    }
                }
            }

            pointValues.clear();

            final CountDownLatch scanLatch = _latch.get();

            if ((scanLatch != null) && _updates.isEmpty()) {
                scanLatch.countDown();
                _latch.compareAndSet(scanLatch, null);
            }
        } while (!stopping);
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Limits updates.
     *
     * @throws InterruptedException When interrupted.
     */
    void limitUpdates()
        throws InterruptedException
    {
        if (_latch.get() == null) {
            if (_updates.size() > _updatesLimit) {
                if (_latch.compareAndSet(null, new CountDownLatch(1))) {
                    _LOGGER.warn(PAPMessages.LOST_EVENTS);
                }
            }
        }

        final CountDownLatch scanLatch = _latch.get();

        if (scanLatch != null) {
            scanLatch.await();
        }
    }

    /**
     * Sends updates.
     *
     * @param pointValues The updates.
     */
    void sendUpdates(@Nonnull final Collection<PointValue> pointValues)
    {
        for (final PointValue pointValue: pointValues) {
            _updates.add(pointValue);
        }
    }

    /**
     * Starts.
     */
    final void start()
    {
        _thread = new ServiceThread(this, "Datalogger output");

        _LOGGER.debug(ServiceMessages.STARTING_THREAD, _thread.getName());

        _thread.start();
    }

    /**
     * Stops.
     *
     * @param joinTimeout The join timeout.
     */
    final void stop(final long joinTimeout)
    {
        if (_thread != null) {
            _updates.add(PointValue.NULL);
            Require.ignored(_thread.join(_LOGGER, joinTimeout));

            if (_sender.isPresent()) {
                _sender.get().close();
            }

            if (_storeClient.isPresent()) {
                _storeClient.get().tearDown();
            }

            _traces.tearDown();
            _thread = null;
        }
    }

    /**
     * Storage monitor check.
     *
     * @return True unless on alert.
     */
    @CheckReturnValue
    boolean storageMonitorCheck()
    {
        return _storageMonitor.check();
    }

    private static final Logger _LOGGER = Logger.getInstance(Output.class);

    private final AtomicReference<CountDownLatch> _latch =
        new AtomicReference<CountDownLatch>();
    private final Optional<SOMSender> _sender;
    private final StorageMonitor _storageMonitor;
    private final Optional<ProxyStoreClient> _storeClient;
    private ServiceThread _thread;
    private final Traces _traces;
    private final BlockingQueue<PointValue> _updates =
        new LinkedBlockingQueue<>();
    private final int _updatesLimit;

    /**
     * Builder.
     */
    static final class Builder
    {
        /**
         * No instances.
         */
        public Builder() {}

        /**
         * Applies properties.
         *
         * @param outputProperties The output properties.
         *
         * @return This (null on failure).
         */
        @Nullable
        @CheckReturnValue
        Builder applyProperties(@Nonnull final KeyedGroups outputProperties)
        {
            final KeyedGroups queueProperties = outputProperties
                .getGroup(QUEUE_PROPERTIES);

            if (queueProperties.isMissing()) {
                final ProxyStoreClient storeClient;

                if (outputProperties.containsGroupKey(STORE_PROPERTIES)) {
                    storeClient = new ProxyStoreClient();

                    if (!storeClient
                        .setUp(_dataloggerApp.getConfig(), outputProperties)) {
                        return null;
                    }

                    _dataloggerApp
                        .getLogger()
                        .debug(
                            PAPMessages.DATALOGGER_DESTINATION_STORE,
                            storeClient.getStore().getServerName());
                } else {
                    storeClient = null;
                }

                _storeClient = Optional.ofNullable(storeClient);
                _sender = Optional.empty();
            } else {
                final SOMFactory factory = new SOMFactory(
                    _dataloggerApp.getConfig());
                final SOMFactory.Queue factoryQueue = factory
                    .createQueue(queueProperties);

                _sender = Optional.of(factoryQueue.createSender(true));

                if (!_sender.isPresent()) {
                    return null;
                }

                _dataloggerApp
                    .getLogger()
                    .debug(
                        PAPMessages.DATALOGGER_DESTINATION_QUEUE,
                        _sender.get());

                if (_sender.get().isRemote()) {
                    _dataloggerApp
                        .getLogger()
                        .warn(
                            ServiceMessages.REMOTE_SERVICE_WARNING,
                            _sender.get());
                }

                _storeClient = Optional.empty();
            }

            // Sets up the traces.

            _traces = new Traces();

            if (!_traces
                .setUp(
                    _dataloggerApp.getDataDir(),
                    _dataloggerApp
                        .getConfigProperties()
                        .getGroup(Traces.TRACES_PROPERTIES),
                    _dataloggerApp.getSourceUUID(),
                    outputProperties.getString(TRACES_PROPERTY))) {
                return null;
            }

            // Sets up the storage monitor.

            _storageMonitor = new StorageMonitor(
                Logger.getInstance(Output.class));

            if (!_storageMonitor
                .setUp(
                    outputProperties.getGroup(STORAGE_PROPERTIES),
                    _dataloggerApp.getDataDir())) {
                return null;
            }

            _updatesLimit = outputProperties.getInt(LIMIT_PROPERTY, 0);

            if (_updatesLimit <= 0) {
                _updatesLimit = Integer.MAX_VALUE;
            }

            return this;
        }

        /**
         * Builds an output.
         *
         * @return The output.
         */
        @Nonnull
        @CheckReturnValue
        Output build()
        {
            return new Output(
                _sender,
                _storeClient,
                _storageMonitor,
                _updatesLimit,
                _traces);
        }

        /**
         * Sets the datalogger app.
         *
         * @param dataloggerApp The datalogger app.
         *
         * @return This.
         */
        @Nonnull
        Builder setDataloggerApp(@Nonnull final DataloggerAppImpl dataloggerApp)
        {
            _dataloggerApp = dataloggerApp;

            return this;
        }

        /** Limit property. */
        public static final String LIMIT_PROPERTY = "limit";

        /** Queue properties. */
        public static final String QUEUE_PROPERTIES = "queue";

        /** Storage properties. */
        public static final String STORAGE_PROPERTIES = "storage";

        /** Store properties. */
        public static final String STORE_PROPERTIES = "store";

        /** Traces subdirectory property. */
        public static final String TRACES_PROPERTY = "traces";

        private DataloggerAppImpl _dataloggerApp;
        private Optional<SOMSender> _sender;
        private StorageMonitor _storageMonitor;
        private Optional<ProxyStoreClient> _storeClient;
        private Traces _traces;
        private int _updatesLimit;
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
