/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LRC.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.pap.modbus.transport;

import java.util.zip.Checksum;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/** LRC.
 */
public class LRC
    implements Checksum
{
    /** {@inheritDoc}
     */
    @Override
    public long getValue()
    {
        return -_lrc & 0xFF;
    }

    /** {@inheritDoc}
     */
    @Override
    public void reset()
    {
        _lrc = 0;
    }

    /** {@inheritDoc}
     */
    @Override
    public void update(final int b)
    {
        _lrc += b;
    }

    /** {@inheritDoc}
     */
    @Override
    public final void update(
            @Nonnull final byte[] bytes,
            @Nonnegative int offset,
            @Nonnegative int length)
    {
        while (--length >= 0) {
            update(bytes[offset++]);
        }
    }

    private byte _lrc;
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
