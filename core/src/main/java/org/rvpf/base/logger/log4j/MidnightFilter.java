/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MidnightFilter.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.logger.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import org.rvpf.base.logger.Logger;

/**
 * Midnight filter.
 */
@Plugin(
    name = "MidnightFilter",
    category = "Core",
    elementType = "filter",
    printObject = true
)
public final class MidnightFilter
    extends AbstractFilter
{
    /**
     * Constructs an instance.
     */
    private MidnightFilter() {}

    /**
     * Creates a midnight filter.
     *
     * @return A midnight filter.
     */
    @PluginFactory
    public static MidnightFilter createFilter()
    {
        return new MidnightFilter();
    }

    /** {@inheritDoc}
     */
    @Override
    public Result filter(final LogEvent event)
    {
        if (Logger.MIDNIGHT_LOGGER_NAME.equals(event.getLoggerName())) {
            return Result.ACCEPT;
        }

        return Level.WARN
            .isLessSpecificThan(event.getLevel())? Result.ACCEPT: Result.DENY;
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
