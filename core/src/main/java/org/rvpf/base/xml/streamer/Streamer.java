/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Streamer.java 4099 2019-06-26 21:33:35Z SFB $
 */

package org.rvpf.base.xml.streamer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Iterator;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer;

/**
 * Streamer.
 *
 * <p>Facade to (de)serialize values.</p>
 *
 * <p>The current implementation uses <a href="http://xstream.codehaus.org/">
 * XStream</a> as back end.</p>
 */
@NotThreadSafe
public abstract class Streamer
{
    /**
     * Returns a new instance.
     *
     * @return The new instance.
     */
    @Nonnull
    @CheckReturnValue
    public static Streamer newInstance()
    {
        return new XStreamStreamer();
    }

    /**
     * Returns the serializable represented by an XML element.
     *
     * @param xmlElement The XML element.
     *
     * @return The serializable (may have a null value).
     */
    @Nullable
    @CheckReturnValue
    public Serializable fromXML(@Nonnull final XMLElement xmlElement)
    {
        final XMLElement parentElement = new XMLElement("");

        parentElement.addChild(Require.notNull(xmlElement));

        final Input input = newInput(parentElement);
        final Serializable serializable = input.next();

        input.close();

        return serializable;
    }

    /**
     * Returns a new input.
     *
     * @param reader A reader.
     *
     * @return The new input.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Input newInput(@Nonnull Reader reader);

    /**
     * Returns a new input.
     *
     * @param xmlDocument An XML document.
     *
     * @return The new input.
     */
    @Nonnull
    @CheckReturnValue
    public Input newInput(@Nonnull final XMLDocument xmlDocument)
    {
        return newInput(new XMLElement("", xmlDocument.getRootElement()));
    }

    /**
     * Returns a new input.
     *
     * @param parentElement A parent element.
     *
     * @return The new input.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Input newInput(@Nonnull XMLElement parentElement);

    /**
     * Returns a new input.
     *
     * @param file An input file.
     * @param charset An optional charset.
     *
     * @return The new input.
     *
     * @throws FileNotFoundException When the file is not found.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Input newInput(
            @Nonnull File file,
            @Nonnull Optional<Charset> charset)
        throws FileNotFoundException;

    /**
     * Returns a new input.
     *
     * @param inputStream An input stream.
     * @param charset An optional charset.
     *
     * @return The new input.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Input newInput(
            @Nonnull InputStream inputStream,
            @Nonnull Optional<Charset> charset);

    /**
     * Returns a new output.
     *
     * @param writer A writer.
     *
     * @return The new output.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Output newOutput(@Nonnull Writer writer);

    /**
     * Returns a new output.
     *
     * @param parentElement A parent element.
     *
     * @return The new output.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Output newOutput(@Nonnull XMLElement parentElement);

    /**
     * Returns a new output.
     *
     * @param file An output file.
     * @param charset An optional charset.
     *
     * @return The new output.
     *
     * @throws FileNotFoundException When the file is not found.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Output newOutput(
            @Nonnull File file,
            @Nonnull Optional<Charset> charset)
        throws FileNotFoundException;

    /**
     * Returns a new output.
     *
     * @param outputStream An output stream.
     * @param charset An optional charset.
     *
     * @return The new output.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Output newOutput(
            @Nonnull OutputStream outputStream,
            @Nonnull Optional<Charset> charset);

    /**
     * Sets up this.
     *
     * @param configProperties The optional Configuration properties.
     * @param moduleProperties The optional module properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public abstract boolean setUp(
            @Nonnull Optional<KeyedGroups> configProperties,
            @Nonnull Optional<KeyedValues> moduleProperties);

    /**
     * Tears down what has been set up.
     */
    public abstract void tearDown();

    /**
     * Returns a serializable as an XML element.
     *
     * @param serializable The serializable.
     *
     * @return The XML element.
     */
    @Nonnull
    @CheckReturnValue
    public XMLElement toXML(@Nullable final Serializable serializable)
    {
        final XMLElement parentElement = new XMLElement("");
        final Output output = newOutput(parentElement);

        Require.success(output.add(serializable));
        output.close();

        return parentElement.getChild(0);
    }

    /**
     * Validated.
     */
    public interface Validated
    {
        /**
         * Validates.
         *
         * @return True when valid.
         */
        @CheckReturnValue
        boolean validate();
    }


    /**
     * Input.
     */
    public abstract static class Input
        implements Iterator<Serializable>, Iterable<Serializable>
    {
        /**
         * Constructs an input.
         */
        protected Input()
        {
            _reader = null;
        }

        /**
         * Constructs an input.
         *
         * @param reader A reader.
         */
        protected Input(@Nonnull final Reader reader)
        {
            _reader = reader;
        }

        /**
         * Constructs an input.
         *
         * @param file An input file.
         * @param charset An optional charset.
         *
         * @throws FileNotFoundException When the file is not found.
         */
        protected Input(
                @Nonnull final File file,
                @Nonnull final Optional<Charset> charset)
            throws FileNotFoundException
        {
            this(new FileInputStream(file), charset);
        }

        /**
         * Constructs an input.
         *
         * @param inputStream An input stream.
         * @param charset An optional charset.
         */
        protected Input(
                @Nonnull final InputStream inputStream,
                @Nonnull final Optional<Charset> charset)
        {
            _reader = new InputStreamReader(
                inputStream,
                charset.isPresent()? charset.get(): StandardCharsets.UTF_8);
        }

        /**
         * Closes the input.
         */
        public abstract void close();

        /** {@inheritDoc}
         */
        @Override
        public Iterator<Serializable> iterator()
        {
            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public abstract Serializable next();

        /** {@inheritDoc}
         */
        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Skips the next entry.
         *
         * @return True if there was an entry to skip.
         */
        @CheckReturnValue
        public abstract boolean skip();

        /**
         * Gets the reader.
         *
         * @return The optional reader.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<Reader> getReader()
        {
            return Optional.ofNullable(_reader);
        }

        private final Reader _reader;
    }


    /**
     * Output.
     */
    public abstract static class Output
    {
        /**
         * Constructs an output.
         */
        protected Output()
        {
            _writer = null;
        }

        /**
         * Constructs an output.
         *
         * @param writer A writer.
         */
        protected Output(@Nonnull final Writer writer)
        {
            _writer = new _NoCloseWriter(writer);
        }

        /**
         * Constructs an output.
         *
         * @param file An output file.
         * @param charset An optional charset.
         *
         * @throws FileNotFoundException When the file is not found.
         */
        protected Output(
                @Nonnull final File file,
                @Nonnull final Optional<Charset> charset)
            throws FileNotFoundException
        {
            _writer = new OutputStreamWriter(
                new FileOutputStream(file),
                charset.isPresent()? charset.get(): StandardCharsets.UTF_8);
        }

        /**
         * Constructs an output.
         *
         * @param outputStream An output stream.
         * @param charset An optional charset.
         */
        protected Output(
                @Nonnull final OutputStream outputStream,
                @Nonnull final Optional<Charset> charset)
        {
            _writer = new _NoCloseWriter(
                new OutputStreamWriter(
                    outputStream,
                    charset.isPresent()
                    ? charset.get(): StandardCharsets.UTF_8));
        }

        /**
         * Adds a serializable.
         *
         * @param serializable The serializable.
         *
         * @return False when a {@link Validated} serializable is invalid.
         */
        @CheckReturnValue
        public abstract boolean add(Serializable serializable);

        /**
         * Closes the output.
         */
        public abstract void close();

        /**
         * Flushes the output.
         */
        public abstract void flush();

        /**
         * Gets the writer.
         *
         * @return The optional writer.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<Writer> getWriter()
        {
            return Optional.ofNullable(_writer);
        }

        /**
         * Validates a serializable.
         *
         * @param serializable The serializable.
         *
         * @return True when valid.
         */
        @CheckReturnValue
        protected boolean validate(final Serializable serializable)
        {
            if (serializable instanceof Validated) {
                return ((Validated) serializable).validate();
            }

            return true;
        }

        private final Writer _writer;

        /**
         * No close writer.
         */
        private static class _NoCloseWriter
            extends FilterWriter
        {
            /**
             * Constructs an instance.
             *
             * @param writer A writer.
             */
            _NoCloseWriter(@Nonnull final Writer writer)
            {
                super(writer);
            }

            /** {@inheritDoc}
             *
             * <p>Ignores any close request.</p>
             */
            @Override
            public void close() {}
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
