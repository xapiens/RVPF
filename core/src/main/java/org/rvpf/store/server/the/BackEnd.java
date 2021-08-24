/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BackEnd.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.store.server.archiver.Archiver;
import org.rvpf.store.server.archiver.ScheduledArchiver;

/**
 * Back-end.
 */
public interface BackEnd
{
    /**
     * Begins updates.
     */
    void beginUpdates();

    /**
     * Closes.
     */
    void close();

    /**
     * Commits.
     */
    void commit();

    /**
     * Creates store values for a store query.
     *
     * @param query The store query.
     * @param identity The optional requesting identity.
     *
     * @return The store values.
     */
    @Nonnull
    @CheckReturnValue
    StoreValues createResponse(
            @Nonnull StoreValuesQuery query,
            @Nonnull Optional<Identity> identity);

    /**
     * Deletes a point value.
     *
     * @param versionedValue The point value to delete.
     *
     * @return The number of deleted values (0 .. 2).
     */
    @CheckReturnValue
    int delete(@Nonnull VersionedValue versionedValue);

    /**
     * Ends updates.
     */
    void endUpdates();

    /**
     * Returns a new archiver.
     *
     * @return The new archiver.
     */
    @Nonnull
    @CheckReturnValue
    default Archiver newArchiver()
    {
        return new ScheduledArchiver();
    }

    /**
     * Opens.
     *
     * @throws ServiceNotAvailableException On failure.
     */
    void open()
        throws ServiceNotAvailableException;

    /**
     * Purges points values.
     *
     * @param pointUUIDs The UUID of the points to purge.
     * @param timeInterval A time interval.
     *
     * @return The number of values purged.
     */
    @CheckReturnValue
    default int purge(
            @Nonnull final UUID[] pointUUIDs,
            @Nonnull final TimeInterval timeInterval)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Rolls back.
     */
    void rollback();

    /**
     * Sets up the back end.
     *
     * @param theStoreAppImpl The TheStore application instance.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull TheStoreServiceAppImpl theStoreAppImpl);

    /**
     * Asks if the back end supports count.
     *
     * @return True if count is supported.
     */
    @CheckReturnValue
    boolean supportsCount();

    /**
     * Asks if the back end supports purge.
     *
     * @return True if purge is supported.
     */
    @CheckReturnValue
    default boolean supportsPurge()
    {
        return false;
    }

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Updates a point value.
     *
     * @param versionedValue The point value to update.
     */
    void update(@Nonnull VersionedValue versionedValue);
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
