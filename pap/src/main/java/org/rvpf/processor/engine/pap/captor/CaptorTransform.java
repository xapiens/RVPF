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

import java.io.Serializable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.processor.engine.rpn.Program;
import org.rvpf.processor.engine.rpn.Task;
import org.rvpf.processor.engine.rpn.selector.SelectedBehavior;
import org.rvpf.processor.engine.rpn.selector.SelectorTransform;
import org.rvpf.processor.engine.rpn.selector.SelectsBehavior;

/**
 * Captor transform.
 */
public final class CaptorTransform
    extends SelectorTransform
{
    /**
     * Constructs an instance.
     *
     * @param engine The captor engine
     */
    CaptorTransform(@Nonnull final CaptorEngine engine)
    {
        super(engine);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean executeFinalProgram(
            final Task task,
            final Optional<Program> finalProgram)
    {
        if (!super.executeFinalProgram(task, finalProgram)) {
            return false;
        }

        final Context context = (Context) task.getContext();

        for (final PointValue result: context.getResults()) {
            context.addUpdate(result);
        }

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean executeInitialProgram(
            final Task task,
            final Optional<Program> initialProgram)
    {
        final Context context = (Context) task.getContext();

        if (!context.getFirstStep().isPresent()) {
            return false;
        }

        return super.executeInitialProgram(task, initialProgram);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean executeStepProgram(
            final Task task,
            final Optional<Program> stepProgram)
    {
        final Context context = (Context) task.getContext();
        final PointValue currentStep = context.getCurrentStep().get();

        if (stepProgram.isPresent()) {
            final PointValue result = context.getResult().get();
            final Serializable savedState = result.getState();

            result.setStamp(currentStep.getStamp());
            result.setState(currentStep.getState());
            context.clearUpdated();

            if (!task.execute(stepProgram.get()).isPresent()) {
                return false;
            }

            if (context.isUpdated()) {
                context.addResult(result.copy());
            }

            result.setState(savedState);
        } else {
            context
                .addResult(
                    currentStep
                        .morph(context.getResultPoint(), Optional.empty()));
        }

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
        return Optional.of(Batch.DISABLED_UPDATE);
    }

    /** {@inheritDoc}
     */
    @Override
    protected Class<? extends SelectedBehavior> selectedBehaviorClass()
    {
        return CapturedBehavior.class;
    }

    /** {@inheritDoc}
     */
    @Override
    protected Class<? extends SelectsBehavior> selectsBehaviorClass()
    {
        return CapturesBehavior.class;
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

        /** {@inheritDoc}
         */
        @Override
        protected void update(final Serializable value)
        {
            super.update(value);

            _updated = true;
        }

        /**
         * Adds a result.
         *
         * @param result The result.
         */
        void addResult(@Nonnull final PointValue result)
        {
            _results.add(result);
        }

        /**
         * Clears the updated indicator.
         */
        void clearUpdated()
        {
            _updated = false;
        }

        /**
         * Gets the capture.
         *
         * @return The capture.
         */
        @Nonnull
        @CheckReturnValue
        Capture getCapture()
        {
            return (Capture) getSelection().get();
        }

        /**
         * Gets the results.
         *
         * @return The results.
         */
        @Nonnull
        @CheckReturnValue
        Collection<PointValue> getResults()
        {
            return _results;
        }

        /**
         * Asks if updated.
         *
         * @return True if updated.
         */
        @CheckReturnValue
        boolean isUpdated()
        {
            return _updated;
        }

        private final Collection<PointValue> _results = new LinkedList<>();
        private boolean _updated;
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
