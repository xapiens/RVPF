/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: G40V0.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3.object.groupCategory.analogOutputs;

import java.nio.ByteBuffer;

import org.rvpf.pap.dnp3.object.ObjectInstance;
import org.rvpf.pap.dnp3.object.ObjectVariation;

/**
 * Any Analog Output Status.
 */
public final class G40V0
    extends ObjectInstance.Abstract
{
    /** {@inheritDoc}
     */
    @Override
    public void dumpToBuffer(final ByteBuffer buffer) {}

    /** {@inheritDoc}
     */
    @Override
    public int getObjectLength()
    {
        return 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public ObjectVariation getObjectVariation()
    {
        return AnalogOutputStatusVariation.ANY;
    }

    /** {@inheritDoc}
     */
    @Override
    public void loadFromBuffer(final ByteBuffer buffer) {}
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
