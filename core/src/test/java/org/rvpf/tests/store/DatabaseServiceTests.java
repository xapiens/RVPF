/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DatabaseServiceTests.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.tests.store;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Require;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.database.DatabaseService;
import org.rvpf.store.database.DatabaseServiceActivator;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;
import org.rvpf.tests.core.CoreTestsMessages;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Database server tests.
 */
public final class DatabaseServiceTests
    extends ServiceTests
{
    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUp()
        throws Exception
    {
        setProperty(_SERVER_PORT_1_PROPERTY, String.valueOf(allocateTCPPort()));
        setProperty(_SERVER_PORT_2_PROPERTY, String.valueOf(allocateTCPPort()));
        setProperty(_SERVER_PORT_3_PROPERTY, String.valueOf(allocateTCPPort()));

        _databaseServiceActivator = startService(
            DatabaseServiceActivator.class,
            Optional.empty());

        final DatabaseService databaseService =
            (DatabaseService) _databaseServiceActivator
                .getService();

        databaseService.getConfig().getClassLoader().activate();
        _dialectSupport = databaseService.getDialectSupport();
        Require
            .success(
                _dialectSupport
                    .setUp(
                            null,
                                    databaseService.getDatabaseDataDir(),
                                    "Tests"));

        getThisLogger()
            .info(
                CoreTestsMessages.DATABASE_DIALECT,
                _dialectSupport.getDialectName());

        _driver = _dialectSupport
            .getDefaultClientDriverClassDef()
            .createInstance(Driver.class);
        getThisLogger()
            .info(CoreTestsMessages.DRIVER_CLASS, _driver.getClass().getName());

        _connectionURL = databaseService.getConnectionURL();
        _connectionURL += "./Tests";
        _connectionURL += _dialectSupport.getDefaultConnectionOptions();
        getThisLogger().info(CoreTestsMessages.CONNECTION_URL, _connectionURL);
    }

    /**
     * Tests the service.
     *
     * @throws Exception On failure.
     */
    @Test
    public void shouldAllowAccessToDatabaseService()
        throws Exception
    {
        final Connection connection = _driver
            .connect(_connectionURL, new Properties());

        getThisLogger()
            .info(
                CoreTestsMessages.DRIVER_VERSION,
                connection.getMetaData().getDriverVersion());

        final DatabaseMetaData metaData = connection.getMetaData();
        String tableName = _dialectSupport.getTableName();

        if (metaData.storesUpperCaseIdentifiers()) {
            tableName = tableName.toUpperCase(Locale.ROOT);
        } else if (metaData.storesLowerCaseIdentifiers()) {
            tableName = tableName.toLowerCase(Locale.ROOT);
        }

        final Optional<String> catalogName = _dialectSupport.getCatalogName();
        final String schemaName = _dialectSupport.getSchemaName();
        final ResultSet resultSet = metaData
            .getTables(catalogName.orElse(null), schemaName, tableName, null);
        final boolean hasTable = resultSet.next();

        resultSet.close();

        if (!hasTable) {
            final Statement statement = connection.createStatement();

            for (final String sql: _dialectSupport.getCreateTableSQL()) {
                getThisLogger().trace(StoreMessages.SQL, sql);
                Require.failure(statement.execute(sql));
            }

            statement.close();
        }

        final byte[] point = UUID.generate().toBytes();
        final long stamp = DateTime.now().previousDay().toRaw();
        final byte[] state = Externalizer.externalize("TestState");
        final byte[] value = Externalizer.externalize("TestValue");

        final PreparedStatement insertStatement = connection
            .prepareStatement(_dialectSupport.getInsertSQL().toString());

        _dialectSupport
            .setInsertStatement(
                insertStatement,
                point,
                stamp,
                DateTime.now().toRaw(),
                state,
                value);
        Require.failure(insertStatement.execute());
        Require.success(insertStatement.getUpdateCount() == 1);
        insertStatement.close();

        final PreparedStatement updateStatement = connection
            .prepareStatement(_dialectSupport.getUpdateSQL().toString());

        _dialectSupport
            .setUpdateStatement(
                updateStatement,
                point,
                stamp,
                DateTime.now().toRaw(),
                null,
                null);
        Require.failure(updateStatement.execute());
        Require.success(updateStatement.getUpdateCount() == 1);
        updateStatement.close();

        final PreparedStatement deleteStatement = connection
            .prepareStatement(_dialectSupport.getDeleteSQL(false).toString());

        _dialectSupport
            .setDeleteStatement(deleteStatement, point, stamp, stamp);
        Require.failure(deleteStatement.execute());
        Require.success(deleteStatement.getUpdateCount() == 1);
        deleteStatement.close();

        connection.close();
    }

    /**
     * Tears down this.
     *
     * @throws Exception On failure.
     */
    @AfterClass
    public void tearDown()
        throws Exception
    {
        if (_databaseServiceActivator != null) {
            stopService(_databaseServiceActivator);
            _databaseServiceActivator = null;
        }
    }

    private static final String _SERVER_PORT_1_PROPERTY =
        "database.server.port.1";
    private static final String _SERVER_PORT_2_PROPERTY =
        "database.server.port.2";
    private static final String _SERVER_PORT_3_PROPERTY =
        "database.server.port.3";

    private String _connectionURL;
    private ServiceActivator _databaseServiceActivator;
    private DialectSupport _dialectSupport;
    private Driver _driver;
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
