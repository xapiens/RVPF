/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CryptTests.java 4099 2019-06-26 21:33:35Z SFB $
 */

package org.rvpf.tests.base.security;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.security.ClientSecurityContext;
import org.rvpf.base.security.Crypt;
import org.rvpf.base.security.ServerSecurityContext;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.Complex;
import org.rvpf.base.value.Rational;
import org.rvpf.base.value.Tuple;
import org.rvpf.tests.core.CoreTestsMessages;
import org.rvpf.tests.service.ServiceTests;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Crypt tests.
 */
public final class CryptTests
    extends ServiceTests
{
    private CryptTests() {}

    /**
     * Sets up this.
     *
     * @throws Exception On failure.
     */
    @BeforeClass
    public void setUp()
        throws Exception
    {
        final ClientSecurityContext clientSecurityContext =
            new ClientSecurityContext(
                getThisLogger());
        final KeyedGroups clientSecurityProperties = getConfig()
            .getPropertiesGroup(CLIENT_SECURITY_PROPERTIES);
        boolean success;

        success = clientSecurityContext
            .setUp(getConfig().getProperties(), clientSecurityProperties);
        Require.success(success);
        success = _clientCrypt
            .setUp(
                clientSecurityContext.getCryptProperties(),
                Optional.empty());
        Require.success(success);
        _clientPrivateKey1 = getConfig()
            .getStringValue(_CLIENT_PRIVATE_KEY_1_PROPERTY)
            .get();
        _clientPrivateKey2 = getConfig()
            .getStringValue(_CLIENT_PRIVATE_KEY_2_PROPERTY)
            .get();

        final ServerSecurityContext serverSecurityContext =
            new ServerSecurityContext(
                getThisLogger());
        final KeyedGroups serverSecurityProperties = getConfig()
            .getPropertiesGroup(SERVER_SECURITY_PROPERTIES);

        success = serverSecurityContext
            .setUp(getConfig().getProperties(), serverSecurityProperties);
        Require.success(success);
        success = _serverCrypt
            .setUp(
                serverSecurityContext.getCryptProperties(),
                Optional.empty());
        Require.success(success);
        _serverPrivateKey1 = getConfig()
            .getStringValue(_SERVER_PRIVATE_KEY_1_PROPERTY)
            .get();
        _serverPrivateKey2 = getConfig()
            .getStringValue(_SERVER_PRIVATE_KEY_2_PROPERTY)
            .get();

        final boolean secure = _serverCrypt.isSecure()
                && _clientCrypt.isSecure();

        getThisLogger()
            .info(CoreTestsMessages.CRYPT_SECURE, Boolean.valueOf(secure));
    }

    /**
     * Tears down what has been set up.
     *
     * @throws Exception On failure.
     */
    @AfterClass(alwaysRun = true)
    public void tearDown()
        throws Exception
    {
        _clientCrypt.tearDown();
        _serverCrypt.tearDown();
    }

    /**
     * Tests encrypt.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testEncrypt()
        throws Exception
    {
        Crypt.Result cryptResult;
        Serializable testValue;

        testValue = null;

        cryptResult = _serverCrypt.encrypt(testValue, new String[0]);
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.notNull(cryptResult.getSerializable());
        Require.failure(cryptResult.isVerified());

        cryptResult = _clientCrypt
            .decrypt(cryptResult.getSerializable(), new String[0]);
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.failure(cryptResult.isVerified());
        Require.equal(cryptResult.getSerializable(), testValue);

        testValue = Rational.valueOf(1, 3);

        cryptResult = _serverCrypt
            .encrypt(testValue, new String[] {_clientPrivateKey1});
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.notNull(cryptResult.getSerializable());
        Require.failure(cryptResult.isVerified());

        cryptResult = _clientCrypt
            .decrypt(
                cryptResult.getSerializable(),
                new String[] {_clientPrivateKey1});
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.failure(cryptResult.isVerified());
        Require.equal(cryptResult.getSerializable(), testValue);
    }

    /**
     * Tests encrypt and sign.
     *
     * @throws Exception On failure.
     */
    @Test(dependsOnMethods =
    {
        "testEncrypt", "testSign"
    })
    public void testEncryptAndSign()
        throws Exception
    {
        Crypt.Result cryptResult;
        Serializable testValue;
        Serializable encrypted;

        testValue = null;

        cryptResult = _serverCrypt
            .encryptAndSign(testValue, new String[0], new String[0]);
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.notNull(cryptResult.getSerializable());
        Require.failure(cryptResult.isVerified());

        encrypted = cryptResult.getSerializable();

        if (_clientCrypt.isSecure()) {
            cryptResult = _clientCrypt
                .verify(encrypted, new String[] {_serverPrivateKey2});
            Require.notNull(cryptResult);
            Require.success(cryptResult.isSuccess());
            Require.failure(cryptResult.isVerified());
        }

        cryptResult = _clientCrypt
            .verify(encrypted, new String[] {_serverPrivateKey1});
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.success(cryptResult.isVerified());

        cryptResult = _clientCrypt
            .verifyAndDecrypt(
                encrypted,
                new String[] {_serverPrivateKey1},
                new String[0]);
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.success(cryptResult.isVerified());
        Require.equal(cryptResult.getSerializable(), testValue);

        testValue = new Tuple(
            new Serializable[] {Rational.valueOf(
                1,
                3), Complex.cartesian(1.0, 3.0), });

        cryptResult = _serverCrypt
            .encryptAndSign(
                testValue,
                new String[0],
                new String[] {_serverPrivateKey2});
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.notNull(cryptResult.getSerializable());
        Require.failure(cryptResult.isVerified());

        encrypted = cryptResult.getSerializable();

        cryptResult = _clientCrypt.verify(encrypted, new String[0]);
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.success(cryptResult.isVerified());

        cryptResult = _clientCrypt
            .verifyAndDecrypt(encrypted, new String[0], new String[0]);
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.success(cryptResult.isVerified());
        Require.equal(cryptResult.getSerializable(), testValue);
    }

    /**
     * Tests sign.
     *
     * @throws Exception On failure.
     */
    @Test
    public void testSign()
        throws Exception
    {
        Crypt.Result cryptResult;
        Serializable testValue;

        testValue = null;

        cryptResult = _clientCrypt.sign(testValue, new String[0]);
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.notNull(cryptResult.getSerializable());
        Require.failure(cryptResult.isVerified());

        cryptResult = _serverCrypt
            .verify(cryptResult.getSerializable(), new String[0]);
        Require.success(cryptResult.isSuccess());
        Require.equal(cryptResult.getSerializable(), testValue);
        Require.success(cryptResult.isVerified());

        testValue = Complex.cartesian(1.0, 3.0);

        cryptResult = _clientCrypt
            .sign(
                testValue,
                new String[] {_clientPrivateKey1, _clientPrivateKey2});
        Require.notNull(cryptResult);
        Require.success(cryptResult.isSuccess());
        Require.notNull(cryptResult.getSerializable());
        Require.failure(cryptResult.isVerified());

        cryptResult = _serverCrypt
            .verify(cryptResult.getSerializable(), new String[0]);
        Require.success(cryptResult.isSuccess());
        Require.equal(cryptResult.getSerializable(), testValue);
        Require.success(cryptResult.isVerified());
    }

    private static final String _CLIENT_PRIVATE_KEY_1_PROPERTY =
        "tests.client.private.key.1";
    private static final String _CLIENT_PRIVATE_KEY_2_PROPERTY =
        "tests.client.private.key.2";
    private static final String _SERVER_PRIVATE_KEY_1_PROPERTY =
        "tests.server.private.key.1";
    private static final String _SERVER_PRIVATE_KEY_2_PROPERTY =
        "tests.server.private.key.2";

    private final Crypt _clientCrypt = new Crypt();
    private String _clientPrivateKey1;
    private String _clientPrivateKey2;
    private final Crypt _serverCrypt = new Crypt();
    private String _serverPrivateKey1;
    private String _serverPrivateKey2;
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
