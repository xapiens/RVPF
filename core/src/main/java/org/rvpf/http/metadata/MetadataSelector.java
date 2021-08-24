/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataSelector.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.http.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Entity;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.config.TopologicalErrorException;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.http.AbstractServlet;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.GroupEntity;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.ParamsEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.metadata.entity.SyncEntity;
import org.rvpf.metadata.entity.TransformEntity;

/**
 * Metadata selector.
 */
public final class MetadataSelector
{
    /**
     * Accepts a request document.
     *
     * @param requestDocument The request document.
     *
     * @throws AbstractServlet.BadRequestException When appropriate.
     */
    void acceptRequestDocument(
            @Nonnull final XMLDocument requestDocument)
        throws AbstractServlet.BadRequestException
    {
        _rootElement = requestDocument.getRootElement();

        _after = _toStamp(
            _rootElement
                .getAttributeValue(
                    MetadataFilter.AFTER_ATTRIBUTE,
                    Optional.empty()));
        _domain = _rootElement
            .getAttributeValue(
                MetadataFilter.DOMAIN_ATTRIBUTE,
                Optional.empty())
            .orElse(null);

        for (final XMLElement attributesElement:
                _rootElement.getChildren(MetadataFilter.ATTRIBUTES_ELEMENT)) {
            final Optional<String> usage = attributesElement
                .getAttributeValue(USAGE_ATTRIBUTE, Optional.empty());

            if (usage.isPresent()) {
                _usages.add(usage.get().trim().toLowerCase(Locale.ROOT));
            }
        }

        for (final XMLElement textsElement:
                _rootElement.getChildren(MetadataFilter.TEXTS_ELEMENT)) {
            final Optional<String> lang = textsElement
                .getAttributeValue(LANG_ATTRIBUTE, Optional.empty());

            if (lang.isPresent()) {
                _langs.add(lang.get().trim().toLowerCase(Locale.ROOT));
            }
        }

        _includeProperties = !_rootElement
            .getChildren(MetadataFilter.PROPERTIES_ELEMENT)
            .isEmpty();
        _includeReplicates = !_rootElement
            .getChildren(MetadataFilter.REPLICATES_ELEMENT)
            .isEmpty();

        _contentsSelectors = _acceptSelectors(
            MetadataFilter.CONTENTS_ELEMENT,
            CONTENT_ENTITY);
        _enginesSelectors = _acceptSelectors(
            MetadataFilter.ENGINES_ELEMENT,
            ENGINE_ENTITY);
        _groupsSelectors = _acceptSelectors(
            MetadataFilter.GROUPS_ELEMENT,
            GROUP_ENTITY);
        _inputsSelectors = _acceptSelectors(
            MetadataFilter.INPUTS_ELEMENT,
            POINT_ENTITY);
        _originsSelectors = _acceptSelectors(
            MetadataFilter.ORIGINS_ELEMENT,
            ORIGIN_ENTITY);
        _pointsSelectors = _acceptSelectors(
            MetadataFilter.POINTS_ELEMENT,
            POINT_ENTITY);
        _resultsSelectors = _acceptSelectors(
            MetadataFilter.RESULTS_ELEMENT,
            POINT_ENTITY);
        _storesSelectors = _acceptSelectors(
            MetadataFilter.STORES_ELEMENT,
            STORE_ENTITY);
        _syncsSelectors = _acceptSelectors(
            MetadataFilter.SYNCS_ELEMENT,
            SYNC_ENTITY);
        _transformsSelectors = _acceptSelectors(
            MetadataFilter.TRANSFORMS_ELEMENT,
            TRANSFORM_ENTITY);

        _rootElement = null;
    }

    /**
     * Gets the value of the 'domain' attribute.
     *
     * @return The optional value of the 'domain' attribute.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getDomain()
    {
        return Optional.ofNullable(_domain);
    }

    /**
     * Gets the languages to include for information texts.
     *
     * @return The languages to include for information texts.
     */
    @Nonnull
    @CheckReturnValue
    Set<String> getLangs()
    {
        return Require.notNull(_langs);
    }

    /**
     * Gets the usages to include for attributes.
     *
     * @return The usages to include for attributes.
     */
    @Nonnull
    @CheckReturnValue
    Set<String> getUsages()
    {
        return Require.notNull(_usages);
    }

    /**
     * Selects metadata from a master.
     *
     * @param masterMetadata The master metadata.
     *
     * @return The optional selected metadata.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Metadata> selectFrom(@Nonnull final Metadata masterMetadata)
    {
        final Collection<? extends Entity> groups;
        Collection<? extends Entity> contents;
        Collection<? extends Entity> engines;
        Collection<? extends Entity> origins;
        Collection<? extends Entity> stores;
        Collection<? extends Entity> syncs;
        Collection<? extends Entity> transforms;

        // Returns null if the medatata is unmodified.

        if (_after != null) {
            final Optional<DateTime> stamp = masterMetadata.getStamp();

            if (stamp.isPresent() && stamp.get().isNotAfter(_after)) {
                return Optional.empty();
            }
        }

        // Initializes new metadata.

        _masterMetadata = masterMetadata;

        _selectedMetadata = new Metadata(_masterMetadata.getConfig());
        _selectedMetadata.updateStamp(_masterMetadata.getStamp());
        _selectedMetadata.setDomain(_masterMetadata.getDomain());

        if (_includeProperties) {
            _selectedMetadata
                .getProperties()
                .add(_masterMetadata.getProperties());
        }

        _setEntities(_masterMetadata.getEntities().orElse(null));

        // Selects reference entities.

        contents = _getSelected(
            _contentsSelectors,
            ContentEntity.class,
            ContentEntity.ENTITY_PREFIX,
            _contentsSelectors.needAll()
            ? _masterMetadata.getContentEntities(): null);
        engines = _getSelected(
            _enginesSelectors,
            EngineEntity.class,
            EngineEntity.ENTITY_PREFIX,
            _enginesSelectors.needAll()
            ? _masterMetadata.getEngineEntities(): null);
        groups = _getSelected(
            _groupsSelectors,
            GroupEntity.class,
            GroupEntity.ENTITY_PREFIX,
            _groupsSelectors.needAll()
            ? _masterMetadata.getGroupEntities(): null);
        origins = _getSelected(
            _originsSelectors,
            OriginEntity.class,
            OriginEntity.ENTITY_PREFIX,
            _originsSelectors.needAll()
            ? _masterMetadata.getOriginEntities(): null);
        stores = _getSelected(
            _storesSelectors,
            StoreEntity.class,
            StoreEntity.ENTITY_PREFIX,
            _storesSelectors.needAll()
            ? _masterMetadata.getStoreEntities(): null);
        syncs = _getSelected(
            _syncsSelectors,
            SyncEntity.class,
            SyncEntity.ENTITY_PREFIX,
            _syncsSelectors.needAll()? _masterMetadata.getSyncEntities(): null);
        transforms = _getSelected(
            _transformsSelectors,
            TransformEntity.class,
            TransformEntity.ENTITY_PREFIX,
            _transformsSelectors.needAll()? _masterMetadata
                .getTransformEntities(): null);
        transforms = _filterTransforms(transforms, engines);

        // Selects points.

        if (_pointsSelectors.arePresent()
                || _inputsSelectors.arePresent()
                || _resultsSelectors.arePresent()) {
            final Collection<? extends Entity> inputs = _getSelected(
                _inputsSelectors,
                PointEntity.Definition.class,
                PointEntity.ENTITY_PREFIX,
                _inputsSelectors.needAll()? _masterMetadata
                    .getPointsCollection(): null);
            final Collection<? extends Entity> results = _getSelected(
                _resultsSelectors,
                PointEntity.Definition.class,
                PointEntity.ENTITY_PREFIX,
                _resultsSelectors.needAll()? _masterMetadata
                    .getPointsCollection(): null);
            final Collection<PointEntity> points = _getClonedPoints(
                contents,
                origins,
                stores,
                syncs,
                transforms,
                groups,
                inputs,
                results);

            if (contents != null) {
                for (final Entity entity: contents) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addContentEntity(Optional.of((ContentEntity) entity));
                }
            }

            if (engines != null) {
                for (final Entity entity: engines) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addEngineEntity(Optional.of((EngineEntity) entity));
                }
            }

            if (origins != null) {
                for (final Entity entity: origins) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addOriginEntity(Optional.of((OriginEntity) entity));
                }
            }

            if (stores != null) {
                for (final Entity entity: stores) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addStoreEntity(Optional.of((StoreEntity) entity));
                }
            }

            if (syncs != null) {
                for (final Entity entity: syncs) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addSyncEntity(Optional.of((SyncEntity) entity));
                }
            }

            if (transforms != null) {
                for (final Entity entity: transforms) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addTransformEntity(
                            Optional.of((TransformEntity) entity));
                }
            }

            for (final PointEntity point: points) {
                _putEntity(point);
                _selectedMetadata.addPointEntity(Optional.of(point));

                for (final String alias:
                        _masterMetadata.getPointAliases(point)) {
                    _selectedMetadata.addPointAlias(point, alias);
                }
            }
        } else {    // No points.
            if (_contentsSelectors.arePresent()) {
                if (contents == null) {
                    contents = _masterMetadata.getContentEntities();
                }

                for (final Entity entity: contents) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addContentEntity(Optional.of((ContentEntity) entity));
                }
            }

            if (_enginesSelectors.arePresent()) {
                if (engines == null) {
                    engines = _masterMetadata.getEngineEntities();
                }

                for (final Entity entity: engines) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addEngineEntity(Optional.of((EngineEntity) entity));
                }
            }

            if (_originsSelectors.arePresent()) {
                if (origins == null) {
                    origins = _masterMetadata.getOriginEntities();
                }

                for (final Entity entity: origins) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addOriginEntity(Optional.of((OriginEntity) entity));
                }
            }

            if (_storesSelectors.arePresent()) {
                if (stores == null) {
                    stores = _masterMetadata.getStoreEntities();
                }

                for (final Entity entity: stores) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addStoreEntity(Optional.of((StoreEntity) entity));
                }
            }

            if (_syncsSelectors.arePresent()) {
                if (syncs == null) {
                    syncs = _masterMetadata.getSyncEntities();
                }

                for (final Entity entity: syncs) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addSyncEntity(Optional.of((SyncEntity) entity));
                }
            }

            if (_transformsSelectors.arePresent()) {
                if (transforms == null) {
                    transforms = _masterMetadata.getTransformEntities();
                }

                for (final Entity entity: transforms) {
                    _putEntity(entity);
                    _selectedMetadata
                        .addTransformEntity(
                            Optional.of((TransformEntity) entity));
                }
            }
        }

        if (_groupsSelectors.arePresent()) {
            for (final Entity entity: _getClonedGroups(groups)) {
                _selectedMetadata
                    .addGroupEntity(Optional.of((GroupEntity) entity));
                _putEntity(entity);
            }
        }

        final Metadata selectedMetadata = _selectedMetadata;

        _masterMetadata = null;
        _selectedMetadata = null;

        selectedMetadata.keepEntities(Optional.ofNullable(_entities));

        return Optional.of(selectedMetadata);
    }

    private static void _addGroupMember(
            final Optional<? extends Entity> member,
            final GroupEntity group)
    {
        if (member.isPresent()) {
            try {
                Require.success(group.addMember(member.get()));
            } catch (final TopologicalErrorException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private static DateTime _toStamp(
            final Optional<String> valueString)
        throws AbstractServlet.BadRequestException
    {
        try {
            return valueString
                .isPresent()? DateTime.now().valueOf(valueString.get()): null;
        } catch (final IllegalArgumentException exception) {
            throw new AbstractServlet.BadRequestException(
                "Bad stamp: " + exception.getMessage());
        }
    }

    private _Selectors _acceptSelectors(
            final String elementName,
            final String entityName)
        throws AbstractServlet.BadRequestException
    {
        final _Selectors selectors = new _Selectors();

        for (final XMLElement entities: _rootElement.getChildren(elementName)) {
            final _Selector selector = new _Selector();
            Optional<String> value;

            value = entities
                .getAttributeValue(WILD_ATTRIBUTE, Optional.empty());

            if (value.isPresent()) {
                selector.setWild(ValueConverter.wildToPattern(value.get()));
            }

            value = entities
                .getAttributeValue(REGEXP_ATTRIBUTE, Optional.empty());

            if (value.isPresent()) {
                selector
                    .setRegexp(
                        Pattern.compile(value.get(), Pattern.CASE_INSENSITIVE));
            }

            selector
                .setGroup(
                    entities
                        .getAttributeValue(GROUP_ATTRIBUTE, Optional.empty()));

            for (final XMLElement entity: entities.getChildren(entityName)) {
                final Optional<String> reference = entity
                    .getAttributeValue(entityName, Optional.empty());

                if (!reference.isPresent()) {
                    throw new AbstractServlet.BadRequestException(
                        "A '" + entityName + "' should have a '" + entityName
                        + "' attribute");
                }

                selector.addReference(reference.get());
            }

            selectors.add(selector);
        }

        return selectors;
    }

    private GroupEntity _cloneGroup(final GroupEntity originalGroup)
    {
        final GroupEntity clonedGroup = originalGroup.copy();

        for (final Entity member: originalGroup.getMembers()) {
            switch (member.getPrefix()) {
                case ContentEntity.ENTITY_PREFIX: {
                    _addGroupMember(
                        _selectedMetadata.getContentEntity(member.getName()),
                        clonedGroup);

                    break;
                }
                case OriginEntity.ENTITY_PREFIX: {
                    _addGroupMember(
                        _selectedMetadata.getOriginEntity(member.getName()),
                        clonedGroup);

                    break;
                }
                case StoreEntity.ENTITY_PREFIX: {
                    _addGroupMember(
                        _selectedMetadata.getStoreEntity(member.getName()),
                        clonedGroup);

                    break;
                }
                case EngineEntity.ENTITY_PREFIX: {
                    _addGroupMember(
                        _selectedMetadata.getEngineEntity(member.getName()),
                        clonedGroup);

                    break;
                }
                case TransformEntity.ENTITY_PREFIX: {
                    _addGroupMember(
                        _selectedMetadata.getTransformEntity(member.getName()),
                        clonedGroup);

                    break;
                }
                case PointEntity.ENTITY_PREFIX: {
                    _addGroupMember(
                        _selectedMetadata
                            .getPointByUUID(member.getUUID().get()),
                        clonedGroup);

                    break;
                }
                case GroupEntity.ENTITY_PREFIX: {
                    _addGroupMember(
                        Optional.of(_cloneGroup((GroupEntity) member)),
                        clonedGroup);

                    break;
                }
                default: {
                    throw new InternalError();
                }
            }
        }

        return clonedGroup;
    }

    private Collection<PointEntity> _clonePoints(
            final Set<PointEntity> selectedPoints)
    {
        for (final PointEntity selectedPoint: selectedPoints) {
            final PointEntity clonedPoint = _clonedPoint(selectedPoint);

            if (_inputsSelectors.arePresent()) {
                for (final PointRelation relation: selectedPoint.getInputs()) {
                    clonedPoint.addInputRelation(_cloneRelation(relation));
                }
            }

            if (_resultsSelectors.arePresent()) {
                for (final PointRelation relation: selectedPoint.getResults()) {
                    if (!_inputsSelectors.arePresent()
                            || !selectedPoints.contains(
                                relation.getResultPoint())) {
                        final PointInput clonedRelation = _cloneRelation(
                            relation);

                        clonedRelation
                            .getResultPointEntity()
                            .addInputRelation(clonedRelation);
                    }
                }
            }

            if (_includeReplicates) {
                for (final Point.Replicate replicate:
                        selectedPoint.getReplicates()) {
                    clonedPoint
                        .addReplicate(
                            new Point.Replicate(
                                _clonedPoint(replicate.getPoint()),
                                replicate.getConvert()));
                }
            }
        }

        return _clonedPointsMap.values();
    }

    private PointInput _cloneRelation(final PointRelation relation)
    {
        final PointInput clonedRelation = new PointInput((PointInput) relation);

        clonedRelation
            .setInputPoint(_clonedPoint(clonedRelation.getInputPoint()));
        clonedRelation
            .setResultPoint(_clonedPoint(clonedRelation.getResultPoint()));

        if (!_syncsSelectors.arePresent()) {
            clonedRelation.setSyncEntity(Optional.empty());
        }

        return clonedRelation;
    }

    private PointEntity _clonedPoint(final Point point)
    {
        if (point == null) {
            return null;
        }

        PointEntity clonedPoint = _clonedPointsMap.get(point.getUUID().get());

        if (clonedPoint == null) {
            clonedPoint = ((PointEntity) point).copy();

            if (_contentsSelectors.arePresent()) {
                _selectedMetadata
                    .addContentEntity(clonedPoint.getContentEntity());
            } else {
                clonedPoint.clearContentEntity();
            }

            if (_originsSelectors.arePresent()) {
                _selectedMetadata
                    .addOriginEntity(clonedPoint.getOriginEntity());
            } else {
                clonedPoint.setOriginEntity(Optional.empty());
            }

            if (_storesSelectors.arePresent()) {
                _selectedMetadata.addStoreEntity(clonedPoint.getStoreEntity());
            } else {
                clonedPoint.setStoreEntity(Optional.empty());
            }

            if (_syncsSelectors.arePresent()) {
                _selectedMetadata.addSyncEntity(clonedPoint.getSyncEntity());
            } else {
                clonedPoint.setSyncEntity(Optional.empty());
            }

            if (_transformsSelectors.arePresent()) {
                _selectedMetadata
                    .addTransformEntity(clonedPoint.getTransformEntity());
            } else {
                clonedPoint.clearTransformEntity();
            }

            clonedPoint.resetInputs();
            clonedPoint.resetResults();
            clonedPoint.resetReplicates();

            _clonedPointsMap.put(point.getUUID().get(), clonedPoint);
        }

        return clonedPoint;
    }

    private Collection<? extends Entity> _filterTransforms(
            Collection<? extends Entity> transforms,
            final Collection<? extends Entity> engines)
    {
        if (engines == null) {
            return transforms;
        }

        if (transforms == null) {
            if (_transformsSelectors.arePresent()
                    || _pointsSelectors.arePresent()) {
                transforms = _masterMetadata.getTransformEntities();
            } else {
                return null;
            }
        }

        final Set<Entity> filter = new HashSet<>(engines);
        final Collection<Entity> filtered = new LinkedList<>();

        for (final Entity entity: transforms) {
            final TransformEntity transform = (TransformEntity) entity;

            if (filter.contains(transform.getEngineEntity().get())) {
                filtered.add(transform);
            }
        }

        return filtered;
    }

    private Collection<GroupEntity> _getClonedGroups(
            Collection<? extends Entity> selectedGroups)
    {
        final Collection<GroupEntity> clonedGroups = new LinkedList<>();

        if (selectedGroups == null) {
            selectedGroups = _masterMetadata.getGroupEntities();
        }

        for (final Entity entity: selectedGroups) {
            clonedGroups.add(_cloneGroup((GroupEntity) entity));
        }

        return clonedGroups;
    }

    private Collection<PointEntity> _getClonedPoints(
            final Collection<? extends Entity> contents,
            final Collection<? extends Entity> origins,
            final Collection<? extends Entity> stores,
            final Collection<? extends Entity> syncs,
            final Collection<? extends Entity> transforms,
            final Collection<? extends Entity> groups,
            final Collection<? extends Entity> inputs,
            final Collection<? extends Entity> results)
    {
        final Collection<PointEntity> clonedPoints;

        // Gets the result points from the selected inputs.

        final Set<PointEntity> inputsResults;

        if (_inputsSelectors.areSelective()) {    // Inputs are selective.
            inputsResults = new HashSet<>();

            for (final Entity input: inputs) {
                for (final PointRelation relation:
                        ((PointEntity) input).getResults()) {
                    inputsResults.add((PointEntity) relation.getResultPoint());
                }
            }
        } else {
            inputsResults = null;
        }

        // Gets the input points from the selected results.

        final Set<PointEntity> resultsInputs;

        if (_resultsSelectors.areSelective()) {    // Results are selective.
            resultsInputs = new HashSet<>();

            for (final Entity result: results) {
                for (final PointRelation relation:
                        ((PointEntity) result).getInputs()) {
                    resultsInputs.add((PointEntity) relation.getInputPoint());
                }
            }
        } else {
            resultsInputs = null;
        }

        // Gets the explicitly selected points.

        final Collection<Point> allPoints = ((inputs != null)
                || _pointsSelectors.needAll())? _masterMetadata
                    .getPointsCollection(): null;
        Collection<? extends Entity> pointsCollection;

        pointsCollection = _getSelected(
            _pointsSelectors,
            PointEntity.Definition.class,
            PointEntity.ENTITY_PREFIX,
            allPoints);

        if (pointsCollection == null) {
            pointsCollection = (allPoints != null)? allPoints: _masterMetadata
                .getPointsCollection();
        }

        // Filters the selected points.

        final Set<PointEntity> selectedPoints = new HashSet<>();

        for (final Entity entity: pointsCollection) {
            final PointEntity point = (PointEntity) entity;

            if (_contentsSelectors.areSelective()
                    && !contents.contains(
                        point.getContentEntity().orElse(null))) {
                continue;
            }

            if (_originsSelectors.areSelective()
                    && !origins.contains(
                        point.getOriginEntity().orElse(null))) {
                continue;
            }

            if (_storesSelectors.areSelective()
                    && !stores.contains(point.getStoreEntity().orElse(null))) {
                continue;
            }

            if (_syncsSelectors.areSelective()
                    && !syncs.contains(point.getSyncEntity().orElse(null))) {
                continue;
            }

            if (_transformsSelectors.areSelective()
                    && !transforms.contains(
                        point.getTransformEntity().orElse(null))) {
                continue;
            }

            if (_groupsSelectors.areSelective()) {
                for (final Entity groupEntity: groups) {
                    if (((GroupEntity) groupEntity).contains(point, true)) {
                        break;
                    }
                }

                continue;
            }

            if ((inputsResults != null)
                    && (inputs != null)
                    && !inputsResults.contains(point)
                    && !inputs.contains(point.getSyncEntity().orElse(null))) {
                continue;
            }

            if ((resultsInputs != null) && !resultsInputs.contains(point)) {
                continue;
            }

            selectedPoints.add(point);
        }

        _clonedPointsMap = new HashMap<>(selectedPoints.size() * 2);
        clonedPoints = _clonePoints(selectedPoints);
        _clonedPointsMap = null;

        return clonedPoints;
    }

    private ParamsEntity _getEntity(String key, final String prefix)
    {
        final Map<String, Entity> entities = _masterMetadata
            .getEntities()
            .get();
        ParamsEntity entity;

        key = key.trim().toUpperCase(Locale.ROOT);

        if (UUID.isUUID(key)) {
            entity = (ParamsEntity) entities.get(key);
        } else {
            entity = (ParamsEntity) entities.get(prefix + key);

            if ((entity != null) && !prefix.equals(entity.getPrefix())) {
                entity = null;
            }
        }

        return entity;
    }

    private Collection<? extends Entity> _getSelected(
            final _Selectors selectors,
            final Class<? extends Entity> entityClass,
            final String prefix,
            final Collection<? extends Entity> collection)
    {
        if (!selectors.arePresent()) {
            return null;
        }

        if (!selectors.areSelective()) {
            return collection;
        }

        final Collection<Entity> selected = new LinkedHashSet<Entity>();

        for (final _Selector selector: selectors) {
            final Collection<? extends Entity> groupMembers;

            for (final String ref: selector.getReferences()) {
                final ParamsEntity entity = _getEntity(ref, prefix);

                if (entity != null) {
                    selected.add(entity);
                }
            }

            if (selector.getGroup().isPresent()) {
                final Optional<GroupEntity> group = _masterMetadata
                    .getGroupEntity(selector.getGroup());

                groupMembers = group
                    .isPresent()? group
                        .get()
                        .getMembers(entityClass, true): null;
            } else {
                groupMembers = null;
            }

            if (selector.usesPatterns()) {
                final Collection<? extends Entity> entities = (selector
                    .getGroup()
                    .isPresent())? groupMembers: collection;

                if (entities != null) {
                    for (final Entity entity: entities) {
                        final String entityName = entity.getName().get();

                        if (selector.matches(entityName)) {
                            selected.add(entity);
                        }
                    }
                }
            } else if (groupMembers != null) {
                selected.addAll(groupMembers);
            }
        }

        return selected;
    }

    private void _putEntity(final Entity entity)
    {
        final String name = entity.getName().orElse(null);

        if (name != null) {
            _entities.put(entity.getPrefix() + name, entity);
        } else {
            final UUID uuid = entity.getUUID().get();

            _entities.put(uuid.toRawString(), entity);
        }
    }

    @SuppressWarnings("unchecked")
    private void _setEntities(final HashMap<String, Entity> entities)
    {
        _entities = (HashMap<String, Entity>) entities.clone();
    }

    /** Content entity. */
    public static final String CONTENT_ENTITY = "content";

    /** Engine entity. */
    public static final String ENGINE_ENTITY = "engine";

    /** Group attribute. */
    public static final String GROUP_ATTRIBUTE = "group";

    /** Group entity. */
    public static final String GROUP_ENTITY = "group";

    /** Lang attribute. */
    public static final String LANG_ATTRIBUTE = "lang";

    /** Origin entity. */
    public static final String ORIGIN_ENTITY = "origin";

    /** Point entity. */
    public static final String POINT_ENTITY = "point";

    /** Regexp attribute. */
    public static final String REGEXP_ATTRIBUTE = "regexp";

    /** Store entity. */
    public static final String STORE_ENTITY = "store";

    /** Sync entity. */
    public static final String SYNC_ENTITY = "sync";

    /** Transform entity. */
    public static final String TRANSFORM_ENTITY = "transform";

    /** Usage attribute. */
    public static final String USAGE_ATTRIBUTE = "usage";

    /** Wild attribute. */
    public static final String WILD_ATTRIBUTE = "wild";

    private DateTime _after;
    private Map<UUID, PointEntity> _clonedPointsMap;
    private _Selectors _contentsSelectors;
    private String _domain;
    private _Selectors _enginesSelectors;
    private HashMap<String, Entity> _entities;
    private _Selectors _groupsSelectors;
    private boolean _includeProperties;
    private boolean _includeReplicates;
    private _Selectors _inputsSelectors;
    private final Set<String> _langs = new TreeSet<String>();;
    private Metadata _masterMetadata;
    private _Selectors _originsSelectors;
    private _Selectors _pointsSelectors;
    private _Selectors _resultsSelectors;
    private XMLElement _rootElement;
    private Metadata _selectedMetadata;
    private _Selectors _storesSelectors;
    private _Selectors _syncsSelectors;
    private _Selectors _transformsSelectors;
    private final Set<String> _usages = new TreeSet<String>();

    /**
     * Selector.
     */
    private static final class _Selector
    {
        /**
         * Constructs an instance.
         */
        _Selector() {}

        /**
         * Adds a reference.
         *
         * @param reference The reference.
         */
        void addReference(@Nonnull final String reference)
        {
            _references.add(reference);
        }

        /**
         * Gets the group.
         *
         * @return The optional group.
         */
        @Nonnull
        @CheckReturnValue
        Optional<String> getGroup()
        {
            return _group;
        }

        /**
         * Gets the references.
         *
         * @return The references.
         */
        @Nonnull
        @CheckReturnValue
        List<String> getReferences()
        {
            return _references;
        }

        /**
         * Asks if selective.
         *
         * @return True if selective.
         */
        @CheckReturnValue
        boolean isSelective()
        {
            return usesPatterns()
                   || _group.isPresent()
                   || !_references.isEmpty();
        }

        /**
         * Asks if a name matches.
         *
         * @param name The name.
         *
         * @return True if the name matches.
         */
        @CheckReturnValue
        boolean matches(@Nonnull final String name)
        {
            return ((_regexp != null) && _regexp.matcher(name).matches())
                   || ((_wild != null) && _wild.matcher(name).matches());
        }

        /**
         * Asks if all is needed.
         *
         * @return True if all is needed.
         */
        @CheckReturnValue
        boolean needsAll()
        {
            return !_group.isPresent() && usesPatterns();
        }

        /**
         * Sets the group.
         *
         * @param group The group.
         */
        void setGroup(@Nonnull final Optional<String> group)
        {
            _group = group;
        }

        /**
         * Sets the regular expression.
         *
         * @param regexp The regular expression.
         */
        void setRegexp(@Nonnull final Pattern regexp)
        {
            _regexp = regexp;
        }

        /**
         * Sets the wild pattern.
         *
         * @param wild The wild pattern.
         */
        void setWild(@Nonnull final Pattern wild)
        {
            _wild = wild;
        }

        /**
         * Asks if patterns are used.
         *
         * @return True if pattern are used.
         */
        @CheckReturnValue
        boolean usesPatterns()
        {
            return (_wild != null) || (_regexp != null);
        }

        private Optional<String> _group = Optional.empty();
        private final List<String> _references = new LinkedList<>();
        private Pattern _regexp;
        private Pattern _wild;
    }


    /**
     * Selectors.
     */
    private static final class _Selectors
        implements Iterable<_Selector>
    {
        /**
         * Constructs an instance.
         */
        _Selectors() {}

        @Override
        public Iterator<_Selector> iterator()
        {
            return _selectors.iterator();
        }

        /**
         * Adds a selector.
         *
         * @param selector The selector.
         */
        void add(@Nonnull final _Selector selector)
        {
            _selectors.add(selector);
            _selective |= selector.isSelective();
            _needAll |= selector.needsAll();
        }

        /**
         * Asks if selectors are present.
         *
         * @return True if selectors are present.
         */
        @CheckReturnValue
        boolean arePresent()
        {
            return !_selectors.isEmpty();
        }

        /**
         * Asks if any selector is selective.
         *
         * @return True if any selector is selective.
         */
        @CheckReturnValue
        boolean areSelective()
        {
            return _selective;
        }

        /**
         * Asks if any selector needs all or none are selective.
         *
         * @return True if any selector needs all or none are selective.
         */
        @CheckReturnValue
        boolean needAll()
        {
            return _needAll || !_selective;
        }

        private boolean _needAll;
        private boolean _selective;
        private final List<_Selector> _selectors = new LinkedList<>();
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
