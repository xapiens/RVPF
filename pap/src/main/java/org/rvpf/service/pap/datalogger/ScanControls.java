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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Content;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.sync.CrontabSync;
import org.rvpf.base.sync.ElapsedSync;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.content.BooleanContent;
import org.rvpf.content.FloatingPointContent;
import org.rvpf.content.NumberContent;
import org.rvpf.content.StringContent;
import org.rvpf.pap.PAPMessages;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Scan controls.
 */
final class ScanControls
{
    /**
     * Constructs an instance.
     *
     * @param scannerName The scanner name.
     * @param schedules The schedules.
     * @param storeListenerBuilders The store listener builders.
     * @param pointValueActions The point value actions.
     * @param barrierOpen True if the barrier is open.
     */
    ScanControls(
            @Nonnull final String scannerName,
            @Nonnull final Collection<ScanSchedule> schedules,
            @Nonnull final Map<UUID,
            _StoreListener.Builder> storeListenerBuilders,
            @Nonnull final Map<UUID,
            BiConsumer<ScanControls, PointValue>> pointValueActions,
            final boolean barrierOpen)
    {
        _scannerName = scannerName;
        _schedules = schedules;

        for (final Map.Entry<UUID, _StoreListener.Builder> storeListenerBuilder:
                storeListenerBuilders.entrySet()) {
            _storeListeners
                .put(
                    storeListenerBuilder.getKey(),
                    storeListenerBuilder.getValue().setOwner(this).build());
        }

        _pointValueActions = pointValueActions;
        _barrierOpen = barrierOpen;
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Asks if the barrier is open.
     *
     * @return True if the barrier is open.
     */
    @CheckReturnValue
    public boolean isBarrierOpen()
    {
        return _barrierOpen;
    }

    /**
     * Starts.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean start()
    {
        boolean success = true;

        for (final _StoreListener storeListener: _storeListeners.values()) {
            success &= storeListener.setUp();
        }

        if (!success) {
            return false;
        }

        _started = true;

        if (_syncPointValue != null) {
            _onPointValue(_syncPointValue);
            _syncPointValue = null;
        }

        for (final _StoreListener storeListener: _storeListeners.values()) {
            storeListener.start();
        }

        return true;
    }

    /**
     * Stops.
     *
     * @param joinTimeout The join timeout.
     */
    public void stop(final long joinTimeout)
    {
        for (final _StoreListener storeListener: _storeListeners.values()) {
            storeListener.stop(joinTimeout);
        }
    }

    /**
     * Gets the scanner name.
     *
     * @return The scanner name.
     */
    @Nonnull
    @CheckReturnValue
    String _getScannerName()
    {
        return _scannerName;
    }

    void _onBarrierPointValue(final PointValue pointValue)
    {
        final Boolean barrier = _BOOLEAN_CONTENT.decode(pointValue);

        if (barrier != null) {
            _barrierOpen = barrier.booleanValue();
        }
    }

    void _onCrontabPointValue(final PointValue pointValue)
    {
        if (_started) {
            final Optional<Content> content = pointValue
                .getPoint()
                .get()
                .getContent();
            final String value = (String) content.get().decode(pointValue);

            if (value != null) {
                final CrontabSync crontabSync = new CrontabSync();

                if (crontabSync.setUp(value)) {
                    _resync(crontabSync);
                }
            }
        } else {
            _setSyncPointValue(pointValue);
        }
    }

    void _onEachPointValue(final PointValue pointValue)
    {
        if (_started) {
            for (final ScanSchedule schedule: _schedules) {
                schedule.triggerNow();
            }
        }
    }

    void _onElapsedPointValue(final PointValue pointValue)
    {
        if (_started) {
            final Content content = pointValue
                .getPoint()
                .get()
                .getContent()
                .orElse(null);
            final Number value = (Number) content.decode(pointValue);

            if (value != null) {
                final ElapsedTime elapsedTime;

                if (content instanceof FloatingPointContent) {
                    elapsedTime = ElapsedTime.fromSeconds(value.doubleValue());
                } else {
                    elapsedTime = ElapsedTime.fromMillis(value.longValue());
                }

                _resync(new ElapsedSync(elapsedTime, Optional.empty()));
            }
        } else {
            _setSyncPointValue(pointValue);
        }
    }

    /**
     * Called on new point values.
     *
     * @param pointValues The point values.
     */
    void _onPointValues(@Nonnull final Collection<PointValue> pointValues)
    {
        for (final PointValue pointValue: pointValues) {
            final UUID pointUUID = pointValue.getPointUUID();
            final DateTime pointValueStamp = pointValue.getStamp();
            final DateTime previousStamp = _pointValueStamps.get(pointUUID);

            if ((previousStamp == null)
                    || pointValueStamp.isNotBefore(previousStamp)) {
                _onPointValue(pointValue);
                _pointValueStamps.put(pointUUID, pointValueStamp);
            }
        }
    }

    private void _onPointValue(final PointValue pointValue)
    {
        synchronized (_pointValueActions) {
            final BiConsumer<ScanControls, PointValue> pointValueAction =
                _pointValueActions
                    .get(pointValue.getPointUUID());

            if (pointValueAction != null) {
                pointValueAction.accept(this, pointValue);
            }
        }
    }

    private void _resync(final Sync sync)
    {
        for (final ScanSchedule schedule: _schedules) {
            schedule.resync(sync);
        }
    }

    private void _setSyncPointValue(final PointValue syncPointValue)
    {
        if ((_syncPointValue == null)
                || syncPointValue.getStamp().isAfter(
                    _syncPointValue.getStamp())) {
            _syncPointValue = syncPointValue;
        }
    }

    private static final BooleanContent _BOOLEAN_CONTENT = new BooleanContent();
    static final Logger _LOGGER = Logger.getInstance(ScanControls.class);

    private volatile boolean _barrierOpen;
    private final Map<UUID, BiConsumer<ScanControls, PointValue>> _pointValueActions;
    private final Map<UUID, DateTime> _pointValueStamps = new HashMap<>();
    private final String _scannerName;
    private final Collection<ScanSchedule> _schedules;
    private boolean _started;
    private final Map<UUID, _StoreListener> _storeListeners = new HashMap<>();
    private PointValue _syncPointValue;

    /**
     * Scanner builder.
     */
    static final class Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /**
         * Adds a schedule.
         *
         * @param schedule The schedule.
         *
         * @return This.
         */
        @Nonnull
        Builder addSchedule(@Nonnull final ScanSchedule schedule)
        {
            _schedules.add(schedule);

            return this;
        }

        /**
         * Builds scan controls.
         *
         * @return The scan controls.
         */
        @Nonnull
        @CheckReturnValue
        ScanControls build()
        {
            return new ScanControls(
                _scannerName,
                _schedules,
                _storeListenerBuilders,
                _pointValueActions,
                _barrierOpen);
        }

        /**
         * Asks if the barrier is open.
         *
         * @return True if the barrier is open.
         */
        @CheckReturnValue
        boolean isBarrierOpen()
        {
            return _barrierOpen;
        }

        /**
         * Asks if this schedule may trigger scans.
         *
         * @return True if this schedule may trigger scans.
         */
        @CheckReturnValue
        boolean mayTrigger()
        {
            return _mayTrigger;
        }

        /**
         * Sets the barrier point.
         *
         * @param barrierPoint The barrier point.
         *
         * @return This.
         */
        @Nonnull
        Builder setBarrierPoint(@Nonnull final Point barrierPoint)
        {
            _barrierOpen = false;

            _setPointValueAction(
                barrierPoint,
                ScanControls::_onBarrierPointValue);

            return this;
        }

        /**
         * Sets the crontab point.
         *
         * @param crontabPoint The crontab point.
         *
         * @return This.
         */
        @Nonnull
        Builder setCrontabPoint(@Nonnull final Point crontabPoint)
        {
            final Content content = crontabPoint.getContent().orElse(null);

            if ((content != null) && (content instanceof StringContent)) {
                _mayTrigger = true;

                _setPointValueAction(
                    crontabPoint,
                    ScanControls::_onCrontabPointValue);
            } else {
                _LOGGER.warn(PAPMessages.CRONTAB_REQUIRES_STRING, crontabPoint);
            }

            return this;
        }

        /**
         * Sets the each point.
         *
         * @param eachPoint The each point.
         *
         * @return This.
         */
        @Nonnull
        Builder setEachPoint(@Nonnull final Point eachPoint)
        {
            _mayTrigger = true;

            _setPointValueAction(eachPoint, ScanControls::_onEachPointValue);

            return this;
        }

        /**
         * Sets the elapsed point.
         *
         * @param elapsedPoint The elapsed point.
         *
         * @return This.
         */
        @Nonnull
        Builder setElapsedPoint(@Nonnull final Point elapsedPoint)
        {
            final Content content = elapsedPoint.getContent().orElse(null);

            if ((content != null) && (content instanceof NumberContent)) {
                _mayTrigger = true;

                _setPointValueAction(
                    elapsedPoint,
                    ScanControls::_onElapsedPointValue);
            } else {
                _LOGGER.warn(PAPMessages.ELAPSED_REQUIRES_NUMBER, elapsedPoint);
            }

            return this;
        }

        /**
         * Sets the scanner name.
         *
         * @param scannerName The scanner name.
         *
         * @return This.
         */
        @Nonnull
        Builder setScannerName(final String scannerName)
        {
            _scannerName = scannerName;

            return this;
        }

        private void _setPointValueAction(
                final Point point,
                final BiConsumer<ScanControls, PointValue> action)
        {
            final Store store = point.getStore().get();
            _StoreListener.Builder storeListenerBuilder = _storeListenerBuilders
                .get(store.getUUID());

            if (storeListenerBuilder == null) {
                storeListenerBuilder = _StoreListener
                    .newBuilder()
                    .setStore(store);
                _storeListenerBuilders
                    .put(store.getUUID(), storeListenerBuilder);
            }

            storeListenerBuilder.addPoint(point);

            _pointValueActions.put(point.getUUID().get(), action);
        }

        private boolean _barrierOpen = true;
        private boolean _mayTrigger;
        private final Map<UUID, BiConsumer<ScanControls, PointValue>> _pointValueActions =
            new HashMap<>();
        private String _scannerName;
        private final Collection<ScanSchedule> _schedules = new HashSet<>();
        private final Map<UUID, _StoreListener.Builder> _storeListenerBuilders =
            new HashMap<>();
    }


    /**
     * Store listener.
     */
    private static final class _StoreListener
        implements ServiceThread.Target
    {
        /**
         * Constructs an instance.
         *
         * @param owner The owner instance.
         * @param store The store.
         * @param points The points.
         */
        public _StoreListener(
                @Nonnull final ScanControls owner,
                @Nonnull final Store store,
                @Nonnull final Collection<UUID> points)
        {
            _owner = owner;
            _store = store;
            _points = points;
            _thread = new ServiceThread(
                this,
                "Datalogger scanner '" + owner._getScannerName() + "' store '"
                + _store.getName() + "' listener");
        }

        /** {@inheritDoc}
         */
        @Override
        public void run()
            throws Exception
        {
            for (;;) {
                final StoreValues storeValues = _store.deliver(0, -1);

                if (storeValues == null) {
                    throw new ServiceNotAvailableException();
                }

                if (!storeValues.isSuccess()) {
                    throw new ServiceNotAvailableException(
                        storeValues.getException().get());
                }

                _owner._onPointValues(storeValues);
            }
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
         * Sets up.
         *
         * @return True on success.
         */
        @CheckReturnValue
        boolean setUp()
        {
            try {
                if (!_store.subscribe(_points)) {
                    return false;
                }
            } catch (final StoreAccessException exception) {
                _LOGGER
                    .warn(
                        ServiceMessages.STORE_ACCESS_FAILED,
                        _store,
                        exception.getMessage());

                return false;
            }

            final Optional<StoreValues[]> subscribedValues = _store
                .getSubscribedValues();

            if (subscribedValues.isPresent()) {
                for (final StoreValues storeValues: subscribedValues.get()) {
                    _owner._onPointValues(storeValues);
                }
            }

            return true;
        }

        /**
         * Starts.
         */
        void start()
        {
            _LOGGER.debug(ServiceMessages.STARTING_THREAD, _thread.getName());
            _thread.start();
        }

        /**
         * Stops.
         *
         * @param joinTimeout The join timeout.
         */
        void stop(final long joinTimeout)
        {
            Require.ignored(_thread.interruptAndJoin(_LOGGER, joinTimeout));
            _store.close();
        }

        private final ScanControls _owner;
        private final Collection<UUID> _points;
        private final Store _store;
        private final ServiceThread _thread;

        /**
         * Store listener builder.
         */
        static final class Builder
        {
            /**
             * Constructs an instance.
             */
            Builder() {}

            /**
             * Adds a point.
             *
             * @param point The point.
             *
             * @return This.
             */
            @Nonnull
            Builder addPoint(@Nonnull final Point point)
            {
                _points.add(point.getUUID().get());

                return this;
            }

            /**
             * Builds a proxy thread.
             *
             * @return The proxy thread.
             */
            @Nonnull
            _StoreListener build()
            {
                return new _StoreListener(_owner, _store, _points);
            }

            /**
             * Sets the owner.
             *
             * @param owner The owner.
             *
             * @return This.
             */
            @Nonnull
            Builder setOwner(@Nonnull final ScanControls owner)
            {
                _owner = owner;

                return this;
            }

            /**
             * Sets the store.
             *
             * @param store The store.
             *
             * @return This.
             */
            @Nonnull
            Builder setStore(@Nonnull final Store store)
            {
                _store = store;

                return this;
            }

            private ScanControls _owner;
            private final Collection<UUID> _points = new LinkedList<>();
            private Store _store;
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
