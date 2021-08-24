/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: XMLPointValuesPortClient.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base.xml.streamer;

import java.io.IOException;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.DateTime;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;

/**
 * XML point values port client.
 */
@NotThreadSafe
public final class XMLPointValuesPortClient
    extends StreamedMessagesPortClient
{
    /**
     * Constructs an instance.
     *
     * @param client The client identification.
     */
    public XMLPointValuesPortClient(@Nonnull final String client)
    {
        super(client);
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
    {
        super.close();

        tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    public void open(
            final String addressString,
            final Optional<SecurityContext> securityContext)
        throws ServiceNotAvailableException
    {
        if (!setUp(Optional.empty(), Optional.empty())) {
            throw new ServiceNotAvailableException();
        }

        super.open(addressString, securityContext);
    }

    /**
     * Sends a point value.
     *
     * @param pointValue The point value.
     *
     * @throws IOException When appropriate.
     */
    public void sendValue(@Nonnull PointValue pointValue)
        throws IOException
    {
        if ((pointValue.getState() == DELETED_STATE)
                && !(pointValue instanceof VersionedValue.Deleted)) {
            pointValue = new VersionedValue.Deleted(pointValue);
        }

        addMessage(pointValue);
    }

    /**
     * Sends a point value.
     *
     * @param point The point name.
     * @param stamp The time stamp.
     * @param state The optional value state.
     * @param value The optional value.
     *
     * @throws IOException When appropriate.
     */
    public void sendValue(
            @Nonnull final String point,
            @Nonnull final String stamp,
            @Nonnull final Optional<String> state,
            @Nonnull final Optional<String> value)
        throws IOException
    {
        final PointValue pointValue = new PointValue(
            point,
            Optional.of(DateTime.now().valueOf(stamp)),
            state.orElse(null),
            value.orElse(null));

        sendValue(pointValue);
    }

    /** Deleted state. */
    public static final String DELETED_STATE = "deleted";
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
