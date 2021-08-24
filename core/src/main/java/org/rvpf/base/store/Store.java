/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Store.java 4079 2019-06-12 13:40:21Z SFB $
 */

package org.rvpf.base.store;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.value.PointValue;

/**
 * Store.
 *
 * <p>This interface is the client's view of the point values storage system. It
 * is used to query and update values.</p>
 */
public interface Store
    extends Comparable<Store>
{
    /**
     * Adds a store query for point values.
     *
     * <p>The queries will be sent on the next call to {@link #nextValues}.</p>
     *
     * @param query The store query.
     *
     * @return True unless cancelled.
     */
    @CheckReturnValue
    boolean addQuery(@Nonnull StoreValuesQuery query);

    /**
     * Added an update for a point value.
     *
     * <p>The updates will be sent on the next call to {@link #sendUpdates}.</p>
     *
     * @param pointValue The point value.
     *
     * @see #supportsDelete()
     */
    void addUpdate(@Nonnull PointValue pointValue);

    /**
     * Requests the binding of a point.
     *
     * <p>This allows communications with stores on different metadata domains.
     * The client and server will try to map the possibly different UUID using
     * the point's 'Tag' parameter (the point's name will be used when this
     * parameter is absent).</p>
     *
     * @param point The point.
     */
    void bind(@Nonnull Point point);

    /**
     * Asks if this store can confirm point values.
     *
     * @return True if it can confirm point values.
     */
    @CheckReturnValue
    boolean canConfirm();

    /**
     * Closes the store.
     *
     * <p>May be called redundantly.</p>
     */
    void close();

    /**
     * Confirms a point value.
     *
     * @param pointValue The point value.
     * @param confirmValue When true, {@link PointValue#sameValueAs} should be
     *                     called.
     *
     * @return True to confirm.
     *
     * @throws InterruptedException When interrupted.
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean confirm(
            @Nonnull PointValue pointValue,
            boolean confirmValue)
        throws InterruptedException, StoreAccessException;

    /**
     * Connects.
     *
     * @throws StoreAccessException On store access problem.
     */
    void connect()
        throws StoreAccessException;

    /**
     * Delivers subscribed point values from store update events.
     *
     * <p>A 'deliver' request is an alternative to a 'pull' request. Starting
     * values are taken as the return of the 'subscribe' request; the follow up
     * is then made with 'deliver' request.</p>
     *
     * @param limit A limit for the number of values.
     * @param timeout A time limit in millis to wait for the first message
     *                (negative for infinite).
     *
     * @return The store values.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws StoreAccessException On store access problem.
     *
     * @see #supportsDeliver()
     */
    @Nonnull
    @CheckReturnValue
    StoreValues deliver(
            final int limit,
            final long timeout)
        throws InterruptedException, StoreAccessException;

    /**
     * Gets the exceptions.
     *
     * @return The optional exceptions (each null on success).
     */
    @Nonnull
    @CheckReturnValue
    Optional<Exception[]> getExceptions();

    /**
     * Gets the store entity name.
     *
     * @return The store entity name.
     */
    @Nonnull
    @CheckReturnValue
    String getName();

    /**
     * Gets this store's parameters.
     *
     * @return The store's parameters.
     */
    @Nonnull
    @CheckReturnValue
    Params getParams();

    /**
     * Gets the queries batch limit.
     *
     * @return The queries batch limit.
     */
    @CheckReturnValue
    int getQueriesBatchLimit();

    /**
     * Gets the response limit.
     *
     * @return The response limit.
     */
    @CheckReturnValue
    int getResponseLimit();

    /**
     * Gets the values from the subscribed call.
     *
     * @return The optional values.
     */
    @Nonnull
    @CheckReturnValue
    Optional<StoreValues[]> getSubscribedValues();

    /**
     * Gest the store entity UUID.
     *
     * @return The store entity UUID.
     */
    @Nonnull
    @CheckReturnValue
    UUID getUUID();

    /**
     * Gets the update count.
     *
     * @return The update count.
     */
    @CheckReturnValue
    int getUpdateCount();

    /**
     * Gets the updates not yet sent.
     *
     * @return The updates.
     */
    @Nonnull
    @CheckReturnValue
    Collection<PointValue> getUpdates();

    /**
     * Impersonates an other user.
     *
     * <p>Ends the current impersonation, if any, and begins a new one if the
     * user argument is not empty.
     *
     * @param user The other user (empty string for anonymous, empty to cancel).
     *
     * @throws StoreAccessException On store access problem.
     */
    void impersonate(
            @Nonnull Optional<String> user)
        throws StoreAccessException;

    /**
     * Asks if a null value removes that value.
     *
     * @return True if null removes.
     */
    @CheckReturnValue
    boolean isNullRemoves();

    /**
     * Iterates on the point values returned for a store query.
     *
     * <p>The iteration can extend beyond the store query limit.</p>
     *
     * <p>Note: the returned iterable and its iterator are not thread safe.</p>
     *
     * @param storeQuery The store query.
     *
     * @return An iterable on store values.
     */
    @Nonnull
    @CheckReturnValue
    Iterable<PointValue> iterate(@Nonnull StoreValuesQuery storeQuery);

    /**
     * Returns the next store values to our store queries.
     *
     * <p>Any pending query is first sent.</p>
     *
     * @return The next store values or empty when done.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws StoreAccessException On store access problem.
     */
    @Nonnull
    @CheckReturnValue
    Optional<StoreValues> nextValues()
        throws InterruptedException, StoreAccessException;

    /**
     * Probes the state of the store session.
     *
     * @return True when everything is fine.
     *
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean probe()
        throws StoreAccessException;

    /**
     * Pulls points values.
     *
     * <p>This differs from a 'select' call by insisting for a 'pull' query and
     * allowing a wait for a non empty response. Also, if there are subscribed
     * points, the returned values will be restricted to those points.</p>
     *
     * <p>A 'pull' query gets point values based on the time of their last
     * update ('version') instead of the time associated with the value
     * ('stamp').</p>
     *
     * <p>Note: the filtering for subscribed point values may represent a
     * significant overhead; when appropriate, 'deliver' could provide better
     * performance.</p>
     *
     * @param query The store query (must be a 'pull' query).
     * @param timeout A time limit in millis to wait for the first value
     *                (negative for infinite).
     *
     * @return The points values.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws StoreAccessException On store access problem.
     *
     * @see #select(StoreValuesQuery)
     * @see #deliver(int, long)
     * @see #supportsPull()
     */
    @Nonnull
    @CheckReturnValue
    StoreValues pull(
            @Nonnull StoreValuesQuery query,
            long timeout)
        throws InterruptedException, StoreAccessException;

    /**
     * Purges point values.
     *
     * @param pointUUIDs The UUID of the points to purge.
     * @param timeInterval A time interval.
     *
     * @return The number of point values purged.
     *
     * @throws StoreAccessException On store access problem.
     *
     * @see #supportsPurge()
     */
    @CheckReturnValue
    int purge(
            @Nonnull UUID[] pointUUIDs,
            @Nonnull TimeInterval timeInterval)
        throws StoreAccessException;

    /**
     * Selects the store values for a store query.
     *
     * <p>The processing of the query must not interfere with the batch
     * operation mode.</p>
     *
     * @param query The store query.
     *
     * @return The store values.
     *
     * @throws InterruptedException When the service is stopped.
     * @throws StoreAccessException On store access problem.
     */
    @Nonnull
    @CheckReturnValue
    StoreValues select(
            @Nonnull StoreValuesQuery query)
        throws InterruptedException, StoreAccessException;

    /**
     * Sends the pending updates.
     *
     * @return False when a warning has been logged.
     *
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean sendUpdates()
        throws StoreAccessException;

    /**
     * Sends updates.
     *
     * @param updates The updates to send.
     *
     * @return False when a warning has been logged.
     *
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean sendUpdates(
            @Nonnull Collection<PointValue> updates)
        throws StoreAccessException;

    /**
     * Subscribes to point values store update events.
     *
     * <p>This calls collects the last value for each point at the time of the
     * subscription. These values will be available with a call to
     * 'getSubscribedValues'.</p>
     *
     * @param points The points.
     *
     * @return False when a warning has been logged.
     *
     * @throws StoreAccessException On store access problem.
     *
     * @see #deliver(int, long)
     * @see #pull(StoreValuesQuery, long)
     * @see #getSubscribedValues()
     * @see #supportsSubscribe()
     */
    @CheckReturnValue
    boolean subscribe(
            @Nonnull Collection<UUID> points)
        throws StoreAccessException;

    /**
     * Subscribes to point values store update events.
     *
     * <p>This calls collects the last value for each point at the time of the
     * subscription. These values will be available with a call to
     * 'getSubscribedValues'.</p>
     *
     * @param points The points.
     *
     * @return False when a warning has been logged.
     *
     * @throws StoreAccessException On store access problem.
     *
     * @see #deliver(int, long)
     * @see #pull(StoreValuesQuery, long)
     * @see #getSubscribedValues()
     * @see #supportsSubscribe()
     */
    @CheckReturnValue
    boolean subscribe(@Nonnull UUID[] points)
        throws StoreAccessException;

    /**
     * Gets the value types supported by this store.
     *
     * @return The supported value types.
     *
     * @throws StoreAccessException On store access problem.
     */
    @Nonnull
    @CheckReturnValue
    EnumSet<Externalizer.ValueType> supportedValueTypes()
        throws StoreAccessException;

    /**
     * Asks if this store supports count.
     *
     * @return True if count is supported.
     *
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean supportsCount()
        throws StoreAccessException;

    /**
     * Asks if this store supports delete.
     *
     * @return True if delete is supported.
     *
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean supportsDelete()
        throws StoreAccessException;

    /**
     * Asks if this store supports deliver.
     *
     * @return True if deliver is supported.
     *
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean supportsDeliver()
        throws StoreAccessException;

    /**
     * Asks if this store supports pull queries.
     *
     * @return True if pull is supported.
     *
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean supportsPull()
        throws StoreAccessException;

    /**
     * Asks if this store supports purge.
     *
     * @return True if purge is supported.
     *
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean supportsPurge()
        throws StoreAccessException;

    /**
     * Asks if this store supports subscribe.
     *
     * @return True if subscribe is supported.
     *
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean supportsSubscribe()
        throws StoreAccessException;

    /**
     * Unsubscribes from points values events.
     *
     * @param points The points.
     *
     * @return False when a warning has been logged.
     *
     * @throws StoreAccessException On store access problem.
     */
    @CheckReturnValue
    boolean unsubscribe(@Nonnull UUID[] points)
        throws StoreAccessException;

    /** Time limit for keeping point values in the store. */
    String ARCHIVE_TIME_PARAM = "ArchiveTime";

    /** Specifies the RMI URI for the server binding. */
    String BINDING_PARAM = "Binding";

    /** Requests the dynamic binding of the points to their UUID. */
    String BIND_POINTS_PARAM = "BindPoints";

    /** Asks that Notices be confirmed by a request to this Store. */
    String CONFIRM_PARAM = "Confirm";

    /** Maximum number of confirm retries. */
    String CONFIRM_RETRIES_PARAM = "ConfirmRetries";

    /** Confirm retry delay. */
    String CONFIRM_RETRY_DELAY_PARAM = "ConfirmRetryDelay";

    /** Default store name. */
    String DEFAULT_STORE_NAME = "Store";

    /** Time limit for keeping point values in the store. */
    String LIFE_TIME_PARAM = "LifeTime";

    /** Specifies the name for the RMI registry. */
    String NAME_PARAM = "Name";

    /** Specifies if a null value should cause a removal from a store. */
    String NULL_REMOVES_PARAM = "NullRemoves";

    /** Specifies the password for authentication to the server. */
    String PASSWORD_PARAM = "Password";

    /** Specifies the name of a queue properties group. */
    String QUEUE_PARAM = "Queue";

    /** The archiver must respect the point version. */
    String RESPECT_VERSION_PARAM = "RespectVersion";

    /** Specifies the security properties for connection to the server. */
    String SECURITY_PARAM = "Security";

    /** Specifies the user for identification to the server. */
    String USER_PARAM = "User";
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
