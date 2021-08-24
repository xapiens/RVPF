/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SubscriberSession.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.som;

import java.io.Serializable;
import java.rmi.RemoteException;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.rvpf.base.rmi.SessionException;

/** Subscriber.
 */
public interface SubscriberSession
    extends SOMSession
{
    /** Receives messages.
     *
     * @param limit The maximum number of messages.
     * @param timeout A time limit in millis to wait for the first message
     *                (negative for infinite).
     *
     * @return The messages.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException From session security check.
     */
    @Nonnull
    @CheckReturnValue
    Serializable[] receive(@Nonnegative int limit, long timeout)
        throws RemoteException, SessionException;
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
