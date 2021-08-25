/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ControlStatusCode.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.content;

import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.EnumCode;

/**
 * Control status code.
 */
public enum ControlStatusCode
    implements EnumCode
{
    SUCCESS(0),
    TIMEOUT(1),
    NO_SELECT(2),
    FORMAT_ERROR(3),
    NOT_SUPPORTED(4),
    ALREADY_ACTIVE(5),
    HARDWARE_ERROR(6),
    LOCAL(7),
    TOO_MANY_OBJS(8),
    NOT_AUTHORIZED(9),
    AUTOMATION_INHIBIT(10),
    PROCESSING_LIMITED(11),
    OUT_OF_RANGE(12),
    UNDEFINED(127);

    /**
     * Constructs an instance.
     *
     * @param code The code.
     */
    ControlStatusCode(final int code)
    {
        _code = code;
    }

    /**
     * Gets the instance for a code.
     *
     * @param code The code.
     *
     * @return The instance.
     *
     * @throws AssertionError For unkonwn code.
     */
    @Nonnull
    @CheckReturnValue
    public static ControlStatusCode instance(
            final int code)
        throws AssertionError
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

    private static final Map<Integer, ControlStatusCode> _CODE_MAP = MapFactory
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
