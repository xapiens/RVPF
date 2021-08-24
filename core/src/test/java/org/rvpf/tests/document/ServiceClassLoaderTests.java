/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceClassLoaderTests.java 4111 2019-07-29 14:49:57Z SFB $
 */

package org.rvpf.tests.document;

import java.io.File;

import org.rvpf.base.tool.Require;
import org.rvpf.config.entity.ClassLibEntity;
import org.rvpf.service.ServiceClassLoader;

import org.testng.annotations.Test;

/**
 * Service class loader tests.
 */
public final class ServiceClassLoaderTests
{
    private ServiceClassLoaderTests() {}

    /**
     * Should be able to find a class using a class library.
     *
     * @throws Exception On failure.
     */
    @Test
    public static void shouldFindWithClassLib()
        throws Exception
    {
        final ServiceClassLoader classLoader;
        final String className;
        Exception catchedException = null;

        // Given a new class loader provided with a class library
        classLoader = _newServiceClassLoader();
        classLoader
            .addFromClassLib(
                ClassLibEntity
                    .newBuilder()
                    .addLocation(new File(_TESTS_CLASSES_EXAMPLE_DIR).toURI())
                    .build());

        // and a class in the class library directory,
        className = _TESTS_CLASS_EXAMPLE;

        // when asking for the class by its name,
        try {
            Class.forName(className, true, classLoader);
        } catch (final ClassNotFoundException exception) {
            catchedException = exception;
        }

        // then it should succeed.
        Require.success(catchedException, "class not found");
    }

    /**
     * Should need a class library when not in class path.
     */
    @Test
    public static void shouldNeedClassLib()
    {
        final ServiceClassLoader classLoader;
        final String className;
        Exception catchedException = null;

        // Given a new class loader
        classLoader = _newServiceClassLoader();

        // and a class in a directory not in the class path,
        className = _TESTS_CLASS_EXAMPLE;

        // when asking for the class by its name,
        try {
            Class.forName(className, true, classLoader);
        } catch (final ClassNotFoundException exception) {
            catchedException = exception;
        }

        // then it should fail.
        Require.notNull(catchedException, "class not found");
    }

    private static ServiceClassLoader _newServiceClassLoader()
    {
        return ServiceClassLoader
            .getInstance(
                new ClassLoader(
                    ServiceClassLoaderTests.class.getClassLoader()) {}
        );
    }

    private static final String _TESTS_CLASSES_EXAMPLE_DIR;
    private static final String _TESTS_CLASS_EXAMPLE =
        "org.rvpf.tests.example.OperationsExample";

    static {
        _TESTS_CLASSES_EXAMPLE_DIR = System.getenv("RVPF_TESTS_CLASSES");
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
