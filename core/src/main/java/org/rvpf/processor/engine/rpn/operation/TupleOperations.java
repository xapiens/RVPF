/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TupleOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.Tuple;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.RPNEngine;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Tuple operations.
 */
class TupleOperations
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
        final Tuple container = (Tuple) task.getContainer().get();

        switch ((_Code) reference.getCode()) {
            case APPEND: {
                _doAppend(task.getStack(), container);

                break;
            }
            case GET: {
                _doGet(task.getStack(), container);

                break;
            }
            case PUT: {
                _doPut(task.getStack(), container);

                break;
            }
            case REMOVE: {
                _doRemove(task.getStack(), container);

                break;
            }
            case SIZE: {
                _doSize(task.getStack(), container);

                break;
            }
            case VALUES: {
                _doValues(task.getStack(), container);

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
        _limit = getParams()
            .getInt(RPNEngine.LOOP_LIMIT_PARAM, RPNEngine.DEFAULT_LOOP_LIMIT);

        register("append", _Code.APPEND, _APPLYING_TUPLE_TOP_PRESENT);
        register("get", _Code.GET, _APPLYING_TUPLE_TOP_LONG);
        register("put", _Code.PUT, _APPLYING_TUPLE_TOP_LONG_PRESENT);
        register("remove", _Code.REMOVE, _APPLYING_TUPLE_TOP_LONG);
        register("size", _Code.SIZE, _APPLYING_TUPLE);
        register("values", _Code.VALUES, _APPLYING_TUPLE);
    }

    private static void _doAppend(
            final Stack stack,
            final Tuple container)
        throws Stack.LimitsException
    {
        container.add(stack.pop());
    }

    private static void _doGet(
            final Stack stack,
            final Tuple container)
        throws Task.ExecuteException, Stack.AccessException
    {
        final int index = stack.popIntValue();

        if (index >= 0) {
            if (index < container.size()) {
                stack.push(container.get(index));
            } else {
                stack.push((Serializable) null);
            }
        } else {
            throw new Task.ExecuteException(
                ProcessorMessages.TUPLE_INDEX_OUT_OF_BOUNDS,
                String.valueOf(index));
        }
    }

    private static void _doRemove(
            final Stack stack,
            final Tuple container)
        throws Task.ExecuteException, Stack.AccessException
    {
        final int index = stack.popIntValue();

        if (index >= 0) {
            if (index < container.size()) {
                stack.push(container.remove(index));
            } else {
                stack.push((Serializable) null);
            }
        } else {
            throw new Task.ExecuteException(
                ProcessorMessages.TUPLE_INDEX_OUT_OF_BOUNDS,
                String.valueOf(index));
        }
    }

    private static void _doSize(final Stack stack, final Tuple container)
    {
        stack.push(Integer.valueOf(container.size()));
    }

    private static void _doValues(final Stack stack, final Tuple container)
    {
        for (final Serializable value: container) {
            stack.push(value);
        }
    }

    private void _doPut(
            final Stack stack,
            final Tuple container)
        throws Task.ExecuteException, Stack.AccessException
    {
        final int index = stack.popIntValue();

        if ((index >= 0) && (index < _limit)) {
            if (index < container.size()) {
                container.set(index, stack.pop());
            } else {
                int limit = container.size();

                container.ensureCapacity(index + 1);

                while (limit++ < index) {
                    container.add(null);
                }

                container.add(stack.pop());
            }
        } else {
            throw new Task.ExecuteException(
                ProcessorMessages.TUPLE_INDEX_OUT_OF_BOUNDS,
                String.valueOf(index));
        }
    }

    private static final Filter _APPLYING_TUPLE = new Filter()
        .isApplying(Tuple.class);
    private static final Filter _APPLYING_TUPLE_TOP_LONG = new Filter()
        .isApplying(Tuple.class)
        .isLong(0)
        .and();
    private static final Filter _APPLYING_TUPLE_TOP_LONG_PRESENT = new Filter()
        .isApplying(Tuple.class)
        .isLong(0)
        .and()
        .isPresent(1)
        .and();
    private static final Filter _APPLYING_TUPLE_TOP_PRESENT = new Filter()
        .isApplying(Tuple.class)
        .isPresent(0)
        .and();

    private int _limit;

    /**
     * Code.
     */
    private enum _Code
    {
        APPEND,
        GET,
        PUT,
        REMOVE,
        SIZE,
        VALUES
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
