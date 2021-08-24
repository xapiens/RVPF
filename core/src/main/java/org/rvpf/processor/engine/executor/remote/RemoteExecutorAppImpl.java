/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteExecutorAppImpl.java 4056 2019-06-04 15:21:02Z SFB $
 */

package org.rvpf.processor.engine.executor.remote;

import java.util.Optional;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.processor.ProcessorServiceAppImpl;
import org.rvpf.service.Service;
import org.rvpf.service.app.ServiceAppImpl;

/**
 * Remote executor application implementation.
 */
public class RemoteExecutorAppImpl
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

        final Config config = service.getConfig();

        _sessionFactory = new RemoteEngineFactoryImpl();

        if (!_sessionFactory.setUp(config)) {
            return false;
        }

        final KeyedGroups processorProperties = config
            .getPropertiesGroup(ProcessorServiceAppImpl.PROCESSOR_PROPERTIES);
        final Optional<String> name = processorProperties
            .getString(NAME_PROPERTY);

        _serverName = service
            .registerServer(
                _sessionFactory,
                name.isPresent()? name.get(): DEFAULT_NAME);

        return _serverName != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_serverName != null) {
            getService().unregisterServer(_serverName);
            _serverName = null;
        }

        if (_sessionFactory != null) {
            _sessionFactory.tearDown();
            _sessionFactory = null;
        }

        super.tearDown();
    }

    /** Default name for the server. */
    public static final String DEFAULT_NAME = "RemoteExecutor";

    /** Name for the server. */
    public static final String NAME_PROPERTY = "server.name";

    private String _serverName;
    private RemoteEngineFactoryImpl _sessionFactory;
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
