/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.tests.pap.dnp3;

import java.io.Serializable;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.logger.Messages.Entry;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.tests.Tests;
import org.rvpf.tests.service.ServiceTests;

/**
 * DNP3 tests.
 */
public abstract class DNP3Tests
    extends ServiceTests
{
    /**
     * Constructs an instance.
     *
     * @param suppport The modbus support.
     * @param owner The owner.
     */
    protected DNP3Tests(
            @Nonnull final DNP3TestsSupport suppport,
            @Nonnull final Tests owner)
    {
        _owner = owner;
        _support = suppport;
    }

    /** {@inheritDoc}
     */
    @Override
    public void expectLogs(final Entry... entries)
    {
        _owner.expectLogs(entries);
    }

    /** {@inheritDoc}
     */
    @Override
    public void requireLogs(final Entry... entries)
    {
        _owner.requireLogs(entries);
    }

    /**
     * Tests.
     *
     * @throws Exception On failure
     */
    public final void test()
        throws Exception
    {
        _testWriteRequests();

        if (!isWriteOnly()) {
            _testReadRequests();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void waitForLogs(final Entry... entries)
        throws InterruptedException
    {
        _owner.waitForLogs(entries);
    }

    /**
     * Gets the next point value.
     *
     * @return The next point value (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Optional<PointValue> getNextPointValue();

    /**
     * Gets the origin.
     *
     * @return The origin.
     */
    @Nonnull
    @CheckReturnValue
    protected final Origin getOrigin()
    {
        return getSupport().getOutstationOrigin();
    }

    /**
     * Gets the owner.
     *
     * @return The owner.
     */
    @Nonnull
    @CheckReturnValue
    protected Tests getOwner()
    {
        return _owner;
    }

    /**
     * Gets the support.
     *
     * @return The support.
     */
    @Nonnull
    @CheckReturnValue
    protected final DNP3TestsSupport getSupport()
    {
        return _support;
    }

    /**
     * Asks if these tests are write-only.
     *
     * @return True if write-only.
     */
    @CheckReturnValue
    protected boolean isWriteOnly()
    {
        return false;
    }

    /**
     * Puts a a point value.
     *
     * @param pointValue The point value.
     *
     * @throws Exception On failure.
     */
    protected abstract void putPointValue(
            @Nonnull PointValue pointValue)
        throws Exception;

    private final PointValue _expectUpdate(
            final PointValue sentPointValue)
        throws Exception
    {
        final Point point = sentPointValue.getPoint().get();
        final Serializable value = sentPointValue.getValue();
        final PointValue receivedPointValue = getNextPointValue().get();

        Require.equal(receivedPointValue.getPointUUID(), point.getUUID().get());

        final Serializable expectedValue = (value instanceof Number)? Long
            .valueOf(((Number) value).longValue()): value;
        final Serializable receivedValue = (receivedPointValue
            .getValue() instanceof Number)? Long
                .valueOf(
                    ((Number) receivedPointValue.getValue()).longValue()): receivedPointValue
                            .getValue();

        if (expectedValue instanceof Tuple) {
            final Tuple expectedTuple = (Tuple) expectedValue;
            final Tuple receivedTuple = (Tuple) receivedValue;

            Require.success(receivedTuple.size() == expectedTuple.size());

            for (int i = 0; i < expectedTuple.size(); ++i) {
                Serializable expectedItem = expectedTuple.get(i);
                Serializable receivedItem = receivedTuple.get(i);

                if (expectedItem instanceof Number) {
                    expectedItem = Long
                        .valueOf(((Number) expectedItem).longValue());
                }

                if (receivedItem instanceof Number) {
                    receivedItem = Long
                        .valueOf(((Number) receivedItem).longValue());
                }

                Require.equal(receivedItem, expectedItem);
            }
        } else {
            Require.equal(receivedValue, expectedValue);
        }

        return receivedPointValue;
    }

    private void _receive(final PointValue pointValue)
        throws Exception
    {
        putPointValue(pointValue);

        final PointValue receivedPointValue = _support
            .getMaster()
            .read(pointValue.getPoint().get());

        Require
            .equal(
                receivedPointValue.getPointUUID(),
                pointValue.getPointUUID());
        Require.equal(receivedPointValue.getValue(), pointValue.getValue());
    }

    private void _send(final PointValue pointValue)
        throws Exception
    {
        _support.getMaster().write(pointValue);
        _expectUpdate(pointValue);
    }

    private void _testReadRequests()
        throws Exception
    {
        _receive(
            new PointValue(
                _support.getPoint(_TESTS_ANALOG_INPUT_SHORT_FLAGS_1),
                Optional.of(DateTime.now()),
                null,
                Short.valueOf((short) 1234)));

        _receive(
            new PointValue(
                _support.getPoint(_TESTS_BINARY_INPUT_FLAGS_1),
                Optional.of(DateTime.now()),
                null,
                Boolean.TRUE));

        _receive(
            new PointValue(
                _support.getPoint(_TESTS_COUNTER_FLAGS_1),
                Optional.of(DateTime.now()),
                null,
                Integer.valueOf((short) 12345)));
    }

    private void _testWriteRequests()
        throws Exception
    {
        _send(
            new PointValue(
                _support.getPoint(_TESTS_ANALOG_OUTPUT_SHORT_1),
                Optional.of(DateTime.now()),
                null,
                Short.valueOf((short) 1234)));

        _send(
            new PointValue(
                _support.getPoint(_TESTS_BINARY_OUTPUT_FLAGS_1),
                Optional.of(DateTime.now()),
                null,
                Boolean.TRUE));
    }

    private static final String _TESTS_ANALOG_INPUT_SHORT_FLAGS_1 =
        "TESTS-DNP3.AI-SHORT-FLAGS.1";
    private static final String _TESTS_ANALOG_OUTPUT_SHORT_1 =
        "TESTS-DNP3.AO-SHORT.1";
    private static final String _TESTS_BINARY_INPUT_FLAGS_1 =
        "TESTS-DNP3.BI-FLAGS.1";
    private static final String _TESTS_BINARY_OUTPUT_FLAGS_1 =
        "TESTS-DNP3.BO-FLAGS.1";
    private static final String _TESTS_COUNTER_FLAGS_1 =
        "TESTS-DNP3.COUNTER-FLAGS.1";

    private final Tests _owner;
    private final DNP3TestsSupport _support;
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
