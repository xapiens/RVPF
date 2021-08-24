/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Batch.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.metadata.processor;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.DateTime;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;

/**
 * Batch.
 */
public interface Batch
{
    /**
     * Accepts notices.
     *
     * @param notices The notices.
     *
     * @throws MemoryLimitException When the memory limit is exceeded.
     */
    void acceptNotices(
            @Nonnull Collection<PointValue> notices)
        throws MemoryLimitException;

    /**
     * Adds a store values query for the point's store.
     *
     * @param storeValuesQuery The store values query
     */
    void addStoreValuesQuery(@Nonnull StoreValuesQuery storeValuesQuery);

    /**
     * Adds an update.
     *
     * @param pointValue The point value update.
     */
    void addUpdate(@Nonnull PointValue pointValue);

    /**
     * Forgets an input value.
     *
     * <p>This is used by the replication logic.</p>
     *
     * @param inputValue The value to forget.
     */
    void forgetInputValue(@Nonnull PointValue inputValue);

    /**
     * Gets the look up pass.
     *
     * @return The look up pass (first is 1).
     */
    @CheckReturnValue
    int getLookUpPass();

    /**
     * Gets a point value as specified in a batch query.
     *
     * @param batchQuery The batch query.
     *
     * @return The point value (with null stamp if absent).
     */
    @Nonnull
    @CheckReturnValue
    PointValue getPointValue(@Nonnull final BatchValuesQuery batchQuery);

    /**
     * Gets point values as specified in a batch query.
     *
     * @param batchQuery The batch query.
     *
     * @return The point values.
     */
    @Nonnull
    @CheckReturnValue
    Collection<PointValue> getPointValues(
            @Nonnull final BatchValuesQuery batchQuery);

    /**
     * Gets a result value.
     *
     * @param pointValue A point value.
     *
     * @return The optional result value.
     */
    @Nonnull
    @CheckReturnValue
    Optional<ResultValue> getResultValue(@Nonnull final PointValue pointValue);

    /**
     * Queues a signal
     *
     * @param name The signal's name.
     * @param info Additional optional informations.
     */
    void queueSignal(
            @Nonnull final String name,
            @Nonnull final Optional<String> info);

    /**
     * Replaces the supplied result value.
     *
     * @param resultValue A result value.
     * @param caller The behavior calling this.
     *
     * @return A caller supplied result value (null on failure).
     */
    @Nullable
    @CheckReturnValue
    ResultValue replaceResultValue(
            @Nonnull ResultValue resultValue,
            @Nonnull final Behavior caller);

    /**
     * Schedules an update.
     *
     * @param update The update.
     */
    void scheduleUpdate(@Nonnull PointValue update);

    /**
     * Sets the cutoff to the point value.
     *
     * <p>An empty point value clears the cutoff.</p>
     *
     * @param pointValue The point value holding the time (clock content).
     */
    void setCutoff(@Nonnull Optional<NormalizedValue> pointValue);

    /**
     * Sets up a result for processing.
     *
     * @param stamp Time specification for the result.
     * @param caller The Behavior calling this.
     *
     * @return The result.
     */
    @Nonnull
    ResultValue setUpResultValue(
            @Nonnull final DateTime stamp,
            @Nonnull final Behavior caller);

    /**
     * Sets up a result for processing.
     *
     * @param newResult The new result.
     * @param caller The behavior calling this.
     *
     * @return The result.
     */
    @Nonnull
    ResultValue setUpResultValue(
            @Nonnull final ResultValue newResult,
            @Nonnull final Behavior caller);

    /**
     * Sets up a result value for processing.
     *
     * @param notice The point value triggering the set up of the result.
     * @param stamp Time specification for the result.
     * @param caller The behavior calling this.
     *
     * @return The result value (null on failure).
     */
    @Nullable
    ResultValue setUpResultValue(
            @Nonnull final PointValue notice,
            @Nonnull final DateTime stamp,
            final Behavior caller);

    /**
     * Sets the updates filtered control value.
     *
     * @param updatesFiltered The updates filtered control value.
     */
    void setUpdatesFiltered(final boolean updatesFiltered);

    /** Disabled update marker. */
    PointValue DISABLED_UPDATE = new PointValue() {}
    ;

    /** Point event comparator. * */
    Comparator<PointValue> POINT_EVENT_COMPARATOR = Comparator
        .comparing(PointValue::getNullablePoint)
        .thenComparing(Comparator.comparing(PointValue::getStamp));
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
