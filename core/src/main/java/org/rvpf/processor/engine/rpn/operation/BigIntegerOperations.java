/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BigIntegerOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.rvpf.base.tool.Require;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Big integer operations.
 */
public class BigIntegerOperations
    extends SimpleOperations
{
    /**
     * Pops the big integer at the top of the stack.
     *
     * @param stack The stack.
     *
     * @return The big integer at the top.
     *
     * @throws Stack.AccessException When appropriate.
     */
    public static BigInteger popBigIntegerValue(
            final Stack stack)
        throws Stack.AccessException
    {
        final Serializable object = stack.pop();

        if (object instanceof BigInteger) {
            return (BigInteger) object;
        }

        if (object instanceof Number) {
            return BigInteger.valueOf(((Number) object).longValue());
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
            case AND: {
                _doAnd(stack);

                break;
            }
            case BIG_INT: {
                _doBigInt(stack);

                break;
            }
            case BITS: {
                _doBits(stack);

                break;
            }
            case CLEAR: {
                _doClear(stack);

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
            case FLIP: {
                _doFlip(stack);

                break;
            }
            case GCD: {
                _doGcd(stack);

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
            case LE: {
                _doLe(stack);

                break;
            }
            case LOW_1: {
                _doLow1(stack);

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
            case SET: {
                _doSet(stack);

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
            case TEST: {
                _doTest(stack);

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
        register("0?", _Code.ZERO_Q, TOP_BIG_INTEGER);
        register("0+?", _Code.ZERO_PLUS_Q, TOP_BIG_INTEGER);
        register("0-?", _Code.ZERO_MINUS_Q, TOP_BIG_INTEGER);
        register("abs", _Code.ABS, TOP_BIG_INTEGER);
        register("+", _Code.ADD, EITHER_BIG_INTEGER);
        register("and", _Code.AND, EITHER_BIG_INTEGER);
        register("bigint", _Code.BIG_INT, Filter.TOP_PRESENT);
        register("bits", _Code.BITS, TOP_BIG_INTEGER);
        register("clear", _Code.CLEAR, TOP_LONG_BIG_INTEGER);
        register("/", _Code.DIV, EITHER_BIG_INTEGER);
        register("/%", _Code.DIV_REM, EITHER_BIG_INTEGER);
        register("--", _Code.DEC, TOP_BIG_INTEGER);
        register("eq", _Code.EQ, EITHER_BIG_INTEGER);
        register("flip", _Code.FLIP, TOP_LONG_BIG_INTEGER);
        register("gcd", _Code.GCD, EITHER_BIG_INTEGER);
        register("ge", _Code.GE, EITHER_BIG_INTEGER);
        register("gt", _Code.GT, EITHER_BIG_INTEGER);
        register("++", _Code.INC, TOP_BIG_INTEGER);
        register("le", _Code.LE, EITHER_BIG_INTEGER);
        register("low1", _Code.LOW_1, TOP_BIG_INTEGER);
        register("lshft", _Code.LSHFT, TOP_LONG_BIG_INTEGER);
        register("lt", _Code.LT, EITHER_BIG_INTEGER);
        register("max", _Code.MAX, EITHER_BIG_INTEGER);
        register("min", _Code.MIN, EITHER_BIG_INTEGER);
        register("mod", _Code.MOD, EITHER_BIG_INTEGER);
        register("*", _Code.MUL, EITHER_BIG_INTEGER);
        register("ne", _Code.NE, EITHER_BIG_INTEGER);
        register("neg", _Code.NEG, TOP_BIG_INTEGER);
        register("not", _Code.NOT, TOP_BIG_INTEGER);
        register("or", _Code.OR, EITHER_BIG_INTEGER);
        register("%", _Code.REM, EITHER_BIG_INTEGER);
        register("rshft", _Code.RSHFT, TOP_LONG_BIG_INTEGER);
        register("set", _Code.SET, TOP_LONG_BIG_INTEGER);
        register("sgn", _Code.SGN, TOP_BIG_INTEGER);
        register("-", _Code.SUB, EITHER_BIG_INTEGER);
        register("test", _Code.TEST, TOP_LONG_BIG_INTEGER);
        register("xor", _Code.XOR, EITHER_BIG_INTEGER);
    }

    private static void _doAbs(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).abs());
    }

    private static void _doAdd(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.add(y));
    }

    private static void _doAnd(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.and(y));
    }

    private static void _doBigInt(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Serializable top = stack.peek();

        if (top instanceof String) {
            final String x = stack.popStringValue();

            try {
                stack.push(new BigInteger(x));
            } catch (final NumberFormatException exception) {
                fail(ProcessorMessages.CONVERT_FAILED, x, "bigint");
            }
        } else if (top instanceof BigDecimal) {
            stack.push(((BigDecimal) top).toBigInteger());
        } else {
            stack.push(popBigIntegerValue(stack));
        }
    }

    private static void _doBits(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).bitCount());
    }

    private static void _doClear(final Stack stack)
        throws Stack.AccessException
    {
        final int y = stack.popIntValue();
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.clearBit(y));
    }

    private static void _doDec(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).subtract(BigInteger.ONE));
    }

    private static void _doDiv(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        try {
            stack.push(x.divide(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.DIVISION_ERROR, x, y);
        }
    }

    private static void _doDivRem(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        BigInteger y = popBigIntegerValue(stack);
        BigInteger x = popBigIntegerValue(stack);

        if (y.signum() < 0) {
            x = x.negate();
            y = y.negate();
        }

        try {
            final BigInteger[] result = x.divideAndRemainder(y);

            stack.push(result[1]);
            stack.push(result[0]);
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.DIVISION_ERROR, x, y);
        }
    }

    private static void _doEq(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(y.equals(x));
    }

    private static void _doFlip(final Stack stack)
        throws Stack.AccessException
    {
        final int y = stack.popIntValue();
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.flipBit(y));
    }

    private static void _doGcd(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.gcd(y));
    }

    private static void _doGe(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(y.compareTo(x) >= 0);
    }

    private static void _doGt(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(y.compareTo(x) > 0);
    }

    private static void _doInc(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).add(BigInteger.ONE));
    }

    private static void _doLe(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(y.compareTo(x) <= 0);
    }

    private static void _doLow1(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).getLowestSetBit());
    }

    private static void _doLshft(final Stack stack)
        throws Stack.AccessException
    {
        final int y = stack.popIntValue();
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.shiftLeft(y));
    }

    private static void _doLt(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(y.compareTo(x) < 0);
    }

    private static void _doMax(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.max(y));
    }

    private static void _doMin(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.min(y));
    }

    private static void _doMod(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        BigInteger y = popBigIntegerValue(stack);
        BigInteger x = popBigIntegerValue(stack);

        if (y.signum() < 0) {
            x = x.negate();
            y = y.negate();
        }

        try {
            stack.push(x.mod(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.DIVISION_ERROR, x, y);
        }
    }

    private static void _doMul(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.multiply(y));
    }

    private static void _doNe(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(!y.equals(x));
    }

    private static void _doNeg(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).negate());
    }

    private static void _doNot(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).not());
    }

    private static void _doOr(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.or(y));
    }

    private static void _doRem(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        BigInteger y = popBigIntegerValue(stack);
        BigInteger x = popBigIntegerValue(stack);

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

    private static void _doRshft(final Stack stack)
        throws Stack.AccessException
    {
        final int y = stack.popIntValue();
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.shiftRight(y));
    }

    private static void _doSet(final Stack stack)
        throws Stack.AccessException
    {
        final int y = stack.popIntValue();
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.setBit(y));
    }

    private static void _doSgn(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).signum());
    }

    private static void _doSub(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.subtract(y));
    }

    private static void _doTest(final Stack stack)
        throws Stack.AccessException
    {
        final int y = stack.popIntValue();
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.testBit(y));
    }

    private static void _doXor(final Stack stack)
        throws Stack.AccessException
    {
        final BigInteger y = popBigIntegerValue(stack);
        final BigInteger x = popBigIntegerValue(stack);

        stack.push(x.xor(y));
    }

    private static void _doZeroMinusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).signum() <= 0);
    }

    private static void _doZeroPlusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).signum() >= 0);
    }

    private static void _doZeroQ(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigIntegerValue(stack).signum() == 0);
    }

    /** Both must be big integers. */
    public static final Filter BOTH_BIG_INTEGER = new Filter()
        .is(0, BigInteger.class)
        .is(1, BigInteger.class)
        .and();

    /** Either must be big integers. */
    public static final Filter EITHER_BIG_INTEGER = new Filter()
        .is(0, BigInteger.class)
        .is(1, BigInteger.class)
        .isLong(1)
        .or()
        .and()
        .isLong(0)
        .is(1, BigInteger.class)
        .and()
        .or();

    /** Top must be a big integer. */
    public static final Filter TOP_BIG_INTEGER = new Filter()
        .is(0, BigInteger.class);

    /** Both must be big integers. */
    public static final Filter TOP_LONG_BIG_INTEGER = new Filter()
        .isLong(0)
        .is(1, BigInteger.class)
        .and();

    /**
     * Code.
     */
    private enum _Code
    {
        ABS,
        ADD,
        AND,
        BIG_INT,
        BITS,
        CLEAR,
        DEC,
        DIV,
        DIV_REM,
        EQ,
        FLIP,
        GCD,
        GE,
        GT,
        INC,
        LE,
        LOW_1,
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
        SET,
        SGN,
        SUB,
        TEST,
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
