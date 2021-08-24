/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SessionClientContext.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.base.rmi;

import java.net.URI;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.security.ClientSecurityContext;
import org.rvpf.base.tool.Require;

/**
 * Session client context.
 *
 * <p>Holds a client context by keeping a reference to a server URI and by
 * generating an identifying UUID.</p>
 *
 * <p>The generated UUID is used to establish a relation between RMI server
 * sessions and the client's context for socket creation. This is needed because
 * the framework allows configurations where mutiple services are running within
 * the same process, each with its own client / server properties.</p>
 */
@ThreadSafe
public final class SessionClientContext
{
    /**
     * Constructs an instance.
     *
     * @param registryEntry The registry entry.
     * @param logger The logger instance to use.
     */
    public SessionClientContext(
            @Nonnull final RegistryEntry registryEntry,
            @Nonnull final Logger logger)
    {
        _registryEntry = Require.notNull(registryEntry);
        _logger = Require.notNull(logger);
        _uuid = UUID.generate();
        _securityContext = new ClientSecurityContext(logger);
    }

    /**
     * Gets the security context.
     *
     * @return The security context.
     */
    @Nonnull
    @CheckReturnValue
    public ClientSecurityContext getSecurityContext()
    {
        return _securityContext;
    }

    /**
     * Gets the server name.
     *
     * @return The server name.
     */
    @Nonnull
    @CheckReturnValue
    public String getServerName()
    {
        return _registryEntry.getLookupKey();
    }

    /**
     * Gets the server URI.
     *
     * @return The server URI.
     */
    @Nonnull
    @CheckReturnValue
    public URI getServerURI()
    {
        return _registryEntry.getURI();
    }

    /**
     * Gets the timeout.
     *
     * @return The timeout.
     */
    @Nonnegative
    @CheckReturnValue
    public int getTimeout()
    {
        return _timeout;
    }

    /**
     * Gets the identifying UUID.
     *
     * @return The identifying UUID.
     */
    @Nonnull
    @CheckReturnValue
    public UUID getUUID()
    {
        return _uuid;
    }

    /**
     * Asks if this context is private.
     *
     * @return True if this context is private.
     */
    @CheckReturnValue
    public boolean isPrivate()
    {
        return _registryEntry.isPrivate();
    }

    /**
     * Asks if the server is remote.
     *
     * @return True if the server is remote.
     */
    @CheckReturnValue
    public boolean isRemote()
    {
        return _registryEntry.isRemote();
    }

    /**
     * Performs a lookup of the registry entry.
     *
     * @param sessionMode The session mode name.
     *
     * @return The remote instance.
     *
     * @throws NotBoundException When the server name is not currently bound.
     * @throws RemoteException When the communication with the registry failed.
     * @throws AccessException When access is denied.
     */
    @Nonnull
    @CheckReturnValue
    public Remote lookup(
            @Nonnull final String sessionMode)
        throws AccessException, RemoteException, NotBoundException
    {
        return _registryEntry.lookup(sessionMode, _logger);
    }

    /**
     * Registers itself.
     *
     * @return False if already registered.
     */
    public synchronized boolean register()
    {
        if (isPrivate()) {
            return true;
        }

        final String serverName = getServerName();

        if (_registrations == 0) {
            if (!ClientSocketFactory.register(this)) {
                throw new RuntimeException(
                    Message.format(
                        BaseMessages.CONTEXT_REGISTER_FAILED,
                        getUUID(),
                        serverName));
            }

            _logger
                .debug(BaseMessages.CONTEXT_REGISTERED, getUUID(), serverName);
        }

        return ++_registrations == 1;
    }

    /**
     * Sets the timeout.
     *
     * @param timeout The timeout.
     */
    public void setTimeout(@Nonnegative final int timeout)
    {
        _timeout = timeout;
    }

    /**
     * Unregisters itself.
     *
     * @return False if not registered.
     */
    public synchronized boolean unregister()
    {
        if (isPrivate()) {
            return true;
        }

        Require.success(_registrations > 0);

        final String serverName = getServerName();

        if (_registrations == 1) {
            if (!ClientSocketFactory.unregister(this)) {
                throw new RuntimeException(
                    Message.format(
                        BaseMessages.CONTEXT_UNREGISTER_FAILED,
                        getUUID(),
                        serverName));
            }

            _logger
                .debug(
                    BaseMessages.CONTEXT_UNREGISTERED,
                    getUUID(),
                    serverName);
        }

        return --_registrations == 0;
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    Logger getLogger()
    {
        return _logger;
    }

    private final Logger _logger;
    @GuardedBy("this")
    private int _registrations;
    private final RegistryEntry _registryEntry;
    private final ClientSecurityContext _securityContext;
    private volatile int _timeout;
    private final UUID _uuid;
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
