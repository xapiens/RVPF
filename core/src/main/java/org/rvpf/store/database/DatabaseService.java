/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DatabaseService.java 4115 2019-08-04 14:17:56Z SFB $
 */
package org.rvpf.store.database;

import java.io.File;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import org.rvpf.service.Service;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;

/** Database service.
 */
public interface DatabaseService
    extends Service
{
    /** Gets the connection URL.
     *
     * @return The connection URL.
     */
    @Nonnull
    @CheckReturnValue
    String getConnectionURL();

    /** Gets the database data directory.
     *
     * @return The database data directory.
     */
    @Nonnull
    @CheckReturnValue
    File getDatabaseDataDir();

    /** Gets a dialect support object.
     *
     * <p>Used by tests procedures.</p>
     *
     * @return The dialect support object.
     */
    @Nonnull
    @CheckReturnValue
    DialectSupport getDialectSupport();
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
