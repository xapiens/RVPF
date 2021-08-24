/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractBehavior.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.processor.behavior;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Origin;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.entity.SyncEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.ProcessorMessages;

/**
 * Abstract behavior.
 */
public abstract class AbstractBehavior
    implements Behavior
{
    /** {@inheritDoc}
     *
     * <p>Extends comparison to the behaviors chain.</p>
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }

        final AbstractBehavior otherBehavior = (AbstractBehavior) other;

        if (!getEntity().equals(otherBehavior.getEntity())) {
            return false;
        }

        if (isInherited() != otherBehavior.isInherited()) {
            return false;
        }

        final Optional<Behavior> nextBehavior = getNext();

        if (!nextBehavior.isPresent()) {
            return !otherBehavior.getNext().isPresent();
        }

        return getNext().equals(otherBehavior.getNext());
    }

    /** {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public final <T extends Behavior> Optional<T> getBehavior(
            final Class<T> behaviorClass)
    {
        if (behaviorClass.isInstance(this)) {
            return Optional.of((T) this);
        } else if (_next != null) {
            return _next.getBehavior(behaviorClass);
        }

        return Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public final BehaviorEntity getEntity()
    {
        return Require.notNull(_entity);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Point getInputPoint()
    {
        return _relation.getInputPoint();
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getName()
    {
        return _entity.getName().orElseGet(() -> getClass().getName());
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<Behavior> getNext()
    {
        return Optional.ofNullable(_next);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Params getParams()
    {
        return _entity.getParams();
    }

    /** {@inheritDoc}
     */
    @Override
    public final ProxyEntity getProxyEntity()
    {
        return getEntity();
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<PointRelation> getRelation()
    {
        return Optional.ofNullable(_relation);
    }

    /** {@inheritDoc}
     */
    @Override
    public final Optional<Sync> getRelationSync()
    {
        return _relation.getSync();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<SyncEntity> getRelationSyncEntity()
    {
        return _relation.getSyncEntity();
    }

    /** {@inheritDoc}
     */
    @Override
    public Point getResultPoint()
    {
        return _relation.getResultPoint();
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isInherited()
    {
        return _inherited;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInputExtrapolated()
    {
        return (_next != null) && _next.isInputExtrapolated();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInputInterpolated()
    {
        return (_next != null) && _next.isInputInterpolated();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInputRequired()
    {
        return (_next != null) && _next.isInputRequired();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInputValid(
            final PointValue inputValue,
            final ResultValue resultValue)
    {
        return (_next == null) || _next.isInputValid(inputValue, resultValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isPrimary()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isResultFetched(
            final PointValue noticeValue,
            final ResultValue resultValue)
    {
        return (_next != null)
               && _next.isResultFetched(noticeValue, resultValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSynchronized()
    {
        return (_next != null) && _next.isSynchronized();
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultValue newResultValue(final Optional<DateTime> stamp)
    {
        return new ResultValue(getResultPoint(), stamp);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean prepareSelect(
            final ResultValue resultValue,
            final Batch batch)
    {
        return (_next == null) || _next.prepareSelect(resultValue, batch);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean prepareTrigger(
            final PointValue noticeValue,
            final Batch batch)
    {
        return (_next == null) || _next.prepareTrigger(noticeValue, batch);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean select(final ResultValue resultValue, final Batch batch)
    {
        return (_next == null) || _next.select(resultValue, batch);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setInherited(final boolean inherited)
    {
        _inherited = inherited;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setNext(final Behavior next)
    {
        _next = next;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void setRelation(final PointRelation relation)
    {
        _relation = (PointInput) relation;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        _entity = (BehaviorEntity) proxyEntity;

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_next != null) {
            _next.tearDown();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getClass()
            .getSimpleName() + "@" + Integer.toHexString(
                System.identityHashCode(this));
    }

    /** {@inheritDoc}
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        if (_next != null) {
            _next.trigger(noticeValue, batch);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean validate()
    {
        if (!doValidate()) {
            return false;
        }

        return (_next == null) || _next.validate();
    }

    /**
     * Does validate this behavior.
     *
     * @return True when valid.
     */
    @CheckReturnValue
    protected boolean doValidate()
    {
        return true;
    }

    /**
     * Gets a boolean value for a key, providing a default.
     *
     * @param key The name of the value.
     * @param defaultValue The default value for the key.
     *
     * @return The requested value or the provided default.
     */
    @CheckReturnValue
    protected boolean getBooleanInputValue(
            @Nonnull final String key,
            final boolean defaultValue)
    {
        final PointRelation relation = getRelation().get();
        final boolean pointParamValue = relation
            .getInputPoint()
            .getParams()
            .getBoolean(key, defaultValue);

        return relation.getParams().getBoolean(key, pointParamValue);
    }

    /**
     * Gets a double value for a key, providing a default.
     *
     * @param key The name of the value.
     * @param defaultValue The default value for the key.
     *
     * @return The requested value or the provided default.
     */
    @CheckReturnValue
    protected double getDoubleInputValue(
            @Nonnull final String key,
            final double defaultValue)
    {
        final PointRelation relation = getRelation().get();
        final double pointParamValue = relation
            .getInputPoint()
            .getParams()
            .getDouble(key, defaultValue);

        return relation.getParams().getDouble(key, pointParamValue);
    }

    /**
     * Gets an elapsed time value for a key, providing a default.
     *
     * @param key The name of the value.
     * @param defaultValue The optional default value for the key.
     * @param emptyValue The optional assumed value for empty.
     *
     * @return The requested value, empty, or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    protected Optional<ElapsedTime> getElapsedInputValue(
            @Nonnull final String key,
            @Nonnull final Optional<ElapsedTime> defaultValue,
            @Nonnull final Optional<ElapsedTime> emptyValue)
    {
        final PointRelation relation = getRelation().get();
        final Optional<ElapsedTime> pointParamValue = relation
            .getInputPoint()
            .getParams()
            .getElapsed(key, defaultValue, emptyValue);

        return relation
            .getParams()
            .getElapsed(key, pointParamValue, emptyValue);
    }

    /**
     * Gets the first relation with the specified primary behavior Class.
     *
     * @param behaviorClass The behavior implementation class.
     * @param optional True if the relation is optional.
     *
     * @return The requested relation or empty.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<PointInput> getInput(
            @Nonnull final Class<? extends Behavior> behaviorClass,
            final boolean optional)
    {
        final List<? extends PointRelation> inputs = getResultPoint()
            .getInputs();

        for (final PointRelation element: inputs) {
            final PointInput input = (PointInput) element;

            if (behaviorClass
                .isInstance(input.getPrimaryBehavior().orElse(null))) {
                return Optional.of(input);
            }
        }

        if (!optional) {
            getThisLogger()
                .warn(
                    ProcessorMessages.INPUT_BEHAVIOR_NOT_FOUND,
                    behaviorClass.getSimpleName());
        }

        return Optional.empty();
    }

    /**
     * Gets all relations with the specified primary behavior class.
     *
     * @param behaviorClass The behavior implementation class.
     *
     * @return The list of the requested relations.
     */
    @Nonnull
    @CheckReturnValue
    protected final List<PointRelation> getRelations(
            @Nonnull final Class<? extends Behavior> behaviorClass)
    {
        final List<? extends PointRelation> inputs = getResultPoint()
            .getInputs();
        final List<PointRelation> relations = new LinkedList<PointRelation>();

        for (final PointRelation element: inputs) {
            final PointInput input = (PointInput) element;

            if (behaviorClass
                .isInstance(input.getPrimaryBehavior().orElse(null))) {
                relations.add(input);
            }
        }

        return relations;
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return _logger;
    }

    /**
     * Gets the result point's transform.
     *
     * @return The transform.
     */
    @Nonnull
    @CheckReturnValue
    protected final Transform getTransform()
    {
        return ((PointEntity) getResultPoint()).getTransform().get();
    }

    /**
     * Validates that the point does not have results other than itself on the
     * same processor.
     *
     * @return True when validated.
     */
    @CheckReturnValue
    protected final boolean validateNoResults()
    {
        final Point point = getResultPoint();
        final Origin origin = point.getOrigin().orElse(null);

        for (final PointRelation result: point.getResults()) {
            final Point resultPoint = result.getResultPoint();

            if ((resultPoint != point)
                    && (resultPoint.getOrigin().orElse(null) == origin)) {
                getThisLogger()
                    .error(ProcessorMessages.POINT_NOT_INPUT, getResultPoint());

                return false;
            }
        }

        return true;
    }

    /**
     * Validates the result point's transform.
     *
     * <p>To be valid, the transform must implement or extend the support
     * class.</p>
     *
     * @param supportClass The support class.
     *
     * @return True when the transform is valid.
     */
    @CheckReturnValue
    protected final boolean validateTransform(
            @Nonnull final Class<? extends Transform> supportClass)
    {
        if (!supportClass.isInstance(getTransform())) {
            getThisLogger()
                .error(
                    ProcessorMessages.BEHAVIOR_TRANSFORM,
                    getName(),
                    getTransform().getName());

            return false;
        }

        return true;
    }

    private BehaviorEntity _entity;
    private boolean _inherited;
    private final Logger _logger = Logger.getInstance(getClass());
    private Behavior _next;
    private PointInput _relation;
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
