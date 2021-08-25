/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3StationPoint.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.pap.dnp3.object.ObjectRange;
import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.pap.dnp3.object.PointType;
import org.rvpf.pap.dnp3.object.content.DataType;
import org.rvpf.pap.dnp3.transport.LogicalDevice;

/**
 * Station point.
 */
public final class DNP3StationPoint
{
    /**
     * Constructs an instance.
     *
     * @param point The point definition from metadata.
     * @param logicalDevice The logical device.
     * @param pointType The point type.
     * @param objectRange The object range.
     * @param dataType The data type.
     */
    public DNP3StationPoint(
            @Nonnull final Point point,
            @Nonnull final LogicalDevice logicalDevice,
            @Nonnull final PointType pointType,
            @Nonnull final ObjectRange objectRange,
            @Nonnull final DataType dataType)
    {
        _point = point;
        _logicalDevice = logicalDevice;
        _pointType = pointType;
        _objectRange = objectRange;
        _dataType = dataType;
    }

    /**
     * Gets the data type.
     *
     * @return The data type.
     */
    @Nonnull
    @CheckReturnValue
    public DataType getDataType()
    {
        return _dataType;
    }

    /**
     * Gets the logical device.
     *
     * @return The logical device.
     */
    @Nonnull
    @CheckReturnValue
    public LogicalDevice getLogicalDevice()
    {
        return _logicalDevice;
    }

    /**
     * Gets the object range.
     *
     * @return The object range.
     */
    @Nonnull
    @CheckReturnValue
    public ObjectRange getObjectRange()
    {
        return _objectRange;
    }

    /**
     * Gets the point.
     *
     * @return The point.
     */
    @Nonnull
    @CheckReturnValue
    public Point getPoint()
    {
        return _point;
    }

    /**
     * Gets the point type.
     *
     * @return The point type.
     */
    @Nonnull
    @CheckReturnValue
    public PointType getPointType()
    {
        return _pointType;
    }

    /**
     * Gets the support for this type of point.
     *
     * @return The support.
     */
    @Nonnull
    @CheckReturnValue
    public PointType.Support getSupport()
    {
        return getPointType().getSupport();
    }

    /**
     * Asks if this point is read-only.
     *
     * @return True if this point is read-only.
     */
    @CheckReturnValue
    public boolean isReadOnly()
    {
        return getSupport().isReadOnly();
    }

    /**
     * Gets the input variation.
     *
     * @return The input variation.
     */
    @Nonnull
    @CheckReturnValue
    ObjectVariation getInputVariation()
    {
        return _pointType.getSupport().getInputVariation();
    }

    /**
     * Gets the input variation for a data type.
     *
     * @param dataType The data type.
     *
     * @return The input variation (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<ObjectVariation> getInputVariation(
            @Nonnull final DataType dataType)
    {
        return _pointType.getSupport().getInputVariation(dataType);
    }

    /**
     * Gets the output variation for a data type.
     *
     * @param dataType The data type.
     *
     * @return The output variation (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<ObjectVariation> getOutputVariation(
            @Nonnull final DataType dataType)
    {
        return _pointType.getSupport().getOutputVariation(dataType);
    }

    private final DataType _dataType;
    private final LogicalDevice _logicalDevice;
    private final ObjectRange _objectRange;
    private final Point _point;
    private final PointType _pointType;
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
