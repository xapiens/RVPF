/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ElementReader.java 3897 2019-02-16 19:14:07Z SFB $
 */

package org.rvpf.base.xml.streamer.xstream;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.tool.Require;
import org.rvpf.base.xml.XMLAttribute;
import org.rvpf.base.xml.XMLElement;

import com.thoughtworks.xstream.core.util.FastStack;
import com.thoughtworks.xstream.io.xml.AbstractDocumentReader;

/**
 * Element reader.
 */
@NotThreadSafe
final class ElementReader
    extends AbstractDocumentReader
{
    /**
     * Constructs an instance.
     *
     * @param rootElement The root element.
     */
    ElementReader(@Nonnull final XMLElement rootElement)
    {
        super(Require.notNull(rootElement));
    }

    /** {@inheritDoc}
     */
    @Override
    public String getAttribute(final int index)
    {
        if (_currentAttributes == null) {
            _currentAttributes = _currentElement.getAttributes();
        }

        return _currentAttributes.get(index).getValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getAttribute(final String name)
    {
        return _currentElement
            .getAttributeValue(name, Optional.empty())
            .orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getAttributeCount()
    {
        return _currentElement.getAttributeCount();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getAttributeName(final int index)
    {
        if (_currentAttributes == null) {
            _currentAttributes = _currentElement.getAttributes();
        }

        return _currentAttributes.get(index).getName();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getNodeName()
    {
        return _currentElement.getName();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getValue()
    {
        return _currentElement.getText();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Object getChild(final int index)
    {
        _elementStack.push(_currentElement);

        return _currentElement.getChild(index);
    }

    /** {@inheritDoc}
     */
    @Override
    protected int getChildCount()
    {
        return _currentElement.getChildCount();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Object getParent()
    {
        return _elementStack.pop();
    }

    /** {@inheritDoc}
     */
    @Override
    protected void reassignCurrentElement(final Object current)
    {
        _currentElement = (XMLElement) current;
        _currentAttributes = null;
    }

    private List<XMLAttribute> _currentAttributes;
    private XMLElement _currentElement;
    private final FastStack _elementStack = new FastStack(4);
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
