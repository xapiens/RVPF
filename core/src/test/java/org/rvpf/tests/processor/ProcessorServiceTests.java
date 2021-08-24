/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ProcessorServiceTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.processor;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.store.Store;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.document.version.VersionControl;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.processor.ProcessorServiceActivator;
import org.rvpf.service.ServiceActivator;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Processor service tests.
 */
public final class ProcessorServiceTests
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
        setUpAlerter();
        _storeService = startStoreService(true);

        _noticeReceiver = getMessaging()
            .createClientReceiver(
                getMetadata(_storeService)
                    .getPropertiesGroup(NOTIFIER_QUEUE_PROPERTIES));
        _noticeReceiver.purge();
        _noticeSender = getMessaging()
            .createServerSender(
                getMetadata(_storeService)
                    .getPropertiesGroup(_RECEPTIONIST_QUEUE_PROPERTIES));

        _processorService = startService(
            ProcessorServiceActivator.class,
            Optional.empty());
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
        if (_processorService != null) {
            stopService(_processorService);
            _processorService = null;
        }

        _noticeSender.close();
        _noticeSender = null;

        if (_store != null) {
            _store.close();
            _store = null;
        }

        _noticeReceiver.commit();
        _noticeReceiver.purge();
        _noticeReceiver.close();
        _noticeReceiver = null;

        stopService(_storeService);
        _storeService = null;

        tearDownAlerter();
    }

    /**
     * Tests the control support.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testService")
    public void testControl()
        throws Exception
    {
        final PointValue pointValue;

        pointValue = _generateValue(
            _UPDATE_DOCUMENT_POINT_NAME,
            Integer.valueOf(1234));

        expectSignals(VersionControl.UPDATE_DOCUMENT_SIGNAL);
        Require.success(pointValue.updateStore());

        _verifyNotice(_UPDATE_DOCUMENT_POINT_NAME, pointValue.getValue(), true);
        _noticeSender.send(PointValue.NULL);
        _noticeSender.commit();

        final Signal updateDocument = waitForSignal(
            VersionControl.UPDATE_DOCUMENT_SIGNAL);

        Require
            .equal(
                Integer.valueOf(updateDocument.getInfo().get()),
                pointValue.getValue());
    }

    /**
     * Tests the script support.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testService")
    public void testScriptSupport()
        throws Exception
    {
        final PointValue pointValue;

        pointValue = _generateValue(
            _NUMERIC_POINT_1_NAME,
            Double.valueOf(22.0));

        pointValue.setState("Test state");
        Require.success(pointValue.updateStore());

        _verifyNotice(_NUMERIC_POINT_1_NAME, Double.valueOf(22.0), true);
        _noticeSender.send(PointValue.NULL);
        _noticeSender.commit();

        PointValue notice;

        notice = _verifyNotice(
            _NUMERIC_POINT_2_NAME,
            Double.valueOf(20.0),
            false);
        Require.equal(notice.getState(), pointValue.getState());

        notice = _verifyNotice(
            _NUMERIC_POINT_5_NAME,
            Double.valueOf(20.0),
            false);
        Require.equal(notice.getState(), pointValue.getState());
    }

    /**
     * Tests the service.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testService()
        throws Exception
    {
        PointValue pointValue;

        _startTime = DateTime.now();

        pointValue = _generateValue(_NUMERIC_POINT_3_NAME, Double.valueOf(2.0));
        _noticeSender.send(pointValue);
        _noticeSender.send(PointValue.NULL);
        _noticeSender.commit();

        _verifyNotice(_NUMERIC_POINT_3_NAME, Double.valueOf(2.0), true);
        _noticeSender.send(PointValue.NULL);
        _noticeSender.commit();

        pointValue = new PointValue(pointValue);
        pointValue.setValue(Double.valueOf(3.0));
        _noticeSender.send(pointValue);
        _noticeSender.send(PointValue.NULL);
        _noticeSender.commit();

        _verifyNotice(_NUMERIC_POINT_3_NAME, Double.valueOf(3.0), false);

        pointValue = new PointValue(pointValue);
        pointValue.setValue(null);
        _noticeSender.send(pointValue);
        _noticeSender.send(PointValue.NULL);
        _noticeSender.commit();

        final PointValue notice = _verifyNotice(
            _NUMERIC_POINT_3_NAME,
            null,
            true);

        Require.success(notice.isDeleted());
        _noticeSender.send(PointValue.NULL);
        _noticeSender.commit();

        _noticeSender.send(pointValue);
        _noticeSender.send(PointValue.NULL);
        _noticeSender.commit();

        pointValue = new PointValue(pointValue);
        pointValue.setValue(Double.valueOf(4.0));
        _noticeSender.send(pointValue);
        _noticeSender.send(PointValue.NULL);
        _noticeSender.commit();

        _verifyNotice(_NUMERIC_POINT_3_NAME, Double.valueOf(4.0), false);
    }

    private PointValue _generateValue(
            final String pointName,
            final Serializable value)
        throws Exception
    {
        final PointEntity point = (PointEntity) getMetadata(_processorService)
            .getPointByName(pointName)
            .get();
        final PointEntity pointClone = point.copy();
        final PointValue pointValue = new PointValue(
            pointClone,
            Optional.of(DateTime.now()),
            null,
            value);

        pointClone
            .setStoreEntity(
                Optional.of(pointClone.getStoreEntity().get().copy()));

        if (_store == null) {
            Require
                .success(
                    pointClone.setUpStore(getMetadata(_processorService)),
                    "Connected to store");
            _store = pointClone.getStore().get();
        } else {
            pointClone.getStoreEntity().get().setInstance((Proxied) _store);
        }

        return pointValue;
    }

    private PointValue _verifyNotice(
            final String pointName,
            final Serializable value,
            final boolean forward)
        throws Exception
    {
        final Point point = getMetadata(_processorService)
            .getPointByName(pointName)
            .get();
        final PointValue notice;

        notice = (PointValue) _noticeReceiver.receive(getTimeout());
        Require.notNull(notice, "Received notice");
        Require.equal(notice.getPointUUID(), point.getUUID().get());
        Require.equal(notice.getValue(), value);
        Require
            .success(
                notice.getStamp().isNotBefore(_startTime)
                && notice.getStamp().isNotAfter(DateTime.now()),
                "Notice stamp since start time");

        if (forward) {
            _noticeSender.send(notice);
        }

        return notice;
    }

    private static final String _NUMERIC_POINT_1_NAME = "TESTS.NUMERIC.01";
    private static final String _NUMERIC_POINT_2_NAME = "TESTS.NUMERIC.02";
    private static final String _NUMERIC_POINT_3_NAME = "TESTS.NUMERIC.03";
    private static final String _NUMERIC_POINT_5_NAME = "TESTS.NUMERIC.05";
    private static final String _RECEPTIONIST_QUEUE_PROPERTIES =
        "tests.processor.receptionist.queue";
    private static final String _UPDATE_DOCUMENT_POINT_NAME =
        "TESTS.UPDATE.DOCUMENT";

    private MessagingSupport.Receiver _noticeReceiver;
    private MessagingSupport.Sender _noticeSender;
    private ServiceActivator _processorService;
    private DateTime _startTime;
    private Store _store;
    private ServiceActivator _storeService;
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
