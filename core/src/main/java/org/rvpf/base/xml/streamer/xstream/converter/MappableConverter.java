/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MappableConverter.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base.xml.streamer.xstream.converter;

import java.io.Serializable;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.util.Mappable;
import org.rvpf.base.value.Tuple;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Mappable converter.
 */
@NotThreadSafe
public abstract class MappableConverter
    implements Converter, XStreamStreamer.Converter
{
    /** {@inheritDoc}
     */
    @Override
    public final void marshal(
            final Object source,
            final HierarchicalStreamWriter writer,
            final MarshallingContext context)
    {
        ((Mappable) source).writeMap(new _WriterMap(writer, context));
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final XStreamStreamer streamer)
    {
        _xstream = streamer.getXStream();
        _xstream.registerConverter(this, XStream.PRIORITY_NORMAL + 1);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown() {}

    /** {@inheritDoc}
     */
    @Override
    public final Object unmarshal(
            final HierarchicalStreamReader reader,
            final UnmarshallingContext context)
    {
        final Class<?> type = context.getRequiredType();
        final Map<String, Serializable> map = new HashMap<>();
        final Mappable mappable;

        try {
            mappable = (Mappable) type.getConstructor().newInstance();
        } catch (final Exception exception) {
            throw new ConversionException(exception);
        }

        final Iterator<?> attributeIterator = reader.getAttributeNames();

        while (attributeIterator.hasNext()) {
            final String attributeName = (String) attributeIterator.next();

            map.put(attributeName, reader.getAttribute(attributeName));
        }

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            final Serializable value;
            String className = reader.getAttribute(CLASS_ATTRIBUTE);

            if (className == null) {    // Accepts 'type' as alias.
                className = reader.getAttribute(TYPE_ATTRIBUTE);
            }

            if (className != null) {
                value = (Serializable) context
                    .convertAnother(
                        mappable,
                        getXStream().getMapper().realClass(className));
            } else if (reader.hasMoreChildren()) {
                value = (Serializable) context
                    .convertAnother(mappable, LinkedList.class);
            } else {
                value = reader.getValue();
            }

            map.put(reader.getNodeName(), value);

            reader.moveUp();
        }

        mappable.readMap(map);

        return mappable;
    }

    /**
     * Gets the XStream instance.
     *
     * @return The XStream instance.
     */
    protected final XStream getXStream()
    {
        return _xstream;
    }

    /** Class attribute. */
    public static final String CLASS_ATTRIBUTE = "class";

    /** Type attribute. */
    public static final String TYPE_ATTRIBUTE = "type";

    private XStream _xstream;

    @NotThreadSafe
    private final class _WriterMap
        extends AbstractMap<String, Serializable>
    {
        /**
         * Constructs an instance.
         *
         * @param writer The writer.
         * @param context The context.
         */
        _WriterMap(
                @Nonnull final HierarchicalStreamWriter writer,
                @Nonnull final MarshallingContext context)
        {
            _writer = writer;
            _context = context;
        }

        /** {@inheritDoc}
         */
        @Override
        public Set<Entry<String, Serializable>> entrySet()
        {
            return null;
        }

        /**
         * Puts a field value.
         *
         * @param key The field name.
         * @param value The field value.
         *
         * @return A null value.
         */
        @Override
        public Serializable put(final String key, final Serializable value)
        {
            if (value == null) {
                if (key == Mappable.SIMPLE_STRING_MODE) {
                    _attributeMode = true;
                } else if (key == Mappable.SERIALIZABLE_MODE) {
                    _attributeMode = false;
                }

                return null;
            }

            if (_attributeMode) {
                _writer.addAttribute(key, (String) value);
            } else {
                _writer.startNode(key);

                if (!((value instanceof Collection<?>)
                        && (value.getClass() != Tuple.class))) {
                    _writer
                        .addAttribute(
                            CLASS_ATTRIBUTE,
                            getXStream()
                                .getMapper()
                                .serializedClass(value.getClass()));
                }

                _context.convertAnother(value);
                _writer.endNode();
            }

            return null;
        }

        private boolean _attributeMode;
        private final MarshallingContext _context;
        private final HierarchicalStreamWriter _writer;
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
