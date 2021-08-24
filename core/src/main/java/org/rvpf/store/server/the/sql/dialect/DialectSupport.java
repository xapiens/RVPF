/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DialectSupport.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.sql.dialect;

import java.io.File;

import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.store.server.StoreCursor;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.the.TheStoreServiceAppImpl;
import org.rvpf.store.server.the.sql.TheStoreConnection;

/**
 * Database dialect support.
 */
public interface DialectSupport
{
    /**
     * Gets the catalog name.
     *
     * @return The catalog name.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getCatalogName();

    /**
     * Gets the SQL for statements that create the table.
     *
     * @return The SQL.
     */
    @Nonnull
    @CheckReturnValue
    List<String> getCreateTableSQL();

    /**
     * Gets the default client driver class definition.
     *
     * @return The default client driver class definition.
     */
    @Nonnull
    @CheckReturnValue
    ClassDefImpl getDefaultClientDriverClassDef();

    /**
     * Gets the default connection options.
     *
     * @return The default connection options.
     */
    @Nonnull
    @CheckReturnValue
    default String getDefaultConnectionOptions()
    {
        return "";
    }

    /**
     * Gets the default connection password.
     *
     * @return The optional default connection password.
     */
    @Nonnull
    @CheckReturnValue
    default Optional<String> getDefaultConnectionPassword()
    {
        return Optional.empty();
    }

    /**
     * Gets the default connection shared indicator.
     *
     * @return The default connection shared indicator.
     */
    @CheckReturnValue
    default boolean getDefaultConnectionShared()
    {
        return false;
    }

    /**
     * Gets the default connection URL.
     *
     * <p>Will always fail when not overridden.</p>
     *
     * @param storeDataDir The store data directory.
     * @param storeEntityName The store entity name.
     *
     * @return The default connection URL.
     */
    @Nonnull
    @CheckReturnValue
    default String getDefaultConnectionURL(
            @Nonnull File storeDataDir,
            @Nonnull String storeEntityName)
    {
        return Require.failure().toString();
    }

    /**
     * Gets the default connection user.
     *
     * <p>Will always fail when not overridden.</p>
     *
     * @return The default connection user.
     */
    @Nonnull
    @CheckReturnValue
    default String getDefaultConnectionUser()
    {
        return Require.failure().toString();
    }

    /**
     * Gets the SQL for a DELETE statement.
     *
     * @param purge True when the operation is a purge.
     *
     * @return The SQL.
     */
    @Nonnull
    @CheckReturnValue
    StringBuilder getDeleteSQL(boolean purge);

    /**
     * Gets the dialect name.
     *
     * @return The dialect name.
     */
    @Nonnull
    @CheckReturnValue
    String getDialectName();

    /**
     * Gets the SQL for an INSERT statement.
     *
     * @return The SQL.
     */
    @Nonnull
    @CheckReturnValue
    StringBuilder getInsertSQL();

    /**
     * Gets the schema name.
     *
     * @return The schema name.
     */
    @Nonnull
    @CheckReturnValue
    String getSchemaName();

    /**
     * Gets the SQL for a SELECT statement.
     *
     * @param cursor The store cursor.
     *
     * @return The SQL.
     */
    @Nonnull
    @CheckReturnValue
    StringBuilder getSelectSQL(StoreCursor cursor);

    /**
     * Gets the table name.
     *
     * @return The table name.
     */
    String getTableName();

    /**
     * Gets the SQL for an UPDATE statement.
     *
     * @return The SQL.
     */
    StringBuilder getUpdateSQL();

    /**
     * Called on the first connection.
     *
     * @param connection The newly established connection.
     */
    void onFirstConnection(TheStoreConnection connection);

    /**
     * Registers the JDBC driver.
     *
     * @param driverClassDef The driver class definition.
     *
     * @return The driver class or null.
     */
    Class<? extends Driver> registerDriver(ClassDef driverClassDef);

    /**
     * Sets a DELETE statement.
     *
     * @param statement The Statement prepared from {@link #getDeleteSQL}.
     * @param point The bytes of the Point's UUID.
     * @param startStamp The raw value of the start time stamp.
     * @param finishStamp The raw value of the finish time stamp.
     *
     * @return The number of columns set.
     *
     * @throws SQLException From java.sql.
     */
    int setDeleteStatement(
            @Nonnull PreparedStatement statement,
            @Nonnull byte[] point,
            long startStamp,
            long finishStamp)
        throws SQLException;

    /**
     * Sets an INSERT statement according to the PointValue.
     *
     * @param statement The Statement prepared from {@link #getInsertSQL}.
     * @param point The bytes of the Point's UUID.
     * @param stamp The raw value of the Point's time stamp.
     * @param version The raw value of the version stamp.
     * @param state The value's state.
     * @param value The Point's serialized value.
     *
     * @return The number of columns set.
     *
     * @throws SQLException From java.sql.
     */
    int setInsertStatement(
            PreparedStatement statement,
            byte[] point,
            long stamp,
            long version,
            byte[] state,
            byte[] value)
        throws SQLException;

    /**
     * Sets a SELECT statement according to the Request and request type.
     *
     * @param statement The statement prepared from {@link #getInsertSQL}.
     * @param cursor The store cursor for which the statement has been prepared.
     *
     * @return The number of columns set.
     *
     * @throws SQLException From java.sql.
     */
    int setSelectStatement(
            PreparedStatement statement,
            StoreCursor cursor)
        throws SQLException;

    /**
     * Sets up the basic informations for dialect support.
     *
     * @param storeAppImpl The store application implementation.
     * @param storeDataDir The store data directory.
     * @param storeEntityName The store entity name.
     *
     * @return True on success.
     */
    boolean setUp(
            StoreServiceAppImpl storeAppImpl,
            File storeDataDir,
            String storeEntityName);

    /**
     * Sets an UPDATE statement according to the PointValue.
     *
     * @param statement The Statement prepared from {@link #getUpdateSQL}.
     * @param point The bytes of the Point's UUID.
     * @param stamp The raw value of the Point's time stamp.
     * @param version The raw value of the version stamp.
     * @param state The value's serialized state.
     * @param value The Point's serialized value.
     *
     * @return The number of columns set.
     *
     * @throws SQLException From java.sql.
     */
    int setUpdateStatement(
            PreparedStatement statement,
            byte[] point,
            long stamp,
            long version,
            byte[] state,
            byte[] value)
        throws SQLException;

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /** The name of the archive table. */
    String ARCHIVE_TABLE_PROPERTY = "sql.table.archive";

    /** The name of the database catalog. */
    String CATALOG_PROPERTY = "sql.catalog";

    /** Default archive table name. */
    String DEFAULT_ARCHIVE_TABLE_NAME = "Archive";

    /** Default catalog name. */
    String DEFAULT_CATALOG_NAME = null;

    /** Default point column. */
    String DEFAULT_POINT_COLUMN = "Point";

    /** Default schema name. */
    String DEFAULT_SCHEMA_NAME = "";

    /** Default snapshot table name. */
    String DEFAULT_SNAPSHOT_TABLE_NAME = "Snapshot";

    /** Default stamp column. */
    String DEFAULT_STAMP_COLUMN = "Stamp";

    /** Default state column. */
    String DEFAULT_STATE_COLUMN = "State";

    /** Default value column. */
    String DEFAULT_VALUE_COLUMN = "Value";

    /** Default version column. */
    String DEFAULT_VERSION_COLUMN = "Version";

    /** The name of the Point column. */
    String POINT_COLUMN_PROPERTY = "sql.column.point";

    /** The name of the database schema. */
    String SCHEMA_PROPERTY = "sql.schema";

    /** The name of the snapshot table. */
    String SNAPSHOT_TABLE_PROPERTY = "sql.table.snapshot";

    /** The name of the Stamp column. */
    String STAMP_COLUMN_PROPERTY = "sql.column.stamp";

    /** The name of the State column. */
    String STATE_COLUMN_PROPERTY = "sql.column.state";

    /** The name of the database table. */
    String TABLE_PROPERTY = "sql.table";

    /** The name of the Value column. */
    String VALUE_COLUMN_PROPERTY = "sql.column.value";

    /** The name of the Version column. */
    String VERSION_COLUMN_PROPERTY = "sql.column.version";

    /**
     * Abstract dialect support.
     */
    abstract class Abstract
        implements DialectSupport
    {
        /**
         * Constructs an instance.
         *
         * @param dialectName The dialect name.
         */
        protected Abstract(@Nonnull final String dialectName)
        {
            _dialectName = dialectName;

            getThisLogger().debug(StoreMessages.DIALECT_SELECTED, dialectName);
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<String> getCatalogName()
        {
            return Optional.ofNullable(_catalogName);
        }

        /** {@inheritDoc}
         */
        @Override
        public List<String> getCreateTableSQL()
        {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc}
         */
        @Override
        public ClassDefImpl getDefaultClientDriverClassDef()
        {
            return getDefaultDriverClassDef().get();
        }

        /** {@inheritDoc}
         */
        @Override
        public StringBuilder getDeleteSQL(final boolean purge)
        {
            final StringBuilder sqlString = new StringBuilder("DELETE FROM ");

            sqlString.append(getCatalogSchemaTable());
            sqlString.append(" WHERE ");
            sqlString.append(getPointColumn());
            sqlString.append("=?");

            if (!isSnapshot()) {
                sqlString.append(" AND ");
                sqlString.append(getStampColumn());

                if (purge) {
                    sqlString.append(">=?");
                    sqlString.append(" AND ");
                    sqlString.append(getStampColumn());
                    sqlString.append("<=?");
                } else {
                    sqlString.append("=?");
                }
            }

            getThisLogger().trace(StoreMessages.SQL, sqlString);

            return sqlString;
        }

        /** {@inheritDoc}
         */
        @Override
        public final String getDialectName()
        {
            return _dialectName;
        }

        /** {@inheritDoc}
         */
        @Override
        public StringBuilder getInsertSQL()
        {
            final StringBuilder sqlString = new StringBuilder("INSERT INTO ");

            sqlString.append(getCatalogSchemaTable());
            sqlString.append(" (");
            sqlString.append(getPointColumn());
            sqlString.append(",");
            sqlString.append(getStampColumn());
            sqlString.append(",");
            sqlString.append(getVersionColumn());
            sqlString.append(",");
            sqlString.append(getStateColumn());
            sqlString.append(",");
            sqlString.append(getValueColumn());
            sqlString.append(") VALUES(?,?,?,?,?)");

            getThisLogger().trace(StoreMessages.SQL, sqlString);

            return sqlString;
        }

        /** {@inheritDoc}
         */
        @Override
        public final String getSchemaName()
        {
            return _schemaName;
        }

        /** {@inheritDoc}
         */
        @Override
        public StringBuilder getSelectSQL(final StoreCursor cursor)
        {
            final StringBuilder sqlString = new StringBuilder("SELECT ");
            boolean where = false;

            if (cursor.isCount()) {
                sqlString.append("COUNT(1)");
            } else {
                sqlString.append(getPointColumn());
                sqlString.append(",");
                sqlString.append(getStampColumn());
                sqlString.append(",");
                sqlString.append(getVersionColumn());
                sqlString.append(",");
                sqlString.append(getStateColumn());
                sqlString.append(",");
                sqlString.append(getValueColumn());
            }

            sqlString.append(" FROM ");
            sqlString.append(getCatalogSchemaTable());

            if (cursor.getPointUUID().isPresent()) {
                final boolean includeDeleted = cursor.isIncludeDeleted();

                sqlString.append(" WHERE ");
                where = true;

                if (includeDeleted) {
                    sqlString.append('(');
                }

                sqlString.append(getPointColumn());
                sqlString.append("=?");

                if (includeDeleted) {
                    sqlString.append(" OR ");
                    sqlString.append(getPointColumn());
                    sqlString.append("=?");
                    sqlString.append(')');
                }
            }

            final String columnName = cursor
                .isPull()? getVersionColumn(): getStampColumn();

            if (cursor.getAfter().isPresent()) {
                if (where) {
                    sqlString.append(" AND ");
                } else {
                    sqlString.append(" WHERE ");
                    where = true;
                }

                sqlString.append(columnName);
                sqlString.append(">?");
            }

            if (cursor.getBefore().isPresent()) {
                if (where) {
                    sqlString.append(" AND ");
                } else {
                    sqlString.append(" WHERE ");
                    where = true;
                }

                sqlString.append(columnName);
                sqlString.append("<?");
            }

            if (cursor.isNullIgnored()) {
                if (where) {
                    sqlString.append(" AND ");
                } else {
                    sqlString.append(" WHERE ");
                    where = true;
                }

                sqlString.append(getValueColumn());
                sqlString.append(" IS NOT NULL");
            }

            if (!(isSnapshot() || cursor.isCount())) {
                sqlString.append(" ORDER BY ");

                if (!(cursor.isPull() || cursor.isIncludeDeleted())) {
                    sqlString.append(getPointColumn());
                    sqlString.append(",");
                }

                sqlString.append(columnName);

                if (cursor.isReverse()) {
                    sqlString.append(" DESC");
                }
            }

            getThisLogger()
                .trace(
                    StoreMessages.SQL_FOR,
                    Integer.toHexString(cursor.getType()),
                    sqlString);

            return sqlString;
        }

        /** {@inheritDoc}
         */
        @Override
        public final String getTableName()
        {
            return _tableName;
        }

        /** {@inheritDoc}
         */
        @Override
        public StringBuilder getUpdateSQL()
        {
            final StringBuilder sqlString = new StringBuilder("UPDATE ");

            sqlString.append(getCatalogSchemaTable());
            sqlString.append(" SET ");

            if (isSnapshot()) {
                sqlString.append(getStampColumn());
                sqlString.append("=?,");
            }

            sqlString.append(getVersionColumn());
            sqlString.append("=?,");
            sqlString.append(getStateColumn());
            sqlString.append("=?,");
            sqlString.append(getValueColumn());
            sqlString.append("=? WHERE ");
            sqlString.append(getPointColumn());
            sqlString.append("=?");

            if (!isSnapshot()) {
                sqlString.append(" AND ");
                sqlString.append(getStampColumn());
                sqlString.append("=?");
            }

            getThisLogger().trace(StoreMessages.SQL, sqlString);

            return sqlString;
        }

        /** {@inheritDoc}
         */
        @Override
        public void onFirstConnection(final TheStoreConnection connection) {}

        /** {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public final Class<? extends Driver> registerDriver(
                ClassDef driverClassDef)
        {
            if (driverClassDef == null) {
                driverClassDef = getDefaultDriverClassDef().orElse(null);
            }

            Class<?> driverClass = null;

            if (driverClassDef != null) {
                driverClass = driverClassDef.getInstanceClass();
            }

            try {
                return (Class<? extends Driver>) driverClass;
            } catch (final ClassCastException exception) {
                getThisLogger()
                    .warn(StoreMessages.BAD_DRIVER_CLASS, driverClass);

                return null;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public int setDeleteStatement(
                final PreparedStatement statement,
                final byte[] point,
                final long startStamp,
                final long finishStamp)
            throws SQLException
        {
            int index = 0;

            statement.setBytes(++index, point);

            if (!_snapshot) {
                statement.setLong(++index, startStamp);

                if (startStamp < finishStamp) {
                    statement.setLong(++index, finishStamp);
                }
            }

            return index;
        }

        /** {@inheritDoc}
         */
        @Override
        public int setInsertStatement(
                final PreparedStatement statement,
                final byte[] point,
                final long stamp,
                final long version,
                final byte[] state,
                final byte[] value)
            throws SQLException
        {
            int index = 0;

            statement.setBytes(++index, point);
            statement.setLong(++index, stamp);
            statement.setLong(++index, version);
            statement.setBytes(++index, state);
            statement.setBytes(++index, value);

            return index;
        }

        /** {@inheritDoc}
         */
        @Override
        public int setSelectStatement(
                final PreparedStatement statement,
                final StoreCursor cursor)
            throws SQLException
        {
            final int limit = cursor.getLimit();
            int index = 0;

            Require.success(limit > 0);
            statement.setMaxRows(limit);
            statement.setFetchSize(limit);

            if (cursor.getPointUUID().isPresent()) {
                statement
                    .setBytes(++index, cursor.getPointUUID().get().toBytes());

                if (cursor.isIncludeDeleted()) {
                    statement
                        .setBytes(
                            ++index,
                            cursor.getPointUUID().get().deleted().toBytes());
                }
            }

            if (cursor.getAfter().isPresent()) {
                statement.setLong(++index, cursor.getAfter().get().toRaw());
            }

            if (cursor.getBefore().isPresent()) {
                statement.setLong(++index, cursor.getBefore().get().toRaw());
            }

            return index;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(
                final StoreServiceAppImpl storeAppImpl,
                final File storeDataDir,
                final String storeEntityName)
        {
            final KeyedGroups serverProperties;

            if (storeAppImpl != null) {
                serverProperties = storeAppImpl.getServerProperties();
            } else {
                serverProperties = new KeyedGroups();
                serverProperties.freeze();
            }

            _snapshot = serverProperties
                .getBoolean(TheStoreServiceAppImpl.SNAPSHOT_PROPERTY);
            _pullDisabled = (_snapshot
                    || (storeAppImpl != null))? storeAppImpl
                        .isPullDisabled(): true;

            _catalogName = serverProperties
                .getString(
                    CATALOG_PROPERTY,
                    Optional.ofNullable(getDefaultCatalogName()))
                .orElse(null);
            _schemaName = serverProperties
                .getString(SCHEMA_PROPERTY, Optional.of(getDefaultSchemaName()))
                .get();
            _tableName = serverProperties
                .getString(
                    isSnapshot()
                    ? SNAPSHOT_TABLE_PROPERTY: ARCHIVE_TABLE_PROPERTY)
                .orElse(null);

            if (_tableName == null) {
                _tableName = serverProperties
                    .getString(
                        TABLE_PROPERTY,
                        Optional.of(getDefaultTableName()))
                    .get();
            }

            _pointColumn = serverProperties
                .getString(
                    POINT_COLUMN_PROPERTY,
                    Optional.of(DEFAULT_POINT_COLUMN))
                .get();
            _stampColumn = serverProperties
                .getString(
                    STAMP_COLUMN_PROPERTY,
                    Optional.of(DEFAULT_STAMP_COLUMN))
                .get();
            _stateColumn = serverProperties
                .getString(
                    STATE_COLUMN_PROPERTY,
                    Optional.of(DEFAULT_STATE_COLUMN))
                .get();
            _valueColumn = serverProperties
                .getString(
                    VALUE_COLUMN_PROPERTY,
                    Optional.of(DEFAULT_VALUE_COLUMN))
                .get();
            _versionColumn = serverProperties
                .getString(
                    VERSION_COLUMN_PROPERTY,
                    Optional.of(DEFAULT_VERSION_COLUMN))
                .get();

            final StringBuilder stringBuilder = new StringBuilder();

            if ((_catalogName != null) && (_catalogName.length() > 0)) {
                stringBuilder.append(_catalogName);
                stringBuilder.append('.');
            }

            if (_schemaName.length() > 0) {
                stringBuilder.append(_schemaName);
                stringBuilder.append('.');
            }

            stringBuilder.append(_tableName);
            _catalogSchemaTable = stringBuilder.toString();

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public int setUpdateStatement(
                final PreparedStatement statement,
                final byte[] point,
                final long stamp,
                final long version,
                final byte[] state,
                final byte[] value)
            throws SQLException
        {
            int index = 0;

            if (isSnapshot()) {
                statement.setLong(++index, stamp);
            }

            statement.setLong(++index, version);
            statement.setBytes(++index, state);
            statement.setBytes(++index, value);
            statement.setBytes(++index, point);

            if (!isSnapshot()) {
                statement.setLong(++index, stamp);
            }

            return index;
        }

        /** {@inheritDoc}
         */
        @Override
        public void tearDown()
        {
            synchronized (_properties) {
                final Iterable<String> keys = new ArrayList<String>(
                    _properties.keySet());

                for (final String key: keys) {
                    final String value = _properties.get(key);

                    if (value != null) {
                        System.setProperty(key, value);
                    } else if (System.getProperty(key) != null) {
                        System.clearProperty(key);
                    }

                    _properties.remove(key);
                }
            }
        }

        /**
         * Sets a system property.
         *
         * @param key The property key.
         * @param value The property value.
         */
        protected static void setSystemProperty(
                @Nonnull final String key,
                @Nonnull final String value)
        {
            final String previousValue = System.setProperty(key, value);

            synchronized (_properties) {
                if (!_properties.containsKey(key)) {
                    _properties.put(key, previousValue);
                }
            }
        }

        /**
         * Gets the catalog schema table name.
         *
         * @return The catalog schema table name.
         */
        @Nonnull
        @CheckReturnValue
        protected final String getCatalogSchemaTable()
        {
            return _catalogSchemaTable;
        }

        /**
         * Gets the default catalog name.
         *
         * @return The default catalog name.
         */
        protected String getDefaultCatalogName()
        {
            return DEFAULT_CATALOG_NAME;
        }

        /**
         * Gets the default driver class definition.
         *
         * @return The optional default driver class definition.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<ClassDefImpl> getDefaultDriverClassDef()
        {
            return Optional.empty();
        }

        /**
         * Gets the default schema name.
         *
         * @return The default schema name.
         */
        protected String getDefaultSchemaName()
        {
            return DEFAULT_SCHEMA_NAME;
        }

        /**
         * Gets the default table name.
         *
         * @return The default table name.
         */
        @Nonnull
        @CheckReturnValue
        protected String getDefaultTableName()
        {
            return isSnapshot()
                   ? DEFAULT_SNAPSHOT_TABLE_NAME: DEFAULT_ARCHIVE_TABLE_NAME;
        }

        /**
         * Gets the point column name.
         *
         * @return The point column name.
         */
        @Nonnull
        @CheckReturnValue
        protected final String getPointColumn()
        {
            return _pointColumn;
        }

        /**
         * Gets the stamp column name.
         *
         * @return The stamp column name.
         */
        @Nonnull
        @CheckReturnValue
        protected final String getStampColumn()
        {
            return _stampColumn;
        }

        /**
         * Gets the state column name.
         *
         * @return The state column name.
         */
        @Nonnull
        @CheckReturnValue
        protected final String getStateColumn()
        {
            return _stateColumn;
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        protected final Logger getThisLogger()
        {
            return Logger.getInstance(getClass());
        }

        /**
         * Gets the value column name.
         *
         * @return The value column name.
         */
        @Nonnull
        @CheckReturnValue
        protected final String getValueColumn()
        {
            return _valueColumn;
        }

        /**
         * Gets the version column name.
         *
         * @return The version column name.
         */
        @Nonnull
        @CheckReturnValue
        protected final String getVersionColumn()
        {
            return _versionColumn;
        }

        /**
         * Asks if pull is disabled.
         *
         * @return True if pull is disabled.
         */
        @CheckReturnValue
        protected final boolean isPullDisabled()
        {
            return _pullDisabled;
        }

        /**
         * Asks for the snapshot indicator.
         *
         * @return The snapshot indicator.
         */
        @CheckReturnValue
        protected final boolean isSnapshot()
        {
            return _snapshot;
        }

        private static final Map<String, String> _properties = new HashMap<>();

        private String _catalogName;
        private String _catalogSchemaTable;
        private final String _dialectName;
        private String _pointColumn;
        private boolean _pullDisabled;
        private String _schemaName;
        private boolean _snapshot;
        private String _stampColumn;
        private String _stateColumn;
        private String _tableName;
        private String _valueColumn;
        private String _versionColumn;
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
