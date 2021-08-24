/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataServiceImpl.java 4097 2019-06-25 15:35:48Z SFB $
 */

package org.rvpf.service.metadata;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.Point;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.tool.Require;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.ServiceImpl;

/**
 * Metadata service implementation.
 */
@ThreadSafe
public abstract class MetadataServiceImpl
    extends ServiceImpl
    implements MetadataService
{
    /** {@inheritDoc}
     */
    @Override
    public final Metadata getMetadata()
    {
        return Require.notNull(_metadata);
    }

    /**
     * Asks if the service has metadata.
     *
     * @return True If the service has metadata.
     */
    @CheckReturnValue
    public final boolean hasMetadata()
    {
        return _metadata != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean loadMetadata(final MetadataFilter filter)
    {
        final Metadata metadata = MetadataDocumentLoader
            .fetchMetadata(
                Require.notNull(filter),
                Optional.of(getConfig()),
                getServiceUUID(),
                Optional.empty());

        if (metadata == null) {
            return false;
        }

        if (!onNewMetadata(metadata)) {
            return false;
        }

        metadata.setService(this);
        _metadata = metadata;

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void monitorStores()
    {
        for (final StoreEntity storeEntity: _metadata.getStoreEntities()) {
            monitorService(
                Optional.empty(),
                storeEntity.getUUID(),
                storeEntity.getName());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean reloadMetadata()
    {
        restoreConfigState();

        return loadMetadata(getMetadata().getFilter().clone());
    }

    /** {@inheritDoc}
     */
    @Override
    public final void resetPointsStore()
        throws ServiceNotAvailableException
    {
        for (final Point point: getMetadata().getPointsCollection()) {
            ((PointEntity) point).tearDownStore();
        }

        for (final Point point: getMetadata().getPointsCollection()) {
            if (!((PointEntity) point).setUpStore(getMetadata())) {
                throw new ServiceNotAvailableException();
            }
        }
    }

    /**
     * Called on new metadata.
     *
     * @param metadata The new metadata.
     *
     * @return False to reject the metadata.
     */
    @CheckReturnValue
    protected boolean onNewMetadata(@Nonnull final Metadata metadata)
    {
        return metadata != null;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean onSignal(final Signal signal)
    {
        if (REFRESH_METADATA_SIGNAL.equalsIgnoreCase(signal.getName())) {
            setRestartSignaled(true);

            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void tearDown()
    {
        // Lets the points do their cleanup.

        if (_metadata != null) {
            _metadata.tearDownPoints();
        }

        // Tears down everything else.

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void tearDownConfig()
    {
        if (_metadata != null) {
            _metadata.tearDown();
            _metadata = null;
        }

        super.tearDownConfig();
    }

    private volatile Metadata _metadata;
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
