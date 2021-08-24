/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DatabaseServiceAppImpl.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.database;

import java.io.File;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.app.ServiceAppImpl;
import org.rvpf.store.database.support.ServerSupport;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;

/**
 * Database service application implementation.
 */
public final class DatabaseServiceAppImpl
    extends ServiceAppImpl
{
    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Service service)
    {
        if (!super.setUp(service)) {
            return false;
        }

        _serverProperties = service
            .getConfig()
            .getPropertiesGroup(SERVER_PROPERTIES);

        if (_serverProperties.isMissing()) {
            getThisLogger()
                .error(ServiceMessages.PROPERTIES_NOT_FOUND, SERVER_PROPERTIES);

            return false;
        }

        final Optional<ClassDef> supportClass = _serverProperties
            .getClassDef(SUPPORT_CLASS_PROPERTY, Optional.empty());

        if (!supportClass.isPresent()) {
            getThisLogger()
                .error(BaseMessages.MISSING_PROPERTY, SUPPORT_CLASS_PROPERTY);

            return false;
        }

        getThisLogger()
            .info(
                StoreMessages.SERVER_SUPPORT,
                supportClass.get().getClassName());

        final KeyedGroups supportProperties = _serverProperties
            .getGroup(SUPPORT_PROPERTIES);

        if (supportProperties.isMissing()) {
            getThisLogger()
                .error(
                    ServiceMessages.PROPERTIES_NOT_FOUND,
                    SUPPORT_PROPERTIES);

            return false;
        }

        _serverSupport = supportClass.get().createInstance(ServerSupport.class);

        if (_serverSupport == null) {
            return false;
        }

        try {
            return _serverSupport
                .setUp(supportProperties, getDatabaseDataDir());
        } catch (final Exception exception) {
            getThisLogger().error(exception, ServiceMessages.SET_UP_FAILED);

            return false;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        _serverSupport.start();
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        _serverSupport.stop();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_serverSupport != null) {
            try {
                _serverSupport.tearDown();
            } catch (final Exception exception) {
                getThisLogger()
                    .error(exception, ServiceMessages.TEAR_DOWN_FAILED);
            }

            _serverSupport = null;
        }

        super.tearDown();
    }

    /**
     * Gets the connection URL.
     *
     * @return The connection URL.
     */
    @Nonnull
    @CheckReturnValue
    String getConnectionURL()
    {
        return _serverSupport.getConnectionURL();
    }

    /**
     * Gets the database data directory.
     *
     * @return The database data directory.
     */
    @Nonnull
    @CheckReturnValue
    File getDatabaseDataDir()
    {
        if (_databaseDataDir == null) {
            _databaseDataDir = Config
                .dataDir(
                    Optional.of(getService().getDataDir()),
                    _serverProperties,
                    DATABASE_DATA_DIR_PROPERTY,
                    DEFAULT_DATABASE_DATA_DIR);
            getThisLogger()
                .debug(
                    StoreMessages.DATABASE_DATA_DIR,
                    _databaseDataDir.getAbsolutePath());
        }

        return _databaseDataDir;
    }

    /**
     * Gets a dialect support object.
     *
     * <p>Used by tests procedures.</p>
     *
     * @return The dialect support object.
     */
    @Nonnull
    @CheckReturnValue
    DialectSupport getDialectSupport()
    {
        return _serverSupport.getDialectSupport();
    }

    /** Specifies the database directory path. */
    public static final String DATABASE_DATA_DIR_PROPERTY = "data.dir";

    /** Default database directory path. */
    public static final String DEFAULT_DATABASE_DATA_DIR = "database";

    /** Server properties. */
    public static final String SERVER_PROPERTIES = "database.server";

    /** Support class property. */
    public static final String SUPPORT_CLASS_PROPERTY = "support.class";

    /** Support properties. */
    public static final String SUPPORT_PROPERTIES = "support.properties";

    private volatile File _databaseDataDir;
    private KeyedGroups _serverProperties;
    private volatile ServerSupport _serverSupport;
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
