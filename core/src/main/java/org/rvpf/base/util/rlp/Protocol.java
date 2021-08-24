/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Protocol.java 3983 2019-05-14 11:11:45Z SFB $
 */

package org.rvpf.base.util.rlp;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Protocol.
 *
 * <p>Enumeration of some IP protocols.</p>
 */
public enum Protocol
{
    IP(0),
    ICMP(1),
    IGMP(2),
    GGP(3),
    TCP(6),
    EGP(8),
    UDP(17),
    UNKNOWN(-1);

    /**
     * Constructs an instance.
     *
     * @param code The protocol code.
     */
    Protocol(final int code)
    {
        _code = code;
    }

    /**
     * Gets a protocol by its code.
     *
     * @param code The protocol code.
     *
     * @return The status.
     */
    @Nonnull
    @CheckReturnValue
    public static Protocol get(final int code)
    {
        final Protocol status = _MAP.get(Integer.valueOf(code));

        return (status != null)? status: UNKNOWN;
    }

    /**
     * Returns a string for the protocol code.
     *
     * @param code The protocol code.
     *
     * @return A string for the protocol code.
     */
    @Nonnull
    @CheckReturnValue
    public static String toString(final int code)
    {
        return get(code).name() + "(" + code + ")";
    }

    /**
     * Returns the protocol code.
     *
     * @return The protocol code.
     */
    @CheckReturnValue
    public int code()
    {
        return _code;
    }

    private static final Map<Integer, Protocol> _MAP = new HashMap<>();

    static {
        for (final Protocol protocol: values()) {
            _MAP.put(Integer.valueOf(protocol.code()), protocol);
        }
    }

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
