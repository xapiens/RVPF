/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyStoreServiceAppImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.store.server.proxy;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.store.StoreSessionFactory;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.metadata.MetadataService;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreMetadataFilter;
import org.rvpf.store.server.StoreServiceAppImpl;

/**
 * Proxy store service application implementation.
 */
public class ProxyStoreServiceAppImpl
    extends StoreServiceAppImpl
{
    /** {@inheritDoc}
     */
    @Override
    public boolean areNoticesFiltered()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public ProxyStoreServer getServer()
    {
        return Require.notNull(_server);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewMetadata(final Metadata metadata)
    {
        getService().saveMonitored();

        return super.onNewMetadata(metadata);
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        stop();

        if (_server != null) {
            _server.tearDown();
            _server = null;
        }

        _tearDownPoints();

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doPendingActions()
        throws InterruptedException, ServiceNotAvailableException
    {
        if (_resetNeeded) {
            getService().resetPointsStore();
            _server.resetPointStores();
            _resetNeeded = false;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getDefaultStoreName()
    {
        return StoreSessionFactory.DEFAULT_PROXY_STORE_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean refreshMetadata()
    {
        final MetadataService service = getService();

        if (!service.reloadMetadata()) {
            return false;
        }

        service.monitorStores();
        service.restoreMonitored();
        _tearDownPoints();
        _resetNeeded = true;

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp()
    {
        if (!super.setUp(SERVER_PROPERTIES)) {
            return false;
        }

        _server = new ProxyStoreServer();

        if (!_server.setUp(this)) {
            return false;
        }

        return registerServer(
            getServerProperties().getString(NAME_PROPERTY, getEntityName()),
            _server);
    }

    /** {@inheritDoc}
     */
    @Override
    protected StoreMetadataFilter storeMetadataFilter(
            final String storeName,
            final Collection<String> partnerNames)
    {
        final KeyedGroups stores;
        final List<String> storeNames = new LinkedList<>();

        stores = getServerProperties().getGroup(STORES_PROPERTIES);

        if (!stores.isMissing() && !stores.getBoolean(ALL_PROPERTY)) {
            for (final String name: stores.getStrings(NAME_PROPERTY)) {
                storeNames.add(name);
            }

            if (storeNames.isEmpty()) {
                getThisLogger().error(StoreMessages.NO_STORE);

                return null;
            }
        }

        return new ProxyStoreMetadataFilter(storeNames);
    }

    private void _tearDownPoints()
    {
        final Metadata metadata = getMetadata();

        if (metadata != null) {
            metadata.tearDownPoints();
        }
    }

    /** Asks for all 'Store' entities. */
    public static final String ALL_PROPERTY = "all";

    /** Name of a 'Store' entity. */
    public static final String NAME_PROPERTY = "name";

    /** Store server properties. */
    public static final String SERVER_PROPERTIES = "store.server.proxy";

    /** The enumeration of proxied stores. */
    public static final String STORES_PROPERTIES = "stores";

    private boolean _resetNeeded;
    private ProxyStoreServer _server;
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
