/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StackOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;

import javax.management.ObjectName;

import org.rvpf.base.tool.Require;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;
import org.rvpf.processor.engine.rpn.Task.ExecuteException;

/**
 * Stack operations.
 */
public final class StackOperations
    extends SimpleOperations
{
    /** {@inheritDoc}
     */
    @Override
    public void execute(
            final Task task,
            final SimpleOperation.Reference reference)
        throws ExecuteException, Stack.AccessException
    {
        final Stack stack = task.getStack();

        switch ((_Code) reference.getCode()) {
            case AT: {
                _doAt(stack);

                break;
            }
            case CLEAR: {
                _doClear(stack);

                break;
            }
            case COPY: {
                _doCopy(stack);

                break;
            }
            case DEFAULT: {
                _doDefault(stack);

                break;
            }
            case DEPTH: {
                _doDepth(stack);

                break;
            }
            case DEPTH_ALL: {
                _doDepthAll(stack);

                break;
            }
            case DROP: {
                _doDrop(stack);

                break;
            }
            case DUMP: {
                _doDump(stack);

                break;
            }
            case DUMP_ALL: {
                _doDumpAll(stack);

                break;
            }
            case DUP: {
                _doDup(stack);

                break;
            }
            case EQ: {
                _doEq(stack);

                break;
            }
            case MARK: {
                _doMark(stack);

                break;
            }
            case MARK_Q: {
                _doMarkQ(stack);

                break;
            }
            case NE: {
                _doNe(stack);

                break;
            }
            case NIP: {
                _doNip(stack);

                break;
            }
            case NULL: {
                _doNull(stack);

                break;
            }
            case NULL_Q: {
                _doNullQ(stack);

                break;
            }
            case OVER: {
                _doOver(stack);

                break;
            }
            case REVERSE: {
                _doReverse(stack);

                break;
            }
            case ROLL: {
                _doRoll(stack);

                break;
            }
            case SWAP: {
                _doSwap(stack);

                break;
            }
            case TUCK: {
                _doTuck(stack);

                break;
            }
            case UNMARK: {
                _doUnmark(stack);

                break;
            }
            case UNMARK_ALL: {
                _doUnmarkAll(stack);

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
        register("at", _Code.AT, Filter.TOP_LONG);
        register("clear", _Code.CLEAR, Filter.TOP_LONG);
        register("copy", _Code.COPY, Filter.TOP_LONG);
        register("default", _Code.DEFAULT, Filter.BOTH_PRESENT);
        register("depth", _Code.DEPTH);
        register("depth*", _Code.DEPTH_ALL);
        register("drop", _Code.DROP, Filter.TOP_PRESENT);
        register("dump", _Code.DUMP);
        register("dump*", _Code.DUMP_ALL);
        register("dup", _Code.DUP, Filter.TOP_PRESENT);
        register(":", _Code.DUP, Filter.TOP_PRESENT);
        register("eq", _Code.EQ, Filter.BOTH_PRESENT);
        register("mark", _Code.MARK);
        register("[", _Code.MARK);
        register("mark?", _Code.MARK_Q);
        register("ne", _Code.NE, Filter.BOTH_PRESENT);
        register("nip", _Code.NIP, Filter.BOTH_PRESENT);
        register("null", _Code.NULL);
        register("null?", _Code.NULL_Q, Filter.TOP_PRESENT);
        register("over", _Code.OVER, Filter.BOTH_PRESENT);
        register("reverse", _Code.REVERSE);
        register("roll", _Code.ROLL, Filter.BOTH_LONG);
        register("swap", _Code.SWAP, Filter.BOTH_PRESENT);
        register("tuck", _Code.TUCK, Filter.BOTH_PRESENT);
        register("unmark", _Code.UNMARK);
        register("]", _Code.UNMARK);
        register("unmark*", _Code.UNMARK_ALL);
    }

    private static void _doAt(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(stack.peek(stack.popIntValue()));
    }

    private static void _doClear(final Stack stack)
        throws Stack.AccessException
    {
        int n = stack.popIntValue();

        while (n-- > 0) {
            stack.pop();
        }
    }

    private static void _doCopy(final Stack stack)
        throws Stack.AccessException
    {
        final int offset = stack.popIntValue();
        int n = offset;

        while (n-- > 0) {
            final Serializable object = stack.peek(offset);

            stack.push(object);
        }
    }

    private static void _doDefault(
            final Stack stack)
        throws Stack.LimitsException
    {
        final Serializable x = stack.pop();

        if (stack.peek() == null) {
            stack.pop();
            stack.push(x);
        }
    }

    private static void _doDepth(final Stack stack)
    {
        stack.push(Integer.valueOf(stack.size()));
    }

    private static void _doDepthAll(final Stack stack)
    {
        stack.push(Integer.valueOf(stack.totalSize()));
    }

    private static void _doDrop(final Stack stack)
        throws Stack.LimitsException
    {
        stack.pop();
    }

    private static void _doDup(final Stack stack)
        throws Stack.LimitsException
    {
        stack.push(stack.peek());
    }

    private static void _doEq(final Stack stack)
        throws Stack.LimitsException
    {
        final Object y = stack.pop();
        final Object x = stack.pop();

        if (y == null) {
            stack.push(x == null);
        } else {
            stack.push(y.equals(x));
        }
    }

    private static void _doMark(final Stack stack)
    {
        Require.notNull(stack.mark());
    }

    private static void _doMarkQ(final Stack stack)
    {
        stack.push(stack.isMarked());
    }

    private static void _doNe(final Stack stack)
        throws Stack.LimitsException
    {
        final Object y = stack.pop();
        final Object x = stack.pop();

        if (y == null) {
            stack.push(x != null);
        } else {
            stack.push(!y.equals(x));
        }
    }

    private static void _doNip(final Stack stack)
        throws Stack.LimitsException
    {
        final Serializable x = stack.pop();

        stack.pop();
        stack.push(x);
    }

    private static void _doNull(final Stack stack)
    {
        stack.push((Serializable) null);
    }

    private static void _doNullQ(final Stack stack)
        throws Stack.LimitsException
    {
        stack.push(stack.pop() == null);
    }

    private static void _doOver(final Stack stack)
        throws Stack.LimitsException
    {
        stack.push(stack.peek(1));
    }

    private static void _doReverse(final Stack stack)
    {
        stack.reverse();
    }

    private static void _doRoll(final Stack stack)
        throws Stack.AccessException
    {
        int j = stack.popIntValue();
        final int n = stack.popIntValue();
        Serializable object;

        if (j < 0) {
            while (j++ < 0) {
                object = stack.remove(n);
                stack.push(object);
            }
        } else {
            while (j-- > 0) {
                object = stack.pop();
                stack.insert(n - 1, object);
            }
        }
    }

    private static void _doSwap(final Stack stack)
        throws Stack.LimitsException
    {
        final Serializable y = stack.pop();
        final Serializable x = stack.pop();

        stack.push(y);
        stack.push(x);
    }

    private static void _doTuck(final Stack stack)
        throws Stack.LimitsException
    {
        final Serializable y = stack.pop();
        final Serializable x = stack.pop();

        stack.push(y);
        stack.push(x);
        stack.push(y);
    }

    private static void _doUnmark(final Stack stack)
    {
        stack.unmark();
    }

    private static void _doUnmarkAll(final Stack stack)
    {
        stack.unmarkAll();
    }

    private void _doDump(final Stack stack)
    {
        _dump(stack.toArray());
    }

    private void _doDumpAll(final Stack stack)
    {
        _dump(stack.toArrayAll());
    }

    private void _dump(final Object[] values)
    {
        if (getThisLogger().isDebugEnabled()) {
            final StringBuilder stringBuilder = new StringBuilder();

            for (final Object value: values) {
                stringBuilder.append("\n\t\t\t\t");

                if (value != null) {
                    stringBuilder.append(value.getClass().getSimpleName());
                    stringBuilder.append(": ");
                }

                if (value instanceof String) {
                    stringBuilder.append(ObjectName.quote((String) value));
                } else {
                    stringBuilder.append(String.valueOf(value));
                }
            }

            stringBuilder.append("\n\t\t\t\tEnd");
            getThisLogger().debug(ProcessorMessages.RPN_STACK, stringBuilder);
        }
    }

    /**
     * Code.
     */
    private enum _Code
    {
        AT,
        CLEAR,
        COPY,
        DEFAULT,
        DEPTH,
        DEPTH_ALL,
        DROP,
        DUMP,
        DUMP_ALL,
        DUP,
        EQ,
        NE,
        MARK,
        MARK_Q,
        NIP,
        NULL,
        NULL_Q,
        OVER,
        REVERSE,
        ROLL,
        SWAP,
        TUCK,
        UNMARK,
        UNMARK_ALL
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
