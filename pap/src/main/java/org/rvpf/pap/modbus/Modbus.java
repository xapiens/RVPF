/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Modbus.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus;

import org.rvpf.base.ElapsedTime;
import org.rvpf.pap.PAP;
import org.rvpf.pap.SerialPortWrapper;
import org.rvpf.pap.modbus.message.ReadDiscreteInputs;
import org.rvpf.pap.modbus.message.ReadInputRegisters;
import org.rvpf.pap.modbus.message.WriteMultipleCoils;
import org.rvpf.pap.modbus.message.WriteMultipleRegisters;
import org.rvpf.pap.modbus.transport.Transport;

/**
 * Modbus.
 */
public interface Modbus
    extends PAP
{
    /** Array size attribute (point). */
    String ARRAY_SIZE_ATTRIBUTE = "ARRAY_SIZE";

    /** Array type (point). */
    String ARRAY_TYPE = "ARRAY";

    /** Attributes usage (origin and point). */
    String ATTRIBUTES_USAGE = "MODBUS";

    /** Batch size attribute (origin). */
    String BATCH_SIZE_ATTRIBUTE = "BATCH_SIZE";

    /** Bit attribute (point). */
    String BIT_ATTRIBUTE = "BIT";

    /** Bit type (point). */
    String BIT_TYPE = "BIT";

    /** Coil address attribute (point). */
    String COIL_ADDRESS_ATTRIBUTE = "COIL_ADDRESS";

    /** Connect timeout attribute (origin). */
    String CONNECT_TIMEOUT_ATTRIBUTE = "CONNECT_TIMEOUT";

    /** Default batch size (origin). */
    int DEFAULT_BATCH_SIZE = 1;

    /** Default connect timeout (origin). */
    ElapsedTime DEFAULT_CONNECT_TIMEOUT = ElapsedTime.fromMillis(1000);

    /** Default port. */
    int DEFAULT_PORT = 502;    // Modbus spec.

    /** Default request retry interval (origin). */
    ElapsedTime DEFAULT_REQUEST_RETRY_INTERVAL = ElapsedTime.fromMillis(3000);

    /** Default request timeout (origin). */
    ElapsedTime DEFAULT_REQUEST_TIMEOUT = ElapsedTime.fromMillis(60000);

    /** Default serial control (origin). */
    boolean DEFAULT_SERIAL_CONTROL = false;

    /** Default serial mode (origin). */
    String DEFAULT_SERIAL_MODE = Transport.SERIAL_MODE_RTU_NAME;    // Modbus spec.

    /** Default serial modem (origin). */
    boolean DEFAULT_SERIAL_MODEM = false;

    /** Default serial parity (origin). */
    String DEFAULT_SERIAL_PARITY =
        SerialPortWrapper.Builder.PARITY_NAME_EVEN;    // Modbus spec.

    /** Default serial speed (origin). */
    int DEFAULT_SERIAL_SPEED = 9600;

    /** Default unit identifier (origin). */
    int DEFAULT_UNIT_IDENTIFIER = 1;

    /** Discrete address attribute (point). */
    String DISCRETE_ADDRESS_ATTRIBUTE = "DISCRETE_ADDRESS";

    /** Double type (point). */
    String DOUBLE_TYPE = "DOUBLE";

    /** Float param (point). */
    String FLOAT_PARAM = "Float";

    /** Float type (point). */
    String FLOAT_TYPE = "FLOAT";

    /** Ignored attribute (point). */
    String IGNORED_ATTRIBUTE = "IGNORED";

    /** Input address attribute (point). */
    String INPUT_ADDRESS_ATTRIBUTE = "INPUT_ADDRESS";

    /** Integer type (point). */
    String INTEGER_TYPE = "INTEGER";

    /** Little endian attribute (origin). */
    String LITTLE_ENDIAN_ATTRIBUTE = "LITTLE_ENDIAN";

    /** Long type (point). */
    String LONG_TYPE = "LONG";

    /** Mask attribute (point). */
    String MASK_ATTRIBUTE = "MASK";

    /** Mask type (point). */
    String MASK_TYPE = "MASK";

    /** Maximum size of a coil array. */
    int MAXIMUM_COIL_ARRAY = WriteMultipleCoils.MAXIMUM_QUANTITY;

    /** Maximum size of a discrete array. */
    int MAXIMUM_DISCRETE_ARRAY = ReadDiscreteInputs.MAXIMUM_QUANTITY;

    /** Maximum size of an input array. */
    int MAXIMUM_INPUT_ARRAY = ReadInputRegisters.MAXIMUM_QUANTITY;

    /** Maximum size of a register array. */
    int MAXIMUM_REGISTER_ARRAY = WriteMultipleRegisters.MAXIMUM_QUANTITY;

    /** Maximum unit identifier. */
    int MAXIMUM_UNIT_IDENTIFIER = 247;    // Modbus spec.

    /** Middle endian attribute (origin). */
    String MIDDLE_ENDIAN_ATTRIBUTE = "MIDDLE_ENDIAN";

    /** Minimum unit identifier. */
    int MINIMUM_UNIT_IDENTIFIER = 1;    // Modbus spec.

    /** Properties. */
    String PROPERTIES = "modbus";

    /** Protocol name. */
    String PROTOCOL_NAME = "Modbus";

    /** Register address attribute (point). */
    String REGISTER_ADDRESS_ATTRIBUTE = "REGISTER_ADDRESS";

    /** Request retries attribute (origin). */
    String REQUEST_RETRIES_ATTRIBUTE = "REQUEST_RETRIES";

    /** Request retry interval attribute (origin). */
    String REQUEST_RETRY_INTERVAL_ATTRIBUTE = "REQUEST_RETRY_INTERVAL";

    /** Request timeout attribute (origin). */
    String REQUEST_TIMEOUT_ATTRIBUTE = "REQUEST_TIMEOUT";

    /** Sequence address attribute (origin). */
    String SEQUENCE_ADDRESS_ATTRIBUTE = "SEQUENCE_ADDRESS";

    /** Sequence type (point). */
    String SEQUENCE_TYPE = "SEQUENCE";

    /** Serial control attribute (origin). */
    String SERIAL_CONTROL_ATTRIBUTE = "SERIAL_CONTROL";

    /** Serial control property. */
    String SERIAL_CONTROL_PROPERTY = "serial.control";

    /** Serial modem attribute (origin). */
    String SERIAL_MODEM_ATTRIBUTE = "SERIAL_MODEM";

    /** Serial modem property. */
    String SERIAL_MODEM_PROPERTY = "serial.modem";

    /** Serial mode attribute (origin). */
    String SERIAL_MODE_ATTRIBUTE = "SERIAL_MODE";

    /** Serial mode property. */
    String SERIAL_MODE_PROPERTY = "serial.mode";

    /** Serial parity attribute (origin). */
    String SERIAL_PARITY_ATTRIBUTE = "SERIAL_PARITY";

    /** Serial parity property. */
    String SERIAL_PARITY_PROPERTY = "serial.parity";

    /** Serial port attribute (origin). */
    String SERIAL_PORT_ATTRIBUTE = "SERIAL_PORT";

    /** Serial port property. */
    String SERIAL_PORT_PROPERTY = "serial.port";

    /** Serial speed attribute (origin). */
    String SERIAL_SPEED_ATTRIBUTE = "SERIAL_SPEED";

    /** Serial speed property. */
    String SERIAL_SPEED_PROPERTY = "serial.speed";

    /** Short type (point). */
    String SHORT_TYPE = "SHORT";

    /** Signed attribute (point). */
    String SIGNED_ATTRIBUTE = "SIGNED";

    /** Signed param (point). */
    String SIGNED_PARAM = "Signed";

    /** Size param (point). */
    String SIZE_PARAM = "Size";

    /** Socket address attribute. */
    String SOCKET_ADDRESS_ATTRIBUTE = "SOCKET_ADDRESS";

    /** Socket address property. */
    String SOCKET_ADDRESS_PROPERTY = "socket.address";

    /** Socket port attribute. */
    String SOCKET_PORT_ATTRIBUTE = "SOCKET_PORT";

    /** Socket port property. */
    String SOCKET_PORT_PROPERTY = "socket.port";

    /** Stamp address attribute (origin). */
    String STAMP_ADDRESS_ATTRIBUTE = "STAMP_ADDRESS";

    /** Stamp tick attribute (origin). */
    String STAMP_TICK_ATTRIBUTE = "STAMP_TICK";

    /** Stamp type (point). */
    String STAMP_TYPE = "STAMP";

    /** Time address attribute (origin). */
    String TIME_ADDRESS_ATTRIBUTE = "TIME_ADDRESS";

    /** Time type (point). */
    String TIME_TYPE = "TIME";

    /** Type attribute (point). */
    String TYPE_ATTRIBUTE = "TYPE";

    /** Unit identifier attribute (origin). */
    String UNIT_IDENTIFIER_ATTRIBUTE = "UNIT_IDENTIFIER";

    /** Unit identifier property. */
    String UNIT_IDENTIFIER_PROPERTY = "unit.identifier";
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
