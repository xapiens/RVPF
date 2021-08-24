/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteEngineFactoryImpl.java 4081 2019-06-14 14:54:30Z SFB $
 */

package org.rvpf.processor.engine.executor.remote;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import java.util.Optional;

import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.UUID;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.Session.ConnectionMode;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.config.Config;
import org.rvpf.processor.ProcessorServiceAppImpl;
import org.rvpf.service.rmi.ExportedSessionImpl;
import org.rvpf.service.rmi.SessionFactoryImpl;

/**
 * Remote Engine (session) Factory implementation.
 */
@ThreadSafe
public class RemoteEngineFactoryImpl
    extends SessionFactoryImpl
    implements RemoteEngineFactory
{
    /** {@inheritDoc}
     */
    @Override
    public RemoteEngineSession createEngineSession(
            final UUID uuid,
            final String clientName)
        throws SessionException
    {
        return (RemoteEngineSession) createSession(uuid, clientName);
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized boolean setUp(final Config config)
    {
        if (!super.setUp(config)) {
            return false;
        }

        _executeRoles = getConfig()
            .getPropertiesGroup(ProcessorServiceAppImpl.PROCESSOR_PROPERTIES)
            .getStrings(ROLE_EXECUTE_PROPERTY);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected synchronized void fillRolesMap()
    {
        if (_executeRoles != null) {
            mapRoles(EXECUTE_ROLE, _executeRoles);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected Session newSession(
            final ConnectionMode connectionMode,
            final Optional<RMIClientSocketFactory> clientSocketFactory,
            final Optional<RMIServerSocketFactory> serverSocketFactory,
            final Object reference)
    {
        final ExportedSessionImpl session = new RemoteEngineSessionImpl(
            this,
            connectionMode,
            reference.toString());

        session
            .open(
                clientSocketFactory.orElse(null),
                serverSocketFactory.orElse(null));

        return session;
    }

    /** The execute role. */
    public static final String EXECUTE_ROLE = "Execute";

    /** The role needed to submit updates to the server. */
    public static final String ROLE_EXECUTE_PROPERTY = "server.role.execute";

    private String[] _executeRoles;
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
