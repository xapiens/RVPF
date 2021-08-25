/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StoreConnection.java 4020 2019-05-23 14:37:22Z SFB $
 */

package org.rvpf.jdbc;

import java.net.URI;
import java.net.URISyntaxException;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.rmi.RegistryEntry;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.rmi.SessionConnectFailedException;
import org.rvpf.base.rmi.SessionException;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.store.StoreSessionProxy;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;

/**
 * Store Connection.
 */
public final class StoreConnection
    implements Connection
{
    /**
     * Constructs an instance.
     *
     * @param driver The driver instance.
     * @param url The store connection string.
     * @param info Additional informations.
     *
     * @throws SQLException When the connect fails.
     */
    StoreConnection(
            @Nonnull final StoreDriver driver,
            @Nonnull final String url,
            @Nonnull final Properties info)
        throws SQLException
    {
        _driver = driver;

        try {
            _serverURI = new URI(url);
        } catch (final URISyntaxException exception) {
            throw JDBCMessages.BAD_CONNECTION_URL.exception(url);
        }

        final String user = info.getProperty(StoreDriver.USER_PROPERTY, null);
        final Object passwordObject = info.get(StoreDriver.PASSWORD_PROPERTY);
        final char[] password = (passwordObject instanceof String)
            ? ((String) passwordObject)
                .toCharArray(): (char[]) passwordObject;
        final RegistryEntry registryEntry = RegistryEntry
            .newBuilder()
            .setServerURI(_serverURI)
            .build();

        _storeSession = (StoreSessionProxy) StoreSessionProxy
            .newBuilder()
            .setRegistryEntry(registryEntry)
            .setLoginUser(Optional.ofNullable(user))
            .setLoginPassword(Optional.ofNullable(password))
            .setClientName("JDBC")
            .setClientLogger(_LOGGER)
            .build();

        if (_storeSession == null) {
            throw JDBCMessages.CONNECT_FAILED.exception();
        }

        _user = _storeSession.hasLoginInfo()? user: null;

        try {
            _storeSession.connect();
        } catch (final SessionConnectFailedException exception) {
            throw JDBCMessages.CONNECT_FAILED.wrap(exception);
        }

        _autoCommitLimit = Integer
            .parseInt(
                info
                    .getProperty(
                            StoreDriver.AUTO_COMMIT_LIMIT_PROPERTY,
                                    _DEFAULT_AUTO_COMMIT_LIMIT));
        _LOGGER
            .debug(
                JDBCMessages.AUTO_COMMIT_LIMIT,
                String.valueOf(_autoCommitLimit));
    }

    /** {@inheritDoc}
     */
    @Override
    public void abort(final Executor executor)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void clearWarnings()
        throws SQLException
    {
        _assertOpen();

        _warnings = null;
    }

    /** {@inheritDoc}
     */
    @Override
    public void close()
        throws SQLException
    {
        if (_closed.compareAndSet(false, true)) {
            synchronized (_statements) {
                while (!_statements.isEmpty()) {
                    _statements.iterator().next().close();
                }
            }

            synchronized (_resultSets) {
                while (!_resultSets.isEmpty()) {
                    _resultSets.iterator().next().close();
                }
            }

            synchronized (_storeSession) {
                _storeSession.disconnect();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void commit()
        throws SQLException
    {
        _assertOpen();

        if (_autoCommit) {
            throw JDBCMessages.AUTOCOMMIT.exception();
        }

        if (_transactionFailed) {
            throw JDBCMessages.TRANSACTION_FAILED.exception();
        }

        _closeNotHeld();

        if (!_updates.isEmpty()) {
            final Collection<Exception> exceptions;
            int warnings = 0;

            _LOGGER
                .debug(
                    JDBCMessages.COMMITTING,
                    String.valueOf(_updates.size()),
                    Integer.valueOf(_updates.size()));

            try {
                exceptions = _storeSession.update(_updates);
            } catch (final SessionException exception) {
                throw JDBCMessages.COMMIT_FAILED.wrap(exception);
            }

            _updates.clear();

            if (exceptions == null) {
                throw JDBCMessages.COMMIT_FAILED
                    .wrap(new ServiceClosedException());
            }

            for (final Exception exception: exceptions) {
                if (exception != null) {
                    _addWarning(exception.getMessage());
                    ++warnings;
                }
            }

            if (warnings > 0) {
                _LOGGER
                    .debug(
                        JDBCMessages.COMMIT_WARNINGS,
                        String.valueOf(warnings));
            } else {
                _LOGGER.debug(JDBCMessages.COMMITTED);
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public Array createArrayOf(
            final String typeName,
            final Object[] elements)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public Blob createBlob()
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public Clob createClob()
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public NClob createNClob()
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public SQLXML createSQLXML()
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public Statement createStatement()
        throws SQLException
    {
        return createStatement(
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY,
            _holdability);
    }

    /** {@inheritDoc}
     */
    @Override
    public Statement createStatement(
            final int type,
            final int concurrency)
        throws SQLException
    {
        return createStatement(type, concurrency, _holdability);
    }

    /** {@inheritDoc}
     */
    @Override
    public Statement createStatement(
            final int type,
            final int concurrency,
            final int holdability)
        throws SQLException
    {
        _assertOpen();

        if ((concurrency != ResultSet.CONCUR_READ_ONLY)
                && (concurrency != ResultSet.CONCUR_UPDATABLE)) {
            throw JDBCMessages.RESULT_SET_CONCURRENCY_NOT_SUPPORTED
                .exception(String.valueOf(concurrency));
        }

        final StoreStatement statement;

        synchronized (_statements) {
            statement = new StoreStatement(
                this,
                type,
                concurrency,
                holdability);
            _statements.add(statement);
        }

        return statement;
    }

    /** {@inheritDoc}
     */
    @Override
    public Struct createStruct(
            final String typeName,
            final Object[] attributes)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized boolean getAutoCommit()
        throws SQLException
    {
        _assertOpen();

        return _autoCommit;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getCatalog()
        throws SQLException
    {
        _assertOpen();

        return null;
    }

    /** {@inheritDoc}
     */
    @Override
    public Properties getClientInfo()
        throws SQLException
    {
        _assertOpen();

        return new Properties();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getClientInfo(final String name)
        throws SQLException
    {
        return null;
    }

    /** {@inheritDoc}
     */
    @Override
    public int getHoldability()
        throws SQLException
    {
        _assertOpen();

        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    /** {@inheritDoc}
     */
    @Override
    public DatabaseMetaData getMetaData()
        throws SQLException
    {
        _assertOpen();

        return new StoreDatabaseMetaData(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public int getNetworkTimeout()
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public String getSchema()
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public int getTransactionIsolation()
        throws SQLException
    {
        _assertOpen();

        return TRANSACTION_READ_COMMITTED;
    }

    /** {@inheritDoc}
     */
    @Override
    public Map<String, Class<?>> getTypeMap()
        throws SQLException
    {
        _assertOpen();

        return Collections.emptyMap();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized SQLWarning getWarnings()
        throws SQLException
    {
        _assertOpen();

        return _warnings;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isClosed()
        throws SQLException
    {
        return _closed.get();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isReadOnly()
        throws SQLException
    {
        _assertOpen();

        return _readOnly;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isValid(final int timeout)
        throws SQLException
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isWrapperFor(final Class<?> iface)
        throws SQLException
    {
        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public String nativeSQL(final String sql)
        throws SQLException
    {
        _assertOpen();

        return null;
    }

    /** {@inheritDoc}
     */
    @Override
    public CallableStatement prepareCall(final String sql)
        throws SQLException
    {
        _assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public CallableStatement prepareCall(
            final String sql,
            final int resultSetType,
            final int resultSetConcurrency)
        throws SQLException
    {
        _assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public CallableStatement prepareCall(
            final String sql,
            final int resultSetType,
            final int resultSetConcurrency,
            final int resultSetHoldability)
        throws SQLException
    {
        _assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(
            final String sql)
        throws SQLException
    {
        return prepareStatement(
            sql,
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY,
            _holdability);
    }

    /** {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(
            final String sql,
            final int autoGeneratedKeys)
        throws SQLException
    {
        return prepareStatement(sql);
    }

    /** {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(
            final String sql,
            final int[] columnIndexes)
        throws SQLException
    {
        return prepareStatement(sql);
    }

    /** {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(
            final String sql,
            final String[] columnNames)
        throws SQLException
    {
        return prepareStatement(sql);
    }

    /** {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(
            final String sql,
            final int type,
            final int concurrency)
        throws SQLException
    {
        return prepareStatement(sql, type, concurrency, _holdability);
    }

    /** {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(
            final String sql,
            final int type,
            final int concurrency,
            final int holdability)
        throws SQLException
    {
        _assertOpen();

        if ((concurrency != ResultSet.CONCUR_READ_ONLY)
                && (concurrency != ResultSet.CONCUR_UPDATABLE)) {
            throw JDBCMessages.RESULT_SET_CONCURRENCY_NOT_SUPPORTED
                .exception(String.valueOf(concurrency));
        }

        final PreparedStoreStatement statement;

        synchronized (_statements) {
            statement = new PreparedStoreStatement(
                this,
                type,
                concurrency,
                holdability,
                sql);
            _statements.add(statement);
        }

        return statement;
    }

    /** {@inheritDoc}
     */
    @Override
    public void releaseSavepoint(final Savepoint savepoint)
        throws SQLException
    {
        _assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void rollback()
        throws SQLException
    {
        _assertOpen();

        if (_autoCommit) {
            throw JDBCMessages.AUTOCOMMIT.exception();
        }

        _closeNotHeld();

        final int rolledBack = _updates.size();

        _updates.clear();

        if (rolledBack > 0) {
            _LOGGER
                .debug(
                    JDBCMessages.ROLLED_BACK,
                    String.valueOf(rolledBack),
                    Integer.valueOf(rolledBack));
        }

        _transactionFailed = false;
    }

    /** {@inheritDoc}
     */
    @Override
    public void rollback(final Savepoint savepoint)
        throws SQLException
    {
        _assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized void setAutoCommit(
            final boolean autoCommit)
        throws SQLException
    {
        _assertOpen();

        if (!_autoCommit) {
            commit();
        }

        _autoCommit = autoCommit;
        _LOGGER.debug(JDBCMessages.AUTO_COMMIT, Boolean.valueOf(autoCommit));
    }

    /** {@inheritDoc}
     */
    @Override
    public void setCatalog(final String catalog)
        throws SQLException
    {
        _assertOpen();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setClientInfo(
            final Properties properties)
        throws SQLClientInfoException {}

    /** {@inheritDoc}
     */
    @Override
    public void setClientInfo(
            final String name,
            final String value)
        throws SQLClientInfoException {}

    /** {@inheritDoc}
     */
    @Override
    public void setHoldability(final int holdability)
        throws SQLException
    {
        _assertOpen();

        if ((holdability != ResultSet.CLOSE_CURSORS_AT_COMMIT)
                && (holdability != ResultSet.HOLD_CURSORS_OVER_COMMIT)) {
            throw JDBCMessages.RESULT_SET_HOLDABILITY_NOT_SUPPORTED
                .exception(String.valueOf(holdability));
        }

        if (holdability != _holdability) {
            _holdability = holdability;
            _LOGGER
                .debug(
                    JDBCMessages.CLOSE_CURSORS,
                    Boolean
                        .valueOf(_holdability
                        == ResultSet.CLOSE_CURSORS_AT_COMMIT));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void setNetworkTimeout(
            final Executor executor,
            final int milliseconds)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setReadOnly(final boolean readOnly)
        throws SQLException
    {
        _assertOpen();

        _readOnly = readOnly;
    }

    /** {@inheritDoc}
     */
    @Override
    public Savepoint setSavepoint()
        throws SQLException
    {
        _assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public Savepoint setSavepoint(final String name)
        throws SQLException
    {
        _assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setSchema(final String schema)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setTransactionIsolation(final int level)
        throws SQLException
    {
        _assertOpen();

        if (level != TRANSACTION_READ_COMMITTED) {
            throw JDBCMessages.TRANSACTION_LEVEL_NOT_SUPPORTED
                .exception(String.valueOf(level));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public void setTypeMap(
            final Map<String, Class<?>> typeMap)
        throws SQLException
    {
        _assertOpen();

        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /** {@inheritDoc}
     */
    @Override
    public <T> T unwrap(final Class<T> iface)
        throws SQLException
    {
        throw JDBCMessages.FEATURE_NOT_SUPPORTED.exception();
    }

    /**
     * Adds point values for update.
     *
     * @param pointValues The point values.
     */
    synchronized void addUpdates(final PointValue[] pointValues)
    {
        _updates.addAll(Arrays.asList(pointValues));
    }

    /**
     * Creates a store result set.
     *
     * @return The store result set.
     *
     * @throws SQLException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    DefaultResultSet createResultSet()
        throws SQLException
    {
        final DefaultResultSet resultSet = new DefaultResultSet(this);

        _resultSets.add(resultSet);

        return resultSet;
    }

    /**
     * Fails the current transaction.
     */
    synchronized void failTransaction()
    {
        if (!_autoCommit) {
            _transactionFailed = true;
        }
    }

    /**
     * Gets the auto commit limit.
     *
     * @return The auto commit limit.
     */
    @CheckReturnValue
    int getAutoCommitLimit()
    {
        return _autoCommitLimit;
    }

    /**
     * Gets the store driver.
     *
     * @return The store driver.
     */
    @Nonnull
    @CheckReturnValue
    StoreDriver getDriver()
    {
        return _driver;
    }

    /**
     * Gets the name associated with a UUID.
     *
     * @param uuid The UUID.
     *
     * @return The optional name.
     *
     * @throws SQLException On store session exception.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getPointName(@Nonnull final UUID uuid)
        throws SQLException
    {
        Optional<String> name = _storePoints.getName(Require.notNull(uuid));

        if (!name.isPresent()) {
            final PointBinding.Request.Builder requestBuilder =
                PointBinding.Request
                    .newBuilder();
            final Optional<PointBinding[]> bindings;

            requestBuilder.selectUUID(uuid);

            try {
                bindings = _storeSession
                    .getPointBindings(requestBuilder.build());

                if (!bindings.isPresent()) {
                    throw new ServiceClosedException();
                }
            } catch (final SessionException exception) {
                throw JDBCMessages.SESSION_EXCEPTION.wrap(exception);
            }

            if (bindings.get().length == 1) {
                name = Optional.of(bindings.get()[0].getName());
                _storePoints.register(uuid, name.get());
            }
        }

        return name;
    }

    /**
     * Gets the UUID associated with a name.
     *
     * @param name The name.
     *
     * @return The UUID (empty if unknown).
     *
     * @throws SQLException On store session exception.
     */
    @Nonnull
    @CheckReturnValue
    Optional<UUID> getPointUUID(@Nonnull final String name)
        throws SQLException
    {
        Optional<UUID> uuid = _storePoints.getUUID(name);

        if (!uuid.isPresent()) {
            final PointBinding.Request.Builder requestBuilder =
                PointBinding.Request
                    .newBuilder();
            final Optional<PointBinding[]> bindings;

            requestBuilder.selectName(name);

            try {
                bindings = _storeSession
                    .getPointBindings(requestBuilder.build());

                if (!bindings.isPresent()) {
                    throw new ServiceClosedException();
                }
            } catch (final SessionException exception) {
                throw JDBCMessages.SESSION_EXCEPTION.wrap(exception);
            }

            if (bindings.get().length > 0) {
                uuid = Optional.of(bindings.get()[0].getUUID());
                _storePoints.register(uuid.get(), name);
            }
        }

        return uuid;
    }

    /**
     * Gets the UUIDs for points.
     *
     * @param bindingRequest The point binding request.
     *
     * @return The UUIDs.
     *
     * @throws SQLException On store session exception.
     */
    @Nonnull
    @CheckReturnValue
    UUID[] getPointsUUID(
            final PointBinding.Request bindingRequest)
        throws SQLException
    {
        final Optional<PointBinding[]> bindings;
        final UUID[] uuids;

        try {
            bindings = _storeSession.getPointBindings(bindingRequest);

            if (!bindings.isPresent()) {
                throw new ServiceClosedException();
            }
        } catch (final SessionException exception) {
            throw JDBCMessages.SESSION_EXCEPTION.wrap(exception);
        }

        uuids = new UUID[bindings.get().length];

        for (int i = 0; i < bindings.get().length; ++i) {
            final UUID uuid = bindings.get()[i].getUUID();

            uuids[i] = uuid;
            _storePoints.register(uuid, bindings.get()[i].getName());
        }

        return uuids;
    }

    /**
     * Gets the server URI.
     *
     * @return The server URI.
     */
    @Nonnull
    @CheckReturnValue
    URI getServerURI()
    {
        return _serverURI;
    }

    /**
     * Gets the store session.
     *
     * @return The store session.
     */
    @Nonnull
    @CheckReturnValue
    StoreSessionProxy getStoreSession()
    {
        return _storeSession;
    }

    /**
     * Gets the user.
     *
     * @return The optional user.
     */
    @Nonnull
    @CheckReturnValue
    Optional<String> getUser()
    {
        return Optional.ofNullable(_user);
    }

    /**
     * Called by store result sets when they close.
     *
     * @param resultSet The store result set.
     */
    void resultSetClosed(@Nonnull final ResultSet resultSet)
    {
        synchronized (_resultSets) {
            _resultSets.remove(resultSet);
        }
    }

    /**
     * Called by store statements when they close.
     *
     * @param statement The store statement.
     */
    void statementClosed(@Nonnull final StoreStatement statement)
    {
        synchronized (_statements) {
            _statements.remove(statement);
        }
    }

    private void _addWarning(final String message)
    {
        final SQLWarning warning = new SQLWarning(message);

        if (_warnings == null) {
            _warnings = warning;
        } else {
            _warnings.setNextWarning(warning);
        }
    }

    private void _assertOpen()
        throws SQLException
    {
        if (isClosed()) {
            throw JDBCMessages.CONNECTION_CLOSED.exception();
        }
    }

    private void _closeNotHeld()
        throws SQLException
    {
        final List<ResultSet> resultSets = new LinkedList<ResultSet>();

        for (final ResultSet resultSet: _resultSets) {
            final Statement statement = resultSet.getStatement();

            if ((statement != null)
                    && (statement.getResultSetHoldability()
                        != ResultSet.HOLD_CURSORS_OVER_COMMIT)) {
                resultSets.add(resultSet);
            }
        }

        for (final ResultSet resultSet: resultSets) {
            resultSet.close();
        }
    }

    private static final String _DEFAULT_AUTO_COMMIT_LIMIT = "1000";
    private static final Logger _LOGGER = Logger
        .getInstance(StoreConnection.class);

    private volatile boolean _autoCommit = true;
    private final int _autoCommitLimit;
    private final AtomicBoolean _closed = new AtomicBoolean();
    private final StoreDriver _driver;
    private int _holdability = ResultSet.HOLD_CURSORS_OVER_COMMIT;
    private volatile boolean _readOnly;
    private final Set<DefaultResultSet> _resultSets = new HashSet<>();
    private final URI _serverURI;
    private final Set<StoreStatement> _statements = new HashSet<>();
    private final StorePoints _storePoints = new StorePoints();
    private final StoreSessionProxy _storeSession;
    private volatile boolean _transactionFailed;
    private final List<PointValue> _updates = new LinkedList<>();
    private final String _user;
    private SQLWarning _warnings;
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
