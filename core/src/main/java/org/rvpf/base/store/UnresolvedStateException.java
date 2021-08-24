/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UnresolvedStateException.java 4040 2019-05-31 18:55:08Z SFB $
 */

package org.rvpf.base.store;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Message;
import org.rvpf.base.value.State;

/**
 * Unresolved state exception.
 */
@Immutable
public final class UnresolvedStateException
    extends Exception
{
    /**
     * Constructs an instance.
     *
     * @param state The state.
     */
    public UnresolvedStateException(@Nonnull final State state)
    {
        super(Message.format(BaseMessages.UNRESOLVED_STATE, state));
    }

    /**
     * Constructs an instance.
     *
     * @param state The state.
     * @param pointString An optional point identification string.
     */
    public UnresolvedStateException(
            @Nonnull final State state,
            @Nonnull final String pointString)
    {
        super(
            Message.format(BaseMessages.UNRESOLVED_VALUE, state, pointString));
    }

    private static final long serialVersionUID = 1L;
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
