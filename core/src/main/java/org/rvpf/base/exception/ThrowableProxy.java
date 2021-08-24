/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ThrowableProxy.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.exception;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Throwable proxy.
 */
@Immutable
public final class ThrowableProxy
    extends Throwable
{
    /**
     * Constructs an instance.
     *
     * @param throwable The proxied throwable.
     */
    public ThrowableProxy(@Nonnull final Throwable throwable)
    {
        _name = throwable.getClass().getName();
        super.setStackTrace(throwable.getStackTrace());

        final Throwable cause = throwable.getCause();

        if (cause != null) {
            initCause(new ThrowableProxy(cause));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void setStackTrace(final StackTraceElement[] stackTrace)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final String message = getLocalizedMessage();

        return (message != null)? (_name + ": " + message): _name;
    }

    private static final long serialVersionUID = 1L;

    private final String _name;
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
