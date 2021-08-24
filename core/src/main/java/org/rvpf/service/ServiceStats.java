/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ServiceStats.java 4080 2019-06-12 20:21:38Z SFB $
 */

package org.rvpf.service;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.Stats;
import org.rvpf.base.StatsOwner;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.Profiler;
import org.rvpf.base.tool.ValueConverter;

/**
 * Service stats.
 */
@ThreadSafe
public class ServiceStats
    extends Stats
{
    /**
     * Constructs an instance.
     *
     * @param statsOwner The stats owner.
     */
    public ServiceStats(@Nonnull final StatsOwner statsOwner)
    {
        _statsOwner = statsOwner;
    }

    /**
     * Gets the log enabled indicator.
     *
     * @return The log enabled indicator.
     */
    @CheckReturnValue
    public final boolean isLogEnabled()
    {
        return _logEnabled;
    }

    /**
     * Logs the stats.
     *
     * @param intermediate False means a final log.
     */
    public final synchronized void log(final boolean intermediate)
    {
        if (!isLogEnabled()) {
            return;
        }

        _statsOwner.updateStats();

        if (intermediate) {
            final ServiceStats stats = (ServiceStats) getIntermediate();

            stats._openLog(_INTERMEDIATE_MODE);
            stats.buildText();
            stats._closeLog(Logger.getMidnightLogger());
            setSnapshot(clone());
        } else {
            if (getSnapshot().isPresent()) {
                log(true);
                clearSnapshot();
                _openLog(_FINAL_MODE);
            } else {
                _openLog(_SERVICE_MODE);
            }

            buildText();
            _closeLog(Logger.getInstance(_statsOwner.getClass().getName()));
        }
    }

    /**
     * Sets the log enabled indicator.
     *
     * @param logEnabled The log enabled indicator.
     */
    public final void setLogEnabled(final boolean logEnabled)
    {
        _logEnabled = logEnabled;
    }

    /**
     * Notifies that this has been updated.
     */
    public final void updated()
    {
        if (_statsOwner != null) {
            _statsOwner.onStatsUpdated();
        }
    }

    /**
     * Freezes the stats.
     */
    protected void freeze() {}

    /**
     * Gets the stats owner.
     *
     * @return The stats owner.
     */
    @Nonnull
    @CheckReturnValue
    protected final StatsOwner getStatsOwner()
    {
        return _statsOwner;
    }

    private void _closeLog(final Logger logger)
    {
        logger.info(BaseMessages.VERBATIM, getText());
        clearText();
        clearMargin();
    }

    private void _openLog(final int mode)
    {
        setLogTime(DateTime.now());
        setMargin(Profiler.margin());

        final String name = _statsOwner.getObjectName().toString();
        final Optional<String> version = _statsOwner.getObjectVersion();

        addText(
            Message
                .format(
                    ServiceMessages.SERVICE_STATS,
                    Integer.valueOf(mode),
                    name,
                    ValueConverter.toInteger(version.isPresent()),
                    version.orElse(null),
                    getLogTime().orElse(null)));
    }

    private static final int _FINAL_MODE = 2;
    private static final int _INTERMEDIATE_MODE = 1;
    private static final int _SERVICE_MODE = 0;
    private static final long serialVersionUID = 1L;

    private transient volatile boolean _logEnabled;
    private final transient StatsOwner _statsOwner;
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
