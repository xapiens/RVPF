/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.protocol;

import java.nio.ByteBuffer;

import etherip.data.CipException;

import etherip.types.CNPath;
import etherip.types.CNService;

/**
 * Message Router PDU (Protocol Data Unit)
 *
 * @author Kay Kasemir, László Pataki
 */
@SuppressWarnings("hiding")
public class MessageRouterProtocol
    extends ProtocolAdapter
{
    /**
     * Initialize
     *
     * @param service
     *            Service for request
     * @param path
     *            Path for request
     * @param body
     *            Protocol embedded in the message request/response
     */
    public MessageRouterProtocol(
            final CNService service,
            final CNPath path,
            final Protocol body)
    {
        this.service = service;
        this.path = path;
        this.body = body;
    }

    /**
     * @param status The status
     * @return Description of status
     */
    public static String decodeStatus(final int status)
    {
        // Spec 4, p.46 and 1756-RM005A-EN-E
        switch (status) {
            case 0x00:
                return "OK";
            case 0x01:
                return "extended error";
            case 0x04:
                return "unknown tag or Path error";
            case 0x05:
                return "instance not found";
            case 0x06:
                return "buffer too small, partial data only";
            case 0x08:
                return "service not supported";
            case 0x09:
                return "invalid Attribute";
            case 0x13:
                return "not enough data";
            case 0x14:
                return "attribute not supported, ext. shows attribute";
            case 0x15:
                return "too much data";
            case 0x1E:
                return "one of the MultiRequests failed";
            case 0xFF:
                return "access beyond end of object";
            default:
                break;
        }

        return "<unknown>";
    }

    /** {@inheritDoc} */
    @Override
    public void decode(
            final ByteBuffer buf,
            final int available,
            final StringBuilder log)
        throws Exception
    {
        final byte service_code = buf.get();
        final CNService reply = CNService.forCode(service_code);

        if (reply == null) {
            throw new Exception(
                "Received reply with unknown service code 0x"
                + Integer.toHexString(
                    service_code));
        }

        if (!reply.isReply()) {
            throw new Exception("Expected reply, got " + reply);
        }

        final int reserved = buf.get();

        this.status = buf.get();

        final int ext_status_size = buf.get();

        this.ext_status = new int[ext_status_size];

        for (int i = 0; i < ext_status_size; ++i) {
            this.ext_status[i] = buf.getShort();
        }

        // Followed by data...
        if (log != null) {
            log.append("MR Response\n");
            log.append("USINT service           : ").append(reply).append("\n");
            log
                .append("USINT reserved          : 0x")
                .append(Integer.toHexString(reserved))
                .append("\n");
            log
                .append("USINT status            : 0x")
                .append(Integer.toHexString(this.status))
                .append(" (")
                .append(")\n");
            log
                .append("USINT ext. stat. size   : 0x")
                .append(Integer.toHexString(ext_status_size))
                .append("\n");

            for (final int ext: this.ext_status) {
                log
                    .append("USINT ext status        : 0x")
                    .append(Integer.toHexString(ext))
                    .append(" (")
                    .append(")\n");
            }
        }

        final CNService expected_reply = this.service.getReply();

        if (this.status != 0) {
            if (this.status == 6) {    // Not an error, we need to ask for remaining
                this.partialTransfert = true;
            } else {
                if (ext_status_size > 0) {
                    throw new CipException(this.status, this.ext_status[0]);
                }

                throw new CipException(this.status, 0);
            }
        }

        if ((expected_reply != null) && (expected_reply != reply)) {
            throw new Exception(
                "Expected " + expected_reply + ", got " + reply);
        }

        this.body.decode(buf, available - 4 - 2 * ext_status_size, log);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    public void encode(
            final ByteBuffer buf,
            final StringBuilder log)
        throws Exception
    {
        buf.put(this.service.getCode());
        this.path.encode(buf, log);

        if (log != null) {
            log.append("MR Request\n");
            log
                .append("USINT service           : ")
                .append(this.service)
                .append("\n");
            log
                .append("USINT path              : ")
                .append(this.path)
                .append("\n");
        }

        this.body.encode(buf, log);
    }

    /** @return Extended status codes of response */
    public int[] getExtendedStatus()
    {
        return this.ext_status;
    }

    /** {@inheritDoc} */
    @Override
    public int getRequestSize()
    {
        return 2 + this.path.getRequestSize() + this.body.getRequestSize();
    }

    /** {@inheritDoc} */
    @Override
    public int getResponseSize(final ByteBuffer buf)
        throws Exception
    {
        throw new IllegalStateException("Unknown response size");
    }

    /** @return Status code of response */
    public int getStatus()
    {
        return this.status;
    }

    public boolean isPartialTransfert()
    {
        return partialTransfert;
    }

    final protected Protocol body;
    private int[] ext_status = new int[0];
    final private CNPath path;
    final private CNService service;
    private int status = 0;
    private boolean partialTransfert = false;;
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
