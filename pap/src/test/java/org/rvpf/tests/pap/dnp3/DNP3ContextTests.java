/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3ContextTests.java 3926 2019-04-23 13:10:47Z SFB $
 */

package org.rvpf.tests.pap.dnp3;

import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.config.Config;
import org.rvpf.metadata.Metadata;
import org.rvpf.pap.dnp3.DNP3;
import org.rvpf.pap.dnp3.DNP3OutstationContext;
import org.rvpf.pap.dnp3.DNP3Support;
import org.rvpf.tests.pap.PAPContextTests;

import org.testng.annotations.BeforeMethod;

/**
 * DNP3 context tests.
 */
public final class DNP3ContextTests
    extends PAPContextTests
{
    /**
     * Constructs an instance.
     */
    protected DNP3ContextTests()
    {
        super(DNP3.ATTRIBUTES_USAGE);
    }

    /**
     * Sets up the outstation context.
     */
    @BeforeMethod
    public void setUpOutstationContext()
    {
        final DNP3Support support = new DNP3Support();

        _outstationContext = support
            .newServerContext(
                new Metadata(new Config("")),
                new String[0],
                Optional.empty());
        Require.notNull(_outstationContext);
    }

    /** {@inheritDoc}
     */
    @Override
    protected DNP3OutstationContext getServerContext()
    {
        return _outstationContext;
    }

    private DNP3OutstationContext _outstationContext;
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
