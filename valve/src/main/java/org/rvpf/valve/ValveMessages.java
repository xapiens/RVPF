/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValveMessages.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.valve;

import org.rvpf.base.logger.Messages;

/** Valve messages.
 */
public enum ValveMessages
    implements Messages.Entry
{
    BAD_ADDRESS,
    BUFFER_SIZE,
    BYTES_TRANSFERED,
    CLOSING_CONNECTION,
    CONNECTION_ACCEPTED,
    CONNECTION_NORMAL,
    CONNECTION_REFUSED,
    CONNECTION_REQUESTED,
    CONNECTION_STRING,
    CONNECTIONS_STATS,
    CONTROL_ACCEPTED,
    CONTROL_ADDRESS,
    CONTROL_HANDSHAKE_TIMEOUT,
    CONTROL_INVERTED,
    CONTROLLED,
    CONTROLLED_ADDRESS,
    CONTROLLED_CONNECTIONS_LIMIT,
    CONTROLLED_HANDSHAKE_TIMEOUT,
    DIRECT,
    DIRECT_ADDRESS,
    DIRECT_CONNECTIONS_LIMIT,
    DIRECT_HANDSHAKE_TIMEOUT,
    DIRECTION_CLOSED,
    EMPTY_PROPERTY,
    FILTER_CLASS,
    INPUT_CLOSED,
    LISTEN_ADD_FAILED,
    LISTEN_ADD_FAILED_,
    LISTENING,
    LISTENING_TYPE,
    LOADING_PROPERTIES,
    LOCKING_CONTROLLED_PORTS,
    MISSING_PROPERTY,
    NO_SERVICE_PORT,
    OUTPUT_CLOSED,
    PAUSES_RESUMES,
    PROPERTIES_FILE_PATH,
    PROPERTIES_LOAD_FAILED,
    READ_COUNT,
    SERVER_ADDRESS,
    SERVER_CONNECT_FAILED,
    SERVER_CONNECTED,
    SERVER_SOCKET,
    SSL_CLIENT_CONNECTION_ACCEPTED,
    SSL_DATA_SIZE,
    SSL_DELEGATED_COMPLETED,
    SSL_DELEGATED_FAILED,
    SSL_DELEGATING,
    SSL_HANDSHAKE_TIMED_OUT,
    SSL_NETWORK_INPUT_SIZE,
    SSL_NETWORK_OUTPUT_SIZE,
    SSL_SERVER_CONNECTION_ACCEPTED,
    SSL_UNEXPECTED_CLOSE,
    SSL_UNWRAP_FAILED,
    SSL_UNWRAP_RESULT,
    SSL_WRAP_FAILED,
    SSL_WRAP_RESULT,
    STARTED_TYPE,
    STOPPED_LISTENING,
    STOPPED_TYPE,
    UNLOCKING_CONTROLLED_PORTS,
    WRITE_COUNT;

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

    private static final String _BUNDLE_NAME = "org.rvpf.messages.valve";

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
