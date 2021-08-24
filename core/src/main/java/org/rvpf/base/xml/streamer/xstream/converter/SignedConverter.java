/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SignedConverter.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base.xml.streamer.xstream.converter;

import org.rvpf.base.security.Crypt;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer.Converter;

import com.thoughtworks.xstream.XStream;

/**
 * Signed converter.
 */
public class SignedConverter
    implements Converter
{
    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final XStreamStreamer streamer)
    {
        final XStream xstream = streamer.getXStream();
        final Class<?> signedClass = Crypt.signedClass();

        xstream.alias(SIGNED_ELEMENT, signedClass);
        xstream.aliasField(SIGNED_ELEMENT, signedClass, SIGNED_FIELD);
        xstream.aliasField(SIGNATURE_ELEMENT, signedClass, SIGNATURE_FIELD);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown() {}

    /** Signature element. */
    public static final String SIGNATURE_ELEMENT = "signature";

    /** Signature field. */
    public static final String SIGNATURE_FIELD = "_signature";

    /** Signed element. */
    public static final String SIGNED_ELEMENT = "signed";

    /** Signed field. */
    public static final String SIGNED_FIELD = "_signed";
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
