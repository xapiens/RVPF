/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Controller.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.valve;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Controller.
 *
 * <p>Instances of this class are used by services to control a Valve. Where the
 * Valve is configured by its own properties file, this class get its
 * configuration from its creator configuration. Since more than one Valve may
 * be configured, all the informations needed to complete the connection to the
 * control port are supplied in a properties group.</p>
 */
public final class Controller
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param config The config.
     */
    public Controller(@Nonnull final Config config)
    {
        _config = config;
    }

    /**
     * Closes this controller.
     */
    public void close()
    {
        final Socket socket = _socket.getAndSet(null);

        if (socket != null) {
            final ServiceThread thread = _thread.getAndSet(null);

            if (thread != null) {
                _LOGGER
                    .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            }

            try {
                socket.close();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            _LOGGER
                .debug(
                    ServiceMessages.VALVE_CLOSED,
                    socket.getRemoteSocketAddress());

            if (thread != null) {
                try {
                    thread.join();
                } catch (final InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }

                if (thread.getThrowable().isPresent()) {
                    throw new RuntimeException(thread.getThrowable().get());
                }
            }
        }
    }

    /**
     * Opens a control connection to a valve.
     *
     * @param propertiesName The name of a group of properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean open(@Nonnull final String propertiesName)
    {
        final KeyedGroups valveProperties = _config
            .getPropertiesGroup(propertiesName);

        if (valveProperties.isMissing()) {
            _LOGGER.error(ServiceMessages.MISSING_PROPERTIES, propertiesName);

            return false;
        }

        final String addressString = valveProperties
            .getString(CONTROL_ADDRESS_PROPERTY)
            .orElse(null);

        if (addressString == null) {
            _LOGGER.error(ServiceMessages.NO_CONTROL_ADDRESS, propertiesName);

            return false;
        }

        final Optional<InetSocketAddress> socketAddress = Inet
            .socketAddress(addressString);

        if (!socketAddress.isPresent()) {
            _LOGGER.error(BaseMessages.BAD_ADDRESS, addressString);

            return false;
        }

        final ElapsedTime retryDelay = valveProperties
            .getElapsed(
                RETRY_DELAY_PROPERTY,
                Optional.of(DEFAULT_RETRY_DELAY),
                Optional.of(ElapsedTime.INFINITY))
            .get();
        final boolean retry = valveProperties.getBoolean(RETRY_PROPERTY);

        _required = valveProperties.getBoolean(REQUIRED_PROPERTY, retry);

        if (_required && _LOGGER.isDebugEnabled()) {
            _LOGGER.debug(ServiceMessages.VALVE_CONNECTION_REQUIRED);

            if (retry) {
                _LOGGER
                    .debug(ServiceMessages.CONNECTION_RETRY_DELAY, retryDelay);
            }
        }

        final SecurityContext securityContext = new SecurityContext(_LOGGER);
        final KeyedGroups securityProperties = valveProperties
            .getGroup(SecurityContext.SECURITY_PROPERTIES);

        if (!securityContext
            .setUp(_config.getProperties(), securityProperties)) {
            return false;
        }

        final boolean secure = securityContext.isCertified()
                || securityContext.isSecure()
                || !Inet.isOnLocalHost(socketAddress.get().getAddress());
        final boolean connected = _connect(
            socketAddress.get(),
            securityContext,
            secure,
            retry,
            retryDelay);

        if (connected) {
            final ServiceThread thread = new ServiceThread(
                this,
                "Valve connection monitor");

            if (_thread.compareAndSet(null, thread)) {
                _LOGGER
                    .debug(ServiceMessages.STARTING_THREAD, thread.getName());
                thread.start();
            }
        }

        return connected;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        final Socket socket = _socket.get();
        final InputStream inputStream;

        if (socket == null) {
            return;
        }

        try {
            inputStream = socket.getInputStream();

            while (inputStream.read() >= 0) {}
        } catch (final IOException exception) {
            // Lost the connection.
        }

        if (_socket.get() != null) {    // Not a normal close.
            final Message message = new Message(
                ServiceMessages.VALVE_CONNECTION_LOST);

            if (_required) {
                _LOGGER.error(message);

                if (_config.hasService()) {
                    _config.getService().fail();
                }
            } else {
                _LOGGER.warn(message);
            }
        }
    }

    private boolean _connect(
            final SocketAddress socketAddress,
            final SecurityContext securityContext,
            final boolean secure,
            final boolean retry,
            final ElapsedTime retryDelay)
    {
        boolean retrying = false;

        for (;;) {
            try {
                final Socket socket;

                if (secure) {    // Use a SSL connection.
                    try {
                        securityContext.checkForSecureOperation();
                    } catch (final SSLException exception) {
                        _LOGGER
                            .error(
                                BaseMessages.VERBATIM,
                                exception.getMessage());

                        return false;
                    }

                    final SSLContext sslContext = securityContext
                        .createSSLContext();

                    socket = sslContext.getSocketFactory().createSocket();
                    socket.connect(socketAddress);
                    ((SSLSocket) socket).startHandshake();
                } else {    // Use a normal connection.
                    socket = new Socket();
                    socket.connect(socketAddress);
                }

                _socket.set(socket);
                _LOGGER.info(ServiceMessages.VALVE_OPENED, socketAddress);

                break;
            } catch (final FileNotFoundException exception) {
                if (_required) {
                    _LOGGER
                        .error(BaseMessages.VERBATIM, exception.getMessage());

                    return false;
                }

                _LOGGER.warn(BaseMessages.VERBATIM, exception.getMessage());

                break;
            } catch (final IOException exception) {
                final Message message = new Message(
                    ServiceMessages.VALVE_CONNECTION_FAILED,
                    socketAddress,
                    exception.getMessage());

                if (retry) {
                    if (!retrying) {
                        _LOGGER.warn(message);
                        _LOGGER.info(ServiceMessages.RETRYING);
                        retrying = true;
                    }

                    try {
                        _config.getService().snooze(retryDelay);
                    } catch (final InterruptedException interruptedException) {
                        _LOGGER.warn(ServiceMessages.INTERRUPTED);
                        Thread.currentThread().interrupt();

                        return false;
                    }
                } else if (_required) {
                    _LOGGER.error(message);

                    return false;
                } else {
                    _LOGGER.info(message);

                    break;
                }
            }
        }

        return true;
    }

    /**
     * Specifies the interface and port listening for the control of the valve.
     * It must have the form "interface:port" (without the quotes).
     */
    public static final String CONTROL_ADDRESS_PROPERTY = "control.address";

    /** Default retry delay. */
    public static final ElapsedTime DEFAULT_RETRY_DELAY = ElapsedTime
        .fromMillis(15000);

    /**
     * Indicates that a successful connection to the control port of the valve
     * is required for this service to complete its initialization.
     */
    public static final String REQUIRED_PROPERTY = "required";

    /** The elapsed time before retrying to connect. */
    public static final String RETRY_DELAY_PROPERTY = "retry.delay";

    /** Requests retries for a successful connection. */
    public static final String RETRY_PROPERTY = "retry";
    private static final Logger _LOGGER = Logger.getInstance(Controller.class);

    private final Config _config;
    private boolean _required;
    private final AtomicReference<Socket> _socket = new AtomicReference<>();
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
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
