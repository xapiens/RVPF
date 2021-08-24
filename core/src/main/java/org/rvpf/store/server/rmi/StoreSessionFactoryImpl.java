/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreSessionFactoryImpl.java 4006 2019-05-18 16:28:13Z SFB $
 */

package org.rvpf.store.server.rmi;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.UUID;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.store.StoreSession;
import org.rvpf.base.store.StoreSessionFactory;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.rmi.ExportedSessionImpl;
import org.rvpf.service.rmi.SessionFactoryImpl;
import org.rvpf.service.rmi.SessionImpl;
import org.rvpf.store.server.StoreServer;

/**
 * Store server RMI session factory implementation.
 */
@ThreadSafe
public final class StoreSessionFactoryImpl
    extends SessionFactoryImpl
    implements StoreSessionFactory
{
    /** {@inheritDoc}
     */
    @Override
    public StoreSession createStoreSession(
            final UUID uuid,
            final String clientName)
        throws SessionException
    {
        return (StoreSession) createSession(uuid, clientName);
    }

    /**
     * Rebinds a UUID.
     *
     * @param oldUUID The old UUID.
     * @param newUUID The new UUID.
     */
    public synchronized void rebind(
            @Nonnull final UUID oldUUID,
            @Nonnull final UUID newUUID)
    {
        for (final Session session: getSessions()) {
            ((StoreSessionImpl) session).rebind(oldUUID, newUUID);
        }
    }

    /**
     * Sets up this remote server session factory.
     *
     * @param config The config.
     * @param serverProperties The store server properties.
     * @param server The server providing access to the store.
     *
     * @return True on success.
     */
    public synchronized boolean setUp(
            final Config config,
            final KeyedGroups serverProperties,
            final StoreServer server)
    {
        _server = server;
        _serverProperties = serverProperties;

        return setUp(config);
    }

    /** {@inheritDoc}
     */
    @Override
    @GuardedBy("this")
    protected void fillRolesMap()
    {
        _addRoleByProperty(StoreSessionImpl.DELETE_ROLE, ROLE_DELETE_PROPERTY);
        _addRoleByProperty(
            StoreSessionImpl.IMPERSONATE_ROLE,
            ROLE_IMPERSONATE_PROPERTY);
        _addRoleByProperty(StoreSessionImpl.INFO_ROLE, ROLE_INFO_PROPERTY);
        _addRoleByProperty(StoreSessionImpl.LISTEN_ROLE, ROLE_LISTEN_PROPERTY);
        _addRoleByProperty(StoreSessionImpl.PURGE_ROLE, ROLE_PURGE_PROPERTY);
        _addRoleByProperty(StoreSessionImpl.QUERY_ROLE, ROLE_QUERY_PROPERTY);
        _addRoleByProperty(StoreSessionImpl.UPDATE_ROLE, ROLE_UPDATE_PROPERTY);
    }

    /** {@inheritDoc}
     */
    @Override
    protected Session newSession(
            final SessionImpl.ConnectionMode connectionMode,
            final Optional<RMIClientSocketFactory> clientSocketFactory,
            final Optional<RMIServerSocketFactory> serverSocketFactory,
            final Object reference)
    {
        final ExportedSessionImpl session = new StoreSessionImpl(
            this,
            connectionMode,
            _server,
            (reference != null)? reference.toString(): "?");

        session
            .open(
                clientSocketFactory.orElse(null),
                serverSocketFactory.orElse(null));

        return session;
    }

    @GuardedBy("this")
    private void _addRoleByProperty(
            final String role,
            final String roleProperty)
    {
        mapRoles(role, _serverProperties.getStrings(roleProperty));
    }

    /** The role needed to submit deletes to the server. */
    public static final String ROLE_DELETE_PROPERTY = "role.delete";

    /** The role needed to impersonate an other user. */
    public static final String ROLE_IMPERSONATE_PROPERTY = "role.impersonate";

    /** The role needed to obtain points informations from server. */
    public static final String ROLE_INFO_PROPERTY = "role.info";

    /** The role needed to listen to notices from the server. */
    public static final String ROLE_LISTEN_PROPERTY = "role.listen";

    /** The role needed to submit purges to the server. */
    public static final String ROLE_PURGE_PROPERTY = "role.purge";

    /** The role needed to submit queries to the server. */
    public static final String ROLE_QUERY_PROPERTY = "role.query";

    /** The role needed to submit updates to the server. */
    public static final String ROLE_UPDATE_PROPERTY = "role.update";

    private volatile StoreServer _server;
    private volatile KeyedGroups _serverProperties;
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
