/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Alerter.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.service;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.alert.Event;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.Listeners;
import org.rvpf.config.Config;
import org.rvpf.service.som.SOMAlerter;

/**
 * Alerter.
 *
 * <p>Classes implementing this interface take the responsibility of the
 * transport of {@link Alert} objects between related services.</p>
 *
 * <p>Each service must have its own instance of the class.</p>
 */
public interface Alerter
{
    /**
     * Adds a listener.
     *
     * @param listener The listener to be added.
     *
     * @return True unless already added.
     */
    @CheckReturnValue
    boolean addListener(@Nonnull Listener listener);

    /**
     * Gets the active instance.
     *
     * @return The optional active instance.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Alerter> getActiveInstance();

    /**
     * Asks if this alerter is embedded.
     *
     * @return True if it is embedded.
     */
    @CheckReturnValue
    boolean isEmbedded();

    /**
     * Asks if this alerter is running.
     *
     * @return True if it is running.
     */
    @CheckReturnValue
    boolean isRunning();

    /**
     * Gets the shared indicator.
     *
     * @return True if this alerter is shared.
     */
    @CheckReturnValue
    boolean isShared();

    /**
     * Gets the stealth indicator.
     *
     * @return The stealth indicator.
     */
    @CheckReturnValue
    boolean isStealth();

    /**
     * Removes a listener.
     *
     * @param listener The listener to be removed.
     *
     * @return True if it was present.
     */
    @CheckReturnValue
    boolean removeListener(@Nonnull Listener listener);

    /**
     * Sends an alert.
     *
     * @param alert The alert.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    void send(@Nonnull Alert alert)
        throws InterruptedException, ServiceNotAvailableException;

    /**
     * Sets up the alerter.
     *
     * @param config The service configuration.
     * @param alerterProperties The alerter configuration properties.
     * @param owner The alerter owner.
     *
     * @return True value on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull Config config,
            @Nonnull KeyedGroups alerterProperties,
            @Nonnull Thread.UncaughtExceptionHandler owner);

    /**
     * Starts.
     *
     * @throws InterruptedException When interrupted.
     */
    void start()
        throws InterruptedException;

    /**
     * Stops.
     */
    void stop();

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Alert listener.
     */
    interface Listener
    {
        /**
         * Called on alert.
         *
         * @param alert The alert (empty on failure).
         *
         * @return False when the alert does not needs further processing by
         *         this listener.
         */
        @CheckReturnValue
        boolean onAlert(@Nonnull Optional<Alert> alert);
    }


    /**
     * Abstract alerter.
     *
     * <p>Implements basic functionalities and common constants for
     * {@link Alerter} implementations.</p>
     */
    @ThreadSafe
    abstract class Abstract
        implements Alerter
    {
        /**
         * Constructs an instance.
         */
        protected Abstract()
        {
            _sharedContext = sharedContext();
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean addListener(final Listener listener)
        {
            return _listeners.add(listener);
        }

        /** {@inheritDoc}
         */
        @Override
        public final Optional<Alerter> getActiveInstance()
        {
            synchronized (Alerter.class) {
                return _shared? _sharedContext
                    .getSharedAlerter(): Optional.of(this);
            }
        }

        /**
         * Gets the alerter properties.
         *
         * @return The alerter properties.
         */
        @Nonnull
        @CheckReturnValue
        public final KeyedGroups getAlerterProperties()
        {
            return Require.notNull(_alerterProperties);
        }

        /**
         * Gets the config.
         *
         * @return The config.
         */
        @Nonnull
        @CheckReturnValue
        public final Config getConfig()
        {
            return Require.notNull(_config);
        }

        /**
         * Gets the configuration properties.
         *
         * @return The configuration properties.
         */
        @Nonnull
        @CheckReturnValue
        public final KeyedGroups getConfigProperties()
        {
            return getConfig().getProperties();
        }

        /**
         * Gets the owner.
         *
         * @return The owner.
         */
        @Nonnull
        @CheckReturnValue
        public final Object getOwner()
        {
            return Require.notNull(_owner);
        }

        /**
         * Gets the service.
         *
         * @return The optional service.
         */
        @Nonnull
        @CheckReturnValue
        public final Optional<Service> getService()
        {
            return _service;
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        public final Logger getThisLogger()
        {
            return _logger;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isEmbedded()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean isShared()
        {
            return _shared;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isStealth()
        {
            return _stealth;
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean removeListener(final Listener listener)
        {
            return _listeners.remove(listener);
        }

        /** {@inheritDoc}
         */
        @Override
        public final void send(
                final Alert alert)
            throws InterruptedException, ServiceNotAvailableException
        {
            Require.notNull(alert);

            if (!(isStealth() && (alert instanceof Event))) {
                getThisLogger().debug(ServiceMessages.ALERT_PUBLISHING, alert);
                doSend(alert);
                getThisLogger().trace(ServiceMessages.ALERT_PUBLISHED, alert);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean setUp(
                final Config config,
                final KeyedGroups alerterProperties,
                final Thread.UncaughtExceptionHandler owner)
        {
            _stealth = alerterProperties.getBoolean(STEALTH_PROPERTY);

            if (isStealth()) {
                getThisLogger().info(ServiceMessages.ALERTER_STEALTH);
            }

            _shared = !isStealth()
                      && alerterProperties.getBoolean(SHARED_PROPERTY);

            synchronized (Alerter.class) {
                if (!_shared || (_sharedContext.getSharedAlerter() == null)) {
                    _service = Optional
                        .ofNullable(
                            config.hasService()? config.getService(): null);
                    _config = config;
                    _alerterProperties = alerterProperties;
                    _owner = owner;

                    if (!doSetUp()) {
                        return false;
                    }

                    final boolean midnightEnabled = isEmbedded()
                            && !isStealth();

                    if (midnightEnabled) {
                        getThisLogger()
                            .info(ServiceMessages.MIDNIGHT_EVENT_ENABLED);
                    }

                    _midnightEnabled = midnightEnabled;

                    if (_shared) {
                        _sharedContext.setSharedAlerter(Optional.of(this));
                        _sharedContext.getHolders().incrementAndGet();
                        getThisLogger().debug(ServiceMessages.ALERTER_SHARING);
                    }

                    getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);
                } else {
                    _sharedContext.getHolders().incrementAndGet();
                    getThisLogger().debug(ServiceMessages.ALERTER_SHARED);
                }
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public final void start()
            throws InterruptedException
        {
            if (_shared) {
                if (_sharedContext.getHolders().get() != 1) {
                    return;
                }
            }

            doStart();

            if (_midnightEnabled) {
                final Optional<Timer> timer = _getTimer();

                if (timer.isPresent()) {
                    final Optional<ElapsedTime> watchdogPeriod =
                        _alerterProperties
                            .getElapsed(
                                WATCHDOG_PERIOD_PROPERTY,
                                Optional.of(DEFAULT_WATCHDOG_PERIOD),
                                Optional.empty());
                    final Optional<ElapsedTime> watchdogTrigger =
                        _alerterProperties
                            .getElapsed(
                                WATCHDOG_TRIGGER_PROPERTY,
                                Optional.of(DEFAULT_WATCHDOG_TRIGGER),
                                Optional.empty());

                    if (watchdogPeriod.isPresent()
                            && watchdogTrigger.isPresent()) {
                        getThisLogger()
                            .debug(
                                ServiceMessages.WATCHDOG_PERIOD,
                                watchdogPeriod.get());
                        getThisLogger()
                            .debug(
                                ServiceMessages.WATCHDOG_TRIGGER,
                                watchdogTrigger.get());

                        final _Watchdog watchdog = new _Watchdog(
                            watchdogTrigger.get().toMillis());

                        timer
                            .get()
                            .schedule(
                                watchdog,
                                0,
                                watchdogPeriod.get().toMillis());
                        _watchdog = watchdog;
                    }
                }

                _scheduleMidnightEvent(true);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public final void stop()
        {
            if (_shared) {
                if (_sharedContext.getHolders().get() != 1) {
                    return;
                }
            }

            final _MidnightEvent midnightEvent = _midnightEvent.getAndSet(null);

            if (midnightEvent != null) {
                midnightEvent.cancel();
            }

            final _Watchdog watchdog = _watchdog;

            if (watchdog != null) {
                _watchdog = null;
                watchdog.cancel();
            }

            doStop();
        }

        /** {@inheritDoc}
         */
        @Override
        public final void tearDown()
        {
            stop();

            synchronized (Alerter.class) {
                if (!_shared
                        || (_sharedContext.getHolders().decrementAndGet()
                            == 0)) {
                    _listeners.clear();

                    doTearDown();

                    _config = null;
                    _alerterProperties = null;
                    _service = null;
                    _owner = null;

                    if (_shared) {
                        _sharedContext.setSharedAlerter(Optional.empty());
                    }

                    getThisLogger().debug(ServiceMessages.TEAR_DOWN_COMPLETED);
                }
            }
        }

        /**
         * Does send an alert.
         *
         * @param alert The alert.
         *
         * @throws InterruptedException When the service is stopped.
         * @throws ServiceNotAvailableException When the service is not
         *                                      available.
         */
        protected abstract void doSend(
                @Nonnull Alert alert)
            throws InterruptedException, ServiceNotAvailableException;

        /**
         * Does set up the alerter.
         *
         * @return A true value on success.
         */
        @CheckReturnValue
        protected abstract boolean doSetUp();

        /**
         * Does start.
         *
         * @throws InterruptedException When interrupted.
         */
        protected void doStart()
            throws InterruptedException {}

        /**
         * Does stop.
         */
        protected void doStop() {}

        /**
         * Does tear down what has been set up.
         */
        protected abstract void doTearDown();

        /**
         * Notifies listeners.
         *
         * @param alert An alert (empty on failure).
         */
        protected final void notifyListeners(
                @Nonnull final Optional<Alert> alert)
        {
            for (final Listener listener: _listeners) {
                Require.ignored(listener.onAlert(alert));
            }
        }

        /**
         * Returns the shared context.
         *
         * @return The shared context.
         */
        @Nonnull
        @CheckReturnValue
        protected abstract SharedContext sharedContext();

        /**
         * Asks if there is a midnight event.
         *
         * @return True if there is a midnight event.
         */
        boolean _hasMidnightEvent()
        {
            return _midnightEvent.get() != null;
        }

        /**
         * Schedules the midnight event.
         *
         * @param starting True when starting.
         */
        void _scheduleMidnightEvent(final boolean starting)
        {
            final Optional<Timer> timer = _getTimer();

            if (timer.isPresent()) {
                final DateTime midnight = DateTime.now().nextDay();
                final _MidnightEvent midnightEvent = new _MidnightEvent();

                try {
                    timer.get().schedule(midnightEvent, midnight.toTimestamp());
                } catch (final IllegalStateException exception) {
                    getThisLogger()
                        .debug(ServiceMessages.MIDNIGHT_EVENT_CANCELED);

                    return;
                }

                if ((_midnightEvent.getAndSet(midnightEvent) == null)
                        && !starting) {
                    midnightEvent.cancel();
                    _midnightEvent.set(null);
                    getThisLogger()
                        .debug(ServiceMessages.MIDNIGHT_EVENT_CANCELED);
                } else {
                    getThisLogger()
                        .debug(
                            ServiceMessages.MIDNIGHT_EVENT_SCHEDULED,
                            midnight);
                }
            } else {
                getThisLogger().debug(ServiceMessages.MIDNIGHT_EVENT_CANCELED);
            }
        }

        void _sendEvent(@Nonnull final Event event)
        {
            getThisLogger()
                .trace(ServiceMessages.PUBLISHING_EVENT, event.getName());

            try {
                send(event);
            } catch (final InterruptedException exception) {
                throw new InternalError(exception);    // Should not happen.
            } catch (final ServiceNotAvailableException exception) {
                final Optional<Service> service = getService();

                if (service.isPresent()) {
                    getThisLogger()
                        .error(exception, ServiceMessages.RESTART_NEEDED);
                    service.get().restart(false);

                    return;
                }
            }
        }

        private Optional<Timer> _getTimer()
        {
            final Optional<Service> service = _service;

            return service
                .isPresent()? service.get().getTimer(): Optional.empty();
        }

        /** The time in millis before an alerter topic connect retry. */
        public static final String CONNECTION_RETRY_DELAY_PROPERTY =
            "connection.retry.delay";

        /** Default connection retry delay. */
        public static final ElapsedTime DEFAULT_CONNECTION_RETRY_DELAY =
            ElapsedTime
                .fromMillis(15000);

        /** Default watchdog period in milliseconds. */
        public static final ElapsedTime DEFAULT_WATCHDOG_PERIOD = ElapsedTime
            .fromMillis(60000);

        /** Default watchdog periods. */
        public static final ElapsedTime DEFAULT_WATCHDOG_TRIGGER = ElapsedTime
            .fromMillis(300000);

        /** Embeds the alerter topic server within the service. */
        public static final String EMBEDDED_PROPERTY = "embedded";

        /** Tries to share the alerter. */
        public static final String SHARED_PROPERTY = "shared";

        /** Disables response to 'Ping' signals. */
        public static final String STEALTH_PROPERTY = "stealth";

        /** Watchdog period property. */
        public static final String WATCHDOG_PERIOD_PROPERTY = "watchdog.period";

        /** Watchdog trigger property. */
        public static final String WATCHDOG_TRIGGER_PROPERTY =
            "watchdog.trigger";

        private KeyedGroups _alerterProperties;
        private volatile Config _config;
        private final Listeners<Listener> _listeners = new Listeners<>();
        private final Logger _logger = Logger.getInstance(getClass());
        private boolean _midnightEnabled;
        private final AtomicReference<_MidnightEvent> _midnightEvent =
            new AtomicReference<>();
        private Object _owner;
        private volatile Optional<Service> _service;
        private boolean _shared;
        private final SharedContext _sharedContext;
        private boolean _stealth;
        private volatile _Watchdog _watchdog;

        /**
         * Midnight event.
         */
        private class _MidnightEvent
            extends TimerTask
        {
            /**
             * Constructs an instance.
             */
            _MidnightEvent() {}

            /** {@inheritDoc}
             */
            @Override
            public void run()
            {
                if (_hasMidnightEvent()) {
                    _sendEvent(
                        new Event(
                            Service.MIDNIGHT_EVENT,
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty()));

                    _scheduleMidnightEvent(false);
                }
            }
        }


        /**
         * Watchdog.
         */
        private class _Watchdog
            extends TimerTask
        {
            /**
             * Constructs an instance.
             *
             * @param trigger Maximum elapsed milliseconds.
             */
            _Watchdog(final long trigger)
            {
                _trigger = trigger;
                _logID = Logger.currentLogID().orElse(null);
            }

            /** {@inheritDoc}
             */
            @Override
            public void run()
            {
                if (_previousSystemMillis == 0) {
                    Logger.setLogID(Optional.ofNullable(_logID));
                    getThisLogger().debug(ServiceMessages.WATCHDOG_STARTED);
                    _previousSystemMillis = System.currentTimeMillis();

                    return;
                }

                final long systemMillis = System.currentTimeMillis();
                final long elapsedMillis = systemMillis - _previousSystemMillis;

                if (elapsedMillis > (_trigger)) {
                    final Optional<Service> service = getService();

                    getThisLogger()
                        .info(
                            ServiceMessages.WATCHDOG_TRIGGERED,
                            ElapsedTime.fromMillis(elapsedMillis));
                    _sendEvent(
                        new Event(
                            Service.WATCHDOG_EVENT,
                            service.isPresent()? Optional
                                .of(service.get().getServiceName()): Optional
                                        .empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.of(
                                ElapsedTime.fromMillis(elapsedMillis))));
                }

                _previousSystemMillis = systemMillis;
            }

            private String _logID;
            private long _previousSystemMillis;
            private final long _trigger;
        }
    }


    /**
     * Alerter factory.
     */
    @ThreadSafe
    final class Factory
    {
        private Factory() {}

        /**
         * Gets an alerter.
         *
         * @param config The configuration.
         * @param owner The alerter owner.
         *
         * @return The new alerter (null on failure).
         */
        @Nullable
        @CheckReturnValue
        public static Alerter getAnAlerter(
                @Nonnull final Config config,
                @Nonnull final Thread.UncaughtExceptionHandler owner)
        {
            final KeyedGroups alerterProperties = config
                .getPropertiesGroup(SERVICE_ALERTER_PROPERTIES);
            final ClassDef classDef = alerterProperties
                .getClassDef(ALERTER_CLASS_PROPERTY, DEFAULT_ALERTER_CLASS);
            final Alerter alerter = classDef.createInstance(Alerter.class);

            if ((alerter == null)
                    || !alerter.setUp(config, alerterProperties, owner)) {
                return null;
            }

            return alerter.getActiveInstance().orElse(null);
        }

        /** Specifies an alternative to the SOM alerter. */
        public static final String ALERTER_CLASS_PROPERTY = "alerter.class";

        /** Default alerter. */
        public static final ClassDef DEFAULT_ALERTER_CLASS = new ClassDefImpl(
            SOMAlerter.class);

        /** Service alerter properties. */
        public static final String SERVICE_ALERTER_PROPERTIES =
            "service.alerter";
    }


    /**
     * Shared context.
     */
    class SharedContext
    {
        /**
         * Gets the holders.
         *
         * @return The holders.
         */
        @Nonnull
        @CheckReturnValue
        public AtomicInteger getHolders()
        {
            return _holders;
        }

        /**
         * Gets the shared alerter.
         *
         * @return The optional shared alerter.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<Alerter> getSharedAlerter()
        {
            return _sharedAlerter;
        }

        /**
         * Sets the shared alerter.
         *
         * @param sharedAlerter The shared alerter.
         */
        public void setSharedAlerter(
                @Nonnull final Optional<Alerter> sharedAlerter)
        {
            _sharedAlerter = sharedAlerter;
        }

        private final AtomicInteger _holders = new AtomicInteger();
        private volatile Optional<Alerter> _sharedAlerter = Optional.empty();
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
