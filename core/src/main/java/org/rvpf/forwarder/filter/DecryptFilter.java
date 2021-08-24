/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DecryptFilter.java 3932 2019-04-25 12:53:53Z SFB $
 */

package org.rvpf.forwarder.filter;

import java.io.Serializable;

import org.rvpf.base.security.Crypt;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.forwarder.ForwarderModule;

/**
 * Decrypt filter.
 */
public class DecryptFilter
    extends CryptFilter
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable[] filter(Serializable message)
    {
        if (Crypt.isEncrypted(message)) {
            final Crypt.Result cryptResult = getCrypt()
                .decrypt(message, _decryptionKeyIdents);

            if (cryptResult.isSuccess()) {
                message = cryptResult.getSerializable();
            } else {
                cryptException(cryptResult);
                message = null;
            }
        } else if (isStrict()) {
            message = null;
        }

        return (message != null)? new Serializable[] {message, }: NO_MESSAGES;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(
            final ForwarderModule forwarderModule,
            final KeyedGroups filterProperties)
    {
        if (!super.setUp(forwarderModule, filterProperties)) {
            return false;
        }

        _decryptionKeyIdents = filterProperties
            .getStrings(Crypt.DECRYPT_KEY_PROPERTY);

        for (final String decryptionKeyIdent: _decryptionKeyIdents) {
            getThisLogger()
                .debug(ForwarderMessages.DECRYPTION_KEY, decryptionKeyIdent);
        }

        return true;
    }

    private String[] _decryptionKeyIdents;
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
