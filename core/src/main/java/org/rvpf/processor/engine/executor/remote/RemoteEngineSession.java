/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteEngineSession.java 4018 2019-05-23 13:32:33Z SFB $
 */

package org.rvpf.processor.engine.executor.remote;

import java.io.Serializable;

import java.rmi.RemoteException;

import java.util.List;

import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.Session;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.config.Config;

/**
 * Remote Engine Session.
 */
public interface RemoteEngineSession
    extends Session
{
    /**
     * Disposes of a context.
     *
     * @param context The context.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException On session problems.
     */
    void disposeContext(
            @Nonnull Serializable context)
        throws RemoteException, SessionException;

    /**
     * Executes the processing of a request for a result value.
     *
     * @param resultValue The requested result.
     * @param params Result parameters.
     * @param context The context.
     *
     * @return A List of point values (null on failure).
     *
     * @throws ServiceNotAvailableException From session security check.
     * @throws InterruptedException When interrupted.
     * @throws RemoteException From RMI RunTime.
     */
    List<PointValue> execute(
            ResultValue resultValue,
            String[] params,
            Serializable context)
        throws ServiceNotAvailableException, InterruptedException,
               RemoteException;

    /**
     * Returns a new context.
     *
     * @param params The caller's params.
     * @param logger The caller's logger.
     *
     * @return The new context.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException On session problems.
     */
    Serializable newContext(
            Params params,
            Logger logger)
        throws RemoteException, SessionException;

    /**
     * Sets up this engine executor.
     *
     * @param name The caller's name.
     * @param params The caller's params.
     * @param config The config.
     * @param logger The caller's logger.
     *
     * @return True on success.
     *
     * @throws SessionException From session security check.
     * @throws RemoteException From RMI RunTime.
     */
    boolean setUp(
            String name,
            Params params,
            Config config,
            Logger logger)
        throws SessionException, RemoteException;

    /**
     * Tears down what has been set up.
     *
     * @throws RemoteException From RMI RunTime.
     */
    void tearDown()
        throws RemoteException;
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
