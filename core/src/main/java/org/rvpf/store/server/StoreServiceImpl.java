/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreServiceImpl.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.server;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.StatsOwner;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.metadata.app.MetadataServiceAppHolderImpl;

/**
 * Store service implementation.
 */
public abstract class StoreServiceImpl
    extends MetadataServiceAppHolderImpl
{
    /**
     * Gets the server.
     *
     * @return The server.
     */
    @Nonnull
    @CheckReturnValue
    public final StoreServer getServer()
    {
        return ((StoreServiceApp) getMetadataServiceApp()).getServer();
    }

    /**
     * Gets the server name.
     *
     * @return The server name.
     */
    @Nonnull
    @CheckReturnValue
    public final String getServerName()
    {
        return ((StoreServiceApp) getMetadataServiceApp()).getServerName();
    }

    /**
     * Gets the store entity.
     *
     * @return The optional store entity.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<StoreEntity> getStoreEntity()
    {
        return ((StoreServiceApp) getMetadataServiceApp()).getStoreEntity();
    }

    /** {@inheritDoc}
     */
    @Override
    protected final StoreStats createStats(final StatsOwner statsOwner)
    {
        return new StoreStats(statsOwner);
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
