/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteExecutorTransform.java 4038 2019-05-31 16:39:02Z SFB $
 */

package org.rvpf.processor.engine.executor.remote;

import java.io.Serializable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotReadyException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.AbstractTransform;

/**
 * Remote transform.
 */
public class RemoteExecutorTransform
    extends AbstractTransform
{
    /**
     * Constructs an instance.
     *
     * @param executor The engine executor.
     */
    RemoteExecutorTransform(@Nonnull final RemoteEngineProxy executor)
    {
        _executor = Require.notNull(executor);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<PointValue> applyTo(
            final ResultValue resultValue,
            final Batch batch)
        throws InterruptedException, ServiceNotReadyException
    {
        final Point resultPoint = resultValue.getPoint().get();
        final List<PointValue> response;

        try {
            if (_context == null) {
                _context = _executor
                    .newContext(
                        getParams(),
                        Logger
                            .getInstance(
                                    getClass().getName() + ':' + getName()));
            }

            response = _executor
                .execute(
                    resultValue,
                    resultPoint.getParams().getStrings(Point.PARAM_PARAM),
                    _context);
        } catch (final SessionException exception) {
            throw new ServiceNotReadyException(exception);
        }

        PointValue pointValue;

        if (response == null) {
            pointValue = null;
        } else {
            final Iterator<PointValue> iterator = response.iterator();

            for (;;) {
                pointValue = iterator.next().restore(getMetadata());

                if (!pointValue.hasPointUUID()) {
                    getThisLogger()
                        .warn(
                            ProcessorMessages.POINT_UNKNOWN,
                            pointValue.getPointName().orElse(null));
                    pointValue = null;
                }

                if (iterator.hasNext()) {
                    if (pointValue != null) {
                        addUpdate(
                            new NormalizedValue(pointValue).encoded(),
                            batch);
                    }
                } else {
                    break;
                }
            }
        }

        return Optional.ofNullable(pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean usesFetchedResult()
    {
        return true;
    }

    private Serializable _context;
    private final RemoteEngineProxy _executor;
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
