/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SimpleOperations.java 4013 2019-05-21 10:10:45Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.logger.Messages;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Simple operations.
 *
 * <p>Abstract class base to build a simple operations module.</p>
 */
public abstract class SimpleOperations
    extends Operations
{
    /**
     * Executes an operation code within the context of a task.
     *
     * <p>Must be implemented by each module.</p>
     *
     * @param task Task executing the program.
     * @param reference Reference to the operation.
     *
     * @throws Task.ExecuteException when appropriate.
     * @throws Stack.AccessException When raised by the stack.
     */
    public abstract void execute(
            @Nonnull Task task,
            @Nonnull SimpleOperation.Reference reference)
        throws Task.ExecuteException, Stack.AccessException;

    /**
     * Fails silently.
     *
     * @throws Task.ExecuteException Always.
     */
    protected static void fail()
        throws Task.ExecuteException
    {
        throw new Task.ExecuteException();
    }

    /**
     * Fails with a message.
     *
     * @param entry The message format.
     * @param params The message parameters.
     *
     * @throws Task.ExecuteException Always.
     */
    protected static void fail(
            @Nonnull final Messages.Entry entry,
            @Nonnull final Object... params)
        throws Task.ExecuteException
    {
        throw new Task.ExecuteException(entry, params);
    }

    /**
     * Registers an operation without filter.
     *
     * @param name The operation name.
     * @param code The operation code.
     *
     * @throws Operation.OverloadException From {@link Operation#register}.
     */
    protected final void register(
            @Nonnull final String name,
            @Nonnull final Enum<?> code)
        throws Operation.OverloadException
    {
        register(name, code, Optional.empty());
    }

    /**
     * Registers an operation.
     *
     * @param name The operation name.
     * @param code The operation code.
     * @param filter The operation overload filter.
     *
     * @throws Operation.OverloadException From {@link Operation#register}.
     */
    protected final void register(
            @Nonnull final String name,
            @Nonnull final Enum<?> code,
            @Nonnull final Filter filter)
        throws Operation.OverloadException
    {
        new SimpleOperation(name, this, code, Optional.of(filter))
            .register(getRegistrations());
    }

    /**
     * Registers an operation.
     *
     * @param name The operation name.
     * @param code The operation code.
     * @param filter The optional operation overload filter.
     *
     * @throws Operation.OverloadException From {@link Operation#register}.
     */
    protected final void register(
            @Nonnull final String name,
            @Nonnull final Enum<?> code,
            @Nonnull final Optional<Filter> filter)
        throws Operation.OverloadException
    {
        new SimpleOperation(name, this, code, filter)
            .register(getRegistrations());
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
