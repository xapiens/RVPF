/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LoginInfo.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.util;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Login informations.
 *
 * <p>Holds login informations: the user identification and its associated
 * password.</p>
 */
@Immutable
public final class LoginInfo
{
    /**
     * Constructs a login info instance.
     *
     * @param user The user to associate with the session.
     * @param password The password authenticating the user.
     */
    public LoginInfo(
            @Nonnull Optional<String> user,
            @Nonnull Optional<char[]> password)
    {
        if (user.isPresent()) {
            user = Optional.of(user.get().trim());

            if (user.get().isEmpty()) {
                user = Optional.empty();
            }
        }

        if (user.isPresent()) {
            if (!password.isPresent()) {
                password = Optional.empty();
            }
        } else {
            password = Optional.empty();
        }

        _user = user;
        _password = password;
    }

    /**
     * Gets the password.
     *
     * <p>The password will be empty if and only if the user is empty.</p>
     *
     * @return The optional password.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<char[]> getPassword()
    {
        return _password;
    }

    /**
     * Gets the user.
     *
     * @return The optional user.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getUser()
    {
        return _user;
    }

    /**
     * Asks if this login info is enabled.
     *
     * @return True if enabled.
     */
    @CheckReturnValue
    public boolean isEnabled()
    {
        return getUser().isPresent();
    }

    private final Optional<char[]> _password;
    private final Optional<String> _user;
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
