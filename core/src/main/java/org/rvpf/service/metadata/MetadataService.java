/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataService.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.service.metadata;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.Service;

/**
 * Metadata service.
 */
public interface MetadataService
    extends Service
{
    /**
     * Gets the service metadata.
     *
     * <p>Promoted to public access to help the service tests.</p>
     *
     * @return The metadata object.
     */
    @Nonnull
    @CheckReturnValue
    Metadata getMetadata();

    /**
     * Loads the metadata.
     *
     * @param metadataFilter The metadata filter.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean loadMetadata(@Nonnull MetadataFilter metadataFilter);

    /**
     * Monitors stores.
     */
    void monitorStores();

    /**
     * Reloads the metadata.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean reloadMetadata();

    /**
     * Resets the association of the points to their store.
     *
     * @throws ServiceNotAvailableException When the service is not available.
     */
    void resetPointsStore()
        throws ServiceNotAvailableException;

    /** The refresh metadata signal name. */
    String REFRESH_METADATA_SIGNAL = "RefreshMetadata";
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
