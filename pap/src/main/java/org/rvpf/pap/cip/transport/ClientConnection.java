/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ClientConnection.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.pap.cip.transport;

import java.io.IOException;
import java.io.Serializable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Attributes;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.exception.ConnectFailedException;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.Tuple;
import org.rvpf.pap.PAPConnection;
import org.rvpf.pap.PAPConnectionListener;
import org.rvpf.pap.cip.CIP;
import org.rvpf.pap.cip.CIPMessages;
import org.rvpf.pap.cip.CIPProxy;

import etherip.types.CIPData;

/**
 * Connection.
 */
public final class ClientConnection
    extends PAPConnection.Abstract
{
    /**
     * Constructs an instance.
     *
     * @param address The remote address.
     * @param port The remote port.
     * @param slot The CIP slot.
     * @param timeout The timeout in milliseconds.
     * @param remoteProxy The remote proxy.
     * @param listener A listener.
     */
    public ClientConnection(
            @Nonnull final String address,
            final int port,
            final int slot,
            final long timeout,
            @Nonnull final CIPProxy remoteProxy,
            @Nonnull final PAPConnectionListener listener)
    {
        _ethernetIP = EtherIPWrapper
            .newBuilder()
            .setAddress(address)
            .setPort(port)
            .setSlot(slot)
            .setTimeout(timeout)
            .build();
        _remoteProxy = remoteProxy;
        _listener = listener;
    }

    /** {@inheritDoc}
     */
    @Override
    public void doClose()
        throws IOException
    {
        if (_ethernetIP.isConnected()) {
            try {
                _ethernetIP.close();
            } catch (final IOException exception) {
                throw exception;
            } catch (final Exception exception) {
                throw new IOException(exception);
            }

            Require
                .ignored(
                    _listener
                        .onLostConnection(
                                _remoteProxy,
                                        Optional.ofNullable(_exception)));
        }
    }

    /**
     * Opens.
     *
     * @throws ConnectFailedException On failure.
     */
    public void open()
        throws ConnectFailedException
    {
        if (!_ethernetIP.isConnected()) {
            try {
                _ethernetIP.connect();
            } catch (final Exception connectException) {
                try {
                    _ethernetIP.close();
                } catch (final Exception closeException) {
                    // Ignores.
                }

                throw new ConnectFailedException(
                    CIPMessages.CONNECT_FAILED,
                    _ethernetIP.getAddress(),
                    connectException.getMessage());
            }

            Require.ignored(_listener.onNewConnection(_remoteProxy));
        }
    }

    /**
     * Sends read requests.
     *
     * @param transaction The transaction.
     *
     * @throws ServiceNotAvailableException When the service is not available.
     */
    public void sendReadRequests(
            @Nonnull final ReadTransaction transaction)
        throws ServiceNotAvailableException
    {
        if (transaction.isEmpty()) {
            return;
        }

        final Collection<ReadTransaction.Request> requests = transaction
            .getRequests();
        final String[] tags = new String[requests.size()];
        final int[] elements = new int[tags.length];
        int index = 0;

        for (final ReadTransaction.Request request: requests) {
            final Point point = request.getPoint();
            final Optional<Attributes> pointAttributes = point
                .getAttributes(CIP.ATTRIBUTES_USAGE);

            tags[index] = _remoteProxy.getContext().getTag(point);
            elements[index++] = (pointAttributes
                .isPresent())? pointAttributes
                    .get()
                    .getInt(CIP.ELEMENTS_ATTRIBUTE, 1): 1;
        }

        final CIPData[] values;

        try {
            values = _ethernetIP.readTags(tags, elements);

            final DateTime now = DateTime.now();

            index = 0;

            for (final ReadTransaction.Request request: requests) {
                final String tag = tags[index];
                final CIPData data = values[index];

                if (data == null) {
                    getThisLogger()
                        .warn(
                            CIPMessages.READ_TAG_FAILED,
                            tag,
                            _ethernetIP.decodeStatus(index));
                    transaction.addResponse(request, Optional.empty());

                    continue;
                }

                final int dataElements = elements[index++];
                final Serializable value;

                if (data.getElementCount() != dataElements) {
                    getThisLogger()
                        .warn(CIPMessages.ELEMENT_COUNT_MISMATCH, tag);
                    transaction.addResponse(request, Optional.empty());

                    continue;
                }

                if (dataElements > 1) {
                    final Tuple tuple = new Tuple(dataElements);

                    for (int element = 0; element < dataElements; ++element) {
                        tuple.add(_convert(data, element));
                    }

                    value = tuple;
                } else {
                    value = _convert(data, 0);
                }

                final PointValue pointValue = new PointValue(
                    request.getPoint(),
                    Optional.of(now),
                    null,
                    value);

                transaction.addResponse(request, Optional.of(pointValue));

                if (!_typesByTag.containsKey(tag)) {
                    _typesByTag.put(tag, data.getType());
                }
            }
        } catch (final Exception exception) {
            _exception = exception;
            close();

            throw new ServiceNotAvailableException(exception);
        }
    }

    /**
     * Sends write requests.
     *
     * @param transaction The transaction.
     *
     * @throws ServiceNotAvailableException When the service is not available.
     */
    public void sendWriteRequests(
            @Nonnull final WriteTransaction transaction)
        throws ServiceNotAvailableException
    {
        if (transaction.isEmpty()) {
            return;
        }

        final Collection<WriteTransaction.Request> requests = transaction
            .getRequests();

        final String[] tags = new String[requests.size()];
        final CIPData[] values = new CIPData[tags.length];
        final int[] elements = new int[tags.length];
        final Collection<String> newTags = new LinkedList<>();
        int index = 0;

        for (final WriteTransaction.Request request: requests) {
            final PointValue pointValue = request.getPointValue();
            final Point point = pointValue.getPoint().get();
            final Optional<Attributes> pointAttributes = point
                .getAttributes(CIP.ATTRIBUTES_USAGE);
            final String tag = _remoteProxy.getContext().getTag(point);

            tags[index] = tag;

            if (!_typesByTag.containsKey(tag)) {
                newTags.add(tag);
            }

            elements[index++] = (pointAttributes
                .isPresent())? pointAttributes
                    .get()
                    .getInt(CIP.ELEMENTS_ATTRIBUTE, 1): 1;
        }

        if (!newTags.isEmpty()) {
            final CIPData[] dataArray;

            try {
                dataArray = _ethernetIP
                    .readTags(newTags.toArray(new String[newTags.size()]));
            } catch (final Exception exception) {
                _exception = exception;
                close();

                throw new ServiceNotAvailableException(exception);
            }

            final Iterator<String> iterator = newTags.iterator();

            for (final CIPData data: dataArray) {
                final String tag = iterator.next();

                if (data != null) {
                    _typesByTag.put(tag, data.getType());
                }
            }
        }

        index = 0;

        try {
            Requests: for (final WriteTransaction.Request request: requests) {
                final PointValue pointValue = request.getPointValue();
                final String tag = tags[index];
                final int tagElements = elements[index];
                final Serializable value = pointValue.getValue();
                final CIPData.Type type = _typesByTag.get(tag);

                if (type == null) {
                    transaction.addResponse(request, false);

                    continue;
                }

                final CIPData data = new CIPData(type, tagElements);

                if (value instanceof Tuple) {
                    if (((Tuple) value).size() != tagElements) {
                        getThisLogger()
                            .warn(CIPMessages.TUPLE_SIZE_MISMATCH, tag);
                        transaction.addResponse(request, false);

                        continue;
                    }

                    int element = 0;

                    for (final Serializable item: (Tuple) value) {
                        if (item instanceof Number) {
                            data.set(element++, (Number) item);
                        } else if (item instanceof Boolean) {
                            data
                                .set(
                                    element++,
                                    Byte
                                        .valueOf(
                                                (byte) (((Boolean) item)
                                                        .booleanValue()? -1
                                                        : 0)));
                        } else {
                            getThisLogger().warn(CIPMessages.TUPLE_ITEMS, tag);
                            transaction.addResponse(request, false);

                            continue Requests;
                        }
                    }
                } else if (tagElements > 1) {
                    getThisLogger().warn(CIPMessages.TUPLE_REQUIRED, tag);
                    transaction.addResponse(request, false);

                    continue;
                } else if (value instanceof Number) {
                    data.set(0, (Number) value);
                } else if (value instanceof Boolean) {
                    data
                        .set(
                            0,
                            Byte
                                .valueOf(
                                        (byte) (((Boolean) value).booleanValue()
                                        ? -1: 0)));
                } else {
                    getThisLogger()
                        .warn(CIPMessages.VALUES_MUST_BE_NUMBER, tag);
                    transaction.addResponse(request, false);

                    continue;
                }

                values[index++] = data;
                transaction.addResponse(request, true);
            }

            _ethernetIP.writeTags(tags, values);
        } catch (final Exception exception) {
            transaction.setException(exception);
        }

        for (int i = 0; i < tags.length; ++i) {
            final int status = _ethernetIP.getStatus(i);

            if (status != 0) {
                getThisLogger()
                    .warn(
                        CIPMessages.WRITE_TAG_FAILED,
                        tags[i],
                        _ethernetIP.decodeStatus(i));
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected String getName()
    {
        return _ethernetIP.getAddress();
    }

    private static Serializable _convert(
            @Nonnull final CIPData data,
            final int element)
        throws Exception
    {
        final Serializable value;

        if (data.isNumeric()) {
            final Number number = data.getNumber(element);

            switch (data.getType()) {
                case BOOL: {
                    value = (number
                        .byteValue() != 0)? Boolean.TRUE: Boolean.FALSE;

                    break;
                }
                case USINT: {
                    value = Short.valueOf((short) (number.byteValue() & 0xFF));

                    break;
                }
                case UINT: {
                    value = Integer.valueOf(number.shortValue() & 0xFFFF);

                    break;
                }
                case UDINT: {
                    value = Long.valueOf(number.intValue() & 0xFFFFFFFFL);

                    break;
                }
                default: {
                    value = number;

                    break;
                }
            }
        } else {
            try {
                value = data.getString();
            } catch (final Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        return value;
    }

    private final EthernetIPWrapper _ethernetIP;
    private volatile Exception _exception;
    private final PAPConnectionListener _listener;
    private final CIPProxy _remoteProxy;
    private final Map<String, CIPData.Type> _typesByTag = new HashMap<>();
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
