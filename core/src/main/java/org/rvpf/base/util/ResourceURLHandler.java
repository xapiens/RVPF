/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ResourceURLHandler.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import javax.annotation.concurrent.Immutable;
import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Message;

/** Resource URL Handler.
 *
 * <p>Allows a URL to specify a resource on the class path by using 'resource'
 * as the protocol name. The resource path is put immediately after the colon;
 * if this path begins with a slash, it is an absolute path.</p>
 */
@Immutable
public final class ResourceURLHandler
    extends URLStreamHandler
{
    /** {@inheritDoc}
     */
    @Override
    protected URLConnection openConnection(URL url)
        throws IOException
    {
        String name = url.getHost();
        String file = url.getFile();

        if (file != null) {
            while (file.startsWith("/")) {
                file = file.substring(1);
            }
            name += file;
        }

        url = Thread.currentThread()
            .getContextClassLoader()
            .getResource(name);
        if (url == null) {
            throw new FileNotFoundException(
                Message.format(BaseMessages.RESOURCE_NOT_FOUND, name));
        }

        return url.openConnection();
    }

    /** Resource protocol. */
    public static final String RESOURCE_PROTOCOL = "resource";
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
