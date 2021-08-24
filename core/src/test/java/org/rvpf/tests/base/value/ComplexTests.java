/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ComplexTests.java 4068 2019-06-09 14:51:30Z SFB $
 */

package org.rvpf.tests.base.value;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.Complex;
import org.rvpf.tests.Tests;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Complex tests.
 */
public final class ComplexTests
    extends Tests
{
    /**
     * Provides cartesian to polar conversion values.
     *
     * @return Values for the cartesian, then values for the expected polar.
     */
    @DataProvider
    public static Object[][] provideCartesianToPolar()
    {
        return new Object[][] {
            // 0.0 + 0.0j --> 0.0 cis 0.0
            {_double(0.0), _double(0.0), _double(0.0), _double(0.0), },

            // 1.0 + 0.0j --> 1.0 cis 0.0
            {_double(1.0), _double(0.0), _double(1.0), _double(0.0), },

            // -1.0 + 0.0j --> 1.0 cis PI
            {_double(-1.0), _double(0.0), _double(1.0), _double(Math.PI), },

            // 0.0 + 1.0j --> 1.0 cis PI/2
            {_double(0.0), _double(1.0), _double(1.0), _double(Math.PI / 2), },

            // 0.0 + -1.0j --> 1.0 cis -PI/2
            {_double(
                0.0), _double(-1.0), _double(1.0), _double(-Math.PI / 2), },
        };
    }

    /**
     * Provides polar to cartesian values.
     *
     * @return Values for the polar, then values for the expected cartesian.
     */
    @DataProvider
    public static Object[][] providePolarToCartesian()
    {
        return new Object[][] {
            // 1.0 cis 0.0 --> 1.0 + 0.0j
            {_double(
                1.0), _double(
                    0.0), _double(
                        1.0), _double(0.0), _double(0.0), _double(0.0), },

            // 1.0 cis PI --> -1.0 + 0.0j
            {_double(
                1.0), _double(
                    Math.PI), _double(
                        -1.0), _double(0.0), _double(0.0), _double(1.0), },

            // 1.0 cis -PI --> -1.0 + 0.0j
            {_double(
                1.0), _double(
                    -Math.PI), _double(
                        -1.0), _double(0.0), _double(0.0), _double(1.0), },

            // 1.0 cis PI/2 --> 0.0 + 1.0j
            {_double(
                1.0), _double(
                    Math.PI / 2), _double(
                        0.0), _double(1.0), _double(1.0), _double(0.0), },

            // 1.0 cis -PI/2 --> 0.0 + -1.0j
            {_double(
                1.0), _double(
                    -Math.PI / 2), _double(
                        0.0), _double(-1.0), _double(1.0), _double(0.0), },
        };
    }

    /**
     * Provides polar to normalized polar values.
     *
     * @return The created polar values, then the normalized polar values.
     */
    @DataProvider
    public static Object[][] providePolarToNormalized()
    {
        return new Object[][] {
            // 1.0 cis PI*3 --> 1.0 cis PI
            {_double(
                1.0), _double(Math.PI * 3), _double(1.0), _double(Math.PI)},

            // 1.0 cis -PI*3 --> 1.0 cis -PI
            {_double(
                1.0), _double(-Math.PI * 3), _double(1.0), _double(-Math.PI)},

            // 0.0 cis PI*3 --> 0.0 cis 0.0
            {_double(0.0), _double(Math.PI * 3), _double(0.0), _double(0.0)},

            // -1.0 cis 0.0 --> 1.0 cis PI
            {_double(-1.0), _double(0.0), _double(1.0), _double(Math.PI)},
        };
    }

    /**
     * Provides string to cartesian values.
     *
     * @return A string, then the values for the expected cartesian.
     */
    @DataProvider
    public static Object[][] provideStringToCartesian()
    {
        return new Object[][] {
            //
            {"3.0+4.0j", _double(3.0), _double(4.0), },

            //
            {"3.0", _double(3.0), _double(0.0), },

            //
            {"4.0j", _double(0.0), _double(4.0), },

            //
            {"3.0 - 4.0j", _double(3.0), _double(-4.0), },
        };
    }

    /**
     * Provides string to polar values.
     *
     * @return A string, then the values for the expected polar.
     */
    @DataProvider
    public static Object[][] provideStringToPolar()
    {
        return new Object[][] {
            //
            {"3.0 cis 1.0", _double(3.0), _double(1.0), },

            //
            {"3.0 cis", _double(3.0), _double(0.0), },

            //
            {"cis 1.0", _double(0.0), _double(1.0), },

            //
            {"3.0 cis -1.0", _double(3.0), _double(-1.0), },
        };
    }

    /**
     * Should convert cartesian to polar.
     *
     * @param real The real part.
     * @param imaginary The imaginary part.
     * @param magnitude The magnitude expected.
     * @param angle The angle expected.
     */
    @Test(dataProvider = "provideCartesianToPolar")
    public static void shouldConvertCartesianToPolar(
            final double real,
            final double imaginary,
            final double magnitude,
            final double angle)
    {
        final Complex.Cartesian cartesian;
        final Complex.Polar polar;

        // Given a complex in cartesian coordinates,
        cartesian = Complex.cartesian(real, imaginary);

        // when converted to polar coordinates,
        polar = cartesian.toPolar();

        // then it should have the expected values.
        Require.equal(polar, Complex.polar(magnitude, angle), "polar");
    }

    /**
     * Should convert polar to cartesian.
     *
     * @param magnitude The magnitude.
     * @param angle The angle.
     * @param real The real part expected.
     * @param imaginary The imaginary part expected.
     * @param realRef A reference for the real part.
     * @param imaginaryRef A reference for the imaginary part.
     */
    @Test(dataProvider = "providePolarToCartesian")
    public static void shouldConvertPolarToCartesian(
            final double magnitude,
            final double angle,
            final double real,
            final double imaginary,
            final double realRef,
            final double imaginaryRef)
    {
        final Complex.Polar polar;
        final Complex.Cartesian cartesian;

        // Given a complex in polar coordinates,
        polar = Complex.polar(magnitude, angle);

        // when converted to cartesian coordinates,
        cartesian = polar.toCartesian();

        // then it should have the expected values within tolerance.
        Require.equal(cartesian.real(), real, realRef, "cartesian");
        Require
            .equal(cartesian.imaginary(), imaginary, imaginaryRef, "cartesian");
    }

    /**
     * Should convert string to cartesian.
     *
     * @param string A string representation for the cartesian value.
     * @param real The real part expected.
     * @param imaginary The imaginary part expected.
     */
    @Test(dataProvider = "provideStringToCartesian")
    public static void shouldConvertStringToCartesian(
            final String string,
            final double real,
            final double imaginary)
    {
        final Complex complex;

        // Given a valid cartesian string representation of a complex value,
        // when converting this string to a complex value,
        complex = Complex.valueOf(string);

        // then it should have the expected cartesian coordinates.
        Require.equal(complex, Complex.cartesian(real, imaginary), "cartesian");
    }

    /**
     * Should convert string to polar.
     *
     * @param string A string representation for the polar value.
     * @param magnitude The magnitude.
     * @param angle The angle.
     */
    @Test(dataProvider = "provideStringToPolar")
    public static void shouldConvertStringToPolar(
            final String string,
            final double magnitude,
            final double angle)
    {
        final Complex complex;

        // Given a valid polar string representation of a complex value,
        // when converting this string to a complex value,
        complex = Complex.valueOf(string);

        // then it should have the expected polar coordinates.
        Require.equal(complex, Complex.polar(magnitude, angle), "polar");
    }

    /**
     * Should create polar normalized.
     *
     * <p>Tests the normalization performed in the constructor.</p>
     *
     * @param magnitude The magnitude.
     * @param angle The angle.
     * @param expectedMagnitude The expected magnitude.
     * @param expectedAngle The expected angle.
     */
    @Test(dataProvider = "providePolarToNormalized")
    public static void shouldCreatePolarNormalized(
            final double magnitude,
            final double angle,
            final double expectedMagnitude,
            final double expectedAngle)
    {
        final Complex.Polar polar;

        // Given polar coordinates and expected normalized coordinates,
        // when creating a complex value with non normalized coordinates,
        polar = Complex.polar(magnitude, angle);

        // then this complex should have normalized polar coordinates.
        Require
            .equal(
                polar,
                Complex.polar(expectedMagnitude, expectedAngle),
                "polar");
    }

    /**
     * Should support cartesian abs.
     */
    @Test
    public static void shouldSupportCartesianAbs()
    {
        final Complex complex;
        final double result;

        // Given a complex value in cartesian coordinates,
        complex = Complex.cartesian(3.0, 4.0);

        // when computing its absolute value,
        result = complex.abs();

        // then the result should be as expected.
        Require.equal(Double.valueOf(result), Double.valueOf(5.0));
    }

    /**
     * Should support cartesian add.
     */
    @Test
    public static void shouldSupportCartesianAdd()
    {
        final Complex complex1;
        final Complex complex2;
        final Complex result;

        // Given two complex values in cartesian coordinates,
        complex1 = Complex.cartesian(3.0, 4.0);
        complex2 = Complex.cartesian(-3.0, -4.0);

        // when adding the second to the first,
        result = complex1.add(complex2);

        // then the result should be as expected.
        Require.equal(result, Complex.cartesian(0.0, 0.0), "cartesian");
    }

    /**
     * Should support cartesian conjugate.
     */
    @Test
    public static void shouldSupportCartesianConjugate()
    {
        final Complex complex;
        final Complex result;

        // Given a complex value in cartesian coordinates,
        complex = Complex.cartesian(3.0, 4.0);

        // when computing its conjugate value,
        result = complex.conjugate();

        // then the result should be as expected.
        Require.equal(result, Complex.cartesian(3.0, -4.0), "cartesian");
    }

    /**
     * Should support cartesian divide.
     */
    @Test
    public static void shouldSupportCartesianDivide()
    {
        final Complex complex1;
        final Complex complex2;
        final Complex result;

        // Given two complex values in cartesian coordinates,
        complex1 = Complex.cartesian(9.0, -38.0);
        complex2 = Complex.cartesian(-5.0, -6.0);

        // when dividing the first by the second,
        result = complex1.divide(complex2);

        // then the result should be as expected.
        Require.equal(result, Complex.cartesian(3.0, 4.0), "cartesian");
    }

    /**
     * Should support cartesian multiply.
     */
    @Test
    public static void shouldSupportCartesianMultiply()
    {
        final Complex complex1;
        final Complex complex2;
        final Complex result;

        // Given two complex values in cartesian coordinates,
        complex1 = Complex.cartesian(3.0, 4.0);
        complex2 = Complex.cartesian(-5.0, -6.0);

        // when multiplying the first by the second,
        result = complex1.multiply(complex2);

        // then the result should be as expected.
        Require.equal(result, Complex.cartesian(9.0, -38.0), "cartesian");
    }

    /**
     * Should support cartesian negate.
     */
    @Test
    public static void shouldSupportCartesianNegate()
    {
        final Complex complex;
        final Complex result;

        // Given a complex value in cartesian coordinates,
        complex = Complex.cartesian(3.0, 4.0);

        // when computing its negated value,
        result = complex.negate();

        // then the result should be as expected.
        Require.equal(result, Complex.cartesian(-3.0, -4.0), "cartesian");
    }

    /**
     * Should support cartesian subtract.
     */
    @Test
    public static void shouldSupportCartesianSubtract()
    {
        final Complex complex1;
        final Complex complex2;
        final Complex result;

        // Given two complex values in cartesian coordinates,
        complex1 = Complex.cartesian(3.0, 4.0);
        complex2 = Complex.cartesian(3.0, 4.0);

        // when subtracting the first by the second,
        result = complex1.subtract(complex2);

        // then the result should be as expected.
        Require.equal(result, Complex.cartesian(0.0, 0.0), "cartesian");
    }

    /**
     * Should support polar abs.
     */
    @Test
    public static void shouldSupportPolarAbs()
    {
        final Complex complex;
        final double result;

        // Given a complex value in polar coordinates,
        complex = Complex.polar(3.0, 2.0);

        // when computing its absolute value,
        result = complex.abs();

        // then the result should be as expected.
        Require.equal(Double.valueOf(result), Double.valueOf(3.0));
    }

    /**
     * Should support polar conjugate.
     */
    @Test
    public static void shouldSupportPolarConjugate()
    {
        final Complex complex;
        final Complex result;

        // Given a complex value in polar coordinates,
        complex = Complex.polar(3.0, 2.0);

        // when computing its conjugate value,
        result = complex.conjugate();

        // then the result should be as expected.
        Require.equal(result, Complex.polar(3.0, -2.0), "polar");
    }

    /**
     * Should support polar divide.
     */
    @Test
    public static void shouldSupportPolarDivide()
    {
        final Complex complex1;
        final Complex complex2;
        final Complex result;

        // Given two complex values in polar coordinates,
        complex1 = Complex.polar(3.0, 2.0);
        complex2 = Complex.polar(3.0, 2.0);

        // when dividing the first by the second,
        result = complex1.divide(complex2);

        // then the result should be as expected.
        Require.equal(result, Complex.polar(1.0, 0.0), "polar");
    }

    /**
     * Should support polar multiply.
     */
    @Test
    public static void shouldSupportPolarMultiply()
    {
        final Complex complex1;
        final Complex complex2;
        final Complex result;

        // Given two complex values in polar coordinates,
        complex1 = Complex.polar(3.0, 2.0);
        complex2 = Complex.polar(-3.0, 2.0);

        // when multiplying the first by the second,
        result = complex1.multiply(complex2);

        // then the result should be as expected.
        Require.equal(result, Complex.polar(9.0, 0.8584073464102069), "polar");
    }

    /**
     * Should support polar negate.
     */
    @Test
    public static void shouldSupportPolarNegate()
    {
        final Complex complex;
        final Complex result;

        // Given a complex value in polar coordinates,
        complex = Complex.polar(3.0, 2.0);

        // when computing its negated value,
        result = complex.negate();

        // then the result should be as expected.
        Require.equal(result, Complex.polar(3.0, -1.1415926535897931), "polar");
    }

    private static Double _double(final double value)
    {
        return Double.valueOf(value);
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
