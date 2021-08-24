/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RPNMetadataFilter.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http.rpn;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.entity.EngineEntity;

/**
 * RPN metadata filter.
 */
public final class RPNMetadataFilter
    extends MetadataFilter
{
    /**
     * Constructs an instance.
     *
     * @param engineName The engine name.
     */
    public RPNMetadataFilter(@Nonnull final String engineName)
    {
        super(false);

        _engineName = Require.notNull(engineName);
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
    public boolean areEnginesFiltered()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areEnginesNeeded()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areEntitiesKept()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean arePointsNeeded()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean areTransformsNeeded()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<String> getClientIdent()
    {
        return Optional.of(_RPN_IDENT);
    }

    /**
     * Gets the engine name.
     *
     * @return The engine name.
     */
    @Nonnull
    @CheckReturnValue
    public String getEngineName()
    {
        return _engineName;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isEngineNeeded(final EngineEntity engineEntity)
    {
        return _engineName
            .equalsIgnoreCase(engineEntity.getName().orElse(null));
    }

    /** {@inheritDoc}
     */
    @Override
    protected void includeEnginesXML(final XMLElement root)
    {
        root
            .addChild(ENGINES_ELEMENT)
            .setAttribute(ENGINE_ATTRIBUTE, _engineName);
    }

    private static final String _RPN_IDENT = "RPN";

    private final String _engineName;
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
