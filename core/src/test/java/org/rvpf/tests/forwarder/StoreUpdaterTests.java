/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreUpdaterTests.java 4061 2019-06-06 16:55:09Z SFB $
 */

package org.rvpf.tests.forwarder;

import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.config.Config;
import org.rvpf.forwarder.ForwarderServiceActivator;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.proxy.ProxyStoreServiceActivator;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Store updater tests.
 */
public final class StoreUpdaterTests
    extends StoreClientTests
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

        _storeService = startStoreService(true);
        _proxyService = startService(
            ProxyStoreServiceActivator.class,
            Optional.empty());
        _forwarderService = startService(
            ForwarderServiceActivator.class,
            Optional.of(_FORWARDER_NAME));
        _updatesSender = getMessaging()
            .createClientSender(_STORE_UPDATER_QUEUE_NAME);
        _noticeReceiver = getMessaging()
            .createClientReceiver(_NOTIFIER_QUEUE_NAME);
        _noticeReceiver.purge();
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
        _updatesSender.close();
        _updatesSender = null;

        _noticeReceiver.commit();
        _noticeReceiver.purge();
        _noticeReceiver.close();
        _noticeReceiver = null;

        if (_forwarderService != null) {
            stopService(_forwarderService);
            _forwarderService = null;
        }

        if (_proxyService != null) {
            stopService(_proxyService);
            _proxyService = null;
        }

        if (_storeService != null) {
            stopService(_storeService);
            _storeService = null;
        }

        tearDownAlerter();
    }

    /**
     * Tests the update of a known point.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testUpdateKnownPoint()
        throws Exception
    {
        final DateTime stamp = DateTime.now();

        _updatesSender
            .send(
                new PointValue(
                    _UPDATE_POINT_NAME,
                    Optional.of(stamp),
                    null,
                    Double.valueOf(6.0)));
        _updatesSender.commit();

        final PointValue pointValue = (PointValue) _noticeReceiver
            .receive(getTimeout());

        Require.equal(pointValue.getStamp(), stamp);
        Require.equal(pointValue.getValue(), Double.valueOf(6.0));
    }

    /**
     * Tests the update of an unkown point.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testUpdateUnknownPoint()
        throws Exception
    {
        final DateTime stamp = DateTime.now();

        expectLogs(BaseMessages.POINT_UPDATE_FAILED);
        _updatesSender
            .send(
                new PointValue(
                    _UNKNOWN_POINT_UUID,
                    Optional.of(stamp),
                    null,
                    null));
        _updatesSender.commit();
        waitForLogs(BaseMessages.POINT_UPDATE_FAILED);
    }

    private static final String _FORWARDER_NAME = "StoreUpdater";
    private static final String _NOTIFIER_QUEUE_NAME = "TestsNotifier";
    private static final String _STORE_UPDATER_QUEUE_NAME = "TestsStoreUpdater";
    private static final String _TESTS_PROPERTIES = "rvpf-forwarder.properties";
    private static final UUID _UNKNOWN_POINT_UUID = UUID
        .fromString("53b6d106-4011-454e-a19a-f3b1f3cc1493")
        .get();
    private static final String _UPDATE_POINT_NAME = "TESTS.NUMERIC.01";

    private ServiceActivator _forwarderService;
    private MessagingSupport.Receiver _noticeReceiver;
    private ServiceActivator _proxyService;
    private ServiceActivator _storeService;
    private MessagingSupport.Sender _updatesSender;
}

/*
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License version 2.1 as published by
 * the Free Software Foundation. This software is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License along with this software; if
 * not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA
 */
