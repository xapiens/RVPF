/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMServerImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.som;

import java.net.URI;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.som.SOMServer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ServiceRegistry;
import org.rvpf.service.rmi.SessionFactoryImpl;
import org.rvpf.service.rmi.SessionImpl;
import org.rvpf.service.rmi.SessionSecurityContext;

/**
 * SOM server implementation.
 */
public abstract class SOMServerImpl
    extends SessionFactoryImpl
    implements SOMServer
{
    /**
     * Constructs an instance.
     *
     * @param securityContext The optional security context.
     */
    protected SOMServerImpl(
            @Nonnull final Optional<SessionSecurityContext> securityContext)
    {
        super(securityContext);
    }

    /** {@inheritDoc}
     */
    @Override
    public ElapsedTime getKeepAlive()
    {
        return _keepAlive;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getName()
    {
        return Require.notNull(_name);
    }

    /**
     * Gets the URI.
     *
     * @return The URI.
     */
    @Nonnull
    @CheckReturnValue
    public URI getURI()
    {
        return Require.notNull(_registryEntry.getURI());
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean removeSession(final SessionImpl session)
    {
        if (!super.removeSession(session)) {
            return false;
        }

        synchronized (_mutex) {
            _mutex.notifyAll();
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean setUp(final Config config)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets up this server.
     *
     * @param config The configuration.
     * @param somProperties The queue properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public abstract boolean setUp(
            @Nonnull final Config config,
            @Nonnull final KeyedGroups somProperties);

    /**
     * Stops this SOM service.
     *
     * @param timeout A time limit in millis to wait for clients (negative for
     *                infinite).
     *
     * @return True if the service has been stopped.
     */
    public final boolean stop(final int timeout)
    {
        synchronized (_mutex) {
            final long startMillis = (timeout > 0)? System
                .currentTimeMillis(): 0;

            while (getSessionCount() > 0) {
                try {
                    if (timeout == 0) {
                        return false;
                    }

                    if (timeout > 0) {
                        final long elapsedMillis = System
                            .currentTimeMillis() - startMillis;

                        if ((elapsedMillis < 0) || (elapsedMillis >= timeout)) {
                            return false;
                        }

                        _mutex.wait(timeout - elapsedMillis);
                    } else {
                        _mutex.wait();
                    }
                } catch (final InterruptedException exception) {
                    Thread.currentThread().interrupt();

                    return false;
                }
            }

            close();
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        final String serverName = _serverName;

        if (serverName != null) {
            _serverName = null;
            ServiceRegistry
                .getInstance()
                .unregister(serverName, getThisLogger());
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public final String toString()
    {
        return getName() + " " + super.toString();
    }

    /**
     * Binds this server to the registry.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean bind()
    {
        _serverName = ServiceRegistry
            .getInstance()
            .registerServer(this, _registryEntry.getURI(), getThisLogger());

        return _serverName != null;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void fillRolesMap()
    {
        if (_readRoles != null) {
            mapRoles(READ_ROLE, _readRoles);
        }

        if (_writeRoles != null) {
            mapRoles(WRITE_ROLE, _writeRoles);
        }
    }

    /**
     * Sets up this server.
     *
     * @param config The configuration.
     * @param somProperties The queue properties.
     * @param defaultBindingPrefix The default binding prefix.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean setUp(
            @Nonnull final Config config,
            @Nonnull final KeyedGroups somProperties,
            @Nonnull final String defaultBindingPrefix)
    {
        if (hasSecurityContext() && !super.setUp(config)) {
            return false;
        }

        final String name;

        if (hasSecurityContext()) {
            _registryEntry = RegistryEntry
                .newBuilder()
                .setBinding(somProperties.getString(SOMServer.BINDING_PROPERTY))
                .setName(somProperties.getString(SOMServer.NAME_PROPERTY))
                .setDefaultPrefix(defaultBindingPrefix)
                .setDefaultRegistryAddress(ServiceRegistry.getRegistryAddress())
                .setDefaultRegistryPort(ServiceRegistry.getRegistryPort())
                .build();

            if (_registryEntry == null) {
                return false;
            }

            name = _registryEntry.getName();
            _readRoles = somProperties.getStrings(READ_ROLE_PROPERTY);
            _writeRoles = somProperties.getStrings(WRITE_ROLE_PROPERTY);

            final Optional<ElapsedTime> defaultKeepAlive = getConfig()
                .getElapsedValue(
                    DEFAULT_KEEP_ALIVE_PROPERTY,
                    Optional.of(DEFAULT_KEEP_ALIVE),
                    Optional.of(ElapsedTime.EMPTY));

            _keepAlive = somProperties
                .getElapsed(
                    KEEP_ALIVE_PROPERTY,
                    defaultKeepAlive,
                    Optional.of(ElapsedTime.EMPTY))
                .orElse(null);
            getThisLogger().debug(ServiceMessages.KEEP_ALIVE, name, _keepAlive);
        } else {
            name = somProperties
                .getString(SOMServer.NAME_PROPERTY)
                .orElse(null);
        }

        if ((name == null) || name.isEmpty()) {
            getThisLogger().error(ServiceMessages.MISSING_NAME);

            return false;
        }

        _name = name;

        return true;
    }

    /** The default keep-alive. */
    public static final ElapsedTime DEFAULT_KEEP_ALIVE = ElapsedTime
        .fromRaw(5 * ElapsedTime.MINUTE.toRaw());

    /** The default keep-alive property. */
    public static final String DEFAULT_KEEP_ALIVE_PROPERTY =
        "service.som.keep.alive";

    /** The keep-alive property. */
    public static final String KEEP_ALIVE_PROPERTY = "keep.alive";

    /** The read role. */
    public static final String READ_ROLE = "Read";

    /** The security role needed to read from this service. */
    public static final String READ_ROLE_PROPERTY = "role.read";

    /** The write role. */
    public static final String WRITE_ROLE = "Write";

    /** The security role needed to write to this service. */
    public static final String WRITE_ROLE_PROPERTY = "role.write";

    private ElapsedTime _keepAlive;
    private final Object _mutex = new Object();
    private String _name;
    private String[] _readRoles;
    private RegistryEntry _registryEntry;
    private String _serverName;
    private String[] _writeRoles;

    /**
     * Descriptor.
     */
    @Immutable
    protected static class Descriptor
    {
        /**
         * Constructs an instance.
         *
         * @param modeName The service operation mode name.
         * @param clientName The client name.
         */
        public Descriptor(
                @Nonnull final String modeName,
                @Nonnull final String clientName)
        {
            _modeName = modeName;
            _clientName = clientName;
        }

        /**
         * Gets the client name.
         *
         * @return The client name.
         */
        @Nonnull
        @CheckReturnValue
        public String getClientName()
        {
            return _clientName;
        }

        /**
         * Gets the service operation mode name.
         *
         * @return The service operation mode name.
         */
        @Nonnull
        @CheckReturnValue
        public String getModeName()
        {
            return _modeName;
        }

        private final String _clientName;
        private final String _modeName;
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
