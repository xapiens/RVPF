/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Parser.java 3973 2019-05-10 16:49:57Z SFB $
 */

package org.rvpf.jdbc;

import java.sql.SQLException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.sync.CrontabSync;
import org.rvpf.base.sync.ElapsedSync;
import org.rvpf.base.sync.StampsSync;

/**
 * Parser.
 */
final class Parser
{
    /**
     * Parses SQL text.
     *
     * @param sql The SQL text.
     * @param parameters An optional parameters holder (initially empty).
     *
     * @return A request.
     *
     * @throws SQLException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    Request parseSQL(
            @Nonnull final String sql,
            @Nonnull final Optional<List<Token.Parameter>> parameters)
        throws SQLException
    {
        final Logger logger = Logger.getInstance(getClass());
        final Request request;
        Token token;

        logger.debug(JDBCMessages.SQL, sql);

        _tokenizer.reset(sql, parameters);

        token = _nextToken(true);

        if (token == Token.SELECT) {
            request = _parseSelect();
        } else if (token == Token.INSERT) {
            request = _parseInsert();
        } else if (token == Token.UPDATE) {
            request = _parseUpdate();
        } else if (token == Token.DELETE) {
            request = _parseDelete();
        } else {
            throw _unexpectedToken(token);
        }

        token = _nextToken(false);

        if (token != null) {
            throw _unexpectedToken(token);
        }

        request.setParameters(parameters);

        return request;
    }

    private Token _nextToken(final boolean required)
        throws SQLException
    {
        final Optional<Token> token = _tokenizer.nextToken();

        if (required && !token.isPresent()) {
            throw JDBCMessages.UNEXPECTED_END.exception();
        }

        return token.orElse(null);
    }

    private Request _parseDelete()
        throws SQLException
    {
        final Request request = new Request.Update(Token.DELETE);
        Token token = _nextToken(true);

        if (token == Token.ALL) {
            request.setAll();
            token = _nextToken(true);
        }

        if (token == Token.FROM) {
            _parseTable(request, false, true);
        } else {
            request.setTableName(StoreDriver.ARCHIVE_TABLE);
            _putBackToken(token);
        }

        _parseWhere(request);

        return request;
    }

    private Request _parseInsert()
        throws SQLException
    {
        final Request.Update request = new Request.Update(Token.INSERT);
        final List<StoreColumn> columns = request.getColumns();
        Token token = _nextToken(true);

        if (token == Token.INTO) {
            _parseTable(request, false, true);
        } else {
            _putBackToken(token);
            _parseTable(request, false, false);
        }

        token = _nextToken(true);

        if (token == Token.LEFT) {
            boolean pointSeen = false;
            boolean stampSeen = false;

            for (;;) {
                token = _nextToken(true);

                if (!token.isWord() || token.isReserved()) {
                    throw _unexpectedToken(token);
                }

                final StoreColumn column = request.getColumn(token);

                if ((column == StoreColumn.POINT)
                        || (column == StoreColumn.POINT_NAME)
                        || (column == StoreColumn.POINT_UUID)) {
                    if (pointSeen) {
                        throw JDBCMessages.AMBIGUOUS_KEY.exception(column);
                    }

                    pointSeen = true;
                } else if (column == StoreColumn.STAMP) {
                    if (stampSeen) {
                        throw JDBCMessages.AMBIGUOUS_KEY.exception(column);
                    }

                    stampSeen = true;
                } else if (column == StoreColumn.VERSION) {
                    throw JDBCMessages.COLUMN_READ_ONLY.exception(column);
                }

                columns.add(column);
                token = _nextToken(true);

                if (token == Token.RIGHT) {
                    break;
                }

                if (token != Token.COMMA) {
                    throw _unexpectedToken(token);
                }
            }

            if (!(pointSeen && stampSeen)) {
                throw JDBCMessages.MISSING_KEY_COLUMN.exception();
            }

            token = _nextToken(true);
        }

        if (token == Token.VALUES) {
            if (columns.isEmpty()) {
                columns.add(StoreColumn.POINT);
                columns.add(StoreColumn.STAMP);
                columns.add(StoreColumn.STATE);
                columns.add(StoreColumn.VALUE);
            }

            for (;;) {
                final Iterator<StoreColumn> columnIterator = columns.iterator();
                int valueCount = 0;

                token = _nextToken(true);

                if (token != Token.LEFT) {
                    throw _unexpectedToken(token);
                }

                for (;;) {
                    final StoreColumn column = columnIterator.next();

                    token = _nextToken(true);

                    if (token.isValue() || (token == Token.NULL)) {}
                    else if (token.isReserved()) {
                        if ((column == StoreColumn.STAMP)
                                && (token == Token.NOW)) {
                            token = new Token.Value(
                                token.toString(),
                                DateTime.now());
                        } else if ((column == StoreColumn.STAMP)
                                   && (token == Token.TODAY)) {
                            token = new Token.Value(
                                token.toString(),
                                DateTime.now().midnight());
                        } else if ((column == StoreColumn.STAMP)
                                   && (token == Token.YESTERDAY)) {
                            token = new Token.Value(
                                token.toString(),
                                DateTime.now().previousDay());
                        } else {
                            throw _unexpectedToken(token);
                        }

                        Token nextToken = _nextToken(false);

                        if (nextToken == Token.LEFT) {    // Accepts optional '()'.
                            nextToken = _nextToken(true);

                            if (nextToken != Token.RIGHT) {
                                throw _unexpectedToken(nextToken);
                            }
                        } else {
                            _putBackToken(nextToken);
                        }
                    } else {
                        throw _unexpectedToken(token);
                    }

                    request.getValueTokens().add(token);
                    ++valueCount;
                    token = _nextToken(true);

                    if (token == Token.RIGHT) {
                        break;
                    }

                    if ((token != Token.COMMA)
                            || (valueCount > columns.size())) {
                        throw _unexpectedToken(token);
                    }
                }

                if (valueCount < columns.size()) {
                    throw _unexpectedToken(token);
                }

                token = _nextToken(false);

                if (token == null) {
                    break;
                }

                if (token != Token.COMMA) {
                    throw _unexpectedToken(token);
                }
            }
        } else if (token == Token.SET) {
            if (!columns.isEmpty()) {
                throw _unexpectedToken(token);
            }

            _parseSet(request);
        } else {
            throw _unexpectedToken(token);
        }

        return request;
    }

    private Request _parseSelect()
        throws SQLException
    {
        final Request.Query request = new Request.Query(Token.SELECT);
        String tableAlias = null;
        final List<StoreColumn> columns = new LinkedList<StoreColumn>();
        final List<String> aliases = new LinkedList<String>();
        Token token = _nextToken(false);

        if (token == Token.ALL) {
            request.setAll();
        } else {
            _putBackToken(token);
        }

        for (;;) {
            Token nextToken;

            token = _nextToken(false);

            if ((token == null)
                    || (token.isReserved() && (token != Token.ASTERISK))) {
                break;
            }

            if (!token.isWord()) {
                throw _unexpectedToken(token);
            }

            nextToken = _nextToken(false);

            if (nextToken == Token.PERIOD) {
                if (token == Token.ASTERISK) {
                    throw _unexpectedToken(nextToken);
                }

                if (tableAlias == null) {
                    tableAlias = token.toString();
                } else if (!tableAlias.equalsIgnoreCase(token.toString())) {
                    throw JDBCMessages.MULTIPLE_TABLES.exception();
                }

                nextToken = _nextToken(true);

                if (!nextToken.isWord()
                        || (nextToken.isReserved()
                            && (nextToken != Token.ASTERISK))) {
                    throw _unexpectedToken(nextToken);
                }

                token = nextToken;
                nextToken = _nextToken(true);
            }

            final StoreColumn column;
            final String alias;

            if (token == Token.ASTERISK) {
                column = StoreColumn.ALL;
                alias = null;
            } else {
                column = request.getColumn(token);

                if ((column == StoreColumn.COUNT)
                        && (nextToken == Token.LEFT)) {
                    nextToken = _nextToken(true);

                    if (nextToken != Token.ASTERISK) {
                        throw _unexpectedToken(nextToken);
                    }

                    nextToken = _nextToken(true);

                    if (nextToken != Token.RIGHT) {
                        throw _unexpectedToken(nextToken);
                    }

                    nextToken = _nextToken(false);
                }

                if (nextToken == Token.AS) {
                    nextToken = _nextToken(true);

                    if (nextToken.isReserved()) {
                        throw _unexpectedToken(nextToken);
                    }
                }

                if ((nextToken != null) && !nextToken.isReserved()) {
                    alias = nextToken.unquoted();
                    nextToken = _nextToken(false);
                } else {
                    alias = null;
                }
            }

            columns.add(column);
            aliases.add(alias);

            if (nextToken != Token.COMMA) {
                if ((nextToken == null) || nextToken.isReserved()) {
                    token = nextToken;

                    break;
                }

                throw _unexpectedToken(nextToken);
            }
        }

        if (columns.isEmpty()) {
            columns.add(StoreColumn.ALL);
            aliases.add(null);
        }

        if (token == Token.FROM) {
            _parseTable(request, true, true);

            if ((tableAlias != null)
                    && !tableAlias.equalsIgnoreCase(
                        request.getTableAlias().orElse(null))) {
                throw JDBCMessages.MULTIPLE_TABLES.exception();
            }
        } else {
            request.setTableName(StoreDriver.ARCHIVE_TABLE);
            _putBackToken(token);
        }

        final Iterator<String> alias = aliases.iterator();

        for (final StoreColumn column: columns) {
            if (column == StoreColumn.ALL) {
                alias.next();
                request.addColumn(StoreColumn.POINT_NAME, null);
                request.addColumn(StoreColumn.POINT_UUID, null);

                if (request
                    .getTableName()
                    .orElse(null) == StoreDriver.ARCHIVE_TABLE) {
                    request.addColumn(StoreColumn.STAMP, null);
                    request.addColumn(StoreColumn.VERSION, null);
                    request.addColumn(StoreColumn.STATE, null);
                    request.addColumn(StoreColumn.VALUE, null);
                }
            } else if (column == StoreColumn.COUNT) {
                if (columns.size() != 1) {
                    throw JDBCMessages.UNEXPECTED_COLUMN.exception(column);
                }

                request.addColumn(column, alias.next());
            } else if ((request.getTableName().orElse(
                    null) == StoreDriver.POINTS_TABLE)
                       && (column != StoreColumn.POINT_NAME)
                       && (column != StoreColumn.POINT_UUID)) {
                throw JDBCMessages.UNKNOWN_COLUMN.exception(column);
            } else {
                if (column == StoreColumn.INTERPOLATED) {
                    request.setInterpolated(true);
                } else if (column == StoreColumn.EXTRAPOLATED) {
                    request.setExtrapolated(true);
                }

                request.addColumn(column, alias.next());
            }
        }

        _parseWhere(request);

        return request;
    }

    private void _parseSet(final Request.Update request)
        throws SQLException
    {
        Token token;

        for (;;) {
            token = _nextToken(true);

            if (!token.isWord() || token.isReserved()) {
                throw _unexpectedToken(token);
            }

            final StoreColumn column = request.getColumn(token);

            if ((column != StoreColumn.VALUE)
                    && (column != StoreColumn.STATE)) {
                throw JDBCMessages.COLUMN_READ_ONLY.exception(column);
            }

            request.getColumns().add(column);
            token = _nextToken(true);

            if (token != Token.EQ) {
                throw _unexpectedToken(token);
            }

            token = _nextToken(true);

            if (token.isValue() || (token == Token.NULL)) {
                request.getValueTokens().add(token);
            } else {
                throw _unexpectedToken(token);
            }

            token = _nextToken(true);

            if (token == null) {
                break;
            }

            if (token != Token.COMMA) {
                _putBackToken(token);

                break;
            }
        }
    }

    private void _parseTable(
            final Request request,
            final boolean readOnly,
            final boolean required)
        throws SQLException
    {
        Token token;

        token = _nextToken(true);

        if (!required && token.isReserved()) {
            request.setTableName(StoreDriver.ARCHIVE_TABLE);
            _putBackToken(token);

            return;
        }

        if (!token.isWord() || token.isReserved()) {
            throw _unexpectedToken(token);
        }

        request.setTableName(StoreDriver.getTableName(token.toString()));

        if (!readOnly
                && (request.getTableName().orElse(
                    null) != StoreDriver.ARCHIVE_TABLE)) {
            throw JDBCMessages.TABLE_READ_ONLY
                .exception(request.getTableName().orElse(null));
        }

        token = _nextToken(false);

        if (token != null) {
            if (token == Token.AS) {
                token = _nextToken(true);

                if (!token.isWord() || token.isReserved()) {
                    throw _unexpectedToken(token);
                }
            }

            if (token.isWord() && !token.isReserved()) {
                request.setTableAlias(token.toString());
            } else {
                _putBackToken(token);
            }
        }
    }

    private void _parseTime(final Request request)
        throws SQLException
    {
        final Token token = _nextToken(true);

        if (!token.isReserved()) {
            throw _unexpectedToken(token);
        }

        final Token.Word.Reserved operatorToken = (Token.Word.Reserved) token;
        final Token valueToken = _nextToken(true);
        final DateTime dateTime;
        final Token.Value dateTimeToken;

        if (valueToken.isParameter()) {
            dateTime = null;
        } else if (valueToken.isQuoted()) {
            final String dateTimeString = (String) valueToken.getValue();

            dateTime = (dateTimeString != null)? DateTime
                .now()
                .valueOf(dateTimeString): null;
        } else if (token.isReserved()) {
            if (valueToken == Token.NOW) {
                dateTime = DateTime.now();
            } else if (valueToken == Token.TODAY) {
                dateTime = DateTime.now().midnight();
            } else if (valueToken == Token.YESTERDAY) {
                dateTime = DateTime.now().previousDay();
            } else if (valueToken == Token.BOT) {
                dateTime = DateTime.BEGINNING_OF_TIME;
            } else if (valueToken == Token.EOT) {
                dateTime = DateTime.END_OF_TIME;
            } else {
                throw _unexpectedToken(valueToken);
            }

            Token nextToken = _nextToken(false);

            if (nextToken == Token.LEFT) {    // Accepts optional '()'.
                nextToken = _nextToken(true);

                if (nextToken != Token.RIGHT) {
                    throw _unexpectedToken(nextToken);
                }
            } else {
                _putBackToken(nextToken);
            }
        } else {
            throw _unexpectedToken(valueToken);
        }

        dateTimeToken = (dateTime != null)? new Token.Value(
            valueToken.toString(),
            dateTime): (Token.Parameter) valueToken;

        if (operatorToken == Token.EQ) {
            request.setLowStampToken(dateTimeToken, operatorToken);
            request.setHighStampToken(dateTimeToken, operatorToken);
        } else if ((operatorToken == Token.GE) || (operatorToken == Token.GT)) {
            request.setLowStampToken(dateTimeToken, operatorToken);
        } else if ((operatorToken == Token.LT) || (operatorToken == Token.LE)) {
            request.setHighStampToken(dateTimeToken, operatorToken);
        } else {
            throw _unexpectedToken(operatorToken);
        }
    }

    private Request _parseUpdate()
        throws SQLException
    {
        final Request.Update request = new Request.Update(Token.UPDATE);
        Token token;

        token = _nextToken(true);

        if (token == Token.ALL) {
            request.setAll();
        } else {
            _putBackToken(token);
        }

        _parseTable(request, false, false);

        token = _nextToken(true);

        if (token != Token.SET) {
            throw _unexpectedToken(token);
        }

        _parseSet(request);

        _parseWhere(request);

        token = _nextToken(false);

        if (token != null) {
            throw _unexpectedToken(token);
        }

        return request;
    }

    private void _parseWhere(final Request request)
        throws SQLException
    {
        Token token = _nextToken(false);
        StoreColumn timeColumn = null;

        if (token == Token.WHERE) {
            boolean isPull = false;
            boolean isNotNull = false;

            do {
                final Token nextToken;
                final StoreColumn column;

                token = _nextToken(true);
                nextToken = _nextToken(false);

                if (nextToken == Token.PERIOD) {
                    if (!token.unquoted().equalsIgnoreCase(
                            request.getTableAlias().orElse(null))
                            && !token.getValue().toString().equalsIgnoreCase(
                                request.getTableName().orElse(null))) {
                        throw JDBCMessages.UNKNOWN_TABLE.exception(token);
                    }

                    token = _nextToken(true);
                } else {
                    _putBackToken(nextToken);
                }

                column = request.getColumn(token);

                if ((column == StoreColumn.POINT)
                        || (column == StoreColumn.POINT_NAME)
                        || (column == StoreColumn.POINT_UUID)) {
                    final Token operatorToken = _nextToken(true);

                    if (operatorToken != Token.EQ) {
                        if ((request.getTableName().orElse(
                                null) != StoreDriver.POINTS_TABLE)
                                || (column == StoreColumn.POINT_UUID)
                                || ((operatorToken != Token.LIKE)
                                    && (operatorToken != Token.REGEXP))) {
                            throw _unexpectedToken(token);
                        }
                    }

                    token = _nextToken(true);

                    if (!token.isValue()) {
                        throw _unexpectedToken(token);
                    }

                    request
                        .selectPoint(
                            column,
                            (Token.Word.Reserved) operatorToken,
                            (Token.Value) token);
                } else if (request
                    .getTableName()
                    .orElse(null) != StoreDriver.ARCHIVE_TABLE) {
                    throw JDBCMessages.UNEXPECTED_COLUMN.exception(column);
                } else if (column == StoreColumn.VALUE) {
                    for (final Token word:
                            new Token[] {Token.IS, Token.NOT, Token.NULL, }) {
                        token = _nextToken(true);

                        if (token != word) {
                            throw _unexpectedToken(token);
                        }
                    }

                    isNotNull = true;
                } else if (column == StoreColumn.STAMP) {
                    if (timeColumn == StoreColumn.VERSION) {
                        throw JDBCMessages.UNEXPECTED_COLUMN.exception(column);
                    }

                    timeColumn = StoreColumn.STAMP;
                    _parseTime(request);
                } else if (request.isQuery()
                           && (column == StoreColumn.VERSION)) {
                    if (timeColumn == StoreColumn.STAMP) {
                        throw JDBCMessages.UNEXPECTED_COLUMN.exception(column);
                    }

                    timeColumn = StoreColumn.VERSION;
                    isPull = true;
                    _parseTime(request);
                } else if (request.isQuery()
                           && ((column == StoreColumn.CRONTAB)
                           || (column == StoreColumn.ELAPSED)
                           || (column == StoreColumn.STAMPS)
                           || (column == StoreColumn.TIME_LIMIT))) {
                    if (_nextToken(true) != Token.EQ) {
                        throw _unexpectedToken(token);
                    }

                    token = _nextToken(true);

                    if (!token.isQuoted()) {
                        throw _unexpectedToken(token);
                    }

                    if (column == StoreColumn.CRONTAB) {
                        final String crontabString = (String) token.getValue();
                        final CrontabSync crontabSync = new CrontabSync();

                        if (crontabSync.setUp(crontabString)) {
                            request.setSync(crontabSync);
                        } else {
                            throw JDBCMessages.BAD_CRONTAB_ENTRY
                                .exception(crontabString);
                        }
                    } else if (column == StoreColumn.ELAPSED) {
                        request
                            .setSync(
                                new ElapsedSync(
                                    (String) token.getValue(),
                                    Optional.empty()));
                    } else if (column == StoreColumn.STAMPS) {
                        request
                            .setSync(
                                new StampsSync(
                                    _STAMPS_SPLIT_PATTERN.split(
                                            (String) token.getValue())));
                    } else if (column == StoreColumn.TIME_LIMIT) {
                        request
                            .setTimeLimit(
                                ElapsedTime
                                    .fromString((String) token.getValue()));
                    }
                } else {
                    throw JDBCMessages.UNEXPECTED_COLUMN.exception(column);
                }

                token = _nextToken(false);
            } while (token == Token.AND);

            if (request
                .getTableName()
                .orElse(null) == StoreDriver.ARCHIVE_TABLE) {
                if (isPull) {
                    if (isNotNull) {
                        throw JDBCMessages.UNEXPECTED_COLUMN
                            .exception(StoreColumn.VALUE);
                    }

                    request.setPull(true);
                } else if (!request.getPointColumn().isPresent()) {
                    throw JDBCMessages.POINT_NOT_SPECIFIED.exception();
                } else if (!request.isAll()) {
                    request.setSynced(true);
                }

                if (isNotNull) {
                    request.setNullIgnored(true);
                }
            }
        }

        if ((request.getTableName().orElse(null) == StoreDriver.ARCHIVE_TABLE)
                && (token == Token.LIMIT)) {
            token = _nextToken(true);

            if (!token.isValue()) {
                throw _unexpectedToken(token);
            }

            request.setLimitToken((Token.Value) token);
        } else {
            _putBackToken(token);
        }
    }

    private void _putBackToken(final Token token)
    {
        _tokenizer.nextToken(token);
    }

    private SQLException _unexpectedToken(final Token token)
    {
        return _tokenizer.unexpectedToken(token.toString());
    }

    private static final Pattern _STAMPS_SPLIT_PATTERN = Pattern.compile(",");

    private final Tokenizer _tokenizer = new Tokenizer();
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
