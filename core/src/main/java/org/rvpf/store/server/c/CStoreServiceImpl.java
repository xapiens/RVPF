/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CStoreServiceImpl.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.server.c;

import javax.annotation.CheckReturnValue;

import org.rvpf.service.metadata.app.MetadataServiceApp;
import org.rvpf.store.server.StoreServiceImpl;

/**
 * C store service implementation.
 */
public final class CStoreServiceImpl
    extends StoreServiceImpl
{
    /**
     * Asks if the C store class is implemented.
     *
     * <p>Allows skipping tests if the native library is not available.</p>
     *
     * @return True if this class is implemented.
     */
    @CheckReturnValue
    public static boolean isImplemented()
    {
        return CStore.isImplemented();
    }

    /** {@inheritDoc}
     */
    @Override
    protected MetadataServiceApp newMetadataServiceApp()
    {
        return new CStoreServiceAppImpl();
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
