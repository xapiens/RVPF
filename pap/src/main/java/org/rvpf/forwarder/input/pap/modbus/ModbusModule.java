/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ModbusModule.java 3946 2019-05-02 13:49:32Z SFB $
 */

package org.rvpf.forwarder.input.pap.modbus;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.input.pap.PAPModule;
import org.rvpf.pap.PAPServer;
import org.rvpf.pap.modbus.Modbus;
import org.rvpf.pap.modbus.ModbusSupport;

/**
 * Modbus module.
 */
public final class ModbusModule
    extends PAPModule
{
    /** {@inheritDoc}
     */
    @Override
    protected Optional<ElapsedTime> getDefaultBatchWait()
    {
        return _DEFAULT_BATCH_WAIT;
    }

    /** {@inheritDoc}
     */
    @Override
    protected PAPInput newInput(final PAPServer server)
    {
        return new _ModbusInput(server);
    }

    /** {@inheritDoc}
     */
    @Override
    protected ModbusSupport newSupport()
    {
        return new ModbusSupport();
    }

    /** {@inheritDoc}
     */
    @Override
    protected KeyedGroups protocolProperties(final KeyedGroups moduleProperties)
    {
        return moduleProperties.getGroup(Modbus.PROPERTIES);
    }

    /** {@inheritDoc}
     */
    @Override
    protected String usage()
    {
        return Modbus.ATTRIBUTES_USAGE;
    }

    private static final Optional<ElapsedTime> _DEFAULT_BATCH_WAIT = Optional
        .of(ElapsedTime.fromRaw(1 * ElapsedTime.MINUTE.toRaw()));

    /**
     * Modbus input.
     */
    private final class _ModbusInput
        extends PAPInput
    {
        /**
         * Constructs an instance.
         *
         * @param server The server.
         */
        _ModbusInput(@Nonnull final PAPServer server)
        {
            super(server);
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "Modbus input";
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSourceName()
        {
            return "Modbus";
        }
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
