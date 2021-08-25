/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BasicUpdateTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.jdbc;

import java.sql.Statement;

import org.rvpf.base.tool.Require;
import org.rvpf.jdbc.StoreConnection;

import org.testng.annotations.Test;

/**
 * Basic update tests.
 */
public final class BasicUpdateTests
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
        int count;

        count = statement
            .executeUpdate(
                "INSERT VALUES ('" + _TEST_POINT_NAME + "',NOW,NULL,'1.0')");
        Require.success(count == 1);
        Require.equal(null, statement.getWarnings());

        count = statement
            .executeUpdate(
                "DELETE ALL WHERE Point='" + _TEST_POINT_NAME + "' LIMIT 0");
        Require.success(count >= 1);
        Require.success(statement.getWarnings() == null);

        statement.close();
        connection.close();
    }

    private static final String _TEST_POINT_NAME = "TESTS.NUMERIC.01";
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
