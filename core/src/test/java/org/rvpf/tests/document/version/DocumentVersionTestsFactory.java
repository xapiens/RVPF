/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentVersionTestsFactory.java 4057 2019-06-04 18:44:38Z SFB $
 */

package org.rvpf.tests.document.version;

import java.util.Optional;

import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.document.version.VersionControl;
import org.rvpf.tests.core.CoreTestsMessages;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.Factory;

/**
 * Document version tests factory.
 */
public final class DocumentVersionTestsFactory
    extends ServiceTests
{
    /**
     * Creates the tests.
     *
     * @return The tests.
     */
    @Factory
    public Object[] createTests()
    {
        final boolean enabled;

        if (getConfig().containsProperty(_SVN_CLASSLIB_PROPERTY)) {
            final KeyedGroups properties = getConfig()
                .getPropertiesGroup(VersionControl.DOCUMENT_VERSION_PROPERTIES);
            final Optional<String> workspace = properties
                .isMissing()? Optional
                    .empty(): properties
                        .getString(VersionControl.WORKSPACE_PROPERTY);

            enabled = workspace
                .isPresent()? getConfig()
                    .getFile(workspace.get())
                    .isDirectory(): false;
        } else {
            enabled = false;
        }

        final Object[] tests;

        if (enabled) {
            tests = new Object[] {new DocumentVersionTests(), };
        } else {
            getThisLogger().info(CoreTestsMessages.VERSION_CONTROL_NOT_ENABLED);
            tests = new Object[0];
        }

        return tests;
    }

    private static final String _SVN_CLASSLIB_PROPERTY = "svn.classlib";
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
