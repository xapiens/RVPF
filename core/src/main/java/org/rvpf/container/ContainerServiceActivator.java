/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ContainerServiceActivator.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.container;

import javax.annotation.Nonnull;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceImpl;

/** Container service activator.
 *
 * @see ContainerServiceImpl
 */
public final class ContainerServiceActivator
    extends ServiceActivator
{
    /** Allows operation in stand alone mode.
     *
     * <p>As a program, it expects one optional argument: the configuration file
     * specification. It will default to "rvpf-config.xml".</p>
     *
     * @param args The program arguments.
     */
    public static void main(@Nonnull final String[] args)
    {
        new ContainerServiceActivator().run(args);
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceImpl createServiceImpl()
    {
        return createServiceImpl(ContainerServiceImpl.class);
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
