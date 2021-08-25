/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: WriteTransaction.java 4086 2019-06-16 16:36:41Z SFB $
 */

package org.rvpf.pap.modbus.message;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAPWriteTransaction;

/**
 * Write transaction.
 */
public interface WriteTransaction
{
    /**
     * Request.
     */
    abstract class Request
        extends Transaction.Request
        implements PAPWriteTransaction.Request
    {
        /**
         * Constructs an instance.
         */
        protected Request() {}

        /**
         * Constructs an instance.
         *
         * @param prefix The prefix.
         */
        protected Request(@Nonnull final Prefix prefix)
        {
            super(prefix);
        }

        /**
         * Gets the address.
         *
         * @return The address.
         */
        @CheckReturnValue
        public final int getAddress()
        {
            return _address;
        }

        /**
         * Gets the point value.
         *
         * @return The point value (may be empty).
         */
        @Nonnull
        @CheckReturnValue
        public Optional<PointValue> getPointValue()
        {
            return Optional.ofNullable(_pointValue);
        }

        /** {@inheritDoc}
         */
        @Override
        public DateTime getStamp()
        {
            return _stamp;
        }

        /**
         * Gets the single value.
         *
         * @return The value.
         */
        @CheckReturnValue
        public final short getValue()
        {
            return ((_values != null) && (_values.length > 0))? _values[0]: 0;
        }

        /**
         * Gets the values.
         *
         * @return The values.
         */
        @Nonnull
        @CheckReturnValue
        public final short[] getValues()
        {
            return (_values != null)? _values: Transaction.NO_VALUES;
        }

        /** {@inheritDoc}
         */
        @Override
        public int getWriteAddress()
        {
            return getAddress();
        }

        /** {@inheritDoc}
         */
        @Override
        public short[] getWriteValues()
        {
            return getValues();
        }

        /**
         * Sets the point value.
         *
         * @param pointValue The point value.
         */
        public void setPointValue(@Nonnull final PointValue pointValue)
        {
            _pointValue = Require.notNull(pointValue);
        }

        /**
         * Sets the address.
         *
         * @param address The address.
         */
        protected void setAddress(final int address)
        {
            _address = address & 0xFFFF;
        }

        /**
         * Sets the single value.
         *
         * @param value The value.
         */
        protected void setValue(final int value)
        {
            setValues(new short[] {(short) value});
        }

        /**
         * Sets the values.
         *
         * @param values The values.
         */
        protected void setValues(@Nonnull final short[] values)
        {
            _values = values;
        }

        private int _address;
        private PointValue _pointValue;
        private final DateTime _stamp = DateTime.now();
        private short[] _values;
    }


    /**
     * Response.
     */
    abstract class Response
        extends Transaction.Response
        implements PAPWriteTransaction.Response
    {
        /**
         * Constructs an instance.
         *
         * @param request The request.
         */
        protected Response(@Nonnull final Request request)
        {
            super(request);
        }

        /**
         * Constructs an instance.
         *
         * @param prefix The prefix.
         * @param request The request.
         */
        protected Response(
                @Nonnull final Prefix prefix,
                @Nonnull final Request request)
        {
            super(prefix, request);
        }

        /** {@inheritDoc}
         */
        @Override
        public Request getRequest()
        {
            return (Request) super.getRequest();
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
