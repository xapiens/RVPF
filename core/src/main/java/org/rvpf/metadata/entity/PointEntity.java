/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointEntity.java 4100 2019-06-29 17:22:34Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.Content;
import org.rvpf.base.Entity;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.UUID;
import org.rvpf.base.store.Store;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.filter.DisabledFilter;
import org.rvpf.base.value.filter.ValueFilter;
import org.rvpf.config.TopologicalErrorException;
import org.rvpf.config.UndefinedEntityException;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Permissions;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.entity.filter.StepFilterFactory;
import org.rvpf.metadata.entity.filter.ValueFilterFactory;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.service.ServiceMessages;

/**
 * Point entity.
 *
 * <p>Holds the informations on a point and is the reference for all the values
 * processed by the framework.</p>
 */
public abstract class PointEntity
    extends ParamsEntity
    implements Point
{
    /**
     * Constructs an instance.
     */
    protected PointEntity()
    {
        _originEntity = Optional.empty();
        _storeEntity = Optional.empty();
        _syncEntity = Optional.empty();
        _permissionsEntity = Optional.empty();
        _contentEntity = Optional.empty();
        _transformEntity = Optional.empty();
    }

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected PointEntity(@Nonnull final PointEntity other)
    {
        super(other);

        _originEntity = other._originEntity;
        _storeEntity = other._storeEntity;
        _syncEntity = other._syncEntity;
        _inputRelations = other._inputRelations;
        _resultRelations = other._resultRelations;
        _permissionsEntity = other._permissionsEntity;
        _nullRemoves = other._nullRemoves;
        _replicates = other._replicates;
        _recalcLatest = other._recalcLatest;
        _volatile = other._volatile;
        _tidied = other._tidied;
        _dropped = other._dropped;
        _level = other._level;
        _contentEntity = other._contentEntity;
        _transformEntity = other._transformEntity;
    }

    /**
     * Adds an input relation.
     *
     * @param inputRelation The input relation.
     */
    public final void addInputRelation(@Nonnull final PointInput inputRelation)
    {
        if (_inputRelations == null) {
            _inputRelations = new LinkedList<>();
        }

        _inputRelations.add(Require.notNull(inputRelation));
    }

    /**
     * Adds a replicate.
     *
     * @param replicate The replicate.
     */
    public final void addReplicate(@Nonnull final Replicate replicate)
    {
        if (replicate.getPoint() == this) {
            throw new IllegalArgumentException();
        }

        if (_replicates == null) {
            _replicates = new LinkedList<>();
        }

        _replicates.add(replicate);
    }

    /**
     * Adjust the level of this point's dependents.
     *
     * @throws TopologicalErrorException When a recursive reference is found.
     */
    public final void adjustLevel()
        throws TopologicalErrorException
    {
        _adjustLevel(0);
    }

    /**
     * Clears the content entity for this point.
     */
    public final void clearContentEntity()
    {
        _contentEntity = Optional.empty();
    }

    /**
     * Clears the input relations.
     *
     * <p>Removes all input relations while keeping the indication that this
     * point has held input relations.</p>
     */
    public final void clearInputs()
    {
        if (_inputRelations != null) {
            _inputRelations.clear();
        }
    }

    /**
     * Clears the result relations.
     *
     * <p>Removes all result relations while keeping the indication that this
     * point has held result relations.</p>
     */
    public final void clearResults()
    {
        if (_resultRelations != null) {
            _resultRelations.clear();
        }
    }

    /**
     * Clears the transform entity for this point.
     */
    public final void clearTransformEntity()
    {
        _transformEntity = Optional.empty();
    }

    /**
     * Closes the current store connection.
     */
    public final void close()
    {
        final Optional<? extends Store> store = getStore();

        if (store.isPresent()) {
            store.get().close();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final int compareTo(final Entity other)
    {
        if (other == this) {
            return 0;
        }

        final PointEntity otherPoint = (PointEntity) other;
        int comparison;

        comparison = _level - otherPoint._level;

        if (comparison == 0) {
            final UUID pointUUID = getUUID().get();
            final UUID otherPointUUID = otherPoint.getUUID().get();

            comparison = pointUUID.compareTo(otherPointUUID);
        }

        return comparison;
    }

    /**
     * Asks if the content implements a specified interface.
     *
     * @param name The name of the ClassDef representing the interface.
     *
     * @return True if the interface is implemented.
     *
     * @throws UndefinedEntityException When the interface is undefined.
     */
    @CheckReturnValue
    public final boolean contentIs(
            @Nonnull final String name)
        throws UndefinedEntityException
    {
        if (!_contentEntity.isPresent()) {
            return false;
        }

        return _contentEntity.get().is(Require.notNull(name));
    }

    /** {@inheritDoc}
     */
    @Override
    public abstract PointEntity copy();

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (super.equals(other)) {
            final PointEntity otherPoint = (PointEntity) other;

            if (!Objects.equals(_originEntity, otherPoint._originEntity)) {
                return false;
            }

            if (!Objects.equals(_contentEntity, otherPoint._contentEntity)) {
                return false;
            }

            if (!Objects.equals(_storeEntity, otherPoint._storeEntity)) {
                return false;
            }

            if (!Objects.equals(_syncEntity, otherPoint._syncEntity)) {
                return false;
            }

            if (!Objects
                .equals(_transformEntity, otherPoint._transformEntity)) {
                return false;
            }

            if (!Objects
                .equals(_permissionsEntity, otherPoint._permissionsEntity)) {
                return false;
            }

            if (_volatile != otherPoint._volatile) {
                return false;
            }

            final List<Replicate> replicates = getReplicates();
            final List<Replicate> otherReplicates = otherPoint.getReplicates();

            if (replicates.size() != otherReplicates.size()) {
                return false;
            }

            final Iterator<Replicate> replicatesIterator = replicates
                .iterator();
            final Iterator<Replicate> otherReplicatesIterator = otherReplicates
                .iterator();

            while (replicatesIterator.hasNext()) {
                if (!Objects
                    .equals(
                        replicatesIterator.next().getPointUUID(),
                        otherReplicatesIterator.next().getPointUUID())) {
                    return false;
                }
            }

            return getInputs().equals(otherPoint.getInputs());
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public final ValueFilter filter()
    {
        final ClassDef filterFactoryClassDef = getParams()
            .getClassDef(FILTER_PARAM, DEFAULT_FILTER);
        final ValueFilterFactory filterFactory = filterFactoryClassDef
            .createInstance(ValueFilterFactory.class);

        return (filterFactory != null)? filterFactory
            .newFilter(getParams()): new DisabledFilter();
    }

    /**
     * Flags the point as having result relations.
     */
    public void flagResultRelations()
    {
        if (_resultRelations == null) {
            _resultRelations = new LinkedList<>();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<Content> getContent()
    {
        return _contentEntity
            .isPresent()? Optional
                .of((Content) _contentEntity.get().getInstance().get()): Optional
                    .empty();
    }

    /**
     * Gets the content entity for this point.
     *
     * @return The optional content entity.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ContentEntity> getContentEntity()
    {
        return _contentEntity;
    }

    /**
     * Gets the point's definition.
     *
     * <p>Allows a reference to supply the referent.</p>
     *
     * @return The optional point's definition.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Optional<PointEntity> getDefinition();

    /** {@inheritDoc}
     */
    @Override
    public final String getElementName()
    {
        return ELEMENT_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public final List<PointInput> getInputs()
    {
        return (_inputRelations != null)? Collections
            .unmodifiableList(_inputRelations): _EMPTY_RELATIONS_LIST;
    }

    /** {@inheritDoc}
     */
    @Override
    public final int getLevel()
    {
        return _level;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<? extends Origin> getOrigin()
    {
        return _originEntity;
    }

    /**
     * Gets the origin entity for this point.
     *
     * @return The optional origin entity.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<OriginEntity> getOriginEntity()
    {
        return _originEntity;
    }

    /**
     * Gets the permissions.
     *
     * @return The optional permissions.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<? extends Permissions> getPermissions()
    {
        if (!_permissionsEntity.isPresent() && _storeEntity.isPresent()) {
            return _storeEntity.get().getPermissions();
        }

        return _permissionsEntity;
    }

    /**
     * Gets the premissions entity.
     *
     * @return The optional permissions entity.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<PermissionsEntity> getPermissionsEntity()
    {
        return _permissionsEntity;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getPrefix()
    {
        return ENTITY_PREFIX;
    }

    /**
     * Gets the recalc limit.
     *
     * @return The recalc limit.
     */
    @CheckReturnValue
    public final int getRecalcLatest()
    {
        return _recalcLatest;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getReferenceName()
    {
        return ENTITY_REFERENCE_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public List<Replicate> getReplicates()
    {
        return (_replicates != null)? Collections
            .unmodifiableList(_replicates): _EMPTY_REPLICATES_LIST;
    }

    /** {@inheritDoc}
     */
    @Override
    public final List<PointInput> getResults()
    {
        return (_resultRelations != null)? Collections
            .unmodifiableList(_resultRelations): _EMPTY_RELATIONS_LIST;
    }

    /** {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public final Optional<? extends Store> getStore()
    {
        return _storeEntity
            .isPresent()? (Optional<? extends Store>) _storeEntity
                .get()
                .getInstance(): Optional.empty();
    }

    /**
     * Gets the store entity for this point.
     *
     * @return The optional store entity.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<StoreEntity> getStoreEntity()
    {
        return _storeEntity;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<Sync> getSync()
    {
        return _syncEntity
            .isPresent()? Optional
                .of(_syncEntity.get().getSync()): Optional.empty();
    }

    /**
     * Gets the sync entity.
     *
     * @return The optional sync entity.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<SyncEntity> getSyncEntity()
    {
        return _syncEntity;
    }

    /**
     * Gets this point's transform.
     *
     * @return The optional transform used to compute values for this point.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Transform> getTransform()
    {
        return Optional
            .ofNullable(
                _transformEntity.isPresent()? _transformEntity
                    .get()
                    .getTransform(): null);
    }

    /**
     * Gets the transform entity for this point.
     *
     * @return The optional transform entity.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<TransformEntity> getTransformEntity()
    {
        return _transformEntity;
    }

    /**
     * Asks if this point has input relations.
     *
     * @return True if it has input relations.
     */
    @CheckReturnValue
    public final boolean hasInputRelations()
    {
        return _inputRelations != null;
    }

    /**
     * Asks if this point has result relations.
     *
     * @return True if it has result relations.
     */
    @CheckReturnValue
    public final boolean hasResultRelations()
    {
        return _resultRelations != null;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /**
     * Asks if this point is a definition.
     *
     * @return True if this is a definition.
     */
    @CheckReturnValue
    public abstract boolean isDefinition();

    /** {@inheritDoc}
     */
    @Override
    public final boolean isDropped()
    {
        return _dropped;
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isNullRemoves(final boolean defaultNullRemoves)
    {
        return (_nullRemoves != null)? _nullRemoves
            .booleanValue(): defaultNullRemoves;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSynced()
    {
        return _syncEntity.isPresent();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isVolatile()
    {
        return _volatile;
    }

    /**
     * Reset the input relations.
     */
    public final void resetInputs()
    {
        _inputRelations = null;
    }

    /**
     * Resets this point's replicates.
     */
    public final void resetReplicates()
    {
        _replicates = null;
    }

    /**
     * Resets the result relations.
     */
    public final void resetResults()
    {
        _resultRelations = null;
    }

    /**
     * Sets the content entity for this point.
     *
     * @param content The content entity.
     */
    public final void setContentEntity(@Nonnull final ContentEntity content)
    {
        _contentEntity = Optional.of(content);
    }

    /**
     * Sets the point's definition.
     *
     * @param definition The point's definition.
     */
    public abstract void setDefinition(@Nonnull PointEntity definition);

    /**
     * Sets the dropped indicator.
     *
     * @param dropped The new value of the dropped indicator.
     */
    public final void setDropped(final boolean dropped)
    {
        _dropped = dropped;
    }

    /**
     * Sets the origin entity for this point.
     *
     * @param originEntity The optional origin entity.
     */
    public final void setOriginEntity(
            @Nonnull final Optional<OriginEntity> originEntity)
    {
        _originEntity = originEntity;
    }

    /**
     * Sets the permissions.
     *
     * @param permissions The optional permissions.
     */
    public final void setPermissionsEntity(
            @Nonnull final Optional<PermissionsEntity> permissions)
    {
        _permissionsEntity = permissions;
    }

    /**
     * Sets the store entity for this point.
     *
     * @param storeEntity The optional store entity.
     */
    public final void setStoreEntity(
            @Nonnull final Optional<StoreEntity> storeEntity)
    {
        _storeEntity = storeEntity;
    }

    /**
     * Sets the synchronization reference for this point.
     *
     * @param syncEntity The optional entity used for synchronization.
     */
    public final void setSyncEntity(
            @Nonnull final Optional<SyncEntity> syncEntity)
    {
        _syncEntity = syncEntity;
    }

    /**
     * Sets the transform entity for this point.
     *
     * @param transform The transform entity.
     */
    public final void setTransformEntity(
            @Nonnull final TransformEntity transform)
    {
        _transformEntity = Optional.of(transform);
    }

    /**
     * Sets up this point.
     *
     * @param metadata The metadata.
     *
     * @return True if successful.
     */
    @CheckReturnValue
    public final boolean setUp(@Nonnull final Metadata metadata)
    {
        if (_setUp) {
            return true;
        }

        final Proxied proxied;

        // Sets up the content definition.

        if (_contentEntity.isPresent()) {
            if (!_contentEntity.get().setUp(metadata)) {
                return _failed();
            }

            try {
                proxied = (Proxied) getContent().get().getInstance(this);
            } catch (final ClassCastException exception) {
                getThisLogger()
                    .error(
                        ServiceMessages.CONTENT_PROXIED,
                        _contentEntity.get().getName());

                return _failed();
            }

            if (proxied == null) {
                return _failed();
            }

            _contentEntity = Optional
                .of((ContentEntity) _contentEntity.get().getProxy(proxied));
        }

        // Sets up the store client access.

        if (!setUpStore(metadata)) {
            return _failed();
        }

        // Sets up the transform.

        if (_transformEntity.isPresent()) {
            if (!_transformEntity.get().setUp(metadata)) {
                return _failed();
            }

            _recalcLatest = (getParams()
                .containsValueKey(
                    RECALC_LATEST_PARAM)? getParams(): _transformEntity
                        .get()
                        .getParams())
                .getInt(RECALC_LATEST_PARAM, -1);
        }

        // Sets up the sync.

        if (_syncEntity.isPresent() && !_syncEntity.get().setUp()) {
            return _failed();
        }

        // Sets up the volatile indicator.

        _volatile = getParams().getBoolean(VOLATILE_PARAM);

        // Sets up the null removes indicator.

        if (getParams().containsValueKey(NULL_REMOVES_PARAM)) {
            _nullRemoves = Boolean
                .valueOf(getParams().getBoolean(NULL_REMOVES_PARAM));
        }

        _setUp = true;

        return true;
    }

    /**
     * Sets up the input relations of this point.
     *
     * @param metadata The metadata.
     *
     * @return True if successful.
     */
    @CheckReturnValue
    public final boolean setUpRelations(@Nonnull final Metadata metadata)
    {
        if (_transformEntity.isPresent()) {
            final Transform transform = _transformEntity.get().getTransform();
            final Iterator<TransformEntity.Arg> args = _transformEntity
                .get()
                .getArgs()
                .iterator();
            final Optional<? extends Transform> proxiedTransform;
            TransformEntity.Arg arg = null;

            if (transform == null) {
                return _failed();
            }

            for (final PointInput pointInput: getInputs()) {
                if (args.hasNext()) {
                    arg = args.next();
                } else if ((arg != null)
                           && !arg.isMultiple()
                           && !pointInput.isControl()) {
                    getThisLogger()
                        .warn(ServiceMessages.POINT_INPUTS_HIGH, this);
                    arg = null;
                }

                if (!pointInput.setUp(metadata, Optional.ofNullable(arg))) {
                    return _failed();
                }
            }

            if (args.hasNext()) {
                if (!args.next().isMultiple()) {
                    getThisLogger()
                        .warn(ServiceMessages.POINT_INPUTS_LOW, this);
                }
            }

            for (final PointInput pointInput: getInputs()) {
                if (!pointInput.validate()) {
                    return _failed();
                }
            }

            proxiedTransform = transform.getInstance(this);

            if (!proxiedTransform.isPresent()) {
                return _failed();
            }

            _transformEntity = Optional
                .of(
                    (TransformEntity) _transformEntity
                        .get()
                        .getProxy(proxiedTransform.get()));
        }

        return true;
    }

    /**
     * Sets up the store associated with this point.
     *
     * @param metadata The metadata.
     *
     * @return True if successful.
     */
    @CheckReturnValue
    public final boolean setUpStore(@Nonnull final Metadata metadata)
    {
        if (_storeEntity.isPresent()) {
            if (!_storeEntity.get().setUp(metadata)) {
                return false;
            }

            final Store storeInstance = getStore().orElse(null);

            if (storeInstance == null) {
                return false;
            }

            if (storeInstance.isNullRemoves()
                    || getParams().containsValueKey(NULL_REMOVES_PARAM)) {
                _nullRemoves = Boolean
                    .valueOf(getParams().getBoolean(NULL_REMOVES_PARAM, true));
            } else {
                _nullRemoves = null;
            }

            _storeEntity = Optional
                .of(
                    (StoreEntity) _storeEntity
                        .get()
                        .getProxy((Proxied) storeInstance));

            storeInstance.bind(this);
        }

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    public final void tearDown()
    {
        for (final PointInput pointInput: getInputs()) {
            pointInput.tearDown();
        }

        if (_syncEntity.isPresent()) {
            _syncEntity.get().tearDown();
        }

        if (_transformEntity.isPresent()) {
            _transformEntity.get().tearDown();
        }

        tearDownStore();

        if (_contentEntity.isPresent()) {
            _contentEntity.get().tearDown();
        }
    }

    /**
     * Tears down the store associated with this point.
     */
    public final void tearDownStore()
    {
        if (_storeEntity.isPresent()) {
            _storeEntity.get().tearDown();
        }
    }

    /**
     * Tidies the point entity.
     *
     * <p>This is called after the document load.</p>
     *
     * @return True on success.
     */
    public final boolean tidy()
    {
        if (_tidied) {
            return true;
        }

        _tidied = _tidyInputRelations() && _tidyReplicates();

        return _tidied;
    }

    /** {@inheritDoc}
     *
     * @return Its ident, name, uuid (String) or "?".
     */
    @Override
    public final String toString()
    {
        final Optional<String> name = getName();

        if (name.isPresent()) {
            return name.get();
        }

        final Optional<UUID> uuid = getUUID();

        return (uuid.isPresent())? uuid.get().toString(): "?";
    }

    private void _addResultRelation(final PointInput resultRelation)
    {
        flagResultRelations();

        _resultRelations.add(resultRelation);
    }

    private void _adjustLevel(final int level)
        throws TopologicalErrorException
    {
        if (_busy) {
            throw new TopologicalErrorException(this);
        }

        if (level > _level) {
            _level = level;
            _busy = true;

            for (final PointRelation result: getResults()) {
                final PointEntity point = (PointEntity) result.getResultPoint();

                if (point != this) {
                    point._adjustLevel(_level + 1);
                }
            }

            for (final Replicate replicate: getReplicates()) {
                if (replicate.getPoint() == this) {
                    throw new TopologicalErrorException(this);
                }
            }

            _busy = false;
        }
    }

    private boolean _failed()
    {
        getThisLogger().error(ServiceMessages.POINT_SET_UP_FAILED, this);

        return false;
    }

    private boolean _tidyInputRelations()
    {
        if (_inputRelations == null) {
            return true;
        }

        boolean success = true;

        for (final Iterator<PointInput> iterator = _inputRelations.iterator();
                iterator.hasNext(); ) {
            final PointInput inputRelation = iterator.next();
            PointEntity inputPoint = inputRelation.getInputPointEntity();

            if (inputPoint.isDropped()) {
                iterator.remove();
            } else {
                if (!inputPoint.isDefinition()) {
                    inputPoint = inputPoint.getDefinition().orElse(null);

                    if (inputPoint != null) {
                        inputRelation.setInputPoint(inputPoint);
                    } else {
                        getThisLogger()
                            .error(
                                ServiceMessages.POINT_INPUT_UNDEFINED,
                                this,
                                inputRelation.getInputPointEntity());
                        success = false;

                        continue;
                    }
                }

                inputPoint._addResultRelation(inputRelation);
            }
        }

        return success;
    }

    private boolean _tidyReplicates()
    {
        if (_replicates == null) {
            return true;
        }

        boolean success = true;

        for (int i = 0; i < _replicates.size(); ++i) {
            final Replicate replicate = _replicates.get(i);
            PointEntity pointEntity = (PointEntity) replicate.getPoint();

            if (!pointEntity.isDefinition()) {
                pointEntity = pointEntity.getDefinition().orElse(null);

                if (pointEntity != null) {
                    _replicates
                        .set(
                            i,
                            new Point.Replicate(
                                pointEntity,
                                replicate.getConvert()));
                } else {
                    getThisLogger()
                        .error(
                            ServiceMessages.POINT_REPLICATE_UNDEFINED,
                            this,
                            replicate.getPoint());
                    success = false;
                }
            }
        }

        return success;
    }

    /** Default filter. */
    public static final ClassDef DEFAULT_FILTER = new ClassDefImpl(
        StepFilterFactory.class);

    /** Point element name. */
    public static final String ELEMENT_NAME = "Point";

    /** Point entity prefix. */
    public static final String ENTITY_PREFIX = "P";

    /** Point reference name. */
    public static final String ENTITY_REFERENCE_NAME = "point";

    /**  */

    private static final List<Replicate> _EMPTY_REPLICATES_LIST = Collections
        .unmodifiableList(new LinkedList<Replicate>());
    private static final List<PointInput> _EMPTY_RELATIONS_LIST = Collections
        .unmodifiableList(new LinkedList<PointInput>());

    private boolean _busy;
    private Optional<ContentEntity> _contentEntity;
    private volatile boolean _dropped;
    private List<PointInput> _inputRelations;
    private int _level = -1;
    private Boolean _nullRemoves;
    private Optional<OriginEntity> _originEntity;
    private Optional<PermissionsEntity> _permissionsEntity;
    private int _recalcLatest;
    private List<Replicate> _replicates;
    private List<PointInput> _resultRelations;
    private boolean _setUp;
    private Optional<StoreEntity> _storeEntity;
    private Optional<SyncEntity> _syncEntity;
    private boolean _tidied;
    private Optional<TransformEntity> _transformEntity;
    private boolean _volatile;

    /**
     * Defined Point Entity.
     */
    public static final class Definition
        extends PointEntity
    {
        /**
         * Constructs an instance.
         */
        public Definition() {}

        private Definition(final Definition other)
        {
            super(other);
        }

        /** {@inheritDoc}
         */
        @Override
        public Definition copy()
        {
            return new Definition(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object other)
        {
            if (this == other) {
                return true;
            }

            return super.equals(other);
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<PointEntity> getDefinition()
        {
            return Optional.of(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            return super.hashCode();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isDefinition()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void setDefinition(final PointEntity definition)
        {
            Require.failure();
        }
    }


    /**
     * Undefined Point Entity.
     */
    public static final class Reference
        extends PointEntity
    {
        /**
         * Constructs an instance.
         */
        public Reference() {}

        private Reference(final Reference other)
        {
            super(other);

            _definition = other._definition;
        }

        /** {@inheritDoc}
         */
        @Override
        public Reference copy()
        {
            return new Reference(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object other)
        {
            if (this == other) {
                return true;
            }

            return super.equals(other);
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<PointEntity> getDefinition()
        {
            return Optional.ofNullable(_definition);
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            return super.hashCode();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isDefinition()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public void setDefinition(final PointEntity definition)
        {
            _definition = definition;
        }

        private PointEntity _definition;
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
