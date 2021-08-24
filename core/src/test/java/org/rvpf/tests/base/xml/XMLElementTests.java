/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: XMLElementTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.xml;

import java.util.List;
import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.XMLAttribute;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.tests.Tests;

import org.testng.annotations.Test;

/**
 * XML element tests.
 */
public class XMLElementTests
    extends Tests
{
    /**
     * Should accept attributes.
     */
    @Test(priority = 20)
    public static void shouldAcceptAttributes()
    {
        final XMLElement xmlElement;

        // Given a new XML element,
        xmlElement = new XMLElement("Test");

        // when adding attributes,
        xmlElement.setAttribute("attr-1", "value-1");
        xmlElement.setAttribute("attr-2", "value-2");

        // then these attributes should be accessibles.
        Require
            .success(xmlElement.getAttributeCount() == 2, "attribute count");
        Require
            .content(
                xmlElement.getAttributes(),
                "attributes",
                new XMLAttribute("attr-1", "value-1"),
                new XMLAttribute("attr-2", "value-2"));
        Require
            .equal(
                xmlElement.getAttributeValue("attr-1", Optional.empty()).get(),
                "value-1",
                "attriabute 1 value");
        Require
            .equal(
                xmlElement.getAttributeValue("attr-2", Optional.empty()).get(),
                "value-2",
                "attribute 2 value");
    }

    /**
     * Should accept children.
     */
    @SuppressWarnings("unchecked")
    @Test(priority = 20)
    public static void shouldAcceptChildren()
    {
        final XMLElement xmlElement;
        final XMLElement child1;
        final XMLElement child2;

        // Given a new XML element
        xmlElement = new XMLElement("Test");

        // and 2 other XML elements,
        child1 = new XMLElement("Child-1");
        child2 = new XMLElement("Child-2");

        // when adding these elements as children,
        xmlElement.addChild(child1);
        xmlElement.addChild(child2);

        // then these children should be accessibles.
        Require.success(xmlElement.getChildCount() == 2, "child count");
        Require
            .content(
                (List<XMLElement>) xmlElement.getChildren(),
                "children",
                child1,
                child2);
        Require
            .content(
                (List<XMLElement>) xmlElement.getChildren("Child-2"),
                "child 2 list",
                child2);
        Require
            .same(
                xmlElement.getFirstChild("Child-2").get(),
                child2,
                "first child 2");
        Require.same(xmlElement.getChild(0), child1, "child at 0");
        Require.same(xmlElement.getChild(1), child2, "child at 1");
    }

    /**
     * Should accept text.
     */
    @Test(priority = 20)
    public static void shouldAcceptText()
    {
        final XMLElement xmlElement;

        // Given an XML element,
        xmlElement = new XMLElement("Test");

        // when adding some text,
        xmlElement.addText("Some");
        xmlElement.addText(" text.");

        // then this text should be accessible.
        Require.equal(xmlElement.getText(), "Some text.", "element text");
    }

    /**
     * Should allow read access to name.
     */
    @Test(priority = 10)
    public static void shouldAllowAccessToName()
    {
        final String name = " Test ";
        final XMLElement xmlElement;
        final String returnedName;
        final String returnPath;

        // Given a new XML element,
        xmlElement = new XMLElement(name);

        // when asking for its name
        returnedName = xmlElement.getName();

        // and path,
        returnPath = xmlElement.getPath();

        // then it should return a trimmed version of the name
        Require.equal(returnedName, name.trim(), "element name");

        // and the path.
        Require.equal(returnPath, '/' + returnedName, "element path");
    }

    /**
     * Should be case sensitive.
     */
    @Test(priority = 30)
    public static void shouldBeCaseSensitive()
    {
        final XMLElement xmlElement;
        final String attributeValue;
        final Optional<XMLElement> childElement;

        // Given an XML element
        xmlElement = new XMLElement("Test");

        // and an attribute with lower case name
        xmlElement.setAttribute("attr", "value");

        // and an element with lower case name,
        xmlElement.addChild(new XMLElement("child"));

        // when asking for the attribute and element with upper case,
        attributeValue = xmlElement
            .getAttributeValue("ATTR", Optional.empty())
            .orElse(null);
        childElement = xmlElement.getFirstChild("CHILD");

        // then nothing should be returned.
        Require.success(attributeValue == null, "attribute value");
        Require.notPresent(childElement, "child");
    }

    /**
     * Should be functional when empty.
     */
    @Test(priority = 10)
    public static void shouldBeFunctionalWhenEmpty()
    {
        final XMLElement xmlElement;
        final int attributeCount;
        final List<XMLAttribute> attributes;
        final String attributeValue;
        final int childCount;
        final List<? extends XMLElement> children;
        final List<? extends XMLElement> childrenForName;
        final Optional<XMLElement> firstChildForName;
        final String text;

        // Given a new XML element without children, text or attributes,
        xmlElement = new XMLElement("Test");

        // when asked for its content,
        attributeCount = xmlElement.getAttributeCount();
        attributes = xmlElement.getAttributes();
        attributeValue = xmlElement
            .getAttributeValue("test", Optional.empty())
            .orElse(null);
        childCount = xmlElement.getChildCount();
        children = xmlElement.getChildren();
        childrenForName = xmlElement.getChildren("Test");
        firstChildForName = xmlElement.getFirstChild("Test");
        text = xmlElement.getText();

        // then it should have returned the expected values.
        Require.success(attributeCount == 0, "attribute count");
        Require.success(attributes.isEmpty(), "attributes list");
        Require.success(attributeValue == null, "attribute value");
        Require.success(childCount == 0, "child count");
        Require.success(children.isEmpty(), "children list");
        Require.success(childrenForName.isEmpty(), "children for name");
        Require.notPresent(firstChildForName, "first child for name");
        Require.success(text.isEmpty(), "element text");
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
