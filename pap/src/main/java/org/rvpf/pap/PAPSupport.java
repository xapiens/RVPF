/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.pap;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.ClassDef;
import org.rvpf.base.UUID;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.tool.Traces;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.metadata.Metadata;
import org.rvpf.processor.engine.pap.PAPSplitter;

/**
 * PAP support.
 */
public interface PAPSupport
{
    /**
     * Gets the protocol support instances.
     *
     * @param properties The properties specifying the support classDefs.
     *
     * @return The protocol support instances.
     */
    @Nonnull
    @CheckReturnValue
    static PAPSupport[] getProtocolSupports(
            @Nonnull final KeyedValues properties)
    {
        final PAPSupport[] protocolSupports;

        final ClassDef[] classDefs = properties
            .getClassDefs(PAP.PROTOCOL_SUPPORT_CLASS_PROPERTIES);

        protocolSupports = new PAPSupport[classDefs.length];

        for (int i = 0; i < protocolSupports.length; ++i) {
            protocolSupports[i] = classDefs[i].createInstance(PAPSupport.class);
        }

        return protocolSupports;
    }

    /**
     * Gets the attributes usage for the protocol.
     *
     * @return The attributes usage.
     */
    @Nonnull
    @CheckReturnValue
    String getAttributesUsage();

    /**
     * Gets the metadata filter UUID.
     *
     * @return The metadata filter UUID.
     */
    @Nonnull
    @CheckReturnValue
    UUID getMetadataFilterUUID();

    /**
     * Gets the protocol name.
     *
     * @return The protocol name.
     */
    @Nonnull
    @CheckReturnValue
    String getProtocolName();

    /**
     * Returns a new client.
     *
     * @param clientContext The client context.
     *
     * @return The new client.
     */
    @Nonnull
    @CheckReturnValue
    PAPClient newClient(@Nonnull PAPContext clientContext);

    /**
     * Returns a new client context.
     *
     * @param metadata The metadata.
     * @param traces The traces (optional).
     *
     * @return The client context (null on failure).
     */
    @Nullable
    @CheckReturnValue
    PAPContext newClientContext(
            @Nonnull Metadata metadata,
            @Nonnull Optional<Traces> traces);

    /**
     * Returns a new server.
     *
     * @param serverContext The server context.
     *
     * @return The new server.
     */
    @Nonnull
    @CheckReturnValue
    PAPServer newServer(@Nonnull PAPContext serverContext);

    /**
     * Returns a new server context.
     *
     * @param metadata The metadata.
     * @param originNames The origin names.
     * @param traces The traces (optional).
     *
     * @return The server context (null on failure).
     */
    @Nullable
    @CheckReturnValue
    PAPContext newServerContext(
            @Nonnull Metadata metadata,
            @Nonnull final String[] originNames,
            @Nonnull Optional<Traces> traces);

    /**
     * Returns a new splitter.
     *
     * @return The splitter.
     */
    @Nonnull
    @CheckReturnValue
    PAPSplitter newSplitter();

    /**
     * Returns a string of supported value type codes.
     *
     * @return The string of supported value type codes.
     *
     * @throws SessionException On session exception.
     */
    @Nonnull
    @CheckReturnValue
    String supportedValueTypeCodes()
        throws SessionException;

    /**
     * Abstract.
     */
    abstract class Abstract
        implements PAPSupport
    {
        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object)
        {
            return (object != null) && (object.getClass() == getClass());
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            return getClass().hashCode();
        }
    }
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
