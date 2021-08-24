/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointValuesDumper.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.util;

import java.util.Optional;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Points;
import org.rvpf.base.logger.Message;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;

/**
 * Point values dumper.
 *
 * <p>Helper class for scripts: on {@link #call}, it dumps the response
 * {@link StoreValues} to a {@link StoreValuesQuery} on an {@link Output}.</p>
 */
@ThreadSafe
public final class PointValuesDumper
    implements Callable<Integer>
{
    /**
     * Constructs an instance.
     *
     * @param storeQuery The store query.
     * @param storeSessionProxy A store session proxy.
     * @param points A point definitions holder (optional).
     * @param output A Dumper.Output.
     */
    public PointValuesDumper(
            @Nonnull final StoreValuesQuery storeQuery,
            @Nonnull final StoreSessionProxy storeSessionProxy,
            @Nonnull final Optional<Points> points,
            @Nonnull final Output output)
    {
        _storeQuery = Require.notNull(storeQuery);
        _storeSessionProxy = Require.notNull(storeSessionProxy);
        _points = Require.notNull(points);
        _output = Require.notNull(output);
    }

    /** {@inheritDoc}
     *
     * @return The number of point values dumped.
     */
    @Override
    public Integer call()
        throws Exception
    {
        if (_storeQuery.isPull() && !_storeSessionProxy.supportsPull()) {
            throw new IllegalArgumentException(
                Message.format(BaseMessages.PULL_QUERIES_NOT_SUPPORTED));
        }

        int count = 0;

        try {
            for (final PointValue pointValue:
                    _storeSessionProxy.iterate(_storeQuery, _points)) {
                _output.output((VersionedValue) pointValue);
                ++count;
            }
        } catch (final StoreValuesQuery.IterationException exception) {
            throw exception.getCause();
        }

        _output.flush();

        return Integer.valueOf(count);
    }

    private final Output _output;
    private final Optional<Points> _points;
    private final StoreValuesQuery _storeQuery;
    private final StoreSessionProxy _storeSessionProxy;

    /**
     * Output.
     */
    public interface Output
    {
        /**
         * Flushes the output.
         *
         * @throws Exception When appropriate.
         */
        void flush()
            throws Exception;

        /**
         * Outputs a versioned value.
         *
         * @param versionedValue The versioned value.
         *
         * @throws Exception When appropriate.
         */
        void output(
                @Nonnull final VersionedValue versionedValue)
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
