/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.processor.engine.pap.captor;

import org.rvpf.base.tool.Require;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;
import org.rvpf.processor.engine.rpn.operation.Operation;
import org.rvpf.processor.engine.rpn.operation.SimpleOperation;
import org.rvpf.processor.engine.rpn.selector.SelectorOperations;

/**
 * Captor operations.
 */
public final class CaptorOperations
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

        final CaptorTransform.Context context = (CaptorTransform.Context) task
            .getContext();
        final Capture capture = context.getCapture();
        final Stack stack = task.getStack();

        switch ((_Code) referenceCode) {
            case LIMIT_AFTER: {
                _doLimitAfter(capture, stack);

                break;
            }
            case LIMIT_BEFORE: {
                _doLimitBefore(capture, stack);

                break;
            }
            case TIME_AFTER: {
                _doTimeAfter(capture, stack);

                break;
            }
            case TIME_BEFORE: {
                _doTimeBefore(capture, stack);

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

        register("limit>", _Code.LIMIT_AFTER);
        register("limit<", _Code.LIMIT_BEFORE);
        register("time>", _Code.TIME_AFTER);
        register("time<", _Code.TIME_BEFORE);
    }

    private static void _doLimitAfter(final Capture capture, final Stack stack)
    {
        stack.push(capture.getLimitAfter());
    }

    private static void _doLimitBefore(final Capture capture, final Stack stack)
    {
        stack.push(capture.getLimitBefore());
    }

    private static void _doTimeAfter(final Capture capture, final Stack stack)
    {
        stack.push(capture.getTimeAfter());
    }

    private static void _doTimeBefore(final Capture capture, final Stack stack)
    {
        stack.push(capture.getTimeBefore());
    }

    /**
     * Code.
     */
    private enum _Code
    {
        LIMIT_AFTER,
        LIMIT_BEFORE,
        TIME_AFTER,
        TIME_BEFORE;
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
