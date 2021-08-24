/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AgentSessionImpl.java 4006 2019-05-18 16:28:13Z SFB $
 */

package org.rvpf.jmx;

import java.io.IOException;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.Unreferenced;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.management.MBeanServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnection;
import javax.management.remote.rmi.RMIConnectorServer;

import org.rvpf.base.rmi.SessionException;
import org.rvpf.service.rmi.SessionImpl;

/**
 * Agent session implementation.
 *
 * <p>Provides controlled access to the creation of a
 * {@link javax.management.remote.rmi.RMIConnection RMIConnection} to a remote
 * {@link javax.management.MBeanServer MBeanServer}.</p>
 */
public final class AgentSessionImpl
    extends AgentSessionFactoryImpl.AbstractAgentSession
    implements AgentSession, Unreferenced
{
    /**
     * Constructs an instance.
     *
     * @param clientSocketFactory The optional RMI client socket factory instance.
     * @param serverSocketFactory The optional RMI server socket factory instance.
     *
     * @throws IOException From super.
     */
    AgentSessionImpl(
            @Nonnull final Optional<RMIClientSocketFactory> clientSocketFactory,
            @Nonnull final Optional<RMIServerSocketFactory> serverSocketFactory)
        throws IOException
    {
        super(clientSocketFactory, serverSocketFactory);
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
    {
        super.close();

        final RMIConnectorServer connectorServer = _connectorServer;

        if (connectorServer != null) {
            try {
                connectorServer.stop();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        final SessionImpl session = _session;

        if (session != null) {
            if (!session.isClosed()) {
                session.close();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public ConnectionMode getConnectionMode()
    {
        return _session.getConnectionMode();
    }

    /** {@inheritDoc}
     */
    @Override
    public void login(
            final String identifier,
            final char[] password)
        throws SessionException
    {
        _session.login(identifier, password);
    }

    /** {@inheritDoc}
     */
    @Override
    public void logout()
    {
        _session.logout();
    }

    /** {@inheritDoc}
     */
    @Override
    public RMIConnection newClient(final Object credentials)
        throws IOException
    {
        try {
            _session.securityCheck(CONNECT_ROLE);
        } catch (final SessionException exception) {
            throw new RuntimeException(exception);
        }

        return super.newClient(credentials);
    }

    /** {@inheritDoc}
     */
    @Override
    public void unreferenced()
    {
        close();
    }

    /**
     * Asks if the connector server is active.
     *
     * @return A true value if it is active.
     */
    @CheckReturnValue
    boolean isConnectorServerActive()
    {
        final RMIConnectorServer connectorServer = _connectorServer;

        return (connectorServer != null) && connectorServer.isActive();
    }

    /**
     * Opens this.
     *
     * @param clientName A descriptive name for the client.
     * @param sessionFactory The factory creating this.
     * @param connectionMode The connection mode.
     *
     * @throws IOException On I/O exception.
     */
    void open(
            @Nonnull final String clientName,
            @Nonnull final AgentSessionFactoryImpl sessionFactory,
            @Nonnull final ConnectionMode connectionMode)
        throws IOException
    {
        final MBeanServer mbeanServer = sessionFactory.getServer();

        synchronized (this) {
            final SessionImpl session = new SessionImpl(
                clientName,
                sessionFactory,
                connectionMode)
            {
                @Override
                public void close()
                {
                    super.close();

                    if (isConnectorServerActive()) {
                        AgentSessionImpl.this.close();
                    }
                }

                @Override
                public boolean isClosed()
                {
                    return !isConnectorServerActive();
                }
            };

            session.open();
            _session = session;

            _connectorServer = new RMIConnectorServer(
                new JMXServiceURL(getProtocol(), null, 0),
                null,
                this,
                mbeanServer);
            _connectorServer.start();
        }
    }

    /** Connect role. */
    static final String CONNECT_ROLE = "Connect";

    private volatile RMIConnectorServer _connectorServer;
    private volatile SessionImpl _session;
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
