/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Task.java 4036 2019-05-31 11:19:42Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.Point;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.logger.Messages;
import org.rvpf.base.tool.Require;
import org.rvpf.base.value.Container;
import org.rvpf.base.value.NormalizedValue;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.metadata.processor.Batch;
import org.rvpf.metadata.processor.BatchValuesQuery;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.operation.OperationReference;

/**
 * Executes a {@link Program} to compute a value from a result's inputs.
 */
public final class Task
{
    /**
     * Constructs an instance.
     *
     * @param context The context.
     */
    public Task(@Nonnull final Context context)
    {
        _context = context;
    }

    /**
     * Calls a program.
     *
     * @param program The program.
     *
     * @throws ExecuteException When appropriate.
     */
    public void call(@Nonnull final Program program)
        throws ExecuteException
    {
        final Program savedProgram = _program;
        final int savedPC = _pc;

        _program = Require.notNull(program);
        _pc = 0;

        for (;;) {
            final Optional<OperationReference> reference = nextReference();

            if (!reference.isPresent()) {
                break;
            }

            try {
                reference.get().execute(this);
            } catch (final Stack.AccessException exception) {
                _context.cancel();

                throw new ExecuteException(
                    BaseMessages.VERBATIM,
                    exception.getMessage(reference.get()));
            }
        }

        if (_program != null) {
            _program = savedProgram;
            _pc = savedPC;
        }
    }

    /**
     * Ends this task.
     */
    public void end()
    {
        _program = null;
    }

    /**
     * Executes the program to compute a result value.
     *
     * @param program The program.
     *
     * @return The result (empty if it should be dropped).
     */
    @Nonnull
    @CheckReturnValue
    public Optional<PointValue> execute(@Nonnull final Program program)
    {
        boolean failed = false;

        try {
            call(program);
        } catch (final ExecuteException exception) {
            failed = true;

            if (!getFailReturnsNull()) {
                _context.cancel();
            }

            if (exception.getMessage() != null) {
                _context
                    .getLogger()
                    .warn(BaseMessages.VERBATIM, exception.getMessage());
            }
        }

        if (!failed) {
            while (getStack().isEmpty() && getStack().isMarked()) {
                getStack().unmark();
            }

            if (getStack().size() == 1) {
                try {
                    _context.update(getStack().pop());
                } catch (final Stack.AccessException exception) {
                    throw new InternalError(exception);    // Should not happen.
                }
            } else if (getStack().size() != 0) {
                _context.cancel();
                _context
                    .getLogger()
                    .warn(
                        ProcessorMessages.VALUES_ON_STACK,
                        _context.getResultPoint(),
                        String.valueOf(getStack().size()));
            }
        }

        return _context.getResult();
    }

    /**
     * Gets the container.
     *
     * @return The optional container.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Container> getContainer()
    {
        return Optional.ofNullable(_container);
    }

    /**
     * Gets the context.
     *
     * @return The context.
     */
    @Nonnull
    @CheckReturnValue
    public Context getContext()
    {
        return _context;
    }

    /**
     * Gets the 'FailReturnsNull' indicator.
     *
     * @return The 'FailReturnsNull' indicator.
     */
    @CheckReturnValue
    public boolean getFailReturnsNull()
    {
        return _context
            .getResultPoint()
            .get()
            .getParams()
            .getBoolean(
                Point.FAIL_RETURNS_NULL_PARAM,
                _context.getFailReturnsNull());
    }

    /**
     * Gets a numbered input.
     *
     * @param index An origin one index identifying the input.
     *
     * @return The requested input or empty.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<PointValue> getInput(@Nonnull final Integer index)
    {
        return _context.getInputValue(index.intValue() - 1);
    }

    /**
     * Gets the input count.
     *
     * @return The input count.
     */
    @Nonnull
    @CheckReturnValue
    public Integer getInputCount()
    {
        final int[] limits = _context.getInputLimits();

        return Integer.valueOf(limits[1] - limits[0]);
    }

    /**
     * Get the input values.
     *
     * @return The requested normalized values.
     */
    @Nonnull
    @CheckReturnValue
    public Serializable[] getInputs()
    {
        final int[] limits = _context.getInputLimits();
        final Serializable[] inputValues =
            new Serializable[limits[1] - limits[0]];

        for (int i = 0; i < inputValues.length; ++i) {
            final Optional<? extends PointValue> input = getNormalizedInput(
                Integer.valueOf(limits[0] + i + 1));

            inputValues[i] = input.get().getValue();
        }

        return inputValues;
    }

    /**
     * Gets a numbered memory value.
     *
     * @param index An origin one index identifying the memory value.
     *
     * @return The requested value or null.
     */
    @Nullable
    @CheckReturnValue
    public Serializable getMemory(@Nonnull final Integer index)
    {
        return _context.getMemoryValue(index.intValue() - 1);
    }

    /**
     * Gets a normalized numbered input.
     *
     * @param index An origin one index identifying the input.
     *
     * @return The requested normalized input or empty.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<? extends PointValue> getNormalizedInput(
            @Nonnull final Integer index)
    {
        return _context.getNormalizedInputValue(index.intValue() - 1);
    }

    /**
     * Gets a numbered parameter value.
     *
     * @param index An origin one index identifying the parameter value.
     *
     * @return The requested value or empty.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<String> getParam(@Nonnull final Integer index)
    {
        return _context.getParamValue(index.intValue() - 1);
    }

    /**
     * Gets the result point.
     *
     * @return The optional result point.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<Point> getResultPoint()
    {
        return _context.getResultPoint();
    }

    /**
     * Gets the task's stack.
     *
     * @return The stack.
     */
    @Nonnull
    @CheckReturnValue
    public Stack getStack()
    {
        if (_stack == null) {
            _stack = new Stack(Optional.of(this));
        }

        return _stack;
    }

    /**
     * Informs that the mark has changed.
     *
     * @param stack The new mark.
     */
    public void markChangedStack(@Nonnull final Stack stack)
    {
        _stack = stack;
    }

    /**
     * Returns the next operation reference from code.
     *
     * <p>The virtual program counter is advanced.</p>
     *
     * @return The next operation reference or empty.
     */
    @Nonnull
    @CheckReturnValue
    public Optional<OperationReference> nextReference()
    {
        final Optional<OperationReference> reference = (_program != null)
            ? _program
                .get(_pc): Optional.empty();

        if (reference.isPresent()) {
            ++_pc;
        }

        return reference;
    }

    /**
     * Sets the container.
     *
     * @param container The optional container.
     */
    public void setContainer(@Nonnull final Optional<Container> container)
    {
        _container = container.orElse(null);
    }

    /**
     * Sets a numbered input value.
     *
     * @param index An origin one index identifying the input value.
     * @param value A serializable object to use as the value.
     */
    public void setInput(
            @Nonnull final Integer index,
            @Nullable final Serializable value)
    {
        Optional<NormalizedValue> pointValue = _context
            .getNormalizedInputValue(index.intValue() - 1);

        if (pointValue.isPresent()) {
            pointValue.get().setValue(value);
        } else {
            pointValue = Optional.of(new NormalizedValue());
            pointValue.get().setValue(value);
            _context.setInputValue(index.intValue() - 1, pointValue.get());
        }
    }

    /**
     * Sets a numbered memory value.
     *
     * @param index An origin one index identifying the memory value.
     * @param value A serializable object to use as the value.
     */
    public void setMemory(
            @Nonnull final Integer index,
            @Nullable final Serializable value)
    {
        _context.setMemoryValue(index.intValue() - 1, value);
    }

    private Container _container;
    private final Context _context;
    private int _pc;
    private Program _program;
    private Stack _stack;

    /**
     * Task context.
     */
    public static class Context
    {
        /**
         * Constructs an instance.
         *
         * @param resultValue The requested result value.
         * @param failReturnsNull The FailReturnsNull indicator.
         * @param batch The current Batch context (optional).
         * @param timeZone The time zone (optional).
         * @param logger The logger.
         */
        protected Context(
                @Nonnull final ResultValue resultValue,
                final boolean failReturnsNull,
                @Nonnull final Optional<Batch> batch,
                @Nonnull final Optional<TimeZone> timeZone,
                @Nonnull final Logger logger)
        {
            final List<PointValue> resultInputValues = resultValue
                .getInputValues();

            _result = resultValue;
            _failReturnsNull = failReturnsNull;
            _resultPoint = resultValue.getPoint().get();
            _inputValues = resultInputValues
                .toArray(new PointValue[resultInputValues.size()]);
            _batch = batch;
            setTimeZone(timeZone);
            _logger = logger;
            _params = null;
        }

        /**
         * Adds an update.
         *
         * @param update The update.
         */
        public void addUpdate(@Nonnull final PointValue update)
        {
            _batch.get().addUpdate(update);
        }

        /**
         * Gets the DateTime context.
         *
         * @return The DateTime context.
         */
        @Nonnull
        @CheckReturnValue
        public DateTime.Context getDateTimeContext()
        {
            if (_dateTimeContext == null) {
                _dateTimeContext = new DateTime.Context(
                    (_timeZone != null)? _timeZone: DateTime.getTimeZone());
            }

            return _dateTimeContext;
        }

        /**
         * Gets input limits.
         *
         * @return The input limits.
         */
        @Nonnull
        @CheckReturnValue
        public int[] getInputLimits()
        {
            return new int[] {0, _inputValues.length};
        }

        /**
         * Gets the logger.
         *
         * @return The logger.
         */
        @Nonnull
        @CheckReturnValue
        public Logger getLogger()
        {
            return _logger;
        }

        /**
         * Gets a memory value.
         *
         * @param index The origin 0 index of the value.
         *
         * @return The requested value.
         */
        @Nullable
        @CheckReturnValue
        public Serializable getMemoryValue(final int index)
        {
            if ((_memoryValues == null) || (_memoryValues.size() <= index)) {
                return null;
            }

            return _memoryValues.get(index);
        }

        /**
         * Gets a normalized input value.
         *
         * @param index The origin 0 index of the value.
         *
         * @return The requested PointValue or empty.
         */
        @SuppressWarnings("unchecked")
        @Nonnull
        @CheckReturnValue
        public Optional<NormalizedValue> getNormalizedInputValue(
                final int index)
        {
            Optional<? extends PointValue> inputValue = getInputValue(index);

            if (inputValue.isPresent()) {
                inputValue = Optional.of(inputValue.get().normalized());
                _inputValues[index] = inputValue.get();
            }

            return (Optional<NormalizedValue>) inputValue;
        }

        /**
         * Gets the result value.
         *
         * @return The current value of the result.
         */
        @Nonnull
        @CheckReturnValue
        public PointValue getNormalizedResultValue()
        {
            _result = _result.normalized();

            return _result;
        }

        /**
         * Gets a param value.
         *
         * @param index The origin 0 index of the value.
         *
         * @return The requested value (may be empty).
         */
        @Nonnull
        @CheckReturnValue
        public Optional<String> getParamValue(final int index)
        {
            if (_params == null) {
                _params = _resultPoint
                    .getParams()
                    .getStrings(Point.PARAM_PARAM);
            }

            if (index >= _params.length) {
                return Optional.empty();
            }

            return Optional.ofNullable(_params[index]);
        }

        /**
         * Gets the result.
         *
         * @return The optional current result.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<PointValue> getResult()
        {
            return Optional.ofNullable(_result);
        }

        /**
         * Gets the result point.
         *
         * @return The optional result point.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<Point> getResultPoint()
        {
            return Optional.ofNullable(_resultPoint);
        }

        /**
         * Gets the stored value.
         *
         * @return The optional stored value.
         */
        @Nonnull
        @CheckReturnValue
        public Optional<PointValue> getStoredValue()
        {
            if (!_batch.isPresent() || (_result == null)) {
                return Optional.empty();
            }

            final BatchValuesQuery.Builder batchValuesQueryBuilder =
                BatchValuesQuery
                    .newBuilder()
                    .setPoint(_result.getPoint());

            batchValuesQueryBuilder.setAt(_result.getStamp());

            return Optional
                .of(
                    _batch
                        .get()
                        .getPointValue(batchValuesQueryBuilder.build()));
        }

        /**
         * Sets an input value.
         *
         * @param index The origin 0 index of the value.
         * @param inputValue The new input value.
         */
        public void setInputValue(
                final int index,
                @Nonnull final NormalizedValue inputValue)
        {
            _inputValues[index] = inputValue;
        }

        /**
         * Sets a memory value.
         *
         * @param index The origin 0 index of the value.
         * @param value The new value.
         */
        public void setMemoryValue(
                final int index,
                @Nullable final Serializable value)
        {
            if (_memoryValues == null) {
                _memoryValues = new ArrayList<Serializable>();
            }

            while (_memoryValues.size() <= index) {
                _memoryValues.add(null);
            }

            _memoryValues.set(index, value);
        }

        /**
         * Sets the time zone.
         *
         * @param timeZone The optional time zone.
         */
        public void setTimeZone(@Nonnull final Optional<TimeZone> timeZone)
        {
            _timeZone = timeZone
                .isPresent()? timeZone.get(): TimeZone.getDefault();
            _dateTimeContext = null;
        }

        /**
         * Cancels the processing.
         */
        protected void cancel()
        {
            _result = null;
        }

        /**
         * Gets the FailReturnsNull indicator.
         *
         * @return The FailReturnsNull indicator.
         */
        @CheckReturnValue
        protected boolean getFailReturnsNull()
        {
            return _failReturnsNull;
        }

        /**
         * Gets a numbered input.
         *
         * @param index An origin zero index identifying the input.
         *
         * @return The requested input or empty.
         */
        @Nonnull
        @CheckReturnValue
        protected Optional<PointValue> getInputValue(final int index)
        {
            return Optional
                .ofNullable(
                    (index < _inputValues.length)? _inputValues[index]: null);
        }

        /**
         * Updates the result.
         *
         * @param value The new value for the result.
         */
        protected void update(@Nullable final Serializable value)
        {
            getNormalizedResultValue().setValue(value);
        }

        private final Optional<Batch> _batch;
        private DateTime.Context _dateTimeContext;
        private final boolean _failReturnsNull;
        private final PointValue[] _inputValues;
        private final Logger _logger;
        private List<Serializable> _memoryValues;
        private String[] _params;
        private PointValue _result;
        private final Point _resultPoint;
        private TimeZone _timeZone;
    }


    /**
     * Execute Exception.
     */
    public static class ExecuteException
        extends Exception
    {
        /**
         * Constructs an instance.
         */
        public ExecuteException() {}

        /**
         * Constructs a instance.
         *
         * @param entry The messages entry.
         * @param params The message parameters.
         */
        public ExecuteException(
                @Nonnull final Messages.Entry entry,
                @Nonnull final Object... params)
        {
            super(Message.format(entry, params));
        }

        private static final long serialVersionUID = 1L;
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
