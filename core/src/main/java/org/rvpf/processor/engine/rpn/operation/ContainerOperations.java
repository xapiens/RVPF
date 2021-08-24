/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ContainerOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.Container;
import org.rvpf.base.value.Dict;
import org.rvpf.base.value.Tuple;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Compiler;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Container operations.
 */
public class ContainerOperations
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
        final Stack stack = task.getStack();

        switch ((Code) reference.getCode()) {
            case CONTAINER_Q: {
                _doContainerQ(stack);

                break;
            }
            case DICT: {
                _doDict(stack);

                break;
            }
            case DICT_Q: {
                _doDictQ(stack);

                break;
            }
            case TUPLE: {
                _doTuple(stack);

                break;
            }
            case TUPLE_Q: {
                _doTupleQ(stack);

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
        register("container?", Code.CONTAINER_Q, Filter.TOP_PRESENT);
        register("dict", Code.DICT);
        register("dict?", Code.DICT_Q, Filter.TOP_PRESENT);
        register("tuple", Code.TUPLE);
        register("tuple?", Code.TUPLE_Q, Filter.TOP_PRESENT);

        new _ApplyOperation("apply").register(getRegistrations());

        new DictOperations().setUp(getRegistrations(), getParams());
        new TupleOperations().setUp(getRegistrations(), getParams());
    }

    private static void _doContainerQ(
            @Nonnull final Stack stack)
        throws Stack.LimitsException
    {
        stack.push(stack.pop() instanceof Container);
    }

    private static void _doDict(@Nonnull final Stack stack)
    {
        stack.push(new Dict());
    }

    private static void _doDictQ(
            @Nonnull final Stack stack)
        throws Stack.LimitsException
    {
        stack.push(stack.pop() instanceof Dict);
    }

    private static void _doTuple(@Nonnull final Stack stack)
    {
        stack.push(new Tuple());
    }

    private static void _doTupleQ(
            @Nonnull final Stack stack)
        throws Stack.LimitsException
    {
        stack.push(stack.pop() instanceof Tuple);
    }

    /** Top must be a container. */
    public static final Filter TOP_CONTAINER = new Filter()
        .is(0, Container.class);

    private enum Code
    {
        CONTAINER_Q,
        DICT,
        DICT_Q,
        TUPLE,
        TUPLE_Q
    }

    /**
     * Apply operation.
     */
    private static final class _ApplyOperation
        extends Operation.Abstract
    {
        /**
         * Constructs an instance.
         *
         * @param name The name of this operation.
         */
        _ApplyOperation(@Nonnull final String name)
        {
            super(name, Optional.of(TOP_CONTAINER));
        }

        /** {@inheritDoc}
         */
        @Override
        public OperationReference newReference(final Compiler compiler)
        {
            return new _Reference();
        }

        /**
         * Reference.
         */
        private final class _Reference
            extends OperationReference
        {
            /**
             * Constructs an instance.
             */
            _Reference() {}

            /** {@inheritDoc}
             */
            @Override
            public void execute(
                    final Task task)
                throws Task.ExecuteException, Stack.AccessException
            {
                filter(task, this);

                final Optional<Container> topContainer = Optional
                    .ofNullable((Container) task.getStack().pop());
                final Optional<Container> oldContainer = task.getContainer();

                task.setContainer(topContainer);
                _reference.execute(task);
                task.setContainer(oldContainer);
            }

            /** {@inheritDoc}
             */
            @Override
            public Operation getOperation()
            {
                return _ApplyOperation.this;
            }

            /** {@inheritDoc}
             */
            @Override
            public void setUp(
                    final Compiler compiler)
                throws Compiler.CompileException
            {
                _reference = compiler.nextReference().orElse(null);

                if ((_reference == null)
                        || (_reference.getOperation()
                            instanceof BlockOperation.EndOperation)) {
                    throw new Compiler.CompileException(
                        ProcessorMessages.MISSING_INSTRUCTION,
                        getName());
                }
            }

            private OperationReference _reference;
        }
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
