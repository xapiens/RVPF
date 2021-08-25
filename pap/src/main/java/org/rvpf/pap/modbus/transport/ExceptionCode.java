/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ExceptionCode.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.transport;

import java.util.Locale;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Exception code.
 */
public enum ExceptionCode
{
    ILLEGAL_FUNCTION(0x01),
    ILLEGAL_DATA_ADDRESS(0x02),
    ILLEGAL_DATA_VALUE(0x03),
    SLAVE_DEVICE_FAILURE(0x04),
    ACKNOWLEDGE(0x05),
    SLAVE_DEVICE_BUSY(0x06),
    NEGATIVE_ACKNOWLEDGE(0x07),
    MEMORY_PARITY_ERROR(0x08),
    GATEWAY_PATH_UNAVAILABLE(0x0A),
    GATEWAY_TARGET_DEVICE_FAILED_TO_RESPOND(0x0B),
    UNKNOWN_EXCEPTION_CODE(0x00);

    /**
     * Constructs an instance.
     *
     * @param exceptionCodeByte The exception code byte.
     */
    ExceptionCode(final int exceptionCodeByte)
    {
        _codeByte = (byte) exceptionCodeByte;
    }

    /**
     * Gets the instance for an exception code byte.
     *
     * @param exceptionCodeByte The exception code byte.
     *
     * @return The exception code instance.
     */
    @Nonnull
    @CheckReturnValue
    public static ExceptionCode getInstance(final byte exceptionCodeByte)
    {
        for (final ExceptionCode exceptionCode: values()) {
            if (exceptionCode.getCodeByte() == exceptionCodeByte) {
                return exceptionCode;
            }
        }

        return UNKNOWN_EXCEPTION_CODE;
    }

    /**
     * Gets the exception code byte.
     *
     * @return The exception code byte.
     */
    @CheckReturnValue
    public byte getCodeByte()
    {
        return _codeByte;
    }

    /**
     * Gets the exception code name.
     *
     * @return The exception code name.
     */
    @Nonnull
    @CheckReturnValue
    public String getName()
    {
        String name = toString();

        if (this == UNKNOWN_EXCEPTION_CODE) {
            name += String
                .format(
                    (Locale) null,
                    "(0x%02X)",
                    Integer.valueOf(_codeByte & 0xFF));
        }

        return name;
    }

    private final byte _codeByte;
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
