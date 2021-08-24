/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreAccessException.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.store;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.rmi.SessionException;

/**
 * Access exception.
 */
public final class StoreAccessException
    extends ServiceNotAvailableException
{
    /**
     * Creates an instance.
     *
     * @param storeUUID The optional store UUID.
     * @param cause The cause of the exception.
     */
    public StoreAccessException(
            @Nonnull final Optional<UUID> storeUUID,
            @Nonnull final SessionException cause)
    {
        super(cause);

        _storeUUID = storeUUID.orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized SessionException getCause()
    {
        return (SessionException) super.getCause();
    }

    /**
     * Gets the store UUID.
     *
     * @return The optional store UUID.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<UUID> getStoreUUID()
    {
        return Optional.ofNullable(_storeUUID);
    }

    private static final long serialVersionUID = 1L;

    private final UUID _storeUUID;
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
