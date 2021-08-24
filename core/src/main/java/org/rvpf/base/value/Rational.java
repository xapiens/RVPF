/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Rational.java 3983 2019-05-14 11:11:45Z SFB $
 */

package org.rvpf.base.value;

import java.math.BigInteger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Rational.
 */
@Immutable
public final class Rational
    extends Number
    implements Comparable<Rational>
{
    /**
     * Constructs an instance.
     *
     * @param numerator The numerator.
     * @param denominator The denominator.
     */
    private Rational(final long numerator, final long denominator)
    {
        _numerator = numerator;
        _denominator = denominator;
    }

    /**
     * Returns a rational number from the value of a string.
     *
     * @param string The string.
     *
     * @return The rational number.
     */
    @Nonnull
    @CheckReturnValue
    public static Rational valueOf(@Nonnull final String string)
    {
        final int slashIndex = string.indexOf('/');
        final long numerator;
        final long denominator;

        if (slashIndex < 0) {
            numerator = Long.parseLong(string);
            denominator = 1;
        } else {
            numerator = Long.parseLong(string.substring(0, slashIndex).trim());
            denominator = Long
                .parseLong(string.substring(slashIndex + 1).trim());
        }

        return Rational.valueOf(numerator, denominator);
    }

    /**
     * Returns a rational number having the values specified.
     *
     * @param numerator The numerator.
     * @param denominator The denominator.
     *
     * @return The rational number.
     */
    @Nonnull
    @CheckReturnValue
    public static Rational valueOf(long numerator, long denominator)
    {
        if (denominator == 0) {
            throw new ArithmeticException();
        }

        if (numerator == Long.MIN_VALUE) {
            throw new ArithmeticException();
        }

        if (denominator < 0) {
            if (denominator == Long.MIN_VALUE) {
                throw new ArithmeticException();
            }

            numerator = -numerator;
            denominator = -denominator;
        }

        final long gcd = _gcd(numerator, denominator);

        if (gcd > 1) {
            numerator /= gcd;
            denominator /= gcd;
        }

        return new Rational(numerator, denominator);
    }

    /**
     * Returns the absolute value of this number.
     *
     * @return The absolute value.
     */
    @Nonnull
    @CheckReturnValue
    public Rational abs()
    {
        return (_numerator < 0)? negate(): this;
    }

    /**
     * Adds an integer.
     *
     * @param integer The (long) integer.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Rational add(final long integer)
    {
        return add(Rational.valueOf(integer, 1));
    }

    /**
     * Adds an other rational.
     *
     * @param other The other rational.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Rational add(@Nonnull final Rational other)
    {
        final Rational sum;

        if (_denominator == other._denominator) {
            sum = Rational.valueOf(_numerator + other._numerator, _denominator);
        } else if (((_numerator + other._numerator) < Integer.MAX_VALUE)
                   && ((_denominator + other._denominator)
                   < Integer.MAX_VALUE)) {
            sum = Rational
                .valueOf(
                    (_numerator * other._denominator)
                    + (other._numerator * _denominator),
                    _denominator * other._denominator);
        } else {
            sum = Rational
                .valueOf(
                    BigInteger
                        .valueOf(_numerator)
                        .multiply(BigInteger.valueOf(other._denominator))
                        .add(
                                BigInteger
                                        .valueOf(other._numerator)
                                        .multiply(
                                                BigInteger
                                                        .valueOf(_denominator)))
                        .longValue(),
                    BigInteger
                        .valueOf(_denominator)
                        .multiply(BigInteger.valueOf(other._denominator))
                        .longValue());
        }

        return sum;
    }

    /** {@inheritDoc}
     */
    @Override
    public int compareTo(final Rational other)
    {
        return Long
            .compare(
                _numerator * other._denominator,
                _denominator * other._numerator);
    }

    /**
     * Divides by an integer.
     *
     * @param integer The (long) integer.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Rational divide(final long integer)
    {
        return Rational.valueOf(_numerator, _denominator * integer);
    }

    /**
     * Divides by an other rational.
     *
     * @param other The other rational.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Rational divide(@Nonnull final Rational other)
    {
        return multiply(other.reciprocal());
    }

    /** {@inheritDoc}
     */
    @Override
    public double doubleValue()
    {
        return (double) _numerator / (double) _denominator;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (object == this) {
            return true;
        }

        if (object instanceof Rational) {
            final Rational other = (Rational) object;

            return (_numerator == other._numerator)
                   && (_denominator == other._denominator);
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public float floatValue()
    {
        return (float) _numerator / (float) _denominator;
    }

    /**
     * Gets the denominator.
     *
     * @return The denominator.
     */
    @CheckReturnValue
    public long getDenominator()
    {
        return _denominator;
    }

    /**
     * Gets the numerator.
     *
     * @return The numerator.
     */
    @CheckReturnValue
    public long getNumerator()
    {
        return _numerator;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final long bits = _numerator ^ _denominator;

        return (int) (bits ^ (bits >>> 32));
    }

    /** {@inheritDoc}
     */
    @Override
    public int intValue()
    {
        return (int) (_numerator / _denominator);
    }

    /** {@inheritDoc}
     */
    @Override
    public long longValue()
    {
        return _numerator / _denominator;
    }

    /**
     * Multiplies by an integer.
     *
     * @param integer The (long) integer.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Rational multiply(final long integer)
    {
        return Rational.valueOf(_numerator * integer, _denominator);
    }

    /**
     * Multiplies by an other rational.
     *
     * @param other The other rational.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Rational multiply(@Nonnull final Rational other)
    {
        final Rational product;

        if (((_numerator + other._numerator) < Integer.MAX_VALUE)
                && ((_denominator + other._denominator) < Integer.MAX_VALUE)) {
            product = Rational
                .valueOf(
                    _numerator * other._numerator,
                    _denominator * other._denominator);
        } else {
            final BigRational bigProduct = BigRational
                .valueOf(this)
                .multiply(BigRational.valueOf(other));

            product = Rational
                .valueOf(
                    bigProduct.getNumerator().longValue(),
                    bigProduct.getDenominator().longValue());
        }

        return product;
    }

    /**
     * Negates this number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Rational negate()
    {
        if (_numerator == Long.MIN_VALUE) {
            throw new ArithmeticException();
        }

        return Rational.valueOf(-_numerator, _denominator);
    }

    /**
     * Returns the reciprocal value of this number.
     *
     * @return The reciprocal value.
     */
    @Nonnull
    @CheckReturnValue
    public Rational reciprocal()
    {
        return Rational.valueOf(_denominator, _numerator);
    }

    /**
     * Returns the signum function of this number.
     *
     * @return The signum function.
     */
    @CheckReturnValue
    public int signum()
    {
        return Long.signum(_numerator);
    }

    /**
     * Subtracts an integer.
     *
     * @param integer The (long) integer.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Rational subtract(final long integer)
    {
        return add(Rational.valueOf(-integer, 1));
    }

    /**
     * Subtracts an other rational.
     *
     * @param other The other rational.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Rational subtract(@Nonnull final Rational other)
    {
        return add(other.negate());
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return Long.toString(_numerator) + '/' + Long.toString(_denominator);
    }

    private static long _gcd(long m, long n)
    {
        long r;

        if (m < 0) {
            m = -m;
        }

        for (;;) {
            r = m % n;

            if (r == 0) {
                break;
            }

            m = n;
            n = r;
        }

        return n;
    }

    private static final long serialVersionUID = 1L;

    private final long _denominator;
    private final long _numerator;
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
