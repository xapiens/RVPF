/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ReplicatorEngine.java 3956 2019-05-06 11:17:05Z SFB $
 */

package org.rvpf.processor.engine.replicator;

import java.util.Optional;

import org.rvpf.base.PointRelation;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.engine.AbstractEngine;

/**
 * Replicator Engine.
 */
public class ReplicatorEngine
    extends AbstractEngine
{
    /** {@inheritDoc}
     */
    @Override
    public Transform createTransform(final TransformEntity proxyEntity)
    {
        final ReplicatorTransform transform = new ReplicatorTransform();

        if (!transform.setUp(getMetadata(), proxyEntity)) {
            return null;
        }

        return transform;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<BehaviorEntity> getDefaultBehavior(
            final PointRelation relation)
    {
        return getDefaultBehavior(ReplicatedBehavior.class);
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
