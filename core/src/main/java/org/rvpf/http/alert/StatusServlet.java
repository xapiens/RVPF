/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: StatusServlet.java 3961 2019-05-06 20:14:59Z SFB $
 */

package org.rvpf.http.alert;

import java.io.IOException;

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
 * Alerter status servlet.
 */
@ThreadSafe
public final class StatusServlet
    extends AbstractServlet
{
    /**
     * Initializes the servlet state.
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
        final XMLDocument responseDocument = new XMLDocument(STATUS_ROOT);
        final XMLElement responseRoot = responseDocument.getRootElement();

        responseRoot.setAttribute(STAMP_ATTRIBUTE, DateTime.now().toString());

        for (final Event event: _context.getServiceEvents()) {
            final XMLElement element = responseRoot.addChild(SERVICE_ELEMENT);
            final Optional<UUID> sourceUUID = event.getSourceUUID();

            element.setAttribute(NAME_ATTRIBUTE, event.getSourceServiceName());
            element.setAttribute(ENTITY_ATTRIBUTE, event.getSourceEntityName());

            if (sourceUUID.isPresent()) {
                element
                    .setAttribute(UUID_ATTRIBUTE, sourceUUID.get().toString());
            }

            element.setAttribute(EVENT_ATTRIBUTE, event.getName());
            element.setAttribute(STAMP_ATTRIBUTE, event.getStamp().toString());
        }

        sendGetResponse(request, response, responseDocument);
    }

    /** Entity attribute. */
    public static final String ENTITY_ATTRIBUTE = "entity";

    /** Event attribute. */
    public static final String EVENT_ATTRIBUTE = "event";

    /** Name attribute. */
    public static final String NAME_ATTRIBUTE = "name";

    /** Service element. */
    public static final String SERVICE_ELEMENT = "service";

    /** Root element. */
    public static final String STATUS_ROOT = "status";

    /** UUID attribute. */
    public static final String UUID_ATTRIBUTE = "uuid";

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
