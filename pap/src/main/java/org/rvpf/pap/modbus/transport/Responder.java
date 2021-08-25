/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Responder.java 4082 2019-06-14 18:38:55Z SFB $
 */

package org.rvpf.pap.modbus.transport;

import java.io.IOException;

import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.message.ErrorResponse;
import org.rvpf.pap.modbus.message.Transaction;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Responder.
 */
public final class Responder
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param connection The connection for transmitting responses.
     */
    Responder(@Nonnull final Connection connection)
    {
        _connection = connection;
    }

    /**
     * Adds a response.
     *
     * @param response The response.
     */
    public void addResponse(@Nonnull final Transaction.Response response)
    {
        _responses.add(response);
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        final Transport transport = _connection.getTransport();

        for (;;) {
            final Transaction.Response response;

            try {
                response = _responses.take();
            } catch (final InterruptedException exception) {
                break;
            }

            if (response instanceof ErrorResponse) {
                final ErrorResponse errorResponse = (ErrorResponse) response;
                final Transaction.Request request = errorResponse.getRequest();
                final String requestCodeString = (request
                    != Transaction.Request.NULL)? request
                        .getName(): String
                            .format(
                                    (Locale) null,
                                            "0x%02X" + Integer.valueOf(
                                                    errorResponse.getErrorCode()
                                                    & 0xFF));

                _LOGGER
                    .trace(
                        ModbusMessages.SENDING_ERROR_RESPONSE,
                        errorResponse.getExceptionCodeName(),
                        requestCodeString);
            } else {
                _LOGGER
                    .trace(ModbusMessages.SENDING_RESPONSE, response.getName());
            }

            try {
                response.write(transport);
                transport.flush();
            } catch (final IOException exception) {
                _LOGGER
                    .debug(
                        ModbusMessages.FAILED_SEND_RESPONSE,
                        transport.getRemoteAddress(),
                        exception);

                break;
            }
        }

        try {
            transport.close();
        } catch (final IOException exception) {
            // Ignores.
        }
    }

    /**
     * Starts.
     */
    public void start()
    {
        final ServiceThread thread = new ServiceThread(
            this,
            "Modbus server (responder to " + _connection.getRemoteAddress()
            + ")");

        if (_thread.compareAndSet(null, thread)) {
            _LOGGER.debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /**
     * Stops.
     */
    public void stop()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            _LOGGER.debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            _connection.stop();
            Require.ignored(thread.interruptAndJoin(_LOGGER, 0));
        }
    }

    private static final Logger _LOGGER = Logger.getInstance(Responder.class);

    private final Connection _connection;
    private final BlockingQueue<Transaction.Response> _responses =
        new LinkedBlockingQueue<>();
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
