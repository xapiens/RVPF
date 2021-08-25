/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PreparedStatementTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.jdbc.StoreColumn;
import org.rvpf.jdbc.StoreConnection;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Prepared statement tests.
 */
public final class PreparedStatementTests
    extends JDBCTests
{
    /**
     * Query tests.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"updateTests"})
    public void queryTests()
        throws Exception
    {
        PreparedStatement statement;
        ResultSet resultSet;

        statement = _connection
            .prepareStatement(
                "SELECT WHERE Point_name='" + _NUMERIC_TEST_POINT + "'");
        resultSet = statement.executeQuery();
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(_resultSetSize(resultSet) == 1);
        resultSet.close();
        statement.close();

        statement = _connection
            .prepareStatement("SELECT WHERE Point_name=? AND Stamp>=? LIMIT ?");
        statement.setString(1, _NUMERIC_TEST_POINT);
        statement
            .setObject(2, _startMinute.after(-4 * ElapsedTime.MINUTE.toRaw()));
        statement.setInt(3, 2);
        resultSet = statement.executeQuery();
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(resultSet.next());
        Require
            .success(resultSet.getDouble(StoreColumn.VALUE.name()) == -4.0);
        Require.success(resultSet.next());
        Require
            .success(resultSet.getDouble(StoreColumn.VALUE.name()) == -3.0);
        resultSet.close();
        statement.close();
    }

    /**
     * Sets up the tests.
     *
     * @throws Exception On failure.
     */
    @BeforeTest
    public void setUp()
        throws Exception
    {
        _connection = (StoreConnection) getConnection();
        _startMinute = DateTime.now().floored(ElapsedTime.MINUTE);

        purgePointValues(_connection, _NUMERIC_TEST_POINT);
    }

    /**
     * Tears down what has been set up.
     *
     * @throws SQLException On failure.
     */
    @AfterTest(alwaysRun = true)
    public void tearDown()
        throws SQLException
    {
        purgePointValues(_connection, _NUMERIC_TEST_POINT);

        _connection.close();
    }

    /**
     * Update tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void updateTests()
        throws Exception
    {
        final long minuteRaw = ElapsedTime.MINUTE.toRaw();
        PreparedStatement statement;
        int count;

        statement = _connection
            .prepareStatement(
                "INSERT VALUES ('" + _NUMERIC_TEST_POINT + "','"
                + _startMinute.after(
                    -1 * minuteRaw) + "',NULL,'-1.0')");
        count = statement.executeUpdate();
        Require.success(count == 1);
        Require.equal(null, statement.getWarnings());
        statement.close();

        statement = _connection.prepareStatement("INSERT VALUES(?,?,NULL,?)");
        statement.setString(1, _NUMERIC_TEST_POINT);
        statement.setObject(2, _startMinute.after(-2 * minuteRaw));
        statement.setDouble(3, -2.0);
        count = statement.executeUpdate();
        Require.success(count == 1);
        Require.equal(null, statement.getWarnings());

        statement.setObject(2, _startMinute.after(-3 * minuteRaw));
        statement.setDouble(3, -3.0);
        count = statement.executeUpdate();
        Require.success(count == 1);
        Require.equal(null, statement.getWarnings());

        statement.setObject(2, _startMinute.after(-4 * minuteRaw));
        statement.setDouble(3, -4.0);
        statement.addBatch();
        statement.setObject(2, _startMinute.after(-5 * minuteRaw));
        statement.setDouble(3, -5.0);
        statement.addBatch();

        for (final int i: statement.executeBatch()) {
            Require.success(i == 1);
        }

        statement.close();
    }

    private static int _resultSetSize(
            final ResultSet resultSet)
        throws SQLException
    {
        resultSet.last();

        return resultSet.getRow();
    }

    private static final String _NUMERIC_TEST_POINT = "TESTS.NUMERIC.01";

    private StoreConnection _connection;
    private DateTime _startMinute;
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
