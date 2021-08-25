/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: WriteMultipleCoils.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.pap.modbus.message;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.transport.Transport;

/**
 * Write multiple coils.
 */
public interface WriteMultipleCoils
{
    /** Write multiple coils. */
    short FUNCTION_CODE = 0x0F;

    /** Maximum quantity of coils. */
    short MAXIMUM_QUANTITY = 1968;

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
         * Constructs an insstance.
         *
         * @param startingAddress The starting address.
         * @param outputValues The output values.
         */
        public Request(
                final int startingAddress,
                @Nonnull final short[] outputValues)
        {
            final int quantity = outputValues.length;

            if ((quantity < 1) || (MAXIMUM_QUANTITY < quantity)) {
                throw new IllegalArgumentException();
            }

            setAddress(startingAddress);
            setValues(outputValues);
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
            return 5 + _byteCount(getValues().length);
        }

        /** {@inheritDoc}
         */
        @Override
        public Request read(
                final Transport transport)
            throws IOException, Transaction.FormatException
        {
            setAddress(transport.receiveShort() + 1);

            final int quantity = transport.receiveShort() & 0xFFFF;
            final int byteCount = transport.receiveByte() & 0xFF;

            if (byteCount != _byteCount(quantity)) {
                throw new Transaction.FormatException(
                    ModbusMessages.BYTE_COUNT_OUTPUTS,
                    String.valueOf(byteCount),
                    Short.valueOf((short) quantity));
            }

            if (!getPrefix().checkDataLength(5 + byteCount)) {
                throw new Transaction.FormatException(
                    ModbusMessages.MESSAGE_LENGTH_MBAP);
            }

            final short[] values = new short[quantity];
            int index = 0;

            for (int i = 0; i < byteCount; ++i) {
                final byte value = transport.receiveByte();
                int shift = Math.min(8, quantity - index);

                for (--shift; shift >= 0; --shift) {
                    values[index] = (short) ((value >> shift) & 1);

                    if (++index >= quantity) {
                        break;
                    }
                }
            }

            setValues(values);

            readSuffix(transport);

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public void write(final Transport transport)
            throws IOException
        {
            final short[] values = getValues();
            final int quantity = values.length;
            final int byteCount = _byteCount(quantity);

            write(5 + byteCount, transport);

            transport.sendShort(getAddress() - 1);
            transport.sendShort(quantity);
            transport.sendByte(byteCount);

            int index = 0;

            for (int i = 0; i < byteCount; ++i) {
                int shift = Math.min(8, quantity - index);
                byte valueByte = 0;

                for (--shift; shift >= 0; --shift) {
                    final int value = (values[index] != 0)? 1: 0;

                    valueByte |= value << shift;

                    if (++index >= quantity) {
                        break;
                    }
                }

                transport.sendByte(valueByte);
            }

            writeSuffix(transport);
        }

        private static int _byteCount(final int quantity)
        {
            int byteCount = quantity / 8;

            if ((quantity % 8) != 0) {
                ++byteCount;
            }

            return byteCount;
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
            final short quantity = transport.receiveShort();

            if (address != getRequest().getAddress()) {
                throw new Transaction.FormatException(
                    ModbusMessages.STARTING_ADDRESS_MATCH);
            }

            if (quantity != getRequest().getValues().length) {
                throw new Transaction.FormatException(
                    ModbusMessages.QUANTITY_OUTPUTS_MATCH);
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
            transport.sendShort(getRequest().getValues().length);

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
