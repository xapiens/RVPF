/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BasicOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.Point;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Program;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Basic operations.
 */
public final class BasicOperations
    extends SimpleOperations
{
    /** {@inheritDoc}
     */
    @Override
    public void execute(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Task.ExecuteException, Stack.LimitsException
    {
        switch ((_Code) reference.getCode()) {
            case BPT: {
                _doBpt(task);

                break;
            }
            case CALL: {
                _doCall(task, reference);

                break;
            }
            case CONSTANT_VALUE: {
                _doConstantValue(task.getStack(), reference);

                break;
            }
            case DELETED_Q: {
                _doDeletedQ(task.getStack());

                break;
            }
            case DUP_INPUT_STORE: {
                _doDupInputStore(task, reference);

                break;
            }
            case DUP_MEMORY_STORE: {
                _doDupMemoryStore(task, reference);

                break;
            }
            case DUP_RESULT_STATE_STORE: {
                _doDupResultStateStore(task);

                break;
            }
            case DUP_RESULT_VALUE_STORE: {
                _doDupResultValueStore(task);

                break;
            }
            case FAIL: {
                _doFail();

                break;
            }
            case INPUT_ALL_VALUES: {
                _doInputAllValues(task);

                break;
            }
            case INPUT_ALL_VALUES_REQ: {
                _doInputAllValuesReq(task);

                break;
            }
            case INPUT_COUNT: {
                _doInputCount(task);

                break;
            }
            case INPUT_POINT: {
                _doInputPoint(task, reference);

                break;
            }
            case INPUT_VALUE_Q: {
                _doInputValueQ(task, reference);

                break;
            }
            case INPUT_VALUE_REQ: {
                _doInputValueReq(task, reference);

                break;
            }
            case INPUT_STAMP: {
                _doInputStamp(task, reference);

                break;
            }
            case INPUT_STATE: {
                _doInputState(task, reference);

                break;
            }
            case INPUT_VALUE_STORE: {
                _doInputValueStore(task, reference);

                break;
            }
            case INPUT_VALUE: {
                _doInputValue(task, reference);

                break;
            }
            case MEMORY_REQ: {
                _doMemoryReq(task, reference);

                break;
            }
            case MEMORY_STORE: {
                _doMemoryStore(task, reference);

                break;
            }
            case MEMORY: {
                _doMemory(task, reference);

                break;
            }
            case NOP: {
                break;
            }
            case PARAM_REQ: {
                _doParamReq(task, reference);

                break;
            }
            case PARAM: {
                _doParam(task, reference);

                break;
            }
            case REQ: {
                _doReq(task.getStack());

                break;
            }
            case RESULT_POINT: {
                _doResultPoint(task);

                break;
            }
            case RESULT_STAMP: {
                _doResultStamp(task);

                break;
            }
            case RESULT_STATE: {
                _doResultState(task);

                break;
            }
            case RESULT_STATE_STORE: {
                _doResultStateStore(task);

                break;
            }
            case RESULT_VALUE_STORE: {
                _doResultValueStore(task);

                break;
            }
            case RESULT_STORED_VALUE: {
                _doResultStoredValue(task);

                break;
            }
            case RESULT_STORED_Q: {
                _doResultStoredQ(task);

                break;
            }
            case RESULT_STORED_VALUE_REQ: {
                _doResultStoredValueReq(task);

                break;
            }
            case RESULT_VALUE: {
                _doResultValue(task);

                break;
            }
            case RESULT_VALUE_REQ: {
                _doResultValueReq(task);

                break;
            }
            case RETURN: {
                _doReturn(task);

                break;
            }
            case TYPE: {
                _doType(task);

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
        register("bpt", _Code.BPT);
        register("deleted?", _Code.DELETED_Q);
        register("fail", _Code.FAIL);
        register("$#", _Code.INPUT_COUNT);
        register("$*", _Code.INPUT_ALL_VALUES);
        register("$*!", _Code.INPUT_ALL_VALUES_REQ);
        register("nop", _Code.NOP);
        register("!", _Code.REQ, Filter.TOP_PRESENT);
        register("$0", _Code.RESULT_VALUE);
        register("$0!", _Code.RESULT_VALUE_REQ);
        register("$0=", _Code.RESULT_VALUE_STORE, Filter.TOP_PRESENT);
        register(":$0=", _Code.DUP_RESULT_VALUE_STORE, Filter.TOP_PRESENT);
        register("return", _Code.RETURN);
        register("$0.", _Code.RESULT_POINT);
        register("$0@", _Code.RESULT_STAMP);
        register("$0$", _Code.RESULT_STATE);
        register("$0$=", _Code.RESULT_STATE_STORE, Filter.TOP_PRESENT);
        register(":$0$=", _Code.DUP_RESULT_STATE_STORE, Filter.TOP_PRESENT);
        register("stored", _Code.RESULT_STORED_VALUE);
        register("stored?", _Code.RESULT_STORED_Q);
        register("stored!", _Code.RESULT_STORED_VALUE_REQ);
        register("type", _Code.TYPE, Filter.TOP_PRESENT);
    }

    private static void _doCall(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Task.ExecuteException
    {
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();

        task.call((Program) operand);
    }

    private static void _doConstantValue(
            final Stack stack,
            final SimpleOperation.Reference reference)
    {
        stack.push(((OperandOperation.Reference) reference).getOperand());
    }

    private static void _doDeletedQ(
            final Stack stack)
        throws Stack.LimitsException
    {
        stack.push(stack.pop() == _DELETED_STATE);
    }

    private static void _doDupInputStore(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Stack.LimitsException
    {
        final Serializable value = task.getStack().peek();
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();

        task.setInput((Integer) operand, value);
    }

    private static void _doDupMemoryStore(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Stack.LimitsException
    {
        final Serializable value = task.getStack().peek();
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();

        task.setMemory((Integer) operand, value);
    }

    private static void _doDupResultStateStore(
            final Task task)
        throws Stack.LimitsException
    {
        final PointValue resultValue = task.getContext().getResult().get();

        resultValue.setState(task.getStack().peek());
    }

    private static void _doDupResultValueStore(
            final Task task)
        throws Stack.LimitsException
    {
        final PointValue resultValue = task
            .getContext()
            .getNormalizedResultValue();

        resultValue.setValue(task.getStack().peek());
    }

    private static void _doFail()
        throws Task.ExecuteException
    {
        fail();
    }

    private static void _doInputAllValues(final Task task)
    {
        final Serializable[] values = task.getInputs();
        final Stack stack = task.getStack();

        for (final Serializable value: values) {
            stack.push(value);
        }
    }

    private static void _doInputAllValuesReq(
            final Task task)
        throws Task.ExecuteException
    {
        final Serializable[] values = task.getInputs();
        final Stack stack = task.getStack();

        for (final Serializable value: values) {
            if (value == null) {
                fail();
            }

            stack.push(value);
        }
    }

    private static void _doInputCount(final Task task)
    {
        task.getStack().push(task.getInputCount());
    }

    private static void _doInputPoint(
            final Task task,
            final SimpleOperation.Reference reference)
    {
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();
        final Optional<PointValue> input = task.getInput((Integer) operand);

        task
            .getStack()
            .push(input.isPresent()? input.get().getPointName().get(): null);
    }

    private static void _doInputStamp(
            final Task task,
            final SimpleOperation.Reference reference)
    {
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();
        final Optional<PointValue> input = task.getInput((Integer) operand);

        task.getStack().push(input.isPresent()? input.get().getStamp(): null);
    }

    private static void _doInputState(
            final Task task,
            final SimpleOperation.Reference reference)
    {
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();
        final Optional<PointValue> input = task.getInput((Integer) operand);
        final Serializable state = input
            .isPresent()? (input
                .get()
                .isDeleted()? _DELETED_STATE: input.get().getState()): null;

        task.getStack().push(state);
    }

    private static void _doInputValue(
            final Task task,
            final SimpleOperation.Reference reference)
    {
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();
        final Optional<? extends PointValue> input = task
            .getNormalizedInput((Integer) operand);

        task.getStack().push(input.isPresent()? input.get().getValue(): null);
    }

    private static void _doInputValueQ(
            final Task task,
            final SimpleOperation.Reference reference)
    {
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();
        final Optional<PointValue> input = task.getInput((Integer) operand);

        task.getStack().push(input.isPresent());
    }

    private static void _doInputValueReq(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Task.ExecuteException, Stack.LimitsException
    {
        _doInputValue(task, reference);

        if (task.getStack().peek() == null) {
            fail();
        }
    }

    private static void _doInputValueStore(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Stack.LimitsException
    {
        final Serializable value = task.getStack().pop();
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();

        task.setInput((Integer) operand, value);
    }

    private static void _doMemory(
            final Task task,
            final SimpleOperation.Reference reference)
    {
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();

        task.getStack().push(task.getMemory((Integer) operand));
    }

    private static void _doMemoryReq(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Task.ExecuteException, Stack.LimitsException
    {
        _doMemory(task, reference);

        if (task.getStack().peek() == null) {
            fail();
        }
    }

    private static void _doMemoryStore(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Stack.LimitsException
    {
        final Serializable value = task.getStack().pop();
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();

        task.setMemory((Integer) operand, value);
    }

    private static void _doParam(
            final Task task,
            final SimpleOperation.Reference reference)
    {
        final Object operand = ((OperandOperation.Reference) reference)
            .getOperand();

        task.getStack().push(task.getParam((Integer) operand).orElse(null));
    }

    private static void _doParamReq(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Task.ExecuteException, Stack.LimitsException
    {
        _doParam(task, reference);

        if (task.getStack().peek() == null) {
            final Object operand = ((OperandOperation.Reference) reference)
                .getOperand();
            final Optional<Point> point = task.getResultPoint();

            fail(
                ProcessorMessages.MISSING_POINT_PARAM,
                operand,
                point.orElse(null));
        }
    }

    private static void _doReq(
            final Stack stack)
        throws Task.ExecuteException, Stack.LimitsException
    {
        if (stack.pop() == null) {
            fail();
        }
    }

    private static void _doResultPoint(final Task task)
    {
        final PointValue resultValue = task.getContext().getResult().get();

        task.getStack().push(resultValue.getPointName().get());
    }

    private static void _doResultStamp(final Task task)
    {
        final PointValue resultValue = task.getContext().getResult().get();

        task.getStack().push(resultValue.getStamp());
    }

    private static void _doResultState(final Task task)
    {
        final PointValue resultValue = task.getContext().getResult().get();

        task.getStack().push(resultValue.getState());
    }

    private static void _doResultStateStore(
            final Task task)
        throws Stack.LimitsException
    {
        final PointValue resultValue = task.getContext().getResult().get();

        resultValue.setState(task.getStack().pop());
    }

    private static void _doResultStoredQ(final Task task)
    {
        final PointValue pointValue = task.getContext().getStoredValue().get();

        task.getStack().push(pointValue.isPresent());
    }

    private static void _doResultStoredValue(final Task task)
    {
        final PointValue pointValue = task.getContext().getStoredValue().get();

        task
            .getStack()
            .push(pointValue.isPresent()? pointValue.getValue(): null);
    }

    private static void _doResultStoredValueReq(
            final Task task)
        throws Task.ExecuteException, Stack.LimitsException
    {
        _doResultValue(task);

        if (task.getStack().peek() == null) {
            fail();
        }
    }

    private static void _doResultValue(final Task task)
    {
        final PointValue resultValue = task
            .getContext()
            .getNormalizedResultValue();

        task.getStack().push(resultValue.getValue());
    }

    private static void _doResultValueReq(
            final Task task)
        throws Task.ExecuteException, Stack.LimitsException
    {
        _doResultValue(task);

        if (task.getStack().peek() == null) {
            fail();
        }
    }

    private static void _doResultValueStore(
            final Task task)
        throws Stack.LimitsException
    {
        final PointValue resultValue = task
            .getContext()
            .getNormalizedResultValue();

        resultValue.setValue(task.getStack().pop());
    }

    private static void _doReturn(final Task task)
    {
        task.end();
    }

    private static void _doType(final Task task)
        throws Stack.LimitsException
    {
        final Serializable x = task.getStack().pop();

        task.getStack().push((x != null)? x.getClass().getName(): "null");
    }

    private void _doBpt(final Task task)
    {
        // Breakpoint opportunity.
    }

    /** Calls a program. */
    public static final Enum<_Code> CALL_CODE = _Code.CALL;

    /** Pushes a constant. */
    public static final Enum<_Code> CONSTANT_VALUE_CODE = _Code.CONSTANT_VALUE;

    /** Stores peek in an input value. */
    public static final Enum<_Code> DUP_INPUT_STORE_CODE =
        _Code.DUP_INPUT_STORE;

    /** Stores peek in a memory location. */
    public static final Enum<_Code> DUP_MEMORY_STORE_CODE =
        _Code.DUP_MEMORY_STORE;

    /** Pushes an input stamp. */
    public static final Enum<_Code> INPUT_POINT_CODE = _Code.INPUT_POINT;

    /** Pushes an input stamp. */
    public static final Enum<_Code> INPUT_STAMP_CODE = _Code.INPUT_STAMP;

    /** Pushes an input state. */
    public static final Enum<_Code> INPUT_STATE_CODE = _Code.INPUT_STATE;

    /** Pushes an input value. */
    public static final Enum<_Code> INPUT_VALUE_CODE = _Code.INPUT_VALUE;

    /** Queries an input value. */
    public static final Enum<_Code> INPUT_VALUE_Q_CODE = _Code.INPUT_VALUE_Q;

    /** Requires an input value. */
    public static final Enum<_Code> INPUT_VALUE_REQ_CODE =
        _Code.INPUT_VALUE_REQ;

    /** Stores in an input value. */
    public static final Enum<_Code> INPUT_VALUE_STORE_CODE =
        _Code.INPUT_VALUE_STORE;

    /** Pushes a memory location. */
    public static final Enum<_Code> MEMORY_CODE = _Code.MEMORY;

    /** Requires a memory location. */
    public static final Enum<_Code> MEMORY_REQ_CODE = _Code.MEMORY_REQ;

    /** Stores in a memory location. */
    public static final Enum<_Code> MEMORY_STORE_CODE = _Code.MEMORY_STORE;

    /** Pushes a parameter value. */
    public static final Enum<_Code> PARAM_CODE = _Code.PARAM;

    /** Requires a parameter value. */
    public static final Enum<_Code> PARAM_REQ_CODE = _Code.PARAM_REQ;
    private static final Serializable _DELETED_STATE = new Serializable()
    {
        private static final long serialVersionUID = 1L;
    };

    /**
     * Code.
     */
    private enum _Code
    {
        CALL,
        CONSTANT_VALUE,
        DELETED_Q,
        DUP_INPUT_STORE,
        DUP_MEMORY_STORE,
        INPUT_POINT,
        INPUT_STAMP,
        INPUT_STATE,
        INPUT_VALUE,
        INPUT_VALUE_Q,
        INPUT_VALUE_REQ,
        INPUT_VALUE_STORE,
        MEMORY,
        MEMORY_REQ,
        MEMORY_STORE,
        PARAM,
        PARAM_REQ,
        BPT,
        DUP_RESULT_STATE_STORE,
        DUP_RESULT_VALUE_STORE,
        FAIL,
        INPUT_ALL_VALUES,
        INPUT_ALL_VALUES_REQ,
        INPUT_COUNT,
        NOP,
        REQ,
        RESULT_POINT,
        RESULT_STAMP,
        RESULT_STATE,
        RESULT_STATE_STORE,
        RESULT_STORED_Q,
        RESULT_STORED_VALUE,
        RESULT_STORED_VALUE_REQ,
        RESULT_VALUE,
        RESULT_VALUE_REQ,
        RESULT_VALUE_STORE,
        RETURN,
        TYPE
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
