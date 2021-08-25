/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.types;

import etherip.protocol.Protocol;

/**
 * Control Net Path
 * <p>
 * Example (with suitable static import):
 * <p>
 * <code>CNPath path = Identity().instance(1).attr(7)</code>
 *
 * @author Kay Kasemir, László Pataki
 */
@SuppressWarnings("javadoc")
abstract public class CNPath
    implements Protocol
{
    /**
     * Create path to ConnectionManager object
     *
     * @return {@link CNPath}
     */
    public static CNPath ConnectionManager()
    {
        return new CNClassPath(0x06, "ConnectionManager");
    }

    public static CNClassPath ConnectionObject()
    {
        return new CNClassPath(0x05, "ConnectionObject");
    }

    public static CNClassPath EthernetLink()
    {
        return new CNClassPath(0xf6, "Ethernet Link");
    }

    // Objects, see Spec 10 p. 1, 13, 25

    /**
     * Create path to Identity object
     *
     * @return {@link CNClassPath}
     */
    public static CNClassPath Identity()
    {
        return new CNClassPath(0x01, "Identity");
    }

    /**
     * Create path to MessageRouter object
     *
     * @return {@link CNClassPath}
     */
    public static CNClassPath MessageRouter()
    {
        return new CNClassPath(0x02, "MessageRouter");
    }

    public static CNClassPath Port()
    {
        return new CNClassPath(0xf4, "Port");
    }

    /**
     * Create symbol path
     *
     * @param name
     *            Name of the tag to put into symbol path
     * @return {@link CNPath}
     */
    public static CNSymbolPath Symbol(final String path)
    {
        return new CNSymbolPath(path);
    }

    /*
     * Logix5000 Data Access Class to read symbol names and types
     */
    public static CNClassPath SymbolList()
    {
        return new CNClassPath(0x6B, "Symbol List").instance(0);
    }

    public static CNClassPath TcpIpInterface()
    {
        return new CNClassPath(0xf5, "TCP/IP Interface");
    }

    /*
     * Logix5000 Data Access Class to read template attributes
     */
    public static CNClassPath TemplateAttributes()
    {
        return new CNClassPath(0x6C, "Template attributes");
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
