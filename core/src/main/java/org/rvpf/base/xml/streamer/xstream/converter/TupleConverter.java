/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TupleConverter.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base.xml.streamer.xstream.converter;

import javax.annotation.Nonnull;

import org.rvpf.base.value.Tuple;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Tuple converter.
 */
public class TupleConverter
    extends CollectionConverter
    implements XStreamStreamer.Converter
{
    /**
     * Constructs a tuple converter.
     *
     * @param mapper The mapper.
     */
    public TupleConverter(@Nonnull final Mapper mapper)
    {
        super(mapper);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") final Class type)
    {
        return type.equals(Tuple.class);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final XStreamStreamer streamer)
    {
        final XStream xstream = streamer.getXStream();

        xstream.registerConverter(this, XStream.PRIORITY_NORMAL + 1);

        xstream.alias(TUPLE_ELEMENT, Tuple.class);

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
        final Tuple tuple = (Tuple) super.unmarshal(reader, context);

        tuple.freeze();

        return tuple;
    }

    /** Dict element. */
    public static final String TUPLE_ELEMENT = "tuple";
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
