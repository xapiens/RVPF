/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CIPClientContext.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.pap.cip;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Traces;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAP;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;

/**
 * CIP client context.
 */
public final class CIPClientContext
    extends CIPContext
{
    /**
     * Constructs an instance.
     *
     * @param metadata The metadata.
     * @param traces The traces (optional).
     */
    CIPClientContext(
            @Nonnull final Metadata metadata,
            @Nonnull final Optional<Traces> traces)
    {
        super(Optional.of(metadata), traces);
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
    public String getProtocolName()
    {
        return CIP.PROTOCOL_NAME;
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
    protected int getDefaultPortForRemoteOrigin()
    {
        return CIP.DEFAULT_TCP_PORT;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean isRemoteOriginNeeded(
            final Origin remoteOrigin,
            final Attributes originAttributes)
    {
        final String socketAddressString = originAttributes
            .getString(CIP.TCP_ADDRESS_ATTRIBUTE)
            .get();

        if (socketAddressString.isEmpty()) {
            getThisLogger()
                .warn(
                    PAPMessages.MISSING_ATTRIBUTE,
                    CIP.TCP_ADDRESS_ATTRIBUTE,
                    CIP.ATTRIBUTES_USAGE,
                    remoteOrigin);

            return false;
        }

        return _remoteOrigins.add(remoteOrigin.getNameInUpperCase().get());
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean isRemotePointNeeded(
            final Point remotePoint,
            final Attributes pointAttributes)
    {
        final Optional<? extends Origin> remoteOrigin = remotePoint.getOrigin();

        return (remoteOrigin.isPresent())
               && _remoteOrigins.contains(
                   remoteOrigin.get().getNameInUpperCase().get());
    }

    /** {@inheritDoc}
     */
    @Override
    protected CIPProxy newRemoteProxy(final Origin remoteOrigin)
    {
        return new CIPServerProxy(this, remoteOrigin);
    }

    /**
     * Sets the connection listener.
     *
     * @param connectionListener The connection listener.
     */
    void setConnectionListener(
            @Nonnull final PAPConnectionListener connectionListener)
    {
        for (final PAPProxy remoteProxy: getRemoteProxies()) {
            ((CIPServerProxy) remoteProxy)
                .setConnectionListener(connectionListener);
        }
    }

    private final Set<String> _remoteOrigins = new HashSet<>();
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
