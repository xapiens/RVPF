/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: BlockOperation.java 4028 2019-05-26 18:11:34Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Compiler;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Block operation.
 */
final class BlockOperation
    extends Operation.Abstract
{
    /**
     * Creates an instance.
     *
     * @param name The name of this operation.
     */
    BlockOperation(@Nonnull final String name)
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
     * End Operation.
     */
    static class EndOperation
        extends Operation.Abstract
    {
        /**
         * Creates an instance.
         *
         * @param name The name of this operation.
         */
        EndOperation(@Nonnull final String name)
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
        _Reference() {}

        /** {@inheritDoc}
         */
        @Override
        public void execute(
                final Task task)
            throws Task.ExecuteException, Stack.AccessException
        {
            for (final OperationReference reference: _references) {
                reference.execute(task);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public Operation getOperation()
        {
            return BlockOperation.this;
        }

        /** {@inheritDoc}
         */
        @Override
        public void setUp(
                final Compiler compiler)
            throws Compiler.CompileException
        {
            for (;;) {
                final Optional<OperationReference> reference = compiler
                    .nextReference();

                if (!reference.isPresent()) {
                    throw new Compiler.CompileException(
                        ProcessorMessages.MISSING_BLOCK_END);
                }

                if (reference.get().getOperation() instanceof EndOperation) {
                    break;
                }

                _references.add(reference.get());
            }
        }

        private final List<OperationReference> _references =
            new LinkedList<OperationReference>();
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
