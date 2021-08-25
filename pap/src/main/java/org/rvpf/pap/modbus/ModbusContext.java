/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusContext.java 4086 2019-06-16 16:36:41Z SFB $
 */

package org.rvpf.pap.modbus;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.BaseMessages;
import org.rvpf.base.Entity;
import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Traces;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAP;
import org.rvpf.pap.PAPContext;
import org.rvpf.pap.PAPMessages;

/**
 * Modbus context.
 */
public abstract class ModbusContext
    extends PAPContext
{
    /**
     * Constructs an instance.
     *
     * @param metadata The optional metadata.
     * @param traces The traces (optional).
     */
    ModbusContext(
            @Nonnull final Optional<Metadata> metadata,
            @Nonnull final Optional<Traces> traces)
    {
        super(new ModbusSupport(), metadata, traces);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean addRemoteOrigin(
            final Origin remoteOrigin,
            final Attributes originAttributes)
    {
        final boolean isFirstProxy = getRemoteProxyByOrigin().isEmpty();

        if (_remoteProxyByInetAddress.isEmpty() && !isFirstProxy) {
            getThisLogger().warn(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);

            return false;
        }

        final ModbusProxy remoteProxy = newRemoteProxy(remoteOrigin);

        if (!remoteProxy.setUp(originAttributes)) {
            return false;
        }

        if (!_addRemoteProxyAddresses(
                remoteProxy,
                isFirstProxy,
                originAttributes)) {
            return false;
        }

        final String serialPortName = remoteProxy.getSerialPortName();

        if (!serialPortName.isEmpty()) {
            _remoteProxyBySerialPortName
                .put(serialPortName.toUpperCase(Locale.ROOT), remoteProxy);
        }

        registerRemoteProxy(remoteProxy);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean addRemotePoint(
            final Point remotePoint,
            final Attributes pointAttributes)
    {
        final ModbusProxy remoteProxy = (ModbusProxy) getRemoteProxy(
            remotePoint)
            .orElse(null);

        if (remoteProxy == null) {
            return false;
        }

        if (pointAttributes.getBoolean(PAP.CONNECTION_STATE_ATTRIBUTE)) {
            return true;
        }

        final Integer registerAddress = getRegisterAttribute(
            pointAttributes,
            Modbus.REGISTER_ADDRESS_ATTRIBUTE,
            remotePoint)
            .orElse(null);
        final Integer coilAddress = getRegisterAttribute(
            pointAttributes,
            Modbus.COIL_ADDRESS_ATTRIBUTE,
            remotePoint)
            .orElse(null);
        final Integer inputAddress = getRegisterAttribute(
            pointAttributes,
            Modbus.INPUT_ADDRESS_ATTRIBUTE,
            remotePoint)
            .orElse(null);
        final Integer discreteAddress = getRegisterAttribute(
            pointAttributes,
            Modbus.DISCRETE_ADDRESS_ATTRIBUTE,
            remotePoint)
            .orElse(null);

        if ((registerAddress == null)
                && (coilAddress == null)
                && (inputAddress == null)
                && (discreteAddress == null)) {
            getThisLogger().warn(ModbusMessages.NO_ADDRESS, remotePoint);

            return false;
        }

        final Optional<Integer> bit = pointAttributes
            .getInteger(Modbus.BIT_ATTRIBUTE, Optional.empty());
        final boolean signed = pointAttributes
            .getBoolean(Modbus.SIGNED_ATTRIBUTE);
        final Optional<Integer> arraySize = pointAttributes
            .getInteger(Modbus.ARRAY_SIZE_ATTRIBUTE, Optional.empty());
        final Optional<Integer> mask = pointAttributes
            .getInteger(Modbus.MASK_ATTRIBUTE, Optional.empty());
        String valueType = pointAttributes
            .getString(Modbus.TYPE_ATTRIBUTE)
            .orElse(null);

        if (valueType != null) {
            valueType = valueType.toUpperCase(Locale.ROOT);
        } else if (arraySize.isPresent()) {
            valueType = Modbus.ARRAY_TYPE;
        } else if (mask.isPresent()) {
            valueType = Modbus.MASK_TYPE;
        } else {
            valueType = (bit.isPresent()
                    || (coilAddress != null)
                    || (discreteAddress != null))? Modbus.BIT_TYPE
                    : Modbus.SHORT_TYPE;
        }

        if (arraySize.isPresent()) {
            if (!Modbus.ARRAY_TYPE.equals(valueType)) {
                getThisLogger()
                    .warn(
                        PAPMessages.BAD_ATTRIBUTE_VALUE,
                        Modbus.TYPE_ATTRIBUTE,
                        remotePoint,
                        valueType);

                return false;
            }

            if ((arraySize.get().intValue() < 1)
                    || ((registerAddress != null)
                        && (arraySize.get().intValue()
                            > Modbus.MAXIMUM_REGISTER_ARRAY))
                    || ((inputAddress != null)
                        && (arraySize.get().intValue()
                            > Modbus.MAXIMUM_INPUT_ARRAY))
                    || ((coilAddress != null)
                        && (arraySize.get().intValue()
                            > Modbus.MAXIMUM_COIL_ARRAY))
                    || ((discreteAddress != null)
                        && (arraySize.get().intValue()
                            > Modbus.MAXIMUM_DISCRETE_ARRAY))) {
                getThisLogger()
                    .warn(
                        PAPMessages.BAD_ATTRIBUTE_VALUE,
                        Modbus.ARRAY_SIZE_ATTRIBUTE,
                        remotePoint,
                        arraySize);

                return false;
            }
        }

        if (bit.isPresent()) {
            if ((registerAddress == null) && (inputAddress == null)) {
                getThisLogger()
                    .warn(ModbusMessages.NO_ADDRESS_FOR_BIT, remotePoint);

                return false;
            }

            if ((bit.get().intValue() < 0) || (bit.get().intValue() > 15)) {
                getThisLogger()
                    .warn(
                        PAPMessages.BAD_ATTRIBUTE_VALUE,
                        Modbus.BIT_ATTRIBUTE,
                        remotePoint,
                        bit.get());

                return false;
            }

            if (!Modbus.BIT_TYPE.equals(valueType)) {
                getThisLogger()
                    .warn(
                        PAPMessages.BAD_ATTRIBUTE_VALUE,
                        Modbus.TYPE_ATTRIBUTE,
                        remotePoint,
                        valueType);

                return false;
            }
        } else if (Modbus.BIT_TYPE.equals(valueType)) {
            if ((registerAddress != null) || (inputAddress != null)) {
                getThisLogger()
                    .warn(
                        PAPMessages.MISSING_ATTRIBUTE,
                        Modbus.BIT_ATTRIBUTE,
                        Modbus.ATTRIBUTES_USAGE,
                        remotePoint);

                return false;
            }
        } else if (Modbus.MASK_TYPE.equals(valueType)) {
            if (registerAddress == null) {
                getThisLogger()
                    .warn(
                        PAPMessages.MISSING_ATTRIBUTE,
                        Modbus.REGISTER_ADDRESS_ATTRIBUTE,
                        Modbus.ATTRIBUTES_USAGE,
                        remotePoint);

                return false;
            }
        }

        final boolean ignored = pointAttributes
            .getBoolean(Modbus.IGNORED_ATTRIBUTE);
        boolean success;

        if (Modbus.BIT_TYPE.equals(valueType)) {
            success = remoteProxy
                .setUpBitsRegister(
                    Optional.ofNullable(registerAddress),
                    bit.orElse(null),
                    remotePoint,
                    ignored,
                    false);
            success &= remoteProxy
                .setUpDiscreteRegister(
                    Optional.ofNullable(coilAddress),
                    remotePoint,
                    ignored,
                    false);
            success &= remoteProxy
                .setUpBitsRegister(
                    Optional.ofNullable(inputAddress),
                    bit.orElse(null),
                    remotePoint,
                    ignored,
                    true);
            success &= remoteProxy
                .setUpDiscreteRegister(
                    Optional.ofNullable(discreteAddress),
                    remotePoint,
                    ignored,
                    true);
        } else if (Modbus.SHORT_TYPE.equals(valueType)) {
            success = remoteProxy
                .setUpShortRegister(
                    Optional.ofNullable(registerAddress),
                    remotePoint,
                    ignored,
                    false,
                    signed);
            success &= remoteProxy
                .setUpShortRegister(
                    Optional.ofNullable(inputAddress),
                    remotePoint,
                    ignored,
                    true,
                    signed);
        } else if (Modbus.INTEGER_TYPE.equals(valueType)) {
            success = remoteProxy
                .setUpIntegerRegister(
                    Optional.ofNullable(registerAddress),
                    remotePoint,
                    ignored,
                    false,
                    signed);
            success &= remoteProxy
                .setUpIntegerRegister(
                    Optional.ofNullable(inputAddress),
                    remotePoint,
                    ignored,
                    true,
                    signed);
        } else if (Modbus.FLOAT_TYPE.equals(valueType)) {
            success = remoteProxy
                .setUpFloatRegister(
                    Optional.ofNullable(registerAddress),
                    remotePoint,
                    ignored,
                    false);
            success &= remoteProxy
                .setUpFloatRegister(
                    Optional.ofNullable(inputAddress),
                    remotePoint,
                    ignored,
                    true);
        } else if (Modbus.LONG_TYPE.equals(valueType)) {
            success = remoteProxy
                .setUpLongRegister(
                    Optional.ofNullable(registerAddress),
                    remotePoint,
                    ignored,
                    false);
            success &= remoteProxy
                .setUpLongRegister(
                    Optional.ofNullable(inputAddress),
                    remotePoint,
                    ignored,
                    true);
        } else if (Modbus.DOUBLE_TYPE.equals(valueType)) {
            success = remoteProxy
                .setUpDoubleRegister(
                    Optional.ofNullable(registerAddress),
                    remotePoint,
                    ignored,
                    false);
            success &= remoteProxy
                .setUpDoubleRegister(
                    Optional.ofNullable(inputAddress),
                    remotePoint,
                    ignored,
                    true);
        } else if (Modbus.ARRAY_TYPE.equals(valueType)) {
            if (!arraySize.isPresent()) {
                getThisLogger()
                    .warn(
                        PAPMessages.MISSING_ATTRIBUTE,
                        Modbus.ARRAY_SIZE_ATTRIBUTE,
                        remotePoint);

                return false;
            }

            final int size = arraySize.get().intValue();

            if (size <= 0) {
                getThisLogger()
                    .warn(
                        PAPMessages.BAD_ATTRIBUTE_VALUE,
                        Modbus.ARRAY_SIZE_ATTRIBUTE,
                        remotePoint,
                        String.valueOf(size));

                return false;
            }

            success = remoteProxy
                .setUpDiscreteArrayRegister(
                    Optional.ofNullable(discreteAddress),
                    size,
                    remotePoint,
                    ignored,
                    true);
            success |= remoteProxy
                .setUpDiscreteArrayRegister(
                    Optional.ofNullable(coilAddress),
                    size,
                    remotePoint,
                    ignored,
                    false);
            success |= remoteProxy
                .setUpWordArrayRegister(
                    Optional.ofNullable(inputAddress),
                    size,
                    remotePoint,
                    ignored,
                    true);
            success |= remoteProxy
                .setUpWordArrayRegister(
                    Optional.ofNullable(registerAddress),
                    size,
                    remotePoint,
                    ignored,
                    false);
        } else if (Modbus.MASK_TYPE.equals(valueType)) {
            success = remoteProxy
                .setUpMaskedRegister(
                    Optional.ofNullable(registerAddress),
                    remotePoint,
                    ignored,
                    mask.get().intValue());
        } else if (Modbus.STAMP_TYPE.equals(valueType)) {
            success = remoteProxy
                .setUpStampRegister(
                    Optional.ofNullable(registerAddress),
                    false);
            success &= remoteProxy
                .setUpStampRegister(Optional.ofNullable(inputAddress), true);
        } else if (Modbus.TIME_TYPE.equals(valueType)) {
            success = remoteProxy
                .setUpTimeRegister(Optional.ofNullable(registerAddress), false);
            success &= remoteProxy
                .setUpTimeRegister(Optional.ofNullable(inputAddress), true);
        } else if (Modbus.SEQUENCE_TYPE.equals(valueType)) {
            success = remoteProxy
                .setUpSequenceRegister(
                    Optional.ofNullable(registerAddress),
                    false);
            success &= remoteProxy
                .setUpSequenceRegister(Optional.ofNullable(inputAddress), true);
        } else {
            getThisLogger()
                .warn(
                    PAPMessages.BAD_ATTRIBUTE_VALUE,
                    Modbus.TYPE_ATTRIBUTE,
                    remotePoint,
                    valueType);

            return false;
        }

        if (success) {
            registerRemotePoint(remotePoint);
        }

        return success;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getProtocolName()
    {
        return Modbus.PROTOCOL_NAME;
    }

    /** {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends ModbusProxy> getRemoteProxies()
    {
        return (Collection<? extends ModbusProxy>) super.getRemoteProxies();
    }

    /**
     * Gets the remote proxy for the specified Inet address.
     *
     * @param inetAddress The address.
     *
     * @return The remote proxy (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ModbusProxy> getRemoteProxyByInetAddress(
            @Nonnull final InetAddress inetAddress)
    {
        final ModbusProxy remoteProxy;

        if (_remoteProxyByInetAddress.isEmpty()) {
            final Collection<? extends ModbusProxy> proxies =
                getRemoteProxies();

            if (proxies.isEmpty()) {
                remoteProxy = null;
            } else {
                remoteProxy = proxies.iterator().next();
            }
        } else {
            remoteProxy = _remoteProxyByInetAddress.get(inetAddress);
        }

        return Optional.ofNullable(remoteProxy);
    }

    /**
     * Gets the remote proxy for the specified serial port name.
     *
     * @param serialPortName The serial port name.
     *
     * @return The remote proxy (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ModbusProxy> getRemoteProxyBySerialPortName(
            @Nonnull final String serialPortName)
    {
        return Optional
            .ofNullable(
                _remoteProxyBySerialPortName
                    .get(serialPortName.trim().toUpperCase(Locale.ROOT)));
    }

    /**
     * Gets a register attribute.
     *
     * @param attributes The entity attributes.
     * @param key The attribute key.
     * @param entity The entity.
     *
     * @return The attribute (empty if absent).
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<Integer> getRegisterAttribute(
            @Nonnull final Attributes attributes,
            @Nonnull final String key,
            @Nonnull final Entity entity)
    {
        final Optional<Integer> attribute = attributes
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
            }
        }

        return attribute;
    }

    /** {@inheritDoc}
     */
    @Override
    protected abstract ModbusProxy newRemoteProxy(Origin remoteOrigin);

    private boolean _addRemoteProxyAddresses(
            final ModbusProxy remoteProxy,
            final boolean isFirstProxy,
            final Attributes originAttributes)
    {
        final String[] socketAddressStrings = originAttributes
            .getStrings(Modbus.SOCKET_ADDRESS_ATTRIBUTE);
        final List<InetSocketAddress> socketAddresses = remoteProxy
            .getSocketAddresses();

        for (final String remoteProxyAddress: socketAddressStrings) {
            if (remoteProxy.supportsWildcardAddress()
                    && "*".equals(remoteProxyAddress.trim())) {
                if (!isFirstProxy || (socketAddressStrings.length > 1)) {
                    getThisLogger()
                        .warn(PAPMessages.WILDCARD_ADDRESS_RESTRICTS);

                    return false;
                }

                return true;
            }

            final Optional<InetSocketAddress> socketAddress = Inet
                .socketAddress(remoteProxyAddress);

            if (!socketAddress.isPresent()) {
                getThisLogger()
                    .warn(BaseMessages.BAD_ADDRESS, remoteProxyAddress);

                return false;
            }

            int port = socketAddress.get().getPort();

            if (port <= 0) {
                port = originAttributes
                    .getInt(
                        Modbus.SOCKET_PORT_ATTRIBUTE,
                        getDefaultPortForRemoteOrigin());
            }

            try {
                for (final InetAddress address:
                        InetAddress
                            .getAllByName(
                                    socketAddress.get().getHostString())) {
                    remoteProxy
                        .addSocketAddress(new InetSocketAddress(address, port));
                }
            } catch (final UnknownHostException exception) {
                getThisLogger()
                    .warn(
                        PAPMessages.UNKNOWN_ORIGIN_ADDRESS,
                        remoteProxy.getOrigin(),
                        remoteProxyAddress);

                return false;
            }
        }

        for (final InetSocketAddress socketAddress: socketAddresses) {
            final ModbusProxy otherProxy = _remoteProxyByInetAddress
                .get(socketAddress.getAddress());

            if (otherProxy != null) {
                getThisLogger()
                    .warn(
                        PAPMessages.AMBIGUOUS_ORIGIN_ADDRESS,
                        remoteProxy.getOrigin(),
                        socketAddress.getAddress(),
                        otherProxy.getOrigin());

                return false;
            }

            _remoteProxyByInetAddress
                .put(socketAddress.getAddress(), remoteProxy);
        }

        return true;
    }

    private final Map<InetAddress, ModbusProxy> _remoteProxyByInetAddress =
        new HashMap<>();
    private final Map<String, ModbusProxy> _remoteProxyBySerialPortName =
        new HashMap<>();
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
