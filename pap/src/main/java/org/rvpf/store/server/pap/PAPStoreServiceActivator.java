/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPStoreServiceActivator.java 3886 2019-02-07 18:42:34Z SFB $
 */

package org.rvpf.store.server.pap;

import javax.annotation.Nonnull;

import org.rvpf.base.util.Version;
import org.rvpf.pap.PAPVersion;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceImpl;

/**
 * PAP store service activator.
 */
public class PAPStoreServiceActivator
    extends ServiceActivator
{
    /**
     * Allows operation in stand alone mode.
     *
     * <p>As a program, it expects one optional argument: the configuration file
     * specification. It will default to "rvpf-config.xml".</p>
     *
     * @param args The program arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        new PAPStoreServiceActivator().run(args);
    }

    /** {@inheritDoc}
     */
    @Override
    public Version getVersion()
    {
        return new PAPVersion();
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceImpl createServiceImpl()
    {
        return createServiceImpl(PAPStoreServiceImpl.class);
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
