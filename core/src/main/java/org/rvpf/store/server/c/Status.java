/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Status.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.server.c;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Status.
 */
public enum Status    // Must match status codes in CStoreImpl.h.
{
    SUCCESS(0),
    UNKNOWN(-1001),
    BAD_HANDLE(-1002),
    FAILED(-1003),
    IGNORED(-1004),
    POINT_UNKNOWN(-1005),
    ILLEGAL_STATE(-1006),
    DISCONNECTED(-1007),
    UNSUPPORTED(-1008),
    UNRECOVERABLE(-1009),;

    Status(final int code)
    {
        _code = code;
    }

    /**
     * Gets a status by its code.
     *
     * @param code The status code.
     *
     * @return The status.
     */
    @CheckReturnValue
    public static Status get(final int code)
    {
        final Status status = (code == 0)? SUCCESS: _MAP
            .get(Integer.valueOf(code));

        return (status != null)? status: UNKNOWN;
    }

    /**
     * Returns a string for the code.
     *
     * @param code The code.
     *
     * @return A string for the code.
     */
    @Nonnull
    @CheckReturnValue
    public static String toString(final int code)
    {
        return get(code).name() + "(" + code + ")";
    }

    /**
     * Returns the status code.
     *
     * @return The status code.
     */
    @CheckReturnValue
    public int code()
    {
        return _code;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return name() + "(" + code() + ")";
    }

    /** Bad handle code. */
    public static final int BAD_HANDLE_CODE = BAD_HANDLE.code();

    /** Disconnected code. */
    public static final int DISCONNECTED_CODE = DISCONNECTED.code();

    /** Failed code. */
    public static final int FAILED_CODE = FAILED.code();

    /** Ignored code. */
    public static final int IGNORED_CODE = IGNORED.code();

    /** Illegal state code. */
    public static final int ILLEGAL_STATE_CODE = ILLEGAL_STATE.code();

    /** Point unknown code. */
    public static final int POINT_UNKNOWN_CODE = POINT_UNKNOWN.code();

    /** Success code. */
    public static final int SUCCESS_CODE = SUCCESS.code();

    /** Unrecoverable code. */
    public static final int UNRECOVERABLE_CODE = UNRECOVERABLE.code();

    /** Unsupported code. */
    public static final int UNSUPPORTED_CODE = UNSUPPORTED.code();
    private static final Map<Integer, Status> _MAP = new HashMap<Integer,
        Status>();

    static {
        for (final Status status: values()) {
            _MAP.put(Integer.valueOf(status.code()), status);
        }
    }

    private final int _code;

    /**
     * Failed exception.
     */
    public static final class FailedException
        extends Exception
    {
        FailedException(final int code)
        {
            _code = code;
        }

        /** {@inheritDoc}
         */
        @Override
        public String getMessage()
        {
            return "Failed: " + Status.toString(_code);
        }

        private static final long serialVersionUID = 1L;

        private final int _code;
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
