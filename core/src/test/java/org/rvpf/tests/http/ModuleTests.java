/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModuleTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import java.nio.charset.StandardCharsets;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.rvpf.base.tool.Require;
import org.rvpf.base.util.PasswordAuthenticator;
import org.rvpf.config.Config;
import org.rvpf.http.HTTPServerActivator;
import org.rvpf.http.HTTPServerImpl;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceThread;
import org.rvpf.tests.core.CoreTestsMessages;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.BeforeTest;

/**
 * Module tests.
 */
public abstract class ModuleTests
    extends StoreClientTests
{
    /**
     * Resets the authenticator.
     *
     * @throws Exception On failure.
     */
    public static final void resetAuthenticator()
        throws Exception
    {
        Authenticator.setDefault(null);
    }

    /**
     * Sets up properties.
     */
    @BeforeTest
    public static void setUpProperties()
    {
        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);
    }

    /**
     * Assigns the authenticator.
     */
    public final void assignAuthenticator()
    {
        final String user = _server
            .getConfig()
            .get()
            .getStringValue(USER_PROPERTY)
            .get();
        final Optional<char[]> password = _server
            .getConfig()
            .get()
            .getPasswordValue(PASSWORD_PROPERTY);

        PasswordAuthenticator.setUp(user, password);
    }

    /**
     * Gets a connection's response decoded message.
     *
     * @param connection The HTTP connection.
     *
     * @return The decoded message.
     *
     * @throws IOException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    protected static String getResponseMessage(
            @Nonnull final HttpURLConnection connection)
        throws IOException
    {
        try {
            return URLDecoder
                .decode(
                    connection.getResponseMessage(),
                    StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Gets the listener port.
     *
     * @return The listener port.
     */
    @CheckReturnValue
    protected final int getListenerPort()
    {
        if (_listenerPort == 0) {
            int listenerPort;

            for (;;) {
                listenerPort = ((HTTPServerImpl) _server.getService())
                    .getListenerPort(0);

                if (listenerPort >= 0) {
                    break;
                }

                ServiceThread.yieldAll();
            }

            Require
                .success(
                    listenerPort > 1023,
                    "Unexpected listener port: " + listenerPort);

            _listenerPort = listenerPort;
        }

        return _listenerPort;
    }

    /**
     * Gets the server.
     *
     * @return The server.
     */
    @Nonnull
    @CheckReturnValue
    protected final ServiceActivator getServer()
    {
        return Require.notNull(_server);
    }

    /**
     * Opens an HTTP(S) URL connection on the specified path.
     *
     * @param path The path for the URL.
     * @param secure True asks for SSL.
     *
     * @return The HTTP URL connection.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    protected final HttpURLConnection openConnection(
            @Nonnull final String path,
            final boolean secure)
        throws Exception
    {
        final URL url = new URL(
            secure? "https": "http",
            "localhost",
            getListenerPort(),
            "/" + path);
        final HttpURLConnection connection = (HttpURLConnection) url
            .openConnection();

        if (secure) {
            if (_socketFactory == null) {
                _socketFactory = getSecurityContext()
                    .createSSLContext()
                    .getSocketFactory();
            }

            ((HttpsURLConnection) connection)
                .setSSLSocketFactory(_socketFactory);
        }

        getThisLogger()
            .debug(CoreTestsMessages.CONNECTION, connection.getURL());

        return connection;
    }

    /**
     * Sets the listener port.
     *
     * @param listenerPort The listener port.
     */
    protected final void setListenerPort(final int listenerPort)
    {
        _listenerPort = listenerPort;
    }

    /**
     * Sets up the HTTP server to test a module.
     *
     * @throws Exception On failure.
     */
    protected final void setUpServer()
        throws Exception
    {
        setUpAlerter();

        _server = createService(
            HTTPServerActivator.class,
            Optional.of(getClass().getSimpleName()));
    }

    /**
     * Starts the server.
     *
     * @throws Exception On failure.
     */
    protected final void startServer()
        throws Exception
    {
        if (_server == null) {
            setUpServer();
        }

        setListenerPort(0);
        startService(_server);

        assignAuthenticator();
    }

    /**
     * Stops the server.
     *
     * @throws Exception On failure.
     */
    protected final void stopServer()
        throws Exception
    {
        if (_server != null) {
            stopService(_server);
            _server = null;
        }

        resetAuthenticator();
    }

    private static final String _TESTS_PROPERTIES = "rvpf-http.properties";

    private int _listenerPort;
    private ServiceActivator _server;
    private SSLSocketFactory _socketFactory;
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
