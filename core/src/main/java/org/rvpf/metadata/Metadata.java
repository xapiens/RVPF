/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Metadata.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.metadata;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.BaseMessages;
import org.rvpf.base.Entity;
import org.rvpf.base.Point;
import org.rvpf.base.Points;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.IdentityHashSet;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.config.Config;
import org.rvpf.config.ConfigProperties;
import org.rvpf.config.TopologicalErrorException;
import org.rvpf.document.exporter.MetadataExporter;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.GroupEntity;
import org.rvpf.metadata.entity.OriginEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.metadata.entity.SyncEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.service.ServiceMessages;

/**
 * Metadata.
 *
 * <p>Holds the metadata needed by a service for its configuration and
 * activities. It is both a subclass of {@link Config} and the owner of an
 * overriding {@link Config} object.</p>
 */
public final class Metadata
    extends Config
    implements Points
{
    /**
     * Constructs an instance.
     *
     * <p>RVPF processes using the metadata are also provided with a
     * configuration file which may augment or override some parts included in
     * the metadata resource. The Metadata constructor is thus informed of this
     * configuration file to allow the override during properties access.</p>
     *
     * @param config The process configuration.
     */
    public Metadata(@Nonnull final Config config)
    {
        super(
            config.getServiceName(),
            new ConfigProperties(
                ServiceMessages.METADATA_PROPERTIES_TYPE,
                Optional.of(config.getServiceName())));

        setClassLoader(Optional.of(config.getClassLoader()));

        getProperties().setOverrider(config.getProperties());
        updateStamp(config.getStamp());

        _config = config;
    }

    /**
     * Adds a content entity to this metadata.
     *
     * @param contentEntity The optional content entity.
     */
    public void addContentEntity(
            @Nonnull final Optional<ContentEntity> contentEntity)
    {
        if (contentEntity.isPresent()) {
            final Optional<String> name = contentEntity
                .get()
                .getNameInUpperCase();

            if (name.isPresent() && !_contentsByName.containsKey(name.get())) {
                _contentsByName
                    .put(name.get(), new WeakReference<>(contentEntity.get()));
            }
        }
    }

    /**
     * Adds an engine entity to this metadata.
     *
     * @param engineEntity The optional engine entity.
     */
    public void addEngineEntity(
            @Nonnull final Optional<EngineEntity> engineEntity)
    {
        if (engineEntity.isPresent()) {
            final Optional<String> name = engineEntity
                .get()
                .getNameInUpperCase();

            if (name.isPresent() && !_enginesByName.containsKey(name.get())) {
                _enginesByName
                    .put(name.get(), new WeakReference<>(engineEntity.get()));
            }
        }
    }

    /**
     * Adds a group entity to this metadata.
     *
     * @param groupEntity The optional group entity.
     */
    public void addGroupEntity(@Nonnull final Optional<GroupEntity> groupEntity)
    {
        if (groupEntity.isPresent()) {
            final Optional<String> name = groupEntity
                .get()
                .getNameInUpperCase();

            if (name.isPresent() && !_groupsByName.containsKey(name.get())) {
                _groupsByName.put(name.get(), groupEntity.get());
            }
        }
    }

    /**
     * Adds an origin entity to this metadata.
     *
     * @param originEntity The optional origin entity.
     */
    public void addOriginEntity(
            @Nonnull final Optional<OriginEntity> originEntity)
    {
        if (originEntity.isPresent()) {
            final Optional<String> name = originEntity
                .get()
                .getNameInUpperCase();

            if (name.isPresent() && !_originsByName.containsKey(name.get())) {
                _originsByName
                    .put(name.get(), new WeakReference<>(originEntity.get()));
            }
        }
    }

    /**
     * Adds an alias for a point.
     *
     * @param pointEntity The point entity.
     * @param alias The alias.
     */
    public void addPointAlias(
            @Nonnull final PointEntity pointEntity,
            @Nonnull final String alias)
    {
        final UUID pointUUID = pointEntity.getUUID().get();
        Set<String> aliases = _pointsAliases.get(pointUUID);

        if (aliases == null) {
            aliases = new HashSet<>();
            _pointsAliases.put(pointUUID, aliases);
        }

        aliases.add(alias);

        final Optional<Reference<Point>> previousReference = _points
            .addAlias(alias, pointEntity);
        final Optional<Point> previousPoint = previousReference
            .isPresent()? Optional
                .of(previousReference.get().get()): Optional.empty();

        if (previousPoint.isPresent()
                && !previousPoint.get().equals(pointEntity)) {
            getThisLogger()
                .warn(
                    ServiceMessages.POINT_ALIAS_HIDES,
                    alias,
                    pointEntity,
                    previousPoint.get());
        }
    }

    /**
     * Adds a point entity to this metadata.
     *
     * @param pointEntity The optional point entity.
     */
    public void addPointEntity(@Nonnull final Optional<PointEntity> pointEntity)
    {
        if (pointEntity.isPresent()) {
            _points.addPoint(pointEntity.get());
        }
    }

    /**
     * Adds a store entity to this metadata.
     *
     * @param storeEntity The optional store entity.
     */
    public void addStoreEntity(@Nonnull final Optional<StoreEntity> storeEntity)
    {
        if (storeEntity.isPresent()) {
            final Optional<String> name = storeEntity
                .get()
                .getNameInUpperCase();

            if (name.isPresent() && !_storesByName.containsKey(name.get())) {
                _storesByName
                    .put(name.get(), new WeakReference<>(storeEntity.get()));
            }
        }
    }

    /**
     * Adds a sync entity to this metadata.
     *
     * @param syncEntity The optional sync entity.
     */
    public void addSyncEntity(@Nonnull final Optional<SyncEntity> syncEntity)
    {
        if (syncEntity.isPresent()) {
            final Optional<String> name = syncEntity.get().getNameInUpperCase();

            if (name.isPresent() && !_syncsByName.containsKey(name.get())) {
                _syncsByName
                    .put(name.get(), new WeakReference<>(syncEntity.get()));
            }
        }
    }

    /**
     * Adds a transform entity to this metadata.
     *
     * @param transformEntity The optional transform entity.
     */
    public void addTransformEntity(
            @Nonnull final Optional<TransformEntity> transformEntity)
    {
        if (transformEntity.isPresent()) {
            final Optional<String> name = transformEntity
                .get()
                .getNameInUpperCase();

            if (name.isPresent()
                    && !_transformsByName.containsKey(name.get())) {
                _transformsByName
                    .put(
                        name.get(),
                        new WeakReference<>(transformEntity.get()));
            }
        }
    }

    /**
     * Adjusts points level.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean adjustPointsLevel()
    {
        boolean success = true;

        for (final Point point: getPointsCollection()) {
            try {
                ((PointEntity) point).adjustLevel();
            } catch (final TopologicalErrorException exception) {
                getThisLogger()
                    .error(BaseMessages.VERBATIM, exception.getMessage());
                success = false;
            }
        }

        return success;
    }

    /**
     * Anchors an entity.
     *
     * @param entity The entity.
     */
    public void anchor(@Nonnull final Entity entity)
    {
        _anchor.add(entity);
    }

    /**
     * Cleans up unreferenced entities.
     */
    public void cleanUp()
    {
        _defaultBehaviors.clear();

        boolean cleaned;

        do {
            cleaned = false;

            System.gc();

            cleaned |= _cleanUp(_contentsByName);
            cleaned |= _cleanUp(_enginesByName);
            cleaned |= _cleanUp(_points.getUUIDMap());
            cleaned |= _cleanUp(_points.getNamesMap());
            cleaned |= _cleanUp(_originsByName);
            cleaned |= _cleanUp(_storesByName);
            cleaned |= _cleanUp(_syncsByName);
            cleaned |= _cleanUp(_transformsByName);

            for (final GroupEntity group: getGroupEntities()) {
                cleaned |= group.cleanUp();
            }
        } while (cleaned);
    }

    /** {@inheritDoc}
     *
     * <p>The execution of this method is potentially expensive in CPU and
     * memory. Together with the corresponding method in Entity classes, it is
     * intended as a support for validation tests of the
     * {@link org.rvpf.document.exporter.MetadataExporter}.</p>
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (other instanceof Metadata) {
            final Metadata otherMetadata = (Metadata) other;

            if (!getDomain().equals(otherMetadata.getDomain())) {
                return false;
            }

            if (!getProperties().equals(otherMetadata.getProperties())) {
                return false;
            }

            if (!Objects.equals(_attributes, otherMetadata._attributes)) {
                return false;
            }

            final List<Point> points = new ArrayList<>(getPointsCollection());
            final List<Point> otherPoints = new ArrayList<>(
                otherMetadata.getPointsCollection());

            points.sort(Entity.UUID_COMPARATOR);
            otherPoints.sort(Entity.UUID_COMPARATOR);

            if (!points.equals(otherPoints)) {
                return false;
            }

            for (final Point point: points) {
                if (!getPointAliases(point)
                    .equals(otherMetadata.getPointAliases(point))) {
                    return false;
                }
            }

            if (_groupsByName.size() != otherMetadata._groupsByName.size()) {
                return false;
            }

            for (final Map.Entry<String, GroupEntity> groupEntry:
                    _groupsByName.entrySet()) {
                if (!groupEntry
                    .getValue()
                    .equals(
                        otherMetadata._groupsByName.get(groupEntry.getKey()))) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Gets the attributes.
     *
     * @return The attributes.
     */
    @Nonnull
    @CheckReturnValue
    public KeyedGroups getAttributes()
    {
        return _attributes;
    }

    /**
     * Gets the attributes for an usage.
     *
     * @param usage The usage.
     *
     * @return The attributes.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Attributes> getAttributes(@Nonnull final String usage)
    {
        return Optional
            .ofNullable(
                (Attributes) _attributes
                    .getObject(usage.toUpperCase(Locale.ROOT)));
    }

    /**
     * Gets the configuration.
     *
     * <p>Although the metadata is itself a Config instance, this method returns
     * a different object: the overriding configuration supplied to the
     * constructor.</p>
     *
     * @return The overriding configuration.
     */
    @Nonnull
    @CheckReturnValue
    public Config getConfig()
    {
        return _config;
    }

    /**
     * Gets the content entities registered in this metadata.
     *
     * @return A collection of content entities.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<ContentEntity> getContentEntities()
    {
        final List<ContentEntity> contentEntities = new LinkedList<>();

        for (final Reference<ContentEntity> reference:
                _contentsByName.values()) {
            if (reference != null) {
                final ContentEntity contentEntity = reference.get();

                if (contentEntity != null) {
                    contentEntities.add(contentEntity);
                }
            }
        }

        return contentEntities;
    }

    /**
     * Gets the content entity with the specified name.
     *
     * @param name The optional name.
     *
     * @return The optional content entity.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<ContentEntity> getContentEntity(
            @Nonnull final Optional<String> name)
    {
        final Reference<ContentEntity> reference = name
            .isPresent()? _contentsByName
                .get(name.get().trim().toUpperCase(Locale.ROOT)): null;

        return Optional.ofNullable((reference != null)? reference.get(): null);
    }

    /**
     * Gets a default behavior entity wrapping the specified class.
     *
     * @param behaviorClass The wrapped class of the requested behavior entity.
     *
     * @return The behavior entity (null when not found).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<BehaviorEntity> getDefaultBehavior(
            @Nonnull final Class<?> behaviorClass)
    {
        return Optional.ofNullable(_defaultBehaviors.get(behaviorClass));
    }

    /**
     * Gets the domain of this metadata.
     *
     * @return The domain of this metadata.
     */
    @Nonnull
    @CheckReturnValue
    public String getDomain()
    {
        return (_domain != null)? _domain: "";
    }

    /**
     * Gets the engine entities registered in this metadata.
     *
     * @return A collection of engine entities.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<EngineEntity> getEngineEntities()
    {
        final List<EngineEntity> engineEntities = new LinkedList<>();

        for (final Reference<EngineEntity> reference: _enginesByName.values()) {
            final EngineEntity engineEntity = (reference != null)? reference
                .get(): null;

            if (engineEntity != null) {
                engineEntities.add(engineEntity);
            }
        }

        return engineEntities;
    }

    /**
     * Gets the engine entity with the specified name.
     *
     * @param name The optional name.
     *
     * @return The optional engine entity.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<EngineEntity> getEngineEntity(
            @Nonnull final Optional<String> name)
    {
        final Reference<EngineEntity> reference = name
            .isPresent()? _enginesByName
                .get(name.get().trim().toUpperCase(Locale.ROOT)): null;

        return Optional.ofNullable((reference != null)? reference.get(): null);
    }

    /**
     * Gets the filter.
     *
     * @return The filter.
     */
    @Nonnull
    @CheckReturnValue
    public MetadataFilter getFilter()
    {
        MetadataFilter filter = _filter;

        if (filter == null) {
            filter = new MetadataFilter(false);
            _filter = filter;
        }

        return filter;
    }

    /**
     * Gets the group entities registered in this metadata.
     *
     * @return A collection of group entities.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<GroupEntity> getGroupEntities()
    {
        return _groupsByName.values();
    }

    /**
     * Gets the group entity with the specified name.
     *
     * @param name The optional name.
     *
     * @return The optional group entity.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<GroupEntity> getGroupEntity(
            @Nonnull final Optional<String> name)
    {
        return Optional
            .ofNullable(
                name.isPresent()? _groupsByName
                    .get(name.get().trim().toUpperCase(Locale.ROOT)): null);
    }

    /**
     * Gets the origin entities registered in this metadata.
     *
     * @return A collection of origin entities.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<OriginEntity> getOriginEntities()
    {
        final List<OriginEntity> originEntities = new LinkedList<>();

        for (final Reference<OriginEntity> reference: _originsByName.values()) {
            final OriginEntity originEntity = (reference != null)? reference
                .get(): null;

            if (originEntity != null) {
                originEntities.add(originEntity);
            }
        }

        return originEntities;
    }

    /**
     * Gets the origin entity with the specified name.
     *
     * @param name The optional name.
     *
     * @return The optional origin entity.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<OriginEntity> getOriginEntity(
            @Nonnull final Optional<String> name)
    {
        final Reference<OriginEntity> reference = name
            .isPresent()? _originsByName
                .get(name.get().trim().toUpperCase(Locale.ROOT)): null;

        return Optional.ofNullable((reference != null)? reference.get(): null);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Point> getPoint(final String key)
    {
        return _points.getPoint(key);
    }

    /**
     * Gets the aliases for a point.
     *
     * @param point The point.
     *
     * @return The set of aliases.
     */
    @Nonnull
    @CheckReturnValue
    public Set<String> getPointAliases(@Nonnull final Point point)
    {
        Set<String> aliases = _pointsAliases.get(point.getUUID().get());

        if (aliases == null) {
            aliases = Collections.emptySet();
        }

        return aliases;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Point> getPointByName(final String name)
    {
        return _points.getPointByName(name);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Point> getPointByUUID(final UUID uuid)
    {
        return _points.getPointByUUID(uuid);
    }

    /** {@inheritDoc}
     */
    @Override
    public Collection<Point> getPointsCollection()
    {
        return _points.getPointsCollection();
    }

    /**
     * Gets the store entities registered in this metadata.
     *
     * @return A collection of store entities.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<StoreEntity> getStoreEntities()
    {
        final List<StoreEntity> storeEntities = new LinkedList<>();

        for (final Reference<StoreEntity> reference: _storesByName.values()) {
            final StoreEntity storeEntity = (reference != null)? reference
                .get(): null;

            if (storeEntity != null) {
                storeEntities.add(storeEntity);
            }
        }

        return storeEntities;
    }

    /**
     * Gets the store entity with the specified name.
     *
     * @param name The optional name (empty or unknown returns empty).
     *
     * @return The optional store entity.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<StoreEntity> getStoreEntity(
            @Nonnull final Optional<String> name)
    {
        final Reference<StoreEntity> reference = name
            .isPresent()? _storesByName
                .get(name.get().trim().toUpperCase(Locale.ROOT)): null;

        return Optional.ofNullable((reference != null)? reference.get(): null);
    }

    /**
     * Gets the sync entities registered in this metadata.
     *
     * @return A collection of sync entities.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<SyncEntity> getSyncEntities()
    {
        final List<SyncEntity> syncEntities = new LinkedList<>();

        for (final Reference<SyncEntity> reference: _syncsByName.values()) {
            final SyncEntity syncEntity = (reference != null)? reference
                .get(): null;

            if (syncEntity != null) {
                syncEntities.add(syncEntity);
            }
        }

        return syncEntities;
    }

    /**
     * Gets the sync entity with the specified name.
     *
     * @param name The optional name.
     *
     * @return The optional sync entity.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<SyncEntity> getSyncEntity(
            @Nonnull final Optional<String> name)
    {
        final Reference<SyncEntity> reference = name
            .isPresent()? _syncsByName
                .get(name.get().trim().toUpperCase(Locale.ROOT)): null;

        return Optional.ofNullable((reference != null)? reference.get(): null);
    }

    /**
     * Gets the transform entities registered in this metadata.
     *
     * @return A collection of transform entities.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<TransformEntity> getTransformEntities()
    {
        final List<TransformEntity> transformEntities = new LinkedList<>();

        for (final Reference<TransformEntity> reference:
                _transformsByName.values()) {
            final TransformEntity transformEntity = (reference != null)
                ? reference
                    .get(): null;

            if (transformEntity != null) {
                transformEntities.add(transformEntity);
            }
        }

        return transformEntities;
    }

    /**
     * Gets the transform entity with the specified name.
     *
     * @param name The optional name.
     *
     * @return The optional transform entity.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<TransformEntity> getTransformEntity(
            @Nonnull final Optional<String> name)
    {
        final Reference<TransformEntity> reference = name
            .isPresent()? _transformsByName
                .get(name.get().trim().toUpperCase(Locale.ROOT)): null;

        return Optional.ofNullable((reference != null)? reference.get(): null);
    }

    /**
     * Gets a URI for an entity.
     *
     * @param entity The entity.
     *
     * @return The URI.
     */
    @Nonnull
    @CheckReturnValue
    public URI getURI(@Nonnull final Entity entity)
    {
        try {
            return new URI(
                RVPF_SCHEME,
                entity.getElementName().toLowerCase(
                    Locale.ROOT) + ":" + getDomain(),
                entity.getName().orElse(null));
        } catch (final URISyntaxException exception) {
            throw new RuntimeException(exception);    // Should not happen.
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public URL getURL()
    {
        final URL url = _url;

        return (url != null)? url: getConfig().getURL();
    }

    /**
     * Overrides {@link java.lang.Object#hashCode}.
     *
     * <p>The metadata objects are not designed to be used as keys in any map
     * object.</p>
     *
     * @return The hash code for its contents.
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Asks if an entity is anchored.
     *
     * @param entity The entity.
     *
     * @return True if the entity is anchored.
     */
    @CheckReturnValue
    public boolean isAnchored(@Nonnull final Entity entity)
    {
        return _anchor.contains(entity);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isIncluded(final URL url)
    {
        return super.isIncluded(url) || _config.isIncluded(url);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean registerClassLib(final String classLibName)
    {
        return _config.registerClassLib(classLibName);
    }

    /**
     * Sets attributes.
     *
     * @param attributes The attributes.
     */
    public final void setAttributes(@Nonnull final KeyedGroups attributes)
    {
        _attributes = Require.notNull(attributes);
    }

    /**
     * Sets the default behavior entity wrapping the specified class.
     *
     * @param behaviorClass The wrapped class of the behavior entity.
     * @param behaviorEntity The behavior entity.
     */
    public void setDefaultBehavior(
            @Nonnull final Class<?> behaviorClass,
            @Nonnull final BehaviorEntity behaviorEntity)
    {
        _defaultBehaviors.put(behaviorClass, behaviorEntity);
    }

    /**
     * Sets the domain of this metadata.
     *
     * @param domain The domain of this metadata.
     */
    public void setDomain(@Nonnull String domain)
    {
        domain = domain.trim();

        if (domain.isEmpty()) {
            domain = null;
        }

        _domain = domain;
    }

    /**
     * Sets the filter.
     *
     * @param filter The filter.
     */
    public void setFilter(@Nonnull final MetadataFilter filter)
    {
        _filter = Require.notNull(filter);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setURL(final URL url)
    {
        _url = url;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        super.tearDown();

        if (_config != null) {
            _config.tearDown();
        }
    }

    /**
     * Tears down points.
     */
    public void tearDownPoints()
    {
        for (final Point point: getPointsCollection()) {
            ((PointEntity) point).tearDown();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return toXML().toString();
    }

    /**
     * Converts to XML.
     *
     * @return The XML representation.
     */
    public XMLDocument toXML()
    {
        return MetadataExporter
            .export(
                this,
                Optional.of(Collections.<String>emptySet()),
                Optional.of(Collections.<String>emptySet()),
                true);
    }

    /**
     * Validates points relationships.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean validatePointsRelationships()
    {
        boolean success = true;

        for (final Point point: getPointsCollection()) {
            if (!((PointEntity) point).setUp(this)) {
                success = false;
            }
        }

        if (success) {
            for (final Point point: getPointsCollection()) {
                final PointEntity pointEntity = (PointEntity) point;

                if (pointEntity.getTransformEntity() != null) {
                    if (!pointEntity.setUpRelations(this)) {
                        success = false;
                    }
                }
            }
        }

        if (success) {
            for (final Point point: getPointsCollection()) {
                final Optional<Transform> transform = ((PointEntity) point)
                    .getTransform();

                if (transform.isPresent() && !transform.get().setUp(point)) {
                    success = false;
                }
            }
        }

        return success;
    }

    private static boolean _cleanUp(final Map<?, ?> map)
    {
        boolean cleaned = false;

        for (final Iterator<?> i = map.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
            final Reference<?> reference = (Reference<?>) entry.getValue();

            if (reference.get() == null) {
                i.remove();
                cleaned = true;
            }
        }

        return cleaned;
    }

    /** RVPF URI scheme. */
    public static final String RVPF_SCHEME = "rvpf";

    private final Set<Entity> _anchor = new IdentityHashSet<>();
    private KeyedGroups _attributes = new KeyedGroups();
    private final Config _config;
    private final Map<String, Reference<ContentEntity>> _contentsByName =
        new HashMap<>();
    private final Map<Class<?>, BehaviorEntity> _defaultBehaviors =
        new HashMap<>();
    private String _domain;
    private final Map<String, Reference<EngineEntity>> _enginesByName =
        new HashMap<>();
    private MetadataFilter _filter;
    private final Map<String, GroupEntity> _groupsByName = new HashMap<>();
    private final Map<String, Reference<OriginEntity>> _originsByName =
        new HashMap<>();
    private final Points.Impl _points = new Points.Impl();
    private final Map<UUID, Set<String>> _pointsAliases = new WeakHashMap<>();
    private final Map<String, Reference<StoreEntity>> _storesByName =
        new HashMap<>();
    private final Map<String, Reference<SyncEntity>> _syncsByName =
        new HashMap<>();
    private final Map<String, Reference<TransformEntity>> _transformsByName =
        new HashMap<>();
    private URL _url;
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
