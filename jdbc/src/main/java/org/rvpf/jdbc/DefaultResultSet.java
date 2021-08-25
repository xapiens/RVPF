/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DefaultResultSet.java 3971 2019-05-09 21:30:00Z SFB $
 */

package org.rvpf.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import java.math.BigDecimal;

import java.net.URL;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.ValueConverter;

/**
 * Default result set.
 */
class DefaultResultSet
    implements ResultSet
{
    /**
     * Constructs an instance.
     *
     * @param connection The store connection.
     *
     * @throws SQLException When appropriate.
     */
    DefaultResultSet(
            @Nonnull final StoreConnection connection)
        throws SQLException
    {
        this(connection, Optional.empty());
    }

    /**
     * Constructs an instance.
     *
     * @param connection The store connection.
     * @param metadata The store result set metadata.
     *
     * @throws SQLException When appropriate.
     */
    DefaultResultSet(
            @Nonnull final StoreConnection connection,
            @Nonnull final Optional<StoreResultSetMetaData> metadata)
        throws SQLException
    {
        _connection = connection;
        _metaData = metadata.orElse(new StoreResultSetMetaData());
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean absolute(final int row)
        throws SQLException
    {
        moveToCurrentRow();

        if (isForwardOnly()) {
            if (row == -1) {
                return last();
            }

            if (row < _currentRow) {
                throw _forwardOnlyException();
            }

            while (_currentRow < row) {
                if (!hasNext()) {
                    fetchRows();
                }

                _advance();

                if (_currentRowValues == null) {
                    break;
                }
            }

            return _currentRowValues != null;
        } else if (row > 0) {
            while (_rows.size() < row) {
                if (_rowCount >= 0) {
                    _setCurrentRow(_rows.size());

                    return false;
                }

                fetchRows();
            }

            _setCurrentRow(row - 1);

            return _advance();
        } else if (row < 0) {
            afterLast();

            if (-row > _rows.size()) {
                beforeFirst();

                return false;
            }

            _setCurrentRow(_rows.size() + row);

            return _advance();
        } else {
            fetchRows();
            beforeFirst();

            return false;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void afterLast()
        throws SQLException
    {
        moveToCurrentRow();

        if (isForwardOnly()) {
            if (((_currentRow == 0) || (_currentRowValues != null)) && last()) {
                _advance();
            }
        } else {
            do {
                _setCurrentRow(_rows.size() + 1);
                fetchRows();
            } while (_rowCount < 0);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void beforeFirst()
        throws SQLException
    {
        moveToCurrentRow();

        if (isForwardOnly()) {
            if (_currentRow > 0) {
                throw _forwardOnlyException();
            }
        } else {
            _setCurrentRow(0);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void cancelRowUpdates()
        throws SQLException
    {
        assertOpen();
    }

    /** {@inheritDoc}
     */
    @Override
    public final synchronized void clearWarnings()
        throws SQLException
    {
        assertOpen();

        _warnings = null;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
        throws SQLException
    {
        if (!_closed) {
            _closed = true;
            _warnings = null;
            _connection.resultSetClosed(this);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void deleteRow()
        throws SQLException
    {
        assertOpen();

        throw resultSetReadOnly();
    }

    /** {@inheritDoc}
     */
    @Override
    public final int findColumn(final String columnName)
        throws SQLException
    {
        assertOpen();

        return _metaData.findColumn(columnName);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean first()
        throws SQLException
    {
        moveToCurrentRow();

        if (_currentRow == 0) {
            return next();
        }

        if (_currentRow == 1) {
            return _currentRowValues != null;
        }

        if (isForwardOnly()) {
            throw _forwardOnlyException();
        }

        _setCurrentRow(0);

        return _advance();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Array getArray(final int columnNumber)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Array getArray(final String columnName)
        throws SQLException
    {
        return getArray(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final InputStream getAsciiStream(
            final int columnNumber)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final InputStream getAsciiStream(
            final String columnName)
        throws SQLException
    {
        return getAsciiStream(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final BigDecimal getBigDecimal(
            final int columnNumber)
        throws SQLException
    {
        final Object object = getObject(columnNumber);

        if (object == null) {
            return null;
        }

        if (object instanceof BigDecimal) {
            return (BigDecimal) object;
        }

        if (object instanceof Number) {
            return new BigDecimal(((Number) object).doubleValue());
        }

        if (object instanceof String) {
            return new BigDecimal((String) object);
        }

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final BigDecimal getBigDecimal(
            final String columnName)
        throws SQLException
    {
        return getBigDecimal(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Deprecated
    @Override
    public final BigDecimal getBigDecimal(
            final int columnNumber,
            final int scale)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Deprecated
    @Override
    public final BigDecimal getBigDecimal(
            final String columnName,
            final int scale)
        throws SQLException
    {
        return getBigDecimal(findColumn(columnName), scale);
    }

    /** {@inheritDoc}
     */
    @Override
    public final InputStream getBinaryStream(
            final int columnNumber)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final InputStream getBinaryStream(
            final String columnName)
        throws SQLException
    {
        return getBinaryStream(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final Blob getBlob(final int columnNumber)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Blob getBlob(final String columnName)
        throws SQLException
    {
        return getBlob(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean getBoolean(final int columnNumber)
        throws SQLException
    {
        final Object object = getObject(columnNumber);

        if (object == null) {
            return false;
        }

        if (object instanceof Boolean) {
            return ((Boolean) object).booleanValue();
        }

        if (object instanceof String) {
            return ValueConverter.isTrue((String) object);
        }

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean getBoolean(final String columnName)
        throws SQLException
    {
        return getBoolean(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final byte getByte(final int columnNumber)
        throws SQLException
    {
        return (byte) getLong(columnNumber);
    }

    /** {@inheritDoc}
     */
    @Override
    public final byte getByte(final String columnName)
        throws SQLException
    {
        return getByte(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public byte[] getBytes(final int columnNumber)
        throws SQLException
    {
        getObject(columnNumber);

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final byte[] getBytes(final String columnName)
        throws SQLException
    {
        return getBytes(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final Reader getCharacterStream(
            final int columnNumber)
        throws SQLException
    {
        final String string = getString(columnNumber);

        return (string != null)? new StringReader(string): null;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Reader getCharacterStream(
            final String columnName)
        throws SQLException
    {
        return getCharacterStream(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final Clob getClob(final int columnNumber)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Clob getClob(final String columnName)
        throws SQLException
    {
        return getClob(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public int getConcurrency()
        throws SQLException
    {
        assertOpen();

        return CONCUR_READ_ONLY;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getCursorName()
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public Date getDate(final int columnNumber)
        throws SQLException
    {
        getObject(columnNumber);

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Date getDate(final String columnName)
        throws SQLException
    {
        return getDate(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public Date getDate(
            final int columnNumber,
            final Calendar calendar)
        throws SQLException
    {
        getObject(columnNumber);

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Date getDate(
            final String columnName,
            final Calendar calendar)
        throws SQLException
    {
        return getDate(findColumn(columnName), calendar);
    }

    /** {@inheritDoc}
     */
    @Override
    public final double getDouble(final int columnNumber)
        throws SQLException
    {
        final Object object = getObject(columnNumber);

        if (object == null) {
            return 0.0;
        }

        if (object instanceof Number) {
            return ((Number) object).doubleValue();
        }

        if (object instanceof String) {
            try {
                return Double.parseDouble((String) object);
            } catch (final NumberFormatException exception) {
                throw JDBCMessages.WRONG_DATA_TYPE.wrap(exception);
            }
        }

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final double getDouble(final String columnName)
        throws SQLException
    {
        return getDouble(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final int getFetchDirection()
        throws SQLException
    {
        assertOpen();

        return FETCH_FORWARD;
    }

    /** {@inheritDoc}
     */
    @Override
    public final int getFetchSize()
        throws SQLException
    {
        assertOpen();

        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public final float getFloat(final int columnNumber)
        throws SQLException
    {
        return (float) getDouble(columnNumber);
    }

    /** {@inheritDoc}
     */
    @Override
    public final float getFloat(final String columnName)
        throws SQLException
    {
        return getFloat(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public int getHoldability()
        throws SQLException
    {
        assertOpen();

        return CLOSE_CURSORS_AT_COMMIT;
    }

    /** {@inheritDoc}
     */
    @Override
    public final int getInt(final int columnNumber)
        throws SQLException
    {
        return (int) getLong(columnNumber);
    }

    /** {@inheritDoc}
     */
    @Override
    public final int getInt(final String columnName)
        throws SQLException
    {
        return getInt(findColumn(columnName));
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

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final long getLong(final String columnName)
        throws SQLException
    {
        return getLong(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final StoreResultSetMetaData getMetaData()
        throws SQLException
    {
        assertOpen();

        return _metaData;
    }

    /** {@inheritDoc}
     */
    @Override
    public Reader getNCharacterStream(final int columnIndex)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public Reader getNCharacterStream(
            final String columnName)
        throws SQLException
    {
        return getNCharacterStream(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public NClob getNClob(final int columnIndex)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public NClob getNClob(final String columnName)
        throws SQLException
    {
        return getNClob(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public String getNString(final int columnIndex)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getNString(final String columnName)
        throws SQLException
    {
        return getNString(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final Object getObject(final int columnNumber)
        throws SQLException
    {
        assertOpen();

        return _getColumnValue(columnNumber);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Object getObject(final String columnName)
        throws SQLException
    {
        return getObject(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T getObject(
            final int columnIndex,
            final Class<T> type)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Object getObject(
            final int columnNumber,
            final Map<String, Class<?>> map)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T getObject(
            final String columnLabel,
            final Class<T> type)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Object getObject(
            final String columnName,
            final Map<String, Class<?>> map)
        throws SQLException
    {
        return getObject(findColumn(columnName), map);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Ref getRef(final int columnNumber)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Ref getRef(final String columnName)
        throws SQLException
    {
        return getRef(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final int getRow()
        throws SQLException
    {
        assertOpen();

        return _currentRow;
    }

    /** {@inheritDoc}
     */
    @Override
    public RowId getRowId(final int columnIndex)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public RowId getRowId(final String columnName)
        throws SQLException
    {
        return getRowId(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public SQLXML getSQLXML(final int columnIndex)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public SQLXML getSQLXML(final String columnName)
        throws SQLException
    {
        return getSQLXML(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final short getShort(final int columnNumber)
        throws SQLException
    {
        return (short) getLong(columnNumber);
    }

    /** {@inheritDoc}
     */
    @Override
    public final short getShort(final String columnName)
        throws SQLException
    {
        return getShort(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public Statement getStatement()
        throws SQLException
    {
        assertOpen();

        return null;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getString(final int columnNumber)
        throws SQLException
    {
        final Object value = getObject(columnNumber);

        return (value != null)? value.toString(): null;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getString(final String columnName)
        throws SQLException
    {
        return getString(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public Time getTime(final int columnNumber)
        throws SQLException
    {
        getObject(columnNumber);

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Time getTime(final String columnName)
        throws SQLException
    {
        return getTime(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public Time getTime(
            final int columnNumber,
            final Calendar calendar)
        throws SQLException
    {
        getObject(columnNumber);

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Time getTime(
            final String columnName,
            final Calendar calendar)
        throws SQLException
    {
        return getTime(findColumn(columnName), calendar);
    }

    /** {@inheritDoc}
     */
    @Override
    public Timestamp getTimestamp(final int columnNumber)
        throws SQLException
    {
        getObject(columnNumber);

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Timestamp getTimestamp(
            final String columnName)
        throws SQLException
    {
        return getTimestamp(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public Timestamp getTimestamp(
            final int columnNumber,
            final Calendar calendar)
        throws SQLException
    {
        getObject(columnNumber);

        throw JDBCMessages.WRONG_DATA_TYPE.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Timestamp getTimestamp(
            final String columnName,
            final Calendar calendar)
        throws SQLException
    {
        return getTimestamp(findColumn(columnName), calendar);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getType()
        throws SQLException
    {
        assertOpen();

        return TYPE_FORWARD_ONLY;
    }

    /** {@inheritDoc}
     */
    @Override
    public final URL getURL(final int columnNumber)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final URL getURL(final String columnName)
        throws SQLException
    {
        return getURL(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Deprecated
    @Override
    public final InputStream getUnicodeStream(
            final int columnNumber)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Deprecated
    @Override
    public final InputStream getUnicodeStream(
            final String columnName)
        throws SQLException
    {
        return getUnicodeStream(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public final synchronized SQLWarning getWarnings()
        throws SQLException
    {
        assertOpen();

        return _warnings;
    }

    /** {@inheritDoc}
     */
    @Override
    public void insertRow()
        throws SQLException
    {
        assertOpen();

        throw resultSetReadOnly();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isAfterLast()
        throws SQLException
    {
        assertOpen();

        fetchRows();

        return (_currentRowValues == null) && (_currentRow > 0);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isBeforeFirst()
        throws SQLException
    {
        assertOpen();

        fetchRows();

        return hasNext() && (_currentRow == 0);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isClosed()
    {
        return _closed;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isFirst()
        throws SQLException
    {
        assertOpen();

        return _currentRow == 1;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isLast()
        throws SQLException
    {
        assertOpen();

        if (_currentRow == _rows.size()) {
            _rowsIterator = null;
        }

        fetchRows();

        return (_currentRowValues != null) && !hasNext();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isWrapperFor(final Class<?> iface)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean last()
        throws SQLException
    {
        moveToCurrentRow();

        if (isForwardOnly()) {
            if ((_currentRowValues == null) && (_currentRow > 0)) {
                throw _forwardOnlyException();
            }

            do {
                while (hasNext()) {
                    _advance();
                }

                fetchRows();
            } while (hasNext());

            return _currentRowValues != null;
        }

        afterLast();

        return relative(-1);
    }

    /** {@inheritDoc}
     */
    @Override
    public void moveToCurrentRow()
        throws SQLException
    {
        assertOpen();
    }

    /** {@inheritDoc}
     */
    @Override
    public void moveToInsertRow()
        throws SQLException
    {
        assertOpen();

        throw resultSetReadOnly();
    }

    /** {@inheritDoc}
     */
    @Override
    public final synchronized boolean next()
        throws SQLException
    {
        return relative(1);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean previous()
        throws SQLException
    {
        return relative(-1);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void refreshRow()
        throws SQLException
    {
        moveToCurrentRow();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean relative(int rows)
        throws SQLException
    {
        moveToCurrentRow();

        if (rows < 0) {
            if (isForwardOnly()) {
                throw _forwardOnlyException();
            }

            if ((_currentRow + rows) <= 0) {
                _setCurrentRow(0);

                return false;
            }

            if (_rowsIterator == null) {
                _rowsIterator = _rows.listIterator(_currentRow + rows - 1);
            } else {
                while (rows++ <= 0) {
                    _rowsIterator.previous();
                }
            }

            _advance();
        } else {
            while (rows-- > 0) {
                fetchRows();

                if (!_advance()) {
                    break;
                }
            }
        }

        return _currentRowValues != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean rowDeleted()
        throws SQLException
    {
        assertOpen();

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean rowInserted()
        throws SQLException
    {
        assertOpen();

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean rowUpdated()
        throws SQLException
    {
        assertOpen();

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setFetchDirection(final int direction)
        throws SQLException
    {
        assertOpen();

        if (isForwardOnly() && (direction != FETCH_FORWARD)) {
            throw _forwardOnlyException();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setFetchSize(final int rows)
        throws SQLException
    {
        assertOpen();
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T unwrap(final Class<T> iface)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateArray(
            final int columnNumber,
            final Array value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateArray(
            final String columnName,
            final Array value)
        throws SQLException
    {
        updateArray(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateAsciiStream(
            final int columnIndex,
            final InputStream stream)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateAsciiStream(
            final String columnName,
            final InputStream stream)
        throws SQLException
    {
        updateAsciiStream(findColumn(columnName), stream);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateAsciiStream(
            final int columnNumber,
            final InputStream stream,
            final int length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateAsciiStream(
            final int columnIndex,
            final InputStream stream,
            final long length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateAsciiStream(
            final String columnName,
            final InputStream stream,
            final int length)
        throws SQLException
    {
        updateAsciiStream(findColumn(columnName), stream, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateAsciiStream(
            final String columnName,
            final InputStream stream,
            final long length)
        throws SQLException
    {
        updateAsciiStream(findColumn(columnName), stream, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateBigDecimal(
            final int columnNumber,
            final BigDecimal value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateBigDecimal(
            final String columnName,
            final BigDecimal value)
        throws SQLException
    {
        updateBigDecimal(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateBinaryStream(
            final int columnIndex,
            final InputStream stream)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateBinaryStream(
            final String columnName,
            final InputStream stream)
        throws SQLException
    {
        updateBinaryStream(findColumn(columnName), stream);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateBinaryStream(
            final int columnNumber,
            final InputStream stream,
            final int length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateBinaryStream(
            final int columnIndex,
            final InputStream stream,
            final long length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateBinaryStream(
            final String columnName,
            final InputStream stream,
            final int length)
        throws SQLException
    {
        updateBinaryStream(findColumn(columnName), stream, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateBinaryStream(
            final String columnName,
            final InputStream stream,
            final long length)
        throws SQLException
    {
        updateBinaryStream(findColumn(columnName), stream, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateBlob(
            final int columnNumber,
            final Blob value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateBlob(
            final int columnIndex,
            final InputStream stream)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateBlob(
            final String columnName,
            final Blob value)
        throws SQLException
    {
        updateBlob(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateBlob(
            final String columnName,
            final InputStream stream)
        throws SQLException
    {
        updateBlob(findColumn(columnName), stream);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateBlob(
            final int columnIndex,
            final InputStream inputStream,
            final long length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateBlob(
            final String columnName,
            final InputStream stream,
            final long length)
        throws SQLException
    {
        updateBlob(findColumn(columnName), stream, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateBoolean(
            final int columnNumber,
            final boolean value)
        throws SQLException
    {
        updateObject(columnNumber, Boolean.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateBoolean(
            final String columnName,
            final boolean value)
        throws SQLException
    {
        updateBoolean(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateByte(
            final int columnNumber,
            final byte value)
        throws SQLException
    {
        updateObject(columnNumber, Byte.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateByte(
            final String columnName,
            final byte value)
        throws SQLException
    {
        updateByte(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateBytes(
            final int columnNumber,
            final byte[] value)
        throws SQLException
    {
        updateObject(columnNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateBytes(
            final String columnName,
            final byte[] value)
        throws SQLException
    {
        updateBytes(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateCharacterStream(
            final int columnIndex,
            final Reader reader)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateCharacterStream(
            final String columnName,
            final Reader reader)
        throws SQLException
    {
        updateCharacterStream(findColumn(columnName), reader);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateCharacterStream(
            final int columnNumber,
            final Reader reader,
            final int length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateCharacterStream(
            final int columnIndex,
            final Reader reader,
            final long length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateCharacterStream(
            final String columnName,
            final Reader reader,
            final int length)
        throws SQLException
    {
        updateCharacterStream(findColumn(columnName), reader, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateCharacterStream(
            final String columnName,
            final Reader reader,
            final long length)
        throws SQLException
    {
        updateCharacterStream(findColumn(columnName), reader, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateClob(
            final int columnNumber,
            final Clob value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateClob(
            final int columnIndex,
            final Reader reader)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateClob(
            final String columnName,
            final Clob value)
        throws SQLException
    {
        updateClob(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateClob(
            final String columnName,
            final Reader reader)
        throws SQLException
    {
        updateClob(findColumn(columnName), reader);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateClob(
            final int columnIndex,
            final Reader reader,
            final long length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateClob(
            final String columnName,
            final Reader reader,
            final long length)
        throws SQLException
    {
        updateClob(findColumn(columnName), reader, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateDate(
            final int columnNumber,
            final Date value)
        throws SQLException
    {
        updateObject(columnNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateDate(
            final String columnName,
            final Date value)
        throws SQLException
    {
        updateDate(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateDouble(
            final int columnNumber,
            final double value)
        throws SQLException
    {
        updateObject(columnNumber, Double.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateDouble(
            final String columnName,
            final double value)
        throws SQLException
    {
        updateDouble(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateFloat(
            final int columnNumber,
            final float value)
        throws SQLException
    {
        updateObject(columnNumber, Float.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateFloat(
            final String columnName,
            final float value)
        throws SQLException
    {
        updateFloat(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateInt(
            final int columnNumber,
            final int value)
        throws SQLException
    {
        updateObject(columnNumber, Integer.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateInt(
            final String columnName,
            final int value)
        throws SQLException
    {
        updateInt(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateLong(
            final int columnNumber,
            final long value)
        throws SQLException
    {
        updateObject(columnNumber, Long.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateLong(
            final String columnName,
            final long value)
        throws SQLException
    {
        updateLong(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNCharacterStream(
            final int columnIndex,
            final Reader reader)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNCharacterStream(
            final String columnName,
            final Reader reader)
        throws SQLException
    {
        updateNCharacterStream(findColumn(columnName), reader);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNCharacterStream(
            final int columnIndex,
            final Reader reader,
            final long length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNCharacterStream(
            final String columnName,
            final Reader reader,
            final long length)
        throws SQLException
    {
        updateNCharacterStream(findColumn(columnName), reader, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNClob(
            final int columnIndex,
            final NClob nClob)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNClob(
            final int columnIndex,
            final Reader reader)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNClob(
            final String columnName,
            final NClob nClob)
        throws SQLException
    {
        updateNClob(findColumn(columnName), nClob);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNClob(
            final String columnName,
            final Reader reader)
        throws SQLException
    {
        updateNClob(findColumn(columnName), reader);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNClob(
            final int columnIndex,
            final Reader reader,
            final long length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNClob(
            final String columnName,
            final Reader reader,
            final long length)
        throws SQLException
    {
        updateNClob(findColumn(columnName), reader, length);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNString(
            final int columnIndex,
            final String nString)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateNString(
            final String columnName,
            final String nString)
        throws SQLException
    {
        updateNString(findColumn(columnName), nString);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateNull(final int columnNumber)
        throws SQLException
    {
        updateObject(columnNumber, null);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateNull(final String columnName)
        throws SQLException
    {
        updateNull(findColumn(columnName));
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateObject(
            final int columnNumber,
            final Object value)
        throws SQLException
    {
        assertOpen();

        throw resultSetReadOnly();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateObject(
            final String columnName,
            final Object value)
        throws SQLException
    {
        updateObject(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateObject(
            final int columnNumber,
            final Object value,
            final int scale)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateObject(
            final String columnName,
            final Object value,
            final int scale)
        throws SQLException
    {
        updateObject(findColumn(columnName), value, scale);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateRef(
            final int columnNumber,
            final Ref value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateRef(
            final String columnName,
            final Ref value)
        throws SQLException
    {
        updateRef(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateRow()
        throws SQLException
    {
        assertOpen();

        throw resultSetReadOnly();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateRowId(
            final int columnIndex,
            final RowId rowId)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateRowId(
            final String columnName,
            final RowId rowId)
        throws SQLException
    {
        updateRowId(findColumn(columnName), rowId);
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateSQLXML(
            final int columnIndex,
            final SQLXML xmlObject)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void updateSQLXML(
            final String columnName,
            final SQLXML xmlObject)
        throws SQLException
    {
        updateSQLXML(findColumn(columnName), xmlObject);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateShort(
            final int columnNumber,
            final short value)
        throws SQLException
    {
        updateObject(columnNumber, Short.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateShort(
            final String columnName,
            final short value)
        throws SQLException
    {
        updateShort(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateString(
            final int columnNumber,
            final String value)
        throws SQLException
    {
        updateObject(columnNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateString(
            final String columnName,
            final String value)
        throws SQLException
    {
        updateString(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateTime(
            final int columnNumber,
            final Time value)
        throws SQLException
    {
        updateObject(columnNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateTime(
            final String columnName,
            final Time value)
        throws SQLException
    {
        updateTime(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateTimestamp(
            final int columnNumber,
            final Timestamp value)
        throws SQLException
    {
        updateObject(columnNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void updateTimestamp(
            final String columnName,
            final Timestamp value)
        throws SQLException
    {
        updateTimestamp(findColumn(columnName), value);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean wasNull()
        throws SQLException
    {
        assertOpen();

        return _wasNull;
    }

    /**
     * Returns a "ResultSet ReadOnly" SQLException.
     *
     * @return An SQLException.
     */
    @Nonnull
    @CheckReturnValue
    static SQLException resultSetReadOnly()
    {
        return JDBCMessages.RESULT_SET_READ_ONLY.exception();
    }

    /**
     * Adds a column.
     *
     * @param name The optional column name.
     * @param label The optional column label.
     * @param displaySize The column display size.
     * @param type The column type.
     * @param precision The column precision.
     * @param scale The column scale.
     * @param nullable True if the column may be null.
     * @param objectClass The optional fetched object class.
     */
    final void addColumn(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<String> label,
            final int displaySize,
            final int type,
            final int precision,
            final int scale,
            final boolean nullable,
            @Nonnull final Optional<Class<?>> objectClass)
    {
        _metaData
            .addColumn(
                name,
                label,
                displaySize,
                type,
                precision,
                scale,
                nullable,
                objectClass,
                false);
    }

    /**
     * Adds a row.
     *
     * @param rowValues The row values.
     */
    final void addRow(@Nonnull final Object... rowValues)
    {
        _rows.add(rowValues);
    }

    /**
     * Asserts that this result set is open.
     *
     * @throws SQLException When it is not.
     */
    final void assertOpen()
        throws SQLException
    {
        if (_closed) {
            throw JDBCMessages.RESULT_SET_CLOSED.exception();
        }
    }

    /**
     * Does fetch rows.
     *
     * @throws SQLException On failure.
     */
    void doFetchRows()
        throws SQLException {}

    /**
     * Fetches rows.
     *
     * @throws SQLException On failure.
     */
    final void fetchRows()
        throws SQLException
    {
        if ((_rowCount < 0) && !hasNext()) {
            final int rowsSize = _rows.size();

            _rowsIterator = null;
            doFetchRows();

            if (_rows.size() == rowsSize) {
                _rowCount = isForwardOnly()? _currentRow: _rows.size();
            }
        }
    }

    /**
     * Gets the connection.
     *
     * @return The connection.
     */
    @Nonnull
    @CheckReturnValue
    final StoreConnection getConnection()
    {
        return _connection;
    }

    /**
     * Gets the current row.
     *
     * @return The current row.
     */
    @CheckReturnValue
    final int getCurrentRow()
    {
        return (_currentRowValues != null)? _currentRow: -1;
    }

    /**
     * Asks if there is a next row.
     *
     * @return True if there is a next row.
     */
    @CheckReturnValue
    final boolean hasNext()
    {
        if (isForwardOnly()) {
            return !_rows.isEmpty();
        }

        if (_rowsIterator == null) {
            _rowsIterator = (_currentRow <= _rows.size())? _rows
                .listIterator(_currentRow): null;
        }

        return (_rowsIterator != null) && _rowsIterator.hasNext();
    }

    /**
     * Asks if this result set is forward-only.
     *
     * @return True if it is forward-only.
     */
    @CheckReturnValue
    boolean isForwardOnly()
    {
        return true;
    }

    private static SQLException _forwardOnlyException()
    {
        return JDBCMessages.RESULT_SET_FORWARD_ONLY.exception();
    }

    private boolean _advance()
    {
        if (!hasNext()) {
            if (_currentRowValues != null) {
                ++_currentRow;
                _currentRowValues = null;
            }

            return false;
        }

        _currentRowValues = isForwardOnly()? _rows
            .removeFirst(): _rowsIterator.next();
        ++_currentRow;

        return true;
    }

    private Object _getColumnValue(final int columnNumber)
        throws SQLException
    {
        assertOpen();

        if (_currentRowValues == null) {
            throw JDBCMessages.NO_CURRENT_ROW.exception();
        }

        final Object value;

        try {
            value = _currentRowValues[columnNumber - 1];
        } catch (final IndexOutOfBoundsException exception) {
            throw JDBCMessages.INVALID_COLUMN_NUMBER
                .exception(String.valueOf(columnNumber));
        }

        _wasNull = value == null;

        return value;
    }

    private void _setCurrentRow(final int row)
    {
        _currentRow = row;
        _currentRowValues = null;
        _rowsIterator = (_currentRow <= _rows.size())? _rows
            .listIterator(_currentRow): null;
    }

    private boolean _closed;
    private final StoreConnection _connection;
    private int _currentRow;
    private Object[] _currentRowValues;
    private final StoreResultSetMetaData _metaData;
    private int _rowCount = -1;
    private final LinkedList<Object[]> _rows = new LinkedList<Object[]>();
    private ListIterator<Object[]> _rowsIterator;
    private SQLWarning _warnings;
    private boolean _wasNull;
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
