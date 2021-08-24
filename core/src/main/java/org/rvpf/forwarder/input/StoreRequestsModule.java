/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreRequestsModule.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.forwarder.input;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.store.Store;
import org.rvpf.base.store.StoreAccessException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.PointValue;
import org.rvpf.forwarder.BatchControl;
import org.rvpf.forwarder.ForwarderMessages;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.StoreEntity;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreMetadataFilter;
import org.rvpf.store.server.StoreServiceAppImpl;

/**
 * Store requests module.
 */
@NotThreadSafe
public final class StoreRequestsModule
    extends InputModule
{
    /** {@inheritDoc}
     */
    @Override
    public boolean needsMetadata()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean onMetadataRefreshed()
    {
        getInput().close();

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp(final KeyedGroups moduleProperties)
    {
        final KeyedGroups storeProperties = moduleProperties
            .getGroup(STORE_PROPERTIES);
        final String storeName = storeProperties
            .getString(NAME_PROPERTY, Optional.of(Store.DEFAULT_STORE_NAME))
            .get();

        getThisLogger().info(ForwarderMessages.INPUT_STORE, storeName);

        final StoreMetadataFilter filter = new StoreMetadataFilter(
            Optional.of(storeName),
            Optional.empty());

        if (!loadMetadata(filter)) {
            return false;
        }

        final Optional<StoreEntity> storeEntity = filter.getStoreEntity();

        if (!storeEntity.isPresent()) {
            getThisLogger().error(ServiceMessages.STORE_NOT_FOUND, storeName);

            return false;
        }

        setInput(new _Input(storeEntity.get()));

        return super.setUp(moduleProperties);
    }

    /**
     * Gets the metadata.
     *
     * @return The metadata.
     */
    @Nonnull
    @CheckReturnValue
    Metadata _getMetadata()
    {
        return getMetadata();
    }

    /**
     * Reads the mark.
     *
     * @return The mark (empty when not found).
     *
     * @throws IOException On mark file errors.
     */
    @Nonnull
    @CheckReturnValue
    Optional<StoreValuesQuery.Mark> _readMark()
        throws IOException
    {
        final File file = new File(_controlDirectory, MARK_FILE_NAME);
        final BufferedReader reader;

        try {
            reader = new BufferedReader(
                new InputStreamReader(
                    new FileInputStream(file),
                    StandardCharsets.UTF_8));
        } catch (final FileNotFoundException exception) {
            return Optional.empty();
        }

        try {
            final DateTime stamp = DateTime.fromString(_readMarkLine(reader));
            final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
                .newBuilder();
            final StoreValuesQuery query = queryBuilder.setPull(true).build();

            return Optional.of(query.newMark(Optional.empty(), stamp, 0));
        } finally {
            reader.close();
        }
    }

    /**
     * Sets up the control directory.
     *
     * @param controlDirPath The control directory path.
     *
     * @return True on success.
     */
    @CheckReturnValue
    boolean _setUpControlDir(@Nonnull final String controlDirPath)
    {
        _controlDirectory = new File(controlDirPath);

        if (!_controlDirectory.isDirectory()) {
            if (_controlDirectory.mkdirs()) {
                getThisLogger()
                    .info(
                        ForwarderMessages.CONTROL_DIRECTORY_CREATED,
                        _controlDirectory);
            } else {
                getThisLogger()
                    .error(
                        ForwarderMessages.CONTROL_DIRECTORY_CREATE_FAILED,
                        _controlDirectory.getAbsolutePath());

                return false;
            }
        } else {
            getThisLogger()
                .info(
                    ForwarderMessages.CONTROL_DIRECTORY,
                    _controlDirectory.getAbsolutePath());
        }

        return true;
    }

    /**
     * Writes the mark.
     *
     * @param mark The mark.
     *
     * @throws IOException On mark file errors.
     */
    void _writeMark(
            @Nonnull final StoreValuesQuery.Mark mark)
        throws IOException
    {
        final File file = new File(_controlDirectory, MARK_FILE_NAME);
        final BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(file),
                StandardCharsets.UTF_8));

        try {
            writer.write(mark.getStamp().toFullString());
            writer.newLine();
        } finally {
            writer.close();
        }
    }

    private static String _readMarkLine(
            @Nonnull final BufferedReader reader)
        throws IOException
    {
        for (;;) {
            String line = reader.readLine();

            if (line == null) {
                throw new EOFException();
            }

            line = line.trim();

            if ((line.length() > 0) && !line.startsWith("#")) {
                return line;
            }
        }
    }

    /** The module's control directory. */
    public static final String CONTROL_DIR_PROPERTY = "control.dir";

    /** The deliver property. */
    public static final String DELIVER_PROPERTY = "deliver";

    /** The mark file name. */
    public static final String MARK_FILE_NAME = "Mark";

    /** The name property. */
    public static final String NAME_PROPERTY = "name";

    /** The notices property. */
    public static final String NOTICES_PROPERTY = "notices";

    /** The point property. */
    public static final String POINT_PROPERTY = "point";

    /** The pull property. */
    public static final String PULL_PROPERTY = "pull";

    /** The store properties. */
    public static final String STORE_PROPERTIES = "store";

    private File _controlDirectory;

    private enum _InputState
    {
        PULL_FIRST,
        DELIVER,
        PULL_ALL;
    }

    private final class _Input
        extends AbstractInput
    {
        _Input(@Nonnull final StoreEntity storeEntity)
        {
            _storeEntity = storeEntity;
        }

        /** {@inheritDoc}
         */
        @Override
        public void close()
        {
            final Store store = _store;

            if (store != null) {
                _store = null;

                if (_subscribed != null) {
                    try {
                        store.unsubscribe(_subscribed);
                    } catch (final StoreAccessException exception) {
                        getThisLogger()
                            .debug(
                                ForwarderMessages.UNSUBSCRIBE_CLOSE_FAILED,
                                exception);
                    }
                }

                store.close();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean commit()
        {
            super.commit();

            getTraces().commit();

            if (_mark.isPresent()) {
                try {
                    _writeMark(_mark.get());
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }
            }

            return super.commit();
        }

        /** {@inheritDoc}
         */
        @Override
        public String getDisplayName()
        {
            return "Pull Notifier input";
        }

        /** {@inheritDoc}
         */
        @Override
        public String getSourceName()
        {
            return _storeEntity.getName().get();
        }

        /** {@inheritDoc}
         */
        @Override
        public Optional<Serializable[]> input(
                final BatchControl batchControl)
            throws InterruptedException
        {
            final Store store = _store;

            if (store == null) {
                return Optional.empty();
            }

            final int limit = batchControl.getLimit();
            final StoreValues response;

            try {
                switch (_state) {
                    case DELIVER: {
                        response = store.deliver(limit, -1);

                        break;
                    }
                    case PULL_ALL: {
                        final StoreValuesQuery query;

                        if (_mark.isPresent()) {
                            query = _mark.get().createQuery();
                        } else {
                            final StoreValuesQuery.Builder queryBuilder =
                                StoreValuesQuery
                                    .newBuilder();

                            queryBuilder.setPull(true);
                            queryBuilder.setLimit(limit);
                            query = queryBuilder.build();
                        }

                        response = store.pull(query, -1);

                        if (response != null) {
                            _mark = response.mark(limit);
                        }

                        break;
                    }
                    case PULL_FIRST: {
                        final StoreValuesQuery query;

                        if (_mark.isPresent()) {
                            query = _mark.get().createQuery();
                        } else {
                            final StoreValuesQuery.Builder queryBuilder =
                                StoreValuesQuery
                                    .newBuilder();

                            queryBuilder.setPull(true);
                            queryBuilder.setLimit(limit);
                            query = queryBuilder.build();
                        }

                        response = store.pull(query, 0);

                        if (response != null) {
                            if (response.isEmpty()) {
                                _state = _InputState.DELIVER;

                                return input(batchControl);
                            }

                            _mark = response.mark(limit);
                        }

                        break;
                    }
                    default: {
                        throw Require.failure();
                    }
                }
            } catch (final StoreAccessException exception) {
                if (exception.getCause() instanceof ServiceClosedException) {
                    if (_store != null) {
                        getThisLogger()
                            .warn(ServiceMessages.STORE_CLOSED, store);
                    }
                } else {
                    getThisLogger()
                        .warn(
                            exception,
                            ServiceMessages.STORE_ACCESS_FAILED,
                            store,
                            exception.getMessage());
                }

                return Optional.empty();
            }

            final ListIterator<PointValue> iterator = response.iterator();

            while (iterator.hasNext()) {
                PointValue pointValue = iterator.next();
                final String pointName = pointValue.getPointName().get();

                pointValue = pointValue.reset();
                pointValue.setPointName(pointName);
                iterator.set(pointValue);
                getTraces().add(pointValue);
            }

            final List<PointValue> responseValues = response.getPointValues();

            return Optional
                .of(
                    responseValues
                        .toArray(new PointValue[responseValues.size()]));
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isClosed()
        {
            return _store == null;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isReliable()
        {
            return _badConfig;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean open()
        {
            if (!_storeEntity.setUp(_getMetadata())) {
                return false;
            }

            final Optional<? extends Store> store = _storeEntity.getStore();

            if (!store.isPresent()) {
                return false;
            }

            _store = store.get();

            final String[] pointKeys = _pointsKeys;
            boolean notices = _notices;

            try {
                if (_pull) {
                    if (!_store.supportsPull()) {
                        return _error(
                            ForwarderMessages.PULL_NOT_SUPPORTED,
                            _storeEntity.getName());
                    }

                    if (_deliver) {
                        if (!_store.supportsDeliver()) {
                            return _error(
                                ForwarderMessages.DELIVER_NOT_SUPPORTED,
                                _storeEntity.getName());
                        }

                        notices |= pointKeys == null;
                        _state = _InputState.PULL_FIRST;
                    } else {
                        _state = _InputState.PULL_ALL;
                    }
                } else if (_deliver) {
                    if (!_store.supportsDeliver()) {
                        return _error(
                            ForwarderMessages.DELIVER_NOT_SUPPORTED,
                            _storeEntity.getName());
                    }

                    notices |= pointKeys == null;
                    _state = _InputState.DELIVER;
                } else if (_store.supportsPull()) {
                    if (_controlDirPath == null) {
                        getThisLogger()
                            .error(
                                BaseMessages.MISSING_PROPERTY,
                                CONTROL_DIR_PROPERTY);
                        _badConfig = true;

                        return false;
                    }

                    if (!_setUpControlDir(_controlDirPath)) {
                        _badConfig = true;

                        return false;
                    }

                    try {
                        _mark = _readMark();
                    } catch (final IOException exception) {
                        return _error(
                            ForwarderMessages.MARK_READ_FAILED,
                            exception.getMessage());
                    }

                    if (!_mark.isPresent()) {
                        getThisLogger().info(ForwarderMessages.MARK_MISSING);
                    }

                    if (_store.supportsDeliver()) {
                        _state = _InputState.PULL_FIRST;
                        notices |= pointKeys == null;
                    } else {
                        _state = _InputState.PULL_ALL;
                    }
                } else if (_store.supportsDeliver()) {
                    _state = _InputState.DELIVER;
                    notices |= pointKeys == null;
                } else {
                    return _error(
                        ForwarderMessages.PULL_NOR_DELIVER,
                        _storeEntity.getName());
                }
            } catch (final StoreAccessException exception) {
                return _error(
                    ServiceMessages.STORE_ACCESS_FAILED,
                    _store,
                    exception.getMessage());
            }

            final UUID[] points;

            if (notices) {
                final Collection<Point> noticesFilter = StoreServiceAppImpl
                    .makeNoticesFilter(_getMetadata());
                final Iterator<Point> iterator = noticesFilter.iterator();

                points = new UUID[noticesFilter.size()];

                for (int i = 0; i < points.length; ++i) {
                    points[i] = iterator.next().getUUID().get();
                }
            } else {
                if (pointKeys != null) {
                    points = new UUID[pointKeys.length];

                    for (int i = 0; i < points.length; ++i) {
                        final Optional<Point> point = _getMetadata()
                            .getPoint(pointKeys[i]);

                        if (point.isPresent()) {
                            points[i] = point.get().getUUID().get();
                        }
                    }
                } else {
                    points = null;
                }
            }

            if (points != null) {
                boolean success = false;

                try {
                    if (_store.supportsSubscribe()) {
                        success = _store.subscribe(points);
                    } else {
                        _error(
                            ForwarderMessages.SUBSCRIBE_UNSUPPORTED,
                            _storeEntity.getName());
                    }
                } catch (final StoreAccessException exception) {
                    getThisLogger()
                        .warn(ForwarderMessages.SUBSCRIBE_FAILED, exception);
                } finally {
                    if (!success) {
                        _store.close();
                        _store = null;

                        return false;
                    }
                }

                final List<UUID> subscribed = new ArrayList<UUID>(
                    points.length);

                for (int i = 0; i < points.length; ++i) {
                    subscribed.add(points[i]);
                }

                _subscribed = subscribed.toArray(new UUID[subscribed.size()]);
            } else {
                _subscribed = null;
            }

            return true;
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean setUp(final KeyedGroups moduleProperties)
        {
            if (!super.setUp(moduleProperties)) {
                return false;
            }

            _controlDirPath = moduleProperties
                .getString(CONTROL_DIR_PROPERTY)
                .orElse(null);

            _pull = moduleProperties.getBoolean(PULL_PROPERTY);
            _deliver = moduleProperties.getBoolean(DELIVER_PROPERTY);
            _notices = moduleProperties.getBoolean(NOTICES_PROPERTY);

            _pointsKeys = moduleProperties.getStrings(POINT_PROPERTY);

            if (_pointsKeys.length == 0) {
                _pointsKeys = null;
            }

            return true;
        }

        private boolean _error(
                final Messages.Entry entry,
                final Object... params)
        {
            getThisLogger().error(entry, params);
            _badConfig = true;

            return false;
        }

        private boolean _badConfig;
        private String _controlDirPath;
        private boolean _deliver;
        private Optional<StoreValuesQuery.Mark> _mark = Optional.empty();
        private boolean _notices;
        private String[] _pointsKeys;
        private boolean _pull;
        private _InputState _state;
        private volatile Store _store;
        private final StoreEntity _storeEntity;
        private UUID[] _subscribed;
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
