/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractSink.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.client;

import java.util.Optional;

import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;

/**
 * Abstract sink.
 */
public abstract class AbstractSink
    extends AbstractStore
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<StoreValues> nextValues()
    {
        return Optional.empty();
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues select(
            final StoreValuesQuery query)
        throws InterruptedException, StoreAccessException
    {
        return new StoreValues(query);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
    {
        return true;
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
