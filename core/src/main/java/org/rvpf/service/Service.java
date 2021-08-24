/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Service.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.service;

import java.io.File;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.UUID;
import org.rvpf.base.alert.Alert;
import org.rvpf.config.Config;
import org.rvpf.service.Alerter.Listener;
import org.rvpf.service.rmi.SessionFactory;

/**
 * Service.
 */
public interface Service
    extends ServiceBase
{
    /**
     * Adds an alert listener.
     *
     * @param listener The listener to be added.
     *
     * @return True if the alerter is running and the listener has been added.
     */
    @CheckReturnValue
    boolean addAlertListener(@Nonnull Alerter.Listener listener);

    /**
     * Disables suspend.
     *
     * @throws InterruptedException When interrupted.
     */
    void disableSuspend()
        throws InterruptedException;

    /**
     * Enables suspend.
     */
    void enableSuspend();

    /**
     * Exports the JMX agent.
     *
     * @return A true value on success.
     */
    @CheckReturnValue
    boolean exportAgent();

    /**
     * Gets the alerter.
     *
     * @return The alerter.
     */
    @Nonnull
    @CheckReturnValue
    Alerter getAlerter();

    /**
     * Gets the service config.
     *
     * @return The config.
     */
    @Nonnull
    @CheckReturnValue
    Config getConfig();

    /**
     * Gets the data directory.
     *
     * @return The data directory.
     */
    @Nonnull
    @CheckReturnValue
    File getDataDir();

    /**
     * Gets the name of the associated entity.
     *
     * @return The optional name.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getEntityName();

    /**
     * Gets the join timeout.
     *
     * @return The join timeout.
     */
    @CheckReturnValue
    long getJoinTimeout();

    /**
     * Gets the optional source UUID.
     *
     * @return The optional source UUID.
     */
    @Nonnull
    @CheckReturnValue
    Optional<UUID> getOptionalSourceUUID();

    /**
     * Gets the service activator owning this.
     *
     * @return The service activator.
     */
    @Nonnull
    @CheckReturnValue
    ServiceActivator getServiceActivator();

    /**
     * Gets the service UUID.
     *
     * @return The optional service UUID.
     */
    @Nonnull
    @CheckReturnValue
    Optional<UUID> getServiceUUID();

    /**
     * Gets the source UUID.
     *
     * @return The source UUID.
     */
    @Nonnull
    @CheckReturnValue
    UUID getSourceUUID();

    /**
     * Asks if the service is running.
     *
     * @return A true value if running.
     */
    @CheckReturnValue
    boolean isRunning();

    /**
     * Asks if the service has been started.
     *
     * @return A true value if started.
     */
    @CheckReturnValue
    boolean isStarted();

    /**
     * Asks if the service has been stopped.
     *
     * @return A true value if stopped.
     */
    @CheckReturnValue
    boolean isStopped();

    /**
     * Asks if the service is stopping.
     *
     * @return A true value if stopping.
     */
    @CheckReturnValue
    boolean isStopping();

    /**
     * Asks if the service waits until running.
     *
     * @return True if the service waits until running.
     */
    @CheckReturnValue
    boolean isWait();

    /**
     * Asks if the service is a zombie.
     *
     * @return A true value if zombie.
     */
    @CheckReturnValue
    boolean isZombie();

    /**
     * Registers a service to be monitored.
     *
     * <p>Does nothing if the service name and UUID are both empty.</p>
     *
     * @param name The optional service name.
     * @param uuid The optional UUID identifying the service.
     * @param reference An optional service reference.
     */
    void monitorService(
            @Nonnull Optional<String> name,
            @Nonnull Optional<UUID> uuid,
            @Nonnull Optional<String> reference);

    /**
     * Registers a RMI server.
     *
     * @param server The server object.
     * @param serverPath The path to the server.
     *
     * @return The server name (null on failure).
     */
    @Nullable
    @CheckReturnValue
    String registerServer(
            @Nonnull SessionFactory server,
            @Nonnull String serverPath);

    /**
     * Removes an alert listener.
     *
     * @param listener The listener to be removed.
     *
     * @return True if it was present.
     */
    @CheckReturnValue
    boolean removeAlertListener(@Nonnull Listener listener);

    /**
     * Restarts the service.
     *
     * @param delayed A true value if start should be delayed.
     */
    void restart(boolean delayed);

    /**
     * Restores the config state.
     */
    void restoreConfigState();

    /**
     * Restores monitored services.
     */
    void restoreMonitored();

    /**
     * Resumes.
     */
    void resume();

    /**
     * Saves the config state.
     */
    void saveConfigState();

    /**
     * Saves monitored services.
     */
    void saveMonitored();

    /**
     * Sends an alert.
     *
     * @param alert The alert.
     */
    void sendAlert(@Nonnull Alert alert);

    /**
     * Sends an event.
     *
     * @param name The event's name.
     * @param info Additional optional informations.
     */
    void sendEvent(@Nonnull String name, @Nonnull Optional<Object> info);

    /**
     * Sends a signal.
     *
     * @param name The signal's name.
     * @param info Additional optional informations.
     */
    void sendSignal(
            @Nonnull String name,
            @Nonnull Optional<? extends Object> info);

    /**
     * Sets the restart enabled indicator.
     *
     * @param restartEnabled The restart enabled indicator.
     *
     * @return The previous value of the indicator.
     */
    @CheckReturnValue
    boolean setRestartEnabled(boolean restartEnabled);

    /**
     * Sets the source UUID.
     *
     * @param sourceUUID The source UUID.
     */
    void setSourceUUID(@Nonnull UUID sourceUUID);

    /**
     * Informs that the start is progressing.
     */
    void starting();

    /**
     * Informs that the stop is progressing.
     */
    void stopping();

    /**
     * Suspends.
     *
     * @throws InterruptedException When interrupted.
     */
    void suspend()
        throws InterruptedException;

    /**
     * Tries to suspend.
     *
     * @param timeout The timeout in milliseconds.
     *
     * @return A true value on success.
     *
     * @throws InterruptedException When interrupted.
     */
    @CheckReturnValue
    boolean trySuspend(long timeout)
        throws InterruptedException;

    /**
     * Unregisters a RMI server.
     *
     * @param serverName The name of the server.
     */
    void unregisterServer(@Nonnull String serverName);

    /** The midnight event name. */
    String MIDNIGHT_EVENT = "Midnight";

    /** The ping signal name. */
    String PING_SIGNAL = "Ping";

    /** The pong event name. */
    String PONG_EVENT = "Pong";

    /** The restart now signal name. */
    String RESTART_NOW_SIGNAL = "RestartNow";

    /** The restart signal name. */
    String RESTART_SIGNAL = "Restart";

    /** The service resumed event name. */
    String RESUMED_EVENT = "ServiceResumed";

    /** The resume signal name. */
    String RESUME_SIGNAL = "Resume";

    /** The service started event name. */
    String STARTED_EVENT = "ServiceStarted";

    /** The stopped event name. */
    String STOPPED_EVENT = "ServiceStopped";

    /** The stop now signal name. */
    String STOP_NOW_SIGNAL = "StopNow";

    /** The stop signal name. */
    String STOP_SIGNAL = "Stop";

    /** The service suspended event name. */
    String SUSPENDED_EVENT = "ServiceSuspended";

    /** The suspend signal name. */
    String SUSPEND_SIGNAL = "Suspend";

    /** The watchdog event name. */
    String WATCHDOG_EVENT = "Watchdog";

    /** The service is zombie event name. */
    String ZOMBIE_EVENT = "ServiceIsZombie";
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
