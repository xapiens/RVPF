/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SummarizerOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.selector.summarizer;

import org.rvpf.base.tool.Require;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;
import org.rvpf.processor.engine.rpn.operation.Operation;
import org.rvpf.processor.engine.rpn.operation.SimpleOperation;
import org.rvpf.processor.engine.rpn.selector.SelectorOperations;

/**
 * Summarizer operations.
 */
public final class SummarizerOperations
    extends SelectorOperations
{
    /** {@inheritDoc}
     */
    @Override
    public void execute(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Task.ExecuteException, Stack.LimitsException
    {
        final Object referenceCode = reference.getCode();

        if (!(referenceCode instanceof _Code)) {
            super.execute(task, reference);

            return;
        }

        final SummarizerTransform.Context context =
            (SummarizerTransform.Context) task
                .getContext();
        final Summary summary = context.getSummary();
        final Stack stack = task.getStack();

        switch ((_Code) referenceCode) {
            case POSITION: {
                _doPosition(summary, stack);

                break;
            }
            case RUNNING_Q: {
                _doRunningQ(summary, stack);

                break;
            }
            default: {
                Require.failure();
            }
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected void setUp()
        throws Operation.OverloadException
    {
        super.setUp();

        register("position", _Code.POSITION);
        register("running?", _Code.RUNNING_Q);
    }

    private static void _doPosition(final Summary summary, final Stack stack)
    {
        stack.push(summary.getResultPosition());
    }

    private static void _doRunningQ(final Summary summary, final Stack stack)
    {
        stack.push(summary.isRunningInterval());
    }

    /**
     * Code.
     */
    private enum _Code
    {
        POSITION,
        RUNNING_Q;
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
