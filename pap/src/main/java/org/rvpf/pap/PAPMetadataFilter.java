/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPMetadataFilter.java 3885 2019-02-05 20:22:42Z SFB $
 */

package org.rvpf.pap;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.document.loader.MetadataFilter;

/**
 * PAP metadata filter.
 */
public class PAPMetadataFilter
    extends MetadataFilter
{
    /**
     * Constructs an instance.
     *
     * @param attributesUsages The attributes usages.
     */
    public PAPMetadataFilter(@Nonnull final Set<String> attributesUsages)
    {
        super(true);

        _attributesUsages = Require.notNull(attributesUsages);
    }

    /**
     * Constructs an instance.
     *
     * <p>Used by scripts.</p>
     *
     * @param attributesUsage The attributes usage.
     */
    public PAPMetadataFilter(@Nonnull final String attributesUsage)
    {
        super(true);

        _attributesUsages = new HashSet<>(1);
        _attributesUsages.add(Require.notNull(attributesUsage));
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
    public boolean areAttributesNeeded(final String usage)
    {
        return _attributesUsages.contains(usage.toUpperCase(Locale.ROOT));
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
    public boolean arePointsNeeded()
    {
        return true;
    }

    private final Set<String> _attributesUsages;
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
