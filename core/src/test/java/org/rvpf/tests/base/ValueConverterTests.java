/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValueConverterTests.java 4112 2019-08-02 20:00:26Z SFB $
 */

package org.rvpf.tests.base;

import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.tests.Tests;

import org.testng.annotations.Test;

/**
 * Value converter tests.
 */
public class ValueConverterTests
    extends Tests
{
    /**
     * The 'canonicalize' method should compose decomposed.
     */
    @Test(priority = 20)
    public static void canonicalizeShouldComposeDecomposed()
    {
        final Optional<String> input;
        final Optional<String> result;

        // Given a decomposed value for input,
        input = Optional.of("\u0041\u0301");

        // which should be seen as not canonical,
        Require.failure(ValueConverter.isCanonical(input));

        // when proceeding with canonicalize,
        result = ValueConverter.canonicalizeString(input);

        // then is should return a composed value
        Require.equal("\u00C1", result.get());

        // and see it as canonical.
        Require.success(ValueConverter.isCanonical(result));
    }

    /**
     * The 'canonicalize' method should return empty for empty.
     */
    @Test(priority = 10)
    public static void canonicalizeShouldReturnEmptyForEmpty()
    {
        final Optional<String> input;
        final Optional<String> result;

        // Given an empty value for  input,
        input = Optional.empty();

        // when proceeding with canonicalize,
        result = ValueConverter.canonicalizeString(input);

        // then it should return an empty value
        Require.notPresent(result);

        // and see it as canonical.
        Require.success(ValueConverter.isCanonical(result));
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
