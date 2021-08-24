/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: PGPCryptEngineWrapperImpl.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.ext.bc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.security.SecureRandom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.bc.BcPGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc
    .BcPublicKeyKeyEncryptionMethodGenerator;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.security.Crypt;
import org.rvpf.base.security.CryptEngineWrapper;
import org.rvpf.base.security.CryptException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.ResourceFileFactory;
import org.rvpf.base.util.Version;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * PGP crypt engine wrapper implementation.
 */
public final class PGPCryptEngineWrapperImpl
    implements CryptEngineWrapper
{
    /** {@inheritDoc}
     */
    @Override
    public void decrypt(
            final InputStream encryptedStream,
            final String[] decryptionKeyIdents,
            final OutputStream decryptedStream)
        throws CryptException
    {
        final Set<Long> decryptionKeyIDs = new HashSet<>();

        for (final String decryptionKeyIdent: decryptionKeyIdents) {
            _LOGGER.trace(BCMessages.DECRYPTION_KEY, decryptionKeyIdent);
            decryptionKeyIDs.add(Long.valueOf(_keyID(decryptionKeyIdent)));
        }

        try {
            final InputStream decoderStream = PGPUtil
                .getDecoderStream(encryptedStream);
            final PGPObjectFactory objectFactory = new PGPObjectFactory(
                decoderStream,
                new BcKeyFingerprintCalculator());
            final PGPEncryptedDataList encryptedDataList =
                (PGPEncryptedDataList) objectFactory
                    .nextObject();
            PGPPublicKeyEncryptedData encryptedData = null;
            PGPSecretKey secretKey = null;

            for (final Object object: encryptedDataList) {
                encryptedData = (PGPPublicKeyEncryptedData) object;

                final long decryptionKeyID = encryptedData.getKeyID();

                if (decryptionKeyIDs.isEmpty()
                        || decryptionKeyIDs.contains(
                            Long.valueOf(decryptionKeyID))) {
                    final PGPSecretKey key = _getSecretKey(decryptionKeyID);

                    if (key != null) {
                        secretKey = key;

                        break;
                    }
                }
            }

            if (secretKey == null) {
                throw new CryptException(BCMessages.NO_DECRYPTION_KEY_MATCHED);
            }

            final PGPPrivateKey decryptionKey = _getPrivateKey(secretKey);
            final InputStream clearStream = encryptedData
                .getDataStream(
                    new BcPublicKeyDataDecryptorFactory(decryptionKey));
            final PGPObjectFactory plainFactory = new BcPGPObjectFactory(
                clearStream);
            final PGPCompressedData compressedData =
                (PGPCompressedData) plainFactory
                    .nextObject();
            final PGPLiteralData literalData =
                (PGPLiteralData) new BcPGPObjectFactory(
                    compressedData.getDataStream())
                    .nextObject();

            _copy(literalData.getInputStream(), decryptedStream);
        } catch (final IOException|PGPException exception) {
            throw new CryptException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void encrypt(
            final InputStream inputStream,
            final String[] encryptionKeyIdents,
            final OutputStream encryptedStream)
        throws CryptException
    {
        final Collection<PGPPublicKey> encryptionKeys = new LinkedList<>();

        if (encryptionKeyIdents.length > 0) {
            for (final String targetKeyIdent: encryptionKeyIdents) {
                encryptionKeys.add(_getEncryptionKey(targetKeyIdent));
            }
        } else {
            encryptionKeys.add(_getEncryptionKey(null));
        }

        if (_LOGGER.isTraceEnabled()) {
            for (final PGPPublicKey encryptionKey: encryptionKeys) {
                _LOGGER
                    .trace(
                        BCMessages.ENCRYPTION_KEY,
                        _keyIdent(encryptionKey.getKeyID()));
            }
        }

        try {
            final ByteArrayOutputStream compressedStream =
                new ByteArrayOutputStream();
            final PGPCompressedDataGenerator compressedDataGenerator =
                new PGPCompressedDataGenerator(
                    PGPCompressedData.ZLIB);
            final PGPLiteralDataGenerator literalDataGenerator =
                new PGPLiteralDataGenerator();

            _copy(
                inputStream,
                literalDataGenerator
                    .open(
                        compressedDataGenerator.open(compressedStream),
                        PGPLiteralData.TEXT,
                        "",
                        inputStream.available(),
                        new Date()));
            literalDataGenerator.close();
            compressedDataGenerator.close();

            final BcPGPDataEncryptorBuilder dataEncryptorBuilder =
                new BcPGPDataEncryptorBuilder(
                    CIPHER_ALGORITHM);

            dataEncryptorBuilder.setWithIntegrityPacket(true);
            dataEncryptorBuilder.setSecureRandom(_RANDOM);

            final PGPEncryptedDataGenerator encryptedDataGenerator =
                new PGPEncryptedDataGenerator(
                    dataEncryptorBuilder);

            for (final PGPPublicKey targetKey: encryptionKeys) {
                encryptedDataGenerator
                    .addMethod(
                        new BcPublicKeyKeyEncryptionMethodGenerator(targetKey));
            }

            final OutputStream armoredStream = new ArmoredOutputStream(
                encryptedStream);
            final byte[] compressedBytes = compressedStream.toByteArray();
            final OutputStream encryptorStream = encryptedDataGenerator
                .open(armoredStream, compressedBytes.length);

            encryptorStream.write(compressedBytes);
            encryptorStream.close();
            armoredStream.close();
        } catch (final IOException|PGPException exception) {
            throw new CryptException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isSecure()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final KeyedGroups cryptProperties)
    {
        final Manifest manifest = Version.getManifest(PGPException.class);

        _LOGGER
            .info(
                BCMessages.BC_VERSION,
                Version
                    .getManifestAttribute(
                            manifest,
                                    Version.APPLICATION_NAME_ATTRIBUTE),
                Version
                    .getManifestAttribute(
                            manifest,
                                    Version.BUNDLE_VERSION_ATTRIBUTE));

        try {
            final KeyedGroups publicProperties = cryptProperties
                .getGroup(Crypt.PUBLIC_PROPERTIES);
            final Optional<String> publicPath = _path(
                publicProperties.getString(Crypt.PATH_PROPERTY));

            if (publicPath.isPresent()) {
                _LOGGER.debug(BaseMessages.PUBLIC_PATH, publicPath.get());

                final InputStream publicStream = new FileInputStream(
                    publicPath.get());
                final InputStream decoderStream = PGPUtil
                    .getDecoderStream(publicStream);

                _publicKeyRingCollection = new BcPGPPublicKeyRingCollection(
                    decoderStream);
                decoderStream.close();

                if (_LOGGER.isTraceEnabled()) {
                    for (final PGPPublicKeyRing publicKeyRing:
                            _publicKeyRingCollection) {
                        for (final PGPPublicKey publicKey: publicKeyRing) {
                            _LOGGER
                                .trace(
                                    BCMessages.PUBLIC_KEY,
                                    _keyIdent(publicKey.getKeyID()));
                        }
                    }
                }
            }

            final KeyedGroups privateProperties = cryptProperties
                .getGroup(Crypt.PRIVATE_PROPERTIES);
            final Optional<String> privatePath = _path(
                privateProperties.getString(Crypt.PATH_PROPERTY));

            if (privatePath.isPresent()) {
                _LOGGER.debug(BaseMessages.PRIVATE_PATH, privatePath.get());

                final InputStream privateStream = new FileInputStream(
                    privatePath.get());
                final InputStream decoderStream = PGPUtil
                    .getDecoderStream(privateStream);

                _secretKeyRingCollection = new BcPGPSecretKeyRingCollection(
                    decoderStream);
                decoderStream.close();

                if (_LOGGER.isTraceEnabled()) {
                    for (final PGPSecretKeyRing secretKeyRing:
                            _secretKeyRingCollection) {
                        for (final PGPSecretKey secretKey: secretKeyRing) {
                            _LOGGER
                                .trace(
                                    BCMessages.SECRET_KEY,
                                    _keyIdent(secretKey.getKeyID()));
                        }
                    }
                }

                for (final KeyedGroups key:
                        privateProperties.getGroups(Crypt.KEY_PROPERTIES)) {
                    final Optional<String> ident = key
                        .getString(Crypt.IDENT_PROPERTY, Optional.of(""));
                    final Optional<char[]> password = key
                        .getPassword(Crypt.PASSWORD_PROPERTY);

                    _privateKeysPassword
                        .put(
                            ident.isPresent()? ident.get(): _EMPTY_IDENT,
                            password.isPresent()
                            ? password.get(): _EMPTY_PASSWORD);
                }
            }
        } catch (final IOException|PGPException exception) {
            _LOGGER
                .error(
                    exception,
                    BCMessages.UNEXPECTED_EXCEPTION,
                    exception.getMessage());

            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void sign(
            final InputStream inputStream,
            final String[] signingKeyIdents,
            final OutputStream signatureStream)
        throws CryptException
    {
        final Collection<PGPSecretKey> signingKeys =
            new ArrayList<PGPSecretKey>(
                Math.max(signingKeyIdents.length, 1));

        if (signingKeyIdents.length > 0) {
            for (final String signingKeyIdent: signingKeyIdents) {
                signingKeys.add(_getSecretKey(signingKeyIdent));
            }
        } else {
            signingKeys.add(_getSecretKey(null));
        }

        if (_LOGGER.isTraceEnabled()) {
            for (final PGPSecretKey signingKey: signingKeys) {
                _LOGGER
                    .trace(
                        BCMessages.SIGNING_KEY,
                        _keyIdent(signingKey.getKeyID()));
            }
        }

        try {
            final ArmoredOutputStream armoredStream = new ArmoredOutputStream(
                signatureStream);
            final byte[] inputBytes = new byte[inputStream.available()];

            Require
                .success(
                    inputStream
                        .read(
                                inputBytes,
                                        0,
                                        inputBytes.length) == inputBytes.length);

            for (final PGPSecretKey signingKey: signingKeys) {
                final PGPPublicKey publicKey = signingKey.getPublicKey();
                final PGPPrivateKey privateKey = _getPrivateKey(signingKey);
                final PGPContentSignerBuilder signerBuilder =
                    new BcPGPContentSignerBuilder(
                        publicKey.getAlgorithm(),
                        HASH_ALGORITHM);
                final PGPSignatureGenerator signatureGenerator =
                    new PGPSignatureGenerator(
                        signerBuilder);

                signatureGenerator
                    .init(PGPSignature.CANONICAL_TEXT_DOCUMENT, privateKey);

                final Iterator<?> userIDs = publicKey.getUserIDs();

                if (userIDs.hasNext()) {
                    final PGPSignatureSubpacketGenerator subpacketGenerator =
                        new PGPSignatureSubpacketGenerator();

                    subpacketGenerator
                        .setSignerUserID(false, (String) userIDs.next());
                    signatureGenerator
                        .setHashedSubpackets(subpacketGenerator.generate());
                }

                signatureGenerator.update(inputBytes);
                signatureGenerator.generate().encode(armoredStream);
            }

            armoredStream.close();
        } catch (final IOException|PGPException exception) {
            throw new CryptException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        _publicKeyRingCollection = null;
        _secretKeyRingCollection = null;
        _privateKeysPassword = null;
        _privateKeys.clear();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean verify(
            final InputStream signedStream,
            final InputStream signatureStream,
            final String[] verificationKeyIdents)
        throws CryptException
    {
        final Set<Long> verificationKeyIDs = new HashSet<>();

        for (final String verificationKeyIdent: verificationKeyIdents) {
            _LOGGER.trace(BCMessages.VERIFICATION_KEY, verificationKeyIdent);
            verificationKeyIDs.add(Long.valueOf(_keyID(verificationKeyIdent)));
        }

        boolean verified = false;

        try {
            final InputStream decoderStream = PGPUtil
                .getDecoderStream(signatureStream);
            final PGPObjectFactory objectFactory = new PGPObjectFactory(
                decoderStream,
                new BcKeyFingerprintCalculator());
            final PGPSignatureList signatureList =
                (PGPSignatureList) objectFactory
                    .nextObject();

            if (signatureList.isEmpty()) {
                return false;
            }

            final byte[] inputBytes = new byte[signedStream.available()];

            Require
                .success(
                    signedStream
                        .read(
                                inputBytes,
                                        0,
                                        inputBytes.length) == inputBytes.length);

            for (final PGPSignature signature: signatureList) {
                final long keyID = signature.getKeyID();

                if (verificationKeyIDs.isEmpty()
                        || verificationKeyIDs.contains(Long.valueOf(keyID))) {
                    final PGPPublicKey key = _getPublicKey(keyID);

                    _LOGGER
                        .trace(
                            BCMessages.VERIFICATION_KEY_IN_SIGNATURE,
                            _keyIdent(keyID));

                    if (key == null) {
                        _LOGGER
                            .trace(
                                BCMessages.VERIFICATION_KEY_UNKNOWN,
                                _keyIdent(keyID));

                        return false;
                    }

                    signature
                        .init(new BcPGPContentVerifierBuilderProvider(), key);
                    signature.update(inputBytes);

                    if (!signature.verify()) {
                        return false;
                    }

                    verified = true;
                } else {
                    _LOGGER
                        .trace(
                            BCMessages.SIGNATURE_KEY_IGNORED,
                            _keyIdent(keyID));
                }
            }
        } catch (final IOException|PGPException exception) {
            throw new CryptException(exception);
        }

        return verified;
    }

    private static void _copy(
            final InputStream input,
            final OutputStream output)
        throws IOException
    {
        final byte[] buffer = new byte[1024];

        for (;;) {
            final int length = input.read(buffer);

            if (length < 0) {
                break;
            }

            output.write(buffer, 0, length);
        }
    }

    private static long _keyID(final String keyIdent)
    {
        return Long.parseUnsignedLong(keyIdent, 16);
    }

    private static String _keyIdent(final long keyID)
    {
        return Long.toHexString(keyID);
    }

    private static Optional<String> _path(
            final Optional<String> optionalPath)
        throws FileNotFoundException
    {
        if (!optionalPath.isPresent()) {
            return Optional.empty();
        }

        final String path = optionalPath.get().trim();

        if (path.isEmpty()) {
            return Optional.empty();
        }

        final File file = ResourceFileFactory.newResourceFile(path);

        if ((file == null) || !file.isFile()) {
            throw new FileNotFoundException(
                Message.format(BaseMessages.RESOURCE_NOT_FOUND, path));
        }

        return Optional.of(file.getAbsolutePath());
    }

    private PGPPublicKey _getEncryptionKey(
            final String ident)
        throws CryptException
    {
        if (ident == null) {
            for (final PGPPublicKeyRing keyRing: _publicKeyRingCollection) {
                for (final PGPPublicKey key: keyRing) {
                    if (key.isEncryptionKey()) {
                        return key;
                    }
                }
            }

            throw new CryptException(BCMessages.NO_ENCRYPTION_KEYS);
        }

        final PGPPublicKey key;

        try {
            key = _publicKeyRingCollection.getPublicKey(_keyID(ident));
        } catch (final PGPException exception) {
            throw new CryptException(exception);
        }

        if (key == null) {
            throw new CryptException(
                BCMessages.ENCRYPTION_KEY_NOT_FOUND,
                ident);
        }

        return key;
    }

    private PGPPrivateKey _getPrivateKey(
            final PGPSecretKey secretKey)
        throws CryptException
    {
        final long keyID = secretKey.getKeyID();
        PGPPrivateKey privateKey = _privateKeys.get(Long.valueOf(keyID));

        if (privateKey == null) {
            final String secretKeyIdent = _keyIdent(secretKey.getKeyID());
            char[] password = _privateKeysPassword.get(secretKeyIdent);

            if (password == null) {
                password = _privateKeysPassword.get(_EMPTY_IDENT);

                if (password == null) {
                    throw new CryptException(
                        BCMessages.KEY_PASSWORD_MISSING,
                        secretKeyIdent);
                }
            }

            final PBESecretKeyDecryptor decryptor =
                new BcPBESecretKeyDecryptorBuilder(
                    new BcPGPDigestCalculatorProvider())
                    .build(password);

            try {
                privateKey = secretKey.extractPrivateKey(decryptor);
            } catch (final PGPException exception) {
                throw new CryptException(exception);
            }

            _privateKeys.put(Long.valueOf(keyID), privateKey);
        }

        return privateKey;
    }

    private PGPPublicKey _getPublicKey(final long keyID)
        throws CryptException
    {
        try {
            return _publicKeyRingCollection.getPublicKey(keyID);
        } catch (final PGPException exception) {
            throw new CryptException(exception);
        }
    }

    private PGPSecretKey _getSecretKey(final long keyID)
        throws CryptException
    {
        try {
            return _secretKeyRingCollection.getSecretKey(keyID);
        } catch (final PGPException exception) {
            throw new CryptException(exception);
        }
    }

    private PGPSecretKey _getSecretKey(final String ident)
        throws CryptException
    {
        try {
            PGPSecretKey secretKey = null;

            if (ident != null) {
                secretKey = _secretKeyRingCollection
                    .getSecretKey(_keyID(ident));
            } else {
                for (final PGPSecretKeyRing keyRing: _secretKeyRingCollection) {
                    for (final PGPSecretKey key: keyRing) {
                        if (key.isSigningKey()) {
                            secretKey = key;

                            break;
                        }
                    }
                }
            }

            if (secretKey == null) {
                throw new CryptException(
                    BCMessages.SECRET_KEY_NOT_FOUND,
                    ident);
            }

            return secretKey;
        } catch (final PGPException exception) {
            throw new RuntimeException(exception);
        }
    }

    /** Cipher algorithm. */
    public static final int CIPHER_ALGORITHM = PGPEncryptedData.BLOWFISH;

    /** Hash algorithm. */
    public static final int HASH_ALGORITHM = HashAlgorithmTags.RIPEMD160;
    private static final String _EMPTY_IDENT = "";
    private static final char[] _EMPTY_PASSWORD = new char[0];
    private static final Logger _LOGGER = Logger
        .getInstance(PGPCryptEngineWrapperImpl.class);
    private static final SecureRandom _RANDOM = new SecureRandom();

    private final Map<Long, PGPPrivateKey> _privateKeys = new HashMap<>();
    private Map<String, char[]> _privateKeysPassword =
        new ConcurrentHashMap<>();
    private PGPPublicKeyRingCollection _publicKeyRingCollection;
    private PGPSecretKeyRingCollection _secretKeyRingCollection;
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
