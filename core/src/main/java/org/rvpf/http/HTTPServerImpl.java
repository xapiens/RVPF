/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: HTTPServerImpl.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.service.metadata.app.MetadataServiceApp;
import org.rvpf.service.metadata.app.MetadataServiceAppHolderImpl;

/**
 * HTTP server implementation.
 *
 * <p>This server supplies HTTP(S) access to selected RVPF functionalities
 * supplied by the modules specified in the configuration.</p>
 *
 * <p>Listeners are specified by the "http.server.listener" properties groups,
 * realms are specified by the "http.server.realm" properties groups and
 * contexts are specified by the "http.server.context" properties groups.</p>
 *
 * <p>Each listener may be made confidential (HTTPS) by specifying a keystore. A
 * confidential listener may require 'certified' connections (client
 * certificate).</p>
 *
 * <p>Each realm must have a name and be configured by its own properties file.
 * </p>
 *
 * <p>Each context must be associated with one static resources path and/or one
 * module.</p>
 *
 * <p>Each module may configure as many servlets as necessary, each with a
 * different relative path.</p>
 */
@ThreadSafe
public final class HTTPServerImpl
    extends MetadataServiceAppHolderImpl
{
    /**
     * Gets the listener port.
     *
     * @param index The listener index.
     *
     * @return The listener port.
     */
    public Optional<String> getListenerHost(final int index)
    {
        return ((HTTPServerAppImpl) getMetadataServiceApp())
            .getListenerHost(index);
    }

    /**
     * Gets the listener port.
     *
     * @param index The listener index.
     *
     * @return The listener port.
     */
    @CheckReturnValue
    public int getListenerPort(final int index)
    {
        return ((HTTPServerAppImpl) getMetadataServiceApp())
            .getListenerPort(index);
    }

    /** {@inheritDoc}
     */
    @Override
    protected MetadataServiceApp newMetadataServiceApp()
    {
        return new HTTPServerAppImpl();
    }

    /**
     * Gets the listener count.
     *
     * @return The listener count.
     */
    @CheckReturnValue
    int getListenerCount()
    {
        return ((HTTPServerAppImpl) getMetadataServiceApp()).getListenerCount();
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
