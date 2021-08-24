/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PublisherSession.java 3902 2019-02-20 22:30:12Z SFB $
 */

package org.rvpf.base.som;

import java.io.Serializable;

import java.rmi.RemoteException;

import javax.annotation.Nonnull;

import org.rvpf.base.rmi.SessionException;

/**
 * Publisher session.
 */
public interface PublisherSession
    extends SOMSession
{
    /**
     * Sends messages.
     *
     * @param messages The messages.
     *
     * @throws RemoteException From RMI RunTime.
     * @throws SessionException From session security check.
     */
    void send(@Nonnull Serializable[] messages)
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
