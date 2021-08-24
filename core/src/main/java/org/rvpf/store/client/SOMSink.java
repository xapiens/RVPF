/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SOMSink.java 4101 2019-06-30 14:56:50Z SFB $
 */

package org.rvpf.store.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Params;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.som.QueueProxy;
import org.rvpf.base.som.SOMProxy;
import org.rvpf.base.som.SOMServer;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.service.ServiceMessages;

/**
 * SOM sink.
 *
 * <p>This class provides a client side access to a sink's SOM queue.</p>
 */
public final class SOMSink
    extends AbstractSink
{
    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
    {
        if (_sender != null) {
            _sender.disconnect();
            _sender = null;
        }

        super.close();
    }

    /** {@inheritDoc}
     */
    @Override
    public void connect()
        throws StoreAccessException
    {
        final QueueProxy.Sender sender = _sender;

        if (sender == null) {
            throw accessException(new ServiceClosedException());
        }

        try {
            sender.connect();
        } catch (final SessionException exception) {
            throw accessException(exception);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<Exception[]> getExceptions()
    {
        return Optional.of(new Exception[_updateExceptions]);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean probe()
        throws StoreAccessException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean sendUpdates(
            final Collection<PointValue> updates)
        throws StoreAccessException
    {
        _updateExceptions = updates.size();

        if (!updates.isEmpty()) {
            final PointValue[] updatesArray = updates
                .toArray(new PointValue[updates.size()]);

            if (getThisLogger().isTraceEnabled()) {
                Arrays
                    .stream(updatesArray)
                    .forEach(
                        update -> getThisLogger()
                            .trace(BaseMessages.SENDING_POINT_UPDATE, update));
            }

            try {
                _sender.send(updatesArray, true);
            } catch (final SessionException exception) {
                throw accessException(exception);
            }

            getThisLogger().trace(BaseMessages.POINT_UPDATES_SUCCEEDED);
            updates.clear();
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final Params params = getParams();
        final Optional<String> queuePropertiesName = params
            .getString(QUEUE_PARAM);
        final KeyedGroups queueProperties;

        if (queuePropertiesName.isPresent()) {
            queueProperties = metadata
                .getPropertiesGroup(queuePropertiesName.get());

            if (queueProperties.isMissing()) {
                getThisLogger()
                    .warn(
                        ServiceMessages.MISSING_PROPERTIES,
                        queuePropertiesName.get());

                return false;
            }
        } else {
            queueProperties = new KeyedGroups();

            _setPropertiesValue(
                queueProperties,
                SOMServer.BINDING_PROPERTY,
                params.getString(BINDING_PARAM));
            _setPropertiesValue(
                queueProperties,
                SOMServer.NAME_PROPERTY,
                params.getString(NAME_PARAM, proxyEntity.getName()));

            final Optional<String> securityParam = getParams()
                .getString(SECURITY_PARAM);

            if (securityParam.isPresent()) {
                queueProperties
                    .setGroup(
                        SecurityContext.SECURITY_PROPERTIES,
                        metadata.getPropertiesGroup(securityParam.get()));
            } else {
                queueProperties
                    .removeGroup(SecurityContext.SECURITY_PROPERTIES);
            }

            _setPropertiesValue(
                queueProperties,
                SOMProxy.USER_PROPERTY,
                params.getString(USER_PARAM));
            _setPropertiesValue(
                queueProperties,
                SOMProxy.PASSWORD_PROPERTY,
                params.getString(PASSWORD_PARAM));
        }

        final QueueProxy.Sender sender = QueueProxy.Sender
            .newBuilder()
            .prepare(
                getMetadata().getProperties(),
                queueProperties,
                metadata.getServiceName(),
                getThisLogger())
            .setAutoconnect(true)
            .build();

        if (sender == null) {
            return false;
        }

        _sender = sender;

        getThisLogger().debug(ServiceMessages.SET_UP_COMPLETED);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doAddQuery(final StoreValuesQuery query) {}

    /** {@inheritDoc}
     */
    @Override
    protected String supportedValueTypeCodes()
    {
        return Externalizer.ValueType
            .setToString(EnumSet.allOf(Externalizer.ValueType.class));
    }

    private static void _setPropertiesValue(
            final KeyedGroups properties,
            final String propertyName,
            final Optional<String> value)
    {
        if (value.isPresent()) {
            properties.setValue(propertyName, value.get());
        } else {
            properties.removeValue(propertyName);
        }
    }

    private volatile QueueProxy.Sender _sender;
    private volatile int _updateExceptions;
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
