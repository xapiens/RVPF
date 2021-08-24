/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Operation.java 4066 2019-06-07 20:23:56Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.Compiler;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Operation.
 */
public interface Operation
{
    /**
     * Executes this for the specified Task.
     *
     * @param task The Task.
     *
     * @throws Task.ExecuteException when appropriate.
     */
    void execute(@Nonnull Task task)
        throws Task.ExecuteException;

    /**
     * Apply the overloading filters to locate the appropriate operation.
     *
     * @param task The task owning the stack.
     * @param reference The operation reference.
     *
     * @return The selected operation.
     *
     * @throws Task.ExecuteException On failure.
     */
    @Nonnull
    @CheckReturnValue
    Operation filter(
            @Nonnull Task task,
            @Nonnull OperationReference reference)
        throws Task.ExecuteException;

    /**
     * Gets the name of this operation.
     *
     * @return The name of this operation.
     */
    @Nonnull
    @CheckReturnValue
    String getName();

    /**
     * Returns a new reference to this operation.
     *
     * @param compiler The compiler generating the program.
     *
     * @return The operation reference.
     *
     * @throws Compiler.CompileException On failure to build reference.
     */
    @Nonnull
    @CheckReturnValue
    OperationReference newReference(
            @Nonnull Compiler compiler)
        throws Compiler.CompileException;

    /**
     * Registers itself on a registrations map.
     *
     * @param registrations The registrations map.
     *
     * @throws OverloadException When not a subclass of the overloaded.
     */
    void register(
            @Nonnull Map<String, Operation> registrations)
        throws OverloadException;

    /**
     * Abstract operation.
     */
    abstract class Abstract
        implements Operation
    {
        /**
         * Creates an instance.
         *
         * @param name The name of this operation.
         */
        protected Abstract(@Nonnull final String name)
        {
            this(name, Optional.empty());
        }

        /**
         * Creates an instance.
         *
         * @param name The name of this operation.
         * @param filter Optional filter to apply for overload resolution.
         */
        protected Abstract(
                @Nonnull final String name,
                @Nonnull final Optional<Filter> filter)
        {
            _name = Require.notNull(name);
            _filter = filter.orElse(null);
        }

        /** {@inheritDoc}
         */
        @Override
        public void execute(final Task task)
            throws Task.ExecuteException {}

        /** {@inheritDoc}
         */
        @Override
        public final Operation filter(
                final Task task,
                final OperationReference reference)
            throws Task.ExecuteException
        {
            final Operation filtered;

            if (_filter == null) {
                filtered = this;
            } else if (_filter.accept(task)) {
                filtered = this;
            } else if (_overloaded == null) {
                filtered = null;
            } else {
                filtered = _overloaded.filter(task, reference);
            }

            if (filtered == null) {
                throw new Task.ExecuteException(
                    ProcessorMessages.OPERATION_ARGS,
                    this,
                    String.valueOf(reference.getPosition()),
                    task.getResultPoint().get());
            }

            return filtered;
        }

        /** {@inheritDoc}
         */
        @Override
        public final String getName()
        {
            return _name;
        }

        /** {@inheritDoc}
         */
        @Override
        public OperationReference newReference(final Compiler compiler)
        {
            return new _Reference(this);
        }

        /** {@inheritDoc}
         */
        @Override
        public final void register(
                final Map<String, Operation> registrations)
            throws OverloadException
        {
            _overloaded = registrations
                .put(_name.toUpperCase(Locale.ROOT), this);

            if ((_overloaded != null)
                    && (_filter != null)
                    && (!_overloaded.getClass().isInstance(this))) {
                throw new OverloadException(this, _overloaded);
            }
        }

        /**
         * Returns a String representation of itself.
         *
         * @return This Operation's name.
         */
        @Override
        public final String toString()
        {
            return _name;
        }

        private Filter _filter;
        private String _name;
        private Operation _overloaded;

        /**
         * Reference.
         */
        private static final class _Reference
            extends OperationReference
        {
            /**
             * Creates an instance.
             *
             * @param operation The operation referenced.
             */
            _Reference(@Nonnull final Operation operation)
            {
                _operation = operation;
            }

            /** {@inheritDoc}
             */
            @Override
            public void execute(final Task task)
                throws Task.ExecuteException
            {
                _operation.execute(task);
            }

            /** {@inheritDoc}
             */
            @Override
            public Operation getOperation()
            {
                return _operation;
            }

            private final Operation _operation;
        }
    }


    /**
     * Overload Exception.
     */
    final class OverloadException
        extends Exception
    {
        /**
         * Constructs an OverloadException.
         *
         * @param overloader The Operation doing the overload.
         * @param overloaded The Operation being overloaded.
         */
        public OverloadException(
                @Nonnull final Object overloader,
                @Nonnull final Object overloaded)
        {
            super("Class '" + overloader.getClass().getName()
                  + "' is not a subclass of '"
                  + overloaded.getClass().getName() + "'");
        }

        private static final long serialVersionUID = 1L;
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
