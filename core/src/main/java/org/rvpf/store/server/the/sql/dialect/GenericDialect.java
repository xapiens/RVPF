/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: GenericDialect.java 4115 2019-08-04 14:17:56Z SFB $
 */
package org.rvpf.store.server.the.sql.dialect;

/** Generic database dialect support.
 */
public final class GenericDialect
    extends DialectSupport.Abstract
{
    /** Constructs an instance.
     */
    public GenericDialect()
    {
        super(DIALECT_NAME);
    }

    /** Dialect name. */
    public static final String DIALECT_NAME = "Generic";
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
