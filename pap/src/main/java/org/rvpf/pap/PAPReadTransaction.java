/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PAPReadTransaction.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.pap;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Point;
import org.rvpf.base.value.PointValue;

/**
 * PAP read transaction.
 */
public interface PAPReadTransaction
    extends PAPTransaction
{
    /**
     * Request.
     */
    interface Request
        extends PAPTransaction.Request
    {
        /**
         * Gets the point for the request.
         *
         * @return The point.
         */
        @Nonnull
        @CheckReturnValue
        Point getPoint();
    }


    /**
     * Response.
     */
    interface Response
        extends PAPTransaction.Response
    {
        /**
         * Gets the point value from a response.
         *
         * @return The optional point value.
         */
        @Nonnull
        @CheckReturnValue
        Optional<PointValue> getPointValue();
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
