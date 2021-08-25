/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusTestsSupport.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap.modbus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Origin;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.PAP;
import org.rvpf.pap.modbus.ModbusClient;
import org.rvpf.pap.modbus.ModbusClientContext;
import org.rvpf.pap.modbus.ModbusClientProxy;
import org.rvpf.pap.modbus.ModbusProxy;
import org.rvpf.pap.modbus.ModbusServer;
import org.rvpf.pap.modbus.ModbusServerContext;
import org.rvpf.pap.modbus.ModbusSupport;
import org.rvpf.pap.modbus.register.Register;
import org.rvpf.tests.pap.PAPTestsSupport;

/**
 * Modbus tests support.
 */
public final class ModbusTestsSupport
    extends PAPTestsSupport
{
    /**
     * Constructs an instance.
     *
     * @param metadata The metadata.
     */
    public ModbusTestsSupport(@Nonnull final Metadata metadata)
    {
        super(Optional.of(metadata));

        final ModbusSupport support = new ModbusSupport();
        final KeyedGroups socketProperties = metadata
            .getPropertiesGroup(_SOCKET_LISTENER_PROPERTIES);
        final KeyedGroups serialProperties = metadata
            .getPropertiesGroup(_SERIAL_LISTENER_PROPERTIES);
        final ModbusServerContext serverContext = support
            .newServerContext(
                metadata,
                new String[] {_MODBUS_ORIGIN_NAME},
                Optional.empty());
        KeyedGroups serverProperties;

        Require.notNull(serverContext);
        _server = support.newServer(serverContext);

        if (!socketProperties.isEmpty()) {
            serverProperties = new KeyedGroups();
            serverProperties
                .addGroup(PAP.LISTENER_PROPERTIES, socketProperties);
            Require.success(_server.setUp(serverProperties));
        }

        if (!serialProperties.isEmpty()) {
            serverProperties = new KeyedGroups();
            serverProperties
                .addGroup(PAP.LISTENER_PROPERTIES, serialProperties);
            Require.success(_server.setUp(serverProperties));
        }

        final ModbusClientContext clientContext = support
            .newClientContext(metadata, Optional.empty());

        Require.notNull(clientContext);
        _client = support.newClient(clientContext);
        Require.notNull(_client);

        _serverOrigin = _client
            .getOrigin(Optional.of(_MODBUS_ORIGIN_NAME))
            .get();

        _remoteProxy = _server.getClientProxy(_serverOrigin).get();

        for (final Register register: _remoteProxy.getCoils()) {
            for (final Point point: register.getPoints()) {
                _coilsByPoint.put(point, register);
            }
        }

        for (final Register register: _remoteProxy.getDiscretes()) {
            for (final Point point: register.getPoints()) {
                _discretesByPoint.put(point, register);
            }
        }

        for (final Register register: _remoteProxy.getInputs()) {
            for (final Point point: register.getPoints()) {
                _inputsByPoint.put(point, register);
            }
        }

        for (final Register register: _remoteProxy.getRegisters()) {
            for (final Point point: register.getPoints()) {
                _registersByPoint.put(point, register);
            }
        }
    }

    /**
     * Gets the client.
     *
     * @return The client.
     */
    @Nonnull
    @CheckReturnValue
    public ModbusClient getClient()
    {
        return _client;
    }

    /**
     * Gets the coils by point.
     *
     * @return The coils by point.
     */
    @Nonnull
    @CheckReturnValue
    public Map<Point, Register> getCoilsByPoint()
    {
        return _coilsByPoint;
    }

    /**
     * Gets the discretes by point.
     *
     * @return The discretes by point.
     */
    @Nonnull
    @CheckReturnValue
    public Map<Point, Register> getDiscretesByPoint()
    {
        return _discretesByPoint;
    }

    /**
     * Gets the inputs by point.
     *
     * @return The inputs by point.
     */
    @Nonnull
    @CheckReturnValue
    public Map<Point, Register> getInputsByPoint()
    {
        return _inputsByPoint;
    }

    /**
     * Gets a point, given its key.
     *
     * @param key The point's key.
     *
     * @return The point.
     */
    @Nonnull
    @CheckReturnValue
    public Point getPoint(@Nonnull final String key)
    {
        return getMetadata().get().getPoint(key).get();
    }

    /**
     * Gets the registers by point.
     *
     * @return The registers by point.
     */
    @Nonnull
    @CheckReturnValue
    public Map<Point, Register> getRegistersByPoint()
    {
        return _registersByPoint;
    }

    /**
     * Gets the remote proxy.
     *
     * @return The remote proxy.
     */
    @Nonnull
    @CheckReturnValue
    public ModbusProxy getRemoteProxy()
    {
        return _remoteProxy;
    }

    /**
     * Gets the server.
     *
     * @return The server.
     */
    @Nonnull
    @CheckReturnValue
    public ModbusServer getServer()
    {
        _serverLocal = true;

        return _server;
    }

    /**
     * Gets the origin.
     *
     * @return The origin.
     */
    @Nonnull
    @CheckReturnValue
    public Origin getServerOrigin()
    {
        return _serverOrigin;
    }

    /**
     * Asks if the server is local.
     *
     * @return True if the server is local.
     */
    @CheckReturnValue
    public boolean isServerLocal()
    {
        return _serverLocal;
    }

    /** Listen port property. */
    public static final String LISTEN_PORT_PROPERTY =
        "tests.modbus.listen.port";

    /**  */

    private static final String _MODBUS_ORIGIN_NAME = "TestsModbus";
    private static final String _SERIAL_LISTENER_PROPERTIES =
        "modbus.listener.serial";
    private static final String _SOCKET_LISTENER_PROPERTIES =
        "modbus.listener.socket";

    private final ModbusClient _client;
    private final Map<Point, Register> _coilsByPoint = new HashMap<Point,
        Register>();
    private final Map<Point, Register> _discretesByPoint = new HashMap<Point,
        Register>();
    private final Map<Point, Register> _inputsByPoint = new HashMap<Point,
        Register>();
    private final Map<Point, Register> _registersByPoint = new HashMap<Point,
        Register>();
    private final ModbusClientProxy _remoteProxy;
    private final ModbusServer _server;
    private volatile boolean _serverLocal;
    private final Origin _serverOrigin;
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
