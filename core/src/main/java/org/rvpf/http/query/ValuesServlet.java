/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ValuesServlet.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.http.query;

import java.io.IOException;
import java.io.Serializable;

import java.security.Principal;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.concurrent.ThreadSafe;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.ElapsedTime;
import org.rvpf.base.Point;
import org.rvpf.base.TimeInterval;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Message;
import org.rvpf.base.store.StoreValues;
import org.rvpf.base.store.StoreValuesQuery;
import org.rvpf.base.sync.CrontabSync;
import org.rvpf.base.sync.ElapsedSync;
import org.rvpf.base.sync.StampsSync;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.http.AbstractServlet;
import org.rvpf.http.HTTPMessages;
import org.rvpf.http.ServiceSessionException;

/**
 * Values servlet.
 */
@ThreadSafe
public final class ValuesServlet
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
        _context = (QueryContext) getServletContext()
            .getAttribute(QueryModule.QUERY_REQUESTER_ATTRIBUTE);
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
        final XMLDocument responsesDocument = _createResponsesDocument();
        final boolean isCount = request
            .getServletPath()
            .endsWith(QueryModule.COUNT_PATH);

        try {
            _processRequest(isCount, request, responsesDocument);
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
        final boolean isCount = request
            .getServletPath()
            .endsWith(QueryModule.COUNT_PATH);

        try {
            _processRequests(
                isCount,
                request.getUserPrincipal(),
                requestsDocument,
                responsesDocument);
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

    private static StoreValuesQuery.Builder _prepareQueryBuilder(
            final String pointString,
            final DateTime notBefore,
            final DateTime after,
            final DateTime at,
            final DateTime before,
            final DateTime notAfter,
            final Integer rows,
            final Integer limit,
            final Boolean notNull,
            final Boolean reverse,
            final Boolean interpolated,
            final Boolean extrapolated,
            final Boolean normalized,
            final ElapsedTime polatorTimeLimit)
    {
        final StoreValuesQuery.Builder queryBuilder = StoreValuesQuery
            .newBuilder();

        if (pointString != null) {
            if (UUID.isUUID(pointString)) {
                queryBuilder.setPointUUID(UUID.fromString(pointString).get());
            } else {
                queryBuilder.setPoint(new Point.Named(pointString));
            }
        }

        if (notBefore != null) {
            queryBuilder.setNotBefore(notBefore);
        }

        if (after != null) {
            queryBuilder.setAfter(after);
        }

        if (at != null) {
            queryBuilder.setAt(at);
        }

        if (before != null) {
            queryBuilder.setBefore(before);
        }

        if (notAfter != null) {
            queryBuilder.setNotAfter(notAfter);
        }

        if (rows != null) {
            queryBuilder.setRows(rows.intValue());
        }

        if (limit != null) {
            queryBuilder.setLimit(limit.intValue());
        }

        if (notNull != null) {
            queryBuilder.setNotNull(notNull.booleanValue());
        }

        if (reverse != null) {
            queryBuilder.setReverse(reverse.booleanValue());
        }

        if (interpolated != null) {
            queryBuilder.setInterpolated(interpolated.booleanValue());
        }

        if (extrapolated != null) {
            queryBuilder.setExtrapolated(extrapolated.booleanValue());
        }

        if (normalized != null) {
            queryBuilder.setNormalized(normalized.booleanValue());
        }

        if (polatorTimeLimit != null) {
            queryBuilder.setPolatorTimeLimit(Optional.of(polatorTimeLimit));
        }

        return queryBuilder;
    }

    private static void _processResponse(
            final boolean isCount,
            final StoreValues storeResponse,
            final XMLDocument responsesDocument,
            final XMLElement responseElement)
    {
        if (storeResponse.isSuccess()) {
            if (isCount) {
                final XMLElement countElement = responseElement
                    .addChild(COUNT_ELEMENT);

                countElement
                    .setAttribute(
                        VALUE_ATTRIBUTE,
                        Optional.of(String.valueOf(storeResponse.getCount())));
            } else {
                for (final PointValue pointValue: storeResponse) {
                    final XMLElement valueElement = responseElement
                        .addChild(VALUE_ELEMENT);
                    final Serializable state = pointValue.getState();
                    final Serializable value = pointValue.getValue();

                    valueElement
                        .setAttribute(
                            STAMP_ATTRIBUTE,
                            Optional.of(pointValue.getStamp().toString()));

                    if (state != null) {
                        valueElement
                            .setAttribute(
                                STATE_ATTRIBUTE,
                                Optional.of(state.toString()));
                    }

                    if (value != null) {
                        final String valueString = value.toString();

                        if (valueString.isEmpty()) {
                            valueElement
                                .setAttribute(
                                    VALUE_ATTRIBUTE,
                                    Optional.of(valueString));
                        } else {
                            valueElement.addText(valueString);
                        }
                    }

                    if (pointValue.isSynthesized()) {
                        final String synthesized = pointValue
                            .isInterpolated()? INTERPOLATED_VALUE: (pointValue
                                .isExtrapolated()? EXTRAPOLATED_VALUE: "");

                        valueElement
                            .setAttribute(
                                SYNTHESIZED_ATTRIBUTE,
                                Optional.of(synthesized));
                    }
                }
            }

            if (!storeResponse.isComplete()) {
                final StoreValuesQuery.Mark mark = storeResponse
                    .getMark()
                    .get();
                final DateTime stamp = mark.getStamp();
                final XMLElement markElement = responseElement
                    .addChild(MARK_ELEMENT);

                markElement
                    .setAttribute(
                        STAMP_ATTRIBUTE,
                        Optional.of(stamp.toString()));
            }
        } else {
            final Exception exception = storeResponse.getException().get();
            final XMLElement messageElement = responseElement
                .addChild(MESSAGE_ELEMENT);

            messageElement
                .setAttribute(
                    EXCEPTION_ATTRIBUTE,
                    Optional.of(exception.getClass().getName()));
            messageElement.addText(exception.getMessage());
        }
    }

    private static void _processResponses(
            final boolean isCount,
            final StoreValues[] responses,
            final XMLDocument responsesDocument)
    {
        final Iterator<? extends XMLElement> iterator = responsesDocument
            .getRootElement()
            .getChildren(RESPONSE_ELEMENT)
            .iterator();

        for (final StoreValues response: responses) {
            final XMLElement responseElement = iterator.next();

            if (response != null) {
                _processResponse(
                    isCount,
                    response,
                    responsesDocument,
                    responseElement);
            }
        }
    }

    private static Boolean _toBoolean(
            final String valueString)
        throws BadRequestException
    {
        return (valueString != null)? Boolean
            .valueOf(toBoolean(Optional.of(valueString))): null;
    }

    private static ElapsedTime _toElapsed(
            final String valueString)
        throws BadRequestException
    {
        try {
            return ElapsedTime
                .fromString(Optional.ofNullable(valueString))
                .orElse(null);
        } catch (final IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    private static Integer _toInteger(
            final String valueString)
        throws BadRequestException
    {
        final Integer value;

        if (valueString != null) {
            try {
                value = Integer.valueOf(valueString);
            } catch (final NumberFormatException exception) {
                throw new BadRequestException(
                    Message.format(
                        HTTPMessages.UNRECOGNIZED_INTEGER,
                        valueString));
            }
        } else {
            value = null;
        }

        return value;
    }

    private void _processRequest(
            final boolean isCount,
            final HttpServletRequest request,
            final XMLDocument responsesDocument)
        throws BadRequestException, ServiceSessionException
    {
        final Principal principal = request.getUserPrincipal();
        String pointString = canonicalize(
            Optional.ofNullable(request.getParameter(POINT_ATTRIBUTE)))
            .orElse(null);
        final String notBeforeString = canonicalize(
            Optional.ofNullable(request.getParameter(NOT_BEFORE_ATTRIBUTE)))
            .orElse(null);
        final String afterString = canonicalize(
            Optional.ofNullable(request.getParameter(AFTER_ATTRIBUTE)))
            .orElse(null);
        final String atString = canonicalize(
            Optional.ofNullable(request.getParameter(AT_ATTRIBUTE)))
            .orElse(null);
        final String beforeString = canonicalize(
            Optional.ofNullable(request.getParameter(BEFORE_ATTRIBUTE)))
            .orElse(null);
        final String notAfterString = canonicalize(
            Optional.ofNullable(request.getParameter(NOT_AFTER_ATTRIBUTE)))
            .orElse(null);
        final String rowsString = canonicalize(
            Optional.ofNullable(request.getParameter(ROWS_ATTRIBUTE)))
            .orElse(null);
        final String limitString = canonicalize(
            Optional.ofNullable(request.getParameter(LIMIT_ATTRIBUTE)))
            .orElse(null);
        final String nullsString = canonicalize(
            Optional.ofNullable(request.getParameter(NULLS_ATTRIBUTE)))
            .orElse(null);
        final String reverseString = canonicalize(
            Optional.ofNullable(request.getParameter(REVERSE_ATTRIBUTE)))
            .orElse(null);
        final String interpolatedString = canonicalize(
            Optional.ofNullable(request.getParameter(INTERPOLATED_ATTRIBUTE)))
            .orElse(null);
        final String extrapolatedString = canonicalize(
            Optional.ofNullable(request.getParameter(EXTRAPOLATED_ATTRIBUTE)))
            .orElse(null);
        final String normalizedString = canonicalize(
            Optional.ofNullable(request.getParameter(NORMALIZED_ATTRIBUTE)))
            .orElse(null);
        final String crontabString = canonicalize(
            Optional.ofNullable(request.getParameter(CRONTAB_ATTRIBUTE)))
            .orElse(null);
        final String elapsedString = canonicalize(
            Optional.ofNullable(request.getParameter(ELAPSED_ATTRIBUTE)))
            .orElse(null);
        final String polatorTimeLimitString = canonicalize(
            Optional
                .ofNullable(request.getParameter(POLATOR_TIME_LIMIT_ATTRIBUTE)))
            .orElse(null);
        final XMLElement responseElement = responsesDocument
            .getRootElement()
            .addChild(RESPONSE_ELEMENT);
        StoreValues storeResponse;

        if (pointString == null) {
            pointString = "";
        } else {
            pointString = pointString.trim();
        }

        if (pointString.isEmpty()) {
            throw new BadRequestException(
                Message.format(HTTPMessages.MISSING_POINT_REFERENCE));
        }

        responseElement.removeAttribute(REQUEST_ATTRIBUTE);
        responseElement.setAttribute(POINT_ATTRIBUTE, pointString);

        try {
            final StoreValuesQuery.Builder queryBuilder = _prepareQueryBuilder(
                pointString,
                toStamp(Optional.ofNullable(notBeforeString)).orElse(null),
                toStamp(Optional.ofNullable(afterString)).orElse(null),
                toStamp(Optional.ofNullable(atString)).orElse(null),
                toStamp(Optional.ofNullable(beforeString)).orElse(null),
                toStamp(Optional.ofNullable(notAfterString)).orElse(null),
                _toInteger(rowsString),
                _toInteger(limitString),
                _toBoolean(nullsString),
                _toBoolean(reverseString),
                _toBoolean(interpolatedString),
                _toBoolean(extrapolatedString),
                _toBoolean(normalizedString),
                _toElapsed(polatorTimeLimitString));

            if (crontabString != null) {
                final CrontabSync crontabSync = new CrontabSync();

                if (crontabSync.setUp(crontabString)) {
                    queryBuilder.setSync(crontabSync);
                } else {
                    throw new BadRequestException(
                        Message.format(
                            BaseMessages.BAD_CRONTAB_ENTRY,
                            crontabString));
                }
            }

            if (elapsedString != null) {
                queryBuilder
                    .setSync(new ElapsedSync(elapsedString, Optional.empty()));
            }

            queryBuilder.setCount(isCount);
            storeResponse = _context
                .select(
                    new StoreValuesQuery[] {queryBuilder.build(), },
                    Optional.ofNullable(principal))[0];
        } catch (final TimeInterval.InvalidIntervalException exception) {
            storeResponse = new StoreValues(exception);
        }

        _processResponse(
            isCount,
            storeResponse,
            responsesDocument,
            responseElement);
    }

    private void _processRequests(
            final boolean isCount,
            final Principal principal,
            final XMLDocument requestsDocument,
            final XMLDocument responsesDocument)
        throws BadRequestException, ServiceSessionException
    {
        final List<StoreValuesQuery> queries =
            new LinkedList<StoreValuesQuery>();

        for (final XMLElement requestElement:
                requestsDocument
                    .getRootElement()
                    .getChildren(REQUEST_ELEMENT)) {
            final String idString = requestElement
                .getAttributeValue(ID_ATTRIBUTE, Optional.empty())
                .orElse(null);
            String pointString = requestElement
                .getAttributeValue(POINT_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String notBeforeString = requestElement
                .getAttributeValue(NOT_BEFORE_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String afterString = requestElement
                .getAttributeValue(AFTER_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String atString = requestElement
                .getAttributeValue(AT_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String beforeString = requestElement
                .getAttributeValue(BEFORE_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String notAfterString = requestElement
                .getAttributeValue(NOT_AFTER_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String rowsString = requestElement
                .getAttributeValue(ROWS_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String limitString = requestElement
                .getAttributeValue(LIMIT_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String nullsString = requestElement
                .getAttributeValue(NULLS_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String reverseString = requestElement
                .getAttributeValue(REVERSE_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String interpolatedString = requestElement
                .getAttributeValue(INTERPOLATED_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String extrapolatedString = requestElement
                .getAttributeValue(EXTRAPOLATED_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String normalizedString = requestElement
                .getAttributeValue(NORMALIZED_ATTRIBUTE, Optional.empty())
                .orElse(null);
            final String polatorTimeLimitString = requestElement
                .getAttributeValue(
                    POLATOR_TIME_LIMIT_ATTRIBUTE,
                    Optional.empty())
                .orElse(null);
            final Optional<XMLElement> syncElement = requestElement
                .getFirstChild(SYNC_ELEMENT);
            final String syncType = (syncElement
                .isPresent())? syncElement
                    .get()
                    .getAttributeValue(TYPE_ATTRIBUTE, Optional.empty())
                    .orElse(null): null;
            final String syncText = ((syncElement.isPresent())
                    && (syncType != null))? syncElement.get().getText(): null;
            final XMLElement responseElement = responsesDocument
                .getRootElement()
                .addChild(RESPONSE_ELEMENT);

            if (pointString == null) {
                pointString = "";
            } else {
                pointString = pointString.trim();
            }

            if (pointString.isEmpty()) {
                throw new BadRequestException(
                    Message.format(HTTPMessages.MISSING_POINT_REFERENCE));
            }

            responseElement
                .setAttribute(REQUEST_ATTRIBUTE, Optional.ofNullable(idString));
            responseElement
                .setAttribute(POINT_ATTRIBUTE, Optional.of(pointString));

            StoreValuesQuery.Builder queryBuilder = null;

            try {
                queryBuilder = _prepareQueryBuilder(
                    pointString,
                    toStamp(Optional.ofNullable(notBeforeString)).orElse(null),
                    toStamp(Optional.ofNullable(afterString)).orElse(null),
                    toStamp(Optional.ofNullable(atString)).orElse(null),
                    toStamp(Optional.ofNullable(beforeString)).orElse(null),
                    toStamp(Optional.ofNullable(notAfterString)).orElse(null),
                    _toInteger(rowsString),
                    _toInteger(limitString),
                    _toBoolean(nullsString),
                    _toBoolean(reverseString),
                    _toBoolean(interpolatedString),
                    _toBoolean(extrapolatedString),
                    _toBoolean(normalizedString),
                    _toElapsed(polatorTimeLimitString));

                if (syncText != null) {
                    if (CRONTAB_VALUE.equalsIgnoreCase(syncType)) {
                        final CrontabSync crontabSync = new CrontabSync();

                        if (crontabSync.setUp(syncText)) {
                            queryBuilder.setSync(crontabSync);
                        } else {
                            throw new BadRequestException(
                                Message.format(
                                    BaseMessages.BAD_CRONTAB_ENTRY,
                                    syncText));
                        }
                    } else if (ELAPSED_VALUE.equalsIgnoreCase(syncType)) {
                        queryBuilder
                            .setSync(
                                new ElapsedSync(syncText, Optional.empty()));
                    } else if (STAMPS_VALUE.equalsIgnoreCase(syncType)) {
                        queryBuilder
                            .setSync(
                                new StampsSync(
                                    _STAMPS_SPLIT_PATTERN.split(syncText)));
                    } else {
                        throw new BadRequestException(
                            Message.format(
                                HTTPMessages.UNRECOGNIZED_SYNC_TYPE,
                                syncType));
                    }
                }

                queryBuilder.setCount(isCount);
            } catch (final TimeInterval.InvalidIntervalException exception) {
                _processResponse(
                    isCount,
                    new StoreValues(exception),
                    responsesDocument,
                    responseElement);
                queryBuilder = null;
            }

            if (queryBuilder != null) {
                queries.add(queryBuilder.build());
            }
        }

        _processResponses(
            isCount,
            _context
                .select(
                    queries.toArray(new StoreValuesQuery[queries.size()]),
                    Optional.ofNullable(principal)),
            responsesDocument);
    }

    /** After attribute. */
    public static final String AFTER_ATTRIBUTE = "after";

    /** At attribute. */
    public static final String AT_ATTRIBUTE = "at";

    /** Before attribute. */
    public static final String BEFORE_ATTRIBUTE = "before";

    /** Count element. */
    public static final String COUNT_ELEMENT = "count";

    /** Crontab attribute. */
    public static final String CRONTAB_ATTRIBUTE = "crontab";

    /** Crontab value. */
    public static final String CRONTAB_VALUE = "crontab";

    /** Elapsed attribute. */
    public static final String ELAPSED_ATTRIBUTE = "elapsed";

    /** Elapsed value. */
    public static final String ELAPSED_VALUE = "elapsed";

    /** Exception attribute. */
    public static final String EXCEPTION_ATTRIBUTE = "exception";

    /** Extrapolated attribute. */
    public static final String EXTRAPOLATED_ATTRIBUTE = "extrapolated";

    /** Extrapolated value. */
    public static final String EXTRAPOLATED_VALUE = "extrapolated";

    /** ID attribute. */
    public static final String ID_ATTRIBUTE = "id";

    /** Interpolated attribute. */
    public static final String INTERPOLATED_ATTRIBUTE = "interpolated";

    /** Interpolated value. */
    public static final String INTERPOLATED_VALUE = "interpolated";

    /** Limit attribute. */
    public static final String LIMIT_ATTRIBUTE = "limit";

    /** Mark element. */
    public static final String MARK_ELEMENT = "mark";

    /** Message element. */
    public static final String MESSAGE_ELEMENT = "message";

    /** Normalized attribute. */
    public static final String NORMALIZED_ATTRIBUTE = "normalized";

    /** Not after attribute. */
    public static final String NOT_AFTER_ATTRIBUTE = "notAfter";

    /** Not before attribute. */
    public static final String NOT_BEFORE_ATTRIBUTE = "notBefore";

    /** Nulls attribute. */
    public static final String NULLS_ATTRIBUTE = "nulls";

    /** Point attribute. */
    public static final String POINT_ATTRIBUTE = "point";

    /** Polator time limit attribute. */
    public static final String POLATOR_TIME_LIMIT_ATTRIBUTE =
        "polatorTimeLimit";

    /** Requests root element. */
    public static final String REQUESTS_ROOT = "requests";

    /** Request attribute. */
    public static final String REQUEST_ATTRIBUTE = "request";

    /** Request element. */
    public static final String REQUEST_ELEMENT = "request";

    /** Responses root element. */
    public static final String RESPONSES_ROOT = "responses";

    /** Response element. */
    public static final String RESPONSE_ELEMENT = "response";

    /** Reverse attribute. */
    public static final String REVERSE_ATTRIBUTE = "reverse";

    /** Rows attribute. */
    public static final String ROWS_ATTRIBUTE = "rows";

    /** Stamps value. */
    public static final String STAMPS_VALUE = "stamps";

    /** State attribute. */
    public static final String STATE_ATTRIBUTE = "state";

    /** Sync element. */
    public static final String SYNC_ELEMENT = "sync";

    /** Synthesized attribute. */
    public static final String SYNTHESIZED_ATTRIBUTE = "synthesized";

    /** Type attribute. */
    public static final String TYPE_ATTRIBUTE = "type";

    /** Value attribute. */
    public static final String VALUE_ATTRIBUTE = "value";

    /** Value element. */
    public static final String VALUE_ELEMENT = "value";

    /**  */

    private static final Pattern _STAMPS_SPLIT_PATTERN = Pattern.compile(",");
    private static final long serialVersionUID = 1L;

    private transient volatile QueryContext _context;
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
