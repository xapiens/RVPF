/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.protocol;

import java.nio.ByteBuffer;

import etherip.types.CIPData;
import etherip.types.CNService;

/**
 * Protocol body for {@link CNService#CIP_ReadData}
 *
 * @author Kay Kasemir
 */
@SuppressWarnings({"hiding"})
public class CIPReadDataProtocol
    extends ProtocolAdapter
{
    /**
     * Create a read protocol message that requests a single element
     */
    public CIPReadDataProtocol()
    {
        this.count = 1;
    }

    /**
     * Create a read protocol message that requests one or more elements if request is an array
     *
     * @param count
     */
    public CIPReadDataProtocol(final short count)
    {
        this.count = count;
    }

    @Override
    public void decode(
            final ByteBuffer buf,
            final int available,
            final StringBuilder log)
        throws Exception
    {
        if (available <= 0) {
            this.data = null;

            if (log != null) {
                log.append("USINT type, data        : - nothing-\n");
            }

            return;
        }

        final CIPData.Type type = CIPData.Type.forCode(buf.getShort());
        final byte[] raw = new byte[available - 2];

        buf.get(raw);
        this.data = new CIPData(type, raw);

        if (log != null) {
            log
                .append("USINT type, data        : ")
                .append(this.data)
                .append("\n");
        }
    }

    @Override
    public void encode(final ByteBuffer buf, final StringBuilder log)
    {
        buf.putShort(this.count);    // elements

        if (log != null) {
            log
                .append("USINT elements          : ")
                .append(this.count)
                .append("\n");
        }
    }

    final public CIPData getData()
    {
        return this.data;
    }

    @Override
    public int getRequestSize()
    {
        return 2;
    }

    private final short count;
    private CIPData data;
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
