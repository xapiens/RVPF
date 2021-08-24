/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ContainerServiceImpl.java 3935 2019-04-28 14:42:38Z SFB $
 */

package org.rvpf.container;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.service.ServiceActivator;
import org.rvpf.service.app.ServiceApp;
import org.rvpf.service.app.ServiceAppHolderImpl;

/**
 * Container service implementation.
 *
 * <p>This RVPF service is a container for other RVPF services.</p>
 */
public final class ContainerServiceImpl
    extends ServiceAppHolderImpl
{
    /**
     * Gets the service activator for a service name or alias.
     *
     * @param serviceKey The service name or alias.
     *
     * @return The service activator (empty if unknown).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<ServiceActivator> getServiceActivator(
            @Nonnull final String serviceKey)
    {
        final ContainerServiceAppImpl mainApp =
            (ContainerServiceAppImpl) getServiceApp();

        return mainApp.getServiceActivator(serviceKey);
    }

    /** {@inheritDoc}
     */
    @Override
    protected ServiceApp newServiceApp()
    {
        return new ContainerServiceAppImpl();
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
