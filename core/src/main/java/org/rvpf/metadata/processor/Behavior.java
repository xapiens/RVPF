/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Behavior.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.metadata.processor;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.BehaviorEntityReference;
import org.rvpf.metadata.entity.SyncEntity;
import org.rvpf.processor.behavior.PrimaryBehavior;

/**
 * Behavior.
 * <p>
 * Behavior instances are applied to input relations to control the effect of an
 * input on the production of results. They then select the appropriate
 * instances of their input.
 * </p>
 * <p>
 * Each step first calls a 'prepare' method, usually to request values from the
 * store. The associated method, {@link #trigger} or {@link #select} is then
 * called.
 * </p>
 * <p>
 * An important responsability of behaviors is to validate their context.
 * </p>
 *
 * @see BehaviorEntityReference
 */
public interface Behavior
    extends Proxied
{
    /**
     * Gets the first behavior with the specified Class.
     *
     * @param behaviorClass The behavior implementation Class.
     * @param <T> The type of the returned value.
     *
     * @return The optional requested behavior.
     */
    @Nonnull
    @CheckReturnValue
    <T extends Behavior> Optional<T> getBehavior(
            @Nonnull Class<T> behaviorClass);

    /**
     * Gets the behavior entity.
     *
     * @return The behavior entity.
     */
    @Nonnull
    @CheckReturnValue
    BehaviorEntity getEntity();

    /**
     * Gets the input point.
     *
     * @return The input point.
     */
    @Nonnull
    @CheckReturnValue
    Point getInputPoint();

    /**
     * Gets the next behavior.
     *
     * @return The next behavior or empty.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Behavior> getNext();

    /**
     * Gets the point relation to which this behavior instance is attached.
     *
     * @return The optional point relation.
     */
    @Nonnull
    @CheckReturnValue
    Optional<PointRelation> getRelation();

    /**
     * Gets the relation's sync.
     *
     * @return The relation's sync.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Sync> getRelationSync();

    /**
     * Gets the relation's sync entity.
     *
     * @return The relation's optional sync entity.
     */
    @Nonnull
    @CheckReturnValue
    Optional<SyncEntity> getRelationSyncEntity();

    /**
     * Gets the result point.
     *
     * @return The result point.
     */
    @Nonnull
    @CheckReturnValue
    Point getResultPoint();

    /**
     * Asks if this behavior is inherited.
     *
     * @return True if this behavior is inherited.
     */
    @CheckReturnValue
    boolean isInherited();

    /**
     * Asks if an input value should be extrapolated.
     *
     * @return True if the input value should be extrapolated.
     */
    @CheckReturnValue
    boolean isInputExtrapolated();

    /**
     * Asks if an input value should be interpolated.
     *
     * @return True if the input value should be interpolated.
     */
    @CheckReturnValue
    boolean isInputInterpolated();

    /**
     * Asks if an input value is required.
     *
     * @return True if an input value is required.
     */
    @CheckReturnValue
    boolean isInputRequired();

    /**
     * Asks if a point value is valid for the computation of a result value.
     *
     * @param inputValue The point value.
     * @param resultValue The result value.
     *
     * @return True if the point value is valid.
     */
    @CheckReturnValue
    boolean isInputValid(
            @Nonnull PointValue inputValue,
            @Nonnull ResultValue resultValue);

    /**
     * Asks if this behavior has primary responsibility.
     *
     * <p>Primary responsibility invloves mainly the trigger logic.</p>
     *
     * @return True if this behavior is primary.
     *
     * @see PrimaryBehavior
     */
    @CheckReturnValue
    boolean isPrimary();

    /**
     * Asks if result value has been fetched.
     *
     * @param noticeValue The triggering point value
     * @param resultValue The the result value.
     *
     * @return True if the result value has been fetched.
     */
    @CheckReturnValue
    boolean isResultFetched(
            @Nonnull PointValue noticeValue,
            @Nonnull ResultValue resultValue);

    /**
     * Asks if this behavior is synchronized.
     *
     * @return True if it is synchronized.
     */
    @CheckReturnValue
    boolean isSynchronized();

    /**
     * Returns a new result value.
     *
     * <p>This method is called when a new result value is needed to allow a
     * behavior to supply a specialized class able to hold behavior-specific
     * informations.</p>
     *
     * @param stamp The time stamp of the value.
     *
     * @return The new result value.
     */
    @Nonnull
    @CheckReturnValue
    ResultValue newResultValue(@Nonnull Optional<DateTime> stamp);

    /**
     * Allows a behavior to prepare for a call to {@link #select}.
     *
     * <p>The result value has been triggered either by this behavior or the
     * behavior of an other input.
     *
     * Will be called until all the batch behaviors return true.</p>
     *
     * @param resultValue The result value to be computed.
     * @param batch The current batch context.
     *
     * @return True if ready for select.
     */
    @CheckReturnValue
    boolean prepareSelect(
            @Nonnull ResultValue resultValue,
            @Nonnull Batch batch);

    /**
     * Allows a behavior to prepare for a call to {@link #trigger}.
     *
     * <p>Will be called until all the batch behaviors return true.</p>
     *
     * @param noticeValue A point value acting as potential trigger.
     * @param batch The current batch context.
     *
     * @return True if ready for trigger.
     */
    @CheckReturnValue
    boolean prepareTrigger(
            @Nonnull PointValue noticeValue,
            @Nonnull Batch batch);

    /**
     * Selects input values for computing the result.
     *
     * <p>This is called after all the calls to the {@link #prepareSelect}
     * method have returned true.</p>
     *
     * @param resultValue The result value to be computed.
     * @param batch The current batch context.
     *
     * @return False if a problem was identified.
     */
    @CheckReturnValue
    boolean select(@Nonnull ResultValue resultValue, @Nonnull Batch batch);

    /**
     * Sets the inherited indicator.
     *
     * @param inherited A new value for the inherited indicator.
     */
    void setInherited(boolean inherited);

    /**
     * Sets the next behavior.
     *
     * @param next The next behavior.
     */
    void setNext(@Nonnull Behavior next);

    /**
     * Sets the point relation to which this behavior instance is attached.
     *
     * @param relation The point relation.
     */
    void setRelation(@Nonnull PointRelation relation);

    /**
     * Triggers the computation of results if needed.
     *
     * <p>This is called after all the calls to the {@link #prepareTrigger}
     * method have returned true.</p>
     *
     * @param noticeValue A point value acting as potential trigger.
     * @param batch The current batch context.
     */
    void trigger(@Nonnull PointValue noticeValue, @Nonnull Batch batch);

    /**
     * Validates this behavior.
     *
     * @return True when valid.
     */
    @CheckReturnValue
    boolean validate();
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
