/** Related Values Processing Framework.
 *
 * Copyright (c) 2003-2019 Serge Brisson.
 *
 * This software is distributable under the LGPL license.
 * See details at the bottom of this file.
 *
 * $Id: ConfigDocumentLoader.java 4103 2019-07-01 13:31:25Z SFB $
 */

package org.rvpf.document.loader;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.File;
import java.io.Reader;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.Optional;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.rvpf.base.BaseMessages;
import org.rvpf.base.DateTime;
import org.rvpf.base.logger.Logger;
import org.rvpf.base.logger.Logger.LogLevel;
import org.rvpf.base.tool.Require;
import org.rvpf.base.tool.ValueConverter;
import org.rvpf.base.util.ResourceFileFactory;
import org.rvpf.config.Config;
import org.rvpf.config.ConfigProperties;
import org.rvpf.config.UndefinedEntityException;
import org.rvpf.config.entity.ClassDefEntity;
import org.rvpf.config.entity.ClassLibEntity;
import org.rvpf.config.entity.PropertiesDefEntity;
import org.rvpf.config.entity.PropertyDefEntity;
import org.rvpf.service.ServiceClassLoader;
import org.rvpf.service.ServiceContext;
import org.rvpf.service.ServiceMessages;

/**
 * Config document.
 */
public class ConfigDocumentLoader
    extends DocumentLoader
    implements PropertyChangeListener
{
    /**
     * Constructs an instance.
     *
     * @param config Config updated by this Document.
     * @param root The name of the root element.
     */
    ConfigDocumentLoader(
            @Nonnull final Config config,
            @Nonnull final String root)
    {
        _config = config;
        _config.setRootName(root);

        addPrefixNames(_PREFIX_NAMES);

        handle(ConfigElementLoader.CLASS_DEF_ENTITY, new ClassDefLoader());
        handle(ConfigElementLoader.CLASS_LIB_ENTITY, new ClassLibLoader());
        handle(ConfigElementLoader.PROPERTIES_ELEMENT, new PropertiesLoader());
        handle(
            ConfigElementLoader.PROPERTIES_DEF_ENTITY,
            new PropertiesDefLoader());
        handle(ConfigElementLoader.PROPERTY_ELEMENT, new PropertyLoader());
        handle(
            ConfigElementLoader.PROPERTY_DEF_ENTITY,
            new PropertyDefLoader());
        handle(ConfigElementLoader.SERVICE_ELEMENT, new ServiceLoader());

        setSubstitutionEnabled(
            _config.getBooleanValue(Config.SUBSTITUTION_ENABLED_PROPERTY));
        setSubstitutionDeferred(
            _config.getBooleanValue(Config.SUBSTITUTION_DEFERRED_PROPERTY));
        _setValidationEnabled(
            !_config
                .getBooleanValue(
                    VALIDATION_DISABLED_PROPERTY,
                    !_config
                            .getBooleanValue(
                                    VALIDATION_ENABLED_PROPERTY,
                                            true)));

        setEntities(_config.returnEntities());
    }

    /**
     * Constructs an instance.
     *
     * @param config Config updated by this document.
     */
    private ConfigDocumentLoader(final Config config)
    {
        this(config, CONFIG_ROOT);
    }

    /**
     * Loads the config.
     *
     * @param serviceName The name of the service the config is loaded for.
     * @param from The optional location to load from.
     * @param classLoader An optional class loader.
     *
     * @return The config (null on failure).
     */
    @Nullable
    @CheckReturnValue
    public static Config loadConfig(
            @Nonnull final String serviceName,
            @Nonnull Optional<String> from,
            @Nonnull final Optional<ServiceClassLoader> classLoader)
    {
        final Config config = new Config(serviceName);
        final ConfigDocumentLoader documentLoader = new ConfigDocumentLoader(
            config);

        if (classLoader.isPresent()) {
            config.setClassLoader(classLoader);
        }

        if (!from.isPresent()) {
            from = config
                .getStringValue(CONFIG_PROPERTY, Optional.of(DEFAULT_CONFIG));
        }

        if (!documentLoader.loadFrom(from.get(), Optional.empty())) {
            return null;
        }

        if (!serviceName.isEmpty()) {
            final Optional<ServiceContext> serviceContext = config
                .getServiceContext(serviceName);

            if (serviceContext.isPresent()) {
                try {
                    config
                        .getClassLoader()
                        .addFromClassLibs(serviceContext.get().getClassLibs());
                } catch (final UndefinedEntityException exception) {
                    Logger
                        .getInstance(ConfigDocumentLoader.class)
                        .error(BaseMessages.VERBATIM, exception.getMessage());

                    return null;
                }
            } else {
                Logger
                    .getInstance(ConfigDocumentLoader.class)
                    .warn(ServiceMessages.SERVICE_NOT_CONFIGURED, serviceName);

                return null;
            }

            config.setUpService();
        }

        return config;
    }

    /**
     * Called when a monitored property changes.
     *
     * @param event The event describing the change.
     */
    @Override
    public final void propertyChange(final PropertyChangeEvent event)
    {
        final String key = event.getPropertyName();
        final Object value = event.getNewValue();

        if ((value != null) && !(value instanceof String)) {
            getThisLogger().warn(ServiceMessages.UNEXPECTED_VALUE_TYPE, key);
        } else if (Config.SUBSTITUTION_ENABLED_PROPERTY.equals(key)) {
            setSubstitutionEnabled(
                ValueConverter
                    .convertToBoolean(
                        ServiceMessages.DYNAMIC_TYPE.toString(),
                        Config.SUBSTITUTION_ENABLED_PROPERTY,
                        Optional.ofNullable((String) value),
                        false));
        } else if (Config.SUBSTITUTION_DEFERRED_PROPERTY.equals(key)) {
            setSubstitutionDeferred(
                ValueConverter
                    .convertToBoolean(
                        ServiceMessages.DYNAMIC_TYPE.toString(),
                        Config.SUBSTITUTION_DEFERRED_PROPERTY,
                        Optional.ofNullable((String) value),
                        false));
        } else if (VALIDATION_DISABLED_PROPERTY.equals(key)) {
            _setValidationEnabled(
                !ValueConverter
                    .convertToBoolean(
                        ServiceMessages.DYNAMIC_TYPE.toString(),
                        VALIDATION_DISABLED_PROPERTY,
                        Optional.ofNullable((String) value),
                        false));
        } else if (VALIDATION_ENABLED_PROPERTY.equals(key)) {
            _setValidationEnabled(
                ValueConverter
                    .convertToBoolean(
                        ServiceMessages.DYNAMIC_TYPE.toString(),
                        VALIDATION_ENABLED_PROPERTY,
                        Optional.ofNullable((String) value),
                        false));
        } else {
            Require.failure("Unexpected property change event: " + key);
        }
    }

    /** {@inheritDoc}
     */
    @Override
    protected RootHandler getRootHandler()
    {
        return new ConfigHandler();
    }

    /** {@inheritDoc}
     */
    @Override
    protected final String getRootName()
    {
        return _config.getRootName();
    }

    /** {@inheritDoc}
     */
    @Override
    protected URL getURL()
    {
        return _config.getURL();
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean loadFrom(final String from, final Optional<Reader> reader)
    {
        final boolean success;

        setSubstitutionEnabled(
            _config.getBooleanValue(Config.SUBSTITUTION_ENABLED_PROPERTY));
        setSubstitutionDeferred(
            _config.getBooleanValue(Config.SUBSTITUTION_DEFERRED_PROPERTY));
        _setValidationEnabled(
            !_config
                .getBooleanValue(
                    VALIDATION_DISABLED_PROPERTY,
                    !_config
                            .getBooleanValue(
                                    VALIDATION_ENABLED_PROPERTY,
                                            true)));

        final ConfigProperties configProperties = _config.getProperties();

        configProperties
            .addPropertyChangeListener(
                Config.SUBSTITUTION_ENABLED_PROPERTY,
                this);
        configProperties
            .addPropertyChangeListener(
                Config.SUBSTITUTION_DEFERRED_PROPERTY,
                this);
        configProperties
            .addPropertyChangeListener(VALIDATION_DISABLED_PROPERTY, this);
        configProperties
            .addPropertyChangeListener(VALIDATION_ENABLED_PROPERTY, this);

        success = super.loadFrom(from, reader);

        configProperties.removePropertyChangeListener(this);

        configProperties.freeze();

        if (success) {
            if (_config.getStamp().isPresent()) {
                getThisLogger()
                    .debug(
                        ServiceMessages.DOCUMENT_TIME,
                        _config.getStamp().orElse(null));
            }

            _config.keepEntities(getEntities());
        }

        return success && !getThisLogger().hasLogged(LogLevel.ERROR);
    }

    /** {@inheritDoc}
     */
    @Override
    protected boolean read(final String xml)
    {
        if (!super.read(xml)) {
            return false;
        }

        _config.keepEntities(getEntities());

        return true;
    }

    /** {@inheritDoc}
     */
    @Override
    protected final void setURL(final URL url)
    {
        _config.setURL(url);

        if (ResourceFileFactory.FILE_PROTOCOL.equalsIgnoreCase(
                url.getProtocol())
                && !_config.getConfigDir().isPresent()) {
            final String configDir;

            try {
                configDir = new File(new URI(url.toString())).getParent();
            } catch (final URISyntaxException exception) {
                throw new RuntimeException(exception);
            }

            getThisLogger().debug(ServiceMessages.CONFIG_DIRECTORY, configDir);
            _config.setConfigDir(configDir.replace('\\', '/'));
        }
    }

    /** {@inheritDoc}
     */
    @Override
    final Config getConfig()
    {
        return _config;
    }

    /**
     * Asks if validation is enabled.
     *
     * @return True if validation is enabled.
     */
    boolean isValidationEnabled()
    {
        return _validationEnabled;
    }

    private void _setValidationEnabled(final boolean validationEnabled)
    {
        if (_validationEnabled != validationEnabled) {
            _validationEnabled = validationEnabled;
            getThisLogger()
                .debug(
                    ServiceMessages.VALIDATION_ENABLED,
                    ValueConverter.toInteger(_validationEnabled));
        }
    }

    /** Property holding the path to the config XML text. */
    public static final String CONFIG_PROPERTY = "config";

    /** Root element of config XML text. */
    public static final String CONFIG_ROOT = "config";

    /** Default source of config XML text. */
    public static final String DEFAULT_CONFIG = "rvpf-config.xml";

    /** The document type strings. */
    public static final String[] DOCTYPE_STRINGS = new String[] {
        "-//Serge Brisson//DTD RVPF//EN",
        "http://rvpf.org/dtd/rvpf.dtd", };

    /** Validation control properties. */
    public static final String VALIDATION_DISABLED_PROPERTY =
        "validation.disabled";
    public static final String VALIDATION_ENABLED_PROPERTY =
        "validation.enabled";
    private static final String[][] _PREFIX_NAMES = {
        {ClassDefEntity.ENTITY_PREFIX, ConfigElementLoader.CLASS_DEF_ENTITY},
        {ClassLibEntity.ENTITY_PREFIX, ConfigElementLoader.CLASS_LIB_ENTITY},
        {PropertiesDefEntity.ENTITY_PREFIX,
         ConfigElementLoader.PROPERTIES_DEF_ENTITY, },
        {PropertyDefEntity.ENTITY_PREFIX,
         ConfigElementLoader.PROPERTY_DEF_ENTITY},
    };

    private final Config _config;
    private boolean _validationEnabled;

    /**
     * Configuration handler.
     */
    class ConfigHandler
        extends ConfigDocumentLoader.RootHandler
    {
        /** {@inheritDoc}
         */
        @Override
        protected void onRootEnd()
        {
            super.onRootEnd();

            if (isEnabled()) {
                final Optional<String> stampAttribute = getAttribute(
                    ConfigElementLoader.STAMP_ATTRIBUTE);
                final Optional<DateTime> stamp = stampAttribute
                    .isPresent()? Optional
                        .of(
                            DateTime
                                    .now()
                                    .valueOf(stampAttribute.get())): Optional
                                            .empty();

                getConfig().updateStamp(stamp);
            }
        }
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
