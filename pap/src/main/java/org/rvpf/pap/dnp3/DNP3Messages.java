/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3Messages.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3;

import org.rvpf.base.logger.Messages;

/**
 * DNP3 messages.
 */
public enum DNP3Messages
    implements Messages.Entry
{
    APPLICATION_HEADER_INCOMPLETE,
    BAD_CRC,
    BAD_LOGICAL_DEVICE,
    CONNECTION_FAILED,
    CONNECTION_OPENED,
    DUPLICATE_LOGICAL_DEVICE,
    FRAGMENT_BUFFER_OVERFLOW,
    FRAME_RESPONSE_TIMEOUT,
    IGNORED_FRAME,
    INVALID_FRAME_LENGTH,
    INVALID_START_FIELD,
    INVERTED_DIR_BIT,
    INVERTED_FIR_BIT,
    LOST_CONNECTION,
    LOST_CONNECTION_,
    MASTER_CONNECTION_ACCEPTED,
    MISSING_FRAME_DATA,
    NEW_CONNECTION,
    NO_FUNC_CODE_SUPPORT,
    OBJECT_UNKNOWN,
    OUTSTATION_CONNECTION_ACCEPTED,
    PARAMETER_ERROR,
    PREFIX_CODE_NOT_SUPPORTED,
    RANGE_CODE_NOT_SUPPORTED,
    READ_COMMIT_FAILED,
    RECEIVED_FROM,
    RECEIVED_POINT_VALUE,
    RECEIVED_REQUEST,
    RECEIVED_RESPONSE,
    RECONFIGURED_OBJECT,
    REGISTERED_LOCAL_ADDRESS,
    SEGMENT_BUFFER_OVERFLOW,
    SENDING_REQUEST,
    SENDING_RESPONSE,
    SENT_POINT_VALUE,
    SENT_TO,
    STARTED_LISTENING,
    STOPPED_LISTENING,
    TRYING_CONNECTION_FROM_TO,
    TRYING_CONNECTION_THRU,
    TRYING_CONNECTION_TO,
    UNCONFIGURED_OBJECT,
    UNEXPECTED_FRAGMENT,
    UNEXPECTED_FRAME_DATA,
    UNEXPECTED_LINK_STATE,
    UNEXPECTED_RESPONSE_ITEMS,
    UNEXPECTED_SEGMENT_SEQUENCE,
    UNKNOWN_DATA_TYPE,
    UNKNOWN_FUNCTION_CODE,
    UNKNOWN_GROUP_CODE,
    UNKNOWN_LOGICAL_DEVICE,
    UNKNOWN_POINT_TYPE,
    UNKNOWN_PREFIX_CODE,
    UNKNOWN_RANGE_CODE,
    UNKNOWN_VARIATION,
    UNSUPPORTED_REQUEST,
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

    private static final String _BUNDLE_NAME = "org.rvpf.messages.dnp3";

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
