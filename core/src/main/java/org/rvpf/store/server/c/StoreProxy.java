/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreProxy.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.store.server.c;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import java.nio.charset.Charset;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.PointUnknownException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionConnectFailedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Coder;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.State;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.service.rmi.ServiceRegistry;
import org.rvpf.store.server.StoreMessages;

/**
 * Store proxy.
 */
public final class StoreProxy
{
    /**
     * Describes an exception.
     *
     * @param exception The exception.
     *
     * @return The description.
     */
    static byte[] describeException(final Throwable exception)
    {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);

        exception.printStackTrace(printWriter);
        printWriter.close();

        return new Coder().encode(stringWriter.toString());
    }

    /**
     * Connects.
     *
     * @return A status code.
     */
    synchronized int connect()
    {
        if (_storeSessionProxy != null) {
            _LOGGER
                .error(
                    StoreMessages.ALREADY_CONNECTED,
                    _storeSessionProxy.getServerName());

            return Status.FAILED_CODE;
        }

        _keyedGroups.freeze();

        final RegistryEntry registryEntry = RegistryEntry
            .newBuilder()
            .setBinding(_keyedGroups.getString(BINDING_PROPERTY))
            .setName(_keyedGroups.getString(NAME_PROPERTY))
            .setDefaultName(Store.DEFAULT_STORE_NAME)
            .setDefaultRegistryAddress(ServiceRegistry.getRegistryAddress())
            .setDefaultRegistryPort(ServiceRegistry.getRegistryPort())
            .build();

        _storeSessionProxy = (StoreSessionProxy) StoreSessionProxy
            .newBuilder()
            .setRegistryEntry(registryEntry)
            .setConfigProperties(_keyedGroups)
            .setSecurityProperties(
                _keyedGroups.getGroup(SecurityContext.SECURITY_PROPERTIES))
            .setLoginUser(_keyedGroups.getString(USER_PROPERTY))
            .setLoginPassword(_keyedGroups.getPassword(PASSWORD_PROPERTY))
            .setClientName(
                _keyedGroups
                    .getString(SESSION_PROPERTY, Optional.of(_DEFAULT_SESSION))
                    .get())
            .setClientLogger(_LOGGER)
            .build();

        if (_storeSessionProxy == null) {
            return Status.FAILED_CODE;
        }

        try {
            _storeSessionProxy.connect();
        } catch (final SessionConnectFailedException exception) {
            _LOGGER.error(StoreMessages.CONNECT_FAILED, exception.getMessage());

            return Status.FAILED_CODE;
        }

        return Status.SUCCESS_CODE;
    }

    /**
     * Counts values.
     *
     * @param serverHandle A server handle for the point.
     * @param startTime The inclusive start time.
     * @param endTime The exclusive end time.
     * @param limit A limit for the number of values.
     *
     * @return The values.
     */
    long count(
            final int serverHandle,
            final long startTime,
            final long endTime,
            final int limit)
    {
        long count;

        try {
            final UUID uuid = _serverPoints.get(Integer.valueOf(serverHandle));

            if (uuid != null) {
                final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                    .newBuilder()
                    .setPointUUID(uuid);

                if (startTime <= endTime) {
                    queryBuilder.setNotBefore(DateTime.fromRaw(startTime));
                    queryBuilder.setBefore(DateTime.fromRaw(endTime));
                    queryBuilder.setReverse(false);
                } else {
                    queryBuilder.setAfter(DateTime.fromRaw(endTime));
                    queryBuilder.setNotAfter(DateTime.fromRaw(startTime));
                    queryBuilder.setReverse(true);
                }

                queryBuilder.setAll(true);
                queryBuilder.setCount(true);

                final Optional<StoreValues> storeResponse = _storeSessionProxy
                    .select(queryBuilder.build());

                count = (storeResponse.isPresent()
                         && storeResponse.get().isSuccess())? storeResponse
                                 .get()
                                 .getCount(): -1;
            } else {
                count = -1;
            }
        } catch (final Throwable throwable) {
            _LOGGER.warn(throwable, StoreMessages.EXCEPTION_ON_COUNT);
            count = -1;
        }

        return count;
    }

    /**
     * Deletes points values.
     *
     * @param serverHandles The server handles for the points.
     * @param times The times of the values.
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    int delete(
            final int[] serverHandles,
            final long[] times,
            final int[] statusCodes)
    {
        try {
            Require
                .success(
                    (times.length == serverHandles.length)
                    && (statusCodes.length == serverHandles.length));

            final PointValue[] updates = new PointValue[serverHandles.length];

            for (int i = 0; i < serverHandles.length; ++i) {
                final UUID uuid = _serverPoints
                    .get(Integer.valueOf(serverHandles[i]));

                if (uuid != null) {
                    updates[i] = new VersionedValue.Deleted(
                        uuid,
                        Optional.of(DateTime.fromRaw(times[i])));
                }
            }

            return _updateStatusCodes(
                _storeSessionProxy.update(updates),
                statusCodes);
        } catch (final Throwable throwable) {
            _LOGGER.warn(throwable, StoreMessages.EXCEPTION_ON_DELETE);

            return Status.FAILED_CODE;
        }
    }

    /**
     * Delivers values.
     *
     * @param limit A limit for the number of values.
     * @param timeout A time limit in millis to wait for the first message
     *                (negative for infinite).
     *
     * @return The values.
     */
    Values deliver(final int limit, final long timeout)
    {
        final Values values = new Values();
        final StoreSessionProxy storeSession;

        synchronized (this) {
            storeSession = _storeSessionProxy;
        }

        if (storeSession != null) {
            try {
                final StoreValues storeResponse = storeSession
                    .deliver(limit, timeout);

                if (storeResponse != null) {
                    for (final PointValue pointValue: storeResponse) {
                        final Integer clientHandle = _clientHandles
                            .get(pointValue.getPointUUID());

                        if (clientHandle != null) {
                            _addValue(
                                pointValue,
                                clientHandle.intValue(),
                                values);
                        } else {
                            _LOGGER
                                .warn(
                                    StoreMessages.UNEXPECTED_DELIVERY,
                                    pointValue);
                        }
                    }
                } else {
                    values.setStatus(Status.DISCONNECTED);
                }
            } catch (final Throwable throwable) {
                _LOGGER.warn(throwable, StoreMessages.EXCEPTION_ON_DELIVER);
                values.setStatus(Status.FAILED);
            }
        } else {
            values.setStatus(Status.DISCONNECTED);
        }

        return values;
    }

    /**
     * Disconnects.
     *
     * @return A status code.
     */
    synchronized int disconnect()
    {
        if (_storeSessionProxy != null) {
            _storeSessionProxy.tearDown();
            _storeSessionProxy = null;
        }

        _clientHandles.clear();
        _serverPoints.clear();
        _serverHandles.clear();

        return Status.SUCCESS_CODE;
    }

    /**
     * Disposes.
     */
    void dispose()
    {
        disconnect();
    }

    /**
     * Exchanges handles.
     *
     * @param tags Tags for the points (input).
     * @param clientHandles Client handles for the points (input).
     * @param serverHandles Server handles for the points (output).
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    synchronized int exchangeHandles(
            final byte[][] tags,
            final int[] clientHandles,
            final int[] serverHandles,
            final int[] statusCodes)
    {
        if (_storeSessionProxy == null) {
            return Status.DISCONNECTED_CODE;
        }

        for (int i = 0; i < tags.length; ++i) {
            final String name = Require.notNull(_coder.decode(tags[i]));

            if ((name.length() > 0) && (clientHandles[i] != 0)) {
                final PointBinding.Request.Builder requestBuilder =
                    PointBinding.Request
                        .newBuilder();

                requestBuilder.selectName(name);

                try {
                    final Optional<PointBinding[]> bindings = _storeSessionProxy
                        .getPointBindings(requestBuilder.build());

                    if (!bindings.isPresent()) {
                        throw new ServiceClosedException();
                    }

                    final UUID uuid = bindings.get()[0].getUUID();
                    Integer serverHandle;

                    serverHandle = _serverHandles.get(uuid);

                    if (serverHandle == null) {
                        serverHandle = Integer.valueOf(++_lastHandle);
                        _serverPoints.put(serverHandle, uuid);
                        _serverHandles.put(uuid, serverHandle);
                    }

                    serverHandles[i] = serverHandle.intValue();
                    _clientHandles.put(uuid, Integer.valueOf(clientHandles[i]));
                } catch (final SessionException exception) {
                    statusCodes[i] = Status.FAILED_CODE;
                }
            }
        }

        return Status.SUCCESS_CODE;
    }

    /**
     * Gets a state code.
     *
     * @param nameBytes The state name.
     * @param serverHandle An optional server handle for the point.
     *
     * @return The state code.
     *
     * @throws Status.FailedException When the request fails.
     */
    int getStateCode(
            final byte[] nameBytes,
            final int serverHandle)
        throws Status.FailedException
    {
        final String stateName = _coder.decode(nameBytes);
        final UUID pointUUID;

        if (serverHandle != 0) {
            pointUUID = _serverPoints.get(Integer.valueOf(serverHandle));

            if (pointUUID == null) {
                throw new Status.FailedException(Status.BAD_HANDLE_CODE);
            }
        } else {
            pointUUID = null;
        }

        final State state;

        try {
            state = _storeSessionProxy
                .resolve(
                    new State(Optional.empty(), Optional.ofNullable(stateName)),
                    pointUUID);
        } catch (final SessionException exception) {
            throw new Status.FailedException(Status.FAILED_CODE);
        }

        if (state == null) {
            throw new Status.FailedException(Status.FAILED_CODE);
        }

        return state.getCode().get().intValue();
    }

    /**
     * Gets a state name.
     *
     * @param code The state code.
     * @param serverHandle An optional server handle for the point.
     *
     * @return The state name (null when not found).
     */
    byte[] getStateName(final int code, final int serverHandle)
    {
        final UUID pointUUID;

        if (serverHandle != 0) {
            pointUUID = _serverPoints.get(Integer.valueOf(serverHandle));

            if (pointUUID == null) {
                return null;
            }
        } else {
            pointUUID = null;
        }

        State state;

        try {
            state = _storeSessionProxy
                .resolve(
                    new State(
                        Optional.of(Integer.valueOf(code)),
                        Optional.empty()),
                    pointUUID);
        } catch (final SessionException exception) {
            state = null;
        }

        return (state != null)? _coder.encode(state.getName().get()): null;
    }

    /**
     * Interrupts.
     */
    synchronized void interrupt()
    {
        try {
            _storeSessionProxy.interrupt();
        } catch (final SessionException exception) {
            // Ignores.
        }
    }

    /**
     * Puts an environment entry.
     *
     * @param entryBytes The entry (key=value).
     */
    void putEnv(final byte[] entryBytes)
    {
        final String entryString = _coder.decode(entryBytes);

        Require.notNull(entryString);

        final int eqPos = entryString.indexOf('=');

        if (eqPos > 0) {
            final String key = entryString.substring(0, eqPos).trim();
            final String value = entryString.substring(eqPos + 1).trim();

            _keyedGroups.setValue(key, value);
        }
    }

    /**
     * Reads values.
     *
     * @param serverHandle A server handle for the point.
     * @param startTime The inclusive start time.
     * @param endTime The exclusive end time.
     * @param limit A limit for the number of values.
     *
     * @return The values.
     */
    Values read(
            final int serverHandle,
            final long startTime,
            final long endTime,
            final int limit)
    {
        final Values values = new Values();

        try {
            final UUID uuid = _serverPoints.get(Integer.valueOf(serverHandle));

            if (uuid != null) {
                final int clientHandle = _clientHandles.get(uuid).intValue();
                final StoreValuesQuery.Builder storeQueryBuilder =
                    StoreValuesQuery
                        .newBuilder()
                        .setPointUUID(uuid);

                if (startTime <= endTime) {
                    storeQueryBuilder.setNotBefore(DateTime.fromRaw(startTime));
                    storeQueryBuilder.setBefore(DateTime.fromRaw(endTime));
                    storeQueryBuilder.setReverse(false);
                } else {
                    storeQueryBuilder.setAfter(DateTime.fromRaw(endTime));
                    storeQueryBuilder.setNotAfter(DateTime.fromRaw(startTime));
                    storeQueryBuilder.setReverse(true);
                }

                storeQueryBuilder.setRows(limit);

                try {
                    for (final PointValue pointValue:
                            _storeSessionProxy
                                .iterate(
                                        storeQueryBuilder.build(),
                                                Optional.empty())) {
                        _addValue(pointValue, clientHandle, values);
                    }
                } catch (final StoreValuesQuery.IterationException exception) {
                    values
                        .setStatus(
                            (exception.getCause()
                            instanceof InterruptedException)? Status.DISCONNECTED
                                : Status.FAILED);
                }
            } else {
                values.setStatus(Status.BAD_HANDLE);
            }
        } catch (final Throwable throwable) {
            _LOGGER.warn(throwable, StoreMessages.EXCEPTION_ON_READ);
            values.setStatus(Status.FAILED);
        }

        return values;
    }

    /**
     * Releases handles.
     *
     * @param serverHandles Server handles for the points.
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    synchronized int releaseHandles(
            final int[] serverHandles,
            final int[] statusCodes)
    {
        if (_storeSessionProxy == null) {
            return Status.DISCONNECTED_CODE;
        }

        for (int i = 0; i < serverHandles.length; ++i) {
            if (serverHandles[i] != 0) {
                final Integer serverHandle = Integer.valueOf(serverHandles[i]);
                final UUID uuid = _serverPoints.remove(serverHandle);

                if (uuid != null) {
                    _serverHandles.remove(uuid);
                    _clientHandles.remove(uuid);
                } else {
                    statusCodes[i] = Status.FAILED_CODE;
                }
            }
        }

        return Status.SUCCESS_CODE;
    }

    /**
     * Subscribes to point value events.
     *
     * @param serverHandles Server handles for the points.
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    synchronized int subscribe(
            final int[] serverHandles,
            final int[] statusCodes)
    {
        if (_storeSessionProxy == null) {
            return Status.DISCONNECTED_CODE;
        }

        try {
            Require.success(serverHandles.length == statusCodes.length);

            final UUID[] points = new UUID[serverHandles.length];

            for (int i = 0; i < serverHandles.length; ++i) {
                points[i] = _serverPoints
                    .get(Integer.valueOf(serverHandles[i]));

                if (points[i] == null) {
                    statusCodes[i] = Status.BAD_HANDLE_CODE;
                }
            }

            final StoreValues[] responses = _storeSessionProxy
                .subscribe(points);

            if (responses == null) {
                return Status.DISCONNECTED_CODE;
            }

            final Exception[] exceptions = new Exception[points.length];

            for (int i = 0; i < points.length; ++i) {
                exceptions[i] = responses[i].getException().orElse(null);
            }

            return _updateStatusCodes(exceptions, statusCodes);
        } catch (final Throwable throwable) {
            _LOGGER.warn(throwable, StoreMessages.EXCEPTION_ON_SUBSCRIBE);

            return Status.FAILED_CODE;
        }
    }

    /**
     * Returns the supported value type codes.
     *
     * @return The supported value type codes.
     */
    byte[] supportedValueTypeCodes()
    {
        final String typeCodes;

        try {
            typeCodes = _storeSessionProxy.supportedValueTypeCodes();
        } catch (final SessionException exception) {
            return new byte[0];
        }

        return _coder.encode(typeCodes);
    }

    /**
     * Asks if the store supports pull queries.
     *
     * @return True if pull queries are supported.
     */
    boolean supportsCount()
    {
        try {
            return _storeSessionProxy.supportsCount();
        } catch (final SessionException exception) {
            return false;
        }
    }

    /**
     * Asks if the store supports delete.
     *
     * @return True if delete is supported.
     */
    boolean supportsDelete()
    {
        try {
            return _storeSessionProxy.supportsDelete();
        } catch (final SessionException exception) {
            return false;
        }
    }

    /**
     * Asks if the store supports deliver.
     *
     * @return True if deliver is supported.
     */
    boolean supportsDeliver()
    {
        try {
            return _storeSessionProxy.supportsDeliver();
        } catch (final SessionException exception) {
            return false;
        }
    }

    /**
     * Asks if the store supports pull queries.
     *
     * @return True if pull queries are supported.
     */
    boolean supportsPull()
    {
        try {
            return _storeSessionProxy.supportsPull();
        } catch (final SessionException exception) {
            return false;
        }
    }

    /**
     * Asks if the store supports subscriptions.
     *
     * @return True if subscriptions are supported.
     */
    boolean supportsSubscribe()
    {
        try {
            return _storeSessionProxy.supportsSubscribe();
        } catch (final SessionException exception) {
            return false;
        }
    }

    /**
     * Unsubscribes from point value events.
     *
     * @param serverHandles Server handles for the points.
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    synchronized int unsubscribe(
            final int[] serverHandles,
            final int[] statusCodes)
    {
        if (_storeSessionProxy == null) {
            return Status.DISCONNECTED_CODE;
        }

        try {
            Require.success(serverHandles.length == statusCodes.length);

            final UUID[] points = new UUID[serverHandles.length];

            for (int i = 0; i < serverHandles.length; ++i) {
                points[i] = _serverPoints
                    .get(Integer.valueOf(serverHandles[i]));

                if (points[i] == null) {
                    statusCodes[i] = Status.BAD_HANDLE_CODE;
                }
            }

            return _updateStatusCodes(
                _storeSessionProxy.unsubscribe(points),
                statusCodes);
        } catch (final Throwable throwable) {
            _LOGGER.warn(throwable, StoreMessages.EXCEPTION_ON_UNSUBSCRIBE);

            return Status.FAILED_CODE;
        }
    }

    /**
     * Specifies the use of a charset.
     *
     * @param nameBytes The charset name in US-ASCII.
     */
    void useCharset(final byte[] nameBytes)
    {
        final String nameString = new Coder(Charset.forName("US-ASCII"))
            .decode(nameBytes);

        _coder = new Coder(Charset.forName(nameString));
    }

    /**
     * Writes points values.
     *
     * @param values The points values.
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    int write(final Values values, final int[] statusCodes)
    {
        try {
            Require.success(statusCodes.length == values.size());

            final PointValue[] updates = new PointValue[values.size()];

            for (int i = 0; i < values.size(); ++i) {
                final UUID uuid = _serverPoints
                    .get(Integer.valueOf(values.next()));

                if (uuid != null) {
                    final DateTime stamp = DateTime.fromRaw(values.getTime());
                    final Serializable value = Externalizer
                        .internalize(
                            values.getValue(),
                            Optional.ofNullable(_coder));
                    final Serializable state = (values
                        .getQuality() != 0)? Integer
                            .valueOf(values.getQuality()): null;

                    updates[i] = new PointValue(
                        uuid,
                        Optional.of(stamp),
                        state,
                        value);
                }
            }

            return _updateStatusCodes(
                _storeSessionProxy.update(updates),
                statusCodes);
        } catch (final Throwable throwable) {
            _LOGGER.warn(throwable, StoreMessages.EXCEPTION_ON_WRITE);

            return Status.FAILED_CODE;
        }
    }

    private static int _updateStatusCodes(
            final Exception[] exceptions,
            final int[] statusCodes)
    {
        if (exceptions == null) {
            return Status.DISCONNECTED_CODE;
        }

        for (int i = 0; i < exceptions.length; ++i) {
            if ((exceptions[i] != null)
                    && (statusCodes[i] == Status.SUCCESS_CODE)) {
                if (exceptions[i] instanceof NullPointerException) {
                    statusCodes[i] = Status.BAD_HANDLE_CODE;
                } else if (exceptions[i] instanceof PointUnknownException) {
                    statusCodes[i] = Status.POINT_UNKNOWN_CODE;
                } else if (exceptions[i] instanceof IllegalStateException) {
                    statusCodes[i] = Status.ILLEGAL_STATE_CODE;
                } else {
                    statusCodes[i] = Status.FAILED_CODE;
                }
            }
        }

        return Status.SUCCESS_CODE;
    }

    private void _addValue(
            final PointValue pointValue,
            final int clientHandle,
            final Values values)
    {
        final long time = pointValue.getStamp().toRaw();
        final boolean deleted = pointValue.isDeleted();
        final byte[] value;
        int quality = 0;

        if (deleted) {
            value = null;
        } else {
            final Serializable state = pointValue.getState();

            value = Externalizer
                .externalize(
                    pointValue.getValue(),
                    Optional.ofNullable(_coder));

            if (state instanceof Number) {
                quality = ((Number) state).intValue();
            } else if (state instanceof String) {
                try {
                    quality = Integer.parseInt((String) state);
                } catch (final NumberFormatException exception) {
                    // Ignores.
                }
            }
        }

        values.add(clientHandle, time, deleted, quality, value);
    }

    /** Binding property. */
    public static final String BINDING_PROPERTY = "binding";

    /** Name property. */
    public static final String NAME_PROPERTY = "name";

    /** Password property. */
    public static final String PASSWORD_PROPERTY = "password";

    /** Session name property. */
    public static final String SESSION_PROPERTY = "session";

    /** User property. */
    public static final String USER_PROPERTY = "user";
    private static final String _DEFAULT_SESSION = "c-store";
    private static final Logger _LOGGER = Logger.getInstance(StoreProxy.class);

    private final Map<UUID, Integer> _clientHandles = new HashMap<>();
    private volatile Coder _coder;
    private final KeyedGroups _keyedGroups = new KeyedGroups();
    private int _lastHandle;
    private final Map<UUID, Integer> _serverHandles = new HashMap<>();
    private final Map<Integer, UUID> _serverPoints = new HashMap<>();
    private volatile StoreSessionProxy _storeSessionProxy;
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
