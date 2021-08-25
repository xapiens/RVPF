/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusMessages.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus;

import org.rvpf.base.logger.Messages;

/**
 * Modbus messages.
 */
public enum ModbusMessages
    implements Messages.Entry
{
    BAD_MESSAGE_TERMINATION,
    BAD_TRANSPORT_SUFFIX,
    BAD_UNIT_IDENTIFIER,
    BATCH_SIZE,
    BYTE_COUNT_INPUTS,
    BYTE_COUNT_OUTPUTS,
    BYTE_COUNT_REGISTERS,
    CLIENT_CONNECTION_ACCEPTED,
    CLIENT_CONNECTION_CLOSED,
    CLIENT_CONNECTION_REJECTED,
    CLOSING_CONNECTION,
    CONFIGURED_POINTS,
    CONFIGURED_PROXIES,
    CONNECT_TIMEOUT,
    FAILED_SEND_REQUEST,
    FAILED_SEND_RESPONSE,
    FUNCTION_CODE_MATCH,
    IGNORED_MESSAGE_FOR_UNIT,
    INVALID_ADDRESS,
    LISTEN_ADDRESS,
    LISTEN_PORT,
    MESSAGE_LENGTH_MBAP,
    NO_ADDRESS,
    NO_ADDRESS_FOR_BIT,
    NO_ORIGIN,
    OUT_OF_SEQUENCE,
    OUTPUT_ADDRESS_MATCH,
    OUTPUT_POINT_VALUE,
    OUTPUT_VALUE_MATCH,
    OVERLOADED_ADDRESS,
    OVERLOADED_BIT,
    POINT_SPLITS_MIDDLE_ENDIAN,
    PROCESSING_REQUEST,
    QUANTITY_OUTPUTS_MATCH,
    QUANTITY_REGISTERS_MATCH,
    READ_ONLY_REGISTER,
    RECEIVED_ERROR_RESPONSE,
    RECEIVED_REQUEST,
    RECEIVED_RESPONSE,
    REGISTER_ADDRESS_MATCH,
    REGISTER_AND_MASK_MATCH,
    REGISTER_OR_MASK_MATCH,
    REGISTER_VALUE_MATCH,
    REQUEST_RETRIES,
    REQUEST_RETRY_INTERVAL,
    REQUEST_TIMEOUT,
    SENDING_ERROR_RESPONSE,
    SENDING_REQUEST,
    SENDING_RESPONSE,
    SERIAL_MODE,
    SERVER_CONNECTION_CLOSED,
    SERVER_CONNECTION_SUCCEEDED,
    SERVER_CONNECTIONS_FAILED,
    STARTED_LISTENING,
    STARTED_SERVER,
    STARTING_ADDRESS_MATCH,
    STARTING_SERVER,
    STOPPED_LISTENING,
    STOPPED_SERVER,
    STOPPING_SERVER,
    TICK_EXPIRED,
    TIMEOUT_ON_REQUEST,
    TRYING_SERVER_CONNECTION,
    UNCONFIGURED_COIL,
    UNCONFIGURED_DISCRETE,
    UNCONFIGURED_INPUT,
    UNCONFIGURED_REGISTER,
    UNEXPECTED_FUNCTION_CODE,
    UNEXPECTED_RESPONSE,
    UNEXPECTED_UNIT_IDENTIFIER,
    UNRECOGNIZED_SERIAL_MODE;

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

    private static final String _BUNDLE_NAME = "org.rvpf.messages.modbus";

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
