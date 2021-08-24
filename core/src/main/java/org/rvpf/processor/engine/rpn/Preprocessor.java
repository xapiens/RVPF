/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Preprocessor.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.processor.ProcessorMessages;

/**
 * Processes tokens supplied by the {@link Tokenizer} by expanding macros before
 * returning a {@link Token} to the {@link Compiler}.
 */
final class Preprocessor
{
    /**
     * Constructs an inatance.
     *
     * @param tokenizer The calling tokenizer.
     * @param macroDefs The optional macro instruction definitions.
     * @param loopLimit The recursion loop limit.
     */
    Preprocessor(
            @Nonnull final Tokenizer tokenizer,
            @Nonnull final Optional<Map<String, MacroDef>> macroDefs,
            final int loopLimit)
    {
        _tokenizer = tokenizer;
        _macroDefs = macroDefs;
        _loopLimit = loopLimit;

        if (_macroDefs.isPresent()) {
            _expansion = new StringBuilder();
        }
    }

    /**
     * Closes this instance.
     */
    void close()
    {
        if (_expanded) {
            _LOGGER.trace(ProcessorMessages.MACRO_EXPANSION, _expansion);
            _expanded = false;
            _expansion = null;
        }

        _recursions = null;
        _macroDefs = null;
        _tokenizer = null;
    }

    /**
     * Processes end of token.
     *
     * @return True on success.
     *
     * @throws Compiler.CompileException When macro expansion detects a problem.
     */
    @CheckReturnValue
    boolean processEndOfToken()
        throws Compiler.CompileException
    {
        // Ends insertion or source.

        if ((_args != null)
                && ((_macro == null) || (_macro.getMacroDef() != null))) {
            throw new Compiler.CompileException(
                ProcessorMessages.UNBALANCED_PARENTHESIS,
                _name);
        }

        return _popMacro();
    }

    /**
     * Processes a token if recognized by this preprocessor.
     *
     * @param token The token.
     *
     * @return True if the token has been processed.
     *
     * @throws Compiler.CompileException When macro expansion detects a problem.
     */
    @CheckReturnValue
    boolean processToken(
            @Nonnull final Token token)
        throws Compiler.CompileException
    {
        Require.notNull(_tokenizer);

        if (!_macroDefs.isPresent()) {    // No definitions: just ignores commas.
            return token == Token.COMMA;
        }

        if (_args != null) {    // Collecting macro args.
            _collectMacroArgs(token);

            return true;
        }

        if (token == Token.COMMA) {    // Ignores commas outside of macro args.
            return true;
        }

        if (token.isOtherName()) {    // May be a macro.
            final String word = token.toString();

            {
                final int index = word.indexOf('(');

                if (index > 0) {    // Looks like a macro with args.
                    _name = word.substring(0, index + 1);
                    _args = new LinkedList<>();

                    if (word.length() > (index + 1)) {    // Splits the args.
                        _pushMacro(
                            new _Macro(_macro),
                            word.substring(index + 1));
                    }

                    return true;
                }
            }

            if (_macro != null) {    // Expanding a macro.
                final String text = _macro.getArg(word);

                if (text != null) {    // Token is a formal parameter name.
                    _pushMacro(_NULL_MACRO, text);

                    return true;
                }
            }

            // Recognizes word macros, but not recursively.
            {
                final MacroDef macroDef = _macroDefs.get().get(word);

                if ((macroDef != null)
                        && ((_recursions == null) || !_recursions.containsKey(
                            macroDef))) {
                    _pushMacro(macroDef, macroDef.getText());

                    return true;
                }
            }
        }

        // No processing done on this token.

        _expansion.append(' ');
        _expansion.append(token);

        return false;
    }

    private void _collectMacroArgs(
            final Token token)
        throws Compiler.CompileException
    {
        if ((token == Token.COMMA) && (_parenLevel == 0)) {    // Next arg.
            _args.add((_arg != null)? _arg.toString(): "");
            _arg = new StringBuilder();
        } else if (token != Token.RIGHT_PAREN) {    // Some arg text.
            String arg = token.toString();
            int position = 0;

            for (;;) {    // Updates parenthesis level.
                position = arg.indexOf('(', position);

                if (position < 0) {
                    break;
                }

                ++_parenLevel;
                position = position + 1;
            }

            if (_arg == null) {    // Beginning the arg.
                _arg = new StringBuilder();
            } else {    // Continuing the arg.
                _arg.append(' ');
            }

            if ((_macro != null)
                    && (token.isOtherName())) {    // Expanding a macro.
                final String text = _macro.getArg(arg);

                if (text != null) {    // Token is a formal parameter name.
                    arg = text;
                }
            }

            _arg.append(arg);
        } else if (_parenLevel > 0) {    // Nested arg.
            _arg.append(token.toString());
            --_parenLevel;
        } else {    // End of args.
            if (_arg != null) {
                _args.add(_arg.toString());
                _arg = null;
            }

            final MacroDef macroDef = _macroDefs.get().get(_name);

            if (macroDef == null) {
                throw new Compiler.CompileException(
                    ProcessorMessages.UNKNOWN_MACRO,
                    _name);
            }

            _pushMacro(macroDef, macroDef.getText());
            _name = null;
        }
    }

    private boolean _popMacro()
    {
        if (_macro == null) {
            return false;
        }

        if (_macro.getMacroDef() != null) {
            _recursions
                .put(
                    _macro.getMacroDef(),
                    Integer
                        .valueOf(
                                _recursions
                                        .get(_macro.getMacroDef())
                                        .intValue() - 1));
        }

        _macro = ((_macros != null)
                  && !_macros.isEmpty())? _macros.removeLast(): null;

        return true;
    }

    private void _pushMacro(
            final _Macro macro,
            final String text)
        throws Compiler.CompileException
    {
        if (!_expanded) {
            _LOGGER
                .trace(ProcessorMessages.MACRO_SOURCE, _tokenizer.getSource());
        }

        if (_macro != null) {
            if (_macros == null) {
                _macros = new LinkedList<>();
            }

            _macros.addLast(_macro);
        }

        _macro = macro;

        _tokenizer.insert(text);
        _expanded = true;
    }

    private void _pushMacro(
            final MacroDef macroDef,
            final String text)
        throws Compiler.CompileException
    {
        if (text.length() > 0) {
            if (_recursions == null) {
                _recursions = new IdentityHashMap<>();
            }

            final Integer recursion = _recursions.get(macroDef);

            if ((recursion != null) && (recursion.intValue() >= _loopLimit)) {
                throw new Compiler.CompileException(
                    ProcessorMessages.MACRO_RECURSION,
                    macroDef.getKey());
            }

            _pushMacro(new _Macro(macroDef), text);

            if (_args != null) {
                _macro.map(_args);
                _args = null;
            }

            _recursions
                .put(
                    macroDef,
                    Integer
                        .valueOf((recursion != null)
                        ? (recursion.intValue() + 1): 0));
        }
    }

    private static final _Macro _NULL_MACRO = new _Macro((MacroDef) null);
    private static final Logger _LOGGER = Logger
        .getInstance(Preprocessor.class);

    private StringBuilder _arg;
    private List<String> _args;
    private boolean _expanded;
    private StringBuilder _expansion;
    private final int _loopLimit;
    private _Macro _macro;
    private Optional<Map<String, MacroDef>> _macroDefs;
    private LinkedList<_Macro> _macros;
    private String _name;
    private int _parenLevel;
    private Map<MacroDef, Integer> _recursions;
    private Tokenizer _tokenizer;

    private static final class _Macro
    {
        _Macro(final _Macro macro)
        {
            this((MacroDef) null);

            if (macro != null) {
                _args = macro._args;
            }
        }

        _Macro(final MacroDef macroDef)
        {
            _macroDef = macroDef;
        }

        /**
         * Gets the text of an argument corresponding to a parameter name.
         *
         * @param name The parameter name.
         *
         * @return The text of the argument or null.
         */
        String getArg(final String name)
        {
            return (_args != null)? _args.get(name): null;
        }

        MacroDef getMacroDef()
        {
            return _macroDef;
        }

        void map(final List<String> args)
            throws Compiler.CompileException
        {
            _args = _macroDef.map(args.toArray(new String[args.size()]));
        }

        private Map<String, String> _args;
        private final MacroDef _macroDef;
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
