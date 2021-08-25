/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JDBCMessages.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.jdbc;

import java.sql.SQLException;

// [JDBC 4 begins]
import java.sql.SQLFeatureNotSupportedException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

// [JDBC 4 ends]
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;

/**
 * JDBC messages.
 */
public enum JDBCMessages
    implements Messages.Entry
{
    AMBIGUOUS_KEY("S1000"),
    AUTO_COMMIT,
    AUTO_COMMIT_LIMIT,
    AUTOCOMMIT("25000"),
    BAD_CONNECTION_URL("08001"),
    BAD_CRONTAB_ENTRY("S1000"),
    CLOSE_CURSORS,
    COLUMN_READ_ONLY("S1000"),
    COMMIT_FAILED("S1000"),
    COMMIT_WARNINGS,
    COMMITTED,
    COMMITTING,
    CONNECT_FAILED("08004"),
    CONNECTION_CLOSED("08003"),
    DUPLICATE_ALIAS("S1000"),
    FEATURE_NOT_SUPPORTED("0A000"),
    INVALID_ARGUMENT("S1009"),
    INVALID_COLUMN_NUMBER("S1002"),
    INVALID_ESCAPE("37000"),
    INVALID_NUMBER_FORMAT("37000"),
    INVALID_PARAMETER_NUMBER("S1093"),
    MISSING_KEY_COLUMN("S1000"),
    MISSING_QUOTE("37000"),
    MULTIPLE_TABLES("37000"),
    NO_CURRENT_ROW("S1000"),
    NO_RESULT_SET("S1000"),
    NOT_A_NAME("S1000"),
    NOT_A_NUMBER("S1000"),
    NOT_A_STAMP("S1000"),
    NOT_A_UUID("S1000"),
    NOT_AN_UPDATE("S1000"),
    NOT_ON_INSERT_ROW("S1000"),
    ON_INSERT_ROW("S1000"),
    PATTERN_SYNTAX_ERROR("S1000"),
    POINT_NOT_SPECIFIED("S1000"),
    POINT_ROWS_RETURNED,
    POINTS_RETURNED,
    RESULT_SET_CLOSED("S1000"),
    RESULT_SET_CONCURRENCY_NOT_SUPPORTED("S1000"),
    RESULT_SET_FORWARD_ONLY("S1000"),
    RESULT_SET_HOLDABILITY_NOT_SUPPORTED("S1000"),
    RESULT_SET_READ_ONLY("S1000"),
    ROLLED_BACK,
    SESSION_EXCEPTION("S1000"),
    SQL,
    STAMP_NOT_SPECIFIED("S1000"),
    STATEMENT_CLOSED("S1000"),
    TABLE_READ_ONLY("S1000"),
    TRANSACTION_FAILED("25000"),
    TRANSACTION_LEVEL_NOT_SUPPORTED("S1000"),
    UNEXPECTED_COLUMN("S1000"),
    UNEXPECTED_END("37000"),
    UNEXPECTED_TOKEN("37000"),
    UNKNOWN_COLUMN("42S22"),
    UNKNOWN_POINT("S1000"),
    UNKNOWN_TABLE("42S02"),
    UPDATE_FAILED,
    UPDATE_QUEUED,
    UPDATED,
    VALUE_ROWS_FETCHED,
    VALUES_RETURNED,
    WRONG_DATA_TYPE("37000");

    /**
     * Constructs an instance.
     */
    JDBCMessages()
    {
        this(null);
    }

    /**
     * Constructs an instance.
     *
     * @param state The SQLState associated to the message.
     */
    JDBCMessages(final String state)
    {
        _state = state;
    }

    /** {@inheritDoc}
     */
    @Override
    public String getBundleName()
    {
        return _BUNDLE_NAME;
    }

    /** {@inheritDoc}
     */
    @Override
    public synchronized String toString()
    {
        if (_string == null) {
            _string = Messages.getString(this);
        }

        return _string;
    }

    /**
     * Converts this to a SQLException.
     *
     * @param args Message text arguments.
     *
     * @return A SQLException.
     */
    SQLException exception(@Nonnull final Object... args)
    {
        if (this == FEATURE_NOT_SUPPORTED) {
            return new SQLFeatureNotSupportedException(
                Message.format(this, args),
                _state,
                -ordinal());
        }

        return new SQLException(Message.format(this, args), _state, -ordinal());
    }

    /**
     * Wraps a throwable.
     *
     * @param cause The throwable.
     * @param args Message text arguments.
     *
     * @return A SQLException.
     */
    @Nonnull
    @CheckReturnValue
    SQLException wrap(
            @Nonnull final Throwable cause,
            @Nonnull final Object... args)
    {
        final SQLException exception = exception(args);

        exception.initCause(cause);

        return exception;
    }

    private static final String _BUNDLE_NAME = "org.rvpf.messages.jdbc";

    private final String _state;
    private String _string;
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
