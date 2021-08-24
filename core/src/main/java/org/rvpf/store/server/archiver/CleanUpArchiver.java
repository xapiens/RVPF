/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CleanUpArchiver.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.store.server.archiver;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreSessionProxy.ValuesIterator;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.rmi.ServiceRegistry;
import org.rvpf.store.server.StoreMessages;

/**
 * Clean-up archiver.
 */
public final class CleanUpArchiver
    extends Archiver.Abstract
{
    /**
     * Cleans up.
     *
     * @param storeName The name of the store to clean up.
     * @param metadata The current metadata.
     * @param atticProperties Attic properties.
     *
     * @return True on success.
     *
     * @throws StoreAccessException On failure.
     */
    @CheckReturnValue
    public boolean cleanUp(
            @Nonnull final String storeName,
            @Nonnull final Metadata metadata,
            @Nonnull final KeyedGroups atticProperties)
        throws StoreAccessException
    {
        if (!ServiceRegistry.setUp(metadata.getConfig().getProperties())) {
            return false;
        }

        if (!metadata.validatePointsRelationships()) {
            return false;
        }

        final Optional<StoreEntity> optionalStoreEntity = metadata
            .getStoreEntity(Optional.of(storeName));

        if (!optionalStoreEntity.isPresent()) {
            getThisLogger().error(ServiceMessages.STORE_NOT_FOUND, storeName);

            return false;
        }

        final StoreEntity storeEntity = optionalStoreEntity.get();

        if (!storeEntity.setUp(metadata)) {
            return false;
        }

        if (!setUpAttic(
                atticProperties,
                metadata.getConfig(),
                metadata.getDataDir())) {
            return false;
        }

        final Store store = storeEntity.getStore().get();

        final Set<UUID> knownUUIDs = new HashSet<>();

        for (final Point point: metadata.getPointsCollection()) {
            if (point.getStore().get() == store) {
                knownUUIDs.add(point.getUUID().get());
            }
        }

        final StoreValuesQuery storeQuery = StoreValuesQuery
            .newBuilder()
            .setAll(true)
            .build();
        final StoreSessionProxy.ValuesIterator valuesIterator =
            (ValuesIterator) store
                .iterate(storeQuery);
        final Set<UUID> seenUUIDs = new HashSet<>();
        final Set<UUID> unknownUUIDs = new HashSet<>();
        final Attic attic = getAttic().orElse(null);

        while (valuesIterator.hasNext()) {
            final StoreValues storeValues = valuesIterator.getStoreValues();
            final List<PointValue> purgedValues = new LinkedList<>();

            for (final PointValue storeValue: storeValues) {
                final UUID seenUUID = storeValue.getPointUUID();

                seenUUIDs.add(seenUUID);

                if (!knownUUIDs.contains(seenUUID)) {
                    unknownUUIDs.add(seenUUID);
                    purgedValues.add(new VersionedValue.Purged(storeValue));
                }
            }

            if (attic != null) {
                attic.put(purgedValues);
                attic.commit();
            }

            Require.success(store.sendUpdates(purgedValues));

            storeValues.clear();
        }

        getThisLogger()
            .info(
                StoreMessages.ARCHIVER_CLEAN_UP,
                Integer.valueOf(knownUUIDs.size()),
                Integer.valueOf(seenUUIDs.size()),
                Integer.valueOf(unknownUUIDs.size()));

        if (attic != null) {
            attic.commit();
            attic.tearDown();
        }

        updateStats();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start() {}

    /** {@inheritDoc}
     */
    @Override
    public void stop() {}
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
