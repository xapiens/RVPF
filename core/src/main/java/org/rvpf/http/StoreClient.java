/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreClient.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.config.Config;
import org.rvpf.store.client.ProxyStoreClient;

/**
 * Store client.
 */
public abstract class StoreClient
    extends HTTPModule.Context
{
    /**
     * Gets the store.
     *
     * @return The store.
     */
    @Nonnull
    @CheckReturnValue
    protected StoreSessionProxy getStore()
    {
        return _storeClient.getStore();
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return _logger;
    }

    /**
     * Impersonates a user.
     *
     * @param user The other user (empty string for anonymous, empty to cancel).
     *
     * @throws ServiceSessionException On store session exception.
     */
    protected void impersonate(
            @Nonnull final Optional<String> user)
        throws ServiceSessionException
    {
        _storeClient.impersonate(user);
    }

    /**
     * Sets up this.
     *
     * @param config The Config.
     * @param contextProperties The context properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean setUp(
            @Nonnull final Config config,
            @Nonnull final KeyedGroups contextProperties)
    {
        return _storeClient.setUp(config, contextProperties);
    }

    /**
     * Tears down what has been set up.
     */
    protected void tearDown()
    {
        _storeClient.tearDown();
    }

    private final Logger _logger = Logger.getInstance(getClass());
    private final ProxyStoreClient _storeClient = new ProxyStoreClient();
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
