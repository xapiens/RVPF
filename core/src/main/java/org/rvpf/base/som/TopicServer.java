/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TopicServer.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.som;

import java.rmi.RemoteException;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.rvpf.base.UUID;
import org.rvpf.base.rmi.SessionException;

/** Topic server.
 */
public interface TopicServer
    extends SOMServer
{
    /** Creates a publisher session.
     *
     * @param uuid A UUID used to locate the RMI client instance.
     * @param clientName A descriptive name for the client.
     *
     * @return The publisher session.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException When the session creation fails.
     */
    @Nonnull
    @CheckReturnValue
    PublisherSession createPublisherSession(
            @Nonnull UUID uuid,
            @Nonnull String clientName)
        throws RemoteException, SessionException;

    /** Creates a subscriber session.
     *
     * @param uuid A UUID used to locate the RMI client instance.
     * @param clientName A descriptive name for the client.
     *
     * @return The subscriber session.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException When the session creation fails.
     */
    @Nonnull
    @CheckReturnValue
    SubscriberSession createSubscriberSession(
            @Nonnull UUID uuid,
            @Nonnull String clientName)
        throws RemoteException, SessionException;

    /** Gets the topic info.
     *
     * @return The topic info.
     *
     * @throws RemoteException From RMI RunTime.
     */
    @Nonnull
    @CheckReturnValue
    TopicInfo getInfo()
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
