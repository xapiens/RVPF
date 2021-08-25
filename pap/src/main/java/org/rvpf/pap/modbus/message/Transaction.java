/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Transaction.java 4098 2019-06-25 16:46:46Z SFB $
 */

package org.rvpf.pap.modbus.message;

import java.io.IOException;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.exception.ConnectFailedException;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;
import org.rvpf.pap.PAPTransaction;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.ModbusServerProxy;
import org.rvpf.pap.modbus.transport.ExceptionCode;
import org.rvpf.pap.modbus.transport.Transport;

/**
 * Transaction.
 */
public interface Transaction
{
    /** No values. */
    short[] NO_VALUES = new short[0];

    /**
     * State.
     */
    enum State
    {
        INACTIVE,    // Initial and final state.
        ACTIVE,    // Initial processing or reprocessing.
        QUEUED,    // Waiting to be sent.
        SENT,    // Sent.
        ANSWERED,    // Answered.
        FAILED,    // Failed.
    }

    final class FormatException
        extends Exception
    {
        /**
         * Constructs an instance.
         *
         * @param entry The messages entry.
         * @param params The message parameters.
         */
        public FormatException(
                @Nonnull final Messages.Entry entry,
                @Nonnull final Object... params)
        {
            super(Message.format(entry, params));
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Request.
     */
    abstract class Request
        implements PAPTransaction.Request
    {
        /**
         * Constructs an instance.
         */
        protected Request()
        {
            _state = State.INACTIVE;
        }

        /**
         * Constructs an instance.
         *
         * @param prefix The prefix.
         */
        protected Request(@Nonnull final Prefix prefix)
        {
            _state = State.ACTIVE;
            _prefix = prefix;
        }

        /**
         * Creates a response.
         *
         * @param values The response values.
         *
         * @return The response.
         */
        @Nonnull
        @CheckReturnValue
        public abstract Transaction.Response createResponse(
                @Nonnull short[] values);

        /**
         * Returns an error response.
         *
         * @return The error response.
         */
        @Nonnull
        @CheckReturnValue
        public final ErrorResponse errorResponse()
        {
            return new ErrorResponse(this);
        }

        /**
         * Gets the exception code.
         *
         * @return exceptionCode The exception code.
         */
        @CheckReturnValue
        public final byte getExceptionCode()
        {
            return _exceptionCode;
        }

        /**
         * Gets the function code.
         *
         * @return The function code.
         */
        @CheckReturnValue
        public abstract byte getFunctionCode();

        /**
         * Gets the length of this response.
         *
         * @return The length.
         */
        @CheckReturnValue
        public abstract int getLength();

        /**
         * Gets a name for the request.
         *
         * @return The name.
         */
        @Nonnull
        @CheckReturnValue
        public final String getName()
        {
            return getClass().getEnclosingClass().getSimpleName();
        }

        /**
         * Gets the prefix.
         *
         * @return The prefix.
         */
        @Nonnull
        @CheckReturnValue
        public final Prefix getPrefix()
        {
            return Require.notNull(_prefix);
        }

        /**
         * Gets the read address.
         *
         * @return The read address.
         */
        @CheckReturnValue
        public int getReadAddress()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Gets the read quantity.
         *
         * @return The read quantity.
         */
        @CheckReturnValue
        public int getReadQuantity()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<? extends PAPTransaction.Response> getResponse()
            throws InterruptedException, ConnectFailedException
        {
            return getServerProxy().getResponse(this);
        }

        /**
         * Gets the number of retries.
         *
         * @return The number of retries.
         */
        @CheckReturnValue
        public final int getRetries()
        {
            return _retries;
        }

        /**
         * Gets the server proxy.
         *
         * @return The server proxy.
         */
        @Nonnull
        @CheckReturnValue
        public final ModbusServerProxy getServerProxy()
        {
            return Require.notNull(_serverProxy);
        }

        /**
         * Gets the date-time stamp of the request.
         *
         * @return The stamp.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime getStamp()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Gets the state.
         *
         * @return The state.
         */
        @Nonnull
        @CheckReturnValue
        public final State getState()
        {
            return _state;
        }

        /**
         * Gets the write address.
         *
         * @return The write address.
         */
        @CheckReturnValue
        public int getWriteAddress()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Gets the write values.
         *
         * @return The write values.
         */
        @Nonnull
        @CheckReturnValue
        public short[] getWriteValues()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Asks if this request has been answered.
         *
         * @return True if it has been answered.
         */
        @CheckReturnValue
        public final boolean hasBeenAnswered()
        {
            return _state == State.ANSWERED;
        }

        /**
         * Asks if this request has failed.
         *
         * @return True if this request has failed.
         */
        @CheckReturnValue
        public final boolean hasFailed()
        {
            return _state == State.FAILED;
        }

        /**
         * Asks if this request is valid.
         *
         * @return True when valid.
         */
        @CheckReturnValue
        public final boolean isValid()
        {
            return _exceptionCode == 0;
        }

        /**
         * Reads from the transport.
         *
         * @param transport The transport.
         *
         * @return This request.
         *
         * @throws IOException When appropriate.
         * @throws FormatException On transaction format exception.
         */
        @Nonnull
        public abstract Request read(
                @Nonnull Transport transport)
            throws IOException, FormatException;

        /**
         * Sets the exception code.
         *
         * @param exceptionCode The exception code.
         */
        public final void setExceptionCode(
                @Nonnull final ExceptionCode exceptionCode)
        {
            _exceptionCode = exceptionCode.getCodeByte();
        }

        /**
         * Sets the server proxy.
         *
         * @param serverProxy The server proxy.
         */
        public final void setServerProxy(
                @Nonnull final ModbusServerProxy serverProxy)
        {
            _serverProxy = serverProxy;
        }

        /**
         * Sets the state.
         *
         * @param state The state.
         */
        public final void setState(@Nonnull final State state)
        {
            _state = state;

            synchronized (this) {
                this.notifyAll();
            }
        }

        /**
         * Updates the number of retries.
         *
         * @return True while inside the limit.
         */
        @CheckReturnValue
        public boolean updateRetries()
        {
            Require.success(getState() == State.FAILED);

            final int retries = _retries;

            if (retries >= getServerProxy().getRequestRetries()) {
                return false;
            }

            _retries = retries + 1;

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public final boolean waitForResponse()
            throws InterruptedException, ConnectFailedException
        {
            return getServerProxy().waitForResponse(this);
        }

        /**
         * Writes to the transport.
         *
         * @param transport The transport.
         *
         * @throws IOException When appropriate.
         */
        public abstract void write(
                @Nonnull Transport transport)
            throws IOException;

        /**
         * Reads the suffix.
         *
         * @param transport The transport.
         *
         * @throws IOException When appropriate.
         * @throws FormatException On bad suffix.
         */
        protected static void readSuffix(
                @Nonnull final Transport transport)
            throws IOException, FormatException
        {
            if (!transport.receiveSuffix()) {
                throw new Transaction.FormatException(
                    ModbusMessages.BAD_TRANSPORT_SUFFIX);
            }
        }

        /**
         * Writes the suffix.
         *
         * @param transport The transport.
         *
         * @throws IOException When appropriate.
         */
        protected static void writeSuffix(
                @Nonnull final Transport transport)
            throws IOException
        {
            transport.sendSuffix();
        }

        /**
         * Specifies the expected number of bytes.
         *
         * @param bytes The expected number of bytes.
         *
         * @throws FormatException When it does not match the prefix.
         */
        protected final void expectedBytes(
                final int bytes)
            throws FormatException
        {
            if (!getPrefix().checkDataLength(bytes)) {
                throw new FormatException(ModbusMessages.MESSAGE_LENGTH_MBAP);
            }
        }

        /**
         * Writes to the transport.
         *
         * @param length The response length (excluding the function code).
         * @param transport The transport.
         *
         * @throws IOException When appropriate.
         */
        protected final void write(
                final int length,
                @Nonnull final Transport transport)
            throws IOException
        {
            if (_prefix == null) {
                _prefix = transport.newPrefix();
            }

            _prefix.saveDataLength(length);
            transport.sendPrefix(_prefix);
            transport.sendByte(getFunctionCode());
        }

        /** Null request. */
        public static final Request NULL;

        static {
            NULL = new Request()
            {
                @Override
                public void write(final Transport transport)
                    throws IOException
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Request read(
                        final Transport transport)
                    throws IOException, FormatException
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int getLength()
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public byte getFunctionCode()
                {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Response createResponse(final short[] values)
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        private byte _exceptionCode;
        private Prefix _prefix;
        private volatile int _retries;
        private volatile ModbusServerProxy _serverProxy;
        private volatile State _state;
    }


    /**
     * Response.
     */
    abstract class Response
        implements PAPTransaction.Response
    {
        /**
         * Constructs an instance.
         *
         * @param request The request.
         */
        protected Response(@Nonnull final Request request)
        {
            this(request.getPrefix().copy(), request);
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
            _prefix = Require.notNull(prefix);
            _request = Require.notNull(request);
        }

        /**
         * Gets the function code.
         *
         * @return The fucntion code.
         */
        @CheckReturnValue
        public byte getFunctionCode()
        {
            final Request request = Require.notNull(getRequest());

            return request.getFunctionCode();
        }

        /**
         * Gets the length of this response.
         *
         * @return The length.
         */
        @CheckReturnValue
        public abstract int getLength();

        /**
         * Gets a name for the response.
         *
         * @return The name.
         */
        public final String getName()
        {
            final Class<?> enclosingClass = getClass().getEnclosingClass();

            return (enclosingClass != null)? enclosingClass
                .getSimpleName(): getClass().getSimpleName();
        }

        /**
         * Gets the prefix.
         *
         * @return The prefix.
         */
        public final Prefix getPrefix()
        {
            return _prefix;
        }

        /**
         * Gets the request.
         *
         * @return The request.
         */
        @Nonnull
        @CheckReturnValue
        public Request getRequest()
        {
            return _request;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isSuccess()
        {
            return true;
        }

        /**
         * Reads from the transport.
         *
         * @param transport The transport.
         *
         * @throws IOException When appropriate.
         * @throws FormatException On transaction format exception.
         */
        public abstract void read(
                @Nonnull Transport transport)
            throws IOException, FormatException;

        /**
         * Writes to the transport.
         *
         * @param transport The transport.
         *
         * @throws IOException When appropriate.
         */
        public abstract void write(
                @Nonnull Transport transport)
            throws IOException;

        /**
         * Reads the suffix.
         *
         * @param transport The transport.
         *
         * @throws IOException When appropriate.
         * @throws FormatException On bad suffix.
         */
        protected static void readSuffix(
                @Nonnull final Transport transport)
            throws IOException, FormatException
        {
            if (!transport.receiveSuffix()) {
                throw new Transaction.FormatException(
                    ModbusMessages.BAD_TRANSPORT_SUFFIX);
            }
        }

        /**
         * Writes the suffix.
         *
         * @param transport The transport.
         *
         * @throws IOException When appropriate.
         */
        protected static final void writeSuffix(
                @Nonnull final Transport transport)
            throws IOException
        {
            transport.sendSuffix();
        }

        /**
         * Specifies the expected number of bytes.
         *
         * @param bytes The expected number of bytes.
         *
         * @throws FormatException When it does not match the prefix.
         */
        protected void expectedBytes(final int bytes)
            throws FormatException
        {
            if (!getPrefix().checkDataLength(bytes)) {
                throw new FormatException(ModbusMessages.MESSAGE_LENGTH_MBAP);
            }
        }

        /**
         * Writes to the transport.
         *
         * @param length The response specific data length.
         * @param transport The transport.
         *
         * @throws IOException When appropriate.
         */
        protected final void write(
                final int length,
                @Nonnull final Transport transport)
            throws IOException
        {
            _prefix.saveDataLength(length);
            transport.sendPrefix(_prefix);
            transport.sendByte(getFunctionCode());
        }

        /** Null response. */
        public static final Response NULL;

        static {
            NULL = new Transaction.Response(new Prefix((byte) 0), Request.NULL)
            {
                /** {@inheritDoc}
                 */
                @Override
                public void write(final Transport transport)
                    throws IOException
                {
                    throw new UnsupportedOperationException();
                }

                /** {@inheritDoc}
                 */
                @Override
                public void read(
                        final Transport transport)
                    throws IOException, Transaction.FormatException
                {
                    throw new UnsupportedOperationException();
                }

                /** {@inheritDoc}
                 */
                @Override
                public int getLength()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        private final Prefix _prefix;
        private final Request _request;
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
