/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: InfoServlet.java 3980 2019-05-13 12:52:38Z SFB $
 */

package org.rvpf.http.query;

import java.io.IOException;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rvpf.base.DateTime;
import org.rvpf.base.UUID;
import org.rvpf.base.store.PointBinding;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.AbstractServlet;
import org.rvpf.http.ServiceSessionException;

/**
 * Info servlet.
 */
@ThreadSafe
public final class InfoServlet
    extends AbstractServlet
{
    /**
     * Initializes the servlet state.
     *
     * @throws ServletException When appropriate.
     */
    @Override
    public void init()
        throws ServletException
    {
        _requester = (QueryContext) getServletContext()
            .getAttribute(QueryModule.QUERY_REQUESTER_ATTRIBUTE);
        setLogID(_requester.getLogID());
    }

    /** {@inheritDoc}
     */
    @Override
    protected void doGet(
            final HttpServletRequest request,
            final HttpServletResponse response)
        throws IOException
    {
        final XMLDocument responsesDocument = _createResponsesDocument();

        try {
            String select = canonicalize(
                Optional.ofNullable(request.getParameter(SELECT_PARAMETER)))
                .orElse(null);

            if (select == null) {
                select = POINTS_QUERY;
            }

            if (POINTS_QUERY.equalsIgnoreCase(select)) {
                final PointBinding.Request query = _makePointsRequest(
                    request.getParameter(UUID_ATTRIBUTE),
                    request.getParameter(NAME_ATTRIBUTE),
                    request.getParameter(WILD_ATTRIBUTE),
                    request.getParameter(REGEXP_ATTRIBUTE));

                _processQueries(
                    new PointBinding.Request[] {query},
                    responsesDocument);
            } else {
                throw new BadRequestException("Only 'points' may be selected");
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
        final XMLDocument requestsDocument = parseRequest(request, response);

        if (requestsDocument == null) {
            return;
        }

        final XMLDocument responsesDocument = _createResponsesDocument();

        try {
            _processQueries(requestsDocument, responsesDocument);
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

        sendPostResponse(request, response, responsesDocument);
    }

    private static XMLDocument _createResponsesDocument()
    {
        final XMLDocument responsesDocument = new XMLDocument(RESPONSES_ROOT);
        final XMLElement responsesRoot = responsesDocument.getRootElement();

        responsesRoot.setAttribute(STAMP_ATTRIBUTE, DateTime.now().toString());

        return responsesDocument;
    }

    private static PointBinding.Request _makePointsRequest(
            final String uuidString,
            final String name,
            final String wild,
            final String regexp)
        throws BadRequestException
    {
        final PointBinding.Request.Builder requestBuilder = PointBinding.Request
            .newBuilder();
        int selected = 0;

        if (uuidString != null) {
            requestBuilder.selectUUID(UUID.fromString(uuidString).get());
            ++selected;
        }

        if (name != null) {
            requestBuilder.selectName(name);
            ++selected;
        }

        if (wild != null) {
            requestBuilder.selectWild(wild);
            ++selected;
        }

        if (regexp != null) {
            final Pattern pattern;

            try {
                pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
            } catch (final PatternSyntaxException exception) {
                throw new BadRequestException(exception.getMessage());
            }

            requestBuilder.selectPattern(pattern);
            ++selected;
        }

        if (selected != 1) {
            throw new BadRequestException(
                "One and only one of '" + UUID_ATTRIBUTE + "', '"
                + NAME_ATTRIBUTE + "', '" + WILD_ATTRIBUTE + "' or '"
                + REGEXP_ATTRIBUTE + "' must be specified");
        }

        return requestBuilder.build();
    }

    private void _processQueries(
            @Nonnull final PointBinding.Request[] queries,
            @Nonnull final XMLDocument responsesDocument)
        throws ServiceSessionException
    {
        final PointBinding[] responses = _requester.getPoints(queries);

        for (final PointBinding pointBinding: responses) {
            final XMLElement pointElement = responsesDocument
                .getRootElement()
                .addChild(POINT_ELEMENT);

            pointElement.setAttribute(NAME_ATTRIBUTE, pointBinding.getName());
            pointElement
                .setAttribute(
                    UUID_ATTRIBUTE,
                    pointBinding.getUUID().toString());
        }
    }

    private void _processQueries(
            @Nonnull final XMLDocument requestsDocument,
            @Nonnull final XMLDocument responsesDocument)
        throws BadRequestException, ServiceSessionException
    {
        final List<PointBinding.Request> queries =
            new LinkedList<PointBinding.Request>();

        for (final XMLElement element:
                requestsDocument.getRootElement().getChildren()) {
            final String name = element.getName();

            if (POINTS_QUERY.equals(name)) {
                final String uuidString = element
                    .getAttributeValue(UUID_ATTRIBUTE, Optional.empty())
                    .orElse(null);
                final PointBinding.Request query = _makePointsRequest(
                    uuidString,
                    element
                        .getAttributeValue(NAME_ATTRIBUTE, Optional.empty())
                        .orElse(null),
                    element
                        .getAttributeValue(WILD_ATTRIBUTE, Optional.empty())
                        .orElse(null),
                    element
                        .getAttributeValue(REGEXP_ATTRIBUTE, Optional.empty())
                        .orElse(null));

                queries.add(query);
            } else {
                throw new BadRequestException(
                    "Unsupported requests element: " + name);
            }
        }

        _processQueries(
            queries.toArray(new PointBinding.Request[queries.size()]),
            responsesDocument);
    }

    /** Name attribute. */
    public static final String NAME_ATTRIBUTE = "name";

    /** Points query. */
    public static final String POINTS_QUERY = "points";

    /** Point element. */
    public static final String POINT_ELEMENT = "point";

    /** Regexp attribute. */
    public static final String REGEXP_ATTRIBUTE = "regexp";

    /** Requests root element. */
    public static final String REQUESTS_ROOT = "requests";

    /** Responses root element. */
    public static final String RESPONSES_ROOT = "responses";

    /** Select parameter. */
    public static final String SELECT_PARAMETER = "select";

    /** UUID attribute. */
    public static final String UUID_ATTRIBUTE = "uuid";

    /** Wild attribute. */
    public static final String WILD_ATTRIBUTE = "wild";

    /**  */

    private static final long serialVersionUID = 1L;

    private transient volatile QueryContext _requester;
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
