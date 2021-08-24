/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Program.java 4032 2019-05-27 20:50:32Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.processor.engine.rpn.operation.OperationReference;

/**
 * Contains the instructions produced by the {@link Compiler} and executer by
 * {@link Task}s.
 */
public final class Program
    implements Serializable
{
    /**
     * Constructs a program object.
     */
    Program() {}

    /**
     * Adds an operation reference to this program.
     *
     * @param reference The operation reference.
     */
    void add(@Nonnull final OperationReference reference)
    {
        _referencesList.add(reference);
    }

    /**
     * Freezes the code of this program.
     */
    void freeze()
    {
        _references = _referencesList
            .toArray(new OperationReference[_referencesList.size()]);
        _referencesList = null;
    }

    /**
     * Gets the operation reference at the specified program counter.
     *
     * @param pc The program counter (PC).
     *
     * @return The optional operation reference.
     */
    @Nonnull
    @CheckReturnValue
    Optional<OperationReference> get(final int pc)
    {
        return (pc < _references.length)? Optional
            .of(_references[pc]): Optional.empty();
    }

    private static final long serialVersionUID = 1L;

    private OperationReference[] _references;
    private List<OperationReference> _referencesList = new LinkedList<>();
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
