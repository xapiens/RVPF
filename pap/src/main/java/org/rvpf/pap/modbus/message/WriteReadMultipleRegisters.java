/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: WriteReadMultipleRegisters.java 4086 2019-06-16 16:36:41Z SFB $
 */

package org.rvpf.pap.modbus.message;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.message.Transaction.FormatException;
import org.rvpf.pap.modbus.transport.Transport;

/**
 * Write/read multiple registers.
 */
public interface WriteReadMultipleRegisters
{
    /** Write/read multiple registers. */
    short FUNCTION_CODE = 0x17;

    /** Maximum read quantity. */
    short MAXIMUM_READ_QUANTITY = 125;

    /** Maximum write quantity. */
    short MAXIMUM_WRITE_QUANTITY = 121;

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
         * @param writeStartingAddress The write starting address.
         * @param writeValues The write register values.
         * @param readStartingAddress The read starting address.
         * @param readQuantity The read quantity of registers.
         */
        public Request(
                final int writeStartingAddress,
                @Nonnull final short[] writeValues,
                final int readStartingAddress,
                final int readQuantity)
        {
            final int writeQuantity = writeValues.length;

            if ((writeQuantity < 1)
                    || (MAXIMUM_WRITE_QUANTITY < writeQuantity)) {
                throw new IllegalArgumentException();
            }

            if ((readQuantity < 1) || (MAXIMUM_READ_QUANTITY < readQuantity)) {
                throw new IllegalArgumentException();
            }

            _setWriteAddress(writeStartingAddress);
            _setWriteValues(writeValues);
            _setReadAddress(readStartingAddress);
            _setReadQuantity(readQuantity);
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
            return 4 + 5 + (getWriteValues().length * 2);
        }

        /** {@inheritDoc}
         */
        @Override
        public int getReadAddress()
        {
            return getAddress();
        }

        /** {@inheritDoc}
         */
        @Override
        public int getReadQuantity()
        {
            return getQuantity();
        }

        /** {@inheritDoc}
         */
        @Override
        public DateTime getStamp()
        {
            return _stamp;
        }

        /** {@inheritDoc}
         */
        @Override
        public int getWriteAddress()
        {
            return _address;
        }

        /** {@inheritDoc}
         */
        @Override
        public short[] getWriteValues()
        {
            return (_values != null)? _values: Transaction.NO_VALUES;
        }

        /** {@inheritDoc}
         */
        @Override
        public Request read(
                final Transport transport)
            throws IOException, FormatException
        {
            _setReadAddress(transport.receiveShort() + 1);
            _setReadQuantity(transport.receiveShort());

            _setWriteAddress(transport.receiveShort() + 1);

            final short writeQuantity = transport.receiveShort();
            final int writeByteCount = transport.receiveByte() & 0xFF;

            if (writeByteCount != (writeQuantity * 2)) {
                throw new Transaction.FormatException(
                    ModbusMessages.BYTE_COUNT_REGISTERS,
                    Integer.valueOf(writeByteCount),
                    Short.valueOf(writeQuantity));
            }

            if (!getPrefix().checkDataLength(4 + 5 + writeByteCount)) {
                throw new Transaction.FormatException(
                    ModbusMessages.MESSAGE_LENGTH_MBAP);
            }

            final short[] writeValues = new short[writeQuantity];

            for (int i = 0; i < writeQuantity; ++i) {
                writeValues[i] = transport.receiveShort();
            }

            _setWriteValues(writeValues);

            readSuffix(transport);

            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public void write(final Transport transport)
            throws IOException
        {
            final short[] writeValues = getWriteValues();

            write(4 + 5 + (writeValues.length * 2), transport);

            transport.sendShort(getReadAddress() - 1);
            transport.sendShort(getReadQuantity());
            transport.sendShort(getWriteAddress() - 1);
            transport.sendShort(writeValues.length);
            transport.sendByte(writeValues.length * 2);

            for (final short value: writeValues) {
                transport.sendShort(value);
            }

            writeSuffix(transport);
        }

        private void _setReadAddress(final int readAddress)
        {
            setAddress(readAddress);
        }

        private void _setReadQuantity(final int readQuantity)
        {
            setQuantity(readQuantity);
        }

        private void _setWriteAddress(final int writeAddress)
        {
            _address = writeAddress & 0xFFFF;
        }

        private void _setWriteValues(final short[] values)
        {
            _values = values;
        }

        private int _address;
        private final DateTime _stamp = DateTime.now();
        private short[] _values;
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
            throws IOException, FormatException
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
