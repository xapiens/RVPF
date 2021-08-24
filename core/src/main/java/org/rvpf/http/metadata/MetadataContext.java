/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MetadataContext.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http.metadata;

import java.io.Serializable;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.Crypt;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.http.HTTPMessages;
import org.rvpf.http.HTTPModule;
import org.rvpf.metadata.Metadata;

/**
 * Metadata context.
 */
final class MetadataContext
    extends HTTPModule.Context
{
    /**
     * Gets the metadata.
     *
     * @return The optional metadata.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Metadata> getMetadata()
    {
        return Optional.ofNullable(_metadata);
    }

    /**
     * Locks the metadata.
     */
    void lockMetadata()
    {
        _lock.readLock().lock();
    }

    /**
     * Prepares the XML document for sending.
     *
     * @param metadataDocument The metadata document.
     *
     * @return The prepared document.
     *
     * @throws Exception On failure.
     */
    XMLDocument prepareXML(final XMLDocument metadataDocument)
        throws Exception
    {
        if (_crypt == null) {
            return metadataDocument;
        }

        Serializable serializable = metadataDocument.toString();

        if (_encrypt) {
            final Crypt.Result encryptResult = _crypt
                .encrypt(serializable, _encryptionKeyIdents);

            if (encryptResult.isSuccess()) {
                serializable = encryptResult.getSerializable();
            } else {
                throw encryptResult.getException();
            }
        }

        if (_sign) {
            final Crypt.Result signResult = _crypt
                .sign(serializable, _signingKeyIdents);

            if (signResult.isSuccess()) {
                serializable = signResult.getSerializable();
            } else {
                throw signResult.getException();
            }
        }

        return new XMLDocument(_crypt.getStreamer().toXML(serializable));
    }

    /**
     * Sets the metadata
     *
     * @param metadata The metadata.
     *
     * @throws InterruptedException When the service si stopped.
     */
    void setMetadata(
            @Nonnull final Metadata metadata)
        throws InterruptedException
    {
        _lock.writeLock().lockInterruptibly();
        _metadata = metadata;
        _lock.writeLock().unlock();
    }

    /**
     * Sets up this.
     *
     * @param configProperties The config properties.
     * @param metadataProperties The metadata configuration properties.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean setUp(
            @Nonnull final KeyedGroups configProperties,
            @Nonnull final KeyedGroups metadataProperties)
    {
        _encrypt = metadataProperties.getBoolean(Crypt.ENCRYPT_PROPERTY);

        if (_encrypt) {
            _LOGGER.debug(HTTPMessages.WILL_ENCRYPT);
            _encryptionKeyIdents = metadataProperties
                .getStrings(Crypt.ENCRYPT_KEY_PROPERTY);

            for (final String keyIdent: _encryptionKeyIdents) {
                _LOGGER.debug(HTTPMessages.ENCRYPTION_KEY, keyIdent);
            }
        }

        _sign = metadataProperties.getBoolean(Crypt.SIGN_PROPERTY);

        if (_sign) {
            _LOGGER.debug(HTTPMessages.WILL_SIGN);
            _signingKeyIdents = metadataProperties
                .getStrings(Crypt.SIGN_KEY_PROPERTY);

            for (final String signingKeyIdent: _signingKeyIdents) {
                _LOGGER.debug(HTTPMessages.SIGNING_KEY, signingKeyIdent);
            }
        }

        if (_encrypt || _sign) {
            final SecurityContext securityContext = new SecurityContext(
                _LOGGER);
            final KeyedGroups securityProperties = metadataProperties
                .getGroup(SecurityContext.SECURITY_PROPERTIES);

            if (!securityContext.setUp(configProperties, securityProperties)) {
                return false;
            }

            _crypt = new Crypt();

            if (!_crypt
                .setUp(
                    securityContext.getCryptProperties(),
                    Optional.empty())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tears down what has been set up.
     */
    void tearDown()
    {
        if (_crypt != null) {
            _crypt.tearDown();
        }
    }

    /**
     * Unlocks the metadata.
     */
    void unlockMetadata()
    {
        _lock.readLock().unlock();
    }

    private static final Logger _LOGGER = Logger
        .getInstance(MetadataContext.class);

    private Crypt _crypt;
    private boolean _encrypt;
    private String[] _encryptionKeyIdents;
    private final ReentrantReadWriteLock _lock = new ReentrantReadWriteLock();
    private Metadata _metadata;
    private boolean _sign;
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
