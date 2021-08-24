/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JSONSupport.java 4005 2019-05-18 15:52:45Z SFB $
 */

package org.rvpf.base.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import java.net.URL;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.UnicodeStreamReader;

/**
 * JSON support.
 */
public final class JSONSupport
{
    /**
     * Static methods only.
     */
    private JSONSupport() {}

    /**
     * Loads an XML document from a JSON file.
     *
     * @param file The JSON file.
     *
     * @return The XML document (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public static XMLDocument load(@Nonnull final File file)
    {
        final Reader reader;

        try {
            reader = new UnicodeStreamReader(file);
        } catch (final FileNotFoundException exception) {
            Logger
                .getInstance(XMLDocument.class)
                .warn(BaseMessages.FILE_NOT_FOUND, file);

            return null;
        }

        final XMLElement rootElement;

        try {
            rootElement = parse(reader);
        } catch (final XMLDocument.ParseException exception) {
            final _ParseException parseException = (_ParseException) exception
                .getCause();
            final JsonLocation location = parseException.getLocation();

            Logger
                .getInstance(XMLDocument.class)
                .warn(
                    BaseMessages.JSON_PARSE_FAILED,
                    file,
                    Long.valueOf(location.getLineNumber()),
                    Long.valueOf(location.getColumnNumber()),
                    parseException.getText());

            return null;
        }

        return new XMLDocument(rootElement);
    }

    /**
     * Loads an XML document from a JSON URL.
     *
     * @param url The JSON URL.
     * @param encoding The optional character encoding name.
     *
     * @return The XML document (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public static XMLDocument load(
            @Nonnull final URL url,
            @Nonnull final Optional<String> encoding)
    {
        final XMLElement rootElement;

        try {
            rootElement = parse(url);
        } catch (final XMLDocument.ParseException exception) {
            final _ParseException parseException = (_ParseException) exception
                .getCause();
            final JsonLocation location = parseException.getLocation();

            Logger
                .getInstance(XMLDocument.class)
                .warn(
                    BaseMessages.JSON_PARSE_FAILED,
                    url,
                    Long.valueOf(location.getLineNumber()),
                    Long.valueOf(location.getColumnNumber()),
                    parseException.getText());

            return null;
        }

        return new XMLDocument(rootElement);
    }

    /**
     * Parses JSON from a file.
     *
     * @param file The JSON file.
     *
     * @return The root element.
     *
     * @throws XMLDocument.ParseException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    public static XMLElement parse(
            @Nonnull final File file)
        throws XMLDocument.ParseException
    {
        try {
            return parse(new FileInputStream(file));
        } catch (final FileNotFoundException exception) {
            throw new XMLDocument.ParseException(exception);
        }
    }

    /**
     * Parses JSON from an input stream.
     *
     * @param inputStream An input stream for the JSON text.
     *
     * @return The root element.
     *
     * @throws XMLDocument.ParseException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    public static XMLElement parse(
            @Nonnull final InputStream inputStream)
        throws XMLDocument.ParseException
    {
        try (final JsonParser parser = Json.createParser(inputStream)) {
            return _parse(parser);
        }
    }

    /**
     * Parses JSON from a reader.
     *
     * @param reader A reader for the JSON text.
     *
     * @return The root element.
     *
     * @throws XMLDocument.ParseException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    public static XMLElement parse(
            @Nonnull final Reader reader)
        throws XMLDocument.ParseException
    {
        try (final JsonParser parser = Json.createParser(reader)) {
            return _parse(parser);
        }
    }

    /**
     * Parses JSON from a file.
     *
     * @param string The JSON string.
     *
     * @return The root element.
     *
     * @throws XMLDocument.ParseException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    public static XMLElement parse(
            @Nonnull final String string)
        throws XMLDocument.ParseException
    {
        try (final JsonParser parser = Json.createParser(
                new StringReader(string))) {
            return _parse(parser);
        }
    }

    /**
     * Parses JSON from an URL.
     *
     * @param url The URL of the JSON text.
     *
     * @return The root element.
     *
     * @throws XMLDocument.ParseException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    public static XMLElement parse(
            @Nonnull final URL url)
        throws XMLDocument.ParseException
    {
        try {
            return _parse(Json.createParser(url.openStream()));
        } catch (final IOException exception) {
            throw new XMLDocument.ParseException(exception);
        }
    }

    /**
     * Converts an XML document to a JSON string.
     *
     * @param source The XML document.
     * @return The JSON string.
     */
    @Nonnull
    @CheckReturnValue
    public static String toJSON(@Nonnull final XMLDocument source)
    {
        final StringWriter stringWriter = new StringWriter();

        try {
            toJSON(source, stringWriter);
        } catch (final IOException exception) {
            throw new InternalError(exception);
        }

        return stringWriter.toString();
    }

    /**
     * Converts an XML document to JSON writer.
     *
     * @param source The XML document.
     * @param writer The JSON writer.
     *
     * @throws IOException From the destination.
     */
    public static void toJSON(
            @Nonnull final XMLDocument source,
            @Nonnull final Writer writer)
        throws IOException
    {
        try (final JsonGenerator generator = Json.createGenerator(writer)) {
            final XMLElement rootElement = source.getRootElement();

            generator.writeStartObject();
            generator.writeStartObject(rootElement.getName());
            _toJSON(rootElement, generator);
            generator.writeEnd();
            generator.writeEnd();
        }
    }

    private static XMLElement _parse(
            final JsonParser parser)
        throws XMLDocument.ParseException
    {
        _ParserState currentState = new _ParserState(null, null, null, null);

        try {
            while (parser.hasNext()) {
                final JsonParser.Event event = parser.next();

                switch (event) {
                    case START_OBJECT: {
                        if (!currentState.isNull()) {
                            final String name = currentState.getName();

                            currentState = new _ParserState(
                                event,
                                currentState,
                                name,
                                new XMLElement(name));
                        }

                        break;
                    }
                    case END_ARRAY: {
                        if (currentState.getEvent()
                                != JsonParser.Event.START_ARRAY) {
                            throw new _ParseException(
                                "unexpected end of array");
                        }

                        currentState = currentState.getParent();

                        break;
                    }
                    case END_OBJECT: {
                        if (currentState.getEvent()
                                != JsonParser.Event.START_OBJECT) {
                            throw new _ParseException(
                                "unexpected end of object");
                        }

                        final XMLElement element = currentState.getElement();

                        currentState = currentState.getParent();

                        if (currentState.isNull()) {
                            return element;
                        }

                        currentState.getElement().addChild(element);

                        break;
                    }
                    case KEY_NAME: {
                        currentState = new _ParserState(
                            event,
                            currentState,
                            parser.getString(),
                            null);

                        break;
                    }
                    case START_ARRAY: {
                        currentState = new _ParserState(
                            event,
                            currentState,
                            null,
                            null);

                        break;
                    }
                    case VALUE_FALSE: {
                        currentState
                            .getElement()
                            .setAttribute(currentState.getName(), false);
                        currentState = currentState.getParent();

                        break;
                    }
                    case VALUE_NULL: {
                        throw new _ParseException("unsupported null value");
                    }
                    case VALUE_NUMBER: {
                        currentState
                            .getElement()
                            .setAttribute(
                                currentState.getName(),
                                parser.getString());
                        currentState = currentState.getParent();

                        break;
                    }
                    case VALUE_STRING: {
                        final XMLElement element = currentState.getElement();

                        if (currentState.getName().isEmpty()) {
                            element.addText(parser.getString());
                        } else {
                            element
                                .setAttribute(
                                    currentState.getName(),
                                    parser.getString());
                        }

                        currentState = currentState.getParent();

                        break;
                    }
                    case VALUE_TRUE: {
                        currentState
                            .getElement()
                            .setAttribute(currentState.getName(), true);
                        currentState = currentState.getParent();

                        break;
                    }
                    default: {
                        throw new _ParseException(
                            "unexpected event '" + event.name() + "'");
                    }
                }
            }

            throw new _ParseException("unexpected end of input");
        } catch (final _ParseException exception) {
            exception.setLocation(parser.getLocation());

            throw new XMLDocument.ParseException(exception);
        }
    }

    private static void _toJSON(
            final XMLElement element,
            final JsonGenerator generator)
    {
        for (final XMLAttribute attribute: element.getAttributes()) {
            generator.write(attribute.getName(), attribute.getValue());
        }

        for (final String name: element.getChildNames()) {
            final List<? extends XMLElement> children = element
                .getChildren(name);

            if (children.size() == 1) {
                generator.writeStartObject(name);
                _toJSON(children.get(0), generator);
                generator.writeEnd();
            } else {
                generator.writeStartArray(name);

                for (final XMLElement child: children) {
                    generator.writeStartObject();
                    _toJSON(child, generator);
                    generator.writeEnd();
                }

                generator.writeEnd();
            }
        }

        final String text = element.getText();

        if (!text.isEmpty()) {
            generator.write("", text);
        }
    }

    private static class _ParseException
        extends Exception
    {
        /**
         * Constructs an instance.
         *
         * @param text Explanatory text.
         */
        _ParseException(final String text)
        {
            _text = text;
        }

        /** {@inheritDoc}
         */
        @Override
        public String getMessage()
        {
            final JsonLocation location = getLocation();

            return _text + " at " + location.getLineNumber() + ':'
                   + location.getColumnNumber();
        }

        /**
         * Gets the location.
         *
         * @return The location.
         */
        JsonLocation getLocation()
        {
            return _location.get();
        }

        /**
         * Gets the text.
         *
         * @return The text.
         */
        String getText()
        {
            return _text;
        }

        /**
         * Sets the location.
         *
         * @param location The location.
         */
        void setLocation(final JsonLocation location)
        {
            _location.set(Require.notNull(location));
        }

        private static final long serialVersionUID = 1L;

        private final AtomicReference<JsonLocation> _location =
            new AtomicReference<JsonLocation>();
        private final String _text;
    }


    private static class _ParserState
    {
        /**
         * Constructs an instance.
         *
         * @param event The JSON parse event.
         * @param parent The parent state (null at root).
         * @param name The new name.
         * @param element The new element.
         */
        _ParserState(
                final JsonParser.Event event,
                final _ParserState parent,
                final String name,
                final XMLElement element)
        {
            _event = event;
            _parent = parent;
            _name = (name != null)
                    ? name: (_parent != null)? _parent._name: null;
            _element = (element != null)
                       ? element: (_parent != null)? _parent._element: null;
        }

        /**
         * Gets the element.
         *
         * @return The element.
         *
         * @throws _ParseException When there is no element.
         */
        XMLElement getElement()
            throws _ParseException
        {
            if (_element == null) {
                throw new _ParseException("no object");
            }

            return _element;
        }

        /**
         * Gets the event.
         *
         * @return The event.
         */
        JsonParser.Event getEvent()
        {
            return _event;
        }

        /**
         * Gets the name.
         *
         * @return The name.
         *
         * @throws _ParseException When there is no name.
         */
        String getName()
            throws _ParseException
        {
            if (_name == null) {
                throw new _ParseException("no name");
            }

            return _name;
        }

        /**
         * Gets the parent.
         *
         * @return The parent.
         *
         * @throws _ParseException When there is no parent.
         */
        _ParserState getParent()
            throws _ParseException
        {
            _ParserState parent = _parent;

            if (parent == null) {
                throw new _ParseException("no parent");
            }

            if (parent.getEvent() == JsonParser.Event.KEY_NAME) {
                parent = parent.getParent();
            }

            return parent;
        }

        /**
         * Asks if this is the null state.
         *
         * @return True if this is the null state.
         */
        boolean isNull()
        {
            return _parent == null;
        }

        private final XMLElement _element;
        private final JsonParser.Event _event;
        private final String _name;
        private final _ParserState _parent;
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
