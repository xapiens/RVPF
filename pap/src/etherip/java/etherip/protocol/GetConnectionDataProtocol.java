/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.protocol;

import java.nio.ByteBuffer;

import java.util.logging.Logger;

import etherip.data.CipException;
import etherip.data.ConnectionData;

/**
 * Connection Data decoder
 *
 * @see CIP_Vol2_1.4: 3-5.5.5
 * @author László Pataki
 */
@SuppressWarnings(
{
    "boxing", "javadoc"
})
public class GetConnectionDataProtocol
    extends ProtocolAdapter
{
    public static String bytesToHex(final byte[] bytes)
    {
        final StringBuilder sb = new StringBuilder();

        for (final byte b: bytes) {
            sb.append(String.format("%02X ", b));
        }

        return sb.toString();
    }

    @Override
    public void decode(
            final ByteBuffer buf,
            final int available,
            final StringBuilder oldLog)
        throws Exception
    {
        this.connectionData = new ConnectionData();

        // Service
        final byte[] raw = new byte[1];

        buf.get(raw);

        // CIP Connection Manager byte
        buf.get(raw);

        // Statuses
        buf.get(raw);
        this.connectionData.setConnectionGeneralStatus(raw[0]);
        buf.get(raw);
        this.connectionData.setConnectionAdditionalStatus(raw[0]);

        if (log != null) {
            log.finest("ConnectionData value      : " + this.connectionData);
        }

        // Skip remaining bytes
        final int rest = available - 1 - 1 - 1 - 1;

        if (rest > 0) {
            if (log != null) {
                final byte[] arr = new byte[buf.remaining()];

                buf.get(arr);
                log.warning("\nRemaining bytes: " + bytesToHex(arr));
            }

            buf.position(buf.position() + rest);
        }

        if (this.connectionData.getConnectionGeneralStatus() != 0) {
            throw new CipException(
                this.connectionData.getConnectionGeneralStatus(),
                (byte) 0);
        }
    }

    /** @return Value read from response */
    final public ConnectionData getValue()
    {
        return this.connectionData;
    }

    final public static Logger log = Logger
        .getLogger(GetConnectionDataProtocol.class.getName());

    private ConnectionData connectionData;
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
