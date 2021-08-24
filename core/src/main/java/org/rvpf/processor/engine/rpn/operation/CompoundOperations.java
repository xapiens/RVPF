/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: CompoundOperations.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

/**
 * Compound operations.
 */
public final class CompoundOperations
    extends Operations
{
    /** {@inheritDoc}
     */
    @Override
    protected void setUp()
        throws Operation.OverloadException
    {
        new BlockOperation("{").register(getRegistrations());
        new BlockOperation.EndOperation("}").register(getRegistrations());
        new CondOperation.IfOperation("if").register(getRegistrations());
        new CondOperation.IfOperation.ElseOperation(
            "else").register(getRegistrations());
        new CondOperation.UnlessOperation(
            "unless").register(getRegistrations());
        new LoopOperation.BreakOperation("break").register(getRegistrations());
        new LoopOperation.ContinueOperation(
            "continue").register(getRegistrations());
        new LoopOperation.DoOperation(
            "do",
            getParams()).register(getRegistrations());
        new LoopOperation.WhileOperation(
            "while",
            getParams()).register(getRegistrations());
        new LoopOperation.ReduceOperation(
            "reduce",
            getParams(),
            false).register(getRegistrations());
        new LoopOperation.ReduceOperation(
            "#reduce",
            getParams(),
            true).register(getRegistrations());
        new TryOperation("try").register(getRegistrations());
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
