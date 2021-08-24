/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceBase.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.service;

import java.util.Optional;
import java.util.Timer;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;

/**
 * Service base.
 */
public interface ServiceBase
    extends Thread.UncaughtExceptionHandler
{
    /**
     * Adds supplementary service stats.
     *
     * @param serviceStats The service stats.
     */
    void addStats(@Nonnull ServiceStats serviceStats);

    /**
     * Fails the service.
     *
     * <p>This is called by the service implementation or one of its children
     * when an unexpected condition is detected. It may be overridden.</p>
     */
    void fail();

    /**
     * Gets the Service name.
     *
     * @return The Service name.
     */
    @Nonnull
    @CheckReturnValue
    String getServiceName();

    /**
     * Gets the service stats.
     *
     * @return The first service stats.
     */
    @Nonnull
    @CheckReturnValue
    ServiceStats getStats();

    /**
     * Gets the service timer.
     *
     * @return The optional service timer.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Timer> getTimer();

    /**
     * Asks if the JMX registration is enabled.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    boolean isJMXRegistrationEnabled();

    /**
     * Snoozes for the specified time.
     *
     * <p>Caution: calling this method while holding a lock object other than
     * this instance may cause a stall.</p>
     *
     * @param snoozeTime The snooze time.
     *
     * @throws InterruptedException When interrupted.
     */
    void snooze(@Nonnull ElapsedTime snoozeTime)
        throws InterruptedException;

    /**
     * Informs that the start is progressing.
     *
     * @param waitHint An optional additional time in milliseconds.
     */
    void starting(@Nonnull Optional<ElapsedTime> waitHint);

    /**
     * Informs that the stop is progressing.
     *
     * @param waitHint Additional time in milliseconds.
     */
    void stopping(@Nonnull Optional<ElapsedTime> waitHint);

    /**
     * Wakes up all snoozers.
     */
    void wakeUp();
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
