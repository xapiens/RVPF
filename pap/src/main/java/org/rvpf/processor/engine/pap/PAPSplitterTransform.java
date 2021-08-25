/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPSplitterTransform.java 4087 2019-06-16 18:12:18Z SFB $
 */

package org.rvpf.processor.engine.pap;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.base.value.filter.ValueFilter;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.AbstractTransform;

/**
 * PAP splitter transform.
 */
public final class PAPSplitterTransform
    extends AbstractTransform
{
    /**
     * Constructs an instance.
     *
     * @param splitter The splitter.
     */
    public PAPSplitterTransform(@Nonnull final PAPSplitter splitter)
    {
        _splitter = splitter;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<PointValue> applyTo(
            final ResultValue resultValue,
            final Batch batch)
        throws InterruptedException, ServiceNotAvailableException
    {
        final PointValue inputValue = resultValue.getInputValues().get(0);
        final Optional<PAPSplitter.Splitted> splitted = _splitter
            .split(inputValue);

        if (!splitted.isPresent()) {
            return Optional.empty();
        }

        if (inputValue.getValue() instanceof Tuple) {
            final PointValue thawed = inputValue.thawed();

            thawed.setValue(splitted.get());

            if (thawed != inputValue) {
                thawed.freeze();
            }
        }

        final Point resultPoint = resultValue.getPoint().get();

        resultValue.setValue(splitted.get().get(resultPoint).orElse(null));

        final ValueFilter filter = _filters.get(resultValue.getPoint().get());

        if (filter != null) {
            for (final PointValue filteredValue:
                    filter.filter(Optional.of(resultValue))) {
                resultValue.setValue(filteredValue.getValue());
            }
        }

        return Optional.of(resultValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Point point)
    {
        final List<? extends PointRelation> pointInputs = point.getInputs();

        if (pointInputs.size() != 1) {
            getThisLogger().error(ProcessorMessages.POINT_EQ_1_INPUT, point);

            return false;
        }

        final ValueFilter filter = point.filter();

        if (!filter.isDisabled()) {
            _filters.put(point, point.filter());
        }

        final Point splittedPoint = pointInputs.get(0).getInputPoint();

        return _splitter.setUp(splittedPoint);
    }

    private final Map<Point, ValueFilter> _filters = new IdentityHashMap<>();
    private final PAPSplitter _splitter;
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
