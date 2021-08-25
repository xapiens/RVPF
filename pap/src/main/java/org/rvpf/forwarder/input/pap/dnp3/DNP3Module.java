/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3Module.java 3946 2019-05-02 13:49:32Z SFB $
 */

package org.rvpf.forwarder.input.pap.dnp3;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.input.pap.PAPModule;
import org.rvpf.pap.PAPServer;
import org.rvpf.pap.dnp3.DNP3;
import org.rvpf.pap.dnp3.DNP3Support;

/**
 * DNP3 module.
 */
public final class DNP3Module
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
        return new _DNP3Input(server);
    }

    /** {@inheritDoc}
     */
    @Override
    protected DNP3Support newSupport()
    {
        return new DNP3Support();
    }

    /** {@inheritDoc}
     */
    @Override
    protected KeyedGroups protocolProperties(final KeyedGroups moduleProperties)
    {
        return moduleProperties.getGroup(DNP3.PROPERTIES);
    }

    /** {@inheritDoc}
     */
    @Override
    protected String usage()
    {
        return DNP3.ATTRIBUTES_USAGE;
    }

    private static final Optional<ElapsedTime> _DEFAULT_BATCH_WAIT = Optional
        .of(ElapsedTime.fromRaw(1 * ElapsedTime.MINUTE.toRaw()));

    /**
     * DNP3 input.
     */
    private final class _DNP3Input
        extends PAPInput
    {
        /**
         * Constructs an instance.
         *
         * @param outstation The outstation.
         */
        _DNP3Input(@Nonnull final PAPServer outstation)
        {
            super(outstation);
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "DNP3 input";
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSourceName()
        {
            return "DNP3";
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
