/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreSession.java 4079 2019-06-12 13:40:21Z SFB $
 */

package org.rvpf.base.store;

import java.rmi.RemoteException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.State;

/**
 * Store server session interface.
 *
 * <p>Specifies the remote interaction with the store server.</p>
 */
public interface StoreSession
    extends Session
{
    /**
     * Delivers point value events.
     *
     * @param limit A limit for the number of values.
     * @param timeout A time limit in millis to wait for the first message
     *                (negative for infinite).
     *
     * @return The point values (empty on timeout, null on service closed).
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     *
     * @see Store#deliver(int, long)
     */
    @Nullable
    @CheckReturnValue
    StoreValues deliver(
            final int limit,
            final long timeout)
        throws RemoteException, SessionException;

    /**
     * Gets point binding informations on selected points.
     *
     * @param bindingRequests Selection specifications.
     *
     * @return The point bindings informations (null on service closed).
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @Nullable
    @CheckReturnValue
    PointBinding[] getPointBindings(
            @Nonnull PointBinding.Request[] bindingRequests)
        throws RemoteException, SessionException;

    /**
     * Gets the state groups.
     *
     * @return The state groups (null on service closed).
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @Nullable
    @CheckReturnValue
    State.Group[] getStateGroups()
        throws RemoteException, SessionException;

    /**
     * Impersonates an other user.
     *
     * @param user The other user (empty for anonymous, null to cancel).
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException From session security check.
     */
    void impersonate(
            @Nullable String user)
        throws RemoteException, SessionException;

    /**
     * Interrupts the current delivery.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException From session security check.
     */
    void interrupt()
        throws RemoteException, SessionException;

    /**
     * Probes the state of a session.
     *
     * @return True when everything is fine.
     *
     * @throws SessionException On session problems.
     * @throws RemoteException From RMI RunTime.
     */
    @CheckReturnValue
    boolean probe()
        throws RemoteException, SessionException;

    /**
     * Pulls points values.
     *
     * @param query The store query.
     * @param timeout A time limit in millis to wait for the first value
     *                (negative for infinite).
     *
     * @return The point values (empty on timeout, null on service closed).
     *
     * @throws ServiceNotAvailableException When the service is not available.
     * @throws RemoteException From RMI RunTime.
     *
     * @see Store#pull(StoreValuesQuery, long)
     */
    @Nullable
    @CheckReturnValue
    StoreValues pull(
            @Nonnull StoreValuesQuery query,
            long timeout)
        throws RemoteException, ServiceNotAvailableException;

    /**
     * Purges point values.
     *
     * @param pointUUIDs The UUID of the points to purge.
     * @param timeInterval A time interval.
     *
     * @return The number of point values purged.
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     *
     * @see Store#purge(UUID[], TimeInterval)
     */
    @CheckReturnValue
    int purge(
            @Nonnull UUID[] pointUUIDs,
            @Nonnull TimeInterval timeInterval)
        throws RemoteException, SessionException;

    /**
     * Resolves a state for a point.
     *
     * @param state The state.
     * @param pointUUID The optional point UUID.
     *
     * @return The resolved state.
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @Nullable
    @CheckReturnValue
    State resolve(
            @Nonnull State state,
            @Nullable UUID pointUUID)
        throws RemoteException, SessionException;

    /**
     * Selects point values.
     *
     * @param queries An array of store queries.
     *
     * @return An array of store values (null on service closed).
     *
     * @throws SessionException On session exception.
     * @throws RemoteException From RMI RunTime.
     */
    @Nullable
    @CheckReturnValue
    StoreValues[] select(
            @Nonnull StoreValuesQuery[] queries)
        throws RemoteException, SessionException;

    /**
     * Subscribes to point values events.
     *
     * @param pointUUIDs The point UUIDs.
     *
     * @return The last store value for each point (null on service closed).
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @Nullable
    @CheckReturnValue
    StoreValues[] subscribe(
            @Nonnull UUID[] pointUUIDs)
        throws RemoteException, SessionException;

    /**
     * Returns a string of supported value type codes.
     *
     * @return The string of supported value type codes.
     *
     * @throws ServiceNotAvailableException When the service is not available.
     * @throws RemoteException From RMI RunTime.
     */
    @Nonnull
    @CheckReturnValue
    String supportedValueTypeCodes()
        throws RemoteException, ServiceNotAvailableException;

    /**
     * Asks if the server supports count.
     *
     * @return True if count is supported.
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @CheckReturnValue
    boolean supportsCount()
        throws RemoteException, SessionException;

    /**
     * Asks if the server supports delete.
     *
     * @return True if delete is supported.
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @CheckReturnValue
    boolean supportsDelete()
        throws RemoteException, SessionException;

    /**
     * Asks if the server supports deliver.
     *
     * @return True if deliver is supported.
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @CheckReturnValue
    boolean supportsDeliver()
        throws RemoteException, SessionException;

    /**
     * Asks if the server supports pull.
     *
     * @return True if pull is supported.
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @CheckReturnValue
    boolean supportsPull()
        throws RemoteException, SessionException;

    /**
     * Asks if the server supports purge.
     *
     * @return True if purge is supported.
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @CheckReturnValue
    boolean supportsPurge()
        throws RemoteException, SessionException;

    /**
     * Asks if the server supports subscribe.
     *
     * @return True if subscribe is supported.
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @CheckReturnValue
    boolean supportsSubscribe()
        throws RemoteException, SessionException;

    /**
     * Unsubscribes from point values events.
     *
     * @param pointUUIDs The point UUIDs.
     *
     * @return An exception array (null on service closed, each null on
     *         success).
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    @Nullable
    @CheckReturnValue
    Exception[] unsubscribe(
            @Nonnull UUID[] pointUUIDs)
        throws RemoteException, SessionException;

    /**
     * Updates point values.
     *
     * @param pointValues The point values.
     *
     * @return An exception array (null on service closed, each null on
     *         success).
     *
     * @throws ServiceNotAvailableException When the service is not available.
     * @throws RemoteException From RMI RunTime.
     */
    @Nullable
    @CheckReturnValue
    Exception[] update(
            @Nonnull PointValue[] pointValues)
        throws RemoteException, ServiceNotAvailableException;
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
