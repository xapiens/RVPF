/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Verifier.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.jnlp.loader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.security.CodeSigner;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathValidator;
import java.security.cert.PKIXParameters;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.TrustStoreConfig;

/**
 * Verifier: verifies the presence and validity of digital signatures.
 */
@ThreadSafe
final class Verifier
{
    /**
     * Constructs an instance.
     */
    private Verifier()
    {
        try {
            _validator = CertPathValidator
                .getInstance(CertPathValidator.getDefaultType());
        } catch (final NoSuchAlgorithmException exception) {
            throw new InternalError(exception);
        }

        final String defaultStoreType = KeyStore.getDefaultType();
        final String storeType = System
            .getProperty(_TRUST_STORE_TYPE_PROPERTY, defaultStoreType);
        final String storeProvider = System
            .getProperty(_TRUST_STORE_PROVIDER_PROPERTY);
        final String storeProperty = System.getProperty(_TRUST_STORE_PROPERTY);
        final File storeFile = (storeProperty != null)? new File(
            storeProperty): new File(
                System.getProperty(_JAVA_HOME_PROPERTY),
                _CERTS_PATH);

        if (storeType != defaultStoreType) {
            _LOGGER
                .info(
                    BaseMessages.STORE_TYPE,
                    TrustStoreConfig.KIND,
                    storeType);
        }

        if (storeProvider != null) {
            _LOGGER
                .info(
                    BaseMessages.STORE_PROVIDER,
                    TrustStoreConfig.KIND,
                    storeProvider);
        }

        _LOGGER
            .info(
                BaseMessages.STORE_PATH,
                TrustStoreConfig.KIND,
                storeFile.getAbsolutePath());

        try {
            final KeyStore store = (storeProvider != null)? KeyStore
                .getInstance(
                    storeType,
                    storeProvider): KeyStore.getInstance(storeType);

            try (final InputStream stream = new FileInputStream(storeFile)) {
                store.load(stream, null);
            }

            _validatorParams = new PKIXParameters(store);
            _validatorParams.setRevocationEnabled(storeProperty == null);
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton instance.
     */
    @Nonnull
    @CheckReturnValue
    static synchronized Verifier getInstance()
    {
        if (_instance == null) {
            _instance = new Verifier();
        }

        return _instance;
    }

    /**
     * Verifies a cached JNLP file.
     *
     * @param jnlpFile The JNLP file.
     * @param mainJar The main Jar.
     *
     * @return True on success.
     *
     * @throws IOException When appropriate.
     */
    @CheckReturnValue
    static boolean verifyJNLP(
            @Nonnull final File jnlpFile,
            @Nonnull final JarFile mainJar)
        throws IOException
    {
        final JarEntry jarEntry = mainJar.getJarEntry(_JNLP_ENTRY);

        // Fails if the reference JNLP file is absent.
        if (jarEntry == null) {
            return false;
        }

        final long size = jarEntry.getSize();

        // Fails if the files are not the same size.
        if (size != jnlpFile.length()) {
            return false;
        }

        final byte[] jarBuffer = new byte[(int) size];

        // Reads the reference JNLP file into a byte buffer.
        try (final InputStream jarInputStream = new BufferedInputStream(
                mainJar.getInputStream(jarEntry))) {
            if (jarInputStream.read(jarBuffer) != size) {
                return false;
            }
        }

        final byte[] fileBuffer = new byte[(int) size];

        // Reads the JNLP file into a byte buffer.
        try (final InputStream fileInputStream = new BufferedInputStream(
                new FileInputStream(jnlpFile))) {
            if (fileInputStream.read(fileBuffer) != size) {
                return false;
            }
        }

        return Arrays.equals(fileBuffer, jarBuffer);
    }

    /**
     * Verifies a cached Jar file and optionally extracts content.
     *
     * @param cachedFile The cached file.
     * @param isNew True if the cached file is new.
     * @param extractDir The optional content target directory.
     *
     * @return True on success.
     *
     * @throws IOException When appropriate.
     */
    @CheckReturnValue
    boolean verifyJar(
            @Nonnull final File cachedFile,
            final boolean isNew,
            @Nonnull final Optional<File> extractDir)
        throws IOException
    {
        final JarFile jarFile = new JarFile(cachedFile);
        final Enumeration<JarEntry> entries = jarFile.entries();
        CodeSigner[] signers = null;

        // Makes sure entries are signed.
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();

            // Ignores META-INF contents, directories and empty entries.
            if (entry.getName().startsWith("META-INF/")
                    || entry.getName().endsWith("/")
                    || (entry.getSize() == 0)) {
                continue;
            }

            // Streams the entry to verify signatures.
            _streamEntry(jarFile, entry, isNew, extractDir);

            final CodeSigner[] entrySigners = entry.getCodeSigners();

            // Makes sure entry is signed.
            if ((entrySigners == null) || (entrySigners.length == 0)) {
                _LOGGER.error(JNLPMessages.ENTRY_NOT_SIGNED);

                return false;
            }

            // Makes sure all entries have the same signatures.
            if (signers == null) {
                signers = entrySigners;
            } else {
                if (!Arrays.equals(entrySigners, signers)) {
                    _LOGGER.error(JNLPMessages.CERTIFICATES_DIFFER);

                    return false;
                }
            }
        }

        // Makes sure signed entries are present.
        for (final Map.Entry<String, Attributes> entry:
                jarFile.getManifest().getEntries().entrySet()) {
            for (final Object key: entry.getValue().keySet()) {
                final String name = key.toString().toUpperCase(Locale.ROOT);

                if (name.endsWith("-DIGEST") || name.contains("-DIGEST-")) {
                    if (jarFile.getEntry(entry.getKey()) == null) {
                        _LOGGER.error(JNLPMessages.SIGNED_ENTRY_MISSING);

                        return false;
                    }

                    break;
                }
            }
        }

        // Validates signers.
        if (signers != null) {
            synchronized (this) {
                for (final CodeSigner signer: signers) {
                    if (!_validatedSigners.contains(signer)) {
                        try {
                            _validator
                                .validate(
                                    signer.getSignerCertPath(),
                                    _validatorParams);
                        } catch (final GeneralSecurityException exception) {
                            _LOGGER
                                .error(
                                    exception,
                                    JNLPMessages.SIGNATURE_VALIDATION);

                            return false;
                        }

                        _validatedSigners.add(signer);
                    }
                }
            }
        }

        return true;
    }

    private static void _streamEntry(
            final JarFile jarFile,
            final JarEntry entry,
            final boolean isNew,
            final Optional<File> extractDir)
        throws IOException
    {
        OutputStream outputStream = null;

        try (final InputStream inputStream = jarFile.getInputStream(entry)) {
            if ((extractDir.isPresent())
                    && (entry.getName().indexOf('/') < 0)) {
                final File file = new File(extractDir.get(), entry.getName());

                if (isNew || !file.exists()) {
                    outputStream = new FileOutputStream(file);
                }
            }

            final byte[] buffer = new byte[_STREAM_BUFFER_SIZE];

            for (;;) {
                final int count = inputStream.read(buffer, 0, buffer.length);

                if (count <= 0) {
                    break;
                }

                if (outputStream != null) {
                    outputStream.write(buffer, 0, count);
                }
            }
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    private static final String _CERTS_PATH = "lib/security/cacerts";
    private static final String _JAVA_HOME_PROPERTY = "java.home";
    private static final String _JNLP_ENTRY = "JNLP-INF/APPLICATION.JNLP";
    private static final Logger _LOGGER = Logger.getInstance(Verifier.class);
    private static final int _STREAM_BUFFER_SIZE = 32768;
    private static final String _TRUST_STORE_PROPERTY =
        "javax.net.ssl.trustStore";
    private static final String _TRUST_STORE_PROVIDER_PROPERTY =
        "javax.net.ssl.trustStoreProvider";
    private static final String _TRUST_STORE_TYPE_PROPERTY =
        "javax.net.ssl.trustStoreType";
    @GuardedBy("class")
    private static Verifier _instance;

    @GuardedBy("this")
    private final Set<CodeSigner> _validatedSigners = new HashSet<CodeSigner>();
    @GuardedBy("this")
    private final CertPathValidator _validator;
    @GuardedBy("this")
    private final PKIXParameters _validatorParams;
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
