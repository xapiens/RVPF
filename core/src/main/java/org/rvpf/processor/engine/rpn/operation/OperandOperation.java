/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: OperandOperation.java 4028 2019-05-26 18:11:34Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.processor.engine.rpn.Compiler;

/**
 * Operand operation.
 */
public final class OperandOperation
    extends SimpleOperation
{
    /**
     * Constructs an Operation.
     *
     * @param name Name of the Operation.
     * @param module Module implementing the Operation.
     * @param code Operation code.
     */
    public OperandOperation(
            @Nonnull final String name,
            @Nonnull final SimpleOperations module,
            @Nonnull final Enum<?> code)
    {
        super(name, module, code, Optional.empty());
    }

    /** {@inheritDoc}
     */
    @Override
    public OperationReference newReference(final Compiler compiler)
    {
        return new Reference(compiler);
    }

    /**
     * Reference.
     */
    public final class Reference
        extends SimpleOperation.Reference
    {
        /**
         * Creates a new reference.
         *
         * @param compiler The compiler.
         */
        Reference(@Nonnull final Compiler compiler)
        {
            _operand = Require.notNull(compiler.getOperand());
        }

        /**
         * Gets the operand.
         *
         * @return The operand.
         */
        public Serializable getOperand()
        {
            return _operand;
        }

        private final Serializable _operand;
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
