/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TryOperation.java 4028 2019-05-26 18:11:34Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import javax.annotation.Nonnull;

import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Compiler;
import org.rvpf.processor.engine.rpn.Compiler.CompileException;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Try operation.
 */
final class TryOperation
    extends Operation.Abstract
{
    /**
     * Creates an insatance.
     *
     * @param name The name of this operation.
     */
    TryOperation(@Nonnull final String name)
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
            final Stack mark = task.getStack().mark();

            try {
                _onTry.execute(task);
            } catch (final Task.ExecuteException exception) {
                if (exception.getMessage() != null) {
                    throw exception;
                }

                if (mark.isDropped()) {
                    fail("The '" + _onTry + "' operation left a bad stack");
                }

                while (task.getStack() != mark) {
                    task.getStack().unmark();
                }

                mark.clear();

                _onCatch.execute(task);
            }

            while (!mark.isDropped()) {
                task.getStack().unmark();
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public Operation getOperation()
        {
            return TryOperation.this;
        }

        /** {@inheritDoc}
         */
        @Override
        public void setUp(final Compiler compiler)
            throws CompileException
        {
            _onTry = compiler.nextReference().orElse(null);

            if (BlockOperation.EndOperation.class.isInstance(_onTry)) {
                _onTry = null;
            } else {
                _onCatch = compiler.nextReference().orElse(null);

                if (BlockOperation.EndOperation.class.isInstance(_onCatch)) {
                    _onCatch = null;
                }
            }

            if ((_onTry == null) || (_onCatch == null)) {
                throw new CompileException(
                    ProcessorMessages.MISSING_2_INSTRUCTIONS,
                    "try");
            }
        }

        private OperationReference _onCatch;
        private OperationReference _onTry;
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
