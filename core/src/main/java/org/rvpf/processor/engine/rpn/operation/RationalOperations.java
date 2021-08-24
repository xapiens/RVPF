/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RationalOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.Rational;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Stack.AccessException;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Rational operations.
 */
public class RationalOperations
    extends SimpleOperations
{
    /**
     * Pops the rational at the top of the stack.
     *
     * @param stack The stack.
     *
     * @return The eational at the top.
     *
     * @throws AccessException When appropriate.
     */
    public static Rational popRationalValue(
            final Stack stack)
        throws AccessException
    {
        final Serializable object = stack.pop();

        if (object instanceof Rational) {
            return (Rational) object;
        }

        if (object instanceof Number) {
            return Rational.valueOf(((Number) object).longValue(), 1);
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
            case RECIP: {
                _doRecip(stack);

                break;
            }
            case RAT: {
                _doRat(stack);

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
        register("0?", _Code.ZERO_Q, TOP_RATIONAL);
        register("0+?", _Code.ZERO_PLUS_Q, TOP_RATIONAL);
        register("0-?", _Code.ZERO_MINUS_Q, TOP_RATIONAL);
        register("abs", _Code.ABS, TOP_RATIONAL);
        register("neg", _Code.NEG, TOP_RATIONAL);
        register("+", _Code.ADD, EITHER_RATIONAL);
        register("-", _Code.SUB, EITHER_RATIONAL);
        register("/", _Code.DIV, EITHER_RATIONAL);
        register("*", _Code.MUL, EITHER_RATIONAL);
        register("1/", _Code.RECIP, TOP_RATIONAL);
        register("rat", _Code.RAT, Filter.TOP_STRING);
        register("rat", _Code.RAT, Filter.BOTH_LONG);
        register("sgn", _Code.SGN, TOP_RATIONAL);
        register("split", _Code.SPLIT, TOP_RATIONAL);
    }

    private static void _doAbs(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popRationalValue(stack).abs());
    }

    private static void _doAdd(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Rational y = popRationalValue(stack);
        final Rational x = popRationalValue(stack);

        try {
            stack.push(x.add(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.ADDITION_ERROR, x, y);
        }
    }

    private static void _doDiv(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Rational y = popRationalValue(stack);
        final Rational x = popRationalValue(stack);

        try {
            stack.push(x.divide(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.DIVISION_ERROR, x, y);
        }
    }

    private static void _doMul(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Rational y = popRationalValue(stack);
        final Rational x = popRationalValue(stack);

        try {
            stack.push(x.multiply(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.MULTIPLICATION_ERROR, x, y);
        }
    }

    private static void _doNeg(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popRationalValue(stack).negate());
    }

    private static void _doRat(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        if (stack.peek() instanceof String) {
            final String string = stack.popStringValue();

            try {
                stack.push(Rational.valueOf(string));
            } catch (final NumberFormatException exception) {
                fail(ProcessorMessages.CONVERT_FAILED, string, "rat");
            }
        } else {
            final long y = stack.popLongValue();
            final long x = stack.popLongValue();

            stack.push(Rational.valueOf(x, y));
        }
    }

    private static void _doRecip(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popRationalValue(stack).reciprocal());
    }

    private static void _doSgn(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popRationalValue(stack).signum());
    }

    private static void _doSplit(final Stack stack)
        throws Stack.AccessException
    {
        final Rational rational = popRationalValue(stack);

        stack.push(rational.getNumerator());
        stack.push(rational.getDenominator());
    }

    private static void _doSub(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Rational y = popRationalValue(stack);
        final Rational x = popRationalValue(stack);

        try {
            stack.push(x.subtract(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.SUBTRACTION_ERROR, x, y);
        }
    }

    private static void _doZeroMinusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popRationalValue(stack).signum() <= 0);
    }

    private static void _doZeroPlusQ(
            final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popRationalValue(stack).signum() >= 0);
    }

    private static void _doZeroQ(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popRationalValue(stack).getNumerator() == 0);
    }

    /** Either must be rationals. */
    public static final Filter EITHER_RATIONAL = new Filter()
        .is(0, Rational.class)
        .is(1, Rational.class)
        .isLong(1)
        .or()
        .and()
        .isLong(0)
        .is(1, Rational.class)
        .and()
        .or();

    /** Top must be a rational. */
    public static final Filter TOP_RATIONAL = new Filter()
        .is(0, Rational.class);

    /**
     * Code.
     */
    private enum _Code
    {
        ABS,
        ADD,
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
