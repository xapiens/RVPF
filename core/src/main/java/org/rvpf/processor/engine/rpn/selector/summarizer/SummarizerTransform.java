/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: SummarizerTransform.java 4032 2019-05-27 20:50:32Z SFB $
 */

package org.rvpf.processor.engine.rpn.selector.summarizer;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.processor.engine.rpn.RPNExecutor;
import org.rvpf.processor.engine.rpn.selector.SelectedBehavior;
import org.rvpf.processor.engine.rpn.selector.SelectorTransform;
import org.rvpf.processor.engine.rpn.selector.SelectsBehavior;

/**
 * Summarizer transform.
 */
public final class SummarizerTransform
    extends SelectorTransform
{
    /**
     * Constructs an instance.
     *
     * @param engine The summarizer engine.
     */
    SummarizerTransform(final SummarizerEngine engine)
    {
        super(engine);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean usesFetchedResult()
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Context newContext(
            final ResultValue resultValue,
            final Batch batch)
    {
        return new Context(resultValue, batch);
    }

    /** {@inheritDoc}
     */
    @Override
    protected Optional<PointValue> returnResult(
            final ResultValue resultValue,
            final Optional<PointValue> pointValue)
    {
        return RPNExecutor.returnResult(resultValue, pointValue);
    }

    /** {@inheritDoc}
     */
    @Override
    protected Class<? extends SelectedBehavior> selectedBehaviorClass()
    {
        return SummarizedBehavior.class;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Class<? extends SelectsBehavior> selectsBehaviorClass()
    {
        return SummarizesBehavior.class;
    }

    /**
     * Context.
     */
    class Context
        extends SelectorTransform.Context
    {
        /**
         * Constructs an instance.
         *
         * @param resultValue The requested result value.
         * @param batch The current batch context.
         */
        public Context(
                @Nonnull final ResultValue resultValue,
                @Nonnull final Batch batch)
        {
            super(resultValue, batch);
        }

        /**
         * Gets the summary.
         *
         * @return The summary.
         */
        @Nonnull
        @CheckReturnValue
        Summary getSummary()
        {
            return (Summary) getSelection().get();
        }
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
