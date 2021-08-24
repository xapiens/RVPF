/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AlertForwarderTests.java 4061 2019-06-06 16:55:09Z SFB $
 */

package org.rvpf.tests.forwarder;

import java.util.Optional;

import org.rvpf.base.alert.Alert;
import org.rvpf.base.tool.Require;
import org.rvpf.config.Config;
import org.rvpf.forwarder.ForwarderServiceActivator;
import org.rvpf.service.ServiceActivator;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Alert forwarder tests.
 */
public final class AlertForwarderTests
    extends ServiceTests
{
    /**
     * Sets up properties.
     */
    @BeforeTest
    public static void setUpProperties()
    {
        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);
    }

    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUp()
        throws Exception
    {
        setUpAlerter();

        _forwarderService = startService(
            ForwarderServiceActivator.class,
            Optional.of(_FORWARDER_NAME));

        _alertSender = getMessaging()
            .createClientSender(_TO_ALERTER_QUEUE_NAME);
        _alertReceiver = getMessaging()
            .createClientReceiver(_FROM_ALERTER_QUEUE_NAME);
        _alertReceiver.purge();
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
        _alertSender.close();
        _alertSender = null;
        _alertReceiver.commit();
        _alertReceiver.close();
        _alertReceiver = null;

        if (_forwarderService != null) {
            stopService(_forwarderService);
            _forwarderService = null;
        }

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
        final long timeout = getTimeout();
        Alert alert;

        _forwarderService.getService().sendEvent("TestEvent", Optional.empty());

        expectAlerts(TestAlert.class);
        _forwarderService.getService().sendAlert(new TestAlert("Test1"));
        alert = (Alert) _alertReceiver.receive(timeout);
        Require.equal(alert.getName(), "Test1");
        alert = waitForAlert(TestAlert.class);
        Require.equal(alert.getName(), "Test1");

        expectAlerts(TestAlert.class);
        _alertSender.send(new TestAlert("Test2"));
        _alertSender.commit();
        alert = (Alert) _alertReceiver.receive(timeout);
        Require.equal(alert.getName(), "Test2");
        alert = waitForAlert(TestAlert.class);
        Require.equal(alert.getName(), "Test2");
    }

    private static final String _FORWARDER_NAME = "Alert";
    private static final String _FROM_ALERTER_QUEUE_NAME = "TestsFromAlerter";
    private static final String _TESTS_PROPERTIES = "rvpf-forwarder.properties";
    private static final String _TO_ALERTER_QUEUE_NAME = "TestsToAlerter";

    private MessagingSupport.Receiver _alertReceiver;
    private MessagingSupport.Sender _alertSender;
    private ServiceActivator _forwarderService;

    /**
     * Test alert.
     */
    public static final class TestAlert
        extends Alert
    {
        /**
         * Constructs an instance.
         */
        public TestAlert() {}

        public TestAlert(final String name)
        {
            super(
                name,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        }

        /** {@inheritDoc}
         */
        @Override
        protected String getTypeString()
        {
            return "Test alert";
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
