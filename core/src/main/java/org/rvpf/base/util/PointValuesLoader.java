/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointValuesLoader.java 4079 2019-06-12 13:40:21Z SFB $
 */

package org.rvpf.base.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;

/**
 * Point values loader.
 *
 * <p>Helper class for scripts: on {@link #call}, it sends the
 * {@link PointValue}s obtained from an {@link Input} to a store via a
 * {@link StoreSessionProxy}.</p>
 */
@ThreadSafe
public final class PointValuesLoader
    implements Callable<Integer>
{
    /**
     * Constructs an instance.
     *
     * @param storeSessionProxy A store session proxy.
     * @param input An input.
     */
    public PointValuesLoader(
            @Nonnull final StoreSessionProxy storeSessionProxy,
            @Nonnull final Input input)
    {
        _storeProxy = Require.notNull(storeSessionProxy);
        _input = Require.notNull(input);
    }

    /** {@inheritDoc}
     *
     * @return The number of point values loaded.
     */
    @Override
    public Integer call()
        throws Exception
    {
        final List<PointValue> pointValues = new ArrayList<PointValue>(
            _batchLimit);
        int count = 0;

        for (;;) {
            final Optional<PointValue> pointValue = _input.input();

            if (!pointValue.isPresent()) {
                _flush(pointValues);

                break;
            }

            pointValues.add(pointValue.get());

            if (pointValues.size() >= _batchLimit) {
                _flush(pointValues);
            }

            ++count;
        }

        return Integer.valueOf(count);
    }

    /**
     * Gets the batch limit.
     *
     * @return The batch limit.
     */
    @CheckReturnValue
    public int getBatchLimit()
    {
        return _batchLimit;
    }

    /**
     * Sets the batch limit.
     *
     * @param batchLimit The batch limit.
     */
    public void setBatchLimit(final int batchLimit)
    {
        _batchLimit = batchLimit;
    }

    private void _flush(final List<PointValue> pointValues)
        throws Exception
    {
        if (!_storeProxy.updateAndCheck(pointValues, _LOGGER)) {
            throw new RuntimeException();
        }

        pointValues.clear();
    }

    /** Default batch limit. */
    public static final int DEFAULT_BATCH_LIMIT = 1000;

    /**  */

    private static final Logger _LOGGER = Logger
        .getInstance(PointValuesLoader.class);

    private volatile int _batchLimit = DEFAULT_BATCH_LIMIT;
    private final Input _input;
    private final StoreSessionProxy _storeProxy;

    /**
     * Input.
     */
    public interface Input
    {
        /**
         * Inputs a point value.
         *
         * @return A point value (empty when done).
         *
         * @throws Exception When appropriate.
         */
        Optional<PointValue> input()
            throws Exception;
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
