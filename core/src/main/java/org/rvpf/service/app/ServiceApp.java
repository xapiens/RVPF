/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceApp.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.service.app;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.StatsOwner;
import org.rvpf.base.alert.Alert;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceStats;

/**
 * Service application.
 *
 * <p>The methods of this interface are calls by the service application holder
 * to its client, the service application.</p>
 */
public interface ServiceApp
{
    /**
     * Creates a stats instance.
     *
     * @param statsOwner The stats owner.
     *
     * @return The stats instance.
     */
    @Nonnull
    @CheckReturnValue
    ServiceStats createStats(@Nonnull StatsOwner statsOwner);

    /**
     * Called when a alert has been received.
     *
     * <p>Caution: this is called while synchronized on the service.</p>
     *
     * @param alert The alert.
     *
     * @return False to inhibit further actions on the alert.
     */
    @CheckReturnValue
    boolean onAlert(@Nonnull Alert alert);

    /**
     * Called when a event has been received.
     *
     * <p>Caution: this is called while synchronized on the service.</p>
     *
     * @param event The event.
     *
     * @return False to inhibit further actions on the event.
     */
    @CheckReturnValue
    boolean onEvent(@Nonnull Event event);

    /**
     * Called when some services are not ready.
     *
     * <p>This is called by the pending actions processing when it not known if
     * all registered services are ready. This may be used by a service thread
     * to trigger some state refresh when the pending actions processing is
     * completed; at that time, all the registered services will be ready.</p>
     */
    void onServicesNotReady();

    /**
     * Called when the pending actions processing is completed.
     */
    void onServicesReady();

    /**
     * Called when a signal has been received.
     *
     * <p>Caution: this is called while synchronized on the service.</p>
     *
     * @param signal The signal.
     *
     * @return False to inhibit further actions on the signal.
     */
    @CheckReturnValue
    boolean onSignal(@Nonnull Signal signal);

    /**
     * Sets up the application.
     *
     * <p>Overidden as needed.</p>
     *
     * <p>Called by the framework and by overriding classes at the beginning of
     * the override.</p>
     *
     * @param service The service holding this application.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull Service service);

    /**
     * Starts the application.
     *
     * <p>Overidden as needed by the application.</p>
     *
     * <p>Called by the framework.</p>
     *
     * <p>Should return only when the application is started.</p>
     */
    void start();

    /**
     * Stops the application.
     *
     * <p>Overidden as needed by the application.</p>
     *
     * <p>Called by the framework.</p>
     */
    void stop();

    /**
     * Tears down the application.
     *
     * <p>Overidden as needed by the application.</p>
     *
     * <p>Called by the framework and by overriding classes at the end of the
     * override.</p>
     */
    void tearDown();
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
