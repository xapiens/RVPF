/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMServer.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.som;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.rvpf.base.ElapsedTime;

/** SOM server.
 */
public interface SOMServer
    extends Remote
{
    /** Gets the keep-alive value.
     *
     * @return The keep-alive value.
     *
     * @throws RemoteException From RMI RunTime.
     */
    @Nonnull
    @CheckReturnValue
    ElapsedTime getKeepAlive()
        throws RemoteException;

    /** Gets the name of this.
     *
     * @return The name of this.
     *
     * @throws RemoteException From RMI RunTime.
     */
    @Nonnull
    @CheckReturnValue
    String getName()
        throws RemoteException;

    /** The server RMI registry binding property. */
    String BINDING_PROPERTY = "binding";

    /** The default queue binding prefix. */
    String DEFAULT_QUEUE_BINDING_PREFIX = "queue/";

    /** The default topic binding prefix. */
    String DEFAULT_TOPIC_BINDING_PREFIX = "topic/";

    /** The server name property. */
    String NAME_PROPERTY = "name";
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
