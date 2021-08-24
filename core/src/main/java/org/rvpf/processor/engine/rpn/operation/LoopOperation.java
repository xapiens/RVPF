/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: LoopOperation.java 4028 2019-05-26 18:11:34Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.Params;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Compiler;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.RPNEngine;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Loop operation.
 */
abstract class LoopOperation
    extends Operation.Abstract
{
    /**
     * Creates an instance.
     *
     * @param name The name of this Operation.
     * @param filter Optional filter to apply for overload resolution.
     * @param params The engine's params.
     */
    LoopOperation(
            @Nonnull final String name,
            @Nonnull final Optional<Filter> filter,
            @Nonnull final Params params)
    {
        super(name, filter);

        _limit = params
            .getInt(RPNEngine.LOOP_LIMIT_PARAM, RPNEngine.DEFAULT_LOOP_LIMIT);
    }

    /**
     * Gets the loop limit.
     *
     * @return The loop limit.
     */
    int getLimit()
    {
        return _limit;
    }

    private final int _limit;

    /**
     * Break operation.
     */
    static final class BreakOperation
        extends Operation.Abstract
    {
        /**
         * Creates an instance.
         *
         * @param name The name of this operation.
         */
        BreakOperation(@Nonnull final String name)
        {
            super(name);
        }

        /** {@inheritDoc}
         */
        @Override
        public void execute(final Task task)
            throws Task.ExecuteException
        {
            throw new _BreakException();
        }
    }


    /**
     * Continue operation.
     */
    static final class ContinueOperation
        extends Operation.Abstract
    {
        /**
         * Creates an instance.
         *
         * @param name The name of this operation.
         */
        ContinueOperation(@Nonnull final String name)
        {
            super(name);
        }

        /** {@inheritDoc}
         */
        @Override
        public void execute(final Task task)
            throws Task.ExecuteException
        {
            throw new _ContinueException();
        }
    }


    /**
     * Do operation.
     */
    static final class DoOperation
        extends LoopOperation
    {
        /**
         * Creates an instance.
         *
         * @param name The name of this operation.
         * @param params The engine's params.
         */
        DoOperation(@Nonnull final String name, @Nonnull final Params params)
        {
            super(name, Optional.empty(), params);
        }

        /** {@inheritDoc}
         */
        @Override
        public OperationReference newReference(final Compiler compiler)
        {
            return new _Reference(this);
        }

        private final class _Reference
            extends LoopOperation._Reference
        {
            _Reference(final LoopOperation operation)
            {
                super(operation);
            }

            /** {@inheritDoc}
             */
            @Override
            public void execute(
                    final Task task)
                throws Task.ExecuteException, Stack.AccessException
            {
                int count = getOperation().getLimit();

                try {
                    do {
                        try {
                            super.execute(task);
                        } catch (final _ContinueException exception) {
                            // Continues.
                        }
                    } while (task.getStack().popBooleanValue()
                             && (--count > 0));

                    if (count <= 0) {
                        fail("Do iterations exceeded "
                             + getOperation().getLimit());
                    }
                } catch (final _BreakException exception) {
                    // Breaks.
                }
            }
        }
    }


    /**
     * Reduce operation.
     */
    static final class ReduceOperation
        extends LoopOperation
    {
        /**
         * Creates an instance.
         *
         * @param name The name of this operation.
         * @param params The engine's params.
         * @param popTarget True to pop a target.
         */
        ReduceOperation(
                @Nonnull final String name,
                @Nonnull final Params params,
                final boolean popTarget)
        {
            super(
                name,
                popTarget? Optional.of(Filter.TOP_LONG): Optional.empty(),
                params);

            _popTarget = popTarget;
        }

        /** {@inheritDoc}
         */
        @Override
        public OperationReference newReference(final Compiler compiler)
        {
            return new _Reference(this);
        }

        boolean popTarget()
        {
            return _popTarget;
        }

        private final boolean _popTarget;

        /**
         * Reference.
         */
        private final class _Reference
            extends LoopOperation._Reference
        {
            /**
             * Constructs an instance.
             *
             * @param operation A loop operation.
             */
            _Reference(final LoopOperation operation)
            {
                super(operation);
            }

            /** {@inheritDoc}
             */
            @Override
            public void execute(
                    final Task task)
                throws Task.ExecuteException, Stack.AccessException
            {
                filter(task, this);

                final Stack stack = task.getStack();
                final int target = popTarget()? stack.popIntValue(): 1;
                int count = getOperation().getLimit();

                try {
                    while (!stack.isDropped()
                            && (stack.size() > target)
                            && (--count >= 0)) {
                        try {
                            super.execute(task);
                        } catch (final _ContinueException exception) {
                            // Continues.
                        }
                    }

                    if (count < 0) {
                        fail("Reduce iterations exceeded "
                             + getOperation().getLimit());
                    }
                } catch (final _BreakException exception) {
                    // Breaks.
                }
            }
        }
    }


    /**
     * While operation.
     */
    static final class WhileOperation
        extends LoopOperation
    {
        /**
         * Creates an instance.
         *
         * @param name The name of this operation.
         * @param params The engine's params.
         */
        WhileOperation(@Nonnull final String name, @Nonnull final Params params)
        {
            super(name, Optional.of(Filter.TOP_BOOLEAN), params);
        }

        /** {@inheritDoc}
         */
        @Override
        public OperationReference newReference(final Compiler compiler)
        {
            return new _Reference(this);
        }

        /**
         * Reference.
         */
        private final class _Reference
            extends LoopOperation._Reference
        {
            /**
             * Constructs an instance.
             *
             * @param operation The loop operation.
             */
            _Reference(final LoopOperation operation)
            {
                super(operation);
            }

            /** {@inheritDoc}
             */
            @Override
            public void execute(
                    final Task task)
                throws Task.ExecuteException, Stack.AccessException
            {
                filter(task, this);

                int count = getOperation().getLimit();

                try {
                    while (task.getStack().popBooleanValue()
                            && (--count >= 0)) {
                        try {
                            super.execute(task);
                        } catch (final _ContinueException exception) {
                            // Continues.
                        }
                    }

                    if (count < 0) {
                        fail("While iterations exceeded "
                             + getOperation().getLimit());
                    }
                } catch (final _BreakException exception) {
                    // Breaks.
                }
            }
        }
    }


    /**
     * Break exception.
     */
    private static final class _BreakException
        extends Task.ExecuteException
    {
        /**
         * Constructs an instance.
         */
        _BreakException()
        {
            super(ProcessorMessages.UNEXPECTED_OPERATION, "break");
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Continue exception.
     */
    private static final class _ContinueException
        extends Task.ExecuteException
    {
        /**
         * Constructs an instance.
         */
        _ContinueException()
        {
            super(ProcessorMessages.UNEXPECTED_OPERATION, "continue");
        }

        private static final long serialVersionUID = 1L;
    }


    /**
     * Reference.
     */
    private abstract static class _Reference
        extends OperationReference
    {
        /**
         * Constructs an instance.
         *
         * @param operation The loop operation.
         */
        _Reference(final LoopOperation operation)
        {
            _operation = operation;
        }

        /** {@inheritDoc}
         */
        @Override
        public void execute(
                final Task task)
            throws Task.ExecuteException, Stack.AccessException
        {
            _reference.execute(task);
        }

        /** {@inheritDoc}
         */
        @Override
        public final LoopOperation getOperation()
        {
            return _operation;
        }

        /** {@inheritDoc}
         */
        @Override
        public final void setUp(
                final Compiler compiler)
            throws Compiler.CompileException
        {
            _reference = compiler.nextReference().orElse(null);

            if ((_reference == null)
                    || (_reference.getOperation()
                        instanceof BlockOperation.EndOperation)) {
                throw new Compiler.CompileException(
                    ProcessorMessages.MISSING_INSTRUCTION,
                    getOperation().getName());
            }
        }

        private final LoopOperation _operation;
        private OperationReference _reference;
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
