/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Require.java 4109 2019-07-22 23:31:37Z SFB $
 */

package org.rvpf.base.tool;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Require.
 *
 * <p>
 * Provides assertion services. Replaces the use of the 'assert' instruction
 * which depends on a compile time switch (-ea).
 *
 * Also replaces the use of the AssertJ library which is expected to be run in
 * a test environment.
 *
 * Also replaces the generic use of IllegalStateException with FailureException
 * which subclasses AssertionError instead of RuntimeException.
 * </p>
 *
 * <p>This class does not use reflection.</p>
 */
@Immutable
public final class Require
{
    /**
     * No instances (static methods only).
     */
    private Require() {}

    /**
     * The 'content' methods ensure that all the values returned by the
     * iterable match the supplied contents in value, order and number.
     */

    /**
     * Requires a specific content for an iterable.
     *
     * @param iterable The iterable.
     * @param content The expected content.
     * @param <T> The type of the content objects.
     */
    @SafeVarargs
    public static <T> void content(
            @Nonnull final Iterable<T> iterable,
            @Nonnull final T... content)
    {
        if (!_content(iterable, content)) {
            throw new FailureException();
        }
    }

    /**
     * Requires a specific content for an iterable.
     *
     * @param iterable The iterable.
     * @param text Explanatory text.
     * @param content The expected content.
     * @param <T> The type of the content objects.
     */
    @SafeVarargs
    public static <T> void content(
            @Nonnull final Iterable<T> iterable,
            @Nonnull final String text,
            @Nonnull final T... content)
    {
        if (!_content(iterable, content)) {
            throw new FailureException(text);
        }
    }

    /**
     * Requires object equal.
     *
     * @param reference The reference object.
     * @param value The value object.
     */
    public static void equal(
            @Nullable final Object reference,
            @Nullable final Object value)
    {
        if (!Objects.equals(value, reference)) {
            throw new FailureException();
        }
    }

    /**
     * Requires object equal.
     *
     * @param reference The reference object.
     * @param value The value object.
     * @param explanation Explanatory text message.
     */
    public static void equal(
            @Nullable final Object reference,
            @Nullable final Object value,
            @Nonnull final Object explanation)
    {
        if (!Objects.equals(value, reference)) {
            throw new FailureException(explanation);
        }
    }

    /**
     * Requires double equal.
     *
     * @param reference The double reference.
     * @param value The double value.
     * @param sample A sample value.
     * @param explanation Explanatory text.
     */
    public static void equal(
            final double reference,
            final double value,
            final double sample,
            @Nonnull final Object explanation)
    {
        if (!_equal(reference, value, sample)) {
            throw new FailureException(explanation);
        }
    }

    /**
     * Forces a failure.
     *
     * <p>Often used in the 'default' clause of a 'switch' instruction.</p>
     *
     * @return A value to satisfy compiler verifications.
     *
     * @throws FailureException Without explanatory text.
     */
    public static FailureException failure()
        throws FailureException
    {
        throw new FailureException();
    }

    /**
     * Failure.
     *
     * @param value The value which must be false.
     *
     * @throws FailureException Without explanatory text.
     */
    public static void failure(final boolean value)
    {
        if (value) {
            throw new FailureException();
        }
    }

    /**
     * Failure.
     *
     * @param explanation Explanatory text.
     *
     * @return A value to satisfy compiler verifications.
     *
     * @throws FailureException With explanatory text.
     */
    public static FailureException failure(
            @Nonnull final Object explanation)
        throws FailureException
    {
        throw new FailureException(explanation);
    }

    /**
     * Failure.
     *
     * @param value The value which must be false.
     * @param explanation Explanatory text.
     *
     * @throws FailureException When the value is true.
     */
    public static void failure(
            final boolean value,
            @Nonnull final Object explanation)
    {
        if (value) {
            throw new FailureException(explanation);
        }
    }

    /**
     * Failure.
     *
     * @param cause The failure cause.
     * @param explanation Explanatory text.
     *
     * @return A value to satisfy compiler verifications.
     *
     * @throws FailureException Without explanatory text.
     */
    public static FailureException failure(
            @Nonnull final Exception cause,
            @Nonnull final Object explanation)
        throws FailureException
    {
        throw new FailureException(explanation, cause);
    }

    /** The 'ignored' methods are used to avoid FindBugs warnings. */

    /**
     * Ignores a boolean value.
     *
     * @param value The value to ignore.
     */
    public static void ignored(final boolean value) {}

    /**
     * Ignores a double value.
     *
     * @param value The value to ignore.
     */
    public static void ignored(final double value) {}

    /**
     * Ignores a long value.
     *
     * @param value The value to ignore.
     */
    public static void ignored(final long value) {}

    /**
     * Ignores an object value.
     *
     * @param value The value to ignore.
     */
    public static void ignored(final Object value) {}

    /**
     * Requires a not empty string.
     *
     * @param string The tested string.
     *
     * @return The tested string (may be ignored).
     *
     * @throws FailureException For null or empty tested string.
     */
    @Nonnull
    public static String notEmpty(
            @Nullable final String string)
        throws FailureException
    {
        if ((string == null) || (string.length() == 0)) {
            throw new FailureException();
        }

        return string;
    }

    /**
     * Requires a not empty trimmed string.
     *
     * @param string The tested string.
     *
     * @return The tested string trimmed.
     *
     * @throws FailureException For null or empty tested string.
     */
    @Nonnull
    @CheckReturnValue
    public static String notEmptyTrimmed(
            @Nullable String string)
        throws FailureException
    {
        if (string == null) {
            throw new FailureException();
        }

        string = string.trim();

        if (string.length() == 0) {
            throw new FailureException();
        }

        return string;
    }

    /**
     * The 'notNull' methods return the verified object with its type when not
     * null which allows verification and utilisation in a single statement.
     */

    /**
     * Requires an object.
     *
     * @param object The tested object.
     * @param <T> The type of the tested object.
     *
     * @return The tested object when not null.
     *
     * @throws FailureException For null tested object.
     */
    @Nonnull
    public static <T> T notNull(
            @Nullable final T object)
        throws FailureException
    {
        if (object == null) {
            throw new FailureException();
        }

        return object;
    }

    /**
     * Requires an object.
     *
     * @param object The tested object.
     * @param explanation Explanatory text.
     * @param <T> The type of the tested object.
     *
     * @return The tested object when not null.
     *
     * @throws FailureException For null tested object.
     */
    @Nonnull
    public static <T> T notNull(
            @Nullable final T object,
            @Nonnull final Object explanation)
        throws FailureException
    {
        if (object == null) {
            throw new FailureException(explanation);
        }

        return object;
    }

    /**
     * Requires an optional object to not be present.
     *
     * @param object The tested optional object.
     * @param <T> The type of the tested object.
     *
     * @throws FailureException For present tested object.
     */
    @Nonnull
    public static <T> void notPresent(
            @Nonnull final Optional<T> object)
        throws FailureException
    {
        if (object.isPresent()) {
            throw new FailureException();
        }
    }

    /**
     * Requires an optional object to not be present.
     *
     * @param object The tested optional object.
     * @param explanation Explanatory text.
     * @param <T> The type of the tested object.
     *
     * @throws FailureException For present tested object.
     */
    @Nonnull
    public static <T> void notPresent(
            @Nonnull final Optional<T> object,
            @Nonnull final Object explanation)
        throws FailureException
    {
        if (object.isPresent()) {
            throw new FailureException(explanation);
        }
    }

    /**
     * Requires an optional object to be present.
     *
     * @param object The tested optional object.
     * @param <T> The type of the tested object.
     *
     * @throws FailureException For non present tested object.
     */
    @Nonnull
    public static <T> void present(
            @Nonnull final Optional<T> object)
        throws FailureException
    {
        if (!object.isPresent()) {
            throw new FailureException();
        }
    }

    /**
     * Requires an optional object to be present.
     *
     * @param object The tested optional object.
     * @param explanation Explanatory text.
     * @param <T> The type of the tested object.
     *
     * @throws FailureException For non present tested object.
     */
    @Nonnull
    public static <T> void present(
            @Nonnull final Optional<T> object,
            @Nonnull final Object explanation)
        throws FailureException
    {
        if (!object.isPresent()) {
            throw new FailureException(explanation);
        }
    }

    /**
     * Requires the two arguments to be the same object.
     *
     * @param reference The reference object.
     * @param value The value object.
     */
    public static void same(
            @Nullable final Object reference,
            @Nullable final Object value)
    {
        if (reference != value) {
            throw new FailureException();
        }
    }

    /**
     * Requires the two arguments to be the same object.
     *
     * @param reference The reference object.
     * @param value The value object.
     * @param explanation Explanatory text.
     */
    public static void same(
            @Nullable final Object reference,
            @Nullable final Object value,
            @Nonnull final Object explanation)
    {
        if (value != reference) {
            throw new FailureException(explanation);
        }
    }

    /**
     * Requires success.
     *
     * @param value The value which must be true.
     */
    public static void success(final boolean value)
    {
        if (!value) {
            throw new FailureException();
        }
    }

    /**
     * Requires success.
     *
     * @param value The value which must be true.
     * @param explanation Explanatory text.
     *
     * @throws FailureException When the value is false.
     */
    public static void success(
            final boolean value,
            @Nonnull final Object explanation)
    {
        if (!value) {
            throw new FailureException(explanation);
        }
    }

    /**
     * Requires success.
     *
     * @param exception A possible exception.
     * @param explanation Explanatory text.
     *
     * @throws FailureException When the exception is not null.
     */
    public static void success(
            @Nullable final Exception exception,
            @Nonnull final Object explanation)
    {
        if (exception != null) {
            throw new FailureException(explanation, exception);
        }
    }

    @SafeVarargs
    private static <T> boolean _content(
            final Iterable<T> iterable,
            final T... content)
    {
        final int contentLength = content.length;
        int index = 0;

        for (final T value: iterable) {
            if ((index >= contentLength)
                    || !Objects.equals(value, content[index++])) {
                return false;
            }
        }

        return index == contentLength;
    }

    private static boolean _equal(
            final double reference,
            final double value,
            final double sample)
    {
        return Math.abs(reference - value) <= Math.ulp(sample);
    }

    /**
     * Failure exception.
     *
     * <p>As a subclass of AssertionError, this allows detection of failed
     * assertions detected by this class.</p>
     */
    public static class FailureException
        extends AssertionError
    {
        /**
         * Constructs an instance.
         */
        public FailureException()
        {
            super();
        }

        /**
         * Constructs an instance.
         *
         * @param cause Some explanatory text or the cause of the failure.
         */
        public FailureException(@Nonnull final Object cause)
        {
            super(cause);
        }

        /**
         * Constructs an instance.
         *
         * @param text Explanatory text.
         * @param cause The cause of the failure.
         */
        public FailureException(
                @Nonnull final Object text,
                @Nonnull final Exception cause)
        {
            super(text.toString(), cause);
        }

        private static final long serialVersionUID = 1L;
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
