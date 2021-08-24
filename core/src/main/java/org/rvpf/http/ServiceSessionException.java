/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceSessionException.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.http;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.LoginFailedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.rmi.UnauthorizedAccessException;

/** Service session exception.
 */
public class ServiceSessionException
    extends SessionException
{
    public ServiceSessionException(@Nonnull final SessionException exception)
    {
        super(exception);

        if ((exception instanceof LoginFailedException)
                || (exception instanceof UnauthorizedAccessException)) {
            _statusCode = HttpServletResponse.SC_UNAUTHORIZED;
        } else {
            Logger.getInstance(getClass())
                .warn(exception, BaseMessages.VERBATIM, exception.getMessage());
            _statusCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        }
    }

    /** Gets the status code.
     *
     * @return The status code.
     */
    @CheckReturnValue
    public int getStatusCode()
    {
        return _statusCode;
    }

    private static final long serialVersionUID = 1L;

    private final int _statusCode;
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
