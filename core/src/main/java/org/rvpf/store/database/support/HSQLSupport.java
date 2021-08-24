/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: HSQLSupport.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.database.support;

import java.io.File;
import java.io.PrintWriter;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;
import org.rvpf.store.server.the.sql.dialect.HSQLDialect;

/**
 * HSQL server support.
 */
public class HSQLSupport
    extends ServerSupport.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public String getConnectionURL()
    {
        return "jdbc:hsqldb:hsql://"
               + Inet.getLocalHostAddress().getHostAddress() + ":"
               + _port.toString() + "/";
    }

    /** {@inheritDoc}
     */
    @Override
    public DialectSupport getDialectSupport()
    {
        return new HSQLDialect();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(KeyedGroups supportProperties, final File dataDir)
    {
        if (!super.setUp(supportProperties, dataDir)) {
            return false;
        }

        _server = _SERVER_CLASS_DEF.createInstance(Object.class);

        if (_server == null) {
            return false;
        }

        if (!supportProperties.containsValueKey(_DATABASE_0_PROPERTY)) {
            final Optional<String> dbname0 = supportProperties
                .getString(_DBNAME_0_PROPERTY);
            final String database;

            supportProperties = supportProperties.copy();

            if (dbname0.isPresent()) {
                database = dbname0.get();
            } else {
                supportProperties.setValue(_DBNAME_0_PROPERTY, "");
                database = _DEFAULT_DATABASE;
            }

            supportProperties.setValue(_DATABASE_0_PROPERTY, database);
            supportProperties.freeze();
        } else if (!supportProperties.containsValueKey(_DBNAME_0_PROPERTY)) {
            supportProperties = supportProperties.copy();
            supportProperties.setValue(_DBNAME_0_PROPERTY, "");
            supportProperties.freeze();
        }

        final Properties properties = new Properties();
        final File baseDir = new File(dataDir, _BASE_DIR);

        for (final Map.Entry<String, String> entry:
                supportProperties.toPropertiesMap().entrySet()) {
            final String argKey = entry.getKey();
            String argValue = entry.getValue();

            if (argKey.startsWith(_DATABASE_PROPERTIES)) {
                argValue = new File(baseDir, argValue).getPath();
            }

            properties.setProperty(argKey, argValue);
        }

        final Object hsqlProperties = _PROPERTIES_CLASS_DEF
            .createInstance(Object.class);

        if (hsqlProperties == null) {
            return false;
        }

        invoke(
            _server,
            getMethod(_server, _SET_LOG_WRITER_METHOD, PrintWriter.class),
            getThisLogger().getPrintWriter(Logger.LogLevel.TRACE));

        invoke(
            hsqlProperties,
            getMethod(
                hsqlProperties,
                _ADD_PROPERTIES_METHOD,
                properties.getClass()),
            properties);

        invoke(
            _server,
            getMethod(
                _server,
                _SET_PROPERTIES_METHOD,
                hsqlProperties.getClass()),
            hsqlProperties);

        _port = (Integer) invoke(_server, getMethod(_server, _GET_PORT_METHOD))
            .get();

        getThisLogger().info(StoreMessages.SERVER_PORT, _port.toString());

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        invoke(_server, getMethod(_server, _START_METHOD));
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        invoke(_server, getMethod(_server, _STOP_METHOD));
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_server != null) {
            invoke(_server, getMethod(_server, _SHUTDOWN_METHOD));
            _server = null;
        }

        super.tearDown();
    }

    private static final String _ADD_PROPERTIES_METHOD = "addProperties";
    private static final String _BASE_DIR = "hsql";
    private static final String _DATABASE_0_PROPERTY = "server.database.0";
    private static final String _DATABASE_PROPERTIES = "server.database";
    private static final String _DBNAME_0_PROPERTY = "server.dbname.0";
    private static final String _DEFAULT_DATABASE = "default";
    private static final String _GET_PORT_METHOD = "getPort";
    private static final ClassDef _PROPERTIES_CLASS_DEF = new ClassDefImpl(
        "org.hsqldb.persist.HsqlProperties");
    private static final ClassDef _SERVER_CLASS_DEF = new ClassDefImpl(
        "org.hsqldb.server.Server");
    private static final String _SET_LOG_WRITER_METHOD = "setLogWriter";
    private static final String _SET_PROPERTIES_METHOD = "setProperties";
    private static final String _SHUTDOWN_METHOD = "shutdown";
    private static final String _START_METHOD = "start";
    private static final String _STOP_METHOD = "stop";

    private Integer _port;
    private Object _server;
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
