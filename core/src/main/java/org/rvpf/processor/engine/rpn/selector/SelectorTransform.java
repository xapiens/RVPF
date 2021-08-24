/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.processor.engine.rpn.selector;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.Point;
import org.rvpf.base.PointRelation;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.entity.PointInput;
import org.rvpf.metadata.entity.ProxyEntity;
import org.rvpf.metadata.entity.TransformEntity;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.Transform;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.AbstractTransform;
import org.rvpf.processor.engine.rpn.Compiler;
import org.rvpf.processor.engine.rpn.MacroDef;
import org.rvpf.processor.engine.rpn.Program;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Selector transform.
 */
public abstract class SelectorTransform
    extends AbstractTransform
{
    /**
     * Constructs an instance.
     *
     * @param engine The selector engine.
     */
    public SelectorTransform(@Nonnull final SelectorEngine engine)
    {
        _engine = Require.notNull(engine);
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<PointValue> applyTo(
            final ResultValue resultValue,
            final Batch batch)
    {
        final Context context = newContext(resultValue, batch);

        final Task task = new Task(context);

        if (!executeInitialProgram(
                task,
                Optional.ofNullable(_initialProgram))) {
            return Optional.empty();
        }

        while (context.nextStep()) {
            if (!executeStepProgram(task, Optional.ofNullable(_stepProgram))) {
                return Optional.empty();
            }
        }

        if (!executeFinalProgram(task, Optional.ofNullable(_finalProgram))) {
            return Optional.empty();
        }

        return returnResult(resultValue, context.getResult());
    }

    /** {@inheritDoc}
     */
    @Override
    public Optional<? extends Transform> getInstance(final Point point)
    {
        final List<? extends PointRelation> inputs = point.getInputs();

        if (inputs.size() < 2) {
            getThisLogger().error(ProcessorMessages.POINT_GE_2_INPUTS, point);

            return Optional.empty();
        }

        if (!selectsBehaviorClass()
            .isInstance(
                ((PointInput) inputs.get(0))
                    .getPrimaryBehavior()
                    .orElse(null))) {
            getThisLogger().error(ProcessorMessages.FIRST_INPUT_SELECTS, point);

            return Optional.empty();
        }

        if (!selectedBehaviorClass()
            .isInstance(
                ((PointInput) inputs.get(inputs.size() - 1))
                    .getPrimaryBehavior()
                    .orElse(null))) {
            getThisLogger().error(ProcessorMessages.LAST_INPUT_SELECTED, point);

            return Optional.empty();
        }

        return Optional.of(this);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean setUp(final Metadata metadata, final ProxyEntity proxyEntity)
    {
        if (!super.setUp(metadata, proxyEntity)) {
            return false;
        }

        final List<TransformEntity.Arg> args = ((TransformEntity) proxyEntity)
            .getArgs();

        if ((args.size() < 2) && !args.isEmpty()) {
            getThisLogger()
                .error(
                    ProcessorMessages.TRANSFORM_GE_2_ARGS,
                    proxyEntity.getName());

            return false;
        }

        final Optional<Map<String, MacroDef>> macroDefs;
        final Optional<Map<String, Program>> wordDefs;

        try {
            macroDefs = _engine.getMacroDefs(proxyEntity);
            wordDefs = _engine.getWordDefs(proxyEntity, macroDefs);
        } catch (final Compiler.CompileException exception) {
            getThisLogger()
                .error(BaseMessages.VERBATIM, exception.getMessage());

            return false;
        }

        final String initialProgramSource = getParams()
            .getString(INITIAL_PROGRAM_PARAM)
            .orElse(null);
        final String stepProgramSource = getParams()
            .getString(STEP_PROGRAM_PARAM)
            .orElse(null);
        final String finalProgramSource = getParams()
            .getString(FINAL_PROGRAM_PARAM)
            .orElse(null);

        if (initialProgramSource != null) {
            _initialProgram = _engine
                .compile(
                    initialProgramSource,
                    macroDefs,
                    wordDefs,
                    _getThisLogger());

            if (_initialProgram == null) {
                return false;
            }
        } else {
            _initialProgram = null;
        }

        if (stepProgramSource != null) {
            _stepProgram = _engine
                .compile(
                    stepProgramSource,
                    macroDefs,
                    wordDefs,
                    _getThisLogger());

            if (_stepProgram == null) {
                return false;
            }
        } else {
            _stepProgram = null;
        }

        if (finalProgramSource != null) {
            _finalProgram = _engine
                .compile(
                    finalProgramSource,
                    macroDefs,
                    wordDefs,
                    _getThisLogger());

            if (_finalProgram == null) {
                return false;
            }
        } else {
            _finalProgram = null;
        }

        _failReturnsNull = getParams()
            .getBoolean(Point.FAIL_RETURNS_NULL_PARAM);

        return true;
    }

    /**
     * Executes the final program.
     *
     * @param task The current task.
     * @param finalProgram The optional final program.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean executeFinalProgram(
            @Nonnull final Task task,
            @Nonnull final Optional<Program> finalProgram)
    {
        if (finalProgram.isPresent()) {
            if (!task.execute(finalProgram.get()).isPresent()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Executes the initial program.
     *
     * @param task The current task.
     * @param initialProgram The optional initial program.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean executeInitialProgram(
            @Nonnull final Task task,
            @Nonnull final Optional<Program> initialProgram)
    {
        if (initialProgram.isPresent()) {
            if (!task.execute(initialProgram.get()).isPresent()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Executes the step program.
     *
     * @param task The current task.
     * @param stepProgram The optional step program.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean executeStepProgram(
            @Nonnull final Task task,
            @Nonnull final Optional<Program> stepProgram)
    {
        if (stepProgram.isPresent()) {
            if (!task.execute(stepProgram.get()).isPresent()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a new context.
     *
     * @param resultValue The requested result value.
     * @param batch The current batch context.
     *
     * @return The new context.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Context newContext(
            @Nonnull ResultValue resultValue,
            @Nonnull Batch batch);

    /**
     * Returns a result.
     *
     * @param resultValue The result value.
     * @param pointValue The optional point value
     *
     * @return The optional resulting value.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Optional<PointValue> returnResult(
            @Nonnull ResultValue resultValue,
            @Nonnull Optional<PointValue> pointValue);

    /**
     * Returns the selected behavior class.
     *
     * @return The selected behavior class.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Class<? extends SelectedBehavior> selectedBehaviorClass();

    /**
     * Returns the selects behavior class.
     *
     * @return The selects behavior class.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract Class<? extends SelectsBehavior> selectsBehaviorClass();

    Logger _getThisLogger()
    {
        return getThisLogger();
    }

    /**
     * Gets the failReturnsNull indicator.
     *
     * @return True if fail returns null.
     */
    @CheckReturnValue
    boolean _isFailReturnsNull()
    {
        return _failReturnsNull;
    }

    private final SelectorEngine _engine;
    private boolean _failReturnsNull;
    private Program _finalProgram;
    private Program _initialProgram;
    private Program _stepProgram;

    /**
     * Context.
     */
    protected abstract class Context
        extends Task.Context
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
            super(
                resultValue,
                _isFailReturnsNull(),
                Optional.of(batch),
                Optional.empty(),
                _getThisLogger());

            final Point resultPoint = resultValue.getPoint().get();

            _offset = resultPoint.getInputs().size() - 1;
            _steps = super.getInputLimits()[1] - _offset;

            _selection = (resultValue instanceof Selection)? Optional
                .of((Selection) resultValue): Optional.empty();
        }

        /**
         * Gets the current step.
         *
         * @return The optional current step.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<? extends PointValue> getCurrentStep()
        {
            return getStep(_step);
        }

        /**
         * Gets the first step.
         *
         * @return The optional first step.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<? extends PointValue> getFirstStep()
        {
            return getStep(1);
        }

        /** {@inheritDoc}
         */
        @Override
        public int[] getInputLimits()
        {
            return new int[] {_offset, _offset + _steps};
        }

        /**
         * Gets the last step.
         *
         * @return The optional last step.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<? extends PointValue> getLastStep()
        {
            return getStep(_steps);
        }

        /**
         * Gets the next step.
         *
         * @return The optional next step.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<? extends PointValue> getNextStep()
        {
            return getStep(_step + 1);
        }

        /**
         * Gets the previous step.
         *
         * @return The optional previous step.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<? extends PointValue> getPreviousStep()
        {
            return getStep(_step - 1);
        }

        /**
         * Gets the start point value for the interval.
         *
         * @return The optional point value.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<? extends PointValue> getStart()
        {
            return _selection
                .isPresent()? _selection
                    .get()
                    .getStartValue(): Optional.empty();
        }

        /**
         * Gets the current step number in the iteration.
         *
         * <p>The step number is 0 in the initial program, 1 to the total number
         * of steps in the step program and 1 more in the final program.</p>
         *
         * @return The number of the current step.
         */
        @Nonnull
        @CheckReturnValue
        public Long getStepNumber()
        {
            return Long.valueOf(_step);
        }

        /**
         * Gets the stop point value for the interval.
         *
         * @return The optional point value.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<PointValue> getStop()
        {
            return _selection
                .isPresent()? _selection.get().getStopValue(): Optional.empty();
        }

        /**
         * Gets the selection.
         *
         * @return The optional selection.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<Selection> getSelection()
        {
            return _selection;
        }

        /**
         * Returns the next step.
         *
         * @return The next step.
         */
        @CheckReturnValue
        protected boolean nextStep()
        {
            return ++_step <= _steps;
        }

        private Optional<? extends PointValue> getStep(final int step)
        {
            if ((step < 1) || (step > _steps)) {
                return Optional.empty();
            }

            return getNormalizedInputValue((_offset + step) - 1);
        }

        private final int _offset;
        private Optional<Selection> _selection;
        private int _step;
        private final int _steps;
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
