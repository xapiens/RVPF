/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DerbySupport.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.database.support;

import java.io.File;
import java.io.PrintWriter;

import java.util.Map;
import java.util.Optional;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.store.server.the.sql.dialect.DerbyDialect;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;

/**
 * Derby server support.
 */
public class DerbySupport
    extends ServerSupport.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public String getConnectionURL()
    {
        return "jdbc:derby://" + _host + ":" + String.valueOf(
            _portNumber) + "/";
    }

    /** {@inheritDoc}
     */
    @Override
    public DialectSupport getDialectSupport()
    {
        return new DerbyDialect();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            KeyedGroups supportProperties,
            final File databaseDataDir)
    {
        supportProperties.freeze();

        if (!super.setUp(supportProperties, databaseDataDir)) {
            return false;
        }

        if (!supportProperties.containsValueKey(_SYSTEM_HOME_PROPERTY)) {
            supportProperties = supportProperties.copy();
            supportProperties
                .setValue(_SYSTEM_HOME_PROPERTY, _DEFAULT_SYSTEM_HOME);
        }

        _host = supportProperties.getString(_HOST_PROPERTY).orElse(null);

        if (_host == null) {
            supportProperties = supportProperties.thawed();
            supportProperties.setValue(_HOST_PROPERTY, "0.0.0.0");
            _host = Inet.getLocalHostAddress().getHostAddress();
        }

        _portNumber = supportProperties
            .getInt(_PORT_NUMBER_PROPERTY, _DEFAULT_PORT_NUMBER);

        for (final Map.Entry<String, String> entry:
                supportProperties.toPropertiesMap().entrySet()) {
            final String argKey = entry.getKey();
            String argValue = entry.getValue();

            if (_SYSTEM_HOME_PROPERTY.equals(argKey)) {
                argValue = new File(databaseDataDir, argValue).getPath();
            }

            setSystemProperty(
                _SYSTEM_PROPERTY_PREFIX + argKey,
                Optional.ofNullable(argValue));
        }

        _server = _SERVER_CLASS_DEF.createInstance(Object.class);

        return _server != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        invoke(
            _server,
            getMethod(_server, _START_METHOD, PrintWriter.class),
            getThisLogger().getPrintWriter(Logger.LogLevel.INFO));
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        invoke(_server, getMethod(_server, _SHUTDOWN_METHOD));
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _server = null;

        super.tearDown();
    }

    private static final int _DEFAULT_PORT_NUMBER = 1527;
    private static final String _DEFAULT_SYSTEM_HOME = "derby";
    private static final String _HOST_PROPERTY = "drda.host";
    private static final String _PORT_NUMBER_PROPERTY = "drda.portNumber";
    private static final ClassDef _SERVER_CLASS_DEF = new ClassDefImpl(
        "org.apache.derby.drda.NetworkServerControl");
    private static final String _SHUTDOWN_METHOD = "shutdown";
    private static final String _START_METHOD = "start";
    private static final String _SYSTEM_HOME_PROPERTY = "system.home";
    private static final String _SYSTEM_PROPERTY_PREFIX = "derby.";

    private String _host;
    private int _portNumber;
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
