/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusClient.java 4086 2019-06-16 16:36:41Z SFB $
 */

package org.rvpf.pap.modbus;

import java.time.ZonedDateTime;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ConnectFailedException;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAPClient;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.PAPTransaction;
import org.rvpf.pap.modbus.message.MaskWriteRegister;
import org.rvpf.pap.modbus.message.ReadCoils;
import org.rvpf.pap.modbus.message.ReadDiscreteInputs;
import org.rvpf.pap.modbus.message.ReadHoldingRegisters;
import org.rvpf.pap.modbus.message.ReadInputRegisters;
import org.rvpf.pap.modbus.message.ReadTransaction;
import org.rvpf.pap.modbus.message.Transaction;
import org.rvpf.pap.modbus.message.WriteMultipleCoils;
import org.rvpf.pap.modbus.message.WriteMultipleRegisters;
import org.rvpf.pap.modbus.message.WriteReadMultipleRegisters;
import org.rvpf.pap.modbus.message.WriteSingleCoil;
import org.rvpf.pap.modbus.message.WriteSingleRegister;
import org.rvpf.pap.modbus.message.WriteTransaction;
import org.rvpf.pap.modbus.register.Register;

/**
 * Modbus client.
 */
public final class ModbusClient
    extends PAPClient.Abstract
{
    /**
     * Constructs an instance
     *
     * @param clientContext The Modbus client context.
     */
    public ModbusClient(@Nonnull final ModbusClientContext clientContext)
    {
        super(clientContext);
    }

    /**
     * Converts a TIME value to a DateTime.
     *
     * @param words The TIME value in 4 Modbus words.
     *
     * @return The DateTime.
     */
    @Nonnull
    @CheckReturnValue
    public static DateTime dateTime(@Nonnull final short[] words)
    {
        final ZonedDateTime zonedDateTime = ZonedDateTime
            .of(
                (words[0] >> 8) + 2000,
                words[0] & 0xFF,
                words[1] >> 8,
                words[1] & 0xFF,
                0,
                0,
                0,
                DateTime.getZoneId());

        return DateTime
            .fromZonedDateTime(zonedDateTime)
            .after(words[2] * ElapsedTime.SECOND.toRaw())
            .after(words[3] * (ElapsedTime.MILLI.toRaw() / 10));
    }

    /**
     * Gets the first word of a stamp value.
     *
     * @param stamp The time stamp for the value.
     *
     * @return The first word of a stamp value.
     */
    @CheckReturnValue
    public static short stamp0(@Nonnull final DateTime stamp)
    {
        return (short) (stamp.scaled(ElapsedTime.SECOND) % 3600);
    }

    /**
     * Gets the second word of a stamp value.
     *
     * @param stamp The time stamp for the value.
     *
     * @return The second word of a stamp value.
     */
    @CheckReturnValue
    public static short stamp1(@Nonnull final DateTime stamp)
    {
        return (short) (stamp.scaled(ElapsedTime.MILLI.toRaw() / 10) % 10000);
    }

    /**
     * Clears the completed requests (with or without success).
     *
     * @param origin The origin representing the server.
     *
     * @return The completed requests (empty when not connected).
     */
    @Nonnull
    @CheckReturnValue
    public Collection<Transaction.Request> clearCompletedRequests(
            @Nonnull final Origin origin)
    {
        final Optional<? extends PAPProxy> serverProxy = getServerProxy(origin);

        return serverProxy
            .isPresent()? ((ModbusServerProxy) serverProxy.get())
                .clearCompletedRequests(): Collections.emptyList();
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        disconnect();

        super.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean connect(final Origin origin)
    {
        final Optional<? extends PAPProxy> serverProxy = getServerProxy(origin);

        if (!serverProxy.isPresent()) {
            return false;
        }

        return ((ModbusServerProxy) serverProxy.get())
            .connect(true)
            .isPresent();
    }

    /** {@inheritDoc}
     */
    @Override
    public void disconnect(final Origin origin)
    {
        final Optional<PAPProxy> serverProxy = forgetServerProxy(origin);

        if (serverProxy.isPresent()) {
            serverProxy.get().disconnect();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] fetchPointValues(
            final Point[] points)
        throws InterruptedException, ServiceNotAvailableException
    {
        final ReadTransaction.Request[] requests =
            new ReadTransaction.Request[points.length];
        int i = 0;

        for (final Point point: points) {
            requests[i++] = requestPointValue(point).get();
        }

        if (!waitForResponses()) {
            throw new ServiceNotAvailableException();
        }

        final PointValue[] pointValues = new PointValue[points.length];

        for (i = 0; i < pointValues.length; ++i) {
            final ReadTransaction.Request request = requests[i];
            final Optional<? extends PAPTransaction.Response> response = request
                .getResponse();

            if (!response.isPresent()) {
                throw new ServiceNotAvailableException();
            }

            if (response.get().isSuccess()) {
                pointValues[i] = ((ReadTransaction.Response) response.get())
                    .getPointValue()
                    .get();
            } else {
                pointValues[i] = null;
            }
        }

        return pointValues;
    }

    /**
     * Mask writes a register.
     *
     * @param serverOrigin The origin representing the server.
     * @param registerAddress The register address.
     * @param andMask The AND mask.
     * @param orMask The OR mask.
     *
     * @return The request sent (empty when the origin server proxy is unknown).
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> maskWriteRegister(
            @Nonnull final Origin serverOrigin,
            final int registerAddress,
            final int andMask,
            final int orMask)
        throws ConnectFailedException
    {
        return _sendRequest(
            serverOrigin,
            new MaskWriteRegister.Request(registerAddress, andMask, orMask));
    }

    /** {@inheritDoc}
     */
    @Override
    public void open() {}

    /**
     * Reads coils.
     *
     * @param serverOrigin The origin representing the server.
     * @param startingAddress The starting address.
     * @param quantityOfCoils The quantity of coils.
     *
     * @return The request sent (empty when the origin server proxy is unknown).
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> readCoils(
            @Nonnull final Origin serverOrigin,
            final int startingAddress,
            final int quantityOfCoils)
        throws ConnectFailedException
    {
        return _sendRequest(
            serverOrigin,
            new ReadCoils.Request(startingAddress, quantityOfCoils));
    }

    /**
     * Reads discrete inputs.
     *
     * @param serverOrigin The origin representing the server.
     * @param startingAddress The starting address.
     * @param quantityOfInputs The quantity of inputs.
     *
     * @return The request sent (empty when the origin server proxy is unknown).
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> readDiscreteInputs(
            @Nonnull final Origin serverOrigin,
            final int startingAddress,
            final int quantityOfInputs)
        throws ConnectFailedException
    {
        return _sendRequest(
            serverOrigin,
            new ReadDiscreteInputs.Request(startingAddress, quantityOfInputs));
    }

    /**
     * Reads holding registers.
     *
     * @param serverOrigin The origin representing the server.
     * @param startingAddress The starting address.
     * @param quantityOfRegisters The quantity of registers.
     *
     * @return The request sent (empty when the origin server proxy is unknown).
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> readHoldingRegisters(
            @Nonnull final Origin serverOrigin,
            final int startingAddress,
            final int quantityOfRegisters)
        throws ConnectFailedException
    {
        return _sendRequest(
            serverOrigin,
            new ReadHoldingRegisters.Request(
                startingAddress,
                quantityOfRegisters));
    }

    /**
     * Reads input registers.
     *
     * @param serverOrigin The origin representing the server.
     * @param startingAddress The starting address.
     * @param quantityOfInputs The quantity of inputs.
     *
     * @return The request sent (empty when the origin server proxy is unknown).
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> readInputRegisters(
            @Nonnull final Origin serverOrigin,
            final int startingAddress,
            final int quantityOfInputs)
        throws ConnectFailedException
    {
        return _sendRequest(
            serverOrigin,
            new ReadInputRegisters.Request(startingAddress, quantityOfInputs));
    }

    /**
     * Requests a point value.
     *
     * @param point The point.
     *
     * @return The optional request sent.
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<ReadTransaction.Request> requestPointValue(
            @Nonnull final Point point)
        throws ConnectFailedException
    {
        final Optional<? extends PAPProxy> serverProxy = getServerProxy(
            point.getOrigin().get());
        final Optional<Register> register = serverProxy
            .isPresent()? ((ModbusServerProxy) serverProxy.get())
                .getRegister(point): Optional.empty();

        if (!register.isPresent()) {
            return Optional.empty();
        }

        final ReadTransaction.Request request = register
            .get()
            .createReadRequest();

        request.setPoint(point);

        return Optional
            .ofNullable(
                (ReadTransaction.Request) ((ModbusServerProxy) serverProxy.get())
                    .sendRequest(request));
    }

    /** {@inheritDoc}
     */
    @Override
    public Exception[] updatePointValues(
            final PointValue[] pointValues)
        throws ConnectFailedException, InterruptedException
    {
        final Exception[] exceptions = new Exception[pointValues.length];
        final Transaction.Request[] requests =
            new Transaction.Request[pointValues.length];

        for (int i = 0; i < pointValues.length; i++) {
            try {
                requests[i] = writePointValue(pointValues[i]).orElse(null);
            } catch (final ConnectFailedException exception) {
                exceptions[i] = new ServiceNotAvailableException(exception);

                continue;
            }

            if (requests[i] == null) {
                exceptions[i] = new ServiceNotAvailableException();
            }
        }

        for (int i = 0; i < pointValues.length; i++) {
            final Transaction.Request request = requests[i];

            if (request == null) {
                continue;
            }

            final Optional<? extends PAPTransaction.Response> response;

            response = request.getResponse();

            if (!response.isPresent() || !response.get().isSuccess()) {
                exceptions[i] = new Exception();

                continue;
            }
        }

        return exceptions;
    }

    /**
     * Wait for a response to each request.
     *
     * @param origin The origin representing the server.
     *
     * @return True unless a request has failed.
     *
     * @throws InterruptedException When interrupted.
     * @throws ConnectFailedException When connect failed.
     */
    @CheckReturnValue
    public boolean waitForResponse(
            @Nonnull final Origin origin)
        throws InterruptedException, ConnectFailedException
    {
        final Optional<? extends PAPProxy> serverProxy = getServerProxy(origin);

        return serverProxy
            .isPresent()? ((ModbusServerProxy) serverProxy.get())
                .waitForResponses(): false;
    }

    /**
     * Wait for a response to each request.
     *
     * @return True unless a request has failed.
     *
     * @throws InterruptedException When interrupted.
     * @throws ConnectFailedException When connect failed.
     */
    @CheckReturnValue
    public boolean waitForResponses()
        throws InterruptedException, ConnectFailedException
    {
        final Collection<PAPProxy> serverProxies = getKnownServerProxies();

        boolean success = true;

        for (final PAPProxy serverProxy: serverProxies) {
            success &= ((ModbusServerProxy) serverProxy).waitForResponses();
        }

        return success;
    }

    /**
     * Writes multiple coils.
     *
     * @param serverOrigin The origin representing the server.
     * @param startingAddress The starting address.
     * @param outputValues The output values.
     *
     * @return The request sent (empty when the origin server proxy is unknown).
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> writeMultipleCoils(
            @Nonnull final Origin serverOrigin,
            final int startingAddress,
            @Nonnull final short[] outputValues)
        throws ConnectFailedException
    {
        return _sendRequest(
            serverOrigin,
            new WriteMultipleCoils.Request(startingAddress, outputValues));
    }

    /**
     * Writes multiple registers.
     *
     * @param serverOrigin The origin representing the server.
     * @param startingAddress The starting address.
     * @param registerValues The register values.
     *
     * @return The request sent (empty when the origin server proxy is unknown).
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> writeMultipleRegisters(
            @Nonnull final Origin serverOrigin,
            final int startingAddress,
            @Nonnull final short[] registerValues)
        throws ConnectFailedException
    {
        return _sendRequest(
            serverOrigin,
            new WriteMultipleRegisters.Request(
                startingAddress,
                registerValues));
    }

    /**
     * Writes a point value.
     *
     * @param pointValue The point value.
     *
     * @return The request sent.
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> writePointValue(
            @Nonnull final PointValue pointValue)
        throws ConnectFailedException
    {
        final Point point = pointValue.getPoint().get();
        final Optional<? extends PAPProxy> serverProxy = getServerProxy(
            point.getOrigin().get());
        final Optional<Register> register = serverProxy
            .isPresent()? ((ModbusServerProxy) serverProxy.get())
                .getRegister(point): Optional.empty();

        if (!register.isPresent()) {
            return Optional.empty();
        }

        if (register.get().isReadOnly()) {
            getThisLogger().warn(ModbusMessages.READ_ONLY_REGISTER);

            return Optional.empty();
        }

        register.get().putPointValue(pointValue);

        final WriteTransaction.Request request = register
            .get()
            .createWriteRequest();

        request.setPointValue(pointValue);

        return Optional
            .ofNullable(
                ((ModbusServerProxy) serverProxy.get()).sendRequest(request));
    }

    /**
     * Writes/reads multiple registers.
     *
     * @param serverOrigin The origin representing the server.
     * @param writeStartingAddress The write starting address.
     * @param writeRegisterValues The write register values.
     * @param readStartingAddress The read starting address.
     * @param readQuantityOfregisters The read quantity of registers.
     *
     * @return The request sent (empty when the origin server proxy is unknown).
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> writeReadMultipleRegisters(
            @Nonnull final Origin serverOrigin,
            final int writeStartingAddress,
            @Nonnull final short[] writeRegisterValues,
            final int readStartingAddress,
            final int readQuantityOfregisters)
        throws ConnectFailedException
    {
        return _sendRequest(
            serverOrigin,
            new WriteReadMultipleRegisters.Request(
                writeStartingAddress,
                writeRegisterValues,
                readStartingAddress,
                readQuantityOfregisters));
    }

    /**
     * Writes a single coil.
     *
     * @param serverOrigin The origin representing the server.
     * @param outputAddress The output address.
     * @param outputValue The output value.
     *
     * @return The request sent (empty when the origin server proxy is unknown).
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> writeSingleCoil(
            @Nonnull final Origin serverOrigin,
            final int outputAddress,
            final int outputValue)
        throws ConnectFailedException
    {
        return _sendRequest(
            serverOrigin,
            new WriteSingleCoil.Request(outputAddress, outputValue));
    }

    /**
     * Writes a single register.
     *
     * @param serverOrigin The origin representing the server.
     * @param registerAddress The register address.
     * @param registerValue The register value.
     *
     * @return The request sent (empty when the origin server proxy is unknown).
     *
     * @throws ConnectFailedException When connect failed.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Transaction.Request> writeSingleRegister(
            @Nonnull final Origin serverOrigin,
            final int registerAddress,
            final int registerValue)
        throws ConnectFailedException
    {
        return _sendRequest(
            serverOrigin,
            new WriteSingleRegister.Request(registerAddress, registerValue));
    }

    private Optional<Transaction.Request> _sendRequest(
            final Origin origin,
            final Transaction.Request request)
        throws ConnectFailedException
    {
        final Optional<? extends PAPProxy> serverProxy = getServerProxy(origin);

        if (!serverProxy.isPresent()) {
            return Optional.empty();
        }

        ((ModbusServerProxy) serverProxy.get()).sendRequest(request);

        return Optional.of(request);
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
