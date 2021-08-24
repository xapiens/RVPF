/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RemoteExecutorEngine.java 4056 2019-06-04 15:21:02Z SFB $
 */

package org.rvpf.processor.engine.executor.remote;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.engine.AbstractEngine;
import org.rvpf.service.rmi.ServiceRegistry;

/**
 * Remote engine.
 */
public class RemoteExecutorEngine
    extends AbstractEngine
{
    /** {@inheritDoc}
     */
    @Override
    public Transform createTransform(final TransformEntity proxyEntity)
    {
        final RemoteExecutorTransform transform = new RemoteExecutorTransform(
            _sessionProxy);

        if (!transform.setUp(getMetadata(), proxyEntity)) {
            return null;
        }

        return transform;
    }

    /**
     * Gets the session proxy.
     *
     * @return The session proxy.
     */
    @Nonnull
    @CheckReturnValue
    public RemoteEngineProxy getSessionProxy()
    {
        return Require.notNull(_sessionProxy);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final Optional<String> name = getParams().getString(NAME_PARAM);
        final RegistryEntry registryEntry = RegistryEntry
            .newBuilder()
            .setBinding(getParams().getString(BINDING_PARAM))
            .setName(name.isPresent()? name: proxyEntity.getName())
            .setDefaultName(RemoteExecutorAppImpl.DEFAULT_NAME)
            .setDefaultRegistryAddress(ServiceRegistry.getRegistryAddress())
            .setDefaultRegistryPort(ServiceRegistry.getRegistryPort())
            .build();

        if (registryEntry == null) {
            return false;
        }

        final Optional<String> securityParam = getParams()
            .getString(SECURITY_PARAM);
        final KeyedGroups securityProperties = securityParam
            .isPresent()? metadata
                .getPropertiesGroup(
                    securityParam.get()): KeyedGroups.MISSING_KEYED_GROUP;

        _sessionProxy = (RemoteEngineProxy) RemoteEngineProxy
            .newBuilder()
            .setRegistryEntry(registryEntry)
            .setConfigProperties(metadata.getProperties())
            .setSecurityProperties(securityProperties)
            .setLoginUser(getParams().getString(USER_PARAM))
            .setLoginPassword(getParams().getPassword(PASSWORD_PARAM))
            .setAutoconnect(true)
            .setClientName(metadata.getServiceName())
            .setClientLogger(getThisLogger())
            .build();

        if (_sessionProxy == null) {
            getThisLogger().error(BaseMessages.CONNECTION_SET_UP_FAILED);

            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_sessionProxy != null) {
            _sessionProxy.disconnect();
            _sessionProxy = null;
        }

        super.tearDown();
    }

    private RemoteEngineProxy _sessionProxy;
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
