/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPMessages.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap;

import org.rvpf.base.logger.Messages;

/**
 * PAP messages.
 */
public enum PAPMessages
    implements Messages.Entry
{
    AMBIGUOUS_ORIGIN_ADDRESS,
    BAD_ATTRIBUTE_VALUE,
    BAD_PARAMETER_VALUE,
    BEGINNING_SCANNER_RETRIES,
    CANCELLED_,
    CLOSE_FAILED,
    CONFIGURED_PROXY,
    CONFLICTING_ATTRIBUTES,
    CONTROL_POINT_UNKNOWN,
    CRONTAB_REQUIRES_STRING,
    DATALOGGER_DESTINATION_QUEUE,
    DATALOGGER_DESTINATION_STORE,
    DATALOGGER_SCANNER_NAME,
    DATALOGGER_SCANNER_SYNC,
    DSR_OFF,
    DSR_ON,
    ELAPSED_REQUIRES_NUMBER,
    FRAME_ERROR,
    JSSC_VERSION,
    LOST_EVENTS,
    MISSING_ATTRIBUTE,
    MISSING_ATTRIBUTES,
    MISSING_ORIGIN,
    MISSING_POSITION_PARAMETER,
    MULTIPLE_CONNECTION_STATE,
    NO_LISTENERS,
    NO_POINTS_TO_SCAN,
    NO_PROTOCOL_FOR_ENTITY,
    NO_SCHEDULE_FOR_POINT,
    OVERRUN_ERROR,
    PARITY_ERROR,
    POINT_EQ_1_INPUT,
    POINTS_TO_SCAN,
    PROTOCOL_CONFLICT,
    READ_COMMIT_FAILED,
    REMOVES_NOT_ALLOWED,
    SCAN_STARTS_AT,
    SCANNER_NO_RETRIES,
    SCANNER_ORIGINATES,
    SCANNER_RETRY_INTERVAL,
    SCANNER_RETRY_INTERVAL_TEXT,
    SCANNER_RETRY_LIMIT,
    SCANNER_RETRY_SUCCEEDED,
    SCANNING_NOW,
    SERIAL_PORT_CLOSED,
    SERIAL_PORT_OPENED,
    SERVER_CONNECTION_FAILED,
    START_STOP_NEEDED,
    STARTED_SERVICES,
    STOPPED_SERVICES,
    SUPPORTED_PROTOCOL,
    UNKNOWN_ORIGIN,
    UNKNOWN_ORIGIN_ADDRESS,
    UNKNOWN_PROTOCOL,
    UNRECOGNIZED_PARITY,
    WILDCARD_ADDRESS_RESTRICTS,
    WRITE_COMMIT_FAILED;

    /** {@inheritDoc}
     */
    @Override
    public String getBundleName()
    {
        return _BUNDLE_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized String toString()
    {
        if (_string == null) {
            _string = Messages.getString(this);
        }

        return _string;
    }

    private static final String _BUNDLE_NAME = "org.rvpf.messages.pap";

    private String _string;
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
