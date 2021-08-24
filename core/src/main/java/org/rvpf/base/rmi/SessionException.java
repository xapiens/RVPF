/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SessionException.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.rmi;

import javax.annotation.Nonnull;

import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.exception.ThrowableProxy;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;

/**
 * Session exception.
 */
public class SessionException
    extends ServiceNotAvailableException
{
    /**
     * Constructs an instance.
     *
     * @param cause The exception cause.
     */
    public SessionException(@Nonnull final Exception cause)
    {
        super(cause);
    }

    /**
     * Constructs an instance.
     *
     * @param message The message.
     */
    public SessionException(@Nonnull final Message message)
    {
        super(message);
    }

    /**
     * Constructs an instance.
     *
     * @param cause The exception cause.
     */
    public SessionException(@Nonnull final ThrowableProxy cause)
    {
        super(cause);
    }

    /**
     * Constructs an instance.
     *
     * @param messageEntry The message entry.
     * @param params The message parameters.
     */
    public SessionException(
            @Nonnull final Messages.Entry messageEntry,
            @Nonnull final Object... params)
    {
        super(messageEntry, params);
    }

    /**
     * Constructs an instance.
     */
    protected SessionException() {}

    private static final long serialVersionUID = 1L;
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
