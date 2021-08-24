/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreDataSource.java 4057 2019-06-04 18:44:38Z SFB $
 */

package org.rvpf.store.server.sql;

import java.io.File;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.store.server.StoreMessages;

/**
 * Store data source.
 */
public abstract class StoreDataSource
{
    /**
     * Gets a connection.
     *
     * @return The connection.
     *
     * @throws SQLException From the actual data source.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized StoreConnection getConnection()
        throws SQLException
    {
        StoreConnection storeConnection;

        storeConnection = _storeConnection;

        if (_storeConnection == null) {
            storeConnection = newStoreConnection(
                _dataSource.getConnection(),
                _shared);
        }

        if (!_connected) {
            final DatabaseMetaData databaseMetaData = storeConnection
                .getDatabaseMetaData();

            getThisLogger()
                .info(
                    StoreMessages.USING_JDBC_DRIVER,
                    databaseMetaData.getDriverName(),
                    databaseMetaData.getDriverVersion(),
                    String.valueOf(databaseMetaData.getJDBCMajorVersion()),
                    String.valueOf(databaseMetaData.getJDBCMinorVersion()));
            getThisLogger()
                .info(
                    StoreMessages.CONNECTED_DATABASE,
                    databaseMetaData.getDatabaseProductName(),
                    databaseMetaData.getDatabaseProductVersion());

            onFirstConnection(storeConnection);

            if (_shared) {
                _storeConnection = storeConnection;
            }

            _connected = true;
        }

        return storeConnection;
    }

    /**
     * Sets up the data source.
     *
     * @param connectionProperties The connection properties.
     * @param storeDataDir The store data directory.
     * @param storeEntityName The store entity name.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public final boolean setUp(
            @Nonnull final KeyedGroups connectionProperties,
            @Nonnull final File storeDataDir,
            @Nonnull final String storeEntityName)
    {
        final String defaultConnectionOptions = getDefaultConnectionOptions();
        final String defaultConnectionURL = getDefaultConnectionURL(
            storeDataDir,
            storeEntityName) + defaultConnectionOptions;
        final String url = connectionProperties
            .getString(URL_PROPERTY, Optional.of(defaultConnectionURL))
            .get();

        if (url == null) {
            getThisLogger()
                .error(
                    BaseMessages.MISSING_PROPERTY_IN,
                    URL_PROPERTY,
                    connectionProperties.getName().orElse(null));

            return false;
        }

        final ClassDef classDef = connectionProperties
            .getClassDef(
                PooledDataSource.IMPL_PROPERTY,
                PooledDataSource.DEFAULT_IMPL);

        _dataSource = classDef.createInstance(PooledDataSource.class);

        if (_dataSource == null) {
            return false;
        }

        getThisLogger().info(StoreMessages.USING_URL, url);
        _dataSource.setUrl(url);

        final Class<? extends Driver> driverClass = registerDriver(
            connectionProperties
                .getClassDef(DRIVER_CLASS_PROPERTY, Optional.empty())
                .orElse(null));

        if (driverClass != null) {
            getThisLogger()
                .debug(StoreMessages.USING_DRIVER, driverClass.getName());
        }

        String username = connectionProperties
            .getString(USER_PROPERTY)
            .orElse(null);

        if ((username == null) || username.isEmpty()) {
            username = getDefaultConnectionUser();
        }

        _dataSource.setUsername(username);

        String password = connectionProperties
            .getString(PASSWORD_PROPERTY)
            .orElse(null);

        if ((password == null) || password.isEmpty()) {
            password = getDefaultConnectionPassword().orElse(null);
        }

        _dataSource.setPassword(password);

        _dataSource.setDefaultAutoCommit(Boolean.FALSE);

        _shared = connectionProperties
            .getBoolean(SHARED_PROPERTY, getDefaultConnectionShared());

        if (_shared) {
            getThisLogger().debug(StoreMessages.SHARED_CONNECTION);
        }

        final KeyedGroups customProperties = connectionProperties
            .getGroup(CUSTOM_PROPERTIES);

        for (final String key: customProperties.getValuesKeys()) {
            final String value = customProperties
                .getString(key, Optional.of(""))
                .get();

            _dataSource.addConnectionProperty(key, value);
            getThisLogger()
                .debug(StoreMessages.CUSTOM_DRIVER_PROPERTY, key, value);
        }

        try {
            _dataSource
                .setLogWriter(
                    Logger
                        .getInstance(_dataSource.getClass())
                        .getPrintWriter(Logger.LogLevel.DEBUG));
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    public final void tearDown()
    {
        if (_dataSource != null) {
            if (_storeConnection != null) {
                _storeConnection.tearDown();
                _storeConnection = null;
            }

            try {
                _dataSource.close();
                getThisLogger().debug(StoreMessages.DATA_SOURCE_CLOSED);
            } catch (final SQLException exception) {
                throw new RuntimeException(exception);
            }

            _dataSource = null;
            _connected = false;
        }
    }

    /**
     * Gets the default connection options.
     *
     * @return The default connection options.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract String getDefaultConnectionOptions();

    /**
     * Gets the default connection password.
     *
     * @return The optional default connection password.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Optional<String> getDefaultConnectionPassword();

    /**
     * Gets the default connection shared indicator.
     *
     * @return The default connection shared indicator.
     */
    @CheckReturnValue
    protected abstract boolean getDefaultConnectionShared();

    /**
     * Gets the default connection URL.
     *
     * @param storeDataDir The store data directory.
     * @param storeEntityName The store entity name.
     *
     * @return The default connection URL.
     */
    @Nonnull
    protected abstract String getDefaultConnectionURL(
            @Nonnull File storeDataDir,
            @Nonnull String storeEntityName);

    /**
     * Gets the default connection user.
     *
     * @return The default connection user.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract String getDefaultConnectionUser();

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        if (_logger == null) {
            _logger = Logger.getInstance(getClass());
        }

        return _logger;
    }

    /**
     * Creates a new store connection.
     *
     * @param connection The wrapped {@link Connection}.
     * @param shared Specifies if the {@link Connection} is shared.
     *
     * @return A store connection.
     */
    @Nonnull
    @CheckReturnValue
    protected StoreConnection newStoreConnection(
            @Nonnull final Connection connection,
            final boolean shared)
    {
        return new StoreConnection(connection, shared);
    }

    /**
     * Called on the first connection.
     *
     * @param storeConnection The newly established connection.
     */
    protected void onFirstConnection(
            @Nonnull final StoreConnection storeConnection)
    {
        Require.notNull(storeConnection);
    }

    /**
     * Registers the JDBC driver.
     *
     * @param driverClassDef The driver class definition.
     *
     * @return The driver class or null.
     */
    @Nullable
    @CheckReturnValue
    protected abstract Class<? extends Driver> registerDriver(
            @Nullable ClassDef driverClassDef);

    /** Custom properties. */
    public static final String CUSTOM_PROPERTIES = "properties";

    /** The JDBC driver class. */
    public static final String DRIVER_CLASS_PROPERTY = "driver.class";

    /** The password for connection to the database. */
    public static final String PASSWORD_PROPERTY = "password";

    /**
     * Indicates that a single connection will be shared. This is useful only
     * for some JDBC drivers having problems with multiple connections to the
     * same database.
     */
    public static final String SHARED_PROPERTY = "shared";

    /** The URL for connection to the database. */
    public static final String URL_PROPERTY = "url";

    /** The user for connection to the database. */
    public static final String USER_PROPERTY = "user";

    private volatile boolean _connected;
    private volatile PooledDataSource _dataSource;
    private volatile Logger _logger;
    private volatile boolean _shared;
    private volatile StoreConnection _storeConnection;
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
