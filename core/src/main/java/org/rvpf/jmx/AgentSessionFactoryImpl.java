/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AgentSessionFactoryImpl.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.jmx;

import java.io.IOException;

import java.lang.management.ManagementFactory;

import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.rmi.RMIJRMPServerImpl;

import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.StatsHolder;
import org.rvpf.service.rmi.SessionFactoryImpl;
import org.rvpf.service.rmi.SessionImpl;
import org.rvpf.service.rmi.SessionSecurityContext;

/**
 * Agent session factory implementation.
 *
 * <p>Creates {@link org.rvpf.jmx.AgentSession AgentSession} instances.</p>
 *
 * <p>Binding an instance of this class to a RMI registry allows remote access
 * to a {@link javax.management.MBeanServer MBeanServer}. This access is
 * controlled by the security properties of the configuration.</p>
 */
@ThreadSafe
final class AgentSessionFactoryImpl
    extends SessionFactoryImpl
    implements AgentSessionFactory
{
    /**
     * Constructs an instance.
     *
     * @param securityContext The security context.
     */
    AgentSessionFactoryImpl(
            @Nonnull final SessionSecurityContext securityContext)
    {
        super(Optional.of(securityContext));
    }

    /** {@inheritDoc}
     */
    @Override
    public AgentSession createAgentSession(
            final UUID uuid,
            final String clientName)
        throws SessionException
    {
        return (AgentSession) createSession(uuid, clientName);
    }

    /**
     * Sets up this agent session factory.
     *
     * @param config The config.
     * @param platform True to use the platform server.
     *
     * @return True on success.
     */
    public boolean setUp(@Nonnull final Config config, final boolean platform)
    {
        if (!setUp(config)) {
            return false;
        }

        synchronized (this) {
            _platform = platform;

            if (_platform) {
                _server = ManagementFactory.getPlatformMBeanServer();
            } else {
                _server = MBeanServerFactory
                    .createMBeanServer(StatsHolder.getDefaultDomain());
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        synchronized (this) {
            if (_server != null) {
                if (!_platform) {
                    MBeanServerFactory.releaseMBeanServer(_server);
                }

                _server = null;
            }
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    @GuardedBy("this")
    protected void fillRolesMap()
    {
        final KeyedGroups jmxProperties = getConfig()
            .getPropertiesGroup(Agent.JMX_PROPERTIES);

        if (jmxProperties != null) {
            mapRoles(
                AgentSessionImpl.CONNECT_ROLE,
                jmxProperties.getStrings(Agent.ROLE_CONNECT_PROPERTY));
        }
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
        final AgentSessionImpl session;

        try {
            session = new AgentSessionImpl(
                clientSocketFactory,
                serverSocketFactory);

            session.open(reference.toString(), this, connectionMode);
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return session;
    }

    /**
     * Gets the server.
     *
     * @return The server.
     */
    @Nonnull
    @CheckReturnValue
    synchronized MBeanServer getServer()
    {
        return Require.notNull(_server);
    }

    private boolean _platform;
    private MBeanServer _server;

    /**
     * Abstract agent session.
     */
    abstract static class AbstractAgentSession
        extends RMIJRMPServerImpl
    {
        /**
         * Constructs an instance.
         *
         * @param clientSocketFactory The optional RMI client Socket Factory instance.
         * @param serverSocketFactory The optional RMI server Socket Factory instance.
         *
         * @throws IOException From super.
         */
        AbstractAgentSession(
                @Nonnull final Optional<RMIClientSocketFactory> clientSocketFactory,
                @Nonnull final Optional<RMIServerSocketFactory> serverSocketFactory)
            throws IOException
        {
            super(
                0,
                clientSocketFactory.orElse(null),
                serverSocketFactory.orElse(null),
                null);
        }

        /** {@inheritDoc}
         *
         * <p>Hides the IOException.</p>
         */
        @Override
        public void close()
        {
            if (_closed.compareAndSet(false, true)) {
                try {
                    super.close();
                } catch (final IOException exception) {
                    Logger
                        .getInstance(getClass())
                        .debug(
                            ServiceMessages.CLOSE_EXCEPTION,
                            exception.getMessage());
                }
            }
        }

        private final AtomicBoolean _closed = new AtomicBoolean();
    }
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
