/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id$
 */

package org.rvpf.http.update;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.AbstractServlet;
import org.rvpf.http.ServiceSessionException;

/**
 * Purge servlet.
 */
@ThreadSafe
public final class PurgeServlet
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
        _context = (PurgeContext) getServletContext()
            .getAttribute(PurgeModule.PURGE_CONTEXT_ATTRIBUTE);
        setLogID(_context.getLogID());
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doGet(
            final HttpServletRequest request,
            final HttpServletResponse response)
        throws IOException
    {
        final String point = canonicalize(
            Optional.ofNullable(request.getParameter(POINT_ATTRIBUTE)))
            .orElse(null);
        final Optional<Exception[]> responses;

        response.setHeader("Cache-Control", "no-cache");

        try {
            final String action = PURGE_ELEMENT;
            final PointValue update = _createPurge(action, point);

            responses = _context
                .purge(
                    new PointValue[] {update},
                    Optional.ofNullable(request.getUserPrincipal()));

            if (!responses.isPresent()) {
                throw new ServiceSessionException(new ServiceClosedException());
            }
        } catch (final BadRequestException exception) {
            response
                .sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    exception.getMessage());

            return;
        } catch (final ServiceSessionException exception) {
            response
                .sendError(exception.getStatusCode(), exception.getMessage());

            return;
        }

        sendGetResponse(
            request,
            response,
            _createResponsesDocument(responses.get(), response));
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doPost(
            final HttpServletRequest request,
            final HttpServletResponse response)
        throws IOException
    {
        final XMLDocument requestDocument = parseRequest(request, response);

        if (requestDocument == null) {
            return;
        }

        final Optional<Exception[]> responses;

        try {
            responses = _context
                .purge(
                    _createPurges(requestDocument),
                    Optional.ofNullable(request.getUserPrincipal()));

            if (!responses.isPresent()) {
                throw new ServiceSessionException(new ServiceClosedException());
            }
        } catch (final BadRequestException exception) {
            response
                .sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    exception.getMessage());

            return;
        } catch (final ServiceSessionException exception) {
            response
                .sendError(exception.getStatusCode(), exception.getMessage());

            return;
        }

        sendPostResponse(
            request,
            response,
            _createResponsesDocument(responses.get(), response));
    }

    private static XMLDocument _createResponsesDocument(
            @Nonnull final Exception[] responses,
            @Nonnull final HttpServletResponse response)
    {
        final XMLDocument responsesDocument = new XMLDocument(RESPONSES_ROOT);
        final XMLElement responsesRoot = responsesDocument.getRootElement();

        responsesRoot.setAttribute(STAMP_ATTRIBUTE, DateTime.now().toString());

        for (final Exception exception: responses) {
            final XMLElement responseElement = responsesRoot
                .addChild(RESPONSE_ELEMENT);

            if (exception != null) {
                final XMLElement messageElement = responseElement
                    .addChild(MESSAGE_ELEMENT);
                final String message = exception.getMessage();

                messageElement
                    .setAttribute(
                        EXCEPTION_ATTRIBUTE,
                        exception.getClass().getName());

                if (message != null) {
                    messageElement.addText(message);
                }
            }
        }

        return responsesDocument;
    }

    private PointValue _createPurge(
            final String action,
            final String pointString)
        throws BadRequestException, ServiceSessionException
    {
        final Optional<UUID> pointUUID;
        final PointValue update;

        if (pointString == null) {
            throw new BadRequestException("The Point reference is missing");
        }

        pointUUID = _context.getPointUUID(pointString);

        if (pointUUID.isPresent()) {
            update = new PointValue(
                pointUUID.get(),
                Optional.empty(),
                null,
                null);
        } else {
            update = new PointValue();
            update.setValue(pointString);
        }

        return update;
    }

    private PointValue[] _createPurges(
            @Nonnull final XMLDocument updateDocument)
        throws BadRequestException, ServiceSessionException
    {
        final List<? extends XMLElement> updateElements = updateDocument
            .getRootElement()
            .getChildren();
        final List<PointValue> updates = new ArrayList<PointValue>(
            updateElements.size());

        for (final XMLElement element: updateElements) {
            final String action = element.getName();
            final String point = element
                .getAttributeValue(POINT_ATTRIBUTE, Optional.empty())
                .orElse(null);

            if (!PURGE_ELEMENT.equals(action)) {
                throw new BadRequestException(
                    "The action '" + action + "' is not recognized");
            }

            updates.add(_createPurge(action, point));
        }

        return updates.toArray(new PointValue[updates.size()]);
    }

    /** After attribute. */
    public static final String AFTER_ATTRIBUTE = "after";

    /** Before attribute. */
    public static final String BEFORE_ATTRIBUTE = "before";

    /** Exception attribute. */
    public static final String EXCEPTION_ATTRIBUTE = "exception";

    /** Message element. */
    public static final String MESSAGE_ELEMENT = "message";

    /** Not after attribute. */
    public static final String NOT_AFTER_ATTRIBUTE = "notAfter";

    /** Not before attribute. */
    public static final String NOT_BEFORE_ATTRIBUTE = "notBefore";

    /** Point attribute. */
    public static final String POINT_ATTRIBUTE = "point";

    /** Purges root element. */
    public static final String PURGES_ROOT = "purges";

    /** Purge element. */
    public static final String PURGE_ELEMENT = "purge";

    /** Responses root element. */
    public static final String RESPONSES_ROOT = "responses";

    /** Response element. */
    public static final String RESPONSE_ELEMENT = "response";

    /** State attribute. */
    public static final String STATE_ATTRIBUTE = "state";

    /** Value attribute. */
    public static final String VALUE_ATTRIBUTE = "value";

    /**  */

    private static final long serialVersionUID = 1L;

    private transient volatile PurgeContext _context;
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
