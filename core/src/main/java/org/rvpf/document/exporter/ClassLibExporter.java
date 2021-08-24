/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClassLibExporter.java 3949 2019-05-03 15:35:40Z SFB $
 */

package org.rvpf.document.exporter;

import java.net.URI;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.config.TopologicalErrorException;
import org.rvpf.config.UndefinedEntityException;
import org.rvpf.config.entity.ClassLibEntity;
import org.rvpf.document.loader.ConfigElementLoader;

/**
 * ClassLib exporter.
 */
final class ClassLibExporter
    extends DefEntityExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     */
    ClassLibExporter(@Nonnull final ConfigExporter owner)
    {
        super(owner, ClassLibEntity.class);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        final ClassLibEntity classLibEntity = (ClassLibEntity) entity;

        if (!classLibEntity.isDefined()) {
            throw new RuntimeException(
                new UndefinedEntityException(classLibEntity));
        }

        super.export(entity, element);

        final Optional<Boolean> cached = classLibEntity.getCached();

        if (cached.isPresent()) {
            element
                .setAttribute(
                    ConfigElementLoader.CACHED_ATTRIBUTE,
                    Optional.of(cached.get().booleanValue()? "1": "0"));
        }

        for (final URI location: classLibEntity.getLocations()) {
            final XMLElement urlElement = createElement(
                ConfigElementLoader.LOCATION_ELEMENT);

            urlElement.addText(location.toString());
            element.addChild(urlElement);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        return ConfigElementLoader.CLASS_LIB_ENTITY;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void registerReferences(final Entity entity)
    {
        super.registerReferences(entity);

        final ClassLibEntity classLib = (ClassLibEntity) entity;

        for (final Entity classLibReference: classLib.getClassLibs()) {
            registerReference(Optional.of(classLibReference));
        }

        try {
            classLib.adjustLevel();
        } catch (final TopologicalErrorException exception) {
            throw new RuntimeException(exception);
        }
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
