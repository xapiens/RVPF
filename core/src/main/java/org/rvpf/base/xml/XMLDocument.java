/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: XMLDocument.java 4005 2019-05-18 15:52:45Z SFB $
 */

package org.rvpf.base.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import javax.xml.parsers.SAXParserFactory;

import org.apache.xml.resolver.tools.CatalogResolver;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.UnicodeStreamReader;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

/**
 * XML document.
 *
 * <p>Instances of this class are not thread safe.</p>
 */
@NotThreadSafe
public final class XMLDocument
    implements XMLElement.Factory
{
    /**
     * Constructs an instance.
     */
    public XMLDocument() {}

    /**
     * Constructs an instance.
     *
     * @param name The root element's name.
     */
    public XMLDocument(@Nonnull final String name)
    {
        setRootElement(name);
    }

    /**
     * Constructs an instance.
     *
     * @param rootElement The root element.
     */
    public XMLDocument(@Nonnull final XMLElement rootElement)
    {
        setRootElement(Optional.of(rootElement));
    }

    /**
     * Loads an XML document from a file.
     *
     * @param file The file.
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

        final XMLDocument xmlDocument = new XMLDocument();

        try {
            xmlDocument.parse(reader);
        } catch (final XMLDocument.ParseException exception) {
            Logger
                .getInstance(XMLDocument.class)
                .warn(
                    BaseMessages.XML_PARSE_FAILED,
                    file,
                    exception.getCause());

            return null;
        } finally {
            try {
                reader.close();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        return xmlDocument;
    }

    /**
     * Loads an XML document from a URL.
     *
     * @param url The URL.
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
        final XMLDocument xmlDocument = new XMLDocument();

        try {
            xmlDocument.parse(url, encoding);
        } catch (final XMLDocument.ParseException exception) {
            Logger
                .getInstance(XMLDocument.class)
                .warn(BaseMessages.XML_PARSE_FAILED, url, exception.getCause());

            return null;
        }

        return xmlDocument;
    }

    /**
     * Creates a new element.
     *
     * @param name The element's name.
     *
     * @return The element.
     */
    @Nonnull
    @CheckReturnValue
    public XMLElement createXMLElement(@Nonnull final String name)
    {
        return getElementFactory().newXMLElement(Require.notNull(name));
    }

    /**
     * Gets the element factory.
     *
     * @return The element factory.
     */
    @Nonnull
    @CheckReturnValue
    public XMLElement.Factory getElementFactory()
    {
        return (_elementFactory != null)? _elementFactory: this;
    }

    /**
     * Gets the root element.
     *
     * @return The root element.
     */
    @Nonnull
    @CheckReturnValue
    public XMLElement getRootElement()
    {
        return _rootElement;
    }

    /** {@inheritDoc}
     */
    @Override
    @Nonnull
    @CheckReturnValue
    public XMLElement newXMLElement(@Nonnull final String name)
    {
        return new XMLElement(name);
    }

    /**
     * Parses XML from a reader.
     *
     * @param reader A reader for the XML text.
     *
     * @return The root element.
     *
     * @throws ParseException When appropriate.
     */
    @Nonnull
    public XMLElement parse(@Nonnull final Reader reader)
        throws ParseException
    {
        return _parse(new InputSource(reader));
    }

    /**
     * Parses XML from a string.
     *
     * @param string The XML string.
     *
     * @return The root element.
     *
     * @throws ParseException When appropriate.
     */
    @Nonnull
    public XMLElement parse(@Nonnull final String string)
        throws ParseException
    {
        return parse(new StringReader(string));
    }

    /**
     * Parses XML from a file.
     *
     * @param file The XML file.
     * @param encoding The optional character encoding name.
     *
     * @return The root element.
     *
     * @throws ParseException When appropriate.
     */
    @Nonnull
    public XMLElement parse(
            @Nonnull final File file,
            @Nonnull final Optional<String> encoding)
        throws ParseException
    {
        return _parse(file.toURI().toString(), encoding);
    }

    /**
     * Parses XML from an input stream.
     *
     * @param inputStream An input stream for the XML text.
     * @param encoding The optional character encoding name.
     *
     * @return The root element.
     *
     * @throws ParseException When appropriate.
     */
    @Nonnull
    public XMLElement parse(
            @Nonnull final InputStream inputStream,
            @Nonnull final Optional<String> encoding)
        throws ParseException
    {
        final InputSource inputSource = new InputSource(inputStream);

        if (encoding.isPresent()) {
            inputSource.setEncoding(encoding.get());
        }

        return _parse(inputSource);
    }

    /**
     * Parses XML from an URL.
     *
     * @param url The URL of the XML text.
     * @param encoding The character encoding name (may be empty).
     *
     * @return The root element.
     *
     * @throws ParseException When appropriate.
     */
    @Nonnull
    public XMLElement parse(
            @Nonnull final URL url,
            @Nonnull final Optional<String> encoding)
        throws ParseException
    {
        return _parse(url.toString(), encoding);
    }

    /**
     * Sets the default handler.
     *
     * @param handler The default handler.
     */
    public void setDefaultHandler(
            @Nonnull final Optional<XMLElement.Handler> handler)
    {
        _defaultHandler = handler.orElse(null);
    }

    /**
     * Sets the document type strings.
     *
     * @param strings An array holding the public and system strings.
     */
    public void setDocTypeStrings(@Nonnull final String[] strings)
    {
        setDocTypeStrings(
            Optional.ofNullable(strings[0]),
            Optional.ofNullable(strings[1]));
    }

    /**
     * Sets the document type strings.
     *
     * @param publicString The optional public string.
     * @param systemString The optional system string.
     */
    public void setDocTypeStrings(
            @Nonnull final Optional<String> publicString,
            @Nonnull final Optional<String> systemString)
    {
        _publicString = publicString.orElse(null);
        _systemString = systemString.orElse(null);
    }

    /**
     * Sets the element factory.
     *
     * @param elementFactory The element factory.
     */
    public void setElementFactory(
            @Nonnull final XMLElement.Factory elementFactory)
    {
        _elementFactory = Require.notNull(elementFactory);
    }

    /**
     * Sets an element handler.
     *
     * @param elementPath The element path.
     * @param handler The element handler (empty means remove).
     */
    public void setElementHandler(
            @Nonnull final String elementPath,
            @Nonnull final Optional<XMLElement.Handler> handler)
    {
        if (handler.isPresent()) {
            _elementHandlers.put(elementPath, handler.get());
        } else {
            _elementHandlers.remove(elementPath);
        }
    }

    /**
     * Sets the processing instruction handler.
     *
     * @param piHandler The optional processing instruction handler.
     */
    public void setPIHandler(@Nonnull final Optional<PIHandler> piHandler)
    {
        _piHandler = piHandler.orElse(null);
    }

    /**
     * Sets the root element.
     *
     * @param rootElement The root element.
     */
    public void setRootElement(@Nonnull final Optional<XMLElement> rootElement)
    {
        _rootElement = rootElement.orElse(null);
    }

    /**
     * Sets the root element.
     *
     * @param name The root element's name.
     */
    public void setRootElement(@Nonnull final String name)
    {
        setRootElement(Optional.of(createXMLElement(name)));
    }

    /**
     * Sets the root handler.
     *
     * @param handler The optional root handler.
     */
    public void setRootHandler(
            @Nonnull final Optional<XMLElement.Handler> handler)
    {
        _rootHandler = handler.orElse(null);
    }

    /**
     * Sets the validating indicator.
     *
     * @param validating The validating indicator.
     */
    public void setValidating(final boolean validating)
    {
        _validating = validating;
    }

    /**
     * Sets the XML reader.
     *
     * @param xmlReader The optional XML reader.
     */
    public void setXMLReader(@Nonnull final Optional<XMLReader> xmlReader)
    {
        _xmlReader = xmlReader.orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return toXML(Optional.empty(), true);
    }

    /**
     * Returns this as a XML string.
     *
     * @param encoding The optional encoding identification for the XML header.
     * @param indented True requests indented output.
     *
     * @return The XML string.
     */
    @Nonnull
    @CheckReturnValue
    public String toXML(
            @Nonnull final Optional<String> encoding,
            final boolean indented)
    {
        final StringBuilder stringBuilder = new StringBuilder();

        try {
            toXML(encoding, indented, stringBuilder);
        } catch (final IOException exception) {
            throw new InternalError(exception);
        }

        return stringBuilder.toString();
    }

    /**
     * Converts this to XML.
     *
     * @param encoding The optional encoding identification for the XML header.
     * @param indented True requests indented output.
     * @param destination The destination.
     *
     * @throws IOException From the destination.
     */
    public void toXML(
            @Nonnull final Optional<String> encoding,
            final boolean indented,
            @Nonnull final Appendable destination)
        throws IOException
    {
        destination.append("<?xml version='1.0'");

        if (encoding.isPresent()) {
            final String trimmedEncoding = encoding.get().trim();

            if (trimmedEncoding.length() > 0) {
                destination.append(" encoding='");
                destination.append(trimmedEncoding);
                destination.append("'");
            }
        }

        destination.append("?>\n");

        if (_rootElement != null) {
            destination.append("<!DOCTYPE ");
            destination.append(_rootElement.getName());

            if (_publicString != null) {
                destination.append(" PUBLIC \"");
                destination.append(_publicString);

                if (_systemString != null) {
                    destination.append("\" '");
                    destination.append(_systemString);
                    destination.append("'");
                } else {
                    destination.append("\"");
                }
            } else if (_systemString != null) {
                destination.append(" SYSTEM '");
                destination.append(_systemString);
                destination.append("'");
            }

            destination.append(">\n");

            _rootElement.toXML(indented? 0: -1, destination);
        }
    }

    /**
     * Gets an element handler.
     *
     * @param elementPath The element path.
     *
     * @return The element handler.
     */
    @Nonnull
    @CheckReturnValue
    XMLElement.Handler _getElementHandler(@Nonnull final String elementPath)
    {
        final XMLElement.Handler handler = _elementHandlers.get(elementPath);

        return (handler != null)? handler: _defaultHandler;
    }

    /**
     * Gets the processing instruction handler.
     *
     * @return The optional processing instruction handler.
     */
    @Nonnull
    @CheckReturnValue
    Optional<PIHandler> _getPIHandler()
    {
        return Optional.ofNullable(_piHandler);
    }

    /**
     * Gets the root handler.
     *
     * @return The optional root handler.
     */
    @Nonnull
    @CheckReturnValue
    Optional<XMLElement.Handler> _getRootHandler()
    {
        return Optional.ofNullable(_rootHandler);
    }

    private XMLElement _parse(
            final InputSource inputSource)
        throws ParseException
    {
        if (_xmlReader == null) {
            if (_parserFactory == null) {
                _parserFactory = SAXParserFactory.newInstance();
            }

            _parserFactory.setNamespaceAware(true);
            _parserFactory.setValidating(_validating);

            try {
                _xmlReader = _parserFactory.newSAXParser().getXMLReader();
            } catch (final RuntimeException exception) {
                throw exception;
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        _rootElement = createXMLElement("");

        final DefaultHandler2 xmlHandler = new _XMLHandler();

        _xmlReader.setEntityResolver(new CatalogResolver());
        _xmlReader.setContentHandler(xmlHandler);
        _xmlReader.setErrorHandler(xmlHandler);

        try {
            _xmlReader.parse(inputSource);
        } catch (final IOException exception) {
            throw new ParseException(exception);
        } catch (final SAXException exception) {
            final Throwable cause = exception.getCause();

            if (cause instanceof ParseException) {
                throw (ParseException) cause;
            } else if (cause != null) {
                throw new ParseException(cause);
            }

            throw new ParseException(exception);
        }

        return _rootElement;
    }

    private XMLElement _parse(
            final String systemId,
            final Optional<String> encoding)
        throws ParseException
    {
        final InputSource inputSource = new InputSource(systemId);

        if (encoding.isPresent()) {
            inputSource.setEncoding(encoding.get());
        }

        return _parse(inputSource);
    }

    private static SAXParserFactory _parserFactory;

    private XMLElement.Handler _defaultHandler;
    private XMLElement.Factory _elementFactory;
    private final Map<String, XMLElement.Handler> _elementHandlers =
        new HashMap<>();
    private PIHandler _piHandler;
    private String _publicString;
    private XMLElement _rootElement;
    private XMLElement.Handler _rootHandler;
    private String _systemString;
    private boolean _validating;
    private XMLReader _xmlReader;

    /**
     * Processing instruction handler.
     */
    public interface PIHandler
    {
        /**
         * Called on processing instructions.
         *
         * @param target The processing instruction target.
         * @param data The optional processing instruction data.
         *
         * @throws PIException On error.
         */
        void onPI(
                @Nonnull String target,
                @Nonnull String data)
            throws PIException;

        /**
         * PI exception.
         */
        public static final class PIException
            extends Exception
        {
            private static final long serialVersionUID = 1L;
        }
    }


    /**
     * Element reader.
     */
    public static final class ElementReader
        extends InputStreamReader
        implements XMLElement.Handler
    {
        /**
         * Constructs an instance.
         *
         * @param input The actual input stream.
         */
        public ElementReader(@Nonnull final InputStream input)
        {
            super(input, StandardCharsets.UTF_8);
        }

        /** {@inheritDoc}
         *
         * <p>Ignores any close request.</p>
         */
        @Override
        public void close() {}

        /** {@inheritDoc}
         */
        @Override
        public boolean markSupported()
        {
            return !_inputEnded;
        }

        /** {@inheritDoc}
         */
        @Override
        public XMLElement onElementEnd(@Nonnull final XMLElement element)
        {
            _elementEnded = true;

            return element;
        }

        /** {@inheritDoc}
         */
        @Override
        public XMLElement onElementStart(@Nonnull final XMLElement element)
        {
            return element;
        }

        /** {@inheritDoc}
         */
        @Override
        public int read()
            throws IOException
        {
            final char[] buffer = new char[1];
            final int read = read(buffer, 0, buffer.length);

            return (read > 0)? buffer[0]: read;
        }

        /** {@inheritDoc}
         */
        @Override
        public int read(final char[] buffer, final int offset, final int length)
        {
            int read;

            if (_elementEnded || _inputEnded) {
                read = -1;
            } else {
                try {
                    read = super.read(buffer, offset, length);
                } catch (final IOException exception) {
                    read = -1;
                }

                if (read <= 0) {
                    _inputEnded = true;
                }
            }

            return read;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean ready()
            throws IOException
        {
            return !_elementEnded && super.ready();
        }

        /** {@inheritDoc}
         */
        @Override
        public void reset()
        {
            _elementEnded = false;
        }

        private boolean _elementEnded;
        private boolean _inputEnded;
    }


    /**
     * Parse exception.
     */
    public static final class ParseException
        extends Exception
    {
        /**
         * Constructs an instance.
         *
         * @param cause The exception cause.
         */
        public ParseException(@Nonnull final Throwable cause)
        {
            super(cause);
        }

        /** {@inheritDoc}
         */
        @Override
        @Nonnull
        public Throwable getCause()
        {
            return Require.notNull(super.getCause());
        }

        private static final long serialVersionUID = 1L;
    }


    private final class _XMLHandler
        extends DefaultHandler2
    {
        /**
         * Constructs an instance.
         */
        _XMLHandler()
        {
            _element = getRootElement();

            if (_element.isNameEmpty()) {
                _element.clearName();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void characters(
                final char[] buffer,
                final int start,
                final int length)
            throws SAXException
        {
            if (_element != null) {
                _element.addText(new String(buffer, start, length));
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void endElement(
                final String uri,
                final String localName,
                final String name)
            throws SAXException
        {
            try {
                if (_element != null) {
                    final XMLElement.Handler elementHandler =
                        _getElementHandler(
                            _element.getPath());

                    if (elementHandler != null) {
                        _element = elementHandler.onElementEnd(_element);
                    }
                }

                if ((_element != null) && _parents.isEmpty()) {
                    final Optional<XMLElement.Handler> rootHandler =
                        _getRootHandler();

                    if (rootHandler.isPresent()) {
                        _element = rootHandler.get().onElementEnd(_element);
                    }
                }
            } catch (final Exception exception) {
                throw new SAXException(exception);
            }

            if (_parents.isEmpty()) {
                setRootElement(Optional.ofNullable(_element));
            } else {
                final XMLElement parent = _parents.removeLast();

                if (_element != null) {
                    parent.addChild(_element);
                }

                _element = parent;
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void error(final SAXParseException exception)
            throws SAXException
        {
            throw exception;
        }

        /** {@inheritDoc}
         */
        @Override
        public void processingInstruction(
                final String target,
                final String data)
            throws SAXException
        {
            final Optional<PIHandler> piHandler = _getPIHandler();

            if (piHandler.isPresent()) {
                try {
                    piHandler
                        .get()
                        .onPI(
                            target,
                            (data != null)? data: "");
                } catch (final Exception exception) {
                    throw new SAXException(exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public InputSource resolveEntity(
                final String name,
                final String publicId,
                final String baseURI,
                final String systemId)
        {
            return new InputSource(new StringReader(""));
        }

        /** {@inheritDoc}
         */
        @Override
        public void startElement(
                final String uri,
                final String localName,
                final String name,
                final Attributes attributes)
            throws SAXException
        {
            final String elementName = (localName.length() > 0)? localName: name;

            if ((_element != null) && _element.isNameNull()) {
                _element.setName(elementName);
            } else {
                _parents.addLast(_element);

                if (_element != null) {
                    final String prefix = _element.getPath();

                    _element = createXMLElement(elementName);
                    _element.setPath(prefix);
                }
            }

            if (_element != null) {
                final int attributesLength = attributes.getLength();

                for (int i = 0; i < attributesLength; ++i) {
                    String attributeName = attributes.getLocalName(i);

                    if (attributeName.isEmpty()) {
                        attributeName = attributes.getQName(i);
                    }

                    _element
                        .setAttribute(attributeName, attributes.getValue(i));
                }

                try {
                    if (_parents.isEmpty()) {
                        final Optional<XMLElement.Handler> rootHandler =
                            _getRootHandler();

                        if (rootHandler.isPresent()) {
                            _element = rootHandler
                                .get()
                                .onElementStart(_element);
                        }
                    }

                    if (_element != null) {
                        final XMLElement.Handler elementHandler =
                            _getElementHandler(
                                _element.getPath());

                        if (elementHandler != null) {
                            _element = elementHandler.onElementStart(_element);
                        }
                    }
                } catch (final Exception exception) {
                    throw new SAXException(exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void warning(
                final SAXParseException exception)
            throws SAXException
        {
            throw exception;
        }

        private XMLElement _element;
        private final LinkedList<XMLElement> _parents = new LinkedList<>();
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
