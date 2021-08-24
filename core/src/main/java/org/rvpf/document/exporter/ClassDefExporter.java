/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClassDefExporter.java 4005 2019-05-18 15:52:45Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.config.TopologicalErrorException;
import org.rvpf.config.UndefinedEntityException;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.config.entity.ClassLibEntity;
import org.rvpf.document.loader.ConfigElementLoader;

/**
 * ClassDef exporter.
 */
final class ClassDefExporter
    extends DefEntityExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     */
    ClassDefExporter(@Nonnull final ConfigExporter owner)
    {
        super(owner, ClassDefEntity.class);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        final ClassDefEntity classDefEntity = (ClassDefEntity) entity;

        if (!classDefEntity.isDefined()) {
            throw new RuntimeException(
                new UndefinedEntityException(classDefEntity));
        }

        super.export(entity, element);

        element
            .setAttribute(
                ConfigElementLoader.PACKAGE_ATTRIBUTE,
                Optional.of(classDefEntity.getPackageName()));
        element
            .setAttribute(
                ConfigElementLoader.MEMBER_ATTRIBUTE,
                Optional.of(classDefEntity.getMember()));

        final Optional<ClassLibEntity> classLibEntity = classDefEntity
            .getClassLib();

        if (classLibEntity.isPresent()) {
            element
                .setAttribute(
                    ConfigElementLoader.CLASS_LIB_REFERENCE,
                    reference(classLibEntity));
        }

        for (final ClassDefEntity implemented:
                classDefEntity.getImplemented()) {
            final XMLElement implementsElement = createElement(
                ConfigElementLoader.IMPLEMENTS_ELEMENT);

            implementsElement
                .setAttribute(
                    ConfigElementLoader.CLASS_DEF_REFERENCE,
                    reference(Optional.of(implemented)));
            element.addChild(implementsElement);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        return ConfigElementLoader.CLASS_DEF_ENTITY;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void registerReferences(final Entity entity)
    {
        super.registerReferences(entity);

        final ClassDefEntity classDefEntity = (ClassDefEntity) entity;

        registerReference(classDefEntity.getClassLib());

        for (final Entity implementedReference:
                classDefEntity.getImplemented()) {
            registerReference(Optional.of(implementedReference));
        }

        try {
            classDefEntity.adjustLevel();
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
