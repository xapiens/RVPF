/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: Filter.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn;

import java.util.Iterator;
import java.util.LinkedList;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.State;

/**
 * Filter.
 *
 * <p>A filter is a small program to verify the content of a task stack. It
 * provides a set of methods to prepare the program and a single method (
 * {@link #accept accept}) to execute that program.</p>
 *
 * <p>The program preparation methods always return the filter instance to allow
 * chained calls.</p>
 */
public final class Filter
{
    public Filter() {}

    /**
     * Accepts or rejects the values on a Stack according to this Filter's
     * program.
     *
     * @param task The Task owning the Stack to verify.
     *
     * @return True if accepted.
     */
    @CheckReturnValue
    public boolean accept(@Nonnull final Task task)
    {
        _pc = _code.iterator();
        _stack.clear();
        _task = task;

        for (;;) {
            final Code code = (Code) _fetch();

            if (code == null) {
                break;
            }

            switch (code) {
                case AND: {
                    _doAnd();

                    break;
                }
                case IS: {
                    _doIs();

                    break;
                }
                case IS_APPLYING: {
                    _doIsApplying();

                    break;
                }
                case IS_LONG: {
                    _doIsLong();

                    break;
                }
                case IS_PRESENT: {
                    _doIsPresent();

                    break;
                }
                case OR: {
                    _doOr();

                    break;
                }
                default: {
                    Require.failure();
                }
            }
        }

        _pc = null;
        _task = null;

        return _pop();
    }

    /**
     * Adds an operation to 'and' both items on the top of the local stack.
     *
     * @return This Filter.
     */
    @Nonnull
    public Filter and()
    {
        _add(Code.AND);

        return this;
    }

    /**
     * Adds an operation to check if the object at an offset on the Task Stack
     * is of the specified class.
     *
     * @param at Offset on the Task Stack.
     * @param type The class.
     *
     * @return This Filter.
     */
    @Nonnull
    public Filter is(final int at, final Class<?> type)
    {
        _add(Code.IS);
        _add(Integer.valueOf(at));
        _add(type);

        return this;
    }

    /**
     * Adds an operation to check if the Task is currently applying operations
     * to a container.
     *
     * @param type The container class.
     *
     * @return This Filter.
     */
    @Nonnull
    public Filter isApplying(@Nonnull final Class<?> type)
    {
        _add(Code.IS_APPLYING);
        _add(type);

        return this;
    }

    /**
     * Adds an operation to check if the object at an offset on the Task Stack
     * can be used as a long.
     *
     * @param at Offset on the Task Stack.
     *
     * @return This Filter.
     */
    @Nonnull
    public Filter isLong(final int at)
    {
        _add(Code.IS_LONG);
        _add(Integer.valueOf(at));

        return this;
    }

    /**
     * Adds an operation to check if the object at an offset on the Task Stack
     * is present (a null value is considered present).
     *
     * @param at Offset on the Task Stack.
     *
     * @return This Filter.
     */
    @Nonnull
    public Filter isPresent(final int at)
    {
        _add(Code.IS_PRESENT);
        _add(Integer.valueOf(at));

        return this;
    }

    /**
     * Adds an operation to 'or' both items on the top of the local stack.
     *
     * @return This Filter.
     */
    @Nonnull
    public Filter or()
    {
        _add(Code.OR);

        return this;
    }

    private void _add(final Code code)
    {
        _code.add(code);
    }

    private void _add(final Object object)
    {
        _code.add(object);
    }

    private Object _at(final int index)
        throws Stack.LimitsException
    {
        return _task.getStack().peek(index);
    }

    private void _doAnd()
    {
        _push(_pop() & _pop());
    }

    private void _doIs()
    {
        final int index = ((Integer) _fetch()).intValue();
        final Class<?> type = (Class<?>) _fetch();

        try {
            _push(type.isInstance(_at(index)));
        } catch (final Stack.LimitsException exception) {
            _push(false);
        }
    }

    private void _doIsApplying()
    {
        final Class<?> type = (Class<?>) _fetch();

        _push(type.isInstance(_task.getContainer().orElse(null)));
    }

    private void _doIsLong()
    {
        final int index = ((Integer) _fetch()).intValue();

        try {
            final Object object = _at(index);

            _push(
                (object instanceof Long)
                || (object instanceof Integer)
                || (object instanceof Short)
                || (object instanceof Byte));
        } catch (final Stack.LimitsException exception) {
            _push(false);
        }
    }

    private void _doIsPresent()
    {
        final int index = ((Integer) _fetch()).intValue();

        try {
            _at(index);
            _push(true);
        } catch (final Stack.LimitsException exception) {
            _push(false);
        }
    }

    private void _doOr()
    {
        _push(_pop() | _pop());
    }

    private Object _fetch()
    {
        if (_pc.hasNext()) {
            return _pc.next();
        }

        return null;
    }

    private boolean _pop()
    {
        try {
            return ((Boolean) _stack.pop()).booleanValue();
        } catch (final Stack.LimitsException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void _push(final boolean verdict)
    {
        _stack.push(Boolean.valueOf(verdict));
    }

    /** Both must be Integers. */
    public static final Filter BOTH_LONG = new Filter()
        .isLong(0)
        .isLong(1)
        .and();

    /** Both must be Numbers. */
    public static final Filter BOTH_NUMBER = new Filter()
        .is(0, Number.class)
        .is(1, Number.class)
        .and();

    /** Both must be Objects. */
    public static final Filter BOTH_OBJECT = new Filter()
        .is(0, Object.class)
        .is(1, Object.class)
        .and();

    /** Both must be present. */
    public static final Filter BOTH_PRESENT = new Filter().isPresent(1);

    /** Both must be Strings. */
    public static final Filter BOTH_STRING = new Filter()
        .is(0, String.class)
        .is(1, String.class)
        .and();

    /** Top must be a Boolean. */
    public static final Filter TOP_BOOLEAN = new Filter().is(0, Boolean.class);

    /** Top must be an Integer. */
    public static final Filter TOP_LONG = new Filter().isLong(0);

    /** Top must be a Number. */
    public static final Filter TOP_NUMBER = new Filter().is(0, Number.class);

    /** Top must be an Object. */
    public static final Filter TOP_OBJECT = new Filter().is(0, Object.class);

    /** Top must be present. */
    public static final Filter TOP_PRESENT = new Filter().isPresent(0);

    /** Top must be a State. */
    public static final Filter TOP_STATE = new Filter().is(0, State.class);

    /** Top must be a String. */
    public static final Filter TOP_STRING = new Filter().is(0, String.class);

    private final LinkedList<Object> _code = new LinkedList<Object>();
    private Iterator<Object> _pc;
    private final Stack _stack = new Stack();
    private Task _task;

    private enum Code
    {
        AND,
        IS,
        IS_APPLYING,
        IS_LONG,
        IS_PRESENT,
        OR
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
