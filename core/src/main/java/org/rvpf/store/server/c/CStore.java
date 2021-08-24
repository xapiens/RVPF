/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CStore.java 4100 2019-06-29 17:22:34Z SFB $
 */

package org.rvpf.store.server.c;

import java.io.File;
import java.io.Serializable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.tool.Coder;
import org.rvpf.base.tool.Externalizer;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.State;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.service.ServiceThread;
import org.rvpf.store.server.StoreMessages;

/**
 * C store.
 */
final class CStore
    implements ServiceThread.Target
{
    /**
     * Constructs an instance.
     *
     * @param storeAppImpl The calling store application implementation.
     * @param implFile The implementation shared object file.
     * @param coder The text encoder / decoder.
     * @param connectionRetryDelay The delay before retrying a connection.
     * @param envProperties The environment properties.
     */
    CStore(
            @Nonnull final CStoreServiceAppImpl storeAppImpl,
            @Nonnull final File implFile,
            @Nonnull final Coder coder,
            @Nonnull final ElapsedTime connectionRetryDelay,
            @Nonnull final KeyedGroups envProperties)
    {
        _storeAppImpl = Require.notNull(storeAppImpl);
        _implFile = Require.notNull(implFile);
        _coder = Require.notNull(coder);
        _connectionRetryDelay = Require.notNull(connectionRetryDelay);
        _envProperties = Require.notNull(envProperties);
    }

    /** {@inheritDoc}
     */
    @Override
    public void run()
        throws InterruptedException
    {
        try {
            synchronized (this) {
                boolean retryNotified = false;

                for (;;) {
                    final int status = connect(_contextHandle);

                    _connected = status == Status.SUCCESS_CODE;

                    if (_connected) {
                        break;
                    }

                    if ((status == Status.UNRECOVERABLE_CODE)
                            || (_connectionRetryDelay == null)) {
                        _logger.error(StoreMessages.FAILED_CONNECT);

                        break;
                    }

                    if (!retryNotified) {
                        _logger.info(StoreMessages.RETRYING_CONNECT);
                        retryNotified = true;
                    }

                    _storeAppImpl.getService().snooze(_connectionRetryDelay);
                }

                _logger
                    .debug(
                        ServiceMessages.THREAD_READY,
                        Thread.currentThread().getName());
                _started = true;
                notifyAll();

                if (!_connected) {
                    return;
                }
            }

            while (_connected) {
                final Runnable task = _queue.take();

                if (_connected) {
                    task.run();
                }
            }
        } finally {
            synchronized (this) {
                if (_connected) {
                    disconnect(_contextHandle);
                    _connected = false;
                }

                _clientPoints.clear();
                _serverHandles.clear();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void finalize()
        throws Throwable
    {
        _tearDown();

        super.finalize();
    }

    /**
     * Gets an instance for the specified implementation.
     *
     * @param storeAppImpl The calling store application implementation.
     * @param implFile The implementation shared object file.
     * @param charset The charset used for encoding / decoding.
     * @param connectionRetryDelay The delay before retrying a connection.
     * @param envProperties The optional environment properties.
     *
     * @return The instance (null on failure).
     */
    @Nullable
    @CheckReturnValue
    static synchronized CStore getInstance(
            @Nonnull final CStoreServiceAppImpl storeAppImpl,
            @Nonnull final File implFile,
            @Nonnull final Charset charset,
            @Nonnull final ElapsedTime connectionRetryDelay,
            @Nonnull final KeyedGroups envProperties)
    {
        CStore instance = _SINGLETONS.get(implFile);

        if (instance == null) {
            instance = new CStore(
                storeAppImpl,
                implFile,
                new Coder(charset),
                connectionRetryDelay,
                envProperties);

            if (instance._setUp()) {
                if (!instance._supportsConnections()) {
                    _SINGLETONS.put(implFile, instance);
                }
            } else {
                instance._tearDown();
                instance = null;
            }
        } else if (!charset.equals(instance.getCoder().getCharset())) {
            _LOGGER.error(StoreMessages.INCONSISTENT_CHARSET, implFile);
            instance = null;
        } else if (!envProperties.equals(instance.getEnvProperties())) {
            _LOGGER.error(StoreMessages.INCONSISTENT_ENV, implFile);
            instance = null;
        } else if (!instance._preempt(storeAppImpl)) {
            instance.dispose();
            instance = null;
        }

        if ((instance != null) && !instance._start()) {
            instance._tearDown();
            instance = null;
        }

        return instance;
    }

    /**
     * Asks if this class is implemented.
     *
     * <p>Allows skipping tests if the native library is not available.</p>
     *
     * @return True if this class is implemented.
     */
    @CheckReturnValue
    static boolean isImplemented()
    {
        return _IMPLEMENTED;
    }

    /**
     * Counts values for a point.
     *
     * @param uuid The UUID for the point.
     * @param startTime The inclusive start time.
     * @param endTime The exclusive end time.
     * @param limit A limit for the number of values.
     *
     * @return A container for the values.
     *
     * @throws Status.FailedException When the count operation fails.
     */
    @Nonnull
    @CheckReturnValue
    Long count(
            @Nonnull final UUID uuid,
            @Nonnull final DateTime startTime,
            @Nonnull final DateTime endTime,
            final int limit)
        throws Status.FailedException
    {
        _protectSingleThread();

        final int serverHandle = getServerHandle(uuid);
        final AtomicLong count = new AtomicLong(-1);
        final int statusCode = count(
            _contextHandle,
            serverHandle,
            startTime.toRaw(),
            endTime.toRaw(),
            limit,
            count);

        if (statusCode != Status.SUCCESS_CODE) {
            throw new Status.FailedException(statusCode);
        }

        return Long.valueOf(count.get());
    }

    /**
     * Deletes point values.
     *
     * @param pointValues The point values.
     *
     * @return The individual status codes.
     *
     * @throws Status.FailedException When the delete operation fails.
     */
    @Nonnull
    @CheckReturnValue
    int[] delete(
            @Nonnull final Collection<PointValue> pointValues)
        throws Status.FailedException
    {
        final int[] serverHandles = new int[pointValues.size()];
        final long[] times = new long[serverHandles.length];
        final Iterator<PointValue> valueIterator = pointValues.iterator();

        for (int i = 0; i < serverHandles.length; ++i) {
            final PointValue pointValue = valueIterator.next();

            serverHandles[i] = getServerHandle(pointValue.getPointUUID());
            times[i] = pointValue.getStamp().toRaw();
        }

        return delete(serverHandles, times);
    }

    /**
     * Deletes points values.
     *
     * @param serverHandles The server handles for the points.
     * @param times The times for the values.
     *
     * @return The individual status codes.
     *
     * @throws Status.FailedException When the delete operation fails.
     */
    @Nonnull
    @CheckReturnValue
    int[] delete(
            @Nonnull final int[] serverHandles,
            @Nonnull final long[] times)
        throws Status.FailedException
    {
        _protectSingleThread();

        final int[] statusCodes = new int[serverHandles.length];
        final int statusCode = delete(
            _contextHandle,
            Require.notNull(serverHandles),
            Require.notNull(times),
            statusCodes);

        if (statusCode != Status.SUCCESS_CODE) {
            throw new Status.FailedException(statusCode);
        }

        return statusCodes;
    }

    /**
     * Delivers.
     *
     * @param limit A limit for the number of values.
     * @param timeout A time limit in millis to wait for the first message
     *                (negative for infinite).
     *
     * @return A container for the values.
     *
     * @throws Status.FailedException When the deliver operation fails.
     */
    @Nonnull
    @CheckReturnValue
    Values deliver(
            final int limit,
            final long timeout)
        throws Status.FailedException
    {
        _protectSingleThread();

        final Values values = new Values();
        final int statusCode = deliver(_contextHandle, limit, timeout, values);

        if ((statusCode != Status.SUCCESS_CODE)
                && (statusCode != Status.DISCONNECTED_CODE)) {
            throw new Status.FailedException(statusCode);
        }

        values.setStatus(Status.get(statusCode));

        return values;
    }

    /**
     * Disposes.
     */
    synchronized void dispose()
    {
        synchronized (CStore.class) {
            _tearDown();
            _SINGLETONS.remove(_implFile);
        }
    }

    /**
     * Executes a task.
     *
     * @param task The task.
     *
     * @throws InterruptedException When interrupted.
     */
    void execute(@Nonnull final Task<?> task)
        throws InterruptedException
    {
        if (_connected) {
            if (supportsThreads()) {
                task.run();
            } else {
                try {
                    _queue.put(task);

                    synchronized (task) {
                        while (!task.isDone()) {
                            if (_connected) {
                                task.wait();
                            } else {
                                throw new InterruptedException();
                            }
                        }
                    }
                } catch (final InterruptedException
                         |RuntimeException exception) {
                    throw exception;
                } catch (final Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        } else {
            throw new InterruptedException();
        }
    }

    /**
     * Forgets points.
     *
     * @param points The points to forget.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean forget(@Nonnull final Collection<Point> points)
    {
        _protectSingleThread();

        if (!points.isEmpty()) {
            final int[] serverHandles = _serverHandles(points);
            final Iterator<Point> pointsIterator = points.iterator();

            for (int i = 0; i < serverHandles.length; ++i) {
                final Point point = pointsIterator.next();
                final String pointName = point.getName().get();
                final UUID pointUUID = point.getUUID().get();

                if (_storeAppImpl
                    .getServer()
                    .removeBinding(
                        new PointBinding(
                            pointName,
                            pointUUID,
                            Optional.empty()))) {
                    _logger.trace(StoreMessages.UNBOUND, point);
                } else {
                    serverHandles[i] = 0;    // Shared.
                }
            }

            final int statusCode = releaseHandles(
                _contextHandle,
                serverHandles,
                new int[serverHandles.length]);

            for (final Point point: points) {
                final UUID uuid = point.getUUID().get();

                _clientPoints.remove(_clientHandles.remove(uuid));
                _serverHandles.remove(uuid);
            }

            if (statusCode != Status.SUCCESS_CODE) {
                if (statusCode == Status.DISCONNECTED_CODE) {
                    _logger.debug(StoreMessages.DISCONNECTED);
                } else {
                    _logger
                        .warn(
                            StoreMessages.FAILED_FORGET,
                            Status.toString(statusCode));
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Forgets point bindings.
     *
     * @param bindings The point bindings to forget.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean forget(@Nonnull final PointBinding[] bindings)
    {
        _protectSingleThread();

        if (bindings.length > 0) {
            final int[] serverHandles = new int[bindings.length];

            for (int i = 0; i < serverHandles.length; ++i) {
                final PointBinding binding = bindings[i];

                if (_storeAppImpl.getServer().removeBinding(binding)) {
                    _logger.trace(StoreMessages.UNBOUND, binding);
                    serverHandles[i] = getServerHandle(binding.getServerUUID());
                } else {
                    serverHandles[i] = 0;    // Shared.
                }
            }

            final int statusCode = releaseHandles(
                _contextHandle,
                serverHandles,
                new int[serverHandles.length]);

            for (final PointBinding binding: bindings) {
                final UUID uuid = binding.getUUID();

                _clientPoints.remove(_clientHandles.remove(uuid));
                _serverHandles.remove(uuid);
            }

            if (statusCode != Status.SUCCESS_CODE) {
                if (statusCode == Status.DISCONNECTED_CODE) {
                    _logger.debug(StoreMessages.DISCONNECTED);
                } else {
                    _logger
                        .warn(
                            StoreMessages.FAILED_FORGET,
                            Status.toString(statusCode));
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Gets the coder.
     *
     * @return The coder.
     */
    @Nonnull
    @CheckReturnValue
    Coder getCoder()
    {
        return _coder;
    }

    /**
     * Gets the environment properties.
     *
     * @return The environment properties.
     */
    @Nonnull
    @CheckReturnValue
    KeyedGroups getEnvProperties()
    {
        return _envProperties;
    }

    /**
     * Gets a code for a quality name.
     *
     * @param qualityName The quality name.
     *
     * @return A quality code (null on failure).
     */
    @Nullable
    @CheckReturnValue
    Integer getQualityCode(final String qualityName)
    {
        Integer code = _qualityCodes.get(qualityName);

        if (code != null) {
            return (code == _NO_CODE)? null: code;
        }

        code = getQualityCode(_contextHandle, _coder.encode(qualityName));

        if (code == null) {
            _logger.warn(StoreMessages.NO_CODE_FOR_QUALITY, qualityName);
        }

        _qualityCodes
            .put(
                qualityName,
                (code != null)? code: _NO_CODE);

        return code;
    }

    /**
     * Gets a name for a quality code.
     *
     * @param qualityCode The quality code.
     *
     * @return A quality name (empty on failure).
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getQualityName(@Nonnull final Integer qualityCode)
    {
        String name = _qualityNames.get(qualityCode);

        if (name != null) {
            return (name == _NO_NAME)? Optional.empty(): Optional.of(name);
        }

        final byte[] qualityName = getQualityName(
            _contextHandle,
            qualityCode.intValue());

        if (qualityName != null) {
            name = _coder.decode(qualityName);
        } else {
            _logger.warn(StoreMessages.NO_NAME_FOR_QUALITY, qualityCode);
        }

        _qualityNames
            .put(
                qualityCode,
                (name != null)? name: _NO_NAME);

        return Optional.ofNullable(name);
    }

    /**
     * Gets the server handle for a UUID.
     *
     * @param uuid The UUID.
     *
     * @return The server handle (0 when not found).
     */
    @CheckReturnValue
    int getServerHandle(@Nonnull final UUID uuid)
    {
        final Integer serverHandle = _serverHandles.get(Require.notNull(uuid));

        return (serverHandle != null)? serverHandle.intValue(): 0;
    }

    /**
     * Gets a code for a state name.
     *
     * @param serverHandle The server handle for the point.
     * @param stateName The state name.
     *
     * @return A state code (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    Optional<Integer> getStateCode(
            final int serverHandle,
            @Nonnull final String stateName)
    {
        return Optional
            .ofNullable(
                getStateCode(
                    _contextHandle,
                    serverHandle,
                    _coder.encode(stateName)));
    }

    /**
     * Gets a name for a state code.
     *
     * @param serverHandle The server handle for the point.
     * @param stateCode The state code.
     *
     * @return A state name (may be empty).
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getStateName(
            final int serverHandle,
            @Nonnull final Integer stateCode)
    {
        final String stateName = _coder
            .decode(
                getStateName(
                    _contextHandle,
                    serverHandle,
                    stateCode.intValue()));

        return Optional.ofNullable(stateName);
    }

    /**
     * Interrupts.
     */
    void interrupt()
    {
        if (_connected) {
            interrupt(_contextHandle);
        }
    }

    /**
     * Logs a message at a specified level.
     *
     * @param level The level.
     * @param messageBytes The message bytes.
     */
    void log(final int level, final byte[] messageBytes)
    {
        final Logger.LogLevel logLevel = Logger.LogLevel.get(level);

        if (_logger.isEnabledFor(logLevel)) {
            _logger
                .log(
                    logLevel,
                    new Message(
                        BaseMessages.VERBATIM,
                        _coder.decode(messageBytes)));
        }
    }

    /**
     * Returns point values from values.
     *
     * @param values The values.
     *
     * @return The point values.
     */
    Collection<PointValue> pointValues(final Values values)
    {
        _protectSingleThread();

        final List<PointValue> pointValues = new ArrayList<PointValue>(
            values.size());

        while (values.hasNext()) {
            final int clientHandle = values.next();

            if (clientHandle == 0) {
                break;
            }

            final UUID uuid = _clientPoints.get(Integer.valueOf(clientHandle));

            if (uuid == null) {
                continue;
            }

            final DateTime stamp = DateTime.fromRaw(values.getTime());
            final PointValue pointValue;

            if (values.isDeleted()) {
                pointValue = new VersionedValue.Deleted(
                    uuid,
                    Optional.of(stamp));
            } else {
                final Integer quality = Integer.valueOf(values.getQuality());
                final State state = (values
                    .getQuality() != 0)? new State(
                        Optional.of(quality),
                        getQualityName(quality)): null;
                Serializable value = Externalizer
                    .internalize(values.getValue(), Optional.of(_coder));

                if ((value instanceof State)
                        && !((State) value).getName().isPresent()) {
                    final Optional<Integer> stateCode = ((State) value)
                        .getCode();

                    if (stateCode.isPresent()) {
                        final Integer serverHandle = _serverHandles.get(uuid);
                        final Optional<String> stateName;

                        if (serverHandle == null) {
                            continue;
                        }

                        stateName = getStateName(
                            serverHandle.intValue(),
                            stateCode.get());

                        if (stateName.isPresent()) {
                            value = new State(stateCode, stateName);
                        }
                    }
                }

                pointValue = new PointValue(
                    uuid,
                    Optional.of(stamp),
                    state,
                    value);
            }

            pointValues.add(pointValue);
        }

        return pointValues;
    }

    /**
     * Reads values for a point.
     *
     * @param uuid The UUID for the point.
     * @param startTime The inclusive start time.
     * @param endTime The exclusive end time.
     * @param limit A limit for the number of values.
     *
     * @return A container for the values.
     *
     * @throws Status.FailedException When the read operation fails.
     */
    Values read(
            final UUID uuid,
            final DateTime startTime,
            final DateTime endTime,
            final int limit)
        throws Status.FailedException
    {
        _protectSingleThread();

        final int serverHandle = getServerHandle(uuid);
        final Values values = new Values();
        final int statusCode = read(
            _contextHandle,
            serverHandle,
            startTime.toRaw(),
            endTime.toRaw(),
            limit,
            values);

        if (statusCode != Status.SUCCESS_CODE) {
            throw new Status.FailedException(statusCode);
        }

        return values;
    }

    /**
     * Rebinds a UUID.
     *
     * @param oldUUID The old UUID.
     * @param newUUID The UUID.
     */
    void rebind(final UUID oldUUID, final UUID newUUID)
    {
        if (!newUUID.equals(oldUUID)) {
            Integer handle;

            handle = Require.notNull(_serverHandles.remove(oldUUID));
            _serverHandles.put(newUUID, handle);

            handle = Require.notNull(_clientHandles.remove(oldUUID));
            _clientHandles.put(newUUID, handle);
            _clientPoints.put(handle, newUUID);

            _storeAppImpl.rebind(oldUUID, newUUID);
        }
    }

    /**
     * Resolves points.
     *
     * @param points The points to resolve.
     *
     * @return True on success.
     */
    boolean resolve(final Collection<Point> points)
    {
        _protectSingleThread();

        final int count = points.size();

        if (count > 0) {
            final String[] tags = new String[count];
            final byte[][] encodedTags = new byte[count][];
            final int[] clientHandles = new int[count];
            final int[] serverHandles = new int[count];
            final int[] statusCodes = new int[count];
            Iterator<Point> pointsIterator;

            pointsIterator = points.iterator();

            for (int i = 0; i < count; ++i) {
                final Point point = pointsIterator.next();
                String tag = point
                    .getParams()
                    .getString(Point.TAG_PARAM, point.getName())
                    .orElse(null);

                tag = (tag != null)? tag.trim(): "";

                if (tag.isEmpty()) {
                    _logger.warn(StoreMessages.POINT_UNNAMED, point);
                }

                final Optional<PointBinding> binding = _storeAppImpl
                    .getServer()
                    .getBinding(tag);

                if (binding.isPresent()) {
                    final UUID serverUUID = Require
                        .notNull(binding.get().getServerUUID());
                    final UUID pointUUID = point.getUUID().get();

                    rebind(serverUUID, pointUUID);
                    tag = "";
                }

                tags[i] = tag;
                encodedTags[i] = _coder.encode(tag);
                clientHandles[i] = ++_lastHandle;
            }

            final int statusCode = exchangeHandles(
                _contextHandle,
                encodedTags,
                clientHandles,
                serverHandles,
                statusCodes);

            if (statusCode == Status.SUCCESS_CODE) {
                pointsIterator = points.iterator();

                for (int i = 0; i < count; ++i) {
                    if (encodedTags[i].length > 0) {
                        final Point point = pointsIterator.next();
                        final int serverHandle = serverHandles[i];

                        if (serverHandle != 0) {
                            final UUID pointUUID = point.getUUID().get();

                            _serverHandles
                                .put(pointUUID, Integer.valueOf(serverHandle));
                            _clientPoints
                                .put(
                                    Integer.valueOf(clientHandles[i]),
                                    pointUUID);
                            _clientHandles
                                .put(
                                    pointUUID,
                                    Integer.valueOf(clientHandles[i]));

                            _storeAppImpl
                                .getServer()
                                .addBinding(
                                    new PointBinding(
                                        tags[i],
                                        pointUUID,
                                        Optional.empty()));
                        } else {
                            _logger.warn(StoreMessages.POINT_UNKNOWN, point);
                        }
                    }
                }
            } else {
                if (statusCode == Status.DISCONNECTED_CODE) {
                    _logger.debug(StoreMessages.DISCONNECTED);
                } else {
                    _logger
                        .warn(
                            StoreMessages.FAILED_RESOLVE,
                            Status.toString(statusCode));
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Resolves points from bindings.
     *
     * @param bindings The bindings.
     *
     * @return True on success.
     */
    boolean resolve(final PointBinding[] bindings)
    {
        _protectSingleThread();

        final int count = bindings.length;

        if (count > 0) {
            final byte[][] tags = new byte[count][];
            final int[] clientHandles = new int[count];
            final int[] serverHandles = new int[count];
            final int[] statusCodes = new int[count];

            for (int i = 0; i < count; ++i) {
                String tag = bindings[i].getName();
                final Optional<PointBinding> binding = _storeAppImpl
                    .getServer()
                    .getBinding(tag);

                if (binding.isPresent()) {
                    rebind(
                        binding.get().getServerUUID(),
                        bindings[i].getServerUUID());
                    tag = "";
                }

                tags[i] = _coder.encode(tag);
                clientHandles[i] = ++_lastHandle;
            }

            final int statusCode = exchangeHandles(
                _contextHandle,
                tags,
                clientHandles,
                serverHandles,
                statusCodes);

            if (statusCode == Status.SUCCESS_CODE) {
                for (int i = 0; i < count; ++i) {
                    final int serverHandle = serverHandles[i];

                    if (serverHandle != 0) {
                        final UUID serverUUID = bindings[i].getServerUUID();

                        _serverHandles
                            .put(serverUUID, Integer.valueOf(serverHandle));
                        _clientPoints
                            .put(Integer.valueOf(clientHandles[i]), serverUUID);
                        _clientHandles
                            .put(serverUUID, Integer.valueOf(clientHandles[i]));

                        _storeAppImpl.getServer().addBinding(bindings[i]);
                    } else {
                        bindings[i] = null;
                    }
                }
            } else {
                if (statusCode == Status.DISCONNECTED_CODE) {
                    _logger.debug(StoreMessages.DISCONNECTED);
                } else {
                    _logger
                        .warn(
                            StoreMessages.FAILED_RESOLVE,
                            Status.toString(statusCode));
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Stops.
     */
    void stop()
    {
        if (_connected) {
            synchronized (this) {
                disconnect(_contextHandle);
                _connected = false;
            }
        }
    }

    /**
     * Subscribes to point value events.
     *
     * @param points The points.
     *
     * @return True on success.
     */
    boolean subscribe(final Collection<Point> points)
    {
        _protectSingleThread();

        if (!points.isEmpty()) {
            final int statusCode = subscribe(
                _contextHandle,
                _serverHandles(points),
                new int[points.size()]);

            if (statusCode != Status.SUCCESS_CODE) {
                if (statusCode == Status.DISCONNECTED_CODE) {
                    _logger.debug(StoreMessages.DISCONNECTED);
                } else {
                    _logger
                        .warn(
                            StoreMessages.FAILED_SUBSCRIBE,
                            Status.toString(statusCode));
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Returns a string of supported value type codes.
     *
     * @return The string of supported value type codes.
     */
    @Nonnull
    @CheckReturnValue
    synchronized String supportedValueTypeCodes()
    {
        if (_supportedValueTypeCodes == null) {
            _supportedValueTypeCodes = _coder
                .decode(supportedValueTypeCodes(_contextHandle));
            _logger
                .debug(
                    StoreMessages.SUPPORTED_VALUE_TYPES,
                    _supportedValueTypeCodes);
        }

        return _supportedValueTypeCodes;
    }

    /**
     * Asks if this implementation supports count.
     *
     * @return True if count is supported.
     */
    synchronized boolean supportsCount()
    {
        if (_supportsCount == null) {
            _supportsCount = Boolean.valueOf(supportsCount(_contextHandle));
            _logger.debug(StoreMessages.SUPPORTS_COUNT, _supportsCount);
        }

        return _supportsCount.booleanValue();
    }

    /**
     * Asks if this implementation supports delete.
     *
     * @return True if delete is supported.
     */
    synchronized boolean supportsDelete()
    {
        if (_supportsDelete == null) {
            _supportsDelete = Boolean.valueOf(supportsDelete(_contextHandle));
            _logger.debug(StoreMessages.SUPPORTS_DELETE, _supportsDelete);
        }

        return _supportsDelete.booleanValue();
    }

    /**
     * Asks if this implementation supports deliver.
     *
     * @return True if deliver is supported.
     */
    synchronized boolean supportsDeliver()
    {
        if (_supportsDeliver == null) {
            _supportsDeliver = Boolean.valueOf(supportsDeliver(_contextHandle));
            _logger.debug(StoreMessages.SUPPORTS_DELIVER, _supportsDeliver);
        }

        return _supportsDeliver.booleanValue();
    }

    /**
     * Asks if this implementation supports pull queries.
     *
     * @return True if pull queries are supported.
     */
    synchronized boolean supportsPull()
    {
        if (_supportsPull == null) {
            _supportsPull = Boolean.valueOf(supportsPull(_contextHandle));
            _logger.debug(StoreMessages.SUPPORTS_PULL, _supportsPull);
        }

        return _supportsPull.booleanValue();
    }

    /**
     * Asks if this implementation supports subscriptions.
     *
     * @return True if subscriptions are supported.
     */
    synchronized boolean supportsSubscribe()
    {
        if (_supportsSubscribe == null) {
            _supportsSubscribe = Boolean
                .valueOf(supportsSubscribe(_contextHandle));
            _logger.debug(StoreMessages.SUPPORTS_SUBSCRIBE, _supportsSubscribe);
        }

        return _supportsSubscribe.booleanValue();
    }

    /**
     * Asks if this implementation supports threads.
     *
     * @return True if threads are supported.
     */
    synchronized boolean supportsThreads()
    {
        if (_supportsThreads == null) {
            _supportsThreads = Boolean.valueOf(supportsThreads(_contextHandle));
            _logger.debug(StoreMessages.SUPPORTS_THREADS, _supportsThreads);
        }

        return _supportsThreads.booleanValue();
    }

    /**
     * Unsubscribes from point value events.
     *
     * @param points The points.
     *
     * @return True on success.
     */
    boolean unsubscribe(final Collection<Point> points)
    {
        _protectSingleThread();

        if (!points.isEmpty()) {
            final int statusCode = unsubscribe(
                _contextHandle,
                _serverHandles(points),
                new int[points.size()]);

            if (statusCode != Status.SUCCESS_CODE) {
                if (statusCode == Status.DISCONNECTED_CODE) {
                    _logger.debug(StoreMessages.DISCONNECTED);
                } else {
                    _logger
                        .warn(
                            StoreMessages.FAILED_UNSUBSCRIBE,
                            Status.toString(statusCode));
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Writes point values.
     *
     * @param pointValues The point values.
     *
     * @return The individual status codes.
     *
     * @throws Status.FailedException When the delete operation fails.
     */
    int[] write(
            final Collection<PointValue> pointValues)
        throws Status.FailedException
    {
        _protectSingleThread();

        final int[] statusCodes = new int[pointValues.size()];
        final Values values = new Values();

        for (final PointValue pointValue: pointValues) {
            final int handle = getServerHandle(pointValue.getPointUUID());
            final long time = pointValue.getStamp().toRaw();
            final boolean deleted = pointValue.isDeleted();
            final int quality;
            byte[] valueBytes;

            if (deleted) {
                quality = 0;
                valueBytes = null;
            } else {
                final Serializable state = pointValue.getState();
                Serializable value = pointValue.getValue();

                if (state == null) {
                    quality = 0;
                } else if (state instanceof Number) {
                    quality = ((Number) state).intValue();
                } else if (state instanceof String) {
                    final String stateName = (String) state;
                    final Integer stateCode = getQualityCode(stateName);

                    quality = (stateCode != null)? stateCode.intValue(): 0;
                } else {
                    quality = 0;
                }

                if (value instanceof State) {
                    final State stateValue = (State) value;

                    if (!stateValue.getCode().isPresent()
                            && stateValue.getName().isPresent()) {
                        final Optional<Integer> stateCode = getStateCode(
                            handle,
                            stateValue.getName().get());

                        if (stateCode.isPresent()) {
                            value = new State(stateCode, stateValue.getName());
                        }
                    }
                }

                valueBytes = Externalizer
                    .externalize(value, Optional.of(_coder));

                if (!_supportsObjects()
                        && (Externalizer.getTypeCode(
                            valueBytes) == Externalizer.ValueType.OBJECT.getCode())) {
                    valueBytes = Externalizer
                        .externalize(value.toString(), Optional.of(_coder));
                }
            }

            values.add(handle, time, deleted, quality, valueBytes);
        }

        final int statusCode = write(_contextHandle, values, statusCodes);

        if (statusCode != Status.SUCCESS_CODE) {
            throw new Status.FailedException(statusCode);
        }

        return statusCodes;
    }

    private static boolean _areSame(final Object left, final Object right)
    {
        return left == right;
    }

    private boolean _preempt(final CStoreServiceAppImpl storeAppImpl)
    {
        if (storeAppImpl != _storeAppImpl) {
            _LOGGER.warn(StoreMessages.PREEMPTING_LIBRARY, _implFile.getPath());
            _tearDown();
            _storeAppImpl = storeAppImpl;

            if (!_setUp()) {
                return false;
            }
        }

        return true;
    }

    private synchronized void _protectSingleThread()
    {
        if (!supportsThreads() && (Thread.currentThread() != _thread.get())) {
            throw new InternalError("Store restricted to single thread access");
        }
    }

    private int[] _serverHandles(final Collection<Point> points)
    {
        final int count = points.size();
        final int[] serverHandles = new int[count];
        final Iterator<Point> pointsIterator = points.iterator();

        for (int i = 0; i < count; ++i) {
            serverHandles[i] = getServerHandle(
                pointsIterator.next().getUUID().get());
        }

        return serverHandles;
    }

    @CheckReturnValue
    private boolean _setUp()
    {
        _logger = Logger
            .getInstance(_LOGGER.getName() + ":" + _storeAppImpl.getLogName());

        final String implFilePath = _implFile.getPath();

        _libraryHandle = openLibrary(
            implFilePath.getBytes(StandardCharsets.UTF_8));

        if (_libraryHandle == 0) {
            _logger.error(StoreMessages.FAILED_LOAD_LIBRARY, implFilePath);

            return false;
        }

        _contextHandle = contextHandle(
            _libraryHandle,
            _logger.getLogLevel().ordinal());

        if (_contextHandle == 0) {
            _logger.error(StoreMessages.FAILED_GET_CONTEXT, implFilePath);

            return false;
        }

        useCharset(
            _contextHandle,
            new Coder(_US_ASCII).encode(_coder.getCharset().name()));

        if (!_envProperties.isMissing()) {
            for (final Map.Entry<String, List<Object>> entry:
                    _envProperties.getValuesEntries()) {
                final String envEntry = entry
                    .getKey()
                    .trim() + '=' + entry.getValue().get(0).toString().trim();

                _logger.debug(StoreMessages.ENV, envEntry);
                putEnv(_contextHandle, _coder.encode(envEntry));
            }
        }

        return true;
    }

    private synchronized boolean _start()
    {
        final ServiceThread thread = new ServiceThread(
            this,
            "CStore for [" + _storeAppImpl.getService().getServiceName() + "]");

        if (_thread.compareAndSet(null, thread)) {
            _logger.debug(ServiceMessages.STARTING_THREAD, thread.getName());
            thread.start();

            try {
                while (!_started) {
                    wait();
                }
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            }

            if (!_connected) {
                _thread.set(null);
            }
        }

        return _connected;
    }

    private synchronized boolean _supportsConnections()
    {
        final boolean supportsConnections = supportsConnections(_contextHandle);

        _logger
            .debug(
                StoreMessages.SUPPORTS_CONNECTIONS,
                Boolean.valueOf(supportsConnections));

        return supportsConnections;
    }

    private synchronized boolean _supportsObjects()
    {
        return supportedValueTypeCodes()
            .indexOf(Externalizer.ValueType.OBJECT.getCode()) >= 0;
    }

    private synchronized void _tearDown()
    {
        final ServiceThread thread = _thread.getAndSet(null);

        if ((thread != null) && Thread.holdsLock(this)) {
            _logger.debug(ServiceMessages.STOPPING_THREAD, thread.getName());
            thread.interrupt();

            try {
                while (_connected) {
                    wait();
                }
            } catch (final InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        if (_contextHandle != 0) {
            disconnect(_contextHandle);
            freeContext(_contextHandle);
            _contextHandle = 0;
        }

        _clientHandles.clear();
        _clientPoints.clear();
        _serverHandles.clear();
        _qualityNames.clear();
        _qualityCodes.clear();
        _supportsThreads = null;
        _supportsSubscribe = null;
        _supportsPull = null;

        if (_libraryHandle != 0) {
            closeLibrary(_libraryHandle);
            _libraryHandle = 0;
        }
    }

    /**
     * Closes a library.
     *
     * @param libraryHandle The library handle.
     */
    private native void closeLibrary(final long libraryHandle);

    /**
     * Connects.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return A status code.
     */
    private native int connect(final long contextHandle);

    /**
     * Returns a handle for the implementation context.
     *
     * @param libraryHandle The implementation library handle.
     * @param logLevel The log level for the implementation.
     *
     * @return The context handle.
     */
    private native long contextHandle(
            final long libraryHandle,
            final int logLevel);

    /**
     * Counts values.
     *
     * @param contextHandle The implementation context handle.
     * @param serverHandle The server handle for the point.
     * @param startTime The inclusive start time.
     * @param endTime The exclusive end time.
     * @param limit A limit for the number of values.
     * @param count A container for the count.
     *
     * @return A status code.
     */
    private native int count(
            final long contextHandle,
            final int serverHandle,
            final long startTime,
            final long endTime,
            final int limit,
            final AtomicLong count);

    /**
     * Deletes points values.
     *
     * @param contextHandle The implementation context handle.
     * @param serverHandles The server handles for the points.
     * @param times The times for the values.
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    private native int delete(
            final long contextHandle,
            final int[] serverHandles,
            final long[] times,
            final int[] statusCodes);

    /**
     * Delivers values.
     *
     * @param contextHandle The implementation context handle.
     * @param limit A limit for the number of values.
     * @param timeout A time limit in millis to wait for the first message
     *                (negative for infinite).
     * @param container A container for the values.
     *
     * @return A status code.
     */
    private native int deliver(
            final long contextHandle,
            final int limit,
            final long timeout,
            final Values container);

    /**
     * Disconnects.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return A status code.
     */
    private native int disconnect(final long contextHandle);

    /**
     * Exchanges handles.
     *
     * @param contextHandle The implementation context handle.
     * @param tags Tags for the points (input).
     * @param clientHandles Client handles for the points (input).
     * @param serverHandles Server handles for the points (output).
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    private native int exchangeHandles(
            final long contextHandle,
            final byte[][] tags,
            final int[] clientHandles,
            final int[] serverHandles,
            final int[] statusCodes);

    /**
     * Frees the implementation context.
     *
     * @param contextHandle The implementation context handle.
     */
    private native void freeContext(final long contextHandle);

    /**
     * Gets a code for a quality name.
     *
     * @param contextHandle The implementation context handle.
     * @param qualityName The quality name.
     *
     * @return A quality code (may be null).
     */
    private native Integer getQualityCode(
            final long contextHandle,
            final byte[] qualityName);

    /**
     * Gets a name for a quality code.
     *
     * @param contextHandle The implementation context handle.
     * @param qualityCode The quality code.
     *
     * @return A quality name (may be null).
     */
    private native byte[] getQualityName(
            final long contextHandle,
            final int qualityCode);

    /**
     * Gets a code for a state name.
     *
     * @param contextHandle The implementation context handle.
     * @param serverHandle The server handle for the point.
     * @param stateName The state name.
     *
     * @return A state code (may be null).
     */
    private native Integer getStateCode(
            final long contextHandle,
            final int serverHandle,
            final byte[] stateName);

    /**
     * Gets a name for a state code.
     *
     * @param contextHandle The implementation context handle.
     * @param serverHandle The server handle for the point.
     * @param stateCode The state code.
     *
     * @return A state name (may be null).
     */
    private native byte[] getStateName(
            final long contextHandle,
            final int serverHandle,
            final int stateCode);

    /**
     * Interrupts.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return A status code.
     */
    private native int interrupt(final long contextHandle);

    /**
     * Opens a library.
     *
     * @param libraryFilePath The library file path.
     *
     * @return A library handle.
     */
    private native long openLibrary(final byte[] libraryFilePath);

    /**
     * Puts an environment entry.
     *
     * @param contextHandle The implementation context handle.
     * @param entry The entry (key=value).
     *
     * @return A status code.
     */
    private native int putEnv(final long contextHandle, final byte[] entry);

    /**
     * Reads values.
     *
     * @param contextHandle The implementation context handle.
     * @param serverHandle The server handle for the point.
     * @param startTime The inclusive start time.
     * @param endTime The exclusive end time.
     * @param limit A limit for the number of values.
     * @param container A container for the values.
     *
     * @return A status code.
     */
    private native int read(
            final long contextHandle,
            final int serverHandle,
            final long startTime,
            final long endTime,
            final int limit,
            final Values container);

    /**
     * Releases handles.
     *
     * @param contextHandle The implementation context handle.
     * @param serverHandles Server handles for the points.
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    private native int releaseHandles(
            final long contextHandle,
            final int[] serverHandles,
            final int[] statusCodes);

    /**
     * Subscribes to point value events.
     *
     * @param contextHandle The implementation context handle.
     * @param serverHandles Server handles for the points.
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    private native int subscribe(
            final long contextHandle,
            final int[] serverHandles,
            final int[] statusCodes);

    /**
     * Returns a string of supported value type codes.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return The string of supported value type codes.
     */
    @Nonnull
    @CheckReturnValue
    private native byte[] supportedValueTypeCodes(final long contextHandle);

    /**
     * Asks if this implementation supports (multiple) connections.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return True if connections are supported.
     */
    private native boolean supportsConnections(final long contextHandle);

    /**
     * Asks if this implementation supports count.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return True if count is supported.
     */
    private native boolean supportsCount(final long contextHandle);

    /**
     * Asks if this implementation supports delete.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return True if delete is supported.
     */
    private native boolean supportsDelete(final long contextHandle);

    /**
     * Asks if this implementation supports deliver.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return True if deliver is supported.
     */
    private native boolean supportsDeliver(final long contextHandle);

    /**
     * Asks if this implementation supports pull queries.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return True if pull queries are supported.
     */
    private native boolean supportsPull(final long contextHandle);

    /**
     * Asks if this implementation supports subscriptions.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return True if subscriptions are supported.
     */
    private native boolean supportsSubscribe(final long contextHandle);

    /**
     * Asks if this implementation supports threads.
     *
     * @param contextHandle The implementation context handle.
     *
     * @return True if threads are supported.
     */
    private native boolean supportsThreads(final long contextHandle);

    /**
     * Unsubscribes from point value events.
     *
     * @param contextHandle The implementation context handle.
     * @param serverHandles Server handles for the points.
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    private native int unsubscribe(
            final long contextHandle,
            final int[] serverHandles,
            final int[] statusCodes);

    /**
     * Specifies the use of a charset.
     *
     * @param contextHandle The implementation context handle.
     * @param charsetName The charset name in US-ASCII.
     *
     * @return A status code.
     */
    private native int useCharset(
            final long contextHandle,
            final byte[] charsetName);

    /**
     * Writes points values.
     *
     * @param contextHandle The implementation context handle.
     * @param values The points values.
     * @param statusCodes The individual status codes.
     *
     * @return A status code.
     */
    private native int write(
            final long contextHandle,
            final Values values,
            final int[] statusCodes);

    /** Native library for this class. */
    public static final String LIBRARY = "rvpf-c-store";

    /**  */

    private static final boolean _IMPLEMENTED;
    private static final Logger _LOGGER = Logger.getInstance(CStore.class);
    private static final Integer _NO_CODE = Integer.valueOf(0);
    private static final String _NO_NAME = "";
    private static final Map<File, CStore> _SINGLETONS = new HashMap<>();
    private static final Charset _US_ASCII = Charset.forName("US-ASCII");

    static {
        boolean implemented;

        try {
            System.loadLibrary(LIBRARY);
            implemented = true;
        } catch (final UnsatisfiedLinkError exception) {
            implemented = false;
        }

        _IMPLEMENTED = implemented;
    }

    private final Map<UUID, Integer> _clientHandles = new HashMap<>();
    private final Map<Integer, UUID> _clientPoints = new HashMap<>();
    private final Coder _coder;
    private volatile boolean _connected;
    private final ElapsedTime _connectionRetryDelay;
    private volatile long _contextHandle;
    private final KeyedGroups _envProperties;
    private final File _implFile;
    private int _lastHandle;
    private volatile long _libraryHandle;
    private String _logID;
    private volatile Logger _logger;
    private final Map<String, Integer> _qualityCodes = new HashMap<>();
    private final Map<Integer, String> _qualityNames = new HashMap<>();
    private final SynchronousQueue<Runnable> _queue = new SynchronousQueue<>();
    private final Map<UUID, Integer> _serverHandles = new HashMap<>();
    private boolean _started;
    private volatile CStoreServiceAppImpl _storeAppImpl;
    private String _supportedValueTypeCodes;
    private Boolean _supportsCount;
    private Boolean _supportsDelete;
    private Boolean _supportsDeliver;
    private Boolean _supportsPull;
    private Boolean _supportsSubscribe;
    private Boolean _supportsThreads;
    private final AtomicReference<ServiceThread> _thread =
        new AtomicReference<>();

    /**
     * Task.
     *
     * @param <V> The result type returned by this Task.
     */
    static class Task<V>
        extends FutureTask<V>
    {
        Task(@Nonnull final Callable<V> callable)
        {
            super(callable);
        }

        /** {@inheritDoc}
         */
        @SuppressWarnings({"NakedNotify"})
        @Override
        protected synchronized void done()
        {
            notifyAll();
        }
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
