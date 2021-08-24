/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: EventsServlet.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http.alert;

import java.io.IOException;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.concurrent.ThreadSafe;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.alert.Event;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.AbstractServlet;

/**
 * Alerter events servlet.
 */
@ThreadSafe
public final class EventsServlet
    extends AbstractServlet
{
    /**
     * Initializes the Servlet state.
     *
     * @throws ServletException When approriate.
     */
    @Override
    public void init()
        throws ServletException
    {
        _context = (AlertContext) getServletContext()
            .getAttribute(AlertModule.ALERT_CONTEXT_ATTRIBUTE);
        setLogID(_context.getLogID());
    }

    /**
     * Does GET processing.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     *
     * @throws IOException When appropriate.
     */
    @Override
    protected void doGet(
            final HttpServletRequest request,
            final HttpServletResponse response)
        throws IOException
    {
        final AlertContext context = _context;
        final XMLDocument responseDocument = new XMLDocument(EVENTS_ROOT);
        final XMLElement responseRoot = responseDocument.getRootElement();
        final String afterValue = request.getParameter(AFTER_PARAMETER);
        final String waitValue = request.getParameter(WAIT_PARAMETER);
        final Collection<Event> events;

        responseRoot
            .setAttribute(STAMP_ATTRIBUTE, context.getStamp().toString());

        if (afterValue != null) {
            if (AFTER_LAST_VALUE.equalsIgnoreCase(afterValue.trim())) {
                events = Collections.emptyList();
            } else {
                final DateTime after;
                final long timeout;

                try {
                    after = DateTime.now().valueOf(afterValue);
                    timeout = (waitValue != null)? (Long
                        .parseLong(waitValue) * 1000): 0;
                } catch (final IllegalArgumentException exception) {
                    response
                        .sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            exception.getMessage());

                    return;
                }

                if (timeout > 0) {
                    context.waitForEvent(after, timeout);
                }

                events = context.getEvents(after);
            }
        } else {
            events = context.getEvents();
        }

        for (final Event event: events) {
            final XMLElement element = responseRoot.addChild(EVENT_ELEMENT);

            element.setAttribute(NAME_ATTRIBUTE, event.getName());
            element.setAttribute(STAMP_ATTRIBUTE, event.getStamp().toString());
            element
                .setAttribute(SERVICE_ATTRIBUTE, event.getSourceServiceName());

            final Optional<UUID> sourceUUID = event.getSourceUUID();

            if (sourceUUID.isPresent()) {
                element
                    .setAttribute(UUID_ATTRIBUTE, sourceUUID.get().toString());
            }

            element.setAttribute(ENTITY_ATTRIBUTE, event.getSourceEntityName());

            final Optional<String> info = event.getInfo();

            if (info != null) {
                element.setAttribute(INFO_ATTRIBUTE, info.toString());
            }
        }

        sendGetResponse(request, response, responseDocument);
    }

    /** After last. */
    public static final String AFTER_LAST_VALUE = "last";

    /** After parameter. */
    public static final String AFTER_PARAMETER = "after";

    /** Entity attribute. */
    public static final String ENTITY_ATTRIBUTE = "entity";

    /** Root element. */
    public static final String EVENTS_ROOT = "events";

    /** Event element. */
    public static final String EVENT_ELEMENT = "event";

    /** Info attribute. */
    public static final String INFO_ATTRIBUTE = "info";

    /** Name attribute. */
    public static final String NAME_ATTRIBUTE = "name";

    /** Service attribute. */
    public static final String SERVICE_ATTRIBUTE = "service";

    /** UUID attribute. */
    public static final String UUID_ATTRIBUTE = "uuid";

    /** Wait parameter. */
    public static final String WAIT_PARAMETER = "wait";

    /**  */

    private static final long serialVersionUID = 1L;

    private transient volatile AlertContext _context;
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
