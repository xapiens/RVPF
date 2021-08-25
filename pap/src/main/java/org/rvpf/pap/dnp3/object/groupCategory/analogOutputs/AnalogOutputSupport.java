/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AnalogOutputSupport.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.analogOutputs;

import java.util.Optional;

import org.rvpf.base.Content;
import org.rvpf.content.DoubleContent;
import org.rvpf.content.FloatContent;
import org.rvpf.content.IntegerContent;
import org.rvpf.content.LongContent;
import org.rvpf.content.ShortContent;
import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.pap.dnp3.object.PointType;
import org.rvpf.pap.dnp3.object.content.DataType;

/**
 * Analog output support.
 */
public class AnalogOutputSupport
    implements PointType.Support
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<DataType> getDataType(final Content content)
    {
        final DataType dataType;

        if (content instanceof ShortContent) {
            dataType = DataType.INT16;
        } else if (content instanceof IntegerContent) {
            dataType = DataType.INT32;
        } else if (content instanceof FloatContent) {
            dataType = DataType.FLOAT16;
        } else if (content instanceof DoubleContent) {
            dataType = DataType.FLOAT32;
        } else if (content instanceof LongContent) {
            dataType = DataType.INT32;
        } else {
            dataType = null;
        }

        return Optional.ofNullable(dataType);
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectVariation getInputVariation()
    {
        return AnalogOutputStatusVariation.ANY;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<ObjectVariation> getInputVariation(final DataType dataType)
    {
        switch (dataType) {
            case FLOAT16:
                return Optional.of(AnalogOutputStatusVariation.FLOAT_WITH_FLAG);
            case FLOAT32:
                return Optional.of(
                    AnalogOutputStatusVariation.DOUBLE_WITH_FLAG);
            case INT16:
                return Optional.of(AnalogOutputStatusVariation.SHORT_WITH_FLAG);
            case INT32:
                return Optional.of(
                    AnalogOutputStatusVariation.INTEGER_WITH_FLAG);
            default:
                return Optional.empty();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<ObjectVariation> getOutputVariation(final DataType dataType)
    {
        switch (dataType) {
            case FLOAT16:
                return Optional.of(AnalogOutputCommandVariation.FLOAT);
            case FLOAT32:
                return Optional.of(AnalogOutputCommandVariation.DOUBLE);
            case INT16:
                return Optional.of(AnalogOutputCommandVariation.SHORT);
            case INT32:
                return Optional.of(AnalogOutputCommandVariation.INTEGER);
            default:
                return Optional.empty();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public PointType getPointType()
    {
        return PointType.ANALOG_OUTPUT;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isReadOnly()
    {
        return false;
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
