/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DNP3ProtocolException.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap.dnp3;

import java.net.ProtocolException;

import javax.annotation.Nonnull;

import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;

/**
 * DNP3 protocol exception.
 */
public class DNP3ProtocolException
    extends ProtocolException
{
    /**
     * Constructs an instance.
     *
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public DNP3ProtocolException(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        super(Message.format(entry, params));
    }

    /**
     * Constructs an instance.
     *
     * @param cause The exception cause.
     * @param entry The messages entry.
     * @param params The message parameters.
     */
    public DNP3ProtocolException(
            @Nonnull final Throwable cause,
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
    {
        this(entry, params);

        initCause(cause);
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
