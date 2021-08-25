/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Tokenizer.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.jdbc;

import java.sql.SQLException;

import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;

/**
 * Tokenizer.
 */
final class Tokenizer
{
    /**
     * Returns the next token.
     *
     * @return The next token or empty.
     *
     * @throws SQLException When appropriate.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Token> nextToken()
        throws SQLException
    {
        if (_nextToken != null) {
            final Token token = _nextToken;

            _nextToken = null;

            return Optional.of(token);
        }

        while (Character.isWhitespace(_currentChar) && _fetchNextChar()) {}

        if (_currentChar == '\0') {
            return Optional.empty();
        }

        if ((_parameters.isPresent()) && (_currentChar == '?')) {
            return Optional.of(_parameter());
        }

        if (Character.isUnicodeIdentifierStart(_currentChar)) {
            return Optional.of(_identifier());
        }

        if ((_currentChar == '"') || (_currentChar == '\'')) {
            return Optional.of(_quoted());
        }

        if (Character.isDigit(_currentChar)
                || ((_currentChar == '-') && Character.isDigit(_nextChar))) {
            return Optional.of(_numeric());
        }

        return Optional.of(_other());
    }

    /**
     * Puts back a token.
     *
     * @param token The token.
     */
    void nextToken(@Nonnull final Token token)
    {
        Require.success(_nextToken == null);

        _nextToken = token;
    }

    /**
     * Resets this tokenizer.
     *
     * @param text The text to tokenize.
     * @param parameters An optional parameters holder (initially empty).
     */
    void reset(
            @Nonnull final String text,
            @Nonnull final Optional<List<Token.Parameter>> parameters)
    {
        _text = text;
        _parameters = parameters;
        _nextPosition = 0;
        _currentPosition = 0;
        _nextChar = (_text.length() > 0)? _text.charAt(_nextPosition++): '\0';
        _fetchNextChar();
    }

    /**
     * Returns a SQLException about an unexpected token.
     *
     * @param text The text of the unexpected token.
     *
     * @return The SQLException.
     */
    @Nonnull
    @CheckReturnValue
    SQLException unexpectedToken(@Nonnull final String text)
    {
        return JDBCMessages.UNEXPECTED_TOKEN
            .exception(
                String.valueOf(text),
                String.valueOf(_currentPosition),
                _text);
    }

    private boolean _fetchNextChar()
    {
        _currentChar = _nextChar;

        if (_currentPosition < _nextPosition) {
            _currentPosition = (_currentChar != '\0')
                    ? (_nextPosition - 1): _nextPosition;
            _nextChar = (_nextPosition < _text.length())? _text
                .charAt(_nextPosition++): '\0';
        }

        return _currentChar != '\0';
    }

    private Token _identifier()
    {
        final int startPosition = _currentPosition;

        while (_fetchNextChar()
                && Character.isUnicodeIdentifierPart(_currentChar)) {}

        return Token.wordFor(_text.substring(startPosition, _currentPosition));
    }

    private Token _numeric()
        throws SQLException
    {
        final int startPosition = _currentPosition;

        // Eats optional sign and first string of digits.
        while (_fetchNextChar() && Character.isDigit(_currentChar)) {}

        final int intPosition = _currentPosition;

        if (_currentChar == '.') {    // Eats decimal digits.
            while (_fetchNextChar() && Character.isDigit(_currentChar)) {}
        }

        if (Character.toUpperCase(_currentChar) == 'E') {    // Eats exponent.
            _fetchNextChar();

            if ((_currentChar == '-') || (_currentChar == '+')) {
                _fetchNextChar();
            }

            while (Character.isDigit(_currentChar) && _fetchNextChar()) {}
        }

        final Number number;    // Ready for numeric conversion.

        try {
            if (_currentPosition == intPosition) {    // Integer.
                number = Long
                    .valueOf(_text.substring(startPosition, _currentPosition));
            } else {    // Float.
                number = Double
                    .valueOf(_text.substring(startPosition, _currentPosition));
            }
        } catch (final NumberFormatException exception) {
            throw JDBCMessages.INVALID_NUMBER_FORMAT
                .exception(_text.substring(startPosition, _currentPosition));
        }

        return new Token.Numeric(
            _text.substring(startPosition, _currentPosition),
            number);
    }

    private Token _other()
        throws SQLException
    {
        final int startPosition = _currentPosition;

        if (",().*=".indexOf(_currentChar) >= 0) {
            _fetchNextChar();
        } else if ("<>".indexOf(_currentChar) >= 0) {
            _fetchNextChar();

            if (_currentChar == '=') {
                _fetchNextChar();
            }
        } else {
            throw unexpectedToken(String.valueOf(_currentChar));
        }

        final Token token = Token
            .wordFor(_text.substring(startPosition, _currentPosition));

        Require.success(token.isReserved());

        return token;
    }

    private Token _parameter()
    {
        final Token.Parameter parameter = new Token.Parameter();

        _fetchNextChar();
        _parameters.get().add(parameter);

        return parameter;
    }

    private Token _quoted()
        throws SQLException
    {
        final int startPosition = _currentPosition;
        final char quote = _currentChar;
        final StringBuilder unquoted = new StringBuilder();

        _fetchNextChar();    // Eats the start quote.

        while (_currentChar != quote) {
            if (_currentChar == '\0') {
                throw JDBCMessages.MISSING_QUOTE.exception();
            }

            if (_currentChar == '\\') {
                _fetchNextChar();

                if (('0' <= _currentChar) && (_currentChar <= '7')) {
                    int octal = _currentChar - '0';

                    if (('0' <= _nextChar) && (_nextChar <= '7')) {
                        _fetchNextChar();
                        octal = (octal << 3) + _currentChar - '0';

                        if (('0' <= _nextChar)
                                && (_nextChar <= '7')
                                && (octal < 040)) {
                            _fetchNextChar();
                            octal = (octal << 3) + _currentChar - '0';
                        }
                    }

                    _currentChar = (char) octal;
                } else {
                    switch (_currentChar) {
                        case 'a': {
                            _currentChar = (char) 0x7;

                            break;
                        }
                        case 'b': {
                            _currentChar = '\b';

                            break;
                        }
                        case 'f': {
                            _currentChar = (char) 0xC;

                            break;
                        }
                        case 'n': {
                            _currentChar = '\n';

                            break;
                        }
                        case 'r': {
                            _currentChar = '\r';

                            break;
                        }
                        case 't': {
                            _currentChar = '\t';

                            break;
                        }
                        case 'v': {
                            _currentChar = (char) 0xB;

                            break;
                        }
                        default: {
                            throw JDBCMessages.INVALID_ESCAPE.exception();
                        }
                    }
                }
            }

            unquoted.append(_currentChar);
            _fetchNextChar();
        }

        _fetchNextChar();    // Eats the stop quote.

        return new Token.Quoted(
            _text.substring(startPosition, _currentPosition),
            unquoted.toString());
    }

    private char _currentChar;
    private int _currentPosition;
    private char _nextChar;
    private int _nextPosition;
    private Token _nextToken;
    private Optional<List<Token.Parameter>> _parameters;
    private String _text;
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
