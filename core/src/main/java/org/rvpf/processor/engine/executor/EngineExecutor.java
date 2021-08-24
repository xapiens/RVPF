/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EngineExecutor.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.processor.engine.executor;

import java.io.Serializable;

import java.util.List;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.Params;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.config.Config;

/**
 * Engine executor.
 */
public interface EngineExecutor
    extends Serializable
{
    /**
     * Closes this.
     */
    void close();

    /**
     * Disposes of a context.
     *
     * @param context The context.
     */
    void disposeContext(@Nonnull Serializable context);

    /**
     * Executes the processing of a request for a result value.
     *
     * @param resultValue The requested result.
     * @param params Result parameters.
     * @param context The context.
     *
     * @return A List of point values (null on failure).
     *
     * @throws InterruptedException When interrupted.
     * @throws ServiceNotAvailableException When the service is not available.
     */
    @Nullable
    @CheckReturnValue
    List<PointValue> execute(
            @Nonnull ResultValue resultValue,
            @Nonnull String[] params,
            @Nonnull Serializable context)
        throws InterruptedException, ServiceNotAvailableException;

    /**
     * Returns a new context.
     *
     * @param params The caller's parameters.
     * @param logger The caller's logger.
     *
     * @return The new context (null on failure).
     */
    @Nullable
    @CheckReturnValue
    Serializable newContext(@Nonnull Params params, @Nonnull Logger logger);

    /**
     * Sets up this engine executor.
     *
     * @param name The caller's name.
     * @param params The caller's parameters.
     * @param config The configuration.
     * @param logger The caller's logger.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull String name,
            @Nonnull Params params,
            @Nonnull Config config,
            @Nonnull Logger logger);

    /**
     * Tears down what has been set up.
     */
    void tearDown();
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
