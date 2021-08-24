/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProcessorMetadataFilter.java 3965 2019-05-07 22:26:58Z SFB $
 */

package org.rvpf.processor;

import java.util.Iterator;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;

/**
 * Processor filter.
 */
final class ProcessorMetadataFilter
    extends MetadataFilter
{
    /**
     * Constructs an instance.
     *
     * @param processorName The processor name.
     */
    ProcessorMetadataFilter(@Nonnull final String processorName)
    {
        super(false);

        _processorName = Require.notNull(processorName);
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
    public boolean areOriginsRequired()
    {
        return false;
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
    public boolean arePointInputsNeeded(final PointEntity pointEntity)
    {
        return (_processor != null)
               && (pointEntity.getOriginEntity().orElse(null) == _processor);
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
    public boolean areTransformsNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areTransformsRequired()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getClientIdent()
    {
        return Optional.of(_processorName);
    }

    /**
     * Gets the processor (origin entity).
     *
     * @return The optional processor.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<OriginEntity> getProcessor()
    {
        return Optional.ofNullable(_processor);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isOriginNeeded(final OriginEntity originEntity)
    {
        final boolean needed = (_processor == null)
                && _processorName.equalsIgnoreCase(
                    originEntity.getName().orElse(null));

        if (needed) {
            _processor = originEntity;
        }

        return needed;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isPointTransformNeeded(final PointEntity pointEntity)
    {
        return (_processor != null)
               && (pointEntity.getOriginEntity().orElse(null) == _processor);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean tidy(final Metadata metadata)
    {
        if (!super.tidy(metadata)) {
            return false;
        }

        // Prunes unused points.

        for (final Iterator<Point> i =
                metadata.getPointsCollection().iterator();
                i.hasNext(); ) {
            final PointEntity pointEntity = (PointEntity) i.next();

            if (!pointEntity.getOrigin().isPresent()
                    && pointEntity.getResults().isEmpty()) {
                pointEntity.setDropped(true);
                i.remove();
            }
        }

        // Adjusts points level.

        return metadata.adjustPointsLevel();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void includeOriginsXML(final XMLElement root)
    {
        root
            .addChild(ORIGINS_ELEMENT)
            .setAttribute(ORIGIN_ATTRIBUTE, _processorName);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void reset()
    {
        _processor = null;

        super.reset();
    }

    private OriginEntity _processor;
    private final String _processorName;
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
