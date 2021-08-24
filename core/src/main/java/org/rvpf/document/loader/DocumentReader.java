/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentReader.java 3943 2019-04-30 20:18:33Z SFB $
 */

package org.rvpf.document.loader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.UnicodeStreamReader;
import org.rvpf.service.ServiceMessages;

/**
 * Document reader.
 */
final class DocumentReader
    extends BufferedReader
{
    /**
     * Constructs an instance.
     *
     * @param reader The underlying reader.
     * @param fromURL The URL representing the reader.
     * @param stamp The last modified time stamp.
     */
    private DocumentReader(
            final Optional<Reader> reader,
            final URL fromURL,
            final Optional<DateTime> stamp)
    {
        super(reader.get());

        _fromURL = fromURL;
        _stamp = stamp;
    }

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
    static DocumentReader create(
            @Nonnull URL fromURL,
            @Nonnull Optional<Reader> reader,
            @Nonnull Optional<DateTime> stamp)
        throws FileNotFoundException
    {
        if (!reader.isPresent()) {
            try {
                final DocumentStream documentStream = DocumentStream
                    .create(fromURL);

                fromURL = documentStream.getFromURL();
                stamp = documentStream.getStamp();

                if (documentStream.isFile()) {
                    reader = Optional
                        .of(
                            new UnicodeStreamReader(
                                documentStream.getStream()));
                } else {
                    final Optional<String> encoding = documentStream
                        .getEncoding();

                    reader = Optional
                        .of(
                            new InputStreamReader(
                                documentStream.getStream(),
                                (encoding.isPresent())
                                ? encoding.get(): StandardCharsets.UTF_8
                                        .name()));
                }
            } catch (final FileNotFoundException exception) {
                throw exception;
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        } else if (!stamp.isPresent()) {
            final long lastModified = new File(fromURL.getPath())
                .lastModified();

            if (lastModified > 0) {
                stamp = Optional.of(DateTime.fromMillis(lastModified));
            } else {
                Logger
                    .getInstance(DocumentReader.class)
                    .warn(ServiceMessages.TIME_NOT_AVAILABLE, fromURL);
            }
        }

        return new DocumentReader(reader, fromURL, stamp);
    }

    /**
     * Gets the URL.
     *
     * @return The URL.
     */
    @Nonnull
    @CheckReturnValue
    URL getFromURL()
    {
        return _fromURL;
    }

    /**
     * Gets the last modified time stamp.
     *
     * @return The optional last modified time stamp.
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> getStamp()
    {
        return _stamp;
    }

    private final URL _fromURL;
    private final Optional<DateTime> _stamp;
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
