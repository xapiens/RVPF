/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StreamedMessagesTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.forwarder;

import java.io.File;
import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.DateTime;
import org.rvpf.base.alert.Event;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.xml.streamer.StreamedMessagesClient;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer;
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
 * Streamed messages tests.
 */
public final class StreamedMessagesTests
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
        Require.ignored(_TESTS_INPUT_DIR.mkdirs());

        final boolean created = new File(_TESTS_INPUT_DIR, _SEM_OLD_FILE_NAME)
            .createNewFile();

        Require.success(created);

        _forwarderService = startService(
            ForwarderServiceActivator.class,
            Optional.of(_FORWARDER_NAME));

        final KeyedGroups queueProperties = new KeyedGroups();
        final KeyedGroups securityProperties = getConfig()
            .getPropertiesGroup(_SECURITY_PROPERTIES)
            .copy();

        queueProperties.setValue(_NAME_PROPERTY, _QUEUE_NAME);
        securityProperties.setValue(SecurityContext.SECURE_PROPERTY, "1");
        queueProperties
            .addGroup(SecurityContext.SECURITY_PROPERTIES, securityProperties);
        queueProperties.freeze();

        _receiver = getMessaging().createClientReceiver(queueProperties);
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
        _receiver.close();
        _receiver = null;

        stopService(_forwarderService);
    }

    /**
     * Tests XML.
     *
     * @throws Exception On failure.
     */
    @Test
    public void test()
        throws Exception
    {
        final StreamedMessagesClient client = new StreamedMessagesClient();
        final KeyedGroups configProperties = new KeyedGroups();
        final KeyedValues moduleProperties = new KeyedValues();

        configProperties
            .setValue(StreamedMessagesClient.PREFIX_PROPERTY, "test-data-");
        moduleProperties
            .add(
                XStreamStreamer.ANNOTATED_CLASS_PROPERTY,
                _MESSAGE_EXAMPLE_CLASS_NAME);
        Require
            .success(
                client
                    .setUp(
                            _TESTS_INPUT_DIR,
                                    configProperties,
                                    moduleProperties.freeze()));

        client.setNameSuffix(Optional.of("-test"));

        Event event;
        PointValue pointValue;

        event = new Event(
            "Test",
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
        Require.success(client.add(event));

        pointValue = new PointValue(
            "Test",
            Optional.of(DateTime.now()),
            null,
            null);
        Require.success(client.add(pointValue));

        final Serializable messageExample = new ClassDefImpl(
            _MESSAGE_EXAMPLE_CLASS_NAME)
            .createInstance(Serializable.class);

        Require.success(client.add(messageExample));

        final File semFile = new File(
            _TESTS_INPUT_DIR,
            "test-sem-" + client.getNameStamp().toFileName() + "-test.sem");

        client.commit();
        client.tearDown();
        Require.success(semFile.createNewFile());

        final long timeout = getTimeout();

        event = (Event) _receiver.receive(timeout);
        Require.equal(event.getName(), "Test");

        pointValue = (PointValue) _receiver.receive(timeout);
        Require.equal(pointValue.getPointName().get(), "Test");

        final Serializable example = _receiver.receive(timeout);

        Require.equal(example, messageExample);
    }

    private static final String _FORWARDER_NAME = "StreamedMessages";
    private static final String _MESSAGE_EXAMPLE_CLASS_NAME =
        "org.rvpf.tests.example.MessageExample";
    private static final String _NAME_PROPERTY = "name";
    private static final String _QUEUE_NAME = "TestsStreamedMessages";
    private static final String _SECURITY_PROPERTIES = "tests.client.security";
    private static final String _SEM_OLD_FILE_NAME = "test-sem-old.sem";
    private static final File _TESTS_INPUT_DIR = new File("tests/data/tmp");
    private static final String _TESTS_PROPERTIES = "rvpf-forwarder.properties";

    private ServiceActivator _forwarderService;
    private MessagingSupport.Receiver _receiver;
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
