/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StringOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;

import java.util.Formatter;
import java.util.IllegalFormatException;
import java.util.Locale;

import org.rvpf.base.DateTime;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.State;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * String operations.
 */
public final class StringOperations
    extends SimpleOperations
{
    /** {@inheritDoc}
     */
    @Override
    public void execute(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Stack stack = task.getStack();

        switch ((_Code) reference.getCode()) {
            case ADD: {
                _doAdd(stack);

                break;
            }
            case DEBUG: {
                _doDebug(stack);

                break;
            }
            case EMPTY_Q: {
                _doEmptyQ(stack);

                break;
            }
            case ERROR: {
                _doError(stack);

                break;
            }
            case FORMAT: {
                _doFormat(stack);

                break;
            }
            case FORMAT_ALL: {
                _doFormatAll(stack);

                break;
            }
            case INFO: {
                _doInfo(stack);

                break;
            }
            case LOWER: {
                _doLower(stack);

                break;
            }
            case STR: {
                _doStr(stack);

                break;
            }
            case STR_Q: {
                _doStrQ(stack);

                break;
            }
            case SUBSTRING: {
                _doSubstring(stack);

                break;
            }
            case TRIM: {
                _doTrim(stack);

                break;
            }
            case UPPER: {
                _doUpper(stack);

                break;
            }
            case WARN: {
                _doWarn(stack);

                break;
            }
            default: {
                Require.failure();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void setUp()
        throws Operation.OverloadException
    {
        register("+", _Code.ADD, Filter.BOTH_STRING);
        register("debug", _Code.DEBUG, Filter.TOP_PRESENT);
        register("empty?", _Code.EMPTY_Q, Filter.TOP_STRING);
        register("error", _Code.ERROR, Filter.TOP_PRESENT);
        register("format", _Code.FORMAT, Filter.TOP_STRING);
        register("format*", _Code.FORMAT_ALL, Filter.TOP_STRING);
        register("info", _Code.INFO, Filter.TOP_PRESENT);
        register("lower", _Code.LOWER, Filter.TOP_STRING);
        register("str", _Code.STR, Filter.TOP_PRESENT);
        register("str?", _Code.STR_Q, Filter.TOP_PRESENT);
        register("substring", _Code.SUBSTRING, _SUBSTRING_FILTER);
        register("trim", _Code.TRIM, Filter.TOP_STRING);
        register("upper", _Code.UPPER, Filter.TOP_STRING);
        register("warn", _Code.WARN, Filter.TOP_PRESENT);
    }

    private static void _doAdd(final Stack stack)
        throws Stack.AccessException
    {
        final String y = stack.popStringValue();
        final String x = stack.popStringValue();

        stack.push(x + y);
    }

    private static void _doEmptyQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(stack.popStringValue().isEmpty());
    }

    private static void _doFormat(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final String format = stack.popStringValue();

        stack.push(_format(format, stack.toArray()));
    }

    private static void _doFormatAll(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final String format = stack.popStringValue();

        stack.push(_format(format, stack.toArrayAll()));
    }

    private static void _doLower(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(stack.popStringValue().toLowerCase(Locale.ROOT));
    }

    private static void _doStr(final Stack stack)
        throws Stack.LimitsException
    {
        final Serializable x = stack.pop();

        if (x instanceof DateTime) {
            stack
                .push(
                    stack
                        .getTask()
                        .getContext()
                        .getDateTimeContext()
                        .toString((DateTime) x));
        } else if (x instanceof State) {
            final State state = (State) x;
            final String name = state.getName().orElse(null);

            stack
                .push(
                    (name != null)? name: String
                        .valueOf(state.getCode().orElse(null)));
        } else {
            stack.push(String.valueOf(x));
        }
    }

    private static void _doStrQ(final Stack stack)
        throws Stack.LimitsException
    {
        stack.push(stack.pop() instanceof String);
    }

    private static void _doSubstring(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final int to = stack.popIntValue();
        final int from = stack.popIntValue();
        final String string = stack.popStringValue();

        try {
            stack.push(string.substring(from, to));
        } catch (final IndexOutOfBoundsException exception) {
            fail(
                ProcessorMessages.SUBSTRING_INDEX_OUT_OF_BOUNDS,
                string,
                String.valueOf(from),
                String.valueOf(to));
        }
    }

    private static void _doTrim(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(stack.popStringValue().trim());
    }

    private static void _doUpper(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(stack.popStringValue().toUpperCase(Locale.ROOT));
    }

    private static String _format(
            final String format,
            final Object[] values)
        throws Task.ExecuteException
    {
        final Formatter formatter = new Formatter((Locale) null);
        final String result;

        try {
            formatter.format(format, values);
            result = formatter.toString();
        } catch (final IllegalFormatException exception) {
            fail(
                ProcessorMessages.FORMAT_FAILED,
                format,
                exception.getClass().getSimpleName(),
                exception.getMessage());

            throw new InternalError();    // Should not execute.
        } finally {
            formatter.close();
        }

        return result;
    }

    private void _doDebug(final Stack stack)
        throws Stack.LimitsException
    {
        getThisLogger()
            .debug(ProcessorMessages.DEBUG, String.valueOf(stack.pop()));
    }

    private void _doError(
            final Stack stack)
        throws Task.ExecuteException, Stack.LimitsException
    {
        getThisLogger()
            .error(ProcessorMessages.ERROR, String.valueOf(stack.pop()));
        fail();
    }

    private void _doInfo(final Stack stack)
        throws Stack.LimitsException
    {
        getThisLogger()
            .info(ProcessorMessages.INFO, String.valueOf(stack.pop()));
    }

    private void _doWarn(final Stack stack)
        throws Stack.LimitsException
    {
        getThisLogger()
            .warn(ProcessorMessages.WARN, String.valueOf(stack.pop()));
    }

    private static final Filter _SUBSTRING_FILTER = new Filter()
        .isLong(0)
        .isLong(1)
        .and()
        .is(2, String.class)
        .and();

    /**
     * Code.
     */
    private enum _Code
    {
        ADD,
        DEBUG,
        EMPTY_Q,
        ERROR,
        FORMAT,
        FORMAT_ALL,
        INFO,
        LOWER,
        STR,
        STR_Q,
        SUBSTRING,
        TRIM,
        UPPER,
        WARN
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
