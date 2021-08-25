/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPServiceAppImpl.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.service.pap;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.Attributes;
import org.rvpf.base.Entity;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Traces;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPMetadataFilter;
import org.rvpf.pap.PAPSupport;
import org.rvpf.service.metadata.MetadataService;
import org.rvpf.service.metadata.app.MetadataServiceAppImpl;

/**
 * PAP service application implementation.
 */
public abstract class PAPServiceAppImpl
    extends MetadataServiceAppImpl
{
    /**
     * Gets the protocol context for a point.
     *
     * @param point The point.
     * @param traces The context traces (optional).
     *
     * @return The protocol context (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public PAPContext getProtocolContext(
            @Nonnull final Point point,
            @Nonnull final Optional<Traces> traces)
    {
        final Optional<? extends Origin> origin = point.getOrigin();

        if (!origin.isPresent()) {
            getThisLogger().warn(PAPMessages.MISSING_ORIGIN, point);

            return null;
        }

        final PAPContext protocolContext = getProtocolContext(
            origin.get(),
            traces);

        if (protocolContext != null) {
            final Optional<PAPSupport> protocolSupport = getProtocolSupport(
                point);

            if (!protocolSupport.isPresent()) {
                getThisLogger()
                    .warn(
                        PAPMessages.NO_PROTOCOL_FOR_ENTITY,
                        point.getElementName(),
                        point.getName());

                return null;
            }

            if (!protocolSupport.get().equals(protocolContext.getSupport())) {
                getThisLogger().warn(PAPMessages.PROTOCOL_CONFLICT, point);
            }
        }

        return protocolContext;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewMetadata(final Metadata metadata)
    {
        if (!metadata.validatePointsRelationships()) {
            return false;
        }

        return super.onNewMetadata(metadata);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final MetadataService service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        if (!loadMetadata()) {
            return false;
        }

        getMetadata().cleanUp();

        return true;
    }

    /**
     * Gets a metadata filter.
     *
     * @return The metadata filter.
     */
    @Nonnull
    @CheckReturnValue
    protected MetadataFilter getMetadataFilter()
    {
        return new PAPMetadataFilter(getProtocolAttributesUsages());
    }

    /**
     * Gets the protocol attributes usages.
     *
     * @return The protocol attributes usages.
     */
    @Nonnull
    @CheckReturnValue
    protected Set<String> getProtocolAttributesUsages()
    {
        final PAPSupport[] protocolSupports = getProtocolSupports();
        final Set<String> attributesUsages = new HashSet<>(
            protocolSupports.length);

        for (final PAPSupport protocolSupport: protocolSupports) {
            attributesUsages.add(protocolSupport.getAttributesUsage());
        }

        return attributesUsages;
    }

    /**
     * Gets the protocol context for an origin.
     *
     * @param origin The origin.
     * @param traces The context traces (optional).
     *
     * @return The protocol context (null on failure).
     */
    @Nullable
    @CheckReturnValue
    protected PAPContext getProtocolContext(
            @Nonnull final Origin origin,
            @Nonnull final Optional<Traces> traces)
    {
        final Optional<PAPSupport> protocolSupport = getProtocolSupport(origin);

        if (!protocolSupport.isPresent()) {
            getThisLogger()
                .warn(
                    PAPMessages.NO_PROTOCOL_FOR_ENTITY,
                    origin.getElementName(),
                    origin.getName());

            return null;
        }

        PAPContext protocolContext = _protocolContexts
            .get(protocolSupport.get());

        if (protocolContext == null) {
            protocolContext = protocolSupport
                .get()
                .newClientContext(getMetadata(), traces);

            if (protocolContext != null) {
                _protocolContexts.put(protocolSupport.get(), protocolContext);
            }
        }

        return protocolContext;
    }

    /**
     * Gets the protocol support instance for an entity.
     *
     * @param entity The entity.
     *
     * @return The protocol support instance (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    protected Optional<PAPSupport> getProtocolSupport(
            @Nonnull final Entity entity)
    {
        for (final PAPSupport protocolSupport: getProtocolSupports()) {
            final Optional<Attributes> protocolAttributes = entity
                .getAttributes(protocolSupport.getAttributesUsage());

            if (protocolAttributes.isPresent()) {
                return Optional.of(protocolSupport);
            }
        }

        return Optional.empty();
    }

    /**
     * Gets the protocol support instances.
     *
     * @return The protocol support instances.
     */
    @Nonnull
    @CheckReturnValue
    protected PAPSupport[] getProtocolSupports()
    {
        PAPSupport[] protocolSupports = _protocolSupports.get();

        if (protocolSupports == null) {
            protocolSupports = PAPSupport
                .getProtocolSupports(getConfigProperties());

            if (_protocolSupports.compareAndSet(null, protocolSupports)) {
                for (final PAPSupport protocolSupport: protocolSupports) {
                    getThisLogger()
                        .debug(
                            PAPMessages.SUPPORTED_PROTOCOL,
                            protocolSupport.getProtocolName());
                }
            }
        }

        return protocolSupports;
    }

    /**
     * Loads the metadata.
     *
     * <p>Implemented as final by the protocol specific abstract classes.</p>
     *
     * <p>Called only by this class.</p>
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean loadMetadata()
    {
        return loadMetadata(getMetadataFilter());
    }

    private final Map<PAPSupport, PAPContext> _protocolContexts =
        new IdentityHashMap<>();
    private final AtomicReference<PAPSupport[]> _protocolSupports =
        new AtomicReference<>();
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
