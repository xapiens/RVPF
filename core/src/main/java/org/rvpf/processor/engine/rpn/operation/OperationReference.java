/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: OperationReference.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.processor.engine.rpn.Compiler;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Operation reference.
 */
public abstract class OperationReference
{
    /**
     * Executes this operation for the specified task.
     *
     * @param task The task.
     *
     * @throws Task.ExecuteException when appropriate.
     * @throws Stack.AccessException When raised by the stack.
     */
    public abstract void execute(
            @Nonnull Task task)
        throws Task.ExecuteException, Stack.AccessException;

    /**
     * Gets the operation.
     *
     * @return The operation.
     */
    @Nonnull
    @CheckReturnValue
    public abstract Operation getOperation();

    /**
     * Gets the position in the source.
     *
     * @return The position.
     */
    @CheckReturnValue
    public final int getPosition()
    {
        return _position;
    }

    /**
     * Sets the position in the source.
     *
     * @param position The position.
     */
    public final void setPosition(final int position)
    {
        _position = position;
    }

    /**
     * Sets up the reference.
     *
     * @param compiler The compiler generating the program.
     *
     * @throws Compiler.CompileException On failure.
     */
    public void setUp(
            @Nonnull final Compiler compiler)
        throws Compiler.CompileException {}

    /**
     * Returns a String representation of itself.
     *
     * @return This Operation's name.
     */
    @Override
    public final String toString()
    {
        return getOperation().toString();
    }

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
     * @param message The message.
     *
     * @throws Task.ExecuteException Always.
     */
    protected static void fail(
            @Nonnull final String message)
        throws Task.ExecuteException
    {
        throw new Task.ExecuteException(BaseMessages.VERBATIM, message);
    }

    private int _position;
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
