/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MidnightTriggeringPolicy.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.logger.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import org.rvpf.base.DateTime;
import org.rvpf.base.logger.Logger;

/**
 * Midnight triggering policy.
 */
@Plugin(
    name = "MidnightTriggeringPolicy",
    category = "Core",
    printObject = true
)
public final class MidnightTriggeringPolicy
    implements TriggeringPolicy
{
    /**
     * Constructs an instance.
     */
    private MidnightTriggeringPolicy(final int maxWarnings)
    {
        _maxWarnings = maxWarnings;
    }

    /**
     * Creates a triggering policy.
     *
     * @param maxWarnings The maximum number ofwarnings before send.
     *
     * @return The new triggering policy.
     */
    @PluginFactory
    public static MidnightTriggeringPolicy createPolicy(@PluginAttribute(
        value = "maxWarnings",
        defaultInt = _MAX_WARNINGS_BEFORE_SEND
    ) final int maxWarnings)
    {
        return new MidnightTriggeringPolicy(maxWarnings);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event)
    {
        if (event.getLevel().isMoreSpecificThan(Level.ERROR)) {
            return _trigger();
        }

        if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
            if ((++_warnSeen > _maxWarnings) && (_confirmTime == null)) {
                return _trigger();
            }
        } else if ((_warnSeen > 0)
                   && Logger.MIDNIGHT_LOGGER_NAME.equals(
                           event.getLoggerName())) {
            return _trigger();
        } else if ((_confirmTime != null)
                   && _confirmTime.isNotAfter(DateTime.now())) {
            _confirmTime = null;

            return true;
        }

        return false;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "MidnightTriggeringPolicy";
    }

    private boolean _trigger()
    {
        _warnSeen = 0;
        _confirmTime = DateTime.now().nextDay();

        return true;
    }

    private static final int _MAX_WARNINGS_BEFORE_SEND = 99;

    private DateTime _confirmTime;
    private final int _maxWarnings;
    private int _warnSeen;
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
