/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SessionFactory.java 4081 2019-06-14 14:54:30Z SFB $
 */

package org.rvpf.service.rmi;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.security.Realm;
import org.rvpf.config.Config;

/**
 * Session factory.
 */
public interface SessionFactory
{
    /**
     * Adds a session to the set.
     *
     * @param session The session.
     *
     * @return True if the session is new in the set.
     */
    @CheckReturnValue
    boolean addSession(@Nonnull SessionImpl session);

    /**
     * Exports this.
     *
     * @return The stub for this.
     *
     * @throws RemoteException When the export operation fails.
     */
    @Nonnull
    @CheckReturnValue
    Remote export()
        throws RemoteException;

    /**
     * Gets the configuration.
     *
     * @return The configuration.
     */
    @Nonnull
    @CheckReturnValue
    Config getConfig();

    /**
     * Gets the mapped roles for a required role.
     *
     * @param requiredRole The required role.
     *
     * @return The mapped role names.
     */
    @Nonnull
    @CheckReturnValue
    String[] getMappedRoles(String requiredRole);

    /**
     * Gets the realm configuration.
     *
     * @return The optional realm configuration.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Realm> getRealm();

    /**
     * Asks if this has a roles map.
     *
     * @return True if this has a roles map.
     */
    @CheckReturnValue
    boolean hasRolesMap();

    /**
     * Removes a session from the set.
     *
     * @param session The session.
     *
     * @return True if the session was in the set.
     */
    @CheckReturnValue
    boolean removeSession(SessionImpl session);

    /**
     * Unexports this.
     *
     * @throws NoSuchObjectException When the unexport operation fails.
     */
    void unexport()
        throws NoSuchObjectException;
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
