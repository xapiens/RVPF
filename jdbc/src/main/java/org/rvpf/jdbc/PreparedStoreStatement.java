/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PreparedStoreStatement.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;

import java.math.BigDecimal;

import java.net.URL;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Prepared store statement.
 */
final class PreparedStoreStatement
    extends StoreStatement
    implements PreparedStatement
{
    /**
     * Constructs an instance.
     *
     * @param connection The store connection.
     * @param type The result set type.
     * @param concurrency The result set concurrency.
     * @param holdability The result set holdability.
     * @param sql The SQL statement.
     *
     * @throws SQLException On failure to parse the SQL statement.
     */
    PreparedStoreStatement(
            @Nonnull final StoreConnection connection,
            final int type,
            final int concurrency,
            final int holdability,
            @Nonnull final String sql)
        throws SQLException
    {
        super(connection, type, concurrency, holdability);

        _request = getParser()
            .parseSQL(sql, Optional.of(new LinkedList<Token.Parameter>()));

        if (_request.isQuery()) {
            final StoreResultSetMetaData resultSetMetaData =
                new StoreResultSetMetaData();

            resultSetMetaData.adoptQueryRequest((Request.Query) _request);
            _resultSetMetaData = Optional.of(resultSetMetaData);
        } else {
            _resultSetMetaData = Optional.empty();
        }

        _parameterMetaData = new StoreParameterMetaData(
            _request.getParameterCount());
    }

    /** {@inheritDoc}
     */
    @Override
    public void addBatch()
        throws SQLException
    {
        assertOpen();

        addBatch(_request.copy());
    }

    /** {@inheritDoc}
     */
    @Override
    public void clearParameters()
        throws SQLException
    {
        assertOpen();

        _request.clearParameters();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean execute()
        throws SQLException
    {
        assertOpen();
        endResults();
        clearWarnings();

        return execute(_request, _resultSetMetaData);
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSet executeQuery()
        throws SQLException
    {
        assertOpen();
        endResults();
        clearWarnings();

        if (!_request.isQuery()) {
            throw JDBCMessages.NO_RESULT_SET
                .exception(_request.getCommandToken());
        }

        return executeQuery((Request.Query) _request, _resultSetMetaData);
    }

    /** {@inheritDoc}
     */
    @Override
    public int executeUpdate()
        throws SQLException
    {
        assertOpen();
        endResults();
        clearWarnings();

        if (!_request.isUpdate()) {
            throw JDBCMessages.NOT_AN_UPDATE
                .exception(_request.getCommandToken());
        }

        return executeUpdate((Request.Update) _request);
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultSetMetaData getMetaData()
        throws SQLException
    {
        assertOpen();

        return _resultSetMetaData.orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public ParameterMetaData getParameterMetaData()
        throws SQLException
    {
        assertOpen();

        return _parameterMetaData;
    }

    /** {@inheritDoc}
     */
    @Override
    public void setArray(final int i, final Array array)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setAsciiStream(
            final int parameterIndex,
            final InputStream x)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setAsciiStream(
            final int parameterNumber,
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
    public void setAsciiStream(
            final int parameterIndex,
            final InputStream x,
            final long length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setBigDecimal(
            final int parameterNumber,
            final BigDecimal value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setBinaryStream(
            final int parameterIndex,
            final InputStream x)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setBinaryStream(
            final int parameterNumber,
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
    public void setBinaryStream(
            final int parameterIndex,
            final InputStream x,
            final long length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setBlob(final int i, final Blob value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setBlob(
            final int parameterIndex,
            final InputStream inputStream)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setBlob(
            final int parameterIndex,
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
    public void setBoolean(
            final int parameterNumber,
            final boolean value)
        throws SQLException
    {
        setObject(parameterNumber, Boolean.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public void setByte(
            final int parameterNumber,
            final byte value)
        throws SQLException
    {
        setObject(parameterNumber, Byte.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public void setBytes(
            final int parameterNumber,
            final byte[] value)
        throws SQLException
    {
        setObject(parameterNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setCharacterStream(
            final int parameterIndex,
            final Reader reader)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setCharacterStream(
            final int parameterNumber,
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
    public void setCharacterStream(
            final int parameterIndex,
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
    public void setClob(final int i, final Clob value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setClob(
            final int parameterIndex,
            final Reader reader)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setClob(
            final int parameterIndex,
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
    public void setDate(
            final int parameterNumber,
            final Date value)
        throws SQLException
    {
        setObject(parameterNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setDate(
            final int parameterNumber,
            final Date value,
            final Calendar cal)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setDouble(
            final int parameterNumber,
            final double value)
        throws SQLException
    {
        setObject(parameterNumber, Double.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public void setFloat(
            final int parameterNumber,
            final float value)
        throws SQLException
    {
        setObject(parameterNumber, Float.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public void setInt(
            final int parameterNumber,
            final int value)
        throws SQLException
    {
        setObject(parameterNumber, Integer.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public void setLong(
            final int parameterNumber,
            final long value)
        throws SQLException
    {
        setObject(parameterNumber, Long.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public void setNCharacterStream(
            final int parameterIndex,
            final Reader value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setNCharacterStream(
            final int parameterIndex,
            final Reader value,
            final long length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setNClob(
            final int parameterIndex,
            final NClob value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setNClob(
            final int parameterIndex,
            final Reader reader)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setNClob(
            final int parameterIndex,
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
    public void setNString(
            final int parameterIndex,
            final String value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setNull(
            final int parameterNumber,
            final int sqlType)
        throws SQLException
    {
        setObject(parameterNumber, null);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setNull(
            final int parameterNumber,
            final int sqlType,
            final String typeName)
        throws SQLException
    {
        setNull(parameterNumber, sqlType);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setObject(
            final int parameterNumber,
            final Object value)
        throws SQLException
    {
        assertOpen();

        _request.setParameter(parameterNumber, (Serializable) value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setObject(
            final int parameterNumber,
            final Object value,
            final int targetSqlType)
        throws SQLException
    {
        setObject(parameterNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setObject(
            final int parameterNumber,
            final Object value,
            final int targetSqlType,
            final int scale)
        throws SQLException
    {
        setObject(parameterNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setRef(
            final int parameterNumber,
            final Ref value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setRowId(
            final int parameterIndex,
            final RowId x)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setSQLXML(
            final int parameterIndex,
            final SQLXML xmlObject)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setShort(
            final int parameterNumber,
            final short value)
        throws SQLException
    {
        setObject(parameterNumber, Short.valueOf(value));
    }

    /** {@inheritDoc}
     */
    @Override
    public void setString(
            final int parameterNumber,
            final String value)
        throws SQLException
    {
        setObject(parameterNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setTime(
            final int parameterNumber,
            final Time value)
        throws SQLException
    {
        setObject(parameterNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setTime(
            final int parameterNumber,
            final Time value,
            final Calendar cal)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setTimestamp(
            final int parameterNumber,
            final Timestamp value)
        throws SQLException
    {
        setObject(parameterNumber, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setTimestamp(
            final int parameterNumber,
            final Timestamp value,
            final Calendar cal)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setURL(
            final int parameterNumber,
            final URL value)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Deprecated
    @Override
    public void setUnicodeStream(
            final int parameterNumber,
            final InputStream value,
            final int length)
        throws SQLException
    {
        assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    private final ParameterMetaData _parameterMetaData;
    private final Request _request;
    private final Optional<StoreResultSetMetaData> _resultSetMetaData;
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
