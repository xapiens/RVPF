/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AgentSessionFactory.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.jmx;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.annotation.Nonnull;
import org.rvpf.base.UUID;
import org.rvpf.base.rmi.SessionException;

/** Agent session factory interface.
 *
 * <p>Provides an interface for the creation of an
 * {@link org.rvpf.jmx.AgentSession AgentSession}.</p>
 */
public interface AgentSessionFactory
    extends Remote
{
    /** Creates a JMX agent session.
     *
     * @param uuid A UUID used to locate the RMI client instance.
     * @param clientName A descriptive name for the client.
     *
     * @return The new agent session.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException From the session creation.
     */
    AgentSession createAgentSession(
            @Nonnull UUID uuid,
            @Nonnull String clientName)
        throws RemoteException, SessionException;

    /** Default Agent RMI server name. */
    String DEFAULT_SERVER_NAME = "AgentServer";
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
