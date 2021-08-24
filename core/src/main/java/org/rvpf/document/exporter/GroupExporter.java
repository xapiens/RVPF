/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: GroupExporter.java 3949 2019-05-03 15:35:40Z SFB $
 */

package org.rvpf.document.exporter;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.document.loader.MetadataElementLoader;
import org.rvpf.metadata.entity.GroupEntity;

/**
 * Group exporter.
 */
final class GroupExporter
    extends MetadataEntityExporter
{
    /**
     * Constructs an instance.
     *
     * @param owner The exporter owning this.
     */
    GroupExporter(@Nonnull final MetadataExporter owner)
    {
        super(owner, GroupEntity.class);
    }

    /** {@inheritDoc}
     */
    @Override
    protected void export(final Entity entity, final XMLElement element)
    {
        final GroupEntity groupEntity = (GroupEntity) entity;

        super.export(entity, element);
        setAnchored(entity, element);

        for (final Entity memberEntity: groupEntity.getMembers()) {
            final XMLElement memberElement = createElement(
                MetadataElementLoader.MEMBER_ELEMENT);

            memberElement
                .setAttribute(
                    memberEntity.getReferenceName(),
                    reference(Optional.of(memberEntity)));
            element.addChild(memberElement);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getElementName()
    {
        return MetadataElementLoader.GROUP_ENTITY;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void registerReferences(final Entity entity)
    {
        super.registerReferences(entity);

        final GroupEntity groupEntity = (GroupEntity) entity;

        for (final Entity memberEntity: groupEntity.getMembers()) {
            registerReference(Optional.of(memberEntity));
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
