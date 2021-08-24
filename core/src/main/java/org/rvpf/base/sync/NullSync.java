/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.base.sync;

import java.util.Optional;

import org.rvpf.base.DateTime;

/**
 * Null sync.
 *
 * <p>Instances of this class may be used as a place holder for other syncs.</p>
 */
public final class NullSync
    extends Sync.Abstract
{
    /**
     * Constructs an instance.
     */
    public NullSync() {}

    private NullSync(final NullSync other)
    {
        super(other);
    }

    /** {@inheritDoc}
     */
    @Override
    public Sync copy()
    {
        return new NullSync(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getNextStamp()
    {
        return Optional.of(DateTime.END_OF_TIME);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<DateTime> getPreviousStamp()
    {
        return Optional.of(DateTime.BEGINNING_OF_TIME);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isInSync()
    {
        return false;
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
