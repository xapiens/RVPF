/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NoticeModuleTests.java 4095 2019-06-24 17:44:43Z SFB $
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
import org.rvpf.base.value.RecalcTrigger;
import org.rvpf.base.xml.XMLAttribute;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.notice.NoticeModule;
import org.rvpf.http.notice.NoticeServlet;
import org.rvpf.som.SOMContainerServiceActivator;
import org.rvpf.tests.MessagingSupport;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Notice module tests.
 */
public final class NoticeModuleTests
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
        loadConfig(SOMContainerServiceActivator.class);
        loadMetadata(true);

        setUpAlerter();

        _receiver = getMessaging()
            .createServerReceiver(
                getConfig().getPropertiesGroup(_RECEPTIONIST_QUEUE_PROPERTIES));
        _receiver.purge();

        _noticeTime = DateTime.now().floored(ElapsedTime.SECOND);
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

        _receiver.close();
        _receiver = null;

        checkAlerts();
        tearDownAlerter();
    }

    /**
     * Tests the notice module using GET.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testNoticeGET()
        throws Exception
    {
        final StringBuilder noticestringBuilder = new StringBuilder();

        noticestringBuilder
            .append(NoticeServlet.POINT_ATTRIBUTE + "=" + _NUMERIC_POINT_NAME);
        noticestringBuilder
            .append(
                "&" + NoticeServlet.STAMP_ATTRIBUTE + "="
                + _noticeTime.toURLString());
        noticestringBuilder
            .append("&" + NoticeServlet.VALUE_ATTRIBUTE + "=1.0");

        _doGET(noticestringBuilder);
        _expect(_NUMERIC_POINT_NAME, _noticeTime, Double.valueOf(1.0), false);

        noticestringBuilder.setLength(0);
        noticestringBuilder
            .append(NoticeServlet.POINT_ATTRIBUTE + "=" + _NUMERIC_POINT_NAME);
        noticestringBuilder
            .append(
                "&" + NoticeServlet.STAMP_ATTRIBUTE + "="
                + _noticeTime.toURLString());
        noticestringBuilder.append("&" + NoticeServlet.RECALC_PARAMETER);

        _doGET(noticestringBuilder);
        _expect(_NUMERIC_POINT_NAME, _noticeTime, null, true);

        noticestringBuilder.setLength(0);
        noticestringBuilder
            .append(NoticeServlet.POINT_ATTRIBUTE + "=" + _NUMERIC_POINT_NAME);
        noticestringBuilder
            .append(
                "&" + NoticeServlet.STAMP_ATTRIBUTE + "="
                + _noticeTime.toURLString());
        noticestringBuilder
            .append("&" + NoticeServlet.VALUE_ATTRIBUTE + "=2.0");

        _doGET(noticestringBuilder);
        _expect(_NUMERIC_POINT_NAME, _noticeTime, Double.valueOf(2.0), false);

        noticestringBuilder.setLength(0);
        noticestringBuilder
            .append(NoticeServlet.POINT_ATTRIBUTE + "=" + _NUMERIC_POINT_NAME);
        noticestringBuilder
            .append(
                "&" + NoticeServlet.STAMP_ATTRIBUTE + "="
                + _noticeTime.toURLString());

        _doGET(noticestringBuilder);
        _expect(_NUMERIC_POINT_NAME, _noticeTime, null, false);

        _receiver.commit();
    }

    /**
     * Tests the notice module using POST.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testNoticePOST()
        throws Exception
    {
        XMLElement request;

        request = new XMLElement(
            NoticeServlet.NOTICES_ROOT,
            new XMLElement(
                NoticeServlet.NOTICE_ELEMENT,
                new XMLAttribute(
                    NoticeServlet.POINT_ATTRIBUTE,
                    _NUMERIC_POINT_NAME),
                new XMLAttribute(
                    NoticeServlet.STAMP_ATTRIBUTE,
                    _noticeTime.toString()),
                new XMLAttribute(NoticeServlet.VALUE_ATTRIBUTE, "1.0")),
            new XMLElement(
                NoticeServlet.NOTICE_ELEMENT,
                new XMLAttribute(
                    NoticeServlet.POINT_ATTRIBUTE,
                    _NUMERIC_POINT_NAME),
                new XMLAttribute(
                    NoticeServlet.STAMP_ATTRIBUTE,
                    _noticeTime.toString()),
                "2.0"),
            new XMLElement(
                NoticeServlet.NOTICE_ELEMENT,
                new XMLAttribute(
                    NoticeServlet.POINT_ATTRIBUTE,
                    _NUMERIC_POINT_NAME),
                new XMLAttribute(
                    NoticeServlet.STAMP_ATTRIBUTE,
                    _noticeTime.toString())));
        _doPOST(new XMLDocument(request));
        _expect(_NUMERIC_POINT_NAME, _noticeTime, Double.valueOf(1.0), false);
        _expect(_NUMERIC_POINT_NAME, _noticeTime, Double.valueOf(2.0), false);
        _expect(_NUMERIC_POINT_NAME, _noticeTime, null, false);

        request = new XMLElement(
            NoticeServlet.NOTICES_ROOT,
            new XMLElement(
                NoticeServlet.RECALC_ELEMENT,
                new XMLAttribute(
                    NoticeServlet.POINT_ATTRIBUTE,
                    _NUMERIC_POINT_NAME),
                new XMLAttribute(
                    NoticeServlet.STAMP_ATTRIBUTE,
                    _noticeTime.toString())));
        _doPOST(new XMLDocument(request));
        _expect(_NUMERIC_POINT_NAME, _noticeTime, null, true);

        _receiver.commit();
    }

    private void _doGET(final StringBuilder queryStringBuilder)
        throws Exception
    {
        final HttpURLConnection connection = openConnection(
            _CONNECTION_PATH + "?" + queryStringBuilder,
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
            final boolean recalc)
        throws Exception
    {
        final Point point = getPoint(pointName);
        final PointValue notice;

        notice = (PointValue) _receiver.receive(getTimeout());
        Require.notNull(notice, "Received notice");
        Require.equal(notice.getPointUUID(), point.getUUID().get());
        Require.equal(notice.getStamp(), stamp);

        if (recalc) {
            Require.success(notice instanceof RecalcTrigger);
        }
    }

    private static final String _CONNECTION_PATH = NoticeModule.DEFAULT_PATH
        + NoticeModule.ACCEPT_PATH;
    private static final String _NUMERIC_POINT_NAME = "TESTS.NUMERIC.01";
    private static final String _RECEPTIONIST_QUEUE_PROPERTIES =
        "tests.processor.receptionist.queue";

    private DateTime _noticeTime;
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
