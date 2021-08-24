/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NullCryptEngineWrapperImpl.java 4067 2019-06-08 13:39:16Z SFB $
 */

package org.rvpf.base.security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rvpf.base.util.container.KeyedGroups;

/**
 * Null crypt engine wrapper implementation.
 */
public final class NullCryptEngineWrapperImpl
    implements CryptEngineWrapper
{
    /** {@inheritDoc}
     */
    @Override
    public void decrypt(
            final InputStream encryptedStream,
            final String[] decryptionKeyIdents,
            final OutputStream decryptedStream)
    {
        _copy(encryptedStream, decryptedStream);
    }

    /** {@inheritDoc}
     */
    @Override
    public void encrypt(
            final InputStream inputStream,
            final String[] encryptionKeyIdents,
            final OutputStream encryptedStream)
    {
        _copy(inputStream, encryptedStream);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSecure()
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final KeyedGroups cryptProperties)
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void sign(
            final InputStream inputStream,
            final String[] signingKeyIdents,
            final OutputStream signatureStream) {}

    /** {@inheritDoc}
     */
    @Override
    public void tearDown() {}

    /** {@inheritDoc}
     */
    @Override
    public boolean verify(
            final InputStream signedStream,
            final InputStream signatureStream,
            final String[] verificationKeyIdents)
    {
        return true;
    }

    private static void _copy(
            final InputStream input,
            final OutputStream output)
    {
        final byte[] buffer = new byte[1024];

        try {
            for (;;) {
                final int length = input.read(buffer);

                if (length < 0) {
                    break;
                }

                output.write(buffer, 0, length);
            }
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }
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
