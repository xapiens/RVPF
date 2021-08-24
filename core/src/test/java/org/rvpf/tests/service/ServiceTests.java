/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceTests.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.tests.service;

import java.io.FileNotFoundException;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.UUID;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.Bag;
import org.rvpf.base.util.container.HashCounterBag;
import org.rvpf.base.util.container.ListLinkedHashMap;
import org.rvpf.base.util.container.ListMap;
import org.rvpf.config.Config;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.forwarder.ForwarderServiceActivator;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.Alerter;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceActivatorListener;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.metadata.MetadataServiceImpl;
import org.rvpf.som.SOMContainerServiceActivator;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.Tests;
import org.rvpf.tests.TestsMessages;
import org.rvpf.tests.som.SOMSupport;

/**
 * Service tests.
 */
public abstract class ServiceTests
    extends Tests
    implements Alerter.Listener
{
    /**
     * Sets a system property.
     *
     * @param key The property key, prefix not included.
     * @param value The property value.
     */
    public static void setProperty(
            @Nonnull final String key,
            @Nonnull final String value)
    {
        setSystemProperty(Config.SYSTEM_PROPERTY_PREFIX + key, value);
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
        if (_config == null) {
            loadConfig();

            Require.notNull(_config);
        }

        return _config;
    }

    /**
     * Gets the source UUID.
     *
     * @return The source UUID.
     */
    @Nonnull
    @CheckReturnValue
    public UUID getSourceUUID()
    {
        return FAKE_SERVICE_UUID;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onAlert(final Optional<Alert> optionalAlert)
    {
        synchronized (_expectedAlerts) {
            if (!optionalAlert.isPresent()) {
                getThisLogger().debug(TestsMessages.RECEIVED_NULL_ALERT);

                return false;
            }

            final Alert alert = optionalAlert.get();

            if (_expectedAlertClasses.remove(alert.getClass())) {
                _expectedAlerts.add(alert.getClass(), alert);
                getThisLogger()
                    .debug(TestsMessages.RECEIVED_EXPECTED_ALERT, alert);
                _expectedAlerts.notifyAll();

                return false;
            }

            if (alert instanceof Event) {
                final Event event = (Event) alert;

                if (_expectedEventNames.remove(event.getName())) {
                    _expectedEvents.add(event.getName(), event);
                    getThisLogger()
                        .debug(TestsMessages.RECEIVED_EXPECTED_EVENT, event);
                    _expectedAlerts.notifyAll();
                }

                return false;
            } else if (alert instanceof Signal) {
                final Signal signal = (Signal) alert;

                if (_expectedSignalNames.remove(signal.getName())) {
                    _expectedSignals.add(signal.getName(), signal);
                    getThisLogger()
                        .debug(TestsMessages.RECEIVED_EXPECTED_SIGNAL, signal);
                    _expectedAlerts.notifyAll();
                }

                return false;
            }

            return true;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getClass().getName();
    }

    /**
     * Waits for a Runnable represented by a Future.
     *
     * @param future The Future representing the Runnable.
     *
     * @throws Exception On failure.
     */
    public final void waitForRunnable(
            @Nonnull final Future<?> future)
        throws Exception
    {
        try {
            future.get(getTimeout(), TimeUnit.MILLISECONDS);
        } catch (final ExecutionException exception) {
            final Throwable cause = exception.getCause();

            if (cause != null) {
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }

                if (cause instanceof Error) {
                    throw (Error) cause;
                }
            }

            throw exception;
        }
    }

    /**
     * Clears a system property.
     *
     * @param key The property key, prefix not included.
     */
    protected static void clearProperty(@Nonnull final String key)
    {
        clearSystemProperty(Config.SYSTEM_PROPERTY_PREFIX + key);
    }

    /**
     * Gets the metadata from a service activator.
     *
     * @param serviceActivator The service activator.
     *
     * @return The metadata.
     */
    @Nonnull
    @CheckReturnValue
    protected static Metadata getMetadata(
            @Nonnull final ServiceActivator serviceActivator)
    {
        final MetadataServiceImpl service =
            (MetadataServiceImpl) serviceActivator
                .getService();

        return service.getMetadata();
    }

    /**
     * Gets a system property.
     *
     * @param key The property key.
     *
     * @return The property value or empty.
     */
    @Nonnull
    @CheckReturnValue
    protected static Optional<String> getProperty(@Nonnull final String key)
    {
        return getSystemProperty(Config.SYSTEM_PROPERTY_PREFIX + key);
    }

    /**
     * Restores a system property.
     *
     * @param key The property key, prefix not included.
     */
    protected static void restoreProperty(@Nonnull final String key)
    {
        restoreSystemProperty(Config.SYSTEM_PROPERTY_PREFIX + key);
    }

    /**
     * Starts a service.
     *
     * @param serviceActivator The service activator.
     *
     * @throws Exception On failure.
     */
    protected static void startService(
            @Nonnull final ServiceActivator serviceActivator)
        throws Exception
    {
        Require
            .failure(
                serviceActivator.isStarted(),
                "Service " + serviceActivator.getObjectName() + " started");
        serviceActivator.start(true);
        Require
            .success(
                serviceActivator.isStarted(),
                "Service " + serviceActivator.getObjectName() + " started");
        Require
            .failure(
                serviceActivator.isStopped(),
                "Service " + serviceActivator.getObjectName() + " stopped");
    }

    /**
     * Checks alerts.
     */
    protected final void checkAlerts()
    {
        synchronized (_expectedAlerts) {
            if (_alerter != null) {
                Require
                    .success(_expectedAlertClasses.isEmpty(), "Missing alerts");
                Require.success(_expectedAlerts.isEmpty(), "Missed alerts");
                Require
                    .success(_expectedEventNames.isEmpty(), "Missing events");
                Require.success(_expectedEvents.isEmpty(), "Missed events");
                Require
                    .success(_expectedSignalNames.isEmpty(), "Missing signals");
                Require.success(_expectedSignals.isEmpty(), "Missed signals");
            }
        }
    }

    /**
     * Creates a service instance.
     *
     * @param serviceClass The service class.
     * @param name The name part for the service name (may be empty).
     *
     * @return The service.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    protected final ServiceActivator createService(
            @Nonnull final Class<?> serviceClass,
            @Nonnull final Optional<String> name)
        throws Exception
    {
        final ServiceActivator service = (ServiceActivator) serviceClass
            .newInstance();

        service.setObjectName(service.makeObjectName(name));
        Require
            .equal(
                service.getObjectName().getKeyProperty("name"),
                name.orElse(null));
        service.create();
        service.setListener(new _Listener(service));

        return service;
    }

    /**
     * Expects alert classes.
     *
     * @param alertClasses The classes of the expected alerts.
     */
    @SafeVarargs
    protected final void expectAlerts(
            @Nonnull final Class<? extends Alert>... alertClasses)
    {
        synchronized (_expectedAlerts) {
            for (final Class<? extends Alert> alertClass: alertClasses) {
                _expectedAlertClasses.add(alertClass);
            }
        }
    }

    /**
     * Expects events.
     *
     * @param eventNames The name of the expected events.
     */
    protected final void expectEvents(@Nonnull final String... eventNames)
    {
        synchronized (_expectedAlerts) {
            for (final String eventName: eventNames) {
                _expectedEventNames.add(eventName);
            }
        }
    }

    /**
     * Expects signals.
     *
     * @param signalNames The names of the expected signal.
     */
    protected final void expectSignals(@Nonnull final String... signalNames)
    {
        synchronized (_expectedAlerts) {
            for (final String signalName: signalNames) {
                _expectedSignalNames.add(signalName);
            }
        }
    }

    /**
     * Gets the messaging support.
     *
     * @return The messaging support.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    protected MessagingSupport getMessaging()
        throws Exception
    {
        setUpMessaging();

        return _messaging;
    }

    /**
     * Gets a security context for socket communications.
     *
     * @return The security context.
     *
     * @throws FileNotFoundException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    protected final SecurityContext getSecurityContext()
        throws FileNotFoundException
    {
        if (_securityContext == null) {
            _securityContext = new SecurityContext(getThisLogger());
            Require
                .success(
                    _securityContext
                        .setUp(
                                getConfig().getProperties(),
                                        getConfig()
                                                .getPropertiesGroup(
                                                        CLIENT_SECURITY_PROPERTIES)));
        }

        return _securityContext;
    }

    /**
     * Gets the timeout in millis.
     *
     * @return The timeout in millis.
     */
    @CheckReturnValue
    protected int getTimeout()
    {
        return (_config != null)? _timeout: getTimeout(DEFAULT_TIMEOUT);
    }

    /**
     * Loads the config.
     *
     * <p>Redundant calls will be ignored.</p>
     */
    protected final void loadConfig()
    {
        loadConfig("");
    }

    /**
     * Loads the config.
     *
     * @param serviceClass The class of the configured service.
     *
     * @throws Exception On failure.
     */
    protected final void loadConfig(
            @Nonnull final Class<? extends ServiceActivator> serviceClass)
        throws Exception
    {
        final ServiceActivator service = serviceClass.newInstance();

        loadConfig(service.makeObjectName(Optional.empty()).toString());
    }

    /**
     * Loads the config.
     *
     * <p>Redundant non-specific calls will be ignored.</p>
     *
     * @param serviceName The name of the configured service.
     */
    protected final void loadConfig(@Nonnull final String serviceName)
    {
        if (_config == null) {
            _config = ConfigDocumentLoader
                .loadConfig(serviceName, Optional.empty(), Optional.empty());
            Require.notNull(_config, "Config load failed");
            _config.registerClassLoader();

            _timeout = getTimeout(Optional.empty());

            if (_timeout == 0) {
                final Optional<ElapsedTime> timeout = getConfig()
                    .getElapsedValue(
                        TIMEOUT_PROPERTY,
                        DEFAULT_TIMEOUT,
                        Optional.empty());

                if (timeout.isPresent()) {
                    final long millis = timeout.get().toMillis();

                    Require.success(millis <= Integer.MAX_VALUE);
                    _timeout = (int) millis;
                } else {
                    _timeout = -1;
                }

                getThisLogger()
                    .debug(TestsMessages.TESTS_TIMEOUT, timeout.orElse(null));
            }
        } else {
            Require.success(serviceName.isEmpty(), "Prematurely loaded config");
        }
    }

    /**
     * Sends an alert.
     *
     * @param alert The alert.
     *
     * @throws Exception On failure.
     */
    protected final void sendAlert(@Nonnull final Alert alert)
        throws Exception
    {
        _alerter.send(alert);
    }

    /**
     * Sends an event.
     *
     * @param eventName The event name.
     * @param info Additional informations.
     *
     * @throws Exception On failure.
     */
    protected final void sendEvent(
            @Nonnull final String eventName,
            @Nonnull final Optional<Object> info)
        throws Exception
    {
        sendAlert(
            new Event(
                eventName,
                Optional.of(getClass().getName()),
                Optional.empty(),
                Optional.of(FAKE_SERVICE_UUID),
                info));
    }

    /**
     * Sends a signal.
     *
     * @param signalName The signal name.
     * @param info Additional informations.
     *
     * @throws Exception On failure.
     */
    protected final void sendSignal(
            @Nonnull final String signalName,
            @Nonnull final Optional<Object> info)
        throws Exception
    {
        sendAlert(
            new Signal(
                signalName,
                Optional.of(getClass().getName()),
                Optional.empty(),
                Optional.of(FAKE_SERVICE_UUID),
                info));
    }

    /**
     * Sets up an alerter.
     *
     * @throws Exception On failure.
     */
    protected final void setUpAlerter()
        throws Exception
    {
        if (_alerter == null) {
            _expectedAlertClasses =
                new HashCounterBag<Class<? extends Alert>>();
            _expectedAlerts = new ListLinkedHashMap<Class<? extends Alert>,
                    Alert>();
            _expectedEventNames = new HashCounterBag<String>();
            _expectedEvents = new ListLinkedHashMap<String, Event>();
            _expectedSignalNames = new HashCounterBag<String>();
            _expectedSignals = new ListLinkedHashMap<String, Signal>();

            if ((_somContainer == null)
                    && (!getConfig().getBooleanValue(SOM_DISABLED_PROPERTY))) {
                _somContainer = new SOMContainerServiceActivator();
                _somContainer.setListener(new _ListenerBase());
                _somContainer.create();
                _somContainer.start(true);
            }

            _alerter = Require
                .notNull(Alerter.Factory.getAnAlerter(getConfig(), this));
            Require.success(_alerter.addListener(this));
            _alerter.start();

            if (getConfig().getBooleanValue(ALERTS_QUEUED_PROPERTY)) {
                _alertsForwarder = startService(
                    ForwarderServiceActivator.class,
                    Optional.of(ALERTS_FORWARDER_NAME));
                Require.notNull(_alertsForwarder);
            }
        }
    }

    /**
     * Sets up the messaging support.
     *
     * @throws Exception On failure.
     */
    protected final void setUpMessaging()
        throws Exception
    {
        if (_messaging == null) {
            final ClassDef classDef = getConfig()
                .getClassDef(
                    MESSAGING_SUPPORT_PROPERTY,
                    Optional.of(DEFAULT_MESSAGING_SUPPORT))
                .get();

            _messaging = classDef.createInstance(MessagingSupport.class);
            _messaging.setUp(getConfig(), this);
        }
    }

    /**
     * Starts a service.
     *
     * @param serviceClass The service class.
     * @param name A name part.
     *
     * @return The service.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    protected final ServiceActivator startService(
            @Nonnull final Class<?> serviceClass,
            @Nonnull final Optional<String> name)
        throws Exception
    {
        final ServiceActivator service = createService(serviceClass, name);

        if (_alerter != null) {
            expectEvents(Service.STARTED_EVENT);
        }

        startService(service);

        if (_alerter != null) {
            waitForEvent(Service.STARTED_EVENT);
        }

        return service;
    }

    /**
     * Stops a service.
     *
     * @param service The service.
     *
     * @throws InterruptedException When interrupted.
     */
    protected final void stopService(
            @Nonnull final ServiceActivator service)
        throws InterruptedException
    {
        try {
            Require
                .failure(
                    service.isStopped(),
                    "Service " + service.getObjectName() + " stopped");

            if (_timeout > 0) {
                Require.success(service.getService().trySuspend(_timeout));
            }

            if (_alerter != null) {
                expectEvents(Service.STOPPED_EVENT);
            }

            service.stop();

            if (_alerter != null) {
                waitForEvent(Service.STOPPED_EVENT);
            }

            Require
                .success(
                    service.isStopped(),
                    "Service " + service.getObjectName() + " stopped");
            Require
                .failure(
                    service.isStarted(),
                    "Service " + service.getObjectName() + " started");
        } catch (final AssertionError exception) {
            getThisLogger()
                .error(
                    exception,
                    TestsMessages.SERVICE_STOP_FAILED,
                    service.getObjectName());
        }
    }

    /**
     * Tears down the alerter.
     *
     * @throws Exception On failure.
     */
    protected final void tearDownAlerter()
        throws Exception
    {
        if (_alertsForwarder != null) {
            stopService(_alertsForwarder);
            _alertsForwarder = null;
        }

        tearDownMessaging();

        if (_alerter != null) {
            Require.ignored(_alerter.removeListener(this));
            _alerter.tearDown();
            _alerter = null;
        }

        if (_somContainer != null) {
            _somContainer.stop();
            _somContainer.destroy();
            _somContainer = null;
        }
    }

    /**
     * Tears down the messaging support.
     */
    protected final void tearDownMessaging()
    {
        if (_messaging != null) {
            _messaging.tearDown();
            _messaging = null;
        }
    }

    /**
     * Waits for an expected alert.
     *
     * @param alertClass The class of the expected alert.
     *
     * @return The alert.
     *
     * @throws InterruptedException When the service is stopped.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    @CheckReturnValue
    protected final <T extends Alert> T waitForAlert(
            @Nonnull final Class<T> alertClass)
        throws InterruptedException
    {
        synchronized (_expectedAlerts) {
            final long millis = System.currentTimeMillis();
            final long timeout = getTimeout();

            while (!_expectedAlerts.containsKey(alertClass)) {
                _expectedAlerts.wait(1000);
                Require
                    .success(
                        (timeout <= 0)
                        || ((System.currentTimeMillis() - millis) <= timeout),
                        "Alert received");
            }

            return (T) _expectedAlerts.removeFirst(alertClass).get();
        }
    }

    /**
     * Waits for an expected event.
     *
     * @param eventName The name of the expected event.
     *
     * @return The event.
     *
     * @throws InterruptedException When the service is stopped.
     */
    @Nonnull
    protected final Event waitForEvent(
            @Nonnull final String eventName)
        throws InterruptedException
    {
        synchronized (_expectedAlerts) {
            final long millis = System.currentTimeMillis();
            final long timeout = getTimeout();

            while (!_expectedEvents.containsKey(eventName)) {
                _expectedAlerts.wait(1000);
                Require
                    .success(
                        (timeout <= 0)
                        || ((System.currentTimeMillis() - millis) <= timeout),
                        "Event received");
            }

            return _expectedEvents.removeFirst(eventName).get();
        }
    }

    /**
     * Waits for an expected signal.
     *
     * @param signalName The name of the expected signal.
     *
     * @return The signal.
     *
     * @throws InterruptedException When the service is stopped.
     */
    @Nonnull
    protected final Signal waitForSignal(
            @Nonnull final String signalName)
        throws InterruptedException
    {
        synchronized (_expectedAlerts) {
            final long millis = System.currentTimeMillis();
            final long timeout = getTimeout();

            while (!_expectedSignals.containsKey(signalName)) {
                _expectedAlerts.wait(1000);
                Require
                    .success(
                        (timeout <= 0)
                        || ((System.currentTimeMillis() - millis) <= timeout),
                        "Signal received");
            }

            return _expectedSignals.removeFirst(signalName).get();
        }
    }

    /** Alerts forwarder name. */
    public static final String ALERTS_FORWARDER_NAME = "AlertsQueued";

    /** Alerts queued property. */
    public static final String ALERTS_QUEUED_PROPERTY = "tests.alerts.queued";

    /** Client key store. */
    public static final String CLIENT_KEYSTORE = "tests/config/client.keystore";

    /** Client key store password. */
    public static final String CLIENT_KEYSTORE_PASSWORD = "rvpf-tests";

    /** Client security properties. */
    public static final String CLIENT_SECURITY_PROPERTIES =
        "tests.client.security";

    /** Client trust store. */
    public static final String CLIENT_TRUSTSTORE =
        "tests/config/client.truststore";

    /** Default messaging support. */
    public static final ClassDef DEFAULT_MESSAGING_SUPPORT = new ClassDefImpl(
        SOMSupport.class);

    /** Default store service. */
    public static final ClassDef DEFAULT_STORE_SERVICE = new ClassDefImpl(
        "org.rvpf.store.server.the.TheStoreServiceActivator");

    /** Fake service UUID. */
    public static final UUID FAKE_SERVICE_UUID = UUID
        .fromString("b07f636efa72b54ebc89ddff76b37dfe")
        .get();

    /** Messaging support property. */
    public static final String MESSAGING_SUPPORT_PROPERTY = "tests.messaging";

    /** Null alerter property. */
    public static final String NULL_ALERTER_PROPERTY = "tests.alerter.null";

    /** Null notifier property. */
    public static final String NULL_NOTIFIER_PROPERTY = "tests.notifier.null";

    /** Password property. */
    public static final String PASSWORD_PROPERTY = "tests.password";

    /** Replicated queue properties. */
    public static final String REPLICATED_QUEUE_PROPERTIES =
        "tests.store.replicated.queue";

    /** Replicate queue properties. */
    public static final String REPLICATE_QUEUE_PROPERTIES =
        "tests.store.replicate.queue";

    /** Server security properties. */
    public static final String SERVER_SECURITY_PROPERTIES =
        "tests.server.security";

    /** SOM disabled property. */
    public static final String SOM_DISABLED_PROPERTY = "tests.som.disabled";

    /** Store service property. */
    public static final String STORE_SERVICE_PROPERTY = "tests.store.service";

    /** User property. */
    public static final String USER_PROPERTY = "tests.user";

    private Alerter _alerter;
    private ServiceActivator _alertsForwarder;
    private Config _config;
    private Bag<Class<? extends Alert>> _expectedAlertClasses;
    private ListMap<Class<? extends Alert>, Alert> _expectedAlerts;
    private Bag<String> _expectedEventNames;
    private ListMap<String, Event> _expectedEvents;
    private Bag<String> _expectedSignalNames;
    private ListMap<String, Signal> _expectedSignals;
    private MessagingSupport _messaging;
    private SecurityContext _securityContext;
    private ServiceActivator _somContainer;
    private int _timeout;

    /**
     * Listener.
     */
    private final class _Listener
        extends _ListenerBase
    {
        _Listener(final ServiceActivator service)
        {
            _service = service;
        }

        /** {@inheritDoc}
         */
        @Override
        public void restart(final Optional<ElapsedTime> delay)
        {
            _service.stop();

            try {
                if (delay.isPresent()) {
                    Thread.sleep(delay.get().toMillis());
                }

                _service.start();
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void terminate()
        {
            _service.stop();

            getThisLogger().info(ServiceMessages.SERVICE_TERMINATED, _service);
        }

        private final ServiceActivator _service;
    }


    /**
     * Listener base.
     */
    private class _ListenerBase
        implements ServiceActivatorListener
    {
        /**
         * Constructs an instance.
         */
        _ListenerBase() {}

        /** {@inheritDoc}
         */
        @Override
        public void restart(final Optional<ElapsedTime> delay) {}

        /** {@inheritDoc}
         */
        @Override
        public void starting(final Optional<ElapsedTime> waitHint) {}

        /** {@inheritDoc}
         */
        @Override
        public void stopped() {}

        /** {@inheritDoc}
         */
        @Override
        public void stopping(final Optional<ElapsedTime> waitHint) {}

        /** {@inheritDoc}
         */
        @Override
        public void terminate() {}
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
