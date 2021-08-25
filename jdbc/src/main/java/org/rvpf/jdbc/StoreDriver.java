/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreDriver.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

/**
 * Store Driver.
 */
public final class StoreDriver
    implements Driver
{
    /** {@inheritDoc}
     */
    @Override
    public boolean acceptsURL(final String url)
        throws SQLException
    {
        return (url != null) && url.startsWith(URL_PREFIX);
    }

    /** {@inheritDoc}
     */
    @Override
    public Connection connect(
            final String url,
            final Properties info)
        throws SQLException
    {
        if (!acceptsURL(url)) {
            return null;
        }

        return new StoreConnection(
            this,
            url.substring(URL_PREFIX.length()),
            (info != null)? info: new Properties());
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMajorVersion()
    {
        return _DRIVER_MAJOR_VERSION;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getMinorVersion()
    {
        return _DRIVER_MINOR_VERSION;
    }

    /** {@inheritDoc}
     */
    @Override
    public Logger getParentLogger()
        throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException();
    }

    /** {@inheritDoc}
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(
            final String url,
            final Properties info)
        throws SQLException
    {
        final List<DriverPropertyInfo> list =
            new LinkedList<DriverPropertyInfo>();
        DriverPropertyInfo property;

        property = new DriverPropertyInfo(
            USER_PROPERTY,
            info.getProperty(USER_PROPERTY, null));
        property.description = "User";
        property.required = true;
        list.add(property);

        property = new DriverPropertyInfo(
            PASSWORD_PROPERTY,
            info.getProperty(PASSWORD_PROPERTY, null));
        property.description = "Password";
        property.required = true;
        list.add(property);

        property = new DriverPropertyInfo(
            AUTO_COMMIT_LIMIT_PROPERTY,
            info.getProperty(AUTO_COMMIT_LIMIT_PROPERTY, null));
        property.description = "Auto commit limit";
        property.required = false;
        list.add(property);

        return list.toArray(new DriverPropertyInfo[list.size()]);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean jdbcCompliant()
    {
        return false;
    }

    /**
     * Gets this driver's name.
     *
     * @return This driver's name.
     */
    @Nonnull
    @CheckReturnValue
    static String getDriverName()
    {
        return _DRIVER_NAME + " (" + new JDBCVersion().getImplementationTitle()
               + ")";
    }

    /**
     * Gets the table instance.
     *
     * @param tableName A name.
     *
     * @return The table instance.
     *
     * @throws SQLException When there are no table for the name.
     */
    @Nonnull
    @CheckReturnValue
    static String getTableName(String tableName)
        throws SQLException
    {
        tableName = tableName.trim();

        if (ARCHIVE_TABLE.equalsIgnoreCase(tableName)) {
            return ARCHIVE_TABLE;
        }

        if (POINTS_TABLE.equalsIgnoreCase(tableName)) {
            return POINTS_TABLE;
        }

        throw JDBCMessages.UNKNOWN_TABLE.exception(tableName);
    }

    /**
     * Gets this driver's version.
     *
     * @return This driver's version.
     */
    @Nonnull
    @CheckReturnValue
    static String getVersion()
    {
        return _DRIVER_MAJOR_VERSION + "." + _DRIVER_MINOR_VERSION + " ("
               + new JDBCVersion().getImplementationVersion() + ")";
    }

    /** Archive table. */
    public static final String ARCHIVE_TABLE = "ARCHIVE";

    /** Auto commit limit property. */
    public static final String AUTO_COMMIT_LIMIT_PROPERTY =
        "rvpf.auto-commit.limit";

    /** Password property. */
    public static final String PASSWORD_PROPERTY = "password";

    /** Points table. */
    public static final String POINTS_TABLE = "POINTS";

    /** URL prefix. */
    public static final String URL_PREFIX = "jdbc:rvpf:";

    /** User property. */
    public static final String USER_PROPERTY = "user";
    private static final int _DRIVER_MAJOR_VERSION = 0;
    private static final int _DRIVER_MINOR_VERSION = 8;
    private static final String _DRIVER_NAME = "RVPF-Store";

    static {
        try {
            DriverManager.registerDriver(new StoreDriver());
        } catch (final SQLException exception) {
            throw new RuntimeException(exception);
        }
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
