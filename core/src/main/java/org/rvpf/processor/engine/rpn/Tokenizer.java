/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Tokenizer.java 4031 2019-05-27 14:17:25Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.processor.ProcessorMessages;

/**
 * Produces {@link Token}s for the {@link Compiler}.
 */
final class Tokenizer
{
    /**
     * Gets the current position in the source string.
     *
     * @return The current position in the source string.
     */
    @CheckReturnValue
    int getPosition()
    {
        return _lexers
            .isEmpty()? _lexer
                .getCurrentPosition(): _lexers.getFirst().getCurrentPosition();
    }

    /**
     * Gets the source string.
     *
     * @return The source string.
     */
    @Nonnull
    @CheckReturnValue
    String getSource()
    {
        return _lexer.getText();
    }

    /**
     * Initializes.
     *
     * @param source The source string for the program.
     * @param macroDefs The optional macro instruction definitions.
     * @param loopLimit The recursion loop limit.
     *
     * @return This tokenizer.
     *
     * @throws Compiler.CompileException When the initial source scan fails.
     */
    @Nonnull
    Tokenizer initialize(
            @Nonnull final String source,
            @Nonnull final Optional<Map<String, MacroDef>> macroDefs,
            final int loopLimit)
        throws Compiler.CompileException
    {
        _preprocessor = new Preprocessor(this, macroDefs, loopLimit);
        _lexer = new Lexer(source);
        _fetchNextToken();

        return this;
    }

    /**
     * Inserts source text.
     *
     * <p>Called by the preprocessor when expanding macros.</p>
     *
     * @param source The source text.
     *
     * @throws Compiler.CompileException On unterminated comment.
     */
    void insert(@Nonnull final String source)
        throws Compiler.CompileException
    {
        _lexers.addLast(_lexer);
        _lexer = new Lexer(source);
    }

    /**
     * Returns the next token.
     *
     * @return The next token or empty.
     *
     * @throws Compiler.CompileException When preprocessing detects a problem.
     */
    @Nonnull
    Optional<Token> nextToken()
        throws Compiler.CompileException
    {
        if (_lexer == null) {
            return Optional.empty();
        }

        Token nextToken;

        for (;;) {
            nextToken = _nextToken;

            if (nextToken != null) {
                if (_preprocessor.processToken(nextToken)) {
                    _fetchNextToken();

                    continue;
                }
            } else if (_preprocessor.processEndOfToken()) {
                _close();
                _fetchNextToken();

                continue;
            }

            if (nextToken != null) {
                _fetchNextToken();
            } else {
                _preprocessor.close();
                _close();
            }

            break;
        }

        return Optional.ofNullable(nextToken);
    }

    /**
     * Peeks at the next token.
     *
     * @return The optional next token.
     */
    @Nonnull
    @CheckReturnValue
    Optional<Token> peek()
    {
        return Optional.ofNullable(_nextToken);
    }

    private void _close()
    {
        _lexer = _lexers.isEmpty()? null: _lexers.removeLast();
    }

    private void _fetchNextToken()
        throws Compiler.CompileException
    {
        final char current = _lexer.currentChar();

        switch (current) {
            case '\0': {
                _nextToken = null;

                break;
            }
            case '"':
            case '\'': {
                final int startPosition = _lexer.getCurrentPosition();
                final String unquoted = _lexer.getUnquoted();

                _nextToken = new Token.TextConstant(
                    _lexer.getText().substring(
                        startPosition,
                        _lexer.getCurrentPosition()),
                    unquoted);

                break;
            }
            case ',': {
                _nextToken = Token.COMMA;
                _lexer.fetchNextChar();
                _lexer.skipWhitespaces();

                break;
            }
            case ')': {
                _nextToken = Token.RIGHT_PAREN;
                _lexer.fetchNextChar();
                _lexer.skipWhitespaces();

                break;
            }
            default: {
                final String text = _lexer.getWord();
                final Matcher matcher = Tokenizer._VALUE_PATTERN.matcher(text);

                if (matcher.matches()) {
                    _nextToken = new Token.VariableActionName(
                        text,
                        matcher.group(_DUP_GROUP) != null,
                        matcher.group(_TYPE_GROUP).charAt(0),
                        Integer.valueOf(matcher.group(_IDENTIFIER_GROUP)),
                        (matcher.group(
                            _ACTION_GROUP) != null)? matcher
                                .group(_ACTION_GROUP)
                                .charAt(0): ' ');
                } else if ("(".equals(text)) {
                    char next;

                    do {
                        next = _lexer.currentChar();

                        if (next == '\0') {
                            throw new Compiler.CompileException(
                                ProcessorMessages.MISSING_COMMENT_END,
                                "(",
                                ")");
                        }

                        _lexer.fetchNextChar();
                    } while (next != ')');

                    _lexer.skipWhitespaces();
                    _fetchNextToken();
                } else {
                    String token = text;

                    if (token.charAt(0) == '+') {
                        token = token.substring(1);
                    }

                    try {
                        _nextToken = new Token.NumericConstant(
                            text,
                            Long.decode(token));
                    } catch (final NumberFormatException exception1) {
                        try {
                            _nextToken = new Token.NumericConstant(
                                text,
                                Double.valueOf(token));
                        } catch (final NumberFormatException exception2) {
                            _nextToken = new Token.OtherName(text);
                        }
                    }
                }

                break;
            }
        }
    }

    static final char INPUT_TYPE = '$';
    static final char MEMORY_TYPE = '#';
    static final char PARAM_TYPE = '@';
    static final char POINT_ACTION = '.';
    static final char PRESENT_ACTION = '?';
    static final char REQUIRED_ACTION = '!';
    static final char STAMP_ACTION = '@';
    static final char STATE_ACTION = '$';
    static final char STORE_ACTION = '=';
    static final char VALUE_ACTION = ' ';

    /**  */

    private static final int _ACTION_GROUP = 4;
    private static final int _DUP_GROUP = 1;
    private static final int _IDENTIFIER_GROUP = 3;
    private static final int _TYPE_GROUP = 2;
    private static final Pattern _VALUE_PATTERN = Pattern
        .compile("(:)?(\\$|#|@)([1-9][0-9]*+)(=|!|\\?|@|\\$|\\.)?");

    private Lexer _lexer;
    private final LinkedList<Lexer> _lexers = new LinkedList<Lexer>();
    private Token _nextToken;
    private Preprocessor _preprocessor;
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
