/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceActivatorListener.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.service;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;

/**
 * Service activator listener.
 */
public interface ServiceActivatorListener
{
    /**
     * Restarts.
     *
     * @param delay An optional elapsed time to wait between the stop and the
     *              restart.
     */
    void restart(@Nonnull Optional<ElapsedTime> delay);

    /**
     * Informs that the start is progressing.
     *
     * @param waitHint An optional additional elapsed time.
     */
    void starting(@Nonnull Optional<ElapsedTime> waitHint);

    /**
     * Informs that the service has stopped.
     */
    void stopped();

    /**
     * Informs that the stop is progressing.
     *
     * @param waitHint An optional additional elapsed time.
     */
    void stopping(@Nonnull Optional<ElapsedTime> waitHint);

    /**
     * Terminates.
     */
    void terminate();
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
