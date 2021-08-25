/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Request.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.jdbc;

import java.io.Serializable;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.ElapsedTime;
import org.rvpf.base.UUID;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.Sync;
import org.rvpf.base.tool.Require;

/**
 * Request.
 */
abstract class Request
{
    /**
     * Constructs an instance from an other.
     *
     * @param other The other instance.
     */
    protected Request(@Nonnull final Request other)
    {
        _commandToken = other._commandToken;
        _all = other._all;
        _interpolated = other._interpolated;
        _extrapolated = other._extrapolated;
        _limitToken = other._limitToken;
        _lowOperatorToken = other._lowOperatorToken;
        _highOperatorToken = other._highOperatorToken;
        _lowStampToken = other._lowStampToken;
        _highStampToken = other._highStampToken;
        _nullIgnored = other._nullIgnored;
        _pointColumn = other._pointColumn;
        _pointOperatorToken = other._pointOperatorToken;
        _pointToken = other._pointToken;
        _pull = other._pull;
        _sync = other._sync;
        _synced = other._synced;
        _tableAlias = other._tableAlias;
        _tableName = other._tableName;
        _timeLimit = other._timeLimit;

        if (other._parameters != null) {
            _parameters = new ArrayList<>(other._parameters.size());

            for (final Token.Parameter parameter: other._parameters) {
                _parameters.add(new Token.Parameter(parameter));
            }
        }
    }

    /**
     * Constructs an instance.
     *
     * @param commandToken The command token.
     */
    Request(@Nonnull final Token commandToken)
    {
        _commandToken = commandToken;
    }

    /**
     * Creates a copy of this.
     *
     * @return The copy.
     */
    public abstract Request copy();

    /**
     * Clears the parameters.
     */
    final void clearParameters()
    {
        if (_parameters != null) {
            for (final Token.Parameter parameter: _parameters) {
                parameter.setValue(null);
            }
        }
    }

    /**
     * Gets a column by its name.
     *
     * @param token The token holding the name of the column.
     *
     * @return The column.
     *
     * @throws SQLException When the column is unknown.
     */
    @Nonnull
    @CheckReturnValue
    StoreColumn getColumn(@Nonnull final Token token)
        throws SQLException
    {
        final String name = token.toString().toUpperCase(Locale.ROOT);

        try {
            return StoreColumn.valueOf(name);
        } catch (final IllegalArgumentException exception) {
            throw JDBCMessages.UNKNOWN_COLUMN.exception(name);
        }
    }

    /**
     * Gets the command token.
     *
     * @return The command token.
     */
    @Nonnull
    @CheckReturnValue
    final Token getCommandToken()
    {
        return _commandToken;
    }

    /**
     * Gets the parameter count.
     *
     * @return The parameter count.
     */
    @CheckReturnValue
    final int getParameterCount()
    {
        return (_parameters != null)? _parameters.size(): 0;
    }

    /**
     * Gets the parameters.
     *
     * @return The parameters.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<List<Token.Parameter>> getParameters()
    {
        return Optional.ofNullable(_parameters);
    }

    /**
     * Gets the point column.
     *
     * @return The optional point column.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<StoreColumn> getPointColumn()
    {
        return Optional.ofNullable(_pointColumn);
    }

    /**
     * Gets the table alias.
     *
     * @return The optional table alias.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<String> getTableAlias()
    {
        return Optional.ofNullable(_tableAlias);
    }

    /**
     * Gets the table name.
     *
     * @return The optional table name.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<String> getTableName()
    {
        return Optional.ofNullable(_tableName);
    }

    /**
     * Gets the 'ALL' indicator.
     *
     * @return The 'ALL' indicator.
     */
    @CheckReturnValue
    final boolean isAll()
    {
        return _all;
    }

    /**
     * Asks if this is a query request.
     *
     * @return True if this is a query request.
     */
    @CheckReturnValue
    boolean isQuery()
    {
        return false;
    }

    /**
     * Asks if this is an update request.
     *
     * @return True if this is an update request.
     */
    @CheckReturnValue
    boolean isUpdate()
    {
        return false;
    }

    /**
     * Prepares an info query.
     *
     * @return The info  query.
     *
     * @throws SQLException When there is a problem with the query.
     */
    @Nonnull
    @CheckReturnValue
    final PointBinding.Request prepareInfoQuery()
        throws SQLException
    {
        final PointBinding.Request.Builder requestBuilder = PointBinding.Request
            .newBuilder();

        if (_pointOperatorToken == Token.EQ) {
            Serializable point = _pointToken.getValue();

            switch (_pointColumn) {
                case POINT: {
                    if ((point instanceof String)
                            && UUID.isUUID((String) point)) {
                        point = UUID.fromString((String) point).get();
                    }

                    if (point instanceof UUID) {
                        requestBuilder.selectUUID((UUID) point);

                        break;
                    }

                    if (!(point instanceof String)) {
                        throw JDBCMessages.NOT_A_NAME.exception(point);
                    }

                    requestBuilder.selectName((String) point);

                    break;
                }
                case POINT_NAME: {
                    if (!(point instanceof String)) {
                        throw JDBCMessages.NOT_A_NAME.exception(point);
                    }

                    requestBuilder.selectName((String) point);

                    break;
                }
                case POINT_UUID: {
                    if (point instanceof UUID) {
                        requestBuilder.selectUUID((UUID) point);
                    } else if ((point instanceof String)
                               && UUID.isUUID((String) point)) {
                        final UUID pointUUID = UUID
                            .fromString((String) point)
                            .get();

                        requestBuilder.selectUUID(pointUUID);
                    } else {
                        throw JDBCMessages.NOT_A_UUID.exception(point);
                    }

                    break;
                }
                default: {
                    Require.failure();
                }
            }
        } else if (_pointColumn == StoreColumn.POINT_UUID) {
            throw JDBCMessages.NOT_A_UUID.exception(_pointToken.getValue());
        } else if (_pointOperatorToken == Token.LIKE) {
            requestBuilder.selectWild((String) _pointToken.getValue());
        } else if (_pointOperatorToken == Token.REGEXP) {
            try {
                requestBuilder
                    .selectPattern(
                        Pattern
                            .compile(
                                    (String) _pointToken.getValue(),
                                            Pattern.CASE_INSENSITIVE));
            } catch (final PatternSyntaxException exception) {
                throw JDBCMessages.PATTERN_SYNTAX_ERROR
                    .exception((String) _pointToken.getValue());
            }
        }

        return requestBuilder.build();
    }

    /**
     * Prepares a store query.
     *
     * @param connection The store connection.
     *
     * @return The store query.
     *
     * @throws SQLException When there is a problem with the query.
     */
    @Nonnull
    @CheckReturnValue
    final StoreValuesQuery prepareStoreQuery(
            @Nonnull final StoreConnection connection)
        throws SQLException
    {
        final StoreValuesQuery.Builder storeQueryBuilder = StoreValuesQuery
            .newBuilder();

        if (_pointToken != null) {
            Serializable point = _pointToken.getValue();

            Require.success(_pointOperatorToken == Token.EQ);

            switch (_pointColumn) {
                case POINT: {
                    if ((point instanceof String)
                            && UUID.isUUID((String) point)) {
                        point = UUID.fromString((String) point).get();
                    }

                    if (point instanceof UUID) {
                        storeQueryBuilder.setPointUUID((UUID) point);
                        Require.notNull(connection.getPointName((UUID) point));

                        break;
                    }

                    if (!(point instanceof String)) {
                        throw JDBCMessages.NOT_A_NAME.exception(point);
                    }

                    final Optional<UUID> pointUUID = connection
                        .getPointUUID((String) point);

                    if (!pointUUID.isPresent()) {
                        throw JDBCMessages.UNKNOWN_POINT.exception(point);
                    }

                    storeQueryBuilder.setPointUUID(pointUUID.get());

                    break;
                }
                case POINT_NAME: {
                    if (!(point instanceof String)) {
                        throw JDBCMessages.NOT_A_NAME.exception(point);
                    }

                    final Optional<UUID> pointUUID = connection
                        .getPointUUID((String) point);

                    if (!pointUUID.isPresent()) {
                        throw JDBCMessages.UNKNOWN_POINT.exception(point);
                    }

                    storeQueryBuilder.setPointUUID(pointUUID.get());

                    break;
                }
                case POINT_UUID: {
                    final UUID pointUUID;

                    if (point instanceof UUID) {
                        pointUUID = (UUID) point;
                    } else if ((point instanceof String)
                               && UUID.isUUID((String) point)) {
                        pointUUID = UUID.fromString((String) point).get();
                    } else {
                        throw JDBCMessages.NOT_A_UUID.exception(point);
                    }

                    connection.getPointName(pointUUID);
                    storeQueryBuilder.setPointUUID(pointUUID);

                    break;
                }
                default: {
                    Require.failure();
                }
            }
        }

        if (_lowOperatorToken == Token.EQ) {
            storeQueryBuilder.setAt(_lowStampToken.getDateTime());
        } else {
            if (_lowStampToken != null) {
                if (_lowOperatorToken == Token.GE) {
                    storeQueryBuilder
                        .setNotBefore(_lowStampToken.getDateTime());
                } else {
                    Require.success(_lowOperatorToken == Token.GT);
                    storeQueryBuilder.setAfter(_lowStampToken.getDateTime());
                }
            }

            if (_highStampToken != null) {
                if (_highOperatorToken == Token.LE) {
                    storeQueryBuilder
                        .setNotAfter(_highStampToken.getDateTime());
                } else {
                    Require.success(_highOperatorToken == Token.LT);
                    storeQueryBuilder.setBefore(_highStampToken.getDateTime());
                }
            }
        }

        storeQueryBuilder.setPull(_pull);
        storeQueryBuilder.setSynced(_synced);
        storeQueryBuilder.setNotNull(_nullIgnored);
        storeQueryBuilder.setSync(_sync);
        storeQueryBuilder.setInterpolated(_interpolated);
        storeQueryBuilder.setExtrapolated(_extrapolated);
        storeQueryBuilder.setPolatorTimeLimit(Optional.ofNullable(_timeLimit));

        if (_limitToken != null) {
            final int limit = _limitToken.getNumber().intValue();

            storeQueryBuilder.setRows((limit > 0)? limit: -1);
        }

        return storeQueryBuilder.build();
    }

    /**
     * Selects the point.
     *
     * @param pointColumn The point column.
     * @param pointOperatorToken The point operator token.
     * @param pointToken The point token.
     */
    final void selectPoint(
            @Nonnull final StoreColumn pointColumn,
            @Nonnull final Token.Word.Reserved pointOperatorToken,
            @Nonnull final Token.Value pointToken)
    {
        _pointColumn = pointColumn;
        _pointOperatorToken = pointOperatorToken;
        _pointToken = pointToken;
    }

    /**
     * Sets the 'ALL' indicator.
     */
    final void setAll()
    {
        _all = true;
    }

    /**
     * Sets the extrapolated indicator.
     *
     * @param extrapolated The extrapolated indicator.
     */
    void setExtrapolated(final boolean extrapolated)
    {
        _extrapolated = extrapolated;
    }

    /**
     * Sets the high time stamp token.
     *
     * @param stampToken The high time token.
     * @param operatorToken The comparison operator token.
     */
    final void setHighStampToken(
            @Nonnull final Token.Value stampToken,
            @Nonnull final Token.Word.Reserved operatorToken)
    {
        _highStampToken = stampToken;
        _highOperatorToken = operatorToken;
    }

    /**
     * Sets the interpolated indicator.
     *
     * @param interpolated The interpolated indicator.
     */
    void setInterpolated(final boolean interpolated)
    {
        _interpolated = interpolated;
    }

    /**
     * Sets the limit token.
     *
     * @param limitToken The limit token.
     */
    final void setLimitToken(@Nonnull final Token.Value limitToken)
    {
        _limitToken = limitToken;
    }

    /**
     * Sets the low time token.
     *
     * @param stampToken The low time token.
     * @param operatorToken The comparison operator token.
     */
    final void setLowStampToken(
            @Nonnull final Token.Value stampToken,
            @Nonnull final Token.Word.Reserved operatorToken)
    {
        _lowStampToken = stampToken;
        _lowOperatorToken = operatorToken;
    }

    /**
     * Sets the null ignored indicator.
     *
     * @param nullIgnored The null ignored indicator.
     */
    final void setNullIgnored(final boolean nullIgnored)
    {
        _nullIgnored = nullIgnored;
    }

    /**
     * Sets the value of a parameter.
     *
     * @param parameterNumber The parameter number.
     * @param value The parameter value.
     *
     * @throws SQLException When the parameter number is invalid.
     */
    final void setParameter(
            final int parameterNumber,
            @Nullable final Serializable value)
        throws SQLException
    {
        if ((_parameters == null)
                || (parameterNumber < 1)
                || (parameterNumber > _parameters.size())) {
            throw JDBCMessages.INVALID_PARAMETER_NUMBER
                .exception(String.valueOf(parameterNumber));
        }

        _parameters.get(parameterNumber - 1).setValue(value);
    }

    /**
     * Sets the parameters.
     *
     * @param parameters The optional parameters.
     */
    final void setParameters(
            @Nonnull final Optional<List<Token.Parameter>> parameters)
    {
        _parameters = parameters
            .isPresent()? new ArrayList<Token.Parameter>(
                parameters.get()): null;
    }

    /**
     * Sets the pull indicator.
     *
     * @param pull The pull indicator.
     */
    final void setPull(final boolean pull)
    {
        _pull = pull;
    }

    /**
     * Sets the synchronization object.
     *
     * @param sync The synchronization object.
     */
    void setSync(@Nonnull final Sync sync)
    {
        _sync = sync;
    }

    /**
     * Sets the synced indicator.
     *
     * @param synced The synced indicator.
     */
    final void setSynced(final boolean synced)
    {
        _synced = synced;
    }

    /**
     * Sets the table alias.
     *
     * @param tableAlias The table alias.
     */
    final void setTableAlias(@Nonnull final String tableAlias)
    {
        _tableAlias = tableAlias;
    }

    /**
     * Sets the table name.
     *
     * @param tableName The table name.
     */
    final void setTableName(@Nonnull final String tableName)
    {
        _tableName = tableName;
    }

    /**
     * Sets the time limit.
     *
     * @param timeLimit The time limit.
     */
    void setTimeLimit(@Nonnull final ElapsedTime timeLimit)
    {
        _timeLimit = timeLimit;
    }

    private boolean _all;
    private final Token _commandToken;
    private boolean _extrapolated;
    private Token.Word.Reserved _highOperatorToken;
    private Token.Value _highStampToken;
    private boolean _interpolated;
    private Token.Value _limitToken;
    private Token.Word.Reserved _lowOperatorToken;
    private Token.Value _lowStampToken;
    private boolean _nullIgnored;
    private List<Token.Parameter> _parameters;
    private StoreColumn _pointColumn;
    private Token.Word.Reserved _pointOperatorToken;
    private Token.Value _pointToken;
    private boolean _pull;
    private Sync _sync;
    private boolean _synced;
    private String _tableAlias;
    private String _tableName;
    private ElapsedTime _timeLimit;

    /**
     * Query request.
     */
    static final class Query
        extends Request
    {
        /**
         * Constructs an instance.
         *
         * @param command The command name.
         */
        Query(@Nonnull final Token command)
        {
            super(command);

            _aliases = new HashMap<String, StoreColumn>();
            _columns = new LinkedList<StoreColumn>();
            _titles = new LinkedList<String>();
        }

        private Query(@Nonnull final Query other)
        {
            super(other);

            _aliases = new HashMap<String, StoreColumn>(other._aliases);
            _columns = new LinkedList<StoreColumn>(other._columns);
            _titles = new LinkedList<String>(other._titles);
        }

        /** {@inheritDoc}
         */
        @Override
        public Query copy()
        {
            return new Query(this);
        }

        /**
         * Adds a column.
         *
         * @param column The column.
         * @param alias The column alias.
         *
         * @throws SQLException On duplicate alias.
         */
        void addColumn(
                final StoreColumn column,
                final String alias)
            throws SQLException
        {
            _columns.add(column);

            if (alias != null) {
                if (_aliases
                    .put(alias.toUpperCase(Locale.ROOT), column) != null) {
                    throw JDBCMessages.DUPLICATE_ALIAS.exception(alias);
                }

                _titles.add(alias);
            } else {
                _titles.add(column.getLabel());
            }
        }

        /** {@inheritDoc}
         */
        @Override
        StoreColumn getColumn(final Token token)
            throws SQLException
        {
            final StoreColumn column = _aliases
                .get(token.toString().toUpperCase(Locale.ROOT));

            return (column != null)? column: super.getColumn(token);
        }

        /**
         * Gets the columns.
         *
         * @return The columns.
         */
        @Nonnull
        @CheckReturnValue
        List<StoreColumn> getColumns()
        {
            return _columns;
        }

        /**
         * Gets the titles.
         *
         * @return The titles.
         */
        List<String> getTitles()
        {
            return _titles;
        }

        /** {@inheritDoc}
         */
        @Override
        boolean isQuery()
        {
            return true;
        }

        private final Map<String, StoreColumn> _aliases;
        private final List<StoreColumn> _columns;
        private final List<String> _titles;
    }


    /**
     * Update request.
     */
    static final class Update
        extends Request
    {
        /**
         * Constructs an instance.
         *
         * @param commandToken The command token.
         */
        Update(@Nonnull final Token commandToken)
        {
            super(commandToken);

            _columns = new ArrayList<StoreColumn>(4);
            _valueTokens = new ArrayList<Token>(4);
        }

        private Update(@Nonnull final Update other)
        {
            super(other);

            _columns = new ArrayList<StoreColumn>(other._columns);

            if (getParameterCount() > 0) {
                final Iterator<Token.Parameter> parameters = getParameters()
                    .get()
                    .iterator();
                final ListIterator<Token> valueTokens;

                _valueTokens = new ArrayList<Token>(other._valueTokens);
                valueTokens = _valueTokens.listIterator();

                while (valueTokens.hasNext()) {
                    final Token valueToken = valueTokens.next();

                    if (valueToken.isParameter()) {
                        valueTokens.set(parameters.next());
                    }
                }
            } else {
                _valueTokens = new ArrayList<Token>(0);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public Update copy()
        {
            return new Update(this);
        }

        /**
         * Gets the columns.
         *
         * @return The columns.
         */
        List<StoreColumn> getColumns()
        {
            return _columns;
        }

        /**
         * Gets the value tokens.
         *
         * @return The value tokens.
         */
        List<Token> getValueTokens()
        {
            return _valueTokens;
        }

        /**
         * Asks if this is an DELETE request.
         *
         * @return True if this is an DELETE request.
         */
        boolean isDelete()
        {
            return getCommandToken() == Token.DELETE;
        }

        /**
         * Asks if this is an INSERT request.
         *
         * @return True if this is an INSERT request.
         */
        boolean isInsert()
        {
            return getCommandToken() == Token.INSERT;
        }

        /** {@inheritDoc}
         */
        @Override
        boolean isUpdate()
        {
            return true;
        }

        private final List<StoreColumn> _columns;
        private final List<Token> _valueTokens;
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
