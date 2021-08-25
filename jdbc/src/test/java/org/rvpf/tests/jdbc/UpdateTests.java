/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UpdateTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.jdbc;

import java.sql.ResultSet;
import java.sql.Statement;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.tool.Require;
import org.rvpf.jdbc.StoreConnection;

import org.testng.annotations.Test;

/**
 * Update tests.
 */
public final class UpdateTests
    extends JDBCTests
{
    /**
     * INSERT tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public static void insertTests()
        throws Exception
    {
        final StoreConnection connection = (StoreConnection) getConnection();
        final Statement statement = connection.createStatement();
        final DateTime stamp = DateTime.now().floored(ElapsedTime.MINUTE);
        int count;

        count = statement
            .executeUpdate(
                "INSERT INTO Archive VALUES ('" + _NUMERIC_TEST_POINT + "','"
                + stamp + "',NULL,'1.0')");
        Require.success(count == 1);
        Require.equal(null, statement.getWarnings());

        count = statement
            .executeUpdate(
                "UPDATE Archive SET Value='2.0' WHERE Point='"
                + _NUMERIC_TEST_POINT + "' AND Stamp='" + stamp + "'");
        Require.success(count == 1);
        Require.equal(null, statement.getWarnings());

        connection.setAutoCommit(false);

        count = statement
            .executeUpdate(
                "DELETE FROM Archive WHERE Point='" + _NUMERIC_TEST_POINT
                + "' AND Stamp='" + stamp + "'");
        Require.success(count == 1);
        Require.equal(null, statement.getWarnings());

        count = statement
            .executeUpdate(
                "INSERT VALUES ('" + _NUMERIC_TEST_POINT
                + "',NOW(),NULL,'1.0')");
        Require.success(count == 1);
        Require.equal(null, statement.getWarnings());

        connection.commit();

        statement.close();
        connection.close();
    }

    /**
     * Result set update tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public static void resultSetTests()
        throws Exception
    {
        final StoreConnection connection = (StoreConnection) getConnection();
        final Statement statement = connection
            .createStatement(
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_UPDATABLE);
        final DateTime start = DateTime.now().floored(ElapsedTime.MINUTE);
        final long minuteRaw = ElapsedTime.MINUTE.toRaw();

        connection.setAutoCommit(false);
        purgePointValues(connection, _NUMERIC_TEST_POINT, _TEXT_TEST_POINT);
        statement
            .addBatch(
                "INSERT VALUES ('" + _NUMERIC_TEST_POINT + "','" + start.after(
                    -5 * minuteRaw) + "',NULL,'-5')");
        statement
            .addBatch(
                "INSERT VALUES ('" + _NUMERIC_TEST_POINT + "','" + start
                + "',NULL,'0')");
        statement
            .addBatch(
                "INSERT VALUES ('" + _NUMERIC_TEST_POINT + "','" + start.after(
                    -3 * minuteRaw) + "',NULL,'-3')");
        statement
            .addBatch(
                "INSERT VALUES ('" + _NUMERIC_TEST_POINT + "','" + start.after(
                    -2 * minuteRaw) + "',NULL,NULL)");
        statement.executeBatch();
        connection.setAutoCommit(true);

        final ResultSet resultSet = statement
            .executeQuery(
                "SELECT POINT,STAMP,STATE,VALUE FROM Archive WHERE Point_name='"
                + _NUMERIC_TEST_POINT + "' LIMIT 0");

        Require.notNull(resultSet);
        Require.equal(null, statement.getWarnings());

        Require.success(resultSet.next());
        resultSet.updateRow();

        Require.success(resultSet.next());
        resultSet.updateString("VALUE", "-2");
        resultSet.updateRow();

        resultSet.moveToInsertRow();
        resultSet.updateObject("STAMP", start.after(-1 * minuteRaw));
        resultSet.updateString("STATE", "Inserted");
        resultSet.updateString("VALUE", "-1");
        resultSet.insertRow();

        Require.success(resultSet.next());
        resultSet.updateString("STATE", "Tested");
        resultSet.updateRow();

        resultSet.moveToInsertRow();
        resultSet.updateString("POINT", _TEXT_TEST_POINT);
        resultSet.updateObject("STAMP", start.after(-4 * minuteRaw));
        resultSet.updateString("STATE", "Inserted");
        resultSet.updateString("VALUE", "Test");
        resultSet.insertRow();

        resultSet.close();
        statement.close();

        purgePointValues(connection, _NUMERIC_TEST_POINT, _TEXT_TEST_POINT);
        connection.close();
    }

    private static final String _NUMERIC_TEST_POINT = "TESTS.NUMERIC.01";
    private static final String _TEXT_TEST_POINT = "TESTS.TEXT.01";
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
