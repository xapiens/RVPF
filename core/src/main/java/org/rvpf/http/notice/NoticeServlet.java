/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: NoticeServlet.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.http.notice;

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
import org.rvpf.base.Point;
import org.rvpf.base.exception.PointUnknownException;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.RecalcTrigger;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.AbstractServlet;

/**
 * Notice servlet.
 */
@ThreadSafe
public final class NoticeServlet
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
        _context = (NoticeContext) getServletContext()
            .getAttribute(NoticeModule.NOTICE_CONTEXT_ATTRIBUTE);
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
        final Optional<String> stamp = canonicalize(
            Optional.ofNullable(request.getParameter(STAMP_ATTRIBUTE)));
        final String state = canonicalize(
            Optional.ofNullable(request.getParameter(STATE_ATTRIBUTE)))
            .orElse(null);
        final String value = canonicalize(
            Optional.ofNullable(request.getParameter(VALUE_ATTRIBUTE)))
            .orElse(null);

        final List<Exception> responses = new ArrayList<Exception>(1);

        try {
            final boolean recalc = toBoolean(
                Optional.ofNullable(request.getParameter(RECALC_PARAMETER)));

            responses.add(_processNotice(recalc, point, stamp, state, value));
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(exception);
        } catch (final BadRequestException exception) {
            response
                .sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    exception.getMessage());
        }

        final XMLDocument responsesDocument = _createResponsesDocument(
            responses,
            response);

        sendGetResponse(request, response, responsesDocument);
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

        final List<Exception> responses;

        try {
            responses = _processNotices(requestDocument);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(exception);
        } catch (final BadRequestException exception) {
            response
                .sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    exception.getMessage());

            return;
        }

        final XMLDocument responsesDocument = _createResponsesDocument(
            responses,
            response);

        sendPostResponse(request, response, responsesDocument);
    }

    private static XMLDocument _createResponsesDocument(
            @Nonnull final List<Exception> responses,
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

    private Exception _processNotice(
            final boolean recalc,
            final String pointString,
            final Optional<String> stampString,
            final String state,
            final String value)
        throws InterruptedException, BadRequestException
    {
        final Point point = _toPoint(pointString);

        if (point == null) {
            return new PointUnknownException(pointString);
        }

        final Optional<DateTime> stamp = toStamp(stampString);
        final PointValue pointValue = recalc? new RecalcTrigger(
            point,
            stamp): new PointValue(point, stamp, state, value);

        _context.notify(new PointValue[] {pointValue.encoded()});

        return null;
    }

    private List<Exception> _processNotices(
            final XMLDocument noticeDocument)
        throws InterruptedException, BadRequestException
    {
        final List<? extends XMLElement> noticeElements = noticeDocument
            .getRootElement()
            .getChildren();
        final List<PointValue> notices = new ArrayList<PointValue>();
        final List<Exception> responses = new ArrayList<Exception>(
            noticeElements.size());

        for (final XMLElement element: noticeElements) {
            final String name = element.getName();
            final String pointString = element
                .getAttributeValue(POINT_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final Point point = _toPoint(pointString);
            final Optional<DateTime> stamp = toStamp(
                Optional
                    .ofNullable(
                        element
                                .getAttributeValue(
                                        STAMP_ATTRIBUTE,
                                                Optional.empty())
                                .orElse(null)));

            if (point == null) {
                responses.add(new PointUnknownException(pointString));
            } else if (NOTICE_ELEMENT.equals(name)) {
                final String state = element
                    .getAttributeValue(STATE_ATTRIBUTE, Optional.empty())
                    .orElse(null);
                String value;

                value = element
                    .getAttributeValue(VALUE_ATTRIBUTE, Optional.empty())
                    .orElse(null);

                if (value == null) {
                    value = element.getText().trim();

                    if (value.isEmpty()) {
                        value = null;
                    }
                }

                notices
                    .add(new PointValue(point, stamp, state, value).encoded());
                responses.add(null);
            } else if (RECALC_ELEMENT.equals(name)) {
                notices.add(new RecalcTrigger(point, stamp));
                responses.add(null);
            } else {
                throw new BadRequestException(
                    "The element '" + name + "' is not recognized");
            }
        }

        if (!notices.isEmpty()) {
            _context.notify(notices.toArray(new PointValue[notices.size()]));
        }

        return responses;
    }

    private Point _toPoint(final String pointString)
        throws BadRequestException
    {
        final Point point;

        if (pointString != null) {
            point = _context.getMetadata().getPoint(pointString).orElse(null);
        } else {
            throw new BadRequestException("The Point reference is missing");
        }

        return point;
    }

    /** Exception attribute. */
    public static final String EXCEPTION_ATTRIBUTE = "exception";

    /** Message element. */
    public static final String MESSAGE_ELEMENT = "message";

    /** Notices root element. */
    public static final String NOTICES_ROOT = "notices";

    /** Notice element. */
    public static final String NOTICE_ELEMENT = "notice";

    /** Point attribute. */
    public static final String POINT_ATTRIBUTE = "point";

    /** Recalc element. */
    public static final String RECALC_ELEMENT = "recalc";

    /** Recalc parameter. */
    public static final String RECALC_PARAMETER = "recalc";

    /** Responses root element. */
    public static final String RESPONSES_ROOT = "responses";

    /** Response element. */
    public static final String RESPONSE_ELEMENT = "response";

    /** State attribute. */
    public static final String STATE_ATTRIBUTE = "state";

    /** Value attribute. */
    public static final String VALUE_ATTRIBUTE = "value";
    private static final long serialVersionUID = 1L;

    private transient volatile NoticeContext _context;
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
