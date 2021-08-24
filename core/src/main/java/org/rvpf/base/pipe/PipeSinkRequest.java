/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PipeSinkRequest.java 3981 2019-05-13 15:00:56Z SFB $
 */

package org.rvpf.base.pipe;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;

/**
 * Pipe sink request.
 */
@Immutable
public final class PipeSinkRequest
    extends PipeRequest
{
    private PipeSinkRequest(
            final String requestID,
            final int version,
            final PointValue pointValue)
    {
        super(requestID, version);

        _pointValue = pointValue;
    }

    /**
     * Returns the next request.
     *
     * @return The next request (empty when done).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<PipeSinkRequest> nextRequest()
    {
        final Optional<String> line;
        final String[] words;

        line = firstLine();

        if (!line.isPresent()) {
            return Optional.empty();
        }

        words = SPACE_PATTERN.split(line.get(), 0);

        if (words.length != 3) {
            throw error("Unexpected request format: " + line);
        }

        final String requestID = words[0];
        final int version = parseInt(words[1]);
        final String requestType = words[2];
        final PointValue pointValue;

        if (UPDATE_REQUEST_TYPE.equals(requestType)) {
            pointValue = nextPointValue(true);
        } else if (DELETE_REQUEST_TYPE.equals(requestType)) {
            pointValue = new VersionedValue.Deleted(nextPointValue(false));
        } else {
            throw error("Unsupported request type: " + requestType);
        }

        return Optional.of(new PipeSinkRequest(requestID, version, pointValue));
    }

    /**
     * Gets the point value.
     *
     * @return The point value.
     */
    @Nonnull
    @CheckReturnValue
    public PointValue getPointValue()
    {
        return _pointValue.copy();
    }

    /**
     * Gets the request's type.
     *
     * @return The request's type.
     */
    @Nonnull
    @CheckReturnValue
    public RequestType getRequestType()
    {
        return _pointValue.isDeleted()? RequestType.DELETE: RequestType.UPDATE;
    }

    /**
     * Responds to this request.
     *
     * @param summary The response summary.
     */
    public void respond(final int summary)
    {
        writeLine(getRequestID() + " " + summary);
    }

    /** Delete request type. */
    public static final String DELETE_REQUEST_TYPE = "-";

    /** The request format version. */
    public static final int REQUEST_FORMAT_VERSION = 1;

    /** Update request type. */
    public static final String UPDATE_REQUEST_TYPE = "+";

    private final PointValue _pointValue;

    public enum RequestType
    {
        UPDATE("Update"),
        DELETE("Delete");

        /**
         * Constructs an instance.
         *
         * @param name The name of the request type.
         */
        RequestType(final String name)
        {
            _name = name;
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            return _name;
        }

        private final String _name;
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
