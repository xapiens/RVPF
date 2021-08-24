/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMContainerServiceAppImpl.java 4021 2019-05-24 13:01:15Z SFB $
 */

package org.rvpf.som;

import java.net.URI;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.app.ServiceAppImpl;
import org.rvpf.service.rmi.SessionSecurityContext;
import org.rvpf.som.queue.QueueServerImpl;
import org.rvpf.som.topic.TopicServerImpl;

/**
 * SOM container service application implementation.
 */
public final class SOMContainerServiceAppImpl
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

        final SessionSecurityContext securityContext = SOMServerImpl
            .createSecurityContext(
                service.getConfig().getProperties(),
                KeyedGroups.MISSING_KEYED_GROUP,
                getThisLogger());

        if (securityContext == null) {
            return false;
        }

        return _setUpQueues() && _setUpTopics();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        // Tears down each module.

        for (final SOMServerImpl somServer: _somServers.values()) {
            somServer.tearDown();
        }

        _somServers.clear();

        super.tearDown();
    }

    private boolean _setUpQueues()
    {
        final Config config = getService().getConfig();
        final KeyedGroups[] queues = config
            .getPropertiesGroups(SOM_QUEUE_PROPERTIES);

        for (final KeyedGroups queue: queues) {
            final SessionSecurityContext securityContext = SOMServerImpl
                .createSecurityContext(
                    config.getProperties(),
                    queue.getGroup(SecurityContext.SECURITY_PROPERTIES),
                    getThisLogger());

            if (securityContext == null) {
                return false;
            }

            if (!_setUpSOMServer(
                    new QueueServerImpl(Optional.of(securityContext)),
                    queue)) {
                return false;
            }
        }

        return true;
    }

    private boolean _setUpSOMServer(
            final SOMServerImpl somServer,
            final KeyedGroups somProperties)
    {
        if (!somServer.setUp(getService().getConfig(), somProperties)) {
            return false;
        }

        final URI serverURI = somServer.getURI();

        if (_somServers.containsKey(serverURI)) {
            getThisLogger()
                .error(ServiceMessages.DUPLICATE_BIND_NAME, serverURI);
            somServer.tearDown();

            return false;
        }

        _somServers.put(serverURI, somServer);

        getThisLogger()
            .debug(
                ServiceMessages.SOM_MODULE_LOADED,
                somServer.getName(),
                somServer.getClass().getName());

        return true;
    }

    private boolean _setUpTopics()
    {
        final Config config = getService().getConfig();
        final KeyedGroups[] topics = config
            .getPropertiesGroups(SOM_TOPIC_PROPERTIES);

        for (final KeyedGroups topic: topics) {
            final SessionSecurityContext securityContext = SOMServerImpl
                .createSecurityContext(
                    config.getProperties(),
                    topic.getGroup(SecurityContext.SECURITY_PROPERTIES),
                    getThisLogger());

            if (securityContext == null) {
                return false;
            }

            if (!_setUpSOMServer(
                    new TopicServerImpl(Optional.of(securityContext)),
                    topic)) {
                return false;
            }
        }

        return true;
    }

    /** Properties used to define a SOM queue. */
    public static final String SOM_QUEUE_PROPERTIES = "som.queue";

    /** Properties used to define a SOM topic. */
    public static final String SOM_TOPIC_PROPERTIES = "som.topic";

    private final Map<URI, SOMServerImpl> _somServers = new HashMap<>();
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
