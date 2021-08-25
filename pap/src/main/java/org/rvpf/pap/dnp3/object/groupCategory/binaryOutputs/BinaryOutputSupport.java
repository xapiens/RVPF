/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BinaryOutputSupport.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.binaryOutputs;

import java.util.Optional;

import org.rvpf.base.Content;
import org.rvpf.content.BooleanContent;
import org.rvpf.pap.dnp3.object.ObjectVariation;
import org.rvpf.pap.dnp3.object.PointType;
import org.rvpf.pap.dnp3.object.content.DataType;

/**
 * Binary output support.
 */
public class BinaryOutputSupport
    implements PointType.Support
{
    /** {@inheritDoc}
     */
    @Override
    public Optional<DataType> getDataType(final Content content)
    {
        final DataType dataType;

        if (content instanceof BooleanContent) {
            dataType = DataType.BSTR1;
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
        return BinaryOutputVariation.ANY;
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<ObjectVariation> getInputVariation(final DataType dataType)
    {
        switch (dataType) {
            case BSTR1:
                return Optional.of(BinaryOutputVariation.WITH_FLAGS);
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
            case BSTR1:
                return Optional.of(BinaryOutputVariation.WITH_FLAGS);
            default:
                return Optional.empty();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public PointType getPointType()
    {
        return PointType.BINARY_OUTPUT;
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
