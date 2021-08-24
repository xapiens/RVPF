/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataServiceAppImpl.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.service.metadata.app;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.service.Service;
import org.rvpf.service.app.ServiceAppImpl;
import org.rvpf.service.metadata.MetadataService;

/**
 * Metadata service application implementation.
 *
 * <p>This abstract class supplies a minimal implementation for the metadata
 * service application interface. It also supplies help methods (protected) to
 * its subclasses.</p>
 */
public abstract class MetadataServiceAppImpl
    extends ServiceAppImpl
    implements MetadataServiceApp
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getEntityName()
    {
        return Optional.empty();
    }

    /**
     * Gets the metadata.
     *
     * <p>Called as needed.</p>
     *
     * @return The metadata.
     */
    @Nonnull
    @CheckReturnValue
    public Metadata getMetadata()
    {
        return getService().getMetadata();
    }

    /** {@inheritDoc}
     */
    @Override
    public MetadataService getService()
    {
        return (MetadataService) super.getService();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewMetadata(final Metadata metadata)
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final MetadataService service)
    {
        return super.setUp(service);
    }

    /** {@inheritDoc}
     *
     * <p>Overriden to prevent call by subclasses:
     * {@link #setUp(MetadataService)} should be called instead.</p>
     *
     * @deprecated To generate a compile time warning.
     */
    @Override
    @Deprecated
    public boolean setUp(final Service service)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Loads the metadata as specified in a filter.
     *
     * @param metadataFilter The metadata filter.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean loadMetadata(@Nonnull final MetadataFilter metadataFilter)
    {
        return getService().loadMetadata(metadataFilter);
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
