/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.http.update;

import java.security.Principal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.http.ServiceSessionException;
import org.rvpf.http.StoreClient;

/**
 * Purge context.
 */
final class PurgeContext
    extends StoreClient
{
    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(
            final Config config,
            final KeyedGroups contextProperties)
    {
        return super.setUp(config, contextProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void tearDown()
    {
        super.tearDown();

        _pointUUIDs = null;
    }

    /**
     * Gets the UUID for the identified point.
     *
     * @param pointString Either the name or the text representation of the UUID
     *                    of the point.
     *
     * @return The optional UUID of the point.
     *
     * @throws ServiceSessionException On session exception.
     */
    @Nonnull
    @CheckReturnValue
    Optional<UUID> getPointUUID(
            @Nonnull final String pointString)
        throws ServiceSessionException
    {
        final UUID pointUUID;

        if (UUID.isUUID(pointString)) {
            pointUUID = UUID.fromString(pointString).get();
        } else {
            synchronized (this) {
                if (_pointUUIDs == null) {
                    final Optional<PointBinding[]> responses;

                    impersonate(Optional.empty());

                    try {
                        responses = getStore()
                            .getPointBindings(
                                PointBinding.Request.newBuilder().build());
                    } catch (final SessionException exception) {
                        throw new ServiceSessionException(exception);
                    }

                    if (!responses.isPresent()) {
                        throw new ServiceSessionException(
                            new ServiceClosedException());
                    }

                    _pointUUIDs = new HashMap<>(
                        KeyedValues.hashCapacity(responses.get().length));

                    for (final PointBinding binding: responses.get()) {
                        _pointUUIDs
                            .put(
                                binding.getName().toUpperCase(Locale.ROOT),
                                binding.getUUID());
                    }
                }

                pointUUID = _pointUUIDs
                    .get(pointString.trim().toUpperCase(Locale.ROOT));
            }
        }

        return Optional.ofNullable(pointUUID);
    }

    /**
     * Purges the supplied points on the specified interval.
     *
     * @param points The points.
     * @param principal The optional requesting user's principal.
     *
     * @return An optional exception array (each null on success).
     *
     * @throws ServiceSessionException On session exception.
     */
    @Nonnull
    @CheckReturnValue
    synchronized Optional<Exception[]> purge(
            @Nonnull final PointValue[] points,
            @Nonnull final Optional<Principal> principal)
        throws ServiceSessionException
    {
        impersonate(
            Optional.of(principal.isPresent()? principal.get().getName(): ""));

        final Exception[] responses;

        try {
            responses = getStore().update(points);
        } catch (final SessionException exception) {
            throw new ServiceSessionException(exception);
        }

        return Optional.ofNullable(responses);
    }

    private volatile Map<String, UUID> _pointUUIDs;
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
