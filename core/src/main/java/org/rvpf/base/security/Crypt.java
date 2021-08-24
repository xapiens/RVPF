/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Crypt.java 4082 2019-06-14 18:38:55Z SFB $
 */

package org.rvpf.base.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import java.nio.charset.StandardCharsets;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.ProxyReader;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.streamer.Streamer;

/**
 * Crypt.
 */
public final class Crypt
{
    /**
     * Returns the encrypted class.
     *
     * <p>Needed by the encrypted converter.</p>
     *
     * @return The encrypted class.
     */
    @Nonnull
    @CheckReturnValue
    public static Class<?> encryptedClass()
    {
        return Encrypted.class;
    }

    /**
     * Asks if a serializable is encrypted.
     *
     * @param serializable The serializable.
     *
     * @return True if encrypted.
     */
    @CheckReturnValue
    public static boolean isEncrypted(@Nonnull final Serializable serializable)
    {
        return serializable instanceof Encrypted;
    }

    /**
     * Asks if a serializable is signed.
     *
     * @param serializable The serializable.
     *
     * @return True if signed.
     */
    @CheckReturnValue
    public static boolean isSigned(@Nonnull final Serializable serializable)
    {
        return serializable instanceof Signed;
    }

    /**
     * Returns an encrypted object.
     *
     * <p>Needed by tests and the encrypted converter.</p>
     *
     * @param encrypted A string representing an encrypted object.
     *
     * @return An encrypted object.
     */
    @Nonnull
    @CheckReturnValue
    public static Serializable newEncrypted(@Nonnull final String encrypted)
    {
        return new Encrypted(encrypted);
    }

    /**
     * Returns a signed object.
     *
     * <p>Needed by tests.</p>
     *
     * @param serializable A serializable.
     * @param signed A string representing signed object.
     *
     * @return An encrypted object.
     */
    @Nonnull
    @CheckReturnValue
    public static Serializable newSigned(
            @Nonnull final Serializable serializable,
            @Nonnull final String signed)
    {
        return new Signed(serializable, signed);
    }

    /**
     * Returns the signed class.
     *
     * <p>Needed by the signed converter.
     *
     * @return The signed class.
     */
    @Nonnull
    @CheckReturnValue
    public static Class<?> signedClass()
    {
        return Signed.class;
    }

    /**
     * Decrypts a serializable when encrypted.
     *
     * @param serializable The object.
     * @param decryptionKeyIdents The decryption key idents (may be empty).
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized Result decrypt(
            @Nonnull final Serializable serializable,
            @Nonnull final String[] decryptionKeyIdents)
    {
        if (!(serializable instanceof Encrypted)) {
            return new Result(serializable, false, Optional.empty());
        }

        Result result;

        try {
            final String encrypted = ((Encrypted) serializable).getEncrypted();

            _LOGGER.trace(BaseMessages.DECRYPTING, encrypted);

            final byte[] encryptedBytes = encrypted
                .getBytes(StandardCharsets.UTF_8);
            final CryptEngineWrapper engineWrapper = _engineWrapper.get();

            _outputStream.reset();
            engineWrapper
                .decrypt(
                    new ByteArrayInputStream(encryptedBytes),
                    Require.notNull(decryptionKeyIdents),
                    _outputStream);

            try {
                _reader
                    .setProxied(
                        Optional
                            .of(
                                    new StringReader(
                                            _outputStream.toString(
                                                    StandardCharsets.UTF_8.name()))));
            } catch (final UnsupportedEncodingException exception) {
                throw new InternalError(exception);
            }

            result = new Result(_streamerInput.next(), false, Optional.empty());

            _reader.setProxied(Optional.empty());

            if (_LOGGER.isTraceEnabled()) {
                try {
                    _LOGGER
                        .trace(
                            BaseMessages.DECRYPTED,
                            _outputStream
                                .toString(StandardCharsets.UTF_8.name()));
                } catch (final UnsupportedEncodingException exception) {
                    throw new InternalError(exception);
                }
            }
        } catch (final CryptException exception) {
            result = new Result(null, false, Optional.of(exception));
        }

        return result;
    }

    /**
     * Encrypts a serializable.
     *
     * @param serializable The serializable.
     * @param encryptionKeyIdents The encryption key idents (may be empty).
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized Result encrypt(
            @Nullable final Serializable serializable,
            @Nonnull final String[] encryptionKeyIdents)
    {
        Result result;

        try {
            final String xmlString = _toXML(serializable);

            _LOGGER.trace(BaseMessages.ENCRYPTING, xmlString);

            final byte[] xmlBytes = xmlString.getBytes(StandardCharsets.UTF_8);
            final CryptEngineWrapper engineWrapper = _engineWrapper.get();

            _outputStream.reset();
            engineWrapper
                .encrypt(
                    new ByteArrayInputStream(xmlBytes),
                    Require.notNull(encryptionKeyIdents),
                    _outputStream);

            try {
                result = new Result(
                    new Encrypted(
                        _outputStream.toString(StandardCharsets.UTF_8.name())),
                    false,
                    Optional.empty());
            } catch (final UnsupportedEncodingException exception) {
                throw new InternalError(exception);
            }

            _LOGGER.trace(BaseMessages.ENCRYPTED, result.getSerializable());
        } catch (final CryptException exception) {
            result = new Result(null, false, Optional.of(exception));
        }

        return result;
    }

    /**
     * Encryts and signs a serializable.
     *
     * @param serializable The serializable.
     * @param encryptionKeyIdents The encryption key idents (may be empty).
     * @param signingKeyIdents The signing key idents (may be empty).
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Result encryptAndSign(
            @Nullable final Serializable serializable,
            @Nonnull final String[] encryptionKeyIdents,
            @Nonnull final String[] signingKeyIdents)
    {
        Result result = encrypt(serializable, encryptionKeyIdents);

        if (result.isSuccess()) {
            result = sign(result.getSerializable(), signingKeyIdents);
        }

        return result;
    }

    /**
     * Gets the streamer.
     *
     * @return The streamer.
     */
    @Nonnull
    @CheckReturnValue
    public Streamer getStreamer()
    {
        return _streamer;
    }

    /**
     * Asks if the engine is secure.
     *
     * @return True if the engine secure.
     */
    @CheckReturnValue
    public synchronized boolean isSecure()
    {
        final CryptEngineWrapper engineWrapper = _engineWrapper.get();

        return engineWrapper.isSecure();
    }

    /**
     * Loads the content from a file.
     *
     * @param fromFile The file.
     * @param verify True to verify.
     * @param verifyKeyIdents The verify key idents.
     * @param decrypt True to decrypt.
     * @param decryptKeyIdents The decrypt idents.
     *
     * @return The serializable contained in the file (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public Serializable load(
            @Nonnull final File fromFile,
            final boolean verify,
            @Nonnull final String[] verifyKeyIdents,
            final boolean decrypt,
            @Nonnull final String[] decryptKeyIdents)
    {
        final XMLDocument xmlDocument = XMLDocument.load(fromFile);

        if (xmlDocument == null) {
            return null;
        }

        return load(
            xmlDocument,
            fromFile.getAbsolutePath(),
            verify,
            verifyKeyIdents,
            decrypt,
            decryptKeyIdents);
    }

    /**
     * Loads the content from a file.
     *
     * @param xmlDocument The XML document.
     * @param origin The origin of the document.
     * @param verify True to verify.
     * @param verifyKeyIdents The verify key idents (may be empty).
     * @param decrypt True to decrypt.
     * @param decryptKeyIdents The decrypt idents (may be empty).
     *
     * @return The serializable contained in the file (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public Serializable load(
            @Nonnull final XMLDocument xmlDocument,
            @Nonnull final String origin,
            final boolean verify,
            @Nonnull final String[] verifyKeyIdents,
            final boolean decrypt,
            @Nonnull final String[] decryptKeyIdents)
    {
        final Streamer.Input streamerInput = _streamer.newInput(xmlDocument);

        if (!streamerInput.hasNext()) {
            _LOGGER.error(BaseMessages.NOTHING_RECOGNIZABLE, origin);

            return null;
        }

        Serializable serializable = streamerInput.next();

        streamerInput.close();

        if (serializable == null) {
            _LOGGER.error(BaseMessages.NOTHING_RECOGNIZABLE, origin);

            return null;
        }

        if (verify) {
            final Crypt.Result cryptResult = verify(
                serializable,
                verifyKeyIdents);

            if (cryptResult.isFailure()) {
                final Exception exception = cryptResult.getException();

                if (exception.getCause() != null) {
                    throw new RuntimeException(exception.getCause());
                }

                _LOGGER
                    .warn(
                        BaseMessages.VERIFICATION_FAILED_,
                        exception.getMessage());

                return null;
            }

            if (!cryptResult.isVerified()) {
                _LOGGER.warn(BaseMessages.VERIFICATION_FAILED);

                return null;
            }

            serializable = cryptResult.getSerializable();
            Require.notNull(serializable);
        }

        if (decrypt) {
            final Crypt.Result cryptResult = decrypt(
                serializable,
                decryptKeyIdents);

            if (cryptResult.isFailure()) {
                final Exception exception = cryptResult.getException();

                if (exception.getCause() != null) {
                    throw new RuntimeException(exception.getCause());
                }

                _LOGGER
                    .warn(
                        BaseMessages.DECRYPTION_FAILED_,
                        exception.getMessage());

                return null;
            }

            serializable = Require.notNull(cryptResult.getSerializable());
        }

        return serializable;
    }

    /**
     * Sets up this.
     *
     * @param cryptProperties The crypt configuration properties.
     * @param streamerProperties The optional streamer properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public synchronized boolean setUp(
            @Nonnull final KeyedGroups cryptProperties,
            @Nonnull final Optional<KeyedGroups> streamerProperties)
    {
        if (!_streamer.setUp(streamerProperties, Optional.empty())) {
            return false;
        }

        _streamerInput = _streamer.newInput(_reader);
        _streamerOutput = _streamer.newOutput(_writer);

        final ClassDef cryptClassDef = cryptProperties
            .getClassDef(CLASS_PROPERTY, CryptEngineWrapper.IMPL);
        final CryptEngineWrapper engineWrapper = cryptClassDef
            .createInstance(CryptEngineWrapper.class);

        if (engineWrapper == null) {
            return false;
        }

        _LOGGER
            .debug(
                BaseMessages.CRYPT_ENGINE_WRAPPER,
                engineWrapper.getClass().getSimpleName());

        if (!engineWrapper.setUp(cryptProperties)) {
            return false;
        }

        _engineWrapper.set(engineWrapper);

        return true;
    }

    /**
     * Signs a serializable.
     *
     * @param serializable The serializable.
     * @param signingKeyIdents The signing key idents (may be empty).
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized Result sign(
            @Nullable final Serializable serializable,
            @Nonnull final String[] signingKeyIdents)
    {
        Result result;

        try {
            final String xmlString = _toXML(serializable);

            _LOGGER.trace(BaseMessages.SIGNING, xmlString);

            final byte[] xmlBytes = xmlString.getBytes(StandardCharsets.UTF_8);
            final CryptEngineWrapper engineWrapper = _engineWrapper.get();

            _outputStream.reset();
            engineWrapper
                .sign(
                    new ByteArrayInputStream(xmlBytes),
                    Require.notNull(signingKeyIdents),
                    _outputStream);

            final Signed signed;

            try {
                signed = new Signed(
                    serializable,
                    _outputStream.toString(StandardCharsets.UTF_8.name()));
            } catch (final UnsupportedEncodingException exception) {
                throw new InternalError(exception);
            }

            result = new Result(signed, false, Optional.empty());

            _LOGGER.trace(BaseMessages.SIGNED, signed.getSignature());
        } catch (final CryptException exception) {
            result = new Result(null, false, Optional.of(exception));
        }

        return result;
    }

    /**
     * Tears down what has been set up.
     */
    public synchronized void tearDown()
    {
        final CryptEngineWrapper engineWrapper = _engineWrapper.getAndSet(null);

        if (engineWrapper != null) {
            engineWrapper.tearDown();
            _streamer.tearDown();
        }
    }

    /**
     * Verifies the signature of a serializable.
     *
     * @param serializable The serializable.
     * @param verificationKeyIdents The verification key idents (may be empty).
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public synchronized Result verify(
            @Nonnull final Serializable serializable,
            @Nonnull final String[] verificationKeyIdents)
    {
        if (!(serializable instanceof Signed)) {
            return new Result(serializable, false, Optional.empty());
        }

        Result result;

        try {
            final Signed signed = (Signed) serializable;
            final String xmlString = _toXML(signed.getSigned());
            final byte[] xmlBytes = xmlString.getBytes(StandardCharsets.UTF_8);
            final byte[] signatureBytes = signed
                .getSignature()
                .getBytes(StandardCharsets.UTF_8);

            if (_LOGGER.isTraceEnabled()) {
                _LOGGER
                    .trace(
                        BaseMessages.VERIFYING,
                        xmlString,
                        signed.getSignature());
            }

            final CryptEngineWrapper engineWrapper = _engineWrapper.get();
            final boolean verified = engineWrapper
                .verify(
                    new ByteArrayInputStream(xmlBytes),
                    new ByteArrayInputStream(signatureBytes),
                    Require.notNull(verificationKeyIdents));

            result = new Result(signed.getSigned(), verified, Optional.empty());

            _LOGGER.trace(BaseMessages.VERIFIED, Boolean.valueOf(verified));
        } catch (final CryptException exception) {
            result = new Result(null, false, Optional.of(exception));
        }

        return result;
    }

    /**
     * Verifies and decrypts a serializable.
     *
     * @param serializable The serializable.
     * @param verificationKeyIdents The verification key idents (may be empty).
     * @param decryptionKeyIdent The decryption key ident (may be empty).
     *
     * @return The result.
     */
    @Nonnull
    @CheckReturnValue
    public Result verifyAndDecrypt(
            @Nonnull Serializable serializable,
            @Nonnull final String[] verificationKeyIdents,
            @Nonnull final String[] decryptionKeyIdent)
    {
        Result result = verify(serializable, verificationKeyIdents);

        if (result.isSuccess()) {
            if (result.isVerified()) {
                serializable = result.getSerializable();

                final Result decryptResult = decrypt(
                    serializable,
                    decryptionKeyIdent);

                if (decryptResult.isSuccess()) {
                    result = new Result(
                        decryptResult.getSerializable(),
                        true,
                        Optional.empty());
                } else {
                    result = new Result(
                        null,
                        true,
                        Optional.of(decryptResult.getException()));
                }
            }
        }

        return result;
    }

    private String _toXML(final Serializable serializable)
        throws CryptException
    {
        _writer.reset();

        if (!_streamerOutput.add(serializable)) {
            throw new CryptException();
        }

        _streamerOutput.flush();

        return _writer.toString();
    }

    /** Class property. */
    public static final String CLASS_PROPERTY = "class";

    /** Decrypt key property. */
    public static final String DECRYPT_KEY_PROPERTY = "devrypt.key";

    /** Decrypt property. */
    public static final String DECRYPT_PROPERTY = "decrypt";

    /** Encrypt key property. */
    public static final String ENCRYPT_KEY_PROPERTY = "encrypt.key";

    /** Encrypt property. */
    public static final String ENCRYPT_PROPERTY = "encrypt";

    /** Ident property. */
    public static final String IDENT_PROPERTY = SecurityContext.IDENT_PROPERTY;

    /** Key properties. */
    public static final String KEY_PROPERTIES = SecurityContext.KEY_PROPERTIES;

    /** Password property. */
    public static final String PASSWORD_PROPERTY =
        SecurityContext.PASSWORD_PROPERTY;

    /** Path property. */
    public static final String PATH_PROPERTY = SecurityContext.PATH_PROPERTY;

    /** Public properties. */
    public static final String PRIVATE_PROPERTIES = "private";

    /** Public properties. */
    public static final String PUBLIC_PROPERTIES = "public";

    /** Sign key property. */
    public static final String SIGN_KEY_PROPERTY = "sign.key";

    /** Sign property. */
    public static final String SIGN_PROPERTY = "sign";

    /** Verify key property. */
    public static final String VERIFY_KEY_PROPERTY = "verify.key";

    /** Verify property. */
    public static final String VERIFY_PROPERTY = "verify";

    /**  */

    private static final Logger _LOGGER = Logger.getInstance(Crypt.class);

    private final AtomicReference<CryptEngineWrapper> _engineWrapper =
        new AtomicReference<CryptEngineWrapper>();
    private final ByteArrayOutputStream _outputStream =
        new ByteArrayOutputStream();
    private final ProxyReader _reader = new ProxyReader();
    private final Streamer _streamer = Streamer.newInstance();
    private Streamer.Input _streamerInput;
    private Streamer.Output _streamerOutput;
    private final CharArrayWriter _writer = new CharArrayWriter();

    /**
     * A crypt operation result.
     */
    public static final class Result
    {
        /**
         * Constructs an instance.
         *
         * @param serializable A serializable.
         * @param verified True when verified.
         * @param exception An optional crypt exception.
         */
        Result(
                @Nullable final Serializable serializable,
                final boolean verified,
                @Nonnull final Optional<CryptException> exception)
        {
            _exception = exception.orElse(null);
            _serializable = serializable;
            _verified = verified;
        }

        /**
         * Gets the exception.
         *
         * @return The exception.
         */
        @Nonnull
        @CheckReturnValue
        public CryptException getException()
        {
            return Require.notNull(_exception);
        }

        /**
         * Gets the serializable.
         *
         * @return The serializable.
         */
        @Nullable
        @CheckReturnValue
        public Serializable getSerializable()
        {
            return _serializable;
        }

        /**
         * Asks if this represents a failure.
         *
         * @return True if this represents a failure.
         */
        @CheckReturnValue
        public boolean isFailure()
        {
            return _exception != null;
        }

        /**
         * Asks if this represents a success.
         *
         * @return True if this represents a success.
         */
        @CheckReturnValue
        public boolean isSuccess()
        {
            return _exception == null;
        }

        /**
         * Asks if verified.
         *
         * @return True if verified.
         */
        @CheckReturnValue
        public boolean isVerified()
        {
            return _verified;
        }

        private final CryptException _exception;
        private final Serializable _serializable;
        private final boolean _verified;
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
