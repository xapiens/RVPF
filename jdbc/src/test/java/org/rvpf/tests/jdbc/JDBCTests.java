/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JDBCTests.java 4034 2019-05-28 19:57:11Z SFB $
 */

package org.rvpf.tests.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.som.QueueProxy;
import org.rvpf.jdbc.StoreDataSource;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.rmi.ServiceRegistry;
import org.rvpf.store.server.StoreServiceImpl;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * JDBC tests.
 */
public abstract class JDBCTests
    extends StoreClientTests
{
    /**
     * Constructs an instance.
     */
    JDBCTests() {}

    /**
     * Sets up the store.
     *
     * @throws Exception On failure.
     */
    @BeforeSuite
    public void setUpStores()
        throws Exception
    {
        setUpAlerter();

        _storeService = startStoreService(true);
    }

    /**
     * Tears down stores.
     *
     * @throws Exception On failure.
     */
    @AfterSuite(alwaysRun = true)
    public void tearDownStores()
        throws Exception
    {
        if (_storeService != null) {
            final QueueProxy.Receiver receiver = QueueProxy.Receiver
                .newBuilder()
                .prepare(
                    _storeService.getConfig().get().getProperties(),
                    getMetadata(_storeService)
                        .getPropertiesGroup(_NOTIFIER_QUEUE_PROPERTIES),
                    "JDBCTests",
                    Logger.getInstance(JDBCTests.class))
                .build();

            receiver.connect();
            receiver.purge();
            receiver.tearDown();

            stopService(_storeService);
            _storeService = null;
        }

        tearDownAlerter();
    }

    /**
     * Gets a connection to the store.
     *
     * @return The connection.
     *
     * @throws Exception On failure.
     */
    @Nonnull
    @CheckReturnValue
    static Connection getConnection()
        throws Exception
    {
        return getDataSource().getConnection();
    }

    /**
     * Gets a data source for the store.
     *
     * @return The data source.
     */
    @Nonnull
    @CheckReturnValue
    static StoreDataSource getDataSource()
    {
        final StoreServiceImpl storeServiceImpl =
            (StoreServiceImpl) _storeService
                .getService();
        final StoreDataSource dataSource = new StoreDataSource();

        if (!ServiceRegistry.isPrivate()) {
            dataSource.setPort(getRegistryPort());
        }

        dataSource.setDatabase(storeServiceImpl.getServerName());
        dataSource
            .setUser(
                _storeService
                    .getConfig()
                    .get()
                    .getStringValue(_USER_PROPERTY)
                    .get());
        dataSource
            .setPassword(
                _storeService
                    .getConfig()
                    .get()
                    .getPasswordValue(_PASSWORD_PROPERTY)
                    .orElse(null));

        return dataSource;
    }

    /**
     * Purges point values.
     *
     * @param connection The connection.
     * @param pointNames The point names.
     *
     * @throws SQLException On failure.
     */
    static void purgePointValues(
            @Nonnull final Connection connection,
            @Nonnull final String... pointNames)
        throws SQLException
    {
        final boolean autocommit = connection.getAutoCommit();
        final Statement statement = connection.createStatement();

        if (autocommit) {
            connection.setAutoCommit(false);
        }

        for (final String pointName: pointNames) {
            statement
                .executeUpdate(
                    "DELETE ALL WHERE Point='" + pointName + "' LIMIT 0");
        }

        connection.commit();

        statement.close();

        if (autocommit) {
            connection.setAutoCommit(true);
        }
    }

    private static final String _NOTIFIER_QUEUE_PROPERTIES =
        "tests.dispatcher.queue";
    private static final String _PASSWORD_PROPERTY = "tests.password";
    private static final String _USER_PROPERTY = "tests.user";
    private static ServiceActivator _storeService;
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
