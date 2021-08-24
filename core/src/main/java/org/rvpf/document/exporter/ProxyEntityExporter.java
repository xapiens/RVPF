/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyEntityExporter.java 4007 2019-05-18 19:32:32Z SFB $
 */

package org.rvpf.document.exporter;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.MetadataElementLoader;
import org.rvpf.metadata.entity.ProxyEntity;

/**
 * Proxy entity exporter.
 */
public abstract class ProxyEntityExporter
    extends ParamsEntityExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     * @param entityClass The class of the entities exported by this.
     */
    protected ProxyEntityExporter(
            @Nonnull final MetadataExporter owner,
            @Nonnull final Class<? extends Entity> entityClass)
    {
        super(owner, entityClass);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        super.export(entity, element);

        final ProxyEntity proxyEntity = (ProxyEntity) entity;

        element
            .setAttribute(
                MetadataElementLoader.CLASS_DEF_REFERENCE,
                reference(proxyEntity.getClassDef()));
    }

    /** {@inheritDoc}
     */
    @Override
    protected void registerReferences(final Entity entity)
    {
        super.registerReferences(entity);

        final ProxyEntity proxyEntity = (ProxyEntity) entity;

        registerReference(proxyEntity.getClassDef());
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
