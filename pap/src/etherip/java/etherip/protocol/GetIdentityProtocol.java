/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.protocol;

import java.nio.ByteBuffer;

import java.util.Arrays;
import java.util.logging.Logger;

import etherip.data.Identity;

/**
 * Identity Object decoder
 *
 * @see CIP_vol2_v1.4: 5-3.2.2
 * @author László Pataki
 */
@SuppressWarnings(
{
    "boxing", "javadoc"
})
public class GetIdentityProtocol
    extends ProtocolAdapter
{
    @Override
    public void decode(
            final ByteBuffer buf,
            final int available,
            final StringBuilder oldLog)
        throws Exception
    {
        this.identity = new Identity();

        byte[] raw = new byte[2];

        buf.get(raw);
        this.identity
            .setVendorId(
                new Integer(raw[0] & 0xFF) + new Integer(raw[1] & 0xFF));

        buf.get(raw);
        this.identity
            .setDeviceType(
                new Integer(raw[0] & 0xFF) + new Integer(raw[1] & 0xFF));

        buf.get(raw);
        this.identity
            .setProductCode(
                new Integer(raw[0] & 0xFF) + new Integer(raw[1] & 0xFF));

        buf.get(raw);
        this.identity
            .setRevision(
                new Integer[] {new Integer(
                    raw[0] & 0xFF), new Integer(raw[1] & 0xFF)});

        raw = new byte[2];
        buf.get(raw);
        this.identity
            .setStatus(
                String
                    .format("0x%02X", raw[1]) + String.format("%02X", raw[0]));

        raw = new byte[4];
        buf.get(raw);
        this.identity
            .setSerialNumber(
                String
                    .format(
                            "0x%02X",
                                    raw[3]) + String.format(
                                            "%02X",
                                                    raw[2]) + String.format(
                                                            "%02X",
                                                                    raw[1]) + String.format(
                                                                            "%02X",
                                                                                    raw[0]));

        raw = new byte[1];
        buf.get(raw);

        final int len = new Integer(raw[0] & 0xFF);

        raw = new byte[len];
        buf.get(raw);
        this.identity
            .setProductName(new String(Arrays.copyOfRange(raw, 0, raw.length)));

        if (this.log != null) {
            this.log.finest("Identity value      : " + this.identity);
        }

        // Skip remaining bytes
        final int rest = available - 1 - 2 - 2 - 2 - 2 - 2 - 4 - 1 - len;

        if (rest > 0) {
            buf.position(buf.position() + rest);
        }
    }

    /** @return Value read from response */
    final public Identity getValue()
    {
        return this.identity;
    }

    final public Logger log = Logger
        .getLogger(GetIdentityProtocol.class.getName());
    private Identity identity;
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
