/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JSONSupportTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.xml;

import java.util.List;
import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.JSONSupport;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.tests.Tests;

import org.testng.annotations.Test;

/**
 * JSON support tests.
 */
public class JSONSupportTests
    extends Tests
{
    /**
     * Should generate a basic JSON string.
     */
    @Test(priority = 10)
    public static void shouldGenerateBasicString()
    {
        final XMLDocument xmlDocument;
        final String jsonString;
        XMLElement child;

        // Given s basic XML document,
        xmlDocument = new XMLDocument("test");
        xmlDocument.getRootElement().setAttribute("title", "testTitle");
        child = new XMLElement("single");
        child.setAttribute("value", "testValue1");
        child.addText("test text");
        xmlDocument.getRootElement().addChild(child);
        child = new XMLElement("multiple");
        child.setAttribute("value", "testValue2");
        xmlDocument.getRootElement().addChild(child);
        child = new XMLElement("multiple");
        child.setAttribute("value", "testValue3");
        xmlDocument.getRootElement().addChild(child);

        // when generating a JSON string from the document,
        jsonString = JSONSupport.toJSON(xmlDocument);

        // then it should be the basic JSON string.
        Require
            .equal(
                jsonString,
                "{\"test\":{" + "\"title\":\"testTitle\""
                + ",\"single\":{\"value\":\"testValue1\",\"\":\"test text\"}"
                + ",\"multiple\":[{\"value\":\"testValue2\"}"
                + ",{\"value\":\"testValue3\"}]}}");
    }

    /**
     * Should generate a minimal JSON string.
     */
    @Test(priority = 10)
    public static void shouldGenerateMinimalString()
    {
        final XMLDocument xmlDocument;
        final String jsonString;

        // Given an empty XML document,
        xmlDocument = new XMLDocument(".");

        // when generating a JSON string from the document,
        jsonString = JSONSupport.toJSON(xmlDocument);

        // then it should be the minimal JSON string.
        Require.equal(jsonString, "{\".\":{}}");
    }

    /**
     * Should parse a basic JSON string.
     *
     * @throws XMLDocument.ParseException Should not happen.
     */
    @Test(priority = 20)
    public static void shouldParseBasicString()
        throws XMLDocument.ParseException
    {
        final String jsonString;
        final XMLElement rootElement;
        final List<? extends XMLElement> children;

        // Given a basic JSON string,
        jsonString = "{\"test\":{" + "\"title\": \"testTitle\""
                + ", \"single\": {\"value\": \"testValue1\""
                + ",\"\":\"test text\"}"
                + ", \"multiple\": [{\"value\": \"testValue2\"}"
                + ", {\"value\": \"testValue3\"}]}}";

        // when parsing the string to an XML element,
        rootElement = JSONSupport.parse(jsonString);

        // then it should have recognized a basic root element.
        Require.equal(rootElement.getName(), "test");
        Require.success(rootElement.getAttributeCount() == 1);
        Require
            .equal(
                rootElement.getAttributeValue("title", Optional.empty()).get(),
                "testTitle");
        Require.success(rootElement.getChildCount() == 3);
        children = rootElement.getChildren();
        Require.equal(children.get(0).getName(), "single");
        Require.success(children.get(0).getAttributeCount() == 1);
        Require
            .equal(
                children
                    .get(0)
                    .getAttributeValue("value", Optional.empty())
                    .get(),
                "testValue1");
        Require.equal(children.get(0).getText(), "test text");
        Require.equal(children.get(1).getName(), "multiple");
        Require.success(children.get(1).getAttributeCount() == 1);
        Require
            .equal(
                children
                    .get(1)
                    .getAttributeValue("value", Optional.empty())
                    .get(),
                "testValue2");
        Require.equal(children.get(2).getName(), "multiple");
        Require.success(children.get(2).getAttributeCount() == 1);
        Require
            .equal(
                children
                    .get(2)
                    .getAttributeValue("value", Optional.empty())
                    .get(),
                "testValue3");
    }

    /**
     * Should parse a minimal JSON string.
     *
     * @throws XMLDocument.ParseException Should not happen.
     */
    @Test(priority = 10)
    public static void shouldParseMinimalString()
        throws XMLDocument.ParseException
    {
        final String jsonString;
        final XMLElement rootElement;

        // Given a minimal JSON string,
        jsonString = "{\".\":{}}";

        // when parsing the string to an XML element,
        rootElement = JSONSupport.parse(jsonString);

        // then it should have recognized an empty root element.
        Require.success(rootElement.getAttributeCount() == 0);
        Require.success(rootElement.getChildCount() == 0);
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
