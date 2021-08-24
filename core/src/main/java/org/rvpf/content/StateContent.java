/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StateContent.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.content;

import java.io.Serializable;

import java.util.Optional;

import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.State;

/**
 * State content converter.
 */
public final class StateContent
    extends AbstractContent
{
    /** {@inheritDoc}
     */
    @Override
    public State decode(final PointValue pointValue)
    {
        final Serializable value = pointValue.getValue();
        final State state;

        if (value instanceof State) {
            state = (State) value;
        } else if (value instanceof Number) {
            state = new State(
                Optional.of(Integer.valueOf(((Number) value).intValue())),
                Optional.empty());
        } else if (value instanceof String) {
            state = State.fromString((String) value);
        } else {
            if (value != null) {
                warnBadValue(pointValue);
            }

            state = null;
        }

        return state;
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
