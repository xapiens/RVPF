/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ElementWriter.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base.xml.streamer.xstream;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.xml.XMLElement;

import com.thoughtworks.xstream.io.xml.AbstractDocumentWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;

/**
 * Element writer.
 */
@NotThreadSafe
public class ElementWriter
    extends AbstractDocumentWriter
{
    /**
     * Constructs an instance.
     *
     * @param rootElement The root element.
     */
    ElementWriter(@Nonnull final XMLElement rootElement)
    {
        super(rootElement, new XmlFriendlyNameCoder());
    }

    /** {@inheritDoc}
     */
    @Override
    public void addAttribute(final String name, final String value)
    {
        final XMLElement current = (XMLElement) getCurrent();

        current.setAttribute(name, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setValue(final String text)
    {
        final XMLElement current = (XMLElement) getCurrent();

        current.addText(text);
    }

    /** {@inheritDoc}
     */
    @Override
    protected Object createNode(final String name)
    {
        final XMLElement current = (XMLElement) getCurrent();
        final XMLElement element = new XMLElement(name);

        current.addChild(element);

        return element;
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
