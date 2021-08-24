/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractMessage.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.logger.log4j;

import org.apache.logging.log4j.message.Message;

/**
 * Abstract message.
 */
public abstract class AbstractMessage
    implements Message
{
    /** {@inheritDoc}
     */
    @Override
    public final String getFormat()
    {
        return "";
    }

    /** {@inheritDoc}
     */
    @Override
    public final String getFormattedMessage()
    {
        return _formatted;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Object[] getParameters()
    {
        return null;
    }

    /** {@inheritDoc}
     */
    @Override
    public final Throwable getThrowable()
    {
        return null;
    }

    /**
     * Saves a formatted version of the message.
     */
    public final void saveFormatted()
    {
        _formatted = toString();
    }

    private static final long serialVersionUID = 1L;

    private volatile String _formatted;
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
