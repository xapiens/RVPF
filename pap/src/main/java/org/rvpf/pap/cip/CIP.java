/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CIP.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.cip;

import org.rvpf.base.ElapsedTime;
import org.rvpf.pap.PAP;

/**
 * CIP.
 */
public interface CIP
    extends PAP
{
    /** Attributes usage (origin and point). */
    String ATTRIBUTES_USAGE = "CIP";

    /** Default TCP port. */
    int DEFAULT_TCP_PORT = 44818;

    /** Default timeout (origin). */
    ElapsedTime DEFAULT_TIMEOUT = ElapsedTime.fromMillis(2000);

    /** Default UDP port. */
    int DEFAULT_UDP_PORT = 2222;

    /** Elements attribute (point). */
    String ELEMENTS_ATTRIBUTE = "ELEMENTS";

    /** Protocol name. */
    String PROTOCOL_NAME = "CIP";

    /** Slot attribute (origin). */
    String SLOT_ATTRIBUTE = "SLOT";

    /** Tag attribute (point). */
    String TAG_ATTRIBUTE = "TAG";

    /** TCP address attribute (origin). */
    String TCP_ADDRESS_ATTRIBUTE = "TCP_ADDRESS";

    /** TCP port attribute (origin). */
    String TCP_PORT_ATTRIBUTE = "TCP_PORT";

    /** Timeout attribute (origin). */
    String TIMEOUT_ATTRIBUTE = "TIMEOUT";
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
