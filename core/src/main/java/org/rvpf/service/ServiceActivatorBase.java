/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceActivatorBase.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.service;

import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Stats;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Profiler;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.UncaughtExceptionHandler;
import org.rvpf.base.util.Version;

/**
 * Base for service activator.
 *
 * <p>This class establishes the base behavior of RVPF services. It supports the
 * {@link #create}, {@link #start}, {@link #stop} and {@link #destroy} sequence.
 * It also can synthetize a default object name and supports standalone
 * operation.</p>
 */
@ThreadSafe
public abstract class ServiceActivatorBase
    extends StatsHolder
    implements ServiceActivatorBaseMBean, MBeanRegistration
{
    /** {@inheritDoc}
     */
    @Override
    public void create()
        throws Exception
    {
        if (Thread.getDefaultUncaughtExceptionHandler() == null) {
            Thread
                .setDefaultUncaughtExceptionHandler(
                    Logger.getInstance(Logger.class));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void destroy() {}

    /** {@inheritDoc}
     */
    @Override
    public final boolean equals(final Object other)
    {
        if (!(other instanceof ServiceActivatorBase)) {
            return false;
        }

        return getObjectName()
            .equals(((ServiceActivatorBase) other).getObjectName());
    }

    /** {@inheritDoc}
     */
    @Override
    public final ObjectName getObjectName()
    {
        Optional<ObjectName> objectName = objectName();

        if (!objectName.isPresent()) {
            final String objectNameProperty = _objectNameProperty;
            final String nameString = (objectNameProperty != null)? System
                .getProperty(objectNameProperty): null;

            try {
                objectName = Optional
                    .of(
                        (nameString != null)? ObjectName
                            .getInstance(
                                    nameString): makeObjectName(
                                            Optional.empty()));
            } catch (final MalformedObjectNameException exception) {
                throw new RuntimeException(exception);
            }

            setObjectName(objectName.get());
        }

        return objectName.get();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Properties getProperties()
    {
        return _properties;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<String> getProperty(final String key)
    {
        return Optional.ofNullable(_properties.getProperty(key));
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<? extends Stats> getStats()
    {
        return Optional.empty();
    }

    /**
     * Gets the version object.
     *
     * @return The version object.
     */
    @Nonnull
    @CheckReturnValue
    public Version getVersion()
    {
        return new ServiceVersion();
    }

    /** {@inheritDoc}
     */
    @Override
    public final int hashCode()
    {
        return getObjectName().hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isRunning()
    {
        return isStarted();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isStopped()
    {
        return (!isStarted());
    }

    /** {@inheritDoc}
     */
    @Override
    public final void postDeregister() {}

    /** {@inheritDoc}
     */
    @Override
    public final void postRegister(final Boolean done) {}

    /** {@inheritDoc}
     */
    @Override
    public final void preDeregister() {}

    /** {@inheritDoc}
     *
     * <p>This implementation saves the MBeanServer reference for possible use
     * by subclasses. It also supplies a default name if none is given.</p>
     */
    @Override
    public final ObjectName preRegister(
            final MBeanServer server,
            final ObjectName name)
        throws Exception
    {
        _server = server;

        if (name != null) {
            setObjectName(name);
        }

        final ObjectName objectName = getObjectName();

        getThisLogger().debug(ServiceMessages.SERVICE_NAME, objectName);

        return objectName;
    }

    /** {@inheritDoc}
     */
    @Override
    public void restart()
        throws Exception
    {
        restart(Optional.empty());
    }

    /**
     * Restarts the service.
     *
     * @param delay An optional number of milliseconds to wait between the stop
     *              and the restart.
     *
     * @throws Exception As appropriate for the service.
     */
    public final void restart(
            @Nonnull final Optional<ElapsedTime> delay)
        throws Exception
    {
        Require.success(!_restarting);

        _restarting = true;

        final ServiceActivatorListener listener = _listener;

        if (listener != null) {
            listener.restart(delay);
        } else {
            if (!isStopped()) {
                stop();
            }

            if (delay.isPresent()) {
                Thread.sleep(delay.get().toMillis());
            }

            start();
        }

        _restarting = false;
    }

    /**
     * Sets the service listener.
     *
     * @param listener The service listener.
     */
    public final void setListener(
            @Nonnull final ServiceActivatorListener listener)
    {
        _listener = Require.notNull(listener);
    }

    /**
     * Sets the object name property.
     *
     * @param propertyName The optional object name property.
     */
    public final void setObjectNameProperty(
            @Nonnull final Optional<String> propertyName)
    {
        _objectNameProperty = propertyName.orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setProperties(final Properties properties)
    {
        _properties.putAll(properties);
        getThisLogger().debug(ServiceMessages.SERVICE_PROPERTIES, properties);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setProperty(final String key, final String value)
    {
        _properties.setProperty(key, value);
        getThisLogger().debug(ServiceMessages.SERVICE_PROPERTY, key, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
        throws Exception
    {
        if (isStarted()) {
            getThisLogger()
                .warn(ServiceMessages.SERVICE_START_IGNORED, getObjectName());
        } else {
            _stopping = false;
        }
    }

    /**
     * Informs that the start is progressing.
     *
     * @param waitHint An optional dditional time in milliseconds.
     */
    public final void starting(@Nonnull final Optional<ElapsedTime> waitHint)
    {
        final ServiceActivatorListener listener = _listener;

        if (listener != null) {
            listener.starting(waitHint);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        if (isStopped()) {
            getThisLogger()
                .warn(ServiceMessages.SERVICE_STOP_IGNORED, getObjectName());
        } else {
            _stopping = true;
        }
    }

    /**
     * Informs that the stop is progressing.
     *
     * @param waitHint An optional additional elapsed time.
     */
    public final void stopping(@Nonnull final Optional<ElapsedTime> waitHint)
    {
        final ServiceActivatorListener listener = _listener;

        if (listener != null) {
            listener.stopping(waitHint);
        }
    }

    /**
     * Terminates the service.
     *
     * <p>This is called by the service implementation or one of its children
     * when an unexpected condition is detected.</p>
     */
    public void terminate()
    {
        final ServiceActivatorListener listener = _listener;
        final Optional<Thread> mainThread = _clearMainThread();

        if (listener != null) {
            listener.terminate();
        } else if (mainThread.isPresent()) {
            mainThread.get().interrupt();
        } else {
            System.exit(-1);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final String toString()
    {
        return getObjectName().toString();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateStats() {}

    /**
     * Accepts a main argument.
     *
     * @param arg An argument for main args.
     *
     * @return True if accepted.
     */
    @CheckReturnValue
    protected boolean acceptMainArg(@Nonnull final String arg)
    {
        if (arg.startsWith(NAME_ARG_PREFIX)) {
            final String name = arg.substring(NAME_ARG_PREFIX.length()).trim();

            setObjectName(
                makeObjectName(
                    Optional.ofNullable((name.length() > 0)? name: null)));

            return true;
        }

        return false;
    }

    /**
     * Completes the 'create' processing.
     */
    protected void created()
    {
        getThisLogger().debug(ServiceMessages.SERVICE_CREATED, getObjectName());
    }

    /**
     * Completes the 'destroy' processing.
     */
    protected void destroyed()
    {
        getThisLogger()
            .debug(ServiceMessages.SERVICE_DESTROYED, getObjectName());
    }

    /**
     * Allows a subclass to export the JMX agent.
     */
    protected void export() {}

    /**
     * Gets the MBean server instance.
     *
     * @return The MBean server instance.
     */
    @Nonnull
    @CheckReturnValue
    protected final MBeanServer getServer()
    {
        return Require.notNull(_server);
    }

    /**
     * Runs the service.
     *
     * @param args The program arguments.
     */
    protected final void run(@Nonnull final String[] args)
    {
        final boolean standAlone = Logger.class
            .getClassLoader() == getClass().getClassLoader();

        if (standAlone) {
            Thread
                .setDefaultUncaughtExceptionHandler(
                    new UncaughtExceptionHandler());

            System.setProperty("line.separator", "\n");

            Logger.startUp(true);
            Logger.setLogID();
        }

        try {
            try {
                create(args);
                _mainThread.set(Thread.currentThread());
                export();
                start();
            } catch (final Exception exception) {
                exception.printStackTrace();

                return;
            }

            if (isStarted()) {
                Runtime
                    .getRuntime()
                    .addShutdownHook(
                        new Thread(
                            () -> {
                                final Optional<Thread> mainThread =
                                    _clearMainThread();

                                if (mainThread.isPresent()) {
                                    mainThread.get().interrupt();

                                    // Prevents premature exit by the JVM.
                                    try {
                                        mainThread.get().join();
                                    } catch (
                                    final InterruptedException exception) {
                                        throw new RuntimeException(exception);
                                    }
                                }
                            },
                            "Main thread shutdown"));

                if (standAlone) {
                    Profiler.start();
                }

                // Waits until interrupted.
                try {
                    new Semaphore(0).acquire();
                } catch (final InterruptedException exception) {
                    if (standAlone) {
                        Profiler.stop();
                    }

                    if (!isStopped()) {
                        stop();
                    }
                }
            }

            destroy();
            System.gc();
        } catch (final Throwable throwable) {
            if (standAlone) {
                Thread
                    .getDefaultUncaughtExceptionHandler()
                    .uncaughtException(Thread.currentThread(), throwable);
            }
        } finally {
            if (standAlone) {
                Logger.shutDown();
            }
        }
    }

    /**
     * Completes the 'start' processing.
     */
    protected void started() {}

    /**
     * Completes the 'stop' processing.
     */
    protected void stopped()
    {
        final ServiceActivatorListener listener = _listener;

        if (listener != null) {
            listener.stopped();
        }

        if (!_restarting) {
            final Optional<Thread> mainThread = _clearMainThread();

            if (mainThread.isPresent()) {
                mainThread.get().interrupt();
            }
        }
    }

    /**
     * Tears down what has been set up in stand alone mode.
     */
    protected void tearDownStandAlone() {}

    /**
     * Clears the main thread reference.
     *
     * @return The optional reference held before the clear.
     */
    @Nonnull
    final Optional<Thread> _clearMainThread()
    {
        return Optional.ofNullable(_mainThread.getAndSet(null));
    }

    /**
     * Creates the service with program arguments.
     *
     * @param args The program arguments.
     *
     * @throws Exception As appropriate for the service.
     */
    final void create(@Nonnull final String[] args)
        throws Exception
    {
        getVersion().logSystemInfo(getClass().getSimpleName());

        for (final String arg: args) {
            if (!acceptMainArg(arg)) {
                getThisLogger()
                    .warn(ServiceMessages.ARGUMENT_NOT_ACCEPTED, arg);
            }
        }
    }

    /**
     * Asks if this service is stopping.
     *
     * @return True if stopping.
     */
    @CheckReturnValue
    boolean isStopping()
    {
        return _stopping;
    }

    /** Name argument prefix property. */
    public static final String NAME_ARG_PREFIX = "name=";

    /** JMX object name property. */
    public static final String OBJECT_NAME_PROPERTY = "rvpf.object.name";

    private volatile ServiceActivatorListener _listener;
    private AtomicReference<Thread> _mainThread = new AtomicReference<>();
    private volatile String _objectNameProperty = OBJECT_NAME_PROPERTY;
    private final Properties _properties = new Properties();
    private volatile boolean _restarting;
    private volatile MBeanServer _server;
    private volatile boolean _stopping;
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
