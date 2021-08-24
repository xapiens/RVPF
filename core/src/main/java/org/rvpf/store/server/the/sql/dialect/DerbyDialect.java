/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DerbyDialect.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.sql.dialect;

import java.io.File;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.the.sql.SQLBackEnd;

/**
 * Derby database dialect support.
 */
public final class DerbyDialect
    extends DialectSupport.Abstract
{
    /**
     * Constructs an instance.
     */
    public DerbyDialect()
    {
        super(DIALECT_NAME);
    }

    /** {@inheritDoc}
     */
    @Override
    public List<String> getCreateTableSQL()
    {
        final List<String> sql = new ArrayList<>();
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("CREATE TABLE ");
        stringBuilder.append(getCatalogSchemaTable());
        stringBuilder.append(" (");
        stringBuilder.append(getPointColumn());
        stringBuilder.append(" CHAR(16) FOR BIT DATA NOT NULL, ");
        stringBuilder.append(getStampColumn());
        stringBuilder.append(" BIGINT NOT NULL, ");
        stringBuilder.append(getVersionColumn());
        stringBuilder.append(" BIGINT NOT NULL, ");
        stringBuilder.append(getStateColumn());
        stringBuilder.append(" LONG VARCHAR FOR BIT DATA, ");
        stringBuilder.append(getValueColumn());
        stringBuilder.append(" LONG VARCHAR FOR BIT DATA,");
        stringBuilder.append(" PRIMARY KEY (");
        stringBuilder.append(getPointColumn());

        if (!isSnapshot()) {
            stringBuilder.append(",");
            stringBuilder.append(getStampColumn());
        }

        stringBuilder.append("))");
        sql.add(stringBuilder.toString());

        if (!isPullDisabled()) {
            stringBuilder.setLength(0);
            stringBuilder.append("CREATE UNIQUE INDEX ");
            stringBuilder.append(getCatalogSchemaTable());
            stringBuilder.append("_");
            stringBuilder.append(getVersionColumn());
            stringBuilder.append("_IX1 ON ");
            stringBuilder.append(getCatalogSchemaTable());
            stringBuilder.append(" (");
            stringBuilder.append(getVersionColumn());
            stringBuilder.append(")");
            sql.add(stringBuilder.toString());
        }

        if (!isSnapshot()) {
            stringBuilder.setLength(0);
            stringBuilder.append("CREATE INDEX ");
            stringBuilder.append(getCatalogSchemaTable());
            stringBuilder.append("_");
            stringBuilder.append(getPointColumn());
            stringBuilder.append("_IX1 ON ");
            stringBuilder.append(getCatalogSchemaTable());
            stringBuilder.append(" (");
            stringBuilder.append(getPointColumn());
            stringBuilder.append(",");
            stringBuilder.append(getStampColumn());
            stringBuilder.append(" DESC)");
            sql.add(stringBuilder.toString());
        }

        return sql;
    }

    /** {@inheritDoc}
     */
    @Override
    public ClassDefImpl getDefaultClientDriverClassDef()
    {
        return DEFAULT_CLIENT_DRIVER_CLASS_DEF;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getDefaultConnectionOptions()
    {
        return DEFAULT_CONNECTION_OPTIONS;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean getDefaultConnectionShared()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getDefaultConnectionURL(
            final File storeDataDir,
            String storeEntityName)
    {
        try {
            Paths.get(storeEntityName);
        } catch (final InvalidPathException exception) {
            getThisLogger().warn(ServiceMessages.INVALID_PATH, storeEntityName);
            storeEntityName = DEFAULT_DATABASE_NAME;
        }

        return DEFAULT_CONNECTION_URL + storeEntityName;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getDefaultSchemaName()
    {
        return _DEFAULT_SCHEMA_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final StoreServiceAppImpl storeAppImpl,
            final File storeDataDir,
            final String storeEntityName)
    {
        if (!super.setUp(storeAppImpl, storeDataDir, storeEntityName)) {
            return false;
        }

        final KeyedGroups serverProperties;

        if (storeAppImpl != null) {
            serverProperties = storeAppImpl.getServerProperties();
        } else {
            serverProperties = new KeyedGroups();
            serverProperties.freeze();
        }

        final KeyedGroups connectionProperties = serverProperties
            .getGroup(SQLBackEnd.SQL_CONNECTION_PROPERTIES);

        _user = connectionProperties.getString(USER_PROPERTY).orElse(null);

        if (_user != null) {
            _password = connectionProperties
                .getPassword(PASSWORD_PROPERTY)
                .orElse(null);
        }

        if (System.getProperty(SYSTEM_HOME_PROPERTY) == null) {
            final Optional<String> systemHome = serverProperties
                .getString(SYSTEM_HOME_PROPERTY);
            Path systemHomePath = null;

            if (systemHome.isPresent()) {
                try {
                    systemHomePath = Paths.get(systemHome.get());
                } catch (final InvalidPathException exception) {
                    getThisLogger()
                        .warn(ServiceMessages.INVALID_PATH, systemHome.get());
                }
            }

            if (systemHomePath == null) {
                systemHomePath = Paths
                    .get(storeDataDir.getPath() + "/" + DERBY_DIR);
            }

            setSystemProperty(
                SYSTEM_HOME_PROPERTY,
                systemHomePath.toString().replace('\\', '/'));
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_driverRegistered) {
            final Driver driver = DEFAULT_DRIVER_CLASS_DEF
                .createInstance(Driver.class);

            if (driver != null) {
                final Properties properties = new Properties();

                if (_user != null) {
                    properties.setProperty(USER_PROPERTY, _user);

                    if (_password != null) {
                        properties
                            .setProperty(
                                PASSWORD_PROPERTY,
                                new String(_password));
                    }
                }

                try {
                    driver.connect(SHUTDOWN_CONNECTION_URL, properties);
                } catch (final SQLException exception) {
                    // Ignores.
                }

                try {
                    DriverManager.deregisterDriver(driver);
                } catch (final SQLException exception) {
                    throw new RuntimeException(exception);
                }
            }

            _driverRegistered = false;
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<ClassDefImpl> getDefaultDriverClassDef()
    {
        if (!_driverRegistered) {
            final boolean driverLoaded = DEFAULT_DRIVER_CLASS_DEF.isLoaded();
            final Driver driver = DEFAULT_DRIVER_CLASS_DEF
                .createInstance(Driver.class);

            if (driverLoaded) {
                try {
                    DriverManager.registerDriver(driver);
                } catch (final SQLException exception) {
                    throw new RuntimeException(exception);
                }

                _driverRegistered = true;
            }
        }

        return Optional.of(DEFAULT_DRIVER_CLASS_DEF);
    }

    /** Default connection options. */
    public static final String DEFAULT_CONNECTION_OPTIONS = ";create=true";

    /** Default connection URL. */
    public static final String DEFAULT_CONNECTION_URL = "jdbc:derby:";

    /** Default database name. */
    public static final String DEFAULT_DATABASE_NAME = "Default";

    /** Default driver class definition. */
    public static final ClassDefImpl DEFAULT_DRIVER_CLASS_DEF =
        new ClassDefImpl(
            "org.apache.derby.jdbc.EmbeddedDriver");

    /** Default client driver class definition. */
    public static final ClassDefImpl DEFAULT_CLIENT_DRIVER_CLASS_DEF =
        new ClassDefImpl(
            "org.apache.derby.jdbc.ClientDriver");

    /** Default system home. */
    public static final String DEFAULT_SYSTEM_HOME = "data/store/derby";

    /** Derby base directory name. */
    public static final String DERBY_DIR = "derby";

    /** Dialect name. */
    public static final String DIALECT_NAME = "Derby";

    /** The password for connection to the database. */
    public static final String PASSWORD_PROPERTY = "password";

    /** Shutdown connection URL. */
    public static final String SHUTDOWN_CONNECTION_URL =
        "jdbc:derby:;shutdown=true";

    /** System home property. */
    public static final String SYSTEM_HOME_PROPERTY = "derby.system.home";

    /** The user for connection to the database. */
    public static final String USER_PROPERTY = "user";

    /**  */

    private static final String _DEFAULT_SCHEMA_NAME = "APP";

    private boolean _driverRegistered;
    private char[] _password;
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
