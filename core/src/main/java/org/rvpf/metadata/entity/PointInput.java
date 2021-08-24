/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointInput.java 4078 2019-06-11 20:55:00Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.service.ServiceMessages;

/**
 * Point input.
 */
@NotThreadSafe
public final class PointInput
    implements PointRelation
{
    /**
     * Constructs an instance.
     *
     * @param original The original point input.
     */
    public PointInput(@Nonnull final PointInput original)
    {
        _behavior = original._behavior;
        _control = original._control;
        _inputPoint = original._inputPoint;
        _params = original._params;
        _resultPoint = original._resultPoint;
        _syncEntity = original._syncEntity;
        _validated = original._validated;
    }

    /**
     * Creates an instance.
     *
     * @param inputPoint The point entity providing inputs.
     * @param resultPoint The point entity holding results.
     */
    public PointInput(
            @Nonnull final PointEntity inputPoint,
            @Nonnull final PointEntity resultPoint)
    {
        _behavior = Optional.empty();
        _inputPoint = Require.notNull(inputPoint);
        _params = Optional.empty();
        _resultPoint = Require.notNull(resultPoint);
        _syncEntity = Optional.empty();
    }

    /**
     * Adds a behavior entity to this relation.
     *
     * @param behaviorEntity The behavior entity.
     */
    public void addBehaviorEntity(@Nonnull final BehaviorEntity behaviorEntity)
    {
        _addBehaviorEntity(behaviorEntity, false);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (other instanceof PointInput) {
            final UUID inputPointUUID = getInputPoint().getUUID().get();
            final PointInput otherInput = (PointInput) other;

            if (!inputPointUUID
                .equals(otherInput.getInputPoint().getUUID().get())) {
                return false;
            }

            final UUID resultPointUUID = getResultPoint().getUUID().get();

            if (!resultPointUUID
                .equals(otherInput.getResultPoint().getUUID().get())) {
                return false;
            }

            if (!Objects.equals(_behavior, otherInput._behavior)) {
                return false;
            }

            if (!Objects.equals(_syncEntity, otherInput._syncEntity)) {
                return false;
            }

            return getParams().equals(otherInput.getParams());
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public Point getInputPoint()
    {
        return _inputPoint;
    }

    /**
     * Gets the input point entity.
     *
     * @return The input point entity.
     */
    @Nonnull
    @CheckReturnValue
    public PointEntity getInputPointEntity()
    {
        return _inputPoint;
    }

    /** {@inheritDoc}
     */
    @Override
    public Params getParams()
    {
        return _params.isPresent()? _params.get(): Params.EMPTY_PARAMS;
    }

    /**
     * Gets the primary behavior.
     *
     * @return The optional primary behavior.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Behavior> getPrimaryBehavior()
    {
        return _behavior;
    }

    /** {@inheritDoc}
     */
    @Override
    public Point getResultPoint()
    {
        return _resultPoint;
    }

    /**
     * Gets the result point entity.
     *
     * @return The result point entity.
     */
    @Nonnull
    @CheckReturnValue
    public PointEntity getResultPointEntity()
    {
        return _resultPoint;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Sync> getSync()
    {
        return Optional
            .ofNullable(
                (_syncEntity.isPresent())? _syncEntity.get().getSync(): null);
    }

    /**
     * Gets the sync entity.
     *
     * @return The optional sync entity.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<SyncEntity> getSyncEntity()
    {
        return _syncEntity;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Asks if this is a control input.
     *
     * @return True if it is a control input.
     */
    @CheckReturnValue
    public boolean isControl()
    {
        return _control;
    }

    /**
     * Sets the control input indicator.
     *
     * @param control The control input indicator.
     */
    public void setControl(final boolean control)
    {
        _control = control;
    }

    /**
     * Sets a new input point.
     *
     * @param point The new input point.
     */
    public void setInputPoint(@Nonnull final Point point)
    {
        _inputPoint = Require.notNull((PointEntity) point);
    }

    /**
     * Sets the params.
     *
     * @param params The optional params.
     */
    public void setParams(@Nonnull final Optional<Params> params)
    {
        _params = params;

        if (_params.isPresent()) {
            _params.get().freeze();
        }
    }

    /**
     * Sets a new result point.
     *
     * @param point The new result point.
     */
    public void setResultPoint(@Nonnull final Point point)
    {
        _resultPoint = Require.notNull((PointEntity) point);
    }

    /**
     * Sets the sync entity.
     *
     * @param syncEntity The optional sync entity.
     */
    public void setSyncEntity(@Nonnull final Optional<SyncEntity> syncEntity)
    {
        _syncEntity = syncEntity;
    }

    /**
     * Sets up this.
     *
     * @param metadata The metadata.
     * @param arg The optional corresponding transform argument.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(
            @Nonnull final Metadata metadata,
            @Nonnull final Optional<TransformEntity.Arg> arg)
    {
        if (_syncEntity.isPresent() && !_syncEntity.get().setUp()) {
            return false;
        }

        if (arg.isPresent()) {
            final Params argParams = arg.get().getParams();

            if (!argParams.isEmpty()) {
                if (!_params.isPresent()) {
                    _params = Optional.of(new Params());
                }

                _params.get().setDefaults(argParams);
                _params.get().freeze();
            }
        }

        if ((_resultPoint.getTransform().isPresent())
                && (!_behavior.isPresent()
                    || (_behavior.get() instanceof BehaviorEntityReference))) {
            BehaviorEntityReference primaryBehaviorReference = null;

            if (_behavior.isPresent()) {
                primaryBehaviorReference = _primaryBehaviorReference(metadata);

                if (primaryBehaviorReference == null) {
                    return false;
                }

                if (!primaryBehaviorReference.isPrimary()) {
                    primaryBehaviorReference = null;
                }
            }

            if ((primaryBehaviorReference == null) && arg.isPresent()) {
                for (final BehaviorEntity behavior:
                        arg.get().getBehaviorEntities()) {
                    _addBehaviorEntity(behavior, true);
                }

                if (_behavior.isPresent()) {
                    primaryBehaviorReference = _primaryBehaviorReference(
                        metadata);

                    if (primaryBehaviorReference == null) {
                        return false;
                    }

                    if (!primaryBehaviorReference.isPrimary()) {
                        primaryBehaviorReference = null;
                    }
                }
            }

            if (primaryBehaviorReference == null) {
                final Optional<BehaviorEntity> primaryBehaviorEntity =
                    _resultPoint
                        .getTransformEntity()
                        .get()
                        .getDefaultBehavior(this);

                if (primaryBehaviorEntity.isPresent()) {
                    _addBehaviorEntity(primaryBehaviorEntity.get(), true);
                    primaryBehaviorReference = _primaryBehaviorReference(
                        metadata);

                    if (primaryBehaviorReference == null) {
                        return false;
                    }

                    if (!primaryBehaviorReference.isPrimary()) {
                        primaryBehaviorReference = null;
                    }
                }
            }

            if (primaryBehaviorReference == null) {
                _LOGGER.warn(ServiceMessages.NO_PRIMARY, _resultPoint);

                return false;
            }

            _behavior = Optional
                .ofNullable(
                    ((BehaviorEntityReference) _behavior.get()).instantiate());

            if (!_behavior.isPresent()) {
                return false;
            }

            _validated = false;
        }

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    public void tearDown()
    {
        if (_syncEntity.isPresent()) {
            _syncEntity.get().tearDown();
        }

        if (_behavior.isPresent()) {
            _behavior.get().tearDown();
        }
    }

    /**
     * Validates the relation.
     *
     * @return True if the relation is validated.
     */
    @CheckReturnValue
    boolean validate()
    {
        if (!_validated && _behavior.isPresent()) {
            _validated = _behavior.get().validate();
        }

        return _validated;
    }

    private void _addBehaviorEntity(
            final BehaviorEntity behaviorEntity,
            final boolean inherited)
    {
        final BehaviorEntityReference reference = new BehaviorEntityReference(
            this,
            behaviorEntity,
            inherited);

        if (_behavior.isPresent()) {
            ((BehaviorEntityReference) _behavior.get()).add(reference);
        } else {
            _behavior = Optional.of(reference);
        }
    }

    private BehaviorEntityReference _primaryBehaviorReference(
            final Metadata metadata)
    {
        if (!((BehaviorEntityReference) _behavior.get()).setUp(metadata)) {
            return null;
        }

        final BehaviorEntityReference primaryBehaviorReference =
            ((BehaviorEntityReference) _behavior
                .get())
                .primary();

        if (primaryBehaviorReference == null) {
            return null;
        }

        if (primaryBehaviorReference.isPrimary()
                && (_behavior.get() != primaryBehaviorReference)) {
            primaryBehaviorReference
                .add((BehaviorEntityReference) _behavior.get());
            _behavior = Optional.of(primaryBehaviorReference);
        }

        return primaryBehaviorReference;
    }

    private static final Logger _LOGGER = Logger.getInstance(PointInput.class);

    private Optional<Behavior> _behavior;
    private boolean _control;
    private PointEntity _inputPoint;
    private Optional<Params> _params;
    private PointEntity _resultPoint;
    private Optional<SyncEntity> _syncEntity;
    private boolean _validated;
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
