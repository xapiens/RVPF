/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreResultSet.java 4041 2019-06-01 17:49:03Z SFB $
 */

package org.rvpf.jdbc;

import java.io.Serializable;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;

import java.time.ZonedDateTime;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;

/**
 * Store result set.
 */
final class StoreResultSet
    extends DefaultResultSet
{
    /**
     * Constructs an instance.
     *
     * @param connection The store connection.
     * @param statement The store statement.
     * @param queryRequest The query request.
     * @param metaData The metadata.
     *
     * @throws SQLException When appropriate.
     */
    StoreResultSet(
            @Nonnull final StoreConnection connection,
            @Nonnull final StoreStatement statement,
            @Nonnull final Request.Query queryRequest,
            @Nonnull final Optional<StoreResultSetMetaData> metaData)
        throws SQLException
    {
        super(connection, metaData);

        _statement = statement;
        _request = queryRequest;

        _forwardOnly = _statement.getResultSetType() == TYPE_FORWARD_ONLY;
        _readOnly = _statement.getResultSetConcurrency() == CONCUR_READ_ONLY;

        getMetaData().adoptQueryRequest(_request);
    }

    /** {@inheritDoc}
     */
    @Override
    public void cancelRowUpdates()
        throws SQLException
    {
        assertOpen();

        if (_inserting) {
            throw JDBCMessages.ON_INSERT_ROW.exception();
        }

        _updatePointValue = null;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
        throws SQLException
    {
        if (!isClosed()) {
            _flushUpdates();
            _updatePointValue = null;
            _inserting = false;
            _storeResponse = null;
            _statement.resultSetClosed(this);

            super.close();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void deleteRow()
        throws SQLException
    {
        assertOpen();

        if (_inserting) {
            throw JDBCMessages.ON_INSERT_ROW.exception();
        }

        _fetchPointValue();
        _updatePointValue = new VersionedValue.Deleted(_updatePointValue);
        update();
    }

    /** {@inheritDoc}
     */
    @Override
    public byte[] getBytes(final int columnNumber)
        throws SQLException
    {
        final Object object = getObject(columnNumber);

        if (object == null) {
            return null;
        }

        if (object instanceof byte[]) {
            return (byte[]) object;
        }

        if (object instanceof UUID) {
            return ((UUID) object).toBytes();
        }

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getConcurrency()
        throws SQLException
    {
        assertOpen();

        return _readOnly? CONCUR_READ_ONLY: CONCUR_UPDATABLE;
    }

    /** {@inheritDoc}
     */
    @Override
    public Date getDate(final int columnNumber)
        throws SQLException
    {
        final Object object = getObject(columnNumber);

        if (object == null) {
            return null;
        }

        if (object instanceof DateTime) {
            return new Date(((DateTime) object).midnight().toMillis());
        }

        if (object instanceof String) {
            try {
                return Date.valueOf((String) object);
            } catch (final IllegalArgumentException exception) {
                throw JDBCMessages.WRONG_DATA_TYPE.wrap(exception);
            }
        }

        if (object instanceof Date) {
            return (Date) object;
        }

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public Date getDate(
            final int columnNumber,
            final Calendar calendar)
        throws SQLException
    {
        final Object object = getObject(columnNumber);
        final Date date;

        if (object == null) {
            return null;
        }

        if (object instanceof DateTime) {
            date = new Date(((DateTime) object).toMillis());
        } else if (object instanceof String) {
            try {
                date = Date.valueOf((String) object);
            } catch (final IllegalArgumentException exception) {
                throw JDBCMessages.WRONG_DATA_TYPE.wrap(exception);
            }
        } else {
            throw JDBCMessages.WRONG_DATA_TYPE.exception();
        }

        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return new Date(calendar.getTime().getTime());
    }

    /** {@inheritDoc}
     */
    @Override
    public long getLong(final int columnNumber)
        throws SQLException
    {
        final Object object = getObject(columnNumber);

        if (object == null) {
            return 0;
        }

        if (object instanceof Number) {
            return ((Number) object).longValue();
        }

        if (object instanceof String) {
            try {
                return Long.parseLong((String) object);
            } catch (final NumberFormatException exception) {
                throw JDBCMessages.WRONG_DATA_TYPE.wrap(exception);
            }
        }

        if (object instanceof DateTime) {
            return ((DateTime) object).toRaw();
        }

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public Statement getStatement()
        throws SQLException
    {
        assertOpen();

        return _statement;
    }

    /** {@inheritDoc}
     */
    @Override
    public Time getTime(final int columnNumber)
        throws SQLException
    {
        final Object object = getObject(columnNumber);

        if (object == null) {
            return null;
        }

        if (object instanceof DateTime) {
            final ZonedDateTime zonedDateTime = ((DateTime) object)
                .toZonedDateTime()
                .withNano(0);

            return new Time(
                zonedDateTime.toLocalTime().toNanoOfDay() / 1_000_000);
        }

        if (object instanceof String) {
            try {
                return Time.valueOf((String) object);
            } catch (final IllegalArgumentException exception) {
                throw JDBCMessages.WRONG_DATA_TYPE.wrap(exception);
            }
        }

        if (object instanceof Time) {
            return (Time) object;
        }

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public Time getTime(
            final int columnNumber,
            final Calendar calendar)
        throws SQLException
    {
        final Object object = getObject(columnNumber);
        final Time time;

        if (object == null) {
            return null;
        }

        if (object instanceof DateTime) {
            time = new Time(((DateTime) object).toMillis());
        } else if (object instanceof String) {
            try {
                time = Time.valueOf((String) object);
            } catch (final IllegalArgumentException exception) {
                throw JDBCMessages.WRONG_DATA_TYPE.wrap(exception);
            }
        } else {
            throw JDBCMessages.WRONG_DATA_TYPE.exception();
        }

        calendar.setTime(time);
        calendar.set(Calendar.YEAR, 1970);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.MILLISECOND, 0);

        return new Time(calendar.getTime().getTime());
    }

    /** {@inheritDoc}
     */
    @Override
    public Timestamp getTimestamp(final int columnNumber)
        throws SQLException
    {
        final Object object = getObject(columnNumber);

        if (object == null) {
            return null;
        }

        if (object instanceof DateTime) {
            return ((DateTime) object).toTimestamp();
        }

        if (object instanceof String) {
            try {
                return Timestamp.valueOf((String) object);
            } catch (final IllegalArgumentException exception) {
                throw JDBCMessages.WRONG_DATA_TYPE.wrap(exception);
            }
        }

        if (object instanceof Timestamp) {
            return (Timestamp) object;
        }

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public Timestamp getTimestamp(
            final int columnNumber,
            final Calendar calendar)
        throws SQLException
    {
        final Object object = getObject(columnNumber);
        final Timestamp timestamp;

        if (object == null) {
            return null;
        }

        if (object instanceof DateTime) {
            timestamp = ((DateTime) object).toTimestamp();
        } else if (object instanceof String) {
            try {
                timestamp = Timestamp.valueOf((String) object);
            } catch (final IllegalArgumentException exception) {
                throw JDBCMessages.WRONG_DATA_TYPE.wrap(exception);
            }
        } else {
            throw JDBCMessages.WRONG_DATA_TYPE.exception();
        }

        timestamp.setTime(calendar.getTimeInMillis());

        return timestamp;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getType()
        throws SQLException
    {
        assertOpen();

        return _forwardOnly? TYPE_FORWARD_ONLY: TYPE_SCROLL_INSENSITIVE;
    }

    /** {@inheritDoc}
     */
    @Override
    public void insertRow()
        throws SQLException
    {
        assertOpen();

        if (!_inserting) {
            throw JDBCMessages.NOT_ON_INSERT_ROW.exception();
        }

        if (_updatePointValue.getPointUUID() == null) {
            throw JDBCMessages.POINT_NOT_SPECIFIED.exception();
        }

        if (!_updatePointValue.hasStamp()) {
            throw JDBCMessages.STAMP_NOT_SPECIFIED.exception();
        }

        update();
        _updatePointValue = new PointValue(
            _pointUUID,
            Optional.empty(),
            null,
            null);
    }

    /** {@inheritDoc}
     */
    @Override
    public void moveToCurrentRow()
        throws SQLException
    {
        assertOpen();

        _updatePointValue = null;
        _inserting = false;
    }

    /** {@inheritDoc}
     */
    @Override
    public void moveToInsertRow()
        throws SQLException
    {
        assertOpen();

        if (_readOnly) {
            throw JDBCMessages.RESULT_SET_READ_ONLY.exception();
        }

        _updatePointValue = new PointValue(
            _pointUUID,
            Optional.empty(),
            null,
            null);
        _inserting = true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateObject(
            final int columnNumber,
            final Object value)
        throws SQLException
    {
        try {
            final StoreColumn column = _getUpdateColumn(columnNumber);

            switch (column) {
                case POINT: {
                    final UUID uuid;

                    if (value instanceof String) {
                        if (UUID.isUUID((String) value)) {
                            uuid = UUID.fromString((String) value).get();
                        } else {
                            uuid = getConnection()
                                .getPointUUID((String) value)
                                .orElse(null);

                            if (uuid == null) {
                                throw JDBCMessages.UNKNOWN_POINT
                                    .exception(value);
                            }
                        }
                    } else if (value instanceof UUID) {
                        uuid = (UUID) value;
                    } else if (value instanceof byte[]) {
                        uuid = UUID.fromBytes((byte[]) value);
                    } else {
                        throw JDBCMessages.WRONG_DATA_TYPE.exception();
                    }

                    _updatePointValue = _updatePointValue
                        .morph(Optional.of(uuid));

                    break;
                }
                case POINT_NAME: {
                    final UUID uuid;

                    if (value instanceof String) {
                        try {
                            uuid = getConnection()
                                .getPointUUID((String) value)
                                .get();
                        } catch (final IllegalArgumentException exception) {
                            throw JDBCMessages.UNKNOWN_POINT.exception(value);
                        }
                    } else {
                        throw JDBCMessages.WRONG_DATA_TYPE.exception();
                    }

                    _updatePointValue = _updatePointValue
                        .morph(Optional.of(uuid));

                    break;
                }
                case POINT_UUID: {
                    final UUID uuid;

                    if (value instanceof UUID) {
                        uuid = (UUID) value;
                    } else if (value instanceof String) {
                        uuid = UUID.fromString((String) value).orElse(null);
                    } else if (value instanceof byte[]) {
                        uuid = UUID.fromBytes((byte[]) value);
                    } else {
                        throw JDBCMessages.WRONG_DATA_TYPE.exception();
                    }

                    _updatePointValue = _updatePointValue
                        .morph(Optional.ofNullable(uuid));

                    break;
                }
                case STAMP: {
                    final DateTime stamp;

                    if (value instanceof DateTime) {
                        stamp = (DateTime) value;
                    } else if (value instanceof String) {
                        stamp = DateTime
                            .fromString(Optional.of((String) value))
                            .orElse(null);
                    } else if (value instanceof Long) {
                        stamp = DateTime.fromRaw(((Long) value).longValue());
                    } else if (value instanceof Timestamp) {
                        stamp = DateTime.at((Timestamp) value);
                    } else if (value instanceof Date) {
                        stamp = DateTime.fromMillis(((Date) value).getTime());
                    } else {
                        throw JDBCMessages.WRONG_DATA_TYPE.exception();
                    }

                    _updatePointValue.clearStamp();
                    _updatePointValue.setStamp(stamp);

                    break;
                }
                case STATE: {
                    if (value instanceof Serializable) {
                        _updatePointValue.setState((Serializable) value);
                    } else {
                        throw JDBCMessages.WRONG_DATA_TYPE.exception();
                    }

                    break;
                }
                case VALUE: {
                    if (value instanceof Serializable) {
                        _updatePointValue.setValue((Serializable) value);
                    } else {
                        throw JDBCMessages.WRONG_DATA_TYPE.exception();
                    }

                    break;
                }
                default: {
                    throw new InternalError("Unexpected column: " + column);
                }
            }
        } catch (final IllegalArgumentException exception) {
            throw JDBCMessages.WRONG_DATA_TYPE.wrap(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateRow()
        throws SQLException
    {
        assertOpen();

        if (_inserting) {
            throw JDBCMessages.ON_INSERT_ROW.exception();
        }

        _fetchPointValue();
        update();
    }

    /** {@inheritDoc}
     */
    @Override
    void doFetchRows()
        throws SQLException
    {
        if (_storeQuery != null) {
            final List<StoreColumn> columns = _request.getColumns();

            if ((columns.size() == 1)
                    && (columns.get(0) == StoreColumn.COUNT)) {
                _fetchValueCount(_storeQuery, this);
                _storeQuery = null;
            } else {
                _storeQuery = _fetchValueRows(_storeQuery, columns, this);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    boolean isForwardOnly()
    {
        return _forwardOnly;
    }

    /**
     * Sets the store values query.
     *
     * @param storeQuery The optional store values query.
     */
    void setStoreQuery(@Nonnull final Optional<StoreValuesQuery> storeQuery)
    {
        _storeQuery = storeQuery.orElse(null);
        _pointUUID = storeQuery
            .isPresent()? storeQuery.get().getPointUUID().orElse(null): null;
    }

    /**
     * Sets the store response for updates.
     *
     * @param storeResponse The store response.
     *
     * @throws SQLException When appropriate.
     */
    void setStoreResponse(
            @Nonnull final StoreValues storeResponse)
        throws SQLException
    {
        _storeResponse = (!_readOnly)? storeResponse: null;
        _storeResponseRow = getRow() + 1;
    }

    private void _fetchPointValue()
        throws SQLException
    {
        if (_updatePointValue == null) {
            if (_storeResponse == null) {
                throw JDBCMessages.RESULT_SET_READ_ONLY.exception();
            }

            if (getCurrentRow() < 0) {
                throw JDBCMessages.NO_CURRENT_ROW.exception();
            }

            _updatePointValue = new PointValue(
                _storeResponse.getPointValue(
                    getCurrentRow() - _storeResponseRow));
        }
    }

    private void _fetchValueCount(
            StoreValuesQuery storeQuery,
            final StoreResultSet resultSet)
        throws SQLException
    {
        final StoreConnection connection = _statement.getConnection();
        int count = 0;

        for (;;) {
            final StoreValues storeResponse;

            try {
                storeResponse = connection
                    .getStoreSession()
                    .select(storeQuery)
                    .orElse(null);

                if (storeResponse == null) {
                    throw new ServiceClosedException();
                }
            } catch (final SessionException exception) {
                throw JDBCMessages.SESSION_EXCEPTION.wrap(exception);
            }

            count += storeResponse.size();

            if (storeResponse.isComplete()) {
                break;
            }

            storeQuery = storeResponse.createQuery();
        }

        _LOGGER.debug(JDBCMessages.VALUES_RETURNED, Integer.valueOf(count));

        resultSet.addRow(Integer.valueOf(count));
    }

    private StoreValuesQuery _fetchValueRows(
            @Nonnull final StoreValuesQuery storeQuery,
            @Nonnull final List<StoreColumn> columns,
            @Nonnull final StoreResultSet resultSet)
        throws SQLException
    {
        final StoreConnection connection = _statement.getConnection();
        final StoreValues storeResponse;

        try {
            storeResponse = connection
                .getStoreSession()
                .select(storeQuery)
                .orElse(null);

            if (storeResponse == null) {
                throw new ServiceClosedException();
            }
        } catch (final SessionException exception) {
            throw JDBCMessages.SESSION_EXCEPTION.wrap(exception);
        }

        for (final PointValue pointValue: storeResponse) {
            final Object[] row = new Object[columns.size()];
            int columnNumber = 0;

            for (final StoreColumn column: columns) {
                final Object value;

                switch (column) {
                    case POINT: {
                        final Optional<String> name = connection
                            .getPointName(pointValue.getPointUUID());

                        value = name
                            .isPresent()? name
                                .get(): pointValue.getPointUUID().toString();

                        break;
                    }
                    case POINT_NAME: {
                        value = connection
                            .getPointName(pointValue.getPointUUID())
                            .get();

                        break;
                    }
                    case POINT_UUID: {
                        value = pointValue.getPointUUID();

                        break;
                    }
                    case STAMP: {
                        value = pointValue.getStamp();

                        break;
                    }
                    case VERSION: {
                        value = (pointValue instanceof VersionedValue)
                                ? ((VersionedValue) pointValue)
                                    .getVersion(): null;

                        break;
                    }
                    case STATE: {
                        value = pointValue.getState();

                        break;
                    }
                    case VALUE: {
                        value = pointValue.getValue();

                        break;
                    }
                    case INTERPOLATED: {
                        value = Boolean
                            .valueOf(
                                pointValue.isInterpolated()
                                || ((pointValue.getValue() == null)
                                    && pointValue.isSynthesized()));

                        break;
                    }
                    case EXTRAPOLATED: {
                        value = Boolean
                            .valueOf(
                                pointValue.isExtrapolated()
                                || ((pointValue.getValue() == null)
                                    && pointValue.isSynthesized()));

                        break;
                    }
                    default: {
                        throw new InternalError("Unexpected column: " + column);
                    }
                }

                row[columnNumber++] = value;
            }

            resultSet.addRow(row);
        }

        _LOGGER
            .debug(
                JDBCMessages.VALUE_ROWS_FETCHED,
                String.valueOf(storeResponse.size()));

        setStoreResponse(storeResponse);

        return storeResponse.isComplete()? null: storeResponse.createQuery();
    }

    private void _flushUpdates()
        throws SQLException
    {
        if (_updates != null) {
            _statement
                .update(_updates.toArray(new PointValue[_updates.size()]));
            _updates.clear();
            _updates = null;
        }
    }

    private StoreColumn _getUpdateColumn(
            final int columnNumber)
        throws SQLException
    {
        assertOpen();
        _fetchPointValue();

        final StoreColumn storeColumn = getMetaData()
            .getStoreColumn(columnNumber);

        if (storeColumn == StoreColumn.VERSION) {
            throw JDBCMessages.COLUMN_READ_ONLY.exception(storeColumn);
        }

        return storeColumn;
    }

    private void update()
        throws SQLException
    {
        Require.notNull(_updatePointValue);

        if (_updates == null) {
            _updates = new LinkedList<PointValue>();
        }

        _updates.add(_updatePointValue);
        _updatePointValue = null;

        if ((_updates.size() > getConnection().getAutoCommitLimit())
                && (_statement.getResultSetHoldability()
                    == HOLD_CURSORS_OVER_COMMIT)) {
            _flushUpdates();
        }
    }

    private static final Logger _LOGGER = Logger
        .getInstance(StoreResultSet.class);

    private final boolean _forwardOnly;
    private boolean _inserting;
    private UUID _pointUUID;
    private final boolean _readOnly;
    private Request.Query _request;
    private final StoreStatement _statement;
    private StoreValuesQuery _storeQuery;
    private StoreValues _storeResponse;
    private int _storeResponseRow;
    private PointValue _updatePointValue;
    private List<PointValue> _updates;
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
