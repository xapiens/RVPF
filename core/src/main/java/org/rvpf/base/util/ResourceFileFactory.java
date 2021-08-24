/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ResourceFileFactory.java 4079 2019-06-12 13:40:21Z SFB $
 */

package org.rvpf.base.util;

import java.io.File;

import java.net.URL;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;

/**
 * Resource file factory.
 */
public final class ResourceFileFactory
{
    /**
     * No instances.
     */
    private ResourceFileFactory() {}

    /**
     * Returns a new resource file from a specification.
     *
     * @param spec The specification.
     *
     * @return The resource file (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public static File newResourceFile(@Nonnull String spec)
    {
        final File resourceFile;

        spec = spec.trim();

        if (spec.startsWith(RESOURCE_PREFIX)) {
            spec = spec.substring(RESOURCE_PREFIX.length());

            final URL url = Thread
                .currentThread()
                .getContextClassLoader()
                .getResource(spec);

            if (url == null) {
                Logger
                    .getInstance(ResourceFileFactory.class)
                    .warn(BaseMessages.RESOURCE_NOT_FOUND, spec);
                resourceFile = null;
            } else if (FILE_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
                resourceFile = new File(url.getFile()).getAbsoluteFile();
            } else {
                Logger
                    .getInstance(ResourceFileFactory.class)
                    .warn(
                        BaseMessages.PROTOCOL_NOT_SUPPORTED,
                        url.getProtocol(),
                        url);
                resourceFile = null;
            }
        } else {
            resourceFile = new File(spec);
        }

        return resourceFile;
    }

    /** File protocol. */
    public static final String FILE_PROTOCOL = "file";

    /** Resource prefix. */
    public static final String RESOURCE_PREFIX =
        ResourceURLHandler.RESOURCE_PROTOCOL + ":";
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
