/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JNLPProperties.java 4053 2019-06-03 19:22:49Z SFB $
 */

package org.rvpf.jnlp.loader;

import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.util.container.KeyedValues;

/**
 * JNLP properties: wraps JNLP specific system properties.
 *
 * <p>All the system properties with a name beginning with
 * {@value #RVPF_JNLP_PREFIX} are collected with that prefix removed.</p>
 */
@Immutable
public final class JNLPProperties
{
    /**
     * Constructs an instance.
     */
    private JNLPProperties()
    {
        for (final Map.Entry<?, ?> entry: System.getProperties().entrySet()) {
            final String name = (String) entry.getKey();

            if (name.startsWith(RVPF_JNLP_PREFIX)) {
                _properties
                    .setValue(
                        name.substring(RVPF_JNLP_PREFIX.length()),
                        entry.getValue());
            }
        }

        _properties.freeze();
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance.
     */
    @Nonnull
    @CheckReturnValue
    static JNLPProperties getInstance()
    {
        return _INSTANCE;
    }

    /**
     * Gets a boolean value for a key, defaulting to false.
     *
     * @param key The name of the value.
     *
     * @return The requested value or false.
     */
    @CheckReturnValue
    boolean getBooleanValue(@Nonnull final String key)
    {
        return _properties.getBoolean(key, false);
    }

    /**
     * Gets an elapsed time value for a key, providing a value for default and
     * empty.
     *
     * @param key The key for the elapsed time.
     * @param defaultValue The optional default for the key.
     * @param emptyValue The optional assumed value for empty.
     *
     * @return The requested elapsed time, empty, or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    Optional<ElapsedTime> getElapsedValue(
            @Nonnull final String key,
            @Nonnull final Optional<ElapsedTime> defaultValue,
            @Nonnull final Optional<ElapsedTime> emptyValue)
    {
        return _properties.getElapsed(key, defaultValue, emptyValue);
    }

    /**
     * Gets an int value for a key, providing a default.
     *
     * <p>If the key is not found or its value is unrecognized the default will
     * be returned.</p>
     *
     * @param key The name of the value.
     * @param defaultValue The default value for the key.
     *
     * @return The requested value or the provided default.
     */
    @CheckReturnValue
    int getIntValue(@Nonnull final String key, final int defaultValue)
    {
        return _properties.getInt(key, defaultValue);
    }

    /**
     * Gets the first value for a key, providing a default.
     *
     * @param key The value name.
     * @param defaultValue The optional value if the key is not present.
     *
     * @return The first value (or the provided default if none).
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getStringValue(
            @Nonnull final String key,
            @Nonnull final Optional<String> defaultValue)
    {
        return _properties.getString(key, defaultValue);
    }

    /**
     * Substitutes markers in a given text.
     *
     * <p>A substitution marker would be a '${x} property reference.</p>
     *
     * @param text The text possibly containing substitution markers.
     * @param deferred True if substitution of '$${x}' should be deferred.
     *
     * @return The text with markers substituted.
     */
    @Nonnull
    @CheckReturnValue
    String substitute(@Nonnull final String text, final boolean deferred)
    {
        return _properties.substitute(text, deferred);
    }

    /** RVPF-JNLP system properties prefix. */
    public static final String RVPF_JNLP_PREFIX = "rvpf.jnlp.";

    /**  */

    private static final JNLPProperties _INSTANCE = new JNLPProperties();

    private final KeyedValues _properties = new KeyedValues(
        BaseMessages.SYSTEM_TYPE.toString());
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
