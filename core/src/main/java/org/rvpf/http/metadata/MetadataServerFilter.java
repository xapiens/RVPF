/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataServerFilter.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http.metadata;

import java.util.Optional;

import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.Metadata;

/**
 * Metadata server filter.
 */
public final class MetadataServerFilter
    extends MetadataFilter
{
    /**
     * Constructs an instance.
     */
    public MetadataServerFilter()
    {
        super(true);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areAttributesNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areBehaviorsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areContentsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areContentsRequired()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areGroupsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areOriginsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areOriginsRequired()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean arePermissionsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean arePointInputsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean arePointReplicatesNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areStoresNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areStoresRequired()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areTextsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areTransformsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areTransformsRequired()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getClientIdent()
    {
        return Optional.of(_METADATA_SERVER_IDENT);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean tidy(final Metadata metadata)
    {
        if (!super.tidy(metadata)) {
            return false;
        }

        return metadata.adjustPointsLevel();
    }

    private static final String _METADATA_SERVER_IDENT = "MetadataServer";
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
