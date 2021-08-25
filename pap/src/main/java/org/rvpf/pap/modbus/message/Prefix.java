/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Prefix.java 4086 2019-06-16 16:36:41Z SFB $
 */

package org.rvpf.pap.modbus.message;

import java.io.EOFException;
import java.io.IOException;

import java.net.ProtocolException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.pap.modbus.transport.Transport;

/**
 * Prefix.
 */
@NotThreadSafe
public class Prefix
{
    /**
     * Constructs an instance.
     *
     * @param unitIdentifier The unit identifier.
     */
    public Prefix(final byte unitIdentifier)
    {
        _unitIdentifier = unitIdentifier;
    }

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected Prefix(@Nonnull final Prefix other)
    {
        _unitIdentifier = other._unitIdentifier;
    }

    /**
     * Checks the saved data length with the expected data length.
     *
     * @param expectedDataLength The expected data length.
     *
     * @return True if the lengths match.
     */
    @CheckReturnValue
    public boolean checkDataLength(final int expectedDataLength)
    {
        return true;
    }

    /**
     * Creates a copy of this.
     *
     * @return The copy.
     */
    @Nonnull
    @CheckReturnValue
    public Prefix copy()
    {
        return new Prefix(this);
    }

    /**
     * Gets the unit identifier.
     *
     * @return The unit identifier.
     */
    @CheckReturnValue
    public byte getUnitIdentifier()
    {
        return _unitIdentifier;
    }

    /**
     * Sets the data length.
     *
     * @param dataLength The data length.
     */
    public void saveDataLength(final int dataLength) {}

    /**
     * Writes to the transport.
     *
     * @param transport The transport.
     *
     * @throws IOException When appropriate.
     */
    public void write(@Nonnull final Transport transport)
        throws IOException
    {
        transport.sendByte(_unitIdentifier);
    }

    private byte _unitIdentifier;

    public static final class MBAP
        extends Prefix
    {
        /**
         * Constructs an instance.
         *
         * @param transaction A transaction identifier.
         * @param unitIdentifier The unit identifier.
         */
        public MBAP(final short transaction, final byte unitIdentifier)
        {
            super(unitIdentifier);

            _transaction = transaction;
        }

        private MBAP(final MBAP other)
        {
            super(other);

            _dataLength = other._dataLength;
        }

        /**
         * Reads from the transport.
         *
         * @param transport The transport.
         *
         * @return The instance read from the transport.
         *
         * @throws EOFException On end-of-file.
         * @throws IOException When appropriate.
         */
        @Nonnull
        @CheckReturnValue
        public static MBAP read(
                @Nonnull final Transport transport)
            throws EOFException, IOException
        {
            final short transaction = transport.receiveShort();

            if (transport.receiveShort() != 0) {
                throw new ProtocolException();
            }

            final int dataLength = (short) (transport.receiveShort() - 2);

            if ((dataLength & 0xFF00) != 0) {
                throw new ProtocolException();
            }

            final MBAP mbap = new MBAP(transaction, transport.receiveByte());

            mbap.saveDataLength(dataLength);

            return mbap;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean checkDataLength(final int expectedDataLength)
        {
            return _dataLength == expectedDataLength;
        }

        /** {@inheritDoc}
         */
        @Override
        public MBAP copy()
        {
            return new MBAP(this);
        }

        /**
         * Gets the transaction.
         *
         * @return The transaction.
         */
        @CheckReturnValue
        public short getTransaction()
        {
            return _transaction;
        }

        /** {@inheritDoc}
         */
        @Override
        public void saveDataLength(final int dataLength)
        {
            _dataLength = dataLength;
        }

        /** {@inheritDoc}
         */
        @Override
        public void write(final Transport transport)
            throws IOException
        {
            transport.sendShort(_transaction);
            transport.sendShort(0);
            transport.sendShort(2 + _dataLength);
            transport.sendByte(getUnitIdentifier());
        }

        private int _dataLength;
        private short _transaction;
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
