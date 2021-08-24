/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UpdateModuleTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.http;

import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;

import java.net.HttpURLConnection;

import java.nio.charset.StandardCharsets;

import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.update.UpdateModule;
import org.rvpf.http.update.UpdateServlet;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.proxy.ProxyStoreServiceActivator;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Update module tests.
 */
public final class UpdateModuleTests
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
        setUpAlerter();

        setProperty(NULL_NOTIFIER_PROPERTY, "!");
        _storeService = startStoreService(true);
        setUpExpectedUpdates(_storeService, _NUMERIC_POINT_NAME);

        _proxyService = startService(
            ProxyStoreServiceActivator.class,
            Optional.empty());

        final Metadata metadata = getMetadata(_storeService);

        for (final Point point: metadata.getPointsCollection()) {
            Require.success(((PointEntity) point).setUp(metadata));
        }

        _updateTime = DateTime.now().floored(ElapsedTime.SECOND);

        startServer();
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

        getMetadata(_storeService).tearDownPoints();

        if (_proxyService != null) {
            stopService(_proxyService);
            _proxyService = null;
        }

        if (_storeService != null) {
            stopService(_storeService);
            _storeService = null;
        }

        checkAlerts();
        tearDownAlerter();
    }

    /**
     * Tests the update module using GET.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testUpdateGET()
        throws Exception
    {
        final StringBuilder requestStringBuilder = new StringBuilder();

        requestStringBuilder
            .append(UpdateServlet.POINT_ATTRIBUTE + "=" + _NUMERIC_POINT_NAME);
        requestStringBuilder
            .append(
                "&" + UpdateServlet.STAMP_ATTRIBUTE + "="
                + _updateTime.toURLString());
        requestStringBuilder
            .append("&" + UpdateServlet.VALUE_ATTRIBUTE + "=1.0");

        expectUpdates(_NUMERIC_POINT_NAME);
        _doGET(requestStringBuilder);
        _expect(_NUMERIC_POINT_NAME, _updateTime, Double.valueOf(1.0), false);

        requestStringBuilder.setLength(0);
        requestStringBuilder
            .append(UpdateServlet.POINT_ATTRIBUTE + "=" + _NUMERIC_POINT_NAME);
        requestStringBuilder
            .append(
                "&" + UpdateServlet.STAMP_ATTRIBUTE + "="
                + _updateTime.toURLString());
        requestStringBuilder.append("&" + UpdateServlet.DELETE_PARAMETER);

        expectUpdates(_NUMERIC_POINT_NAME);
        _doGET(requestStringBuilder);
        _expect(_NUMERIC_POINT_NAME, _updateTime, null, true);

        requestStringBuilder.setLength(0);
        requestStringBuilder
            .append(UpdateServlet.POINT_ATTRIBUTE + "=" + _NUMERIC_POINT_NAME);
        requestStringBuilder
            .append(
                "&" + UpdateServlet.STAMP_ATTRIBUTE + "="
                + _updateTime.toURLString());
        requestStringBuilder
            .append("&" + UpdateServlet.VALUE_ATTRIBUTE + "=1.2");

        expectUpdates(_NUMERIC_POINT_NAME);
        _doGET(requestStringBuilder);
        _expect(_NUMERIC_POINT_NAME, _updateTime, Double.valueOf(1.2), false);

        requestStringBuilder.setLength(0);
        requestStringBuilder
            .append(UpdateServlet.POINT_ATTRIBUTE + "=" + _NUMERIC_POINT_NAME);
        requestStringBuilder
            .append(
                "&" + UpdateServlet.STAMP_ATTRIBUTE + "="
                + _updateTime.toURLString());

        expectUpdates(_NUMERIC_POINT_NAME);
        _doGET(requestStringBuilder);
        _expect(_NUMERIC_POINT_NAME, _updateTime, null, false);
    }

    /**
     * Tests the update module using POST.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testUpdatePOST()
        throws Exception
    {
        XMLElement requestRoot;
        XMLElement childElement;

        requestRoot = new XMLElement(UpdateServlet.UPDATES_ROOT);
        childElement = new XMLElement(UpdateServlet.UPDATE_ELEMENT);
        childElement
            .setAttribute(UpdateServlet.POINT_ATTRIBUTE, _NUMERIC_POINT_NAME);
        childElement
            .setAttribute(
                UpdateServlet.STAMP_ATTRIBUTE,
                _updateTime.toString());
        childElement.setAttribute(UpdateServlet.VALUE_ATTRIBUTE, "1.0");
        requestRoot.addChild(childElement);
        childElement = new XMLElement(UpdateServlet.UPDATE_ELEMENT);
        childElement
            .setAttribute(UpdateServlet.POINT_ATTRIBUTE, _NUMERIC_POINT_NAME);
        childElement
            .setAttribute(
                UpdateServlet.STAMP_ATTRIBUTE,
                _updateTime.toString());
        childElement.addText("1.2");
        requestRoot.addChild(childElement);
        childElement = new XMLElement(UpdateServlet.UPDATE_ELEMENT);
        childElement
            .setAttribute(UpdateServlet.POINT_ATTRIBUTE, _NUMERIC_POINT_NAME);
        childElement
            .setAttribute(
                UpdateServlet.STAMP_ATTRIBUTE,
                _updateTime.toString());
        requestRoot.addChild(childElement);
        expectUpdates(_NUMERIC_POINT_NAME);
        expectUpdates(_NUMERIC_POINT_NAME);
        expectUpdates(_NUMERIC_POINT_NAME);
        _doPOST(new XMLDocument(requestRoot));
        _expect(_NUMERIC_POINT_NAME, _updateTime, Double.valueOf(1.0), false);
        _expect(_NUMERIC_POINT_NAME, _updateTime, Double.valueOf(1.2), false);
        _expect(_NUMERIC_POINT_NAME, _updateTime, null, false);

        requestRoot = new XMLElement(UpdateServlet.UPDATES_ROOT);
        childElement = new XMLElement(UpdateServlet.DELETE_ELEMENT);
        childElement
            .setAttribute(UpdateServlet.POINT_ATTRIBUTE, _NUMERIC_POINT_NAME);
        childElement
            .setAttribute(
                UpdateServlet.STAMP_ATTRIBUTE,
                _updateTime.toString());
        requestRoot.addChild(childElement);
        expectUpdates(_NUMERIC_POINT_NAME);
        _doPOST(new XMLDocument(requestRoot));
        _expect(_NUMERIC_POINT_NAME, _updateTime, null, true);
    }

    private void _doGET(
            final StringBuilder requestStringBuilder)
        throws Exception
    {
        final HttpURLConnection connection = openConnection(
            _CONNECTION_PATH + "?" + requestStringBuilder,
            true);

        connection.setUseCaches(false);
        Require
            .success(
                connection.getResponseCode() == HttpURLConnection.HTTP_OK);
        connection.disconnect();
    }

    private void _doPOST(final XMLDocument request)
        throws Exception
    {
        final String xml = request.toXML(Optional.empty(), false);
        final HttpURLConnection connection = openConnection(
            _CONNECTION_PATH,
            true);
        final Writer writer;

        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml;charset=UTF-8");
        connection
            .setRequestProperty("Content-Length", String.valueOf(xml.length()));
        writer = new OutputStreamWriter(
            connection.getOutputStream(),
            StandardCharsets.UTF_8);
        writer.write(xml);
        writer.close();
        Require
            .success(
                connection.getResponseCode() == HttpURLConnection.HTTP_OK);
        connection.disconnect();
    }

    private void _expect(
            final String pointName,
            final DateTime stamp,
            final Serializable value,
            final boolean deleted)
        throws Exception
    {
        final Point point = getMetadata(_storeService)
            .getPoint(pointName)
            .get();
        final PointValue update = waitForUpdate(_NUMERIC_POINT_NAME);

        Require.notNull(update, "Update");
        Require.equal(update.getPointUUID(), point.getUUID().get());
        Require.equal(update.getStamp(), stamp);
        Require.equal(update.getValue(), value);
        Require.success(update.isDeleted() == deleted);
    }

    private static final String _CONNECTION_PATH = UpdateModule.DEFAULT_PATH
        + UpdateModule.ACCEPT_PATH;
    private static final String _NUMERIC_POINT_NAME = "TESTS.NUMERIC.01";

    private ServiceActivator _proxyService;
    private ServiceActivator _storeService;
    private DateTime _updateTime;
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
