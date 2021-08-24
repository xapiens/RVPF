/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DatabaseServiceImpl.java 4115 2019-08-04 14:17:56Z SFB $
 */
package org.rvpf.store.database;

import java.io.File;
import org.rvpf.service.app.ServiceApp;
import org.rvpf.service.app.ServiceAppHolderImpl;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;

/** Database service implementation.
 */
public final class DatabaseServiceImpl
    extends ServiceAppHolderImpl
    implements DatabaseService
{
    /** {@inheritDoc}
     */
    @Override
    public String getConnectionURL()
    {
        return ((DatabaseServiceAppImpl) getServiceApp()).getConnectionURL();
    }

    /** {@inheritDoc}
     */
    @Override
    public File getDatabaseDataDir()
    {
        return ((DatabaseServiceAppImpl) getServiceApp()).getDatabaseDataDir();
    }

    /** {@inheritDoc}
     */
    @Override
    public DialectSupport getDialectSupport()
    {
        return ((DatabaseServiceAppImpl) getServiceApp()).getDialectSupport();
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceApp newServiceApp()
    {
        return new DatabaseServiceAppImpl();
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
