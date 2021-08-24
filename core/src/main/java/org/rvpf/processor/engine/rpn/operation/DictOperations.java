/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DictOperations.java 4095 2019-06-24 17:44:43Z SFB $
 */

package org.rvpf.processor.engine.rpn.operation;

import java.io.Serializable;

import java.util.Map;

import org.rvpf.base.tool.Require;
import org.rvpf.base.value.Dict;
import org.rvpf.processor.engine.rpn.Filter;
import org.rvpf.processor.engine.rpn.Stack;
import org.rvpf.processor.engine.rpn.Task;

/**
 * Dict operations.
 */
class DictOperations
    extends SimpleOperations
{
    /** {@inheritDoc}
     */
    @Override
    public void execute(
            final Task task,
            final SimpleOperation.Reference reference)
        throws Task.ExecuteException, Stack.AccessException
    {
        final Dict container = (Dict) task.getContainer().get();

        switch ((_Code) reference.getCode()) {
            case ENTRIES: {
                _doEntries(task.getStack(), container);

                break;
            }
            case GET: {
                _doGet(task.getStack(), container);

                break;
            }
            case KEYS: {
                _doKeys(task.getStack(), container);

                break;
            }
            case PUT: {
                _doPut(task.getStack(), container);

                break;
            }
            case REMOVE: {
                _doRemove(task.getStack(), container);

                break;
            }
            case SIZE: {
                _doSize(task.getStack(), container);

                break;
            }
            case VALUES: {
                _doValues(task.getStack(), container);

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
        register("entries", _Code.ENTRIES, _APPLYING_DICT);
        register("get", _Code.GET, _APPLYING_DICT_TOP_STRING);
        register("keys", _Code.KEYS, _APPLYING_DICT);
        register("put", _Code.PUT, _APPLYING_DICT_TOP_STRING_PRESENT);
        register("remove", _Code.REMOVE, _APPLYING_DICT_TOP_STRING);
        register("size", _Code.SIZE, _APPLYING_DICT);
        register("values", _Code.VALUES, _APPLYING_DICT);
    }

    private static void _doEntries(final Stack stack, final Dict container)
    {
        for (final Map.Entry<String, Serializable> entry:
                container.entrySet()) {
            stack.push(entry.getValue());
            stack.push(entry.getKey());
        }
    }

    private static void _doGet(
            final Stack stack,
            final Dict container)
        throws Stack.AccessException
    {
        stack.push(container.get(stack.popStringValue()));
    }

    private static void _doKeys(final Stack stack, final Dict container)
    {
        for (final Serializable key: container.keySet()) {
            stack.push(key);
        }
    }

    private static void _doPut(
            final Stack stack,
            final Dict container)
        throws Stack.AccessException
    {
        final String key = stack.popStringValue();

        container.put(key, stack.pop());
    }

    private static void _doRemove(
            final Stack stack,
            final Dict container)
        throws Stack.AccessException
    {
        stack.push(container.remove(stack.popStringValue()));
    }

    private static void _doSize(final Stack stack, final Dict container)
    {
        stack.push(Integer.valueOf(container.size()));
    }

    private static void _doValues(final Stack stack, final Dict container)
    {
        for (final Serializable value: container.values()) {
            stack.push(value);
        }
    }

    private static final Filter _APPLYING_DICT = new Filter()
        .isApplying(Dict.class);
    private static final Filter _APPLYING_DICT_TOP_STRING = new Filter()
        .isApplying(Dict.class)
        .is(0, String.class)
        .and();
    private static final Filter _APPLYING_DICT_TOP_STRING_PRESENT = new Filter()
        .isApplying(Dict.class)
        .is(0, String.class)
        .and()
        .isPresent(1)
        .and();

    /**
     * Code.
     */
    private enum _Code
    {
        ENTRIES,
        GET,
        KEYS,
        PUT,
        REMOVE,
        SIZE,
        VALUES
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
