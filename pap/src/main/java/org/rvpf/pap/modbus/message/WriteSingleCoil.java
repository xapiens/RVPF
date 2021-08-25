/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: WriteSingleCoil.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.pap.modbus.message;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.message.Transaction.FormatException;
import org.rvpf.pap.modbus.transport.ExceptionCode;
import org.rvpf.pap.modbus.transport.Transport;

/**
 * Write single coil.
 */
public interface WriteSingleCoil
{
    /** Write single coil. */
    short FUNCTION_CODE = 0x05;

    /**
     * Request.
     */
    final class Request
        extends WriteTransaction.Request
    {
        /**
         * Constructs an instance.
         *
         * @param prefix The prefix.
         */
        public Request(@Nonnull final Prefix prefix)
        {
            super(prefix);
        }

        /**
         * Constructs an instance.
         *
         * @param outputAddress The output address.
         * @param outputValue The output value.
         */
        public Request(final int outputAddress, final int outputValue)
        {
            setAddress(outputAddress);
            setValue((outputValue != 0)? 0xFF00: 0);
        }

        /** {@inheritDoc}
         */
        @Override
        public Transaction.Response createResponse(final short[] values)
        {
            Require.success(values.length == 0);

            return isValid()? new Response(this): errorResponse();
        }

        /** {@inheritDoc}
         */
        @Override
        public byte getFunctionCode()
        {
            return FUNCTION_CODE;
        }

        /** {@inheritDoc}
         */
        @Override
        public int getLength()
        {
            return 4;
        }

        /** {@inheritDoc}
         */
        @Override
        public Request read(
                final Transport transport)
            throws IOException, FormatException
        {
            setAddress(transport.receiveShort() + 1);

            final short value = transport.receiveShort();

            if ((value != 0) && (value != (short) 0xFF00)) {
                setExceptionCode(ExceptionCode.ILLEGAL_DATA_VALUE);
            }

            setValue(value);

            readSuffix(transport);

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public void write(final Transport transport)
            throws IOException
        {
            write(getLength(), transport);

            transport.sendShort(getAddress() - 1);
            transport.sendShort((getValue() != 0)? 0xFF00: 0);

            writeSuffix(transport);
        }
    }


    /**
     * Response.
     */
    final class Response
        extends WriteTransaction.Response
    {
        /**
         * Constructs an instance.
         *
         * @param prefix The prefix.
         * @param request The request.
         */
        public Response(
                @Nonnull final Prefix prefix,
                @Nonnull final Transaction.Request request)
        {
            super(prefix, (WriteTransaction.Request) request);
        }

        /**
         * Constructs an instance.
         *
         * @param request The request.
         */
        Response(@Nonnull final Request request)
        {
            super(request);
        }

        /** {@inheritDoc}
         */
        @Override
        public int getLength()
        {
            return 4;
        }

        /** {@inheritDoc}
         */
        @Override
        public void read(
                final Transport transport)
            throws IOException, Transaction.FormatException
        {
            final int address = (transport.receiveShort() & 0xFFFF) + 1;
            final short value = transport.receiveShort();

            if (address != getRequest().getAddress()) {
                throw new Transaction.FormatException(
                    ModbusMessages.OUTPUT_ADDRESS_MATCH);
            }

            if (value != getRequest().getValue()) {
                throw new Transaction.FormatException(
                    ModbusMessages.OUTPUT_VALUE_MATCH);
            }

            readSuffix(transport);
        }

        /** {@inheritDoc}
         */
        @Override
        public void write(final Transport transport)
            throws IOException
        {
            write(getLength(), transport);

            transport.sendShort(getRequest().getAddress() - 1);
            transport.sendShort(getRequest().getValue());

            writeSuffix(transport);
        }
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
