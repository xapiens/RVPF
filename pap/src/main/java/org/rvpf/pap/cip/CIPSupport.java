/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap.cip;

import java.util.EnumSet;
import java.util.Optional;

import org.rvpf.base.UUID;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Traces;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPServer;
import org.rvpf.pap.PAPSupport;
import org.rvpf.processor.engine.pap.cip.CIPSplitter;

/**
 * CIP support.
 */
public final class CIPSupport
    extends PAPSupport.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public String getAttributesUsage()
    {
        return CIP.ATTRIBUTES_USAGE;
    }

    /** {@inheritDoc}
     */
    @Override
    public UUID getMetadataFilterUUID()
    {
        return METADATA_FILTER_UUID;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getProtocolName()
    {
        return CIP.PROTOCOL_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public CIPClient newClient(final PAPContext clientContext)
    {
        return new CIPClient((CIPClientContext) clientContext);
    }

    /** {@inheritDoc}
     */
    @Override
    public CIPClientContext newClientContext(
            final Metadata metadata,
            final Optional<Traces> traces)
    {
        final CIPClientContext clientContext = new CIPClientContext(
            metadata,
            traces);

        if (!clientContext.setUp()) {
            clientContext.tearDown();

            return null;
        }

        return clientContext;
    }

    /** {@inheritDoc}
     */
    @Override
    public PAPServer newServer(final PAPContext serverContext)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public PAPContext newServerContext(
            final Metadata metadata,
            final String[] originNames,
            final Optional<Traces> traces)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public CIPSplitter newSplitter()
    {
        return new CIPSplitter();
    }

    /** {@inheritDoc}
     */
    @Override
    public String supportedValueTypeCodes()
        throws SessionException
    {
        return Externalizer.ValueType
            .setToString(
                EnumSet
                    .of(
                            Externalizer.ValueType.BOOLEAN,
                                    Externalizer.ValueType.BYTE,
                                    Externalizer.ValueType.DOUBLE,
                                    Externalizer.ValueType.FLOAT,
                                    Externalizer.ValueType.INTEGER,
                                    Externalizer.ValueType.LONG,
                                    Externalizer.ValueType.SHORT));
    }

    /** UUID associated with the metadata filter. */
    public static final UUID METADATA_FILTER_UUID = UUID
        .fromString("9e2e38c6-be4b-4c1d-914a-e72f81b4210d")
        .get();
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
