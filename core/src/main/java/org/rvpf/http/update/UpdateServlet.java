/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: UpdateServlet.java 3950 2019-05-04 14:45:20Z SFB $
 */

package org.rvpf.http.update;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.concurrent.ThreadSafe;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.rmi.ServiceClosedException;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.VersionedValue;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.AbstractServlet;
import org.rvpf.http.ServiceSessionException;

/**
 * Update servlet.
 */
@ThreadSafe
public final class UpdateServlet
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
        _context = (UpdateContext) getServletContext()
            .getAttribute(UpdateModule.UPDATE_CONTEXT_ATTRIBUTE);
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
        final String stamp = canonicalize(
            Optional.ofNullable(request.getParameter(STAMP_ATTRIBUTE)))
            .orElse(null);
        final String state = canonicalize(
            Optional.ofNullable(request.getParameter(STATE_ATTRIBUTE)))
            .orElse(null);
        final String value = canonicalize(
            Optional.ofNullable(request.getParameter(VALUE_ATTRIBUTE)))
            .orElse(null);
        final Optional<Exception[]> responses;

        response.setHeader("Cache-Control", "no-cache");

        try {
            final boolean delete = toBoolean(
                Optional.ofNullable(request.getParameter(DELETE_PARAMETER)));
            final String action = delete? DELETE_ELEMENT: UPDATE_ELEMENT;
            final PointValue update = _createUpdate(
                action,
                point,
                stamp,
                state,
                value);

            responses = _context
                .update(
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
                .update(
                    _createUpdates(requestDocument),
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
            final Exception[] responses,
            final HttpServletResponse response)
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

    private PointValue _createUpdate(
            final String action,
            final String pointString,
            final String stampString,
            final String state,
            final String value)
        throws BadRequestException, ServiceSessionException
    {
        final Optional<UUID> pointUUID;
        PointValue update;

        if (pointString == null) {
            throw new BadRequestException("The Point reference is missing");
        }

        pointUUID = _context.getPointUUID(pointString);

        if (pointUUID.isPresent()) {
            update = new PointValue(
                pointUUID.get(),
                toStamp(Optional.ofNullable(stampString)),
                state,
                value);

            if (DELETE_ELEMENT.equals(action)) {
                update = new VersionedValue.Deleted(update);
            }
        } else {
            update = new PointValue();
            update.setValue(pointString);
        }

        return update;
    }

    private PointValue[] _createUpdates(
            final XMLDocument updateDocument)
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
            final String stamp = element
                .getAttributeValue(STAMP_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String state;
            String value;

            if (UPDATE_ELEMENT.equals(action)) {
                state = element
                    .getAttributeValue(STATE_ATTRIBUTE, Optional.empty())
                    .orElse(null);
                value = element
                    .getAttributeValue(VALUE_ATTRIBUTE, Optional.empty())
                    .orElse(null);

                if (value == null) {
                    value = element.getText().trim();

                    if (value.isEmpty()) {
                        value = null;
                    }
                }
            } else if (DELETE_ELEMENT.equals(action)) {
                state = null;
                value = null;
            } else {
                throw new BadRequestException(
                    "The action '" + action + "' is not recognized");
            }

            updates.add(_createUpdate(action, point, stamp, state, value));
        }

        return updates.toArray(new PointValue[updates.size()]);
    }

    /** Delete element. */
    public static final String DELETE_ELEMENT = "delete";

    /** Delete parameter. */
    public static final String DELETE_PARAMETER = "delete";

    /** Exception attribute. */
    public static final String EXCEPTION_ATTRIBUTE = "exception";

    /** Message element. */
    public static final String MESSAGE_ELEMENT = "message";

    /** Point attribute. */
    public static final String POINT_ATTRIBUTE = "point";

    /** Responses root element. */
    public static final String RESPONSES_ROOT = "responses";

    /** Response element. */
    public static final String RESPONSE_ELEMENT = "response";

    /** State attribute. */
    public static final String STATE_ATTRIBUTE = "state";

    /** Updates root element. */
    public static final String UPDATES_ROOT = "updates";

    /** Update element. */
    public static final String UPDATE_ELEMENT = "update";

    /** Value attribute. */
    public static final String VALUE_ATTRIBUTE = "value";

    /**  */

    private static final long serialVersionUID = 1L;

    private transient volatile UpdateContext _context;
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
