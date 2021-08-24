/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TransformExporter.java 3957 2019-05-06 14:25:42Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.MetadataElementLoader;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.TransformEntity;

/**
 * Transform exporter.
 */
final class TransformExporter
    extends ParamsEntityExporter
{
    /**
     * Creates an instance.
     *
     * @param owner The exporter owning this.
     */
    TransformExporter(@Nonnull final MetadataExporter owner)
    {
        super(owner, TransformEntity.class);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        final TransformEntity transformEntity = (TransformEntity) entity;

        super.export(entity, element);

        element
            .setAttribute(
                MetadataElementLoader.ENGINE_REFERENCE,
                reference(transformEntity.getEngineEntity()));

        for (final TransformEntity.Arg arg: transformEntity.getArgs()) {
            final XMLElement argElement = createElement(
                MetadataElementLoader.ARG_ELEMENT);
            boolean firstBehavior = true;

            argElement
                .setAttribute(
                    MetadataElementLoader.CONTENT_REFERENCE,
                    reference(arg.getContentEntity()));

            if (arg.isMultiple()) {
                argElement
                    .setAttribute(
                        MetadataElementLoader.MULTIPLE_ATTRIBUTE,
                        Optional.of("yes"));
            }

            for (final BehaviorEntity behavior: arg.getBehaviorEntities()) {
                if (firstBehavior) {
                    argElement
                        .setAttribute(
                            MetadataElementLoader.BEHAVIOR_REFERENCE,
                            reference(Optional.of(behavior)));
                    firstBehavior = false;
                } else {
                    final XMLElement behaviorElement = createElement(
                        MetadataElementLoader.BEHAVIOR_REFERENCE);

                    behaviorElement
                        .setAttribute(
                            MetadataElementLoader.BEHAVIOR_REFERENCE,
                            reference(Optional.of(behavior)));

                    argElement.addChild(behaviorElement);
                }
            }

            export(arg.getParams(), argElement);

            element.addChild(argElement);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        return MetadataElementLoader.TRANSFORM_ENTITY;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void registerReferences(final Entity entity)
    {
        super.registerReferences(entity);

        final TransformEntity transformEntity = (TransformEntity) entity;

        registerReference(transformEntity.getEngineEntity());

        for (final TransformEntity.Arg arg: transformEntity.getArgs()) {
            registerReference(arg.getContentEntity());

            for (final BehaviorEntity behavior: arg.getBehaviorEntities()) {
                registerReference(Optional.of(behavior));
            }

            registerReferences(
                arg.getParams(),
                MetadataElementLoader.ARG_ELEMENT);
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
