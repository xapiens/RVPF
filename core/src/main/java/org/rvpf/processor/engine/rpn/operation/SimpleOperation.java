/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SimpleOperation.java 4013 2019-05-21 10:10:45Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.processor.engine.rpn.Compiler;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Simple operation.
 *
 * <p>This class represents an Operation supported by a SimpleOperations module.
 * Each SimpleOperation has a name, an integer code, a reference to the module
 * implementing the SimpleOperation and an optional Filter to select between
 * overloaded SimpleOperations.</p>
 */
public class SimpleOperation
    extends Operation.Abstract
{
    /**
     * Constructs an instance.
     *
     * @param name Name of the operation.
     * @param module Module implementing the operation.
     * @param code Operation code.
     * @param filter Optional filter to apply to match the Operation operands.
     */
    public SimpleOperation(
            @Nonnull final String name,
            @Nonnull final SimpleOperations module,
            @Nonnull final Enum<?> code,
            @Nonnull final Optional<Filter> filter)
    {
        super(name, filter);

        _module = Require.notNull(module);
        _code = code;
    }

    /** {@inheritDoc}
     */
    @Override
    public OperationReference newReference(final Compiler compiler)
    {
        return new Reference();
    }

    /**
     * Gets the code.
     *
     * @return The code.
     */
    final Enum<?> getCode()
    {
        return _code;
    }

    /**
     * Gets the module.
     *
     * @return The module.
     */
    final SimpleOperations getModule()
    {
        return _module;
    }

    private Enum<?> _code;
    private SimpleOperations _module;

    /**
     * Reference.
     */
    public class Reference
        extends OperationReference
    {
        /** {@inheritDoc}
         */
        @Override
        public void execute(
                final Task task)
            throws Task.ExecuteException, Stack.AccessException
        {
            try {
                _operation = (SimpleOperation) filter(task, this);
            } catch (final Task.ExecuteException exception) {
                if (task.getFailReturnsNull()) {
                    throw new Task.ExecuteException();
                }

                throw exception;
            }

            _operation.getModule().execute(task, this);
            _operation = SimpleOperation.this;
        }

        /**
         * Gets the operation code.
         *
         * @return The operation code.
         */
        public final Object getCode()
        {
            return _operation.getCode();
        }

        /**
         * Gets the operation.
         *
         * @return The operation.
         */
        @Override
        public Operation getOperation()
        {
            return _operation;
        }

        private SimpleOperation _operation = SimpleOperation.this;
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
