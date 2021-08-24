/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyStoreServer.java 4053 2019-06-03 19:22:49Z SFB $
 */

package org.rvpf.store.server.proxy;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.rvpf.base.Point;
import org.rvpf.base.Points;
import org.rvpf.base.exception.PointUnknownException;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.value.PointValue;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreServer;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.archiver.Archiver;

/**
 * Proxy store server.
 */
final class ProxyStoreServer
    extends StoreServer.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<Archiver> newArchiver()
    {
        return Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean probe()
    {
        boolean probeSucceeded = true;

        for (final Store store: _pointStores) {
            try {
                probeSucceeded &= store.probe();
            } catch (final StoreAccessException exception) {
                probeSucceeded = false;
            }
        }

        return probeSucceeded;
    }

    /** {@inheritDoc}
     *
     * <p>Note: Since this server accepts queries for more than one store at a
     * time, a null response from a store is converted to a
     * {@link ServiceClosedException}.</p>
     */
    @Override
    public synchronized StoreValues[] select(
            final StoreValuesQuery[] queries,
            final Optional<Identity> identity)
    {
        final StoreValues[] responses = new StoreValues[queries.length];
        final Set<Store> stores = new LinkedHashSet<Store>();
        final Map<StoreValuesQuery, Integer> positions =
            new HashMap<StoreValuesQuery, Integer>();
        final Points points = getMetadata();

        // Splits queries by Store.

        for (int i = 0; i < queries.length; ++i) {
            StoreValuesQuery query = queries[i];

            if (query == null) {
                responses[i] = null;

                continue;
            }

            final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                .newBuilder()
                .copyFrom(query);

            if (!queryBuilder.restore(points)) {
                getThisLogger().trace(StoreMessages.QUERY_RECEIVED, query);
                responses[i] = unknownQueryPoint(query);

                continue;
            }

            query = queryBuilder.build();
            getThisLogger().trace(StoreMessages.QUERY_RECEIVED, query);
            queries[i] = query;

            if (query.isPull()) {
                responses[i] = unsupportedPullQuery();

                continue;
            }

            final Store store = query.getPoint().get().getStore().get();

            if (store.addQuery(query)) {
                stores.add(store);
                positions.put(query, Integer.valueOf(i));
            }
        }

        // Gets responses.

        final String user = identity
            .isPresent()? identity.get().getIdentifier(): null;

        for (final Store store: stores) {
            try {
                store.impersonate(Optional.of((user != null)? user: ""));

                for (;;) {
                    final Optional<StoreValues> optionalResponse = store
                        .nextValues();

                    if (!optionalResponse.isPresent()) {
                        break;
                    }

                    final StoreValues response = optionalResponse.get();
                    final Integer position = positions
                        .get(response.getQuery().get());

                    responses[position.intValue()] = response;
                }
            } catch (final StoreAccessException exception) {
                for (int i = 0; i < queries.length; ++i) {
                    final StoreValuesQuery query = queries[i];

                    if ((query != null) && (responses[i] == null)) {
                        if (query.getPoint().get().getStore().get() == store) {
                            responses[i] = new StoreValues(exception);
                        }
                    }
                }
            } catch (final InterruptedException exception) {
                throw new RuntimeException(exception);    // Should not happen.
            }
        }

        // Avoids null responses.

        for (int i = 0; i < queries.length; ++i) {
            if ((responses[i] == null) && (queries[i] != null)) {
                responses[i] = new StoreValues(queries[i]);
            }
        }

        return responses;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized boolean setUp(final StoreServiceAppImpl storeAppImpl)
    {
        if (!super.setUp(storeAppImpl)) {
            return false;
        }

        _listenerUser = storeAppImpl
            .getServerProperties()
            .getString(UPDATES_LISTENER_USER_PROPERTY, Optional.of(""))
            .get();

        resetPointStores();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public String supportedValueTypeCodes()
        throws SessionException
    {
        if (_supportedValueTypeCodes == null) {
            final EnumSet<Externalizer.ValueType> valueTypes = EnumSet
                .allOf(Externalizer.ValueType.class);

            for (final Store store: _pointStores) {
                try {
                    valueTypes.retainAll(store.supportedValueTypes());
                } catch (final StoreAccessException exception) {
                    throw exception.getCause();
                }
            }

            _supportedValueTypeCodes = Externalizer.ValueType
                .setToString(valueTypes);
        }

        return _supportedValueTypeCodes;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsDeliver()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPurge()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsSubscribe()
    {
        return false;
    }

    /** {@inheritDoc}
     *
     * <p>Note: Since this server accepts updates for more than one store at a
     * time, a null response from a store is converted to a
     * {@link ServiceClosedException}.</p>
     */
    @Override
    public synchronized Exception[] update(
            final PointValue[] updates,
            final Optional<Identity> identity)
    {
        final Exception[] responses = new Exception[updates.length];
        final Map<Store, List<Integer>> stores = new LinkedHashMap<Store,
            List<Integer>>();
        final Points points = getMetadata();

        for (int i = 0; i < updates.length; ++i) {
            final PointValue update = updates[i].restore(points);
            final Optional<Point> point = update.getPoint();

            if (point.isPresent()) {
                final Store store = point.get().getStore().get();
                List<Integer> positions = stores.get(store);

                store.addUpdate(update);

                if (positions == null) {
                    positions = new LinkedList<Integer>();
                    stores.put(store, positions);
                }

                positions.add(Integer.valueOf(i));
            } else if (update.hasPointUUID()) {
                responses[i] = new PointUnknownException(update.getPointUUID());
            } else {
                responses[i] = new PointUnknownException(
                    update.getValue().toString());
            }
        }

        final String user = identity
            .isPresent()? identity.get().getIdentifier(): _listenerUser;

        for (final Map.Entry<Store, List<Integer>> entry: stores.entrySet()) {
            final Store store = entry.getKey();
            final int updateCount = store.getUpdateCount();
            final Iterator<Integer> positions = entry.getValue().iterator();
            final Optional<Exception[]> exceptions;
            final boolean success;

            try {
                store.impersonate(Optional.of((user != null)? user: ""));
                success = store.sendUpdates();
                exceptions = store.getExceptions();
            } catch (final StoreAccessException exception) {
                for (int i = 0; i < updateCount; ++i) {
                    responses[positions.next().intValue()] = exception;
                }

                continue;
            }

            if (!exceptions.isPresent()) {
                final Exception exception = new ServiceClosedException();

                for (int i = 0; i < updateCount; ++i) {
                    responses[positions.next().intValue()] = exception;
                }
            } else if (!success) {
                for (final Exception exception: exceptions.get()) {
                    responses[positions.next().intValue()] = exception;
                }
            }
        }

        return responses;
    }

    /**
     * Resets point stores.
     */
    void resetPointStores()
    {
        _pointStores.clear();

        for (final Point point: getMetadata().getPointsCollection()) {
            final Store store = point.getStore().get();

            if (!_pointStores.contains(store)) {
                _pointStores.add(store);
            }
        }
    }

    /** The user identification for updating from the updates listener. */
    public static final String UPDATES_LISTENER_USER_PROPERTY =
        "updates.listener.user";

    private String _listenerUser;
    private final Collection<Store> _pointStores = new LinkedList<>();
    private volatile String _supportedValueTypeCodes;
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
