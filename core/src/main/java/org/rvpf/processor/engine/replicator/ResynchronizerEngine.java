/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ResynchronizerEngine.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.replicator;

import java.util.List;
import java.util.Optional;

import org.rvpf.base.PointRelation;
import org.rvpf.base.tool.Require;
import org.rvpf.metadata.entity.BehaviorEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Behavior;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.behavior.Synchronized;

/**
 * Resync Engine.
 */
public final class ResynchronizerEngine
    extends ReplicatorEngine
{
    /** {@inheritDoc}
     */
    @Override
    public Transform createTransform(final TransformEntity proxyEntity)
    {
        final Resynchronizer resync = new Resynchronizer();

        if (!resync.setUp(getMetadata(), proxyEntity)) {
            return null;
        }

        return resync;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<BehaviorEntity> getDefaultBehavior(
            final PointRelation relation)
    {
        final List<? extends PointRelation> relations = relation
            .getResultPoint()
            .getInputs();
        final int position = relations.indexOf(relation);
        final Class<? extends Behavior> behaviorClass;

        Require.success(position >= 0);

        if (position == 0) {
            behaviorClass = ResynchronizedBehavior.class;
        } else {
            behaviorClass = Synchronized.class;
        }

        return getDefaultBehavior(behaviorClass);
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
