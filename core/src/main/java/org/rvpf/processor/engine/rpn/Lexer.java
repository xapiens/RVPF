/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Lexer.java 4017 2019-05-22 20:22:12Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.processor.ProcessorMessages;

/**
 * Makes lexical operations for the {@link Tokenizer}.
 */
final class Lexer
{
    /**
     * Constructs an instance.
     *
     * @param text The input text.
     *
     * @throws Compiler.CompileException On unterminated comment.
     */
    Lexer(@Nonnull final String text)
        throws Compiler.CompileException
    {
        _text = text;
        _nextChar = (_text.length() > 0)? _text.charAt(_nextPosition++): '\0';
        fetchNextChar();
        skipWhitespaces();
    }

    /**
     * Returns the current char.
     *
     * @return The current char.
     */
    @CheckReturnValue
    char currentChar()
    {
        return _currentChar;
    }

    /**
     * Fetches the next char.
     */
    void fetchNextChar()
    {
        _currentChar = _nextChar;

        if (_currentPosition < _nextPosition) {
            for (;;) {
                if (_nextPosition < _text.length()) {
                    _currentPosition = _nextPosition - 1;
                    _nextChar = _text.charAt(_nextPosition++);

                    if (_nextChar != '\0') {
                        break;
                    }
                } else {
                    _currentPosition = _nextPosition;
                    _nextChar = '\0';

                    break;
                }
            }
        }
    }

    /**
     * Gets the position of the current character.
     *
     * @return The position of the current character.
     */
    @CheckReturnValue
    int getCurrentPosition()
    {
        return _currentPosition;
    }

    /**
     * Gets the text.
     *
     * @return The text.
     */
    @Nonnull
    @CheckReturnValue
    String getText()
    {
        return _text;
    }

    /**
     * Gets the unquoted text.
     *
     * @return The unquoted text.
     *
     * @throws Compiler.CompileException On quoted text syntax error.
     */
    @Nonnull
    @CheckReturnValue
    String getUnquoted()
        throws Compiler.CompileException
    {
        final char quote = _currentChar;
        final StringBuilder stringBuilder = new StringBuilder();

        fetchNextChar();    // Eats the start quote.

        while (_currentChar != quote) {
            if (_currentChar == '\0') {
                throw new Compiler.CompileException(
                    ProcessorMessages.MISSING_QUOTE);
            }

            if (_currentChar == '\\') {
                fetchNextChar();

                if (('0' <= _currentChar) && (_currentChar <= '7')) {
                    int octal = _currentChar - '0';

                    if (('0' <= _nextChar) && (_nextChar <= '7')) {
                        fetchNextChar();
                        octal = (octal << 3) + _currentChar - '0';

                        if (('0' <= _nextChar)
                                && (_nextChar <= '7')
                                && (octal < 040)) {
                            fetchNextChar();
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
                        case '"':
                        case '\'': {
                            break;
                        }
                        default: {
                            throw new Compiler.CompileException(
                                ProcessorMessages.ESCAPE_USE);
                        }
                    }
                }
            }

            stringBuilder.append(_currentChar);
            fetchNextChar();
        }

        fetchNextChar();    // Eats the stop quote.

        final int position = _currentPosition;

        skipWhitespaces();
        _currentPosition = position;

        return stringBuilder.toString();
    }

    /**
     * Gets the word starting at the current position.
     *
     * @return The word starting at the current position.
     *
     * @throws Compiler.CompileException On unterminated comment.
     */
    @Nonnull
    @CheckReturnValue
    String getWord()
        throws Compiler.CompileException
    {
        final StringBuilder stringBuilder = new StringBuilder();

        do {
            stringBuilder.append(_currentChar);
            fetchNextChar();
        } while ((_currentChar != '\0')
                 && !Character.isWhitespace(_currentChar)
                 && (_currentChar != ',')
                 && (_currentChar != ')'));

        skipWhitespaces();

        return stringBuilder.toString();
    }

    /**
     * Skips whitespaces (including comments).
     *
     * @throws Compiler.CompileException On unterminated comment.
     */
    void skipWhitespaces()
        throws Compiler.CompileException
    {
        int position;

        do {
            while (Character.isWhitespace(_currentChar)) {
                fetchNextChar();
            }

            position = _nextPosition;
            _skipComments();
        } while (_nextPosition != position);
    }

    private void _skipComments()
        throws Compiler.CompileException
    {
        while ((_currentChar == '/') && (_nextChar == '*')) {
            fetchNextChar();    // Skips start of comment.
            fetchNextChar();

            for (;;) {
                if (_currentChar == '\0') {
                    throw new Compiler.CompileException(
                        ProcessorMessages.MISSING_COMMENT_END,
                        "/*",
                        "*/");
                }

                _skipComments();    // Allows nested comments.

                if ((_currentChar == '*') && (_nextChar == '/')) {
                    fetchNextChar();    // Skips end of comment.
                    fetchNextChar();

                    break;
                }

                fetchNextChar();    // Advances.
            }
        }
    }

    private char _currentChar;
    private int _currentPosition;
    private char _nextChar;
    private int _nextPosition;
    private final String _text;
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
