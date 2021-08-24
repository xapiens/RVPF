/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CondOperation.java 4028 2019-05-26 18:11:34Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Compiler;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Conditional operation.
 */
abstract class CondOperation
    extends Operation.Abstract
{
    /**
     * Creates an instance.
     *
     * @param name The name of this operation.
     */
    CondOperation(@Nonnull final String name)
    {
        super(name, Optional.of(Filter.TOP_BOOLEAN));
    }

    /**
     * If Operation.
     */
    static final class IfOperation
        extends CondOperation
    {
        /**
         * Creates an instance.
         *
         * @param name The name of this operation.
         */
        IfOperation(@Nonnull final String name)
        {
            super(name);
        }

        /** {@inheritDoc}
         */
        @Override
        public OperationReference newReference(final Compiler compiler)
        {
            return new _Reference();
        }

        /**
         * Else Operation.
         */
        static final class ElseOperation
            extends Operation.Abstract
        {
            /**
             * Creates an instance.
             *
             * @param name The name of this operation.
             */
            ElseOperation(@Nonnull final String name)
            {
                super(name);
            }
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

                if (task.getStack().popBooleanValue()) {
                    if (_whenTrue != null) {
                        _whenTrue.execute(task);
                    }
                } else {
                    if (_whenFalse != null) {
                        _whenFalse.execute(task);
                    }
                }
            }

            /** {@inheritDoc}
             */
            @Override
            public Operation getOperation()
            {
                return IfOperation.this;
            }

            /** {@inheritDoc}
             */
            @Override
            public void setUp(
                    final Compiler compiler)
                throws Compiler.CompileException
            {
                Optional<OperationReference> reference = compiler
                    .nextReference();

                if (!reference.isPresent()
                        || (reference.get().getOperation()
                            instanceof BlockOperation.EndOperation)) {
                    throw new Compiler.CompileException(
                        ProcessorMessages.MISSING_INSTRUCTION,
                        getName());
                }

                if (reference.get().getOperation() instanceof ElseOperation) {
                    _whenFalse = reference.get();
                } else {
                    _whenTrue = reference.get();
                    reference = compiler.peekReference();

                    if (reference.isPresent()
                            && (reference.get().getOperation()
                                instanceof ElseOperation)) {
                        _whenFalse = reference.get();
                        compiler.nextReference();
                    }

                    if (_whenFalse != null) {
                        reference = compiler.nextReference();

                        if (!reference.isPresent()
                                || (reference.get().getOperation()
                                    instanceof BlockOperation.EndOperation)) {
                            throw new Compiler.CompileException(
                                ProcessorMessages.MISSING_INSTRUCTION,
                                _whenFalse.getOperation().getName());
                        }

                        _whenFalse = reference.get();
                    }
                }
            }

            private OperationReference _whenFalse;
            private OperationReference _whenTrue;
        }
    }


    /**
     * Unless Operation.
     */
    static final class UnlessOperation
        extends CondOperation
    {
        /**
         * Creates an instance.
         *
         * @param name The name of this operation.
         */
        UnlessOperation(@Nonnull final String name)
        {
            super(name);
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

                if (!task.getStack().popBooleanValue()) {
                    _reference.execute(task);
                }
            }

            /** {@inheritDoc}
             */
            @Override
            public Operation getOperation()
            {
                return UnlessOperation.this;
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
