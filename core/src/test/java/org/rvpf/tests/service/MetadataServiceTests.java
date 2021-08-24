/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataServiceTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.service;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.tests.TestsMetadataFilter;

/**
 * Metadata service tests.
 */
public abstract class MetadataServiceTests
    extends ServiceTests
{
    /**
     * Gets the metadata.
     *
     * @return The metadata.
     */
    @Nonnull
    @CheckReturnValue
    protected Metadata getMetadata()
    {
        if (_metadata == null) {
            loadMetadata(true);
        }

        return _metadata;
    }

    /**
     * Gets the metadata.
     *
     * @param setUpPoints True to set-up points.
     *
     * @return The metadata.
     */
    @Nonnull
    @CheckReturnValue
    protected Metadata getMetadata(final boolean setUpPoints)
    {
        final Metadata metadata = getMetadata();

        if (setUpPoints) {
            for (final Point point: metadata.getPointsCollection()) {
                Require.success(((PointEntity) point).setUp(metadata));
            }
        }

        return metadata;
    }

    /**
     * Gets a point, given its key.
     *
     * @param key The point's key.
     *
     * @return The point.
     */
    @Nonnull
    @CheckReturnValue
    protected Point getPoint(@Nonnull final String key)
    {
        return _metadata.getPoint(key).get();
    }

    /**
     * Loads the metadata.
     *
     * <p>Redundant calls will be ignored.</p>
     *
     * @param keepEntities True if entities should be kept.
     */
    protected void loadMetadata(final boolean keepEntities)
    {
        loadMetadata(new TestsMetadataFilter(keepEntities));
    }

    /**
     * Loads the metadata.
     *
     * <p>Redundant calls will be ignored.</p>
     *
     * @param filter The metadata filter.
     */
    protected void loadMetadata(@Nonnull final MetadataFilter filter)
    {
        if (_metadata == null) {
            _metadata = MetadataDocumentLoader
                .fetchMetadata(
                    filter,
                    Optional.of(getConfig()),
                    getConfig().getServiceUUID(),
                    Optional.empty());
            Require.notNull(_metadata, "Loaded metadata");

            if (!filter.areEntitiesKept()) {
                _metadata.cleanUp();
            }
        }
    }

    private Metadata _metadata;
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
