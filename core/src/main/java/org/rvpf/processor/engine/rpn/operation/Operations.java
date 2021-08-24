/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Operations.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.util.Map;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.Params;
import org.rvpf.base.logger.Logger;

/**
 * Operations.
 */
@NotThreadSafe
public abstract class Operations
{
    /**
     * Sets up the Operations.
     *
     * @param registrations The map holding the registrations.
     * @param params The engine's params.
     *
     * @throws Operation.OverloadException From set up.
     */
    public final void setUp(
            @Nonnull final Map<String, Operation> registrations,
            @Nonnull final Params params)
        throws Operation.OverloadException
    {
        _registrations = registrations;
        _params = params;

        setUp();
    }

    /**
     * Gets the Engine's Params.
     *
     * @return The Engine's Params.
     */
    @Nonnull
    @CheckReturnValue
    protected final Params getParams()
    {
        return _params;
    }

    /**
     * Gets the registrations.
     *
     * @return The registrations.
     */
    @Nonnull
    @CheckReturnValue
    protected final Map<String, Operation> getRegistrations()
    {
        return _registrations;
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return _logger;
    }

    /**
     * Sets up operations.
     *
     * @throws Operation.OverloadException From {@link Operation#register}.
     */
    protected abstract void setUp()
        throws Operation.OverloadException;

    private final Logger _logger = Logger.getInstance(getClass());
    private Params _params;
    private Map<String, Operation> _registrations;
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
