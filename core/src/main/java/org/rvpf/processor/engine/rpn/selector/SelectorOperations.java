/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.processor.engine.rpn.selector;

import java.util.Optional;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;
import org.rvpf.processor.engine.rpn.operation.Operation;
import org.rvpf.processor.engine.rpn.operation.SimpleOperation;
import org.rvpf.processor.engine.rpn.operation.SimpleOperations;

/**
 * Selector operations.
 */
public abstract class SelectorOperations
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
        final SelectorTransform.Context context =
            (SelectorTransform.Context) task
                .getContext();
        final Stack stack = task.getStack();

        switch ((_Code) reference.getCode()) {
            case FIRST_STAMP: {
                _doFirstStamp(context, stack);

                break;
            }
            case FIRST_STATE: {
                _doFirstState(context, stack);

                break;
            }
            case FIRST_VALUE: {
                _doFirstValue(context, stack);

                break;
            }
            case LAST_STAMP: {
                _doLastStamp(context, stack);

                break;
            }
            case LAST_STATE: {
                _doLastState(context, stack);

                break;
            }
            case LAST_VALUE: {
                _doLastValue(context, stack);

                break;
            }
            case NEXT_STAMP: {
                _doNextStamp(context, stack);

                break;
            }
            case NEXT_STATE: {
                _doNextState(context, stack);

                break;
            }
            case NEXT_VALUE: {
                _doNextValue(context, stack);

                break;
            }
            case PREV_STAMP: {
                _doPrevStamp(context, stack);

                break;
            }
            case PREV_STATE: {
                _doPrevState(context, stack);

                break;
            }
            case PREV_VALUE: {
                _doPrevValue(context, stack);

                break;
            }
            case START_STAMP: {
                _doStartStamp(context, stack);

                break;
            }
            case START_STATE: {
                _doStartState(context, stack);

                break;
            }
            case START_VALUE: {
                _doStartValue(context, stack);

                break;
            }
            case STEP_NUMBER: {
                _doStepNumber(context, stack);

                break;
            }
            case STEP_STAMP: {
                _doStepStamp(context, stack);

                break;
            }
            case STEP_STATE: {
                _doStepState(context, stack);

                break;
            }
            case STEP_VALUE: {
                _doStepValue(context, stack);

                break;
            }
            case STEP_VALUE_REQ: {
                _doStepValueReq(context, stack);

                break;
            }
            case STOP_STAMP: {
                _doStopStamp(context, stack);

                break;
            }
            case STOP_STATE: {
                _doStopState(context, stack);

                break;
            }
            case STOP_VALUE: {
                _doStopValue(context, stack);

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
        register("$", _Code.STEP_VALUE);
        register("$!", _Code.STEP_VALUE_REQ);
        register("$@", _Code.STEP_STAMP);
        register("$$", _Code.STEP_STATE);
        register("first", _Code.FIRST_VALUE);
        register("first@", _Code.FIRST_STAMP);
        register("first$", _Code.FIRST_STATE);
        register("last", _Code.LAST_VALUE);
        register("last@", _Code.LAST_STAMP);
        register("last$", _Code.LAST_STATE);
        register("next", _Code.NEXT_VALUE);
        register("next@", _Code.NEXT_STAMP);
        register("next$", _Code.NEXT_STATE);
        register("prev", _Code.PREV_VALUE);
        register("prev@", _Code.PREV_STAMP);
        register("prev$", _Code.PREV_STATE);
        register("start", _Code.START_VALUE);
        register("start@", _Code.START_STAMP);
        register("start", _Code.START_STATE);
        register("step", _Code.STEP_NUMBER);
        register("stop", _Code.STOP_VALUE);
        register("stop@", _Code.STOP_STAMP);
        register("stop", _Code.STOP_STATE);
    }

    private static void _doFirstStamp(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getFirstStep();

        stack.push(step.isPresent()? step.get().getStamp(): null);
    }

    private static void _doFirstState(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getFirstStep();

        stack.push(step.isPresent()? step.get().getState(): null);
    }

    private static void _doFirstValue(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getFirstStep();

        stack.push(step.isPresent()? step.get().getValue(): null);
    }

    private static void _doLastStamp(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getLastStep();

        stack.push(step.isPresent()? step.get().getStamp(): null);
    }

    private static void _doLastState(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getLastStep();

        stack.push(step.isPresent()? step.get().getState(): null);
    }

    private static void _doLastValue(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getLastStep();

        stack.push(step.isPresent()? step.get().getValue(): null);
    }

    private static void _doNextStamp(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getNextStep();

        stack.push(step.isPresent()? step.get().getStamp(): null);
    }

    private static void _doNextState(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getNextStep();

        stack.push(step.isPresent()? step.get().getState(): null);
    }

    private static void _doNextValue(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getNextStep();

        stack.push(step.isPresent()? step.get().getValue(): null);
    }

    private static void _doPrevStamp(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getPreviousStep();

        stack.push(step.isPresent()? step.get().getStamp(): null);
    }

    private static void _doPrevState(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getPreviousStep();

        stack.push(step.isPresent()? step.get().getState(): null);
    }

    private static void _doPrevValue(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getPreviousStep();

        stack.push(step.isPresent()? step.get().getValue(): null);
    }

    private static void _doStartStamp(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> start = context.getStart();

        stack.push(start.isPresent()? start.get().getStamp(): null);
    }

    private static void _doStartState(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> start = context.getStart();

        stack.push(start.isPresent()? start.get().getState(): null);
    }

    private static void _doStartValue(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> start = context.getStart();

        stack.push(start.isPresent()? start.get().getValue(): null);
    }

    private static void _doStepNumber(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        stack.push(context.getStepNumber());
    }

    private static void _doStepStamp(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getCurrentStep();

        stack.push(step.isPresent()? step.get().getStamp(): null);
    }

    private static void _doStepState(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getCurrentStep();

        stack.push(step.isPresent()? step.get().getState(): null);
    }

    private static void _doStepValue(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<? extends PointValue> step = context.getCurrentStep();

        stack.push(step.isPresent()? step.get().getValue(): null);
    }

    private static void _doStepValueReq(
            final SelectorTransform.Context context,
            final Stack stack)
        throws Task.ExecuteException, Stack.LimitsException
    {
        _doStepValue(context, stack);

        if (stack.peek() == null) {
            throw new Task.ExecuteException();
        }
    }

    private static void _doStopStamp(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<PointValue> stop = context.getStop();

        stack.push(stop.isPresent()? stop.get().getStamp(): null);
    }

    private static void _doStopState(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<PointValue> stop = context.getStop();

        stack.push(stop.isPresent()? stop.get().getState(): null);
    }

    private static void _doStopValue(
            final SelectorTransform.Context context,
            final Stack stack)
    {
        final Optional<PointValue> stop = context.getStop();

        stack.push(stop.isPresent()? stop.get().getValue(): null);
    }

    /**
     * Code.
     */
    private enum _Code
    {
        FIRST_STAMP,
        FIRST_STATE,
        FIRST_VALUE,
        LAST_STAMP,
        LAST_STATE,
        LAST_VALUE,
        NEXT_STAMP,
        NEXT_STATE,
        NEXT_VALUE,
        PREV_STAMP,
        PREV_STATE,
        PREV_VALUE,
        START_STAMP,
        START_STATE,
        START_VALUE,
        STEP_NUMBER,
        STEP_STAMP,
        STEP_STATE,
        STEP_VALUE,
        STEP_VALUE_REQ,
        STOP_STAMP,
        STOP_STATE,
        STOP_VALUE
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
