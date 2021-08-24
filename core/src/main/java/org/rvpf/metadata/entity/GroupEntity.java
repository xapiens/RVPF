/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: GroupEntity.java 4076 2019-06-11 17:09:30Z SFB $
 */

package org.rvpf.metadata.entity;

import java.lang.ref.Reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.Entity;
import org.rvpf.base.UUID;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.util.container.WeakDeputy;
import org.rvpf.config.TopologicalErrorException;
import org.rvpf.metadata.Text;

/**
 * Group entity.
 *
 * <p>Groups entities. Allows groups hierachy.</p>
 */
public final class GroupEntity
    extends MetadataEntity
{
    /**
     * Constructs an instance.
     *
     * @param name The optional instance name.
     * @param uuid The optional instance UUID.
     * @param attributes The optional attributes.
     * @param texts The optional texts.
     */
    GroupEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Optional<KeyedGroups> attributes,
            @Nonnull final Optional<Map<String, Text>> texts)
    {
        super(name, uuid, attributes, texts);
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Adds a member to this group.
     *
     * @param member The new member.
     *
     * @return True on success.
     *
     * @throws TopologicalErrorException When a cyclic reference is detected.
     */
    @CheckReturnValue
    public boolean addMember(
            @Nonnull final Entity member)
        throws TopologicalErrorException
    {
        final boolean added;
        Set<Reference<Entity>> members;

        members = _membersByClass.get(member.getClass());

        if (members == null) {
            members = new HashSet<Reference<Entity>>();
            _membersByClass.put(member.getClass(), members);
        }

        added = members.add(new WeakDeputy<Entity>(member));

        if (added) {
            if (member.getClass() == getClass()) {
                ((GroupEntity) member)._adjustLevel(_level - 1);
            }

            _members.add(new WeakDeputy<Entity>(member));
        }

        return added;
    }

    /**
     * Cleans up unreferenced entities.
     *
     * @return True if some members were removed.
     */
    @CheckReturnValue
    public boolean cleanUp()
    {
        boolean cleaned = false;

        for (final Iterator<Reference<Entity>> iterator = _members.iterator();
                iterator.hasNext(); ) {
            if (iterator.next().get() == null) {
                iterator.remove();
                cleaned = true;
            }
        }

        return cleaned;
    }

    /** {@inheritDoc}
     */
    @Override
    public int compareTo(final Entity other)
    {
        final GroupEntity otherGroup = (GroupEntity) other;
        int comparison;

        comparison = _level - otherGroup._level;

        if (comparison == 0) {
            comparison = getName().get().compareTo(otherGroup.getName().get());
        }

        return comparison;
    }

    /**
     * Asks if this group contains a specified member.
     *
     * @param member The member.
     * @param recursive True verifies descendant groups members.
     *
     * @return True if it is a member of this group.
     */
    @CheckReturnValue
    public boolean contains(
            @Nonnull final Entity member,
            final boolean recursive)
    {
        if (_membersByClass == null) {
            return false;
        }

        if (getMembers(member.getClass()).contains(member)) {
            return true;
        }

        if (recursive) {
            for (final Entity memberGroup: getMembers(GroupEntity.class)) {
                if (((GroupEntity) memberGroup).contains(member, true)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public GroupEntity copy()
    {
        return newBuilder().copyFrom(this).build();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (!super.equals(other)) {
            return false;
        }

        final GroupEntity otherGroup = (GroupEntity) other;
        final List<Entity> members = getMembers();
        final List<Entity> otherMembers = otherGroup.getMembers();

        if (!members.equals(otherMembers)) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Gets the members.
     *
     * @return The members.
     */
    @Nonnull
    @CheckReturnValue
    public List<Entity> getMembers()
    {
        final List<Entity> members = new ArrayList<>(_members.size());

        for (final Reference<Entity> reference: _members) {
            members.add(reference.get());
        }

        return members;
    }

    /**
     * Gets the immediate members for a given cass.
     *
     * <p>The returned set, if not empty, is the original.</p>
     *
     * @param membersClass The class of the requested members.
     * @param <T> The type of the returned values.
     *
     * @return The members for the given class.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    @CheckReturnValue
    public <T extends Entity> Set<T> getMembers(
            @Nonnull final Class<T> membersClass)
    {
        final Set<Reference<Entity>> memberReferences = (_membersByClass
            != null)? _membersByClass
                .get(membersClass): null;

        if (memberReferences == null) {
            return Collections.emptySet();
        }

        final Set<Entity> members = new HashSet<>();

        for (final Reference<Entity> memberReference: memberReferences) {
            members.add(memberReference.get());
        }

        return (Set<T>) members;
    }

    /**
     * Gets the members for a given class.
     *
     * <p>The returned set, even when recursive is true, is not the
     * original.</p>
     *
     * @param membersClass The class of the requested members.
     * @param recursive True adds descendant groups members.
     * @param <T> The type of the returned values.
     *
     * @return The members for the given class.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    @CheckReturnValue
    public <T extends Entity> Set<T> getMembers(
            @Nonnull final Class<T> membersClass,
            final boolean recursive)
    {
        final Set<Entity> members = new HashSet<Entity>(
            getMembers(Require.notNull(membersClass)));

        if (recursive) {
            for (final Entity member: getMembers(GroupEntity.class)) {
                members
                    .addAll(
                        ((GroupEntity) member).getMembers(membersClass, true));
            }
        }

        return (Set<T>) members;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getPrefix()
    {
        return ENTITY_PREFIX;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getReferenceName()
    {
        return ENTITY_REFERENCE_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    private void _adjustLevel(final int level)
        throws TopologicalErrorException
    {
        if (_busy) {
            throw new TopologicalErrorException(this);
        }

        if (level > _level) {
            _level = level;
            _busy = true;

            for (final Entity member: getMembers(getClass())) {
                ((GroupEntity) member)._adjustLevel(_level - 1);
            }

            _busy = false;
        }
    }

    /** Group element name. */
    public static final String ELEMENT_NAME = "Group";

    /** Group entity prefix. */
    public static final String ENTITY_PREFIX = "U";

    /** Group entity reference name. */
    public static final String ENTITY_REFERENCE_NAME = "group";

    private boolean _busy;
    private int _level;
    private final List<Reference<Entity>> _members = new LinkedList<>();
    private final Map<Class<? extends Entity>, Set<Reference<Entity>>> _membersByClass =
        new HashMap<>();

    /**
     * Builder.
     */
    public static class Builder
        extends MetadataEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /** {@inheritDoc}
         */
        @Override
        public GroupEntity build()
        {
            return new GroupEntity(
                getName(),
                getUUID(),
                getAttributes(),
                getTexts());
        }

        /**
         * Copies the values from an other GroupEntity.
         *
         * @param group The other GroupEntity.
         *
         * @return This.
         */
        @Nonnull
        public final Builder copyFrom(@Nonnull final GroupEntity group)
        {
            super.copyFrom(group);

            return this;
        }
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
