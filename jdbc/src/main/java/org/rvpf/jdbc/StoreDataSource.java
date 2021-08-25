/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreDataSource.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.jdbc;

import java.io.PrintWriter;
import java.io.Serializable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import javax.sql.DataSource;

/**
 * Store DataSource.
 */
public final class StoreDataSource
    implements DataSource, Serializable
{
    /** {@inheritDoc}
     */
    @Override
    public synchronized Connection getConnection()
        throws SQLException
    {
        return getConnection(_user, _password);
    }

    /**
     * Attempts to establish a connection with the data source.
     *
     * @param user The database user name.
     * @param password The database user password.
     *
     * @return A connection to the data source.
     *
     * @throws SQLException On failure.
     */
    @Nonnull
    @CheckReturnValue
    public Connection getConnection(
            final String user,
            final char[] password)
        throws SQLException
    {
        final Properties properties = new Properties();

        if (user != null) {
            properties.put(StoreDriver.USER_PROPERTY, user);
        }

        if (password != null) {
            properties.put(StoreDriver.PASSWORD_PROPERTY, password);
        }

        return new StoreDriver().connect(getURLString(), properties);
    }

    /** {@inheritDoc}
     */
    @Override
    public Connection getConnection(
            final String user,
            final String password)
        throws SQLException
    {
        return getConnection(
            user,
            (password != null)? password.toCharArray(): null);
    }

    /**
     * Gets the database.
     *
     * @return The database.
     */
    public synchronized String getDatabase()
    {
        return _database;
    }

    /**
     * Gets the description.
     *
     * @return The description.
     */
    public synchronized String getDescription()
    {
        return _description;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized PrintWriter getLogWriter()
        throws SQLException
    {
        return _logWriter;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized int getLoginTimeout()
        throws SQLException
    {
        return _loginTimeout;
    }

    /** {@inheritDoc}
     */
    @Override
    public Logger getParentLogger()
        throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the port.
     *
     * @return The port.
     */
    public synchronized int getPort()
    {
        return _port;
    }

    /**
     * Gets the server.
     *
     * @return The server.
     */
    public synchronized String getServer()
    {
        return _server;
    }

    /**
     * Gets the URL string.
     *
     * @return The URL string.
     */
    public synchronized String getURLString()
    {
        if (_urlString == null) {
            final StringBuilder builder = new StringBuilder(
                StoreDriver.URL_PREFIX);

            if (_port > 0) {
                builder.append("rmi://");
                builder.append(_server);
                builder.append(":");
                builder.append(_port);
                builder.append("/");
            }

            builder.append(_database);
            _urlString = builder.toString();
        }

        return _urlString;
    }

    /**
     * Gets the user.
     *
     * @return The user.
     */
    public synchronized String getUser()
    {
        return _user;
    }

    /** {@inheritDoc}
     */

    @Override
    public boolean isWrapperFor(final Class<?> iface)
        throws SQLException
    {
        return false;
    }

    /**
     * Sets the database.
     *
     * @param database The database.
     */
    public synchronized void setDatabase(final String database)
    {
        _database = database;

        if (_database == null) {
            _database = _DEFAULT_DATABASE;
        }

        _urlString = null;
    }

    /**
     * Sets the description.
     *
     * @param description The description.
     */
    public synchronized void setDescription(final String description)
    {
        _description = description;

        if (_description == null) {
            _description = _DEFAULT_DESCRIPTION;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void setLogWriter(
            final PrintWriter logWriter)
        throws SQLException
    {
        _logWriter = logWriter;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void setLoginTimeout(
            final int loginTimeout)
        throws SQLException
    {
        _loginTimeout = loginTimeout;
    }

    /**
     * Sets the password.
     *
     * @param password The password.
     */
    public synchronized void setPassword(final char[] password)
    {
        _password = password;
    }

    /**
     * Sets the port.
     *
     * @param port The port.
     */
    public synchronized void setPort(final int port)
    {
        _port = port;
        _urlString = null;
    }

    /**
     * Sets the server.
     *
     * @param server The server.
     */
    public synchronized void setServer(final String server)
    {
        _server = server;

        if (_server == null) {
            _server = _DEFAULT_SERVER;
        }

        _urlString = null;
    }

    /**
     * Sets the URL string.
     *
     * @param urlString The URL string.
     */
    public synchronized void setURLString(final String urlString)
    {
        _urlString = urlString;
    }

    /**
     * Sets the user.
     *
     * @param user The user.
     */
    public synchronized void setUser(final String user)
    {
        _user = user;
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T unwrap(final Class<T> iface)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    private static final String _DEFAULT_DATABASE = "Store";
    private static final String _DEFAULT_DESCRIPTION = "RVPF-Store DataSource";
    private static final String _DEFAULT_SERVER = "localhost";
    private static final long serialVersionUID = 1L;

    private String _database = _DEFAULT_DATABASE;
    private String _description = _DEFAULT_DESCRIPTION;
    private PrintWriter _logWriter;
    private int _loginTimeout;
    private char[] _password;
    private int _port;
    private String _server = _DEFAULT_SERVER;
    private String _urlString;
    private String _user;
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
