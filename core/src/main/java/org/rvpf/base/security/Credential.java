/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Credential.java 3981 2019-05-13 15:00:56Z SFB $
 */

package org.rvpf.base.security;

import java.util.Arrays;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;

/**
 * Credential.
 */
public final class Credential
{
    /**
     * Constructs an instance.
     *
     * @param password The password.
     */
    public Credential(@Nonnull final char[] password)
    {
        for (int i = 0; ; ++i) {
            if (i == CRYPT_PREFIX.length()) {
                _password = Arrays.copyOfRange(password, i, password.length);
                _crypt = true;

                break;
            }

            if ((i >= password.length)
                    || (password[i] != CRYPT_PREFIX.charAt(i))) {
                _password = password;
                _crypt = false;

                break;
            }
        }
    }

    /**
     * Checks a supplied credential against the reference.
     *
     * @param credential A supplied credential.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean check(@Nonnull Object credential)
    {
        if (Require.notNull(credential) instanceof String) {
            credential = ((String) credential).toCharArray();
        }

        if (credential instanceof char[]) {
            char[] password = (char[]) credential;

            if (_crypt) {
                password = UnixCrypt
                    .crypt(password, new String(_password, 0, 2))
                    .toCharArray();
            }

            return Arrays.equals(password, _password);
        }

        return false;        
    }

    /** CRYPT prefix. */
    public static final String CRYPT_PREFIX = "CRYPT:";

    private final boolean _crypt;
    private final char[] _password;
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
