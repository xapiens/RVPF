/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: QueryModuleTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;

import java.net.HttpURLConnection;

import java.nio.charset.StandardCharsets;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.store.Store;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.xml.XMLAttribute;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.query.InfoServlet;
import org.rvpf.http.query.QueryModule;
import org.rvpf.http.query.ValuesServlet;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.service.ServiceActivator;
import org.rvpf.store.server.proxy.ProxyStoreServiceActivator;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Query module tests.
 */
public final class QueryModuleTests
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
        setUpExpectedUpdates(
            _storeService,
            _NUMERIC_POINT_NAME,
            _COUNT_POINT_NAME);

        _proxyService = startService(
            ProxyStoreServiceActivator.class,
            Optional.empty());

        final Metadata metadata = getMetadata(_storeService);
        final long minuteRaw = ElapsedTime.MINUTE.toRaw();

        for (final Point point: metadata.getPointsCollection()) {
            if (!((PointEntity) point).setUpStore(metadata)) {
                throw new AssertionError(
                    "Connect '" + point + "' connect failed");
            }
        }

        _stopTime = DateTime.now().floored(ElapsedTime.MINUTE);
        _startTime = _stopTime.before(_COUNT_COUNT * minuteRaw);

        for (int i = 1; i <= _COUNT_COUNT; ++i) {
            _storeValue(
                _COUNT_POINT_NAME,
                _startTime.after(i * minuteRaw),
                Long.valueOf(i));
        }

        _numericTime = DateTime.now().floored(ElapsedTime.SECOND);
        _storeValue(_NUMERIC_POINT_NAME, _numericTime, Double.valueOf(1.0));

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
     * Tests query info using GET.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testInfoGET()
        throws Exception
    {
        final StringBuilder queryStringBuilder;

        queryStringBuilder = new StringBuilder();
        queryStringBuilder
            .append(
                InfoServlet.SELECT_PARAMETER + "=" + InfoServlet.POINTS_QUERY);
        queryStringBuilder.append("&" + InfoServlet.WILD_ATTRIBUTE + "=*");

        Require
            .failure(
                _getInfo(_doGET(QueryModule.INFO_PATH, queryStringBuilder))
                    .isEmpty());
    }

    /**
     * Tests query info using POST.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testInfoPOST()
        throws Exception
    {
        final XMLElement request = new XMLElement(
            InfoServlet.REQUESTS_ROOT,
            new XMLElement(
                InfoServlet.POINTS_QUERY,
                new XMLAttribute(InfoServlet.WILD_ATTRIBUTE, "*")));

        Require
            .failure(
                _getInfo(
                    _doPOST(QueryModule.INFO_PATH, new XMLDocument(request)))
                    .isEmpty());
    }

    /**
     * Tests query values using GET.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testValuesGET()
        throws Exception
    {
        final StringBuilder query = new StringBuilder();

        query.append(ValuesServlet.POINT_ATTRIBUTE + "=" + _NUMERIC_POINT_NAME);

        Require
            .success(
                _getCount(
                    _NUMERIC_POINT_NAME,
                    _doGET(
                            QueryModule.VALUES_PATH + QueryModule.COUNT_PATH,
                                    query)) == 1);
        _verifyNumericValues(
            _getValues(
                _NUMERIC_POINT_NAME,
                _doGET(QueryModule.VALUES_PATH, query)));

        query.setLength(0);
        query.append(ValuesServlet.POINT_ATTRIBUTE + "=" + _COUNT_POINT_NAME);

        _verifyCountValue(
            _getValues(
                _COUNT_POINT_NAME,
                _doGET(QueryModule.VALUES_PATH, query)));

        query
            .append(
                "&" + ValuesServlet.AFTER_ATTRIBUTE + "=" + _startTime.before(
                    1).toHexString());
        query
            .append(
                "&" + ValuesServlet.BEFORE_ATTRIBUTE + "=" + _startTime.after(
                    (_COUNT_COUNT * ElapsedTime.MINUTE.toRaw())
                    + 1).toURLString());

        Require
            .success(
                _getCount(
                    _COUNT_POINT_NAME,
                    _doGET(
                            QueryModule.VALUES_PATH + QueryModule.COUNT_PATH,
                                    query)) == _COUNT_COUNT);
        _verifyCountValues(
            _getValues(
                _COUNT_POINT_NAME,
                _doGET(QueryModule.VALUES_PATH, query)),
            _RMI_STORE_LIMIT);

        query
            .append("&" + ValuesServlet.LIMIT_ATTRIBUTE + "=" + _REQUEST_LIMIT);

        _verifyCountValues(
            _getValues(
                _COUNT_POINT_NAME,
                _doGET(QueryModule.VALUES_PATH, query)),
            _REQUEST_LIMIT);
    }

    /**
     * Tests query values using POST.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testValuesPOST()
        throws Exception
    {
        XMLElement request;

        request = new XMLElement(
            ValuesServlet.REQUESTS_ROOT,
            new XMLElement(
                ValuesServlet.REQUEST_ELEMENT,
                new XMLAttribute(
                    ValuesServlet.POINT_ATTRIBUTE,
                    _NUMERIC_POINT_NAME)));
        Require
            .success(
                _getCount(
                    _NUMERIC_POINT_NAME,
                    _doPOST(
                            QueryModule.VALUES_PATH + QueryModule.COUNT_PATH,
                                    new XMLDocument(request))) == 1);
        _verifyNumericValues(
            _getValues(
                _NUMERIC_POINT_NAME,
                _doPOST(QueryModule.VALUES_PATH, new XMLDocument(request))));

        request = new XMLElement(
            ValuesServlet.REQUESTS_ROOT,
            new XMLElement(
                ValuesServlet.REQUEST_ELEMENT,
                new XMLAttribute(
                    ValuesServlet.POINT_ATTRIBUTE,
                    _COUNT_POINT_NAME),
                new XMLAttribute(
                    ValuesServlet.AFTER_ATTRIBUTE,
                    _startTime.before(1).toHexString()),
                new XMLAttribute(
                    ValuesServlet.BEFORE_ATTRIBUTE,
                    _startTime.after(
                            (_COUNT_COUNT * ElapsedTime.MINUTE.toRaw())
                            + 1).toString())));
        Require
            .success(
                _getCount(
                    _COUNT_POINT_NAME,
                    _doPOST(
                            QueryModule.VALUES_PATH + QueryModule.COUNT_PATH,
                                    new XMLDocument(request))) == _COUNT_COUNT);
        _verifyCountValues(
            _getValues(
                _COUNT_POINT_NAME,
                _doPOST(QueryModule.VALUES_PATH, new XMLDocument(request))),
            _RMI_STORE_LIMIT);
    }

    private static int _getCount(
            final String pointName,
            final HttpURLConnection connection)
        throws Exception
    {
        final InputStream inputStream = connection.getInputStream();
        final Reader reader;
        final XMLDocument document;
        final XMLElement root;
        final XMLElement response;
        final String count;

        reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        document = new XMLDocument();
        document.parse(reader);
        reader.close();
        connection.disconnect();

        root = document.getRootElement();
        Require.equal(root.getName(), ValuesServlet.RESPONSES_ROOT);
        Require
            .success(
                root.getChildren(ValuesServlet.RESPONSE_ELEMENT).size() == 1);
        response = root.getFirstChild(ValuesServlet.RESPONSE_ELEMENT).get();
        Require
            .equal(
                response
                    .getAttributeValue(
                            ValuesServlet.POINT_ATTRIBUTE,
                                    Optional.empty())
                    .get(),
                pointName);
        Require.success(response.getChildCount() == 1);

        final XMLElement element = response.getChild(0);

        Require.equal(element.getName(), ValuesServlet.COUNT_ELEMENT);
        count = element
            .getAttributeValue(ValuesServlet.VALUE_ATTRIBUTE, Optional.empty())
            .get();

        return Integer.parseInt(count);
    }

    private static List<? extends XMLElement> _getInfo(
            final HttpURLConnection connection)
        throws Exception
    {
        final InputStream inputStream = connection.getInputStream();
        final Reader reader;
        final XMLDocument document;
        final XMLElement root;

        reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        document = new XMLDocument();
        document.parse(reader);
        reader.close();
        connection.disconnect();

        root = document.getRootElement();
        Require.equal(root.getName(), InfoServlet.RESPONSES_ROOT);

        return root.getChildren();
    }

    private static List<Value> _getValues(
            final String pointName,
            final HttpURLConnection connection)
        throws Exception
    {
        final InputStream inputStream = connection.getInputStream();
        final List<Value> values = new LinkedList<Value>();
        final Reader reader;
        final XMLDocument document;
        final XMLElement root;
        final XMLElement response;

        reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        document = new XMLDocument();
        document.parse(reader);
        reader.close();
        connection.disconnect();

        root = document.getRootElement();
        Require.equal(root.getName(), ValuesServlet.RESPONSES_ROOT);
        Require
            .success(
                root.getChildren(ValuesServlet.RESPONSE_ELEMENT).size() == 1);
        response = root.getFirstChild(ValuesServlet.RESPONSE_ELEMENT).get();
        Require
            .equal(
                response
                    .getAttributeValue(
                            ValuesServlet.POINT_ATTRIBUTE,
                                    Optional.empty())
                    .get(),
                pointName);

        for (final XMLElement element: response.getChildren()) {
            final Value value = new Value();
            final Optional<String> stampAttribute = element
                .getAttributeValue(
                    ValuesServlet.STAMP_ATTRIBUTE,
                    Optional.empty());

            value.stamp = stampAttribute
                .isPresent()? DateTime
                    .now()
                    .valueOf(stampAttribute.get()): null;

            if (element.getName().equals(ValuesServlet.VALUE_ELEMENT)) {
                value.value = element
                    .getAttributeValue(
                        ValuesServlet.VALUE_ATTRIBUTE,
                        Optional.empty())
                    .orElse(null);

                if (value.value == null) {
                    value.value = element.getText();

                    if (value.value.isEmpty()) {
                        value.value = null;
                    }
                }
            } else {
                Require.equal(element.getName(), ValuesServlet.MARK_ELEMENT);
                value.value = _MARK_VALUE;
            }

            values.add(value);
        }

        return values;
    }

    private HttpURLConnection _doGET(
            final String path,
            final StringBuilder queryStringBuilder)
        throws Exception
    {
        final HttpURLConnection connection = openConnection(
            QueryModule.DEFAULT_PATH + path + "?" + queryStringBuilder,
            false);

        connection.setUseCaches(false);

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Require
                .failure(
                    "Connection to '" + connection.getURL()
                    + "' failed with \"" + getResponseMessage(
                        connection) + "\"");
        }

        return connection;
    }

    private HttpURLConnection _doPOST(
            final String path,
            final XMLDocument request)
        throws Exception
    {
        final String xml = request.toXML(Optional.empty(), false);
        final HttpURLConnection connection = openConnection(
            QueryModule.DEFAULT_PATH + path,
            false);
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

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Require
                .failure(
                    "Connection to '" + connection.getURL()
                    + "' failed with \"" + getResponseMessage(
                        connection) + "\"");
        }

        return connection;
    }

    private void _storeValue(
            final String pointName,
            final DateTime stamp,
            final Serializable value)
        throws Exception
    {
        final Point point = getMetadata(_storeService)
            .getPoint(pointName)
            .get();
        final Store store = point.getStore().get();
        final PointValue update;

        expectUpdates(pointName);
        store.addUpdate(new PointValue(point, Optional.of(stamp), null, value));
        Require.success(store.sendUpdates());
        update = waitForUpdate(pointName);
        Require.notNull(update, "Update");
    }

    private void _verifyCountValue(final List<Value> values)
    {
        Require.success(values.size() == 1);

        final Value value = values.get(0);

        Require.equal(value.stamp, _stopTime);
        Require.equal(value.value, Integer.toString(_COUNT_COUNT));
    }

    private void _verifyCountValues(final List<Value> values, final int limit)
    {
        final Iterator<Value> iterator;

        Require.success(values.size() == limit + 1);
        iterator = values.iterator();

        Value value;

        for (int i = 1; i <= limit; ++i) {
            value = iterator.next();

            Require
                .equal(
                    value.stamp,
                    _startTime.after(i * ElapsedTime.MINUTE.toRaw()));
            Require.equal(value.value, Integer.toString(i));
        }

        value = iterator.next();
        Require
            .equal(
                value.stamp,
                _startTime.after((limit + 1) * ElapsedTime.MINUTE.toRaw()));
        Require.equal(value.value, _MARK_VALUE);
    }

    private void _verifyNumericValues(final List<Value> values)
    {
        final Value value;

        Require.success(values.size() == 1);
        value = values.get(0);
        Require.equal(value.stamp, _numericTime);
        Require.equal(value.value, "1.0");
    }

    private static final int _COUNT_COUNT = 5;
    private static final String _COUNT_POINT_NAME = "TESTS.CLOCK.01";
    private static final String _MARK_VALUE = "MARK";
    private static final String _NUMERIC_POINT_NAME = "TESTS.NUMERIC.01";
    private static final int _REQUEST_LIMIT = 2;
    private static final int _RMI_STORE_LIMIT = 3;

    private DateTime _numericTime;
    private ServiceActivator _proxyService;
    private DateTime _startTime;
    private DateTime _stopTime;
    private ServiceActivator _storeService;

    private static final class Value
    {
        Value() {}

        DateTime stamp;
        String value;
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
