/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreSessionFactory.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.store;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.rvpf.base.UUID;
import org.rvpf.base.rmi.SessionException;

/** Store Server Session Factory Interface.
 *
 * <p>Allows the creation of store sessions.</p>
 */
public interface StoreSessionFactory
    extends Remote
{
    /** Creates a store session.
     *
     * @param uuid A UUID used to locate the RMI client instance.
     * @param clientName A descriptive name for the client.
     *
     * @return The new store session.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException When the session creation fails.
     */
    @Nonnull
    @CheckReturnValue
    StoreSession createStoreSession(
            @Nonnull UUID uuid,
            @Nonnull String clientName)
        throws RemoteException, SessionException;

    /** The RMI URI for the server binding. */
    String BINDING_PROPERTY = "binding";

    /** Default ProxyStore RMI server name. */
    String DEFAULT_PROXY_STORE_NAME = "ProxyStore";

    /** The name of the store. */
    String NAME_PROPERTY = "name";

    /** The user password for store connections. */
    String PASSWORD_PROPERTY = "password";

    /** The store properties. */
    String STORE_PROPERTIES = "store";

    /** The user identification for store connections. */
    String USER_PROPERTY = "user";
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
