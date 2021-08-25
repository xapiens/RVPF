/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ControlPort.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.valve;

import java.io.IOException;
import java.io.InputStream;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.service.ServiceMessages;

/**
 * Control port.
 *
 * <p>An instance of this class runs in its caller's thread, listening for
 * control connections, resuming or pausing the controlled ports connections.
 * </p>
 */
final class ControlPort
{
    /**
     * Closes this ControlPort.
     */
    synchronized void close()
    {
        final SocketAddress listenAddress = getListenAddress();

        try {
            if (_socket != null) {
                _socket.close();    // Will be cleared by 'listen'.
            }

            _pause();
            _serverSocket.close();    // Will be cleared by 'listen'.
            _LOGGER.info(ValveMessages.STOPPED_LISTENING, listenAddress);
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Gets the listen address.
     *
     * <p>For unit tests.</p>
     *
     * @return The listen address.
     */
    @Nonnull
    @CheckReturnValue
    SocketAddress getListenAddress()
    {
        return Require.notNull(_serverSocket.getLocalSocketAddress());
    }

    /**
     * Asks if the control is inverted.
     *
     * @return True if the control is inverted.
     */
    @CheckReturnValue
    boolean isInverted()
    {
        return _inverted;
    }

    /**
     * Listens on the server socket.
     */
    void listen()
    {
        _LOGGER.info(ValveMessages.LISTENING, getListenAddress());

        for (;;) {
            final Socket socket;

            // Waits for a connection.

            try {
                socket = _serverSocket.accept();

                if (socket instanceof SSLSocket) {
                    socket.setSoTimeout(_listenTimeout);
                    ((SSLSocket) socket).startHandshake();
                    socket.setSoTimeout(0);
                    socket.setKeepAlive(true);
                }
            } catch (final SocketException exception) {
                break;
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            synchronized (this) {
                _socket = socket;
            }

            if (_LOGGER.isDebugEnabled()) {
                final Object mode;

                if (_serverSocket instanceof SSLServerSocket) {
                    mode = ((SSLServerSocket) _serverSocket)
                        .getNeedClientAuth()? BaseMessages.CONNECTION_CERTIFIED
                            : BaseMessages.CONNECTION_SECURE;
                } else {
                    mode = ValveMessages.CONNECTION_NORMAL;
                }

                _LOGGER
                    .debug(
                        ValveMessages.CONTROL_ACCEPTED,
                        mode,
                        _socket.getLocalSocketAddress(),
                        _socket.getRemoteSocketAddress());
            }

            // Uses the connection to control the unlocking of the controlled
            // ports.
            _control();
        }

        synchronized (this) {
            _socket = null;
        }

        _serverSocket = null;
        _portManager = null;
    }

    /**
     * Opens this control port.
     *
     * @param serverSocket The server socket.
     * @param controlAddress The control address for listening.
     * @param portManager The controlled port manager.
     * @param listenTimeout The timeout while waiting for handshake.
     * @param inverted True for inverted control.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean open(
            @Nonnull final ServerSocket serverSocket,
            @Nonnull final SocketAddress controlAddress,
            @Nonnull final PortManager portManager,
            final int listenTimeout,
            final boolean inverted)
    {
        _serverSocket = serverSocket;

        try {
            _serverSocket.bind(controlAddress);
        } catch (final IOException exception) {
            _LOGGER
                .error(ServiceMessages.BIND_FAILED_, controlAddress, exception);

            return false;
        }

        if (!_serverSocket.isBound()) {
            _LOGGER.error(ServiceMessages.BIND_FAILED, controlAddress);

            return false;
        }

        _portManager = portManager;
        _listenTimeout = listenTimeout;
        _inverted = inverted;

        return true;
    }

    /**
     * Updates the stats.
     *
     * @param stats The stats.
     */
    synchronized void updateStats(@Nonnull final ValveStats stats)
    {
        stats.updateControlStats(_resumes, _pauses);
        _resumes = 0;
        _pauses = 0;
    }

    /**
     * Waits for control.
     *
     * <p>For unit tests.</p>
     *
     * @param control The value to wait for.
     *
     * @throws InterruptedException When interrupted.
     */
    synchronized void waitForControl(
            final boolean control)
        throws InterruptedException
    {
        while (_control != control) {
            wait();
        }
    }

    private void _control()
    {
        final InputStream inputStream;

        // If we can get the input stream, (un)locks the controlled ports.

        synchronized (this) {
            try {
                inputStream = _socket.getInputStream();
            } catch (final IOException exception) {
                return;
            }

            if (_inverted) {
                _pause();
            } else {
                _resume();
            }
        }

        // Waits until the control port is closed or disconnected.

        for (;;) {
            try {
                if (inputStream.read() < 0) {
                    break;
                }
            } catch (final IOException exception) {
                break;
            }
        }

        // (Un)Locks the controlled ports, disposes of the control connection.

        synchronized (this) {
            if (_inverted) {
                _resume();
            } else {
                _pause();
            }

            try {
                _socket.close();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }

            _socket = null;
        }
    }

    private void _pause()
    {
        if (_control) {
            _LOGGER.info(ValveMessages.LOCKING_CONTROLLED_PORTS);

            _portManager.pause();
            ++_pauses;

            _control = false;
            notifyAll();
        }
    }

    private void _resume()
    {
        Require.failure(_control);

        _LOGGER.info(ValveMessages.UNLOCKING_CONTROLLED_PORTS);

        _portManager.resume();
        ++_resumes;

        _control = true;
        notifyAll();
    }

    private static final Logger _LOGGER = Logger.getInstance(ControlPort.class);

    private boolean _control;
    private boolean _inverted;
    private int _listenTimeout;
    private int _pauses;
    private PortManager _portManager;
    private int _resumes;
    private ServerSocket _serverSocket;
    private Socket _socket;
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
