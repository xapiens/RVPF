/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentStream.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.document.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.util.ResourceFileFactory;
import org.rvpf.service.ServiceMessages;

/**
 * Document stream.
 */
public final class DocumentStream
    extends FilterInputStream
{
    private DocumentStream(
            final URLConnection connection,
            final InputStream stream)
        throws MalformedURLException
    {
        super(stream);

        _connection = connection;
        _stream = stream;

        final String protocol = getFromURL().getProtocol();
        final long time;

        if (ResourceFileFactory.FILE_PROTOCOL.equalsIgnoreCase(protocol)) {
            time = new File(getFromURL().getPath()).lastModified();
            _isFile = true;
        } else if ("jar".equalsIgnoreCase(protocol)) {
            final URL jarURL = new URL(getFromURL().getFile());

            if (ResourceFileFactory.FILE_PROTOCOL
                .equalsIgnoreCase(jarURL.getProtocol())) {
                String jarPath = jarURL.getFile();
                final int bangPos = jarPath.indexOf('!');

                if (bangPos >= 0) {
                    jarPath = jarPath.substring(0, bangPos);
                }

                time = new File(jarPath).lastModified();
                _isFile = true;
            } else {
                time = 0;
                _isFile = false;
            }
        } else {
            time = _connection.getDate();
            _isFile = false;
        }

        if (time == 0) {
            Logger
                .getInstance(getClass())
                .warn(ServiceMessages.TIME_NOT_AVAILABLE, getFromURL());
        }

        _stamp = (time > 0)? DateTime.fromMillis(time): null;
    }

    /**
     * Creates a document stream.
     *
     * @param fromURL The document URL.
     *
     * @return A new document stream.
     *
     * @throws FileNotFoundException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    public static DocumentStream create(
            @Nonnull final URL fromURL)
        throws FileNotFoundException
    {
        final URLConnection connection;

        try {
            connection = fromURL.openConnection();
            connection.connect();

            return new DocumentStream(connection, connection.getInputStream());
        } catch (final FileNotFoundException exception) {
            throw exception;
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Gets the connection encoding.
     *
     * @return The connection encoding.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getEncoding()
    {
        return Optional.ofNullable(_connection.getContentEncoding());
    }

    /**
     * Gets the connection URL.
     *
     * @return The connection URL.
     */
    @Nonnull
    @CheckReturnValue
    URL getFromURL()
    {
        return _connection.getURL();
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
        return Optional.ofNullable(_stamp);
    }

    /**
     * Gets the input stream.
     *
     * @return The input stream.
     */
    @Nonnull
    @CheckReturnValue
    InputStream getStream()
    {
        return _stream;
    }

    /**
     * Asks if the stream represents a file.
     *
     * @return True if it does.
     */
    @CheckReturnValue
    boolean isFile()
    {
        return _isFile;
    }

    private final URLConnection _connection;
    private final boolean _isFile;
    private final DateTime _stamp;
    private final InputStream _stream;
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
