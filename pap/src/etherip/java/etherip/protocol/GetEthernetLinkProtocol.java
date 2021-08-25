/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.protocol;

import java.nio.ByteBuffer;

import etherip.data.EthernetLink;
import etherip.data.InterfaceFlags;

/**
 * Ethernet Link Object decoder
 *
 * @see CIP_VOL2_1.4: 5-4.3.2
 *      <p>
 *      Table 5-4.4 Interface Flags Reserved: Shall be set to zero
 * @author László Pataki
 */
@SuppressWarnings(
{
    "boxing", "javadoc"
})
public class GetEthernetLinkProtocol
    extends ProtocolAdapter
{
    @Override
    public void decode(
            final ByteBuffer buf,
            final int available,
            final StringBuilder log)
        throws Exception
    {
        this.ethernetLink = new EthernetLink();

        byte[] raw = new byte[4];

        buf.get(raw);
        this.ethernetLink
            .setInterfaceSpeed(
                new Integer(
                    raw[3] & 0xFF) + new Integer(
                            raw[2] & 0xFF) + new Integer(
                                    raw[1] & 0xFF) + new Integer(
                                            raw[0] & 0xFF));

        buf.get(raw);

        final InterfaceFlags interfaceFlags = new InterfaceFlags();

        interfaceFlags.setActiveLink(((raw[0] >> 0) & 0x1) == 1);
        interfaceFlags.setFullDuplex(((raw[0] >> 1) & 0x1) == 1);
        interfaceFlags.setNegotiationStatus((short) ((raw[0] >> 2) & 0x7));
        interfaceFlags
            .setManualSettingRequiresReset(((raw[0] >> 5) & 0x1) == 1);
        interfaceFlags.setLocalHardwareFault(((raw[0] >> 6) & 0x1) == 1);

        // Reserved: Shall be set to zero
        this.ethernetLink.setInterfaceFlags(interfaceFlags);

        raw = new byte[6];
        buf.get(raw);
        this.ethernetLink
            .setPhysicalAddress(
                String
                    .format(
                            "%02X",
                                    raw[0]) + String.format(
                                            ":%02X",
                                                    raw[1]) + String.format(
                                                            ":%02X",
                                                                    raw[2]) + String.format(
                                                                            ":%02X",
                                                                                    raw[3]) + String.format(
                                                                                            ":%02X",
                                                                                                    raw[4]) + String.format(
                                                                                                            ":%02X",
                                                                                                                    raw[5]));

        if (log != null) {
            log.append("EthernetLink value      : " + this.ethernetLink);
        }

        // Skip remaining bytes
        final int rest = available - 4 - 4 - 6;

        if (rest > 0) {
            buf.position(buf.position() + rest);
        }
    }

    /** @return Value read from response */
    final public EthernetLink getValue()
    {
        return this.ethernetLink;
    }

    private EthernetLink ethernetLink;
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
