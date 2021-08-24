/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Identity.java 3981 2019-05-13 15:00:56Z SFB $
 */

package org.rvpf.base.security;

import java.security.Principal;

import java.util.Set;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;

/**
 * Identity.
 */
public class Identity
    implements Principal
{
    /**
     * Constructs an instance.
     *
     * @param identifier The identifier.
     * @param credential The credential.
     * @param roles The roles.
     */
    protected Identity(
            @Nonnull final String identifier,
            @Nonnull final Credential credential,
            @Nonnull final Set<String> roles)
    {
        _identifier = identifier;
        _credential = credential;
        _roles = Require.notNull(roles);
    }

    /**
     * Authenticates.
     *
     * @param credential A supplied credential.
     *
     * @return True if authenticated.
     */
    @CheckReturnValue
    public boolean authenticate(@Nonnull final Object credential)
    {
        _authenticated = (_credential != null)? _credential
            .check(credential): false;

        return _authenticated;
    }

    /**
     * Gets the identifier.
     *
     * @return The identifier.
     */
    @Nonnull
    @CheckReturnValue
    public String getIdentifier()
    {
        return _identifier;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return getIdentifier();
    }

    /**
     * Gets the roles.
     *
     * @return The roles.
     */
    @Nonnull
    @CheckReturnValue
    public Set<String> getRoles()
    {
        return _roles;
    }

    /**
     * Asks if authenticated.
     *
     * @return True if authenticated.
     */
    @CheckReturnValue
    public boolean isAuthenticated()
    {
        return _authenticated;
    }

    /**
     * Asks if this identity acts in one of the specified roles.
     *
     * @param roles The roles.
     *
     * @return True if a role matches one of the specified roles.
     */
    @CheckReturnValue
    public boolean isInRoles(@Nonnull final String[] roles)
    {
        for (final String role: roles) {
            if (_roles.contains(role)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Resets.
     */
    public void reset()
    {
        _authenticated = false;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getIdentifier();
    }

    private boolean _authenticated;
    private final Credential _credential;
    private final String _identifier;
    private final Set<String> _roles;
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
