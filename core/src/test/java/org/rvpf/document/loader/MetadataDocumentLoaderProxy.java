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

import java.io.Reader;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.metadata.Metadata;

/**
 * Metadata document loader proxy.
 */
public final class MetadataDocumentLoaderProxy
{
    /**
     * Constructs an instance.
     *
     * @param metadata The metadata.
     */
    public MetadataDocumentLoaderProxy(@Nonnull final Metadata metadata)
    {
        _metadataDocumentLoader = new MetadataDocumentLoader(metadata);
    }

    /**
     * Loads this document from the specified location with a supplied reader.
     *
     * @param from The location to load from.
     * @param reader The optional reader.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean loadFrom(
            @Nonnull final String from,
            @Nonnull final Optional<Reader> reader)
    {
        return _metadataDocumentLoader.loadFrom(from, reader);
    }

    /**
     * Reads the document from an XML string.
     *
     * @param xml The XML string.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean read(@Nonnull final String xml)
    {
        return _metadataDocumentLoader.read(xml);
    }

    /**
     * Sets the validating indicator.
     *
     * @param validating The validating indicator.
     */
    public void setValidating(final boolean validating)
    {
        _metadataDocumentLoader.setValidating(validating);
    }

    private final MetadataDocumentLoader _metadataDocumentLoader;
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
