/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: InternalIndication.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.content;

import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.pap.EnumCode;

/**
 * Internal indication.
 */
public enum InternalIndication
    implements EnumCode
{
    BROADCAST("Broadcast Message Received"),
    CLASS_1_EVENTS("Additional Class 1 Event Data Is Available"),
    CLASS_2_EVENTS("Additional Class 2 Event Data Is Available"),
    CLASS_3_EVENTS("Additional Class 3 Event Data Is Available"),
    NEED_TIME("Time Synchronization Required"),
    LOCAL_CONTROL("Some Output Points Are In Local Mode"),
    DEVICE_TROUBLE("Device Trouble"),
    DEVICE_RESTART("Device Restart"),
    NO_FUNC_CODE_SUPPORT("Function Code Not Implemented"),
    OBJECT_UNKNOWN("Object Unknown"),
    PARAMETER_ERROR("Parameter Error"),
    EVENT_BUFFER_OVERFLOW("Event Buffer Overflow"),
    ALREADY_EXECUTING("Operation Is already Executing"),
    CONFIG_CORRUPT("Configuration Corrupt"),
    RESERVED_1("Reserved Bit"),
    RESERVED_2("Reserved Bit");

    /**
     * Constructs an instance.
     *
     * @param title The internal indication title.
     */
    InternalIndication(@Nonnull final String title)
    {
        _title = title;
    }

    /**
     * Gets the values size.
     *
     * @return The values size.
     */
    @CheckReturnValue
    public static int getSize()
    {
        return _CODE_MAP.size();
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
    public static InternalIndication instance(
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
        return ordinal();
    }

    /**
     * Gets the mask for the indication.
     *
     * @return The mask for the indication.
     */
    @CheckReturnValue
    public int getMask()
    {
        return 1 << ordinal();
    }

    /**
     * Gets the title.
     *
     * @return The title.
     */
    @Nonnull
    @CheckReturnValue
    public String getTitle()
    {
        return _title;
    }

    private static final Map<Integer, InternalIndication> _CODE_MAP = MapFactory
        .codeMap(values());

    private final String _title;
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
