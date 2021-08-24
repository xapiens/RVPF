/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: GroupLoader.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Entity;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.config.TopologicalErrorException;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.GroupEntity;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.service.ServiceMessages;

/**
 * Group Entry.
 */
final class GroupLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        if (!getMetadataFilter().areGroupsNeeded()) {
            return;
        }

        final GroupEntity.Builder groupBuilder = GroupEntity.newBuilder();

        setUpEntityBuilder(groupBuilder);

        final GroupEntity groupEntity = groupBuilder.build();

        if (!groupEntity.getName().isPresent()) {
            throw new ValidationException(ServiceMessages.GROUP_NEEDS_NAME);
        }

        addGroupMember(groupEntity);

        final DocumentElement groupElement = getElement();

        for (final DocumentElement memberElement:
                groupElement.getChildren(MEMBER_ELEMENT)) {
            if (!memberElement.isEnabled()) {
                continue;
            }

            if (_addMember(
                    groupEntity,
                    memberElement,
                    PointEntity.ENTITY_REFERENCE_NAME,
                    PointEntity.ENTITY_PREFIX)) {
                continue;
            }

            if (_addMember(
                    groupEntity,
                    memberElement,
                    OriginEntity.ENTITY_REFERENCE_NAME,
                    OriginEntity.ENTITY_PREFIX)) {
                continue;
            }

            if (_addMember(
                    groupEntity,
                    memberElement,
                    StoreEntity.ENTITY_REFERENCE_NAME,
                    StoreEntity.ENTITY_PREFIX)) {
                continue;
            }

            if (_addMember(
                    groupEntity,
                    memberElement,
                    ContentEntity.ENTITY_REFERENCE_NAME,
                    ContentEntity.ENTITY_PREFIX)) {
                continue;
            }

            if (_addMember(
                    groupEntity,
                    memberElement,
                    TransformEntity.ENTITY_REFERENCE_NAME,
                    TransformEntity.ENTITY_PREFIX)) {
                continue;
            }

            if (_addMember(
                    groupEntity,
                    memberElement,
                    EngineEntity.ENTITY_REFERENCE_NAME,
                    EngineEntity.ENTITY_PREFIX)) {
                continue;
            }

            _addMember(
                groupEntity,
                memberElement,
                GroupEntity.ENTITY_REFERENCE_NAME,
                GroupEntity.ENTITY_PREFIX);
        }

        getMetadata().addGroupEntity(Optional.of(groupEntity));
        anchorEntity(groupEntity);
        putEntity(groupEntity);
    }

    /** {@inheritDoc}
     */
    @Override
    String getEntityPrefix()
    {
        return GroupEntity.ENTITY_PREFIX;
    }

    private boolean _addMember(
            final GroupEntity group,
            final DocumentElement element,
            final String attributeName,
            final String prefix)
        throws ValidationException
    {
        final Optional<String> attribute = element
            .getAttributeValue(attributeName, Optional.empty());
        final Entity member = attribute
            .isPresent()? getEntity(attribute.get(), prefix): null;

        if (member == null) {
            return false;
        }

        final boolean added;

        try {
            added = group.addMember(member);
        } catch (final TopologicalErrorException exception) {
            throw new ValidationException(
                BaseMessages.VERBATIM,
                exception.getMessage());
        }

        if (!added) {
            getLogger()
                .warn(
                    ServiceMessages.DUPLICATE_GROUP_ENTRY,
                    attributeName,
                    member.getName(),
                    group.getName());
        }

        return true;
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
