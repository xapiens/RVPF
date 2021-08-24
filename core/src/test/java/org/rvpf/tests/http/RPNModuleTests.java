/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RPNModuleTests.java 4005 2019-05-18 15:52:45Z SFB $
 */

package org.rvpf.tests.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import java.net.HttpURLConnection;

import java.nio.charset.StandardCharsets;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.JSONSupport;
import org.rvpf.base.xml.XMLAttribute;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.AbstractServlet;
import org.rvpf.http.rpn.RPNModule;
import org.rvpf.http.rpn.RPNServlet;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * RPN module tests.
 */
public final class RPNModuleTests
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

        checkAlerts();
        tearDownAlerter();
    }

    /**
     * Tests the RPN module.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testRPNModule()
        throws Exception
    {
        _testRPNModule(false);
        _testRPNModule(true);
    }

    private XMLElement _execute(
            @Nonnull final XMLElement rootElement,
            final boolean json)
        throws Exception
    {
        final XMLDocument document = new XMLDocument(rootElement);
        final String request = json? JSONSupport
            .toJSON(document): document.toXML(Optional.empty(), false);
        final HttpURLConnection connection = openConnection(
            _CONNECTION_PATH,
            false);
        final Writer writer;
        final Reader reader;

        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("POST");
        connection
            .setRequestProperty(
                "Content-Type",
                json
                ? AbstractServlet.JSON_CONTENT_TYPE
                : AbstractServlet.XML_CONTENT_TYPE);
        connection
            .setRequestProperty(
                "Content-Length",
                String.valueOf(request.length()));
        writer = new OutputStreamWriter(
            connection.getOutputStream(),
            StandardCharsets.UTF_8);
        writer.write(request.toString());
        writer.close();

        reader = new BufferedReader(
            new InputStreamReader(
                connection.getInputStream(),
                StandardCharsets.UTF_8));

        if (json) {
            document.setRootElement(Optional.of(JSONSupport.parse(reader)));
        } else {
            document.parse(reader);
        }

        reader.close();
        connection.disconnect();

        return document.getRootElement();
    }

    private void _testRPNModule(final boolean json)
        throws Exception
    {
        XMLElement requestRoot;
        XMLElement responseRoot;
        XMLElement valueElement;

        requestRoot = new XMLElement(
            RPNServlet.REQUEST_ROOT,
            new XMLElement(
                RPNServlet.MACRO_ELEMENT,
                "add(...=0) { [ ... reduce + ] }"),
            new XMLElement(
                RPNServlet.MACRO_ELEMENT,
                "mul(...!) { [ ... reduce * ] }"),
            new XMLElement(
                RPNServlet.PROGRAM_ELEMENT,
                "mul(add($1, $2), @1 int)"),
            new XMLElement(
                RPNServlet.PARAM_ELEMENT,
                new XMLAttribute(RPNServlet.VALUE_ATTRIBUTE, "3")),
            new XMLElement(
                RPNServlet.RESULT_ELEMENT,
                new XMLAttribute(RPNServlet.CONTENT_ATTRIBUTE, "Numeric")),
            new XMLElement(
                RPNServlet.INPUT_ELEMENT,
                new XMLAttribute(RPNServlet.CONTENT_ATTRIBUTE, "Numeric"),
                new XMLAttribute(RPNServlet.VALUE_ATTRIBUTE, "1.0")),
            new XMLElement(
                RPNServlet.INPUT_ELEMENT,
                new XMLAttribute(RPNServlet.CONTENT_ATTRIBUTE, "Numeric"),
                new XMLAttribute(RPNServlet.VALUE_ATTRIBUTE, "2.0")));
        responseRoot = _execute(requestRoot, json);
        Require.equal(responseRoot.getName(), RPNServlet.RESPONSE_ROOT);
        Require
            .notPresent(responseRoot.getFirstChild(RPNServlet.MESSAGE_ELEMENT));
        valueElement = responseRoot
            .getFirstChild(RPNServlet.VALUE_ELEMENT)
            .get();
        Require.equal(valueElement.getText(), "9.0");

        requestRoot = new XMLElement(
            RPNServlet.REQUEST_ROOT,
            new XMLElement(RPNServlet.PROGRAM_ELEMENT, "+"),
            new XMLElement(
                RPNServlet.RESULT_ELEMENT,
                new XMLAttribute(RPNServlet.CONTENT_ATTRIBUTE, "Numeric")));
        responseRoot = _execute(requestRoot, json);
        Require.equal(responseRoot.getName(), RPNServlet.RESPONSE_ROOT);
        Require.present(responseRoot.getFirstChild(RPNServlet.MESSAGE_ELEMENT));
        Require
            .notPresent(responseRoot.getFirstChild(RPNServlet.VALUE_ELEMENT));

        requestRoot = new XMLElement(
            RPNServlet.REQUEST_ROOT,
            new XMLElement(RPNServlet.WORD_ELEMENT, ":* ( n -- n*n ) ; : *"),
            new XMLElement(RPNServlet.PROGRAM_ELEMENT, "$1 $2 * $3 :* /"),
            new XMLElement(
                RPNServlet.RESULT_ELEMENT,
                new XMLAttribute(RPNServlet.UNIT_ATTRIBUTE, "N")),
            new XMLElement(
                RPNServlet.INPUT_ELEMENT,
                new XMLAttribute(RPNServlet.UNIT_ATTRIBUTE, "g"),
                new XMLAttribute(RPNServlet.VALUE_ATTRIBUTE, "500.0")),
            new XMLElement(
                RPNServlet.INPUT_ELEMENT,
                new XMLAttribute(RPNServlet.UNIT_ATTRIBUTE, "km"),
                new XMLAttribute(RPNServlet.VALUE_ATTRIBUTE, "1.0")),
            new XMLElement(
                RPNServlet.INPUT_ELEMENT,
                new XMLAttribute(RPNServlet.UNIT_ATTRIBUTE, "s"),
                new XMLAttribute(RPNServlet.VALUE_ATTRIBUTE, "10.0")));
        responseRoot = _execute(requestRoot, json);
        Require.equal(responseRoot.getName(), RPNServlet.RESPONSE_ROOT);
        Require
            .notPresent(responseRoot.getFirstChild(RPNServlet.MESSAGE_ELEMENT));
        valueElement = responseRoot
            .getFirstChild(RPNServlet.VALUE_ELEMENT)
            .get();
        Require.equal(valueElement.getText(), "5.0");
    }

    private static final String _CONNECTION_PATH = RPNModule.DEFAULT_PATH
        + RPNModule.EXECUTE_PATH;
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
