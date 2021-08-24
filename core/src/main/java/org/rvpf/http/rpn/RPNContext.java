/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RPNContext.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.http.rpn;

import org.rvpf.http.HTTPModule;
import org.rvpf.metadata.Metadata;
import org.rvpf.processor.engine.rpn.RPNExecutor;

/** RPN context.
 */
class RPNContext
    extends HTTPModule.Context
{
    /** Constructs an instance.
     *
     * @param metadata The metadata.
     * @param executor The executor.
     */
    RPNContext(final Metadata metadata, final RPNExecutor executor)
    {
        _metadata = metadata;
        _executor = executor;
    }

    /** Gets the executor.
     *
     * @return The executor.
     */
    RPNExecutor getExecutor()
    {
        return _executor;
    }

    /** Gets the metadata.
     *
     * @return The metadata.
     */
    Metadata getMetadata()
    {
        return _metadata;
    }

    private final RPNExecutor _executor;
    private final Metadata _metadata;
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
