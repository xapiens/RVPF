/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: KeyedGroups.java 4102 2019-06-30 15:41:17Z SFB $
 */

package org.rvpf.base.util.container;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;

/**
 * Dictionary of KeyedValues.
 *
 * <p>Extends KeyedValues to allow a hierarchy of KeyedValues.</p>
 */
@NotThreadSafe
public class KeyedGroups
    extends KeyedValues
{
    /**
     * Constructs an instance.
     */
    public KeyedGroups()
    {
        this(BaseMessages.VALUE_TYPE.toString(), Optional.empty());
    }

    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected KeyedGroups(@Nonnull final KeyedGroups other)
    {
        super(other);

        _name = other._name;
        _groups = other._groups.copy();
    }

    /**
     * Constructs an instance.
     *
     * @param type A string identifying the type of value stored.
     * @param name The optional name for the properties as a group.
     */
    protected KeyedGroups(
            @Nonnull final String type,
            @Nonnull final Optional<String> name)
    {
        super(type);

        _name = name.orElse(null);
        _groups = new KeyedValues(
            Message.format(BaseMessages.GROUP_TYPE, getType()));
    }

    /**
     * Adds all entries from an other KeyedGroups.
     *
     * <p>The new values and groups are appended to those already present.</p>
     *
     * @param keyedGroups The other keyed groups.
     */
    public final void addAll(@Nonnull final KeyedGroups keyedGroups)
    {
        super.addAll(keyedGroups);

        for (final Map.Entry<String, List<Object>> entry:
                keyedGroups.getGroupsEntries()) {
            final String key = entry.getKey();

            for (final Object group: entry.getValue()) {
                addGroup(key, (KeyedGroups) group);
            }
        }
    }

    /**
     * Adds a group of keyed groups.
     *
     * <p>If the key is already in the dictionary, the group is appended to the
     * list of groups for that key.</p>
     *
     * <p>If the key is null, the effect is the same as {@link #addAll}.</p>
     *
     * @param key The name of the group.
     * @param properties The keyed groups.
     */
    public void addGroup(
            @Nonnull final String key,
            @Nonnull final KeyedGroups properties)
    {
        _groups.addObject(key, properties);
    }

    /** {@inheritDoc}
     */
    @Override
    public final void clear()
    {
        super.clear();

        _groups.clear();
    }

    /**
     * Asks if the key is associated with a group.
     *
     * @param key The key to look for.
     *
     * @return True if the key is associated with a group.
     */
    @CheckReturnValue
    public boolean containsGroupKey(@Nonnull final String key)
    {
        return _groups.containsValueKey(key);
    }

    /** {@inheritDoc}
     */
    @Override
    public KeyedGroups copy()
    {
        return new KeyedGroups(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (!super.equals(other)) {
            return false;
        }

        return _groups.equals(((KeyedGroups) other)._groups);
    }

    /** {@inheritDoc}
     */
    @Override
    public KeyedGroups freeze()
    {
        if (!isFrozen()) {
            super.freeze();

            for (final Map.Entry<String, List<Object>> groupsEntry:
                    _groups.getValuesEntries()) {
                for (final Object group: groupsEntry.getValue()) {
                    ((KeyedGroups) group).freeze();
                }
            }
        }

        return this;
    }

    /** {@inheritDoc}
     */
    @Override
    public KeyedGroups frozen()
    {
        return (KeyedGroups) super.frozen();
    }

    /**
     * Gets a group of keyed values.
     *
     * @param key The name of the group.
     *
     * @return The Properties (may be the missing group).
     */
    @Nonnull
    @CheckReturnValue
    public KeyedGroups getGroup(@Nonnull final String key)
    {
        final KeyedGroups group = (KeyedGroups) _groups
            .getObject(Require.notNull(key));

        return (group != null)? group: getMissingGroup();
    }

    /**
     * Gets the groups for the specified key.
     *
     * @param key The groups name.
     *
     * @return An array of groups (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    public KeyedGroups[] getGroups(@Nonnull final String key)
    {
        return _groups.getObjects(key).toArray(getNoGroups());
    }

    /**
     * Gets the set of group entries for readonly access.
     *
     * @return The set of group entries.
     */
    @Nonnull
    @CheckReturnValue
    public final Set<Map.Entry<String, List<Object>>> getGroupsEntries()
    {
        return Collections.unmodifiableSet(_groups.getValuesEntries());
    }

    /**
     * Gets the set of keys for the groups.
     *
     * @return The set of keys for the groups.
     */
    @Nonnull
    @CheckReturnValue
    public final Set<String> getGroupsKeys()
    {
        return _groups.getValuesKeys();
    }

    /**
     * Gets the size of the groups map.
     *
     * @return The size of the groups map.
     */
    @CheckReturnValue
    public int getGroupsSize()
    {
        return _groups.getValuesSize();
    }

    /**
     * Gets the name.
     *
     * @return The optional name.
     */
    public final Optional<String> getName()
    {
        return Optional.ofNullable(_name);
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return super.isEmpty() && _groups.isEmpty();
    }

    /**
     * Asks if this is the missing group.
     *
     * @return True if this is the missing group.
     */
    @CheckReturnValue
    public boolean isMissing()
    {
        return false;
    }

    /**
     * Removes a group.
     *
     * @param key The name of the group.
     */
    public final void removeGroup(@Nonnull final String key)
    {
        _groups.removeValue(key);
    }

    /**
     * Sets a group.
     *
     * @param key The name of the group.
     * @param properties The group.
     */
    public final void setGroup(
            @Nonnull final String key,
            @Nonnull final KeyedGroups properties)
    {
        _groups.setValue(key, properties);
    }

    /** {@inheritDoc}
     */
    @Override
    public void setValue(final String key, final Object value)
    {
        super.setValue(key, value);
    }

    /** {@inheritDoc}
     */
    @Override
    public KeyedGroups thawed()
    {
        return isFrozen()? copy(): this;
    }

    /**
     * Returns the first value for each key in a properties map.
     *
     * @return The properties map.
     */
    @Nonnull
    @CheckReturnValue
    public final Map<String, String> toPropertiesMap()
    {
        final Set<String> keys = getValuesKeys();
        final Map<String, String> propertiesMap = new LinkedHashMap<>(
            hashCapacity(keys.size()));

        for (final String key: keys) {
            propertiesMap.put(key, getString(key).orElse(null));
        }

        return propertiesMap;
    }

    /**
     * Gets the missing group.
     *
     * @return A frozen and empty keyed groups.
     */
    @Nonnull
    @CheckReturnValue
    protected KeyedGroups getMissingGroup()
    {
        return MISSING_KEYED_GROUP;
    }

    /**
     * Gets no groups.
     *
     * @return An empty keyed groups array.
     */
    @Nonnull
    @CheckReturnValue
    protected KeyedGroups[] getNoGroups()
    {
        return NO_KEYED_GROUPS;
    }

    /** Missing keyed groups. */
    public static final KeyedGroups MISSING_KEYED_GROUP;

    /** No keyed groups. */
    public static final KeyedGroups[] NO_KEYED_GROUPS = new KeyedGroups[0];

    /**  */

    private static final long serialVersionUID = 1L;

    static {
        MISSING_KEYED_GROUP = new KeyedGroups()
        {
            /** {@inheritDoc}
             */
            @Override
            public boolean isMissing()
            {
                return true;
            }

            private static final long serialVersionUID = 1L;
        };
        MISSING_KEYED_GROUP.freeze();
    }

    private final KeyedValues _groups;
    private final String _name;
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
