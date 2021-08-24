/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ActualValues.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.store.server.polator;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.store.server.StoreCursor;

/**
 * Inter/extra-polation actual values.
 */
class ActualValues
{
    /**
     * Constructs an instance.
     *
     * @param polator The polator.
     * @param point The point.
     * @param storeCursor The store cursor.
     * @param identity The requesting identity.
     * @param timeLimit An optional time limit for polation.
     */
    ActualValues(
            @Nonnull final Polator.Abstract polator,
            @Nonnull final Point point,
            @Nonnull final StoreCursor storeCursor,
            @Nonnull final Identity identity,
            @Nonnull final Optional<ElapsedTime> timeLimit)
    {
        _point = point;
        _storeCursor = storeCursor;
        _identity = identity;

        if (timeLimit.isPresent()) {
            _timeLimit = timeLimit.get();
        } else {
            _timeLimit = _point
                .getParams()
                .getElapsed(
                    Point.POLATOR_TIME_LIMIT_PARAM,
                    Optional.empty(),
                    Optional.empty())
                .orElse(null);
        }

        _valueNeedsBefore = Math
            .max(
                polator.interpolationNeedsBefore(),
                polator.extrapolationNeedsBefore());
        _valueNeedsAfter = polator.interpolationNeedsAfter();
    }

    /**
     * Gets the point value at the specified stamp.
     *
     * @param stamp The stamp.
     *
     * @return The optional point value.
     */
    @Nonnull
    @CheckReturnValue
    Optional<PointValue> getValueAt(@Nonnull final DateTime stamp)
    {
        final PointValue pointValue;

        if (_selectedInterval != null) {
            Require.success(_selectedInterval.contains(stamp));

            while (!_actualInterval.contains(stamp)) {
                _fetchNextValues();
            }

            pointValue = _actualValues.get(stamp);
        } else {
            if ((_actualInterval != null) && _actualInterval.contains(stamp)) {
                pointValue = _actualValues.get(stamp);
            } else {
                final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                    .newBuilder()
                    .setPoint(_point);

                queryBuilder.setNotBefore(stamp);
                queryBuilder.setRows(1 + _valueNeedsAfter);

                if (_valueNeedsAfter > 0) {
                    if (_timeLimit != null) {
                        queryBuilder
                            .setNotAfter(
                                stamp.after(_timeLimit.mul(_valueNeedsAfter)));
                    }
                } else {
                    queryBuilder.setNotAfter(stamp);
                }

                final StoreValuesQuery query = queryBuilder.build();

                _actualInterval = query.getInterval();
                _actualValues.clear();
                _fetchValues(query);
                pointValue = _actualValues.get(stamp);
            }
        }

        return Optional.ofNullable(pointValue);
    }

    /**
     * Gets point values after a specified time.
     *
     * @param stamp The time stamp.
     * @param count The number of values requested.
     *
     * @return All the point values requested or empty.
     */
    @Nonnull
    @CheckReturnValue
    Optional<PointValue[]> getValuesAfter(
            @Nonnull DateTime stamp,
            final int count)
    {
        if (count == 0) {
            return _EMPTY_POINT_VALUES;
        }

        final PointValue[] pointValues = new PointValue[count];
        int valueIndex = 0;

        for (;;) {
            final Map.Entry<DateTime, PointValue> entry = _actualValues
                .higherEntry(stamp);

            if (entry != null) {
                final DateTime nextStamp = entry.getKey();

                if ((_timeLimit != null)
                        && (nextStamp.sub(stamp).compareTo(_timeLimit) > 0)) {
                    return Optional.empty();
                }

                pointValues[valueIndex++] = entry.getValue();

                if (valueIndex >= count) {
                    break;
                }

                stamp = nextStamp;
            } else if (_mark != null) {
                _fetchNextValues();
            } else if (_selectedInterval == null) {
                final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                    .newBuilder()
                    .setPoint(_point);

                queryBuilder.setAfter(stamp);

                if (_timeLimit != null) {
                    queryBuilder
                        .setNotAfter(
                            stamp.after(_timeLimit.mul(_valueNeedsAfter)));
                }

                queryBuilder.setRows(count - valueIndex);

                final StoreValuesQuery query = queryBuilder.build();

                if (_fetchValues(query) < query.getRows()) {
                    return Optional.empty();
                }
            } else if (!_extendedAfter) {
                if ((_timeLimit != null)
                        && (_selectedInterval.getBefore().get().sub(
                            stamp).compareTo(_timeLimit) > 0)) {
                    return Optional.empty();
                }

                final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                    .newBuilder()
                    .setPoint(_point);
                final DateTime lastSelectedStamp = _selectedInterval
                    .getNotAfter()
                    .get();

                queryBuilder.setAfter(lastSelectedStamp);

                if (_timeLimit != null) {
                    queryBuilder
                        .setNotAfter(
                            lastSelectedStamp
                                .after(_timeLimit.mul(_valueNeedsAfter)));
                }

                queryBuilder.setRows(_valueNeedsAfter);
                _fetchValues(queryBuilder.build());
                _extendedAfter = true;
            } else {
                return Optional.empty();
            }
        }

        return Optional.of(pointValues);
    }

    /**
     * Gets point values before a specified time.
     *
     * @param stamp The time stamp.
     * @param count The number of values requested.
     *
     * @return All the point values requested or empty.
     */
    @Nonnull
    @CheckReturnValue
    Optional<PointValue[]> getValuesBefore(
            @Nonnull DateTime stamp,
            final int count)
    {
        if (count == 0) {
            return _EMPTY_POINT_VALUES;
        }

        final PointValue[] pointValues = new PointValue[count];
        int valueIndex = count;

        for (;;) {
            final Map.Entry<DateTime, PointValue> entry = _actualValues
                .lowerEntry(stamp);

            if (entry != null) {
                final DateTime nextStamp = entry.getKey();

                if ((_timeLimit != null)
                        && (stamp.sub(nextStamp).compareTo(_timeLimit) > 0)) {
                    return Optional.empty();
                }

                pointValues[--valueIndex] = entry.getValue();

                if (valueIndex <= 0) {
                    break;
                }

                stamp = nextStamp;
            } else if (_selectedInterval == null) {
                final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                    .newBuilder()
                    .setPoint(_point);

                queryBuilder.setReverse(true);
                queryBuilder.setBefore(stamp);

                if (_timeLimit != null) {
                    queryBuilder
                        .setNotBefore(stamp.before(_timeLimit.mul(valueIndex)));
                }

                queryBuilder.setRows(valueIndex);

                final StoreValuesQuery query = queryBuilder.build();

                if (_fetchValues(query) < query.getRows()) {
                    return Optional.empty();
                }
            } else if (!_extendedBefore) {
                if ((_timeLimit != null)
                        && (stamp.sub(
                            _selectedInterval.getAfter().get()).compareTo(
                                    _timeLimit) > 0)) {
                    return Optional.empty();
                }

                final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                    .newBuilder()
                    .setPoint(_point);
                final DateTime firstSelectedStamp = _selectedInterval
                    .getNotBefore()
                    .get();

                queryBuilder.setBefore(firstSelectedStamp);

                if (_timeLimit != null) {
                    queryBuilder
                        .setNotBefore(
                            firstSelectedStamp
                                .before(_timeLimit.mul(_valueNeedsBefore)));
                }

                queryBuilder.setRows(_valueNeedsBefore);
                _fetchValues(queryBuilder.build());
                _extendedBefore = true;
            } else {
                return Optional.empty();
            }
        }

        return Optional.of(pointValues);
    }

    /**
     * Selects values for the time interval.
     *
     * @param timeInterval The time interval.
     * @param requestedCount The number of requested values.
     */
    void select(
            @Nonnull final TimeInterval timeInterval,
            final int requestedCount)
    {
        _actualValues.clear();
        _selectedInterval = timeInterval;

        if (_storeCursor.supportsCount()) {
            final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                .newBuilder()
                .setPoint(_point);
            final StoreValues response;

            queryBuilder.setInterval(_selectedInterval);
            queryBuilder.setCount(true);
            response = _createResponse(queryBuilder.build());

            if (response.getCount()
                    > ((long) requestedCount
                       * (_valueNeedsBefore + _valueNeedsAfter))) {
                _selectedInterval = null;
                _actualInterval = null;
            }
        }

        if (_selectedInterval != null) {
            final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                .newBuilder()
                .setPoint(_point);

            queryBuilder.setInterval(_selectedInterval);
            _fetchValues(queryBuilder.build());

            final TimeInterval.Builder timeIntervalBuilder = TimeInterval
                .newBuilder();

            timeIntervalBuilder.setAfter(_selectedInterval.getAfter().get());
            timeIntervalBuilder
                .setBefore(
                    (_mark != null)? _mark.getStamp(): _selectedInterval
                        .getBefore()
                        .get());
            _actualInterval = timeIntervalBuilder.build();
        }
    }

    private StoreValues _createResponse(final StoreValuesQuery query)
    {
        final StoreValues response = _storeCursor
            .createResponse(query, Optional.of(_identity));

        Require.success(response.isSuccess());

        return response;
    }

    private void _fetchNextValues()
    {
        while (_actualValues.size() > _valueNeedsBefore) {
            _actualValues.pollFirstEntry();
        }

        _extendedBefore |= _actualValues.size() == _valueNeedsBefore;

        final DateTime startStamp = _actualValues
            .isEmpty()? null: _actualValues.firstKey();
        final StoreValuesQuery query = _mark.createQuery();
        final TimeInterval queryInterval = query.getInterval();

        _fetchValues(query);

        final TimeInterval.Builder intervalBuilder = TimeInterval.newBuilder();

        if (startStamp != null) {
            intervalBuilder.setNotBefore(startStamp);
        } else {
            intervalBuilder.setAfter(queryInterval.getAfter().get());
        }

        intervalBuilder
            .setBefore(
                (_mark != null)? _mark.getStamp(): queryInterval
                    .getBefore()
                    .get());
        _actualInterval = intervalBuilder.build();
    }

    private int _fetchValues(final StoreValuesQuery query)
    {
        final StoreValues response;

        response = _createResponse(query);

        if (query.isForward()) {
            _mark = response.getMark().orElse(null);
        }

        for (final PointValue actualValue: response) {
            _actualValues.put(actualValue.getStamp(), actualValue);
        }

        return response.size();
    }

    private static final Optional<PointValue[]> _EMPTY_POINT_VALUES = Optional
        .of(new PointValue[0]);

    private TimeInterval _actualInterval;
    private final NavigableMap<DateTime, PointValue> _actualValues =
        new TreeMap<>();
    private boolean _extendedAfter;
    private boolean _extendedBefore;
    private final Identity _identity;
    private StoreValuesQuery.Mark _mark;
    private final Point _point;
    private TimeInterval _selectedInterval;
    private final StoreCursor _storeCursor;
    private final ElapsedTime _timeLimit;
    private final int _valueNeedsAfter;
    private final int _valueNeedsBefore;
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
