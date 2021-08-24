/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PipeEngineRequest.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.pipe;

import java.io.Serializable;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.value.PointValue;

/**
 * Pipe engine request.
 */
@ThreadSafe
public final class PipeEngineRequest
    extends PipeRequest
{
    private PipeEngineRequest(
            @Nonnull final String requestID,
            final int version,
            @Nonnull final String[] transformParams,
            @Nonnull final String[] pointParams,
            @Nonnull final PointValue[] inputs,
            @Nonnull final PointValue result)
    {
        super(requestID, version);

        _transformParams = transformParams;
        _pointParams = pointParams;
        _inputs = inputs;
        _result = result;
    }

    /**
     * Returns the next request.
     *
     * @return The next request (empty when done).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<PipeEngineRequest> nextRequest()
    {
        final Optional<String> line;
        final String[] words;

        line = firstLine();

        if (!line.isPresent()) {
            return Optional.empty();
        }

        words = SPACE_PATTERN.split(line.get(), 0);

        if (words.length != 5) {
            throw error("Unexpected request format: " + line);
        }

        final String requestID = words[0];
        final int version = parseInt(words[1]);

        if (version > REQUEST_FORMAT_VERSION) {
            throw error("Unsupported request format version: " + version);
        }

        final String[] transformParams = new String[parseInt(words[2])];
        final String[] pointParams = new String[parseInt(words[3])];
        final PointValue[] inputs = new PointValue[parseInt(words[4])];
        final PointValue result = nextPointValue(true);

        for (int i = 0; i < transformParams.length; ++i) {
            transformParams[i] = nextLine();
        }

        for (int i = 0; i < pointParams.length; ++i) {
            pointParams[i] = nextLine();
        }

        for (int i = 0; i < inputs.length; ++i) {
            inputs[i] = nextPointValue(false);
        }

        return Optional
            .of(
                new PipeEngineRequest(
                    requestID,
                    version,
                    transformParams,
                    pointParams,
                    inputs,
                    result));
    }

    /**
     * Adds a result.
     *
     * @param result The result.
     */
    public void addResult(@Nonnull final PointValue result)
    {
        _results.add(result);
    }

    /**
     * Clears the results.
     */
    public synchronized void clearResults()
    {
        _result = null;
        _results.clear();
    }

    /**
     * Gets the inputs.
     *
     * @return The inputs.
     */
    @Nonnull
    @CheckReturnValue
    public PointValue[] getInputs()
    {
        return _inputs;
    }

    /**
     * Gets the Point params.
     *
     * @return The Point params.
     */
    @Nonnull
    @CheckReturnValue
    public String[] getPointParams()
    {
        return _pointParams;
    }

    /**
     * Gets the result.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public PointValue getResult()
    {
        return _result;
    }

    /**
     * Gets the Transform params.
     *
     * @return The Transform params.
     */
    @Nonnull
    @CheckReturnValue
    public String[] getTransformParams()
    {
        return _transformParams;
    }

    /**
     * Responds to this request.
     */
    public synchronized void respond()
    {
        final int resultsSize = _results.size();
        final int summary;

        if (_result != null) {
            if ((_result.getValue() != null) || (resultsSize > 0)) {
                summary = 1 + resultsSize;
            } else {
                summary = 0;
            }
        } else if (resultsSize > 0) {
            summary = resultsSize;
        } else {
            summary = -1;
        }

        writeLine(getRequestID() + " " + summary);

        if (summary > 0) {
            for (int i = 0; i < resultsSize; ++i) {
                writePointValue(_results.remove());
            }

            if (_result != null) {
                writePointValue(_result);
            }
        }
    }

    /**
     * Sets the state of the result.
     *
     * <p>Note: must not be called after {@link #clearResults()}.</p>
     *
     * @param state The state of the result.
     */
    public void setResultState(@Nullable final Serializable state)
    {
        final PointValue result = _result;

        if (result == null) {
            throw error("Trying to set the state of a cleared result");
        }

        result.setState(state);
    }

    /**
     * Sets the value of the result.
     *
     * <p>Note: must not be called after {@link #clearResults()}.</p>
     *
     * @param value The value of the result.
     */
    public void setResultValue(@Nullable final Serializable value)
    {
        final PointValue result = _result;

        if (result == null) {
            throw error("Trying to set the value of a cleared result");
        }

        result.setValue(value);
    }

    /** The request format version. */
    public static final int REQUEST_FORMAT_VERSION = 1;

    private final PointValue[] _inputs;
    private final String[] _pointParams;
    private volatile PointValue _result;
    private final Queue<PointValue> _results =
        new ConcurrentLinkedQueue<PointValue>();
    private final String[] _transformParams;
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
