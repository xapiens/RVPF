/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServerSecurityContext.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.security;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.rvpf.base.logger.Logger;

/** Server security context.
 *
 * <p>Keeps security informations to help the creation of server sockets.</p>
 */
@ThreadSafe
public class ServerSecurityContext
    extends SecurityContext
{
    /** Constructs an instance.
     *
     * @param logger The logger instance to use.
     */
    public ServerSecurityContext(@Nonnull final Logger logger)
    {
        super(logger);
    }

    /** {@inheritDoc}
     */
    @Override
    public final boolean isServer()
    {
        return true;
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
