/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ReadHoldingRegisters.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.message;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.transport.Transport;

/**
 * Read holding registers.
 */
public interface ReadHoldingRegisters
{
    /** Read holding registers. */
    short FUNCTION_CODE = 0x03;

    /** Maximum quantity of registers. */
    short MAXIMUM_QUANTITY = 125;

    /**
     * Request.
     */
    final class Request
        extends ReadTransaction.Request
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
         * @param startingAddress The starting address.
         * @param quantity The quantity of registers.
         */
        public Request(final int startingAddress, final int quantity)
        {
            if ((quantity < 1) || (MAXIMUM_QUANTITY < quantity)) {
                throw new IllegalArgumentException();
            }

            setAddress(startingAddress);
            setQuantity(quantity);
        }

        /** {@inheritDoc}
         */
        @Override
        public Transaction.Response createResponse(
                @Nonnull final short[] values)
        {
            return initResponse(new Response(this), values);
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
            throws IOException, Transaction.FormatException
        {
            expectedBytes(4);

            setAddress(transport.receiveShort() + 1);
            setQuantity(transport.receiveShort());

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
            transport.sendShort(getQuantity());

            writeSuffix(transport);
        }
    }


    /**
     * Response.
     */
    final class Response
        extends ReadTransaction.Response
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
            super(prefix, (ReadTransaction.Request) request);
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
            return 1 + (getValues().length * 2);
        }

        /** {@inheritDoc}
         */
        @Override
        public void read(
                final Transport transport)
            throws IOException, Transaction.FormatException
        {
            final int quantity = getRequest().getQuantity();
            final int byteCount = transport.receiveByte() & 0xFF;

            if (byteCount != (quantity * 2)) {
                throw new Transaction.FormatException(
                    ModbusMessages.BYTE_COUNT_REGISTERS,
                    Integer.valueOf(byteCount),
                    Integer.valueOf(quantity));
            }

            expectedBytes(1 + byteCount);

            final short[] values = new short[quantity];

            for (int i = 0; i < quantity; ++i) {
                values[i] = transport.receiveShort();
            }

            setValues(values);

            readSuffix(transport);
        }

        /** {@inheritDoc}
         */
        @Override
        public void write(final Transport transport)
            throws IOException
        {
            final short[] values = getValues();

            write(1 + (values.length * 2), transport);

            transport.sendByte(values.length * 2);

            for (final short value: values) {
                transport.sendShort(value);
            }

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
