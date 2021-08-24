/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TomcatRealm.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.ext.tomcat;

import java.security.Principal;

import java.util.ArrayList;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;

import org.rvpf.base.security.Identity;
import org.rvpf.base.security.Realm;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * Tomcat realm.
 */
class TomcatRealm
    extends RealmBase
{
    /**
     * Constructs an instance.
     *
     * @param realmName The realm name.
     */
    TomcatRealm(final String realmName)
    {
        _realmName = realmName;
    }

    /** {@inheritDoc}
     */
    @Override
    public Principal authenticate(final String identifier, String credentials)
    {
        if (credentials == null) {
            credentials = "";
        }

        final Optional<Identity> identity = _realm
            .authenticate(identifier, credentials.toCharArray());

        if (identity.isPresent()) {
            return new GenericPrincipal(
                identifier,
                credentials,
                new ArrayList<>(identity.get().getRoles()));
        }

        return null;
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getName()
    {
        return _realmName;
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getPassword(final String username)
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     */
    @Override
    protected Principal getPrincipal(String identifier)
    {
        if (identifier.startsWith(_CN_PREFIX)) {
            identifier = identifier.substring(_CN_PREFIX.length());
        }

        final Identity identity = _realm.getIdentity(Optional.of(identifier));

        return new GenericPrincipal(
            identifier,
            null,
            new ArrayList<>(identity.getRoles()));
    }

    /**
     * Sets up this instance.
     *
     * @param realmProperties The realm configuration properties.
     * @param securityContext The security context.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull final KeyedGroups realmProperties,
            @Nonnull final SecurityContext securityContext)
    {
        return _realm.setUp(realmProperties, securityContext);
    }

    private static final String _CN_PREFIX = "CN=";

    private final Realm _realm = new Realm();
    private final String _realmName;
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
