/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SerialConnection.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.pap.dnp3.transport;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.SerialPortWrapper;

/**
 * Serial connection.
 */
public final class SerialConnection
    extends Connection
{
    /**
     * Constructs an instance.
     *
     * @param localEndPoint The local end point.
     * @param remoteEndPoint The remote end point.
     * @param portName The port name.
     * @param portSpeed The port speed.
     */
    public SerialConnection(
            @Nonnull final LocalEndPoint localEndPoint,
            @Nonnull final RemoteEndPoint remoteEndPoint,
            @Nonnull final String portName,
            final int portSpeed)
    {
        super(localEndPoint, remoteEndPoint);

        _serialPort = SerialPortWrapper
            .newBuilder()
            .setPortName(portName)
            .setPortDataBits(8)
            .setPortParity(SerialPortWrapper.Builder.PARITY_NAME_NONE)
            .setPortControl(true)
            .setPortSpeed(portSpeed)
            .build();
    }

    /** {@inheritDoc}
     */
    @Override
    public void doClose()
        throws IOException
    {
        _serialPort.close();

        super.doClose();
    }

    /**
     * Purges the port.
     *
     * @throws IOException On I/O error.
     */
    public void purge()
        throws IOException
    {
        _serialPort.open();
        _serialPort.purge();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return _serialPort.getPortName();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doReceive(final ByteBuffer buffer)
        throws IOException
    {
        Require.success(buffer.hasRemaining());

        final byte[] bytes = _serialPort.read(buffer.remaining());

        if (bytes == null) {
            throw new ClosedChannelException();
        }

        buffer.put(bytes);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doSend(final ByteBuffer buffer)
        throws IOException
    {
        Require.success(buffer.hasRemaining());

        final byte[] bytes = new byte[buffer.remaining()];

        buffer.get(bytes);
        _serialPort.write(bytes);
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getName()
    {
        return _serialPort.getPortName();
    }

    private final SerialPortWrapper _serialPort;
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
