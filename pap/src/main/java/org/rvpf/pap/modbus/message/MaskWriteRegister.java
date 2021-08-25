/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MaskWriteRegister.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.pap.modbus.message;

import java.io.IOException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.message.Transaction.FormatException;
import org.rvpf.pap.modbus.transport.Transport;

/**
 * Mask write register.
 */
public interface MaskWriteRegister
{
    /** Mask write register. */
    short FUNCTION_CODE = 0x16;

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
         * Constructs a request.
         *
         * @param registerAddress The register address.
         * @param andMask The AND mask.
         * @param orMask The OR mask.
         */
        public Request(
                final int registerAddress,
                final int andMask,
                final int orMask)
        {
            setAddress(registerAddress);
            _andMask = andMask & 0xFFFF;
            _orMask = orMask & 0xFFFF;
        }

        /**
         * Applies the masks to the supplied value.
         *
         * @param value The value.
         */
        public void applyMasks(int value)
        {
            value &= _andMask;
            value |= _orMask & ~_andMask;
            setValue(value);
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
            return 6;
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
            return 1;
        }

        /** {@inheritDoc}
         */
        @Override
        public Request read(
                final Transport transport)
            throws IOException, FormatException
        {
            setAddress(transport.receiveShort() + 1);
            _andMask = transport.receiveShort() & 0xFFFF;
            _orMask = transport.receiveShort() & 0xFFFF;

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
            transport.sendShort(_andMask);
            transport.sendShort(_orMask);

            writeSuffix(transport);
        }

        /**
         * Gets the AND mask.
         *
         * @return The AND mask.
         */
        @CheckReturnValue
        int getAndMask()
        {
            return _andMask;
        }

        /**
         * Gets the OR mask.
         *
         * @return The OR mask.
         */
        @CheckReturnValue
        int getOrMask()
        {
            return _orMask;
        }

        private int _andMask;
        private int _orMask;
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
        Response(final Request request)
        {
            super(request);
        }

        /** {@inheritDoc}
         */
        @Override
        public int getLength()
        {
            return 6;
        }

        /** {@inheritDoc}
         */
        @Override
        public void read(
                final Transport transport)
            throws IOException, FormatException
        {
            final int address = (transport.receiveShort() & 0xFFFF) + 1;
            final int andMask = transport.receiveShort() & 0xFFFF;
            final int orMask = transport.receiveShort() & 0xFFFF;
            final Request request = (Request) getRequest();

            if (address != request.getAddress()) {
                throw new Transaction.FormatException(
                    ModbusMessages.REGISTER_ADDRESS_MATCH);
            }

            if (andMask != request.getAndMask()) {
                throw new Transaction.FormatException(
                    ModbusMessages.REGISTER_AND_MASK_MATCH);
            }

            if (orMask != request.getOrMask()) {
                throw new Transaction.FormatException(
                    ModbusMessages.REGISTER_OR_MASK_MATCH);
            }

            readSuffix(transport);
        }

        /** {@inheritDoc}
         */
        @Override
        public void write(final Transport transport)
            throws IOException
        {
            super.write(getLength(), transport);

            final Request request = (Request) getRequest();

            transport.sendShort(request.getAddress() - 1);
            transport.sendShort(request.getAndMask());
            transport.sendShort(request.getOrMask());

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
