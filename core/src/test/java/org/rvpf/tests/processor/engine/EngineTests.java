/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EngineTests.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.tests.processor.engine;

import java.io.Serializable;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Content;
import org.rvpf.base.DateTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.config.Config;
import org.rvpf.content.BigDecimalContent;
import org.rvpf.content.SIContent;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.document.loader.MetadataDocumentLoader;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.EngineEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.Engine;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.BatchControl;
import org.rvpf.processor.CacheManager;
import org.rvpf.processor.ProcessorServiceActivator;
import org.rvpf.tests.TestsMetadataFilter;
import org.rvpf.tests.service.MetadataServiceTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Engine tests.
 */
public abstract class EngineTests
    extends MetadataServiceTests
{
    /**
     * Sets up the metadata.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public final void setUpMetadata()
        throws Exception
    {
        final String serviceName = new ProcessorServiceActivator()
            .makeObjectName(Optional.empty())
            .toString();
        final Config config = ConfigDocumentLoader
            .loadConfig(serviceName, Optional.empty(), Optional.empty());

        config.registerClassLoader();

        _metadata = MetadataDocumentLoader
            .fetchMetadata(
                new TestsMetadataFilter(true),
                Optional.of(config),
                config.getServiceUUID(),
                Optional.empty());
        Require.success(_metadata.adjustPointsLevel());

        for (final Point point: _metadata.getPointsCollection()) {
            Require.success(((PointEntity) point).setUp(_metadata));
        }
    }

    /**
     * Tears down the metadata.
     */
    @AfterClass(alwaysRun = true)
    public final void tearDownMetadata()
    {
        _metadata.tearDownPoints();

        _batchControl = null;
        _metadata = null;
    }

    /**
     * Adds an input relation to a result's point.
     *
     * @param point The input point.
     * @param resultValue The result value.
     * @param params The relation params.
     */
    protected static void addInputRelation(
            final PointEntity point,
            final ResultValue resultValue,
            @Nonnull final Params params)
    {
        final PointEntity resultPoint = (PointEntity) resultValue
            .getPoint()
            .get();
        final PointInput relation = new PointInput(point, resultPoint);

        relation.setParams(Optional.of(params));
        resultPoint.addInputRelation(relation);
    }

    /**
     * Adds an input to a result value.
     *
     * @param content The input point's content entity.
     * @param value The input value.
     * @param resultValue The result value.
     */
    protected static void addInputValue(
            final ContentEntity content,
            final Serializable value,
            final ResultValue resultValue)
    {
        final PointEntity point = new PointEntity.Definition();
        final Params emptyParams = new Params();

        emptyParams.freeze();
        point.setUUID(Optional.of(UUID.generate()));
        point
            .setName(
                Optional
                    .of(TEST_INPUT_POINT_NAME
                    + (resultValue.getInputValues().size() + 1)));
        point.setContentEntity(Require.notNull(content));
        addInputRelation(point, resultValue, emptyParams);

        resultValue
            .addInputValue(
                new PointValue(
                    point,
                    Optional.of(resultValue.getStamp()),
                    null,
                    value));
    }

    /**
     * Returns a new transform.
     *
     * @param engine The engine.
     * @param params Some params (may be empty).
     *
     * @return The new transform.
     */
    protected static Transform newTransform(
            @Nonnull final Engine engine,
            @Nonnull final Params params)
    {
        final TransformEntity.Builder transformBuilder = TransformEntity
            .newBuilder();
        final Transform transform;

        transformBuilder
            .setEngineEntity(
                Optional.of((EngineEntity) engine.getProxyEntity()))
            .setParams(Optional.of(params));

        transform = transformBuilder.build().getTransform();
        Require.notNull(transform);

        return transform;
    }

    /**
     * Gets a BigDecimal content entity.
     *
     * @param scale The scale.
     *
     * @return The content entity.
     */
    protected final ContentEntity getBigDecimalContent(final int scale)
    {
        final ContentEntity contentEntity = getContent(BIG_DECIMAL_CONTENT);
        final PointEntity point = new PointEntity.Definition();
        final Params params = new Params();

        Require.notNull(contentEntity, "BigDecimal content definition");
        params.add(BigDecimalContent.SCALE_PARAM, Integer.valueOf(scale));
        params.freeze();
        point.setParams(Optional.of(params));

        final Content content = contentEntity.getContent();
        final Proxied contentInstance = (Proxied) content.getInstance(point);

        Require
            .notNull(
                contentInstance,
                "BigDecimal content instance for '" + scale + "'");

        return (ContentEntity) contentEntity.getProxy(contentInstance);
    }

    /**
     * Gets a content entity.
     *
     * @param contentName The content's name.
     *
     * @return The content entity.
     */
    protected final ContentEntity getContent(final String contentName)
    {
        final ContentEntity content = getMetadata()
            .getContentEntity(Optional.of(contentName))
            .get();

        Require.success(content.setUp(getMetadata()));

        return content;
    }

    /**
     * Gets the metadata.
     *
     * @return The metadata.
     */
    @Override
    protected final Metadata getMetadata()
    {
        return _metadata;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Point getPoint(final String key)
    {
        return _metadata.getPoint(key).get();
    }

    /**
     * Gets a SI content entity.
     *
     * @param unit The SI unit.
     *
     * @return The content entity.
     */
    protected final ContentEntity getSIContent(final String unit)
    {
        final ContentEntity contentEntity = getContent(SI_CONTENT);
        final PointEntity point = new PointEntity.Definition();
        final Params params = new Params();

        Require.notNull(contentEntity, "SI content definition");
        params.add(SIContent.UNIT_PARAM, unit);
        params.freeze();
        point.setParams(Optional.of(params));

        final Content content = contentEntity.getContent();
        final Proxied contentInstance = (Proxied) content.getInstance(point);

        Require
            .notNull(contentInstance, "SI content instance for '" + unit + "'");

        return (ContentEntity) contentEntity.getProxy(contentInstance);
    }

    /**
     * Returns a new batch.
     *
     * @return The new batch.
     */
    protected final Batch newBatch()
    {
        if (_batchControl == null) {
            _batchControl = new BatchControl();

            Require
                .success(
                    _batchControl
                        .setUp(
                                _metadata,
                                        Optional.empty(),
                                        new CacheManager(),
                                        _metadata.getProperties()));
        }

        return _batchControl.newBatch();
    }

    /**
     * Returns a new result value.
     *
     * @param content The result point's content entity.
     *
     * @return The new result value.
     */
    protected final ResultValue newResultValue(final ContentEntity content)
    {
        final PointEntity point = new PointEntity.Definition();

        point.setUUID(Optional.of(UUID.generate()));
        point.setName(Optional.of(TEST_RESULT_POINT_NAME));
        point.setContentEntity(content);
        point
            .setStoreEntity(
                getMetadata().getStoreEntity(Optional.of(NULL_STORE)));

        return new ResultValue(point, Optional.of(DateTime.now()));
    }

    /**
     * Sets up an engine.
     *
     * @param engine The engine.
     * @param params Some params (may be empty).
     */
    protected final void setUpEngine(
            @Nonnull final Engine engine,
            @Nonnull final Optional<Params> params)
    {
        final EngineEntity.Builder engineBuilder = EngineEntity.newBuilder();

        engineBuilder.setInstance(engine).setParams(params);

        Require.success(engine.setUp(_metadata, engineBuilder.build()));
    }

    /**
     * Sets up a point entity.
     *
     * @param pointEntity The point entity.
     * @param transform The transform.
     */
    protected final void setUpPoint(
            @Nonnull final PointEntity pointEntity,
            @Nonnull final Transform transform)
    {
        pointEntity
            .setTransformEntity((TransformEntity) transform.getProxyEntity());
        Require.success(pointEntity.setUp(getMetadata()));
        Require.success(pointEntity.setUpRelations(getMetadata()));
    }

    /** Big decimal content. */
    public static final String BIG_DECIMAL_CONTENT = "BigDecimal";

    /** Big integer content. */
    public static final String BIG_INTEGER_CONTENT = "BigInteger";

    /** Big rational content. */
    public static final String BIG_RATIONAL_CONTENT = "BigRational";

    /** Celcius content. */
    public static final String CELCIUS_CONTENT = "CELCIUS";

    /** Clock content. */
    public static final String CLOCK_CONTENT = "Clock";

    /** Count content. */
    public static final String COMPLEX_CONTENT = "Complex";

    /** Count content. */
    public static final String COUNT_CONTENT = "Count";

    /** CountDict content. */
    public static final String COUNT_DICT_CONTENT = "CountDict";

    /** CountTuple content. */
    public static final String COUNT_TUPLE_CONTENT = "CountTuple";

    /** Dollars and cents content. */
    public static final String FAHRENHEIT_CONTENT = "FAHRENHEIT";

    /** Logical content. */
    public static final String LOGICAL_CONTENT = "Logical";

    /** Mask content. */
    public static final String MASK_CONTENT = "Mask";

    /** Null store. */
    public static final String NULL_STORE = "NullStore";

    /** Numeric content. */
    public static final String NUMERIC_CONTENT = "Numeric";

    /** Rational content. */
    public static final String RATIONAL_CONTENT = "Rational";

    /** SI content. */
    public static final String SI_CONTENT = "SI";

    /** Text content. */
    public static final String TEXT_CONTENT = "Text";

    /** Unspecified content. */
    public static final String UNSPECIFIED_CONTENT = "Unspecified";

    /** Test input point name. */
    protected static final String TEST_INPUT_POINT_NAME = "TestInput";

    /** Test result point name. */
    protected static final String TEST_RESULT_POINT_NAME = "TestResult";

    private BatchControl _batchControl;
    private Metadata _metadata;
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
