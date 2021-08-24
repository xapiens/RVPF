/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RegistryConfig.java 4059 2019-06-05 20:44:44Z SFB $
 */

package org.rvpf.base.rmi;

import java.net.InetSocketAddress;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Inet;
import org.rvpf.base.util.container.KeyedGroups;

/**
 * Registry configuration.
 */
public final class RegistryConfig
{
    /**
     * Constructs an instance.
     *
     * @param contextProperties The context properties.
     */
    public RegistryConfig(@Nonnull final KeyedGroups contextProperties)
    {
        _rmiRegistryProperties = contextProperties
            .getGroup(RMI_REGISTRY_PROPERTIES);
    }

    /**
     * Gets the registry port.
     *
     * @return The registry port (negative on error).
     */
    @CheckReturnValue
    public int getRegistryPort()
    {
        int registryPort = _rmiRegistryProperties.getInt(PORT_PROPERTY, 0);

        if (registryPort == 0) {
            final Optional<InetSocketAddress> registrySocketAddress =
                getRegistrySocketAddress();

            if (!registrySocketAddress.isPresent()) {
                return -1;
            }

            registryPort = registrySocketAddress.get().getPort();
        }

        return registryPort;
    }

    /**
     * Gets the registry socket address.
     *
     * @return The registry socket address (empty on failure).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<InetSocketAddress> getRegistrySocketAddress()
    {
        final String addressProperty = _rmiRegistryProperties
            .getString(ADDRESS_PROPERTY, Optional.of(DEFAULT_ADDRESS))
            .get();
        Optional<InetSocketAddress> registryAddress;

        registryAddress = Inet.socketAddress(addressProperty);

        if (!registryAddress.isPresent()) {
            Logger
                .getInstance(RegistryConfig.class)
                .warn(BaseMessages.BAD_ADDRESS, addressProperty);
        } else if (registryAddress.get().getPort() != 0) {
            final int registryPort = _rmiRegistryProperties
                .getInt(PORT_PROPERTY, 0);

            if ((registryPort != 0)
                    && (registryAddress.get().getPort() != registryPort)) {
                Logger
                    .getInstance(RegistryEntry.class)
                    .error(
                        BaseMessages.RMI_REGISTRY_CONFLICT,
                        String.valueOf(registryAddress.get().getPort()),
                        String.valueOf(registryPort));
                registryAddress = Optional.empty();
            }
        } else {
            registryAddress = Optional
                .of(
                    new InetSocketAddress(
                        registryAddress.get().getAddress(),
                        _rmiRegistryProperties.getInt(PORT_PROPERTY, 0)));
        }

        return registryAddress;
    }

    /**
     * Asks if the registry should be private.
     *
     * @return True for a private registry.
     */
    @CheckReturnValue
    public boolean isRegistryPrivate()
    {
        return _rmiRegistryProperties.getBoolean(PRIVATE_PROPERTY);
    }

    /**
     * Asks if the registry should be read-only.
     *
     * @return True for a read-only registry.
     */
    @CheckReturnValue
    public boolean isRegistryReadOnly()
    {
        return _rmiRegistryProperties.getBoolean(PROTECTED_PROPERTY);
    }

    /**
     * Asks if the registry may be shared.
     *
     * @return True for a shareable registry.
     */
    @CheckReturnValue
    public boolean isRegistryShared()
    {
        return _rmiRegistryProperties.getBoolean(SHARED_PROPERTY);
    }

    /** Address property. */
    public static final String ADDRESS_PROPERTY = "address";

    /** Default address. */
    public static final String DEFAULT_ADDRESS = Inet.LOCAL_HOST;

    /** Port property. */
    public static final String PORT_PROPERTY = "port";

    /** Private property. */
    public static final String PRIVATE_PROPERTY = "private";

    /** Protected property. */
    public static final String PROTECTED_PROPERTY = "protected";

    /** RMI registry properties. */
    public static final String RMI_REGISTRY_PROPERTIES = "rmi.registry";

    /** Shared property. */
    public static final String SHARED_PROPERTY = "shared";

    private final KeyedGroups _rmiRegistryProperties;
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
