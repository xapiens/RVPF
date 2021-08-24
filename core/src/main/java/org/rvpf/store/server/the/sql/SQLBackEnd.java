/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SQLBackEnd.java 4115 2019-08-04 14:17:56Z SFB $
 */

package org.rvpf.store.server.the.sql;

import java.sql.SQLException;
import java.sql.Statement;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.ClassDef;
import org.rvpf.base.ClassDefImpl;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ServiceNotAvailableException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.security.Identity;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.service.ServiceMessages;
import org.rvpf.store.server.StoreCursor;
import org.rvpf.store.server.StoreMessages;
import org.rvpf.store.server.sql.StoreConnection;
import org.rvpf.store.server.the.BackEnd;
import org.rvpf.store.server.the.TheStoreServiceAppImpl;
import org.rvpf.store.server.the.sql.dialect.DialectSupport;
import org.rvpf.store.server.the.sql.dialect.GenericDialect;

/**
 * SQL back-end.
 */
public class SQLBackEnd
    implements BackEnd
{
    /** {@inheritDoc}
     */
    @Override
    public void beginUpdates()
    {
        _updater.getConnection().lock();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void close()
    {
        final Updater updater = _updater;

        if (updater != null) {
            final StoreConnection connection = updater.getConnection();

            connection.lock();

            try {
                updater.close();
            } finally {
                connection.unlock();
            }

            _updater = null;
        }

        synchronized (_respondersPool) {
            scheduleCleanerTask(Optional.empty());

            for (final Responder responder: _respondersPool) {
                responder.close();
            }

            _respondersPool.clear();
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void commit()
    {
        _updater.commit();
    }

    /** {@inheritDoc}
     */
    @Override
    public StoreValues createResponse(
            final StoreValuesQuery query,
            final Optional<Identity> identity)
    {
        final Responder responder = _getResponder();
        final TheStoreConnection connection = responder.getConnection();
        final StoreCursor cursor = new StoreCursor(
            query,
            _theStoreAppImpl.getServer(),
            responder);
        final StoreValues response;

        connection.lock();

        try {
            response = cursor.createResponse(identity);
            connection.commit();
        } finally {
            connection.unlock();
            _releaseResponder(responder);
        }

        return response;
    }

    /** {@inheritDoc}
     */
    @Override
    public int delete(final VersionedValue versionedValue)
    {
        return _updater.delete(versionedValue);
    }

    /** {@inheritDoc}
     */
    @Override
    public void endUpdates()
    {
        _updater.getConnection().unlock();
    }

    /** {@inheritDoc}
     */
    @Override
    public void open()
        throws ServiceNotAvailableException
    {
        final TheStoreConnection connection;

        try {
            connection = _dataSource.getConnection();
        } catch (final SQLException exception) {
            throw new ServiceNotAvailableException(
                exception,
                StoreMessages.CONNECTION_OPEN_FAILED,
                exception.getMessage());
        }

        // Creates the Archive (when requested).

        if (_create
                && !connection.hasTable(
                    connection.getSupport().getCatalogName(),
                    connection.getSupport().getSchemaName(),
                    connection.getSupport().getTableName())) {
            try {
                final List<String> sqlStrings = connection
                    .getSupport()
                    .getCreateTableSQL();
                final Statement statement = connection.createStatement();

                for (final String sql: sqlStrings) {
                    _LOGGER.trace(StoreMessages.SQL, sql);
                    statement.execute(sql);
                }

                statement.close();
            } catch (final SQLException exception) {
                throw new ServiceNotAvailableException(
                    exception,
                    StoreMessages.STORAGE_CREATE_FAILED,
                    connection.getSupport().getTableName(),
                    exception.getMessage());
            }

            _LOGGER
                .info(
                    StoreMessages.STORAGE_CREATED,
                    connection.getSupport().getTableName());
        }

        _updater = new Updater(connection, _theStoreAppImpl.isDropDeleted());
    }

    /** {@inheritDoc}
     */
    @Override
    public int purge(final UUID[] pointUUIDs, final TimeInterval timeInterval)
    {
        int purged = 0;

        for (final UUID pointUUID: pointUUIDs) {
            purged += _updater.purge(pointUUID, timeInterval);
        }

        return purged;
    }

    /** {@inheritDoc}
     */
    @Override
    public void rollback()
    {
        _updater.rollback();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final TheStoreServiceAppImpl theStoreAppImpl)
    {
        _theStoreAppImpl = theStoreAppImpl;

        // Creates the DataSource and Connection.

        final KeyedGroups serverProperties = theStoreAppImpl
            .getServerProperties();
        final KeyedGroups connectionProperties = serverProperties
            .getGroup(SQL_CONNECTION_PROPERTIES);

        Optional<? extends ClassDef> dialectClassDef = connectionProperties
            .getClassDef(DIALECT_CLASS_PROPERTY, Optional.empty());

        if (!dialectClassDef.isPresent()) {
            dialectClassDef = theStoreAppImpl
                .getConfig()
                .getClassDef(DEFAULT_DIALECT_NAME, Optional.empty());

            if (!dialectClassDef.isPresent()) {
                if (connectionProperties.isMissing()) {
                    _LOGGER
                        .error(
                            ServiceMessages.MISSING_PROPERTIES,
                            SQL_CONNECTION_PROPERTIES);

                    return false;
                }

                dialectClassDef = Optional.of(GENERIC_DIALECT);
            }
        }

        _support = dialectClassDef.get().createInstance(DialectSupport.class);

        if ((_support == null)
                || !_support.setUp(
                    theStoreAppImpl,
                    theStoreAppImpl.getStoreDataDir(),
                    theStoreAppImpl.getEntityName().get())) {
            return false;
        }

        _dataSource = new TheStoreDataSource(_support);

        if (!_dataSource
            .setUp(
                connectionProperties,
                theStoreAppImpl.getStoreDataDir(),
                theStoreAppImpl.getEntityName().get())) {
            return false;
        }

        _selectStatementsLimit = connectionProperties
            .getInt(STATEMENTS_PROPERTY, DEFAULT_SELECT_STATEMENTS_LIMIT);

        if (_selectStatementsLimit != DEFAULT_SELECT_STATEMENTS_LIMIT) {
            _LOGGER
                .debug(
                    StoreMessages.SELECT_STATEMENTS_LIMIT,
                    String.valueOf(_selectStatementsLimit));
        }

        _respondersKeep = serverProperties
            .getElapsed(
                RESPONDER_KEEP_PROPERTY,
                Optional.of(DEFAULT_RESPONDER_KEEP),
                Optional.of(DEFAULT_RESPONDER_KEEP))
            .get();

        if (!DEFAULT_RESPONDER_KEEP.equals(_respondersKeep)) {
            _LOGGER.debug(StoreMessages.RESPONDER_KEEP, _respondersKeep);
        }

        _create = serverProperties.getBoolean(SQL_CREATE_PROPERTY);

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsCount()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean supportsPurge()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public void tearDown()
    {
        if (_dataSource != null) {
            _dataSource.tearDown();
            _dataSource = null;
        }

        if (_support != null) {
            _support.tearDown();
            _support = null;
        }

        _theStoreAppImpl = null;
    }

    /** {@inheritDoc}
     */
    @Override
    public void update(final VersionedValue versionedValue)
    {
        _updater.update(versionedValue);
    }

    /**
     * Schedules the cleaner task.
     *
     * @param delay The schedule delay (empty cancels).
     */
    void scheduleCleanerTask(@Nonnull final Optional<ElapsedTime> delay)
    {
        final Optional<Timer> timer = _theStoreAppImpl.getService().getTimer();

        if (delay.isPresent() && timer.isPresent()) {
            final Deque<Responder> respondersPool = _respondersPool;
            final ElapsedTime respondersKeep = _respondersKeep;

            _cleanerTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    synchronized (respondersPool) {
                        scheduleCleanerTask(Optional.empty());

                        while (!respondersPool.isEmpty()) {
                            final Responder responder = respondersPool
                                .peekLast();
                            final ElapsedTime kept = DateTime
                                .now()
                                .sub(responder.getLastUse().get());

                            if (kept.compareTo(respondersKeep) >= 0) {
                                respondersPool.removeFirst();
                                responder.close();
                            } else {
                                scheduleCleanerTask(
                                    Optional.of(respondersKeep.sub(kept)));

                                break;
                            }
                        }
                    }
                }
            };

            timer.get().schedule(_cleanerTask, delay.get().toMillis());
        } else if (_cleanerTask != null) {
            _cleanerTask.cancel();
            _cleanerTask = null;
        }
    }

    @Nonnull
    @CheckReturnValue
    private Responder _getResponder()
    {
        final Responder responder;

        synchronized (_respondersPool) {
            if (_respondersPool.isEmpty()) {
                final TheStoreConnection connection;

                try {
                    connection = _dataSource.getConnection();
                } catch (final SQLException exception) {
                    throw new RuntimeException(exception);
                }

                responder = new Responder(
                    connection,
                    _theStoreAppImpl.getService().getMetadata(),
                    _theStoreAppImpl.getBackEndLimit(),
                    _selectStatementsLimit);
            } else {
                responder = _respondersPool.removeFirst();
            }
        }

        return responder;
    }

    private void _releaseResponder(final Responder responder)
    {
        synchronized (_respondersPool) {
            if (_updater != null) {
                responder.setLastUse(DateTime.now());
                _respondersPool.addFirst(responder);

                if (_cleanerTask == null) {
                    scheduleCleanerTask(Optional.of(_respondersKeep));
                }
            } else {
                responder.close();
            }
        }
    }

    /** Default dialect name. */
    public static final String DEFAULT_DIALECT_NAME = "TheStoreH2Dialect";

    /** Default responder keep time. */
    public static final ElapsedTime DEFAULT_RESPONDER_KEEP = ElapsedTime
        .fromMillis(300000);

    /** Default select statements limit. */
    public static final int DEFAULT_SELECT_STATEMENTS_LIMIT = 10;

    /** Names the class to use for the database specific SQL dialect. */
    public static final String DIALECT_CLASS_PROPERTY = "dialect.class";

    /** Generic dialect (used if the default dialect is unknown). */
    public static final ClassDef GENERIC_DIALECT = new ClassDefImpl(
        GenericDialect.class);

    /** Responder keep property. */
    public static final String RESPONDER_KEEP_PROPERTY = "responder.keep";

    /** Properties group used to configure the JDBC connection. */
    public static final String SQL_CONNECTION_PROPERTIES = "sql.connection";

    /** The database table will be created if necessary. */
    public static final String SQL_CREATE_PROPERTY = "sql.create";

    /** Statements property. */
    public static final String STATEMENTS_PROPERTY = "statements";

    /**  */

    private static final Logger _LOGGER = Logger.getInstance(SQLBackEnd.class);

    private TimerTask _cleanerTask;
    private boolean _create;
    private TheStoreDataSource _dataSource;
    private TheStoreServiceAppImpl _theStoreAppImpl;
    private ElapsedTime _respondersKeep;
    private final Deque<Responder> _respondersPool = new LinkedList<>();
    private int _selectStatementsLimit;
    private DialectSupport _support;
    private volatile Updater _updater;
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
