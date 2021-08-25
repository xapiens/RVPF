/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UpdatesListenerTests.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.tests.store.server.pap.modbus;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.modbus.ModbusClient;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.the.TheStoreServiceActivator;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.pap.modbus.ModbusTests;
import org.rvpf.tests.pap.modbus.ModbusTestsSupport;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Updates listener tests.
 */
public final class UpdatesListenerTests
    extends StoreClientTests
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
        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);

        setProperty(
            ModbusTestsSupport.LISTEN_PORT_PROPERTY,
            String.valueOf(allocateTCPPort()));

        setUpAlerter();

        _support = new ModbusTestsSupport(getMetadata());

        _storeService = startService(
            TheStoreServiceActivator.class,
            Optional.of(_MODBUS_STORE_NAME));
        _receiver = getMessaging()
            .createClientReceiver(
                getConfig().getPropertiesGroup(_NOTIFIER_QUEUE_PROPERTIES));
        _receiver.purge();
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
        _receiver.commit();
        _receiver.purge();
        _receiver.close();
        _receiver = null;

        stopService(_storeService);
        _storeService = null;

        tearDownAlerter();
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
        final ModbusClient client = _support.getClient();
        final _Tests tests = new _Tests(_support, this);

        Require.success(client.connect(_support.getServerOrigin()));

        tests.test();

        client.disconnect(_support.getServerOrigin());
    }

    /** {@inheritDoc}
     */
    @Override
    protected int getTimeout()
    {
        return super.getTimeout();
    }

    /**
     * Gets the receiver.
     *
     * @return The receiver.
     */
    @Nonnull
    @CheckReturnValue
    MessagingSupport.Receiver getReceiver()
    {
        return Require.notNull(_receiver);
    }

    /**
     * Updates store values.
     *
     * @param pointValues The point values.
     *
     * @throws Exception On failure.
     */
    void updateStoreValues(
            @Nonnull final List<PointValue> pointValues)
        throws Exception
    {
        final Metadata metadata = getMetadata(_storeService);

        for (final ListIterator<PointValue> iterator =
                pointValues.listIterator();
                iterator.hasNext(); ) {
            final PointValue pointValue = iterator.next();
            final Optional<Point> point = metadata
                .getPointByUUID(pointValue.getPointUUID());

            iterator.set(pointValue.morph(point, Optional.empty()));
        }

        updateStoreValues(_storeService, pointValues);
    }

    private static final String _MODBUS_STORE_NAME = "Modbus";
    private static final String _NOTIFIER_QUEUE_PROPERTIES =
        "tests.store.notifier.queue";
    private static final String _TESTS_PROPERTIES = "rvpf-modbus.properties";

    private MessagingSupport.Receiver _receiver;
    private ServiceActivator _storeService;
    private ModbusTestsSupport _support;

    private static final class _Tests
        extends ModbusTests
    {
        /**
         * Constructs an instance.
         *
         * @param suppport The Modbus support.
         * @param owner The owner.
         */
        _Tests(
                @Nonnull final ModbusTestsSupport suppport,
                @Nonnull final UpdatesListenerTests owner)
        {
            super(suppport, owner);
        }

        /** {@inheritDoc}
         */
        @Override
        protected Optional<PointValue> getNextPointValue()
        {
            final UpdatesListenerTests owner =
                (UpdatesListenerTests) getOwner();

            try {
                return Optional
                    .ofNullable(
                        (PointValue) owner
                            .getReceiver()
                            .receive(owner.getTimeout()));
            } catch (final Exception exception) {
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
            throws Exception
        {
            final List<PointValue> pointValues = new LinkedList<PointValue>();

            pointValues
                .add(new PointValue(point, Optional.of(stamp), null, value));
            ((UpdatesListenerTests) getOwner()).updateStoreValues(pointValues);
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
