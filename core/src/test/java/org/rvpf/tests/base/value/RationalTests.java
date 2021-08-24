/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RationalTests.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.tests.base.value;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.Rational;
import org.rvpf.tests.Tests;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Rational tests.
 */
public final class RationalTests
    extends Tests
{
    /**
     * Provides string to rational values.
     *
     * @return A string, then the values for the expected rational.
     */
    @DataProvider
    public static Object[][] provideStringToRational()
    {
        return new Object[][] {
            // 2/4 --> 1/2
            {"2 / 4", _long(1), _long(2), },

            // 2/-4 --> -1/2
            {"2 / -4", _long(-1), _long(2), },

            // -2/-4 --> 1/2
            {"-2/-4", _long(1), _long(2), },
        };
    }

    /**
     * Should convert string to rational.
     *
     * @param string A string representation for the rational value.
     * @param numerator The numerator of the expected rational.
     * @param denominator The denominator of the expected rational.
     */
    @Test(
        dataProvider = "provideStringToRational",
        priority = 20
    )
    public static void shouldConvertStringToRational(
            final String string,
            final Long numerator,
            final Long denominator)
    {
        final Rational rational;

        // Given a valid string representation of a rational value,
        // when converting this string to a rational value,
        rational = Rational.valueOf(string);

        // then it should be the expected rational value.
        Require
            .equal(
                rational,
                Rational
                    .valueOf(numerator.longValue(), denominator.longValue()),
                "rational");
    }

    /**
     * Should reduce to canonical.
     */
    @Test(priority = 10)
    public static void shouldReduceToCanonical()
    {
        final Rational rational;

        // Given a numerator and denominator with a factor > 1,
        // when producing a rational with these,
        rational = Rational.valueOf(2, -4);

        // then the rational should have reduced numerator / denominator.
        Require.success(rational.getNumerator() == -1, "numerator");
        Require.success(rational.getDenominator() == 2, "denominator");
        Require.equal(rational, Rational.valueOf(-1, 2), "rational");
    }

    /**
     * Should support add to long.
     */
    @Test(priority = 30)
    public static void shouldSupportAddLong()
    {
        final Rational rational;
        final Rational result;

        // Given a rational value,
        rational = Rational.valueOf(3, 4);

        // when adding a long value,
        result = rational.add(2);

        // then it should produce the expected value.
        Require.equal(result, Rational.valueOf(11, 4), "rational result");
    }

    /**
     * Should support add to rational.
     */
    @Test(priority = 30)
    public static void shouldSupportAddRational()
    {
        final Rational rational;
        final Rational result;

        // Given a rational value,
        rational = Rational.valueOf(3, 4);

        // when adding a rational value,
        result = rational.add(Rational.valueOf(1, 2));

        // then it should produce the expected value.
        Require.equal(result, Rational.valueOf(5, 4), "rational result");
    }

    /**
     * Tests Rational.
     */
    @Test(priority = 31)
    public static void shouldSupportLargeMultiply()
    {
        final Rational multiplicand;
        final Rational multiplicator;
        final Rational result;

        // Given a large multiplicand
        // and a large multiplicator,
        multiplicand = Rational
            .valueOf(
                (long) Integer.MAX_VALUE + 1,
                (long) Integer.MAX_VALUE + 2);
        multiplicator = Rational
            .valueOf(
                (long) Integer.MAX_VALUE + 3,
                (long) Integer.MAX_VALUE + 4);

        // when multiplying the multiplicand by the multiplier,
        result = multiplicand.multiply(multiplicator);

        // then it should produce the expected value.
        Require
            .equal(
                result,
                Rational.valueOf(4611686022722355200L, 4611686027017322499L),
                "rational result");
    }

    /**
     * Should support multiply by long.
     */
    @Test(priority = 30)
    public static void shouldSupportMultiplyByLong()
    {
        final Rational rational;
        final Rational result;

        // Given a rational value,
        rational = Rational.valueOf(3, 4);

        // when multiplying by a long value,
        result = rational.multiply(2);

        // then it should produce the expected value.
        Require.equal(result, Rational.valueOf(3, 2), "rational result");
    }

    /**
     * Should support multiply by rational.
     */
    @Test(priority = 30)
    public static void shouldSupportMultiplyByRational()
    {
        final Rational rational;
        final Rational result;

        // Given a rational value,
        rational = Rational.valueOf(3, 4);

        // when multiplying by a rational value,
        result = rational.multiply(Rational.valueOf(1, 2));

        // then it should produce the expected value.
        Require.equal(result, Rational.valueOf(3, 8), "rational result");
    }

    /**
     * Should support negate.
     */
    @Test(priority = 30)
    public static void shouldSupportNegate()
    {
        final Rational rational;
        final Rational result;

        // Given a rational value,
        rational = Rational.valueOf(2, -4);

        // when computing its negated value,
        result = rational.negate();

        // then it should produce the expected value.
        Require.equal(result, Rational.valueOf(1, 2), "rational result");
    }

    /**
     * Should support reciprocal.
     */
    @Test(priority = 30)
    public static void shouldSupportReciprocal()
    {
        final Rational rational;
        final Rational result;

        // Given a rational value,
        rational = Rational.valueOf(2, -4);

        // when computing its reciprocal value,
        result = rational.reciprocal();

        // then it should produce the expected value.
        Require.equal(result, Rational.valueOf(-2, 1), "rational result");
    }

    private static Long _long(final long value)
    {
        return Long.valueOf(value);
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
