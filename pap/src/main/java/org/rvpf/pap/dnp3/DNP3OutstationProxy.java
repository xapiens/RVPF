/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3OutstationProxy.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3;

import javax.annotation.Nonnull;

import org.rvpf.base.Origin;
import org.rvpf.pap.PAPProxy;

/**
 * DNP3 outstation proxy.
 */
public final class DNP3OutstationProxy
    extends DNP3Proxy
{
    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param origin The origin representing the remote.
     */
    public DNP3OutstationProxy(
            @Nonnull final DNP3Context context,
            @Nonnull final Origin origin)
    {
        super(context, origin);
    }

    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param name The name for the synthesized origin.
     */
    public DNP3OutstationProxy(
            @Nonnull final DNP3Context context,
            @Nonnull final String name)
    {
        super(context, name);
    }

    private DNP3OutstationProxy(@Nonnull final DNP3OutstationProxy other)
    {
        super(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public PAPProxy copy()
    {
        return new DNP3OutstationProxy(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isMaster()
    {
        return false;
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
