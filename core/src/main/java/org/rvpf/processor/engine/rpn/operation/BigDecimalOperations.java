/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BigDecimalOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.rvpf.base.tool.Require;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Big decimal operations.
 */
public class BigDecimalOperations
    extends SimpleOperations
{
    /**
     * Pops the big decimal at the top of the stack.
     *
     * @param stack The stack.
     *
     * @return The big decimal at the top.
     *
     * @throws Stack.AccessException When appropriate.
     */
    public static BigDecimal popBigDecimalValue(
            final Stack stack)
        throws Stack.AccessException
    {
        final Serializable object = stack.pop();

        if (object instanceof BigDecimal) {
            return (BigDecimal) object;
        }

        if (object instanceof BigInteger) {
            return new BigDecimal((BigInteger) object);
        }

        if (object instanceof Number) {
            return BigDecimal.valueOf(((Number) object).doubleValue());
        }

        throw new Stack.CastException(Number.class, object);
    }

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
            case BIG_DEC: {
                _doBigDec(stack);

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
            case LE: {
                _doLe(stack);

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
            case POINT_LEFT: {
                _doPointLeft(stack);

                break;
            }
            case POINT_RIGHT: {
                _doPointRight(stack);

                break;
            }
            case POW: {
                _doPow(stack);

                break;
            }
            case PREC: {
                _doPrec(stack);

                break;
            }
            case REM: {
                _doRem(stack);

                break;
            }
            case SCALE: {
                _doScale(stack);

                break;
            }
            case SCALE_SET: {
                _doScaleSet(stack);

                break;
            }
            case SGN: {
                _doSgn(stack);

                break;
            }
            case STRIP: {
                _doStrip(stack);

                break;
            }
            case SUB: {
                _doSub(stack);

                break;
            }
            case UNSCALED: {
                _doUnscaled(stack);

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
        register("0?", _Code.ZERO_Q, TOP_BIG_DECIMAL);
        register("0+?", _Code.ZERO_PLUS_Q, TOP_BIG_DECIMAL);
        register("0-?", _Code.ZERO_MINUS_Q, TOP_BIG_DECIMAL);
        register("abs", _Code.ABS, TOP_BIG_DECIMAL);
        register("+", _Code.ADD, EITHER_BIG_DECIMAL);
        register("bigdec", _Code.BIG_DEC, Filter.TOP_PRESENT);
        register("/", _Code.DIV, EITHER_BIG_DECIMAL);
        register("/%", _Code.DIV_REM, EITHER_BIG_DECIMAL);
        register("eq", _Code.EQ, EITHER_BIG_DECIMAL);
        register("ge", _Code.GE, EITHER_BIG_DECIMAL);
        register("gt", _Code.GT, EITHER_BIG_DECIMAL);
        register("le", _Code.LE, EITHER_BIG_DECIMAL);
        register("lt", _Code.LT, EITHER_BIG_DECIMAL);
        register("max", _Code.MAX, EITHER_BIG_DECIMAL);
        register("min", _Code.MIN, EITHER_BIG_DECIMAL);
        register("mod", _Code.MOD, EITHER_BIG_DECIMAL);
        register("*", _Code.MUL, EITHER_BIG_DECIMAL);
        register("ne", _Code.NE, EITHER_BIG_DECIMAL);
        register("neg", _Code.NEG, TOP_BIG_DECIMAL);
        register(".left", _Code.POINT_LEFT, TOP_LONG_BIG_DECIMAL);
        register(".right", _Code.POINT_RIGHT, TOP_LONG_BIG_DECIMAL);
        register("**", _Code.POW, TOP_LONG_BIG_DECIMAL);
        register("prec", _Code.PREC, TOP_BIG_DECIMAL);
        register("%", _Code.REM, EITHER_BIG_DECIMAL);
        register("scale", _Code.SCALE, TOP_BIG_DECIMAL);
        register("scale=", _Code.SCALE_SET, TOP_LONG_BIG_DECIMAL);
        register("sgn", _Code.SGN, TOP_BIG_DECIMAL);
        register("strip", _Code.STRIP, TOP_BIG_DECIMAL);
        register("-", _Code.SUB, EITHER_BIG_DECIMAL);
        register("unscaled", _Code.UNSCALED, TOP_BIG_DECIMAL);
    }

    private static void _doAbs(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigDecimalValue(stack).abs());
    }

    private static void _doAdd(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(x.add(y));
    }

    private static void _doBigDec(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        if (stack.peek() instanceof String) {
            final String x = stack.popStringValue();

            try {
                stack.push(new BigDecimal(x));
            } catch (final NumberFormatException exception) {
                fail(ProcessorMessages.CONVERT_FAILED, x, "bigdec");
            }
        } else {
            stack.push(popBigDecimalValue(stack));
        }
    }

    private static void _doDiv(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        try {
            stack.push(x.divide(y, RoundingMode.HALF_EVEN));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.DIVISION_ERROR, x, y);
        }
    }

    private static void _doDivRem(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        BigDecimal y = popBigDecimalValue(stack);
        BigDecimal x = popBigDecimalValue(stack);

        if (y.signum() < 0) {
            x = x.negate();
            y = y.negate();
        }

        try {
            final BigDecimal[] result = x.divideAndRemainder(y);

            stack.push(result[1]);
            stack.push(result[0]);
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.DIVISION_ERROR, x, y);
        }
    }

    private static void _doEq(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(y.compareTo(x) == 0);
    }

    private static void _doGe(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(y.compareTo(x) >= 0);
    }

    private static void _doGt(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(y.compareTo(x) > 0);
    }

    private static void _doLe(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(y.compareTo(x) <= 0);
    }

    private static void _doLt(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(y.compareTo(x) < 0);
    }

    private static void _doMax(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(x.max(y));
    }

    private static void _doMin(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(x.min(y));
    }

    private static void _doMod(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        BigDecimal y = popBigDecimalValue(stack);
        BigDecimal x = popBigDecimalValue(stack);

        if (y.signum() < 0) {
            x = x.negate();
            y = y.negate();
        }

        try {
            final BigDecimal result = x.remainder(y);

            stack.push((result.signum() >= 0)? result: result.add(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.DIVISION_ERROR, x, y);
        }
    }

    private static void _doMul(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(x.multiply(y));
    }

    private static void _doNe(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(y.compareTo(x) != 0);
    }

    private static void _doNeg(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigDecimalValue(stack).negate());
    }

    private static void _doPointLeft(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final int y = stack.popIntValue();
        final BigDecimal x = popBigDecimalValue(stack);

        try {
            stack.push(x.movePointLeft(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.SCALE_OVERFLOW, x, String.valueOf(y));
        }
    }

    private static void _doPointRight(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final int y = stack.popIntValue();
        final BigDecimal x = popBigDecimalValue(stack);

        try {
            stack.push(x.movePointRight(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.SCALE_OVERFLOW, x, String.valueOf(-y));
        }
    }

    private static void _doPow(final Stack stack)
        throws Stack.AccessException
    {
        final int y = Math.min(stack.popIntValue(), 999999999);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(x.pow(y));
    }

    private static void _doPrec(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigDecimalValue(stack).precision());
    }

    private static void _doRem(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        BigDecimal y = popBigDecimalValue(stack);
        BigDecimal x = popBigDecimalValue(stack);

        if (y.signum() < 0) {
            x = x.negate();
            y = y.negate();
        }

        try {
            stack.push(x.remainder(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.DIVISION_ERROR, x, y);
        }
    }

    private static void _doScale(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigDecimalValue(stack).scale());
    }

    private static void _doScaleSet(
            final Stack stack)
        throws Stack.AccessException
    {
        final int y = stack.popIntValue();
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(x.setScale(y, RoundingMode.HALF_EVEN));
    }

    private static void _doSgn(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigDecimalValue(stack).signum());
    }

    private static void _doStrip(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigDecimalValue(stack).stripTrailingZeros());
    }

    private static void _doSub(final Stack stack)
        throws Stack.AccessException
    {
        final BigDecimal y = popBigDecimalValue(stack);
        final BigDecimal x = popBigDecimalValue(stack);

        stack.push(x.subtract(y));
    }

    private static void _doUnscaled(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigDecimalValue(stack).unscaledValue());
    }

    private static void _doZeroMinusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigDecimalValue(stack).signum() <= 0);
    }

    private static void _doZeroPlusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigDecimalValue(stack).signum() >= 0);
    }

    private static void _doZeroQ(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigDecimalValue(stack).signum() == 0);
    }

    /** Either must be big decimals. */
    public static final Filter EITHER_BIG_DECIMAL = new Filter()
        .is(0, BigDecimal.class)
        .is(1, BigDecimal.class)
        .is(1, Number.class)
        .or()
        .and()
        .is(0, Number.class)
        .is(1, BigDecimal.class)
        .or();

    /** Top must be a big decimal. */
    public static final Filter TOP_BIG_DECIMAL = new Filter()
        .is(0, BigDecimal.class);

    /** Top must be an integer over a big decimal. */
    public static final Filter TOP_LONG_BIG_DECIMAL = new Filter()
        .isLong(0)
        .is(1, BigDecimal.class)
        .and();

    /**
     * Code.
     */
    private enum _Code
    {
        ABS,
        ADD,
        BIG_DEC,
        DIV,
        DIV_REM,
        EQ,
        GE,
        GT,
        LE,
        LT,
        MAX,
        MIN,
        MOD,
        MUL,
        NE,
        NEG,
        POINT_LEFT,
        POINT_RIGHT,
        POW,
        PREC,
        REM,
        SCALE,
        SCALE_SET,
        SGN,
        STRIP,
        SUB,
        UNSCALED,
        ZERO_MINUS_Q,
        ZERO_PLUS_Q,
        ZERO_Q,
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
