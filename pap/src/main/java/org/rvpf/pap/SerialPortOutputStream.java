/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SerialPortOutputStream.java 4102 2019-06-30 15:41:17Z SFB $
 */

package org.rvpf.pap;

import java.io.IOException;
import java.io.OutputStream;

import java.util.Arrays;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;

/**
 * Serial port output stream.
 */
public class SerialPortOutputStream
    extends OutputStream
{
    /**
     * Constructs an instance.
     *
     * @param serialPort The serial port.
     */
    public SerialPortOutputStream(@Nonnull final SerialPortWrapper serialPort)
    {
        _serialPort = serialPort;
    }

    /** {@inheritDoc}
     */
    @Override
    public void write(final int byteInt)
        throws IOException
    {
        _serialPort.write(new byte[] {(byte) byteInt});
    }

    /** {@inheritDoc}
     */
    @Override
    public void write(
            final byte[] buffer,
            final int offset,
            final int length)
        throws IOException
    {
        Require.notNull(buffer);

        if ((offset < 0)
                || (offset > buffer.length)
                || (length < 0)
                || ((offset + length) > buffer.length)
                || ((offset + length) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (length == 0) {
            return;
        }

        _serialPort.write(Arrays.copyOfRange(buffer, offset, offset + length));
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
