/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ParamDefExporter.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.MetadataElementLoader;
import org.rvpf.metadata.entity.ParamDefEntity;

/**
 * ParamDef exporter.
 */
final class ParamDefExporter
    extends DefEntityExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     */
    ParamDefExporter(@Nonnull final MetadataExporter owner)
    {
        super(owner, ParamDefEntity.class);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        final ParamDefEntity paramDefEntity = (ParamDefEntity) entity;

        super.export(entity, element);

        element
            .setAttribute(
                MetadataElementLoader.HOLDER_ATTRIBUTE,
                Optional.of(paramDefEntity.getHolder()));

        if (paramDefEntity.isHidden()) {
            element.setAttribute(MetadataElementLoader.HIDDEN_ATTRIBUTE, true);
        }

        if (paramDefEntity.isMultiple()) {
            element
                .setAttribute(MetadataElementLoader.MULTIPLE_ATTRIBUTE, true);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        return MetadataElementLoader.PARAM_DEF_ENTITY;
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
