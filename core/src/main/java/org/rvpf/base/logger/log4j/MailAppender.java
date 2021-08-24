/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: MailAppender.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.base.logger.log4j;

import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.net.SmtpManager;

/**
 * Mail appender.
 *
 * <p>Compatible with the Apache Log4j 2 SmtpAppender.</p>
 */
@Plugin(
    name = "Mail",
    category = "Core",
    elementType = "appender",
    printObject = true
)
public final class MailAppender
    extends AbstractAppender
{
    /**
     * Constructs an instance.
     *
     * @param name The name of the appender.
     * @param filter The filter.
     * @param policy A triggering policy or null.
     * @param layout The layout to use.
     * @param manager The SMTP manager.
     * @param ignoreExceptions True ignores exceptions.
     */
    private MailAppender(
            final String name,
            final Filter filter,
            final TriggeringPolicy policy,
            final Layout<? extends Serializable> layout,
            final SmtpManager manager,
            final boolean ignoreExceptions)
    {
        super(name, filter, layout, ignoreExceptions);

        _policy = policy;
        _manager = manager;
    }

    /**
     * Create a mail appender.
     *
     * @param name The name of the appender.
     * @param to The comma-separated list of recipient email addresses.
     * @param cc The comma-separated list of CC email addresses.
     * @param bcc The comma-separated list of BCC email addresses.
     * @param from The email address of the sender.
     * @param replyTo The comma-separated list of reply-to email addresses.
     * @param subject The subject of the email message.
     * @param smtpProtocol The SMTP transport protocol.
     * @param smtpHost The SMTP hostname to send to.
     * @param smtpPort The SMTP port to send to.
     * @param smtpUsername The SMTP username.
     * @param smtpPassword The SMTP password.
     * @param smtpDebug Enable mail session debuging on STDOUT.
     * @param filter The filter or null.
     * @param policy The triggering policy or null.
     * @param layout The layout or null.
     * @param bufferSize Log events buffer size.
     * @param ignoreExceptions True ignores exceptions.
     *
     * @return The mail appender.
     */
    @PluginFactory
    public static MailAppender createAppender(
    //J-
        @PluginAttribute("name")
        final String name,
        @PluginAttribute("to")
        final String to,
        @PluginAttribute("cc")
        final String cc,
        @PluginAttribute("bcc")
        final String bcc,
        @PluginAttribute("from")
        final String from,
        @PluginAttribute("replyTo")
        final String replyTo,
        @PluginAttribute("subject")
        final String subject,
        @PluginAttribute("smtpProtocol")
        final String smtpProtocol,
        @PluginAttribute("smtpHost")
        final String smtpHost,
        @PluginAttribute(value = "smtpPort", defaultInt = 0)
        final int smtpPort,
        @PluginAttribute("smtpUsername")
        final String smtpUsername,
        @PluginAttribute("smtpPassword")
        final String smtpPassword,
        @PluginAttribute(value = "smtpDebug", defaultBoolean = false)
        final boolean smtpDebug,
        @PluginElement("Filter")
        Filter filter,
        @PluginElement("Policy")
        final TriggeringPolicy policy,
        @PluginElement("Layout")
        Layout<? extends Serializable> layout,
        @PluginAttribute(value = "bufferSize", defaultInt = _DEFAULT_BUFFER_SIZE)
        final int bufferSize,
        @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true)
        final boolean ignoreExceptions)
    //J+
    {
        if (name == null) {
            LOGGER.error("No name provided for MailAppender");

            return null;
        }

        if (layout == null) {
            layout = HtmlLayout.createDefaultLayout();
        }

        if (filter == null) {
            if (policy == null) {
                filter = ThresholdFilter.createFilter(null, null, null);
            } else {
                filter = ThresholdFilter.createFilter(Level.DEBUG, null, null);
            }
        }

        final SmtpManager manager = SmtpManager.getSmtpManager(
            null,
            to,
            cc,
            bcc,
            from,
            replyTo,
            subject,
            smtpProtocol,
            smtpHost,
            smtpPort,
            smtpUsername,
            smtpPassword,
            smtpDebug,
            filter.toString(),
            bufferSize);

        if (manager == null) {
            return null;
        }

        return new MailAppender(
            name,
            filter,
            policy,
            layout,
            manager,
            ignoreExceptions);
    }

    /** {@inheritDoc}
     */
    @Override
    public void append(final LogEvent event)
    {
        if ((_policy == null) || _policy.isTriggeringEvent(event)) {
            _manager.sendEvents(getLayout(), event);
        } else {
            _manager.add(event);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    public boolean isFiltered(final LogEvent event)
    {
        final boolean filtered = super.isFiltered(event);

        if (filtered && (_policy == null)) {
            _manager.add(event);
        }

        return filtered;
    }

    private static final int _DEFAULT_BUFFER_SIZE = 512;

    private final SmtpManager _manager;
    private final TriggeringPolicy _policy;
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
