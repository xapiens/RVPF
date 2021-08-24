/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EncryptFilter.java 3948 2019-05-02 20:37:43Z SFB $
 */

package org.rvpf.forwarder.filter;

import java.io.Serializable;

import org.rvpf.base.security.Crypt;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.forwarder.ForwarderModule;

/**
 * Encrypt filter.
 */
public class EncryptFilter
    extends CryptFilter
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable[] filter(Serializable message)
    {
        if (isStrict() || !Crypt.isEncrypted(message)) {
            final Crypt.Result cryptResult = getCrypt()
                .encrypt(message, _encryptionKeyIdents);

            if (cryptResult.isSuccess()) {
                message = cryptResult.getSerializable();
            } else {
                cryptException(cryptResult);
                message = null;
            }
        }

        if ((_signFilter != null) && (message != null)) {
            return _signFilter.filter(message);
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

        _encryptionKeyIdents = filterProperties
            .getStrings(Crypt.ENCRYPT_KEY_PROPERTY);

        for (final String keyIdent: _encryptionKeyIdents) {
            getThisLogger().debug(ForwarderMessages.ENCRYPTION_KEY, keyIdent);
        }

        if (filterProperties.getBoolean(Crypt.SIGN_PROPERTY)) {
            _signFilter = new SignFilter();

            if (!_signFilter.setUp(forwarderModule, filterProperties)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_signFilter != null) {
            _signFilter.tearDown();
        }

        super.tearDown();
    }

    private String[] _encryptionKeyIdents;
    private SignFilter _signFilter;
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
