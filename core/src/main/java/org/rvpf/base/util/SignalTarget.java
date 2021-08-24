/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SignalTarget.java 3983 2019-05-14 11:11:45Z SFB $
 */

package org.rvpf.base.util;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.UUID;

/**
 * Signal target.
 */
@Immutable
public final class SignalTarget
{
    /**
     * Constructs an instance.
     *
     * @param name The target name.
     * @param uuid The target UUID.
     * @param reference A reference.
     */
    public SignalTarget(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<String> reference)
    {
        _name = name;
        _uuid = uuid;
        _reference = reference;
    }

    /**
     * Returns a signal target decoded from a string.
     *
     * @param string The optional input string.
     *
     * @return The signal target or empty.
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<SignalTarget> fromString(
            @Nonnull final Optional<String> string)
    {
        final SignalTarget signalTarget;

        if (string.isPresent()) {
            final Matcher matcher = _SIGNAL_TARGET_PATTERN
                .matcher(string.get().trim());

            if (matcher.matches()) {
                final String uuidGroup = matcher.group(_UUID_GROUP);

                signalTarget = new SignalTarget(
                    Optional.ofNullable(matcher.group(_NAME_GROUP)),
                    (uuidGroup != null)? UUID
                        .fromString(uuidGroup): Optional.empty(),
                    Optional.ofNullable(matcher.group(_REFERENCE_GROUP)));
            } else {
                signalTarget = null;
            }
        } else {
            signalTarget = null;
        }

        return Optional.ofNullable(signalTarget);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (other == this) {
            return true;
        }

        if (other instanceof SignalTarget) {
            final SignalTarget otherSignalTarget = (SignalTarget) other;

            return Objects.equals(_name, otherSignalTarget._name)
                   && Objects.equals(_uuid, otherSignalTarget._uuid)
                   && Objects.equals(_reference, otherSignalTarget._reference);
        }

        return false;
    }

    /**
     * Gets the target name.
     *
     * @return The optional target name.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getName()
    {
        return _name;
    }

    /**
     * Gets the reference.
     *
     * @return The optional reference.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getReference()
    {
        return _reference;
    }

    /**
     * Gets the target UUID.
     *
     * @return The optional target UUID.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<UUID> getUUID()
    {
        return _uuid;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append('(');

        if (_name.isPresent()) {
            stringBuilder.append('[');
            stringBuilder.append(_name.get());
            stringBuilder.append(']');
        }

        stringBuilder.append(",");

        if (_uuid.isPresent()) {
            stringBuilder.append(_uuid.get());
        }

        stringBuilder.append(",");

        if (_reference.isPresent()) {
            stringBuilder.append("'");
            stringBuilder.append(_reference.get());
            stringBuilder.append("'");
        }

        stringBuilder.append(')');

        return stringBuilder.toString();
    }

    /** Name field key. */
    public static final String NAME_FIELD = "name";

    /** Reference field key. */
    public static final String REFERENCE_FIELD = "reference";

    /** UUID field key. */
    public static final String UUID_FIELD = "uuid";

    /**  */

    private static final int _NAME_GROUP = 1;
    private static final int _REFERENCE_GROUP = 3;
    private static final int _UUID_GROUP = 2;
    private static final Pattern _SIGNAL_TARGET_PATTERN = Pattern
        .compile(
            "\\((?:\\[(.*?)\\])?+,\\s*+(?:("
            + "[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}"
            + "))?,\\s*+(?:(.*))?\\)",
            Pattern.CASE_INSENSITIVE);

    private final Optional<String> _name;
    private final Optional<String> _reference;
    private final Optional<UUID> _uuid;
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
