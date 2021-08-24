/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BehaviorEntityReference.java 4082 2019-06-14 18:38:55Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.DateTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.service.ServiceMessages;

/**
 * Behavior entity reference.
 *
 * <p>Instances of this class are created while loading the metadata. They
 * establish the behavior chain for a point input without having to instantiate
 * these behaviors.</p>
 *
 * <p>When the processor service has pruned unused points, the
 * {@link org.rvpf.metadata.entity.PointInput#setUp} method will be able to shed
 * these instances, replacing them with the actual behaviors with a single call
 * to {@link #instantiate}.</p>
 */
public final class BehaviorEntityReference
    implements Behavior
{
    /**
     * Constructs an instance.
     *
     * @param relation The point relation holding the reference.
     * @param entity The referenced behavior entity.
     * @param inherited The inherited indicator.
     */
    public BehaviorEntityReference(
            @Nonnull final PointRelation relation,
            @Nonnull final BehaviorEntity entity,
            final boolean inherited)
    {
        _relation = (PointInput) relation;
        _entity = entity;
        _inherited = inherited;
    }

    /**
     * Adds a reference on the behavior chain.
     *
     * @param reference The reference to a behavior entity.
     */
    public void add(@Nonnull final BehaviorEntityReference reference)
    {
        final Optional<Behavior> nextBehavior = getNext();

        if (nextBehavior.isPresent()) {
            ((BehaviorEntityReference) nextBehavior.get()).add(reference);
        } else {
            _next = reference;
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public <T extends Behavior> Optional<T> getBehavior(
            final Class<T> behaviorClass)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public BehaviorEntity getEntity()
    {
        return _entity;
    }

    /** {@inheritDoc}
     */
    @Override
    public Point getInputPoint()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getName()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Behavior> getNext()
    {
        return Optional.ofNullable(_next);
    }

    /** {@inheritDoc}
     */
    @Override
    public Params getParams()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public ProxyEntity getProxyEntity()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<PointRelation> getRelation()
    {
        return Optional.ofNullable(_relation);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Sync> getRelationSync()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<SyncEntity> getRelationSyncEntity()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public Point getResultPoint()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Instantiates the actual behavior.
     *
     * <p>This method recreates the behavior chain by creating fresh instances
     * of the actual behavior classes.</p>
     *
     * @return The actual behavior instance (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public Behavior instantiate()
    {
        final Class<?> behaviorClass = getEntity()
            .getInstance()
            .get()
            .getClass();
        final Behavior instance;
        Behavior next;

        try {
            instance = (Behavior) behaviorClass.getConstructor().newInstance();
        } catch (final ClassCastException exception) {
            _LOGGER
                .warn(
                    ServiceMessages.BEHAVIORS_CLASS,
                    Abstract.class.getName());

            return null;
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }

        instance.setRelation(getRelation().orElse(null));
        instance.setInherited(isInherited());

        if (!instance.setUp(_metadata, getEntity())) {
            return null;
        }

        next = getNext().orElse(null);

        if (next != null) {
            next = ((BehaviorEntityReference) next).instantiate();

            if (next == null) {
                return null;
            }

            instance.setNext(next);
        }

        return instance;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInherited()
    {
        return _inherited;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInputExtrapolated()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInputInterpolated()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInputRequired()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInputValid(
            final PointValue inputValue,
            final ResultValue resultValue)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isPrimary()
    {
        return ((Behavior) getEntity().getInstance().get()).isPrimary();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isResultFetched(
            final PointValue noticeValue,
            final ResultValue resultValue)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSynchronized()
    {
        return ((Behavior) getEntity().getInstance().get()).isSynchronized();
    }

    /** {@inheritDoc}
     */
    @Override
    public ResultValue newResultValue(final Optional<DateTime> stamp)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean prepareSelect(
            final ResultValue resultValue,
            final Batch batch)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean prepareTrigger(
            final PointValue noticeValue,
            final Batch batch)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the primary Behavior entity reference.
     *
     * <p>This method will return null only when a problem is detected. It will
     * return the first behavior entity reference if no behavior is primary.</p>
     *
     * <p>When it returns a primary behavior, it is removed from the chain.</p>
     *
     * @return The primary behavior entity reference (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public BehaviorEntityReference primary()
    {
        return _primary(false);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean select(final ResultValue resultValue, final Batch batch)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setInherited(final boolean inherited)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setNext(final Behavior next)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setRelation(final PointRelation relation)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets up the behaviors in the chain.
     *
     * @param metadata The metadata available to the current process.
     *
     * @return True if successful.
     */
    @CheckReturnValue
    public boolean setUp(@Nonnull final Metadata metadata)
    {
        _metadata = metadata;

        if (!getEntity().setUp(metadata)) {
            return false;
        }

        return (_next == null)
               || ((BehaviorEntityReference) _next).setUp(metadata);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public void trigger(final PointValue noticeValue, final Batch batch)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean validate()
    {
        throw new UnsupportedOperationException();
    }

    private BehaviorEntityReference _primary(final boolean isFound)
    {
        final boolean isSynchronized = isSynchronized();
        final boolean isPrimary = !isSynchronized && isPrimary();
        final BehaviorEntityReference next = (BehaviorEntityReference) getNext()
            .orElse(null);
        final BehaviorEntityReference nextPrimary = (next != null)? next
            ._primary(isPrimary | isFound): null;

        if (isFound) {
            if (isPrimary) {    // Needs computation of 'nextPrimary'.
                _LOGGER
                    .warn(ServiceMessages.MULTIPLE_PRIMARY, getResultPoint());
            }

            return null;
        }

        if ((nextPrimary != null) && nextPrimary.isPrimary()) {
            if (nextPrimary == next) {
                _next = null;
            }

            return nextPrimary;
        }

        return this;
    }

    private static final Logger _LOGGER = Logger
        .getInstance(BehaviorEntityReference.class);

    private BehaviorEntity _entity;
    private boolean _inherited;
    private Metadata _metadata;
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
