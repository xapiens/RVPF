/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServerConnection.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.transport;

import java.io.EOFException;
import java.io.IOException;

import java.net.ProtocolException;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.modbus.ModbusClientProxy;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.message.ErrorResponse;
import org.rvpf.pap.modbus.message.MaskWriteRegister;
import org.rvpf.pap.modbus.message.Prefix;
import org.rvpf.pap.modbus.message.ReadCoils;
import org.rvpf.pap.modbus.message.ReadDiscreteInputs;
import org.rvpf.pap.modbus.message.ReadHoldingRegisters;
import org.rvpf.pap.modbus.message.ReadInputRegisters;
import org.rvpf.pap.modbus.message.Transaction;
import org.rvpf.pap.modbus.message.WriteMultipleCoils;
import org.rvpf.pap.modbus.message.WriteMultipleRegisters;
import org.rvpf.pap.modbus.message.WriteReadMultipleRegisters;
import org.rvpf.pap.modbus.message.WriteSingleCoil;
import org.rvpf.pap.modbus.message.WriteSingleRegister;
import org.rvpf.pap.modbus.message.WriteTransaction;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Server connection.
 */
public final class ServerConnection
    extends Connection
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param transport The transport object.
     * @param clientProxy The client proxy.
     * @param listener An optional listener.
     * @param writeOnly True if writeOnly.
     */
    public ServerConnection(
            @Nonnull final Transport transport,
            @Nonnull final ModbusClientProxy clientProxy,
            @Nonnull final Optional<PAPConnectionListener> listener,
            final boolean writeOnly)
    {
        super(transport, clientProxy, listener);

        _clientProxy = clientProxy;
        _writeOnly = writeOnly;
        _responder = new Responder(this);

        transport.setLittleEndian(_clientProxy.isLittleEndian());
        _clientProxy.setConnection(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
    {
        getThisLogger()
            .debug(
                ModbusMessages.CLIENT_CONNECTION_ACCEPTED,
                getRemoteAddress());

        try {
            final Transport transport = getTransport();

            for (;;) {
                final Prefix prefix = transport.receivePrefix();

                if (prefix.getUnitIdentifier()
                        != transport.getUnitIdentifier()) {
                    getThisLogger()
                        .warn(
                            ModbusMessages.UNEXPECTED_UNIT_IDENTIFIER,
                            String.valueOf(prefix.getUnitIdentifier() & 0xFF));

                    throw new ProtocolException();
                }

                final byte functionCode = transport.receiveByte();

                switch (functionCode) {
                    case ReadCoils.FUNCTION_CODE: {
                        final Transaction.Request request =
                            new ReadCoils.Request(
                                prefix)
                                .read(transport);

                        if (_writeOnly) {
                            break;
                        }

                        _clientProxy.addRequest(request);

                        continue;
                    }
                    case ReadDiscreteInputs.FUNCTION_CODE: {
                        final Transaction.Request request =
                            new ReadDiscreteInputs.Request(
                                prefix)
                                .read(transport);

                        if (_writeOnly) {
                            break;
                        }

                        _clientProxy.addRequest(request);

                        continue;
                    }
                    case ReadHoldingRegisters.FUNCTION_CODE: {
                        final Transaction.Request request =
                            new ReadHoldingRegisters.Request(
                                prefix)
                                .read(transport);

                        if (_writeOnly) {
                            break;
                        }

                        _clientProxy.addRequest(request);

                        continue;
                    }
                    case ReadInputRegisters.FUNCTION_CODE: {
                        final Transaction.Request request =
                            new ReadInputRegisters.Request(
                                prefix)
                                .read(transport);

                        if (_writeOnly) {
                            break;
                        }

                        _clientProxy.addRequest(request);

                        continue;
                    }
                    case WriteReadMultipleRegisters.FUNCTION_CODE: {
                        final Transaction.Request request =
                            new WriteReadMultipleRegisters.Request(
                                prefix)
                                .read(transport);

                        if (_writeOnly) {
                            break;
                        }

                        _clientProxy.addRequest(request);

                        continue;
                    }
                    case WriteSingleCoil.FUNCTION_CODE: {
                        final WriteTransaction.Request request =
                            new WriteSingleCoil.Request(
                                prefix)
                                .read(transport);

                        _clientProxy.addRequest(request);
                        sendResponse(
                            request.createResponse(Transaction.NO_VALUES));

                        continue;
                    }
                    case WriteSingleRegister.FUNCTION_CODE: {
                        final WriteTransaction.Request request =
                            new WriteSingleRegister.Request(
                                prefix)
                                .read(transport);

                        _clientProxy.addRequest(request);
                        sendResponse(
                            request.createResponse(Transaction.NO_VALUES));

                        continue;
                    }
                    case WriteMultipleCoils.FUNCTION_CODE: {
                        final WriteTransaction.Request request =
                            new WriteMultipleCoils.Request(
                                prefix)
                                .read(transport);

                        _clientProxy.addRequest(request);
                        sendResponse(
                            request.createResponse(Transaction.NO_VALUES));

                        continue;
                    }
                    case WriteMultipleRegisters.FUNCTION_CODE: {
                        final WriteTransaction.Request request =
                            new WriteMultipleRegisters.Request(
                                prefix)
                                .read(transport);

                        _clientProxy.addRequest(request);
                        sendResponse(
                            request.createResponse(Transaction.NO_VALUES));

                        continue;
                    }
                    case MaskWriteRegister.FUNCTION_CODE: {
                        final WriteTransaction.Request request =
                            new MaskWriteRegister.Request(
                                prefix)
                                .read(transport);

                        if (_writeOnly) {
                            break;
                        }

                        _clientProxy.addRequest(request);
                        sendResponse(
                            request.createResponse(Transaction.NO_VALUES));

                        continue;
                    }
                    default: {
                        break;
                    }
                }

                sendResponse(
                    new ErrorResponse(
                        prefix,
                        functionCode,
                        ExceptionCode.ILLEGAL_FUNCTION));
            }
        } catch (final EOFException exception) {
            getThisLogger()
                .debug(
                    ModbusMessages.CLIENT_CONNECTION_CLOSED,
                    getRemoteAddress());
        } catch (final IOException exception) {
            getThisLogger().debug(exception, BaseMessages.VERBATIM, exception);
        } catch (final Transaction.FormatException exception) {
            getThisLogger()
                .error(BaseMessages.VERBATIM, exception.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * Sends a response.
     *
     * @param response The response.
     */
    public void sendResponse(@Nonnull final Transaction.Response response)
    {
        _responder.addResponse(response);
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        _responder.start();

        final ServiceThread thread = new ServiceThread(
            this,
            "Modbus server (receiver from " + getRemoteAddress() + ")");

        if (_thread.compareAndSet(null, thread)) {
            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final Thread thread = _thread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            close();

            if (thread != Thread.currentThread()) {
                try {
                    thread.join();
                } catch (final InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
            }

            _responder.stop();

            super.stop();
        }
    }

    private final ModbusClientProxy _clientProxy;
    private final Responder _responder;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
    private final boolean _writeOnly;
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
