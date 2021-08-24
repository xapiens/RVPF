/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PointValueFilter.java 4040 2019-05-31 18:55:08Z SFB $
 */

package org.rvpf.forwarder.filter;

import java.io.Serializable;

import javax.annotation.CheckReturnValue;

import org.rvpf.base.Point;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.PointValueFilters;
import org.rvpf.base.value.PointValue;
import org.rvpf.document.loader.MetadataFilter;
import org.rvpf.forwarder.ForwarderModule;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointEntity;

/**
 * Point value filter.
 */
public final class PointValueFilter
    extends ForwarderFilter.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public synchronized Serializable[] filter(final Serializable message)
    {
        PointValue pointValue = knownPointValue(message).orElse(null);

        if (pointValue == null) {
            return NO_MESSAGES;
        }

        pointValue = pointValue.encoded();

        if (_stampRequired && !pointValue.hasStamp()) {
            pointValue = pointValue.copy();
        }

        final PointValue[] filteredValues = _valueFilters.filter(pointValue);

        for (int i = 0; i < filteredValues.length; ++i) {
            PointValue filteredValue = filteredValues[i];
            final String pointName = filteredValue.getPointName().get();

            filteredValue = filteredValue.reset();
            filteredValue.setPointName(pointName);
            filteredValues[i] = filteredValue;
        }

        return filteredValues;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean needsMetadata()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized boolean onMetadataRefreshed()
    {
        return _loadMetadata();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final ForwarderModule forwarderModule,
            final KeyedGroups filterProperties)
    {
        if (!super.setUp(forwarderModule, filterProperties)) {
            return false;
        }

        _stampRequired = filterProperties.getBoolean(STAMP_REQUIRED_PROPERTY);

        if (!_loadMetadata()) {
            return false;
        }

        return super.setUp(forwarderModule, filterProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean onNewMetadata(final Metadata metadata)
    {
        for (final Point point: metadata.getPointsCollection()) {
            if (!((PointEntity) point).setUp(metadata)) {
                return false;
            }
        }

        _valueFilters.clear();
        _valueFilters.filterPoints(metadata.getPointsCollection());

        return super.onNewMetadata(metadata);
    }

    @CheckReturnValue
    private boolean _loadMetadata()
    {
        final class _MetadataFilter
            extends MetadataFilter
        {
            /**
             * Constructs an instance.
             */
            public _MetadataFilter()
            {
                super(true);
            }

            /** {@inheritDoc}
             */
            @Override
            public boolean areContentsNeeded()
            {
                return true;
            }
        }

        return loadMetadata(new _MetadataFilter());
    }

    /** Stamp required property. */
    public static final String STAMP_REQUIRED_PROPERTY = "stamp.required";

    private boolean _stampRequired;
    private final PointValueFilters _valueFilters = new PointValueFilters();
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
