/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteEngineFactory.java 4018 2019-05-23 13:32:33Z SFB $
 */

package org.rvpf.processor.engine.executor.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.base.rmi.SessionException;

/**
 * Remote Engine (session) Factory.
 */
public interface RemoteEngineFactory
    extends Remote
{
    /**
     * Creates a remote engine session.
     *
     * @param uuid A UUID used to locate the RMI client instance.
     * @param clientName A descriptive name for the client.
     *
     * @return The new agent session.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException When the session creation fails.
     */
    @Nonnull
    @CheckReturnValue
    RemoteEngineSession createEngineSession(
            @Nonnull UUID uuid,
            @Nonnull String clientName)
        throws RemoteException, SessionException;
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
