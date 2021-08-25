/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.protocol;

import static etherip.types.CNPath.ConnectionManager;

import java.nio.ByteBuffer;

import etherip.types.CNService;

/**
 * @see CIP_Vol1_3.3: 3-5.5.5: Get_Connection_Data Service
 * @author László Pataki
 */
@SuppressWarnings(
{
    "hiding", "javadoc"
})
public class ConnectionDataProtocol
    extends ProtocolAdapter
{
    public ConnectionDataProtocol(final Protocol body)
    {
        this.encoder = new MessageRouterProtocol(
            CNService.Get_Connection_Data,
            ConnectionManager(),
            new ProtocolAdapter());
        this.body = body;
    }

    /** {@inheritDoc} */
    @Override
    public void decode(
            final ByteBuffer buf,
            final int available,
            final StringBuilder log)
        throws Exception
    {
        this.body.decode(buf, available, log);
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
        this.encoder.encode(buf, log);

        final short connectionNumber = (short) 1;

        buf.putShort(connectionNumber);
    }

    /** {@inheritDoc} */
    @Override
    public int getRequestSize()
    {
        return this.encoder.getRequestSize() + 2;
    }

    final private Protocol body;
    final private ProtocolEncoder encoder;
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
