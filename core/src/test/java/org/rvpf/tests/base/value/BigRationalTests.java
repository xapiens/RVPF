/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BigRationalTests.java 4099 2019-06-26 21:33:35Z SFB $
 */

package org.rvpf.tests.base.value;

import java.math.BigInteger;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.BigRational;
import org.rvpf.tests.Tests;

import org.testng.annotations.Test;

/**
 * Big rational tests.
 */
public final class BigRationalTests
    extends Tests
{
    /**
     * Should reduce to canonical.
     */
    @Test(priority = 10)
    public static void shouldReduceToCanonical()
    {
        final BigRational fromSmall;
        final BigRational fromLarge;

        // Given a numerator and denominator with a factor > 1,
        // when producing a big rational with these,
        fromSmall = BigRational.valueOf(2, -4);
        fromLarge = BigRational
            .valueOf(
                new BigInteger("200000000000000000000"),
                new BigInteger("-400000000000000000000"));

        // then the rational should have reduced numerator / denominator.
        Require.equal(fromSmall.getNumerator(), _bigInteger(-1), "numerator");
        Require
            .equal(fromSmall.getDenominator(), _bigInteger(2), "denominator");
        Require
            .equal(
                fromSmall,
                BigRational
                    .valueOf(BigInteger.valueOf(-1), BigInteger.valueOf(2)),
                "rational from small");
        Require
            .equal(
                fromLarge,
                BigRational
                    .valueOf(BigInteger.valueOf(-1), BigInteger.valueOf(2)),
                "rational from large");
    }

    /**
     * Should support add to big rational.
     */
    @Test(priority = 30)
    public static void shouldSupportAddBigRational()
    {
        final BigRational rational;
        final BigRational result;

        // Given a rational value,
        rational = BigRational.valueOf(3, 4);

        // when adding a rational value,
        result = rational.add(BigRational.valueOf(1, 2));

        // then it should produce the expected value.
        Require
            .equal(
                result,
                BigRational
                    .valueOf(BigInteger.valueOf(5), BigInteger.valueOf(4)),
                "rational result");
    }

    /**
     * Should support multiply by big rational.
     */
    @Test(priority = 30)
    public static void shouldSupportMultiplyByBigRational()
    {
        // Given a rational value,
        final BigRational rational = BigRational
            .valueOf(
                (long) Integer.MAX_VALUE + 1,
                (long) Integer.MAX_VALUE + 2);

        // when multiplying by a big rational value,
        final BigRational smallResult = rational
            .multiply(
                BigRational
                    .valueOf(
                            (long) Integer.MAX_VALUE + 3,
                                    (long) Integer.MAX_VALUE + 4));
        final BigRational mediumResult = smallResult
            .multiply(
                BigRational
                    .valueOf(
                            (long) Integer.MAX_VALUE + 5,
                                    (long) Integer.MAX_VALUE + 6));
        final BigRational largeResult = mediumResult.multiply(smallResult);

        // then it should produce the expected value.
        Require
            .equal(
                smallResult,
                BigRational
                    .valueOf(
                            new BigInteger("4611686022722355200"),
                                    new BigInteger("4611686027017322499")),
                "small result");
        Require
            .equal(
                mediumResult,
                BigRational
                    .valueOf(
                            new BigInteger("3301173447317719442312396800"),
                                    new BigInteger(
                                            "3301173451929405471477202949")),
                "medium result");
        Require
            .equal(
                largeResult,
                BigRational
                    .valueOf(
                            new BigInteger(
                                    "15223975445577299950801596371172989452943360000"),
                                    new BigInteger(
                                            "15223975481023379977048637563296751999606849551")));
    }

    private static BigInteger _bigInteger(final long integer)
    {
        return BigInteger.valueOf(integer);
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
