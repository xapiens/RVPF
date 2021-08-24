/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UUIDConverter.java 3980 2019-05-13 12:52:38Z SFB $
 */

package org.rvpf.base.xml.streamer.xstream.converter;

import javax.annotation.concurrent.Immutable;

import org.rvpf.base.UUID;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;

/**
 * UUID converter.
 */
@Immutable
public final class UUIDConverter
    extends AbstractSingleValueConverter
    implements XStreamStreamer.Converter
{
    /** {@inheritDoc}
     */
    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") final Class type)
    {
        return type.equals(UUID.class);
    }

    /** {@inheritDoc}
     */
    @Override
    public Object fromString(final String string)
    {
        return UUID.fromString(string).orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final XStreamStreamer streamer)
    {
        final XStream xstream = streamer.getXStream();

        xstream.registerConverter(this, XStream.PRIORITY_NORMAL + 1);

        xstream.alias(UUID_ELEMENT, UUID.class);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown() {}

    /** UUID element. */
    public static final String UUID_ELEMENT = "uuid";
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
