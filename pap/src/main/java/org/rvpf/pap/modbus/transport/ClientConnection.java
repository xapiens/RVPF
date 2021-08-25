/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClientConnection.java 4107 2019-07-13 13:18:26Z SFB $
 */

package org.rvpf.pap.modbus.transport;

import java.io.EOFException;
import java.io.IOException;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.TimeoutMonitor;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.ModbusServerProxy;
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
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Client connection.
 */
public final class ClientConnection
    extends Connection
    implements ServiceThread.Target, TimeoutMonitor.Client
{
    /**
     * Constructs an instance.
     *
     * @param transport The transport object.
     * @param serverProxy The server proxy.
     * @param listener A listener.
     */
    public ClientConnection(
            @Nonnull final Transport transport,
            @Nonnull final ModbusServerProxy serverProxy,
            @Nonnull final PAPConnectionListener listener)
    {
        super(transport, serverProxy, Optional.of(listener));

        transport.setBatchSize(serverProxy.getBatchSize());
        transport.setLittleEndian(serverProxy.isLittleEndian());

        _batchedRequests = new LinkedBlockingDeque<>(transport.getBatchSize());

        if (serverProxy.getRequestTimeout().isInfinity()) {
            _timeoutMonitor = null;
        } else {
            _timeoutMonitor = new TimeoutMonitor(
                serverProxy.getRequestTimeout());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void onTimeoutMonitoring()
    {
        synchronized (_pendingRequests) {
            final Message message = new Message(
                ModbusMessages.TIMEOUT_ON_REQUEST,
                getRemoteName().orElse(null),
                getRemoteAddress());

            getThisLogger().warn(message);
            _exception = new TimeoutException(message.toString());
            stop();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws InterruptedException, IOException
    {
        try {
            final Transport transport = getTransport();

            for (;;) {
                final Prefix prefix = transport.receivePrefix();
                final byte functionCode = transport.receiveByte();
                Transaction.Request request;

                synchronized (_pendingRequests) {
                    _busy = true;
                    request = _batchedRequests.peek();
                }

                if (request == null) {
                    throw new Transaction.FormatException(
                        ModbusMessages.UNEXPECTED_RESPONSE);
                }

                if ((functionCode & 0x7F) != request.getFunctionCode()) {
                    request.setState(Transaction.State.FAILED);

                    throw new Transaction.FormatException(
                        ModbusMessages.FUNCTION_CODE_MATCH,
                        "0x" + Integer.toHexString(functionCode & 0x7F),
                        "0x" + Integer.toHexString(request.getFunctionCode()));
                }

                switch (functionCode) {
                    case ReadCoils.FUNCTION_CODE: {
                        _processResponse(
                            new ReadCoils.Response(prefix, request),
                            transport);

                        break;
                    }
                    case ReadDiscreteInputs.FUNCTION_CODE: {
                        _processResponse(
                            new ReadDiscreteInputs.Response(prefix, request),
                            transport);

                        break;
                    }
                    case ReadHoldingRegisters.FUNCTION_CODE: {
                        _processResponse(
                            new ReadHoldingRegisters.Response(prefix, request),
                            transport);

                        break;
                    }
                    case ReadInputRegisters.FUNCTION_CODE: {
                        _processResponse(
                            new ReadInputRegisters.Response(prefix, request),
                            transport);

                        break;
                    }
                    case WriteReadMultipleRegisters.FUNCTION_CODE: {
                        _processResponse(
                            new WriteReadMultipleRegisters.Response(
                                prefix,
                                request),
                            transport);

                        break;
                    }
                    case WriteSingleCoil.FUNCTION_CODE: {
                        _processResponse(
                            new WriteSingleCoil.Response(prefix, request),
                            transport);

                        break;
                    }
                    case WriteSingleRegister.FUNCTION_CODE: {
                        _processResponse(
                            new WriteSingleRegister.Response(prefix, request),
                            transport);

                        break;
                    }
                    case WriteMultipleCoils.FUNCTION_CODE: {
                        _processResponse(
                            new WriteMultipleCoils.Response(prefix, request),
                            transport);

                        break;
                    }
                    case WriteMultipleRegisters.FUNCTION_CODE: {
                        _processResponse(
                            new WriteMultipleRegisters.Response(
                                prefix,
                                request),
                            transport);

                        break;
                    }
                    case MaskWriteRegister.FUNCTION_CODE: {
                        _processResponse(
                            new MaskWriteRegister.Response(prefix, request),
                            transport);

                        break;
                    }
                    default: {
                        if (functionCode >= 0) {
                            request.setState(Transaction.State.FAILED);

                            throw new Transaction.FormatException(
                                ModbusMessages.UNEXPECTED_FUNCTION_CODE,
                                Byte.valueOf(functionCode));
                        }

                        _processResponse(
                            new ErrorResponse(prefix, request),
                            transport);
                    }
                }

                synchronized (request) {
                    request.notifyAll();
                }

                synchronized (_pendingRequests) {
                    if (_thread.get() == null) {
                        throw new InterruptedException();
                    }

                    _deactivateTimeout();

                    transport.onMessageReceiveCompleted();
                    _batchedRequests.remove();

                    do {
                        request = _pendingRequests.poll();

                        if (request == null) {
                            break;
                        }

                        _batchedRequests.add(request);
                        _doSendRequest(request);
                    } while (_batchedRequests.remainingCapacity() > 0);

                    if (!_batchedRequests.isEmpty()) {
                        _activateTimeout();
                    }

                    _busy = false;
                }
            }
        } catch (final EOFException exception) {
            getThisLogger()
                .debug(
                    ModbusMessages.SERVER_CONNECTION_CLOSED,
                    getRemoteAddress());
            _exception = exception;
        } catch (final IOException exception) {
            if (_thread.get() == Thread.currentThread()) {
                getThisLogger().debug(BaseMessages.VERBATIM, exception);
            }

            _exception = exception;
        } catch (final Transaction.FormatException exception) {
            getThisLogger()
                .error(BaseMessages.VERBATIM, exception.getMessage());
            _exception = exception;
        } catch (final InterruptedException exception) {
            _exception = exception;

            throw exception;
        } finally {
            stop();
        }
    }

    /**
     * Sends a request.
     *
     * @param request The request.
     */
    public void sendRequest(@Nonnull final Transaction.Request request)
    {
        try {
            synchronized (_pendingRequests) {
                request.setState(Transaction.State.QUEUED);

                if (!_busy && _pendingRequests.isEmpty()) {
                    if (_batchedRequests.isEmpty()) {
                        _batchedRequests.add(request);
                        _doSendRequest(request);
                        _activateTimeout();
                    } else if (!_batchedRequests.offer(request)) {
                        _pendingRequests.add(request);
                    }
                } else {
                    _pendingRequests.add(request);
                }
            }
        } catch (final IOException exception) {
            getThisLogger()
                .warn(
                    ModbusMessages.FAILED_SEND_REQUEST,
                    getTransport().getRemoteAddress(),
                    exception);
            _exception = exception;
            stop();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        final ServiceThread thread = new ServiceThread(
            this,
            "Modbus client (receiver from " + getRemoteAddress() + ")");

        if (_thread.compareAndSet(null, thread)) {
            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
            Require
                .ignored(getListener().get().onNewConnection(getRemoteProxy()));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        final Thread thread = _thread.getAndSet(null);

        if (thread != null) {
            _deactivateTimeout();

            if (_timeoutMonitor != null) {
                TimeoutMonitor.shutdown();
            }

            if (thread != Thread.currentThread()) {
                getThisLogger()
                    .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            }

            close();

            Require
                .ignored(
                    getListener()
                        .get()
                        .onLostConnection(
                                getRemoteProxy(),
                                        Optional.ofNullable(_exception)));
            _exception = null;

            synchronized (_pendingRequests) {
                for (final Transaction.Request request: _pendingRequests) {
                    request.setState(Transaction.State.FAILED);
                }

                _pendingRequests.clear();

                for (final Transaction.Request request: _batchedRequests) {
                    request.setState(Transaction.State.FAILED);
                }

                _batchedRequests.clear();
            }

            if (thread != Thread.currentThread()) {
                try {
                    thread.join();
                } catch (final InterruptedException exception) {
                    throw new RuntimeException(exception);
                }
            }

            super.stop();
        }
    }

    private void _activateTimeout()
    {
        if (_timeoutMonitor != null) {
            _timeoutMonitor.setClient(this);
        }
    }

    private void _deactivateTimeout()
    {
        if (_timeoutMonitor != null) {
            _timeoutMonitor.clearClient();
        }
    }

    private void _doSendRequest(
            final Transaction.Request request)
        throws IOException
    {
        getThisLogger()
            .trace(ModbusMessages.SENDING_REQUEST, request.getName());

        final Transport transport = getTransport();

        try {
            transport.onMessageSendBegins();
            request.write(transport);
            transport.flush();
        } catch (final IOException exception) {
            request.setState(Transaction.State.FAILED);

            throw exception;
        }

        request.setState(Transaction.State.SENT);
    }

    private void _processResponse(
            final Transaction.Response response,
            final Transport transport)
        throws IOException, Transaction.FormatException
    {
        response.read(transport);

        if (response instanceof ErrorResponse) {
            getThisLogger()
                .warn(
                    ModbusMessages.RECEIVED_ERROR_RESPONSE,
                    ((ErrorResponse) response).getExceptionCodeName());
        } else {
            getThisLogger()
                .trace(ModbusMessages.RECEIVED_RESPONSE, response.getName());
        }

        ((ModbusServerProxy) getRemoteProxy()).putResponse(response);
    }

    private final BlockingQueue<Transaction.Request> _batchedRequests;
    private boolean _busy;
    private volatile Exception _exception;
    private final Queue<Transaction.Request> _pendingRequests =
        new LinkedList<>();
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
    private final TimeoutMonitor _timeoutMonitor;
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
