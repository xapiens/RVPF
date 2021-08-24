/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Stack.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.io.Serializable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.logger.Message;
import org.rvpf.processor.ProcessorMessages;
import org.rvpf.processor.engine.rpn.operation.OperationReference;

/**
 * Stack.
 */
public final class Stack
{
    /**
     * Constructs a new stack.
     */
    public Stack()
    {
        this(Optional.empty());
    }

    /**
     * Constructs a new stack.
     *
     * @param task The stack's owner.
     */
    public Stack(@Nonnull final Optional<Task> task)
    {
        _task = task;
    }

    private Stack(final Optional<Task> task, final Stack mark)
    {
        this(task);

        _mark = mark;
        _task.get().markChangedStack(this);
    }

    /**
     * Clears this stack.
     */
    public void clear()
    {
        _stack.clear();
    }

    /**
     * Gets the task.
     *
     * @return The task.
     */
    @Nonnull
    @CheckReturnValue
    public Task getTask()
    {
        return _task.get();
    }

    /**
     * Inserts an object at a position on this stack.
     *
     * @param position The origin zero position.
     * @param object The object to insert.
     *
     * @throws LimitsException When the position is outside the stack.
     */
    public void insert(
            final int position,
            @Nullable final Serializable object)
        throws LimitsException
    {
        if ((_mark != null) && (position > _stack.size())) {
            _removeMark().insert(position, object);
        } else {
            try {
                _stack.add(position, object);
            } catch (final IndexOutOfBoundsException exception) {
                throw new LimitsException();
            }
        }
    }

    /**
     * Asks if this stack, as a mark, is dropped.
     *
     * @return True if this stack is dropped.
     */
    @CheckReturnValue
    public boolean isDropped()
    {
        return _stack == null;
    }

    /**
     * Asks if the stack is empty.
     *
     * @return True if the stack is empty.
     */
    @CheckReturnValue
    public boolean isEmpty()
    {
        return _stack.isEmpty();
    }

    /**
     * Asks if this stack is marked.
     *
     * @return True if this stack is marked.
     */
    @CheckReturnValue
    public boolean isMarked()
    {
        return _mark != null;
    }

    /**
     * Marks the stack.
     *
     * @return The mark.
     */
    @Nonnull
    @CheckReturnValue
    public Stack mark()
    {
        if (_task == null) {
            throw new UnsupportedOperationException(
                "Mark is not supported without owner");
        }

        return new Stack(_task, this);
    }

    /**
     * Peeks at the top of this stack.
     *
     * @return The object at the top.
     *
     * @throws LimitsException When the stack is empty.
     */
    @Nullable
    @CheckReturnValue
    public Serializable peek()
        throws LimitsException
    {
        final Serializable object;

        if ((_mark != null) && _stack.isEmpty()) {
            object = _removeMark().peek();
        } else {
            try {
                object = _stack.getFirst();
            } catch (final NoSuchElementException exception) {
                throw new LimitsException();
            }
        }

        return object;
    }

    /**
     * Peeks at the top of this stack.
     *
     * @param expectedClass The expected class.
     *
     * @return The object at the top.
     *
     * @throws LimitsException When the stack is empty.
     * @throws CastException When the value has not the expected class.
     */
    @Nonnull
    @CheckReturnValue
    public Serializable peek(
            @Nonnull final Class<?> expectedClass)
        throws LimitsException, CastException
    {
        final Serializable peeked = peek();

        if (!expectedClass.isInstance(peeked)) {
            throw new CastException(expectedClass, peeked);
        }

        return peeked;
    }

    /**
     * Peeks at a position on this stack.
     *
     * @param position The origin zero position.
     *
     * @return The object at the position.
     *
     * @throws LimitsException When the position is outside the stack.
     */
    @Nullable
    @CheckReturnValue
    public Serializable peek(final int position)
        throws LimitsException
    {
        final Serializable object;

        if ((_mark != null) && (position > _stack.size())) {
            object = _removeMark().peek(position);
        } else {
            try {
                object = _stack.get(position);
            } catch (final IndexOutOfBoundsException exception) {
                throw new LimitsException();
            }
        }

        return object;
    }

    /**
     * Peeks at the double at the top of this stack.
     *
     * @return The double at the top.
     *
     * @throws AccessException When appropriate.
     */
    @CheckReturnValue
    public double peekDoubleValue()
        throws AccessException
    {
        return ((Number) peek(Number.class)).doubleValue();
    }

    /**
     * Peeks at the long at the top of this stack.
     *
     * @return The long at the top.
     *
     * @throws LimitsException When the position is outside the stack.
     * @throws CastException When the value is not a number.
     */
    @CheckReturnValue
    public long peekLongValue()
        throws LimitsException, CastException
    {
        return ((Number) peek(Number.class)).longValue();
    }

    /**
     * Peeks at the string for the value at the top of this stack.
     *
     * @return The string for the value at the top of this stack.
     *
     * @throws LimitsException When the stack is empty.
     */
    @Nonnull
    @CheckReturnValue
    public String peekStringValue()
        throws LimitsException
    {
        try {
            return peek(Object.class).toString();
        } catch (final CastException exception) {
            throw new InternalError(exception);
        }
    }

    /**
     * Pops the object at the top of this stack.
     *
     * @return The object at the top.
     *
     * @throws LimitsException When the stack is empty.
     */
    @Nullable
    public Serializable pop()
        throws LimitsException
    {
        final Serializable object;

        if ((_mark != null) && _stack.isEmpty()) {
            object = _removeMark().pop();
        } else {
            try {
                object = _stack.removeFirst();
            } catch (final NoSuchElementException exception) {
                throw new LimitsException();
            }
        }

        return object;
    }

    /**
     * Pops the object at the top of this stack.
     *
     * @param expectedClass The expected class.
     *
     * @return The object at the top.
     * @throws LimitsException When the stack is empty.
     * @throws CastException When the value has not the expected class.
     */
    @Nullable
    @CheckReturnValue
    public Serializable pop(
            @Nonnull final Class<?> expectedClass)
        throws LimitsException, CastException
    {
        final Serializable popped = pop();

        if (!expectedClass.isInstance(popped)) {
            throw new CastException(expectedClass, popped);
        }

        return popped;
    }

    /**
     * Pops the boolean at the top of this stack.
     *
     * @return The boolean at the top.
     *
     * @throws LimitsException When the stack is empty.
     * @throws CastException When the value is not a boolean.
     */
    @CheckReturnValue
    public boolean popBooleanValue()
        throws LimitsException, CastException
    {
        return ((Boolean) pop(Boolean.class)).booleanValue();
    }

    /**
     * Pops the double at the top of this stack.
     *
     * @return The double at the top.
     *
     * @throws LimitsException When the stack is empty.
     * @throws CastException When the value is not a number.
     */
    @CheckReturnValue
    public double popDoubleValue()
        throws LimitsException, CastException
    {
        return ((Number) pop(Number.class)).doubleValue();
    }

    /**
     * Pops the int at the top of this stack.
     *
     * @return The int at the top.
     *
     * @throws LimitsException When the stack is empty.
     * @throws CastException When the value is not an number.
     */
    @CheckReturnValue
    public int popIntValue()
        throws LimitsException, CastException
    {
        return ((Number) pop(Number.class)).intValue();
    }

    /**
     * Pops the long at the top of this stack.
     *
     * @return The long at the top.
     *
     * @throws LimitsException When the stack is empty.
     * @throws CastException When the value is not a number.
     */
    @CheckReturnValue
    public long popLongValue()
        throws LimitsException, CastException
    {
        return ((Number) pop(Number.class)).longValue();
    }

    /**
     * Pops the string at the top of this stack.
     *
     * @return The string at the top.
     *
     * @throws LimitsException When the stack is empty.
     */
    @Nonnull
    @CheckReturnValue
    public String popStringValue()
        throws LimitsException
    {
        try {
            return pop(Object.class).toString();
        } catch (final CastException exception) {
            throw new InternalError(exception);
        }
    }

    /**
     * Pushes a boolean on this stack.
     *
     * @param value The boolean value.
     */
    public void push(final boolean value)
    {
        _stack.addFirst(Boolean.valueOf(value));
    }

    /**
     * Pushes a double on this stack.
     *
     * @param value The double value.
     */
    public void push(final double value)
    {
        _stack.addFirst(Double.valueOf(value));
    }

    /**
     * Pushes a long on this stack.
     *
     * @param value The long value.
     */
    public void push(final long value)
    {
        _stack.addFirst(Long.valueOf(value));
    }

    /**
     * Pushes an optional object on this stack (null when empty).
     *
     * @param object The optional object.
     */
    public void push(@Nonnull final Optional<? extends Serializable> object)
    {
        _stack.addFirst(object.orElse(null));
    }

    /**
     * Pushes an object on this stack.
     *
     * @param object The object.
     */
    public void push(@Nullable final Serializable object)
    {
        _stack.addFirst(object);
    }

    /**
     * Removes the object at a position on this stack.
     *
     * @param position The origin zero position.
     *
     * @return The removed object.
     *
     * @throws LimitsException When the position is outside the stack.
     */
    @Nullable
    @CheckReturnValue
    public Serializable remove(final int position)
        throws LimitsException
    {
        final Serializable object;

        if ((_mark != null) && (position > _stack.size())) {
            object = _removeMark().remove(position);
        } else {
            try {
                object = _stack.remove(position);
            } catch (final IndexOutOfBoundsException exception) {
                throw new LimitsException();
            }
        }

        return object;
    }

    /**
     * Reverses the content order of this stack.
     */
    public void reverse()
    {
        Collections.reverse(_stack);
    }

    /**
     * Returns the size of this stack.
     *
     * @return The size.
     */
    @CheckReturnValue
    public int size()
    {
        return _stack.size();
    }

    /**
     * Returns this stack's content in an object array.
     *
     * @return The content in an object array.
     */
    @Nonnull
    @CheckReturnValue
    public Object[] toArray()
    {
        return _stack.toArray();
    }

    /**
     * Returns all stacks content in an object array.
     *
     * @return The content in an object array.
     */
    @Nonnull
    @CheckReturnValue
    public Object[] toArrayAll()
    {
        return _addAll(new LinkedList<Object>()).toArray();
    }

    /**
     * Returns the total size of this stack and of the marks.
     *
     * @return The total size.
     */
    @CheckReturnValue
    public int totalSize()
    {
        int size = size();

        if (_mark != null) {
            size += _mark.totalSize();
        }

        return size;
    }

    /**
     * Unmarks the stack.
     */
    public void unmark()
    {
        if (_mark != null) {
            _removeMark();
        }
    }

    /**
     * Remove all marks.
     */
    public void unmarkAll()
    {
        if (_mark != null) {
            _removeMark().unmarkAll();
        }
    }

    private LinkedList<Object> _addAll(final LinkedList<Object> list)
    {
        list.addAll(_stack);

        if (_mark != null) {
            _mark._addAll(list);
        }

        return list;
    }

    private Stack _removeMark()
    {
        final Stack savedMark = _mark;

        while (!_stack.isEmpty()) {
            _mark.push(_stack.removeLast());
        }

        _task.get().markChangedStack(_mark);
        _task = null;
        _mark = null;
        _stack = null;    // Will now throw a NPE on any access.

        return savedMark;
    }

    private Stack _mark;
    private LinkedList<Serializable> _stack = new LinkedList<>();
    private Optional<Task> _task;

    /**
     * Access exception.
     */
    public abstract static class AccessException
        extends Exception
    {
        /**
         * Returns a message for an operation reference.
         *
         * @param reference The operation reference.
         *
         * @return The message.
         */
        abstract String getMessage(@Nonnull OperationReference reference);

        private static final long serialVersionUID = 1L;
    }


    /**
     * Cast exception.
     */
    public static final class CastException
        extends AccessException
    {
        /**
         * Constructs a cast exception.
         *
         * @param expectedClass The expected class.
         * @param objectFound The object found.
         */
        public CastException(
                @Nonnull final Class<?> expectedClass,
                @Nullable final Serializable objectFound)
        {
            _expectedClass = expectedClass;
            _objectFound = objectFound;
        }

        /** {@inheritDoc}
         */
        @Override
        String getMessage(final OperationReference reference)
        {
            return Message
                .format(
                    ProcessorMessages.STACK_CAST,
                    reference,
                    _expectedClass.getName(),
                    String
                        .valueOf(
                                (_objectFound != null)? _objectFound
                                        .getClass()
                                        .getName(): null));
        }

        private static final long serialVersionUID = 1L;

        private final Class<?> _expectedClass;
        private final Serializable _objectFound;
    }


    /**
     * Limits exception.
     */
    public static final class LimitsException
        extends AccessException
    {
        /** {@inheritDoc}
         */
        @Override
        String getMessage(final OperationReference reference)
        {
            return Message.format(ProcessorMessages.STACK_LIMITS, reference);
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
