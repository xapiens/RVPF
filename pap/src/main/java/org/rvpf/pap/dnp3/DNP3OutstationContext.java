/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3OutstationContext.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.pap.dnp3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Traces;
import org.rvpf.metadata.Metadata;

/**
 * DNP3 outstation context.
 */
public final class DNP3OutstationContext
    extends DNP3Context
{
    /**
     * Constructs an instance.
     *
     * @param metadata The optional metadata.
     * @param originNames The origin names.
     * @param traces The traces (optional).
     */
    public DNP3OutstationContext(
            @Nonnull final Optional<Metadata> metadata,
            @Nonnull final String[] originNames,
            @Nonnull final Optional<Traces> traces)
    {
        super(metadata, traces);

        for (final String originName: originNames) {
            _origins.put(originName.toUpperCase(Locale.ROOT), null);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isClientContext()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean isRemoteOriginNeeded(
            final Origin origin,
            final Attributes originAttributes)
    {
        return _origins.put(origin.getNameInUpperCase().get(), origin) == null;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean isRemotePointNeeded(
            final Point point,
            final Attributes pointAttributes)
    {
        final Optional<? extends Origin> origin = point.getOrigin();

        if (!origin.isPresent()
                || (!point.getAttributes(DNP3.ATTRIBUTES_USAGE).isPresent())) {
            return false;
        }

        return _origins.get(origin.get().getNameInUpperCase().get()) != null;
    }

    /** {@inheritDoc}
     */
    @Override
    protected DNP3Proxy newRemoteProxy(final Origin remoteOrigin)
    {
        return new DNP3MasterProxy(this, remoteOrigin);
    }

    private final Map<String, Origin> _origins = new HashMap<>();
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
