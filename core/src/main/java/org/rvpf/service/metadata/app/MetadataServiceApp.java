/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataServiceApp.java 4081 2019-06-14 14:54:30Z SFB $
 */

package org.rvpf.service.metadata.app;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.metadata.Metadata;
import org.rvpf.service.app.ServiceApp;
import org.rvpf.service.metadata.MetadataService;

/**
 * Metadata service application interface.
 *
 * <p>The methods of this interface are calls by the metadata service
 * application holder to its client, the metadata service application.</p>
 */
public interface MetadataServiceApp
    extends ServiceApp
{
    /**
     * Gets the entity name.
     *
     * @return The optional entity name.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getEntityName();

    /**
     * Called on new metadata.
     *
     * <p>Overidden as needed.</p>
     *
     * <p>Called by the framework and by overriding classes at the end of the
     * override.</p>
     *
     * @param metadata The new metadata.
     *
     * @return False to reject the metadata.
     */
    @CheckReturnValue
    boolean onNewMetadata(@Nonnull Metadata metadata);

    /**
     * Sets up the application.
     *
     * <p>Overidden as needed.</p>
     *
     * <p>Called by the framework and by overriding classes at the beginning of
     * the override.</p>
     *
     * @param service The service holding this application.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull MetadataService service);
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
