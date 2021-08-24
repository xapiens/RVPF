/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ConfigDocumentTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.document;

import java.util.Arrays;
import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.config.Config;
import org.rvpf.document.loader.ConfigDocumentLoader;
import org.rvpf.service.ServiceClassLoader;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Configuration document tests.
 */
public final class ConfigDocumentTests
    extends ServiceTests
{
    /**
     * Service config tests.
     */
    @Test(dependsOnMethods = {"anonymousConfigTests"})
    public static void serviceConfigTests()
    {
        final Config config = ConfigDocumentLoader
            .loadConfig(
                _TESTS_SERVICE_NAME,
                Optional.empty(),
                Optional.empty());

        Require.notNull(config);
        Require.present(config.getStringValue(_TESTS_SERVICE_PROPERTY));

        Require
            .success(
                config
                    .getStringValue(_TESTS_CONFIG_PROPERTY_VALUES)
                    .get()
                    .isEmpty());
    }

    /**
     * Sets up this.
     */
    @BeforeClass
    public static void setUp()
    {
        setSystemProperty(Config.RVPF_PROPERTIES, _TESTS_PROPERTIES);
        setSystemProperty(
            Config.SYSTEM_PROPERTY_PREFIX + _TESTS_SYS_PROPERTY,
            "sys-property-value");
    }

    /**
     * Anonymous config tests.
     */
    @Test
    public void anonymousConfigTests()
    {
        final String className = _TESTS_CLASS_EXAMPLE;
        Config config;

        // Configures a ServiceClassLoader with an empty URL[].
        config = new Config("");

        final ServiceClassLoader serviceClassLoader = ServiceClassLoader
            .getInstance(new ClassLoader(getClass().getClassLoader()) {}
        );

        config.setClassLoader(Optional.of(serviceClassLoader));

        Require
            .success(
                config.getStringValues(_TESTS_CONFIG_PROPERTY).length == 0);

        try {
            Class.forName(className, true, config.getClassLoader());
            Require
                .failure(
                    "Class '" + className + "' should not have been found!");
        } catch (final ClassNotFoundException exception) {
            // Required.
        }

        System.clearProperty(_TESTS_SET_SYS_PROPERTY);
        System.clearProperty(_TESTS_SET_SYS_PROPERTIES);

        System.setProperty(_TESTS_SYS_PROPERTIES, "*");

        try {
            config = ConfigDocumentLoader
                .loadConfig("", Optional.empty(), Optional.empty());
        } finally {
            System.clearProperty(_TESTS_SYS_PROPERTIES);
        }

        Require.notNull(config);
        Require
            .success(
                config.getStringValues(_TESTS_CONFIG_PROPERTY).length > 0);

        final String propertyValuesProperty = config
            .getStringValue(_TESTS_CONFIG_PROPERTY_VALUES)
            .get();
        final String[] propertyValues = propertyValuesProperty.split(",");

        Require
            .success(
                Arrays
                    .equals(
                            propertyValues,
                                    config
                                            .getStringValues(
                                                    _TESTS_CONFIG_PROPERTY)));

        try {
            Class.forName(className, true, config.getClassLoader());
        } catch (final Exception exception) {
            Require.failure("Class '" + className + "' should have been found!");
        }

        final String propertyValue = config
            .getStringValue(_TESTS_SYS_PROPERTY)
            .get();

        Require
            .equal(
                config.getStringValue(_TESTS_SYS_PROPERTY).get(),
                propertyValue);

        Require
            .equal(
                config
                    .substitute(
                            "Test ${" + _TESTS_SYS_PROPERTY + "} value",
                                    false),
                "Test " + propertyValue + " value");

        Require.notPresent(config.getStringValue(_TESTS_SERVICE_PROPERTY));

        Require.present(config.getStringValue(_TESTS_REFERENCE_GLOBAL));
        Require.present(config.getStringValue(_TESTS_REFERENCE_LOCAL));

        Require
            .equal(
                config.getStringValue(_TESTS_ENV_PROPERTY).get(),
                _TESTS_ENV_VALUE);

        final String vwop = config.getStringValue(_TESTS_VWOP_PROPERTY).get();

        Require.equal(config.substitute(_TESTS_DEFERRED_VALUE, false), vwop);
        Require
            .equal(
                config.substitute(_TESTS_DEFERRED_VALUE, true),
                _TESTS_DEFERRED_VALUE);

        Require
            .equal(
                System.getProperty(_TESTS_SET_SYS_PROPERTY),
                _TESTS_ENV_VALUE);
        Require
            .equal(
                System.getProperty(_TESTS_SET_SYS_PROPERTIES),
                _TESTS_ENV_VALUE);

        config.tearDown();
    }

    private static final String _TESTS_CLASS_EXAMPLE =
        "org.rvpf.tests.example.OperationsExample";
    private static final String _TESTS_CONFIG_PROPERTY =
        "tests.config.property";
    private static final String _TESTS_CONFIG_PROPERTY_VALUES =
        "tests.config.property.values";
    private static final String _TESTS_ENV_PROPERTY = "tests.env.value";
    private static final String _TESTS_ENV_VALUE = "OK";
    private static final String _TESTS_PROPERTIES =
        "rvpf-config-tests.properties";
    private static final String _TESTS_REFERENCE_GLOBAL =
        "tests.reference.global";
    private static final String _TESTS_REFERENCE_LOCAL =
        "tests.reference.local";
    private static final String _TESTS_SERVICE_NAME = "Tests";
    private static final String _TESTS_SERVICE_PROPERTY =
        "tests.service.property";
    private static final String _TESTS_SET_SYS_PROPERTIES =
        "rvpf.tests.set.sys.properties";
    private static final String _TESTS_SET_SYS_PROPERTY =
        "rvpf.tests.set.sys.property";
    private static final String _TESTS_SYS_PROPERTIES =
        "rvpf.tests.sys.properties";
    private static final String _TESTS_SYS_PROPERTY = "tests.sys.property";
    private static final String _TESTS_VWOP_PROPERTY =
        "tests.value.without.property";
    private static final String _TESTS_DEFERRED_VALUE = "$${"
        + _TESTS_VWOP_PROPERTY + "}";
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
