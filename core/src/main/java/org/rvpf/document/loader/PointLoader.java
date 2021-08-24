/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointLoader.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.document.loader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.rvpf.base.Entity;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PermissionsEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.metadata.entity.SyncEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.service.ServiceMessages;

/**
 * Point loader.
 */
final class PointLoader
    extends MetadataElementLoader
{
    /** {@inheritDoc}
     */
    @Override
    protected void process()
        throws ValidationException
    {
        final DocumentElement pointElement = getElement();
        final Optional<UUID> uuid = pointElement.getUUID();

        if (!uuid.isPresent()) {
            throw new ValidationException(ServiceMessages.POINT_NEEDS_UUID);
        }

        final Optional<String> name = pointElement
            .getAttributeValue(
                DocumentElement.NAME_ATTRIBUTE,
                Optional.empty());
        final PointEntity point = new PointEntity.Definition();

        setUpEntity(point);

        _forgetReference(uuid.get().toString(), point);

        if (name.isPresent()) {
            _forgetReference(name.get(), point);
        }

        _forgetReference(pointElement.getId().orElse(null), point);

        final Set<String> aliases = new HashSet<String>();

        for (final DocumentElement aliasElement:
                pointElement.getChildren(ALIAS_ELEMENT)) {
            if (!aliasElement.isEnabled()) {
                continue;
            }

            final String alias = aliasElement
                .getAttributeValue(DocumentElement.NAME_ATTRIBUTE);

            aliases.add(alias);
            _forgetReference(alias, point);
        }

        final MetadataFilter filter = getMetadataFilter();
        Optional<String> originKey = pointElement
            .getAttributeValue(ORIGIN_REFERENCE, Optional.empty());

        if (!originKey.isPresent()) {
            originKey = pointElement
                .getAttributeValue(PROCESSOR_REFERENCE, Optional.empty());

            if (!originKey.isPresent() && filter.areOriginsRequired()) {
                originKey = Optional.of(UNSPECIFIED_ORIGIN);
            }
        }

        if (originKey.isPresent()) {
            final OriginEntity originEntity = (OriginEntity) getEntity(
                originKey.get(),
                OriginEntity.ENTITY_PREFIX,
                filter.areOriginsFiltered())
                .orElse(null);

            if (originEntity != null) {
                point.setOriginEntity(Optional.of(originEntity));
                Require.success(originEntity.addPointEntity(point));
            }
        }

        final StoreEntity storeEntity;

        {
            final Optional<String> referenceAttribute = pointElement
                .getAttributeValue(
                    STORE_REFERENCE,
                    filter.areStoresRequired()? Optional
                        .of(UNSPECIFIED_STORE): Optional.empty());

            storeEntity = referenceAttribute
                .isPresent()? (StoreEntity) getEntity(
                    referenceAttribute.get(),
                    StoreEntity.ENTITY_PREFIX,
                    filter.areStoresFiltered())
                    .orElse(null): null;
        }

        if (storeEntity != null) {
            point.setStoreEntity(Optional.of(storeEntity));
            Require.success(storeEntity.addPointReference(point));
        }

        if (filter.areContentsNeeded()) {
            final Optional<String> defaultReference = filter
                .areContentsRequired()? Optional
                    .of(UNSPECIFIED_CONTENT): Optional.empty();
            final Optional<String> referenceAttribute = pointElement
                .getAttributeValue(CONTENT_REFERENCE, defaultReference);
            final ContentEntity contentEntity = referenceAttribute
                .isPresent()? (ContentEntity) getEntity(
                    referenceAttribute.get(),
                    ContentEntity.ENTITY_PREFIX): null;

            if (contentEntity != null) {
                point.setContentEntity(contentEntity);
            }
        }

        if (filter.areSyncsNeeded()) {
            final Optional<String> referenceAttribute = pointElement
                .getAttributeValue(SYNC_REFERENCE, Optional.empty());
            final SyncEntity syncEntity = referenceAttribute
                .isPresent()? (SyncEntity) getEntity(
                    referenceAttribute.get(),
                    SyncEntity.ENTITY_PREFIX): null;

            point.setSyncEntity(Optional.ofNullable(syncEntity));
        }

        if (filter.arePermissionsNeeded()) {
            final Optional<String> referenceAttribute = pointElement
                .getAttributeValue(PERMISSIONS_REFERENCE, Optional.empty());
            final PermissionsEntity permissionsEntity = referenceAttribute
                .isPresent()? (PermissionsEntity) getEntity(
                    referenceAttribute.get(),
                    PermissionsEntity.ENTITY_PREFIX): null;

            point.setPermissionsEntity(Optional.ofNullable(permissionsEntity));
        }

        point.setParams(getParams(pointElement, POINT_ENTITY));

        point.setAttributes(getAttributes(pointElement));

        if (filter.isPointTransformNeeded(point)) {
            final Optional<String> referenceAttribute = pointElement
                .getAttributeValue(
                    TRANSFORM_REFERENCE,
                    filter.areTransformsRequired()? Optional
                        .of(UNSPECIFIED_TRANSFORM): Optional.empty());
            final TransformEntity transformEntity = referenceAttribute
                .isPresent()? (TransformEntity) getEntity(
                    referenceAttribute.get(),
                    TransformEntity.ENTITY_PREFIX): null;

            if (transformEntity != null) {
                if (originKey.isPresent() || !filter.areOriginsNeeded()) {
                    point.setTransformEntity(transformEntity);
                } else {
                    final OriginEntity unspecifiedOrigin =
                        (OriginEntity) getEntity(
                            UNSPECIFIED_ORIGIN,
                            OriginEntity.ENTITY_PREFIX,
                            false)
                            .get();

                    point.setOriginEntity(Optional.of(unspecifiedOrigin));
                }
            }
        }

        if (filter.isPointNeeded(point)) {
            final List<DocumentElement> inputElements = pointElement
                .getChildren(INPUT_ELEMENT);

            if (!inputElements.isEmpty()) {
                if (filter.arePointInputsNeeded(point)) {
                    _addInputs(inputElements, point, aliases, filter);
                } else if (filter.arePointInputsFlagged()) {
                    _flagInputs(inputElements, point, aliases);
                }
            }

            if (filter.arePointReplicatesNeeded()) {
                for (final DocumentElement replicateElement:
                        pointElement.getChildren(REPLICATE_ELEMENT)) {
                    if (!replicateElement.isEnabled()) {
                        continue;
                    }

                    final PointEntity replicatePoint = _getPointEntity(
                        replicateElement,
                        aliases,
                        point);
                    final Optional<Boolean> convert = ValueConverter
                        .convertToBoolean(
                            ServiceMessages.ATTRIBUTE_TYPE.toString(),
                            CONVERT_ATTRIBUTE,
                            replicateElement
                                .getAttributeValue(
                                        CONVERT_ATTRIBUTE,
                                                Optional.empty()),
                            Optional.empty());

                    if (replicatePoint == point) {
                        throw new ValidationException(
                            ServiceMessages.SELF_REFERENCE_REPLICATE,
                            point);
                    }

                    point
                        .addReplicate(
                            new Point.Replicate(replicatePoint, convert));
                }
            }

            addGroupMember(point);
            getMetadata().addPointEntity(Optional.of(point));
            anchorEntity(point);

            for (final String alias: aliases) {
                getMetadata().addPointAlias(point, alias);
            }
        } else {
            point.setDropped(true);
        }

        putEntity(point);
    }

    /** {@inheritDoc}
     */
    @Override
    String getEntityPrefix()
    {
        return PointEntity.ENTITY_PREFIX;
    }

    private void _addInputs(
            final List<DocumentElement> inputElements,
            final PointEntity point,
            final Collection<String> aliases,
            final MetadataFilter metadataFilter)
        throws ValidationException
    {
        final Optional<TransformEntity> transformEntity = point
            .getTransformEntity();
        final Iterator<TransformEntity.Arg> argsIterator = transformEntity
            .isPresent()? (transformEntity.get().getArgs().iterator()): null;

        if (!transformEntity.isPresent()
                && metadataFilter.areTransformsRequired()
                && metadataFilter.isPointTransformNeeded(point)) {
            throw new ValidationException(
                ServiceMessages.POINT_NEEDS_TRANSFORM,
                point);
        }

        for (final DocumentElement inputElement: inputElements) {
            if (!inputElement.isEnabled()) {
                continue;
            }

            final String sync = inputElement
                .getAttributeValue(SYNC_REFERENCE, Optional.empty())
                .orElse(null);
            final PointEntity inputPoint = _getPointEntity(
                inputElement,
                aliases,
                point);
            final PointInput relation = new PointInput(inputPoint, point);

            relation
                .setControl(
                    inputElement.getAttributeValue(CONTROL_ATTRIBUTE, false));

            if (metadataFilter.areBehaviorsNeeded()) {
                String behaviorReference = inputElement
                    .getAttributeValue(BEHAVIOR_REFERENCE, Optional.empty())
                    .orElse(null);

                if (behaviorReference != null) {
                    relation
                        .addBehaviorEntity(
                            (BehaviorEntity) getEntity(
                                behaviorReference,
                                BehaviorEntity.ENTITY_PREFIX));
                }

                for (final DocumentElement behaviorElement:
                        inputElement.getChildren(BEHAVIOR_REFERENCE)) {
                    if (!behaviorElement.isEnabled()) {
                        continue;
                    }

                    behaviorReference = behaviorElement
                        .getAttributeValue(BEHAVIOR_REFERENCE, Optional.empty())
                        .orElse(null);

                    if (behaviorReference == null) {
                        throw new ValidationException(
                            ServiceMessages.MISSING_ATTRIBUTE_IN,
                            BEHAVIOR_REFERENCE,
                            behaviorElement.getName());

                    }

                    relation
                        .addBehaviorEntity(
                            (BehaviorEntity) getEntity(
                                behaviorReference,
                                BehaviorEntity.ENTITY_PREFIX));
                }
            }

            final TransformEntity.Arg arg = ((argsIterator != null)
                    && argsIterator.hasNext())? argsIterator.next(): null;

            if (metadataFilter.areSyncsNeeded()) {
                final SyncEntity syncEntity;

                if (sync != null) {
                    syncEntity = (SyncEntity) getEntity(
                        sync,
                        SyncEntity.ENTITY_PREFIX);
                } else if (arg != null) {
                    syncEntity = arg.getSyncEntity().orElse(null);
                } else {
                    syncEntity = null;
                }

                if (syncEntity != null) {
                    relation.setSyncEntity(Optional.of(syncEntity));
                }
            }

            relation.setParams(getParams(inputElement, INPUT_ELEMENT));

            point.addInputRelation(relation);
        }
    }

    private void _flagInputs(
            final List<DocumentElement> inputElements,
            final PointEntity point,
            final Collection<String> aliases)
        throws ValidationException
    {
        for (final DocumentElement inputElement: inputElements) {
            _getPointEntity(inputElement, aliases, point).flagResultRelations();
        }
    }

    private void _forgetReference(
            final String pointKey,
            final PointEntity pointDefinition)
        throws ValidationException
    {
        if (pointKey != null) {
            final Optional<? extends Entity> pointReference = getEntity(
                pointKey,
                PointEntity.ENTITY_PREFIX,
                true);

            if (pointReference.isPresent()) {
                if (((PointEntity) pointReference.get()).isDefinition()) {
                    throw new ValidationException(
                        ServiceMessages.POINT_MULTIPLE,
                        pointReference);
                }

                ((PointEntity) pointReference.get())
                    .setDefinition(pointDefinition);
                removeEntity(pointKey, PointEntity.ENTITY_PREFIX);
            }
        }
    }

    private PointEntity _getPointEntity(
            final DocumentElement inputElement,
            final Collection<String> aliases,
            final PointEntity self)
        throws ValidationException
    {
        final String pointKey = inputElement.getAttributeValue(POINT_REFERENCE);
        PointEntity point;

        if (DocumentElement.SELF_REFERENCE.equals(pointKey)) {
            point = self;
        } else {
            point = (PointEntity) getEntity(
                pointKey,
                PointEntity.ENTITY_PREFIX,
                true)
                .orElse(null);

            if (point == null) {
                point = (PointEntity) getMetadata()
                    .getPointByName(pointKey)
                    .orElse(null);
            }

            if (point == null) {
                final UUID selfUUID = self.getUUID().get();

                if (pointKey.equalsIgnoreCase(self.getName().orElse(null))
                        || pointKey.equalsIgnoreCase(selfUUID.toString())
                        || pointKey.equals(inputElement.getId().orElse(null))) {
                    point = self;
                } else {
                    for (final String alias: aliases) {
                        if (pointKey.equalsIgnoreCase(alias)) {
                            point = self;

                            break;
                        }
                    }
                }
            }

            if (point == null) {
                point = new PointEntity.Reference();
                point.setName(Optional.of(pointKey));
                putEntity(pointKey, point);
            }
        }

        return point;
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
