/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StepFilterEngineTests.java 4101 2019-06-30 14:56:50Z SFB $
 */

package org.rvpf.tests.processor.engine.filter;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.processor.Engine;
import org.rvpf.processor.engine.filter.StepFilterEngine;
import org.rvpf.processor.engine.filter.StepFilteredBehavior;
import org.rvpf.tests.processor.engine.EngineTests;

import org.testng.annotations.Test;

/**
 * Step filter engine tests.
 */
public final class StepFilterEngineTests
    extends EngineTests
{
    /**
     * Tests dead band filtering.
     */
    @Test
    public void testDeadBand()
    {
        final Params params = new Params();

        params.setValue(StepFilteredBehavior.DEADBAND_RATIO_PARAM, "0.01");
        params.setValue(StepFilteredBehavior.FILTER_TIME_LIMIT_PARAM, "01:00");
        params.freeze();

        final StepFilteredBehavior behavior = _prepareBehavior(params);
        final Point inputPoint = behavior.getInputPoint();
        final DateTime stamp = DateTime.now().previousDay();

        Require
            .notNull(
                behavior
                    .filter(
                            new PointValue(
                                    inputPoint,
                                            Optional.of(stamp),
                                            null,
                                            null),
                                    Optional
                                            .of(
                                                    new PointValue(
                                                            inputPoint,
                                                                    Optional.empty(),
                                                                    null,
                                                                    null)),
                                    Optional.empty()));
        Require
            .success(
                behavior
                    .filter(
                            new PointValue(
                                    inputPoint,
                                            Optional.of(
                                                    stamp.after(
                                                            ElapsedTime.HOUR)),
                                            null,
                                            "101"),
                                    Optional
                                            .of(
                                                    new PointValue(
                                                            inputPoint,
                                                                    Optional.of(
                                                                            stamp),
                                                                    null,
                                                                    "100")),
                                    Optional.empty()).length == 0);
        Require
            .success(
                behavior
                    .filter(
                            new PointValue(
                                    inputPoint,
                                            Optional.of(
                                                    stamp.after(
                                                            ElapsedTime.HOUR)),
                                            null,
                                            "102"),
                                    Optional
                                            .of(
                                                    new PointValue(
                                                            inputPoint,
                                                                    Optional.of(
                                                                            stamp),
                                                                    null,
                                                                    "100")),
                                    Optional.empty()).length == 1);
        Require
            .success(
                behavior
                    .filter(
                            new PointValue(
                                    inputPoint,
                                            Optional.of(
                                                    stamp.after(
                                                            ElapsedTime.HOUR).after()),
                                            null,
                                            "101"),
                                    Optional
                                            .of(
                                                    new PointValue(
                                                            inputPoint,
                                                                    Optional.of(
                                                                            stamp),
                                                                    null,
                                                                    "100")),
                                    Optional.empty()).length == 1);
    }

    /**
     * Tests step filtering.
     */
    @Test
    public void testStep()
    {
        final Params params = new Params();

        params.setValue(StepFilteredBehavior.STEP_SIZE_PARAM, "1.0");
        params.setValue(StepFilteredBehavior.DEADBAND_GAP_PARAM, "0.0");
        params.setValue(StepFilteredBehavior.FILTER_TIME_LIMIT_PARAM, "01:00");
        params.freeze();

        final StepFilteredBehavior behavior = _prepareBehavior(params);
        final Point inputPoint = behavior.getInputPoint();
        final DateTime stamp = DateTime.now().previousDay();
        PointValue[] filtered;

        filtered = behavior
            .filter(
                new PointValue(inputPoint, Optional.of(stamp), null, "10.4"),
                Optional.empty(),
                Optional.empty());
        Require.equal(filtered[0].getValue(), Double.valueOf(10.0));

        filtered = behavior
            .filter(
                new PointValue(inputPoint, Optional.of(stamp), null, "10.6"),
                Optional.empty(),
                Optional.empty());
        Require.equal(filtered[0].getValue(), Double.valueOf(11.0));

        filtered = behavior
            .filter(
                new PointValue(inputPoint, Optional.of(stamp), null, null),
                Optional
                    .of(
                            new PointValue(
                                    inputPoint,
                                            Optional.empty(),
                                            null,
                                            null)),
                Optional.empty());
        Require.success(filtered.length == 1);

        filtered = behavior
            .filter(
                new PointValue(
                    inputPoint,
                    Optional.of(stamp.after(ElapsedTime.HOUR)),
                    null,
                    "10.4"),
                Optional
                    .of(
                            new PointValue(
                                    inputPoint,
                                            Optional.of(stamp),
                                            null,
                                            "10.0")),
                Optional.empty());
        Require.success(filtered.length == 0);

        filtered = behavior
            .filter(
                new PointValue(
                    inputPoint,
                    Optional.of(stamp.after(ElapsedTime.HOUR)),
                    null,
                    "10.6"),
                Optional
                    .of(
                            new PointValue(
                                    inputPoint,
                                            Optional.of(stamp),
                                            null,
                                            "10.0")),
                Optional.empty());
        Require.success(filtered.length == 1);

        filtered = behavior
            .filter(
                new PointValue(
                    inputPoint,
                    Optional.of(stamp.after(ElapsedTime.HOUR).after()),
                    null,
                    "10.4"),
                Optional
                    .of(
                            new PointValue(
                                    inputPoint,
                                            Optional.of(stamp),
                                            null,
                                            "10.0")),
                Optional.empty());
        Require.success(filtered.length == 1);

        filtered = behavior
            .filter(
                new PointValue(
                    inputPoint,
                    Optional.of(stamp.after(ElapsedTime.HOUR)),
                    null,
                    "10.6"),
                Optional
                    .of(
                            new PointValue(
                                    inputPoint,
                                            Optional.of(stamp),
                                            null,
                                            "11.0")),
                Optional.empty());
        Require.success(filtered.length == 0);
    }

    private StepFilteredBehavior _prepareBehavior(final Params params)
    {
        final Engine engine = new StepFilterEngine();
        final ResultValue resultValue = newResultValue(
            getContent(NUMERIC_CONTENT));
        final PointEntity resultPoint = (PointEntity) resultValue
            .getPoint()
            .get();
        final Params emptyParams = new Params();

        emptyParams.freeze();
        setUpEngine(engine, Optional.of(emptyParams));
        addInputRelation(resultPoint, resultValue, params);
        setUpPoint(resultPoint, newTransform(engine, emptyParams));

        final PointInput firstInput = resultPoint.getInputs().get(0);

        return (StepFilteredBehavior) firstInput.getPrimaryBehavior().get();
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
