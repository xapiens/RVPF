/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CryptException.java 4067 2019-06-08 13:39:16Z SFB $
 */

package org.rvpf.base.security;

import javax.annotation.Nonnull;

import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;

/**
 * Crypt exception.
 */
public class CryptException
    extends Exception
{
    /**
     * Constructs an instance.
     */
    public CryptException()
    {
        super();
    }

    /**
     * Constructs an instance.
     *
     * @param message A detail message.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public CryptException(final String message)
    {
        super(message);
    }

    /**
     * Constructs an instance.
     *
     * @param cause The exception cause.
     */
    public CryptException(@Nonnull final Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructs an instance.
     *
     * @param messagesEntry A messages entry.
     * @param params The message parameters.
     */
    public CryptException(
            @Nonnull final Messages.Entry messagesEntry,
            @Nonnull final Object... params)
    {
        super(Message.format(messagesEntry, params));
    }

    /**
     * Constructs an instance.
     *
     * @param message A detail message.
     * @param cause The exception cause.
     *
     * @deprecated For temporary use.
     */
    @Deprecated
    public CryptException(
            @Nonnull final String message,
            @Nonnull final Throwable cause)
    {
        super(message, cause);
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
