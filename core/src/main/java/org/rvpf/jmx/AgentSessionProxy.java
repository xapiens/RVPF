/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AgentSessionProxy.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.jmx;

import java.io.IOException;

import java.rmi.RemoteException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIServer;

import org.rvpf.base.logger.Message;
import org.rvpf.base.rmi.CatchedSessionException;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.SessionClientContext;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.rmi.SessionProxy;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.LoginInfo;
import org.rvpf.service.ServiceMessages;

/**
 * Agent session proxy.
 *
 * <p>Wraps the access to a remote
 * {@link javax.management.MBeanServer MBeanServer} thru an
 * {@link org.rvpf.jmx.AgentSession AgentSession}.</p>
 */
public final class AgentSessionProxy
    extends SessionProxy
{
    /**
     * Constructs an instance.
     *
     * @param clientName The client name.
     * @param loginInfo The optional login informations.
     * @param context The session client context.
     * @param listener The optional listener.
     * @param autoconnect The autoconnect indicator.
     * @param objectName The object name.
     */
    AgentSessionProxy(
            @Nonnull final String clientName,
            @Nonnull final Optional<LoginInfo> loginInfo,
            @Nonnull final SessionClientContext context,
            @Nonnull final Optional<Listener> listener,
            final boolean autoconnect,
            @Nonnull final ObjectName objectName)
    {
        super(clientName, loginInfo, context, listener, autoconnect);

        _objectName = objectName;
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Closes the MBean server connection.
     */
    public synchronized void closeConnection()
    {
        if (_connector != null) {
            try {
                _connector.close();
            } catch (final IOException exception) {
                getThisLogger()
                    .debug(
                        ServiceMessages.CONNECTION_CLOSE_FAILED,
                        exception.getMessage());
            }

            _connector = null;
            _agentConnection = null;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void disconnect()
    {
        closeConnection();

        super.disconnect();
    }

    /**
     * Gets the MBean server connection.
     *
     * @return The MBean server connection.
     *
     * @throws SessionException When a connection is not available.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized MBeanServerConnection getConnection()
        throws SessionException
    {
        if (_connector == null) {
            _connector = new RMIConnector((RMIServer) getSession(), null);

            try {
                _connector.connect();
                _agentConnection = _connector.getMBeanServerConnection();
            } catch (final IOException exception) {
                throw new CatchedSessionException(exception);
            }
        }

        return _agentConnection;
    }

    /**
     * Gets a proxy to the MBean.
     *
     * @param objectClass The MBean class.
     * @param <T> The type of the returned value.
     *
     * @return A proxy to the MBean.
     *
     * @throws SessionException When a connection is not available.
     */
    @Nonnull
    @CheckReturnValue
    public <T> T getMBeanProxy(
            @Nonnull final Class<T> objectClass)
        throws SessionException
    {
        return getMBeanProxy(getObjectName(), objectClass, false);
    }

    /**
     * Gets a proxy to a MBean.
     *
     * @param objectName The object name.
     * @param objectClass The MBean class.
     * @param notificationBroadcaster Requests notification emission support.
     * @param <T> The type of the returned value.
     *
     * @return A proxy to the MBean.
     *
     * @throws SessionException When a connection is not available.
     */
    @Nonnull
    @CheckReturnValue
    public <T> T getMBeanProxy(
            @Nonnull final ObjectName objectName,
            @Nonnull final Class<T> objectClass,
            final boolean notificationBroadcaster)
        throws SessionException
    {
        return MBeanServerInvocationHandler
            .newProxyInstance(
                getConnection(),
                objectName,
                objectClass,
                notificationBroadcaster);
    }

    /**
     * Gets an object class name.
     *
     * @param objectName The object name.
     *
     * @return The object class name.
     *
     * @throws SessionException On session exception.
     */
    @Nonnull
    @CheckReturnValue
    public String getObjectClassName(
            @Nonnull final ObjectName objectName)
        throws SessionException
    {
        try {
            return getConnection().getMBeanInfo(objectName).getClassName();
        } catch (final InstanceNotFoundException exception) {
            throw new ServiceClosedException(exception);
        } catch (final Exception exception) {
            throw sessionException(exception);
        }
    }

    /**
     * Gets the object name.
     *
     * @return The object name.
     */
    @Nonnull
    @CheckReturnValue
    public ObjectName getObjectName()
    {
        return Require.notNull(_objectName);
    }

    /**
     * Gets the object names.
     *
     * @return The object names.
     *
     * @throws SessionException When a connection is not available.
     */
    @Nonnull
    @CheckReturnValue
    public ObjectName[] getObjectNames()
        throws SessionException
    {
        final Set<?> names;

        try {
            names = getConnection().queryNames(null, null);
        } catch (final Exception exception) {
            throw sessionException(exception);
        }

        final ObjectName[] objectNames = new ObjectName[names.size()];
        final Iterator<?> iterator = names.iterator();

        for (int i = 0; i < objectNames.length; ++i) {
            objectNames[i] = (ObjectName) iterator.next();
        }

        Arrays.sort(objectNames, Comparator.comparing(ObjectName::toString));

        return objectNames;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Session createSession()
        throws RemoteException, SessionException
    {
        return ((AgentSessionFactory) getFactory())
            .createAgentSession(getContextUUID(), getClientName());
    }

    private MBeanServerConnection _agentConnection;
    private RMIConnector _connector;
    private final ObjectName _objectName;

    /**
     * Builder.
     */
    public static final class Builder
        extends SessionProxy.Builder
    {
        /** {@inheritDoc}
         */
        @Override
        public AgentSessionProxy build()
        {
            if (!setUp()) {
                return null;
            }

            String serverName = getContext()
                .getServerURI()
                .getPath()
                .substring(1);

            if (serverName.isEmpty()
                    || !serverName.startsWith(Agent.REGISTRY_PREFIX)) {
                throw new IllegalArgumentException(
                    Message.format(ServiceMessages.AGENT_URI_PATH));
            }

            serverName = serverName.substring(Agent.REGISTRY_PREFIX.length());

            final ObjectName objectName;

            try {
                objectName = ObjectName.getInstance(serverName);
            } catch (final MalformedObjectNameException exception) {
                throw new RuntimeException(exception);
            }

            return new AgentSessionProxy(
                getClientName(),
                getLoginInfo(),
                getContext(),
                getListener(),
                isAutoconnect(),
                objectName);
        }
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
