/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap.dnp3;

import java.util.EnumSet;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.UUID;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Traces;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPSupport;
import org.rvpf.processor.engine.pap.dnp3.DNP3Splitter;

/**
 * DNP3 support.
 */
public final class DNP3Support
    extends PAPSupport.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public String getAttributesUsage()
    {
        return DNP3.ATTRIBUTES_USAGE;
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
        return DNP3.PROTOCOL_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public DNP3Master newClient(final PAPContext masterContext)
    {
        return newMaster((DNP3MasterContext) masterContext, (short) 1);
    }

    /** {@inheritDoc}
     */
    @Override
    public DNP3MasterContext newClientContext(
            final Metadata metadata,
            final Optional<Traces> traces)
    {
        final DNP3MasterContext masterContext = new DNP3MasterContext(
            Optional.of(metadata),
            traces);

        if (!masterContext.setUp()) {
            masterContext.tearDown();

            return null;
        }

        return masterContext;
    }

    /**
     * Creates a new master.
     *
     * @param masterContext The master context.
     * @param localDeviceAddress The local device address.
     *
     * @return The new master.
     */
    @SuppressWarnings("static-method")
    @Nonnull
    @CheckReturnValue
    public DNP3Master newMaster(
            @Nonnull final DNP3MasterContext masterContext,
            final short localDeviceAddress)
    {
        return new DNP3Master(masterContext, localDeviceAddress);
    }

    /**
     * Returns a new outstation.
     *
     * @param outstationContext The outstation context.
     *
     * @return The new outstation.
     */
    @SuppressWarnings("static-method")
    @Nonnull
    @CheckReturnValue
    public DNP3Outstation newOutstation(
            @Nonnull final DNP3OutstationContext outstationContext)
    {
        return new DNP3Outstation(outstationContext);
    }

    /**
     * Returns a new outstation context.
     *
     * @param metadata The metadata.
     * @param originNames The origin names.
     * @param traces The traces (optional).
     *
     * @return The outstation context (null on failure).
     */
    @SuppressWarnings("static-method")
    @Nullable
    @CheckReturnValue
    public DNP3OutstationContext newOutstationContext(
            @Nonnull final Metadata metadata,
            @Nonnull final String[] originNames,
            @Nonnull final Optional<Traces> traces)
    {
        final DNP3OutstationContext outstationContext =
            new DNP3OutstationContext(
                Optional.of(metadata),
                originNames,
                traces);

        if (!outstationContext.setUp()) {
            outstationContext.tearDown();

            return null;
        }

        return outstationContext;
    }

    /** {@inheritDoc}
     */
    @Override
    public DNP3Outstation newServer(final PAPContext outstationContext)
    {
        return newOutstation((DNP3OutstationContext) outstationContext);
    }

    /** {@inheritDoc}
     */
    @Override
    public DNP3OutstationContext newServerContext(
            final Metadata metadata,
            final String[] originNames,
            final Optional<Traces> traces)
    {
        return newOutstationContext(metadata, originNames, traces);
    }

    /** {@inheritDoc}
     */
    @Override
    public DNP3Splitter newSplitter()
    {
        return new DNP3Splitter();
    }

    /** {@inheritDoc}
     */
    @Override
    public String supportedValueTypeCodes()
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
        .fromString("3f7f1e8e-802d-4d74-ac41-e3132e9e0af9")
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
