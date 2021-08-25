/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.protocol;

import java.nio.ByteBuffer;

import etherip.types.CNService;

/**
 * Protocol for {@link CNService#CIP_MultiRequest}
 * <p>
 * Handles several embedded {@link MessageRouterProtocol} read or write requests.
 *
 * @author Kay Kasemir
 */
@SuppressWarnings({"hiding"})
public class CIPMultiRequestProtocol
    extends ProtocolAdapter
{
    public CIPMultiRequestProtocol(final MessageRouterProtocol... services)
    {
        this.services = services;
    }

    @Override
    public void decode(
            final ByteBuffer buf,
            final int available,
            final StringBuilder log)
        throws Exception
    {
        final int start = buf.position();

        // Count
        final short count = buf.getShort();

        if (log != null) {
            log.append("UINT count              : ").append(count).append("\n");
        }

        // Offset table
        final short[] offset = new short[count];

        for (int i = 0; i < count; ++i) {
            offset[i] = buf.getShort();

            if (log != null) {
                log
                    .append("UINT offset             : ")
                    .append(offset[i])
                    .append("\n");
            }
        }

        // Individual replies
        for (int i = 0; i < count; ++i) {    // Track buffer offset from start
            final int off = buf.position() - start;

            if (off != offset[i]) {
                throw new Exception(
                    "Expected response #" + (i + 1) + " at offset " + offset[i]
                    + ", not " + off);
            }

            // Determine length of this section
            final int section_length;

            if (i < count - 1) {
                section_length = offset[i + 1] - off;    // .. from offset table
            } else {
                section_length = available
                        - offset[i];    // .. from distance to end for last section
            }

            if (log != null) {
                log
                    .append("    \\/\\/ response ")
                    .append(i + 1)
                    .append(" \\/\\/ (offset ")
                    .append(off)
                    .append(" bytes)\n");
            }

            this.services[i].decode(buf, section_length, log);

            if (log != null) {
                log
                    .append("    /\\/\\ response ")
                    .append(i + 1)
                    .append(" /\\/\\\n");
            }
        }
    }

    @Override
    public void encode(
            final ByteBuffer buf,
            final StringBuilder log)
        throws Exception
    {
        final int start = buf.position();

        final short count = (short) this.services.length;

        // Encode service count
        buf.putShort(count);

        if (log != null) {
            log.append("UINT count              : ").append(count).append("\n");
        }

        // Encode offsets to individual requests
        // Offset to 1st item:
        // 2 bytes for 'count', 2 bytes for each offset
        short offset = (short) (2 + 2 * count);

        for (int i = 0; i < count; ++i) {
            buf.putShort(offset);

            if (log != null) {
                log
                    .append("UINT offset             : ")
                    .append(offset)
                    .append("\n");
            }

            // Next offset: After bytes for this request
            offset += this.services[i].getRequestSize();
        }

        for (int i = 0; i < count; ++i) {
            if (log != null) {
                log
                    .append("    \\/\\/ request ")
                    .append(i + 1)
                    .append(" \\/\\/ (offset ")
                    .append(buf.position() - start)
                    .append(" bytes)\n");
            }

            this.services[i].encode(buf, log);

            if (log != null) {
                log
                    .append("    /\\/\\ request ")
                    .append(i + 1)
                    .append(" /\\/\\\n");
            }
        }
    }

    @Override
    public int getRequestSize()
    {
        // Size: 'count' + offset to each service
        int total = 2 + 2 * this.services.length;

        // ..plus bytes of each service itself
        for (final ProtocolAdapter service: this.services) {
            total += service.getRequestSize();
        }

        return total;
    }

    @Override
    public int getResponseSize(final ByteBuffer buf)
        throws Exception
    {
        int total = 0;

        for (final ProtocolAdapter service: this.services) {
            total += service.getResponseSize(null);
        }

        return total;
    }

    final private MessageRouterProtocol[] services;
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
