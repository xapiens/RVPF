/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: HTTPSupport.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http;

import java.util.EventListener;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.security.ServerSecurityContext;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * HTTP Support.
 */
public interface HTTPSupport
{
    /**
     * Destroys the server.
     *
     * @throws Exception Thrown by the server.
     */
    void destroyServer()
        throws Exception;

    /**
     * Gets the listener address.
     *
     * @param index The listener index.
     *
     * @return The optional listener address.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getListenerAddress(int index);

    /**
     * Gets the listener count.
     *
     * @return The listener count.
     */
    @CheckReturnValue
    int getListenerCount();

    /**
     * Gets the listener port.
     *
     * @param index The listener index.
     *
     * @return The listener port.
     */
    @CheckReturnValue
    int getListenerPort(int index);

    /**
     * Asks if the server is started.
     *
     * @return True if the server is started.
     */
    @CheckReturnValue
    boolean isServerStarted();

    /**
     * Sets up a context.
     *
     * @param path The context path.
     * @param resource The optional resource base.
     * @param servlets The servlets map.
     * @param realmName The optional realm name.
     * @param authenticator The optional authenticator identification.
     * @param confidential Confidential channel needed.
     * @param roles The allowed roles (may be empty).
     * @param eventListener The optional context event listener.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUpContext(
            @Nonnull final String path,
            @Nonnull final Optional<String> resource,
            @Nonnull final Map<String, String> servlets,
            @Nonnull final Optional<String> realmName,
            @Nonnull final Optional<String> authenticator,
            final boolean confidential,
            @Nonnull final String[] roles,
            @Nonnull final Optional<EventListener> eventListener);

    /**
     * Sets up a listener.
     *
     * @param address The host interface address.
     * @param port The interface port.
     * @param confidentialPort A confidential port.
     * @param securityContext An optional security context.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUpListener(
            @Nonnull String address,
            int port,
            int confidentialPort,
            @Nonnull Optional<ServerSecurityContext> securityContext);

    /**
     * Sets up a realm.
     *
     * @param realmName The realm name.
     * @param realmProperties The the realm configuration properties.
     * @param securityContext The security context.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUpRealm(
            @Nonnull String realmName,
            @Nonnull KeyedGroups realmProperties,
            @Nonnull SecurityContext securityContext);

    /**
     * Sets up the server.
     *
     * @param httpServerAppImpl The HTTP server application implementation.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUpServer(@Nonnull HTTPServerAppImpl httpServerAppImpl);

    /**
     * Starts the server.
     *
     * @throws Exception Thrown by the server.
     */
    void startServer()
        throws Exception;

    /**
     * Stops the server.
     *
     * @throws Exception Thrown by the server.
     */
    void stopServer()
        throws Exception;

    /** Basic authenticator. */
    String BASIC_AUTHENTICATOR = "Basic";

    /** Certificate authenticator. */
    String CERTIFICATE_AUTHENTICATOR = "Certificate";

    /** Digest authenticator. */
    String DIGEST_AUTHENTICATOR = "Digest";
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
