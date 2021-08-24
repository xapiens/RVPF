/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: TriggerServlet.java 3980 2019-05-13 12:52:38Z SFB $
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
import org.rvpf.base.logger.Message;
import org.rvpf.base.util.SignalTarget;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.AbstractServlet;
import org.rvpf.http.HTTPMessages;
import org.rvpf.service.Service;

/**
 * Alerter trigger servlet.
 */
@ThreadSafe
public final class TriggerServlet
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
        final AlertContext context = _context;
        final String signalName = canonicalize(
            Optional.ofNullable(request.getParameter(SIGNAL_PARAMETER)))
            .orElse(null);
        String info = canonicalize(
            Optional.ofNullable(request.getParameter(INFO_PARAMETER)))
            .orElse(null);

        if (signalName == null) {
            getThisLogger()
                .warn(
                    HTTPMessages.MISSING_REQUEST_PARAMETER,
                    SIGNAL_PARAMETER,
                    request.getQueryString());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);

            return;
        }

        if (Service.PING_SIGNAL.equals(signalName)) {
            if (info == null) {
                final String uuidString = request.getParameter(UUID_PARAMETER);
                final String serviceName = request
                    .getParameter(SERVICE_PARAMETER);
                final UUID serviceUUID;

                if (uuidString != null) {
                    try {
                        serviceUUID = UUID.fromString(uuidString).orElse(null);
                    } catch (final NumberFormatException exception) {
                        getThisLogger().warn(HTTPMessages.BAD_UUID, uuidString);
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);

                        return;
                    }
                } else {
                    serviceUUID = null;
                }

                info = new SignalTarget(
                    Optional.ofNullable(serviceName),
                    Optional.ofNullable(serviceUUID),
                    Optional.empty())
                    .toString();
            }
        } else if (!context.useRestraint()) {
            getThisLogger().warn(HTTPMessages.RESTRAINT_REFUSED, signalName);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);

            return;
        }

        final Message message = new Message(
            HTTPMessages.QUEUED_SIGNAL,
            signalName,
            ((info != null)? (": " + info): ""));
        final boolean restartWasEnabled = context.setRestartEnabled(false);

        try {
            context.queueSignal(signalName, Optional.ofNullable(info));

            if (Service.PING_SIGNAL.equals(signalName)) {
                getThisLogger().debug(message);
            } else {
                getThisLogger().info(message);
            }

            final XMLDocument responseDocument = new XMLDocument(RESPONSE_ROOT);
            final XMLElement responseRoot = responseDocument.getRootElement();

            responseRoot
                .setAttribute(STAMP_ATTRIBUTE, DateTime.now().toString());
            sendGetResponse(request, response, responseDocument);
        } finally {
            context.setRestartEnabled(restartWasEnabled);
        }
    }

    /** Info parameter. */
    public static final String INFO_PARAMETER = "info";

    /** Service parameter. */
    public static final String SERVICE_PARAMETER = "service";

    /** Signal parameter. */
    public static final String SIGNAL_PARAMETER = "signal";

    /** UUID parameter. */
    public static final String UUID_PARAMETER = "uuid";
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
