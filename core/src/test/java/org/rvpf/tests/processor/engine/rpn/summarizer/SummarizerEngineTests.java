/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SummarizerEngineTests.java 4101 2019-06-30 14:56:50Z SFB $
 */

package org.rvpf.tests.processor.engine.rpn.summarizer;

import java.io.Serializable;

import java.util.Optional;
import java.util.Random;

import org.rvpf.base.DateTime;
import org.rvpf.base.Params;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.engine.rpn.operation.BooleanOperations;
import org.rvpf.processor.engine.rpn.operation.CompoundOperations;
import org.rvpf.processor.engine.rpn.operation.DoubleOperations;
import org.rvpf.processor.engine.rpn.operation.LongOperations;
import org.rvpf.processor.engine.rpn.operation.StackOperations;
import org.rvpf.processor.engine.rpn.selector.summarizer.SummarizerEngine;
import org.rvpf.processor.engine.rpn.selector.summarizer.SummarizerTransform;
import org.rvpf.tests.processor.engine.EngineTests;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Summarizer Engine Tests.
 */
public final class SummarizerEngineTests
    extends EngineTests
{
    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeMethod
    public void setUp()
        throws Exception
    {
        _engine = new SummarizerEngine();
        setUpEngine(_engine, Optional.empty());

        _engine.register(new StackOperations());
        _engine.register(new DoubleOperations());
        _engine.register(new LongOperations());
        _engine.register(new BooleanOperations());
        _engine.register(new CompoundOperations());

        _batch = newBatch();
    }

    /**
     * Tests the RSD computation.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testRSD()
        throws Exception
    {
        final Double[] inputs = new Double[100];
        final Float[] results = new Float[inputs.length];
        Double result;

        for (int i = 0; i < inputs.length; ++i) {
            inputs[i] = Double.valueOf(_RANDOM.nextDouble() - 0.5);
        }

        _resultValue = newResultValue(getContent(NUMERIC_CONTENT));
        _setPrograms(
            _RSD_INITIAL_PROGRAM,
            _RSD_STEP_PROGRAM,
            _RSD_FINAL_PROGRAM);
        addInputValue(getContent(CLOCK_CONTENT), DateTime.now(), _resultValue);
        _setStepInputContent(getContent(NUMERIC_CONTENT));
        Require.equal(null, _apply());

        for (int i = 0; i < inputs.length; ++i) {
            _resultValue
                .addInputValue(
                    new PointValue(
                        _stepInputPoint,
                        Optional.of(_resultValue.getStamp()),
                        null,
                        inputs[i]));
            result = (Double) _apply();

            if (i == 0) {
                Require.equal(null, result);
            } else {
                Require.notNull(result);
            }

            results[i] = (result == null)? null: Float
                .valueOf(result.floatValue());
        }

        _resultValue = newResultValue(getContent(NUMERIC_CONTENT));
        _setPrograms(
            _RSD_INITIAL_PROGRAM,
            _RSD_STEP_PROGRAM,
            _RSD_FINAL_PROGRAM);
        addInputValue(getContent(CLOCK_CONTENT), DateTime.now(), _resultValue);
        _setStepInputContent(getContent(NUMERIC_CONTENT));
        Require.equal(null, _apply());

        for (int i = 0; i < inputs.length; ++i) {
            _resultValue
                .addInputValue(
                    new PointValue(
                        _stepInputPoint,
                        Optional.of(_resultValue.getStamp()),
                        null,
                        inputs[i]));
            result = (Double) _apply();
            Require
                .equal(
                    (result == null)? null: Float.valueOf(result.floatValue()),
                    results[i]);
        }
    }

    private Serializable _apply()
        throws Exception
    {
        final PointEntity point = (PointEntity) _resultValue.getPoint().get();

        _resultValue.setValue(null);

        final Optional<PointValue> pointValue = point
            .getTransform()
            .get()
            .applyTo(_resultValue, _batch);

        return pointValue.isPresent()? pointValue.get().getValue(): null;
    }

    private void _setPrograms(
            final String initialProgram,
            final String stepProgram,
            final String finalProgram)
    {
        final Params params = new Params();
        final PointEntity resultPoint = (PointEntity) _resultValue
            .getPoint()
            .get();
        final Transform transform;

        if (initialProgram != null) {
            params
                .setValue(
                    SummarizerTransform.INITIAL_PROGRAM_PARAM,
                    initialProgram);
        }

        if (stepProgram != null) {
            params
                .setValue(SummarizerTransform.STEP_PROGRAM_PARAM, stepProgram);
        }

        if (finalProgram != null) {
            params
                .setValue(
                    SummarizerTransform.FINAL_PROGRAM_PARAM,
                    finalProgram);
        }

        params.freeze();
        transform = newTransform(_engine, params);

        resultPoint
            .setTransformEntity((TransformEntity) transform.getProxyEntity());
    }

    private void _setStepInputContent(final ContentEntity content)
    {
        final Params params = new Params();

        params.freeze();
        _stepInputPoint = new PointEntity.Definition();
        _stepInputPoint.setUUID(Optional.of(UUID.generate()));
        _stepInputPoint.setName(Optional.of("TestStepInput"));
        _stepInputPoint.setContentEntity(content);
        addInputRelation(_stepInputPoint, _resultValue, params);
    }

    private static final Random _RANDOM = new Random(0);
    private static final String _RSD_FINAL_PROGRAM =
        "#1 0? if 0.0 else { #2 $# * #1 : * - abs $# : -- * / sqrt"
        + " #1 $# / abs / }";
    private static final String _RSD_INITIAL_PROGRAM =
        "1 $# gt assert 0.0 :#1= #2=";
    private static final String _RSD_STEP_PROGRAM = "$ #1 + #1= $ : * #2 + #2=";

    private Batch _batch;
    private SummarizerEngine _engine;
    private ResultValue _resultValue;
    private PointEntity _stepInputPoint;
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
