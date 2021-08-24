/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteEngineTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.processor.engine.remote;

import java.io.Serializable;

import java.util.List;
import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.processor.engine.executor.PipeExecutor;
import org.rvpf.processor.engine.executor.remote.RemoteEngineProxy;
import org.rvpf.processor.engine.executor.remote.RemoteEngineSessionImpl;
import org.rvpf.processor.engine.executor.remote.RemoteExecutorActivator;
import org.rvpf.processor.engine.executor.remote.RemoteExecutorEngine;
import org.rvpf.service.ServiceActivator;
import org.rvpf.tests.processor.engine.EngineTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Remote Engine Tests.
 */
public final class RemoteEngineTests
    extends EngineTests
{
    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUp()
        throws Exception
    {
        _server = startService(RemoteExecutorActivator.class, Optional.empty());

        _engine = new RemoteExecutorEngine();
        setUpEngine(_engine, Optional.empty());
    }

    /**
     * Tears down what has been set up.
     *
     * @throws Exception On failure.
     */
    @AfterClass(alwaysRun = true)
    public void tearDown()
        throws Exception
    {
        if (_server != null) {
            stopService(_server);
            _server = null;
        }
    }

    /**
     * Tests this.
     *
     * @throws Exception On failure.
     */
    @Test
    public void test()
        throws Exception
    {
        final RemoteEngineProxy sessionProxy = _engine.getSessionProxy();
        final EngineEntity engineEntity = getMetadata()
            .getEngineEntity(Optional.of(_PIPE_ENGINE_NAME))
            .get();
        final Params engineParams = engineEntity.getParams().copy();

        engineParams
            .setValue(
                RemoteEngineSessionImpl.ENGINE_EXECUTOR_PARAM,
                PipeExecutor.class.getName());
        Require
            .success(
                sessionProxy
                    .setUp(
                            "Tests",
                                    engineParams,
                                    getMetadata(),
                                    getThisLogger()));

        final TransformEntity transformEntity = getMetadata()
            .getTransformEntity(Optional.of(_PIPE_TRANSFORM_NAME))
            .get();
        final Params transformParams = transformEntity.getParams();
        final Serializable context = sessionProxy
            .newContext(transformParams, getThisLogger());

        Require.notNull(context);

        final DateTime stamp = DateTime.now();
        final PointValue inputValue = new PointValue(
            getPoint(_NUMERIC_POINT_1_NAME),
            Optional.of(stamp),
            null,
            Double.valueOf(22.0));
        final Point resultPoint = getPoint(_NUMERIC_POINT_2_NAME);

        Require.notNull(resultPoint);

        final ResultValue resultValue = new ResultValue(
            resultPoint,
            Optional.of(stamp));
        final String[] params = resultPoint
            .getParams()
            .getStrings(Point.PARAM_PARAM);

        resultValue.addInputValue(inputValue);

        final List<PointValue> response = sessionProxy
            .execute(resultValue, params, context);

        Require.success(response.size() == 1);
        Require.equal(response.get(0).getValue(), "20.0");

        sessionProxy.disposeContext(context);
        sessionProxy.tearDown();
    }

    private static final String _NUMERIC_POINT_1_NAME = "TESTS.NUMERIC.01";
    private static final String _NUMERIC_POINT_2_NAME = "TESTS.NUMERIC.02";
    private static final String _PIPE_ENGINE_NAME = "PipeExample";
    private static final String _PIPE_TRANSFORM_NAME = "PipeExample";

    private RemoteExecutorEngine _engine;
    private ServiceActivator _server;
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
