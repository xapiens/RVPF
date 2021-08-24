/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SecurityContext.java 4067 2019-06-08 13:39:16Z SFB $
 */

package org.rvpf.base.security;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * Security context.
 *
 * <p>This is a helper class for the initialization of an SSL context. It is not
 * intended to be generic; it tends to the specific needs of RVPF services.</p>
 */
@ThreadSafe
public class SecurityContext
{
    /**
     * Constructs an instance.
     *
     * @param logger The logger instance to use.
     */
    public SecurityContext(@Nonnull final Logger logger)
    {
        _logger = logger;

        _keyStoreConfig = new KeyStoreConfig(logger);
        _trustStoreConfig = new TrustStoreConfig(logger);
    }

    /**
     * Checks if configured for secure operation.
     *
     * @throws SSLException When the check fails.
     */
    public final void checkForSecureOperation()
        throws SSLException
    {
        useDefaults();

        if (!_keyStoreConfig.getPath().isPresent()
                && (isServer()
                    || !getTrustStoreConfig().getPath().isPresent())) {
            throw new SSLException(BaseMessages.NO_SECURE.toString());
        }

        if (isCertified() && (_keyStoreConfig.getPassword() == null)) {
            throw new SSLException(BaseMessages.NO_CERTIFIED.toString());
        }
    }

    /**
     * Creates an SSL context.
     *
     * <p>This should be called only after all the appropriate attributes have
     * been specified thru their setter.</p>
     *
     * <p>This implementation will try to supply defaults for the missing values
     * from the standard JSSE 'javax.net' properties.</p>
     *
     * @return The SSL context.
     *
     * @throws SSLException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    public final SSLContext createSSLContext()
        throws SSLException
    {
        final SSLContext sslContext;

        useDefaults();

        try {
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            final Optional<String> keyStorePath = _keyStoreConfig.getPath();
            final Optional<char[]> keyStorePassword = _keyStoreConfig
                .getPassword();
            final KeyStore keyStore;
            Optional<char[]> keyPassword = _keyStoreConfig.getKeyPassword();

            if (!keyPassword.isPresent()) {
                keyPassword = keyStorePassword;
            }

            if (keyStorePath.isPresent()) {
                if (keyStorePassword == null) {
                    throw new SSLException(
                        BaseMessages.NO_KEYSTORE_PASSWORD.toString());
                }

                keyStore = _loadStore(_keyStoreConfig);
            } else {
                keyStore = null;
            }

            keyManagerFactory.init(keyStore, keyPassword.orElse(null));

            final TrustManagerFactory trustManagerFactory = TrustManagerFactory
                .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            final Optional<String> trustStorePath = _trustStoreConfig.getPath();
            final KeyStore trustStore;

            if (trustStorePath.isPresent()) {
                trustStore = _loadStore(_trustStoreConfig);
            } else {
                trustStore = keyStore;
            }

            trustManagerFactory.init(trustStore);

            sslContext = SSLContext.getInstance("TLS");
            sslContext
                .init(
                    keyManagerFactory.getKeyManagers(),
                    trustManagerFactory.getTrustManagers(),
                    null);
        } catch (final SSLException exception) {
            throw exception;
        } catch (final Exception exception) {
            final SSLException ssle = new SSLException(
                BaseMessages.SSL_INITIALIZE_FAILED.toString());

            ssle.initCause(exception);

            throw ssle;
        }

        return sslContext;
    }

    /**
     * Gets the crypt properties.
     *
     * @return The crypt properties.
     */
    @Nonnull
    @CheckReturnValue
    public KeyedGroups getCryptProperties()
    {
        final KeyedGroups cryptProperties = _cryptProperties;

        return (cryptProperties != null)
               ? cryptProperties: KeyedGroups.MISSING_KEYED_GROUP;
    }

    /**
     * Gets the KeyStore config.
     *
     * @return The KeyStore config.
     */
    @Nonnull
    @CheckReturnValue
    public KeyStoreConfig getKeyStoreConfig()
    {
        return _keyStoreConfig;
    }

    /**
     * Gets the logger instance.
     *
     * @return The logger instance.
     */
    @Nonnull
    @CheckReturnValue
    public final Logger getLogger()
    {
        return _logger;
    }

    /**
     * Gets the realm properties.
     *
     * @return The realm properties.
     */
    @Nonnull
    @CheckReturnValue
    public KeyedGroups getRealmProperties()
    {
        final KeyedGroups realmProperties = _realmProperties;

        return (realmProperties != null)
               ? realmProperties: KeyedGroups.MISSING_KEYED_GROUP;
    }

    /**
     * Gets the TrustStore config.
     *
     * @return The TrustStore config.
     */
    @Nonnull
    @CheckReturnValue
    public TrustStoreConfig getTrustStoreConfig()
    {
        return _trustStoreConfig;
    }

    /**
     * Gets the certified indicator.
     *
     * @return Returns the certified indicator.
     */
    @CheckReturnValue
    public final boolean isCertified()
    {
        return _certified;
    }

    /**
     * Gets the secure indicator.
     *
     * @return Returns the secure indicator.
     */
    @CheckReturnValue
    public final boolean isSecure()
    {
        return _secure;
    }

    /**
     * Asks if this is a server context.
     *
     * @return True when it is a server context.
     */
    @CheckReturnValue
    public boolean isServer()
    {
        return false;
    }

    /**
     * Sets the certified indicator.
     *
     * @param certified The certified indicator.
     */
    public final void setCertified(final boolean certified)
    {
        if (certified && isServer()) {
            _logger.debug(BaseMessages.CERTIFIED);
        }

        _certified = certified;
    }

    /**
     * Sets the crypt properties.
     *
     * @param cryptProperties The crypt properties.
     */
    public void setCryptProperties(@Nonnull final KeyedGroups cryptProperties)
    {
        _cryptProperties = Require.notNull(cryptProperties);
    }

    /**
     * Sets the realm properties.
     *
     * @param realmProperties The realm properties.
     */
    public void setRealmProperties(@Nonnull final KeyedGroups realmProperties)
    {
        _realmProperties = Require.notNull(realmProperties);
    }

    /**
     * Sets the secure indicator.
     *
     * @param secure The secure indicator.
     */
    public final void setSecure(final boolean secure)
    {
        if (secure && isServer()) {
            _logger.debug(BaseMessages.SECURE);
        }

        _secure = secure;
    }

    /**
     * Sets up this.
     *
     * @param configProperties The configuration properties.
     * @param securityProperties Security properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public final boolean setUp(
            @Nonnull final KeyedGroups configProperties,
            @Nonnull KeyedGroups securityProperties)
    {
        if (securityProperties.isMissing()) {
            securityProperties = configProperties
                .getGroup(
                    configProperties
                        .getString(
                                SECURITY_PROPERTIES,
                                        Optional.of(SECURITY_PROPERTIES))
                        .get());
        }

        if (!securityProperties.isMissing()) {
            try {
                _setUpKeyStore(
                    securityProperties.getGroup(KEYSTORE_PROPERTIES));
                _setUpTrustStore(
                    securityProperties.getGroup(TRUSTSTORE_PROPERTIES));
                setSecure(securityProperties.getBoolean(SECURE_PROPERTY));
                setCertified(securityProperties.getBoolean(CERTIFIED_PROPERTY));

                setCryptProperties(
                    securityProperties.getGroup(CRYPT_PROPERTIES));

                setRealmProperties(
                    securityProperties.getGroup(REALM_PROPERTIES));
            } catch (final FileNotFoundException exception) {
                _logger.error(BaseMessages.VERBATIM, exception.getMessage());

                return false;
            }
        }

        return true;
    }

    /**
     * Uses default values from system properties.
     *
     * @throws SSLException When appropriate.
     */
    protected void useDefaults()
        throws SSLException
    {
        try {
            if (!_keyStoreConfig.getPath().isPresent()) {
                _keyStoreConfig
                    .setPath(_systemProperty("javax.net.ssl.keyStore"));
            }

            if (!_keyStoreConfig.getPassword().isPresent()) {
                final String password = System
                    .getProperty("javax.net.ssl.keyStorePassword", "");

                _keyStoreConfig
                    .setPassword(Optional.of(password.toCharArray()));
            }

            if (!_keyStoreConfig.getProvider().isPresent()) {
                _keyStoreConfig
                    .setProvider(
                        _systemProperty("javax.net.ssl.keyStoreProvider"));
            }

            if (!_keyStoreConfig.getType().isPresent()) {
                _keyStoreConfig
                    .setType(_systemProperty("javax.net.ssl.keyStoreType"));
            }

            if (!_trustStoreConfig.getPath().isPresent()) {
                _trustStoreConfig
                    .setPath(_systemProperty("javax.net.ssl.trustStore"));
            }

            if (!_trustStoreConfig.getPassword().isPresent()) {
                final String password = System
                    .getProperty("javax.net.ssl.trustStorePassword");

                if (password != null) {
                    _trustStoreConfig
                        .setPassword(Optional.of(password.toCharArray()));
                }
            }

            if (!_trustStoreConfig.getProvider().isPresent()) {
                _trustStoreConfig
                    .setProvider(
                        _systemProperty("javax.net.ssl.trustStoreProvider"));
            }

            if (!_trustStoreConfig.getType().isPresent()) {
                _trustStoreConfig
                    .setType(_systemProperty("javax.net.ssl.trustStoreType"));
            }
        } catch (final FileNotFoundException exception) {
            throw new SSLException(exception);
        }
    }

    private static void _setUpStore(
            final StoreConfig storeConfig,
            final KeyedGroups storeProperties)
        throws FileNotFoundException
    {
        storeConfig
            .setVerify(storeProperties.getBoolean(Crypt.VERIFY_PROPERTY));
        storeConfig
            .setVerifyKeyIdents(
                storeConfig.isVerify()? Optional
                    .of(
                            storeProperties
                                    .getStrings(
                                            Crypt.VERIFY_KEY_PROPERTY)): Optional
                                                    .empty());
        storeConfig
            .setDecrypt(storeProperties.getBoolean(Crypt.DECRYPT_PROPERTY));
        storeConfig
            .setDecryptKeyIdents(
                storeConfig.isDecrypt()? Optional
                    .of(
                            storeProperties
                                    .getStrings(
                                            Crypt.DECRYPT_KEY_PROPERTY)): Optional
                                                    .empty());

        storeConfig.setPath(storeProperties.getString(PATH_PROPERTY));
        storeConfig.setType(storeProperties.getString(TYPE_PROPERTY));
        storeConfig.setProvider(storeProperties.getString(PROVIDER_PROPERTY));
        storeConfig.setPassword(storeProperties.getPassword(PASSWORD_PROPERTY));
    }

    private static Optional<String> _systemProperty(final String propertyName)
    {
        return Optional.ofNullable(System.getProperty(propertyName));
    }

    private KeyStore _loadStore(
            final StoreConfig storeConfig)
        throws GeneralSecurityException, IOException
    {
        final InputStream storeStream;

        if (storeConfig.isVerify() || storeConfig.isDecrypt()) {
            final Crypt crypt = new Crypt();

            if (!crypt.setUp(getCryptProperties(), Optional.empty())) {
                throw new GeneralSecurityException();
            }

            final Serializable serializable = crypt
                .load(
                    new File(storeConfig.getPath().get()),
                    storeConfig.isVerify(),
                    storeConfig.getVerifyKeyIdents().orElse(null),
                    storeConfig.isDecrypt(),
                    storeConfig.getDecryptKeyIdents().orElse(null));

            if (!(serializable instanceof byte[])) {
                throw new GeneralSecurityException(
                    Message.format(
                        BaseMessages.NOTHING_RECOGNIZABLE,
                        storeConfig.getPath()));
            }

            storeStream = new ByteArrayInputStream((byte[]) serializable);
        } else {
            storeStream = new FileInputStream(storeConfig.getPath().get());
        }

        Optional<String> type = storeConfig.getType();

        if (!type.isPresent()) {
            type = Optional.of(KeyStore.getDefaultType());
        }

        final Optional<String> provider = storeConfig.getProvider();
        final KeyStore store = !provider
            .isPresent()? KeyStore
                .getInstance(
                    type.get()): KeyStore
                            .getInstance(type.get(), provider.get());

        store.load(storeStream, storeConfig.getPassword().orElse(null));
        storeStream.close();

        return store;
    }

    private void _setUpKeyStore(
            final KeyedGroups keystoreProperties)
        throws FileNotFoundException
    {
        _setUpStore(_keyStoreConfig, keystoreProperties);

        final KeyedGroups keyProperties = keystoreProperties
            .getGroup(KEY_PROPERTIES);

        _keyStoreConfig.setKeyIdent(keyProperties.getString(IDENT_PROPERTY));
        _keyStoreConfig
            .setKeyPassword(keyProperties.getPassword(PASSWORD_PROPERTY));
    }

    private void _setUpTrustStore(
            final KeyedGroups truststoreProperties)
        throws FileNotFoundException
    {
        _setUpStore(_trustStoreConfig, truststoreProperties);
    }

    /** Certified property. */
    public static final String CERTIFIED_PROPERTY = "certified";

    /** Crypt properties. */
    public static final String CRYPT_PROPERTIES = "crypt";

    /** Ident property. */
    public static final String IDENT_PROPERTY = "ident";

    /** Keystore properties. */
    public static final String KEYSTORE_PROPERTIES = "keystore";

    /** Key properties. */
    public static final String KEY_PROPERTIES = "key";

    /** Password property. */
    public static final String PASSWORD_PROPERTY = "password";

    /** Path property. */
    public static final String PATH_PROPERTY = "path";

    /** Provider property. */
    public static final String PROVIDER_PROPERTY = "provider";

    /** Realm properties. */
    public static final String REALM_PROPERTIES = "realm";

    /** Secure property. */
    public static final String SECURE_PROPERTY = "secure";

    /** Security properties. */
    public static final String SECURITY_PROPERTIES = "security";

    /** Truststore properties. */
    public static final String TRUSTSTORE_PROPERTIES = "truststore";

    /** Type property. */
    public static final String TYPE_PROPERTY = "type";

    private volatile boolean _certified;
    private volatile KeyedGroups _cryptProperties;
    private final KeyStoreConfig _keyStoreConfig;
    private final Logger _logger;
    private volatile KeyedGroups _realmProperties;
    private volatile boolean _secure;
    private final TrustStoreConfig _trustStoreConfig;
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
