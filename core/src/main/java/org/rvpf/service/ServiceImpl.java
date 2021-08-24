/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceImpl.java 4081 2019-06-14 14:54:30Z SFB $
 */

package org.rvpf.service;

import java.io.File;

import java.net.URI;
import java.net.URISyntaxException;

import java.rmi.RemoteException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.StatsOwner;
import org.rvpf.base.UUID;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.MemoryLogger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.SignalTarget;
import org.rvpf.config.Config;
import org.rvpf.config.ConfigProperties;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.jmx.Agent;
import org.rvpf.service.Alerter.Listener;
import org.rvpf.service.rmi.ServiceRegistry;
import org.rvpf.service.rmi.SessionFactory;

/**
 * Service implementation.
 *
 * <p>Implements common or default behaviors for service implementations.</p>
 */
public abstract class ServiceImpl
    extends ServiceBaseImpl
    implements Service, Alerter.Listener
{
    /**
     * Creates a new service implementation.
     */
    protected ServiceImpl() {}

    /**
     * Cancels restarters.
     */
    public static void cancelRestarters()
    {
        _Restarter.cancelAll();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean addAlertListener(final Listener listener)
    {
        final Alerter alerter = _alerter;

        if ((alerter == null) || !alerter.isRunning()) {
            return false;
        }

        return alerter.addListener(listener);
    }

    /** {@inheritDoc}
     */
    @Override
    public void disableSuspend()
        throws InterruptedException
    {
        _suspendLock.readLock().lockInterruptibly();
    }

    /** {@inheritDoc}
     */
    @Override
    public void enableSuspend()
    {
        _suspendLock.readLock().unlock();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean exportAgent()
    {
        final boolean result;

        synchronized (_mutex) {
            if (_exported) {
                throw new IllegalStateException(
                    "Already exported: " + getServiceName());
            }

            if (isJMXRegistrationEnabled()) {
                result = Agent
                    .getInstance()
                    .exportService(getServiceActivator());
                _exported = true;
            } else {
                result = true;
            }
        }

        return result;
    }

    /** {@inheritDoc}
     */
    @Override
    public void fail()
    {
        synchronized (_mutex) {
            _stopRequested = true;
            restart(false);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Alerter getAlerter()
    {
        return Require.notNull(_alerter);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Config getConfig()
    {
        return Require.notNull(_config);
    }

    /** {@inheritDoc}
     */
    @Override
    public final File getDataDir()
    {
        return getConfig().getDataDir();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getEntityName()
    {
        return Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public long getJoinTimeout()
    {
        return (_joinTimeout != null)? _joinTimeout.toMillis(): 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<UUID> getOptionalSourceUUID()
    {
        Optional<UUID> sourceUUID = Optional.ofNullable(_sourceUUID.get());

        if (!sourceUUID.isPresent()) {
            sourceUUID = getServiceUUID();
        }

        return sourceUUID;
    }

    /** {@inheritDoc}
     */
    @Override
    public final ServiceActivator getServiceActivator()
    {
        return (ServiceActivator) getServiceActivatorBase();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<UUID> getServiceUUID()
    {
        return Optional.ofNullable(_serviceUUID);
    }

    /** {@inheritDoc}
     */
    @Override
    public UUID getSourceUUID()
    {
        UUID sourceUUID = _sourceUUID.get();

        if (sourceUUID == null) {
            sourceUUID = getServiceUUID().orElse(null);

            if (sourceUUID == null) {
                sourceUUID = UUID.generate();

                if (_sourceUUID.compareAndSet(null, sourceUUID)) {
                    _serviceUUID = sourceUUID;
                }
            }
        }

        return sourceUUID;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isJMXRegistrationEnabled()
    {
        return _jmxRegistrationEnabled;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isRunning()
    {
        switch (_state) {
            case RUNNING:
            case SUSPENDED:
            case RESUMED:
            case STOPPING:
            case ZOMBIE: {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isStarted()
    {       
        switch (_state) {
            case STARTED:
            case RUNNING:
            case SUSPENDED:
            case RESUMED: {
                return true;
            }
            case ZOMBIE: {
                return isThreadStarted();
            }
            default: {
                return false;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isStopped()
    {
        return _state == _State.STOPPED;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isStopping()
    {
        switch (_state) {
            case STOPPING:
            case STOPPED: {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isWait()
    {
        return _wait;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isZombie()
    {
        return _state == _State.ZOMBIE;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void monitorService(
            final Optional<String> name,
            final Optional<UUID> uuid,
            final Optional<String> reference)
    {
        if (_isMonitorEnabled()) {
            _monitor.add(name, uuid, reference);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onAlert(final Optional<Alert> optionalAlert)
    {
        if (!optionalAlert.isPresent()) {
            getThisLogger().debug(ServiceMessages.RECEIVED_NULL_ALERT);
            restart(false);

            return false;
        }

        final Alert alert = optionalAlert.get();

        synchronized (_mutex) {
            if (alert instanceof Event) {
                final Event event = (Event) alert;

                if (isStarted()
                        && Service.MIDNIGHT_EVENT.equalsIgnoreCase(
                            event.getName())) {
                    getThisLogger().info(ServiceMessages.RECEIVED_ALERT, event);
                    logStats(true);
                    MemoryLogger.getInstance().onMidnightEvent();
                } else if (!Service.PONG_EVENT.equalsIgnoreCase(event.getName())
                           || (_isMonitorEnabled() && _monitor.getMonitored(
                                   event).isPresent())) {
                    getThisLogger()
                        .debug(ServiceMessages.RECEIVED_ALERT, event);
                }

                if (_isMonitorEnabled()) {
                    _monitor.onEvent(event);
                }

                if (isRunning()) {
                    Require.ignored(onEvent(event));
                }

                _mutex.notifyAll();

                return false;
            }

            if (alert instanceof Signal) {
                final Signal signal = (Signal) alert;

                if (Service.PING_SIGNAL.equalsIgnoreCase(signal.getName())
                        || Service.SUSPEND_SIGNAL.equalsIgnoreCase(
                            signal.getName())
                        || Service.RESUME_SIGNAL.equalsIgnoreCase(
                            signal.getName())
                        || Service.RESTART_SIGNAL.equalsIgnoreCase(
                            signal.getName())
                        || Service.RESTART_NOW_SIGNAL.equalsIgnoreCase(
                            signal.getName())
                        || Service.STOP_SIGNAL.equalsIgnoreCase(
                            signal.getName())
                        || Service.STOP_NOW_SIGNAL.equalsIgnoreCase(
                            signal.getName())) {
                    final Optional<SignalTarget> signalTarget = SignalTarget
                        .fromString(signal.getInfo());

                    if (signalTarget.isPresent()) {
                        final Optional<UUID> targetUUID = signalTarget
                            .get()
                            .getUUID();

                        if ((targetUUID.isPresent())
                                && (!getSourceUUID().equals(
                                    targetUUID.get()))) {
                            return false;
                        }

                        final Optional<String> targetName = signalTarget
                            .get()
                            .getName();

                        if ((targetName.isPresent())
                                && !getServiceName().equalsIgnoreCase(
                                    targetName.get())) {
                            return false;
                        }
                    }
                }

                getThisLogger().debug(ServiceMessages.RECEIVED_ALERT, signal);

                if (isRunning()) {
                    if (onSignal(signal)) {
                        _doServiceSignalActions(signal);
                    }
                } else {
                    _doServiceSignalActions(signal);
                }

                _mutex.notifyAll();

                return false;
            }
        }

        getThisLogger().debug(ServiceMessages.RECEIVED_ALERT, alert);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String registerServer(
            final SessionFactory server,
            final String serverPath)
    {
        Require.notNull(server);

        if ((serverPath == null) || serverPath.trim().isEmpty()) {
            throw new IllegalArgumentException(
                Message.format(ServiceMessages.SERVER_IDENTIFICATION));
        }

        final URI serverURI;

        try {
            serverURI = new URI(serverPath);
        } catch (final URISyntaxException exception) {
            getThisLogger()
                .error(ServiceMessages.BAD_SERVER_PATH, exception.getMessage());

            return null;
        }

        return ServiceRegistry
            .getInstance()
            .registerServer(server, serverURI, getThisLogger());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean removeAlertListener(final Listener listener)
    {
        final Alerter alerter = _alerter;

        if ((alerter == null) || !alerter.isRunning()) {
            return false;
        }

        return alerter.removeListener(listener);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void restart(final boolean delayed)
    {
        synchronized (_mutex) {
            _restartSignaled = false;
            _restartRequested = true;

            if (_restartEnabled && !_restartInitiated) {
                final Optional<ElapsedTime> delay = delayed? getConfig()
                    .getElapsedValue(
                        RESTART_DELAY_PROPERTY,
                        Optional.of(DEFAULT_RESTART_DELAY),
                        Optional.empty()): Optional.empty();

                _restartInitiated = true;
                new _Restarter(
                    getServiceActivatorBase(),
                    _stopRequested || !_restartAllowed,
                    delay)
                    .start();
            }

            _mutex.notifyAll();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void restoreConfigState()
    {
        getConfig().restoreState(Require.notNull(_configState));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void restoreMonitored()
    {
        if (_isMonitorEnabled()) {
            _monitor.restoreMonitored(_monitoredServices);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void resume()
    {
        _suspendLock.writeLock().unlock();
    }

    /**
     * Runs within the thread.
     *
     * <p>The objective of this method is to streamline the overall processing
     * and exception handling for subclasses. Instead of implementing this
     * method, subclass should implement or override the following methods:</p>
     *
     * <ol>
     *   <li>{@link #doStart} which will be called before the service is
     *     declared 'Running';</li>
     *   <li>{@link #doRun} containing the main processing loop;</li>
     *   <li>{@link #doStop} which will be called when the service is
     *     stopped.</li>
     * </ol>
     */
    @Override
    public final void run()
    {
        boolean success;

        try {
            success = setUp();
        } catch (final Throwable throwable) {
            getThisLogger().error(throwable, ServiceMessages.SET_UP_FAILED);
            success = false;
        }

        if (!success) {
            synchronized (_mutex) {
                if (_isStarting()) {
                    _setState(_State.NONE);
                }
            }

            return;
        }

        getStats()
            .setLogEnabled(
                getConfig().getBooleanValue(STATS_LOG_ENABLED_PROPERTY));

        _addDependencies();

        final Optional<ElapsedTime> delay = getConfig()
            .getElapsedValue(
                STARTUP_DELAY_PROPERTY,
                Optional.empty(),
                Optional.empty());

        getConfig().registerClassLoader();

        if (!isZombie()) {
            _setState(_State.STARTED);
        }

        MemoryLogger
            .getInstance()
            .activate(
                getConfig()
                    .getElapsedValue(
                            MEMORY_LOG_INTERVAL_PROPERTY,
                                    Optional.empty(),
                                    Optional.empty()));

        try {
            if (isZombie()) {
                if (_alerter != null) {
                    _alerter.start();
                }

                sendEvent(Service.ZOMBIE_EVENT, Optional.empty());
            } else {
                if (delay.isPresent()) {
                    getThisLogger().debug(ServiceMessages.STARTUP_DELAY, delay);
                    Thread.sleep(delay.get().toMillis());
                }

                if (_alerter != null) {
                    _alerter.start();
                }

                final boolean restartRequested;

                synchronized (_mutex) {
                    _pinged = false;
                    _doPendingServiceActions();
                    restartRequested = _restartRequested;

                    if (restartRequested) {
                        _mutex.notifyAll();
                    }
                }

                if (!restartRequested) {
                    try {
                        doStart();
                    } catch (final ServiceNotAvailableException exception) {
                        getThisLogger()
                            .error(
                                exception,
                                ServiceMessages.SERVICE_NOT_AVAILABLE);
                        fail();
                    }

                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    if (isStopped()) {
                        return;
                    }

                    if (isStopping()) {
                        doStop();

                        throw new InterruptedException();
                    }

                    sendEvent(Service.STARTED_EVENT, Optional.empty());
                    getThisLogger()
                        .info(
                            ServiceMessages.SERVICE_RUNNING,
                            getServiceName());
                    _setState(_State.RUNNING);
                }
            }

            synchronized (_mutex) {
                try {
                    if (isZombie()) {
                        while (_doPendingServiceActions()) {
                            _mutex.wait();
                        }
                    } else if (isRunning()) {
                        doRun();
                    } else {
                        interrupt();
                    }
                } catch (final ServiceNotAvailableException exception) {
                    if (exception.getCause() != null) {
                        Throwable cause = exception.getCause();

                        if (cause instanceof StoreAccessException) {
                            do {
                                cause = cause.getCause();
                            } while (cause.getCause() != null);
                        }

                        getThisLogger()
                            .error(cause, ServiceMessages.RESTART_NEEDED);
                    } else {
                        getThisLogger().warn(ServiceMessages.RESTART_NEEDED);
                    }

                    restart(true);
                }

                while (!Thread.interrupted()) {
                    _mutex.wait();    // Make sure that we stop on interrupt.
                }

                throw new InterruptedException();
            }
        } catch (final InterruptedException interruptedException) {
            getThisLogger().debug(ServiceMessages.INTERRUPTED);

            try {
                if (isRunning() && !isZombie()) {
                    doStop();

                    try {
                        sendEvent(Service.STOPPED_EVENT, Optional.empty());
                    } catch (final RuntimeException exception) {
                        if (!(exception.getCause()
                                instanceof RemoteException)) {
                            getThisLogger()
                                .warn(
                                    ServiceMessages.ALERT_SEND_FAILED,
                                    Service.STOPPED_EVENT,
                                    exception.getMessage());
                        }
                    }
                }

                if (_alerter != null) {
                    _alerter.stop();
                }
            } catch (final Throwable throwable) {
                getThisLogger()
                    .fatal(throwable, ServiceMessages.HALT_TERMINATED);
            }

            Thread.currentThread().interrupt();
        } catch (final Throwable throwable) {
            uncaughtException(Thread.currentThread(), throwable);
        } finally {
            MemoryLogger.getInstance().deactivate();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void saveConfigState()
    {
        _configState = getConfig().saveState();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void saveMonitored()
    {
        if (_isMonitorEnabled() && (_monitoredServices == null)) {
            _monitoredServices = _monitor.saveMonitored();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void sendAlert(final Alert alert)
    {
        final Alerter alerter = _alerter;

        if ((alerter != null) && alerter.isRunning()) {
            try {
                alerter.send(alert);
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (final ServiceNotAvailableException exception) {
                getThisLogger()
                    .error(exception, ServiceMessages.SERVICE_NOT_AVAILABLE);
                restart(false);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void sendEvent(final String name, final Optional<Object> info)
    {
        sendAlert(
            new Event(
                Require.notNull(name),
                Optional.of(getServiceName()),
                getEntityName(),
                Optional.of(getSourceUUID()),
                info));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void sendSignal(
            final String name,
            final Optional<? extends Object> info)
    {
        sendAlert(
            new Signal(
                Require.notNull(name),
                Optional.of(getServiceName()),
                getEntityName(),
                Optional.of(getSourceUUID()),
                info));
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean setRestartEnabled(final boolean restartEnabled)
    {
        final boolean wasEnabled;

        synchronized (_mutex) {
            if (!_restartInitiated) {
                wasEnabled = _restartEnabled;
                _restartEnabled = restartEnabled && !isStopping();

                if (_restartRequested) {
                    restart(false);
                }
            } else {
                wasEnabled = false;
            }
        }

        return wasEnabled;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setSourceUUID(final UUID sourceUUID)
    {
        _sourceUUID.compareAndSet(null, Require.notNull(sourceUUID));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void starting()
    {
        starting(
            getConfig()
                .getElapsedValue(
                    STARTING_EXTEND_PROPERTY,
                    Optional.of(DEFAULT_STARTING_EXTEND),
                    Optional.empty()));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void stopping()
    {
        stopping(
            getConfig()
                .getElapsedValue(
                    STOPPING_EXTEND_PROPERTY,
                    Optional.of(DEFAULT_STOPPING_EXTEND),
                    Optional.empty()));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void suspend()
        throws InterruptedException
    {
        _suspendLock.writeLock().lockInterruptibly();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean trySuspend(
            final long timeout)
        throws InterruptedException
    {
        return _suspendLock.writeLock().tryLock(timeout, TimeUnit.MILLISECONDS);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void unregisterServer(final String serverName)
    {
        ServiceRegistry.getInstance().unregister(serverName, getThisLogger());
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceStats createStats(final StatsOwner statsOwner)
    {
        return new ServiceStats(statsOwner);
    }

    /**
     * Does pending actions.
     *
     * <p>Caution: this is called while synchronized on mutex.</p>
     *
     * @return False if stopping.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @CheckReturnValue
    protected boolean doPendingActions()
        throws InterruptedException, ServiceNotAvailableException
    {
        synchronized (_mutex) {
            return _doPendingServiceActions();
        }
    }

    /**
     * Does run.
     *
     * @throws Exception Handled by {@link #run}.
     */
    protected void doRun()
        throws Exception
    {
        // Waits until asked to stop.

        synchronized (_mutex) {
            while (doPendingActions()) {
                _mutex.wait();
            }
        }
    }

    /**
     * Does start.
     *
     * @throws Exception Handled by {@link #run}.
     */
    protected abstract void doStart()
        throws Exception;

    /**
     * Does stop.
     *
     * @throws Exception Handled by {@link #run}.
     */
    protected abstract void doStop()
        throws Exception;

    /**
     * Gets the mutex.
     *
     * @return The mutex.
     */
    @Nonnull
    @CheckReturnValue
    protected final Object getMutex()
    {
        return _mutex;
    }

    /**
     * Called when a event has been received.
     *
     * <p>Caution: this is called while synchronized on mutex.</p>
     *
     * @param event The signal.
     *
     * @return False to inhibit further actions on the event.
     */
    @CheckReturnValue
    protected boolean onEvent(@Nonnull final Event event)
    {
        return true;
    }

    /**
     * Called when the service start failed.
     *
     * <p>Caution: this is called while synchronized on mutex.</p>
     *
     * @throws InterruptedException If the service is stopped while waiting.
     */
    protected void onServiceStartFailure()
        throws InterruptedException
    {
        if (_alerter == null) {
            return;    // The start up failed too early.
        }

        if (!getConfig().getBooleanValue(ZOMBIE_ENABLED_PROPERTY)) {
            return;
        }

        if (_isMonitorEnabled()) {
            _monitorEnabled = false;
            _monitor.tearDown();
        }

        _setState(_State.ZOMBIE);
    }

    /**
     * Acts when some services are not ready.
     *
     * <p>Caution: this is called while synchronized on mutex.</p>
     *
     * <p>This is called by the pending actions processing when it not known if
     * all registered services are ready. This may be used by a service thread
     * to trigger some state refresh when the pending actions processing are
     * completed; at that time, all the registered services will be ready.</p>
     */
    protected void onServicesNotReady() {}

    /**
     * Called when a signal has been received.
     *
     * <p>Caution: this is called while synchronized on mutex.</p>
     *
     * @param signal The signal.
     *
     * @return False to inhibit further actions on the signal.
     */
    @CheckReturnValue
    protected boolean onSignal(@Nonnull final Signal signal)
    {
        return true;
    }

    /**
     * Sets the restart signaled indicator.
     *
     * @param restartSignaled The restart signaled indicator.
     */
    protected void setRestartSignaled(final boolean restartSignaled)
    {
        _restartSignaled = restartSignaled;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp()
    {
        if (!super.setUp()) {
            return false;
        }

        // Sets up the config.

        final Config config = ConfigDocumentLoader
            .loadConfig(
                getServiceName(),
                Optional.ofNullable(_configURL),
                Optional.ofNullable(_classLoader));

        if (config == null) {
            return false;
        }

        for (final ServiceContext serviceContext: config.getServiceContexts()) {
            serviceContext.freeze();
        }

        _config = config;

        config.setService(this);
        config.registerClassLoader();
        setLogID(config.getStringValue(SERVICE_LOG_ID_PROPERTY));

        final ConfigProperties serviceProperties = config
            .getServiceProperties();

        serviceProperties.setOverrider(_properties);

        // Sets up the service UUID.

        final Optional<String> uuidString = config
            .getStringValue(SERVICE_UUID_PROPERTY);

        if (uuidString.isPresent() && (uuidString.get().length() > 0)) {
            final UUID serviceUUID = UUID
                .fromString(uuidString.get())
                .orElse(null);

            getThisLogger()
                .info(
                    ServiceMessages.SERVICE_UUID,
                    getServiceName(),
                    serviceUUID);

            _serviceUUID = serviceUUID;
        }

        // Sets up the service context.

        final Optional<ServiceContext> serviceContext = config
            .getServiceContext(getServiceName());

        if (serviceContext.isPresent()) {
            for (final String serviceAlias:
                    serviceContext.get().getServiceAliases()) {
                getThisLogger()
                    .info(ServiceMessages.SERVICE_ALIAS, serviceAlias);
            }
        }

        // Sets up the join timeout.

        _joinTimeout = config
            .getElapsedValue(
                JOIN_TIMEOUT_PROPERTY,
                Optional.of(DEFAULT_JOIN_TIMEOUT),
                Optional.empty())
            .orElse(null);

        if ((_joinTimeout != null)
                && (_joinTimeout.toMillis()
                    != DEFAULT_JOIN_TIMEOUT.toMillis())) {
            getThisLogger().debug(ServiceMessages.JOIN_TIMEOUT, _joinTimeout);
        }

        // Sets up the JMX agent.

        _jmxRegistrationEnabled = Agent.isRegistrationEnabled(config);

        if (isJMXRegistrationEnabled()) {
            if (!Agent.getInstance().setUp(config)) {
                return false;
            }
        }

        // Starts the timer.

        startTimer();

        // Sets up the service registry.

        if (!ServiceRegistry.setUp(config.getProperties())) {
            return false;
        }

        // Sets up the restart indicator.

        _restartAllowed = config.getBooleanValue(RESTART_ALLOWED_PROPERTY);

        if (_restartAllowed) {
            getThisLogger().debug(ServiceMessages.RESTART_ALLOWED);
        }

        // Sets up the alerter.

        _alerter = Alerter.Factory.getAnAlerter(getConfig(), this);

        if (_alerter == null) {
            return false;
        }

        Require.ignored(_alerter.addListener(this));

        if (_alerter.isStealth()) {
            scheduleMidnightLogger();
        }

        // Sets up the monitor.

        _monitorEnabled = (_alerter != null)
                && !config.getBooleanValue(MONITOR_DISABLED_PROPERTY);

        if (_isMonitorEnabled()) {
            if (!_monitor.setUp(this)) {
                return false;
            }
        } else {
            getThisLogger().debug(ServiceMessages.MONITOR_DISABLED);
        }

        return !isStopped();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void tearDown()
    {
        stopTimer();

        final Alerter alerter = _alerter;

        if (alerter != null) {
            _alerter = null;
            alerter.tearDown();
        }

        if (_exported) {
            Agent.getInstance().unexportService(getServiceActivator());
            _exported = false;
        }

        if (_registered) {
            Agent.getInstance().unregisterService(getServiceActivator());
            _registered = false;
        }

        if (_config != null) {
            Agent.getInstance().tearDown(_config);
        }

        if (_isMonitorEnabled()) {
            _monitor.tearDown();
        }

        logStats(false);

        tearDownConfig();

        super.tearDown();
    }

    /**
     * Tears down the config.
     */
    protected void tearDownConfig()
    {
        if (_config != null) {
            _config.tearDown();
            _config = null;
        }
    }

    /**
     * Suspends.
     */
    final void _suspendService()
    {
        if (_suspender == null) {
            return;
        }

        try {
            try {
                suspend();
            } catch (final InterruptedException exception) {
                return;    // Cancels suspend.
            }

            try {
                synchronized (_mutex) {
                    if (_isActive()) {
                        _setState(_State.SUSPENDED);
                        getThisLogger()
                            .info(
                                ServiceMessages.SERVICE_SUSPENDED,
                                getServiceName());
                        _pinged = true;
                    }

                    while (_state == _State.SUSPENDED) {
                        try {
                            _mutex.wait();
                        } catch (final InterruptedException exception) {
                            break;
                        }
                    }

                    if (_state == _State.SUSPENDED) {
                        _setState(_State.RESUMED);
                        getThisLogger()
                            .info(
                                ServiceMessages.SERVICE_RESUMED,
                                getServiceName());
                        _pinged = true;
                    }
                }
            } finally {
                resume();
            }
        } finally {
            _suspender = null;
        }
    }

    /**
     * Sets the properties supplied by the MBean mechanism.
     *
     * @param properties The properties.
     */
    final void putProperties(@Nonnull final Properties properties)
    {
        final ConfigProperties newProperties = new ConfigProperties(
            ServiceMessages.MBEAN_PROPERTY_TYPE);

        newProperties.add(Require.notNull(properties));
        _properties = newProperties;
    }

    /**
     * Sets the class loader.
     *
     * @param classLoader The class loader.
     */
    void setClassLoader(@Nonnull final ServiceClassLoader classLoader)
    {
        _classLoader = Require.notNull(classLoader);
    }

    /**
     * Sets the configuration file specification.
     *
     * @param configURL A file specification string (URL).
     */
    final void setConfigURL(@Nonnull final String configURL)
    {
        _configURL = Require.notNull(configURL);
    }

    /**
     * Starts the service.
     *
     * @param wait True asks to wait until running.
     *
     * @throws InterruptedException If the service is stopped while waiting.
     */
    final void startService(final boolean wait)
        throws InterruptedException
    {
        if (isStarted()) {
            throw new IllegalStateException("Service already started");
        }

        getThisLogger()
            .info(
                ServiceMessages.STARTING_SERVICE,
                getServiceName(),
                getVersion().getImplementationIdent());
        _setState(_State.STARTING);

        _wait = wait;
        startThread();

        synchronized (_mutex) {
            while (_isStarting()) {
                _mutex.wait();
            }
        }

        if (isStarted()) {
            getThisLogger()
                .info(ServiceMessages.SERVICE_STARTED, getServiceName());
            setRestartEnabled(true);

            if (isWait()) {
                synchronized (_mutex) {
                    while (!(isRunning()
                             || isStopping()
                             || _restartRequested)) {
                        _mutex.wait();
                    }
                }
            }
        } else if (isStopped()) {
            getThisLogger()
                .warn(ServiceMessages.SERVICE_CANCELLED, getServiceName());
            tearDown();
        } else {
            onServiceStartFailure();

            if (isZombie()) {
                synchronized (_mutex) {
                    getThisLogger()
                        .warn(
                            ServiceMessages.SERVICE_NOW_ZOMBIE,
                            getServiceName());
                    setRestartEnabled(true);
                }
            } else {
                getThisLogger()
                    .warn(
                        ServiceMessages.SERVICE_START_FAILED,
                        getServiceName());
                tearDown();
                getServiceActivatorBase().terminate();
            }
        }

        if (isStarted() && isJMXRegistrationEnabled()) {
            _registered = Agent
                .getInstance()
                .registerService(getServiceActivator());
        }
    }

    /**
     * Stops the service.
     */
    void stopService()
    {
        final Alerter alerter = _alerter;

        if (alerter != null) {
            Require.ignored(alerter.removeListener(this));
        }

        if (_isStarting()) {
            _setState(_State.STOPPED);
            closeSnoozeAlarm();
        } else if (isStarted()) {
            synchronized (this) {
                if (!isZombie()) {
                    _setState(_State.STOPPING);
                }

                setRestartEnabled(false);

                if (_suspender != null) {
                    _suspender.interrupt();
                }
            }

            closeSnoozeAlarm();

            if (!isZombie()) {
                getThisLogger()
                    .info(ServiceMessages.STOPPING_SERVICE, getServiceName());
            }

            final Optional<String> savedLogID = Logger.currentLogID();
            boolean interrupted = false;

            Logger.setLogID(getLogID());

            try {
                if (!isCurrentThread()) {
                    getThisLogger()
                        .debug(
                            ServiceMessages.STOPPING_THREAD,
                            getThread().getName());
                    interrupt();
                    interrupted = !join();
                    ServiceThread.yieldAll();
                }

                tearDown();
            } finally {
                Logger.restoreLogID(savedLogID);
                _setState(_State.STOPPED);
            }

            if (!isZombie()) {
                getThisLogger()
                    .info(ServiceMessages.SERVICE_STOPPED, getServiceName());
            }

            ServiceThread.yieldAll();

            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void _addDependencies()
    {
        for (final String dependency:
                getConfig().getStringValues(DEPENDENCY_PROPERTY)) {
            final String name;
            final UUID uuid;

            if (UUID.isUUID(dependency)) {
                name = null;
                uuid = UUID.fromString(dependency).get();
            } else {
                name = dependency;
                uuid = null;
            }

            monitorService(
                Optional.ofNullable(name),
                Optional.ofNullable(uuid),
                Optional.empty());
        }
    }

    private boolean _doPendingServiceActions()
        throws InterruptedException
    {
        boolean wasReady = isRunning();

        for (;;) {
            if (_restartSignaled) {
                restart(false);

                return false;
            }

            if (_pinged) {
                final String eventName;

                switch (_state) {
                    case SUSPENDED: {
                        eventName = Service.SUSPENDED_EVENT;

                        break;
                    }
                    case RESUMED: {
                        eventName = Service.RESUMED_EVENT;
                        _setState(_State.RUNNING);

                        break;
                    }
                    case ZOMBIE: {
                        eventName = Service.ZOMBIE_EVENT;

                        break;
                    }
                    default: {
                        eventName = Service.PONG_EVENT;

                        break;
                    }
                }

                sendEvent(eventName, Optional.empty());
                _pinged = false;
            }

            if (_suspender != null) {
                _mutex.wait();

                continue;
            }

            if (_isMonitorEnabled()) {
                if (wasReady && !_monitor.areServicesReady()) {
                    onServicesNotReady();
                    wasReady = false;
                }

                try {
                    if (_monitor.busy()) {
                        _mutex.wait();

                        continue;
                    }
                } catch (final ServiceNotAvailableException exception) {
                    restart(false);

                    return false;
                }
            }

            return true;
        }
    }

    private void _doServiceSignalActions(final Signal signal)
    {
        final String name = signal.getName();

        if (Service.PING_SIGNAL.equalsIgnoreCase(name)) {
            _pinged = true;
        } else if (Service.SUSPEND_SIGNAL.equalsIgnoreCase(name)) {
            if (_isActive() && (_suspender == null)) {
                _suspender = new Thread(
                    () -> _suspendService(),
                    "Service [" + getServiceName() + "] suspender");
                _suspender.start();
            }
        } else if (Service.RESUME_SIGNAL.equalsIgnoreCase(name)) {
            if (_suspender != null) {
                _suspender.interrupt();
            }
        } else if (Service.RESTART_SIGNAL.equalsIgnoreCase(name)) {
            if (!getConfig()
                .getBooleanValue(
                    RESTART_IGNORED_PROPERTY,
                    (_alerter != null) && _alerter.isEmbedded())) {
                setRestartSignaled(true);
            }
        } else if (Service.RESTART_NOW_SIGNAL.equalsIgnoreCase(name)) {
            restart(false);
        } else if (Service.STOP_SIGNAL.equalsIgnoreCase(name)) {
            _stopRequested = true;
            setRestartSignaled(true);
        } else if (Service.STOP_NOW_SIGNAL.equalsIgnoreCase(name)) {
            _stopRequested = true;
            restart(false);
        }
    }

    private boolean _isActive()
    {
        switch (_state) {
            case RUNNING:
            case RESUMED: {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    private boolean _isMonitorEnabled()
    {
        return _monitorEnabled;
    }

    private boolean _isStarting()
    {
        switch (_state) {
            case STARTING: {
                return true;
            }
            case ZOMBIE: {
                return !isThreadStarted();
            }
            default: {
                return false;
            }
        }
    }

    private void _setState(final _State state)
    {
        synchronized (_mutex) {
            _state = state;
            _mutex.notifyAll();
        }
    }

    /** Default join timeout. */
    public static final ElapsedTime DEFAULT_JOIN_TIMEOUT = ElapsedTime
        .fromMillis(60000);

    /** Default restart delay. */
    public static final ElapsedTime DEFAULT_RESTART_DELAY = ElapsedTime
        .fromMillis(60000);

    /** Default starting extend. */
    public static final ElapsedTime DEFAULT_STARTING_EXTEND = ElapsedTime
        .fromMillis(60000);

    /** Default stopping extend. */
    public static final ElapsedTime DEFAULT_STOPPING_EXTEND = ElapsedTime
        .fromMillis(60000);

    /**
     * Contains the UUID of an other service upon which this one is dependent.
     */
    public static final String DEPENDENCY_PROPERTY = "service.dependency";

    /** Join timeout. */
    public static final String JOIN_TIMEOUT_PROPERTY =
        "service.interrupt.timeout";

    /** Memory log interval property. */
    public static final String MEMORY_LOG_INTERVAL_PROPERTY =
        "service.memory.log.interval";

    /** Disables the service monitor. */
    public static final String MONITOR_DISABLED_PROPERTY =
        "service.monitor.disabled";

    /** Allows restarts. */
    public static final String RESTART_ALLOWED_PROPERTY =
        "service.restart.allowed";

    /** Specifies a delay when the service is self-restarting. */
    public static final String RESTART_DELAY_PROPERTY = "service.restart.delay";

    /** Ignores the restart signal. */
    public static final String RESTART_IGNORED_PROPERTY =
        "service.restart.ignored";

    /**
     * Used within a 'service' element to provide an identifying log ID to a
     * service. This may be used to tag log entries.
     */
    public static final String SERVICE_LOG_ID_PROPERTY = "service.log.id";

    /**
     * Used within a 'service' element to provide an identifying UUID to a
     * service. This may be useful in the context of the 'Events' and 'Signals'
     * topics.
     */
    public static final String SERVICE_UUID_PROPERTY = "service.uuid";

    /**
     * The number of millis by which to extend the start time. This is used to
     * prevent a timeout when the service is wrapped.
     */
    public static final String STARTING_EXTEND_PROPERTY =
        "service.starting.extend";

    /** Specifies a delay before making the service available. */
    public static final String STARTUP_DELAY_PROPERTY = "service.startup.delay";

    /** Enables the logging of service stats. */
    public static final String STATS_LOG_ENABLED_PROPERTY =
        "service.stats.log.enabled";

    /**
     * The number of millis by which to extend the stop time. This is used to
     * prevent a timeout when the service is wrapped.
     */
    public static final String STOPPING_EXTEND_PROPERTY =
        "service.stopping.extend";

    /**
     * Enables a 'zombie' state when the service fails to start. This allows
     * the service to respond to 'Signals'.
     */
    public static final String ZOMBIE_ENABLED_PROPERTY =
        "service.zombie.enabled";

    private volatile Alerter _alerter;
    private volatile ServiceClassLoader _classLoader;
    private volatile Config _config;
    private volatile Object _configState;
    private volatile String _configURL;
    private volatile boolean _exported;
    private volatile boolean _jmxRegistrationEnabled;
    private volatile ElapsedTime _joinTimeout;
    private final ServicesMonitor _monitor = new ServicesMonitor();
    private volatile boolean _monitorEnabled;
    private volatile Collection<ServicesMonitor.Monitored> _monitoredServices;
    private final Object _mutex = new Object();
    private volatile boolean _pinged;
    private volatile ConfigProperties _properties;
    private volatile boolean _registered;
    private boolean _restartAllowed;
    private boolean _restartEnabled;
    private boolean _restartInitiated;
    private boolean _restartRequested;
    private volatile boolean _restartSignaled;
    private volatile UUID _serviceUUID;
    private AtomicReference<UUID> _sourceUUID = new AtomicReference<>();
    private volatile _State _state = _State.NONE;
    private boolean _stopRequested;
    private final ReadWriteLock _suspendLock = new ReentrantReadWriteLock(true);
    private volatile Thread _suspender;
    private boolean _wait;

    private enum _State
    {
        NONE,
        STARTING,
        STARTED,
        RUNNING,
        SUSPENDED,
        RESUMED,
        STOPPING,
        STOPPED,
        ZOMBIE
    }

    private static final class _Restarter
        implements Runnable
    {
        /**
         * Creates an instance.
         *
         * @param serviceActivator The service activator.
         * @param stop A true value if the service should only be stopped.
         * @param delay An optional elapsed time to wait between the stop and
         *              the restart.
         */
        _Restarter(
                final ServiceActivatorBase serviceActivator,
                final boolean stop,
                final Optional<ElapsedTime> delay)
        {
            _serviceActivator = serviceActivator;
            _stop = stop;
            _delay = delay;
            _thread = new Thread(
                this,
                "Service [" + _serviceActivator.getObjectName()
                + "] restarter");

            synchronized (_RESTARTERS) {
                _RESTARTERS.add(this);
            }
        }

        /**
         * Runs within the thread.
         */
        @Override
        public void run()
        {
            Logger.setLogID(Optional.ofNullable(_logID));

            try {
                final Logger logger = Logger.getInstance(getClass());

                if (_stop) {
                    logger
                        .info(
                            ServiceMessages.STOPPING_SERVICE,
                            _serviceActivator.getObjectName());
                } else {
                    logger
                        .info(
                            ServiceMessages.RESTARTING_SERVICE,
                            _serviceActivator.getObjectName());
                }

                if (_stop) {
                    _serviceActivator.terminate();
                } else {
                    try {
                        _serviceActivator.restart(_delay);
                    } catch (final Exception exception) {
                        if (!_cancelled) {
                            logger
                                .fatal(
                                    exception,
                                    ServiceMessages.RESTART_FAILED,
                                    _serviceActivator.getObjectName());
                        }
                    }
                }
            } finally {
                synchronized (_RESTARTERS) {
                    _RESTARTERS.remove(this);
                    _RESTARTERS.notifyAll();
                }
            }
        }

        /**
         * Cancels all restarters.
         */
        static void cancelAll()
        {
            synchronized (_RESTARTERS) {
                _cancelled = true;

                for (final _Restarter restarter: _RESTARTERS) {
                    restarter.interrupt();
                }

                while (!_RESTARTERS.isEmpty()) {
                    try {
                        _RESTARTERS.wait();
                    } catch (final InterruptedException exception) {
                        Logger
                            .getInstance(_Restarter.class)
                            .debug(ServiceMessages.INTERRUPTED);
                    }
                }
            }
        }

        /**
         * Interrupts.
         */
        void interrupt()
        {
            _thread.interrupt();
        }

        /**
         * Starts.
         */
        void start()
        {
            _thread.start();
        }

        private static final Set<_Restarter> _RESTARTERS = new HashSet<>();
        private static volatile boolean _cancelled;

        private final Optional<ElapsedTime> _delay;
        private final String _logID = Logger.currentLogID().orElse(null);
        private final ServiceActivatorBase _serviceActivator;
        private final boolean _stop;
        private final Thread _thread;
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
