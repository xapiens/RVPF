/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SequenceRegister.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.modbus.register;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.value.PointValue;
import org.rvpf.pap.modbus.ModbusProxy;

/**
 * Sequence register.
 */
public final class SequenceRegister
    extends WordRegister
{
    /**
     * Constructs an instance.
     *
     * @param address The optional register address.
     * @param remoteProxy The optional remote proxy.
     * @param readOnly True when read-only.
     */
    public SequenceRegister(
            @Nonnull final Optional<Integer> address,
            @Nonnull final Optional<ModbusProxy> remoteProxy,
            final boolean readOnly)
    {
        super(address, Optional.empty(), readOnly);

        _remoteProxy = remoteProxy.orElse(null);
    }

    /** {@inheritDoc}
     */
    @Override
    public PointValue[] getPointValues()
    {
        if (_remoteProxy != null) {
            _remoteProxy.setSequence(getContent() & 0xFFFF);
        }

        return NO_POINT_VALUES;
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
