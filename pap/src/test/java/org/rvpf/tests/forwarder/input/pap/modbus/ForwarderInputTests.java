/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ForwarderInputTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.forwarder.input.pap.modbus;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.forwarder.ForwarderServiceActivator;
import org.rvpf.pap.modbus.ModbusClient;
import org.rvpf.service.ServiceActivator;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.pap.modbus.ModbusTests;
import org.rvpf.tests.pap.modbus.ModbusTestsSupport;
import org.rvpf.tests.service.MetadataServiceTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Forwarder input tests.
 */
public final class ForwarderInputTests
    extends MetadataServiceTests
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

        _forwarderService = startService(
            ForwarderServiceActivator.class,
            Optional.of(_MODBUS_FORWARDER_NAME));
        _receiver = getMessaging()
            .createClientReceiver(
                getConfig().getPropertiesGroup(_FORWARDER_QUEUE_PROPERTIES));
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

        stopService(_forwarderService);
        _forwarderService = null;

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
        final _Tests tests = new _Tests(
            _support,
            _receiver,
            getTimeout(),
            this);
        final ModbusClient client = _support.getClient();

        Require.success(client.connect(_support.getServerOrigin()));

        tests.test();

        client.disconnect(_support.getServerOrigin());
    }

    private static final String _FORWARDER_QUEUE_PROPERTIES =
        "tests.forwarder.queue";
    private static final String _MODBUS_FORWARDER_NAME = "Modbus";
    private static final String _TESTS_PROPERTIES = "rvpf-modbus.properties";

    private ServiceActivator _forwarderService;
    private MessagingSupport.Receiver _receiver;
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
         * @param suppport The Modbus support.
         * @param receiver The receiver.
         * @param timeout The timeout in milliseconds.
         * @param owner The owner.
         */
        _Tests(
                @Nonnull final ModbusTestsSupport suppport,
                @Nonnull final MessagingSupport.Receiver receiver,
                final int timeout,
                @Nonnull final ForwarderInputTests owner)
        {
            super(suppport, owner);

            _receiver = receiver;
            _timeout = timeout;
        }

        /** {@inheritDoc}
         */
        @Override
        protected Optional<PointValue> getNextPointValue()
        {
            try {
                return Optional
                    .ofNullable((PointValue) _receiver.receive(_timeout));
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        protected boolean isWriteOnly()
        {
            return true;
        }

        private final MessagingSupport.Receiver _receiver;
        private final int _timeout;
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
