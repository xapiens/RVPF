/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AlerterModuleTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.http;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.net.HttpURLConnection;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.util.Optional;

import org.rvpf.base.UUID;
import org.rvpf.base.alert.Event;
import org.rvpf.base.alert.Signal;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.SignalTarget;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.alert.AlertModule;
import org.rvpf.http.alert.EventsServlet;
import org.rvpf.http.alert.StatusServlet;
import org.rvpf.http.alert.TriggerServlet;
import org.rvpf.service.Service;
import org.rvpf.service.ServiceImpl;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Alerter module tests.
 */
public final class AlerterModuleTests
    extends ModuleTests
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
        setProperty(ServiceImpl.RESTART_ALLOWED_PROPERTY, "1");

        setUpServer();

        expectSignals(Service.PING_SIGNAL);    // Initial Ping.
        expectEvents(Service.PONG_EVENT);    // Initial Pong.

        startServer();
    }

    /**
     * Should allow access to a static resource.
     *
     * @throws Exception On failure.
     */
    @Test
    public void shouldAllowAccessToStaticResource()
        throws Exception
    {
        waitForSignal(Service.PING_SIGNAL);    // Initial Ping.
        waitForEvent(Service.PONG_EVENT);    // Initial Pong.

        final HttpURLConnection connection;
        final long resourceSize;
        final InputStream resourceStream;
        final byte[] buffer;
        final long read;

        // Given a connection path to a static resource
        // and a positive response when opening the connection,
        connection = openConnection(_RESOURCE_EXAMPLE_PATH, true);
        connection.setUseCaches(false);
        Require
            .success(
                connection.getResponseCode() == HttpURLConnection.HTTP_OK);

        // when getting the content length
        // and reading the content in a buffer,
        resourceSize = connection.getContentLength();
        Require.success(resourceSize > 0);
        buffer = new byte[(int) resourceSize];
        resourceStream = new BufferedInputStream(connection.getInputStream());

        try {
            read = resourceStream.read(buffer);
        } finally {
            resourceStream.close();
        }

        // then the content length should match the read length.
        Require.success(read == resourceSize);
    }

    /**
     * Should fail if not authenticated.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"shouldAllowAccessToStaticResource"})
    public void shouldFailIfNotAuthenticated()
        throws Exception
    {
        final HttpURLConnection connection;

        // Given no authenticators,
        resetAuthenticator();

        // when requesting a Ping signal,
        connection = openConnection(
            AlertModule.DEFAULT_PATH + AlertModule.TRIGGER_PATH + "?"
            + TriggerServlet.SIGNAL_PARAMETER + "=" + Service.PING_SIGNAL,
            true);

        // then the response should be 'Unauthorized'.
        Require
            .success(
                connection.getResponseCode()
                == HttpURLConnection.HTTP_UNAUTHORIZED);
        connection.disconnect();
    }

    /**
     * Should support targeted Ping.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"shouldFailIfNotAuthenticated"})
    public void shouldSupportTargetedPing()
        throws Exception
    {
        final UUID uuid;
        final HttpURLConnection connection;
        final Signal ping;
        final SignalTarget pingTarget;

        // Given an authenticator
        // and a target UUID,
        assignAuthenticator();
        uuid = UUID.generate();

        // when requesting a Ping signal to the target UUID
        // and waiting for that Ping signal,
        expectSignals(Service.PING_SIGNAL);
        connection = openConnection(
            AlertModule.DEFAULT_PATH + AlertModule.TRIGGER_PATH + "?"
            + TriggerServlet.SIGNAL_PARAMETER + "=" + Service.PING_SIGNAL + "&"
            + TriggerServlet.UUID_PARAMETER + "=" + uuid.toString(),
            true);
        Require
            .success(
                connection.getResponseCode() == HttpURLConnection.HTTP_OK);
        connection.disconnect();
        ping = waitForSignal(Service.PING_SIGNAL);

        // then the received signal info should contain the target UUID.
        pingTarget = SignalTarget.fromString(ping.getInfo()).get();
        Require.equal(pingTarget.getUUID().get(), uuid);
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
        stopServer();

        checkAlerts();
        tearDownAlerter();
    }

    /**
     * Tests events.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"shouldSupportTargetedPing"})
    public void testEvents()
        throws Exception
    {
        final String after;
        HttpURLConnection connection;
        InputStream inputStream;
        Reader reader;
        XMLDocument document;
        XMLElement root;

        connection = openConnection(
            AlertModule.DEFAULT_PATH + AlertModule.EVENTS_PATH,
            true);
        connection.setUseCaches(false);
        inputStream = connection.getInputStream();
        reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        document = new XMLDocument();
        document.parse(reader);
        reader.close();
        root = document.getRootElement();
        Require.equal(root.getName(), EventsServlet.EVENTS_ROOT);
        Require.failure(root.getChildren(EventsServlet.EVENT_ELEMENT).isEmpty());

        connection = openConnection(
            AlertModule.DEFAULT_PATH + AlertModule.EVENTS_PATH + "?"
            + EventsServlet.AFTER_PARAMETER + "="
            + EventsServlet.AFTER_LAST_VALUE,
            true);
        connection.setUseCaches(false);
        inputStream = connection.getInputStream();
        reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        document = new XMLDocument();
        document.parse(reader);
        reader.close();
        root = document.getRootElement();
        Require.equal(root.getName(), EventsServlet.EVENTS_ROOT);
        Require
            .success(root.getChildren(EventsServlet.EVENT_ELEMENT).isEmpty());
        after = root
            .getAttributeValue(EventsServlet.STAMP_ATTRIBUTE, Optional.empty())
            .get();

        expectSignals(Service.PING_SIGNAL);
        sendSignal(Service.PING_SIGNAL, Optional.empty());
        connection = openConnection(
            AlertModule.DEFAULT_PATH + AlertModule.EVENTS_PATH + "?"
            + EventsServlet.AFTER_PARAMETER + "=" + URLEncoder.encode(
                after,
                StandardCharsets.UTF_8.name()) + "&"
                + EventsServlet.WAIT_PARAMETER + "=15",
            true);
        connection.setUseCaches(false);
        inputStream = connection.getInputStream();
        reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        document = new XMLDocument();
        document.parse(reader);
        reader.close();
        root = document.getRootElement();
        Require.equal(root.getName(), EventsServlet.EVENTS_ROOT);
        Require.failure(root.getChildren(EventsServlet.EVENT_ELEMENT).isEmpty());

        connection.disconnect();
        waitForSignal(Service.PING_SIGNAL);
    }

    /**
     * Tests a restart trigger.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testStatusRequest"})
    public void testRestartTrigger()
        throws Exception
    {
        final HttpURLConnection connection;
        final Signal signal;
        final Event event;
        UUID uuid;

        expectSignals(Service.PING_SIGNAL);
        sendSignal(
            Service.PING_SIGNAL,
            Optional
                .of(
                    new SignalTarget(
                            Optional.empty(),
                                    Optional.of(getSourceUUID()),
                                    Optional.of("Test-1"))));
        signal = waitForSignal(Service.PING_SIGNAL);
        Require.equal(signal.getSourceUUID().get(), getSourceUUID());
        Require
            .equal(
                (SignalTarget.fromString(signal.getInfo()))
                    .get()
                    .getUUID()
                    .get(),
                getSourceUUID());

        expectSignals(Service.PING_SIGNAL);
        expectEvents(Service.PONG_EVENT);
        sendSignal(Service.PING_SIGNAL, Optional.empty());
        waitForSignal(Service.PING_SIGNAL);
        uuid = waitForEvent(Service.PONG_EVENT).getSourceUUID().get();

        expectEvents(Service.PONG_EVENT);
        sendSignal(
            Service.PING_SIGNAL,
            Optional
                .of(
                    new SignalTarget(
                            Optional.empty(),
                                    Optional.of(uuid),
                                    Optional.of("Test-2"))));
        event = waitForEvent(Service.PONG_EVENT);
        Require.equal(event.getSourceUUID().get(), uuid);

        expectSignals(Service.RESTART_SIGNAL);
        expectEvents(Service.STOPPED_EVENT);
        expectEvents(Service.STARTED_EVENT);
        connection = openConnection(
            AlertModule.DEFAULT_PATH + AlertModule.TRIGGER_PATH + "?"
            + TriggerServlet.SIGNAL_PARAMETER + "=" + Service.RESTART_SIGNAL,
            true);
        Require
            .success(
                connection.getResponseCode() == HttpURLConnection.HTTP_OK);
        connection.disconnect();
        setListenerPort(0);
        waitForSignal(Service.RESTART_SIGNAL);
        waitForEvent(Service.STOPPED_EVENT);
        waitForEvent(Service.STARTED_EVENT);

        expectEvents(Service.PONG_EVENT);
        uuid = getServer().getService().getSourceUUID();
        sendSignal(
            Service.PING_SIGNAL,
            Optional
                .of(
                    new SignalTarget(
                            Optional.empty(),
                                    Optional.of(uuid),
                                    Optional.of("Test-3"))));
        Require
            .equal(
                waitForEvent(Service.PONG_EVENT).getSourceUUID().get(),
                uuid);
    }

    /**
     * Tests a status request.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods = {"testEvents"})
    public void testStatusRequest()
        throws Exception
    {
        final String eventName;
        int retries = 0;
        XMLElement root = null;

        for (;;) {
            final HttpURLConnection connection;
            final InputStream inputStream;
            final Reader reader;
            final XMLDocument document;

            connection = openConnection(
                AlertModule.DEFAULT_PATH + AlertModule.STATUS_PATH,
                true);
            connection.setUseCaches(false);
            inputStream = connection.getInputStream();
            reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            document = new XMLDocument();
            document.parse(reader);
            reader.close();

            connection.disconnect();
            root = document.getRootElement();
            Require.equal(root.getName(), StatusServlet.STATUS_ROOT);

            if ((retries > 1) || (root.getChildCount() > 0)) {
                break;
            }

            Thread.sleep(1000);
            ++retries;
        }

        final XMLElement service = root
            .getFirstChild(StatusServlet.SERVICE_ELEMENT)
            .get();

        Require
            .equal(
                service
                    .getAttributeValue(
                            StatusServlet.UUID_ATTRIBUTE,
                                    Optional.empty())
                    .orElse(null),
                getServer().getService().getSourceUUID().toString());
        Require
            .equal(
                service
                    .getAttributeValue(
                            StatusServlet.NAME_ATTRIBUTE,
                                    Optional.empty())
                    .orElse(null),
                getServer().getObjectName().toString());
        Require
            .equal(
                null,
                service
                    .getAttributeValue(
                            StatusServlet.ENTITY_ATTRIBUTE,
                                    Optional.empty())
                    .orElse(null));
        eventName = service
            .getAttributeValue(StatusServlet.EVENT_ATTRIBUTE, Optional.empty())
            .get();

        if (!Service.STARTED_EVENT.equals(eventName)
                && !Service.PONG_EVENT.equals(eventName)) {
            Require.failure("Unexpected event name: " + eventName);
        }
    }

    private static final String _RESOURCE_EXAMPLE_PATH = "MessageExample.class";
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
