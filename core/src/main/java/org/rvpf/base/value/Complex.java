/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Complex.java 3983 2019-05-14 11:11:45Z SFB $
 */

package org.rvpf.base.value;

import java.io.Serializable;

import java.util.Locale;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Complex.
 */
@Immutable
public abstract class Complex
    implements Serializable
{
    /**
     * Returns a complex number with cartesian representation.
     *
     * @param real The real part.
     * @param imaginary The imaginary part.
     *
     * @return The complex number.
     */
    @Nonnull
    @CheckReturnValue
    public static Cartesian cartesian(final double real, final double imaginary)
    {
        return new Cartesian(real, imaginary);
    }

    /**
     * Returns a complex number with polar representation.
     *
     * @param magnitude The magnitude (r).
     * @param angle The angle (theta).
     *
     * @return The complex number.
     */
    @Nonnull
    @CheckReturnValue
    public static Polar polar(final double magnitude, final double angle)
    {
        return new Polar(magnitude, angle);
    }

    /**
     * Returns a complex number from the value of a string.
     *
     * @param string The string.
     *
     * @return The complex number.
     */
    @Nonnull
    @CheckReturnValue
    public static Complex valueOf(String string)
    {
        string = string.toLowerCase(Locale.ROOT);

        return string
            .contains(_POLAR_INDICATOR)? _polar(string): _cartesian(string);
    }

    /**
     * Computes the absolute value of this complex number.
     *
     * @return The result.
     */
    @CheckReturnValue
    public abstract double abs();

    /**
     * Computes the inverse cosine of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex acos();

    /**
     * Adds an other complex number.
     *
     * @param complex The other complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex add(@Nonnull Complex complex);

    /**
     * Returns the angle.
     *
     * @return The angle.
     */
    @CheckReturnValue
    public abstract double angle();

    /**
     * Computes the argument of this complex number.
     *
     * @return The result.
     */
    @CheckReturnValue
    public abstract double argument();

    /**
     * Computes the inverse sine of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex asin();

    /**
     * Computes the inverse tangent of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex atan();

    /**
     * Returns the conjugate of this complex number.
     *
     * @return The conjugate.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex conjugate();

    /**
     * Computes the cosine of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex cos();

    /**
     * Computes the hyperbolic cosine of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex cosh();

    /**
     * Divides by an other complex number.
     *
     * @param complex The other complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex divide(@Nonnull Complex complex);

    /** {@inheritDoc}
     */
    @Override
    public abstract boolean equals(Object object);

    /**
     * Computes the exponential function of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex exp();

    /** {@inheritDoc}
     */
    @Override
    public abstract int hashCode();

    /**
     * Returns the imaginary part.
     *
     * @return The imaginary part.
     */
    @CheckReturnValue
    public abstract double imaginary();

    /**
     * Asks if this is a cartesian representation.
     *
     * @return True if it is a cartesian representation.
     */
    @CheckReturnValue
    public abstract boolean isCartesian();

    /**
     * Asks if this is an infinite number.
     *
     * @return True if it is infinite.
     */
    @CheckReturnValue
    public abstract boolean isInfinite();

    /**
     * Asks if this is not a number (NaN).
     *
     * @return True if it is NaN.
     */
    @CheckReturnValue
    public abstract boolean isNaN();

    /**
     * Asks if this is a polar representation.
     *
     * @return True if it is a polar representation.
     */
    @CheckReturnValue
    public abstract boolean isPolar();

    /**
     * Computes the natural logarithm of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex log();

    /**
     * Returns the magnitude.
     *
     * @return The magnitude.
     */
    @CheckReturnValue
    public abstract double magnitude();

    /**
     * Multiplies with an other complex number.
     *
     * @param complex The other complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex multiply(@Nonnull Complex complex);

    /**
     * Negates this number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex negate();

    /**
     * Computes this complex number raised at the power of an other.
     *
     * @param complex The other complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex pow(@Nonnull Complex complex);

    /**
     * Returns the real part.
     *
     * @return The real part.
     */
    @CheckReturnValue
    public abstract double real();

    /**
     * Computes the signum function of this complex number.
     *
     * @return The result.
     */
    @CheckReturnValue
    public abstract int signum();

    /**
     * Computes the sine of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex sin();

    /**
     * Computes the hyperbolic sine of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex sinh();

    /**
     * Computes the square root of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex sqrt();

    /**
     * Subtracts an other complex number.
     *
     * @param complex The other complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex subtract(@Nonnull Complex complex);

    /**
     * Computes the tangent of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex tan();

    /**
     * Computes the hyperbolic tangent of this complex number.
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Complex tanh();

    /**
     * Returns a cartesian representation of this.
     *
     * @return The cartesian representation.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Cartesian toCartesian();

    /**
     * Returns an hexadecimal representation of this number.
     *
     * @return The hexadecimal representation.
     */
    @Nonnull
    @CheckReturnValue
    public abstract String toHexString();

    /**
     * Returns a polar representation of this.
     *
     * @return The polar representation.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Polar toPolar();

    /** {@inheritDoc}
     */
    @Override
    public abstract String toString();

    private static Complex _cartesian(String string)
    {
        string = _WHITE_SPACE_PATTERN.matcher(string).replaceAll("");

        if (string.length() < 1) {
            throw new NumberFormatException();
        }

        final int indexLimit = string.length() - 1;
        int index;

        for (index = 1; index < indexLimit; ++index) {
            final char c = string.charAt(index);

            if ((c == '+') || (c == '-')) {
                final char c1 = string.charAt(index - 1);

                if ((c1 != 'e') && (c1 != 'p')) {
                    break;    // Not exponent sign: must be the imaginary part.
                }
            }
        }

        final double real;
        final double imaginary;

        if (index < indexLimit) {    // We have both values.
            if ("ij".indexOf(string.charAt(indexLimit)) < 0) {
                throw new NumberFormatException();
            }

            real = Double.parseDouble(string.substring(0, index));
            imaginary = Double.parseDouble(string.substring(index, indexLimit));
        } else if ("ij".indexOf(string.charAt(indexLimit)) < 0) {
            real = Double.parseDouble(string);
            imaginary = 0.0;
        } else {    // We have only the imaginary part.
            real = 0.0;
            imaginary = Double.parseDouble(string.substring(0, indexLimit));
        }

        return cartesian(real, imaginary);
    }

    private static Complex _polar(final String string)
    {
        final int cis = string.indexOf(_POLAR_INDICATOR);
        final String magnitude = string.substring(0, cis).trim();
        final String angle = string
            .substring(cis + _POLAR_INDICATOR.length())
            .trim();

        return new Polar(
            (magnitude.length() > 0)? Double.parseDouble(magnitude): 0.0,
            (angle.length() > 0)? Double.parseDouble(angle): 0.0);
    }

    private static final String _POLAR_INDICATOR = "cis";    // For "cos + i sin".
    private static final Pattern _WHITE_SPACE_PATTERN = Pattern.compile("\\s");
    private static final long serialVersionUID = 1L;

    /**
     * Cartesian.
     */
    @Immutable
    public static final class Cartesian
        extends Complex
    {
        /**
         * Constructs an instance.
         *
         * @param real The real part.
         * @param imaginary The imaginary part.
         */
        Cartesian(final double real, final double imaginary)
        {
            _real = real;
            _imaginary = imaginary;
        }

        /** {@inheritDoc}
         */
        @Override
        public double abs()
        {
            return Math.hypot(_real, _imaginary);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex acos()
        {
            return add(I.multiply(_sqrt1z())).log().multiply(I.negate());
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex add(final Complex complex)
        {
            final Cartesian cartesian = complex.toCartesian();

            return new Cartesian(
                _real + cartesian._real,
                _imaginary + cartesian._imaginary);
        }

        /** {@inheritDoc}
         */
        @Override
        public double angle()
        {
            return argument();
        }

        /** {@inheritDoc}
         */
        @Override
        public double argument()
        {
            return Math.atan2(_imaginary, _real);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex asin()
        {
            return I.negate().multiply(_sqrt1z().add(multiply(I)).log());
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex atan()
        {
            return I.divide(TWO).multiply(add(I).divide(negate().add(I)).log());
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex conjugate()
        {
            return new Cartesian(_real, -_imaginary);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex cos()
        {
            return new Cartesian(
                Math.cos(_real) * Math.cosh(_imaginary),
                Math.sin(_real) * Math.sinh(_imaginary));
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex cosh()
        {
            return new Cartesian(
                Math.cosh(_real) * Math.cos(_imaginary),
                Math.sinh(_real) * Math.sin(_imaginary));
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex divide(final Complex complex)
        {
            final Cartesian cartesian = complex.toCartesian();
            final double divisor = (cartesian._real * cartesian._real)
                + (cartesian._imaginary * cartesian._imaginary);
            final double real = ((_real * cartesian._real)
                + (_imaginary * cartesian._imaginary)) / divisor;
            final double imaginary = ((_imaginary * cartesian._real)
                - (_real * cartesian._imaginary)) / divisor;

            return new Cartesian(real, imaginary);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object)
        {
            if (object instanceof Cartesian) {
                final Cartesian other = (Cartesian) object;

                return (Double.doubleToLongBits(
                    _real) == Double.doubleToLongBits(other._real))
                       && (Double.doubleToLongBits(
                           _imaginary) == Double.doubleToLongBits(
                                   other._imaginary));
            }

            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex exp()
        {
            final double expReal = Math.exp(_real);

            return new Cartesian(
                expReal * Math.cos(_imaginary),
                expReal * Math.sin(_imaginary));
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            final long bits = Double
                .doubleToLongBits(_real) ^ Double.doubleToLongBits(_imaginary);

            return (int) (bits ^ (bits >>> 32));
        }

        /** {@inheritDoc}
         */
        @Override
        public double imaginary()
        {
            return _imaginary;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isCartesian()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isInfinite()
        {
            return !isNaN()
                   && (Double.isInfinite(
                       _real) || Double.isInfinite(_imaginary));
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isNaN()
        {
            return Double.isNaN(_real) || Double.isNaN(_imaginary);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPolar()
        {
            return false;
        }

        @Override
        public Complex log()
        {
            return new Cartesian(Math.log(abs()), argument());
        }

        /** {@inheritDoc}
         */
        @Override
        public double magnitude()
        {
            return abs();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex multiply(final Complex complex)
        {
            final Cartesian cartesian = complex.toCartesian();

            return new Cartesian(
                (_real * cartesian._real) - (_imaginary * cartesian._imaginary),
                (_imaginary * cartesian._real)
                + (_real * cartesian._imaginary));
        }

        /** {@inheritDoc}
         */
        @Override
        public Cartesian negate()
        {
            return new Cartesian(-_real, -_imaginary);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex pow(final Complex complex)
        {
            return log().multiply(complex).exp();
        }

        /** {@inheritDoc}
         */
        @Override
        public double real()
        {
            return _real;
        }

        /** {@inheritDoc}
         */
        @Override
        public int signum()
        {
            final int signum = (int) Math.signum(_real);

            return (signum != 0)? signum: (int) Math.signum(_imaginary);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex sin()
        {
            return new Cartesian(
                Math.sin(_real) * Math.cosh(_imaginary),
                Math.cos(_real) * Math.sinh(_imaginary));
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex sinh()
        {
            return new Cartesian(
                Math.sinh(_real) * Math.cos(_imaginary),
                Math.cosh(_real) * Math.sin(_imaginary));
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex sqrt()
        {
            final double t2 = Math.sqrt(Math.abs(_real) + abs());
            final double t = t2 / 2;

            return (_real >= 0.0)? new Cartesian(
                t,
                _imaginary / t2): new Cartesian(
                    Math.abs(_imaginary) / t2,
                    Math.signum(_imaginary) * t);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex subtract(final Complex complex)
        {
            final Cartesian cartesian = complex.toCartesian();

            return new Cartesian(
                _real - cartesian._real,
                _imaginary - cartesian._imaginary);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex tan()
        {
            final double real2 = _real * 2.0;
            final double imaginary2 = _imaginary * 2.0;
            final double divisor = Math.cos(real2) + Math.cosh(imaginary2);

            return new Cartesian(
                Math.sin(real2) / divisor,
                Math.sinh(imaginary2) / divisor);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex tanh()
        {
            final double real2 = _real * 2.0;
            final double imaginary2 = _imaginary * 2.0;
            final double divisor = Math.cosh(real2) + Math.cos(imaginary2);

            return new Cartesian(
                Math.sinh(real2) / divisor,
                Math.sin(imaginary2) / divisor);
        }

        /** {@inheritDoc}
         */
        @Override
        public Cartesian toCartesian()
        {
            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public String toHexString()
        {
            final StringBuilder stringBuilder = new StringBuilder(
                Double.toHexString(_real));

            stringBuilder.append((_imaginary >= 0.0)? " + ": " - ");
            stringBuilder.append(Double.toHexString(Math.abs(_imaginary)));
            stringBuilder.append('j');

            return stringBuilder.toString();
        }

        /** {@inheritDoc}
         */
        @Override
        public Polar toPolar()
        {
            return new Polar(magnitude(), angle());
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            final StringBuilder stringBuilder = new StringBuilder(
                Double.toString(_real));

            stringBuilder.append((_imaginary >= 0.0)? '+': '-');
            stringBuilder.append(Double.toString(Math.abs(_imaginary)));
            stringBuilder.append('j');

            return stringBuilder.toString();
        }

        private Complex _sqrt1z()
        {
            return ONE.subtract(multiply(this)).sqrt();
        }

        public static final Cartesian I = new Cartesian(0.0, 1.0);
        public static final Cartesian ONE = new Cartesian(1.0, 0.0);
        public static final Cartesian TWO = new Cartesian(2.0, 0.0);
        private static final long serialVersionUID = 1L;

        private final double _imaginary;
        private final double _real;
    }


    /**
     * Polar.
     */
    @Immutable
    public static final class Polar
        extends Complex
    {
        /**
         * Constructs an instance.
         *
         * @param magnitude The magnitude.
         * @param angle The angle.
         */
        Polar(double magnitude, double angle)
        {
            if (magnitude == 0.0) {
                angle = 0.0;
            } else {
                if (magnitude < 0.0) {
                    angle += Math.PI;
                    magnitude = -magnitude;
                }

                angle = angle % _PI2;

                if (Math.abs(angle) > Math.PI) {
                    angle -= Math.signum(angle) * _PI2;
                }
            }

            _magnitude = magnitude;
            _angle = angle;
        }

        /** {@inheritDoc}
         */
        @Override
        public double abs()
        {
            return magnitude();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex acos()
        {
            return toCartesian().acos();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex add(final Complex complex)
        {
            return toCartesian().add(complex);
        }

        /** {@inheritDoc}
         */
        @Override
        public double angle()
        {
            return _angle;
        }

        /** {@inheritDoc}
         */
        @Override
        public double argument()
        {
            return angle();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex asin()
        {
            return toCartesian().asin();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex atan()
        {
            return toCartesian().atan();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex conjugate()
        {
            return new Polar(_magnitude, -_angle);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex cos()
        {
            return toCartesian().cos();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex cosh()
        {
            return toCartesian().cosh();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex divide(final Complex complex)
        {
            final Polar polar = complex.toPolar();

            return new Polar(
                _magnitude / polar._magnitude,
                _angle - polar._angle);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object)
        {
            if (object instanceof Polar) {
                final Polar other = (Polar) object;

                return (Double.doubleToLongBits(
                    _magnitude) == Double.doubleToLongBits(other._magnitude))
                       && (Double.doubleToLongBits(
                           _angle) == Double.doubleToLongBits(other._angle));
            }

            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex exp()
        {
            return toCartesian().exp();
        }

        /** {@inheritDoc}
         */
        @Override
        public int hashCode()
        {
            final long bits = Double
                .doubleToLongBits(_magnitude) ^ Double.doubleToLongBits(_angle);

            return (int) (bits ^ (bits >>> 32));
        }

        /** {@inheritDoc}
         */
        @Override
        public double imaginary()
        {
            return _magnitude * Math.sin(_angle);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isCartesian()
        {
            return false;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isInfinite()
        {
            return !isNaN()
                   && (Double.isInfinite(
                       _magnitude) || Double.isInfinite(_angle));
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isNaN()
        {
            return Double.isNaN(_magnitude) || Double.isNaN(_angle);
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPolar()
        {
            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex log()
        {
            return new Cartesian(Math.log(_magnitude), _angle);
        }

        /** {@inheritDoc}
         */
        @Override
        public double magnitude()
        {
            return _magnitude;
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex multiply(final Complex complex)
        {
            final Polar polar = complex.toPolar();

            return new Polar(
                _magnitude * polar._magnitude,
                _angle + polar._angle);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex negate()
        {
            return new Polar(-_magnitude, _angle);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex pow(final Complex complex)
        {
            return toCartesian().pow(complex);
        }

        /** {@inheritDoc}
         */
        @Override
        public double real()
        {
            return _magnitude * Math.cos(_angle);
        }

        /** {@inheritDoc}
         */
        @Override
        public int signum()
        {
            final int signum = -(int) Math.signum(Math.abs(_angle) - _PI_2);

            return (signum != 0)? signum: (int) Math.signum(_angle);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex sin()
        {
            return toCartesian().sin();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex sinh()
        {
            return toCartesian().sinh();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex sqrt()
        {
            return toCartesian().sqrt();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex subtract(final Complex complex)
        {
            return toCartesian().subtract(complex);
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex tan()
        {
            return toCartesian().tan();
        }

        /** {@inheritDoc}
         */
        @Override
        public Complex tanh()
        {
            return toCartesian().tanh();
        }

        /** {@inheritDoc}
         */
        @Override
        public Cartesian toCartesian()
        {
            return new Cartesian(
                _magnitude * Math.cos(_angle),
                _magnitude * Math.sin(_angle));
        }

        /** {@inheritDoc}
         */
        @Override
        public String toHexString()
        {
            final StringBuilder stringBuilder = new StringBuilder(
                Double.toHexString(_magnitude));

            stringBuilder.append(" cis ");
            stringBuilder.append(Double.toHexString(_angle));

            return stringBuilder.toString();
        }

        /** {@inheritDoc}
         */
        @Override
        public Polar toPolar()
        {
            return this;
        }

        /** {@inheritDoc}
         */
        @Override
        public String toString()
        {
            final StringBuilder stringBuilder = new StringBuilder(
                Double.toString(_magnitude));

            stringBuilder.append("cis");
            stringBuilder.append(Double.toString(_angle));

            return stringBuilder.toString();
        }

        private static final double _PI_2 = Math.PI / 2;
        private static final double _PI2 = Math.PI * 2;
        private static final long serialVersionUID = 1L;

        private final double _angle;
        private final double _magnitude;
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
