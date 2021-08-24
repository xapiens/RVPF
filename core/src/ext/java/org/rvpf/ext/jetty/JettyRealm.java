/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JettyRealm.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.ext.jetty;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;

import org.rvpf.base.security.Credential;
import org.rvpf.base.security.Identity;
import org.rvpf.base.security.Realm;

/**
 * Jetty realm.
 *
 * <p>Wraps {@link Realm} for Jetty.</p>
 */
public final class JettyRealm
    extends Realm
    implements LoginService
{
    /**
     * Constructs an instance.
     *
     * @param realmName The realm name.
     */
    public JettyRealm(@Nonnull final String realmName)
    {
        _realmName = realmName;
    }

    /** {@inheritDoc}
     */
    @Override
    public IdentityService getIdentityService()
    {
        return _identityService;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return _realmName;
    }

    /** {@inheritDoc}
     */
    @Override
    public UserIdentity login(final String identifier, final Object credentials)
    {
        final Identity identity = getIdentity(Optional.of(identifier));

        return identity
            .authenticate(credentials)? getUserIdentity(identity): null;
    }

    /** {@inheritDoc}
     */
    @Override
    public void logout(final UserIdentity userIdentity)
    {
        final Identity identity = (Identity) userIdentity.getUserPrincipal();

        identity.reset();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setIdentityService(final IdentityService identityService)
    {
        _identityService = identityService;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean validate(final UserIdentity userIdentity)
    {
        final Identity identity = (Identity) userIdentity.getUserPrincipal();

        return identity.isAuthenticated();
    }

    /**
     * Gets the Jetty UserIdentity for the realm identity.
     *
     * @param identity The realm identity.
     *
     * @return The Jetty UserIdentity.
     */
    protected UserIdentity getUserIdentity(final Identity identity)
    {
        UserIdentity userIdentity = ((_Identity) identity).getUserIdentity();

        if (userIdentity == null) {
            final Subject subject = new Subject();

            subject.getPrincipals().add(identity);
            subject.setReadOnly();

            final Collection<String> roles = identity.getRoles();

            userIdentity = _identityService
                .newUserIdentity(
                    subject,
                    identity,
                    roles.toArray(new String[roles.size()]));

            ((_Identity) identity).setUserIdentity(userIdentity);
        }

        return userIdentity;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Identity newIdentity(
            final String identifier,
            final Credential credential,
            final Set<String> roles)
    {
        return new _Identity(identifier, credential, roles);
    }

    private IdentityService _identityService = new DefaultIdentityService();
    private final String _realmName;

    /**
     * Identity.
     */
    private static class _Identity
        extends Identity
    {
        /**
         * Constructs an instance.
         *
         * @param identifier The identifier.
         * @param credential The credential.
         * @param roles The roles.
         */
        _Identity(
                @Nonnull final String identifier,
                @Nonnull final Credential credential,
                @Nonnull final Set<String> roles)
        {
            super(identifier, credential, roles);
        }

        /**
         * Gets the UserIdentity.
         *
         * @return The UserIdentity.
         */
        UserIdentity getUserIdentity()
        {
            return _userIdentity;
        }

        /**
         * Sets the UserIdentity.
         *
         * @param userIdentity The UserIdentity.
         */
        void setUserIdentity(final UserIdentity userIdentity)
        {
            _userIdentity = userIdentity;
        }

        private UserIdentity _userIdentity;
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
