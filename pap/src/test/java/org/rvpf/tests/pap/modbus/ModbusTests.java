/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap.modbus;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ConnectFailedException;
import org.rvpf.base.logger.Messages.Entry;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.pap.modbus.ModbusClient;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.message.ReadTransaction;
import org.rvpf.pap.modbus.message.Transaction;
import org.rvpf.pap.modbus.message.Transaction.Response;
import org.rvpf.pap.modbus.register.Register;
import org.rvpf.tests.Tests;
import org.rvpf.tests.service.ServiceTests;

/**
 * Modbus tests.
 */
public abstract class ModbusTests
    extends ServiceTests
{
    /**
     * Constructs an instance.
     *
     * @param suppport The modbus support.
     * @param owner The owner.
     */
    protected ModbusTests(
            @Nonnull final ModbusTestsSupport suppport,
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
    protected final ModbusTestsSupport getSupport()
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
     * Puts a value for a point.
     *
     * @param point The point.
     * @param stamp A time stamp.
     * @param value The value.
     *
     * @throws Exception On failure.
     */
    protected void putValue(
            @Nonnull final Point point,
            @Nonnull final DateTime stamp,
            @Nonnull final Serializable value)
        throws Exception {}

    private static short[] _sendReadRequest(
            final Optional<Transaction.Request> request)
        throws Exception
    {
        final ReadTransaction.Response response =
            (ReadTransaction.Response) _sendRequest(
                request);

        return response.getValues();
    }

    private static Transaction.Response _sendRequest(
            final Optional<Transaction.Request> request)
        throws Exception
    {
        final Transaction.Response response = (Response) request
            .get()
            .getResponse()
            .get();

        Require.success(response.isSuccess());

        return response;
    }

    private final PointValue _expectUpdate(
            final Point point,
            final Serializable value)
        throws Exception
    {
        final PointValue pointValue = _nextPointValue();

        Require.equal(pointValue.getPointUUID(), point.getUUID().get());

        final Serializable expectedValue = (value instanceof Number)? Long
            .valueOf(((Number) value).longValue()): value;
        final Serializable receivedValue = (pointValue
            .getValue() instanceof Number)? Long
                .valueOf(
                    ((Number) pointValue.getValue()).longValue()): pointValue
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

        if (_support.isServerLocal()) {
            putValue(point, _stamp, value);
        }

        return pointValue;
    }

    private final Origin _getOrigin()
    {
        return getSupport().getServerOrigin();
    }

    private PointValue _nextPointValue()
    {
        final PointValue pointValue = _peekedPointValue;

        if (pointValue != null) {
            _peekedPointValue = null;

            return pointValue;
        }

        return getNextPointValue().get();
    }

    private PointValue _peekPointValue()
    {
        if (_peekedPointValue == null) {
            _peekedPointValue = getNextPointValue().get();
        }

        return _peekedPointValue;
    }

    private PointValue _sendReadPointValueRequest(
            final Point point)
        throws InterruptedException, ConnectFailedException
    {
        final ModbusClient client = _support.getClient();
        final ReadTransaction.Request request = client
            .requestPointValue(point)
            .get();
        final Transaction.Response response = (Response) request
            .getResponse()
            .get();

        return ((ReadTransaction.Response) response).getPointValue().get();
    }

    private void _testReadRequests()
        throws Exception
    {
        final ModbusClient client = _support.getClient();
        final Map<Point, Register> registersByPoint = _support
            .getRegistersByPoint();

        final Point register1 = _support.getPoint(_TESTS_REGISTER_1);
        final Register register1Register = registersByPoint.get(register1);
        final Integer register1Address = register1Register.getAddress().get();
        final Point maskRegister1 = _support.getPoint(_TESTS_MASKED_REGISTER_1);

        _sendRequest(
            client
                .writeSingleRegister(
                    _getOrigin(),
                    register1Address.intValue(),
                    0xFFFF));
        _expectUpdate(register1, Long.valueOf(0xFFFF));

        _sendRequest(
            client
                .writePointValue(
                    new PointValue(
                            maskRegister1,
                                    Optional.of(_stamp),
                                    null,
                                    Integer.valueOf(0x0110))));
        _expectUpdate(register1, Long.valueOf(0xF11F));
        Require
            .equal(
                _sendReadPointValueRequest(maskRegister1).getValue(),
                Short.valueOf((short) 0x0110));

        final Point coil1Point = _support.getPoint(_TESTS_COIL_1);
        final Map<Point, Register> coilsByPoint = _support.getCoilsByPoint();
        final Register coil1Register = coilsByPoint.get(coil1Point);
        final Integer coil1Address = coil1Register.getAddress().get();
        final int coil0Address = coil1Address.intValue() - 1;

        Require
            .success(
                Arrays
                    .equals(
                            _sendReadRequest(
                                    client
                                            .readCoils(
                                                    _getOrigin(),
                                                            coil0Address,
                                                            4)),
                                    new short[] {1, 0, 1, 0, }));
        Require
            .equal(
                _sendReadPointValueRequest(coil1Point).getValue(),
                Boolean.FALSE);

        final Point coilArray1 = _support.getPoint(_TESTS_COIL_ARRAY_1);
        final Tuple tuple = new Tuple();

        tuple.add(Boolean.FALSE);
        tuple.add(Boolean.TRUE);
        tuple.add(Boolean.FALSE);

        putValue(coilArray1, _stamp, tuple.clone());
        Require.equal(_sendReadPointValueRequest(coilArray1).getValue(), tuple);

        final int stampAddress = register1Address.intValue() - 5;

        Require
            .success(
                Arrays
                    .equals(
                            _sendReadRequest(
                                    client
                                            .readHoldingRegisters(
                                                    _getOrigin(),
                                                            stampAddress,
                                                            9)),
                                    new short[] {ModbusClient.stamp0(
                                            _stamp), ModbusClient.stamp1(
                                                    _stamp), 1, 5, 2,
                                            (short) 0xF11F, 5, 4, 6, }));
        Require
            .equal(
                _sendReadPointValueRequest(register1).getValue(),
                Integer.valueOf(0xF11F));

        final Point registerArray1 = _support.getPoint(_TESTS_REGISTER_ARRAY_1);

        tuple.clear();
        tuple.add(Short.valueOf((short) 4));
        tuple.add(Short.valueOf((short) 5));
        tuple.add(Short.valueOf((short) 6));

        putValue(registerArray1, _stamp, tuple.clone());
        Require
            .equal(
                _sendReadPointValueRequest(registerArray1).getValue(),
                tuple);

        final Point discrete1 = _support.getPoint(_TESTS_DISCRETE_1);
        final Point discrete2 = _support.getPoint(_TESTS_DISCRETE_2);
        final Point discrete3 = _support.getPoint(_TESTS_DISCRETE_3);
        final Map<Point, Register> discretesByPoint = _support
            .getDiscretesByPoint();
        final Register discrete0Register = discretesByPoint.get(coil1Point);
        final Integer discrete0Address = discrete0Register.getAddress().get();

        putValue(discrete1, _stamp, Boolean.TRUE);
        putValue(discrete2, _stamp, Boolean.TRUE);
        putValue(discrete3, _stamp, Boolean.TRUE);

        Require
            .success(
                Arrays
                    .equals(
                            _sendReadRequest(
                                    client
                                            .readDiscreteInputs(
                                                    _getOrigin(),
                                                            discrete0Address
                                                                    .intValue(),
                                                            5)),
                                    new short[] {0, 0, 1, 1, 0, }));

        final Point input1 = _support.getPoint(_TESTS_INPUT_1);
        final Point input2 = _support.getPoint(_TESTS_INPUT_2);
        final Map<Point, Register> inputsByPoint = _support.getInputsByPoint();
        final Register input0Register = inputsByPoint.get(discrete1);
        final Integer input0Address = input0Register.getAddress().get();

        putValue(input1, _stamp, Integer.valueOf(11));
        putValue(input2, _stamp, Integer.valueOf(12));

        Require
            .success(
                Arrays
                    .equals(
                            _sendReadRequest(
                                    client
                                            .readInputRegisters(
                                                    _getOrigin(),
                                                            input0Address
                                                                    .intValue(),
                                                            3)),
                                    new short[] {4, 11, 12, }));

        final DateTime then = DateTime.now();
        final short[] timeWords = _sendReadRequest(
            client
                .readInputRegisters(
                    _getOrigin(),
                    input0Address.intValue() + 3,
                    4));
        final DateTime time = ModbusClient.dateTime(timeWords);

        Require.success(then.isNotAfter(time));
        Require.success(time.isNotAfter(DateTime.now()));
    }

    private void _testWriteRequests()
        throws Exception
    {
        final ModbusClient client = _support.getClient();

        final Point coil1 = _support.getPoint(_TESTS_COIL_1);
        final Point coil2 = _support.getPoint(_TESTS_COIL_2);
        final Point coil3 = _support.getPoint(_TESTS_COIL_3);
        final Map<Point, Register> coilsByPoint = _support.getCoilsByPoint();
        final Register coil1Register = coilsByPoint.get(coil1);
        final Integer coil1Address = coil1Register.getAddress().get();
        final int coil0Address = coil1Address.intValue() - 1;

        _stamp = DateTime.now();

        _sendRequest(client.writeSingleCoil(_getOrigin(), coil0Address + 1, 1));
        _expectUpdate(coil1, Boolean.TRUE);

        _sendRequest(
            client
                .writeMultipleCoils(
                    _getOrigin(),
                    coil0Address,
                    new short[] {1, 0, 1, 1, }));
        _expectUpdate(coil1, Boolean.FALSE);
        _expectUpdate(coil2, Boolean.TRUE);
        _expectUpdate(coil3, Boolean.TRUE);

        final Map<Point, Register> registersByPoint = _support
            .getRegistersByPoint();
        final Register bitsRegister = registersByPoint.get(coil1);
        final Integer bitsAddress = bitsRegister.getAddress().get();

        if (!isWriteOnly()) {
            _sendRequest(
                client
                    .maskWriteRegister(
                        _getOrigin(),
                        bitsAddress.intValue(),
                        0b0101,
                        0b0010));
            _expectUpdate(coil1, Boolean.TRUE);
            _expectUpdate(coil2, Boolean.TRUE);
            _expectUpdate(coil3, Boolean.FALSE);
        }

        _sendRequest(
            client
                .writeSingleRegister(_getOrigin(), bitsAddress.intValue(), 0));
        _expectUpdate(coil1, Boolean.FALSE);
        _expectUpdate(coil2, Boolean.FALSE);
        _expectUpdate(coil3, Boolean.FALSE);

        _sendRequest(
            client
                .writeSingleRegister(_getOrigin(), bitsAddress.intValue(), -1));
        _expectUpdate(coil1, Boolean.TRUE);
        _expectUpdate(coil2, Boolean.TRUE);
        _expectUpdate(coil3, Boolean.TRUE);

        final Point coilArray1 = _support.getPoint(_TESTS_COIL_ARRAY_1);
        final Tuple tuple = new Tuple();

        tuple.add(Boolean.TRUE);
        tuple.add(Boolean.FALSE);
        tuple.add(Boolean.TRUE);

        _sendRequest(
            client
                .writePointValue(
                    new PointValue(
                            coilArray1,
                                    Optional.of(DateTime.now()),
                                    null,
                                    tuple)));
        _expectUpdate(coilArray1, tuple);

        final Point register1 = _support.getPoint(_TESTS_REGISTER_1);
        final Point register2 = _support.getPoint(_TESTS_REGISTER_2);
        final Point register3 = _support.getPoint(_TESTS_REGISTER_3);
        final Point register4 = _support.getPoint(_TESTS_REGISTER_4);
        final Register register1Register = registersByPoint.get(register1);
        final Integer register1Address = register1Register.getAddress().get();
        final int stampAddress = register1Address.intValue() - 5;

        _stamp = DateTime
            .now()
            .floored(ElapsedTime.MILLI.toRaw() / 10)
            .after(1 * ElapsedTime.SECOND.toRaw());

        _sendRequest(
            client
                .writeMultipleRegisters(
                    _getOrigin(),
                    stampAddress,
                    new short[] {ModbusClient.stamp0(
                            _stamp), ModbusClient.stamp1(
                                    _stamp), 1, 5, 2, 3, 5, 4, 6, 8192,
                                    18417, }));
        Require.notNull(_peekPointValue());

        if (_support.isServerLocal()) {
            Require.equal(_support.getRemoteProxy().getStamp(), _stamp);
            Require.success(_support.getRemoteProxy().getSequence() == 1);
        }

        Require.equal(_expectUpdate(coil1, Boolean.FALSE).getStamp(), _stamp);
        Require.equal(_expectUpdate(coil2, Boolean.TRUE).getStamp(), _stamp);
        Require.equal(_expectUpdate(coil3, Boolean.FALSE).getStamp(), _stamp);
        Require
            .equal(
                _expectUpdate(register1, Short.valueOf((short) 3)).getStamp(),
                _stamp);
        Require
            .equal(
                _expectUpdate(register2, Integer.valueOf(262149)).getStamp(),
                _stamp);
        Require
            .equal(
                _expectUpdate(register3, Short.valueOf((short) 6)).getStamp(),
                _stamp);
        Require
            .equal(
                _expectUpdate(register4, Float.valueOf(123456)).getStamp(),
                _stamp);

        _sendRequest(
            client
                .writePointValue(
                    new PointValue(
                            register4,
                                    Optional.of(_stamp),
                                    null,
                                    Float.valueOf(654321))));
        Require
            .equal(
                _expectUpdate(register4, Float.valueOf(654321)).getStamp(),
                _stamp);

        final Register register3Register = registersByPoint.get(register3);
        final Integer register3Address = register3Register.getAddress().get();

        if (!isWriteOnly()) {
            Require
                .success(
                    Arrays
                        .equals(
                                _sendReadRequest(
                                        client
                                                .writeReadMultipleRegisters(
                                                        _getOrigin(),
                                                                register3Address
                                                                        .intValue(),
                                                                new short[] {0,
                                                                        0, 0, },
                                                                register1Address
                                                                        .intValue(),
                                                                3)),
                                        new short[] {3, 5, 4, }));
            _expectUpdate(register3, Integer.valueOf((short) 0));
            _expectUpdate(register4, Float.valueOf(0));
        }

        _sendRequest(
            client
                .writePointValue(
                    new PointValue(
                            register3,
                                    Optional.of(_stamp),
                                    null,
                                    Long.valueOf(6))));
        Require
            .equal(
                _expectUpdate(register3, Double.valueOf(6)).getStamp(),
                _stamp);

        final Point registerArray1 = _support.getPoint(_TESTS_REGISTER_ARRAY_1);

        tuple.clear();
        tuple.add(Short.valueOf((short) 1));
        tuple.add(Short.valueOf((short) 2));
        tuple.add(Short.valueOf((short) 3));

        _sendRequest(
            client
                .writePointValue(
                    new PointValue(
                            registerArray1,
                                    Optional.of(_stamp),
                                    null,
                                    tuple)));
        _expectUpdate(registerArray1, tuple);

        // Should log only one warning per unconfigured address.
        expectLogs(ModbusMessages.UNCONFIGURED_REGISTER);
        _sendRequest(
            client
                .writeSingleRegister(
                    _getOrigin(),
                    _TESTS_UNCONFIGURED_REGISTER_ADDRESS,
                    0));
        waitForLogs(ModbusMessages.UNCONFIGURED_REGISTER);
        _sendRequest(
            client
                .writeSingleRegister(
                    _getOrigin(),
                    _TESTS_UNCONFIGURED_REGISTER_ADDRESS,
                    0));
    }

    private static final String _TESTS_COIL_1 = "TESTS-MODBUS.COIL.1";
    private static final String _TESTS_COIL_2 = "TESTS-MODBUS.COIL.2";
    private static final String _TESTS_COIL_3 = "TESTS-MODBUS.COIL.3";
    private static final String _TESTS_COIL_ARRAY_1 =
        "TESTS-MODBUS.COIL-ARRAY.1";
    private static final String _TESTS_DISCRETE_1 = "TESTS-MODBUS.DISCRETE.1";
    private static final String _TESTS_DISCRETE_2 = "TESTS-MODBUS.DISCRETE.2";
    private static final String _TESTS_DISCRETE_3 = "TESTS-MODBUS.DISCRETE.3";
    private static final String _TESTS_INPUT_1 = "TESTS-MODBUS.INPUT.1";
    private static final String _TESTS_INPUT_2 = "TESTS-MODBUS.INPUT.2";
    private static final String _TESTS_MASKED_REGISTER_1 =
        "TESTS-MODBUS.MASKED-REGISTER.1";
    private static final String _TESTS_REGISTER_1 = "TESTS-MODBUS.REGISTER.1";
    private static final String _TESTS_REGISTER_2 = "TESTS-MODBUS.REGISTER.2";
    private static final String _TESTS_REGISTER_3 = "TESTS-MODBUS.REGISTER.3";
    private static final String _TESTS_REGISTER_4 = "TESTS-MODBUS.REGISTER.4";
    private static final String _TESTS_REGISTER_ARRAY_1 =
        "TESTS-MODBUS.REGISTER-ARRAY.1";
    private static final int _TESTS_UNCONFIGURED_REGISTER_ADDRESS = 99;

    private final Tests _owner;
    private PointValue _peekedPointValue;
    private DateTime _stamp;
    private final ModbusTestsSupport _support;
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
