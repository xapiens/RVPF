/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: URLHandlerFactory.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.util;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.rvpf.base.tool.Require;

/**
 * URL Handler Factory.
 *
 * <p>Allows the registration of URL handlers.</p>
 *
 * <p>Note: the first invocation of the {@link #register} method will fail with
 * an {@link Error} if an other {@link URLStreamHandlerFactory} was already
 * established via {@link URL#setURLStreamHandlerFactory}.</p>
 */
@ThreadSafe
public final class URLHandlerFactory
    implements URLStreamHandlerFactory
{
    private URLHandlerFactory() {}

    /**
     * Registers a URL protocol handler.
     *
     * @param protocol The protocol name.
     * @param handler The protocol handler.
     */
    public static synchronized void register(
            @Nonnull final String protocol,
            @Nonnull final URLStreamHandler handler)
    {
        if (_instance == null) {
            _instance = new URLHandlerFactory();
            URL.setURLStreamHandlerFactory(_instance);
        }

        _instance
            ._put(
                Require.notNull(protocol),
                Require.notNull(handler));
    }

    /** {@inheritDoc}
     */
    @Override
    public URLStreamHandler createURLStreamHandler(final String protocol)
    {
        return _handlers.get(protocol);
    }

    private void _put(final String protocol, final URLStreamHandler handler)
    {
        _handlers.put(protocol, handler);
    }

    private static URLHandlerFactory _instance;

    private final Map<String, URLStreamHandler> _handlers =
        new ConcurrentHashMap<String, URLStreamHandler>();
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
