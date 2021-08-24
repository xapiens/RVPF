/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BigRationalOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import static org.rvpf.processor.engine.rpn.operation.BigIntegerOperations
    .popBigIntegerValue;
import static org.rvpf.processor.engine.rpn.operation.RationalOperations
    .popRationalValue;

import java.io.Serializable;

import java.math.BigInteger;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.BigRational;
import org.rvpf.base.value.Rational;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Stack.AccessException;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Big rational operations.
 */
public class BigRationalOperations
    extends SimpleOperations
{
    /**
     * Pops the big rational at the top of the stack.
     *
     * @param stack The stack.
     *
     * @return The big rational at the top.
     *
     * @throws AccessException When appropriate.
     */
    public static BigRational popBigRationalValue(
            final Stack stack)
        throws AccessException
    {
        final Serializable object = stack.pop();

        if (object instanceof BigRational) {
            return (BigRational) object;
        }

        if (object instanceof Rational) {
            return BigRational.valueOf((Rational) object);
        }

        if (object instanceof Number) {
            return BigRational.valueOf(((Number) object).longValue(), 1);
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
            case BIG_RAT: {
                _doBigRat(stack);

                break;
            }
            case BIG_RAT_RAT: {
                _doBigRatRat(stack);

                break;
            }
            case DIV: {
                _doDiv(stack);

                break;
            }
            case MUL: {
                _doMul(stack);

                break;
            }
            case NEG: {
                _doNeg(stack);

                break;
            }
            case RAT: {
                _doRat(stack);

                break;
            }
            case RECIP: {
                _doRecip(stack);

                break;
            }
            case SGN: {
                _doSgn(stack);

                break;
            }
            case SPLIT: {
                _doSplit(stack);

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
        register("0?", _Code.ZERO_Q, TOP_BIG_RATIONAL);
        register("0+?", _Code.ZERO_PLUS_Q, TOP_BIG_RATIONAL);
        register("0-?", _Code.ZERO_MINUS_Q, TOP_BIG_RATIONAL);
        register("abs", _Code.ABS, TOP_BIG_RATIONAL);
        register("neg", _Code.NEG, TOP_BIG_RATIONAL);
        register("+", _Code.ADD, EITHER_BIG_RATIONAL);
        register("-", _Code.SUB, EITHER_BIG_RATIONAL);
        register("/", _Code.DIV, EITHER_BIG_RATIONAL);
        register("*", _Code.MUL, EITHER_BIG_RATIONAL);
        register("1/", _Code.RECIP, TOP_BIG_RATIONAL);
        register("sgn", _Code.SGN, TOP_BIG_RATIONAL);
        register("split", _Code.SPLIT, TOP_BIG_RATIONAL);
        register("bigrat", _Code.BIG_RAT, Filter.TOP_STRING);
        register("bigrat", _Code.BIG_RAT, Filter.BOTH_NUMBER);
        register("bigrat", _Code.BIG_RAT_RAT, RationalOperations.TOP_RATIONAL);
        register("rat", _Code.RAT, TOP_BIG_RATIONAL);
    }

    private static void _doAbs(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigRationalValue(stack).abs());
    }

    private static void _doAdd(final Stack stack)
        throws Stack.AccessException
    {
        final BigRational y = popBigRationalValue(stack);
        final BigRational x = popBigRationalValue(stack);

        stack.push(x.add(y));
    }

    private static void _doBigRat(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        if (stack.peek() instanceof String) {
            final String x = stack.popStringValue();

            try {
                stack.push(BigRational.valueOf(x));
            } catch (final NumberFormatException exception) {
                fail(ProcessorMessages.CONVERT_FAILED, x, "bigrat");
            }
        } else {
            final BigInteger y = popBigIntegerValue(stack);
            final BigInteger x = popBigIntegerValue(stack);

            try {
                stack.push(BigRational.valueOf(x, y));
            } catch (final ArithmeticException exception) {
                fail(ProcessorMessages.CONVERT_FAILED, x + "/" + y, "bigrat");
            }
        }
    }

    private static void _doBigRatRat(
            final Stack stack)
        throws Stack.AccessException
    {
        final Rational rational = popRationalValue(stack);

        stack
            .push(
                BigRational
                    .valueOf(
                            rational.getNumerator(),
                                    rational.getDenominator()));
    }

    private static void _doDiv(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final BigRational y = popBigRationalValue(stack);
        final BigRational x = popBigRationalValue(stack);

        try {
            stack.push(x.divide(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.DIVISION_ERROR, x, y);
        }
    }

    private static void _doMul(final Stack stack)
        throws Stack.AccessException
    {
        final BigRational y = popBigRationalValue(stack);
        final BigRational x = popBigRationalValue(stack);

        stack.push(x.multiply(y));
    }

    private static void _doNeg(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigRationalValue(stack).negate());
    }

    private static void _doRat(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final BigRational rational = popBigRationalValue(stack);

        try {
            stack
                .push(
                    Rational
                        .valueOf(
                                rational.getNumerator().longValue(),
                                        rational.getDenominator().longValue()));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.CONVERT_FAILED, "BigRational", "Rational");
        }
    }

    private static void _doRecip(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigRationalValue(stack).reciprocal());
    }

    private static void _doSgn(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigRationalValue(stack).signum());
    }

    private static void _doSplit(final Stack stack)
        throws Stack.AccessException
    {
        final BigRational bigRational = popBigRationalValue(stack);

        stack.push(bigRational.getNumerator());
        stack.push(bigRational.getDenominator());
    }

    private static void _doSub(final Stack stack)
        throws Stack.AccessException
    {
        final BigRational y = popBigRationalValue(stack);
        final BigRational x = popBigRationalValue(stack);

        stack.push(x.subtract(y));
    }

    private static void _doZeroMinusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigRationalValue(stack).signum() <= 0);
    }

    private static void _doZeroPlusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigRationalValue(stack).signum() >= 0);
    }

    private static void _doZeroQ(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popBigRationalValue(stack).getNumerator().signum() == 0);
    }

    /** Either must be big rationals. */
    public static final Filter EITHER_BIG_RATIONAL = new Filter()
        .is(0, BigRational.class)
        .is(1, BigRational.class)
        .is(1, Rational.class)
        .or()
        .isLong(1)
        .or()
        .and()
        .isLong(0)
        .is(0, Rational.class)
        .or()
        .is(1, BigRational.class)
        .and()
        .or();

    /** Top must be a big rational. */
    public static final Filter TOP_BIG_RATIONAL = new Filter()
        .is(0, BigRational.class);

    /**
     * Code.
     */
    private enum _Code
    {
        ABS,
        ADD,
        BIG_RAT,
        BIG_RAT_RAT,
        DIV,
        MUL,
        NEG,
        RAT,
        RECIP,
        SGN,
        SPLIT,
        SUB,
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
