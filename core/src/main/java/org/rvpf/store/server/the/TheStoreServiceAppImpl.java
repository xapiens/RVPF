/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TheStoreServiceAppImpl.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.CheckReturnValue;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.tool.Require;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.StoreMetadataFilter;
import org.rvpf.store.server.StoreServiceAppImpl;
import org.rvpf.store.server.the.sql.SQLBackEnd;

/**
 * TheStore service application implementation.
 */
public final class TheStoreServiceAppImpl
    extends StoreServiceAppImpl
{
    /** {@inheritDoc}
     */
    @Override
    public TheStoreServer getServer()
    {
        return Require.notNull(_server);
    }

    /**
     * Gets the drop deleted indicator.
     *
     * @return The drop deleted indicator.
     */
    @CheckReturnValue
    public boolean isDropDeleted()
    {
        return _dropDeleted;
    }

    /**
     * Gets the snapshot mode indicator.
     *
     * @return The snapshot mode indicator.
     */
    @CheckReturnValue
    public boolean isSnapshot()
    {
        return _snapshot;
    }

    /** {@inheritDoc}
     */
    @Override
    public void start()
    {
        super.start();

        if (!_validatePointValueVersions()) {
            fail();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        stop();

        if (_server != null) {
            _server.tearDown();
            _server = null;
        }

        if (_backEnd != null) {
            _backEnd.tearDown();
            _backEnd = null;
        }

        super.tearDown();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean setUp()
    {
        if (!super.setUp(SERVER_PROPERTIES)) {
            return false;
        }

        // Accepts to be in snapshot mode.

        _snapshot = getServerProperties().getBoolean(SNAPSHOT_PROPERTY);

        if (isSnapshot()) {
            getThisLogger().info(StoreMessages.SNAPSHOT_MODE);
        }

        // Gets the drop deleted indicator.

        _dropDeleted = getServerProperties().getBoolean(DROP_DELETED_PROPERTY);

        if (isDropDeleted()) {
            getThisLogger().info(StoreMessages.DROP_DELETED);
        }

        // Creates and sets up the back end.

        final ClassDef backEndClassDef = getServerProperties()
            .getClassDef(BACK_END_CLASS_PROPERTY, DEFAULT_BACK_END);

        _backEnd = backEndClassDef.createInstance(BackEnd.class);

        if ((_backEnd == null) || !_backEnd.setUp(this)) {
            return false;
        }

        getThisLogger()
            .debug(StoreMessages.BACK_END, _backEnd.getClass().getSimpleName());

        // Sets up the notifier.

        if (!setUpNotifier()) {
            return false;
        }

        // Sets up and register the server.

        _server = new TheStoreServer();

        if (!_server.setUp(this, _backEnd)) {
            return false;
        }

        if (!registerServer(_server)) {
            return false;
        }

        // Sets up the archiver.

        if (!isSnapshot() && !setUpArchiver()) {
            return false;
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected StoreMetadataFilter storeMetadataFilter(
            final String storeName,
            final Collection<String> partnerNames)
    {
        return new _StoreMetadataFilter(storeName, partnerNames);
    }

    private boolean _validatePointValueVersions()
    {
        if (!_server.supportsPull()) {
            return true;
        }

        final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
            .newBuilder();
        final StoreValues[] responses;

        queryBuilder.setPull(true);
        queryBuilder.setCount(true);
        queryBuilder.setAfter(DateTime.now());

        try {
            responses = _server
                .select(
                    new StoreValuesQuery[] {queryBuilder.build(), },
                    Optional.empty());
        } catch (final SessionException exception) {
            return false;
        }

        final StoreValues response = responses[0];
        final Optional<Exception> exception = response.getException();

        if (exception.isPresent()) {
            getThisLogger()
                .error(
                    exception.get(),
                    BaseMessages.VERBATIM,
                    exception.get().getMessage());

            return false;
        }

        final long count = response.getCount();

        if (count > 0) {
            getThisLogger()
                .error(StoreMessages.VERSIONS_IN_FUTURE, Long.valueOf(count));

            return false;
        }

        return true;
    }

    /** Specifies an alternative to the SQL back end. */
    public static final String BACK_END_CLASS_PROPERTY = "backend.class";

    /** Default back end. */
    public static final ClassDef DEFAULT_BACK_END = new ClassDefImpl(
        SQLBackEnd.class);

    /** Drop deleted property. */
    public static final String DROP_DELETED_PROPERTY = "drop.deleted";

    /** Server properties. */
    public static final String SERVER_PROPERTIES = "store.server.the";

    /** Snapshot property. */
    public static final String SNAPSHOT_PROPERTY = "snapshot";

    private BackEnd _backEnd;
    private boolean _dropDeleted;
    private TheStoreServer _server;
    private boolean _snapshot;

    /**
     * TheStore metadata filter.
     */
    final class _StoreMetadataFilter
        extends StoreMetadataFilter
    {
        /**
         * Constructs an instance.
         *
         * @param storeName The store name.
         * @param partnerNames The name of the store partners (optional).
         */
        _StoreMetadataFilter(
                final String storeName,
                final Collection<String> partnerNames)
        {
            super(Optional.of(storeName), Optional.of(partnerNames));
        }

        /** {@inheritDoc}
         */
        @Override
        public boolean isPointNeeded(
                final PointEntity pointEntity)
            throws ValidationException
        {
            final boolean pointNeeded = super.isPointNeeded(pointEntity);

            if (pointNeeded) {
                final UUID pointUUID = pointEntity.getUUID().get();

                if (pointUUID.isDeleted()) {
                    throw new ValidationException(
                        StoreMessages.POINT_UUID_INCOMPATIBLE,
                        pointEntity);
                }
            }

            return pointNeeded;
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
