/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValueConverter.java 4112 2019-08-02 20:00:26Z SFB $
 */

package org.rvpf.base.tool;

import java.io.File;
import java.io.IOException;

import java.text.Normalizer;

import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;

/**
 * Value converter.
 *
 * <p>This is a utility class to help with the conversion of values. It contains
 * only static methods and constant attributes.</p>
 */
@Immutable
public final class ValueConverter
{
    /**
     * No instances.
     */
    private ValueConverter() {}

    /**
     * Canonicalize a path.
     *
     * <p>
     * Makes the path canonical.
     * On Windows, replaces the back slashes with forward slashes.
     * </p>
     *
     * @param path The path.
     *
     * @return The canonicalized path.
     *
     * @throws IOException On failure.
     */
    @Nonnull
    @CheckReturnValue
    public static String canonicalizePath(
            @Nonnull String path)
        throws IOException
    {
        path = new File(path).getCanonicalPath().replace('\\', '/');

        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            path = path.replace('\\', '/');
        }

        return path;
    }

    /**
     * Canonicalizes a text string.
     *
     * <p>Uses Unicode canonical decomposition followed by canonical composition
     * to ensure a consistent text representation.</p>
     *
     * @param text The optional text string.
     *
     * @return The canonicalized text string (empty on missing text)..
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<String> canonicalizeString(
            @Nonnull final Optional<String> text)
    {
        if (text.isPresent()) {
            return Optional
                .of(Normalizer.normalize(text.get(), Normalizer.Form.NFC));
        }

        return Optional.empty();
    }

    /**
     * Converts a named boolean value, providing a default.
     *
     * @param type The type of value (for logging purpose).
     * @param name The name of the value (for logging purpose).
     * @param valueToConvert The value to convert (may be empty).
     * @param defaultValue The default value.
     *
     * @return The converted value or the provided default.
     */
    @CheckReturnValue
    public static boolean convertToBoolean(
            @Nonnull final String type,
            @Nonnull final String name,
            @Nonnull final Optional<String> valueToConvert,
            final boolean defaultValue)
    {
        return convertToBoolean(
            type,
            name,
            valueToConvert,
            Optional.of(Boolean.valueOf(defaultValue)))
            .get()
            .booleanValue();
    }

    /**
     * Converts a named boolean value, providing a default.
     *
     * <p>The following values will be interpreted as true (ignoring case):</p>
     *
     * <ul>
     *   <li><tt>""</tt></li>
     *   <li><tt>"1"</tt></li>
     *   <li><tt>"TRUE"</tt></li>
     *   <li><tt>"YES"</tt></li>
     *   <li><tt>"ON"</tt></li>
     *   <li><tt>"T"</tt></li>
     *   <li><tt>"Y"</tt></li>
     * </ul>
     *
     * <p>The following values will be interpreted as false (ignoring case):</p>
     *
     * <ul>
     *   <li><tt>"0"</tt></li>
     *   <li><tt>"FALSE"</tt></li>
     *   <li><tt>"NO"</tt></li>
     *   <li><tt>"OFF"</tt></li>
     *   <li><tt>"F"</tt></li>
     *   <li><tt>"N"</tt></li>
     * </ul>
     *
     * <p>If the value is null or does not match one of the recognized values,
     * the default will be returned.</p>
     *
     * @param type The type of value (for logging purpose).
     * @param name The name of the value (for logging purpose).
     * @param valueToConvert The value to convert (may be empty).
     * @param defaultValue The default value (may be empty).
     *
     * @return The converted value or the provided default (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<Boolean> convertToBoolean(
            @Nonnull final String type,
            @Nonnull final String name,
            @Nonnull final Optional<String> valueToConvert,
            @Nonnull final Optional<Boolean> defaultValue)
    {
        final Optional<Boolean> booleanValue;

        if (valueToConvert.isPresent()) {
            final String value = valueToConvert.get().trim();

            if (value.isEmpty()) {
                booleanValue = Optional.of(Boolean.TRUE);
            } else if (isTrue(value)) {
                booleanValue = Optional.of(Boolean.TRUE);
            } else if (isFalse(value)) {
                booleanValue = Optional.of(Boolean.FALSE);
            } else {
                _LOGGER
                    .warn(
                        BaseMessages.CONVERT_BOOLEAN,
                        type,
                        name,
                        value,
                        defaultValue.orElse(null));
                booleanValue = defaultValue;
            }
        } else {
            booleanValue = defaultValue;
        }

        return booleanValue;
    }

    /**
     * Converts a named double value, providing a default.
     *
     * <p>If the value is null, empty or unrecognized, the default will be
     * returned.</p>
     *
     * @param type The type of value (for logging purpose).
     * @param name The name of the value (for logging purpose).
     * @param valueToConvert The value to convert (may be empty).
     * @param defaultValue The default value.
     *
     * @return The converted value or the provided default.
     */
    @CheckReturnValue
    public static double convertToDouble(
            @Nonnull final String type,
            @Nonnull final String name,
            @Nonnull final Optional<String> valueToConvert,
            final double defaultValue)
    {
        return convertToDouble(
            type,
            name,
            valueToConvert,
            Optional.of(Double.valueOf(defaultValue)))
            .get()
            .doubleValue();
    }

    /**
     * Converts a named double value, providing a default.
     *
     * <p>If the value is null, empty or unrecognized, the default will be
     * returned.</p>
     *
     * @param type The type of value (for logging purpose).
     * @param name The name of the value (for logging purpose).
     * @param valueToConvert The value to convert (may be empty).
     * @param defaultValue The default value (may be empty).
     *
     * @return The converted value or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<Double> convertToDouble(
            @Nonnull final String type,
            @Nonnull final String name,
            @Nonnull final Optional<String> valueToConvert,
            @Nonnull final Optional<Double> defaultValue)
    {
        Optional<Double> doubleValue;

        if (valueToConvert.isPresent()) {
            final String value = valueToConvert.get().trim();

            if (value.length() > 0) {
                try {
                    doubleValue = Optional.of(Double.valueOf(value));
                } catch (final NumberFormatException exception) {
                    _LOGGER
                        .warn(
                            BaseMessages.CONVERT_DOUBLE,
                            type,
                            name,
                            value,
                            String.valueOf(defaultValue.orElse(null)));
                    doubleValue = defaultValue;
                }
            } else {
                doubleValue = defaultValue;
            }
        } else {
            doubleValue = defaultValue;
        }

        return doubleValue;
    }

    /**
     * Converts a named int value, providing a default.
     *
     * <p>If the value is null, empty or unrecognized, the default will be
     * returned.</p>
     *
     * @param type The type of value (for logging purpose).
     * @param name The name of the value (for logging purpose).
     * @param valueToConvert The value to convert (may be null).
     * @param defaultValue The default value.
     *
     * @return The converted value or the provided default.
     */
    @CheckReturnValue
    public static int convertToInt(
            @Nonnull final String type,
            @Nonnull final String name,
            @Nonnull final Optional<String> valueToConvert,
            final int defaultValue)
    {
        return convertToInteger(
            type,
            name,
            valueToConvert,
            Optional.of(Integer.valueOf(defaultValue)))
            .get()
            .intValue();
    }

    /**
     * Converts a named int value, providing a default.
     *
     * <p>If the value is null, empty or unrecognized, the default will be
     * returned.</p>
     *
     * @param type The type of value (for logging purpose).
     * @param name The name of the value (for logging purpose).
     * @param valueToConvert The value to convert (may be empty).
     * @param defaultValue The default value (may be empty).
     *
     * @return The converted value or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<Integer> convertToInteger(
            @Nonnull final String type,
            @Nonnull final String name,
            @Nonnull final Optional<String> valueToConvert,
            @Nonnull final Optional<Integer> defaultValue)
    {
        Optional<Integer> integerValue;

        if (valueToConvert.isPresent()) {
            final String value = valueToConvert.get().trim();

            if (value.length() > 0) {
                try {
                    integerValue = Optional.of(Integer.decode(value));
                } catch (final NumberFormatException exception) {
                    _LOGGER
                        .warn(
                            BaseMessages.CONVERT_INTEGER,
                            type,
                            name,
                            value,
                            String.valueOf(defaultValue.orElse(null)));
                    integerValue = defaultValue;
                }
            } else {
                integerValue = defaultValue;
            }
        } else {
            integerValue = defaultValue;
        }

        return integerValue;
    }

    /**
     * Converts a named long value, providing a default.
     *
     * <p>If the value is null, empty or unrecognized, the default will be
     * returned.</p>
     *
     * @param type The type of value (for logging purpose).
     * @param name The name of the value (for logging purpose).
     * @param valueToConvert The value to convert (may be empty).
     * @param defaultValue The default value.
     *
     * @return The converted value or the provided default.
     */
    @CheckReturnValue
    public static long convertToLong(
            @Nonnull final String type,
            @Nonnull final String name,
            @Nonnull final Optional<String> valueToConvert,
            final long defaultValue)
    {
        return convertToLong(
            type,
            name,
            valueToConvert,
            Optional.of(Long.valueOf(defaultValue)))
            .get()
            .longValue();
    }

    /**
     * Converts a named long value, providing a default.
     *
     * <p>If the value is empty or unrecognized, the default will be
     * returned.</p>
     *
     * @param type The type of value (for logging purpose).
     * @param name The name of the value (for logging purpose).
     * @param valueToConvert The value to convert (may be empty).
     * @param defaultValue The default value (may be empty).
     *
     * @return The converted value or the provided default.
     */
    @CheckReturnValue
    public static Optional<Long> convertToLong(
            @Nonnull final String type,
            @Nonnull final String name,
            @Nonnull final Optional<String> valueToConvert,
            @Nonnull final Optional<Long> defaultValue)
    {
        Optional<Long> longValue;

        if (valueToConvert.isPresent()) {
            final String value = valueToConvert.get().trim();

            if (value.length() > 0) {
                try {
                    longValue = Optional.of(Long.decode(value));
                } catch (final NumberFormatException exception) {
                    _LOGGER
                        .warn(
                            BaseMessages.CONVERT_LONG,
                            type,
                            name,
                            value,
                            String.valueOf(defaultValue.orElse(null)));
                    longValue = defaultValue;
                }
            } else {
                longValue = defaultValue;
            }
        } else {
            longValue = defaultValue;
        }

        return longValue;
    }

    /**
     * Asks if a text has a canonical form.
     *
     * @param text The text.
     *
     * @return True if the text has a canonical form.
     */
    @CheckReturnValue
    public static boolean isCanonical(@Nonnull final Optional<String> text)
    {
        if (text.isPresent()) {
            return Normalizer.isNormalized(text.get(), Normalizer.Form.NFC);
        }

        return true;
    }

    /**
     * Asks if a value matches the 'false' pattern.
     *
     * @param value The value.
     *
     * @return True if the value matches the 'false' pattern.
     */
    @CheckReturnValue
    public static boolean isFalse(@Nonnull final String value)
    {
        return _FALSE_PATTERN.matcher(value).matches();
    }

    /**
     * Asks if a value matches the 'true' pattern.
     *
     * @param value The value.
     *
     * @return True if the value matches the 'true' pattern.
     */
    @CheckReturnValue
    public static boolean isTrue(@Nonnull final String value)
    {
        return _TRUE_PATTERN.matcher(value).matches();
    }

    /**
     * Rounds bytes to mebibytes.
     *
     * @param bytes A bytes count.
     *
     * @return The count rounded to mebibytes.
     */
    @CheckReturnValue
    public static long roundToMebibytes(long bytes)
    {
        if (bytes < Long.MAX_VALUE) {
            bytes += ONE_MEBIBYTE / 2;
        }

        return bytes / ONE_MEBIBYTE;
    }

    /**
     * Split fields separated by comma or spaces.
     *
     * @param fields The fields.
     *
     * @return The individual fields.
     */
    @Nonnull
    @CheckReturnValue
    public static String[] splitFields(@Nonnull final String fields)
    {
        return _FIELDS_SPLIT_PATTERN.split(fields);
    }

    /**
     * Split fields separated by comma or spaces.
     *
     * @param fields The fields.
     * @param max The maximum number of fields to separate.
     *
     * @return The individual fields.
     */
    @Nonnull
    @CheckReturnValue
    public static String[] splitFields(
            @Nonnull final String fields,
            final int max)
    {
        return _FIELDS_SPLIT_PATTERN.split(fields, max);
    }

    /**
     * Converts a boolean to an Integer object.
     *
     * @param value The boolean value.
     *
     * @return The Integer object.
     */
    @Nonnull
    @CheckReturnValue
    public static Integer toInteger(final boolean value)
    {
        return Integer.valueOf(value? 1: 0);
    }

    /**
     * Converts a String allowed to contain wild characters to a Pattern.
     *
     * @param wild The 'wild' test.
     *
     * @return The equivalent Pattern.
     */
    @Nonnull
    @CheckReturnValue
    public static Pattern wildToPattern(@Nonnull final String wild)
    {
        final StringBuilder stringBuilder = new StringBuilder(wild.length());

        for (int i = 0; i < wild.length(); i++) {
            final char ch = wild.charAt(i);

            if ("\\^$.+([{|".indexOf(ch) >= 0) {
                stringBuilder.append('\\');
                stringBuilder.append(ch);
            } else if (ch == '*') {
                stringBuilder.append(".*?");
            } else if (ch == '?') {
                stringBuilder.append('.');
            } else {
                stringBuilder.append(ch);
            }
        }

        return Pattern
            .compile(stringBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }

    /** One mebibyte (SI). */
    public static final int ONE_MEBIBYTE = 1024 * 1024;

    /**  */

    private static final Pattern _FALSE_PATTERN = Pattern
        .compile("0|FALSE|NO|OFF|F|N", Pattern.CASE_INSENSITIVE);
    private static final Pattern _FIELDS_SPLIT_PATTERN = Pattern
        .compile("(?:\\s*,\\s*)|\\s+");
    private static final Logger _LOGGER = Logger
        .getInstance(ValueConverter.class);
    private static final Pattern _TRUE_PATTERN = Pattern
        .compile("1|TRUE|YES|ON|T|Y", Pattern.CASE_INSENSITIVE);
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
