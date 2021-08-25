/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap.dnp3.transport;

import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.pap.EnumCode;
import org.rvpf.pap.dnp3.DNP3Messages;
import org.rvpf.pap.dnp3.DNP3ProtocolException;

/**
 * Function code.
 */
public enum FunctionCode
    implements EnumCode
{
    CONFIRM(0, "Confirm", false),
    READ(1, "Read", false),
    WRITE(2, "Write", true),
    SELECT(3, "Select", true),
    OPERATE(4, "Operate", true),
    DIRECT_OPERATE(5, "Direct Operate", true),
    DIRECT_OPERATE_NR(6, "Direct Operate -- No Response", true),
    IMMED_FREEZE(7, "Immediate Freeze", false),
    IMMED_FREEZE_NR(8, "Immediate Freeza -- No Response", false),
    FREEZE_CLEAR(9, "Freeze and Clear", false),
    FREEZE_CLEAR_NR(10, "Freeze and Clear -- No Response", false),
    FREEZE_AT_TIME(11, "Freeze at Time", false),
    FREEZE_AT_TIME_NR(12, "Freeze at Time -- No Response", false),
    COLD_RESTART(13, "Cold Restart", false),
    WARM_RESTART(14, "Warm Restart", false),
    INITIALIZE_DATA(15, "Initializa Data", false),
    INITIALIZE_APPL(16, "Initialize Application", false),
    START_APPL(17, "Start Application", false),
    STOP_APPL(18, "Stop Application", false),
    SAVE_CONFIG(19, "Save Configuration", false),
    ENABLE_UNSOLICITED(20, "Enable Unsolicited Response", false),
    DISABLE_UNSOLICITED(21, "Disable Unsolicited Response", false),
    ASSIGN_CLASS(22, "Assign Class", false),
    DELAY_MEASURE(23, "Delay Measure", false),
    RECORD_CURRENT_TIME(24, "Record Current Time", false),
    OPEN_FILE(25, "Open File", false),
    CLOSE_FILE(26, "Close File", false),
    DELETE_FILE(27, "Delete File", false),
    GET_FILE_INFO(28, "Get File Information", false),
    AUTHENTICATE_FILE(29, "Authenticate File", false),
    ABORT_FILE(30, "Abort File", false),
    ACTIVATE_CONFIG(31, "Activate Configuration", false),
    AUTHENTICATE_REQ(32, "Authentication Request", false),
    AUTH_REQ_NO_ACK(33, "Authentication Request -- No Acknowledgment", false),
    RESPONSE(129, "Solicited Response", true),
    UNSOLICITED_RESPONSE(130, "Unsolicited Response", true),
    AUTHENTICATE_RESP(131, "Authentication Response", false);

    /**
     * Constructs an instance.
     *
     * @param code The code.
     * @param name The name.
     * @param needsValues If the function needs values.
     */
    FunctionCode(
            final int code,
            @Nonnull final String name,
            final boolean needsValues)
    {
        _code = code;
        _name = name;
        _needsValues = needsValues;
    }

    /**
     * Gets the instance for a code.
     *
     * @param code The code.
     *
     * @return The instance.
     *
     * @throws DNP3ProtocolException If unknown.
     */
    @Nonnull
    @CheckReturnValue
    public static FunctionCode instance(
            final int code)
        throws DNP3ProtocolException
    {
        final FunctionCode functionCode = _CODE_MAP.get(Integer.valueOf(code));

        if (functionCode == null) {
            throw new DNP3ProtocolException(
                DNP3Messages.UNKNOWN_FUNCTION_CODE,
                String.valueOf(Math.abs(code)));
        }

        return functionCode;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getCode()
    {
        return _code;
    }

    /**
     * Gets the name.
     *
     * @return The name.
     */
    @Nonnull
    @CheckReturnValue
    public String getName()
    {
        return _name;
    }

    /**
     * Asks if this function needs values.
     *
     * @return True if this function needs values.
     */
    @CheckReturnValue
    public boolean needsValues()
    {
        return _needsValues;
    }

    private static final Map<Integer, FunctionCode> _CODE_MAP = MapFactory
        .codeMap(values());

    private final int _code;
    private final String _name;
    private final boolean _needsValues;
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
