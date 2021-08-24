/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BCMessages.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.ext.bc;

import org.rvpf.base.logger.Messages;

/** Ext messages.
 */
public enum BCMessages
    implements Messages.Entry
{
    BC_VERSION,
    DECRYPTION_KEY,
    ENCRYPTION_KEY,
    ENCRYPTION_KEY_NOT_FOUND,
    KEY_PASSWORD_MISSING,
    NO_DECRYPTION_KEY_MATCHED,
    NO_ENCRYPTION_KEYS,
    PUBLIC_KEY,
    SECRET_KEY,
    SECRET_KEY_NOT_FOUND,
    SIGNATURE_KEY_IGNORED,
    SIGNING_KEY,
    UNEXPECTED_EXCEPTION,
    VERIFICATION_KEY,
    VERIFICATION_KEY_IN_SIGNATURE,
    VERIFICATION_KEY_UNKNOWN;

    /** {@inheritDoc}
     */
    @Override
    public String getBundleName()
    {
        return _BUNDLE_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized String toString()
    {
        if (_string == null) {
            _string = Messages.getString(this);
        }

        return _string;
    }

    private static final String _BUNDLE_NAME = "org.rvpf.ext.bc.messages.bc";

    private String _string;
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
