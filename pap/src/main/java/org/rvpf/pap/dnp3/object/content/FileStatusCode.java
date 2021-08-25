/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: FileStatusCode.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.content;

import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.EnumCode;

/**
 * File status code.
 */
public enum FileStatusCode
    implements EnumCode
{
    SUCCESS(0),
    PERMISSION_DENIED(1),
    INVALID_MODE(2),
    FILE_NOT_FOUND(3),
    FILE_LOCKED(4),
    TOO_MANY_OPEN(5),
    INVALID_HANDLE(6),
    WRITE_BLOCK_SIZE(7),
    COMM_LOST(8),
    CANNOT_ABORT(9),
    NOT_OPENED(16),
    HANDLE_EXPIRED(17),
    BUFFER_OVERRUN(18),
    FATAL(19),
    BLOCK_SEQ(20),
    UNDEFINED(255);

    /**
     * Constructs an instance.
     *
     * @param code The code.
     */
    FileStatusCode(final int code)
    {
        _code = code;
    }

    /**
     * Gets the instance for a code.
     *
     * @param code The code.
     *
     * @return The instance.
     */
    @Nonnull
    @CheckReturnValue
    public static FileStatusCode instance(final int code)
    {
        return Require
            .notNull(
                _CODE_MAP.get(Integer.valueOf(code)),
                String.valueOf(code));
    }

    /** {@inheritDoc}
     */
    @Override
    public int getCode()
    {
        return _code;
    }

    private static final Map<Integer, FileStatusCode> _CODE_MAP = MapFactory
        .codeMap(values());

    private final int _code;
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
