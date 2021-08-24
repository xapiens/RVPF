/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PermissionsEntity.java 4075 2019-06-11 13:13:39Z SFB $
 */

package org.rvpf.metadata.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.base.security.Identity;
import org.rvpf.config.entity.AbstractEntity;
import org.rvpf.metadata.Permissions;

/**
 * Permissions Entity.
 */
public final class PermissionsEntity
    extends AbstractEntity
    implements Permissions
{
    /**
     * Constructs an instance.
     *
     * @param name The optional instance name.
     * @param uuid The optional instance UUID.
     * @param permissions The permissions.
     */
    PermissionsEntity(
            @Nonnull final Optional<String> name,
            @Nonnull final Optional<UUID> uuid,
            @Nonnull final Map<String, Set<Action>> permissions)
    {
        super(name, uuid);

        _permissions = permissions;
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
     * Adopts a child.
     *
     * @param child The child.
     */
    public void adopt(@Nonnull final PermissionsEntity child)
    {
        for (final Map.Entry<String, Set<Action>> entry:
                _permissions.entrySet()) {
            final String role = entry.getKey();

            for (final Action action: entry.getValue()) {
                child.allow(role, action);
            }
        }
    }

    /**
     * Allows an action for a role.
     *
     * @param role The role.
     * @param action The action.
     */
    public void allow(@Nonnull final String role, @Nonnull final Action action)
    {
        Set<Action> permissions = _permissions.get(role);

        if (permissions == null) {
            permissions = EnumSet.noneOf(Action.class);
            _permissions.put(role, permissions);
        }

        permissions.add(action);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean check(final Action action, final Optional<Identity> identity)
    {
        if (!identity.isPresent() || _check(action, "")) {
            return true;    // Action allowed for all.
        }

        for (final String role: identity.get().getRoles()) {
            if (_check(action, role)) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public PermissionsEntity copy()
    {
        return newBuilder().copyFrom(this).build();
    }

    /**
     * Denies an action for a role.
     *
     * @param role The role.
     * @param action The action.
     */
    public void deny(@Nonnull final String role, @Nonnull final Action action)
    {
        final Set<Action> permissions = _permissions.get(role);

        if (permissions != null) {
            permissions.remove(action);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other) {
            return true;
        }

        if (super.equals(other)) {
            return _permissions
                .equals(((PermissionsEntity) other)._permissions);
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Gets the permissions.
     *
     * @return The permissions.
     */
    @Nonnull
    @CheckReturnValue
    public Map<String, Set<Action>> getPermissions()
    {
        return _permissions;
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
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    private boolean _check(final Action action, final String role)
    {
        final Set<Action> permissions = _permissions.get(role);

        return (permissions != null) && permissions.contains(action);
    }

    /** Permissions element name. */
    public static final String ELEMENT_NAME = "Permissions";

    /** Permissions entity prefix. */
    public static final String ENTITY_PREFIX = "I";

    private final Map<String, Set<Action>> _permissions;

    /**
     * Builder.
     */
    public static class Builder
        extends AbstractEntity.Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /** {@inheritDoc}
         */
        @Override
        public PermissionsEntity build()
        {
            return new PermissionsEntity(
                getName(),
                getUUID(),
                (_permissions != null)? _permissions: new HashMap<>());
        }

        /**
         * Copies the values from an other PermissionsEntity.
         *
         * @param permissions The other PermissionsEntity.
         *
         * @return This.
         */
        @Nonnull
        public final Builder copyFrom(
                @Nonnull final PermissionsEntity permissions)
        {
            super.copyFrom(permissions);

            _permissions = permissions.getPermissions();

            return this;
        }

        private Map<String, Set<Action>> _permissions;
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
