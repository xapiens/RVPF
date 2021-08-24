/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClockMetadataFilter.java 3935 2019-04-28 14:42:38Z SFB $
 */

package org.rvpf.clock;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;

/**
 * Clock Filter.
 */
public final class ClockMetadataFilter
    extends MetadataFilter
{
    /**
     * Constructs an instance.
     *
     * @param originName The Origin name.
     */
    ClockMetadataFilter(@Nonnull final String originName)
    {
        super(false);

        _originName = Require.notNull(originName);
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
    public boolean areOriginsFiltered()
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
    public boolean areStoresNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getClientIdent()
    {
        return Optional.of(_CLOCK_IDENT);
    }

    /**
     * Gets the origin Entity.
     *
     * @return The optional origin Entity.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<OriginEntity> getOriginEntity()
    {
        return Optional.ofNullable(_originEntity);
    }

    /**
     * Gets the origin name.
     *
     * @return The origin name.
     */
    @Nonnull
    @CheckReturnValue
    public String getOriginName()
    {
        return _originName;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isOriginNeeded(final OriginEntity originEntity)
    {
        final boolean needed = (_originEntity == null)
                && _originName.equalsIgnoreCase(
                    originEntity.getName().orElse(null));

        if (needed) {
            _originEntity = originEntity;
        }

        return needed;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isPointNeeded(final PointEntity pointEntity)
    {
        return pointEntity.getOriginEntity().orElse(null) == _originEntity;
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset()
    {
        _originEntity = null;

        super.reset();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void includeOriginsXML(final XMLElement root)
    {
        root
            .addChild(ORIGINS_ELEMENT)
            .setAttribute(ORIGIN_ATTRIBUTE, getOriginName());
    }

    private static final String _CLOCK_IDENT = "Clock";

    private OriginEntity _originEntity;
    private final String _originName;
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
