/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LoggerServiceAppImpl.java 4059 2019-06-05 20:44:44Z SFB $
 */

package org.rvpf.service.log4j;

import java.io.IOException;
import java.io.ObjectInputStream;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

import java.util.Optional;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;

import org.apache.logging.log4j.core.net.server.ObjectInputStreamLogEventBridge;
import org.apache.logging.log4j.core.net.server.TcpSocketServer;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.security.ServerSecurityContext;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.app.ServiceAppImpl;

/**
 * Logger service application implementation.
 */
public final class LoggerServiceAppImpl
    extends ServiceAppImpl
{
    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Service service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        final KeyedGroups loggerServerProperties = getConfigProperties()
            .getGroup(LOGGER_SERVER_PROPERTIES);
        final Optional<String> addressString = loggerServerProperties
            .getString(ADDRESS_PROPERTY);

        if (!addressString.isPresent()) {
            getThisLogger()
                .error(BaseMessages.MISSING_PROPERTY, ADDRESS_PROPERTY);

            return false;
        }

        final Optional<InetSocketAddress> socketAddress = Inet
            .socketAddress(addressString.get());

        if (!socketAddress.isPresent()) {
            getThisLogger()
                .error(BaseMessages.BAD_ADDRESS, addressString.get());

            return false;
        }

        final ServerSecurityContext securityContext = new ServerSecurityContext(
            getThisLogger());
        final KeyedGroups securityProperties = loggerServerProperties
            .getGroup(SecurityContext.SECURITY_PROPERTIES);

        if (!securityContext
            .setUp(service.getConfig().getProperties(), securityProperties)) {
            return false;
        }

        final boolean secure = securityContext.isCertified()
                || securityContext.isSecure()
                || !securityProperties.isEmpty()
                || !socketAddress.get().getAddress().isLoopbackAddress();

        final ServerSocketFactory factory;

        if (secure) {
            try {
                securityContext.checkForSecureOperation();
            } catch (final SSLException exception) {
                getThisLogger()
                    .error(BaseMessages.VERBATIM, exception.getMessage());

                return false;
            }

            try {
                factory = securityContext
                    .createSSLContext()
                    .getServerSocketFactory();
            } catch (final SSLException exception) {
                getThisLogger()
                    .error(
                        ServiceMessages.SSL_CREATE_FAILED,
                        exception.getMessage());

                return false;
            }
        } else {
            factory = ServerSocketFactory.getDefault();
        }

        try {
            _serverSocket = factory
                .createServerSocket(
                    socketAddress.get().getPort(),
                    0,
                    socketAddress.get().getAddress());
        } catch (final IOException exception) {
            getThisLogger()
                .error(ServiceMessages.BIND_FAILED_, socketAddress, exception);

            return false;
        }

        if (!_serverSocket.isBound()) {
            getThisLogger().error(ServiceMessages.BIND_FAILED, socketAddress);

            return false;
        }

        if (secure) {
            ((SSLServerSocket) _serverSocket)
                .setNeedClientAuth(securityContext.isCertified());
        }

        try {
            _socketServer = new TcpSocketServer<>(
                _serverSocket.getLocalPort(),
                new ObjectInputStreamLogEventBridge(),
                _serverSocket);
        } catch (final IOException exception) {
            getThisLogger()
                .error(ServiceMessages.LOGGER_SERVER_FAILED, exception);

            return false;
        }

        _thread = new Thread(
            _socketServer,
            "Logger server (listener on "
            + _serverSocket.getLocalSocketAddress() + ")");

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        _thread.start();
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        try {
            _socketServer.shutdown();
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        try {
            _thread.join();
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_serverSocket != null) {
            try {
                _serverSocket.close();
            } catch (final IOException exception) {
                // Ignores.
            }

            _serverSocket = null;
        }

        super.tearDown();
    }

    /** Specifies on which address to listen. */
    public static final String ADDRESS_PROPERTY = "address";

    /** Logger server properties. */
    public static final String LOGGER_SERVER_PROPERTIES = "logger.server";

    private ServerSocket _serverSocket;
    private TcpSocketServer<ObjectInputStream> _socketServer;
    private Thread _thread;
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
