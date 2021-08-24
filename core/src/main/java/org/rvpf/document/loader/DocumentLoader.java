/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: DocumentLoader.java 4096 2019-06-24 23:07:39Z SFB $
 */

package org.rvpf.document.loader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;

import java.net.URL;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.Entity;
import org.rvpf.base.UUID;
import org.rvpf.base.exception.ValidationException;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Logger.LogLevel;
import org.rvpf.base.security.Crypt;
import org.rvpf.base.security.SecurityContext;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.util.ResourceURLHandler;
import org.rvpf.base.util.URLHandlerFactory;
import org.rvpf.base.util.container.KeyedGroups;
import org.rvpf.base.xml.XMLDocument;
import org.rvpf.base.xml.XMLDocument.PIHandler;
import org.rvpf.base.xml.XMLElement;
import org.rvpf.config.Config;
import org.rvpf.service.ServiceMessages;

/**
 * Document.
 */
@NotThreadSafe
public abstract class DocumentLoader
    implements XMLElement.Factory
{
    /**
     * Constructs an instance.
     */
    DocumentLoader() {}

    /** {@inheritDoc}
     */
    @Override
    public XMLElement newXMLElement(final String name)
    {
        return new DocumentElement(name, this);
    }

    /**
     * Sets the validating indicator.
     *
     * @param validating The validating indicator.
     */
    public void setValidating(final boolean validating)
    {
        _validating = validating;
    }

    /**
     * Adds prefix names.
     *
     * @param prefixNames An array of prefix-name tuples.
     */
    protected final void addPrefixNames(@Nonnull final String[][] prefixNames)
    {
        for (final String[] prefixName: prefixNames) {
            _prefixNames.put(prefixName[0], prefixName[1]);
        }
    }

    /**
     * Gets a locally registered Entity.
     *
     * @param key The Entity name.
     * @param prefix The prefix associated with the Entity class.
     *
     * @return The Entity or empty if the Entity is unknown.
     */
    @Nonnull
    @CheckReturnValue
    protected final Optional<? extends Entity> getEntity(
            @Nonnull final String key,
            @Nonnull final String prefix)
    {
        Entity entity;

        if (_entities.isPresent()) {
            entity = _entities.get().get(_entityKey(key, prefix));

            if ((entity != null) && !prefix.equals(entity.getPrefix())) {
                entity = null;
            }
        } else {
            entity = null;
        }

        return Optional.ofNullable(entity);
    }

    /**
     * Gets the name corresponding to a prefix.
     *
     * @param prefix The prefix.
     *
     * @return The name.
     */
    @Nonnull
    @CheckReturnValue
    protected final String getPrefixName(@Nonnull final String prefix)
    {
        return Require.notNull(_prefixNames.get(prefix));
    }

    /**
     * Gets a root handler.
     *
     * @return A root handler.
     */
    @Nonnull
    @CheckReturnValue
    protected RootHandler getRootHandler()
    {
        return new RootHandler();
    }

    /**
     * Gets the root element name for this document.
     *
     * @return The element's name.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract String getRootName();

    /**
     * Gets the document logger.
     *
     * @return The document logger.
     */
    @Nonnull
    @CheckReturnValue
    protected final Logger getThisLogger()
    {
        return _logger;
    }

    /**
     * Gets the URL for this document.
     *
     * @return The URL.
     */
    @Nonnull
    @CheckReturnValue
    protected abstract URL getURL();

    /**
     * Specifies a handler for an XML element.
     *
     * @param name The element name.
     * @param loader The entry object that will handle the element.
     */
    protected final void handle(
            @Nonnull final String name,
            @Nonnull final DocumentElementLoader loader)
    {
        final String path = "/" + getRootName() + "/" + name;

        _getHandlers().add(new Handler(Optional.of(path), Optional.of(loader)));
    }

    /**
     * Loads this document from the specified location with a supplied reader.
     *
     * @param from The location to load from.
     * @param reader The optional reader.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean loadFrom(
            @Nonnull final String from,
            @Nonnull final Optional<Reader> reader)
    {
        final String rootName = getRootName();
        final URL fromURL = getConfig().createURL(from, Optional.empty());
        final DocumentReader documentReader;

        if (fromURL == null) {
            getThisLogger().error(ServiceMessages.BAD_URL, from);

            return false;
        }

        try {
            documentReader = DocumentReader
                .create(fromURL, reader, Optional.empty());
        } catch (final FileNotFoundException exception) {
            getThisLogger()
                .error(
                    ServiceMessages.DOCUMENT_LOAD_FAILED,
                    getRootName(),
                    fromURL,
                    exception.getMessage());

            return false;
        }

        if (!_entities.isPresent()) {
            _entities = Optional.of(new HashMap<String, Entity>());
        }

        setURL(documentReader.getFromURL());
        getThisLogger()
            .debug(ServiceMessages.LOADING_FROM, getRootName(), getURL());

        try {
            _read(documentReader);
        } finally {
            try {
                documentReader.close();
            } catch (final IOException exception) {
                throw new RuntimeException(exception);
            }
        }

        final XMLElement rootElement = _document.getRootElement();

        if ((rootElement != null) && !rootName.equals(rootElement.getName())) {
            getThisLogger()
                .warn(
                    ServiceMessages.UNEXPECTED_ROOT,
                    rootName,
                    rootElement.getName());
        }

        return !getThisLogger().hasLogged(LogLevel.ERROR);
    }

    /**
     * Reads the document from an XML string.
     *
     * @param xml The XML string.
     *
     * @return True on success.
     */
    @CheckReturnValue
    protected boolean read(@Nonnull final String xml)
    {
        final StringReader reader = new StringReader(xml);

        _document = new XMLDocument();
        _document.setElementFactory(this);
        _document.setPIHandler(Optional.of(new DocumentPIHandler(this)));
        _document.setValidating(_validating);

        _registerHandlers();

        try {
            _document.parse(reader);
        } catch (final XMLDocument.ParseException exception) {
            throw new RuntimeException(exception.getCause());
        }

        return !getThisLogger().hasLogged(LogLevel.ERROR);
    }

    /**
     * Sets the entities.
     *
     * @param entities The optional entities.
     */
    protected final void setEntities(
            @Nonnull final Optional<HashMap<String, Entity>> entities)
    {
        _entities = entities;
    }

    /**
     * Sets the deferred substitution indicator.
     *
     * @param substitutionDeferred True if variables substitution should be
     *                             deferred.
     */
    protected final void setSubstitutionDeferred(
            final boolean substitutionDeferred)
    {
        if (_substitutionDeferred != substitutionDeferred) {
            _substitutionDeferred = substitutionDeferred;
            getThisLogger()
                .debug(
                    ServiceMessages.SUBSTITUTION_DEFERRED,
                    ValueConverter.toInteger(_substitutionDeferred));
        }
    }

    /**
     * Sets the variable substitution indicator.
     *
     * @param substitutionEnabled True if variables should be substituted.
     */
    protected final void setSubstitutionEnabled(
            final boolean substitutionEnabled)
    {
        if (_substitutionEnabled != substitutionEnabled) {
            _substitutionEnabled = substitutionEnabled;
            getThisLogger()
                .trace(
                    ServiceMessages.SUBSTITUTION_ENABLED,
                    ValueConverter.toInteger(_substitutionEnabled));
        }
    }

    /**
     * Sets the URL for this document.
     *
     * @param url The URL.
     */
    protected abstract void setURL(@Nonnull URL url);

    /**
     * Returns the document.
     *
     * @return The document.
     */
    @Nonnull
    @CheckReturnValue
    final XMLDocument _getDocument()
    {
        return Require.notNull(_document);
    }

    /**
     * Gets the handlers.
     *
     * @return The handlers.
     */
    @Nonnull
    @CheckReturnValue
    final List<Handler> _getHandlers()
    {
        return _handlers;
    }

    /**
     * Sets the language code.
     *
     * @param lang The optional language code.
     */
    final void _setLang(@Nonnull final Optional<String> lang)
    {
        _lang = lang;
    }

    /**
     * Gets the config applicable for this document.
     *
     * @return The config.
     */
    @Nonnull
    @CheckReturnValue
    abstract Config getConfig();

    /**
     * Gets the context URL.
     *
     * @return The URL.
     */
    @Nonnull
    @CheckReturnValue
    final URL getContextURL()
    {
        return _urls.isEmpty()? getURL(): _urls.getLast();
    }

    /**
     * Gets the entities map.
     *
     * @return The optional entities map.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<HashMap<String, Entity>> getEntities()
    {
        return _entities;
    }

    /**
     * Gets the language code.
     *
     * @return The optional language code.
     */
    @Nonnull
    @CheckReturnValue
    final Optional<String> getLang()
    {
        return _lang;
    }

    /**
     * Includes the specified document.
     *
     * @param from The location of the source.
     * @param verify True to verify the source.
     * @param verifyKeyIdents The verify key idents.
     * @param decrypt True to decrypt the source.
     * @param decryptKeyIdents The decrypt key idents.
     * @param security The name of the optional security properties.
     * @param optional Indicates if the document is optional.
     *
     * @return True on success.
     */
    @CheckReturnValue
    final boolean include(
            @Nonnull final String from,
            final boolean verify,
            @Nonnull final String[] verifyKeyIdents,
            final boolean decrypt,
            @Nonnull final String[] decryptKeyIdents,
            @Nonnull final Optional<String> security,
            final boolean optional)
    {
        final URL fromURL = getConfig()
            .createURL(from, Optional.of(getContextURL()));

        if (fromURL == null) {
            getThisLogger().error(ServiceMessages.BAD_URL, from);

            return false;
        }

        if (getConfig().isIncluded(fromURL)) {
            getThisLogger()
                .debug(
                    ServiceMessages.REDUNDANT_INCLUDE,
                    getRootName(),
                    fromURL);
        } else {
            DocumentReader documentReader;

            try {
                documentReader = DocumentReader
                    .create(fromURL, Optional.empty(), Optional.empty());
            } catch (final FileNotFoundException exception) {
                return _fileNotFound(fromURL, optional, exception);
            }

            getThisLogger()
                .debug(
                    ServiceMessages.INCLUDING,
                    getRootName(),
                    documentReader.getFromURL());

            if (verify || decrypt) {
                final XMLDocument xmlDocument = new XMLDocument();

                try {
                    xmlDocument.parse(documentReader);
                } catch (final XMLDocument.ParseException exception) {
                    throw new RuntimeException(exception.getCause());
                }

                try {
                    documentReader.close();
                } catch (final IOException exception) {
                    throw new RuntimeException(exception);
                }

                final SecurityContext securityContext = new SecurityContext(
                    _logger);
                final KeyedGroups configProperties = getConfig()
                    .getProperties();
                final KeyedGroups securityProperties = security
                    .isPresent()? configProperties
                        .getGroup(
                            security.get()): KeyedGroups.MISSING_KEYED_GROUP;

                if (!securityContext
                    .setUp(configProperties, securityProperties)) {
                    return false;
                }

                final Crypt crypt = new Crypt();

                if (!crypt
                    .setUp(
                        securityContext.getCryptProperties(),
                        Optional.empty())) {
                    return false;
                }

                final Serializable serializable = crypt
                    .load(
                        xmlDocument,
                        from,
                        verify,
                        verifyKeyIdents,
                        decrypt,
                        decryptKeyIdents);

                if (serializable == null) {
                    return false;
                }

                try {
                    documentReader = DocumentReader
                        .create(
                            fromURL,
                            Optional
                                .of(
                                        new StringReader(
                                                String.valueOf(serializable))),
                            documentReader.getStamp());
                } catch (final FileNotFoundException exception) {
                    throw new InternalError(exception);    // Should not happen.
                }
            }

            _read(documentReader);
        }

        return !getThisLogger().hasLogged(LogLevel.ERROR);
    }

    /**
     * Puts an entity on the map.
     *
     * @param key The entity key.
     * @param entity The entity.
     * @param supersede True allows supersede.
     */
    final void putEntity(
            @Nonnull final String key,
            @Nonnull final Entity entity,
            final boolean supersede)
    {
        final String entityKey = _entityKey(key, entity.getPrefix());

        if (!_entities.isPresent()) {
            _entities = Optional.of(new HashMap<String, Entity>());
        }

        if ((_entities.get().put(entityKey, entity) != null) && !supersede) {
            getThisLogger()
                .error(
                    ServiceMessages.MULTIPLE_ENTITY_DEFINITION,
                    getPrefixName(entity.getPrefix()),
                    key);
        }
    }

    /**
     * References the specified document.
     *
     * @param from The location of the source.
     *
     * @return True on success.
     */
    @CheckReturnValue
    final boolean reference(@Nonnull final String from)
    {
        final URL fromURL = getConfig()
            .createURL(from, Optional.of(getContextURL()));
        final DocumentStream stream;

        if (fromURL == null) {
            getThisLogger().error(ServiceMessages.BAD_URL, from);

            return false;
        }

        try {
            stream = DocumentStream.create(fromURL);
            stream.close();
        } catch (final FileNotFoundException exception) {
            getThisLogger()
                .error(
                    ServiceMessages.REFERENCE_FAILED,
                    exception.getMessage());

            return false;
        } catch (final IOException exception) {
            throw new RuntimeException(exception);
        }

        updateStamp(stream.getStamp());

        return true;
    }

    /**
     * Removes an entity.
     *
     * @param key The key of the entity.
     * @param prefix The prefix for the key.
     */
    final void removeEntity(
            @Nonnull final String key,
            @Nonnull final String prefix)
    {
        Require
            .success(
                _entities.isPresent()
                && (_entities.get().remove(_entityKey(key, prefix)) != null));
    }

    /**
     * Substitutes markers in a given text.
     *
     * <p>A substitution marker would be a '${x}' property reference.</p>
     *
     * @param text The text possibly containing substitution markers.
     *
     * @return The text with markers substituted.
     */
    @Nonnull
    @CheckReturnValue
    final String substitute(@Nonnull final String text)
    {
        return Require
            .notNull(
                _substitutionEnabled? getConfig()
                    .substitute(text, _substitutionDeferred): text);
    }

    /**
     * Updates the last modified stamp.
     *
     * @param stamp The optional last modified stamp.
     */
    final void updateStamp(@Nonnull final Optional<DateTime> stamp)
    {
        getConfig().updateStamp(stamp);
    }

    private static String _entityKey(String key, final String prefix)
    {
        key = key.trim();

        if (UUID.isUUID(key)) {
            final UUID uuid = UUID.fromString(key).get();

            key = uuid.toRawString();
        } else {
            key = key.toUpperCase(Locale.ROOT);

            if (!ID_PATTERN.matcher(key).matches()) {
                key = prefix + key;
            }
        }

        return key;
    }

    private boolean _fileNotFound(
            final URL fromURL,
            final boolean optional,
            final FileNotFoundException exception)
    {
        if (optional) {
            getThisLogger()
                .debug(
                    ServiceMessages.OPTIONAL_INCLUDE,
                    getRootName(),
                    fromURL);

            return true;
        }

        getThisLogger()
            .error(
                ServiceMessages.INCLUDE_FAILED,
                getRootName(),
                getURL(),
                exception.getMessage());

        return false;
    }

    private void _read(final DocumentReader documentReader)
    {
        final Object savedValues;

        updateStamp(documentReader.getStamp());
        _urls.addLast(documentReader.getFromURL());

        _document = new XMLDocument();
        _document.setElementFactory(this);
        _document.setPIHandler(Optional.of(new DocumentPIHandler(this)));
        _document.setValidating(_validating);

        _registerHandlers();

        savedValues = getConfig().getProperties().saveMonitoredValues();

        try {
            _document.parse(documentReader);
        } catch (final XMLDocument.ParseException exception) {
            if (!(exception.getCause() instanceof PIHandler.PIException)) {
                final String message = exception.getCause().getMessage();

                getThisLogger()
                    .error(
                        exception,
                        BaseMessages.VERBATIM,
                        (message != null)? message: exception.getMessage());
            }
        } finally {
            getConfig().getProperties().restoreMonitoredValues(savedValues);
        }

        _urls.removeLast();
    }

    private void _registerHandlers()
    {
        _document
            .setDefaultHandler(
                Optional.of(new Handler(Optional.empty(), Optional.empty())));

        for (final Handler handler: _getHandlers()) {
            _document
                .setElementHandler(handler.getPath(), Optional.of(handler));
        }

        _document
            .setElementHandler(
                "/" + getRootName(),
                Optional.of(getRootHandler()));
    }

    /** ID pattern. */
    public static final Pattern ID_PATTERN = Pattern
        .compile("_[0-9A-Z]+", Pattern.CASE_INSENSITIVE);

    /** ID prefix. */
    public static final char ID_PREFIX = '_';

    static {
        URLHandlerFactory
            .register(
                ResourceURLHandler.RESOURCE_PROTOCOL,
                new ResourceURLHandler());
    }

    private XMLDocument _document;
    private Optional<HashMap<String, Entity>> _entities = Optional.empty();
    private final List<Handler> _handlers = new LinkedList<Handler>();
    private Optional<String> _lang = Optional.empty();
    private final Logger _logger = Logger.getInstance(getClass());
    private final Map<String, String> _prefixNames = new HashMap<String,
        String>();
    private boolean _substitutionDeferred;
    private boolean _substitutionEnabled;
    private final LinkedList<URL> _urls = new LinkedList<URL>();
    private boolean _validating;

    /**
     * XML element handler.
     */
    final class Handler
        implements XMLElement.Handler
    {
        /**
         * Constructs an instance.
         *
         * @param path The optional element path.
         * @param loader The optional object that will handle the element.
         */
        Handler(
                @Nonnull final Optional<String> path,
                @Nonnull final Optional<DocumentElementLoader> loader)
        {
            _path = path;
            _loader = loader;

            if (_loader.isPresent()) {
                _loader.get().setDocument(DocumentLoader.this);
            }
        }

        /** {@inheritDoc}
         */
        @Override
        public XMLElement onElementEnd(final XMLElement element)
        {
            if (_loader.isPresent()) {
                Require.success(element instanceof DocumentElement);

                if (!element
                    .getAttributeValue(
                        DocumentElement.LANG_ATTRIBUTE,
                        Optional.empty())
                    .isPresent()) {
                    try {
                        _loader.get().process((DocumentElement) element);
                    } catch (final ValidationException exception) {
                        getThisLogger()
                            .error(
                                BaseMessages.VERBATIM,
                                exception.getMessage());
                    }
                } else {
                    _loader.get().updateTexts((DocumentElement) element);
                }

                return null;
            }

            return element;
        }

        /** {@inheritDoc}
         */
        @Override
        public XMLElement onElementStart(final XMLElement element)
        {
            return element;
        }

        /**
         * Gets the element path.
         *
         * @return The element path.
         */
        @Nonnull
        @CheckReturnValue
        String getPath()
        {
            return _path.get();    // Not called by default handler.
        }

        private final Optional<DocumentElementLoader> _loader;
        private final Optional<String> _path;
    }


    /**
     * Root handler.
     */
    class RootHandler
        implements XMLElement.Handler
    {
        /** {@inheritDoc}
         */
        @Override
        public XMLElement onElementEnd(final XMLElement element)
        {
            Require.success(element instanceof DocumentElement);

            _element = (DocumentElement) element;

            onRootEnd();

            return null;
        }

        /** {@inheritDoc}
         */
        @Override
        public XMLElement onElementStart(final XMLElement element)
        {
            Optional<String> condition;

            Require.success(element instanceof DocumentElement);

            _element = (DocumentElement) element;

            condition = getAttribute(DocumentElement.IF_ATTRIBUTE);

            if (condition.isPresent()
                    && !getConfig().getStringValue(
                        condition.get()).isPresent()) {
                _enabled = false;
            }

            condition = getAttribute(DocumentElement.UNLESS_ATTRIBUTE);

            if (condition.isPresent()
                    && getConfig().getStringValue(
                        condition.get()).isPresent()) {
                _enabled = false;
            }

            if (_enabled) {
                _setLang(getAttribute(DocumentElement.LANG_ATTRIBUTE));
            } else {
                for (final Handler handler: _getHandlers()) {
                    _getDocument()
                        .setElementHandler(handler.getPath(), Optional.empty());
                }
            }

            return element;
        }

        /**
         * Gets a named attribute.
         *
         * @param name The name of the attribute.
         *
         * @return The optional value of the attribute.
         */
        @Nonnull
        @CheckReturnValue
        protected final Optional<String> getAttribute(
                @Nonnull final String name)
        {
            return _element.getAttributeValue(name, Optional.empty());
        }

        /**
         * Asks if this document is enabled.
         *
         * @return True if this document is enabled.
         */
        @CheckReturnValue
        protected final boolean isEnabled()
        {
            return _enabled;
        }

        /**
         * Called at the end of the processing of the root element.
         */
        protected void onRootEnd() {}

        private DocumentElement _element;
        private boolean _enabled = true;
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
