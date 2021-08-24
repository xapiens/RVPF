/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointValueConverter.java 3900 2019-02-19 20:43:24Z SFB $
 */

package org.rvpf.base.xml.streamer.xstream.converter;

import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.RecalcTrigger;
import org.rvpf.base.value.ReplicatedValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.base.xml.streamer.xstream.XStreamStreamer;

/**
 * Point value converter.
 */
@NotThreadSafe
public class PointValueConverter
    extends MappableConverter
{
    /** {@inheritDoc}
     */
    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") final Class type)
    {
        return PointValue.class.isAssignableFrom(type);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final XStreamStreamer streamer)
    {
        if (!super.setUp(streamer)) {
            return false;
        }

        getXStream().alias(POINT_VALUE_ELEMENT, PointValue.class);
        getXStream().alias(VERSIONED_VALUE_ELEMENT, VersionedValue.class);
        getXStream().alias(DELETED_VALUE_ELEMENT, VersionedValue.Deleted.class);
        getXStream().alias(PURGED_VALUE_ELEMENT, VersionedValue.Purged.class);
        getXStream().alias(REPLICATED_VALUE_ELEMENT, ReplicatedValue.class);
        getXStream().alias(RECALC_TRIGGER_ELEMENT, RecalcTrigger.class);

        return true;
    }

    /** Deleted value element. */
    public static final String DELETED_VALUE_ELEMENT = "deleted-value";

    /** Point value element. */
    public static final String POINT_VALUE_ELEMENT = "point-value";

    /** Purged value element. */
    public static final String PURGED_VALUE_ELEMENT = "purged-value";

    /** Recalc trigger element. */
    public static final String RECALC_TRIGGER_ELEMENT = "recalc-trigger";

    /** Replicated value element. */
    public static final String REPLICATED_VALUE_ELEMENT = "replicated-value";

    /** Versioned value element. */
    public static final String VERSIONED_VALUE_ELEMENT = "versioned-value";
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
