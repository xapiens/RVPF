/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreClientTests.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.tests.store;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Message;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.ListLinkedHashMap;
import org.rvpf.base.util.container.ListMap;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.rmi.ServiceRegistry;
import org.rvpf.store.server.StoreServer;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.StoreServiceImpl;
import org.rvpf.store.server.the.TheStoreServiceActivator;
import org.rvpf.store.server.the.TheStoreServiceImpl;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.core.CoreTestsMessages;
import org.rvpf.tests.service.MetadataServiceTests;

/**
 * Store client tests.
 */
public abstract class StoreClientTests
    extends MetadataServiceTests
{
    /**
     * Gets the store with the specified name.
     *
     * @param name The name.
     * @param metadata The metadata holding the store entities.
     *
     * @return The optional store.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<? extends Store> getStore(
            final String name,
            final Metadata metadata)
    {
        final Optional<StoreEntity> optionalStoreEntity = metadata
            .getStoreEntity(Optional.of(name));

        if (!optionalStoreEntity.isPresent()) {
            return Optional.empty();
        }

        final StoreEntity storeEntity = optionalStoreEntity.get();

        Require.success(storeEntity.setUp(metadata));

        return storeEntity.getStore();
    }

    /**
     * Gets the store from a store service activator.
     *
     * @param serviceActivator The store service activator.
     *
     * @return The store.
     */
    protected static final Store getStore(
            final ServiceActivator serviceActivator)
    {
        final StoreEntity storeEntity = getStoreEntityClone(serviceActivator);

        Require.success(storeEntity.setUp(getMetadata(serviceActivator)));

        return storeEntity.getStore().get();
    }

    /**
     * Gets the store entity from a store service activator.
     *
     * @param serviceActivator The store service activator.
     *
     * @return The store entity.
     */
    protected static final StoreEntity getStoreEntity(
            final ServiceActivator serviceActivator)
    {
        return ((StoreServiceImpl) serviceActivator.getService())
            .getStoreEntity()
            .get();
    }

    /**
     * Gets a clone of the store entity from a store service activator.
     *
     * @param serviceActivator The store service activator.
     *
     * @return The clone of the store entity.
     */
    protected static final StoreEntity getStoreEntityClone(
            final ServiceActivator serviceActivator)
    {
        return getStoreEntity(serviceActivator).copy();
    }

    /**
     * Gets the store server from a store service activator.
     *
     * @param serviceActivator The store service activator.
     *
     * @return The store server.
     */
    protected static final StoreServer getStoreServer(
            final ServiceActivator serviceActivator)
    {
        final StoreServer storeServer = ((StoreServiceImpl) serviceActivator
            .getService())
            .getServer();

        Require.notNull(storeServer);

        return storeServer;
    }

    /**
     * Asks if a store service drops deleted values.
     *
     * @param serviceActivator The store service activator.
     *
     * @return True if it does.
     */
    protected static final boolean storeDropsDeleted(
            final ServiceActivator serviceActivator)
    {
        if (serviceActivator instanceof TheStoreServiceActivator) {
            final TheStoreServiceImpl storeServiceImpl =
                (TheStoreServiceImpl) serviceActivator
                    .getService();

            return storeServiceImpl.getMetadataServiceApp().isDropDeleted();
        }

        return true;
    }

    /**
     * Asks if a store service replicates.
     *
     * @param serviceActivator The store service activator.
     *
     * @return True if it does.
     */
    protected static final boolean storeReplicates(
            final ServiceActivator serviceActivator)
    {
        return (serviceActivator instanceof TheStoreServiceActivator)
               && (getMetadata(
                   serviceActivator).containsProperties(
                           REPLICATED_QUEUE_PROPERTIES));
    }

    /**
     * Updates store values.
     *
     * @param serviceActivator The store service activator.
     * @param pointValues The point values.
     *
     * @throws Exception On failure.
     */
    protected static final void updateStoreValues(
            final ServiceActivator serviceActivator,
            final List<PointValue> pointValues)
        throws Exception
    {
        final StoreServer storeServer = getStoreServer(serviceActivator);
        final Exception[] exceptions = storeServer
            .update(
                pointValues.toArray(new PointValue[pointValues.size()]),
                Optional.empty());

        for (final Exception exception: exceptions) {
            if (exception != null) {
                final Message message = new Message(
                    exception,
                    "Response value exception");

                Require.success(exception, message.toString());
            }
        }
    }

    /**
     * Expects updates.
     *
     * @param pointNames The point names of the expected updates.
     */
    protected final synchronized void expectUpdates(final String... pointNames)
    {
        for (final UUID pointUUID: _pointNamesToUUIDs(pointNames)) {
            _expectedUpdates.add(pointUUID, PointValue.NULL);
        }
    }

    /**
     * Gets a store session proxy for a store service.
     *
     * @param serviceActivator The store service activator.
     *
     * @return The store session proxy.
     */
    protected final StoreSessionProxy getStoreProxy(
            final ServiceActivator serviceActivator)
    {
        final Metadata metadata = getMetadata(serviceActivator);
        final StoreEntity storeEntity = metadata
            .getStoreEntity(
                metadata
                    .getStringValue(StoreServiceAppImpl.STORE_NAME_PROPERTY))
            .get();
        final Params storeParams = storeEntity.getParams();
        final RegistryEntry registryEntry = RegistryEntry
            .newBuilder()
            .setBinding(storeParams.getString(Store.BINDING_PARAM))
            .setName(
                storeParams.getString(Store.NAME_PARAM, storeEntity.getName()))
            .setDefaultName(Store.DEFAULT_STORE_NAME)
            .setDefaultRegistryAddress(ServiceRegistry.getRegistryAddress())
            .setDefaultRegistryPort(ServiceRegistry.getRegistryPort())
            .build();
        final StoreSessionProxy sessionProxy =
            (StoreSessionProxy) StoreSessionProxy
                .newBuilder()
                .setRegistryEntry(registryEntry)
                .setLoginUser(storeParams.getString(Store.USER_PARAM))
                .setLoginPassword(storeParams.getPassword(Store.PASSWORD_PARAM))
                .setClientName(getClass().getName())
                .setClientLogger(getThisLogger())
                .build();

        Require.notNull(sessionProxy);

        try {
            sessionProxy.connect();
        } catch (final Exception exception) {
            final Writer stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);

            exception.printStackTrace(printWriter);
            printWriter.close();
            Require.failure(stringWriter.toString());
        }

        return sessionProxy;
    }

    /**
     * Purges replicates.
     *
     * @param serviceActivator The store service activator.
     *
     * @throws Exception On failure.
     */
    protected final void purgeReplicates(
            final ServiceActivator serviceActivator)
        throws Exception
    {
        if (storeReplicates(serviceActivator)) {
            final MessagingSupport.Receiver receiver = getMessaging()
                .createClientReceiver(
                    getMetadata(serviceActivator)
                        .getPropertiesGroup(REPLICATED_QUEUE_PROPERTIES));

            receiver.purge();
            receiver.close();
        }
    }

    /**
     * Purges store values.
     *
     * @param serviceActivator The store service activator.
     *
     * @return The purged value count.
     *
     * @throws Exception On failure.
     */
    protected final int purgeStoreValues(
            final ServiceActivator serviceActivator)
        throws Exception
    {
        final StoreEntity storeEntity = getStoreEntity(serviceActivator);
        final Collection<UUID> pointUUIDs = new LinkedList<UUID>();

        Require.notNull(storeEntity);

        for (final Point point:
                getMetadata(serviceActivator).getPointsCollection()) {
            if (((PointEntity) point)
                .getStoreEntity()
                .orElse(null) == storeEntity) {
                pointUUIDs.add(point.getUUID().get());
            }
        }

        final StoreServer storeServer = getStoreServer(serviceActivator);
        final boolean supportsDelete = storeServer.supportsDelete();
        final boolean supportsPull = storeServer.supportsPull();
        final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
            .newBuilder();
        int purged = 0;

        queryBuilder.setAll(true);
        queryBuilder.setNotNull(!supportsDelete);
        queryBuilder.setIncludeDeleted(supportsPull);

        while (pointUUIDs.size() > 0) {
            final Collection<StoreValuesQuery> queries =
                new ArrayList<StoreValuesQuery>(
                    pointUUIDs.size());

            for (final UUID pointUUID: pointUUIDs) {
                queryBuilder.setPointUUID(pointUUID);
                queries.add(queryBuilder.build());
            }

            final StoreValues[] storeResponses = storeServer
                .select(
                    queries.toArray(new StoreValuesQuery[queries.size()]),
                    Optional.empty());
            final Collection<PointValue> updates = new LinkedList<PointValue>();

            pointUUIDs.clear();

            for (final StoreValues storeResponse: storeResponses) {
                if (storeResponse != null) {
                    final Optional<StoreValuesQuery.Mark> mark = storeResponse
                        .getMark();

                    Require
                        .notPresent(
                            storeResponse.getException(),
                            "Response exception");

                    for (final PointValue pointValue: storeResponse) {
                        updates.add(new VersionedValue.Purged(pointValue));
                    }

                    if (mark.isPresent()) {
                        pointUUIDs
                            .add(
                                mark
                                    .get()
                                    .getQuery()
                                    .getPointUUID()
                                    .orElse(null));
                    }
                }
            }

            final Exception[] exceptions = storeServer
                .update(
                    updates.toArray(new PointValue[updates.size()]),
                    Optional.empty());

            for (final Exception exception: exceptions) {
                Require.success(exception, "Response value exception");
            }

            purged += exceptions.length;
        }

        if (purged > 0) {
            getThisLogger()
                .info(
                    CoreTestsMessages.PURGED_VALUES,
                    String.valueOf(purged),
                    Integer.valueOf(purged));
        }

        return purged;
    }

    /**
     * Sets up expected updates.
     *
     * @param serviceActivator The store service activator.
     * @param pointNames The point names of the expected updates.
     *
     * @throws Exception On failure.
     */
    protected final void setUpExpectedUpdates(
            final ServiceActivator serviceActivator,
            final String... pointNames)
        throws Exception
    {
        Require.success(_expectedUpdatesStore == null);
        _expectedUpdatesStore = getStore(serviceActivator);
        Require.success(_expectedUpdatesStore.supportsDeliver());
        _expectedUpdatesMetadata = getMetadata(serviceActivator);

        final UUID[] pointUUIDs = _pointNamesToUUIDs(pointNames);

        _expectedUpdates = new ListLinkedHashMap<UUID, PointValue>();

        for (final UUID pointUUID: pointUUIDs) {
            _expectedUpdates.put(pointUUID);
        }

        Require.success(_expectedUpdatesStore.subscribe(pointUUIDs));
    }

    /**
     * Starts the store service.
     *
     * @param purge True asks for a values purge.
     *
     * @return The store service activator.
     *
     * @throws Exception On failure.
     */
    protected final ServiceActivator startStoreService(
            final boolean purge)
        throws Exception
    {
        final ClassDef classDef = getConfig()
            .getClassDef(
                STORE_SERVICE_PROPERTY,
                Optional.of(DEFAULT_STORE_SERVICE))
            .get();
        final Class<?> serviceClass = classDef.getInstanceClass();

        Require.notNull(serviceClass);

        final ServiceActivator serviceActivator = startService(
            serviceClass,
            Optional.empty());

        if (purge) {
            purgeStoreValues(serviceActivator);
        }

        return serviceActivator;
    }

    /**
     * Tears down expected updates.
     *
     * @throws Exception On failure.
     */
    protected final void tearDownExpectedUpdates()
        throws Exception
    {
        if (_expectedUpdatesStore != null) {
            if (_expectedUpdates != null) {
                final Set<UUID> keys = _expectedUpdates.keySet();

                Require
                    .success(
                        _expectedUpdatesStore
                            .unsubscribe(keys.toArray(new UUID[keys.size()])));

                _expectedUpdates.clear();
                _expectedUpdates = null;
            }

            _expectedUpdatesStore.close();
            _expectedUpdatesStore = null;
        }
    }

    /**
     * Waits for an update on a specified point.
     *
     * @param pointName The point name for the update.
     *
     * @return The point value.
     *
     * @throws Exception On failure.
     */
    protected final synchronized PointValue waitForUpdate(
            final String pointName)
        throws Exception
    {
        final Point point = _expectedUpdatesMetadata
            .getPointByName(pointName)
            .get();
        final UUID pointUUID = point.getUUID().get();

        for (;;) {
            final List<PointValue> pointValues = _expectedUpdates
                .getAll(pointUUID);

            Require.notNull(pointValues);
            Require.failure(pointValues.isEmpty());

            if (pointValues.get(0) != PointValue.NULL) {
                return pointValues.remove(0);
            }

            final StoreValues storeResponse;

            storeResponse = _expectedUpdatesStore
                .deliver(Short.MAX_VALUE, getTimeout());
            Require.notNull(storeResponse);
            Require
                .notPresent(
                    storeResponse.getException(),
                    "Store response exception");
            Require.failure(storeResponse.isEmpty());

            for (final PointValue pointValue: storeResponse) {
                final List<PointValue> expectedValues = _expectedUpdates
                    .getAll(pointValue.getPointUUID());

                Require.notNull(expectedValues);

                final ListIterator<PointValue> entryIterator = expectedValues
                    .listIterator();

                while (entryIterator.hasNext()) {
                    if (entryIterator.next() == PointValue.NULL) {
                        entryIterator.set(pointValue);

                        break;
                    }
                }
            }
        }
    }

    private UUID[] _pointNamesToUUIDs(final String[] pointNames)
    {
        final UUID[] pointUUIDs = new UUID[pointNames.length];

        for (int i = 0; i < pointUUIDs.length; ++i) {
            final Point point = _expectedUpdatesMetadata
                .getPointByName(pointNames[i])
                .get();

            pointUUIDs[i] = point.getUUID().get();
        }

        return pointUUIDs;
    }

    /** The store notifier queue properties. */
    public static final String NOTIFIER_QUEUE_PROPERTIES =
        "tests.store.notifier.queue";

    /** Replicator enabled property. */
    public static final String REPLICATOR_ENABLED_PROPERTY =
        "tests.store.replicator.enabled";

    /** Snapshot service name. */
    public static final String SNAPSHOT_SERVICE_NAME = "Snapshot";

    /** The store updates listener queue property. */
    public static final String UPDATER_QUEUE_PROPERTIES =
        "tests.store.updater.queue";

    /** Updates listener enabled property. */
    public static final String UPDATES_LISTENER_ENABLED =
        "tests.store.updates.listener.enabled";

    private ListMap<UUID, PointValue> _expectedUpdates;
    private Metadata _expectedUpdatesMetadata;
    private Store _expectedUpdatesStore;
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
