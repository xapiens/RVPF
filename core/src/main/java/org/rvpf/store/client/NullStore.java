/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NullStore.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.store.client;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.value.PointValue;

/**
 * Null Store.
 */
public final class NullStore
    extends AbstractSink
{
    /** {@inheritDoc}
     */
    @Override
    public void connect() {}

    /** {@inheritDoc}
     */
    @Override
    public Optional<Exception[]> getExceptions()
    {
        return Optional.of(new Exception[_updateExceptions]);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean probe()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean sendUpdates(final Collection<PointValue> updates)
    {
        _updateExceptions = updates.size();
        updates.clear();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doAddQuery(final StoreValuesQuery query) {}

    /** {@inheritDoc}
     */
    @Override
    protected String supportedValueTypeCodes()
    {
        return Externalizer.ValueType
            .setToString(EnumSet.allOf(Externalizer.ValueType.class));
    }

    private int _updateExceptions;
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
