/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BooleanOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;
import org.rvpf.base.tool.Require;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Boolean operations.
 */
public final class BooleanOperations
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
            case AND: {
                _doAnd(stack);

                break;
            }
            case ASSERT: {
                _doAssert(stack);

                break;
            }
            case BOOL: {
                _doBool(stack);

                break;
            }
            case COND: {
                _doCond(stack);

                break;
            }
            case FALSE: {
                _doFalse(stack);

                break;
            }
            case FALSE_REQ: {
                _doFalseReq(stack);

                break;
            }
            case NOT: {
                _doNot(stack);

                break;
            }
            case OR: {
                _doOr(stack);

                break;
            }
            case TRUE: {
                _doTrue(stack);

                break;
            }
            case TRUE_REQ: {
                _doTrueReq(stack);

                break;
            }
            case XOR: {
                _doXor(stack);

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
        register("and", _Code.AND, BOTH_BOOLEAN);
        register("assert", _Code.ASSERT);
        register("bool", _Code.BOOL, TOP_BOOLEAN_OR_STRING);
        register("?:", _Code.COND, Filter.TOP_BOOLEAN);
        register("false", _Code.FALSE);
        register("false!", _Code.FALSE_REQ, Filter.TOP_BOOLEAN);
        register("not", _Code.NOT, Filter.TOP_BOOLEAN);
        register("or", _Code.OR, BOTH_BOOLEAN);
        register("true", _Code.TRUE);
        register("true!", _Code.TRUE_REQ, Filter.TOP_BOOLEAN);
        register("xor", _Code.XOR, BOTH_BOOLEAN);
    }

    private static void _doAnd(final Stack stack)
        throws Stack.AccessException
    {
        final boolean y = stack.popBooleanValue();
        final boolean x = stack.popBooleanValue();

        stack.push(x & y);
    }

    private static void _doAssert(
            final Stack stack)
        throws Task.ExecuteException, Stack.LimitsException
    {
        final Object x = stack.pop();

        if ((x == null)
                || ((x instanceof Boolean) && !((Boolean) x).booleanValue())) {
            fail();
        }
    }

    private static void _doBool(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        if (stack.peek() instanceof String) {
            final String x = stack.popStringValue();

            if ("true".equalsIgnoreCase(x)
                    || "on".equalsIgnoreCase(x)
                    || "yes".equalsIgnoreCase(x)
                    || "1".equals(x)) {
                stack.push(Boolean.TRUE);
            } else if ("false".equalsIgnoreCase(x)
                       || "off".equalsIgnoreCase(x)
                       || "no".equalsIgnoreCase(x)
                       || "0".equals(x)) {
                stack.push(Boolean.FALSE);
            } else {
                fail(ProcessorMessages.CONVERT_FAILED, x, "boolean");
            }
        } else {
            stack.push(stack.popLongValue() != 0);
        }
    }

    private static void _doCond(final Stack stack)
        throws Stack.AccessException
    {
        final boolean condition = stack.popBooleanValue();
        final Serializable x = stack.pop();

        if (condition) {
            stack.pop();
            stack.push(x);
        }
    }

    private static void _doFalse(final Stack stack)
    {
        stack.push(false);
    }

    private static void _doFalseReq(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        if (stack.popBooleanValue()) {
            fail();
        }
    }

    private static void _doNot(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(!stack.popBooleanValue());
    }

    private static void _doOr(final Stack stack)
        throws Stack.AccessException
    {
        final boolean y = stack.popBooleanValue();
        final boolean x = stack.popBooleanValue();

        stack.push(x | y);
    }

    private static void _doTrue(final Stack stack)
    {
        stack.push(true);
    }

    private static void _doTrueReq(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        if (!stack.popBooleanValue()) {
            fail();
        }
    }

    private static void _doXor(final Stack stack)
        throws Stack.AccessException
    {
        final boolean y = stack.popBooleanValue();
        final boolean x = stack.popBooleanValue();

        stack.push(x ^ y);
    }

    /** Both must be a boolean. */
    public static final Filter BOTH_BOOLEAN = new Filter()
        .is(0, Boolean.class)
        .is(1, Boolean.class)
        .and();

    /** Top must be a boolean or a string. */
    public static final Filter TOP_BOOLEAN_OR_STRING = new Filter()
        .is(0, Boolean.class)
        .is(0, String.class)
        .or();

    /**
     * Code.
     */
    private enum _Code
    {
        AND,
        ASSERT,
        BOOL,
        FALSE,
        FALSE_REQ,
        COND,
        NOT,
        OR,
        TRUE,
        TRUE_REQ,
        XOR
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
