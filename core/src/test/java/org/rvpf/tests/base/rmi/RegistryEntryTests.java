/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RegistryEntryTests.java 4099 2019-06-26 21:33:35Z SFB $
 */

package org.rvpf.tests.base.rmi;

import java.net.URI;

import java.util.Optional;

import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;
import org.rvpf.tests.Tests;

import org.testng.annotations.Test;

/**
 * Registry entry tests.
 */
public class RegistryEntryTests
    extends Tests
{
    /**
     * Should work with the binding.
     */
    @Test
    public static void shouldWorkWithTheBinding()
    {
        final String name = "TestName";
        final String binding;
        final RegistryEntry registryEntry;

        // Given the binding,
        binding = "rmi://" + Inet.LOCAL_HOST + ":1234/" + name;

        // when creating a registry entry,
        registryEntry = Require
            .notNull(
                RegistryEntry
                    .newBuilder()
                    .setBinding(Optional.of(binding))
                    .build());

        // then the URI should be complete
        Require.equal(registryEntry.getURI(), URI.create(binding));

        // and the name and path should be available.
        Require.equal(registryEntry.getName(), name);
        Require.equal(registryEntry.getPath(), name);
    }

    /**
     * Should work with the binding and name.
     */
    @Test
    public static void shouldWorkWithTheBindingAndName()
    {
        final String binding;
        final String name;
        final RegistryEntry registryEntry;

        // Given the binding and the name,
        binding = "//" + Inet.LOCAL_HOST + ":5678";
        name = "TestName";

        // when creating a registry entry,
        registryEntry = Require
            .notNull(
                RegistryEntry
                    .newBuilder()
                    .setBinding(Optional.of(binding))
                    .setName(Optional.of(name))
                    .build());

        // then the URI should be complete
        Require
            .equal(
                registryEntry.getURI(),
                URI.create("rmi:" + binding + "/" + name));

        // and the name and path should be unchanged.
        Require.equal(registryEntry.getName(), name);
        Require.equal(registryEntry.getPath(), name);
    }

    /**
     * Should work with the binding, name and prefix.
     */
    @Test
    public static void shouldWorkWithTheBindingNameAndPrefix()
    {
        final String binding;
        final String name;
        final String prefix;
        final RegistryEntry registryEntry;

        // Given the binding, the name and a prefix,
        binding = "//" + Inet.LOCAL_HOST + ":5678";
        name = "TestName";
        prefix = "TestPrefix";

        // when creating a registry entry,
        registryEntry = Require
            .notNull(
                RegistryEntry
                    .newBuilder()
                    .setBinding(Optional.of(binding))
                    .setName(Optional.of(name))
                    .setDefaultPrefix(prefix)
                    .build());

        // then the URI should be complete
        Require
            .equal(
                registryEntry.getURI(),
                URI.create("rmi:" + binding + "/" + prefix + "/" + name));

        // and the name and path should be unchanged.
        Require.equal(registryEntry.getName(), name);
        Require.equal(registryEntry.getPath(), prefix + "/" + name);
    }

    /**
     * Should work with the name.
     */
    @Test
    public static void shouldWorkWithTheName()
    {
        final String name;
        final RegistryEntry registryEntry;

        // Given the name,
        name = "TestName";

        // when creating a registry entry,
        registryEntry = Require
            .notNull(
                RegistryEntry.newBuilder().setName(Optional.of(name)).build());

        // then the URI should be complete
        Require
            .equal(
                registryEntry.getURI(),
                URI.create("rmi://" + Inet.LOCAL_HOST + "/" + name));

        // and the name and path should be unchanged.
        Require.equal(registryEntry.getName(), name);
        Require.equal(registryEntry.getPath(), name);
    }

    /**
     * Should work with the name and prefix.
     */
    @Test
    public static void shouldWorkWithTheNameAndPrefix()
    {
        final String name;
        final String prefix;
        final RegistryEntry registryEntry;

        // Given the name and prefix,
        name = "TestName";
        prefix = "TestPrefix";

        // when creating a registry entry,
        registryEntry = Require
            .notNull(
                RegistryEntry
                    .newBuilder()
                    .setName(Optional.of(name))
                    .setDefaultPrefix(prefix)
                    .build());

        // then the URI should be complete
        Require
            .equal(
                registryEntry.getURI(),
                URI
                    .create("rmi://" + Inet.LOCAL_HOST + "/" + prefix + "/"
                    + name));

        // and the name and path should be unchanged.
        Require.equal(registryEntry.getName(), name);
        Require.equal(registryEntry.getPath(), prefix + "/" + name);
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
