/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BigRational.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.base.value;

import java.math.BigInteger;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/** Big rational.
 */
@Immutable
public final class BigRational
    extends Number
    implements Comparable<BigRational>
{
    private BigRational(
            final BigInteger numerator,
            final BigInteger denominator)
    {
        _numerator = numerator;
        _denominator = denominator;
    }

    /** Returns a big rational number from the value of a rational number.
     *
     * @param rational A rational number.
     *
     * @return The big rational number.
     */
    @Nonnull
    @CheckReturnValue
    public static BigRational valueOf(@Nonnull final Rational rational)
    {
        return BigRational.valueOf(
                BigInteger.valueOf(rational.getNumerator()),
                BigInteger.valueOf(rational.getDenominator()));
    }

    /** Returns a big rational number from the value of a string.
     *
     * @param string The string.
     *
     * @return The big rational number.
     */
    @Nonnull
    @CheckReturnValue
    public static BigRational valueOf(@Nonnull final String string)
    {
        final int slashIndex = string.indexOf('/');
        final BigInteger numerator;
        final BigInteger denominator;

        if (slashIndex < 0) {
            numerator = new BigInteger(string);
            denominator = BigInteger.ONE;
        } else {
            numerator = new BigInteger(string.substring(0, slashIndex));
            denominator = new BigInteger(string.substring(slashIndex + 1));
        }

        return BigRational.valueOf(numerator, denominator);
    }

    /** Returns a big rational number from big integers.
     *
     * @param numerator The numerator.
     * @param denominator The denominator.
     *
     * @return The big rational number.
     */
    @Nonnull
    @CheckReturnValue
    public static BigRational valueOf(
            @Nonnull BigInteger numerator,
            @Nonnull BigInteger denominator)
    {
        if (denominator.signum() == 0) {
            throw new ArithmeticException();
        }
        if (denominator.signum() < 0) {
            numerator = numerator.negate();
            denominator = denominator.negate();
        }

        final BigInteger gcd = numerator.gcd(denominator);

        if (gcd.compareTo(BigInteger.ONE) > 0) {
            numerator = numerator.divide(gcd);
            denominator = denominator.divide(gcd);
        }

        return new BigRational(numerator, denominator);
    }

    /** Returns a big rational number from long values.
     *
     * @param numerator The numerator.
     * @param denominator The denominator.
     *
     * @return The big rational number.
     */
    @Nonnull
    @CheckReturnValue
    public static BigRational valueOf(
            final long numerator,
            final long denominator)
    {
        return BigRational.valueOf(
                BigInteger.valueOf(numerator),
                BigInteger.valueOf(denominator));
    }

    /** Returns the absolute value of this number.
     *
     * @return The absolute value.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational abs()
    {
        return (_numerator.signum() < 0)? negate(): this;
    }

    /** Adds an other big rational.
     *
     * @param other The other big rational.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational add(@Nonnull final BigRational other)
    {
        return BigRational.valueOf(
                _numerator.multiply(other._denominator)
                    .add(other._numerator.multiply(_denominator)),
                _denominator.multiply(other._denominator));
    }

    /** Adds an integer.
     *
     * @param integer The (long) integer.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational add(final long integer)
    {
        return add(BigRational.valueOf(integer, 1));
    }

    /** {@inheritDoc}
     */
    @Override
    public int compareTo(final BigRational other)
    {
        return _numerator.multiply(other._denominator)
            .compareTo(_denominator.multiply(other._numerator));
    }

    /** Divides by an other big rational.
     *
     * @param other The other rational.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational divide(@Nonnull final BigRational other)
    {
        return multiply(other.reciprocal());
    }

    /** Divides by an integer.
     *
     * @param integer The (long) integer.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational divide(final long integer)
    {
        return BigRational.valueOf(
                _numerator,
                _denominator.multiply(BigInteger.valueOf(integer)));
    }

    /** {@inheritDoc}
     */
    @Override
    public double doubleValue()
    {
        return _numerator.doubleValue() / _denominator.doubleValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object)
    {
        if (object instanceof BigRational) {
            final BigRational other = (BigRational) object;

            return (_numerator.equals(other._numerator))
                && (_denominator.equals(other._denominator));
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public float floatValue()
    {
        return _numerator.floatValue() / _denominator.floatValue();
    }

    /** Gets the denominator.
     *
     * @return The denominator.
     */
    @Nonnull
    @CheckReturnValue
    public BigInteger getDenominator()
    {
        return _denominator;
    }

    /** Gets the numerator.
     *
     * @return The numerator.
     */
    @Nonnull
    @CheckReturnValue
    public BigInteger getNumerator()
    {
        return _numerator;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return _numerator.hashCode() ^ _denominator.hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public int intValue()
    {
        return _numerator.divide(_denominator)
            .intValue();
    }

    /** {@inheritDoc}
     */
    @Override
    public long longValue()
    {
        return _numerator.divide(_denominator)
            .longValue();
    }

    /** Multiplies by an other big rational.
     *
     * @param other The other rational.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational multiply(@Nonnull final BigRational other)
    {
        return BigRational.valueOf(
                _numerator.multiply(other._numerator),
                _denominator.multiply(other._denominator));
    }

    /** Multiplies by an integer.
     *
     * @param integer The (long) integer.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational multiply(final long integer)
    {
        return BigRational.valueOf(
                _numerator.multiply(BigInteger.valueOf(integer)),
                _denominator);
    }

    /** Negates this number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational negate()
    {
        return BigRational.valueOf(_numerator.negate(), _denominator);
    }

    /** Returns the reciprocal value of this number.
     *
     * @return The reciprocal value.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational reciprocal()
    {
        return BigRational.valueOf(_denominator, _numerator);
    }

    /** Returns the signum function of this number.
     *
     * @return The signum function.
     */
    @CheckReturnValue
    public int signum()
    {
        return _numerator.signum();
    }

    /** Subtracts an other big rational.
     *
     * @param other The other big rational.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational subtract(@Nonnull final BigRational other)
    {
        return add(other.negate());
    }

    /** Subtracts an integer.
     *
     * @param integer The (long) integer.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public BigRational subtract(final long integer)
    {
        return add(BigRational.valueOf(-integer, 1));
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return _numerator.toString() + '/' + _denominator.toString();
    }

    private static final long serialVersionUID = 1L;

    private final BigInteger _denominator;
    private final BigInteger _numerator;
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
