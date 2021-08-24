/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.document.loader;

import java.io.FileNotFoundException;
import java.io.Reader;

import java.net.URL;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;

/**
 * Document reader proxy.
 */
public final class DocumentReaderProxy
{
    /**
     * No instances.
     */
    private DocumentReaderProxy() {}

    /**
     * Creates a document reader.
     *
     * @param fromURL The document URL.
     * @param reader An optional reader.
     * @param stamp An optional reference time for the reader.
     *
     * @return A new document reader.
     *
     * @throws FileNotFoundException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    public static DocumentReader create(
            @Nonnull final URL fromURL,
            @Nonnull final Optional<Reader> reader,
            @Nonnull final Optional<DateTime> stamp)
        throws FileNotFoundException
    {
        return DocumentReader.create(fromURL, reader, stamp);
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
