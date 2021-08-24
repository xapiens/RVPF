/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LongOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import org.rvpf.base.tool.Require;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Long operations.
 */
public final class LongOperations
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
            case ABS: {
                _doAbs(stack);

                break;
            }
            case ADD: {
                _doAdd(stack);

                break;
            }
            case AND: {
                _doAnd(stack);

                break;
            }
            case DEC: {
                _doDec(stack);

                break;
            }
            case DIV: {
                _doDiv(stack);

                break;
            }
            case DIV_REM: {
                _doDivRem(stack);

                break;
            }
            case EQ: {
                _doEq(stack);

                break;
            }
            case GE: {
                _doGe(stack);

                break;
            }
            case GT: {
                _doGt(stack);

                break;
            }
            case INC: {
                _doInc(stack);

                break;
            }
            case INT: {
                _doInt(stack);

                break;
            }
            case INT_Q: {
                _doIntQ(stack);

                break;
            }
            case LE: {
                _doLe(stack);

                break;
            }
            case LSHFT: {
                _doLshft(stack);

                break;
            }
            case LT: {
                _doLt(stack);

                break;
            }
            case MAX: {
                _doMax(stack);

                break;
            }
            case MIN: {
                _doMin(stack);

                break;
            }
            case MOD: {
                _doMod(stack);

                break;
            }
            case MUL: {
                _doMul(stack);

                break;
            }
            case NE: {
                _doNe(stack);

                break;
            }
            case NEG: {
                _doNeg(stack);

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
            case REM: {
                _doRem(stack);

                break;
            }
            case RSHFT: {
                _doRshft(stack);

                break;
            }
            case RSHFTZ: {
                _doRshftz(stack);

                break;
            }
            case SGN: {
                _doSgn(stack);

                break;
            }
            case SUB: {
                _doSub(stack);

                break;
            }
            case XOR: {
                _doXor(stack);

                break;
            }
            case ZERO_MINUS_Q: {
                _doZeroMinusQ(stack);

                break;
            }
            case ZERO_PLUS_Q: {
                _doZeroPlusQ(stack);

                break;
            }
            case ZERO_Q: {
                _doZeroQ(stack);

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
        register("0?", _Code.ZERO_Q, Filter.TOP_LONG);
        register("0+?", _Code.ZERO_PLUS_Q, Filter.TOP_LONG);
        register("0-?", _Code.ZERO_MINUS_Q, Filter.TOP_LONG);
        register("abs", _Code.ABS, Filter.TOP_LONG);
        register("+", _Code.ADD, Filter.BOTH_LONG);
        register("and", _Code.AND, Filter.BOTH_LONG);
        register("/", _Code.DIV, Filter.BOTH_LONG);
        register("/%", _Code.DIV_REM, Filter.BOTH_LONG);
        register("--", _Code.DEC, Filter.TOP_LONG);
        register("eq", _Code.EQ, Filter.BOTH_LONG);
        register("ge", _Code.GE, Filter.BOTH_LONG);
        register("gt", _Code.GT, Filter.BOTH_LONG);
        register("++", _Code.INC, Filter.TOP_LONG);
        register("int", _Code.INT, TOP_NUMBER_OR_STRING);
        register("int?", _Code.INT, Filter.TOP_PRESENT);
        register("le", _Code.LE, Filter.BOTH_LONG);
        register("lshft", _Code.LSHFT, Filter.BOTH_LONG);
        register("lt", _Code.LT, Filter.BOTH_LONG);
        register("max", _Code.MAX, Filter.BOTH_LONG);
        register("min", _Code.MIN, Filter.BOTH_LONG);
        register("mod", _Code.MOD, Filter.BOTH_LONG);
        register("*", _Code.MUL, Filter.BOTH_LONG);
        register("ne", _Code.NE, Filter.BOTH_LONG);
        register("neg", _Code.NEG, Filter.TOP_LONG);
        register("not", _Code.NOT, Filter.TOP_LONG);
        register("or", _Code.OR, Filter.BOTH_LONG);
        register("%", _Code.REM, Filter.BOTH_LONG);
        register("rshft", _Code.RSHFT, Filter.BOTH_LONG);
        register("rshftz", _Code.RSHFTZ, Filter.BOTH_LONG);
        register("sgn", _Code.SGN, Filter.TOP_LONG);
        register("-", _Code.SUB, Filter.BOTH_LONG);
        register("xor", _Code.XOR, Filter.BOTH_LONG);
    }

    private static void _doAbs(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.abs(stack.popLongValue()));
    }

    private static void _doAdd(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(x + y);
    }

    private static void _doAnd(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(x & y);
    }

    private static void _doDec(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(stack.popLongValue() - 1);
    }

    private static void _doDiv(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        try {
            stack.push(x / y);
        } catch (final ArithmeticException exception) {
            fail(
                ProcessorMessages.DIVISION_ERROR,
                String.valueOf(x),
                String.valueOf(y));
        }
    }

    private static void _doDivRem(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        long y = stack.popLongValue();
        long x = stack.popLongValue();

        if (y < 0) {
            x = -x;
            y = -y;
        }

        try {
            stack.push(x % y);
            stack.push(x / y);
        } catch (final ArithmeticException exception) {
            fail(
                ProcessorMessages.DIVISION_ERROR,
                String.valueOf(x),
                String.valueOf(y));
        }
    }

    private static void _doEq(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(y == x);
    }

    private static void _doGe(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(y >= x);
    }

    private static void _doGt(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(y > x);
    }

    private static void _doInc(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(stack.popLongValue() + 1);
    }

    private static void _doInt(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        if (stack.peek() instanceof String) {
            final String x = stack.popStringValue();

            try {
                stack.push(Long.parseLong(x));
            } catch (final NumberFormatException exception) {
                fail(ProcessorMessages.CONVERT_FAILED, x, "int");
            }
        } else {
            stack.push(stack.popLongValue());
        }
    }

    private static void _doIntQ(final Stack stack)
        throws Stack.LimitsException
    {
        final Object x = stack.pop();
        boolean isInt;

        if (x instanceof String) {
            try {
                Long.parseLong((String) x);
                isInt = true;
            } catch (final NumberFormatException exception) {
                isInt = false;
            }
        } else {
            isInt = (x instanceof Long)
                    || (x instanceof Integer)
                    || (x instanceof Short)
                    || (x instanceof Byte);
        }

        stack.push(isInt);
    }

    private static void _doLe(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(y <= x);
    }

    private static void _doLshft(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(x << y);
    }

    private static void _doLt(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(y < x);
    }

    private static void _doMax(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(Math.max(x, y));
    }

    private static void _doMin(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(Math.min(x, y));
    }

    private static void _doMod(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        long y = stack.popLongValue();
        long x = stack.popLongValue();

        if (y < 0) {
            x = -x;
            y = -y;
        }

        try {
            final long result = x % y;

            stack.push((result >= 0)? result: (result + y));
        } catch (final ArithmeticException exception) {
            fail(
                ProcessorMessages.DIVISION_ERROR,
                String.valueOf(x),
                String.valueOf(y));
        }
    }

    private static void _doMul(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(x * y);
    }

    private static void _doNe(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(y != x);
    }

    private static void _doNeg(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(-stack.popLongValue());
    }

    private static void _doNot(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(~stack.popLongValue());
    }

    private static void _doOr(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(x | y);
    }

    private static void _doRem(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        long y = stack.popLongValue();
        long x = stack.popLongValue();

        if (y < 0) {
            x = -x;
            y = -y;
        }

        try {
            stack.push(x % y);
        } catch (final ArithmeticException exception) {
            fail(
                ProcessorMessages.DIVISION_ERROR,
                String.valueOf(x),
                String.valueOf(y));
        }
    }

    private static void _doRshft(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(x >> y);
    }

    private static void _doRshftz(
            final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(x >>> y);
    }

    private static void _doSgn(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Long.signum(stack.popLongValue()));
    }

    private static void _doSub(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(x - y);
    }

    private static void _doXor(final Stack stack)
        throws Stack.AccessException
    {
        final long y = stack.popLongValue();
        final long x = stack.popLongValue();

        stack.push(x ^ y);
    }

    private static void _doZeroMinusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(stack.popLongValue() <= 0);
    }

    private static void _doZeroPlusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(stack.popLongValue() >= 0);
    }

    private static void _doZeroQ(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(stack.popLongValue() == 0);
    }

    /** Top must be a number or a string. */
    public static final Filter TOP_NUMBER_OR_STRING = new Filter()
        .is(0, Number.class)
        .is(0, String.class)
        .or();

    /**
     * Code.
     */
    private enum _Code
    {
        ABS,
        ADD,
        AND,
        DEC,
        DIV,
        DIV_REM,
        EQ,
        GE,
        GT,
        INC,
        INT,
        INT_Q,
        LE,
        LSHFT,
        LT,
        MAX,
        MIN,
        MOD,
        MUL,
        NE,
        NEG,
        NOT,
        OR,
        REM,
        RSHFT,
        RSHFTZ,
        SGN,
        SUB,
        XOR,
        ZERO_MINUS_Q,
        ZERO_PLUS_Q,
        ZERO_Q
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
