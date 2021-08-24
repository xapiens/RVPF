/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteExecutorActivator.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.processor.engine.executor.remote;

import javax.annotation.Nonnull;

import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceImpl;

/**
 * Remote executor activator.
 *
 * @see RemoteExecutorImpl
 */
public class RemoteExecutorActivator
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
        new RemoteExecutorActivator().run(args);
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceImpl createServiceImpl()
    {
        return createServiceImpl(RemoteExecutorImpl.class);
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
