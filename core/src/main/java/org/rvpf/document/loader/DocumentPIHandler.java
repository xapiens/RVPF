/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentPIHandler.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.document.loader;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;

import java.util.Locale;
import java.util.Optional;

import javax.annotation.concurrent.ThreadSafe;

import org.rvpf.base.logger.Logger;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.util.container.KeyedValues;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;

/**
 * Document Processing instruction handler.
 *
 * <p>Instances of this class accept processing instructions from the
 * configuration or metadata documents.</p>
 *
 * <dl class="doc">
 *   <dt>'include'</dt>
 *   <dd>Includes the file specified in the required 'href' attribute.
 *
 *     <dl class="doc">
 *       <dt>'if'</dt>
 *       <dd>The instruction will be processed only if the specified property is
 *         defined.</dd>
 *
 *       <dt>'unless'</dt>
 *       <dd>The instruction will be processed only if the specified property is
 *         not defined.</dd>
 *
 *       <dt>'href'</dt>
 *       <dd>The URL of the file to include.</dd>
 *
 *       <dt>'optional'</dt>
 *       <dd>True when the file is optional.</dd>
 *     </dl>
 *   </dd>
 *
 *   <dt>'reference'</dt>
 *   <dd>References the file specified in the required 'href' attribute.
 *
 *     <dl class="doc">
 *       <dt>'if'</dt>
 *       <dd>The instruction will be processed only if the specified property is
 *         defined.</dd>
 *
 *       <dt>'unless'</dt>
 *       <dd>The instruction will be processed only if the specified property is
 *         not defined.</dd>
 *
 *       <dt>'href'</dt>
 *       <dd>The URL of the referenced file.</dd>
 *     </dl>
 *   </dd>
 *
 *   <dt>'echo'</dt>
 *   <dd>Echoes the content of the 'message' attribute.
 *
 *     <dl class="doc">
 *       <dt>'if'</dt>
 *       <dd>The instruction will be processed only if the specified property is
 *         defined.</dd>
 *
 *       <dt>'unless'</dt>
 *       <dd>The instruction will be processed only if the specified property is
 *         not defined.</dd>
 *
 *       <dt>'message'</dt>
 *       <dd>The text of the message.</dd>
 *     </dl>
 *   </dd>
 *
 *   <dt>'log'</dt>
 *   <dd>Logs the content of the 'message' attribute.
 *
 *     <dl class="doc">
 *       <dt>'if'</dt>
 *       <dd>The instruction will be processed only if the specified property is
 *         defined.</dd>
 *
 *       <dt>'unless'</dt>
 *       <dd>The instruction will be processed only if the specified property is
 *         not defined.</dd>
 *
 *       <dt>'message'</dt>
 *       <dd>The text of the message.</dd>
 *
 *       <dt>'level'</dt>
 *       <dd>The level of the message. Must be absent or one of 'debug', 'info',
 *         'warn' or 'error'</dd>
 *     </dl>
 *   </dd>
 * </dl>
 *
 * <p><strong>Notes:</strong> the name of the instructions and of their
 * attributes is not case sensitive.</p>
 */
@ThreadSafe
final class DocumentPIHandler
    implements XMLDocument.PIHandler
{
    /**
     * Creates an instance.
     *
     * <p>Supplying a non null owner allows the use of the 'if' and 'unless'
     * attributes on processing instructions. It also provides access to
     * properties substitution (when enabled) in the processing instruction
     * attributes value.</p>
     *
     * @param owner The owner.
     */
    DocumentPIHandler(final DocumentLoader owner)
    {
        _owner = owner;
        _logger = Logger
            .getInstance((owner != null)? owner.getClass(): getClass());
    }

    /** {@inheritDoc}
     */
    @Override
    public void onPI(final String target, final String data)
        throws PIException
    {
        final KeyedValues values = _getValues(data);
        String property = _getValue(IF_ARGUMENT, values);
        boolean enabled = (property == null)
                || (_getContextProperty(property) != null);

        if (enabled) {
            property = _getValue(UNLESS_ARGUMENT, values);
            enabled = (property == null)
                      || (_getContextProperty(property) == null);
        }

        if (enabled) {
            switch (target.toLowerCase(Locale.ROOT)) {
                case ECHO_INSTRUCTION: {
                    _echo(values);

                    break;
                }
                case LOG_INSTRUCTION: {
                    _log(values);

                    break;
                }
                case INCLUDE_INSTRUCTION: {
                    _include(values);

                    break;
                }
                case REFERENCE_INSTRUCTION: {
                    _reference(values);

                    break;
                }
                default: {
                    break;    // Ignores.
                }
            }
        }
    }

    private static KeyedValues _getValues(final String data)
    {
        final StreamTokenizer tokenizer = new StreamTokenizer(
            new StringReader(data));
        final KeyedValues values = new KeyedValues();

        tokenizer.resetSyntax();
        tokenizer.wordChars('a', 'z');
        tokenizer.wordChars('A', 'Z');
        tokenizer.whitespaceChars(0, ' ');
        tokenizer.quoteChar('"');
        tokenizer.quoteChar('\'');
        tokenizer.lowerCaseMode(true);

        try {
            while (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
                final String name = tokenizer.sval;

                if (tokenizer.nextToken() != '=') {
                    break;
                }

                tokenizer.nextToken();

                if ((tokenizer.ttype != '\'') && (tokenizer.ttype != '"')) {
                    break;
                }

                values.add(name, tokenizer.sval);
            }
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        return values;
    }

    private void _echo(final KeyedValues values)
        throws PIException
    {
        final String message = _getValue(MESSAGE_ARGUMENT, values);

        if (message == null) {
            _logger
                .error(
                    ServiceMessages.MISSING_PI_ARGUMENT,
                    MESSAGE_ARGUMENT,
                    ECHO_INSTRUCTION);

            throw new PIException();
        }

        System.out.println(message);
    }

    private String _getContextProperty(final String name)
    {
        final Config config = (_owner != null)? _owner.getConfig(): null;
        final String value;

        if (config != null) {
            value = config.getStringValue(name).orElse(null);
        } else {
            value = System.getProperty(name);
        }

        return value;
    }

    private String _getValue(final String name, final KeyedValues values)
    {
        final Optional<String> text = values.getString(name);

        return ((_owner != null)
                && text.isPresent())? _owner
                    .substitute(text.get()): text.orElse(null);
    }

    private String[] _getValues(final String name, final KeyedValues values)
    {
        final String[] texts = values.getStrings(name);

        if (_owner != null) {
            for (int i = 0; i < texts.length; ++i) {
                texts[i] = _owner.substitute(texts[i]);
            }
        }

        return texts;
    }

    private void _include(final KeyedValues values)
        throws PIException
    {
        if (_owner != null) {
            final String href = _getValue(HREF_ARGUMENT, values);

            if (href == null) {
                _logger
                    .error(
                        ServiceMessages.MISSING_PI_ARGUMENT,
                        HREF_ARGUMENT,
                        INCLUDE_INSTRUCTION);

                throw new PIException();
            }

            final boolean verify = ValueConverter
                .convertToBoolean(
                    ServiceMessages.ATTRIBUTE_TYPE.toString(),
                    VERIFY_ARGUMENT,
                    Optional.ofNullable(_getValue(VERIFY_ARGUMENT, values)),
                    false);
            final String[] verifyKeyIdents = _getValues(
                VERIFY_KEY_ARGUMENT,
                values);
            final boolean decrypt = ValueConverter
                .convertToBoolean(
                    ServiceMessages.ATTRIBUTE_TYPE.toString(),
                    DECRYPT_ARGUMENT,
                    Optional.ofNullable(_getValue(DECRYPT_ARGUMENT, values)),
                    false);
            final String[] decryptKeyIdents = _getValues(
                DECRYPT_KEY_ARGUMENT,
                values);
            final Optional<String> security = Optional
                .ofNullable(_getValue(SECURITY_ARGUMENT, values));
            final boolean optional = ValueConverter
                .convertToBoolean(
                    ServiceMessages.ATTRIBUTE_TYPE.toString(),
                    OPTIONAL_ARGUMENT,
                    Optional.ofNullable(_getValue(OPTIONAL_ARGUMENT, values)),
                    false);

            if (!_owner
                .include(
                    href,
                    verify,
                    verifyKeyIdents,
                    decrypt,
                    decryptKeyIdents,
                    security,
                    optional)) {
                throw new PIException();
            }
        } else {
            _logger.warn(ServiceMessages.INCLUDE_NEEDS_OWNER);
        }
    }

    private void _log(final KeyedValues values)
        throws PIException
    {
        final String message = _getValue(MESSAGE_ARGUMENT, values);

        if (message == null) {
            _logger
                .error(
                    ServiceMessages.MISSING_PI_ARGUMENT,
                    MESSAGE_ARGUMENT,
                    LOG_INSTRUCTION);

            throw new PIException();
        }

        final String level = _getValue(LEVEL_ARGUMENT, values);

        if ((level == null) || INFO_LEVEL.equalsIgnoreCase(level)) {
            _logger.info(ServiceMessages.DOCUMENT_LOG, message);
        } else if (DEBUG_LEVEL.equalsIgnoreCase(level)) {
            _logger.debug(ServiceMessages.DOCUMENT_LOG, message);
        } else if (WARN_LEVEL.equalsIgnoreCase(level)) {
            _logger.warn(ServiceMessages.DOCUMENT_LOG, message);
        } else if (ERROR_LEVEL.equalsIgnoreCase(level)) {
            _logger.error(ServiceMessages.DOCUMENT_LOG, message);

            throw new PIException();
        } else {
            _logger.warn(ServiceMessages.LOG_LEVEL_UNKNOWN, level);
            _logger.info(ServiceMessages.DOCUMENT_LOG, message);
        }
    }

    private void _reference(final KeyedValues values)
        throws PIException
    {
        if (_owner != null) {
            final String href = _getValue(HREF_ARGUMENT, values);

            if (href == null) {
                _logger
                    .error(
                        ServiceMessages.MISSING_PI_ARGUMENT,
                        HREF_ARGUMENT,
                        REFERENCE_INSTRUCTION);

                throw new PIException();
            }

            if (!_owner.reference(href)) {
                throw new PIException();
            }
        }
    }

    /** Debug level. */
    public static final String DEBUG_LEVEL = "debug";

    /** Decrypt argument. */
    public static final String DECRYPT_ARGUMENT = "decrypt";

    /** Decrypt key argument. */
    public static final String DECRYPT_KEY_ARGUMENT = "decryptKey";

    /** Echo instruction. */
    public static final String ECHO_INSTRUCTION = "echo";

    /** Error level. */
    public static final String ERROR_LEVEL = "error";

    /** Href argument. */
    public static final String HREF_ARGUMENT = "href";

    /** If argument. */
    public static final String IF_ARGUMENT = "if";

    /** Include instruction. */
    public static final String INCLUDE_INSTRUCTION = "include";

    /** Info level. */
    public static final String INFO_LEVEL = "info";

    /** Level argument. */
    public static final String LEVEL_ARGUMENT = "level";

    /** Log instruction. */
    public static final String LOG_INSTRUCTION = "log";

    /** Message argument. */
    public static final String MESSAGE_ARGUMENT = "message";

    /** Optional argument. */
    public static final String OPTIONAL_ARGUMENT = "optional";

    /** Reference instruction. */
    public static final String REFERENCE_INSTRUCTION = "reference";

    /** Security argument. */
    public static final String SECURITY_ARGUMENT = "security";

    /** Unless argument. */
    public static final String UNLESS_ARGUMENT = "unless";

    /** Verify argument. */
    public static final String VERIFY_ARGUMENT = "verify";

    /** Verify key argument. */
    public static final String VERIFY_KEY_ARGUMENT = "verifyKey";

    /** Warn level. */
    public static final String WARN_LEVEL = "warn";

    private final Logger _logger;
    private final DocumentLoader _owner;
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
