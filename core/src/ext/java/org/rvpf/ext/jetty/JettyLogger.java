/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: JettyLogger.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.ext.jetty;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.logger.Logger;

/**
 * Jetty logger.
 *
 * <p>Implements a custom logger for Jetty.</p>
 */
public final class JettyLogger
    implements org.eclipse.jetty.util.log.Logger
{
    /**
     * Constructs an instance.
     */
    public JettyLogger()
    {
        this("org.mortbay.log");
    }

    /**
     * Constructs an instance.
     *
     * @param name The logger name.
     */
    public JettyLogger(final String name)
    {
        _logger = Logger.getInstance(name);
    }

    /** {@inheritDoc}
     */
    @Override
    public void debug(final Throwable thrown)
    {
        debug("", thrown);
    }

    /** {@inheritDoc}
     */
    @Override
    public void debug(final String format, final Object... params)
    {
        _logger.debug(BaseMessages.VERBATIM, _format(format, params));
    }

    /** {@inheritDoc}
     */
    @Override
    public void debug(final String message, final Throwable thrown)
    {
        _logger.debug(thrown, BaseMessages.VERBATIM, message);
    }

    /** {@inheritDoc}
     */
    @Override
    public org.eclipse.jetty.util.log.Logger getLogger(final String name)
    {
        return new JettyLogger(name);
    }

    /** {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return _logger.getName();
    }

    /** {@inheritDoc}
     */
    @Override
    public void ignore(final Throwable thrown) {}

    /** {@inheritDoc}
     */
    @Override
    public void info(final Throwable thrown)
    {
        info("", thrown);
    }

    /** {@inheritDoc}
     */
    @Override
    public void info(final String format, final Object... params)
    {
        _logger.info(BaseMessages.VERBATIM, _format(format, params));
    }

    /** {@inheritDoc}
     */
    @Override
    public void info(final String message, final Throwable thrown)
    {
        _logger.info(thrown, BaseMessages.VERBATIM, message);
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isDebugEnabled()
    {
        return _logger.isDebugEnabled();
    }

    /** {@inheritDoc}
     */
    @Override
    public void setDebugEnabled(final boolean enabled) {}

    /** {@inheritDoc}
     */
    @Override
    public void warn(final Throwable thrown)
    {
        warn("", thrown);
    }

    /** {@inheritDoc}
     */
    @Override
    public void warn(final String format, final Object... params)
    {
        _logger.warn(BaseMessages.VERBATIM, _format(format, params));
    }

    /** {@inheritDoc}
     */
    @Override
    public void warn(final String message, final Throwable thrown)
    {
        _logger.warn(thrown, BaseMessages.VERBATIM, message);
    }

    private static String _format(String message, final Object... params)
    {
        final StringBuilder builder = new StringBuilder();
        int startIndex = 0;

        message = String.valueOf(message);    // Avoids NPE.

        for (final Object param: params) {
            final int markIndex = message.indexOf(_PARAM_MARK, startIndex);

            if (markIndex < 0) {
                builder.append(message.substring(startIndex));
                builder.append(" ");
                builder.append(param);
                startIndex = message.length();
            } else {
                builder.append(message.substring(startIndex, markIndex));
                builder.append(String.valueOf(param));
                startIndex = markIndex + _PARAM_MARK.length();
            }
        }

        builder.append(message.substring(startIndex));

        return builder.toString();
    }

    private static final String _PARAM_MARK = "{}";

    private final Logger _logger;
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
