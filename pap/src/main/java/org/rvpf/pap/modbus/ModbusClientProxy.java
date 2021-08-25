/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusClientProxy.java 4086 2019-06-16 16:36:41Z SFB $
 */

package org.rvpf.pap.modbus;

import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.filter.ValueFilter;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.modbus.message.MaskWriteRegister;
import org.rvpf.pap.modbus.message.ReadCoils;
import org.rvpf.pap.modbus.message.ReadDiscreteInputs;
import org.rvpf.pap.modbus.message.ReadHoldingRegisters;
import org.rvpf.pap.modbus.message.ReadInputRegisters;
import org.rvpf.pap.modbus.message.ReadTransaction;
import org.rvpf.pap.modbus.message.Transaction;
import org.rvpf.pap.modbus.message.WriteMultipleCoils;
import org.rvpf.pap.modbus.message.WriteMultipleRegisters;
import org.rvpf.pap.modbus.message.WriteReadMultipleRegisters;
import org.rvpf.pap.modbus.message.WriteSingleCoil;
import org.rvpf.pap.modbus.message.WriteSingleRegister;
import org.rvpf.pap.modbus.message.WriteTransaction;
import org.rvpf.pap.modbus.register.ArrayRegister;
import org.rvpf.pap.modbus.register.BitsRegister;
import org.rvpf.pap.modbus.register.DiscreteArrayRegister;
import org.rvpf.pap.modbus.register.DiscreteRegister;
import org.rvpf.pap.modbus.register.DoubleRegister;
import org.rvpf.pap.modbus.register.FloatRegister;
import org.rvpf.pap.modbus.register.IntegerRegister;
import org.rvpf.pap.modbus.register.LongRegister;
import org.rvpf.pap.modbus.register.Register;
import org.rvpf.pap.modbus.register.SequenceRegister;
import org.rvpf.pap.modbus.register.ShortRegister;
import org.rvpf.pap.modbus.register.StampRegister;
import org.rvpf.pap.modbus.register.TimeRegister;
import org.rvpf.pap.modbus.register.WordArrayRegister;
import org.rvpf.pap.modbus.register.WordRegister;
import org.rvpf.pap.modbus.transport.Connection;
import org.rvpf.pap.modbus.transport.ServerConnection;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;

/**
 * Modbus client proxy.
 */
public final class ModbusClientProxy
    extends ModbusProxy
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param origin The origin representing the remote.
     */
    ModbusClientProxy(
            @Nonnull final ModbusContext context,
            @Nonnull final Origin origin)
    {
        super(context, origin);
    }

    private ModbusClientProxy(final ModbusClientProxy other)
    {
        super(other);

        _coilsByAddress.putAll(other._coilsByAddress);
        _discretesByAddress.putAll(other._discretesByAddress);
        _inputsByAddress.putAll(other._inputsByAddress);
        _registersByAddress.putAll(other._registersByAddress);
        _stampTick = other._stampTick;
    }

    /**
     * Adds a request.
     *
     * @param request The request.
     */
    public void addRequest(@Nonnull final Transaction.Request request)
    {
        getThisLogger()
            .trace(
                () -> new Message(
                    ModbusMessages.RECEIVED_REQUEST,
                    request.getName()));

        if (request instanceof WriteTransaction.Request) {
            _writeRequestCount.incrementAndGet();
        }

        _requests.add(request);
    }

    /** {@inheritDoc}
     */
    @Override
    public PAPProxy copy()
    {
        return new ModbusClientProxy(this);
    }

    /**
     * Gets the coil registers
     *
     * <p>Used by tests.</p>
     *
     * @return The coil registers.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<Register> getCoils()
    {
        return _coilsByAddress.values();
    }

    /**
     * Gets a discrete registers.
     *
     * <p>Used by tests.</p>
     *
     * @return The discrete registers.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<Register> getDiscretes()
    {
        return _discretesByAddress.values();
    }

    /**
     * Gets input registers.
     *
     * <p>Used by tests.</p>
     *
     * @return The input registers.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<Register> getInputs()
    {
        return _inputsByAddress.values();
    }

    /**
     * Gets the registers.
     *
     * <p>Used by tests.</p>
     *
     * @return The registers.
     */
    @Nonnull
    @CheckReturnValue
    public Collection<Register> getRegisters()
    {
        return _registersByAddress.values();
    }

    /** {@inheritDoc}
     */
    @Override
    public DateTime getStamp()
    {
        DateTime stamp = super.getStamp();

        if (stamp != null) {
            final DateTime now = DateTime.now();
            final ElapsedTime elapsed = now.sub(stamp);

            if (elapsed.compareTo(_stampTick) >= 0) {
                if (!ElapsedTime.EMPTY.equals(_stampTick)) {
                    getThisLogger().warn(ModbusMessages.TICK_EXPIRED, stamp);
                }

                super.setStamp(Optional.empty());
                stamp = null;
            }
        }

        return stamp;
    }

    /**
     * Asks if this proxy has pending updates.
     *
     * @return True if it has pending updates.
     */
    @CheckReturnValue
    public boolean hasPendingUpdates()
    {
        return _writeRequestCount.get() > 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws ServiceNotAvailableException
    {
        final DateTime startTime = DateTime.now();

        getThisLogger()
            .debug(PAPMessages.STARTED_SERVICES, getOrigin().getName().get());

        try {
            for (;;) {
                final Transaction.Request request;
                final DateTime stamp = getStamp();

                if (_stampTick != null) {
                    request = _requests
                        .poll(_stampTick.toMillis(), TimeUnit.MILLISECONDS);

                    if (request == null) {
                        getThisLogger()
                            .warn(
                                ModbusMessages.TICK_EXPIRED,
                                (stamp != null)? stamp: startTime);
                        setStamp(Optional.empty());

                        continue;
                    }
                } else {
                    request = _requests.take();
                }

                getThisLogger()
                    .trace(
                        () -> new Message(
                            ModbusMessages.PROCESSING_REQUEST,
                            request.getName()));

                switch (request.getFunctionCode()) {
                    case ReadCoils.FUNCTION_CODE: {
                        _sendResponse(
                            request,
                            _readRegisters(
                                request,
                                _coilsByAddress,
                                ModbusMessages.UNCONFIGURED_COIL,
                                false));

                        break;
                    }
                    case ReadDiscreteInputs.FUNCTION_CODE: {
                        _sendResponse(
                            request,
                            _readRegisters(
                                request,
                                _discretesByAddress,
                                ModbusMessages.UNCONFIGURED_DISCRETE,
                                true));

                        break;
                    }
                    case ReadHoldingRegisters.FUNCTION_CODE: {
                        _sendResponse(
                            request,
                            _readRegisters(
                                request,
                                _registersByAddress,
                                ModbusMessages.UNCONFIGURED_REGISTER,
                                false));

                        break;
                    }
                    case ReadInputRegisters.FUNCTION_CODE: {
                        _sendResponse(
                            request,
                            _readRegisters(
                                request,
                                _inputsByAddress,
                                ModbusMessages.UNCONFIGURED_INPUT,
                                true));

                        break;
                    }
                    case WriteReadMultipleRegisters.FUNCTION_CODE: {
                        _sendResponse(
                            request,
                            _writeReadRegisters(
                                request,
                                _registersByAddress,
                                ModbusMessages.UNCONFIGURED_REGISTER));

                        break;
                    }
                    case WriteSingleCoil.FUNCTION_CODE:
                    case WriteMultipleCoils.FUNCTION_CODE: {
                        _writeRegisters(
                            request,
                            _coilsByAddress,
                            ModbusMessages.UNCONFIGURED_COIL);

                        break;
                    }
                    case WriteSingleRegister.FUNCTION_CODE:
                    case WriteMultipleRegisters.FUNCTION_CODE: {
                        _writeRegisters(
                            request,
                            _registersByAddress,
                            ModbusMessages.UNCONFIGURED_REGISTER);

                        break;
                    }
                    case MaskWriteRegister.FUNCTION_CODE: {
                        _maskWriteRegister(
                            request,
                            _registersByAddress,
                            ModbusMessages.UNCONFIGURED_REGISTER);

                        break;
                    }
                    default: {
                        throw new InternalError();
                    }
                }

                if (request instanceof WriteTransaction.Request) {
                    _writeRequestCount.decrementAndGet();
                }
            }
        } catch (final InterruptedException exception) {
            getThisLogger()
                .debug(
                    PAPMessages.STOPPED_SERVICES,
                    getOrigin().getName().get());
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void setConnection(final Connection newConnection)
    {
        final Optional<? extends Connection> oldConnection = getConnection();

        if (oldConnection.isPresent()) {
            oldConnection.get().stop();
        }

        super.setConnection(newConnection);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setSequence(final int sequence)
    {
        final int expected = (getSequence() + 1) & 0xFFFF;

        if (sequence != expected) {
            getThisLogger()
                .warn(
                    ModbusMessages.OUT_OF_SEQUENCE,
                    String.valueOf(expected),
                    String.valueOf(sequence));
        }

        super.setSequence(sequence);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setStamp(final Optional<DateTime> stamp)
    {
        if (_stampTick != null) {
            super.setStamp(stamp);
        }
    }

    /**
     * Starts.
     *
     * @param server The Modbus server.
     */
    public void start(@Nonnull final ModbusServer server)
    {
        final ServiceThread thread = new ServiceThread(
            this,
            "Modbus server (proxy " + getOrigin() + ")");

        if (_thread.compareAndSet(null, thread)) {
            _server = server;

            getThisLogger()
                .debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();
        }
    }

    /**
     * Stops.
     */
    public void stop()
    {
        final Optional<? extends Connection> connection = getConnection();

        if (connection.isPresent()) {
            connection.get().stop();
            forgetConnection();
        }

        final ServiceThread thread = _thread.getAndSet(null);

        if (thread != null) {
            getThisLogger()
                .debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            Require.ignored(thread.interruptAndJoin(getThisLogger(), 0));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsWildcardAddress()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final Attributes originAttributes)
    {
        if (!super.setUp(originAttributes)) {
            return false;
        }

        setStampTick(
            originAttributes
                .getElapsed(
                    Modbus.STAMP_TICK_ATTRIBUTE,
                    Optional.empty(),
                    Optional.empty()));

        if (!setUpStampRegister(
                getRegisterAttribute(
                    originAttributes,
                    Modbus.STAMP_ADDRESS_ATTRIBUTE,
                    getOrigin()),
                false)) {
            return false;
        }

        if (!setUpSequenceRegister(
                getRegisterAttribute(
                    originAttributes,
                    Modbus.SEQUENCE_ADDRESS_ATTRIBUTE,
                    getOrigin()),
                false)) {
            return false;
        }

        if (!setUpTimeRegister(
                getRegisterAttribute(
                    originAttributes,
                    Modbus.TIME_ADDRESS_ATTRIBUTE,
                    getOrigin()),
                false)) {
            return false;
        }

        return true;
    }

    /**
     * Sets the stamp tick.
     *
     * @param stampTick The optional stamp tick.
     */
    void setStampTick(@Nonnull final Optional<ElapsedTime> stampTick)
    {
        _stampTick = stampTick.orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpBitsRegister(
            final Optional<Integer> address,
            final Integer bit,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        final NavigableMap<Integer, Register> registers = readOnly
            ? _inputsByAddress: _registersByAddress;
        final Register register = registers.get(address.get());
        final BitsRegister bitsRegister;

        if (register == null) {
            bitsRegister = new BitsRegister(address, readOnly);
            registers.put(address.get(), bitsRegister);
        } else {
            if (!(register instanceof BitsRegister)) {
                getThisLogger()
                    .warn(ModbusMessages.OVERLOADED_ADDRESS, address, point);

                return false;
            }

            bitsRegister = (BitsRegister) register;
        }

        return bitsRegister
            .setPoint(
                ignored? Optional.empty(): Optional.of(point),
                bit.intValue());
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpDiscreteArrayRegister(
            final Optional<Integer> address,
            final int size,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        final ArrayRegister arrayRegister = new DiscreteArrayRegister(
            address,
            size,
            ignored? Optional.empty(): Optional.of(point),
            readOnly);

        return _setUpArrayRegister(
            arrayRegister,
            readOnly? _discretesByAddress: _coilsByAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpDiscreteRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        return _addRegister(
            new DiscreteRegister(
                address,
                ignored? Optional.empty(): Optional.of(point),
                readOnly),
            readOnly? _discretesByAddress: _coilsByAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpDoubleRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        final NavigableMap<Integer, Register> registersByAddress = readOnly
            ? _inputsByAddress: _registersByAddress;
        final DoubleRegister doubleRegister = new DoubleRegister(
            address,
            ignored? Optional.empty(): Optional.of(point),
            readOnly,
            isMiddleEndian());

        for (int i = 0; i < doubleRegister.size(); ++i) {
            if (!_addRegister(
                    doubleRegister.getWordRegister(i),
                    registersByAddress)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpFloatRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        final NavigableMap<Integer, Register> registersByAddress = readOnly
            ? _inputsByAddress: _registersByAddress;
        final FloatRegister floatRegister = new FloatRegister(
            address,
            ignored? Optional.empty(): Optional.of(point),
            readOnly,
            isMiddleEndian());

        if (!_addRegister(floatRegister, registersByAddress)) {
            return false;
        }

        return _addRegister(
            floatRegister.getNextRegister(),
            registersByAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpIntegerRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly,
            final boolean signed)
    {
        if (!address.isPresent()) {
            return true;
        }

        final NavigableMap<Integer, Register> registersByAddress = readOnly
            ? _inputsByAddress: _registersByAddress;
        final IntegerRegister integerRegister = new IntegerRegister(
            address,
            ignored? Optional.empty(): Optional.of(point),
            signed,
            readOnly,
            isMiddleEndian());

        if (!_addRegister(integerRegister, registersByAddress)) {
            return false;
        }

        return _addRegister(
            integerRegister.getNextRegister(),
            registersByAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpLongRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        final NavigableMap<Integer, Register> registersByAddress = readOnly
            ? _inputsByAddress: _registersByAddress;
        final LongRegister longRegister = new LongRegister(
            address,
            ignored? Optional.empty(): Optional.of(point),
            readOnly,
            isMiddleEndian());

        for (int i = 0; i < longRegister.size(); ++i) {
            if (!_addRegister(
                    longRegister.getWordRegister(i),
                    registersByAddress)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpMaskedRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final int mask)
    {
        return true;    // Ignored.
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpSequenceRegister(
            final Optional<Integer> address,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        return _addRegister(
            new SequenceRegister(address, Optional.of(this), readOnly),
            readOnly? _inputsByAddress: _registersByAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpShortRegister(
            final Optional<Integer> address,
            final Point point,
            final boolean ignored,
            final boolean readOnly,
            final boolean signed)
    {
        if (!address.isPresent()) {
            return true;
        }

        return _addRegister(
            new ShortRegister(
                address,
                ignored? Optional.empty(): Optional.of(point),
                signed,
                readOnly),
            readOnly? _inputsByAddress: _registersByAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpStampRegister(
            final Optional<Integer> address,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        if (_stampTick == null) {
            getThisLogger()
                .warn(
                    PAPMessages.MISSING_ATTRIBUTE,
                    Modbus.STAMP_TICK_ATTRIBUTE,
                    Modbus.ATTRIBUTES_USAGE,
                    getOrigin());
            _stampTick = ElapsedTime.EMPTY;
        }

        final NavigableMap<Integer, Register> registersByAddress = readOnly
            ? _inputsByAddress: _registersByAddress;
        final StampRegister stampRegister = new StampRegister(
            address,
            Optional.of(this),
            readOnly,
            isMiddleEndian());

        if (!_addRegister(stampRegister, registersByAddress)) {
            return false;
        }

        return _addRegister(
            stampRegister.getNextRegister(),
            registersByAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpTimeRegister(
            final Optional<Integer> address,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        final NavigableMap<Integer, Register> registersByAddress = readOnly
            ? _inputsByAddress: _registersByAddress;
        final TimeRegister timeRegister = new TimeRegister(
            address,
            Optional.of(this),
            readOnly,
            isMiddleEndian());
        final StampRegister stampRegister = timeRegister.getStampRegister();

        if (!_addRegister(timeRegister, registersByAddress)) {
            return false;
        }

        if (!_addRegister(timeRegister.getNextRegister(), registersByAddress)) {
            return false;
        }

        if (!_addRegister(stampRegister, registersByAddress)) {
            return false;
        }

        return _addRegister(
            stampRegister.getNextRegister(),
            registersByAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    boolean setUpWordArrayRegister(
            final Optional<Integer> address,
            final int size,
            final Point point,
            final boolean ignored,
            final boolean readOnly)
    {
        if (!address.isPresent()) {
            return true;
        }

        final ArrayRegister arrayRegister = new WordArrayRegister(
            address,
            size,
            ignored? Optional.empty(): Optional.of(point),
            readOnly);

        return _setUpArrayRegister(
            arrayRegister,
            readOnly? _inputsByAddress: _registersByAddress);
    }

    private boolean _addRegister(
            final Register register,
            final NavigableMap<Integer, Register> registersByAddress)
    {
        final Integer address = register.getAddress().get();

        if ((address.intValue() < 1) || (65536 < address.intValue())) {
            getThisLogger()
                .warn(
                    ModbusMessages.INVALID_ADDRESS,
                    address,
                    Arrays.toString(register.getPoints()));

            return false;
        }

        if (registersByAddress.get(address) != null) {
            getThisLogger()
                .warn(
                    ModbusMessages.OVERLOADED_ADDRESS,
                    address,
                    Arrays.toString(register.getPoints()));

            return false;
        }

        registersByAddress.put(address, register);

        return true;
    }

    private void _maskWriteRegister(
            final Transaction.Request request,
            final NavigableMap<Integer, Register> registers,
            final Messages.Entry unconfiguredAddress)
        throws InterruptedException, ServiceNotAvailableException
    {
        final short[] values = _readRegisters(
            request,
            registers,
            unconfiguredAddress,
            false);
        final short value = (values.length == 1)? values[0]: 0;

        ((MaskWriteRegister.Request) request).applyMasks(value);
        _writeRegisters(request, registers, unconfiguredAddress);
    }

    private short[] _readRegisters(
            final Transaction.Request request,
            final NavigableMap<Integer, Register> registers,
            final Messages.Entry unconfiguredAddress,
            final boolean readOnly)
        throws InterruptedException, ServiceNotAvailableException
    {
        final int startingAddress = request.getReadAddress();
        final int quantity = request.getReadQuantity();
        final int stoppingAddress = startingAddress + quantity;
        final List<Point> points = new LinkedList<>();
        final Map<Point, Register> registersByPoint = new IdentityHashMap<>();
        Map.Entry<Integer, Register> ceilingEntry = registers
            .ceilingEntry(Integer.valueOf(startingAddress));

        for (int address = startingAddress; address < stoppingAddress; ) {
            final Register register;

            if ((ceilingEntry == null)
                    || (address < ceilingEntry.getKey().intValue())) {
                register = new WordRegister(
                    Optional.of(Integer.valueOf(address)),
                    Optional.empty(),
                    readOnly);
                getThisLogger()
                    .warn(unconfiguredAddress, String.valueOf(address));
                registers.put(Integer.valueOf(address), register);
            } else {
                register = ceilingEntry.getValue();
                ceilingEntry = registers.higherEntry(ceilingEntry.getKey());

                for (final Point point: register.getPoints()) {
                    if (point != null) {
                        points.add(point);
                        registersByPoint.put(point, register);
                    }
                }
            }

            ++address;
        }

        if (!points.isEmpty()) {
            if ((request instanceof ReadTransaction)
                    && !(request instanceof WriteReadMultipleRegisters)) {
                _server.waitWhileUpdating();
            }

            final ModbusServerContext serverContext =
                (ModbusServerContext) _server
                    .getContext();
            final PointValue[] pointValues = _server
                .getResponder()
                .get()
                .select(points.toArray(new Point[points.size()]));

            for (final PointValue pointValue: pointValues) {
                if (pointValue != null) {
                    final Point proxyPoint = serverContext
                        .getRemotePoint(pointValue.getPointUUID())
                        .orElse(null);

                    if (proxyPoint != null) {
                        final Register register = registersByPoint
                            .get(proxyPoint);

                        register
                            .putPointValue(
                                pointValue
                                    .morph(
                                            Optional.of(proxyPoint),
                                                    Optional.empty())
                                    .encoded());
                    }
                }
            }
        }

        final short[] values = new short[quantity];
        int index = 0;

        for (final Register register:
                registers
                    .subMap(
                            Integer.valueOf(startingAddress),
                                    Integer.valueOf(stoppingAddress))
                    .values()) {
            values[index++] = register.getContent();
        }

        return values;
    }

    private void _sendResponse(
            final Transaction.Request request,
            final short[] values)
        throws InterruptedException
    {
        final Optional<? extends Connection> connection = getConnection();

        if (!connection.isPresent()) {
            throw new InterruptedException();
        }

        ((ServerConnection) connection.get())
            .sendResponse(
                ((ReadTransaction.Request) request).createResponse(values));
    }

    private boolean _setUpArrayRegister(
            final ArrayRegister arrayRegister,
            final NavigableMap<Integer, Register> registersByAddress)
    {
        if (!_addRegister(arrayRegister, registersByAddress)) {
            return false;
        }

        for (int i = 1; i < arrayRegister.size(); ++i) {
            if (!_addRegister(arrayRegister.newMinion(i), registersByAddress)) {
                return false;
            }
        }

        return true;
    }

    private short[] _writeReadRegisters(
            final Transaction.Request request,
            final NavigableMap<Integer, Register> registers,
            final Messages.Entry unconfiguredAddress)
        throws InterruptedException, ServiceNotAvailableException
    {
        _writeRegisters(request, registers, unconfiguredAddress);

        return _readRegisters(request, registers, unconfiguredAddress, false);
    }

    private void _writeRegisters(
            final Transaction.Request request,
            final NavigableMap<Integer, Register> registers,
            final Messages.Entry unconfiguredAddress)
    {
        final int startingAddress = request.getWriteAddress();
        final short[] registerValues = request.getWriteValues();
        final int stoppingAddress = startingAddress + registerValues.length;
        final DateTime requestStamp = request.getStamp();
        Map.Entry<Integer, Register> ceilingEntry = registers
            .ceilingEntry(Integer.valueOf(startingAddress));

        for (int address = startingAddress; address < stoppingAddress; ) {
            final Register register;

            if ((ceilingEntry == null)
                    || (address < ceilingEntry.getKey().intValue())) {
                register = new WordRegister(
                    Optional.of(Integer.valueOf(address)),
                    Optional.empty(),
                    false);
                getThisLogger()
                    .warn(unconfiguredAddress, String.valueOf(address));
                registers.put(Integer.valueOf(address), register);
            } else {
                register = ceilingEntry.getValue();
                ceilingEntry = registers.higherEntry(ceilingEntry.getKey());
            }

            register.setContent(registerValues[address - startingAddress]);
            ++address;
        }

        for (final Register register:
                registers
                    .subMap(
                            Integer.valueOf(startingAddress),
                                    Integer.valueOf(stoppingAddress))
                    .values()) {
            for (PointValue pointValue: register.getPointValues()) {
                if (pointValue != null) {
                    final ModbusServerContext serverContext =
                        (ModbusServerContext) _server
                            .getContext();
                    final Map<Point, ValueFilter> valueFilters = serverContext
                        .getValueFilters();
                    final ValueFilter filter = valueFilters
                        .get(pointValue.getPoint().get());
                    final DateTime stamp = getStamp();

                    pointValue.setStamp((stamp != null)? stamp: requestStamp);

                    if (filter != null) {
                        for (final PointValue filteredValue:
                                filter.filter(Optional.of(pointValue))) {
                            _server.addPointValue(filteredValue);
                        }
                    } else {
                        _server.addPointValue(pointValue);
                    }
                }
            }
        }
    }

    private final NavigableMap<Integer, Register> _coilsByAddress =
        new TreeMap<>();
    private final NavigableMap<Integer, Register> _discretesByAddress =
        new TreeMap<>();
    private final NavigableMap<Integer, Register> _inputsByAddress =
        new TreeMap<>();
    private final NavigableMap<Integer, Register> _registersByAddress =
        new TreeMap<>();
    private final BlockingQueue<Transaction.Request> _requests =
        new LinkedBlockingQueue<>();
    private ModbusServer _server;
    private ElapsedTime _stampTick;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();
    private final AtomicInteger _writeRequestCount = new AtomicInteger();
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
