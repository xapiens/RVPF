/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: RPNServlet.java 4105 2019-07-09 15:41:18Z SFB $
 */

package org.rvpf.http.rpn;

import java.io.IOException;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rvpf.base.Content;
import org.rvpf.base.DateTime;
import org.rvpf.base.Params;
import org.rvpf.base.Point;
import org.rvpf.base.UUID;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Message;
import org.rvpf.base.value.PointValue;
import org.rvpf.base.value.ResultValue;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.content.SIContent;
import org.rvpf.http.AbstractServlet;
import org.rvpf.http.HTTPMessages;
import org.rvpf.metadata.Metadata;
import org.rvpf.metadata.Proxied;
import org.rvpf.metadata.entity.ContentEntity;
import org.rvpf.metadata.entity.PointEntity;
import org.rvpf.processor.engine.rpn.RPNExecutor;

/**
 * RPN servlet.
 */
@ThreadSafe
public final class RPNServlet
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
        _context = (RPNContext) getServletContext()
            .getAttribute(RPNModule.RPN_CONTEXT_ATTRIBUTE);
        setLogID(_context.getLogID());
        _si = null;
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

        if (requestDocument != null) {
            sendPostResponse(
                request,
                response,
                _processRequest(requestDocument));
        }
    }

    private synchronized ContentEntity _getSIContent()
    {
        if (_si == null) {
            final Proxied instance = new SIContent();
            final ContentEntity.Builder contentBuilder = ContentEntity
                .newBuilder();

            contentBuilder.setName("SI");

            _si = contentBuilder.build();

            if (!instance.setUp(_context.getMetadata(), _si)) {
                return null;
            }

            _si.setInstance(instance);
        }

        return _si;
    }

    private ResultValue _prepareResult(
            @Nonnull final XMLElement requestRoot,
            @Nonnull final Logger logger)
    {
        final PointEntity resultPoint = new PointEntity.Definition();
        final XMLElement result = requestRoot
            .getFirstChild(RESULT_ELEMENT)
            .orElse(null);
        final Params params = new Params();
        final ResultValue resultValue;

        if (result == null) {
            logger.error(HTTPMessages.MISSING_ELEMENT, RESULT_ELEMENT);
        } else {
            _setContent(result, resultPoint, logger);
        }

        resultPoint.setUUID(Optional.of(UUID.generate()));

        resultValue = new ResultValue(resultPoint, Optional.of(DateTime.now()));

        for (final XMLElement inputElement:
                requestRoot.getChildren(INPUT_ELEMENT)) {
            final PointEntity inputPoint = new PointEntity.Definition();
            String value;

            _setContent(inputElement, inputPoint, logger);
            inputPoint.setUUID(Optional.of(UUID.generate()));
            value = inputElement
                .getAttributeValue(VALUE_ATTRIBUTE, Optional.empty())
                .orElse(null);

            if (value == null) {
                value = inputElement.getText();

                if (value.isEmpty()) {
                    value = null;
                }
            }

            final PointValue inputValue = new PointValue(
                inputPoint,
                Optional.of(resultValue.getStamp()),
                null,
                value);

            resultValue.addInputValue(inputValue);
        }

        for (final XMLElement paramElement:
                requestRoot.getChildren(PARAM_ELEMENT)) {
            String value;

            value = paramElement
                .getAttributeValue(VALUE_ATTRIBUTE, Optional.empty())
                .orElse(null);

            if (value == null) {
                value = paramElement.getText();
            }

            params.add(Point.PARAM_PARAM, value);
        }

        params.freeze();
        resultPoint.setParams(Optional.of(params));

        return resultValue;
    }

    private XMLDocument _processRequest(
            @Nonnull final XMLDocument requestDocument)
    {
        final XMLElement requestRoot = requestDocument.getRootElement();
        final XMLDocument responseDocument = new XMLDocument(RESPONSE_ROOT);
        final XMLElement responseRoot = responseDocument.getRootElement();
        final Optional<XMLElement> childProgramElement = requestRoot
            .getFirstChild(PROGRAM_ELEMENT);
        final String program = childProgramElement
            .map(childProgram -> childProgram.getText())
            .orElse(null);
        final _ListLogger logger = new _ListLogger();
        final ResultValue resultValue;
        final PointValue responseValue;

        if (program == null) {
            logger.error(HTTPMessages.MISSING_ELEMENT, PROGRAM_ELEMENT);
            resultValue = null;
        } else {
            resultValue = _prepareResult(requestRoot, logger);
        }

        if (logger.isLogEmpty()) {
            final List<String> macros = new LinkedList<>();

            for (final XMLElement macroElement:
                    requestRoot.getChildren(MACRO_ELEMENT)) {
                macros.add(macroElement.getText());
            }

            final List<String> words = new LinkedList<>();

            for (final XMLElement wordElement:
                    requestRoot.getChildren(WORD_ELEMENT)) {
                words.add(wordElement.getText());
            }

            final RPNExecutor executor = _context.getExecutor();

            synchronized (executor) {
                responseValue = executor
                    .execute(
                        program,
                        macros.toArray(new String[macros.size()]),
                        words.toArray(new String[words.size()]),
                        resultValue,
                        logger);
            }
        } else {
            responseValue = null;
        }

        responseRoot.setAttribute(STAMP_ATTRIBUTE, DateTime.now().toString());

        if (logger.isLogEmpty()
                && (responseValue != null)
                && (responseValue.getValue() != null)) {
            responseRoot
                .addChild(VALUE_ELEMENT)
                .addText(responseValue.getValue().toString());
        } else {
            for (final String message: logger.getLog()) {
                responseRoot.addChild(MESSAGE_ELEMENT).addText(message);
            }
        }

        return responseDocument;
    }

    private void _setContent(
            final XMLElement element,
            final PointEntity point,
            final Logger logger)
    {
        final String contentName = element
            .getAttributeValue(CONTENT_ATTRIBUTE, Optional.empty())
            .orElse(null);
        final String unit = element
            .getAttributeValue(UNIT_ATTRIBUTE, Optional.empty())
            .orElse(null);
        final ContentEntity contentEntity;

        if (contentName == null) {
            final Params params;

            if (unit == null) {
                logger
                    .error(
                        HTTPMessages.MISSING_ELEMENT_ATTRIBUTE,
                        element.getName(),
                        CONTENT_ATTRIBUTE);

                return;
            }

            contentEntity = _getSIContent();

            if (contentEntity == null) {
                return;
            }

            params = point.getParams().copy();
            params.add(SIContent.UNIT_PARAM, unit);
            point.setParams(Optional.of(params));
        } else if (unit != null) {
            logger
                .error(
                    HTTPMessages.ATTRIBUTE_CONFLICT,
                    CONTENT_ATTRIBUTE,
                    UNIT_ATTRIBUTE);

            return;
        } else {
            final Metadata metadata = _context.getMetadata();

            contentEntity = metadata
                .getContentEntity(Optional.ofNullable(contentName))
                .orElse(null);

            if (contentEntity == null) {
                logger.error(HTTPMessages.CONTENT_UNKNOWN, contentName);

                return;
            }

            synchronized (contentEntity) {
                if (!contentEntity.setUp(metadata)) {
                    logger.error(HTTPMessages.CONTENT_CONFIG, contentName);

                    return;
                }
            }
        }

        final Content content = contentEntity.getContent();
        final Proxied contentInstance = (Proxied) content.getInstance(point);

        if (contentInstance == null) {
            if (unit == null) {
                logger.error(HTTPMessages.CONTENT_INAPPROPRIATE, contentName);
            } else {
                logger.error(HTTPMessages.UNIT_UNKNOWN, unit);
            }

            return;
        }

        point
            .setContentEntity(
                (ContentEntity) contentEntity.getProxy(contentInstance));
    }

    /** Content attribute. */
    public static final String CONTENT_ATTRIBUTE = "content";

    /** Input element. */
    public static final String INPUT_ELEMENT = "input";

    /** Macro element. */
    public static final String MACRO_ELEMENT = "macro";

    /** Message element. */
    public static final String MESSAGE_ELEMENT = "message";

    /** Param element. */
    public static final String PARAM_ELEMENT = "param";

    /** Program element. */
    public static final String PROGRAM_ELEMENT = "program";

    /** Request root element. */
    public static final String REQUEST_ROOT = "request";

    /** Result element. */
    public static final String RESULT_ELEMENT = "result";

    /** Unit attribute. */
    public static final String UNIT_ATTRIBUTE = "unit";

    /** Value attribute. */
    public static final String VALUE_ATTRIBUTE = "value";

    /** Value element. */
    public static final String VALUE_ELEMENT = "value";

    /** Word element. */
    public static final String WORD_ELEMENT = "word";
    private static final long serialVersionUID = 1L;

    private transient volatile RPNContext _context;
    private transient volatile ContentEntity _si;

    /**
     * List logger.
     */
    private static final class _ListLogger
        extends Logger
    {
        /**
         * Constructs an instance.
         */
        _ListLogger()
        {
            super(_ListLogger.class);
        }

        /** {@inheritDoc}
         */
        @Override
        public void doLog(final LogLevel logLevel, final Message message)
        {
            final String text = message.toString();

            _log.add(text);

            final Optional<Throwable> cause = message.getCause();

            if (cause.isPresent()) {
                super
                    .doLog(
                        LogLevel.DEBUG,
                        new Message(cause.get(), HTTPMessages.MESSAGE, text));
            } else {
                super
                    .doLog(
                        LogLevel.DEBUG,
                        new Message(HTTPMessages.MESSAGE, text));
            }
        }

        /**
         * Gets the log.
         *
         * @return The log.
         */
        @Nonnull
        @CheckReturnValue
        List<String> getLog()
        {
            return _log;
        }

        /**
         * Asks if the log is empty.
         *
         * @return True if the log is empty.
         */
        @CheckReturnValue
        boolean isLogEmpty()
        {
            return _log.isEmpty();
        }

        private static final long serialVersionUID = 1L;

        private final List<String> _log = new LinkedList<>();
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
