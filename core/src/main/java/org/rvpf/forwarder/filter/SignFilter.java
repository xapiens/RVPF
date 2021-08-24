/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SignFilter.java 3961 2019-05-06 20:14:59Z SFB $
 */
package org.rvpf.forwarder.filter;

import java.io.Serializable;
import org.rvpf.base.security.Crypt;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.forwarder.ForwarderModule;

/** Sign filter.
 */
public class SignFilter
    extends CryptFilter
{
    /** {@inheritDoc}
     */
    @Override
    public Serializable[] filter(Serializable message)
    {
        if (isStrict() || !Crypt.isSigned(message)) {
            final Crypt.Result cryptResult =
                getCrypt().sign(message, _signingKeyIdents);

            if (cryptResult.isSuccess()) {
                message = cryptResult.getSerializable();
            } else {
                cryptException(cryptResult);
                message = null;
            }
        }

        return (message != null)? new Serializable[] {message,}: NO_MESSAGES;
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

        _signingKeyIdents =
            filterProperties.getStrings(Crypt.SIGN_KEY_PROPERTY);
        for (final String signingKeyIdent: _signingKeyIdents) {
            getThisLogger().debug(
                ForwarderMessages.SIGNING_KEY,
                signingKeyIdent);
        }

        return true;
    }

    private String[] _signingKeyIdents;
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
