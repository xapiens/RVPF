/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ComplexOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.Complex;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Stack.AccessException;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Complex operations.
 */
public class ComplexOperations
    extends SimpleOperations
{
    /**
     * Pops the complex at the top of the stack.
     *
     * @param stack The stack.
     *
     * @return The complex at the top.
     *
     * @throws AccessException When appropriate.
     */
    public static Complex popComplexValue(
            final Stack stack)
        throws AccessException
    {
        final Serializable object = stack.pop();

        if (object instanceof Complex) {
            return (Complex) object;
        }

        if (object instanceof Number) {
            return Complex.cartesian(((Number) object).doubleValue(), 0.0);
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
            case NEG: {
                _doNeg(stack);

                break;
            }
            case CONJ: {
                _doConj(stack);

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
            case SUB: {
                _doSub(stack);

                break;
            }
            case ARG: {
                _doArg(stack);

                break;
            }
            case ACOS: {
                _doAcos(stack);

                break;
            }
            case ASIN: {
                _doAsin(stack);

                break;
            }
            case ATAN: {
                _doAtan(stack);

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
            case EXP: {
                _doExp(stack);

                break;
            }
            case POW: {
                _doPow(stack);

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
            case I: {
                _doI(stack);

                break;
            }
            case IMAG: {
                _doImag(stack);

                break;
            }
            case REAL: {
                _doReal(stack);

                break;
            }
            case CPLX: {
                _doCplx(stack);

                break;
            }
            case POLAR: {
                _doPolar(stack);

                break;
            }
            case SPLIT: {
                _doSplit(stack);

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
        register("abs", _Code.ABS, TOP_COMPLEX);
        register("conj", _Code.CONJ, TOP_COMPLEX);
        register("neg", _Code.NEG, TOP_COMPLEX);
        register("+", _Code.ADD, EITHER_COMPLEX);
        register("-", _Code.SUB, EITHER_COMPLEX);
        register("/", _Code.DIV, EITHER_COMPLEX);
        register("*", _Code.MUL, EITHER_COMPLEX);
        register("arg", _Code.ARG, TOP_COMPLEX);
        register("acos", _Code.ACOS, TOP_COMPLEX);
        register("asin", _Code.ASIN, TOP_COMPLEX);
        register("atan", _Code.ATAN, TOP_COMPLEX);
        register("cos", _Code.COS, TOP_COMPLEX);
        register("cosh", _Code.COSH, TOP_COMPLEX);
        register("exp", _Code.EXP, TOP_COMPLEX);
        register("i", _Code.I);
        register("imag", _Code.IMAG, TOP_COMPLEX);
        register("polar", _Code.POLAR, Filter.TOP_STRING);
        register("polar", _Code.POLAR, TOP_COMPLEX);
        register("polar", _Code.POLAR, Filter.BOTH_NUMBER);
        register("pow", _Code.POW, EITHER_COMPLEX);
        register("real", _Code.REAL, TOP_COMPLEX);
        register("sgn", _Code.SGN, TOP_COMPLEX);
        register("sin", _Code.SIN, TOP_COMPLEX);
        register("sinh", _Code.SINH, TOP_COMPLEX);
        register("split", _Code.SPLIT, TOP_COMPLEX);
        register("tan", _Code.TAN, TOP_COMPLEX);
        register("tanh", _Code.TANH, TOP_COMPLEX);
        register("cplx", _Code.CPLX, Filter.TOP_STRING);
        register("cplx", _Code.CPLX, TOP_COMPLEX);
        register("cplx", _Code.CPLX, Filter.BOTH_NUMBER);
    }

    private static void _doAbs(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).abs());
    }

    private static void _doAcos(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).acos());
    }

    private static void _doAdd(final Stack stack)
        throws Stack.AccessException
    {
        final Complex y = popComplexValue(stack);
        final Complex x = popComplexValue(stack);

        stack.push(x.add(y));
    }

    private static void _doArg(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).argument());
    }

    private static void _doAsin(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).asin());
    }

    private static void _doAtan(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).atan());
    }

    private static void _doConj(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).conjugate());
    }

    private static void _doCos(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).cos());
    }

    private static void _doCosh(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).cosh());
    }

    private static void _doCplx(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Serializable peek = stack.peek();

        if (peek instanceof String) {
            final String string = stack.popStringValue();

            try {
                stack.push(Complex.valueOf(string).toCartesian());
            } catch (final NumberFormatException exception) {
                fail(ProcessorMessages.CONVERT_FAILED, string, "cplx");
            }
        } else if (peek instanceof Complex) {
            stack.push(popComplexValue(stack).toCartesian());
        } else {
            final double y = stack.popDoubleValue();
            final double x = stack.popDoubleValue();

            stack.push(Complex.cartesian(x, y));
        }
    }

    private static void _doDiv(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Complex y = popComplexValue(stack);
        final Complex x = popComplexValue(stack);

        try {
            stack.push(x.divide(y));
        } catch (final ArithmeticException exception) {
            fail(ProcessorMessages.DIVISION_ERROR, x, y);
        }
    }

    private static void _doExp(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).exp());
    }

    private static void _doI(final Stack stack)
    {
        stack.push(Complex.Cartesian.I);
    }

    private static void _doImag(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).imaginary());
    }

    private static void _doMul(final Stack stack)
        throws Stack.AccessException
    {
        final Complex y = popComplexValue(stack);
        final Complex x = popComplexValue(stack);

        stack.push(x.multiply(y));
    }

    private static void _doNeg(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).negate());
    }

    private static void _doPolar(
            final Stack stack)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Serializable peek = stack.peek();

        if (peek instanceof String) {
            final String x = stack.popStringValue();

            try {
                stack.push(Complex.valueOf(x).toPolar());
            } catch (final NumberFormatException exception) {
                fail(ProcessorMessages.CONVERT_FAILED, x, "polar");
            }
        } else if (peek instanceof Complex) {
            stack.push(popComplexValue(stack).toPolar());
        } else {
            final double y = stack.popDoubleValue();
            final double x = stack.popDoubleValue();

            stack.push(Complex.polar(x, y));
        }
    }

    private static void _doPow(final Stack stack)
        throws Stack.AccessException
    {
        final Complex y = popComplexValue(stack);
        final Complex x = popComplexValue(stack);

        stack.push(x.pow(y));
    }

    private static void _doReal(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).real());
    }

    private static void _doSgn(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).signum());
    }

    private static void _doSin(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).sin());
    }

    private static void _doSinh(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).sinh());
    }

    private static void _doSplit(final Stack stack)
        throws Stack.AccessException
    {
        final Complex complex = popComplexValue(stack);

        if (complex instanceof Complex.Polar) {
            stack.push(complex.magnitude());
            stack.push(complex.angle());
        } else {
            stack.push(complex.real());
            stack.push(complex.imaginary());
        }
    }

    private static void _doSub(final Stack stack)
        throws Stack.AccessException
    {
        final Complex y = popComplexValue(stack);
        final Complex x = popComplexValue(stack);

        stack.push(x.subtract(y));
    }

    private static void _doTan(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).tan());
    }

    private static void _doTanh(final Stack stack)
        throws Stack.AccessException
    {
        stack.push(popComplexValue(stack).tanh());
    }

    /** Top must be a complex. */
    public static final Filter TOP_COMPLEX = new Filter().is(0, Complex.class);

    /** Either must be complex. */
    public static final Filter EITHER_COMPLEX = new Filter()
        .is(0, Complex.class)
        .is(1, Complex.class)
        .is(1, Number.class)
        .or()
        .and()
        .is(0, Number.class)
        .is(1, Complex.class)
        .and()
        .or();

    /**
     * Code.
     */
    private enum _Code
    {
        ABS,
        ACOS,
        ADD,
        ARG,
        ASIN,
        ATAN,
        CONJ,
        COS,
        COSH,
        CPLX,
        DIV,
        EXP,
        I,
        IMAG,
        MUL,
        POLAR,
        NEG,
        POW,
        REAL,
        SGN,
        SIN,
        SINH,
        SPLIT,
        SUB,
        TAN,
        TANH
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
