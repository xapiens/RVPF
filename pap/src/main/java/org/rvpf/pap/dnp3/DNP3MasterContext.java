/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3MasterContext.java 3974 2019-05-11 15:34:04Z SFB $
 */

package org.rvpf.pap.dnp3;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Traces;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAP;
import org.rvpf.pap.PAPProxy;

/**
 * DNP3 master context.
 */
public final class DNP3MasterContext
    extends DNP3Context
{
    /**
     * Constructs an instance.
     *
     * @param metadata The optional metadata.
     * @param traces The traces (optional).
     */
    public DNP3MasterContext(
            @Nonnull final Optional<Metadata> metadata,
            @Nonnull final Optional<Traces> traces)
    {
        super(metadata, traces);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean addRemotePoint(
            final Point remotePoint,
            final Attributes pointAttributes)
    {
        if (pointAttributes.getBoolean(PAP.CONNECTION_STATE_ATTRIBUTE)) {
            final Optional<? extends PAPProxy> remoteProxy = getRemoteProxy(
                remotePoint);

            if (!remoteProxy.isPresent()) {
                return false;
            }

            if (!remoteProxy.get().setConnectionStatePoint(remotePoint)) {
                return false;
            }

            registerRemotePoint(remotePoint);

            return true;
        }

        return super.addRemotePoint(remotePoint, pointAttributes);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isClientContext()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected DNP3Proxy newRemoteProxy(final Origin remoteOrigin)
    {
        return new DNP3OutstationProxy(this, remoteOrigin);
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
