/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CloneEngine.java 4040 2019-05-31 18:55:08Z SFB $
 */

package org.rvpf.processor.engine;

import java.util.Optional;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.behavior.Synchronized;

/**
 * Clone engine.
 */
public final class CloneEngine
    extends AbstractEngine
{
    /** {@inheritDoc}
     */
    @Override
    public Transform createTransform(final TransformEntity proxyEntity)
    {
        final Transform transform = new CloneTransform();

        if (!transform.setUp(getMetadata(), proxyEntity)) {
            return null;
        }

        return transform;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<ClassDef> defaultBehavior()
    {
        return _DEFAULT_BEHAVIOR;
    }

    private static final Optional<ClassDef> _DEFAULT_BEHAVIOR = Optional
        .of(new ClassDefImpl(Synchronized.class));

    private static final class CloneTransform
        extends AbstractTransform
    {
        CloneTransform() {}

        /** {@inheritDoc}
         */
        @Override
        public Optional<PointValue> applyTo(
                final ResultValue resultValue,
                final Batch batch)
        {
            final PointValue inputValue = resultValue.getInputValues().get(0);
            final Optional<DateTime> stamp = resultValue
                .hasStamp()? Optional
                    .of(resultValue.getStamp()): Optional.empty();

            return Optional.of(inputValue.morph(resultValue.getPoint(), stamp));
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<? extends Transform> getInstance(final Point point)
        {
            if (point.getInputs().size() < 1) {
                getThisLogger()
                    .error(
                        ProcessorMessages.TRANSFORM_GE_1_INPUT,
                        getName(),
                        point);

                return Optional.empty();
            }

            return Optional.of(this);
        }
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
