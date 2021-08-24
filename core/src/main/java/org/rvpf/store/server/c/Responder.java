/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Responder.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.server.c;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreCursor;

/**
 * Responder.
 */
final class Responder
    implements StoreCursor.Responder
{
    /**
     * Constructs an instance.
     *
     * @param cStore The C store instance.
     * @param backEndLimit The back-end limit.
     */
    Responder(@Nonnull final CStore cStore, final int backEndLimit)
    {
        _cStore = Require.notNull(cStore);
        _backEndLimit = backEndLimit;
    }

    /** {@inheritDoc}
     */
    @Override
    public long count()
    {
        return _count;
    }

    /** {@inheritDoc}
     */
    @Override
    public int limit()
    {
        return _backEndLimit;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<PointValue> next()
    {
        return _valuesIterator
            .hasNext()? Optional
                .of(_valuesIterator.next().restore(_point)): Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset(final Optional<StoreCursor> storeCursor)
    {
        _valuesIterator = null;
        _point = null;

        if (storeCursor.isPresent()) {
            final Optional<DateTime> after = storeCursor.get().getAfter();
            final Optional<DateTime> before = storeCursor.get().getBefore();
            final DateTime startTime;
            final DateTime endTime;

            if (storeCursor.get().isReverse()) {
                startTime = before
                    .isPresent()? before.get().before(): DateTime.END_OF_TIME;
                endTime = after
                    .isPresent()? after.get(): DateTime.BEGINNING_OF_TIME;
            } else {
                startTime = after
                    .isPresent()? after
                        .get()
                        .after(): DateTime.BEGINNING_OF_TIME;
                endTime = before
                    .isPresent()? before.get(): DateTime.END_OF_TIME;
            }

            try {
                if (storeCursor.get().isCount()) {
                    _count(
                        storeCursor.get().getPointUUID().get(),
                        startTime,
                        endTime,
                        storeCursor.get().getLimit());
                } else {
                    _read(
                        storeCursor.get().getPointUUID().get(),
                        startTime,
                        endTime,
                        storeCursor.get().getLimit());
                    _point = storeCursor.get().getPoint().orElse(null);
                }
            } catch (final InterruptedException exception) {
                Logger
                    .getInstance(getClass())
                    .debug(ServiceMessages.INTERRUPTED);
                Thread.currentThread().interrupt();
            } catch (final RuntimeException exception) {
                throw exception;
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private void _count(
            final UUID pointUUID,
            final DateTime startTime,
            final DateTime endTime,
            final int limit)
        throws ExecutionException, InterruptedException
    {
        final CStore cStore = _cStore;
        final CStore.Task<Long> task = new CStore.Task<Long>(
            new Callable<Long>()
            {
                @Override
                public Long call()
                {
                    try {
                        return cStore
                            .count(pointUUID, startTime, endTime, limit);
                    } catch (final Status.FailedException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            });

        _cStore.execute(task);
        _count = task.get().longValue();
    }

    private void _read(
            final UUID pointUUID,
            final DateTime startTime,
            final DateTime endTime,
            final int limit)
        throws ExecutionException, InterruptedException
    {
        final CStore cStore = _cStore;
        final CStore.Task<Collection<PointValue>> task =
            new CStore.Task<Collection<PointValue>>(
                new Callable<Collection<PointValue>>()
                {
                    @Override
                    public Collection<PointValue> call()
                    {
                        final Values values;

                        try {
                            values = cStore
                                .read(pointUUID, startTime, endTime, limit);
                        } catch (final Status.FailedException exception) {
                            throw new RuntimeException(exception);
                        }

                        return cStore.pointValues(values);
                    }
                });

        _cStore.execute(task);
        _valuesIterator = task.get().iterator();
    }

    private final int _backEndLimit;
    private final CStore _cStore;
    private long _count;
    private Point _point;
    private Iterator<PointValue> _valuesIterator;
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
