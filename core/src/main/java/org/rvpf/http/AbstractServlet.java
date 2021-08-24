/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: AbstractServlet.java 4112 2019-08-02 20:00:26Z SFB $
 */

package org.rvpf.http;

import java.io.IOException;
import java.io.Reader;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rvpf.base.DateTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.xml.JSONSupport;
import org.rvpf.base.xml.XMLDocument;

/**
 * Abstract servlet.
 */
@ThreadSafe
public abstract class AbstractServlet
    extends HttpServlet
{
    /**
     * Canonicalizes text.
     *
     * @param text The optional text.
     *
     * @return The canonicalized text (empty on missing text)..
     */
    @Nonnull
    @CheckReturnValue
    public static Optional<String> canonicalize(
            @Nonnull final Optional<String> text)
    {
        return ValueConverter.canonicalizeString(text);
    }

    /**
     * Sends a response.
     *
     * @param request The GET request.
     * @param response The HTTP response.
     * @param responseDocument The response document.
     *
     * @throws IOException When appropriate.
     */
    protected static void sendGetResponse(
            @Nonnull final HttpServletRequest request,
            @Nonnull final HttpServletResponse response,
            @Nonnull final XMLDocument responseDocument)
        throws IOException
    {
        try {
            response
                .setContentType(
                    toBoolean(
                        Optional
                                .ofNullable(
                                        request
                                                .getParameter(
                                                        JSON_PARAMETER)))? JSON_CONTENT_TYPE
                                                        : XML_CONTENT_TYPE);
            _sendResponse(response, responseDocument);
        } catch (final BadRequestException exception) {
            response
                .sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    exception.getMessage());
        }
    }

    /**
     * Sends a response.
     *
     * @param request The POST request.
     * @param response The HTTP response.
     * @param responseDocument The response document.
     *
     * @throws IOException When appropriate.
     */
    protected static void sendPostResponse(
            @Nonnull final HttpServletRequest request,
            @Nonnull final HttpServletResponse response,
            @Nonnull final XMLDocument responseDocument)
        throws IOException
    {
        response.setContentType(request.getContentType());
        _sendResponse(response, responseDocument);
    }

    /**
     * Gets a boolean value.
     *
     * @param optionalValueString An optional value string..
     *
     * @return The boolean value (false if absent).
     *
     * @throws BadRequestException On a bad value string.
     */
    @CheckReturnValue
    protected static boolean toBoolean(
            @Nonnull final Optional<String> optionalValueString)
        throws BadRequestException
    {
        final boolean value;

        if (optionalValueString.isPresent()) {
            final String valueString = optionalValueString.get().trim();

            if (valueString.isEmpty()) {
                value = true;
            } else if (ValueConverter.isTrue(valueString)) {
                value = true;
            } else if (ValueConverter.isFalse(valueString)) {
                value = false;
            } else {
                throw new BadRequestException(
                    Message.format(
                        HTTPMessages.UNRECOGNIZED_BOOLEAN,
                        valueString));
            }
        } else {
            value = false;
        }

        return value;
    }

    /**
     * Gets a stamp (date-time) value.
     *
     * @param valueString An optional value string.
     *
     * @return The stamp value (empty if absent).
     *
     * @throws BadRequestException On a bad value string.
     */
    @Nonnull
    @CheckReturnValue
    protected static Optional<DateTime> toStamp(
            @Nonnull final Optional<String> valueString)
        throws BadRequestException
    {
        final DateTime stamp;

        try {
            stamp = valueString
                .isPresent()? DateTime.now().valueOf(valueString.get()): null;
        } catch (final IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }

        return Optional.ofNullable(stamp);
    }

    /**
     * Gets the logger.
     *
     * @return The logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return _logger;
    }

    /**
     * Parses the document contained in a POST request.
     *
     * @param request The POST request.
     * @param response The HTTP response.
     *
     * @return A XML document (null on failure).
     *
     * @throws IOException When appropriate.
     */
    @Nullable
    @CheckReturnValue
    protected XMLDocument parseRequest(
            @Nonnull final HttpServletRequest request,
            @Nonnull final HttpServletResponse response)
        throws IOException
    {
        final String contentType = request.getContentType();
        final Reader reader = request.getReader();
        final XMLDocument document = new XMLDocument();

        try {
            if ((contentType != null)
                    && contentType.startsWith(JSON_CONTENT_TYPE)) {
                document.setRootElement(Optional.of(JSONSupport.parse(reader)));
            } else {
                document.parse(reader);
            }
        } catch (final XMLDocument.ParseException exception) {
            getThisLogger()
                .debug(
                    exception.getCause(),
                    HTTPMessages.DOCUMENT_PARSE_FAILED);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);

            return null;
        }

        return document;
    }

    /**
     * Does service processing.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     *
     * @throws ServletException When appropriate.
     * @throws IOException When appropriate.
     */
    @Override
    protected final void service(
            final HttpServletRequest request,
            final HttpServletResponse response)
        throws ServletException, IOException
    {
        Logger.setLogID(Optional.ofNullable(_logID));

        if (getThisLogger().isDebugEnabled()) {
            String pathInfo = request.getPathInfo();
            String queryString = request.getQueryString();

            if (pathInfo == null) {
                pathInfo = "";
            }

            queryString = (queryString != null)? ("?" + queryString): "";
            getThisLogger()
                .debug(
                    HTTPMessages.REQUEST_RECEIVED,
                    request.getMethod(),
                    request.getRemoteHost(),
                    String.valueOf(request.getRemotePort()),
                    request.getContextPath(),
                    request.getServletPath(),
                    pathInfo,
                    queryString);
        }

        super.service(request, response);
    }

    /**
     * Sets the log ID.
     *
     * @param logID The optional log ID.
     */
    protected void setLogID(final Optional<String> logID)
    {
        _logID = logID.orElse(null);
    }

    private static void _sendResponse(
            @Nonnull final HttpServletResponse response,
            @Nonnull final XMLDocument xmlDocument)
        throws IOException
    {
        response.setHeader("Cache-Control", "no-cache");

        if (response.getContentType() == null) {
            response.setContentType(XML_CONTENT_TYPE);
        }

        final String contentType = response.getContentType();
        final String content;

        if ((contentType != null)
                && contentType.startsWith(JSON_CONTENT_TYPE)) {
            content = JSONSupport.toJSON(xmlDocument);
        } else {
            content = xmlDocument.toXML(Optional.empty(), false);
        }

        response.getWriter().write(content);
        response.flushBuffer();
    }

    /** JSON content type. */
    public static final String JSON_CONTENT_TYPE = "application/json";

    /** JSON parameter. */
    public static final String JSON_PARAMETER = "json";

    /** Response root element. */
    public static final String RESPONSE_ROOT = "response";

    /** Stamp attribute. */
    public static final String STAMP_ATTRIBUTE = "stamp";

    /** XML content type. */
    public static final String XML_CONTENT_TYPE =
        "application/xml;charset=UTF-8";

    /**  */

    private static final long serialVersionUID = 1L;

    private String _logID;
    private final Logger _logger = Logger.getInstance(getClass());

    /**
     * Bad request exception.
     */
    public static final class BadRequestException
        extends Exception
    {
        /**
         * Constructs an instance.
         *
         * @param message The explanatory text.
         */
        public BadRequestException(@Nonnull final String message)
        {
            super(message);
        }

        private static final long serialVersionUID = 1L;
    }
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
