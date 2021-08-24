/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RequestedValues.java 3894 2019-02-15 15:28:19Z SFB $
 */

package org.rvpf.store.server.polator;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.StampsSync;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.value.PointValue;

/**
 * Inter/extra-polation requested values.
 */
class RequestedValues
{
    RequestedValues(
            @Nonnull final StoreValuesQuery polatedQuery,
            @Nonnull final ActualValues actualValues)
    {
        _actualValues = actualValues;
        _requestedValues = new StoreValues(polatedQuery);
        _reverse = polatedQuery.isReverse();

        final TimeInterval interval = polatedQuery.getInterval();
        final int requestedRows = polatedQuery.getRows();
        DateTime firstSyncStamp;
        DateTime lastSyncStamp;
        int targetCount;

        if (interval.isInstant()) {
            firstSyncStamp = interval.getBeginning(true);
            lastSyncStamp = firstSyncStamp;

            _sync = new StampsSync(new DateTime[] {firstSyncStamp, });
            targetCount = 1;
        } else {
            _sync = polatedQuery.getSync().get();
            _sync.setLimits(interval);
            targetCount = 0;

            if (_reverse) {
                lastSyncStamp = _sync.getLastStamp();
                firstSyncStamp = lastSyncStamp;

                for (DateTime stamp = lastSyncStamp; stamp != null;
                        stamp = _sync.getPreviousStamp().orElse(null)) {
                    if (targetCount >= polatedQuery.getLimit()) {
                        _requestedValues
                            .mark(
                                polatedQuery.getPointUUID(),
                                stamp,
                                targetCount);

                        break;
                    }

                    firstSyncStamp = stamp;

                    if (++targetCount >= requestedRows) {
                        break;
                    }
                }
            } else {
                firstSyncStamp = _sync.getFirstStamp();
                lastSyncStamp = firstSyncStamp;

                for (DateTime stamp = firstSyncStamp; stamp != null;
                        stamp = _sync.getNextStamp().orElse(null)) {
                    if (targetCount >= polatedQuery.getLimit()) {
                        _requestedValues
                            .mark(
                                polatedQuery.getPointUUID(),
                                stamp,
                                targetCount);

                        break;
                    }

                    lastSyncStamp = stamp;

                    if (++targetCount >= requestedRows) {
                        break;
                    }
                }
            }
        }

        final TimeInterval limits = TimeInterval
            .newBuilder()
            .setNotBefore(firstSyncStamp)
            .setNotAfter(lastSyncStamp)
            .build();

        _sync.setLimits(limits);
        _actualValues.select(limits, targetCount);
    }

    /**
     * Gets the requested values.
     *
     * @return The requested values.
     */
    StoreValues getRequestedValues()
    {
        if (_reverse) {
            _requestedValues.reverse();
        }

        return _requestedValues;
    }

    /**
     * Supplies a requested value and asks for the next stamp.
     *
     * @param requestedValue The requested value (empty on the first call).
     *
     * @return The next stamp (empty when done).
     */
    @Nonnull
    @CheckReturnValue
    Optional<DateTime> nextStamp(
            @Nonnull final Optional<PointValue> requestedValue)
    {
        DateTime nextStamp;

        if (requestedValue.isPresent()) {
            _requestedValues.add(requestedValue.get());
            nextStamp = _sync.getNextStamp().orElse(null);
        } else {
            nextStamp = _sync.getFirstStamp();
        }

        while (nextStamp != null) {
            final Optional<PointValue> actualValue = _actualValues
                .getValueAt(nextStamp);

            if (!actualValue.isPresent()) {
                break;
            }

            _requestedValues.add(actualValue.get());
            nextStamp = _sync.getNextStamp().orElse(null);
        }

        return Optional.ofNullable(nextStamp);
    }

    private final ActualValues _actualValues;
    private final StoreValues _requestedValues;
    private final boolean _reverse;
    private final Sync _sync;
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
