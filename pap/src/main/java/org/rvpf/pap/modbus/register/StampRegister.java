/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StampRegister.java 3978 2019-05-12 10:53:03Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.value.PointValue;
import org.rvpf.pap.modbus.ModbusProxy;

/**
 * Stamp register.
 */
public final class StampRegister
    extends DoubleWordRegister
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param remoteProxy The optional remote proxy.
     * @param readOnly True when read-only.
     * @param middleEndian True if the node is middle-endian.
     */
    public StampRegister(
            @Nonnull final Optional<Integer> address,
            @Nonnull final Optional<ModbusProxy> remoteProxy,
            final boolean readOnly,
            final boolean middleEndian)
    {
        super(address, Optional.empty(), readOnly, middleEndian);

        _remoteProxy = remoteProxy.orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] getPointValues()
    {
        final DateTime now = DateTime.now();
        final int seconds = (int) (now.scaled(ElapsedTime.SECOND) % 3600);
        DateTime stamp = now.floored(ElapsedTime.HOUR);

        if (seconds < 900) {
            if (getContent() > 2700) {
                stamp = stamp.before(ElapsedTime.HOUR);
            }
        } else if (seconds > 2700) {
            if (getContent() < 900) {
                stamp = stamp.after(ElapsedTime.HOUR);
            }
        }

        if (_remoteProxy != null) {
            stamp = stamp
                .after((getContent() * ElapsedTime.SECOND.toRaw())
                       + (getNextRegister().getContent()
                           * (ElapsedTime.MILLI.toRaw() / 10)));
            _remoteProxy.setStamp(Optional.of(stamp));
        }

        return NO_POINT_VALUES;
    }

    /** {@inheritDoc}
     */
    @Override
    public void putPointValue(final PointValue pointValue) {}

    /** {@inheritDoc}
     */
    @Override
    public void setContent(final short content)
    {
        super.setContent(content);

        getNextRegister().setContent((short) 0);
    }

    private final ModbusProxy _remoteProxy;
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
