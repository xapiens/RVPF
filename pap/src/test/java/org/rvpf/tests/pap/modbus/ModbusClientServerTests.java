/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusClientServerTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.pap.modbus;

import java.io.Serializable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.PAPProxy;
import org.rvpf.pap.modbus.ModbusClient;
import org.rvpf.pap.modbus.ModbusMessages;
import org.rvpf.pap.modbus.ModbusServer;
import org.rvpf.tests.service.MetadataServiceTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Modbus client-server tests.
 */
public final class ModbusClientServerTests
    extends MetadataServiceTests
    implements PAPProxy.Responder, PAPConnectionListener
{
    /** {@inheritDoc}
     */
    @Override
    public boolean onLostConnection(
            final PAPProxy remoteProxy,
            final Optional<Exception> cause)
    {
        Require.success(_disconnected.getCount() > 0);

        _disconnected.countDown();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onNewConnection(final PAPProxy remoteProxy)
    {
        Require.failure(_connected);

        _connected = true;

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] select(final Point[] points)
    {
        final PointValue[] pointValues = new PointValue[points.length];

        for (int i = 0; i < pointValues.length; ++i) {
            pointValues[i] = _pointValues.get(points[i].getUUID().get());
        }

        return pointValues;
    }

    /**
     * Sets up this.
     */
    @BeforeClass
    public void setUp()
    {
        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);

        setProperty(
            ModbusTestsSupport.LISTEN_PORT_PROPERTY,
            String.valueOf(allocateTCPPort()));

        _support = new ModbusTestsSupport(getMetadata(true));
    }

    /**
     * Tests.
     *
     * @throws Exception On failure.
     */
    @Test
    public void test()
        throws Exception
    {
        final ModbusServer server = _support.getServer();
        final ModbusClient client = _support.getClient();

        server.setResponder(this);
        server.start();

        client.open();
        Require.success(client.addConnectionListener(this));
        expectLogs(ModbusMessages.CLIENT_CONNECTION_ACCEPTED);
        Require.failure(_connected);
        Require.success(client.connect(_support.getServerOrigin()));
        Require.success(_connected);
        waitForLogs(ModbusMessages.CLIENT_CONNECTION_ACCEPTED);

        new _Tests(_support, this).test();

        Require.success(_disconnected.getCount() > 0);
        server.stop();
        Require
            .success(
                _disconnected.await(getTimeout(), TimeUnit.MILLISECONDS));

        client.disconnect();
        Require.success(client.removeConnectionListener(this));
        client.close();
    }

    /**
     * Puts a value in the point values map.
     *
     * @param pointValue The point value.
     */
    void _putValue(@Nonnull final PointValue pointValue)
    {
        _pointValues.put(pointValue.getPointUUID(), pointValue);
    }

    private static final String _TESTS_PROPERTIES = "rvpf-modbus.properties";

    private boolean _connected;
    private final CountDownLatch _disconnected = new CountDownLatch(1);
    private final Map<UUID, PointValue> _pointValues =
        new ConcurrentHashMap<>();
    private ModbusTestsSupport _support;

    /**
     * Tests.
     */
    private static final class _Tests
        extends ModbusTests
    {
        /**
         * Constructs an instance.
         *
         * @param suppport The Modbus tests support.
         * @param owner The owner.
         */
        _Tests(
                @Nonnull final ModbusTestsSupport suppport,
                @Nonnull final ModbusClientServerTests owner)
        {
            super(suppport, owner);
        }

        /** {@inheritDoc}
         */
        @Override
        protected Optional<PointValue> getNextPointValue()
        {
            final ModbusServer server = getSupport().getServer();

            try {
                return server.nextUpdate(getTimeout());
            } catch (final InterruptedException exception) {
                throw new RuntimeException(exception);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        protected void putValue(
                final Point point,
                final DateTime stamp,
                final Serializable value)
        {
            ((ModbusClientServerTests) getOwner())
                ._putValue(
                    new PointValue(point, Optional.of(stamp), null, value));
        }
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
