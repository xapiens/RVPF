/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractTransform.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.processor.engine;

import java.util.Optional;

import org.rvpf.base.Point;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.Transform;

/**
 * Abstract transform.
 */
public abstract class AbstractTransform
    extends Proxied.Abstract
    implements Transform
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<? extends Transform> getInstance(final Point point)
    {
        return Optional.of(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isNullRemoves(final Point point)
    {
        return _nullRemoves && point.isNullRemoves(true);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Point point)
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        _nullRemoves = getParams().getBoolean(NULL_REMOVES_PARAM);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean usesFetchedResult()
    {
        return false;
    }

    /**
     * Adds an update to the point's store.
     *
     * @param pointValue The point value.
     * @param batch The current batch context.
     */
    protected final void addUpdate(PointValue pointValue, final Batch batch)
    {
        if ((pointValue.getValue() == null)
                && isNullRemoves(pointValue.getPoint().get())) {
            pointValue = new VersionedValue.Deleted(pointValue);
        }

        batch.addUpdate(pointValue);
    }

    private boolean _nullRemoves;
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
