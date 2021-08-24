/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MacroDef.java 4017 2019-05-22 20:22:12Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.processor.ProcessorMessages;

/**
 * Macro instruction definition.
 *
 * <p>Although the same definition may be shared by an engine and multiple
 * transforms, it is used only during a transform compilation, which is done one
 * at a time.</p>
 *
 * <h1>EBNF</h1>
 *
 * <ul>
 *   <li>macro-def = macro-name, [macro-param-list], spaces, words;</li>
 *   <li>macro-name = word;</li>
 *   <li>macro-param-list = '(', [spaces], param-spec, {[spaces], ',', [spaces],
 *     param-spec}, [spaces], ')';</li>
 *   <li>param-spec = (param-name | multiple-params), [required-param |
 *     default-param];</li>
 *   <li>multiple-params = '.', '.', '.';</li>
 *   <li>required-param = '!';</li>
 *   <li>default-param = '=', word - (',' | ')');</li>
 * </ul>
 *
 * @see Compiler
 */
public final class MacroDef
{
    /**
     * Constructs an instance.
     *
     * @param macro The definition text.
     *
     * @throws Compiler.CompileException When the definition is rejected.
     */
    MacroDef(@Nonnull String macro)
        throws Compiler.CompileException
    {
        macro = macro.trim();

        if (macro.isEmpty()) {
            throw new Compiler.CompileException(ProcessorMessages.EMPTY_MACRO);
        }

        final int leftParenIndex = macro.indexOf('(');
        final int spaceIndex = macro.indexOf(' ');

        if (leftParenIndex > 0) {
            if ((spaceIndex > 0) && (spaceIndex < leftParenIndex)) {
                _key = macro.substring(0, spaceIndex);
                _params = null;
                _defaults = null;
                _text = macro.substring(spaceIndex + 1).trim();
            } else {
                final int rightParenIndex = macro
                    .indexOf(')', leftParenIndex + 1);

                _key = macro.substring(0, leftParenIndex + 1);

                if (rightParenIndex < 0) {
                    throw new Compiler.CompileException(
                        ProcessorMessages.RIGHT_PARENTHESIS,
                        _key);
                }

                _params = _PARAMS_SPLIT_PATTERN
                    .split(
                        macro.substring(leftParenIndex + 1, rightParenIndex));
                _defaults = new String[_params.length];

                for (int i = 0; i < _params.length; ++i) {
                    String param = _params[i].trim();
                    final int index = param.indexOf(_DEFAULTED);

                    if (index >= 0) {
                        _defaults[i] = param.substring(index + 1).trim();
                        param = param.substring(0, index).trim();
                    } else if (param.endsWith(_REQUIRED)) {
                        _defaults[i] = _REQUIRED;
                        param = param
                            .substring(0, param.length() - _REQUIRED.length());
                    } else {
                        _defaults[i] = "";
                    }

                    if (param.isEmpty()) {
                        throw new Compiler.CompileException(
                            ProcessorMessages.EMPTY_MACRO_PARAM_NAME);
                    }

                    if ((i < (_params.length - 1)) && _ELLIPSIS.equals(param)) {
                        throw new Compiler.CompileException(
                            ProcessorMessages.ELLIPSIS_LAST);
                    }

                    _params[i] = param;
                }

                _text = macro.substring(rightParenIndex + 1).trim();
            }
        } else if (spaceIndex > 0) {
            _key = macro.substring(0, spaceIndex);
            _params = null;
            _defaults = null;
            _text = macro.substring(spaceIndex + 1).trim();
        } else {
            _key = macro;
            _params = null;
            _defaults = null;
            _text = "";
        }
    }

    /**
     * Gets the formal parameters default value.
     *
     * @return The formal parameters default value.
     */
    @Nullable
    @CheckReturnValue
    String[] getDefaults()
    {
        return _defaults;
    }

    /**
     * Gets this macro def's key.
     *
     * @return This macro def's key.
     */
    @Nonnull
    @CheckReturnValue
    String getKey()
    {
        return _key;
    }

    /**
     * Gets the formal parameters.
     *
     * @return The formal parameters.
     */
    @Nullable
    @CheckReturnValue
    String[] getParams()
    {
        return _params;
    }

    /**
     * Gets this macro def's text.
     *
     * @return This macro def's text.
     */
    @Nonnull
    @CheckReturnValue
    String getText()
    {
        return _text;
    }

    /**
     * Maps arguments to formal parameters.
     *
     * @param args The arguments.
     *
     * @return The value map.
     *
     * @throws Compiler.CompileException When the mapping is incomplete.
     */
    @Nonnull
    @CheckReturnValue
    Map<String, String> map(
            final String[] args)
        throws Compiler.CompileException
    {
        if (!((args.length <= _params.length)
                || ((_params.length > 0) && MacroDef._ELLIPSIS.equals(
                    _params[_params.length - 1])))) {
            throw new Compiler.CompileException(
                ProcessorMessages.MACRO_ARGS,
                getKey());
        }

        final Map<String, String> map = new HashMap<String, String>(
            KeyedValues.hashCapacity(_params.length));

        for (int i = 0; i < _params.length; ++i) {
            final String text;

            if (i < args.length) {
                text = args[i];
            } else {
                text = _defaults[i];

                if (text == _REQUIRED) {
                    throw new Compiler.CompileException(
                        ProcessorMessages.MACRO_ARG,
                        _params[i],
                        getKey());
                }
            }

            map.put(_params[i], text);
        }

        if ((args.length > _params.length)
                && (_params.length > 0)
                && MacroDef._ELLIPSIS.equals(_params[_params.length - 1])) {
            final StringBuilder stringBuilder = new StringBuilder(
                map.get(_ELLIPSIS));

            for (int i = _params.length; i < args.length; ++i) {
                stringBuilder.append(',');
                stringBuilder.append(args[i]);
            }

            map.put(_ELLIPSIS, stringBuilder.toString());
        }

        return map;
    }

    private static final char _DEFAULTED = '=';
    private static final String _ELLIPSIS = "...";
    private static final String _REQUIRED = "!";
    private static final Pattern _PARAMS_SPLIT_PATTERN = Pattern.compile(",");

    private final String[] _defaults;
    private final String _key;
    private final String[] _params;
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
