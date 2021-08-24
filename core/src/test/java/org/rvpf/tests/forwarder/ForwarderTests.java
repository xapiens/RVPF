/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ForwarderTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.forwarder;

import java.io.File;
import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.security.Crypt;
import org.rvpf.base.store.Store;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.base.xml.streamer.StreamedMessagesClient;
import org.rvpf.base.xml.streamer.StreamedMessagesWriter;
import org.rvpf.base.xml.streamer.XMLPointValuesPortClient;
import org.rvpf.config.Config;
import org.rvpf.forwarder.ForwarderServiceActivator;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceActivator;
import org.rvpf.service.Stopper;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.tests.MessagingSupport;
import org.rvpf.tests.store.StoreClientTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Forwarder tests.
 */
public final class ForwarderTests
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
        Require.ignored(_TESTS_TMP_DIR.mkdirs());

        setProperty(NULL_NOTIFIER_PROPERTY, "!");
        _portAddress = Inet.LOCAL_HOST + ":" + allocateTCPPort();
        setProperty(_XML_PORT_ADDRESS_PROPERTY, _portAddress);

        setUpAlerter();
        _storeService = startStoreService(true);

        _sender = getMessaging()
            .createServerSender(
                getConfig().getPropertiesGroup(_RECEPTIONIST_QUEUE_PROPERTIES));
        _jmxRegistrationEnabled = getConfig()
            .getBooleanValue(_JMX_REGISTRATION_ENABLED_PROPERTY);
        _forwarderService = startService(
            ForwarderServiceActivator.class,
            Optional.empty());

        if (_jmxRegistrationEnabled) {
            Require.success(_forwarderService.getService().exportAgent());
        }

        _inputPoint = getMetadata().getPointByName(_INPUT_POINT_NAME).get();
        _confirmedPoint = getMetadata()
            .getPointByName(_CONFIRMED_POINT_NAME)
            .get();
        _resultPoint = getMetadata().getPointByName(_RESULT_POINT_NAME).get();

        Require.notNull(_inputPoint);
        Require.notNull(_confirmedPoint);
        Require.notNull(_resultPoint);

        _crypt = new Crypt();
        Require
            .success(
                _crypt
                    .setUp(
                            getSecurityContext().getCryptProperties(),
                                    Optional.empty()));
    }

    /**
     * Sets up clients.
     *
     * @throws Exception On failure.
     */
    @BeforeMethod
    public void setUpClients()
        throws Exception
    {
        _receiver = getMessaging().createClientReceiver(_FORWARDER_QUEUE_NAME);
        _receiver.purge();

        _noticeReceiver = getMessaging()
            .createClientReceiver(
                getConfig().getPropertiesGroup(_NOTIFIER_QUEUE_PROPERTIES));
        _noticeReceiver.purge();

        _subscriber = getMessaging()
            .createClientSubscriber(
                getConfig().getPropertiesGroup(_PUBLISHER_TOPIC_PROPERTIES));

        _portClient = new XMLPointValuesPortClient("TESTS");
        _portClient.open(_portAddress, Optional.of(getSecurityContext()));
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
        _portClient.close();

        if (_jmxRegistrationEnabled) {
            expectEvents(Service.STOPPED_EVENT);
            Require
                .success(new Stopper().stop(new String[] {_FORWARDER_ALIAS}));
            waitForEvent(Service.STOPPED_EVENT);
        } else if (_forwarderService != null) {
            stopService(_forwarderService);
        }

        _forwarderService = null;

        _sender.close();

        if (_storeService != null) {
            stopService(_storeService);
            _storeService = null;
        }

        tearDownAlerter();
    }

    /**
     * Tears down clients.
     *
     * @throws Exception On failure.
     */
    @AfterMethod(alwaysRun = true)
    public void tearDownClients()
        throws Exception
    {
        _receiver.commit();
        _receiver.purge();
        _receiver.close();
        _receiver = null;

        _noticeReceiver.commit();
        _noticeReceiver.purge();
        _noticeReceiver.close();
        _noticeReceiver = null;

        _subscriber.close();
        _subscriber = null;
    }

    /**
     * Tests port client.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = "testXML")
    public void testPortClient()
        throws Exception
    {
        final long timeout = getTimeout();
        PointValue pointValue;

        _portClient
            .login(
                getMetadata().getStringValue(USER_PROPERTY),
                getMetadata().getPasswordValue(PASSWORD_PROPERTY));

        _portClient
            .sendValue(
                _INPUT_POINT_NAME,
                DateTime.now().toString(),
                Optional.empty(),
                Optional.of("5.0"));
        _portClient
            .sendValue(
                _RESULT_POINT_NAME,
                _resultStamp.toString(),
                Optional.of(XMLPointValuesPortClient.DELETED_STATE),
                Optional.empty());
        _portClient
            .sendValue(
                _RESULT_POINT_NAME,
                DateTime.now().toString(),
                Optional.empty(),
                Optional.of("6.0"));

        _portClient.flush();

        pointValue = (PointValue) _verifyDecrypt(_receiver.receive(timeout));
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(5.0));
        pointValue = (PointValue) _verifyDecrypt(_receiver.receive(timeout));
        Require.equal(pointValue.getPointUUID(), _resultPoint.getUUID().get());
        Require.success(pointValue.isDeleted());
        pointValue = (PointValue) _verifyDecrypt(_receiver.receive(timeout));
        Require.equal(pointValue.getPointUUID(), _resultPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(6.0));

        pointValue = (PointValue) _noticeReceiver.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(5.0));

        pointValue = (PointValue) _subscriber.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(5.0));
        pointValue = (PointValue) _subscriber.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _resultPoint.getUUID().get());
        Require.success(pointValue.isDeleted());
        pointValue = (PointValue) _subscriber.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _resultPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(6.0));

        _portClient
            .sendValue(
                _RESULT_POINT_NAME,
                DateTime.now().toString(),
                Optional.empty(),
                Optional.of("7.0"));

        _portClient.flush();

        pointValue = (PointValue) _verifyDecrypt(_receiver.receive(timeout));
        Require.equal(pointValue.getPointUUID(), _resultPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(7.0));

        pointValue = (PointValue) _subscriber.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _resultPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(7.0));
    }

    /**
     * Tests sender.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testSender()
        throws Exception
    {
        final long timeout = getTimeout();
        PointValue pointValue;

        _sender
            .send(
                new PointValue(
                    _resultPoint,
                    Optional.of(DateTime.now()),
                    null,
                    Double.valueOf(3.0)));
        _sender
            .send(
                new PointValue(
                    _inputPoint,
                    Optional.of(DateTime.now()),
                    null,
                    Double.valueOf(4.0)));
        _sender.commit();
        _sender
            .send(
                new PointValue(
                    _inputPoint,
                    Optional.of(DateTime.now()),
                    null,
                    Double.valueOf(5.0)));
        _sender.commit();

        pointValue = (PointValue) _receiver.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _resultPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(3.0));
        pointValue = (PointValue) _receiver.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(4.0));
        pointValue = (PointValue) _receiver.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(5.0));

        pointValue = (PointValue) _noticeReceiver.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(4.0));
        pointValue = (PointValue) _noticeReceiver.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(5.0));

        pointValue = (PointValue) _subscriber.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _resultPoint.getUUID().get());
        pointValue = (PointValue) _subscriber.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
    }

    /**
     * Tests store.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testStore()
        throws Exception
    {
        final long timeout = getTimeout();
        PointValue pointValue;

        final Store store = getStore(
            getMetadata()
                .getStringValue(StoreServiceAppImpl.STORE_NAME_PROPERTY)
                .get(),
            getMetadata())
            .get();
        final DateTime startStamp = VersionedValue.newVersion();
        final long milliRaw = ElapsedTime.MILLI.toRaw();

        store
            .addUpdate(
                new PointValue(
                    _confirmedPoint,
                    Optional.of(startStamp.before(300 * milliRaw)),
                    null,
                    Double.valueOf(3.0)));
        store
            .addUpdate(
                new PointValue(
                    _confirmedPoint,
                    Optional.of(startStamp.before(200 * milliRaw)),
                    null,
                    Double.valueOf(2.0)));
        store
            .addUpdate(
                new PointValue(
                    _confirmedPoint,
                    Optional.of(startStamp.before(100 * milliRaw)),
                    null,
                    Double.valueOf(1.0)));
        Require.success(store.sendUpdates());

        pointValue = (PointValue) _receiver.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(3.0));
        pointValue = (PointValue) _receiver.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(2.0));
        pointValue = (PointValue) _receiver.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(1.0));

        pointValue = (PointValue) _noticeReceiver.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(3.0));
        pointValue = (PointValue) _noticeReceiver.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(2.0));
        pointValue = (PointValue) _noticeReceiver.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(1.0));

        pointValue = (PointValue) _subscriber.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(3.0));
        pointValue = (PointValue) _subscriber.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(2.0));
        pointValue = (PointValue) _subscriber.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(1.0));
    }

    /**
     * Tests XML.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testXML()
        throws Exception
    {
        _writeXML();

        final long timeout = getTimeout();
        PointValue pointValue;

        pointValue = (PointValue) _receiver.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(1.0));
        pointValue = (PointValue) _receiver.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        Require.equal(pointValue.getValue(), "0.0");
        pointValue = (PointValue) _receiver.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _resultPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(2.0));
        _resultStamp = pointValue.getStamp();

        pointValue = (PointValue) _noticeReceiver.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
        Require.equal(pointValue.getValue(), Double.valueOf(1.0));

        pointValue = (PointValue) _subscriber.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _inputPoint.getUUID().get());
        pointValue = (PointValue) _subscriber.receive(timeout);
        Require
            .equal(pointValue.getPointUUID(), _confirmedPoint.getUUID().get());
        pointValue = (PointValue) _subscriber.receive(timeout);
        Require.equal(pointValue.getPointUUID(), _resultPoint.getUUID().get());
    }

    private Serializable _verifyDecrypt(Serializable message)
    {
        Require.notNull(message);

        if (Crypt.isSigned(message)) {
            final Crypt.Result cryptResult = _crypt
                .verify(message, new String[0]);

            Require.success(cryptResult.isSuccess());
            Require.success(cryptResult.isVerified());
            message = cryptResult.getSerializable();
            Require.notNull(message);
        }

        final Crypt.Result cryptResult = _crypt.decrypt(message, new String[0]);

        Require.success(cryptResult.isSuccess());
        Require.failure(cryptResult.isVerified());

        message = cryptResult.getSerializable();
        Require.notNull(message);

        return message;
    }

    private void _writeXML()
        throws Exception
    {
        final File xmlFile = new File(
            _TESTS_TMP_DIR,
            "tests" + StreamedMessagesClient.DEFAULT_DATA_SUFFIX);

        if (xmlFile.exists()) {
            Require.success(xmlFile.delete());
        }

        final File tmpFile = new File(
            _TESTS_TMP_DIR,
            "tests" + StreamedMessagesClient.DEFAULT_TRANS_SUFFIX);
        final StreamedMessagesWriter messagesWriter =
            new StreamedMessagesWriter();

        Require
            .success(
                messagesWriter.setUp(Optional.empty(), Optional.empty()));
        messagesWriter.open(tmpFile, Optional.empty());

        final PointValue inputPointValue = new PointValue(
            _INPUT_POINT_NAME,
            Optional.of(DateTime.now()),
            null,
            "1.0");

        Require.success(messagesWriter.add(inputPointValue));

        final PointValue confirmedPointValue = new PointValue(
            _CONFIRMED_POINT_NAME,
            Optional.of(DateTime.now()),
            null,
            "0.0");

        Require.success(messagesWriter.add(confirmedPointValue));

        final PointValue resultPointValue = new PointValue(
            _RESULT_POINT_NAME,
            Optional.of(DateTime.now()),
            null,
            "2.0");

        Require.success(messagesWriter.add(resultPointValue));

        messagesWriter.close();

        Require.success(tmpFile.renameTo(xmlFile));

        _forwarderService.getService().wakeUp();
    }

    private static final String _CONFIRMED_POINT_NAME = "TESTS.NUMERIC.03";
    private static final String _FORWARDER_ALIAS = "forwarder";
    private static final String _FORWARDER_QUEUE_NAME = "TestsForwarder";
    private static final String _INPUT_POINT_NAME = "TESTS.NUMERIC.01";
    private static final String _JMX_REGISTRATION_ENABLED_PROPERTY =
        "jmx.registration.enabled";
    private static final String _NOTIFIER_QUEUE_PROPERTIES =
        "tests.store.notifier.queue";
    private static final String _PUBLISHER_TOPIC_PROPERTIES =
        "tests.publisher.topic";
    private static final String _RECEPTIONIST_QUEUE_PROPERTIES =
        "tests.processor.receptionist.queue";
    private static final String _RESULT_POINT_NAME = "TESTS.NUMERIC.02";
    private static final String _TESTS_PROPERTIES = "rvpf-forwarder.properties";
    private static final File _TESTS_TMP_DIR = new File("tests/data/tmp");
    private static final String _XML_PORT_ADDRESS_PROPERTY =
        "tests.xml.port.address";

    private Point _confirmedPoint;
    private Crypt _crypt;
    private ServiceActivator _forwarderService;
    private Point _inputPoint;
    private boolean _jmxRegistrationEnabled;
    private MessagingSupport.Receiver _noticeReceiver;
    private String _portAddress;
    private XMLPointValuesPortClient _portClient;
    private MessagingSupport.Receiver _receiver;
    private Point _resultPoint;
    private DateTime _resultStamp;
    private MessagingSupport.Sender _sender;
    private ServiceActivator _storeService;
    private MessagingSupport.Subscriber _subscriber;
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
