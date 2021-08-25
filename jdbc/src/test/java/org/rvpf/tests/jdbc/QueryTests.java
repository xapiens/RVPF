/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueryTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.jdbc.JDBCMessages;
import org.rvpf.jdbc.StoreColumn;
import org.rvpf.jdbc.StoreConnection;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Query tests.
 */
public final class QueryTests
    extends JDBCTests
{
    /**
     * Archive query tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void archiveQueryTests()
        throws Exception
    {
        final Statement statement = _connection.createStatement();
        ResultSet resultSet;

        resultSet = statement
            .executeQuery(
                "SELECT * FROM Archive WHERE Point_name='"
                + _NUMERIC_TEST_POINT + "'");
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(_resultSetSize(resultSet) == 1);
        resultSet.close();

        resultSet = statement
            .executeQuery(
                "SELECT * FROM Archive WHERE Point_name='"
                + _NUMERIC_TEST_POINT + "' LIMIT 0");
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(_resultSetSize(resultSet) == 4);
        resultSet.close();

        resultSet = statement
            .executeQuery(
                "SELECT * FROM Archive WHERE Point_name='"
                + _NUMERIC_TEST_POINT + "' AND Value IS NOT NULL LIMIT 0");
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(_resultSetSize(resultSet) == 3);
        resultSet.close();

        resultSet = statement
            .executeQuery(
                "SELECT * FROM Archive WHERE Point_name='" + _TEXT_TEST_POINT
                + "'");
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(_resultSetSize(resultSet) == 1);
        resultSet.close();

        resultSet = statement
            .executeQuery(
                "SELECT * FROM Archive WHERE Point_name='" + _TEXT_TEST_POINT
                + "' LIMIT 0");
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(resultSet.isBeforeFirst());
        Require.failure(resultSet.isFirst());
        Require.failure(resultSet.isLast());
        Require.failure(resultSet.isAfterLast());

        Require.success(resultSet.first());
        Require.failure(resultSet.isBeforeFirst());
        Require.success(resultSet.isFirst());
        Require.failure(resultSet.isLast());
        Require.failure(resultSet.isAfterLast());

        Require.success(resultSet.next());
        Require.failure(resultSet.isBeforeFirst());
        Require.failure(resultSet.isFirst());
        Require.failure(resultSet.isLast());
        Require.failure(resultSet.isAfterLast());

        Require.success(resultSet.last());
        Require.failure(resultSet.isBeforeFirst());
        Require.failure(resultSet.isFirst());
        Require.success(resultSet.isLast());
        Require.failure(resultSet.isAfterLast());

        resultSet.afterLast();
        Require.failure(resultSet.isBeforeFirst());
        Require.failure(resultSet.isFirst());
        Require.failure(resultSet.isLast());
        Require.success(resultSet.isAfterLast());

        Require.failure(resultSet.next());
        resultSet.close();

        resultSet = statement
            .executeQuery(
                "SELECT ALL * FROM Archive WHERE Point_name='"
                + _TEXT_TEST_POINT + "' LIMIT 0");
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(_resultSetSize(resultSet) == 4);
        resultSet.close();

        boolean exceptionCatched = false;

        try {
            Require
                .notNull(
                    statement
                        .executeQuery(
                                "SELECT * FROM Archive WHERE Point_name='"
                                + _UNKNOWN_TEST_POINT + "'"));
        } catch (final SQLException sqlException) {
            Require
                .success(
                    sqlException.getErrorCode()
                    == -JDBCMessages.UNKNOWN_POINT.ordinal());
            exceptionCatched = true;
        }

        Require.success(exceptionCatched);

        statement.close();
    }

    /**
     * Points query tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void pointsQueryTests()
        throws Exception
    {
        final Statement statement = _connection.createStatement();
        ResultSet resultSet;

        resultSet = statement
            .executeQuery(
                "SELECT P.Point_name AS Name, P.Point_UUID AS UUID"
                + " FROM Points AS P WHERE P.Point_name='"
                + _NUMERIC_TEST_POINT + "'");
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(resultSet.next());
        getThisLogger()
            .debug(
                JDBCTestsMessages.POINT,
                resultSet.getString("NAME"),
                resultSet.getString("UUID"));
        resultSet.close();

        resultSet = statement
            .executeQuery(
                "SELECT * FROM Points WHERE Point LIKE 'TESTS.NUMERIC.*'");
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());

        while (resultSet.next()) {
            getThisLogger()
                .debug(
                    JDBCTestsMessages.POINT,
                    resultSet.getString(StoreColumn.POINT_NAME.name()),
                    resultSet.getString(StoreColumn.POINT_UUID.name()));
        }

        resultSet.close();

        statement.close();
    }

    /**
     * Inter/extra-polation tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void polationTests()
        throws Exception
    {
        final Statement statement = _connection.createStatement();
        final ResultSet resultSet;

        resultSet = statement
            .executeQuery(
                "SELECT *,INTERPOLATED,EXTRAPOLATED WHERE Point_name='"
                + _NUMERIC_TEST_POINT + "' AND Stamp>='" + _startMinute.after(
                    -5 * ElapsedTime.MINUTE.toRaw()) + "' and STAMP<='"
                    + _startMinute + "' AND ELAPSED='00:01' LIMIT 0");
        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(resultSet.next());
        Require.failure(resultSet.getBoolean(StoreColumn.INTERPOLATED.name()));
        Require.failure(resultSet.getBoolean(StoreColumn.EXTRAPOLATED.name()));
        Require.success(resultSet.next());
        Require
            .success(resultSet.getBoolean(StoreColumn.INTERPOLATED.name()));
        Require.failure(resultSet.getBoolean(StoreColumn.EXTRAPOLATED.name()));
        resultSet.close();

        statement.close();
    }

    /**
     * Archive query tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void scrollTests()
        throws Exception
    {
        final Statement statement = _connection
            .createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        final ResultSet resultSet = statement
            .executeQuery(
                "SELECT * FROM Archive WHERE Point_name='"
                + _NUMERIC_TEST_POINT + "' AND Version>='" + _start + "'");

        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());
        Require.success(resultSet.isBeforeFirst());
        Require.failure(resultSet.isFirst());
        Require.failure(resultSet.isLast());
        Require.failure(resultSet.isAfterLast());

        Require.success(resultSet.first());
        Require.failure(resultSet.isBeforeFirst());
        Require.success(resultSet.isFirst());
        Require.failure(resultSet.isLast());
        Require.failure(resultSet.isAfterLast());

        Require.success(resultSet.next());
        Require.failure(resultSet.isBeforeFirst());
        Require.failure(resultSet.isFirst());
        Require.failure(resultSet.isLast());
        Require.failure(resultSet.isAfterLast());

        Require.success(resultSet.last());
        Require.failure(resultSet.isBeforeFirst());
        Require.failure(resultSet.isFirst());
        Require.success(resultSet.isLast());
        Require.failure(resultSet.isAfterLast());

        resultSet.afterLast();
        Require.failure(resultSet.isBeforeFirst());
        Require.failure(resultSet.isFirst());
        Require.failure(resultSet.isLast());
        Require.success(resultSet.isAfterLast());

        Require.failure(resultSet.next());
        Require.success(resultSet.previous());
        Require.failure(resultSet.isBeforeFirst());
        Require.failure(resultSet.isFirst());
        Require.success(resultSet.isLast());
        Require.failure(resultSet.isAfterLast());
        Require.equal(null, resultSet.getObject("Value"));

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
        _start = DateTime.now();
        _startMinute = _start.floored(ElapsedTime.MINUTE);

        final Statement statement = _connection.createStatement();

        _connection.setAutoCommit(false);
        purgePointValues(_connection, _NUMERIC_TEST_POINT, _TEXT_TEST_POINT);
        statement
            .executeUpdate(
                "INSERT VALUES " + _values(
                    _NUMERIC_TEST_POINT,
                    -5) + "," + _values(
                            _NUMERIC_TEST_POINT,
                                    0) + "," + _values(
                                            _NUMERIC_TEST_POINT,
                                                    -3));
        statement
            .executeUpdate(
                "INSERT VALUES ('" + _NUMERIC_TEST_POINT + "','"
                + _startMinute.after(
                    -2 * ElapsedTime.MINUTE.toRaw()) + "',NULL,NULL)");
        statement
            .executeUpdate(
                "INSERT VALUES " + _values(
                    _TEXT_TEST_POINT,
                    -5) + "," + _values(
                            _TEXT_TEST_POINT,
                                    0) + "," + _values(_TEXT_TEST_POINT, -3));
        statement
            .executeUpdate(
                "INSERT VALUES ('" + _TEXT_TEST_POINT + "','"
                + _startMinute.after(
                    -((2 * ElapsedTime.MINUTE.toRaw())
                    + (30 * ElapsedTime.SECOND.toRaw()))) + "',NULL,'-2.5')");
        _connection.setAutoCommit(true);
        statement.close();
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
        purgePointValues(_connection, _NUMERIC_TEST_POINT, _TEXT_TEST_POINT);

        _connection.close();
    }

    private static int _resultSetSize(
            final ResultSet resultSet)
        throws SQLException
    {
        resultSet.last();

        return resultSet.getRow();
    }

    private String _values(final String pointName, final int offset)
    {
        final StringBuilder builder = new StringBuilder("('");

        builder.append(pointName);
        builder.append("','");
        builder.append(_startMinute.after(offset * ElapsedTime.MINUTE.toRaw()));
        builder.append("',");
        builder.append("NULL");
        builder.append(",'");
        builder.append(offset);
        builder.append("')");

        return builder.toString();
    }

    private static final String _NUMERIC_TEST_POINT = "TESTS.NUMERIC.01";
    private static final String _TEXT_TEST_POINT = "TESTS.TEXT.01";
    private static final String _UNKNOWN_TEST_POINT = "TESTS.UNKNOWN.01";

    private StoreConnection _connection;
    private DateTime _start;
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
