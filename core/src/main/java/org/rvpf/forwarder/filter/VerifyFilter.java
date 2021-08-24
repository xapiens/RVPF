/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: VerifyFilter.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.forwarder.filter;

import java.io.Serializable;

import org.rvpf.base.security.Crypt;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.forwarder.ForwarderModule;

/**
 * Verify filter.
 */
public class VerifyFilter
    extends CryptFilter
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable[] filter(Serializable message)
    {
        if (Crypt.isSigned(message)) {
            final Crypt.Result verifyResult = getCrypt()
                .verify(message, _verificationKeyIdents);
            final boolean verified;

            if (verifyResult.isSuccess()) {
                verified = verifyResult.isVerified();
            } else {
                cryptException(verifyResult);
                verified = false;
            }

            if (!verified) {
                getThisLogger().warn(ForwarderMessages.VERIFICATION_FAILED);
            }

            message = verified? verifyResult.getSerializable(): null;
        } else if (isStrict()) {
            message = null;
        }

        if ((_decryptFilter != null) && (message != null)) {
            return _decryptFilter.filter(message);
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

        _verificationKeyIdents = filterProperties
            .getStrings(Crypt.VERIFY_KEY_PROPERTY);

        for (final String verificationKeyIdent: _verificationKeyIdents) {
            getThisLogger()
                .debug(
                    ForwarderMessages.VERIFICATION_KEY,
                    verificationKeyIdent);
        }

        if (filterProperties.getBoolean(Crypt.DECRYPT_PROPERTY)) {
            _decryptFilter = new DecryptFilter();

            if (!_decryptFilter.setUp(forwarderModule, filterProperties)) {
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
        if (_decryptFilter != null) {
            _decryptFilter.tearDown();
        }

        super.tearDown();
    }

    private DecryptFilter _decryptFilter;
    private String[] _verificationKeyIdents;
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
