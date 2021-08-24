/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TriggeringPolicy.java 4109 2019-07-22 23:31:37Z SFB $
 */

package org.rvpf.base.logger.log4j;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;

import org.apache.logging.log4j.core.LogEvent;

/**
 * Triggering policy.
 *
 * <p>
 * A triggering policy determines if a log event should trigger an SMTP 'send'.
 * </p>
 */
public interface TriggeringPolicy
{
    /**
     * Asks if a log event is a triggering event.
     *
     * @param event The log event.
     *
     * @return True if the log event is a triggering event.
     */
    @CheckReturnValue
    boolean isTriggeringEvent(@Nonnull LogEvent event);
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
