/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AlwaysTriggeringPolicy.java 4109 2019-07-22 23:31:37Z SFB $
 */

package org.rvpf.base.logger.log4j;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Always triggering policy.
 *
 * <p>Under this policy, every log event triggers an SMTP send.</p>
 */
@Plugin(
    name = "AlwaysTriggeringPolicy",
    category = "Core",
    printObject = true
)
public final class AlwaysTriggeringPolicy
    implements TriggeringPolicy
{
    /**
     * Constructs an instance.
     */
    private AlwaysTriggeringPolicy() {}

    /**
     * Creates a triggering policy.
     *
     * @return The new triggering policy.
     */
    @PluginFactory
    public static AlwaysTriggeringPolicy createPolicy()
    {
        return new AlwaysTriggeringPolicy();
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event)
    {
        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "AlwaysTriggeringPolicy";
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
