/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CStoreServiceTestsFactory.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.tests.store;

import org.rvpf.store.server.c.CStoreServiceActivator;
import org.rvpf.tests.Tests;
import org.rvpf.tests.core.CoreTestsMessages;
import org.testng.annotations.Factory;

/** C store service tests factory.
 */
public final class CStoreServiceTestsFactory
    extends Tests
{
    /** Creates the tests.
     *
     * @return The tests.
     */
    @Factory
    public Object[] createTests()
    {
        final Object[] tests;

        if (CStoreServiceActivator.isImplemented()) {
            tests = new Object[] {new CStoreServiceTests(),};
        } else {
            getThisLogger().info(CoreTestsMessages.NATIVE_NOT_FOUND);
            tests = new Object[0];
        }

        return tests;
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
