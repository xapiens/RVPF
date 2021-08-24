/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServicesMonitor.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.UUID;
import org.rvpf.base.alert.Event;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.SignalTarget;
import org.rvpf.base.util.SnoozeAlarm;
import org.rvpf.config.Config;

/**
 * Services monitor.
 *
 * <p>An instance of this class is created by each service (the owner) to
 * monitor other services on which the owner service is dependent.</p>
 */
final class ServicesMonitor
{
    /**
     * Registers a service to be monitored.
     *
     * <p>Does nothing if the name and UUID are both null.</p>
     *
     * @param name The optional service name.
     * @param uuid The optional UUID identifying the service.
     * @param reference An optional service reference.
     */
    synchronized void add(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<String> reference)
    {
        Monitored monitored = (uuid
            .isPresent())? _monitoredByUUID.get(uuid.get()): null;

        if ((monitored == null) && (name.isPresent())) {
            monitored = _monitoredByName.get(name.get());
        }

        if (monitored != null) {
            if (name.isPresent() && uuid.isPresent()) {
                monitored.updateTarget(name, uuid);
                _monitoredByUUID.put(uuid.get(), monitored);
                _monitoredByName.put(name.get(), monitored);
            }
        } else if (name.isPresent() || uuid.isPresent()) {
            monitored = new Monitored(name, uuid, reference);
            _monitored.add(monitored);

            if (uuid.isPresent()) {
                _monitoredByUUID.put(uuid.get(), monitored);
            }

            if (name.isPresent()) {
                _monitoredByName.put(name.get(), monitored);
            }

            _getOwnerLogger()
                .debug(ServiceMessages.MONITORING_STARTED, monitored);
            _ready = false;
        }
    }

    /**
     * Asks if all registered services are known ready.
     *
     * @return True if ready.
     */
    @CheckReturnValue
    synchronized boolean areServicesReady()
    {
        return _ready;
    }

    /**
     * Does monitoring actions, returning a busy indicator.
     *
     * @return True if monitoring actions are not done.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @CheckReturnValue
    synchronized boolean busy()
        throws InterruptedException, ServiceNotAvailableException
    {
        if (_ready) {
            return false;
        }

        _ready = true;

        for (final Monitored monitored: _monitored) {
            final Optional<Boolean> state = monitored.getState();

            if (!state.isPresent() || !state.get().booleanValue()) {
                _ready = false;
            }
        }

        if (_ready) {
            _cancelAwakener();
        } else {
            if (System.currentTimeMillis() >= (_lastPingTime + _pingInterval)) {
                if ((_pingLimit > 0) && (_pings >= _pingLimit)) {
                    _getOwnerLogger().warn(ServiceMessages.PING_LIMIT_RESTART);

                    throw new ServiceNotAvailableException();
                }

                for (final Monitored monitored: _monitored) {
                    final Optional<Boolean> state = monitored.getState();

                    if (!state.isPresent() || !state.get().booleanValue()) {
                        _service
                            .sendSignal(
                                Service.PING_SIGNAL,
                                Optional.of(monitored.getTarget()));
                    }
                }

                ++_pings;
                _scheduleAwakener();
            }
        }

        return !_ready;
    }

    /**
     * Gets the monitored source of an event.
     *
     * @param event The event.
     *
     * @return The monitored (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    synchronized Optional<Monitored> getMonitored(@Nonnull final Event event)
    {
        final Optional<UUID> sourceUUID = event.getSourceUUID();
        final Optional<String> sourceServiceName = event.getSourceServiceName();
        Monitored monitored = (sourceUUID
            .isPresent())? _monitoredByUUID.get(sourceUUID.get()): null;

        if ((monitored == null) && (sourceServiceName.isPresent())) {
            monitored = _monitoredByName.get(sourceServiceName.get());
        }

        return Optional.ofNullable(monitored);
    }

    /**
     * Acts on Event.
     *
     * @param event The Event.
     */
    synchronized void onEvent(@Nonnull final Event event)
    {
        final Optional<Monitored> monitored = getMonitored(event);

        if (!monitored.isPresent()) {
            return;
        }

        final String eventName = event.getName();

        monitored
            .get()
            .updateTarget(event.getSourceServiceName(), event.getSourceUUID());

        if (Service.PONG_EVENT.equalsIgnoreCase(eventName)
                || Service.STARTED_EVENT.equals(eventName)) {
            if (monitored.get().getState().orElse(null) != Boolean.TRUE) {
                monitored.get().setState(Optional.of(Boolean.TRUE));
                _getOwnerLogger()
                    .debug(ServiceMessages.MONITORED_STARTED, monitored);
            }
        } else if (Service.STOPPED_EVENT.equalsIgnoreCase(eventName)
                   || Service.ZOMBIE_EVENT.equals(eventName)) {
            if (monitored.get().getState().orElse(null) != Boolean.FALSE) {
                monitored.get().setState(Optional.of(Boolean.FALSE));
                _getOwnerLogger()
                    .debug(ServiceMessages.MONITORED_STOPPED, monitored);
                _ready = false;
            }
        }
    }

    /**
     * Restores from saved monitored services.
     *
     * @param saved The saved monitored services.
     */
    synchronized void restoreMonitored(
            @Nonnull final Collection<Monitored> saved)
    {
        _cancelAwakener();
        _monitored.clear();
        _monitoredByName.clear();
        _monitoredByUUID.clear();

        for (final Monitored monitored: saved) {
            monitored.setState(Optional.empty());
            _monitored.add(monitored);

            if (monitored.getName().isPresent()) {
                _monitoredByName.put(monitored.getName().get(), monitored);
            }

            if (monitored.getUUID().isPresent()) {
                _monitoredByUUID.put(monitored.getUUID().get(), monitored);
            }
        }

        _ready = false;
    }

    /**
     * Saves the currently monitored services.
     *
     * @return The currently monitored services.
     */
    @Nonnull
    @CheckReturnValue
    synchronized Collection<Monitored> saveMonitored()
    {
        return new ArrayList<>(_monitored);
    }

    /**
     * Sets up the services monitor.
     *
     * @param service The service.
     *
     * @return True on success.
     */
    @CheckReturnValue
    synchronized boolean setUp(@Nonnull final Service service)
    {
        final Logger logger = Logger.getInstance(getClass());
        final Config config = service.getConfig();
        final ElapsedTime pingInterval = config
            .getElapsedValue(
                PING_INTERVAL_PROPERTY,
                Optional.of(DEFAULT_PING_INTERVAL),
                Optional.of(DEFAULT_PING_INTERVAL))
            .get();
        final int pingLimit = config
            .getIntValue(PING_LIMIT_PROPERTY, DEFAULT_PING_LIMIT);

        _service = service;

        if (!SnoozeAlarm
            .validate(pingInterval, this, ServiceMessages.SERVICES_PING_TEXT)) {
            return false;
        }

        synchronized (_service) {
            _pingInterval = pingInterval.toMillis();
            _pingLimit = pingLimit;
        }

        logger
            .debug(
                ServiceMessages.SERVICES_PING,
                pingInterval,
                String.valueOf(pingLimit));

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    synchronized void tearDown()
    {
        final boolean wasMonitoring = !_monitored.isEmpty();

        _cancelAwakener();
        _monitoredByName.clear();
        _monitoredByUUID.clear();
        _monitored.clear();
        _ready = true;

        if (wasMonitoring) {
            _getOwnerLogger().debug(ServiceMessages.MONITORING_STOPPED);
        }
    }

    private void _cancelAwakener()
    {
        if (_awakener != null) {
            _awakener.cancel();
            _awakener = null;
            _getOwnerLogger()
                .info(_ready
                      ? ServiceMessages.REQUIRED_WAIT_DONE
                      : ServiceMessages.REQUIRED_WAIT_CANCELLED);
        }

        _lastPingTime = 0;
        _pings = 0;
    }

    private Logger _getOwnerLogger()
    {
        return Logger.getInstance(_service.getClass());
    }

    private void _scheduleAwakener()
    {
        final Optional<Timer> timer = _service.getTimer();

        if (timer.isPresent()) {
            if (_awakener == null) {
                final Object owner = _service;

                _awakener = new TimerTask()
                {
                    @SuppressWarnings({"NakedNotify"})
                    @Override
                    public void run()
                    {
                        synchronized (owner) {
                            owner.notifyAll();
                        }
                    }
                };
                timer.get().schedule(_awakener, _pingInterval, _pingInterval);
                _getOwnerLogger().info(ServiceMessages.REQUIRED_WAIT_STARTED);
            }

            _lastPingTime = System.currentTimeMillis();
        }
    }

    /** Default ping interval in millis. */
    public static final ElapsedTime DEFAULT_PING_INTERVAL = ElapsedTime
        .fromMillis(60000);

    /** Default ping limit. */
    public static final int DEFAULT_PING_LIMIT = 60;

    /**
     * Specifies the interval between 'ping' signals while waiting for a 'pong'
     * event.
     */
    public static final String PING_INTERVAL_PROPERTY =
        "service.monitor.ping.interval";

    /**
     * Specifies the maximum number of 'ping' signals to send while waiting for
     * a service before performing a self-restart.
     */
    public static final String PING_LIMIT_PROPERTY =
        "service.monitor.ping.limit";

    private TimerTask _awakener;
    private long _lastPingTime;
    private final Set<Monitored> _monitored = new HashSet<>();
    private final Map<String, Monitored> _monitoredByName = new HashMap<>();
    private final Map<UUID, Monitored> _monitoredByUUID = new HashMap<>();
    private long _pingInterval;
    private int _pingLimit;
    private int _pings;
    private boolean _ready = true;
    private volatile Service _service;

    /**
     * Monitored.
     */
    static final class Monitored
    {
        /**
         * Constructs an instance.
         *
         * @param name The optional service name.
         * @param uuid The optional UUID identfying the service.
         * @param reference An optional service reference.
         */
        Monitored(
                @Nonnull final Optional<String> name,
                @Nonnull final Optional<UUID> uuid,
                @Nonnull final Optional<String> reference)
        {
            _target = new SignalTarget(name, uuid, reference);
        }

        /**
         * Returns a string representation of itself.
         *
         * @return Its ident, name, uuid (String) or null.
         */
        @Override
        public String toString()
        {
            return _target.toString();
        }

        /**
         * Gets the name.
         *
         * @return The optional name.
         */
        @Nonnull
        @CheckReturnValue
        Optional<String> getName()
        {
            return _target.getName();
        }

        /**
         * Gets the service state.
         *
         * @return The optional service state.
         */
        @Nonnull
        @CheckReturnValue
        Optional<Boolean> getState()
        {
            return _state;
        }

        /**
         * Gets the target.
         *
         * @return The target.
         */
        @Nonnull
        @CheckReturnValue
        SignalTarget getTarget()
        {
            return _target;
        }

        /**
         * Gets the UUID.
         *
         * @return The optional UUID.
         */
        @Nonnull
        @CheckReturnValue
        Optional<UUID> getUUID()
        {
            return _target.getUUID();
        }

        /**
         * Sets the service state.
         *
         * @param state The service state.
         */
        void setState(@Nonnull final Optional<Boolean> state)
        {
            _state = state;
        }

        /**
         * Updates the target.
         *
         * @param name The service name.
         * @param uuid The UUID identifying the service.
         */
        void updateTarget(
                @Nonnull final Optional<String> name,
                @Nonnull final Optional<UUID> uuid)
        {
            _target = new SignalTarget(name, uuid, _target.getReference());
        }

        private Optional<Boolean> _state = Optional.empty();
        private SignalTarget _target;
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
