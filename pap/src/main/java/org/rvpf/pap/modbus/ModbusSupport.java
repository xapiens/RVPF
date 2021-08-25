/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap.modbus;

import java.util.EnumSet;
import java.util.Optional;

import org.rvpf.base.UUID;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Traces;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPSupport;
import org.rvpf.processor.engine.pap.modbus.ModbusSplitter;

/**
 * Modbus support.
 */
public final class ModbusSupport
    extends PAPSupport.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public String getAttributesUsage()
    {
        return Modbus.ATTRIBUTES_USAGE;
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
        return Modbus.PROTOCOL_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public ModbusClient newClient(final PAPContext clientContext)
    {
        return new ModbusClient((ModbusClientContext) clientContext);
    }

    /** {@inheritDoc}
     */
    @Override
    public ModbusClientContext newClientContext(
            final Metadata metadata,
            final Optional<Traces> traces)
    {
        final ModbusClientContext clientContext = new ModbusClientContext(
            Optional.of(metadata),
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
    public ModbusServer newServer(final PAPContext serverContext)
    {
        return new ModbusServer((ModbusServerContext) serverContext);
    }

    /** {@inheritDoc}
     */
    @Override
    public ModbusServerContext newServerContext(
            final Metadata metadata,
            final String[] originNames,
            final Optional<Traces> traces)
    {
        final ModbusServerContext serverContext = new ModbusServerContext(
            Optional.of(metadata),
            originNames,
            traces);

        if (!serverContext.setUp()) {
            serverContext.tearDown();

            return null;
        }

        return serverContext;
    }

    /** {@inheritDoc}
     */
    @Override
    public ModbusSplitter newSplitter()
    {
        return new ModbusSplitter();
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
        .fromString("42278681-1953-439c-b3c1-f775cec42021")
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
