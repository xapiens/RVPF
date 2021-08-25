/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ReadTransaction.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.message;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.PAPReadTransaction;
import org.rvpf.pap.modbus.register.Register;

/**
 * Read transaction.
 */
public interface ReadTransaction
{
    /**
     * Request.
     */
    abstract class Request
        extends Transaction.Request
        implements PAPReadTransaction.Request
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
        public final int getAddress()
        {
            return _address;
        }

        /** {@inheritDoc}
         */
        @Override
        public Point getPoint()
        {
            return Require.notNull(_point);
        }

        /**
         * Gets the quantity.
         *
         * @return The quantity.
         */
        @CheckReturnValue
        public int getQuantity()
        {
            return _quantity;
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

        /**
         * Sets the point.
         *
         * @param point The point.
         */
        public void setPoint(@Nonnull final Point point)
        {
            _point = point;
        }

        /**
         * Sets the quantity.
         *
         * @param quantity The quantity.
         */
        public void setQuantity(final int quantity)
        {
            _quantity = quantity & 0xFFFF;
        }

        /**
         * Initialize a response.
         *
         * @param response The response.
         * @param values The response values.
         *
         * @return The response.
         */
        protected Response initResponse(
                @Nonnull final Response response,
                @Nonnull final short[] values)
        {
            if (values.length != getQuantity()) {
                throw new IllegalArgumentException();
            }

            response.setValues(values);

            return response;
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

        private int _address;
        private Point _point;
        private int _quantity;
    }


    /**
     * Response.
     */
    abstract class Response
        extends Transaction.Response
        implements PAPReadTransaction.Response
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
        public Optional<PointValue> getPointValue()
        {
            if (!isSuccess()) {
                return Optional.empty();
            }

            final Optional<Register> register = getRequest()
                .getServerProxy()
                .getRegister(getRequest().getPoint());

            if (!register.isPresent()) {
                return Optional.empty();
            }

            register.get().setContents(getValues());

            final PointValue[] pointValues = register.get().getPointValues();

            if (pointValues.length != 1) {
                return Optional.empty();
            }

            final PointValue pointValue = pointValues[0];

            pointValue.setStamp(getStamp());

            return Optional.of(pointValue);
        }

        /** {@inheritDoc}
         */
        @Override
        public Request getRequest()
        {
            return (Request) Require.notNull(super.getRequest());
        }

        /**
         * Gets the date-time stamp of the response.
         *
         * @return The stamp.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime getStamp()
        {
            return _stamp;
        }

        /**
         * Gets the values.
         *
         * @return The values.
         */
        public final short[] getValues()
        {
            return _values;
        }

        /**
         * Sets the values.
         *
         * @param values The values.
         */
        protected void setValues(final short[] values)
        {
            _values = values;
        }

        private final DateTime _stamp = DateTime.now();
        private short[] _values;
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
