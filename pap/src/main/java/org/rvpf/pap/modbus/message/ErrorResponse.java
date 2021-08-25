/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ErrorResponse.java 4086 2019-06-16 16:36:41Z SFB $
 */

package org.rvpf.pap.modbus.message;

import java.io.IOException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.pap.modbus.transport.ExceptionCode;
import org.rvpf.pap.modbus.transport.Transport;

/**
 * Error response.
 */
public final class ErrorResponse
    extends Transaction.Response
{
    /**
     * Constructs an error response.
     *
     * @param prefix The prefix.
     * @param request The original request.
     */
    public ErrorResponse(
            @Nonnull final Prefix prefix,
            @Nonnull final Transaction.Request request)
    {
        super(prefix, request);

        _errorCode = request.getFunctionCode();
    }

    /**
     * Constructs an error response.
     *
     * @param prefix The prefix.
     * @param functionCode The function code of the request.
     * @param exceptionCode The explanatory exception code.
     */
    public ErrorResponse(
            @Nonnull final Prefix prefix,
            final byte functionCode,
            @Nonnull final ExceptionCode exceptionCode)
    {
        super(prefix, Transaction.Request.NULL);

        _errorCode = functionCode;
        _exceptionCode = exceptionCode.getCodeByte();
    }

    /**
     * Constructs an error response.
     *
     * @param request The original request.
     */
    ErrorResponse(@Nonnull final Transaction.Request request)
    {
        super(request);

        _errorCode = request.getFunctionCode();
        _exceptionCode = request.getExceptionCode();
    }

    /**
     * Gets the original function code in error.
     *
     * @return The original function code in error.
     */
    @CheckReturnValue
    public byte getErrorCode()
    {
        return _errorCode;
    }

    /**
     * Gets the exception code.
     *
     * @return The exception code.
     */
    @CheckReturnValue
    public byte getExceptionCode()
    {
        return _exceptionCode;
    }

    /**
     * Gets the exception code name.
     *
     * @return The exception code name.
     */
    @Nonnull
    @CheckReturnValue
    public String getExceptionCodeName()
    {
        return ExceptionCode.getInstance(_exceptionCode).getName();
    }

    /** {@inheritDoc}
     */
    @Override
    public byte getFunctionCode()
    {
        return (byte) (_errorCode | 0x80);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getLength()
    {
        return 1;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSuccess()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public void read(
            final Transport transport)
        throws IOException, Transaction.FormatException
    {
        expectedBytes(1);

        _exceptionCode = transport.receiveByte();

        readSuffix(transport);
    }

    /** {@inheritDoc}
     */
    @Override
    public void write(final Transport transport)
        throws IOException
    {
        super.write(getLength(), transport);

        transport.sendByte(_exceptionCode);

        writeSuffix(transport);
    }

    private final byte _errorCode;
    private byte _exceptionCode;
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
