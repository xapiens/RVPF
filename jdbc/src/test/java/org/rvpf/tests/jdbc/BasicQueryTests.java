/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BasicQueryTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.jdbc.StoreColumn;
import org.rvpf.jdbc.StoreConnection;

import org.testng.annotations.Test;

/**
 * Basic query tests.
 */
public final class BasicQueryTests
    extends JDBCTests
{
    /**
     * Basic archive query tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public static void basicArchiveQueryTests()
        throws Exception
    {
        final StoreConnection connection = (StoreConnection) getConnection();
        final Statement statement = connection.createStatement();
        ResultSet resultSet;
        ResultSetMetaData metadata;

        resultSet = statement.executeQuery("SELECT A.* FROM ARCHIVE A");
        Require.equal(resultSet.getStatement(), statement);
        Require.equal(null, statement.getWarnings());
        metadata = resultSet.getMetaData();
        Require.success(metadata.getColumnCount() == 6);
        Require.equal(metadata.getColumnName(1), StoreColumn.POINT_NAME.name());
        Require.equal(metadata.getColumnName(2), StoreColumn.POINT_UUID.name());
        Require.equal(metadata.getColumnName(3), StoreColumn.STAMP.name());
        Require.equal(metadata.getColumnName(4), StoreColumn.VERSION.name());
        Require.equal(metadata.getColumnName(5), StoreColumn.STATE.name());
        Require.equal(metadata.getColumnName(6), StoreColumn.VALUE.name());
        resultSet.next();
        resultSet.close();

        resultSet = statement.executeQuery("SELECT COUNT(*)");
        Require.equal(resultSet.getStatement(), statement);
        Require.equal(null, statement.getWarnings());
        metadata = resultSet.getMetaData();
        Require.success(metadata.getColumnCount() == 1);
        Require.equal(metadata.getColumnName(1), StoreColumn.COUNT.name());
        Require.success(resultSet.next());
        Require.success(resultSet.getObject(1) instanceof Integer);
        resultSet.next();
        resultSet.close();

        statement.close();
        connection.close();
    }

    /**
     * Basic points query tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public static void basicPointsQueryTests()
        throws Exception
    {
        final StoreConnection connection = (StoreConnection) getConnection();
        final Statement statement = connection.createStatement();
        ResultSet resultSet;
        ResultSetMetaData metadata;

        Require.equal(statement.getConnection(), connection);

        resultSet = statement.executeQuery("SELECT FROM POINTS");
        Require.equal(resultSet.getStatement(), statement);
        Require.equal(null, statement.getWarnings());
        metadata = resultSet.getMetaData();
        Require.success(metadata.getColumnCount() == 2);
        Require.equal(metadata.getColumnName(1), StoreColumn.POINT_NAME.name());
        Require.equal(metadata.getColumnName(2), StoreColumn.POINT_UUID.name());
        Require.success(resultSet.next());
        Require.success(resultSet.getObject(1) instanceof String);
        Require.success(resultSet.getObject(2) instanceof UUID);
        resultSet.close();

        resultSet = statement.executeQuery("SELECT COUNT(*) FROM POINTS");
        Require.equal(resultSet.getStatement(), statement);
        Require.equal(null, statement.getWarnings());
        metadata = resultSet.getMetaData();
        Require.success(metadata.getColumnCount() == 1);
        Require.equal(metadata.getColumnName(1), StoreColumn.COUNT.name());
        Require.success(resultSet.next());
        Require.success(resultSet.getObject(1) instanceof Integer);
        Require.success(((Integer) resultSet.getObject(1)).intValue() > 0);
        resultSet.close();

        statement.close();
        connection.close();
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
