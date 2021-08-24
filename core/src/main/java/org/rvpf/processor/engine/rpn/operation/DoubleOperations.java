/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DoubleOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import org.rvpf.base.tool.Require;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Double operations.
 */
public final class DoubleOperations
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
            case CBRT: {
                _doCbrt(stack);

                break;
            }
            case CEIL: {
                _doCeil(stack);

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
            case E: {
                _doE(stack);

                break;
            }
            case EQ: {
                _doEq(stack);

                break;
            }
            case EQ_UP_TO_Q: {
                _doEqUpToQ(stack);

                break;
            }
            case FLOAT: {
                _doFloat(stack);

                break;
            }
            case FLOAT_Q: {
                _doFloatQ(stack);

                break;
            }
            case FLOOR: {
                _doFloor(stack);

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
            case HYPOT: {
                _doHypot(stack);

                break;
            }
            case INF_Q: {
                _doInfQ(stack);

                break;
            }
            case LE: {
                _doLe(stack);

                break;
            }
            case LOG: {
                _doLog(stack);

                break;
            }
            case LOG10: {
                _doLog10(stack);

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
            case NAN: {
                _doNaN(stack);

                break;
            }
            case NAN_Q: {
                _doNaNQ(stack);

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
            case NEG_INF: {
                _doNegInf(stack);

                break;
            }
            case POS_INF: {
                _doPosInf(stack);

                break;
            }
            case POW: {
                _doPow(stack);

                break;
            }
            case REM: {
                _doRem(stack);

                break;
            }
            case ROUND: {
                _doRound(stack);

                break;
            }
            case SQRT: {
                _doSqrt(stack);

                break;
            }
            case SUB: {
                _doSub(stack);

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
            case ZERO_UP_TO_Q: {
                _doZeroUpToQ(stack);

                break;
            }
            case ACOS: {
                _doACos(stack);

                break;
            }
            case ASIN: {
                _doASin(stack);

                break;
            }
            case ATAN: {
                _doATan(stack);

                break;
            }
            case COS: {
                _doCos(stack);

                break;
            }
            case COSH: {
                _doCosh(stack);

                break;
            }
            case DEG: {
                _doDeg(stack);

                break;
            }
            case PI: {
                _doPI(stack);

                break;
            }
            case RAD: {
                _doRad(stack);

                break;
            }
            case SGN: {
                _doSgn(stack);

                break;
            }
            case SIN: {
                _doSin(stack);

                break;
            }
            case SINH: {
                _doSinh(stack);

                break;
            }
            case TAN: {
                _doTan(stack);

                break;
            }
            case TANH: {
                _doTanh(stack);

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
        register("0?", _Code.ZERO_Q, Filter.TOP_NUMBER);
        register("0+?", _Code.ZERO_PLUS_Q, Filter.TOP_NUMBER);
        register("0-?", _Code.ZERO_MINUS_Q, Filter.TOP_NUMBER);
        register("0~?", _Code.ZERO_UP_TO_Q, Filter.BOTH_NUMBER);
        register("abs", _Code.ABS, Filter.TOP_NUMBER);
        register("+", _Code.ADD, Filter.BOTH_NUMBER);
        register("cbrt", _Code.CBRT, Filter.TOP_NUMBER);
        register("ceil", _Code.CEIL, Filter.TOP_NUMBER);
        register("/", _Code.DIV, Filter.BOTH_NUMBER);
        register("/%", _Code.DIV_REM, Filter.BOTH_NUMBER);
        register("e", _Code.E);
        register("eq", _Code.EQ, Filter.BOTH_NUMBER);
        register("eq~", _Code.EQ_UP_TO_Q, _EQ_UP_TO_FILTER);
        register("float", _Code.FLOAT, TOP_NUMBER_OR_STRING);
        register("float?", _Code.FLOAT_Q, Filter.TOP_PRESENT);
        register("floor", _Code.FLOOR, Filter.TOP_NUMBER);
        register("ge", _Code.GE, Filter.BOTH_NUMBER);
        register("gt", _Code.GT, Filter.BOTH_NUMBER);
        register("hypot", _Code.HYPOT, Filter.BOTH_NUMBER);
        register("+inf", _Code.POS_INF);
        register("-inf", _Code.NEG_INF);
        register("inf?", _Code.INF_Q, TOP_DOUBLE);
        register("le", _Code.LE, Filter.BOTH_NUMBER);
        register("log", _Code.LOG, Filter.TOP_NUMBER);
        register("log10", _Code.LOG10, Filter.TOP_NUMBER);
        register("lt", _Code.LT, Filter.BOTH_NUMBER);
        register("max", _Code.MAX, Filter.BOTH_NUMBER);
        register("min", _Code.MIN, Filter.BOTH_NUMBER);
        register("mod", _Code.MOD, TOP_DOUBLE);
        register("*", _Code.MUL, Filter.BOTH_NUMBER);
        register("nan", _Code.NAN);
        register("nan?", _Code.NAN_Q, TOP_DOUBLE);
        register("ne", _Code.NE, Filter.BOTH_NUMBER);
        register("neg", _Code.NEG, Filter.TOP_NUMBER);
        register("**", _Code.POW, Filter.BOTH_NUMBER);
        register("%", _Code.REM, Filter.BOTH_NUMBER);
        register("round", _Code.ROUND, Filter.TOP_NUMBER);
        register("sqrt", _Code.SQRT, Filter.TOP_NUMBER);
        register("-", _Code.SUB, Filter.BOTH_NUMBER);
        register("acos", _Code.ACOS, Filter.TOP_NUMBER);
        register("asin", _Code.ASIN, Filter.TOP_NUMBER);
        register("atan", _Code.ATAN, Filter.TOP_NUMBER);
        register("cos", _Code.COS, Filter.TOP_NUMBER);
        register("cosh", _Code.COSH, Filter.TOP_NUMBER);
        register("deg", _Code.DEG, Filter.TOP_NUMBER);
        register("pi", _Code.PI);
        register("rad", _Code.RAD, Filter.TOP_NUMBER);
        register("sgn", _Code.SGN, Filter.TOP_NUMBER);
        register("sin", _Code.SIN, Filter.TOP_NUMBER);
        register("sinh", _Code.SINH, Filter.TOP_NUMBER);
        register("tan", _Code.TAN, Filter.TOP_NUMBER);
        register("tanh", _Code.TANH, Filter.TOP_NUMBER);
    }

    private static void _doACos(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.acos(stack.popDoubleValue()));
    }

    private static void _doASin(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.asin(stack.popDoubleValue()));
    }

    private static void _doATan(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.atan(stack.popDoubleValue()));
    }

    private static void _doAbs(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.abs(stack.popDoubleValue()));
    }

    private static void _doAdd(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(x + y);
    }

    private static void _doCbrt(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.cbrt(stack.popDoubleValue()));
    }

    private static void _doCeil(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.ceil(stack.popDoubleValue()));
    }

    private static void _doCos(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.cos(stack.popDoubleValue()));
    }

    private static void _doCosh(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.cosh(stack.popDoubleValue()));
    }

    private static void _doDeg(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.toDegrees(stack.popDoubleValue()));
    }

    private static void _doDiv(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(x / y);
    }

    private static void _doDivRem(
            final Stack stack)
        throws Stack.AccessException
    {
        double y = stack.popDoubleValue();
        double x = stack.popDoubleValue();

        if (y < 0.0) {
            x = -x;
            y = -y;
        }

        stack.push(x % y);
        stack.push(x / y);
    }

    private static void _doE(final Stack stack)
    {
        stack.push(Math.E);
    }

    private static void _doEq(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(y == x);
    }

    private static void _doEqUpToQ(
            final Stack stack)
        throws Stack.AccessException
    {
        final double delta = Math.abs(stack.popDoubleValue());
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(Math.abs(x - y) <= delta);
    }

    private static void _doFloat(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        if (stack.peek() instanceof String) {
            final String x = stack.popStringValue();

            try {
                stack.push(Double.parseDouble(x));
            } catch (final NumberFormatException exception) {
                fail(ProcessorMessages.CONVERT_FAILED, x, "float");
            }
        } else {
            stack.push(stack.popDoubleValue());
        }
    }

    private static void _doFloatQ(
            final Stack stack)
        throws Stack.LimitsException
    {
        final Object x = stack.pop();
        boolean isFloat;

        if (x instanceof String) {
            try {
                Double.parseDouble((String) x);
                isFloat = true;
            } catch (final NumberFormatException exception) {
                isFloat = false;
            }
        } else {
            isFloat = (x instanceof Double) || (x instanceof Float);
        }

        stack.push(isFloat);
    }

    private static void _doFloor(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.floor(stack.popDoubleValue()));
    }

    private static void _doGe(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(y >= x);
    }

    private static void _doGt(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(y > x);
    }

    private static void _doHypot(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(Math.hypot(x, y));
    }

    private static void _doInfQ(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Double.isInfinite(stack.popDoubleValue()));
    }

    private static void _doLe(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(y <= x);
    }

    private static void _doLog(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.log(stack.popDoubleValue()));
    }

    private static void _doLog10(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.log10(stack.popDoubleValue()));
    }

    private static void _doLt(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(y < x);
    }

    private static void _doMax(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(Math.max(x, y));
    }

    private static void _doMin(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(Math.min(x, y));
    }

    private static void _doMod(final Stack stack)
        throws Stack.AccessException
    {
        double y = stack.popDoubleValue();
        double x = stack.popDoubleValue();

        if (y < 0.0) {
            x = -x;
            y = -y;
        }

        final double result = x % y;

        stack.push((result >= 0.0)? result: (result + y));
    }

    private static void _doMul(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(x * y);
    }

    private static void _doNaN(final Stack stack)
    {
        stack.push(Double.NaN);
    }

    private static void _doNaNQ(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Double.isNaN(stack.popDoubleValue()));
    }

    private static void _doNe(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(y != x);
    }

    private static void _doNeg(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(-stack.popDoubleValue());
    }

    private static void _doNegInf(final Stack stack)
    {
        stack.push(Double.NEGATIVE_INFINITY);
    }

    private static void _doPI(final Stack stack)
    {
        stack.push(Math.PI);
    }

    private static void _doPosInf(final Stack stack)
    {
        stack.push(Double.POSITIVE_INFINITY);
    }

    private static void _doPow(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(Math.pow(x, y));
    }

    private static void _doRad(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.toRadians(stack.popDoubleValue()));
    }

    private static void _doRem(final Stack stack)
        throws Stack.AccessException
    {
        double y = stack.popDoubleValue();
        double x = stack.popDoubleValue();

        if (y < 0.0) {
            x = -x;
            y = -y;
        }

        stack.push(x % y);
    }

    private static void _doRound(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.round(stack.popDoubleValue()));
    }

    private static void _doSgn(final Stack stack)
        throws Stack.AccessException
    {
        stack.push((int) Math.signum(stack.popDoubleValue()));
    }

    private static void _doSin(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.sin(stack.popDoubleValue()));
    }

    private static void _doSinh(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.sinh(stack.popDoubleValue()));
    }

    private static void _doSqrt(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.sqrt(stack.popDoubleValue()));
    }

    private static void _doSub(final Stack stack)
        throws Stack.AccessException
    {
        final double y = stack.popDoubleValue();
        final double x = stack.popDoubleValue();

        stack.push(x - y);
    }

    private static void _doTan(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.tan(stack.popDoubleValue()));
    }

    private static void _doTanh(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Math.tanh(stack.popDoubleValue()));
    }

    private static void _doZeroMinusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Boolean.valueOf(stack.popDoubleValue() <= 0.0));
    }

    private static void _doZeroPlusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Boolean.valueOf(stack.popDoubleValue() >= 0.0));
    }

    private static void _doZeroQ(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(Boolean.valueOf(stack.popDoubleValue() == 0.0));
    }

    private static void _doZeroUpToQ(
            final Stack stack)
        throws Stack.AccessException
    {
        final double delta = Math.abs(stack.popDoubleValue());
        final double x = Math.abs(stack.popDoubleValue());

        stack.push(x <= delta);
    }

    /** Top must be a doubles. */
    public static final Filter TOP_DOUBLE = new Filter()
        .is(0, Double.class)
        .is(0, Float.class)
        .or();

    /** Top must be a number or a string. */
    public static final Filter TOP_NUMBER_OR_STRING = new Filter()
        .is(0, Number.class)
        .is(0, String.class)
        .or();
    private static final Filter _EQ_UP_TO_FILTER = new Filter()
        .is(0, Number.class)
        .is(1, Number.class)
        .and()
        .is(2, Number.class)
        .and();

    /**
     * Code.
     */
    private enum _Code
    {
        ABS,
        ACOS,
        ADD,
        ASIN,
        ATAN,
        CBRT,
        CEIL,
        COS,
        COSH,
        DEG,
        DIV,
        DIV_REM,
        E,
        EQ,
        EQ_UP_TO_Q,
        FLOAT,
        FLOAT_Q,
        FLOOR,
        GE,
        GT,
        HYPOT,
        INF_Q,
        LE,
        LOG,
        LOG10,
        LT,
        MAX,
        MIN,
        MOD,
        MUL,
        NAN,
        NAN_Q,
        NE,
        NEG,
        NEG_INF,
        PI,
        POS_INF,
        POW,
        RAD,
        REM,
        ROUND,
        SGN,
        SIN,
        SINH,
        SQRT,
        SUB,
        TAN,
        TANH,
        ZERO_MINUS_Q,
        ZERO_PLUS_Q,
        ZERO_Q,
        ZERO_UP_TO_Q
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
