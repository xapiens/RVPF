/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueryContext.java 4112 2019-08-02 20:00:26Z SFB $
 */

package org.rvpf.http.query;

import java.security.Principal;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.http.ServiceSessionException;
import org.rvpf.http.StoreClient;

/**
 * Query context.
 */
final class QueryContext
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
    }

    /**
     * Gets the point info for the point info queries.
     *
     * @param query An array of point info queries.
     *
     * @return An array point info.
     *
     * @throws ServiceSessionException On session exception.
     */
    @Nonnull
    @CheckReturnValue
    synchronized PointBinding[] getPoints(
            @Nonnull final PointBinding.Request[] query)
        throws ServiceSessionException
    {
        final PointBinding[] response;

        try {
            response = getStore().getPointBindings(query);
        } catch (final SessionException exception) {
            throw new ServiceSessionException(exception);
        }

        if (response == null) {
            throw new ServiceSessionException(new ServiceClosedException());
        }

        return response;
    }

    /**
     * Gets the store values to the store values queries.
     *
     * @param queries An array of store values query.
     * @param principal The optional requesting user's principal.
     *
     * @return An array store values.
     *
     * @throws ServiceSessionException On session exception.
     */
    @Nonnull
    @CheckReturnValue
    synchronized StoreValues[] select(
            @Nonnull final StoreValuesQuery[] queries,
            @Nonnull final Optional<Principal> principal)
        throws ServiceSessionException
    {
        final String user = principal
            .isPresent()? principal.get().getName(): "";

        impersonate(ValueConverter.canonicalizeString(Optional.of(user)));

        final StoreValues[] response;

        try {
            response = getStore().select(queries);
        } catch (final SessionException exception) {
            throw new ServiceSessionException(exception);
        }

        if (response == null) {
            throw new ServiceSessionException(new ServiceClosedException());
        }

        return response;
    }
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
