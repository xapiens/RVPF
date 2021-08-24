/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Realm.java 4055 2019-06-04 13:05:05Z SFB $
 */

package org.rvpf.base.security;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.ResourceFileFactory;
import org.rvpf.base.util.UnicodeStreamReader;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * Realm.
 *
 * <p>The format of the configuration properties file is:</p>
 *
 * <PRE>
 *   identifier: password [,rolename . . .]
 * </PRE>
 */
public class Realm
{
    /**
     * Constructs an instance.
     */
    public Realm() {}

    /**
     * Authenticates an identity.
     *
     * @param identifier The identifier for the identity.
     * @param credential The credential.
     *
     * @return The identity (empty on failure to identify).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Identity> authenticate(
            @Nonnull final String identifier,
            @Nonnull final Object credential)
    {
        final Optional<Identity> identity = Optional
            .ofNullable(_identities.get(Require.notNull(identifier)));

        return (identity.isPresent()
                && identity.get().authenticate(
                    credential))? identity: Optional.empty();
    }

    /**
     * Gets the identity for an identifier.
     *
     * @param identifier The optional identifier.
     *
     * @return The identity (may be 'unknown').
     */
    @Nonnull
    @CheckReturnValue
    public Identity getIdentity(@Nonnull final Optional<String> identifier)
    {
        final Identity identity = identifier
            .isPresent()? _identities.get(identifier.get()): null;

        return (identity != null)? identity: UNKNOWN_IDENTITY;
    }

    /**
     * Asks if this realm is configured.
     *
     * @return True if configured.
     */
    @CheckReturnValue
    public boolean isConfigured()
    {
        return _configured;
    }

    /**
     * Asks if an identity acts as one of the specified roles.
     *
     * @param identifier The optional identifier for the identity.
     * @param roles The roles.
     *
     * @return True if the identity acts in one of the specified roles.
     */
    @CheckReturnValue
    public boolean isInRoles(
            @Nonnull final Optional<String> identifier,
            @Nonnull final String[] roles)
    {
        final Identity identity = identifier
            .isPresent()? _identities.get(identifier.get()): null;

        return (identity != null) && identity.isInRoles(roles);
    }

    /**
     * Sets up this.
     *
     * @param realmProperties The realm configuration properties.
     * @param securityContext The security context.
     *
     * @return True on success.
     */
    @CheckReturnValue
    public boolean setUp(
            @Nonnull final KeyedGroups realmProperties,
            @Nonnull final SecurityContext securityContext)
    {
        final boolean verify = realmProperties
            .getBoolean(Crypt.VERIFY_PROPERTY);
        final boolean decrypt = realmProperties
            .getBoolean(Crypt.DECRYPT_PROPERTY);
        String realmPath = realmProperties
            .getString(SecurityContext.PATH_PROPERTY)
            .orElse(null);

        if ((realmPath == null) || realmPath.trim().isEmpty()) {
            getThisLogger()
                .error(
                    BaseMessages.MISSING_PROPERTY_IN,
                    SecurityContext.PATH_PROPERTY,
                    SecurityContext.REALM_PROPERTIES);

            return false;
        }

        if (verify || decrypt) {
            if (!realmPath.endsWith(_XML_EXT)) {
                realmPath = realmPath + _XML_EXT;
            }
        }

        final File realmFile = ResourceFileFactory.newResourceFile(realmPath);

        if ((realmFile == null) || !realmFile.isFile()) {
            getThisLogger().error(BaseMessages.RESOURCE_NOT_FOUND, realmPath);

            return false;
        }

        getThisLogger().info(BaseMessages.REALM_RESOURCE, realmFile);

        final Properties loadedProperties = new Properties();
        final Reader realmReader;

        if (verify || decrypt) {
            final Crypt crypt = new Crypt();

            if (!crypt
                .setUp(
                    securityContext.getCryptProperties(),
                    Optional.empty())) {
                return false;
            }

            final Serializable serializable = crypt
                .load(
                    realmFile,
                    verify,
                    realmProperties.getStrings(Crypt.VERIFY_KEY_PROPERTY),
                    decrypt,
                    realmProperties.getStrings(Crypt.DECRYPT_KEY_PROPERTY));

            if (serializable == null) {
                return false;
            }

            realmReader = new StringReader(serializable.toString());
        } else {
            try {
                realmReader = new UnicodeStreamReader(realmFile);
            } catch (final FileNotFoundException exception1) {
                getThisLogger()
                    .error(
                        BaseMessages.FILE_NOT_FOUND,
                        realmFile.getAbsolutePath());

                return false;
            }
        }

        try {
            loadedProperties.load(realmReader);
        } catch (final IOException exception) {
            getThisLogger()
                .error(
                    BaseMessages.FILE_READ_FAILED,
                    realmFile.getAbsolutePath(),
                    exception);

            return false;
        }

        for (final Map.Entry<?, ?> entry: loadedProperties.entrySet()) {
            final String identifier = entry.getKey().toString().trim();
            final String value = entry.getValue().toString().trim();
            final int comma = value.indexOf(',');
            final Set<String> roles = new HashSet<>();
            final char[] password;
            final String names;

            if (comma >= 0) {
                password = value.substring(0, comma).trim().toCharArray();
                names = value.substring(comma + 1).trim();
            } else {
                password = value.toCharArray();
                names = "";
            }

            if (names.length() > 0) {
                roles.addAll(Arrays.asList(_ROLES_SPLIT_PATTERN.split(names)));
            }

            _identities
                .put(
                    identifier,
                    newIdentity(identifier, new Credential(password), roles));
        }

        _configured = true;

        return true;
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return Logger.getInstance(getClass());
    }

    /**
     * Returns a new identity.
     *
     * <p>Allows subclasses to extend {@link Identity}.</p>
     *
     * @param identifier The identifier.
     * @param credential The credential.
     * @param roles The roles.
     *
     * @return The new identity.
     */
    @Nonnull
    @CheckReturnValue
    protected Identity newIdentity(
            @Nonnull final String identifier,
            @Nonnull final Credential credential,
            @Nonnull final Set<String> roles)
    {
        return new Identity(identifier, credential, roles);
    }

    /** CRYPT prefix. */
    public static final String CRYPT_PREFIX = "CRYPT:";

    /** Unknown identity. */
    public static final Identity UNKNOWN_IDENTITY = new Identity(
        "",
        new Credential(new char[0]),
        Collections.<String>emptySet());

    /**  */

    private static final Pattern _ROLES_SPLIT_PATTERN = Pattern
        .compile("\\s*,\\s*");
    private static final String _XML_EXT = ".xml";

    private boolean _configured;
    private final Map<String, Identity> _identities = new HashMap<>();
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
