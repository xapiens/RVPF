/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreResultSetMetaData.java 3973 2019-05-10 16:49:57Z SFB $
 */

package org.rvpf.jdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Store result set meta-data.
 */
final class StoreResultSetMetaData
    implements ResultSetMetaData
{
    /** {@inheritDoc}
     */
    @Override
    public String getCatalogName(final int columnNumber)
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public String getColumnClassName(final int columnNumber)
        throws SQLException
    {
        return _getColumn(columnNumber).getObjectClass().getName();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getColumnCount()
        throws SQLException
    {
        return _columns.size();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getColumnDisplaySize(final int columnNumber)
        throws SQLException
    {
        return _getColumn(columnNumber).getDisplaySize();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getColumnLabel(final int columnNumber)
        throws SQLException
    {
        return _getColumn(columnNumber).getLabel();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getColumnName(final int columnNumber)
        throws SQLException
    {
        return _getColumn(columnNumber).getName();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getColumnType(final int columnNumber)
        throws SQLException
    {
        return _getColumn(columnNumber).getType();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getColumnTypeName(final int columnNumber)
        throws SQLException
    {
        return StoreColumn.getTypeName(_getColumn(columnNumber).getType());
    }

    /** {@inheritDoc}
     */
    @Override
    public int getPrecision(final int columnNumber)
        throws SQLException
    {
        return _getColumn(columnNumber).getPrecision();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getScale(final int columnNumber)
        throws SQLException
    {
        return _getColumn(columnNumber).getScale();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getSchemaName(final int columnNumber)
        throws SQLException
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public String getTableName(final int columnNumber)
        throws SQLException
    {
        return (_tableName != null)? _tableName: "";
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isAutoIncrement(final int columnNumber)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isCaseSensitive(final int columnNumber)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isCurrency(final int columnNumber)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isDefinitelyWritable(
            final int columnNumber)
        throws SQLException
    {
        return isWritable(columnNumber);
    }

    /** {@inheritDoc}
     */
    @Override
    public int isNullable(final int columnNumber)
        throws SQLException
    {
        return _getColumn(columnNumber)
            .isNullable()? columnNullable: columnNoNulls;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isReadOnly(final int columnNumber)
        throws SQLException
    {
        return !isWritable(columnNumber);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSearchable(final int columnNumber)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSigned(final int columnNumber)
        throws SQLException
    {
        return false;
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
    public boolean isWritable(final int columnNumber)
        throws SQLException
    {
        return _getColumn(columnNumber).isWritable();
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T unwrap(final Class<T> iface)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
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
     * @param writable True if the column is writable.
     */
    void addColumn(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<String> label,
            final int displaySize,
            final int type,
            final int precision,
            final int scale,
            final boolean nullable,
            @Nonnull final Optional<Class<?>> objectClass,
            final boolean writable)
    {
        final _Column column = new _Column();

        column.setName(name);
        column.setLabel(label);
        column
            .setDisplaySize(
                (displaySize > 0)? displaySize: (label.isPresent()? label
                    .get()
                    .length(): 0));
        column.setType(type);
        column.setPrecision(precision);
        column.setScale(scale);
        column.setNullable(nullable);
        column.setObjectClass(objectClass);
        column.setWritable(writable);
        _columns.add(column);
    }

    /**
     * Adopts a query request.
     *
     * @param request The query request.
     */
    void adoptQueryRequest(@Nonnull final Request.Query request)
    {
        _tableName = request.getTableName().orElse(null);

        final Iterator<String> titles = request.getTitles().iterator();

        for (final StoreColumn storeColumn: request.getColumns()) {
            final _Column column = new _Column(storeColumn);

            column.setLabel(Optional.of(titles.next()));
            _columns.add(column);
        }
    }

    /**
     * Finds a column by its name.
     *
     * @param name The column name.
     *
     * @return The column number.
     *
     * @throws SQLException When the column is unknown.
     */
    @CheckReturnValue
    int findColumn(@Nonnull final String name)
        throws SQLException
    {
        final int columnCount = getColumnCount();

        for (int columnNumber = 1; columnNumber <= columnCount;
                ++columnNumber) {
            final _Column column = _getColumn(columnNumber);

            if (column.getName().equalsIgnoreCase(name)
                    || column.getLabel().equalsIgnoreCase(name)) {
                return columnNumber;
            }
        }

        throw JDBCMessages.UNKNOWN_COLUMN.exception(name);
    }

    /**
     * Gets the store column.
     *
     * @param columnNumber The column number.
     *
     * @return The store column.
     *
     * @throws SQLException When the column number is invalid.
     */
    @Nonnull
    @CheckReturnValue
    StoreColumn getStoreColumn(final int columnNumber)
        throws SQLException
    {
        return _getColumn(columnNumber).getColumn();
    }

    private _Column _getColumn(final int columnNumber)
        throws SQLException
    {
        try {
            return _columns.get(columnNumber - 1);
        } catch (final IndexOutOfBoundsException exception) {
            throw JDBCMessages.INVALID_COLUMN_NUMBER
                .exception(String.valueOf(columnNumber));
        }
    }

    private final List<_Column> _columns = new ArrayList<_Column>();
    private String _tableName;

    /**
     * Column.
     */
    private static final class _Column
    {
        /**
         * Constructs an instance.
         */
        _Column()
        {
            _column = null;
        }

        /**
         * Constructs an instance.
         *
         * @param storeColumn The store column.
         */
        _Column(final StoreColumn storeColumn)
        {
            _column = storeColumn;
            _name = _column.name();
            _displaySize = _column.getDisplaySize();
            _type = _column.getType();
            _nullable = _column.isNullable();
            _objectClass = _column.getObjectClass();
            _writable = _column.isWritable();
        }

        /**
         * Gets the column.
         *
         * @return The column.
         */
        @Nonnull
        @CheckReturnValue
        StoreColumn getColumn()
        {
            return _column;
        }

        /**
         * Gets the display size.
         *
         * @return The display size.
         */
        @CheckReturnValue
        int getDisplaySize()
        {
            return _displaySize;
        }

        /**
         * Gets the label.
         *
         * @return The label.
         */
        @Nonnull
        @CheckReturnValue
        String getLabel()
        {
            return _label;
        }

        /**
         * Gets the name.
         *
         * @return The name.
         */
        @Nonnull
        @CheckReturnValue
        String getName()
        {
            return _name;
        }

        /**
         * Gets the object class.
         *
         * @return The object class.
         */
        @Nonnull
        @CheckReturnValue
        Class<?> getObjectClass()
        {
            return _objectClass;
        }

        /**
         * Gets the precision.
         *
         * @return The precision.
         */
        @CheckReturnValue
        int getPrecision()
        {
            return _precision;
        }

        /**
         * Gets the scale.
         *
         * @return The scale.
         */
        @CheckReturnValue
        int getScale()
        {
            return _scale;
        }

        /**
         * Gets the type.
         *
         * @return The type.
         */
        @CheckReturnValue
        int getType()
        {
            return _type;
        }

        /**
         * Asks if nullable.
         *
         * @return True if nullable.
         */
        @CheckReturnValue
        boolean isNullable()
        {
            return _nullable;
        }

        /**
         * Asks if writable.
         *
         * @return True if writable.
         */
        @CheckReturnValue
        boolean isWritable()
        {
            return _writable;
        }

        /**
         * Sets the display size.
         *
         * @param displaySize The display size.
         */
        void setDisplaySize(final int displaySize)
        {
            _displaySize = displaySize;
        }

        /**
         * Sets the label.
         *
         * @param label The optional label.
         */
        void setLabel(@Nonnull final Optional<String> label)
        {
            _label = label.orElse(null);
        }

        /**
         * Sets the name.
         *
         * @param name The optional name.
         */
        void setName(@Nonnull final Optional<String> name)
        {
            _name = name.orElse(null);
        }

        /**
         * Sets nullable.
         *
         * @param nullable True if nullable.
         */
        void setNullable(final boolean nullable)
        {
            _nullable = nullable;
        }

        /**
         * Sets the object class.
         *
         * @param objectClass The optional object class.
         */
        void setObjectClass(@Nonnull final Optional<Class<?>> objectClass)
        {
            _objectClass = objectClass.orElse(null);
        }

        /**
         * Sets the precision.
         *
         * @param precision The precision.
         */
        void setPrecision(final int precision)
        {
            _precision = precision;
        }

        /**
         * Sets the scale.
         *
         * @param scale The scale.
         */
        void setScale(final int scale)
        {
            _scale = scale;
        }

        /**
         * Sets the type.
         *
         * @param type The type.
         */
        void setType(final int type)
        {
            _type = type;
        }

        /**
         * Sets as writable.
         *
         * @param writable True if writable.
         */
        void setWritable(final boolean writable)
        {
            _writable = writable;
        }

        private final StoreColumn _column;
        private int _displaySize;
        private String _label;
        private String _name;
        private boolean _nullable;
        private Class<?> _objectClass;
        private int _precision;
        private int _scale;
        private int _type;
        private boolean _writable;
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
