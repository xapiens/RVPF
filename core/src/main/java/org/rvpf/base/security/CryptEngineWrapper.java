/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CryptEngineWrapper.java 3981 2019-05-13 15:00:56Z SFB $
 */

package org.rvpf.base.security;

import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * Crypt engine wrapper.
 */
public interface CryptEngineWrapper
{
    /**
     * Decrypts an input.
     *
     * @param encryptedStream The encrypted stream.
     * @param decryptionKeyIdents The decryption key ident (may be empty).
     * @param decryptedStream The decrypted stream.
     *
     * @throws CryptException On failure.
     */
    @CheckReturnValue
    void decrypt(
            @Nonnull InputStream encryptedStream,
            @Nonnull String[] decryptionKeyIdents,
            @Nonnull OutputStream decryptedStream)
        throws CryptException;

    /**
     * Encrypts an input.
     *
     * @param inputStream The input stream.
     * @param encryptionKeyIdents The encryption key idents (may be empty).
     * @param encryptedStream The encrypted stream.
     *
     * @throws CryptException On failure.
     */
    @CheckReturnValue
    void encrypt(
            @Nonnull InputStream inputStream,
            @Nonnull String[] encryptionKeyIdents,
            @Nonnull OutputStream encryptedStream)
        throws CryptException;

    /**
     * Asks if this engine is secure.
     *
     * @return True if secure.
     */
    @CheckReturnValue
    boolean isSecure();

    /**
     * Sets up the crypt.
     *
     * @param cryptProperties The crypt configuration properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(@Nonnull final KeyedGroups cryptProperties);

    /**
     * Signs an input.
     *
     * @param inputStream The input stream.
     * @param signingKeyIdents The signing key idents (may be empty).
     * @param signatureStream The signature stream.
     *
     * @throws CryptException On failure.
     */
    @CheckReturnValue
    void sign(
            @Nonnull InputStream inputStream,
            @Nonnull String[] signingKeyIdents,
            @Nonnull OutputStream signatureStream)
        throws CryptException;

    /**
     * Tears down what has been set up.
     */
    void tearDown();

    /**
     * Verifies a signed input.
     *
     * @param signedStream The signed stream.
     * @param signatureStream The signature stream.
     * @param verificationKeyIdents The verification key idents (may be empty).
     *
     * @return True if verified.
     *
     * @throws CryptException On failure.
     */
    @CheckReturnValue
    boolean verify(
            @Nonnull InputStream signedStream,
            @Nonnull InputStream signatureStream,
            @Nonnull String[] verificationKeyIdents)
        throws CryptException;

    ClassDef IMPL = new ClassDefImpl(NullCryptEngineWrapperImpl.class);
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
