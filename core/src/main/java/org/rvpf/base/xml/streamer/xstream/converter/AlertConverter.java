/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AlertConverter.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base.xml.streamer.xstream.converter;

import javax.annotation.concurrent.Immutable;

import org.rvpf.base.alert.Alert;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer;

/**
 * Alert converter.
 */
@Immutable
public final class AlertConverter
    extends MappableConverter
{
    /** {@inheritDoc}
     */
    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") final Class type)
    {
        return Alert.class.isAssignableFrom(type);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final XStreamStreamer streamer)
    {
        if (!super.setUp(streamer)) {
            return false;
        }

        getXStream().alias(EVENT_ELEMENT, org.rvpf.base.alert.Event.class);
        getXStream().alias(SIGNAL_ELEMENT, org.rvpf.base.alert.Signal.class);
        getXStream().alias(INFO_ELEMENT, org.rvpf.base.alert.Info.class);
        getXStream().alias(WARNING_ELEMENT, org.rvpf.base.alert.Warning.class);
        getXStream().alias(ERROR_ELEMENT, org.rvpf.base.alert.Error.class);
        getXStream().alias(FATAL_ELEMENT, org.rvpf.base.alert.Fatal.class);

        return true;
    }

    /** Error element. */
    public static final String ERROR_ELEMENT = "error";

    /** Event element. */
    public static final String EVENT_ELEMENT = "event";

    /** Fatal element. */
    public static final String FATAL_ELEMENT = "fatal";

    /** Info element. */
    public static final String INFO_ELEMENT = "info";

    /** Signal element. */
    public static final String SIGNAL_ELEMENT = "signal";

    /** Warning element. */
    public static final String WARNING_ELEMENT = "warning";
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
