/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: XMLElement.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.base.xml;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.tool.Require;

/**
 * XML element.
 *
 * <p>Instances of this class are not thread safe.</p>
 */
@NotThreadSafe
public class XMLElement
{
    /**
     * Constructs an instance.
     *
     * <p>Note: this constructor is needed by some script langauges.</p>
     *
     * @param name The element's name.
     */
    public XMLElement(@Nonnull final String name)
    {
        setName(name);
    }

    /**
     * Constructs an instance.
     *
     * @param name The element's name.
     * @param contents The element's contents.
     */
    public XMLElement(
            @Nonnull final String name,
            @Nonnull final Object... contents)
    {
        this(name);

        for (final Object content: contents) {
            if (content instanceof XMLElement) {
                addChild((XMLElement) content);
            } else if (content instanceof XMLAttribute) {
                setAttribute((XMLAttribute) content);
            } else if (content != null) {
                addText(content.toString());
            }
        }
    }

    private XMLElement(final XMLElement other)
    {
        _name = other._name;
        _parent = other._parent;
        _path = other._path;

        if (other._attributeMap != null) {
            _attributeMap = new LinkedHashMap<>(other._attributeMap);
        }

        if (other._children != null) {
            _children = new ArrayList<>(other._children);
        }

        if (other._text != null) {
            _text = new StringBuilder(other._text.toString());
        }
    }

    /**
     * Escapes protected characters.
     *
     * @param text The source text.
     * @param quote The quote character ('"', '\'' or '\0').
     *
     * @return The encoded text.
     */
    @Nonnull
    @CheckReturnValue
    public static String escape(
            @Nonnull final CharSequence text,
            final char quote)
    {
        final StringBuilder stringBuilder = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); ++i) {
            final char next = text.charAt(i);
            final String entity;

            switch (next) {
                case '<': {
                    entity = "&lt;";

                    break;
                }
                case '>': {
                    entity = "&gt;";

                    break;
                }
                case '&': {
                    entity = "&amp;";

                    break;
                }
                case '"': {
                    entity = (quote == next)? "&quot;": null;

                    break;
                }
                case '\'': {
                    entity = (quote == next)? "&apos;": null;

                    break;
                }
                case '\t':
                case '\n':
                case '\r': {
                    entity = null;

                    break;
                }
                default: {
                    entity = (next < ' ')? ("&#" + (int) next + ";"): null;

                    break;
                }
            }

            if (entity != null) {
                stringBuilder.append(entity);
            } else {
                stringBuilder.append(next);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Adds a child.
     *
     * @param name The name of the child.
     *
     * @return The newly created child.
     */
    @Nonnull
    public XMLElement addChild(@Nonnull final String name)
    {
        final XMLElement child = getFactory().newXMLElement(name);

        addChild(child);

        return child;
    }

    /**
     * Adds a child.
     *
     * @param child The child.
     */
    public void addChild(@Nonnull final XMLElement child)
    {
        child._parent = this;

        if (_children == null) {
            _children = new ArrayList<>(4);
        }

        _children.add(child);
        child.setPath(_path);
    }

    /**
     * Adds the element's text.
     *
     * @param text Some text.
     */
    public final void addText(@Nonnull final String text)
    {
        if (_text == null) {
            if (text.trim().isEmpty()) {
                return;
            }

            _text = new StringBuilder();
        }

        _text.append(text);
    }

    /**
     * Asks if this element contains a specified attribute.
     *
     * @param name The name of the attribute.
     *
     * @return True if the attribute is present.
     */
    @CheckReturnValue
    public boolean containsAttribute(@Nonnull final String name)
    {
        return (_attributeMap != null)? _attributeMap.containsKey(name): false;
    }

    /**
     * Creates a copy of this element.
     *
     * @return The copy.
     */
    @Nonnull
    @CheckReturnValue
    public XMLElement copy()
    {
        return new XMLElement(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }

        final XMLElement otherElement = (XMLElement) other;

        if (!_name.equals(otherElement._name)) {
            return false;
        }

        if (_text != null) {
            if (!(_text.equals(otherElement._text))) {
                return false;
            }
        } else if (otherElement._text != null) {
            return false;
        }

        if (_attributeMap != null) {
            if (_attributeMap.size() != otherElement._attributeMap.size()) {
                for (final Map.Entry<String, String> entry:
                        _attributeMap.entrySet()) {
                    if (!(entry
                        .getValue()
                        .equals(
                            otherElement._attributeMap.get(entry.getKey())))) {
                        return false;
                    }
                }
            }
        } else if (otherElement._attributeMap != null) {
            return false;
        }

        if (_children != null) {
            if (!(_children.equals(otherElement._children))) {
                return false;
            }
        } else if (otherElement._children != null) {
            return false;
        }

        return true;
    }

    /**
     * Gets the attributes count.
     *
     * @return The attribute count.
     */
    @CheckReturnValue
    public final int getAttributeCount()
    {
        return (_attributeMap != null)? _attributeMap.size(): 0;
    }

    /**
     * Gets an attribute's value by its name.
     *
     * @param name The name of the attribute.
     * @param defaultValue The value to return if the attribute is not present.
     *
     * @return The value of the attribute or its default.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getAttributeValue(
            @Nonnull final String name,
            @Nonnull final Optional<String> defaultValue)
    {
        final String value = (_attributeMap != null)? _attributeMap
            .get(Require.notNull(name)): null;

        return (value != null)? Optional.of(value): defaultValue;
    }

    /**
     * Gets the element's attributes.
     *
     * @return The element's attributes.
     */
    @Nonnull
    @CheckReturnValue
    public final List<XMLAttribute> getAttributes()
    {
        if (_attributeList == null) {
            if (_attributeMap != null) {
                _attributeList = new ArrayList<>(_attributeMap.size());

                _attributeMap
                    .forEach(
                        (key, value) -> {
                            _attributeList.add(new XMLAttribute(key, value));
                        });
            } else {
                _attributeList = new ArrayList<>(0);
            }
        }

        return _attributeList;
    }

    /**
     * Gets the element's child at an index.
     *
     * @param index An origin 0 index;
     *
     * @return The element's child.
     */
    @Nonnull
    @CheckReturnValue
    public XMLElement getChild(final int index)
    {
        if (_children == null) {
            throw new IndexOutOfBoundsException();
        }

        return _children.get(index);
    }

    /**
     * Gets the child count.
     *
     * @return The child count.
     */
    @CheckReturnValue
    public final int getChildCount()
    {
        return (_children != null)? _children.size(): 0;
    }

    /**
     * Gets the element's child names.
     *
     * @return The child names.
     */
    @Nonnull
    @CheckReturnValue
    public Set<? extends String> getChildNames()
    {
        final Set<String> names = new LinkedHashSet<>();

        if (_children != null) {
            for (final XMLElement child: _children) {
                names.add(child.getName());
            }
        }

        return names;
    }

    /**
     * Gets the element's children.
     *
     * @return The element's children.
     */
    @Nonnull
    @CheckReturnValue
    public List<? extends XMLElement> getChildren()
    {
        if (_children == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(_children);
    }

    /**
     * Gets the element's children with a specific name.
     *
     * @param name The children's name.
     *
     * @return The element's children.
     */
    @Nonnull
    @CheckReturnValue
    public List<? extends XMLElement> getChildren(@Nonnull final String name)
    {
        final List<XMLElement> children = new LinkedList<>();

        if (_children != null) {
            for (final XMLElement child: _children) {
                if (child.getName().equals(name)) {
                    children.add(child);
                }
            }
        }

        return children;
    }

    /**
     * Gets the xml element factory.
     *
     * @return The xml element factory.
     */
    @Nonnull
    @CheckReturnValue
    public Factory getFactory()
    {
        return _DEFAULT_FACTORY;
    }

    /**
     * Gets the first child element with a specific name.
     *
     * @param name The child name.
     *
     * @return The optional child element.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<XMLElement> getFirstChild(@Nonnull final String name)
    {
        if (_children != null) {
            for (final XMLElement child: _children) {
                if (child.getName().equals(name)) {
                    return Optional.of(child);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Gets this element's name.
     *
     * @return The element's name.
     */
    @Nonnull
    @CheckReturnValue
    public final String getName()
    {
        Require.failure(_name.isEmpty());

        return _name;
    }

    /**
     * Gets the parent.
     *
     * @return The parent (empty if this is the root).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<? extends XMLElement> getParent()
    {
        return Optional.ofNullable(_parent);
    }

    /**
     * Gets the path.
     *
     * @return The path.
     */
    @Nonnull
    @CheckReturnValue
    public final String getPath()
    {
        Require.failure(_name.isEmpty());

        return _path;
    }

    /**
     * Gets the element's text.
     *
     * @return The element's text.
     */
    @Nonnull
    @CheckReturnValue
    public String getText()
    {
        return (_text != null)? _text.toString().trim(): "";
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Asks if the name is empty.
     *
     * @return True if the name is empty.
     */
    @CheckReturnValue
    public final boolean isNameEmpty()
    {
        return (_name != null) && _name.isEmpty();
    }

    /**
     * Removes an element's attribute.
     *
     * @param attributeName The attribute name.
     */
    public final void removeAttribute(@Nonnull final String attributeName)
    {
        if (_attributeMap != null) {
            _attributeMap.remove(attributeName);
        }
    }

    /**
     * Sets an element's attribute.
     *
     * @param attribute The attribute.
     */
    public final void setAttribute(@Nonnull final XMLAttribute attribute)
    {
        setAttribute(attribute.getName(), attribute.getValue());
    }

    /**
     * Sets an element's attribute.
     *
     * @param attributeName The attribute name.
     * @param attributeValue The attribute value.
     */
    public final void setAttribute(
            @Nonnull final String attributeName,
            final boolean attributeValue)
    {
        setAttribute(
            attributeName,
            attributeValue? _TRUE: _FALSE);
    }

    /**
     * Sets an element's attribute.
     *
     * @param attributeName The attribute name.
     * @param attributeValue The attribute value (empty means remove).
     */
    public final void setAttribute(
            @Nonnull final String attributeName,
            @Nonnull final Optional<String> attributeValue)
    {
        if (attributeValue.isPresent()) {
            setAttribute(attributeName, attributeValue.get());
        } else if (_attributeMap != null) {
            removeAttribute(attributeName);
        }
    }

    /**
     * Sets an element's attribute.
     *
     * @param attributeName The attribute name.
     * @param attributeValue The attribute value.
     */
    public final void setAttribute(
            @Nonnull final String attributeName,
            @Nonnull final String attributeValue)
    {
        _attributeList = null;

        if (_attributeMap == null) {
            _attributeMap = new LinkedHashMap<>();
        }

        _attributeMap
            .put(
                Require.notNull(attributeName),
                Require.notNull(attributeValue));
    }

    /**
     * Sets the element's name.
     *
     * @param elementName The element's name.
     */
    public final void setName(@Nonnull final String elementName)
    {
        Require.success((_name == null) || _name.isEmpty());

        _name = elementName.trim();
        _path = '/' + _name;
    }

    /** {@inheritDoc}
     */
    @Override
    public final String toString()
    {
        final StringBuilder stringBuilder = new StringBuilder();

        try {
            toXML(0, stringBuilder);
        } catch (final IOException exception) {
            throw new InternalError(exception);
        }

        return stringBuilder.toString();
    }

    /**
     * Disowns the children.
     */
    protected final void disownChildren()
    {
        if (_children != null) {
            _children.clear();
        }
    }

    /**
     * Clears the name to indicates that the root element has not been seen
     * yet.
     */
    final void clearName()
    {
        _name = null;
        _path = null;
    }

    /**
     * Asks if the name is null.
     *
     * @return True if the name is null.
     */
    @CheckReturnValue
    final boolean isNameNull()
    {
        return _name == null;
    }

    /**
     * Sets the path with the supplied prefix.
     *
     * @param prefix The prefix.
     */
    void setPath(@Nonnull final String prefix)
    {
        _path = prefix + '/' + _name;

        if (_children != null) {
            for (final XMLElement element: _children) {
                element.setPath(_path);
            }
        }
    }

    /**
     * Converts this to XML.
     *
     * @param level The indentation level.
     * @param destination The destination.
     *
     * @throws IOException From appendable.
     */
    final void toXML(
            final int level,
            @Nonnull final Appendable destination)
        throws IOException
    {
        for (int i = 0; i < level; ++i) {
            destination.append(_INDENT);
        }

        destination.append('<');
        destination.append(getName());

        if (_attributeMap != null) {
            for (final Map.Entry<String, String> entry:
                    _attributeMap.entrySet()) {
                destination.append(' ');
                destination.append(entry.getKey());
                destination.append("='");
                destination.append(escape(entry.getValue(), '\''));
                destination.append("'");
            }
        }

        if ((_children != null) || (_text != null)) {
            if (_children != null) {
                destination.append(">\n");

                for (final XMLElement element: _children) {
                    element.toXML((level < 0)? level: (level + 1), destination);
                }

                for (int i = 0; i < level; ++i) {
                    destination.append(_INDENT);
                }
            } else {
                destination.append('>');
            }

            if (_text != null) {
                if (_children != null) {
                    destination.append(_INDENT);
                }

                destination.append(escape(_text.toString().trim(), '\0'));

                if (_children != null) {
                    destination.append('\n');

                    for (int i = 0; i < level; ++i) {
                        destination.append(_INDENT);
                    }
                }
            }

            destination.append("</");
            destination.append(getName());
            destination.append(">\n");
        } else {
            destination.append("/>\n");
        }
    }

    private static final Factory _DEFAULT_FACTORY;
    private static final String _FALSE = "0";
    private static final String _INDENT = "    ";
    private static final String _TRUE = "1";

    static {
        _DEFAULT_FACTORY = new Factory()
        {
            @Override
            public XMLElement newXMLElement(final String name)
            {
                return new XMLElement(name);
            }
        };
    }

    private List<XMLAttribute> _attributeList;
    private Map<String, String> _attributeMap;
    private List<XMLElement> _children;
    private String _name;
    private XMLElement _parent;
    private String _path;
    private StringBuilder _text;

    /**
     * Factory.
     */
    public interface Factory
    {
        /**
         * Returns a new element.
         *
         * @param name The element's name.
         *
         * @return The element.
         */
        @Nonnull
        @CheckReturnValue
        XMLElement newXMLElement(@Nonnull String name);
    }


    /**
     * Handler.
     */
    public interface Handler
    {
        /**
         * Called on element end.
         *
         * @param element The element.
         *
         * @return The supplied element, a substitute or null.
         *
         * @throws XMLDocument.ParseException When appropriate.
         */
        @CheckForNull
        XMLElement onElementEnd(
                @Nonnull final XMLElement element)
            throws XMLDocument.ParseException;

        /**
         * Called on element start.
         *
         * @param element The element.
         *
         * @return The supplied element, a substitute or null.
         *
         * @throws XMLDocument.ParseException When appropriate.
         */
        @CheckForNull
        XMLElement onElementStart(
                @Nonnull final XMLElement element)
            throws XMLDocument.ParseException;
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
