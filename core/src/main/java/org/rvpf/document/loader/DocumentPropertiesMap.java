/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentPropertiesMap.java 3946 2019-05-02 13:49:32Z SFB $
 */

package org.rvpf.document.loader;

import java.io.IOException;
import java.io.Reader;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.service.ServiceMessages;

/**
 * Document properties map.
 */
public final class DocumentPropertiesMap
    extends LinkedHashMap<String, String>
{
    /**
     * Fetches properties from a document stream.
     *
     * <p>The returned map respects the insertion order.</p>
     *
     * @param documentStream The document stream.
     * @param reader An optional input stream.
     *
     * @return A map of the properties.
     *
     * @throws IOException On file access error.
     */
    @Nonnull
    @CheckReturnValue
    public static DocumentPropertiesMap fetch(
            @Nonnull final DocumentStream documentStream,
            @Nonnull final Optional<Reader> reader)
        throws IOException
    {
        final DocumentPropertiesMap map = new DocumentPropertiesMap();
        final Properties properties = new Properties()
        {
            @Override
            public Object put(final Object key, final Object value)
            {
                final String previousValue = map
                    .put((String) key, (String) value);

                if (previousValue != null) {
                    Logger
                        .getInstance(DocumentPropertiesMap.class)
                        .debug(
                            ServiceMessages.PREVIOUS_VALUE_OVERRIDDEN,
                            key,
                            value);
                }

                return previousValue;
            }

            private static final long serialVersionUID = 1L;
        };

        map._setStamp(documentStream.getStamp());

        if (reader.isPresent()) {
            properties.load(reader.get());
            reader.get().close();
        } else {
            if (documentStream
                .getFromURL()
                .getFile()
                .toLowerCase(Locale.ROOT)
                .endsWith(".xml")) {
                properties.loadFromXML(documentStream);
            } else {
                properties.load(documentStream);
            }

            documentStream.close();
        }

        return map;
    }

    /**
     * Gets the input last modified time stamp.
     *
     * @return The optional input last modified time stamp.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<DateTime> getStamp()
    {
        return _stamp;
    }

    private void _setStamp(final Optional<DateTime> stamp)
    {
        _stamp = stamp;
    }

    private static final long serialVersionUID = 1L;

    private Optional<DateTime> _stamp = Optional.empty();
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
