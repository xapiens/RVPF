/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Updater.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.server.c;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.Callable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreMessages;

/**
 * Updater.
 */
final class Updater
{
    /**
     * Constructs an instance.
     *
     * @param cStore The C store instance.
     */
    Updater(@Nonnull final CStore cStore)
    {
        _cStore = Require.notNull(cStore);
    }

    /**
     * Commits.
     *
     * @throws Status.FailedException When the commit operation fails.
     */
    void commit()
        throws Status.FailedException
    {
        if (!_deleted.isEmpty()) {
            final CStore.Task<int[]> task = new CStore.Task<int[]>(
                new Callable<int[]>()
                {
                    @Override
                    public int[] call()
                        throws Status.FailedException
                    {
                        return delete();
                    }
                });
            final ListIterator<PointValue> valuesIterator = _deleted
                .listIterator();
            final int[] statusCodes;

            try {
                _cStore.execute(task);
                statusCodes = task.get();
            } catch (final InterruptedException exception) {
                _LOGGER.warn(ServiceMessages.INTERRUPTED);
                Thread.currentThread().interrupt();

                return;
            } catch (final RuntimeException exception) {
                throw exception;
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }

            for (final int statusCode: statusCodes) {
                final PointValue pointValue = valuesIterator.next();

                if (statusCode != Status.SUCCESS_CODE) {    // Not deleted.
                    if (statusCode != Status.IGNORED_CODE) {
                        _LOGGER
                            .warn(
                                StoreMessages.DELETE_FAILED,
                                Status.toString(statusCode),
                                pointValue);
                    }

                    valuesIterator.set(new PointValue(pointValue));
                }
            }
        }

        if (!_updated.isEmpty()) {
            final CStore.Task<int[]> task = new CStore.Task<int[]>(
                new Callable<int[]>()
                {
                    @Override
                    public int[] call()
                        throws Status.FailedException
                    {
                        return write();
                    }
                });
            final Iterator<PointValue> valuesIterator = _updated.iterator();
            final int[] statusCodes;

            try {
                _cStore.execute(task);
                statusCodes = task.get();
            } catch (final InterruptedException exception) {
                _LOGGER.warn(ServiceMessages.INTERRUPTED);
                Thread.currentThread().interrupt();

                return;
            } catch (final RuntimeException exception) {
                throw exception;
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }

            for (final int statusCode: statusCodes) {
                final PointValue pointValue = valuesIterator.next();

                if (statusCode != Status.SUCCESS_CODE) {
                    _LOGGER
                        .warn(
                            StoreMessages.UPDATE_FAILED,
                            Status.toString(statusCode),
                            pointValue);
                }
            }
        }
    }

    /**
     * Deletes.
     *
     * @return The individual status codes.
     *
     * @throws Status.FailedException When the delete operation fails.
     */
    @Nonnull
    @CheckReturnValue
    int[] delete()
        throws Status.FailedException
    {
        return _cStore.delete(_deleted);
    }

    /**
     * Gets the deleted values.
     *
     * @return The deleted values.
     */
    Collection<PointValue> getDeleted()
    {
        return _deleted;
    }

    /**
     * Gets the updated values.
     *
     * @return The updated values.
     */
    Collection<PointValue> getUpdated()
    {
        return _updated;
    }

    /**
     * Updates a value.
     *
     * @param pointValue The new Point value.
     */
    void update(final PointValue pointValue)
    {
        if (pointValue.isDeleted()) {
            _deleted.add(pointValue);
        } else {
            _updated.add(pointValue);
        }
    }

    /**
     * Writes.
     *
     * @return The individual status codes.
     *
     * @throws Status.FailedException When the write operation fails.
     */
    int[] write()
        throws Status.FailedException
    {
        return _cStore.write(_updated);
    }

    private static final Logger _LOGGER = Logger.getInstance(Updater.class);

    private final CStore _cStore;
    private final LinkedList<PointValue> _deleted = new LinkedList<>();
    private final Collection<PointValue> _updated = new LinkedList<>();
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
