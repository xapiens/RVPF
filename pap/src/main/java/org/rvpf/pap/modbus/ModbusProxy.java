/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusProxy.java 4053 2019-06-03 19:22:49Z SFB $
 */

package org.rvpf.pap.modbus;

import java.net.InetSocketAddress;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.DateTime;
import org.rvpf.base.Entity;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.pap.PAPMessages;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.modbus.transport.Connection;

/**
 * Modbus proxy.
 */
public abstract class ModbusProxy
    extends PAPProxy
{
    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected ModbusProxy(@Nonnull final ModbusProxy other)
    {
        super(other);

        _littleEndian = other._littleEndian;
        _middleEndian = other._middleEndian;
        _serialPortName = other._serialPortName;
        _socketAddresses.addAll(other._socketAddresses);
        _unitIdentifier = other._unitIdentifier;
    }

    /**
     * Constructs an instance.
     *
     * @param context The context.
     * @param origin The origin representing the remote.
     */
    protected ModbusProxy(
            @Nonnull final ModbusContext context,
            @Nonnull final Origin origin)
    {
        super(context, origin);
    }

    /**
     * Adds a socket address.
     *
     * @param socketAddress The socket address.
     */
    public final void addSocketAddress(
            @Nonnull final InetSocketAddress socketAddress)
    {
        _socketAddresses.add(socketAddress);
    }

    /** {@inheritDoc}
     */
    @Override
    public void disconnect()
    {
        final Optional<? extends Connection> connection = getConnection();

        if (connection.isPresent() && !connection.get().isClosed()) {
            connection.get().stop();
        }

        super.disconnect();
    }

    /**
     * Forgets the connection.
     */
    public final void forgetConnection()
    {
        _connection = null;
    }

    /**
     * Gets the sequence.
     *
     * @return The sequence.
     */
    @CheckReturnValue
    public final int getSequence()
    {
        return _sequence;
    }

    /**
     * Gets the serial port name.
     *
     * @return The serial port name (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    public final String getSerialPortName()
    {
        return Require.notNull(_serialPortName, "'setUp' has not been called");
    }

    /**
     * Gets the socket addresses.
     *
     * @return The socket addresses.
     */
    @Nonnull
    @CheckReturnValue
    public final List<InetSocketAddress> getSocketAddresses()
    {
        return Collections.unmodifiableList(_socketAddresses);
    }

    /**
     * Gets a time stamp.
     *
     * @return The time stamp.
     */
    @CheckReturnValue
    public DateTime getStamp()
    {
        return _stamp;
    }

    /**
     * Gets the unit identifier.
     *
     * @return The unit identifier.
     */
    @CheckReturnValue
    public final byte getUnitIdentifier()
    {
        return _unitIdentifier;
    }

    /**
     * Gets the little endian indicator.
     *
     * @return The little endian indicator.
     */
    @CheckReturnValue
    public final boolean isLittleEndian()
    {
        return _littleEndian;
    }

    /**
     * Sets the connection.
     *
     * @param connection The connection.
     */
    public void setConnection(@Nonnull final Connection connection)
    {
        _connection = connection;
    }

    /**
     * Sets the sequence.
     *
     * @param sequence The sequence.
     */
    public void setSequence(final int sequence)
    {
        _sequence = sequence;
    }

    /**
     * Sets the stamp.
     *
     * @param stamp The optional stamp.
     */
    public void setStamp(@Nonnull final Optional<DateTime> stamp)
    {
        _stamp = stamp.orElse(null);
    }

    /**
     * Gets the connection.
     *
     * @return The connection (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<? extends Connection> getConnection()
    {
        return Optional.ofNullable(_connection);
    }

    /**
     * Gets a register attribute.
     *
     * @param attributes The entity attributes.
     * @param key The attribute key.
     * @param entity The entity.
     *
     * @return The attribute (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<Integer> getRegisterAttribute(
            @Nonnull final Attributes attributes,
            @Nonnull final String key,
            @Nonnull final Entity entity)
    {
        Optional<Integer> attribute = attributes
            .getInteger(key, Optional.empty());

        if (attribute.isPresent()) {
            if ((attribute.get().intValue() < 0)
                    || (attribute.get().intValue() > 0xFFFF)) {
                getThisLogger()
                    .warn(
                        PAPMessages.BAD_ATTRIBUTE_VALUE,
                        key,
                        entity,
                        String.valueOf(attribute.get()));
                attribute = Optional.empty();
            }
        }

        return attribute;
    }

    /**
     * Gets the middle endian indicator.
     *
     * @return The middle endian indicator.
     */
    @CheckReturnValue
    protected final boolean isMiddleEndian()
    {
        return _middleEndian;
    }

    /**
     * Sets up this proxy.
     *
     * @param originAttributes The origin attributes.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean setUp(@Nonnull final Attributes originAttributes)
    {
        final int unitIdentifier = originAttributes
            .getInt(
                Modbus.UNIT_IDENTIFIER_ATTRIBUTE,
                Modbus.DEFAULT_UNIT_IDENTIFIER);

        if ((unitIdentifier < Modbus.MINIMUM_UNIT_IDENTIFIER)
                || (Modbus.MAXIMUM_UNIT_IDENTIFIER < unitIdentifier)) {
            getThisLogger()
                .warn(
                    ModbusMessages.BAD_UNIT_IDENTIFIER,
                    String.valueOf(unitIdentifier));
            _unitIdentifier = Modbus.DEFAULT_UNIT_IDENTIFIER;
        } else {
            _unitIdentifier = (byte) unitIdentifier;
        }

        _littleEndian = originAttributes
            .getBoolean(Modbus.LITTLE_ENDIAN_ATTRIBUTE);
        _middleEndian = originAttributes
            .getBoolean(Modbus.MIDDLE_ENDIAN_ATTRIBUTE);

        if (_littleEndian) {    // Adjusts for registers interpretation.
            _middleEndian = !_middleEndian;
        }

        _serialPortName = originAttributes
            .getString(Modbus.SERIAL_PORT_ATTRIBUTE, Optional.of(""))
            .get()
            .trim();

        return true;
    }

    /**
     * Sets up a bits register point assignment.
     *
     * @param address The optional register address.
     * @param bit The bit for the point.
     * @param point The point.
     * @param ignored True if ignored.
     * @param readOnly True if read-only.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpBitsRegister(
            @Nonnull Optional<Integer> address,
            @Nonnull Integer bit,
            @Nonnull Point point,
            boolean ignored,
            boolean readOnly);

    /**
     * Sets up a discrete array register.
     *
     * @param address The optional register address.
     * @param size The array size.
     * @param point The point.
     * @param ignored True if ignored.
     * @param readOnly True if read-only.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpDiscreteArrayRegister(
            @Nonnull Optional<Integer> address,
            int size,
            @Nonnull Point point,
            boolean ignored,
            boolean readOnly);

    /**
     * Sets up a discrete register.
     *
     * @param address The optional register address.
     * @param point The point.
     * @param ignored True if ignored.
     * @param readOnly True if read-only.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpDiscreteRegister(
            @Nonnull Optional<Integer> address,
            @Nonnull Point point,
            boolean ignored,
            boolean readOnly);

    /**
     * Sets up a double register.
     *
     * @param address The optional register address.
     * @param point The point.
     * @param ignored True if ignored.
     * @param readOnly True if read-only.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpDoubleRegister(
            @Nonnull Optional<Integer> address,
            @Nonnull Point point,
            boolean ignored,
            boolean readOnly);

    /**
     * Sets up a float register.
     *
     * @param address The optional register address.
     * @param point The point.
     * @param ignored True if ignored.
     * @param readOnly True if read-only.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpFloatRegister(
            @Nonnull Optional<Integer> address,
            @Nonnull Point point,
            boolean ignored,
            boolean readOnly);

    /**
     * Sets up an integer register.
     *
     * @param address The optional register address.
     * @param point The point.
     * @param ignored True value if ignored.
     * @param readOnly True if read-only.
     * @param signed True if signed.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpIntegerRegister(
            @Nonnull Optional<Integer> address,
            @Nonnull Point point,
            boolean ignored,
            boolean readOnly,
            boolean signed);

    /**
     * Sets up a long register.
     *
     * @param address The optional register address.
     * @param point The point.
     * @param ignored True value if ignored.
     * @param readOnly True if read-only.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpLongRegister(
            @Nonnull Optional<Integer> address,
            @Nonnull Point point,
            boolean ignored,
            boolean readOnly);

    /**
     * Sets up a masked register.
     *
     * @param address The optional register address.
     * @param point The point.
     * @param ignored True if ignored.
     * @param mask The mask.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpMaskedRegister(
            @Nonnull Optional<Integer> address,
            @Nonnull Point point,
            boolean ignored,
            int mask);

    /**
     * Sets up a sequence register.
     *
     * @param address The optional register address.
     * @param readOnly True if read-only.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpSequenceRegister(
            @Nonnull Optional<Integer> address,
            boolean readOnly);

    /**
     * Sets up a short register.
     *
     * @param address The optional register address.
     * @param point The point.
     * @param ignored True if ignored.
     * @param readOnly True if read-only.
     * @param signed True if signed.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpShortRegister(
            @Nonnull Optional<Integer> address,
            @Nonnull Point point,
            boolean ignored,
            boolean readOnly,
            boolean signed);

    /**
     * Sets up a stamp register.
     *
     * @param address The optional register address.
     * @param readOnly True if read-only.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpStampRegister(
            @Nonnull Optional<Integer> address,
            boolean readOnly);

    /**
     * Sets up a time register.
     *
     * @param address The optional register address.
     * @param readOnly True if read-only.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpTimeRegister(
            @Nonnull Optional<Integer> address,
            boolean readOnly);

    /**
     * Sets up a word array register.
     *
     * @param address The optional register address.
     * @param size The array size.
     * @param point The point.
     * @param ignored True if ignored.
     * @param readOnly True if read-only.
     *
     * @return True on success.
     */
    @CheckReturnValue
    abstract boolean setUpWordArrayRegister(
            @Nonnull Optional<Integer> address,
            int size,
            @Nonnull Point point,
            boolean ignored,
            boolean readOnly);

    private Connection _connection;
    private boolean _littleEndian;
    private boolean _middleEndian;
    private int _sequence;
    private String _serialPortName;
    private final List<InetSocketAddress> _socketAddresses = new LinkedList<>();
    private DateTime _stamp;
    private byte _unitIdentifier;
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
