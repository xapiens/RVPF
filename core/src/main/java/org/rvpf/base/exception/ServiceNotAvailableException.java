/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceNotAvailableException.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.exception;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;

/**
 * Service not available exception.
 */
@Immutable
public class ServiceNotAvailableException
    extends Exception
{
    /**
     * Constructs an instance.
     */
    public ServiceNotAvailableException() {}

    /**
     * Constructs an instance.
     *
     * @param cause The exception cause.
     */
    public ServiceNotAvailableException(@Nonnull final Exception cause)
    {
        super(cause);
    }

    /**
     * Constructs an instance.
     *
     * @param message The message.
     */
    public ServiceNotAvailableException(@Nonnull final Message message)
    {
        super(message.toString());
    }

    /**
     * Constructs an instance.
     *
     * @param cause The exception cause.
     */
    public ServiceNotAvailableException(@Nonnull final ThrowableProxy cause)
    {
        super(cause);
    }

    /**
     * Constructs an instance.
     *
     * @param messageEntry The message entry.
     * @param params The message parameters.
     */
    public ServiceNotAvailableException(
            @Nonnull final Messages.Entry messageEntry,
            @Nonnull final Object... params)
    {
        super(Message.format(messageEntry, params));
    }

    /**
     * Constructs an instance.
     *
     * @param cause The exception cause.
     * @param messageEntry The message entry.
     * @param params The message parameters.
     */
    public ServiceNotAvailableException(
            @Nonnull final Exception cause,
            @Nonnull final Messages.Entry messageEntry,
            @Nonnull final Object... params)
    {
        super(Message.format(messageEntry, params), cause);
    }

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
