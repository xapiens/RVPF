/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: XStreamStreamer.java 4102 2019-06-30 15:41:17Z SFB $
 */

package org.rvpf.base.xml.streamer.xstream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;

import java.nio.charset.Charset;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.base.xml.streamer.Streamer;
import org.rvpf.base.xml.streamer.xstream.converter.AlertConverter;
import org.rvpf.base.xml.streamer.xstream.converter.BigRationalConverter;
import org.rvpf.base.xml.streamer.xstream.converter.ComplexConverter;
import org.rvpf.base.xml.streamer.xstream.converter.DateTimeConverter;
import org.rvpf.base.xml.streamer.xstream.converter.DictConverter;
import org.rvpf.base.xml.streamer.xstream.converter.DoubleConverter;
import org.rvpf.base.xml.streamer.xstream.converter.EncryptedConverter;
import org.rvpf.base.xml.streamer.xstream.converter.FloatConverter;
import org.rvpf.base.xml.streamer.xstream.converter.PointValueConverter;
import org.rvpf.base.xml.streamer.xstream.converter.RationalConverter;
import org.rvpf.base.xml.streamer.xstream.converter.SignedConverter;
import org.rvpf.base.xml.streamer.xstream.converter.StateConverter;
import org.rvpf.base.xml.streamer.xstream.converter.TupleConverter;
import org.rvpf.base.xml.streamer.xstream.converter.UUIDConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;

/**
 * XStream streamer.
 *
 * <p>Facade to (de)serialize values under an XML fragment representation. The
 * actual work is done by <a href="http://xstream.codehaus.org/">
 * XStream</a>.</p>
 */
@NotThreadSafe
public final class XStreamStreamer
    extends Streamer
{
    /**
     * Gets the XStream instance.
     *
     * @return The XStream instance.
     */
    @CheckReturnValue
    public XStream getXStream()
    {
        return _xstream;
    }

    /** {@inheritDoc}
     */
    @Override
    public Input newInput(final Reader reader)
    {
        return new _InputImpl(reader);
    }

    /** {@inheritDoc}
     */
    @Override
    public Input newInput(final XMLElement parentElement)
    {
        return new _InputImpl(parentElement);
    }

    /** {@inheritDoc}
     */
    @Override
    public Input newInput(
            final File file,
            final Optional<Charset> charset)
        throws FileNotFoundException
    {
        return new _InputImpl(file, charset);
    }

    /** {@inheritDoc}
     */
    @Override
    public Input newInput(
            final InputStream inputStream,
            final Optional<Charset> charset)
    {
        return new _InputImpl(inputStream, charset);
    }

    /** {@inheritDoc}
     */
    @Override
    public Output newOutput(final Writer writer)
    {
        return new _OutputImpl(writer);
    }

    /** {@inheritDoc}
     */
    @Override
    public Output newOutput(final XMLElement parentElement)
    {
        return new _OutputImpl(parentElement);
    }

    /** {@inheritDoc}
     */
    @Override
    public Output newOutput(
            final File file,
            final Optional<Charset> charset)
        throws FileNotFoundException
    {
        return new _OutputImpl(file, charset);
    }

    /** {@inheritDoc}
     */
    @Override
    public Output newOutput(
            final OutputStream outputStream,
            final Optional<Charset> charset)
    {
        return new _OutputImpl(outputStream, charset);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final Optional<KeyedGroups> configProperties,
            final Optional<KeyedValues> moduleProperties)
    {
        _driver = new StaxDriver();
        _xstream = new XStream(
            null,
            _driver,
            new ClassLoaderReference(
                Thread.currentThread().getContextClassLoader()));
        XStream.setupDefaultSecurity(_xstream);
        _xstream.addPermission(AnyTypePermission.ANY);

        if ((!moduleProperties.isPresent())
                || !moduleProperties.get().getBoolean(REFERENCES_PROPERTY)) {
            _xstream.setMode(XStream.NO_REFERENCES);
        }

        if (!_registerConverter(new PointValueConverter())) {
            return false;
        }

        if (!_registerConverter(new AlertConverter())) {
            return false;
        }

        if (!_registerConverter(new UUIDConverter())) {
            return false;
        }

        if (!_registerConverter(new DoubleConverter())) {
            return false;
        }

        if (!_registerConverter(new FloatConverter())) {
            return false;
        }

        if (!_registerConverter(new StateConverter())) {
            return false;
        }

        if (!_registerConverter(new ComplexConverter())) {
            return false;
        }

        if (!_registerConverter(new RationalConverter())) {
            return false;
        }

        if (!_registerConverter(new BigRationalConverter())) {
            return false;
        }

        if (!_registerConverter(new DateTimeConverter())) {
            return false;
        }

        if (!_registerConverter(new TupleConverter(_xstream.getMapper()))) {
            return false;
        }

        if (!_registerConverter(new DictConverter(_xstream.getMapper()))) {
            return false;
        }

        if (!_registerConverter(new EncryptedConverter())) {
            return false;
        }

        if (!_registerConverter(new SignedConverter())) {
            return false;
        }

        if (configProperties.isPresent()) {
            for (final ClassDef converterClassDef:
                    configProperties
                        .get()
                        .getClassDefs(CONVERTER_CLASS_PROPERTY)) {
                if (!_registerConverter(converterClassDef)) {
                    return false;
                }
            }

            for (final ClassDef classDef:
                    configProperties
                        .get()
                        .getClassDefs(ANNOTATED_CLASS_PROPERTY)) {
                if (!_registerAnnotations(classDef)) {
                    return false;
                }
            }
        }

        if (moduleProperties.isPresent()) {
            for (final ClassDef converterClassDef:
                    moduleProperties
                        .get()
                        .getClassDefs(CONVERTER_CLASS_PROPERTY)) {
                if (!_registerConverter(converterClassDef)) {
                    return false;
                }
            }

            for (final ClassDef classDef:
                    moduleProperties
                        .get()
                        .getClassDefs(ANNOTATED_CLASS_PROPERTY)) {
                if (!_registerAnnotations(classDef)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        for (final Converter converter: _converters) {
            converter.tearDown();
        }

        _converters.clear();
        _xstream = null;
        _driver = null;
    }

    /**
     * Gets the stream driver.
     *
     * @return The stream driver.
     */
    @Nonnull
    @CheckReturnValue
    StaxDriver getDriver()
    {
        return Require.notNull(_driver);
    }

    private boolean _registerAnnotations(final ClassDef classDef)
    {
        final Class<?> streamedClass = classDef.getInstanceClass();

        if (streamedClass == null) {
            _LOGGER
                .warn(
                    BaseMessages.ANNOTATIONS_REGISTRATION,
                    classDef.getClassName());

            return false;
        }

        _xstream.processAnnotations(streamedClass);

        _LOGGER
            .trace(
                BaseMessages.REGISTERED_ANNOTATIONS,
                streamedClass.getName());

        return true;
    }

    private boolean _registerConverter(final ClassDef classDef)
    {
        final Converter converterInstance = classDef
            .createInstance(Converter.class);

        return _registerConverter(Require.notNull(converterInstance));
    }

    private boolean _registerConverter(final Converter converter)
    {
        if (!converter.setUp(this)) {
            return false;
        }

        if (_converters.add(converter)) {
            _LOGGER
                .trace(
                    BaseMessages.REGISTERED_CONVERTER,
                    converter.getClass().getName());
        }

        return true;
    }

    /** Annotated class property. */
    public static final String ANNOTATED_CLASS_PROPERTY =
        "xstream.annotated.class";

    /** Converter classes property. */
    public static final String CONVERTER_CLASS_PROPERTY =
        "xstream.converter.class";

    /** References property. */
    public static final String REFERENCES_PROPERTY = "references";

    /** Root element. */
    public static final String ROOT_ELEMENT = "rvpf-data";

    /**  */

    private static final Logger _LOGGER = Logger
        .getInstance(XStreamStreamer.class);

    static {
        final InputStream inputStream = XStream.class
            .getResourceAsStream(
                "/META-INF/maven/com.thoughtworks.xstream/xstream/pom.properties");

        if (inputStream != null) {
            final Properties properties = new Properties();
            final String version;

            try {
                properties.load(inputStream);
            } catch (final IOException exception) {
                // Ignore.
            }

            version = properties.getProperty("version");

            if (version != null) {
                _LOGGER.debug(BaseMessages.XSTREAM_VERSION, version);
            }
        }
    }

    private final Set<Converter> _converters = new HashSet<Converter>();
    private StaxDriver _driver;
    private XStream _xstream;

    /**
     * Owned Converter.
     */
    public interface Converter
    {
        /**
         * Sets up this.
         *
         * @param streamer The calling streamer.
         *
         * @return True on success.
         */
        @CheckReturnValue
        boolean setUp(@Nonnull XStreamStreamer streamer);

        /**
         * Tears down what has been set up.
         */
        void tearDown();
    }


    /**
     * Input implementation.
     */
    private final class _InputImpl
        extends Input
    {
        /**
         * Constructs an instance.
         *
         * @param reader The reader.
         */
        _InputImpl(@Nonnull final Reader reader)
        {
            super(reader);

            _init();
        }

        /**
         * Constructs an instance.
         *
         * @param parentElement The parent element.
         */
        _InputImpl(@Nonnull final XMLElement parentElement)
        {
            _streamerReader = new ElementReader(parentElement);
        }

        /**
         * Constructs an instance.
         *
         * @param file The file.
         * @param charset The optional character set.
         *
         * @throws FileNotFoundException When the file is not found.
         */
        _InputImpl(
                @Nonnull final File file,
                @Nonnull final Optional<Charset> charset)
            throws FileNotFoundException
        {
            super(file, charset);

            _init();
        }

        /**
         * Constructs an instance.
         *
         * @param inputStream The input stream.
         * @param charset The character set.
         */
        _InputImpl(
                @Nonnull final InputStream inputStream,
                @Nonnull final Optional<Charset> charset)
        {
            super(inputStream, charset);

            _init();
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            _streamerReader.close();

            if (_wrapper != null) {
                try {
                    _wrapper.close();
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean hasNext()
        {
            return _streamerReader.hasMoreChildren();
        }

        /** {@inheritDoc}
         */
        @Override
        public Serializable next()
        {
            final Serializable next;

            if (!_streamerReader.hasMoreChildren()) {
                throw new NoSuchElementException();
            }

            _streamerReader.moveDown();
            next = (Serializable) getXStream().unmarshal(_streamerReader);
            _streamerReader.moveUp();

            return next;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean skip()
        {
            final boolean skipped;

            if (_streamerReader.hasMoreChildren()) {
                _streamerReader.moveDown();
                _streamerReader.moveUp();
                skipped = true;
            } else {
                skipped = false;
            }

            return skipped;
        }

        private void _init()
        {
            _wrapper = new _Wrapper(getReader().get());
            _streamerReader = getDriver().createReader(_wrapper);
        }

        private HierarchicalStreamReader _streamerReader;
        private _Wrapper _wrapper;
    }


    /**
     * Output implementation.
     */
    private final class _OutputImpl
        extends Output
    {
        /**
         * Constructs an instance.
         *
         * @param writer The writer.
         */
        _OutputImpl(@Nonnull final Writer writer)
        {
            super(writer);

            _init();
        }

        /**
         * Constructs an instance.
         *
         * @param parentElement The parent element.
         */
        _OutputImpl(@Nonnull final XMLElement parentElement)
        {
            _streamerWriter = new ElementWriter(Require.notNull(parentElement));
        }

        /**
         * Constructs an instance.
         *
         * @param file The file.
         * @param charset An optional character set.
         *
         * @throws FileNotFoundException When the file is not found.
         */
        _OutputImpl(
                @Nonnull final File file,
                @Nonnull final Optional<Charset> charset)
            throws FileNotFoundException
        {
            super(file, charset);

            _init();
        }

        /**
         * Constructs an instance.
         *
         * @param outputStream The output stream.
         * @param charset An optional character set.
         */
        _OutputImpl(
                @Nonnull final OutputStream outputStream,
                @Nonnull final Optional<Charset> charset)
        {
            super(outputStream, charset);

            _init();
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean add(final Serializable serializable)
        {
            if (!validate(serializable)) {
                return false;
            }

            getXStream().marshal(serializable, _streamerWriter);

            if (_xmlStreamWriter != null) {
                try {
                    _xmlStreamWriter.writeCharacters("\n");
                } catch (final XMLStreamException exception) {
                    throw new StreamException(exception);
                }
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            flush();
            _streamerWriter.close();

            if (getWriter().isPresent()) {
                try {
                    getWriter().get().close();
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public void flush()
        {
            _streamerWriter.flush();
        }

        private void _init()
        {
            try {
                _xmlStreamWriter = getDriver()
                    .getOutputFactory()
                    .createXMLStreamWriter(getWriter().get());
                _streamerWriter = getDriver()
                    .createStaxWriter(_xmlStreamWriter, false);
            } catch (final XMLStreamException exception) {
                throw new StreamException(exception);
            }
        }

        private HierarchicalStreamWriter _streamerWriter;
        private XMLStreamWriter _xmlStreamWriter;
    }


    /**
     * Wrapper.
     */
    private static final class _Wrapper
        extends Reader
    {
        /**
         * Constructs an instance.
         *
         * @param reader The reader.
         */
        _Wrapper(@Nonnull final Reader reader)
        {
            _readers.add(new StringReader("<" + ROOT_ELEMENT + ">"));
            _readers.add(reader);
            _readers.add(new StringReader("</" + ROOT_ELEMENT + ">"));
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
            throws IOException
        {
            while (!_readers.isEmpty()) {
                final Reader reader = _readers.remove();

                reader.close();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public int read(
                final char[] buffer,
                final int offset,
                final int length)
            throws IOException
        {
            while (!_readers.isEmpty()) {
                final int read = _readers
                    .getFirst()
                    .read(buffer, offset, length);

                if (read > 0) {
                    return read;
                }

                _readers.removeFirst().close();
            }

            return -1;
        }

        private final LinkedList<Reader> _readers = new LinkedList<Reader>();
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
