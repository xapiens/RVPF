/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueueServer.java 4067 2019-06-08 13:39:16Z SFB $
 */

package org.rvpf.base.som;

import java.rmi.RemoteException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.base.rmi.SessionException;

/**
 * Queue server.
 */
public interface QueueServer
    extends SOMServer
{
    /**
     * Creates a receiver session.
     *
     * @param uuid A UUID used to locate the RMI client instance.
     * @param clientName A descriptive name for the client.
     *
     * @return The receiver session.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException When the session creation fails.
     */
    @Nonnull
    @CheckReturnValue
    ReceiverSession createReceiverSession(
            @Nonnull UUID uuid,
            @Nonnull String clientName)
        throws RemoteException, SessionException;

    /**
     * Creates a sender session.
     *
     * @param uuid A UUID used to locate the RMI client instance.
     * @param clientName A descriptive name for the client.
     *
     * @return The sender session.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException When the session creation fails.
     */
    @Nonnull
    @CheckReturnValue
    SenderSession createSenderSession(
            @Nonnull UUID uuid,
            @Nonnull String clientName)
        throws RemoteException, SessionException;

    /**
     * Gets the queue info.
     *
     * @return The queue info.
     *
     * @throws RemoteException From RMI RunTime.
     */
    @Nonnull
    @CheckReturnValue
    QueueInfo getInfo()
        throws RemoteException;
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
