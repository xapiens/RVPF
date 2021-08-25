/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CRC16.java 3901 2019-02-20 14:06:46Z SFB $
 */

package org.rvpf.pap;

import java.util.zip.Checksum;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * CRC-16.
 */
public abstract class CRC16
    implements Checksum
{
    /** {@inheritDoc}
     */
    @Override
    public long getValue()
    {
        return 0xFFFF & _crc;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "0X" + Long.toHexString(getValue());
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

    /**
     * Creates a CRC-16 table.
     *
     * @param polynomial The polynomial for the table generation.
     *
     * @return The CRC table.
     */
    @Nonnull
    @CheckReturnValue
    protected static short[] table(final int polynomial)
    {
        final short[] table = new short[256];

        for (int i = 0; i < table.length; ++i) {
            int crc = 0;
            int c = i;

            for (int j = 0; j < 8; ++j) {
                crc = (((crc ^ c) & 1) != 0)
                      ? ((crc >> 1) ^ polynomial): (crc >> 1);
                c >>= 1;
            }

            table[i] = (short) crc;
        }

        return table;
    }

    /**
     * Resets the CRC.
     *
     * @param crc The starting value.
     */
    protected final void reset(final short crc)
    {
        _crc = crc;
    }

    /**
     * Updates the CRC.
     *
     * @param aByte A byte.
     * @param table The CRC table.
     */
    protected final void update(final byte aByte, @Nonnull final short[] table)
    {
        _crc = (short) (table[(_crc ^ aByte) & 0xFF] ^ ((_crc >> 8) & 0xFF));
    }

    private short _crc;
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
