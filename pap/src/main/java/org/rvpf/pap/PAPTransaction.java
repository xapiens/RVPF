/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPTransaction.java 4083 2019-06-15 12:53:56Z SFB $
 */

package org.rvpf.pap;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.exception.ConnectFailedException;

/**
 * PAP transaction.
 */
public interface PAPTransaction
{
    /**
     * Request.
     */
    interface Request
    {
        /**
         * Gets the response.
         *
         * @return The response (may be empty).
         *
         * @throws InterruptedException When interrupted.
         * @throws ConnectFailedException When connect failed.
         */
        @Nonnull
        @CheckReturnValue
        Optional<? extends PAPTransaction.Response> getResponse()
            throws InterruptedException, ConnectFailedException;

        /**
         * Waits for a response.
         *
         * @return True unless this request has failed.
         *
         * @throws InterruptedException When interrupted.
         * @throws ConnectFailedException When connect failed.
         */
        @CheckReturnValue
        boolean waitForResponse()
            throws InterruptedException, ConnectFailedException;
    }


    /**
     * Response.
     */
    interface Response
    {
        /**
         * Asks if this is a success response.
         *
         * @return True when this is a success response.
         */
        @CheckReturnValue
        boolean isSuccess();
    }
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
