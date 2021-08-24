/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: H2Support.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.database.support;

import java.io.File;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;
import org.rvpf.store.server.the.sql.dialect.H2Dialect;

/**
 * H2 server support.
 */
public class H2Support
    extends ServerSupport.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public String getConnectionURL()
    {
        return "jdbc:h2:" + _serverURL + "/";
    }

    /** {@inheritDoc}
     */
    @Override
    public DialectSupport getDialectSupport()
    {
        return new H2Dialect();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(KeyedGroups supportProperties, final File dataDir)
    {
        if (!super.setUp(supportProperties, dataDir)) {
            return false;
        }

        final Class<?> serverClass = _SERVER_CLASS_DEF.getInstanceClass();

        if (serverClass == null) {
            return false;
        }

        if (!supportProperties.containsValueKey(_BASE_DIR_PROPERTY)) {
            supportProperties = supportProperties.copy();
            supportProperties.setValue(_BASE_DIR_PROPERTY, _DEFAULT_BASE_DIR);
            supportProperties.freeze();
        }

        final List<String> argsList = new LinkedList<>();
        final Set<String> creates = new LinkedHashSet<>();

        for (final Map.Entry<String, String> entry:
                supportProperties.toPropertiesMap().entrySet()) {
            final String argKey = entry.getKey();
            String argValue = entry.getValue();

            if (_BASE_DIR_PROPERTY.equals(argKey)) {
                argValue = new File(dataDir, argValue).getPath();
            } else if (argKey.startsWith(_TCP_PROPERTIES)) {
                creates.add(_TCP_CREATE_METHOD);
            } else if (argKey.startsWith(_PG_PROPERTIES)) {
                creates.add(_PG_CREATE_METHOD);
            } else if (argKey.startsWith(_WEB_PROPERTIES)) {
                creates.add(_WEB_CREATE_METHOD);
            }

            argsList.add("-" + argKey);

            if ((argValue != null) && (argValue.trim().length() > 0)) {
                argsList.add(argValue);
            }
        }

        argsList.add(_IF_NOT_EXISTS_ARG);

        getThisLogger()
            .trace(StoreMessages.SERVER_ARGS, String.join(" ", argsList));

        final String[] args = argsList.toArray(new String[argsList.size()]);

        for (final String create: creates) {
            final Object server = invoke(
                getMethod(serverClass, create, String[].class),
                new Object[] {args})
                .orElse(null);

            if (server == null) {
                return false;
            }

            _servers.add(server);

            final String serverURL = (String) invoke(
                server,
                getMethod(server, _GET_URL_METHOD))
                .orElse(null);

            if (_serverURL == null) {
                _serverURL = serverURL;
            }

            getThisLogger().info(StoreMessages.SERVER_URL, serverURL);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        for (final Object server: _servers) {
            invoke(server, getMethod(server, _START_METHOD));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void stop()
    {
        for (final Object server: _servers) {
            invoke(server, getMethod(server, _STOP_METHOD));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _servers.clear();

        super.tearDown();
    }

    private static final String _BASE_DIR_PROPERTY = "baseDir";
    private static final String _DEFAULT_BASE_DIR = "h2";
    private static final String _GET_URL_METHOD = "getURL";
    private static final String _IF_NOT_EXISTS_ARG = "-ifNotExists";
    private static final String _PG_CREATE_METHOD = "createPgServer";
    private static final String _PG_PROPERTIES = "pg";
    private static final ClassDef _SERVER_CLASS_DEF = new ClassDefImpl(
        "org.h2.tools.Server");
    private static final String _START_METHOD = "start";
    private static final String _STOP_METHOD = "stop";
    private static final String _TCP_CREATE_METHOD = "createTcpServer";
    private static final String _TCP_PROPERTIES = "tcp";
    private static final String _WEB_CREATE_METHOD = "createWebServer";
    private static final String _WEB_PROPERTIES = "web";

    private String _serverURL;
    private final List<Object> _servers = new ArrayList<>(3);
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
