/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DictConverter.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base.xml.streamer.xstream.converter;

import java.io.Serializable;

import java.util.Map;

import org.rvpf.base.value.Dict;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Dict converter.
 */
public class DictConverter
    extends MapConverter
    implements XStreamStreamer.Converter
{
    /**
     * Constructs a dict converter.
     *
     * @param mapper The mapper.
     */
    public DictConverter(final Mapper mapper)
    {
        super(mapper);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") final Class type)
    {
        return type.equals(Dict.class);
    }

    /** {@inheritDoc}
     */
    @Override
    public void marshal(
            final Object source,
            final HierarchicalStreamWriter writer,
            final MarshallingContext context)
    {
        for (final Map.Entry<?, ?> entry: ((Map<?, ?>) source).entrySet()) {
            writer.startNode(KEY_ELEMENT);
            writer.setValue((String) entry.getKey());
            writer.endNode();
            writeItem(entry.getValue(), context, writer);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final XStreamStreamer streamer)
    {
        final XStream xstream = streamer.getXStream();

        xstream.registerConverter(this, XStream.PRIORITY_NORMAL + 1);

        xstream.alias(DICT_ELEMENT, Dict.class);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown() {}

    /** {@inheritDoc}
     */
    @Override
    public Object unmarshal(
            final HierarchicalStreamReader reader,
            final UnmarshallingContext context)
    {
        final Dict dict = (Dict) super.unmarshal(reader, context);

        dict.freeze();

        return dict;
    }

    /** {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void populateMap(
            final HierarchicalStreamReader reader,
            final UnmarshallingContext context,
            @SuppressWarnings("rawtypes") final Map map,
            @SuppressWarnings("rawtypes") final Map target)
    {
        while (reader.hasMoreChildren()) {
            final String key;
            final Serializable value;

            reader.moveDown();

            if (!reader.getNodeName().equals(KEY_ELEMENT)) {
                throw new XStreamException(reader.getNodeName());
            }

            key = reader.getValue();
            reader.moveUp();

            reader.moveDown();
            value = (Serializable) readItem(reader, context, map);
            reader.moveUp();

            target.put(key, value);
        }
    }

    /** Dict element. */
    public static final String DICT_ELEMENT = "dict";
    public static final String KEY_ELEMENT = "key";
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
