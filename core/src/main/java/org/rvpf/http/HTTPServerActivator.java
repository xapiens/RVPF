/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: HTTPServerActivator.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.http;

import javax.annotation.Nonnull;

import org.rvpf.base.util.Version;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.ServiceImpl;

/**
 * HTTP server activator.
 *
 * @see HTTPServerImpl
 */
public final class HTTPServerActivator
    extends ServiceActivator
    implements HTTPServerActivatorMBean
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
        new HTTPServerActivator().run(args);
    }

    /** {@inheritDoc}
     */
    @Override
    public String[] getAddresses()
    {
        final HTTPServerImpl service = (HTTPServerImpl) getService();
        final int listenerCount = service.getListenerCount();
        final String[] adresses = new String[listenerCount];

        for (int i = 0; i < listenerCount; ++i) {
            adresses[i] = service.getListenerHost(i).orElse(null);
        }

        return adresses;
    }

    /** {@inheritDoc}
     */
    @Override
    public int[] getPorts()
    {
        final HTTPServerImpl service = (HTTPServerImpl) getService();
        final int listenerCount = service.getListenerCount();
        final int[] ports = new int[listenerCount];

        for (int i = 0; i < listenerCount; ++i) {
            ports[i] = service.getListenerPort(i);
        }

        return ports;
    }

    /** {@inheritDoc}
     */
    @Override
    public Version getVersion()
    {
        return new HTTPVersion();
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceImpl createServiceImpl()
    {
        return createServiceImpl(HTTPServerImpl.class);
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
