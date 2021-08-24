/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: KeyedValues.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.base.util.container;

import java.io.Serializable;

import java.lang.reflect.Array;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;

/**
 * Keyed values.
 *
 * <p>Keyed values may hold any number of entries. Each entry is a key
 * associated with one value or more. The order of entries within the dictionary
 * and of values within entry are in the order of their initial insertion.</p>
 */
@NotThreadSafe
public class KeyedValues
    implements Serializable
{
    /**
     * Constructs an instance.
     */
    public KeyedValues()
    {
        this(BaseMessages.VALUE_TYPE.toString());
    }

    /**
     * Constructs an instance.
     *
     * @param type A string identifying the type of value stored.
     */
    public KeyedValues(@Nonnull final String type)
    {
        _type = Require.notNull(type);
        _values = new ListLinkedHashMap<>(4);
    }

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected KeyedValues(@Nonnull final KeyedValues other)
    {
        _type = other._type;
        _values = (ListLinkedHashMap<String, Object>) other._values.clone();
        _hidden = other._hidden;

        if (other._hiddenValues != null) {
            _hiddenValues = new HashSet<>(other._hiddenValues);
        }
    }

    /**
     * Computes the hash capacity needed to hold a specified key count.
     *
     * @param count The expected key count.
     *
     * @return The hash capacity.
     */
    @CheckReturnValue
    public static int hashCapacity(final int count)
    {
        return 1 + (int) (count / HASH_LOAD_FACTOR);
    }

    /**
     * Adds a keyed value.
     *
     * <p>If the key is already in the dictionary, the value is appended to the
     * list of values for that key.</p>
     *
     * @param key The value key.
     * @param value The value.
     */
    public void add(@Nonnull final String key, @Nonnull final Object value)
    {
        addObject(key, value);
    }

    /**
     * Adds all entries from an other keyed values.
     *
     * <p>The new values are appended to those already present.</p>
     *
     * @param keyedValues The other keyed values.
     */
    public final void addAll(@Nonnull final KeyedValues keyedValues)
    {
        for (final String propertyKey: keyedValues.getValuesKeys()) {
            for (final Object value:
                    keyedValues.getValues(propertyKey, Object.class)) {
                add(propertyKey, value);
            }
        }
    }

    /**
     * Asks if the values for a key are hidden.
     *
     * @param key The key.
     *
     * @return True if the values for the key are hidden.
     */
    @CheckReturnValue
    public boolean areValuesHidden(@Nonnull final String key)
    {
        return (_hiddenValues != null)? _hiddenValues.contains(key): false;
    }

    /**
     * Clears the dictionary.
     */
    public void clear()
    {
        checkNotFrozen();

        _values.clear();
        _hiddenValues = null;
    }

    /**
     * Asks if the key is associated with a value.
     *
     * @param key The key to look for.
     *
     * @return True if the key is associated with a value.
     */
    @CheckReturnValue
    public boolean containsValueKey(@Nonnull final String key)
    {
        return _values.containsKey(Require.notNull(key));
    }

    /**
     * Creates a copy of this.
     *
     * @return The copy.
     */
    @Nonnull
    @CheckReturnValue
    public KeyedValues copy()
    {
        return new KeyedValues(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }

        final KeyedValues otherKeyedValues = (KeyedValues) other;

        if (_hidden != otherKeyedValues._hidden) {
            return false;
        }

        return _values.equals(otherKeyedValues._values);
    }

    /**
     * Freezes this.
     *
     * @return This.
     */
    @Nonnull
    public KeyedValues freeze()
    {
        _frozen = true;

        return this;
    }

    /**
     * Returns a frozen copy of this.
     *
     * @return This if already frozen, or a frozen copy.
     */
    @Nonnull
    @CheckReturnValue
    public KeyedValues frozen()
    {
        final KeyedValues frozen;

        if (isFrozen()) {
            frozen = this;
        } else {
            frozen = copy();
            frozen.freeze();
        }

        return frozen;
    }

    /**
     * Gets a boolean value for a key, defaulting to false.
     *
     * <p>If the key is not found or its value is unrecognized, false will be
     * returned.</p>
     *
     * @param key The name of the value.
     *
     * @return The requested value or false.
     *
     * @see #getBoolean(String key, boolean defaultValue)
     */
    @CheckReturnValue
    public final boolean getBoolean(@Nonnull final String key)
    {
        return getBoolean(key, false);
    }

    /**
     * Gets a boolean value for a key, providing a default.
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
    public final boolean getBoolean(
            @Nonnull final String key,
            final boolean defaultValue)
    {
        return getBoolean(key, Optional.of(Boolean.valueOf(defaultValue)))
            .get()
            .booleanValue();
    }

    /**
     * Gets a boolean value for a key, providing a default.
     *
     * <p>If the key is not found or its value is unrecognized the default will
     * be returned.</p>
     *
     * @param key The name of the value.
     * @param defaultValue The default value for the key (may be empty).
     *
     * @return The requested value or the provided default (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Boolean> getBoolean(
            @Nonnull final String key,
            @Nonnull final Optional<Boolean> defaultValue)
    {
        final Object value = getValue(key, Boolean.class);

        if (value != null) {
            if (value instanceof String) {
                final Optional<Boolean> booleanValue = ValueConverter
                    .convertToBoolean(
                        getType(),
                        key,
                        Optional.of((String) value),
                        defaultValue);

                return booleanValue;
            }

            return Optional.of((Boolean) value);
        }

        return defaultValue;
    }

    /**
     * Gets a class definition for a key, providing a default.
     *
     * <p>A string value will be converted to a class definition.</p>
     *
     * @param key The key for the class definition.
     * @param defaultClassDef The default for the key.
     *
     * @return The requested class definition or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    public final ClassDef getClassDef(
            @Nonnull final String key,
            @Nonnull final ClassDef defaultClassDef)
    {
        final ClassDef[] classDefs = getClassDefs(key);

        if (classDefs.length > 1) {
            getThisLogger().warn(BaseMessages.MULTIPLE_VALUES, getType(), key);
        }

        return (classDefs.length > 0)? classDefs[0]: Require
            .notNull(defaultClassDef);
    }

    /**
     * Gets a class definition for a key, providing a default.
     *
     * <p>A string value will be converted to a class definition.</p>
     *
     * @param key The key for the class definition.
     * @param defaultClassDef The default for the key.
     *
     * @return The requested class definition or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ClassDef> getClassDef(
            @Nonnull final String key,
            @Nonnull final Optional<ClassDef> defaultClassDef)
    {
        final ClassDef[] classDefs = getClassDefs(key);

        if (classDefs.length > 1) {
            getThisLogger().warn(BaseMessages.MULTIPLE_VALUES, getType(), key);
        }

        return (classDefs.length > 0)? Optional
            .of(classDefs[0]): defaultClassDef;
    }

    /**
     * Gets the class definition array for the specified key.
     *
     * <p>String values will be converted to class definitionss.</p>
     *
     * @param key The key for the requested class definitions array.
     *
     * @return The class definition array (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public final ClassDef[] getClassDefs(@Nonnull final String key)
    {
        final Object[] values = getValues(key, Object.class);
        final ClassDef[] classDefs = new ClassDef[values.length];

        for (int i = 0; i < values.length; ++i) {
            if (values[i] instanceof ClassDef) {
                classDefs[i] = (ClassDef) values[i];
            } else if (values[i] instanceof String) {
                classDefs[i] = new ClassDefImpl((String) values[i]);
            } else {
                getThisLogger()
                    .warn(
                        BaseMessages.UNEXPECTED_VALUE_CLASS,
                        getType(),
                        key,
                        ClassDef.class.getName(),
                        values[i].getClass().getName());

                return _NO_CLASS_DEFS;
            }
        }

        return classDefs;
    }

    /**
     * Gets a double value for a key, providing a default.
     *
     * <p>If the key is not found or its value is unrecognized the default will
     * be returned.</p>
     *
     * @param key The key for the value.
     * @param defaultValue The default value.
     *
     * @return The requested value or the provided default.
     */
    public final double getDouble(
            @Nonnull final String key,
            final double defaultValue)
    {
        final Object objectValue = getValue(key, Number.class);
        final double doubleValue;

        if (objectValue != null) {
            if (objectValue instanceof Number) {
                doubleValue = ((Number) objectValue).doubleValue();
            } else {
                doubleValue = ValueConverter
                    .convertToDouble(
                        getType(),
                        key,
                        Optional.of((String) objectValue),
                        defaultValue);
            }
        } else {
            doubleValue = defaultValue;
        }

        return doubleValue;
    }

    /**
     * Gets an elapsed time value for a key, providing a value for default and
     * empty.
     *
     * @param key The key for the elapsed time.
     * @param defaultValue The default.
     * @param emptyValue The assumed value for empty.
     *
     * @return The requested elapsed time, empty, or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<ElapsedTime> getElapsed(
            @Nonnull final String key,
            @Nonnull final Optional<ElapsedTime> defaultValue,
            @Nonnull final Optional<ElapsedTime> emptyValue)
    {
        final Optional<String> elapsedString = getString(key);

        if (!elapsedString.isPresent()) {
            return defaultValue;
        }

        Optional<ElapsedTime> elapsedTime;

        try {
            elapsedTime = ElapsedTime
                .fromString(Optional.of(elapsedString.get()));

            if (!elapsedTime.isPresent()) {
                elapsedTime = emptyValue;
            }
        } catch (final IllegalArgumentException exception) {
            getThisLogger()
                .warn(BaseMessages.ILLEGAL_ARGUMENT, exception.getMessage());
            elapsedTime = defaultValue;
        }

        return elapsedTime;
    }

    /**
     * Gets an int value for a key, providing a default.
     *
     * <p>If the key is not found or its value is unrecognized, the default will
     * be returned.</p>
     *
     * @param key The name of the value.
     * @param defaultValue The default value.
     *
     * @return The requested value or the provided default.
     */
    @CheckReturnValue
    public final int getInt(@Nonnull final String key, final int defaultValue)
    {
        return getInteger(key, Optional.of(Integer.valueOf(defaultValue)))
            .get()
            .intValue();
    }

    /**
     * Gets an integer value for a key, providing a default.
     *
     * @param key The name of the value.
     * @param defaultValue The default value (may be empty).
     *
     * @return The requested value or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Integer> getInteger(
            @Nonnull final String key,
            @Nonnull final Optional<Integer> defaultValue)
    {
        final Object value = getValue(key, Number.class);
        final Optional<Integer> integerValue;

        if (value instanceof Integer) {
            integerValue = Optional.of((Integer) value);
        } else if (value instanceof String) {
            integerValue = ValueConverter
                .convertToInteger(
                    getType(),
                    key,
                    Optional.of((String) value),
                    defaultValue);
        } else if (value != null) {
            integerValue = Optional
                .of(Integer.valueOf(((Number) value).intValue()));
        } else {
            integerValue = defaultValue;
        }

        return integerValue;
    }

    /**
     * Gets a long value for a key, providing a default.
     *
     * <p>If the key is not found or its value is unrecognized, the default will
     * be returned.</p>
     *
     * @param key The name of the value.
     * @param defaultValue The default value.
     *
     * @return The requested value or the provided default.
     */
    @CheckReturnValue
    public final long getLong(
            @Nonnull final String key,
            final long defaultValue)
    {
        return getLong(key, Optional.of(Long.valueOf(defaultValue)))
            .get()
            .longValue();
    }

    /**
     * Gets a long value for a key, providing a default.
     *
     * @param key The name of the value.
     * @param defaultValue The default value (may be empty).
     *
     * @return The requested value or the provided default.
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<Long> getLong(
            @Nonnull final String key,
            @Nonnull final Optional<Long> defaultValue)
    {
        final Object value = getValue(key, Number.class);
        final Optional<Long> longValue;

        if (value instanceof Long) {
            longValue = Optional.of((Long) value);
        } else if (value instanceof String) {
            longValue = ValueConverter
                .convertToLong(
                    getType(),
                    key,
                    Optional.of((String) value),
                    defaultValue);
        } else if (value != null) {
            longValue = Optional.of(Long.valueOf(((Number) value).longValue()));
        } else {
            longValue = defaultValue;
        }

        return longValue;
    }

    /**
     * Gets the object for a key.
     *
     * @param key The object key.
     *
     * @return The first object or null.
     */
    @Nullable
    @CheckReturnValue
    public final Object getObject(@Nonnull final String key)
    {
        final List<Object> objects = getObjects(key);
        final Object object;

        if (!objects.isEmpty()) {
            object = objects.get(0);

            if (objects.size() > 1) {
                getThisLogger()
                    .warn(BaseMessages.MULTIPLE_VALUES, getType(), key);
            }
        } else {
            object = null;
        }

        return object;
    }

    /**
     * Gets the object list for a key.
     *
     * @param key The object key.
     *
     * @return The object list (null if none).
     */
    @Nonnull
    @CheckReturnValue
    public List<Object> getObjects(@Nonnull final String key)
    {
        return _values.getAll(Require.notNull(key));
    }

    /**
     * Gets the password value for a key.
     *
     * @param key The value name.
     *
     * @return The password value (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<char[]> getPassword(@Nonnull final String key)
    {
        final String password = (String) getValue(key, String.class);

        return (password != null)? Optional
            .of(password.toCharArray()): Optional.empty();
    }

    /**
     * Gets a string value for a key.
     *
     * @param key The value name.
     *
     * @return The string value (empty if none).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getString(@Nonnull final String key)
    {
        return Optional.ofNullable((String) getValue(key, String.class));
    }

    /**
     * Gets the first value for a key, providing a default.
     *
     * @param key The value name.
     * @param defaultValue The value to return if the key is not present.
     *
     * @return The first value (or the provided default if none).
     */
    @Nonnull
    @CheckReturnValue
    public final Optional<String> getString(
            @Nonnull final String key,
            @Nonnull final Optional<String> defaultValue)
    {
        final Optional<String> value = getString(key);

        return value.isPresent()? value: defaultValue;
    }

    /**
     * Gets the values for the specified key.
     *
     * @param key The values name.
     *
     * @return The String array of values (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public final String[] getStrings(@Nonnull final String key)
    {
        return getValues(key, String.class);
    }

    /**
     * Gets the type of value stored in the dictionary.
     *
     * @return The type.
     */
    @Nonnull
    @CheckReturnValue
    public String getType()
    {
        return _type;
    }

    /**
     * Gets the values for the specified key.
     *
     * @param key The key for the requested values.
     *
     * @return The array of values (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public Object[] getValues(@Nonnull final String key)
    {
        return getObjects(key).toArray();
    }

    /**
     * Gets the Set of entries.
     *
     * @return The Set of entries.
     */
    @Nonnull
    @CheckReturnValue
    public final Set<Map.Entry<String, List<Object>>> getValuesEntries()
    {
        return _values.entrySet();
    }

    /**
     * Gets the set of keys for the values.
     *
     * @return The set of keys for the values.
     */
    @Nonnull
    @CheckReturnValue
    public final Set<String> getValuesKeys()
    {
        return _values.keySet();
    }

    /**
     * Gets the size of the values map.
     *
     * @return The size of the values map.
     */
    @CheckReturnValue
    public int getValuesSize()
    {
        return _values.size();
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Asks if this is empty.
     *
     * @return True if this is empty.
     */
    @CheckReturnValue
    public boolean isEmpty()
    {
        return _values.isEmpty();
    }

    /**
     * Asks if this is frozen.
     *
     * @return True if this is frozen.
     */
    @CheckReturnValue
    public final boolean isFrozen()
    {
        return _frozen;
    }

    /**
     * Gets the hidden indicator.
     *
     * @return The hidden indicator.
     */
    @CheckReturnValue
    public final boolean isHidden()
    {
        return _hidden;
    }

    /**
     * Asks if a key has multiple values.
     *
     * @param key The key.
     *
     * @return True if the key has multiple values.
     */
    @CheckReturnValue
    public boolean isMultiple(@Nonnull final String key)
    {
        final List<Object> objects = getObjects(key);

        return (objects != null) && (objects.size() > 1);
    }

    /**
     * Removes a keyed value.
     *
     * @param key The value key.
     */
    public void removeValue(@Nonnull final String key)
    {
        checkNotFrozen();

        _values.remove(_validatedKey(key));
    }

    /**
     * Sets the hidden indicator.
     *
     * @param hidden The hidden indicator.
     */
    public final void setHidden(final boolean hidden)
    {
        _hidden = hidden;
    }

    /**
     * Sets a keyed value.
     *
     * <p>If the key is already in the dictionary, its value is replaced.</p>
     *
     * @param key The value key.
     * @param value The value.
     */
    public void setValue(@Nonnull String key, @Nonnull final Object value)
    {
        checkNotFrozen();

        key = _validatedKey(key);
        _values.remove(key);
        _values.add(key, Require.notNull(value));
    }

    /**
     * Sets the values hidden for the specified key.
     *
     * @param key The key.
     */
    public void setValuesHidden(@Nonnull final String key)
    {
        checkNotFrozen();

        if (_hiddenValues == null) {
            _hiddenValues = new HashSet<>();
        }

        _hiddenValues.add(key);
    }

    /**
     * Substitutes markers in a given text.
     *
     * <p>A substitution marker would be a '${x}' property reference.</p>
     *
     * <p>Note: when the property ('x') is not found, en empty string is
     * used.</p>
     *
     * @param text The text possibly containing substitution markers.
     * @param deferred True if substitution of '$${x}' should be deferred.
     *
     * @return The text with markers substituted.
     */
    @Nonnull
    @CheckReturnValue
    public final String substitute(@Nonnull String text, final boolean deferred)
    {
        final Matcher matcher = _VARIABLE_PATTERN.matcher(text);
        StringBuffer buffer = null;

        while (matcher.find()) {
            if (!deferred || matcher.group(1).isEmpty()) {
                final String name = matcher.group(2);
                final String value = getString(name, Optional.of("")).get();

                if (buffer == null) {
                    buffer = new StringBuffer();
                }

                matcher
                    .appendReplacement(buffer, Matcher.quoteReplacement(value));
            }
        }

        if (buffer != null) {
            matcher.appendTail(buffer);
            text = buffer.toString();
        }

        return text;
    }

    /**
     * Returns a thawed instance of this.
     *
     * @return A clone if frozen, or this.
     */
    @Nonnull
    @CheckReturnValue
    public KeyedValues thawed()
    {
        return isFrozen()? copy(): this;
    }

    /** {@inheritDoc}
     */
    @Override
    @Nonnull
    @CheckReturnValue
    public String toString()
    {
        return getClass()
            .getSimpleName() + "@" + Integer.toHexString(super.hashCode());
    }

    /**
     * Adds a keyed object.
     *
     * <p>If the key is already in the dictionary, the value is appended to the
     * list of objects for that key.</p>
     *
     * @param key The object key.
     * @param object The object.
     */
    protected final void addObject(
            @Nonnull final String key,
            @Nonnull final Object object)
    {
        checkNotFrozen();

        _values.add(_validatedKey(key), Require.notNull(object));
    }

    /**
     * Checks that this is not frozen.
     */
    protected final void checkNotFrozen()
    {
        Require.failure(_frozen, BaseMessages.FROZEN);
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return Logger.getInstance(getClass());
    }

    /**
     * Gets a value for a key.
     *
     * @param key The key for the requested value.
     * @param valueClass The expected value Class.
     *
     * @return The value.
     */
    @Nullable
    @CheckReturnValue
    protected final Object getValue(
            @Nonnull final String key,
            final Class<?> valueClass)
    {
        final Object object = getObject(key);

        if ((object != null) && !(object instanceof String)) {
            if (!valueClass.isInstance(object)) {
                getThisLogger()
                    .warn(
                        BaseMessages.UNEXPECTED_VALUE_CLASS,
                        getType(),
                        key,
                        valueClass.getName(),
                        object.getClass().getName());

                return null;
            }
        }

        return object;
    }

    /**
     * Gets the values for the specified key.
     *
     * @param key The key for the requested values.
     * @param valueClass The expected Class of the array elements.
     * @param <T> The type of the returned values.
     *
     * @return The array of values (may be empty).
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    @CheckReturnValue
    protected final <T> T[] getValues(
            @Nonnull final String key,
            @Nonnull final Class<T> valueClass)
    {
        final List<Object> objects = getObjects(key);
        T[] values = null;

        if (objects != null) {
            try {
                values = objects
                    .toArray(
                        (T[]) Array.newInstance(valueClass, objects.size()));
            } catch (final ArrayStoreException exception) {
                for (final Object value: objects) {
                    if (!valueClass.isInstance(value)) {
                        getThisLogger()
                            .warn(
                                BaseMessages.UNEXPECTED_VALUE_CLASS,
                                getType(),
                                key,
                                valueClass.getName(),
                                value.getClass().getName());

                        break;
                    }
                }
            }
        }

        if (values == null) {
            values = (T[]) Array.newInstance(valueClass, 0);
        }

        return values;
    }

    /**
     * Sets the object list for a key.
     *
     * @param key The object key.
     * @param objects The object list.
     */
    protected void setObjects(
            @Nonnull final String key,
            @Nonnull final List<Object> objects)
    {
        checkNotFrozen();

        if (objects.isEmpty()) {
            _values.remove(key);
        } else {
            _values.put(key, objects);
        }
    }

    private static String _validatedKey(final String key)
    {
        if (key == ANONYMOUS) {
            return "";
        }

        if (key.isEmpty()) {
            throw new IllegalArgumentException(
                Message.format(BaseMessages.EMPTY_KEY));
        }

        return key;
    }

    /** Anonymous entry. */
    public static final String ANONYMOUS = "\0";

    /** Hash load factor. */
    public static final float HASH_LOAD_FACTOR = 0.75f;

    /** Map entry comparator. */
    protected static final Comparator<Map.Entry<String, List<Object>>> MAP_ENTRY_COMPARATOR =
        Comparator
            .comparing(Map.Entry::getKey);
    private static final ClassDef[] _NO_CLASS_DEFS = new ClassDef[0];
    private static final Pattern _VARIABLE_PATTERN = Pattern
        .compile("\\$(\\$?+)\\{(.+?)\\}");
    private static final long serialVersionUID = 1L;

    private transient boolean _frozen;
    private transient boolean _hidden;
    private transient Set<String> _hiddenValues;
    private final String _type;
    private final ListLinkedHashMap<String, Object> _values;
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
