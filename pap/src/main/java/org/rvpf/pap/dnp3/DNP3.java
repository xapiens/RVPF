/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3;

import org.rvpf.pap.PAP;

/**
 * DNP3.
 */
public interface DNP3
    extends PAP
{
    /** Attributes usage. */
    String ATTRIBUTES_USAGE = "DNP3";

    /** Connect timeout attribute. */
    String CONNECT_TIMEOUT_ATTRIBUTE = "CONNECT_TIMEOUT";

    /** Data type attribute. */
    String DATA_TYPE_ATTRIBUTE = "DATA_TYPE";

    /** Default maximum fragment size. */
    int DEFAULT_MAX_FRAGMENT_SIZE = 2048;

    /** Index attribute. */
    String INDEX_ATTRIBUTE = "INDEX";

    /** Keep-alive timeout attribute. */
    String KEEP_ALIVE_TIMEOUT_ATTRIBUTE = "KEEP_ALIVE_TIMEOUT";

    /** Logical device attribute. */
    String LOGICAL_DEVICE_ATTRIBUTE = "LOGICAL_DEVICE";

    /** Logical device property. */
    String LOGICAL_DEVICE_PROPERTY = "logical.device";

    /** Maximum fragment size attribute. */
    String MAX_FRAGMENT_SIZE_ATTRIBUTE = "MAX_FRAGMENT_SIZE";

    /** Minimum maximum fragment size. */
    int MINIMUM_MAX_FRAGMENT_SIZE = 249;

    /** Point type attribute. */
    String POINT_TYPE_ATTRIBUTE = "POINT_TYPE";

    /** Default DNP3 port number. */
    int PORT = 20000;

    /** Properties. */
    String PROPERTIES = "dnp3";

    /** Protocol name. */
    String PROTOCOL_NAME = "DNP3";

    /** Reply timeout attribute. */
    String REPLY_TIMEOUT_ATTRIBUTE = "REPLY_TIMEOUT";

    /** Serial port attribute. */
    String SERIAL_PORT_ATTRIBUTE = "SERIAL_PORT";

    /** Serial port property. */
    String SERIAL_PORT_PROPERTY = "serial.port";

    /** Serial speed attribute. */
    String SERIAL_SPEED_ATTRIBUTE = "SERIAL_SPEED";

    /** Serial speed property. */
    String SERIAL_SPEED_PROPERTY = "serial.speed";

    /** Start index attribute. */
    String START_INDEX_ATTRIBUTE = "START_INDEX";

    /** Stop index attribute. */
    String STOP_INDEX_ATTRIBUTE = "STOP_INDEX";

    /** TCP address attribute. */
    String TCP_ADDRESS_ATTRIBUTE = "TCP_ADDRESS";

    /** TCP listen address property. */
    String TCP_LISTEN_ADDRESS_PROPERTY = "tcp.listen.address";

    /** TCP listen port property. */
    String TCP_LISTEN_PORT_PROPERTY = "tcp.listen.port";

    /** TCP port attribute. */
    String TCP_PORT_ATTRIBUTE = "TCP_PORT";

    /** Default DNP3 TLS port number. */
    int TLS_PORT = 19999;

    /** UDP address attribute. */
    String UDP_ADDRESS_ATTRIBUTE = "UDP_ADDRESS";

    /** UDP listen address property. */
    String UDP_LISTEN_ADDRESS_PROPERTY = "udp.listen.address";

    /** UDP listen port property. */
    String UDP_LISTEN_PORT_PROPERTY = "udp.listen.port";

    /** UDP port attribute. */
    String UDP_PORT_ATTRIBUTE = "UDP_PORT";
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
