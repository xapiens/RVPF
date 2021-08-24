/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProxyStoreServiceImpl.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.store.server.proxy;

import org.rvpf.service.metadata.app.MetadataServiceApp;
import org.rvpf.store.server.StoreServiceImpl;

/** Proxy store service implementation.
 */
public final class ProxyStoreServiceImpl
    extends StoreServiceImpl
{
    /** {@inheritDoc}
     */
    @Override
    protected MetadataServiceApp newMetadataServiceApp()
    {
        return new ProxyStoreServiceAppImpl();
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
