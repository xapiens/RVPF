/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusClientContext.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.pap.modbus;

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
import org.rvpf.pap.PAPProxy;

/**
 * Modbus client context.
 */
public final class ModbusClientContext
    extends ModbusContext
{
    /**
     * Constructs an instance.
     *
     * @param metadata The optional metadata.
     * @param traces The traces (optional).
     */
    ModbusClientContext(
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
    protected int getDefaultPortForRemoteOrigin()
    {
        return Modbus.DEFAULT_PORT;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean isRemoteOriginNeeded(
            final Origin origin,
            final Attributes originAttributes)
    {
        final String[] socketAddressStrings = originAttributes
            .getStrings(Modbus.SOCKET_ADDRESS_ATTRIBUTE);

        if ((socketAddressStrings.length != 1)
                || !"*".equals(socketAddressStrings[0].trim())) {
            return _origins.add(origin.getNameInUpperCase().get());
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean isRemotePointNeeded(
            final Point point,
            final Attributes pointAttributes)
    {
        final Optional<? extends Origin> origin = point.getOrigin();

        return origin.isPresent()
               && _origins.contains(origin.get().getNameInUpperCase().get());
    }

    /** {@inheritDoc}
     */
    @Override
    protected ModbusServerProxy newRemoteProxy(final Origin origin)
    {
        return new ModbusServerProxy(this, origin, getTraces());
    }

    private final Set<String> _origins = new HashSet<>();
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
