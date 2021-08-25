/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TimeRegister.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.pap.modbus.ModbusProxy;

/**
 * Time register.
 */
public final class TimeRegister
    extends DoubleWordRegister
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param remoteProxy The optional remote proxy.
     * @param readOnly True when read-only.
     * @param middleEndian True if the remote is middle-endian.
     */
    public TimeRegister(
            @Nonnull final Optional<Integer> address,
            @Nonnull final Optional<ModbusProxy> remoteProxy,
            final boolean readOnly,
            final boolean middleEndian)
    {
        super(address, Optional.empty(), readOnly, middleEndian);

        _stampRegister = new StampRegister(
            Optional.of(Integer.valueOf(address.get().intValue() + 2)),
            remoteProxy,
            readOnly,
            middleEndian);
    }

    /** {@inheritDoc}
     */
    @Override
    public short getContent()
    {
        final DateTime now = DateTime.now();
        final DateTime.Fields nowFields = now.toFields();
        final int year = nowFields.year % 100;
        final int month = nowFields.month;
        final int dayOfMonth = nowFields.day;
        final int hourOfDay = nowFields.hour;

        setContent((short) ((year << 8) + month));
        getNextRegister().setContent((short) ((dayOfMonth << 8) + hourOfDay));
        _stampRegister
            .setContent((short) (now.scaled(ElapsedTime.SECOND) % 3600));
        _stampRegister
            .getNextRegister()
            .setContent(
                (short) (now.scaled(ElapsedTime.MILLI.toRaw() / 10) % 10000));

        return super.getContent();
    }

    /**
     * Gets the stamp register.
     *
     * @return The stamp register.
     */
    @Nonnull
    @CheckReturnValue
    public StampRegister getStampRegister()
    {
        return _stampRegister;
    }

    private final StampRegister _stampRegister;
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
