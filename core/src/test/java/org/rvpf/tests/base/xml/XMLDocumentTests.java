/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: XMLDocumentTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.xml;

import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLDocument.ParseException;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.tests.Tests;

import org.testng.annotations.Test;

/**
 * XML document tests.
 */
public class XMLDocumentTests
    extends Tests
{
    /**
     * Should be functional when empty.
     */
    @Test(priority = 10)
    public static void shouldBeFunctionalWhenEmpty()
    {
        final XMLDocument xmlDocument;
        final XMLElement rootElement;
        final String xml;

        // Given an XML document without root,
        xmlDocument = new XMLDocument();

        // when asked for its content,
        rootElement = xmlDocument.getRootElement();
        xml = xmlDocument.toXML(Optional.empty(), false);

        // then it should have returned the expected values.
        Require.success(rootElement == null, "root element");
        Require.equal("<?xml version='1.0'?>\n", xml, "XML string");
    }

    /**
     * Should be functional with an empty root.
     */
    @Test(priority = 10)
    public static void shouldBeFunctionalWithEmptyRoot()
    {
        final XMLElement xmlElement;
        final XMLDocument xmlDocument;
        final XMLElement rootElement;
        final String xml;

        // Given an XML document with an empty root,
        xmlElement = new XMLElement("Test");
        xmlDocument = new XMLDocument(xmlElement);

        // when asked for its content,
        rootElement = xmlDocument.getRootElement();
        xml = xmlDocument.toXML(Optional.empty(), false);

        // then the supplied XML element should be returned.
        Require.same(rootElement, xmlElement, "root element");
        Require
            .equal(
                "<?xml version='1.0'?>\n<!DOCTYPE Test>\n<Test/>\n",
                xml,
                "XML string");
    }

    /**
     * Should parse an attribute.
     *
     * @throws XMLDocument.ParseException Should not happen.
     */
    @Test(priority = 21)
    public static void shouldParseAttribute()
        throws XMLDocument.ParseException
    {
        final XMLDocument xmlDocument;
        final XMLElement rootElement;

        // Given an XML document,
        xmlDocument = new XMLDocument();

        // when parsing an XML string with an attribute,
        rootElement = xmlDocument.parse("<Root attr='value'/>");

        // then it should have recognized the attribute.
        Require.success(rootElement.getAttributeCount() == 1);
        Require
            .equal(
                "value",
                rootElement.getAttributeValue("attr", Optional.empty()).get(),
                "attribute");
    }

    /**
     * Should parse a child.
     *
     * @throws XMLDocument.ParseException Should not happen.
     */
    @Test(priority = 23)
    public static void shouldParseChild()
        throws XMLDocument.ParseException
    {
        final XMLDocument xmlDocument;
        final XMLElement rootElement;
        final XMLElement child;

        // Given an XML document,
        xmlDocument = new XMLDocument();

        // when parsing an XML string with a child,
        rootElement = xmlDocument.parse("<Root><Child/></Root>");

        // then it should have recognized the child.
        Require.success(rootElement.getChildCount() == 1);
        child = rootElement.getFirstChild("Child").get();
        Require.equal(child.getName(), "Child", "child name");
    }

    /**
     * Should parse a minimal XML string.
     *
     * @throws XMLDocument.ParseException Should not happen.
     */
    @Test(priority = 20)
    public static void shouldParseMinimalString()
        throws XMLDocument.ParseException
    {
        final XMLDocument xmlDocument;
        final XMLElement rootElement;

        // Given an XML document,
        xmlDocument = new XMLDocument();

        // when parsing a minimal XML string,
        rootElement = xmlDocument.parse("<T/>");

        // then it should have recognized the root element
        Require.same(rootElement, xmlDocument.getRootElement());
        Require.equal("T", rootElement.getName(), "root name");

        // and it should not contain attributes, text or children.
        Require.success(rootElement.getAttributeCount() == 0);
        Require.equal("", rootElement.getText());
        Require.success(rootElement.getChildCount() == 0);
    }

    /**
     * Should parse text.
     *
     * @throws XMLDocument.ParseException Should not happen.
     */
    @Test(priority = 22)
    public static void shouldParseText()
        throws XMLDocument.ParseException
    {
        final XMLDocument xmlDocument;
        final XMLElement rootElement;

        // Given an XML document,
        xmlDocument = new XMLDocument();

        // when parsing an XML string with some text,
        rootElement = xmlDocument.parse("<Root>Some text.</Root>");

        // then it should have recognized the text.
        Require.equal(rootElement.getText(), "Some text.");
    }

    /**
     * Should throw a parse exception.
     */
    @Test(priority = 29)
    public static void shouldThrowParseException()
    {
        final XMLDocument xmlDocument;
        Exception catchedException = null;

        // Given an XML document,
        xmlDocument = new XMLDocument();

        // when parsing an invalid XML string,
        try {
            xmlDocument.parse("");
        } catch (final XMLDocument.ParseException exception) {
            catchedException = exception;
        }

        // then it should throw a ParseException.
        Require.notNull(catchedException, "parse exception");
    }

    /**
     * Element handler.
     */
    public static class ElementHandler
        implements XMLElement.Handler
    {
        /** {@inheritDoc}
         */
        @Override
        public XMLElement onElementEnd(
                final XMLElement element)
            throws ParseException
        {
            return element;
        }

        /** {@inheritDoc}
         */
        @Override
        public XMLElement onElementStart(
                final XMLElement element)
            throws ParseException
        {
            return element;
        }
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
