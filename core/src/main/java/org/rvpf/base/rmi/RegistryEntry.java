/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RegistryEntry.java 4056 2019-06-04 15:21:02Z SFB $
 */

package org.rvpf.base.rmi;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.tool.Require;

/**
 * Registry entry.
 *
 * <h1>Notes</h1>
 *
 * <ul>
 *   <li>An entry is local if the binding is not specified.</li>
 *   <li>A local entry is private if the local registry is private.</li>
 *   <li>The service name must be specified either explicitly, or by a path in
 *     the binding, or by a default name.</li>
 *   <li>When supplied, a default prefix will be inserted before the name unless
 *     one is already present.</li>
 * </ul>
 */
public final class RegistryEntry
{
    /**
     * Constructs an instance.
     *
     * @param uri The entry URI.
     * @param local True if local.
     */
    RegistryEntry(@Nonnull final URI uri, final boolean local)
    {
        if ("/".equals(uri.getPath())) {
            throw new IllegalArgumentException(
                Message.format(BaseMessages.MISSING_SERVER_IDENT));
        }

        _uri = uri;
        _local = local;
        _remote = !Inet.isOnLocalHost(_uri);
    }

    /**
     * Returns a new builder.
     *
     * @return The new builder.
     */
    @Nonnull
    @CheckReturnValue
    public static Builder newBuilder()
    {
        return new Builder();
    }

    /**
     * Sets the registry connection informations.
     *
     * @param registry The registry.
     * @param registryIsPrivate True if the registry is private.
     */
    public static void setRegistry(
            final Registry registry,
            final boolean registryIsPrivate)
    {
        _registry = registry;
        _registryIsPrivate = registryIsPrivate;
    }

    /**
     * Gets the lookup key.
     *
     * @return The lookup key.
     */
    @Nonnull
    @CheckReturnValue
    public String getLookupKey()
    {
        return (_local
                && (_registry != null))? _getPath(
                    _uri): _uri.getSchemeSpecificPart();
    }

    /**
     * Gets the name.
     *
     * @return The name.
     */
    @Nonnull
    @CheckReturnValue
    public String getName()
    {
        return _getName(_getPath(_uri));
    }

    /**
     * Gets the path.
     *
     * @return The path (always relative).
     */
    @Nonnull
    @CheckReturnValue
    public String getPath()
    {
        return _getPath(_uri);
    }

    /**
     * Gets the URI.
     *
     * @return The URI.
     */
    @Nonnull
    @CheckReturnValue
    public URI getURI()
    {
        return _uri;
    }

    /**
     * Asks if this entry is private.
     *
     * <p>An entry is private if it does not specify a registry location and if
     * the local registry is private.</p>
     *
     * @return True if this entry is private.
     */
    public boolean isPrivate()
    {
        return _local && _registryIsPrivate;
    }

    /**
     * Asks if remote.
     *
     * <p>An entry is remote if the registry is not located on the local
     * machine.</p>
     *
     * @return True if remote.
     */
    public boolean isRemote()
    {
        return _remote;
    }

    /**
     * Performs a lookup of the registry entry.
     *
     * @param sessionMode The session mode name.
     * @param logger The logger.
     *
     * @return The remote instance.
     *
     * @throws NotBoundException When the server name is not currently bound.
     * @throws RemoteException When the communication with the registry failed.
     * @throws AccessException When access is denied.
     */
    @Nonnull
    @CheckReturnValue
    public Remote lookup(
            @Nonnull String sessionMode,
            final Logger logger)
        throws AccessException, RemoteException, NotBoundException
    {
        final String lookupKey = getLookupKey();
        final Remote remote;

        if (sessionMode.length() > 0) {
            sessionMode = " (" + sessionMode + ")";
        }

        logger.debug(BaseMessages.SESSION_SERVER, sessionMode, lookupKey);

        if (_local && (_registry != null)) {
            remote = _registry.lookup(lookupKey);
        } else {
            try {
                remote = Naming.lookup(lookupKey);
            } catch (final MalformedURLException exception) {
                throw new InternalError(exception);
            }
        }

        return remote;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return isPrivate()? _getPath(_uri): _uri.toString();
    }

    static String _getName(final String path)
    {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    static String _getPath(@Nonnull final URI uri)
    {
        String path = uri.getPath();

        if (path == null) {
            path = "";
        } else if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }

    private static Registry _registry;
    private static boolean _registryIsPrivate;

    private final boolean _local;
    private final boolean _remote;
    private final URI _uri;

    /**
     * Builder.
     */
    public static final class Builder
    {
        /**
         * Constructs an instance.
         */
        Builder() {}

        /**
         * Builds a registry entry.
         *
         * @return The registry entry (null on failure).
         */
        @Nullable
        @CheckReturnValue
        public RegistryEntry build()
        {
            final boolean local;
            URI uri;

            if ((_defaultPrefix != null)
                    && !_defaultPrefix.isEmpty()
                    && !_defaultPrefix.endsWith("/")) {
                _defaultPrefix += "/";
            }

            if (_binding != null) {
                uri = _absoluteURI(_binding, null);

                if (uri == null) {
                    return null;
                }

                local = false;
            } else {
                uri = null;
                local = true;
            }

            if (uri == null) {
                if (_name == null) {
                    _name = _defaultName;
                }

                if (_name != null) {
                    uri = _absoluteURI(_name, _defaultPrefix);

                    if (uri == null) {
                        return null;
                    }
                } else {
                    _LOGGER.error(BaseMessages.MISSING_BINDING);

                    return null;
                }
            } else if (_name == null) {
                String path = uri.getPath();

                _name = _getName(path);

                if (_name.isEmpty()) {
                    if (_defaultName != null) {
                        if (_defaultPrefix != null) {
                            path = _defaultPrefix + _defaultName;
                        } else {
                            path = _defaultName;
                        }

                        uri = uri.resolve(path);
                    } else {
                        _LOGGER.error(BaseMessages.MISSING_NAME);

                        return null;
                    }
                }
            } else if ("/".equals(uri.getPath())) {
                String path = uri.getPath();

                if ((_defaultPrefix != null) && (_name.indexOf('/') < 0)) {
                    path += _defaultPrefix;
                }

                path += _name;

                try {
                    uri = uri.resolve(new URI(path));
                } catch (final URISyntaxException exception) {
                    _LOGGER.error(BaseMessages.BAD_ADDRESS, _name);

                    return null;
                }
            }

            if (!"rmi".equals(uri.getScheme())) {
                _LOGGER
                    .error(BaseMessages.SCHEME_NOT_SUPPORTED, uri.getScheme());

                return null;
            }

            return new RegistryEntry(uri, local);
        }

        /**
         * Clears this builder.
         *
         * @return This.
         */
        @Nonnull
        public Builder clear()
        {
            _binding = null;
            _defaultName = null;
            _defaultPrefix = null;
            _defaultRegistryAddress = null;
            _defaultRegistryPort = 0;

            return this;
        }

        /**
         * Copies the values from a registry entry.
         *
         * @param registryEntry The registry entry.
         *
         * @return This.
         */
        @Nonnull
        public Builder copyFrom(@Nonnull final RegistryEntry registryEntry)
        {
            setServerURI(registryEntry.getURI());

            return this;
        }

        /**
         * Sets the binding.
         *
         * @param binding The binding.
         *
         * @return This.
         */
        @Nonnull
        public Builder setBinding(@Nonnull final Optional<String> binding)
        {
            _binding = binding.orElse(null);

            return this;
        }

        /**
         * Sets the default name.
         *
         * @param defaultName The default name.
         *
         * @return This.
         */
        @Nonnull
        public Builder setDefaultName(@Nonnull final String defaultName)
        {
            _defaultName = Require.notNull(defaultName);

            return this;
        }

        /**
         * Sets the default prefix.
         *
         * @param defaultPrefix The default prefix.
         *
         * @return This.
         */
        @Nonnull
        public Builder setDefaultPrefix(@Nonnull final String defaultPrefix)
        {
            _defaultPrefix = Require.notNull(defaultPrefix);

            return this;
        }

        /**
         * Sets the default registry address.
         *
         * @param defaultRegistryAddress The default registry address.
         *
         * @return This.
         */
        @Nonnull
        public Builder setDefaultRegistryAddress(
                @Nonnull final Optional<InetAddress> defaultRegistryAddress)
        {
            _defaultRegistryAddress = defaultRegistryAddress.orElse(null);

            return this;
        }

        /**
         * Sets the default registry port.
         *
         * @param defaultRegistryPort The default registry port.
         *
         * @return This.
         */
        @Nonnull
        public Builder setDefaultRegistryPort(final int defaultRegistryPort)
        {
            _defaultRegistryPort = defaultRegistryPort;

            return this;
        }

        /**
         * Sets the name.
         *
         * @param name The name.
         *
         * @return This.
         */
        @Nonnull
        public Builder setName(@Nonnull final Optional<String> name)
        {
            _name = name.orElse(null);

            return this;
        }

        /**
         * Sets the server connection specification.
         *
         * @param server The server connection specification.
         *
         * @return This.
         *
         * @throws URISyntaxException On bad connection specification.
         */
        @Nonnull
        public Builder setServer(
                @Nonnull final String server)
            throws URISyntaxException
        {
            return setServerURI(new URI(server));
        }

        /**
         * Sets the server URI.
         *
         * @param serverURI The server URI.
         *
         * @return this.
         */
        @Nonnull
        public Builder setServerURI(@Nonnull final URI serverURI)
        {
            if (serverURI.getRawAuthority() != null) {
                _binding = serverURI.toString();
                _name = null;
            } else {
                _binding = null;
                _name = _getPath(serverURI);
            }

            return this;
        }

        private URI _absoluteURI(String string, final String defaultPrefix)
        {
            URI uri;

            if ((defaultPrefix != null) && !string.contains("/")) {
                string = defaultPrefix + string;
            }

            try {
                uri = new URI(string);

                final String scheme = uri.getScheme();
                String authority = uri.getAuthority();

                if (authority == null) {
                    authority = (((_defaultRegistryAddress == null)
                            || _defaultRegistryAddress.isAnyLocalAddress())? Inet.LOCAL_HOST
                            : _defaultRegistryAddress
                                .getHostAddress()) + ((_defaultRegistryPort > 0)
                                ? (":" + String.valueOf(
                                        _defaultRegistryPort)): "");
                } else if ((uri.getPort() < 0) && (_defaultRegistryPort > 0)) {
                    authority += ":" + String.valueOf(_defaultRegistryPort);
                }

                String path = uri.getPath();

                if (path == null) {
                    path = "/";
                } else if (!path.startsWith("/")) {
                    path = "/" + path;
                }

                uri = new URI(
                    (scheme != null)? scheme: "rmi",
                    authority,
                    path,
                    null,
                    null);
            } catch (final URISyntaxException exception) {
                _LOGGER.error(BaseMessages.BAD_ADDRESS, string);

                uri = null;
            }

            return uri;
        }

        private static final Logger _LOGGER = Logger.getInstance(Builder.class);

        private String _binding;
        private String _defaultName;
        private String _defaultPrefix;
        private InetAddress _defaultRegistryAddress;
        private int _defaultRegistryPort;
        private String _name;
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
