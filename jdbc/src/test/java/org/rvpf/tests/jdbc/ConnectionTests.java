/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ConnectionTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.Statement;

import org.rvpf.base.tool.Require;
import org.rvpf.jdbc.StoreConnection;

import org.testng.annotations.Test;

/**
 * Connection tests.
 */
public final class ConnectionTests
    extends JDBCTests
{
    /**
     * Connection tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void connectionTests()
        throws Exception
    {
        final StoreConnection connection = (StoreConnection) getConnection();

        Require.failure(connection.isClosed(), "Connection closed)");

        final DatabaseMetaData metaData = connection.getMetaData();

        Require.equal(metaData.getConnection(), connection);

        getThisLogger()
            .info(
                JDBCTestsMessages.DRIVER_VERSION,
                metaData.getDriverName(),
                metaData.getDriverVersion(),
                Integer.valueOf(metaData.getJDBCMajorVersion()),
                Integer.valueOf(metaData.getJDBCMinorVersion()));

        connection.setAutoCommit(false);

        final Statement statement = connection.createStatement();

        Require.notNull(statement);
        Require.equal(metaData.getConnection(), connection);

        statement.close();

        connection.commit();
        connection.rollback();

        connection.close();
        Require.success(connection.isClosed());

        Require
            .notNull(DriverManager.getDriver(getDataSource().getURLString()));
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
